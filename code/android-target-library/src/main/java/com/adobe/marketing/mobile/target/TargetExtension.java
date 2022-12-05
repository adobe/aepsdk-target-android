/*
 Copyright 2022 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.target;

import androidx.annotation.NonNull;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.NetworkingConstants;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.StreamUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.TimeUtils;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adobe Target is the Adobe Marketing Cloud solution that provides everything you need to tailor and personalize your customers
 * experience on mobile application.
 * <p>
 * It helps you to deliver targeted content within your mobile Application.
 * The extension will provide a way to react to public facing Target APIs to make target request and get responses through asynchronous callbacks.
 * Returns an instance of the Target extension.  Adds all the Listeners to the provided EventHub.
 * The Target extension listens for the following {@link Event}s:
 * <ol>
 * <li>{@link EventType#TARGET} - {@link EventSource#REQUEST_CONTENT}</li>
 * <li>{@code EventType#TARGET} - {@link EventSource#REQUEST_RESET}</li>
 * <li>{@code EventType#TARGET} - {@link EventSource#REQUEST_IDENTITY}</li>
 * <li>{@code EventType#GENERIC_DATA} - {@link EventSource#OS}</li>
 * <li>{@link EventType#CONFIGURATION} - {@link EventSource#RESPONSE_CONTENT}</li>
 * </ol>
 * <p>
 * The Target extension dispatches the following {@code Event}s:
 * <ol>
 * <li>{@code EventType#TARGET} - {@code EventSource#RESPONSE_CONTENT}</li>
 * <li>{@code EventType#TARGET} - {@code EventSource#RESPONSE_IDENTITY}</li>
 * <li>{@code EventType#TARGET} - {@code EventSource#RESPONSE_IDENTITY}</li>
 * </ol>
 * <p>
 * The Target extension has dependencies on the following {@link ServiceProvider} services:
 * <ol>
 * <li>{@link DataStoring}</li>
 * <li>{@link Networking}</li>
 * </ol>
 *
 * @see ServiceProvider
 */
public class TargetExtension extends Extension {
    private static final String CLASS_NAME = "TargetExtension";
    private static final String TARGET_EVENT_DISPATCH_MESSAGE = "Dispatching - Target response content event";

    private DeviceInforming deviceInfoService;
    private NamedCollection dataStore;
    private Networking networkService;
    private final UIService uiService;

    protected String tntId;
    protected String thirdPartyId;
    protected String edgeHost;
    protected String clientCode;
    protected Map<String, JSONObject> prefetchedMbox;
    protected Map<String, JSONObject> loadedMbox;
    protected List<JSONObject> notifications;
    protected Map<String, Object> lastKnownConfigurationState;

    private TargetRequestBuilder targetRequestBuilder;
    private TargetPreviewManager targetPreviewManager;

    // ================================================================================
    // session variables
    // ================================================================================
    protected String sessionId = null;
    protected long sessionTimestampInSeconds = 0L;

    /**
     * Constructor for {@code TargetExtension}.
     * <p>
     * It is invoked during the extension registration to retrieve the extension's details such as name and version.
     *
     * @param extensionApi {@link ExtensionApi} instance.
     */
    protected TargetExtension(final ExtensionApi extensionApi) {
        super(extensionApi);

        deviceInfoService = ServiceProvider.getInstance().getDeviceInfoService();
        dataStore = ServiceProvider.getInstance().getDataStoreService().getNamedCollection(TargetConstants.DATA_STORE_KEY);
        networkService = ServiceProvider.getInstance().getNetworkService();
        uiService = ServiceProvider.getInstance().getUIService();

        prefetchedMbox = new HashMap<>();
        loadedMbox = new HashMap<>();
        notifications = new ArrayList<>();
    }

    /**
     * Retrieve the extension name.
     *
     * @return {@link String} containing the unique name for this extension.
     */
    @NonNull
    @Override
    protected String getName() {
        return TargetConstants.EXTENSION_NAME;
    }

    /**
     * Retrieve the extension friendly name.
     *
     * @return {@link String} containing the friendly name for this extension.
     */
    @Override
    protected String getFriendlyName() { return TargetConstants.FRIENDLY_NAME; }

