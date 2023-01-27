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
import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.Target;
import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.NetworkingConstants;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final DeviceInforming deviceInfoService;
    private final Networking networkService;
    private final UIService uiService;

    private final TargetState targetState;
    private final TargetResponseParser targetResponseParser;
    private final TargetRequestBuilder targetRequestBuilder;
    private final TargetPreviewManager targetPreviewManager;

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
        NamedCollection dataStore = ServiceProvider.getInstance().getDataStoreService().getNamedCollection(TargetConstants.DATA_STORE_KEY);
        networkService = ServiceProvider.getInstance().getNetworkService();
        uiService = ServiceProvider.getInstance().getUIService();

        targetState = new TargetState(dataStore);
        targetResponseParser = new TargetResponseParser();
        targetPreviewManager = new TargetPreviewManager(networkService, uiService);
        targetRequestBuilder = getRequestBuilder();
    }

    /**
     * Constructor for {@code TargetExtension}.
     * <p>
     * Used only for Testing purposes.
     *
     * @param extensionApi {@link ExtensionApi} instance.
     */
    @VisibleForTesting
    protected TargetExtension(final ExtensionApi extensionApi, final DeviceInforming deviceInfoService, final Networking networkService,
                              final UIService uiService, final TargetState targetState, final TargetPreviewManager targetPreviewManager,
                              final TargetRequestBuilder requestBuilder, final TargetResponseParser responseParser) {
        super(extensionApi);
        this.deviceInfoService = deviceInfoService;
        this.networkService = networkService;
        this.uiService = uiService;
        this.targetState = targetState;
        this.targetPreviewManager = targetPreviewManager;
        this.targetRequestBuilder = requestBuilder;
        this.targetResponseParser = responseParser;
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
        return Target.extensionVersion();
    }

    @Override
    public boolean readyForEvent(@NonNull final Event event) {
        targetState.updateConfigurationSharedState(retrieveConfigurationSharedState(event));
        return targetState.getStoredConfigurationSharedState() != null;
    }

    @Override
    protected void onRegistered() {
        getApi().registerEventListener(EventType.TARGET, EventSource.REQUEST_CONTENT, this::handleTargetRequestContentEvent);
        getApi().registerEventListener(EventType.TARGET, EventSource.REQUEST_RESET, this::handleTargetRequestResetEvent);
        getApi().registerEventListener(EventType.TARGET, EventSource.REQUEST_IDENTITY, this::handleTargetRequestIdentityEvent);
        getApi().registerEventListener(EventType.GENERIC_DATA, EventSource.OS, this::handleGenericDataOSEvent);
        getApi().registerEventListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT, this::handleConfigurationResponseContentEvent);
    }

    void handleTargetRequestContentEvent(@NonNull final Event event) {
        if (TargetUtils.isNullOrEmpty(event.getEventData())) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    "handleTargetRequestContentEvent - Failed to process Target request content event, event data is null/ empty.");
            return;
        }

        final Map<String, Object> eventData = event.getEventData();

        if (DataReader.optBoolean(eventData, TargetConstants.EventDataKeys.IS_RAW_EVENT, false)) {
            handleRawRequest(event);
            return;
        }

        if (eventData.containsKey(TargetConstants.EventDataKeys.PREFETCH)) {
            final List<Map<String, Object>> flattenedPrefetchRequests;
            try {
                flattenedPrefetchRequests = (List<Map<String, Object>>) eventData.get(TargetConstants.EventDataKeys.PREFETCH);
            } catch (final ClassCastException e) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                        "handleTargetRequestContentEvent -  Failed to get TargetExtension Prefetch list from event data, %s", e);
                return;
            }
            if (TargetUtils.isNullOrEmpty(flattenedPrefetchRequests)) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                        "handleTargetRequestContentEvent -Failed to retrieve Target Prefetch list (%s)", TargetErrors.NO_PREFETCH_REQUESTS);
                dispatchMboxPrefetchResult(TargetErrors.NO_PREFETCH_REQUESTS, event);
                return;
            }
            final List<TargetPrefetch> targetPrefetchRequests = new ArrayList<>();
            for (Map<String, Object> prefetch : flattenedPrefetchRequests) {
                final TargetPrefetch targetPrefetch = TargetPrefetch.fromEventData(prefetch);
                if (targetPrefetch != null) {
                    targetPrefetchRequests.add(targetPrefetch);
                }
            }
            handleMboxPrefetch(targetPrefetchRequests, event);
            return;
        }

        if (eventData.containsKey(TargetConstants.EventDataKeys.LOAD_REQUEST)) {
            final List<Map<String, Object>> flattenedLocationRequests;
            try {
                flattenedLocationRequests = (List<Map<String, Object>>) eventData.get(TargetConstants.EventDataKeys.LOAD_REQUEST);
            } catch (final ClassCastException e) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                        "handleTargetRequestContentEvent -  Failed to get Target Request list from event data, %s", e);
                return;
            }
            if (TargetUtils.isNullOrEmpty(flattenedLocationRequests)) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                        "handleTargetRequestContentEvent -Failed to retrieve Target location content (%s)", TargetErrors.NO_TARGET_REQUESTS);
                return;
            }
            final List<TargetRequest> targetRequests = new ArrayList<>();
            for (Map<String, Object> request : flattenedLocationRequests) {
                final TargetRequest targetRequest = TargetRequest.fromEventData(request);
                if (targetRequest != null) {
                    targetRequests.add(targetRequest);
                }
            }
            loadRequests(targetRequests, event);
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

    void handleTargetRequestResetEvent(@NonNull final Event event) {
        if (TargetUtils.isNullOrEmpty(event.getEventData())) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    "handleTargetRequestResetEvent - Failed to process Target request content event, event data is null/ empty.");
            return;
        }

        final Map<String, Object> eventData = event.getEventData();

        if (DataReader.optBoolean(eventData, TargetConstants.EventDataKeys.RESET_EXPERIENCE, false)) {
            resetIdentity(event);
            return;
        }
        if (DataReader.optBoolean(eventData, TargetConstants.EventDataKeys.CLEAR_PREFETCH_CACHE, false)) {
            targetState.clearPrefetchedMboxes();
        }
    }

    void handleTargetRequestIdentityEvent(@NonNull final Event event) {
        final Map<String, Object> eventData = event.getEventData();
        if (TargetUtils.isNullOrEmpty(eventData)) {
            dispatchIdentity(event);
            return;
        }

        if (eventData.containsKey(TargetConstants.EventDataKeys.THIRD_PARTY_ID)) {
            final String thirdPartyId = DataReader.optString(eventData, TargetConstants.EventDataKeys.THIRD_PARTY_ID, null);
            setThirdPartyIdInternal(thirdPartyId);
            getApi().createSharedState(targetState.generateSharedState(), event);
        } else if (eventData.containsKey(TargetConstants.EventDataKeys.TNT_ID)) {
            final String tntId = DataReader.optString(eventData, TargetConstants.EventDataKeys.TNT_ID, null);
            setTntIdInternal(tntId);
            getApi().createSharedState(targetState.generateSharedState(), event);
        } else if (eventData.containsKey(TargetConstants.EventDataKeys.SESSION_ID)) {
            final String sessionId = DataReader.optString(eventData, TargetConstants.EventDataKeys.SESSION_ID, null);
            setSessionId(sessionId);
        }
    }

    void handleGenericDataOSEvent(@NonNull final Event event) {
        if (TargetUtils.isNullOrEmpty(event.getEventData())) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    "handleGenericDataOSEvent - Failed to process Generic os event, event data is null/ empty.");
            return;
        }
        final String deepLink = DataReader.optString(event.getEventData(), TargetConstants.PreviewKeys.DEEPLINK, null);

        if (!StringUtils.isNullOrEmpty(deepLink)) {
            setupPreviewMode(deepLink);
        }
    }

    void handleConfigurationResponseContentEvent(@NonNull final Event event) {
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "handleConfigurationResponse - event %s type: %s source: %s ", event.getName(),
                event.getType(), event.getSource());

        if (targetState.getMobilePrivacyStatus() == MobilePrivacyStatus.OPT_OUT) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "handleConfigurationResponse - Clearing saved identities");
            resetIdentity();

            // identifiers are cleared now, set shared state
            getApi().createSharedState(targetState.generateSharedState(), event);
        }
    }

    /**
     * Sets the preview restart url in the target preview manager.
     *
     * @param deepLink the {@link String} deep link received from the public API - SetPreviewRestartDeeplink
     */
    void setPreviewRestartDeepLink(final String deepLink) {
        targetPreviewManager.setRestartDeepLink(deepLink);
    }

    /**
     * Executes a raw Target prefetch or execute request for the provided request event data.
     *
     * @param event the incoming {@link Event} object.
     */
    void handleRawRequest(@NonNull final Event event) {
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "Processing the raw Target request - event %s type: %s source: %s ",
                event.getName(), event.getType(),
                event.getSource());

        final Map<String, Object> eventData = event.getEventData();

        try {
            final Map<String, Object> id =  DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.ID);
            final Map<String, Object> context = DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.CONTEXT);
            final Map<String, Object> experienceCloud =  DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.EXPERIENCE_CLOUD);
            final Map<String, Object> execute = DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.EXECUTE);
            final Map<String, Object> prefetch =  DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.PREFETCH);
            final List<Map<String, Object>> notifications =  DataReader.getTypedListOfMap(Object.class, eventData, TargetConstants.EventDataKeys.NOTIFICATIONS);

            final boolean isContentRequest = (prefetch != null) || (execute != null);

            if (networkService == null) {
                Log.error(TargetConstants.LOG_TAG, CLASS_NAME, "handleRawRequest - (%s)", TargetErrors.NETWORK_SERVICE_UNAVAILABLE);
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            if (targetRequestBuilder == null) {
                Log.error(TargetConstants.LOG_TAG, CLASS_NAME, "handleRawRequest - (%s)", TargetErrors.REQUEST_BUILDER_INIT_FAILED);
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            String propertyToken = targetState.getPropertyToken();
            if (StringUtils.isNullOrEmpty(propertyToken)) {
                final Map<String, Object> property = DataReader.getTypedMap(Object.class, eventData, TargetConstants.EventDataKeys.PROPERTY);
                if (!TargetUtils.isNullOrEmpty(property)) {
                    propertyToken = DataReader.optString(property, TargetConstants.EventDataKeys.TOKEN, "");
                }
            }

            long environmentId = targetState.getEnvironmentId();
            if (environmentId == 0) {
                environmentId = DataReader.optLong(eventData, TargetConstants.EventDataKeys.ENVIRONMENT_ID, 0L);
            }

            final String sendRequestError = prepareForTargetRequest();
            if (sendRequestError != null) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "handleRawRequest - ", sendRequestError);
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            final JSONObject defaultJsonObject = targetRequestBuilder.getDefaultJsonObject(
                    id,
                    context,
                    experienceCloud,
                    environmentId,
                    retrieveIdentitySharedState(event));

            final JSONObject payloadJson = targetRequestBuilder.getRequestPayload(
                    defaultJsonObject,
                    prefetch,
                    execute,
                    notifications,
                    propertyToken);

            if (JSONUtils.isNullOrEmpty(payloadJson)) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                        "handleRawRequest - Cannot send raw Target request, payload json is null or empty.");
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            final String url = getTargetRequestUrl();
            final String payloadJsonString = payloadJson.toString();
            final byte[] payload = payloadJsonString.getBytes(StandardCharsets.UTF_8);
            final Map<String, String> headers = new HashMap<>();
            headers.put(NetworkingConstants.Headers.CONTENT_TYPE, NetworkingConstants.HeaderValues.CONTENT_TYPE_JSON_APPLICATION);
            final int timeout = targetState.getNetworkTimeout();
            final NetworkRequest networkRequest = new NetworkRequest(url, HttpMethod.POST, payload, headers, timeout, timeout);

            Log.debug(TargetConstants.LOG_TAG, "handleRawRequest - Target request was sent with url %s, body %s", url, payloadJsonString);

            networkService.connectAsync(networkRequest, connection -> {
                processTargetRawResponse(connection, isContentRequest, event);
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
     * @param targetPrefetchRequests an {@code List<TargetPrefetch>} representing the desired mboxes to prefetch
     * @param event                  the {@link Event} object
     */
    void handleMboxPrefetch(@NonNull final List<TargetPrefetch> targetPrefetchRequests, @NonNull final Event event) {
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME,
                "handleMboxPrefetch - Prefetched mbox event details - event %s type: %s source: %s ",
                event.getName(), event.getType(), event.getSource());

        if (inPreviewMode()) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.NO_PREFETCH_IN_PREVIEW);
            dispatchMboxPrefetchResult(TargetErrors.NO_PREFETCH_IN_PREVIEW, event);
            return;
        }

        final Map<String, Object> eventData = event.getEventData();
        final Map<String, Object> targetParametersMap = DataReader.optTypedMap(Object.class,
                eventData, TargetConstants.EventDataKeys.TARGET_PARAMETERS, null);
        final TargetParameters targetParameters = TargetParameters.fromEventData(targetParametersMap);
        final Map<String, Object> lifecycleData = retrieveLifecycleSharedState(event);
        final Map<String, Object> identityData = retrieveIdentitySharedState(event);

        prefetchMboxContent(targetPrefetchRequests, targetParameters, lifecycleData, identityData,
                event);
    }

    /**
     * Request multiple Target mboxes in a single network call.
     *
     * @param targetRequests   an {@code List<TargetRequest>} representing the desired mboxes to load
     * @param event            the {@link Event} object
     */
    void loadRequests(@NonNull final List<TargetRequest> targetRequests, @NonNull final Event event) {
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME,
                "loadRequests - event %s type: %s source: %s ",
                event.getName(), event.getType(), event.getSource());
        final Map<String, Object> eventData = event.getEventData();

        final Map<String, Object> targetParametersMap = DataReader.optTypedMap(Object.class,
                eventData, TargetConstants.EventDataKeys.TARGET_PARAMETERS, null);
        final TargetParameters targetParameters = TargetParameters.fromEventData(targetParametersMap);
        final Map<String, Object> lifecycleData = retrieveLifecycleSharedState(event);
        final Map<String, Object> identityData = retrieveIdentitySharedState(event);

        batchRequests(targetRequests, targetParameters, lifecycleData, identityData, event);
    }

    /**
     * Sends display notifications to Target
     * <p>
     * Reads the display tokens from the cache either {@link TargetState#getPrefetchedMbox()} or
     * {@link TargetState#getLoadedMbox()} to send the display notifications.
     * The display notification is not sent if,
     * <ol>
     *     <li> Target Extension is not configured.</li>
     *     <li> Privacy status is opted-out or opt-unknown.</li>
     *     <li> If the mboxes are either loaded previously or not prefetched.</li>
     * </ol>
     *
     * @param event            the {@link Event} object
     */
    void handleLocationsDisplayed(@NonNull final Event event) {
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME,
                "handleLocationsDisplayed - event %s type: %s source: %s ",
                event.getName(), event.getType(), event.getSource());

        final String sendRequestError = prepareForTargetRequest();
        if (sendRequestError != null) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.DISPLAY_NOTIFICATION_SEND_FAILED,
                    sendRequestError);
            return;
        }

        final Map<String, Object> eventData = event.getEventData();
        final List<String> mboxNames = DataReader.optStringList(eventData, TargetConstants.EventDataKeys.MBOX_NAMES, null);
        if (TargetUtils.isNullOrEmpty(mboxNames)) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    "Location displayed unsuccessful (%s) ", TargetErrors.MBOX_NAMES_NULL_OR_EMPTY);
            return;
        }

        final Map<String, Object>  targetParametersMap = DataReader.optTypedMap(Object.class,
                eventData, TargetConstants.EventDataKeys.TARGET_PARAMETERS, null);
        final TargetParameters targetParameters = TargetParameters.fromEventData(targetParametersMap);
        final Map<String, Object> lifecycleData = retrieveLifecycleSharedState(event);
        final Map<String, Object> identityData = retrieveIdentitySharedState(event);

        for (String mboxName : mboxNames) {

            // If loadedMbox contains mboxName then do not send analytics request again
            if (StringUtils.isNullOrEmpty(mboxName) || targetState.getLoadedMbox().containsKey(mboxName)) {
                continue;
            }

            final JSONObject mboxJson;
            if (targetState.getPrefetchedMbox().containsKey(mboxName)) {
                mboxJson = targetState.getPrefetchedMbox().get(mboxName);
            } else {
                Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                        TargetErrors.DISPLAY_NOTIFICATION_SEND_FAILED + TargetErrors.NO_CACHED_MBOX_FOUND,
                        mboxName);
                continue;
            }

            if (!addDisplayNotification(mboxName, mboxJson, targetParameters, lifecycleData, event.getTimestamp())) {
                Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                        "handleLocationsDisplayed - %s mBox not added for display notification.",
                        mboxName);
                continue;
            }

            dispatchAnalyticsForTargetRequest(targetResponseParser.getAnalyticsForTargetPayload(mboxJson,
                    targetState.getSessionId()));
        }

        if (targetState.getNotifications().isEmpty()) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,"handleLocationsDisplayed - " + TargetErrors.DISPLAY_NOTIFICATION_NOT_SENT);
            return;
        }

        sendTargetRequest(null, null, targetParameters,
                lifecycleData,
                identityData, event, connection -> {
                    processNotificationResponse(connection, event);
                });
    }

    /**
     * Sends a click notification to Target if click metrics are enabled for the provided location name.
     * <p>
     * Reads the clicked token from the cached either {@link TargetState#getPrefetchedMbox()} or
     * {@link TargetState#getLoadedMbox()} to send the click notification.
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
    void handleLocationClicked(@NonNull final Event event) {
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME,
                "handleLocationClicked - event %s type: %s source: %s ", event.getName(),
                event.getType(), event.getSource());
        // bail out if the target configuration is not available or if the privacy is opted-out
        final String sendRequestError = prepareForTargetRequest();
        if (sendRequestError != null) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    TargetErrors.CLICK_NOTIFICATION_SEND_FAILED  + sendRequestError);
            return;
        }


        final Map<String, Object> eventData = event.getEventData();
        if (TargetUtils.isNullOrEmpty(eventData)) {
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME,
                    "Location clicked unsuccessful, event data is null or empty ");
            return;
        }

        final String mboxName =  DataReader.optString(eventData, TargetConstants.EventDataKeys.MBOX_NAME, null);
        if (StringUtils.isNullOrEmpty(mboxName)) {
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME,
                    "Location clicked unsuccessful " + TargetErrors.MBOX_NAME_NULL_OR_EMPTY);
            return;
        }

        // Check if the mbox is already prefetched or loaded.
        // if not, Log and bail out
        final JSONObject mboxJson;
        if (targetState.getPrefetchedMbox().containsKey(mboxName)) {
            mboxJson = targetState.getPrefetchedMbox().get(mboxName);
        } else if (targetState.getLoadedMbox().containsKey(mboxName)) {
            mboxJson = targetState.getLoadedMbox().get(mboxName);
        } else {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    TargetErrors.CLICK_NOTIFICATION_SEND_FAILED + TargetErrors.NO_CACHED_MBOX_FOUND,
                    mboxName);
            return;
        }

        final JSONObject clickMetric = targetResponseParser.getClickMetric(mboxJson);
        if (clickMetric == null) {
            Log.warning(TargetConstants.LOG_TAG,
                    TargetErrors.CLICK_NOTIFICATION_SEND_FAILED + TargetErrors.NO_CLICK_METRIC_FOUND,
                    mboxName);
            return;
        }

        // gather the other required shared states
        final Map<String, Object>  targetParametersMap = DataReader.optTypedMap(Object.class,
                eventData, TargetConstants.EventDataKeys.TARGET_PARAMETERS, null);
        final TargetParameters targetParameters = TargetParameters.fromEventData(targetParametersMap);
        final Map<String, Object> lifecycleData = retrieveLifecycleSharedState(event);
        final Map<String, Object> identityData = retrieveIdentitySharedState(event);

        // create and add click notification to the notification list
        if (!addClickedNotificationToList(mboxJson, targetParameters, lifecycleData, event.getTimestamp())) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "handleLocationClicked - %s mBox not added for click notification",
                    mboxName);
            return;
        }

        final Map<String, String> clickMetricA4TParams = targetResponseParser.getAnalyticsForTargetPayload(clickMetric);
        if (!TargetUtils.isNullOrEmpty(clickMetricA4TParams)) {
            dispatchAnalyticsForTargetRequest(targetResponseParser.preprocessAnalyticsForTargetPayload(
                    clickMetricA4TParams, targetState.getSessionId()));
        }

        // send network request
        sendTargetRequest(null, null, targetParameters,
                lifecycleData,
                identityData, event, connection -> processNotificationResponse(connection, event));

    }

    /**
     * Clears all the current identifiers.
     * After clearing the identifiers, creates a shared state at version {@code eventNumber}
     * and dispatches an {@link EventType#TARGET} {@link EventSource#REQUEST_RESET} event.
     *
     * @param event the {@link Event} which triggered this method
     */
    void resetIdentity(@NonNull final Event event) {
        resetIdentity();
        getApi().createSharedState(targetState.generateSharedState(), event);
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
                    "prepareForTargetRequest - TargetRequest preparation failed because (%s)", TargetErrors.NOT_OPTED_IN);
            return TargetErrors.NOT_OPTED_IN;
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
     * @return the server url string
     */
    private String getTargetRequestUrl() {
        // If customServer is not empty return targetRequestUrl with customServer as host
        if (!targetState.getTargetServer().isEmpty()) {
            return String.format(TargetConstants.DELIVERY_API_URL_BASE, targetState.getTargetServer(),
                    targetState.getClientCode(), targetState.getSessionId());
        }

        final String edgeHost = targetState.getEdgeHost();
        final String host = StringUtils.isNullOrEmpty(edgeHost) ?
                String.format(TargetConstants.API_URL_HOST_BASE, targetState.getClientCode())
                : edgeHost;
        return String.format(TargetConstants.DELIVERY_API_URL_BASE, host, targetState.getClientCode(), targetState.getSessionId());
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
            final JSONObject responseJson = targetResponseParser.parseResponseToJson(connection);
            final int responseCode = connection.getResponseCode();
            connection.close();

            if (responseJson == null) {
                Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "processTargetRawResponse - (%s) " + TargetErrors.NULL_RESPONSE_JSON);
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                        "processTargetRawResponse - Received Target response with connection code: " + responseCode);
                final String responseError = targetResponseParser.getErrorMessage(responseJson);
                if (!StringUtils.isNullOrEmpty(responseError)) {
                    Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.ERROR_RESPONSE + responseError);
                }
                dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
                return;
            }

            // save the network request timestamp for computing the session id expiration
            targetState.updateSessionTimestamp(false);
            setTntIdInternal(targetResponseParser.getTntId(responseJson));
            targetState.updateEdgeHost(targetResponseParser.getEdgeHost(responseJson));

            getApi().createSharedState(targetState.generateSharedState(), event);
            dispatchTargetRawResponseIfNeeded(isContentRequest,  JSONUtils.toMap(responseJson), event);
        } catch (final JSONException e) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "processTargetRawResponse - (%s) " + TargetErrors.NULL_RESPONSE_JSON);
            dispatchTargetRawResponseIfNeeded(isContentRequest, null, event);
        }
    }

    /**
     * Internal method to send a Mbox prefetch request.
     *
     * @param targetPrefetchRequests an {@code List<TargetPrefetch>} representing the desired mboxes to preftech
     * @param targetParameters       {@link TargetParameters} object to be passed in all prefetch requests
     * @param lifecycleData          {@code Map<String, Object>} shared state of Lifecycle extension
     * @param identityData           {@code Map<String, Object> shared state of Identity extension
     * @param event          the {@link Event} which triggered this method call
     */
    private void prefetchMboxContent(final List<TargetPrefetch> targetPrefetchRequests,
                                     final TargetParameters targetParameters,
                                     final Map<String, Object> lifecycleData,
                                     final Map<String, Object> identityData,
                                     final Event event) {
        if (TargetUtils.isNullOrEmpty(targetPrefetchRequests)) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    "prefetchMboxContent - Unable to prefetch mbox content, Error %s",
                    TargetErrors.NO_PREFETCH_REQUESTS);
            dispatchMboxPrefetchResult(TargetErrors.NO_PREFETCH_REQUESTS, event);
            return;
        }

        final String sendRequestError = prepareForTargetRequest();
        if (sendRequestError != null) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    "prefetchMboxContent - Unable to prefetch mbox content, Error %s",
                    sendRequestError);
            dispatchMboxPrefetchResult(sendRequestError, event);
            return;
        }

        final String error = sendTargetRequest(null, targetPrefetchRequests, targetParameters,
                lifecycleData, identityData, event, connection -> {

                    if (connection == null) {
                        Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                                "prefetchMboxContent - Unable to prefetch mbox content, Error %s",
                                TargetErrors.NO_CONNECTION);
                        dispatchMboxPrefetchResult(TargetErrors.NO_CONNECTION, event);
                        return;
                    }

                    final JSONObject responseJson = targetResponseParser.parseResponseToJson(connection);
                    final String responseError = targetResponseParser.getErrorMessage(responseJson);
                    final int responseCode = connection.getResponseCode();
                    connection.close();

                    if (responseJson == null) {
                        Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "processTargetRawResponse - (%s) " + TargetErrors.NULL_RESPONSE_JSON);
                        dispatchMboxPrefetchResult(TargetErrors.NULL_RESPONSE_JSON + responseError, event);
                        return;
                    }

                    if (!StringUtils.isNullOrEmpty(responseError)) {
                        if (responseError.contains(TargetErrors.NOTIFICATION_ERROR_TAG)) {
                            targetState.clearNotifications();
                        }
                        Log.error(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.ERROR_RESPONSE + responseError);
                        dispatchMboxPrefetchResult(TargetErrors.ERROR_RESPONSE + responseError, event);
                        return;
                    }

                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                                "prefetchMboxContent - Unable to prefetch mbox content, Error %s",
                                TargetErrors.ERROR_RESPONSE + responseCode);
                        dispatchMboxPrefetchResult(TargetErrors.ERROR_RESPONSE, event);
                        return;
                    }

                    targetState.clearNotifications();

                    // save the network request timestamp for computing the session id expiration
                    targetState.updateSessionTimestamp(false);
                    setTntIdInternal(targetResponseParser.getTntId(responseJson));
                    targetState.updateEdgeHost(targetResponseParser.getEdgeHost(responseJson));

                    getApi().createSharedState(targetState.generateSharedState(), event);

                    final Map<String, JSONObject> prefetchedMboxes = targetResponseParser.extractPrefetchedMboxes(
                            responseJson);
                    if (TargetUtils.isNullOrEmpty(prefetchedMboxes)) {
                        Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.NO_PREFETCH_MBOXES);
                        dispatchMboxPrefetchResult(TargetErrors.NO_PREFETCH_MBOXES, event);
                        return;
                    }

                    targetState.mergePrefetchedMboxJson(prefetchedMboxes);

                    // check if we have duplicates in memory and remove them
                    targetState.removeDuplicateLoadedMboxes();
                    Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                            "prefetchMboxContent - Current cached mboxes : %s, size: %d",
                            Arrays.toString(targetState.getPrefetchedMbox().keySet().toArray()),
                            targetState.getPrefetchedMbox().size());

                    dispatchMboxPrefetchResult(null, event);
                });
        if (!StringUtils.isNullOrEmpty(error)) {
            dispatchMboxPrefetchResult(error, event);
        }
    }

    /**
     * Creates a network connection and sends the request to target server.
     *
     * @param batchRequests    {@link List<TargetRequest>} representing the desired mboxes to load
     * @param prefetchRequests {@link List<TargetPrefetch>} representing the desired mboxes to preftech
     * @param targetParameters {@link TargetParameters} object to be passed in all prefetch requests
     * @param lifecycleData    {@code Map<String, Object>} shared state of Lifecycle extension
     * @param identityData     {@code Map<String, Object>} shared state of Identity} extension
     * @param event    {@link Event} associated Target request content event
     * @param networkCallback {@link NetworkCallback} instance
     */
    private String sendTargetRequest(final List<TargetRequest> batchRequests,
                                     final List<TargetPrefetch> prefetchRequests,
                                     final TargetParameters targetParameters,
                                     final Map<String, Object> lifecycleData,
                                     final Map<String, Object> identityData,
                                     final Event event,
                                     final NetworkCallback networkCallback) {

        if (networkService == null) {
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.NETWORK_SERVICE_UNAVAILABLE);
            return TargetErrors.NETWORK_SERVICE_UNAVAILABLE;
        }

        if (targetRequestBuilder == null) {
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.REQUEST_BUILDER_INIT_FAILED);
            return TargetErrors.REQUEST_BUILDER_INIT_FAILED;
        }

        final Map<String, String> lifecycleContextData = getLifecycleDataForTarget(lifecycleData);
        // Give preference to property token passed in configuration over event data "at_property".
        final String propertyToken = !StringUtils.isNullOrEmpty(targetState.getPropertyToken())
                ? targetState.getPropertyToken()
                : DataReader.optString(event.getEventData(), TargetConstants.EventDataKeys.AT_PROPERTY, "");

        final JSONObject payloadJson = targetRequestBuilder.getRequestPayload(prefetchRequests,
                batchRequests, targetParameters, targetState.getNotifications(), propertyToken,
                identityData, lifecycleContextData);

        if (JSONUtils.isNullOrEmpty(payloadJson)) {
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME,
                    "sendTargetRequest - Unable to send target request, Payload json is (%s)",
                    (payloadJson == null ? "null" : "empty"));
            return TargetErrors.REQUEST_GENERATION_FAILED;
        }

        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", TargetConstants.REQUEST_CONTENT_TYPE);
        final int timeout = targetState.getNetworkTimeout();
        final String url = getTargetRequestUrl();
        final String payloadJsonString = payloadJson.toString();
        final byte[] payload = payloadJsonString.getBytes(StandardCharsets.UTF_8);
        final NetworkRequest networkRequest = new NetworkRequest(url, HttpMethod.POST, payload, headers, timeout, timeout);

        Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                "sendTargetRequest - Target request was sent with url %s, body %s",
                url, payloadJsonString);
        networkService.connectAsync(networkRequest, networkCallback);
        return null;
    }

    /**
     * Internal method to send a batch request.
     * <p>
     *
     * @param targetBatchRequests {@code List<TargetRequest>} representing the desired mboxes to load
     * @param targetParameters    {@link TargetParameters} object to be passed in all prefetch requests
     * @param lifecycleData       {@code Map<String, Object>} shared state of {@code Lifecycle} extension
     * @param identityData        {@code Map<String, Object>} shared state of {@code Identity} extension
     * @param event          {@link Event}  which triggered this method call
     */
    private void batchRequests(final List<TargetRequest> targetBatchRequests,
                               final TargetParameters targetParameters,
                               final Map<String, Object> lifecycleData,
                               final Map<String, Object> identityData,
                               final Event event) {
        if (TargetUtils.isNullOrEmpty(targetBatchRequests)) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    "batchRequests - Unable to process the batch requests, Target Batch Requests are %s.",
                    (targetBatchRequests == null ? "null" : "empty"));
            runDefaultCallbacks(targetBatchRequests, event);
            return;
        }

        final String sendRequestError = prepareForTargetRequest();
        if (sendRequestError != null) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    "batchRequests - Unable to process the batch requests, Error - %s",
                    sendRequestError);
            runDefaultCallbacks(targetBatchRequests, event);
            return;
        }

        final List<TargetRequest> requestsToSend;

        if (!inPreviewMode()) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "Current cached mboxes : %s, size: %d",
                    Arrays.toString(targetState.getPrefetchedMbox().keySet().toArray()), targetState.getPrefetchedMbox().size());
            requestsToSend = processCachedTargetRequest(targetBatchRequests, event);
        } else {
            requestsToSend = targetBatchRequests;
        }

        if (TargetUtils.isNullOrEmpty(requestsToSend) && targetState.getNotifications().isEmpty()) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
                    "Unable to process the batch requests, requests and notifications are empty");
            return;
        }

        final String error = sendTargetRequest(requestsToSend, null, targetParameters,
                lifecycleData, identityData, event, connection -> {
                    processTargetRequestResponse(requestsToSend, connection, event);
                });
        if (!StringUtils.isNullOrEmpty(error)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "batchRequests - " + TargetErrors.NO_CONNECTION);
            runDefaultCallbacks(requestsToSend, event);
        }
    }

    /**
     * Checks if the cached mboxs contain the data for each of the {@code TargetRequest} in the input List.
     * <p>
     * If a cached mbox exists, then dispatch the mbox content.
     *
     * @param batchRequests    an {@code List<TargetRequest>} representing the desired mboxes to load
     * @return {@code List<TargetRequest>} that didn't hit the cache
     */
    List<TargetRequest> processCachedTargetRequest(final List<TargetRequest> batchRequests, final Event event) {
        final List<TargetRequest> requestsToSend = new ArrayList<>();

        for (TargetRequest targetRequest : batchRequests) {
            if (!targetState.getPrefetchedMbox().containsKey(targetRequest.getMboxName())) {
                Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                        "processCachedTargetRequest - (%s) (%s) ",
                        TargetErrors.NO_CACHED_MBOX_FOUND, targetRequest.getMboxName());
                requestsToSend.add(targetRequest);
                continue;
            }

            final JSONObject cachedMboxJson = targetState.getPrefetchedMbox().get(targetRequest.getMboxName());

            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "processCachedTargetRequest - Cached mbox found for %s with data %s",
                    targetRequest.getMboxName(), cachedMboxJson);

            final String content = targetResponseParser.extractMboxContent(cachedMboxJson);
            final Map<String, String> a4tParams = targetResponseParser.getAnalyticsForTargetPayload(cachedMboxJson);
            final Map<String, String> responseTokens = targetResponseParser.getResponseTokens(cachedMboxJson);
            final Map<String, String> clickMetricA4TParams = targetResponseParser.extractClickMetricAnalyticsPayload(
                    cachedMboxJson);

            dispatchMboxContent(StringUtils.isNullOrEmpty(content) ? targetRequest.getDefaultContent() : content,
                    a4tParams, clickMetricA4TParams,
                    responseTokens,
                    targetRequest.getResponsePairId(),
                    event);
        }

        return requestsToSend;
    }

    /**
     * Processes the network response for batch request.
     *
     * @param batchRequests {@code List<TargetRequest>} representing the desired mboxes to load
     * @param connection    {@link HttpConnecting} instance
     * @param event   {@link Event} which triggered this method call
     */
    private void processTargetRequestResponse(final List<TargetRequest> batchRequests,
                                              final HttpConnecting connection,
                                              final Event event) {
        if (connection == null) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "processTargetRequestResponse - (%s)", TargetErrors.NO_CONNECTION);
            runDefaultCallbacks(batchRequests, event);
            return;
        }

        final JSONObject responseJson = targetResponseParser.parseResponseToJson(connection);
        final String responseError = targetResponseParser.getErrorMessage(responseJson);
        final int responseCode = connection.getResponseCode();
        connection.close();

        if (responseJson == null) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "processTargetRequestResponse - (%s) " + TargetErrors.NULL_RESPONSE_JSON);
            runDefaultCallbacks(batchRequests, event);
            return;
        }

        if (!StringUtils.isNullOrEmpty(responseError)) {
            if (responseError.contains(TargetErrors.NOTIFICATION_ERROR_TAG)) {
                targetState.clearNotifications();
            }
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.ERROR_RESPONSE + responseError);
            runDefaultCallbacks(batchRequests, event);
            return;
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME,
                    "processTargetRequestResponse - (%) Error (%s), Error code (%s)",
                    TargetErrors.ERROR_RESPONSE, responseError, responseCode);
            runDefaultCallbacks(batchRequests, event);
            return;
        }

        targetState.clearNotifications();

        // save the network request timestamp for computing the session id expiration
        targetState.updateSessionTimestamp(false);
        setTntIdInternal(targetResponseParser.getTntId(responseJson));
        targetState.updateEdgeHost(targetResponseParser.getEdgeHost(responseJson));

        getApi().createSharedState(targetState.generateSharedState(), event);

        Map<String, JSONObject> batchedMboxes = targetResponseParser.extractBatchedMBoxes(responseJson);
        if (TargetUtils.isNullOrEmpty(batchedMboxes)) {
            runDefaultCallbacks(batchRequests, event);
            return;
        }

        targetState.saveLoadedMbox(batchedMboxes);

        for (TargetRequest targetRequest : batchRequests) {
            if (!batchedMboxes.containsKey(targetRequest.getMboxName())) {
                dispatchMboxContent(targetRequest.getDefaultContent(), null, null, null,
                        targetRequest.getResponsePairId(), event);
                continue;
            }

            final JSONObject mboxJson = batchedMboxes.get(targetRequest.getMboxName());
            final String content = targetResponseParser.extractMboxContent(mboxJson);
            final Map<String, String> responseTokens = targetResponseParser.getResponseTokens(mboxJson);
            final Map<String, String> clickMetricA4TParams = targetResponseParser.extractClickMetricAnalyticsPayload(mboxJson);

            final Map<String, String> a4tParams = targetResponseParser.getAnalyticsForTargetPayload(mboxJson);

            if (!TargetUtils.isNullOrEmpty(a4tParams)) {
                dispatchAnalyticsForTargetRequest(targetResponseParser.getAnalyticsForTargetPayload(mboxJson,
                        targetState.getSessionId()));
            }

            dispatchMboxContent(StringUtils.isNullOrEmpty(content) ? targetRequest.getDefaultContent() : content,
                    a4tParams, clickMetricA4TParams,
                    responseTokens,
                    targetRequest.getResponsePairId(), event);
        }
    }

    /**
     * Adds the display notification for the given mbox to the {@link TargetState#getNotifications()} list.
     *
     * @param mboxName         the displayed mbox name {@link String}
     * @param mboxJson         the cached mbox {@link JSONObject}
     * @param targetParameters {@link TargetParameters} object corresponding to the display location
     * @param lifecycleData    the lifecycle {@code Map<String, Object} that should be added as mbox parameters
     * @param timestamp        {@code long} timestamp associated with the notification event
     * @return {@code boolean} indicating the success of appending the display notification to the notification list
     */
    private boolean addDisplayNotification(final String mboxName, final JSONObject mboxJson,
                                           final TargetParameters targetParameters,
                                           final Map<String, Object> lifecycleData,
                                           final long timestamp) {
        if (targetRequestBuilder == null) {
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.REQUEST_BUILDER_INIT_FAILED);
            return false;
        }

        final Map<String, String> lifecycleContextData = getLifecycleDataForTarget(lifecycleData);
        final JSONObject displayNotificationJson = targetRequestBuilder.getDisplayNotificationJsonObject(mboxName, mboxJson,
                targetParameters, timestamp, lifecycleContextData);

        if (displayNotificationJson == null) {
            Log.debug(TargetConstants.LOG_TAG, "addDisplayNotification - " + TargetErrors.DISPLAY_NOTIFICATION_NULL_FOR_MBOX,
                    mboxName);
            return false;
        }

        targetState.addNotification(displayNotificationJson);
        return true;
    }

    /**
     * Process the network response after the notification network call.
     *
     * @param connection  {@link HttpConnecting} instance
     * @param event the {@link Event} which triggered this method call
     */
    private void processNotificationResponse(final HttpConnecting connection, final Event event) {
        if (connection == null) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "processNotificationResponse - %s", TargetErrors.NO_CONNECTION);
            return;
        }

        final JSONObject responseJson = targetResponseParser.parseResponseToJson(connection);
        final String responseError = targetResponseParser.getErrorMessage(responseJson);
        final int responseCode = connection.getResponseCode();
        connection.close();

        if (responseJson == null) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "processNotificationResponse (%s)" + TargetErrors.NULL_RESPONSE_JSON);
            return;
        }

        if (!StringUtils.isNullOrEmpty(responseError)) {
            if (responseError.contains(TargetErrors.NOTIFICATION_ERROR_TAG)) {
                targetState.clearNotifications();
            }
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.ERROR_RESPONSE + responseError);
            return;
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "processNotificationResponse" + TargetErrors.ERROR_RESPONSE, responseCode);
            return;
        }

        targetState.clearNotifications();

        // save the network request timestamp for computing the session id expiration
        targetState.updateSessionTimestamp(false);
        setTntIdInternal(targetResponseParser.getTntId(responseJson));
        targetState.updateEdgeHost(targetResponseParser.getEdgeHost(responseJson));

        getApi().createSharedState(targetState.generateSharedState(), event);
    }

    /**
     * Adds the clicked notification for the given mbox to the {@link TargetState#getNotifications()} list.
     *
     * @param mboxJson         the clicked notification {@link JSONObject} for the clicked location
     * @param targetParameters {@link TargetParameters} object corresponding to the clicked location
     * @param lifecycleData    the lifecycle {@code Map<String, Object} that should be added to as the mbox parameters
     * @param timestamp        {@code long} timestamp associated with the event, creating the clicked notification
     * @return {@code boolean} indicating the success of appending the click notification to the notification list
     */
    private boolean addClickedNotificationToList(final JSONObject mboxJson,
                                                 final TargetParameters targetParameters,
                                                 final Map<String, Object> lifecycleData,
                                                 final long timestamp) {
        if (targetRequestBuilder == null) {
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.REQUEST_BUILDER_INIT_FAILED);
            return false;
        }

        // set lifecycle data to that targetRequestBuilder
        final Map<String, String> lifecycleContextData = getLifecycleDataForTarget(lifecycleData);
        final JSONObject clickNotificationJson = targetRequestBuilder.getClickNotificationJsonObject(mboxJson, targetParameters,
                    timestamp, lifecycleContextData);

        if (clickNotificationJson == null) {
            Log.debug(TargetConstants.LOG_TAG, "addClickedNotificationToList - %s", TargetErrors.CLICK_NOTIFICATION_NOT_SENT);
            return false;
        }

        targetState.addNotification(clickNotificationJson);
        return true;
    }

    /**
     * Starts preview mode if the deeplink contains the preview query parameters.
     * <p>
     * It then dispatches a new event to messages to create a custom full screen message for target preview.
     * Bail out if the Target configurations are not found or if preview is disabled in Target configuration or if {@code TargetPreviewManager} cannot be instantiated.
     *
     * @param deepLink {@link String} the deep link extracted from the {@link EventType#GENERIC_DATA} {@link EventSource#OS} event
     */
    private void setupPreviewMode(final String deepLink) {
        final String sendRequestError = prepareForTargetRequest();
        if (sendRequestError != null) {
            Log.debug(TargetConstants.LOG_TAG, "setupPreviewMode - " + TargetErrors.TARGET_NOT_ENABLED_FOR_PREVIEW,
                    sendRequestError);
            return;
        }

        if (!targetState.isPreviewEnabled()) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "setupPreviewMode - " + TargetErrors.TARGET_PREVIEW_DISABLED);
            return;
        }

        targetPreviewManager.enterPreviewModeWithDeepLinkParams(targetState.getClientCode(), deepLink);
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
        final Event responseEvent = new Event.Builder(TargetConstants.EventName.TARGET_RAW_RESPONSE_EVENT_NAME, EventType.TARGET,
                EventSource.RESPONSE_CONTENT).setEventData(data).inResponseToEvent(requestEvent).build();
        getApi().dispatch(responseEvent);
    }

    /**
     * Dispatches the Target Mbox Prefetch result.
     *
     * @param error {@code String} prefetch result error, if any.
     * @param event {@code Event} the associated TargetRequestContent event.
     */
    private void dispatchMboxPrefetchResult(final String error, final Event event) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(TargetConstants.EventDataKeys.PREFETCH_ERROR, error);
        eventData.put(TargetConstants.EventDataKeys.PREFETCH_RESULT, error == null);
        // Create Event and dispatch to EventHub
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "dispatchMboxContent - " + TARGET_EVENT_DISPATCH_MESSAGE);
        final Event responseEvent = new Event.Builder(TargetConstants.EventName.PREFETCH_RESPONSE,
                EventType.TARGET,
                EventSource.RESPONSE_CONTENT)
                .setEventData(eventData).inResponseToEvent(event).build();
        getApi().dispatch(responseEvent);
    }

    /**
     * Dispatches the Target Response Content Event.
     *
     * @param content (required) the target content generated by the Target extension object.
     * @param a4tParams (Nullable) the A4T params {@code Map<String, String>} for the Mbox. It will be null if A4T is not enabled on Target.
     * @param clickMetricA4TParams (Nullable) the click metric A4T parameter for the Mbox.
     * @param responseTokens (Nullable) the Response Tokens for the Mbox. It may be null if Response Tokens are not activated on Target.
     * @param pairId  (required) the pairId of the associated TargetRequestContent event.
     * @param event {@code Event} associated target request content event.
     */
    void dispatchMboxContent(final String content,
                             final Map<String, String> a4tParams,
                             final Map<String, String> clickMetricA4TParams,
                             final Map<String, String> responseTokens,
                             final String pairId,
                             final Event event) {
        final Map<String, Object> data = new HashMap<>();
        data.put(TargetConstants.EventDataKeys.TARGET_CONTENT, content);

        final Map<String, Object> dataPayload = new HashMap<>();
        if (a4tParams != null) {
            dataPayload.put(TargetConstants.TargetResponse.ANALYTICS_PAYLOAD, a4tParams);
        }

        if (responseTokens != null) {
            dataPayload.put(TargetConstants.TargetResponse.RESPONSE_TOKENS, responseTokens);
        }

        if (clickMetricA4TParams != null) {
            dataPayload.put(TargetConstants.TargetResponse.CLICK_METRIC_ANALYTICS_PAYLOAD, clickMetricA4TParams);
        }
        data.put(TargetConstants.EventDataKeys.TARGET_DATA_PAYLOAD, dataPayload);

        if (!StringUtils.isNullOrEmpty(pairId)) {
            data.put(TargetConstants.EventDataKeys.TARGET_RESPONSE_PAIR_ID, pairId);
        }
        data.put(TargetConstants.EventDataKeys.TARGET_RESPONSE_EVENT_ID, event.getUniqueIdentifier());

        // Create Event and dispatch to EventHub
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "dispatchMboxContent - " + TARGET_EVENT_DISPATCH_MESSAGE);
        final Event responseEvent = new Event.Builder(TargetConstants.EventName.TARGET_REQUEST_RESPONSE,
                EventType.TARGET, EventSource.RESPONSE_CONTENT).setEventData(data).build();
        getApi().dispatch(responseEvent);
    }

    /**
     * Dispatches an Analytics Event containing the Analytics for Target (A4T) payload
     *
     * @param payload analytics for target (a4t) payload
     */
    void dispatchAnalyticsForTargetRequest(final Map<String, String> payload) {

        if (TargetUtils.isNullOrEmpty(payload)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "dispatchAnalyticsForTargetRequest - Failed to dispatch analytics. Payload is either null or empty");
            return;
        }

        // Create EventData
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(TargetConstants.EventDataKeys.CONTEXT_DATA, payload);
        eventData.put(TargetConstants.EventDataKeys.TRACK_ACTION, TargetConstants.A4T_ACTION_NAME);
        eventData.put(TargetConstants.EventDataKeys.TRACK_INTERNAL, true);

        // Create Event and dispatch
        final Event analyticsForTargetEvent = new Event.Builder(TargetConstants.EventName.ANALYTICS_FOR_TARGET_REQUEST_EVENT_NAME, EventType.ANALYTICS,
                EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();
        getApi().dispatch(analyticsForTargetEvent);
    }

    /**
     * Dispatches a response event for an identity request
     *
     * @param event       {@link Event} the associated TargetRequestIdentity event
     */
    void dispatchIdentity(final Event event) {
        final Map<String, Object> responseEventData = new HashMap<>();
        responseEventData.put(TargetConstants.EventDataKeys.THIRD_PARTY_ID, targetState.getThirdPartyId());
        responseEventData.put(TargetConstants.EventDataKeys.TNT_ID, targetState.getTntId());
        responseEventData.put(TargetConstants.EventDataKeys.SESSION_ID, targetState.getSessionId());
        final Event responseEvent = new Event.Builder(TargetConstants.EventName.IDENTITY_RESPONSE,
                EventType.TARGET, EventSource.RESPONSE_IDENTITY)
                .setEventData(responseEventData)
                .inResponseToEvent(event)
                .build();
        getApi().dispatch(responseEvent);
    }

    /**
     * Gets the {@code TargetRequestBuilder} instance used to build the json request.
     *
     * @return the {@link TargetRequestBuilder} instance
     */
    private TargetRequestBuilder getRequestBuilder() {
        if (deviceInfoService == null) {
            Log.error(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.REQUEST_BUILDER_INIT_FAILED + " Device Info services are not available");
            return null;
        }

        return new TargetRequestBuilder(deviceInfoService, targetPreviewManager, targetState);
    }

    /**
     * Verifies if the target extension is in preview mode.
     *
     * @return {@code boolean} indicating if the target extension is in preview mode
     */
    private boolean inPreviewMode() {
        final String previewParams = targetPreviewManager.getPreviewParameters();
        return !StringUtils.isNullOrEmpty(previewParams);
    }

    /**
     * Saves the tntId and the edge host value derived from it to the {@link TargetState}.
     * <p>
     * If a valid tntId is provided and the privacy status is {@link MobilePrivacyStatus#OPT_OUT} or
     * the provided tntId is same as the existing value, then the method returns with no further action.
     *
     * @param updatedTntId {@link String} containing new tntId that needs to be set.
     */
    void setTntIdInternal(final String updatedTntId) {
        // do not set identifier if privacy is opt-out and the id is not being cleared
        if (targetState.getMobilePrivacyStatus() == MobilePrivacyStatus.OPT_OUT && !StringUtils.isNullOrEmpty(updatedTntId)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "updateTntId - Cannot update Target tntId due to opt out privacy status.");
            return;
        }

        if (tntIdValuesAreEqual(targetState.getTntId(), updatedTntId)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "updateTntId - Won't update Target tntId as provided value is same as the existing tntId value (%s).", updatedTntId);
            return;
        }

        final String edgeHost = extractEdgeHost(updatedTntId);

        if (!StringUtils.isNullOrEmpty(edgeHost)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "updateTntId - The edge host value derived from the given tntId (%s) is (%s).",
                    updatedTntId, edgeHost);
            targetState.updateEdgeHost(edgeHost);
        } else {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "updateTntId - The edge host value cannot be derived from the given tntId (%s) and it is removed from the data store.",
                    updatedTntId);
            targetState.updateEdgeHost(null);
        }

        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "setTntIdInternal - Updating tntId with value (%s).", updatedTntId);
        targetState.updateTntId(updatedTntId);
    }

    /**
     * Saves the {@code #thirdPartyId} to the Target DataStore or remove its key in the dataStore if the newThirdPartyId {@link String} is null
     *
     * @param updatedThirdPartyId newThirdPartyID {@link String} to be set
     */
    void setThirdPartyIdInternal(final String updatedThirdPartyId) {
        // do not set identifier if privacy is opt-out and the id is not being cleared
        if (targetState.getMobilePrivacyStatus() == MobilePrivacyStatus.OPT_OUT && !StringUtils.isNullOrEmpty(updatedThirdPartyId)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "setThirdPartyIdInternal - Cannot update Target thirdPartyId due to opt out privacy status.");
            return;
        }

        if (targetState.getThirdPartyId() != null && targetState.getThirdPartyId().equals(updatedThirdPartyId)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "setThirdPartyIdInternal - New thirdPartyId value is same as the existing thirdPartyId (%s).", targetState.getThirdPartyId());
            return;
        }
        Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "setThirdPartyIdInternal - Updating thirdPartyId with value (%s).", updatedThirdPartyId);
        targetState.updateThirdPartyId(updatedThirdPartyId);
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
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "extractEdgeHost - Provided tntId (%s) contains location hint (%s).", newTntId,
                    locationHint);
        }

        String edgeHost = null;

        if (!StringUtils.isNullOrEmpty(locationHint)) {
            edgeHost = String.format(TargetConstants.API_URL_HOST_BASE, String.format(TargetConstants.EDGE_HOST_BASE,
                    locationHint));
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,  "extractEdgeHost - Edge host (%s) is derived from the provided tntId (%s).",
                    edgeHost, locationHint);
        }

        return edgeHost;
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
    private boolean tntIdValuesAreEqual(final String oldId, final String newId) {
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
     * Saves the provided session id {@code String} to the Target DataStore if it is a new value.
     * Prior to persisting the new session id value, the session is reset and a new session is started.
     *
     * @param newSessionId {@link String} containing the new session id to be set
     */
    void setSessionId(final String newSessionId) {
        // an empty session id will reset the current session
        if (targetState.getMobilePrivacyStatus() == MobilePrivacyStatus.OPT_OUT) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "setSessionId - Cannot update Target session id due to opted out privacy status.");
            return;
        }

        if (StringUtils.isNullOrEmpty(newSessionId)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "setSessionId - Removing session information from the Data store, new session id value is null or empty.");
            targetState.resetSession();
            return;
        }
        // for a new session id: persist the new session id then update the session timestamp. otherwise, just update the timestamp.
        if (!newSessionId.equals(targetState.getSessionId())) {
            targetState.updateSessionId(newSessionId);
        }

        targetState.updateSessionTimestamp(false);

    }

    /**
     * Clears identities including tntId, thirdPartyId and edgeHost.
     */
    private void resetIdentity() {
        setTntIdInternal(null);
        setThirdPartyIdInternal(null);
        targetState.updateEdgeHost(null);
        targetState.resetSession();
    }

    /**
     * Converts data from a lifecycle event into its form desired by Target.
     *
     * @param lifecycleData {@code Map<String, Object} shared state of Lifecycle extension
     * @return {@code Map<String, String>} containing Lifecycle data transformed for Target
     */
    private Map<String, String> getLifecycleDataForTarget(final Map<String, Object> lifecycleData) {
        if (TargetUtils.isNullOrEmpty(lifecycleData)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "getLifecycleDataForTarget - lifecycleData is (%s)",
                    (lifecycleData == null ? "null" : "empty"));
            return null;
        }

        // copy the event's data so we don't accidentally overwrite it for someone else consuming this event
        final Map<String, String> tempLifecycleContextData = new HashMap<>(DataReader.optStringMap(
                lifecycleData,
                TargetConstants.Lifecycle.LIFECYCLE_CONTEXT_DATA, null));

        final Map<String, String> lifecycleContextData = new HashMap<>();

        for (Map.Entry<String, String> kvp : TargetConstants.MAP_TO_CONTEXT_DATA_KEYS.entrySet()) {
            final String value = tempLifecycleContextData.get(kvp.getKey());
            if (!StringUtils.isNullOrEmpty(value)) {
                lifecycleContextData.put(kvp.getValue(), value);
                tempLifecycleContextData.remove(kvp.getKey());
            }
        }

        lifecycleContextData.putAll(tempLifecycleContextData);

        return lifecycleContextData;
    }

    /**
     * Runs the default callback for each of the request in the list.
     *
     * @param batchRequests {@code List<TargetRequest>} to return the default content
     * @param event {@code Event} associated Target Request content event
     */
    private void runDefaultCallbacks(final List<TargetRequest> batchRequests, final Event event) {
        if (TargetUtils.isNullOrEmpty(batchRequests)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "runDefaultCallbacks - Batch requests are (%s)",
                    (batchRequests == null ? "null" : "empty"));
            return;
        }

        for (TargetRequest request : batchRequests) {
            //Pass null for a4t params and response tokens in case of default callback.
            dispatchMboxContent(request.getDefaultContent(), null, null,
                    null, request.getResponsePairId(), event);
        }
    }

    /**
     * Gets the latest valid {@code Lifecycle} shared state at the given {@code event} version.
     *
     * @param event the {@code Lifecycle} state version to retrieve
     * @return the last known valid {@code Lifecycle} state, may be null if no valid state was found
     */
    private Map<String, Object> retrieveLifecycleSharedState(final Event event) {
        final SharedStateResult lifecycleSharedState = getApi().getSharedState(TargetConstants.Lifecycle.EXTENSION_NAME, event, false, SharedStateResolution.ANY);
        return lifecycleSharedState != null ? lifecycleSharedState.getValue() : null;
    }

    /**
     * Gets the latest valid {@code Identity} shared state at the given {@code event} version.
     *
     * @param event the {@code Identity} state version to retrieve
     * @return the last known valid {@code Identity} state, may be null if no valid state was found
     */
    private Map<String, Object> retrieveIdentitySharedState(final Event event) {
        final SharedStateResult identitySharedState = getApi().getSharedState(TargetConstants.Identity.EXTENSION_NAME, event, false, SharedStateResolution.ANY);
        return identitySharedState != null ? identitySharedState.getValue() : null;
    }

    /**
     * Gets the latest valid {@code Configuration} shared state at the given {@code event} version.
     *
     * @param event the {@code Configuration} state version to retrieve
     * @return the last known valid {@code Configuration} state, may be null if no valid state was found
     */
    private Map<String, Object> retrieveConfigurationSharedState(final Event event) {
        final SharedStateResult configSharedState = getApi().getSharedState(TargetConstants.Configuration.EXTENSION_NAME, event, false, SharedStateResolution.ANY);
        return configSharedState != null ? configSharedState.getValue() : null;
    }
}