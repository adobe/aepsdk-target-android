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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private TargetState targetState;
    private TargetRequestBuilder targetRequestBuilder;
    private TargetPreviewManager targetPreviewManager;

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

        targetState = new TargetState(dataStore);
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
    public boolean readyForEvent(@NonNull Event event) {
        targetState.updateConfigurationSharedState(retrieveConfigurationSharedState(event));
        return targetState.getLastKnownConfigurationState() != null;
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

            String propertyToken = targetState.getPropertyToken();
            if (StringUtils.isNullOrEmpty(propertyToken)) {
                final Map<String, Object> property = DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.PROPERTY);
                if (!TargetUtils.isNullOrEmpty(property)) {
                    propertyToken = DataReader.getString(property, TargetConstants.EventDataKeys.TOKEN);
                }
            }

            long environmentId = targetState.getEnvironmentId();
            if (environmentId == 0) {
                environmentId = DataReader.getLong(eventData, TargetConstants.EventDataKeys.ENVIRONMENT_ID);
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

            final String sendRequestError = prepareForTargetRequest();
            if (sendRequestError != null) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "handleRawRequest - ", sendRequestError);
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            targetRequestBuilder.setIdentityData(retrieveIdentitySharedState(event));

            final JSONObject defaultJsonObject = targetRequestBuilder.getDefaultJsonObject(
                    id,
                    context,
                    experienceCloud,
                    environmentId);

            final JSONObject payloadJson = targetRequestBuilder.getRequestPayload(
                    defaultJsonObject,
                    prefetch,
                    execute,
                    notifications,
                    propertyToken);

            if (payloadJson == null || payloadJson.length() == 0) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                        "handleRawRequest - Cannot send raw Target request, payload json is null or empty.");
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            final String url = getTargetRequestUrl(targetState.getTargetServer(), targetState.getClientCode());
            final String payloadJsonString = payloadJson.toString();
            final byte[] payload = payloadJsonString.getBytes(StandardCharsets.UTF_8);
            final Map<String, String> headers = new HashMap<>();
            headers.put(NetworkingConstants.Headers.CONTENT_TYPE, NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION);
            int timeout = targetState.getNetworkTimeout();

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
            targetRequestBuilder = new TargetRequestBuilder(deviceInfoService, previewManager, targetState);
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
     * @return a {@code String} error indicating why the request can't be sent, null otherwise
     */
    private String prepareForTargetRequest() {
        if (targetState.getClientCode().isEmpty()) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "prepareForTargetRequest - TargetRequest preparation failed because (%s)", TargetErrors.NO_CLIENT_CODE);
            return TargetErrors.NO_CLIENT_CODE;
        }

        if (targetState.getMobilePrivacyStatus() != MobilePrivacyStatus.OPT_IN) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "prepareForTargetRequest - TargetRequest preparation failed because (%s)", TargetErrors.OPTED_OUT);
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
     * @param clientCode the value of "target.clientCode" in configuration
     * @return the server url string
     */
    protected String getTargetRequestUrl(final String customServer, final String clientCode) {
        // If customServer is not empty return targetRequestUrl with customServer as host
        if (!customServer.isEmpty()) {
            return String.format(TargetConstants.PREFETCH_API_URL_BASE, customServer, clientCode, targetState.getSessionId());
        }

        final String edgeHost = targetState.getEdgeHost();
        final String host = StringUtils.isNullOrEmpty(edgeHost) ?
                String.format(TargetConstants.API_URL_HOST_BASE, clientCode)
                : edgeHost;
        return String.format(TargetConstants.PREFETCH_API_URL_BASE, host, clientCode, targetState.getSessionId());
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
            targetState.updateSessionTimestamp(false);
            final JSONObject idJson = responseJson.optJSONObject(TargetJson.ID);
            if (idJson != null) {
                targetState.updateTntId(idJson.getString(TargetJson.ID_TNT_ID));
            }
            targetState.updateEdgeHost(responseJson.optString(TargetJson.EDGE_HOST, ""));

            getApi().createSharedState(targetState.generateSharedState(), event);
            dispatchTargetRawResponseIfNeeded(isContentRequest,  JSONUtils.toMap(responseJson), event);
        } catch (final JSONException e) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "processTargetRawResponse - (%s) " + TargetErrors.NULL_RESPONSE_JSON);
            dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
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
}