    /**
     * Retrieve the extension version.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    @Override
    protected String getVersion() {
        return TargetConstants.EXTENSION_VERSION;
    }

    @Override
    protected void onRegistered() {
        getApi().registerEventListener(EventType.TARGET, EventSource.REQUEST_CONTENT, this::handleTargetRequestContentEvent);
        getApi().registerEventListener(EventType.TARGET, EventSource.REQUEST_RESET, this::handleTargetRequestResetEvent);
        getApi().registerEventListener(EventType.TARGET, EventSource.REQUEST_IDENTITY, this::handleTargetRequestIdentityEvent);
        getApi().registerEventListener(EventType.GENERIC_DATA, EventSource.OS, this::handleGenericDataOSEvent);
        getApi().registerEventListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT, this::handleConfigurationResponseContentEvent);
    }

    void handleTargetRequestContentEvent(final Event event) {
        if (event == null || TargetUtils.isNullOrEmpty(event.getEventData())) {
            Log.trace(TargetConstants.LOG_TAG, CLASS_NAME,
                    "handleTargetRequestContentEvent - Failed to process Target request content event, event is null or event data is null/ empty.");
            return;
        }

        final Map<String, Object> eventData = event.getEventData();

        if (DataReader.optBoolean(eventData, TargetConstants.EventDataKeys.IS_RAW_EVENT, false)) {
            handleRawRequest(event);
            return;
        }

        if (DataReader.optTypedList(TargetPrefetch.class, eventData, TargetConstants.EventDataKeys.PREFETCH, null) != null) {
            handleMboxPrefetch(event);
            return;
        }

        if (DataReader.optTypedList(TargetRequest.class, eventData, TargetConstants.EventDataKeys.PREFETCH, null) != null) {
            loadRequests(event);
            return;
        }

        if ( DataReader.optBoolean(eventData, TargetConstants.EventDataKeys.IS_LOCATION_DISPLAYED, false)) {
            handleLocationsDisplayed(event);
            return;
        }

        if (DataReader.optBoolean(eventData, TargetConstants.EventDataKeys.IS_LOCATION_CLICKED, false)) {
            handleLocationClicked(event);
            return;
        }

        final String restartDeeplink = DataReader.optString(eventData, TargetConstants.EventDataKeys.PREVIEW_RESTART_DEEP_LINK, null);
        if (!StringUtils.isNullOrEmpty(restartDeeplink)) {
            setPreviewRestartDeepLink(restartDeeplink);
        }
    }

    void handleTargetRequestResetEvent(final Event event) {
    }

    void handleTargetRequestIdentityEvent(final Event event) {
    }

    void handleGenericDataOSEvent(final Event event) {
    }

    void handleConfigurationResponseContentEvent(final Event event) {
    }

    /**
     * Sets the preview restart url in the target preview manager.
     *
     * @param deepLink the {@link String} deep link received from the public API - SetPreviewRestartDeeplink
     */
    void setPreviewRestartDeepLink(final String deepLink) {
        // todo
    }

    /**
     * Executes a raw Target prefetch or execute request for the provided request event data.
     *
     * @param event the incoming {@link Event} object.
     */
    void handleRawRequest(final Event event) {
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "Processing the raw Target request - event %s type: %s source: %s ",
                event.getName(), event.getType(),
                event.getSource());

        final Map<String, Object> eventData = event.getEventData();

        if (eventData == null || eventData.isEmpty()) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    "handleRawRequest - Cannot process the raw Target request, provided request map is null or empty.");
            return;
        }

        try {
            final Map<String, Object> id =  DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.ID);
            final Map<String, Object> context = DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.CONTEXT);
            final Map<String, Object> experienceCloud =  DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.EXPERIENCE_CLOUD);
            final Map<String, Object> execute = DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.EXECUTE);
            final Map<String, Object> prefetch =  DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.PREFETCH);
            final List<Map<String, Object>> notifications =  DataReader.getTypedListOfMap(Object.class, eventData, TargetConstants.EventDataKeys.NOTIFICATIONS);
            final long environmentId = DataReader.getLong(eventData, TargetConstants.EventDataKeys.ENVIRONMENT_ID);
            final Map<String, Object> property = DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.PROPERTY);
            String propertyToken = null;
            if (!TargetUtils.isNullOrEmpty(property)) {
                propertyToken = DataReader.getString(property, TargetConstants.EventDataKeys.TOKEN);
            }
            final boolean isContentRequest = (prefetch != null) || (execute != null);

            if (networkService == null) {
                Log.error(TargetConstants.LOG_TAG, CLASS_NAME, "handleRawRequest - (%s)", TargetErrors.NETWORK_SERVICE_UNAVAILABLE);
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            targetRequestBuilder = getRequestBuilder();
            if (targetRequestBuilder == null) {
                Log.error(TargetConstants.LOG_TAG, CLASS_NAME, "handleRawRequest - (%s)", TargetErrors.REQUEST_BUILDER_INIT_FAILED);
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            targetRequestBuilder.clean();
            targetRequestBuilder.setTargetInternalParameters(getTntId(), getThirdPartyId());

            final Map<String, Object> configData = retrieveConfigurationSharedState(event);

            final String sendRequestError = prepareForTargetRequest(configData);
            if (sendRequestError != null) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "handleRawRequest - ", sendRequestError);
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            String configPropertyToken = "";
            long configEnvironmentId = 0L;
            if (TargetUtils.isNullOrEmpty(configData)) {
                configEnvironmentId = DataReader.optLong(configData, TargetConstants.Configuration.TARGET_ENVIRONMENT_ID, 0L);
                targetRequestBuilder.setConfigParameters(configEnvironmentId);
                configPropertyToken = DataReader.optString(configData, TargetConstants.Configuration.TARGET_PROPERTY_TOKEN, "");
            }

            targetRequestBuilder.setIdentityData(retrieveIdentitySharedState(event));

            final JSONObject defaultJsonObject = targetRequestBuilder.getDefaultJsonObject(
                    id,
                    context,
                    experienceCloud,
                    (configEnvironmentId != 0L) ? configEnvironmentId : environmentId);

            final JSONObject payloadJson = targetRequestBuilder.getRequestPayload(
                    defaultJsonObject,
                    prefetch,
                    execute,
                    notifications,
                    StringUtils.isNullOrEmpty(configPropertyToken) ? propertyToken : configPropertyToken);

            if (payloadJson == null || payloadJson.length() == 0) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                        "handleRawRequest - Cannot send raw Target request, payload json is null or empty.");
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            final String url = getTargetRequestUrl(DataReader.optString(configData, TargetConstants.Configuration.TARGET_SERVER, ""));
            final String payloadJsonString = payloadJson.toString();
            final byte[] payload = payloadJsonString.getBytes(StandardCharsets.UTF_8);
            final Map<String, String> headers = new HashMap<>();
            headers.put(NetworkingConstants.Headers.CONTENT_TYPE, NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION);
            final int timeout = (configData != null)
                    ? DataReader.optInt(configData, TargetConstants.Configuration.TARGET_NETWORK_TIMEOUT, TargetConstants.DEFAULT_NETWORK_TIMEOUT)
                    : TargetConstants.DEFAULT_NETWORK_TIMEOUT;

            Log.debug(TargetConstants.LOG_TAG, "handleRawRequest - Target request was sent with url %s, body %s", url, payloadJsonString);
            final NetworkRequest networkRequest = new NetworkRequest(url, HttpMethod.POST, payload, headers, timeout, timeout);
            networkService.connectAsync(networkRequest, connection -> {
                processTargetRawResponse(connection, isContentRequest, event);
                connection.close();
            });
        } catch (final DataReaderException e) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "handleRawRequest - Cannot process the raw Target request, the provided request data is invalid (%s).", e.getMessage());
        }
    }

    /**
     * Processes the network response after the Target delivery API call for raw request.
     *
     * @param connection  a {@link HttpConnecting} instance.
     * @param isContentRequest {@code boolean} indicating whether it's a prefetch or execute request.
     * @param event  the incoming {@link Event} object.
     */
    private void processTargetRawResponse(final HttpConnecting connection,
                                          final boolean isContentRequest,
                                          final Event event) {
        if (connection == null) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "processTargetRawResponse - (%s)", TargetErrors.NO_CONNECTION);
            dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
            return;
        }

        try {
            final JSONObject responseJson = new JSONObject(StreamUtils.readAsString(connection.getInputStream()));
            final int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                        "processTargetRawResponse - Received Target response with connection code: " + responseCode);

                final String responseError = StreamUtils.readAsString(connection.getErrorStream());
                if (!StringUtils.isNullOrEmpty(responseError)) {
                    Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.ERROR_RESPONSE + responseError);
                }

                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            // save the network request timestamp for computing the session id expiration
            updateSessionTimestamp();

            final JSONObject idJson = responseJson.optJSONObject(TargetJson.ID);
            setTntIdInternal(idJson == null ? null : idJson.getString(TargetJson.ID_TNT_ID));
            setEdgeHost(responseJson.optString(TargetJson.EDGE_HOST, ""));

            getApi().createSharedState(packageState(), event);
            dispatchTargetRawResponseIfNeeded(isContentRequest,  JSONUtils.toMap(responseJson), event);
        } catch (final Exception e) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "processTargetRawResponse - (%s)", e.getMessage());
        }
    }

    /**
     * Prefetch multiple Target mboxes in a single network call.
     * The content will be cached locally and returned immediately if any prefetched mbox is requested though a loadRequest call.
     *
     * @param event                  the {@link Event} object
     */
    void handleMboxPrefetch(final Event event) {
        // todo
    }

    /**
     * Request multiple Target mboxes in a single network call.
     *
     * @param event            the {@link Event} object
     */
    void loadRequests(final Event event) {
        // todo
    }

    /**
     * Sends display notifications to Target
     * <p>
     * Reads the display tokens from the cache either {@link #prefetchedMbox} or {@link #loadedMbox} to send the display notifications.
     * The display notification is not sent if,
     * <ol>
     *     <li> Target Extension is not configured.</li>
     *     <li> Privacy status is opted-out or opt-unknown.</li>
     *     <li> If the mboxes are either loaded previously or not prefetched.</li>
     * </ol>
     *
     * @param event            the {@link Event} object
     */
    void handleLocationsDisplayed(final Event event) {
        // todo
    }

    /**
     * Sends a click notification to Target if click metrics are enabled for the provided location name.
     * <p>
     * Reads the clicked token from the cached either {@link #prefetchedMbox} or {@link #loadedMbox} to send the click notification.
     * The clicked notification is not sent if,
     * <ol>
     *     <li> Target Extension is not configured.</li>
     *     <li> Privacy status is opted-out or opt-unknown.</li>
     *     <li> If the mbox is either not prefetched or loaded previously.</li>
     *     <li> If the clicked token is empty or null for the loaded mbox.</li>
     * </ol>
     *
     * @param event            the {@link Event} object
     */
    void handleLocationClicked(final Event event) {
        // todo
    }

    /**
     * Gets the {@code TargetRequestBuilder} instance used to build the json request.
     *
     * @return the {@link TargetRequestBuilder} instance
     */
    private TargetRequestBuilder getRequestBuilder() {
        if (targetRequestBuilder == null) {
            if (deviceInfoService == null) {
                Log.error(TargetConstants.LOG_TAG, CLASS_NAME, "Unable to get the request builder, Device Info services are not available");
                return null;
            }

            final TargetPreviewManager previewManager = getPreviewManager();
            targetRequestBuilder = new TargetRequestBuilder(deviceInfoService, previewManager);
        }

        return targetRequestBuilder;
    }

    /**
     * Gets the {@code TargetPreviewManager} instance.
     * <p>
     * Returns null if either of {@link Networking} or {@link UIService} is null.
     *
     * @return the {@link TargetPreviewManager} instance
     */
    private TargetPreviewManager getPreviewManager() {
        if (targetPreviewManager == null) {
            if (networkService == null) {
                Log.error(TargetConstants.LOG_TAG, CLASS_NAME, "Unable to get the request builder, Network services are not available");
                return null;
            } else if (uiService == null) {
                Log.error(TargetConstants.LOG_TAG, CLASS_NAME, "Unable to get the request builder, UI services are not available");
                return null;
            }
            targetPreviewManager = new TargetPreviewManager(networkService, uiService, getApi());
        }

        return targetPreviewManager;
    }

    /**
     * Prepares for the target requests and checks whether a target request can be sent.
     *
     * @param configData the shared state of {@code Configuration} extension
     * @return a {@code String} error indicating why the request can't be sent, null otherwise
     */
    private String prepareForTargetRequest(final Map<String, Object> configData) {
        if (configData == null) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "prepareForTargetRequest - TargetRequest preparation failed because configData is null");
            return TargetErrors.CONFIG_NULL;
        }

        final String newClientCode = DataReader.optString(configData, TargetConstants.Configuration.TARGET_CLIENT_CODE, "");

        if (newClientCode.isEmpty()) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "prepareForTargetRequest - TargetRequest preparation failed because client code is empty");
            return TargetErrors.NO_CLIENT_CODE;
        }

        if (!newClientCode.equals(clientCode)) {
            clientCode = newClientCode;
            edgeHost = null;
        }

        final MobilePrivacyStatus privacyStatus = getMobilePrivacyStatus();

        if (privacyStatus != MobilePrivacyStatus.OPT_IN) {
            return TargetErrors.OPTED_OUT;
        }

        return null;
    }

    /**
     * <p>
     * This method will determine the correct host name to use.
     * In order, the priority is:
     * 1. Custom value set in "target.server" configuration setting (see customServer param)
     * 2. Edge host returned from previous Target server responses
     * 3. Default host created with customer's client code
     * {@code String} sessionId and {@code String} clientCode are also sent as query parameters in the Target request url.
     *
     * @param customServer the value of "target.server" in configuration
     * @return the server url string
     */
    protected String getTargetRequestUrl(final String customServer) {
        // If customServer is not empty return targetRequestUrl with customServer as host
        if (!customServer.isEmpty()) {
            return String.format(TargetConstants.PREFETCH_API_URL_BASE, customServer, clientCode, getSessionId());
        }

        final String edgeHost = getEdgeHost();
        final String host = StringUtils.isNullOrEmpty(edgeHost) ?
                String.format(TargetConstants.API_URL_HOST_BASE, clientCode)
                : edgeHost;
        return String.format(TargetConstants.PREFETCH_API_URL_BASE, host, clientCode, getSessionId());
    }

    /**
     * Loads edgeHost from shared preferences; if not found or there was an error while reading from shared preferences, it returns null.
     * <p>
     * If current session expired, the edge host is reset to null (in memory and persistence), so
     * the next network call will use the target client code.
     * <p>
     *
     * @return the edgeHost {@link String} value
     */
    String getEdgeHost() {
        if (dataStore != null) {
            // If the session expired reset the edge host
            if (isSessionExpired()) {
                setEdgeHost(null);
                Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "getEdgeHost - Resetting edge host to null as session id expired.");
            } else if (StringUtils.isNullOrEmpty(edgeHost)) {
                edgeHost = dataStore.getString(TargetConstants.DataStoreKeys.EDGE_HOST, null);
                Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "getEdgeHost - Retrieved edge host from the data store.");
            }
        }

        return edgeHost;
    }

    /**
     * Saves the new edge host to local variable and in the shared preference
     *
     * @param newEdgeHost new edge host for the Target call which is saved in shared preference
     */
    void setEdgeHost(final String newEdgeHost) {
        if ((edgeHost == null && newEdgeHost == null) || (edgeHost != null && edgeHost.equals(newEdgeHost))) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "setEdgeHost - Data store is not updated as the provided edge host is same as the existing edgeHost");
            return;
        }

        edgeHost = newEdgeHost;

        if (dataStore != null) {
            if (StringUtils.isNullOrEmpty(edgeHost)) {
                Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "setEdgeHost - EdgeHost is null or empty");
                dataStore.remove(TargetConstants.DataStoreKeys.EDGE_HOST);
            } else {
                dataStore.setString(TargetConstants.DataStoreKeys.EDGE_HOST, edgeHost);
            }
        }
    }

    /**
     * Get the session id either from memory or from the datastore if session is not expired.
     *
     * <p>
     * Context: AMSDK-8217
     * Retrieves the session ID from memory. If no value in memory, it reads the value from
     * persistence. If no value is stored in persistence either, it generates a random UUID,
     * sets current timestamp for {@code long} sessionTimestampInSeconds and saves them in persistence.
     * The session id is refreshed after
     * {@link TargetConstants.Configuration#TARGET_SESSION_TIMEOUT} (secs) of inactivity
     * (from the last successful target request).
     * <p>
     * The SessionId is sent in URL query parameters for each Target Network call; based on this Target will
     * route all requests from a session to the same edge to prevent overwriting the profiles.
     * <p>
     *
     * @return the session id value as {@link String}, null if it couldn't be read from persistence
     */
    String getSessionId() {
        if (StringUtils.isNullOrEmpty(sessionId) && dataStore != null) {
            sessionId = dataStore.getString(TargetConstants.DataStoreKeys.SESSION_ID, null);
        }

        // if there is no session id persisted in local data store or if the session id is expired
        // because there was no activity for more than certain amount of time
        // (from the last successful network request), then generate a new session id, save it in persistence storage
        if (StringUtils.isNullOrEmpty(sessionId) || isSessionExpired()) {
            sessionId = UUID.randomUUID().toString();

            if (dataStore != null) {
                dataStore.setString(TargetConstants.DataStoreKeys.SESSION_ID, sessionId);
            }

            // update session id timestamp when the new session id is generated
            updateSessionTimestamp();
        }

        return sessionId;
    }


    /**
     * Verifies if current target session is expired.
     *
     * @return {@code boolean} indicating whether Target session has expired
     */
    private boolean isSessionExpired() {
        if (sessionTimestampInSeconds <= 0 && dataStore != null) {
            sessionTimestampInSeconds = dataStore.getLong(TargetConstants.DataStoreKeys.SESSION_TIMESTAMP, 0L);
        }

        final long currentTimeSeconds = TimeUtils.getUnixTimeInSeconds();
        final int sessionTimeoutInSec = getSessionTimeout();

        return (sessionTimestampInSeconds > 0) && ((currentTimeSeconds - sessionTimestampInSeconds) > sessionTimeoutInSec);
    }

    /**
     * Get the session timeout from config or default session timeout
     *
     * @return {@code int} session timeout from config or default session timeout {@code int} TargetConstants#DEFAULT_TARGET_SESSION_TIMEOUT_SEC
     */
    private int getSessionTimeout() {
        int sessionTimeoutInSec = TargetConstants.DEFAULT_TARGET_SESSION_TIMEOUT_SEC;

        if (TargetUtils.isNullOrEmpty(lastKnownConfigurationState)
                && lastKnownConfigurationState.containsKey(TargetConstants.Configuration.TARGET_SESSION_TIMEOUT)) {
            sessionTimeoutInSec = DataReader.optInt(lastKnownConfigurationState,
                    TargetConstants.Configuration.TARGET_SESSION_TIMEOUT,
                    TargetConstants.DEFAULT_TARGET_SESSION_TIMEOUT_SEC);
        }

        return sessionTimeoutInSec;
    }

    /**
     * Updates {@code long} sessionTimestampInSeconds to current timestamp and stores the new
     * value in persistence.
     *
     * <p>
     * Note: this method needs to be called after each successful target network request in order
     * to compute the session id expiration date properly.
     * <p>
     */
    private void updateSessionTimestamp() {
        sessionTimestampInSeconds = TimeUtils.getUnixTimeInSeconds();
        if (dataStore != null) {
            Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "updateSessionTimestamp - Attempting to update the session timestamp");
            dataStore.setLong(TargetConstants.DataStoreKeys.SESSION_TIMESTAMP, sessionTimestampInSeconds);
        }
    }


    /**
     * Dispatches a Target response event for a raw prefetch or execute request.
     *
     * @param isContentRequest a {@code boolean} indicating whether it is a prefetch or execute request.
     * @param responseData (Nullable) {@code List<Map<String, Object>>} containing the raw request response for the mboxes.
     * @param requestEvent  (required) {@link Event} the associated Target request content event.
     */
    private void dispatchTargetRawResponseIfNeeded(final boolean isContentRequest, final Map<String, Object> responseData,
                                                   final Event requestEvent) {
        if (!isContentRequest) {
            return;
        }
        dispatchTargetRawResponse(responseData, requestEvent);
    }

    /**
     * Dispatches the Target response content event for a raw prefetch or execute request.
     *
     * @param responseData (Nullable) {@code List<Map<String, Object>>} containing the raw execute request response for the mboxes.
     * @param requestEvent  (required) {@link Event} the associated TargetRequestContent event.
     */
    void dispatchTargetRawResponse(final Map<String, Object> responseData, final Event requestEvent) {

        final Map<String, Object> data = new HashMap<>();
        data.put(TargetConstants.EventDataKeys.RESPONSE_DATA, responseData);

        // Create Event and dispatch to EventHub
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "dispatchTargetRawResponse - (%s) ", TARGET_EVENT_DISPATCH_MESSAGE);
        Event responseEvent = new Event.Builder(TargetConstants.EventName.TARGET_RAW_RESPONSE_EVENT_NAME, EventType.TARGET,
                EventSource.RESPONSE_CONTENT).setEventData(data).inResponseToEvent(requestEvent).build();
        getApi().dispatch(responseEvent);
    }

    private Map<String, Object> retrieveIdentitySharedState(final Event event) {
        final SharedStateResult identitySharedState = getApi().getSharedState(TargetConstants.Identity.EXTENSION_NAME, event, false, SharedStateResolution.ANY);
        return identitySharedState != null ? identitySharedState.getValue() : null;
    }

    private Map<String, Object> retrieveConfigurationSharedState(final Event event) {
        final SharedStateResult configSharedState = getApi().getSharedState(TargetConstants.Configuration.EXTENSION_NAME, event, false, SharedStateResolution.ANY);
        return configSharedState != null ? configSharedState.getValue() : null;
    }

    /**
     * Get {@code MobilePrivacyStatus} for this Target extension.
     *
     * @return {@link MobilePrivacyStatus} from the last known Configuration state
     */
    private MobilePrivacyStatus getMobilePrivacyStatus() {

        if (TargetUtils.isNullOrEmpty(lastKnownConfigurationState) ||
                !lastKnownConfigurationState.containsKey(TargetConstants.Configuration.GLOBAL_CONFIG_PRIVACY)) {
            return MobilePrivacyStatus.UNKNOWN;
        }

        final String privacyString = DataReader.optString(lastKnownConfigurationState,
                TargetConstants.Configuration.GLOBAL_CONFIG_PRIVACY, MobilePrivacyStatus.UNKNOWN.getValue());
        return MobilePrivacyStatus.fromString(privacyString);
    }

    /**
     * Saves the tntId and the edge host value derived from it to the Target data store.
     * <p>
     * If a valid tntId is provided and the privacy status is {@link MobilePrivacyStatus#OPT_OUT} or
     * the provided tntId is same as the existing value, then the method returns with no further action.
     * <p>
     * If a null or empty value is provided for the tntId, then both tntId and edge host values are removed from the Target data store.
     *
     * @param newTntId {@link String} containing new tntId that needs to be set.
     */
    private void setTntIdInternal(final String newTntId) {
        // do not set identifier if privacy is opt-out and the id is not being cleared
        if (getMobilePrivacyStatus() == MobilePrivacyStatus.OPT_OUT && !StringUtils.isNullOrEmpty(newTntId)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "setTntIdInternal - Cannot update Target tntId due to opt out privacy status.");
            return;
        }

        if (tntIdValuesAreEqual(tntId, newTntId)) {
            Log.debug(TargetConstants.LOG_TAG,
                    "setTntIdInternal - Won't update Target tntId as provided value is same as the existing tntId value (%s).", newTntId);
            return;
        }

        final String edgeHost = extractEdgeHost(newTntId);

        if (!StringUtils.isNullOrEmpty(edgeHost)) {
            Log.debug(TargetConstants.LOG_TAG, "setTntIdInternal - The edge host value derived from the given tntId (%s) is (%s).",
                    newTntId, edgeHost);
            setEdgeHost(edgeHost);
        } else {
            Log.debug(TargetConstants.LOG_TAG,
                    "setTntIdInternal - The edge host value cannot be derived from the given tntId (%s) and it is removed from the data store.",
                    newTntId);
            setEdgeHost(null);
        }

        Log.trace(TargetConstants.LOG_TAG, "setTntIdInternal - Updating tntId with value (%s).", newTntId);
        tntId = newTntId;

        if (dataStore != null) {
            if (StringUtils.isNullOrEmpty(newTntId)) {
                Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                        "setTntIdInternal - Removed tntId from the data store, provided tntId value is null or empty.");
                dataStore.remove(TargetConstants.DataStoreKeys.TNT_ID);
            } else {
                Log.debug(TargetConstants.LOG_TAG, "setTntIdInternal - Persisted new tntId (%s) in the data store.", newTntId);
                dataStore.setString(TargetConstants.DataStoreKeys.TNT_ID, newTntId);
            }
        } else {
            Log.debug(TargetConstants.LOG_TAG, "setTntIdInternal - " + TargetErrors.TARGET_TNT_ID_NOT_PERSISTED,
                    "Data store is not available.");
        }
    }


    /**
     * Reads the tntId from the Target DataStore and returns null if its unavailable.
     *
     * @return tntId {@link String}
     */
    String getTntId() {
        if (tntId == null && dataStore != null) {
            tntId = dataStore.getString(TargetConstants.DataStoreKeys.TNT_ID, null);
        }
        return tntId;
    }

    /**
     * Compares if the given two tntID's are equal. tntId is a concatenation of {tntId}.{edgehostValue}
     * false is returned when tntID's are different.
     * true is returned when tntID's are same.
     *
     * @param oldId old tntId {@link String}
     * @param newId new tntId {@code String}
     * @return a {@code boolean} variable indicating if the tntId's are equal
     */
    boolean tntIdValuesAreEqual(final String oldId, final String newId) {
        if (oldId == null && newId == null) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "tntIdValuesAreEqual - old and new tntId is null.");
            return true;
        }

        if (oldId == null || newId == null) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "tntIdValuesAreEqual - %s is null.", (oldId == null ? "oldId" : "newId"));
            return false;
        }

        if (oldId.equals(newId)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "tntIdValuesAreEqual - old tntId is equal to new tntId.");
            return true;
        }

        Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "tntIdValuesAreEqual - old tntId is not equal to new tntId.");
        return false;
    }

    /**
     * Packages this extension's state data for sharing.
     *
     * @return {@code Map<String, Object>} of this extension's state data
     */
    private Map<String, Object> packageState() {
        final Map<String, Object> data = new HashMap<>();

        if (!StringUtils.isNullOrEmpty(tntId)) {
            data.put(TargetConstants.EventDataKeys.TNT_ID, tntId);
        }

        if (!StringUtils.isNullOrEmpty(thirdPartyId)) {
            data.put(TargetConstants.EventDataKeys.THIRD_PARTY_ID, thirdPartyId);
        }

        return data;
    }

    /**
     * Derives and returns the edge host value from the provided tntId.
     * <p>
     * The tntId has the format {@literal UUID.<profile location hint>}. The edge host value
     * can be derived from the profile location hint.
     * For example, if the tntId is {@literal 10abf6304b2714215b1fd39a870f01afc.28_20},
     * then the edgeHost will be {@literal mboxedge28.tt.omtrdc.net}.
     * <p>
     * If the provided tntId is null or empty, or if the edge host value cannot be determined
     *
     * @param newTntId {@link String} containing the tntId used to derive the edge host value.
     */
    private String extractEdgeHost(final String newTntId) {
        if (StringUtils.isNullOrEmpty(newTntId)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "extractEdgeHost - Cannot extract Edge host from the provided tntId as it is null or empty.");
            return null;
        }

        final Pattern pattern = Pattern.compile("(?<=[0-9A-Fa-f-]\\.)([\\d][^\\D]*)(?=_)");
        final Matcher matcher = pattern.matcher(newTntId);

        String locationHint = null;

        if (matcher.find()) {
            locationHint = matcher.group();
            Log.debug(TargetConstants.LOG_TAG, "extractEdgeHost - Provided tntId (%s) contains location hint (%s).", newTntId,
                    locationHint);
        }

        String edgeHost = null;

        if (!StringUtils.isNullOrEmpty(locationHint)) {
            edgeHost = String.format(TargetConstants.API_URL_HOST_BASE, String.format(TargetConstants.EDGE_HOST_BASE,
                    locationHint));
            Log.debug(TargetConstants.LOG_TAG, "extractEdgeHost - Edge host (%s) is derived from the provided tntId (%s).",
                    edgeHost, locationHint);
        }

        return edgeHost;
    }

    /**
     * Reads the {@code #thirdPartyId} from the Target DataStore and returns null if its unavailable.
     *
     * @return thirdPartyId {@link String}
     */
    String getThirdPartyId() {
        if (StringUtils.isNullOrEmpty(thirdPartyId) && (dataStore != null)) {
                thirdPartyId = dataStore.getString(TargetConstants.DataStoreKeys.THIRD_PARTY_ID, null);
        }
        return thirdPartyId;
    }

}