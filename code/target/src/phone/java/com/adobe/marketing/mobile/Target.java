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

package com.adobe.marketing.mobile;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.target.AdobeTargetDetailedCallback;
import com.adobe.marketing.mobile.target.TargetExtension;
import com.adobe.marketing.mobile.target.TargetParameters;
import com.adobe.marketing.mobile.target.TargetPrefetch;
import com.adobe.marketing.mobile.target.TargetRequest;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public class containing APIs for the Target extension.
 */
public class Target {
    public static final Class<? extends Extension> EXTENSION = TargetExtension.class;

    static final String LOG_TAG = "Target";
    private static final String CLASS_NAME = "Target";
    static final String EXTENSION_VERSION = "2.0.3";

    static final class EventName {
        static final String PREFETCH_REQUEST = "TargetPrefetchRequest";
        static final String LOAD_REQUEST = "TargetLoadRequest";
        static final String LOCATIONS_DISPLAYED = "TargetLocationsDisplayed";
        static final String LOCATION_CLICKED = "TargetLocationClicked";
        static final String TARGET_REQUEST_RESPONSE = "TargetRequestResponse";
        static final String GET_THIRD_PARTY_ID = "TargetGetThirdPartyIdentifier";
        static final String SET_THIRD_PARTY_ID = "TargetSetThirdPartyIdentifier";
        static final String GET_TNT_ID = "TargetGetTnTIdentifier";
        static final String SET_TNT_ID = "TargetSetTnTIdentifier";
        static final String GET_SESSION_ID = "TargetGetSessionIdentifier";
        static final String SET_SESSION_ID = "TargetSetSessionIdentifier";
        static final String REQUEST_RESET = "TargetRequestReset";
        static final String CLEAR_PREFETCH_CACHE = "TargetClearPrefetchCache";
        static final String SET_PREVIEW_DEEPLINK = "TargetSetPreviewRestartDeeplink";
        static final String TARGET_RAW_REQUEST = "TargetRawRequest";
        static final String TARGET_RAW_NOTIFICATIONS = "TargetRawNotifications";

        private EventName() {}
    }

    static final class EventType {
        static final String TARGET = "com.adobe.eventType.target";

        private EventType() {}
    }


    static final class EventSource {
        static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
        static final String RESPONSE_CONTENT = "com.adobe.eventSource.responseContent";
        static final String REQUEST_IDENTITY = "com.adobe.eventSource.requestIdentity";
        static final String REQUEST_RESET = "com.adobe.eventSource.requestReset";

        private EventSource() {}
    }

    static final class EventDataKeys {
        static final String MBOX_NAME = "name";
        static final String MBOX_NAMES = "names";
        static final String TARGET_PARAMETERS = "targetparams";
        static final String EXECUTE = "execute";
        static final String PREFETCH = "prefetch";
        static final String LOAD_REQUEST = "request";
        static final String PREFETCH_ERROR = "prefetcherror";
        static final String IS_LOCATION_DISPLAYED = "islocationdisplayed";
        static final String IS_LOCATION_CLICKED = "islocationclicked";
        static final String THIRD_PARTY_ID = "thirdpartyid";
        static final String TNT_ID         = "tntid";
        static final String SESSION_ID = "sessionid";
        static final String RESET_EXPERIENCE = "resetexperience";
        static final String CLEAR_PREFETCH_CACHE = "clearcache";
        static final String PREVIEW_RESTART_DEEP_LINK = "restartdeeplink";
        static final String IS_RAW_EVENT = "israwevent";
        static final String NOTIFICATIONS = "notifications";
        static final String RESPONSE_DATA = "responsedata";
        static final String TARGET_RESPONSE_EVENT_ID = "responseEventId";
        static final String TARGET_RESPONSE_PAIR_ID = "responsePairId";
        static final String ANALYTICS_PAYLOAD = "analytics.payload";
        static final String RESPONSE_TOKENS = "responseTokens";
        static final String CLICK_METRIC_ANALYTICS_PAYLOAD = "clickmetric.analytics.payload";
        static final String TARGET_CONTENT = "content";
        static final String TARGET_DATA_PAYLOAD = "data";

        private EventDataKeys() {}
    }

    private static final String NULL_MBOX_MESSAGE = "Mbox name must not be empty or null";
    private static final String NULL_MBOXES_MESSAGE = "List of Mbox names must not be empty or null";
    private static final String NULL_REQUEST_MESSAGE = "The provided request list for mboxes is empty or null";
    private static final String NO_VALID_REQUEST_MESSAGE = "The provided request list for mboxes does not contain valid requests";
    private static final String NULL_RAW_REQUEST_MESSAGE = "The provided request map is empty or null";

    private static final long DEFAULT_TIMEOUT_MS = 5000L;
    private static boolean isResponseListenerRegistered = false;
    private static final ConcurrentHashMap<String, TargetRequest> pendingTargetRequestsMap = new ConcurrentHashMap<>();

    private Target() {}

    /**
     * Returns the version of the {@code Target} extension.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    @NonNull
    public static String extensionVersion() {
        return EXTENSION_VERSION;
    }

    /**
     * Registers the extension with the Mobile Core.
     * <p>
     * Note: This method should be called only once in your application class.
     */
    @Deprecated
    public static void registerExtension() {
        MobileCore.registerExtension(TargetExtension.class, extensionError -> {
            if (extensionError == null) {
                return;
            }

            Log.warning(LOG_TAG, CLASS_NAME,
                    "An error occurred while registering the Target extension: (%s) ", extensionError.getErrorName());
        });
    }

    /**
     * Prefetches multiple Target mboxes simultaneously.
     * <p>
     * Executes a prefetch request to the configured Target server with the TargetPrefetch list provided
     * in the {@code mboxPrefetchList} parameter. This prefetch request will use the provided {@code parameters} for all of
     * the prefetch made in this request. The {@code callback} will be executed when the prefetch has been completed, returning
     * {@code null} if the prefetch was successful or will contain a {@code String} error message otherwise
     * <p>
     * The prefetched mboxes are cached in memory for the current application session and returned when requested.
     *
     * @param mboxPrefetchList a {@code List<TargetPrefetch>} representing the desired mboxes to prefetch
     * @param parameters  a {@code TargetParameters} object containing Target parameters for all mboxes in the request list
     * @param callback           an {@code AdobeCallback<String>} which will be called after the prefetch is complete.  The success parameter
     *                           in the callback will be {@code null} if the prefetch completed successfully, or will contain a {@code String} error message otherwise.
     *                           If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
     * 	         				 eventuality of an unexpected error or if the timeout (5 seconds) is met before the content is prefetched.
     */
    public static void prefetchContent(@NonNull final List<TargetPrefetch> mboxPrefetchList,
                                       @Nullable final TargetParameters parameters,
                                       @Nullable final AdobeCallback<String> callback) {
        final AdobeCallbackWithError<String> callbackWithError = callback instanceof AdobeCallbackWithError ?
                (AdobeCallbackWithError<String>)callback : null;
        final String error;

        if (mboxPrefetchList == null || mboxPrefetchList.isEmpty()) {
            error = String.format("Failed to prefetch Target request (%s).",
                            NULL_REQUEST_MESSAGE);
            Log.warning(LOG_TAG, CLASS_NAME, error);

            if (callbackWithError != null) {
                callbackWithError.fail(AdobeError.UNEXPECTED_ERROR);
            } else if (callback != null) {
                callback.call(error);
            }
            return;
        }

        final List<Map<String, Object>> flattenedPrefetchRequests = new ArrayList<>();
        for (final TargetPrefetch prefetch: mboxPrefetchList) {
            if (prefetch == null) {
                continue;
            }
            flattenedPrefetchRequests.add(prefetch.toEventData());
        }

        if (flattenedPrefetchRequests.isEmpty()) {
            error = String.format("Failed to prefetch Target request (%s).",
                            NO_VALID_REQUEST_MESSAGE);
            Log.warning(LOG_TAG, CLASS_NAME, error);
            if (callbackWithError != null) {
                callbackWithError.fail(AdobeError.UNEXPECTED_ERROR);
            } else if (callback != null) {
                callback.call(error);
            }
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.PREFETCH, flattenedPrefetchRequests);
        if (parameters != null) {
            eventData.put(EventDataKeys.TARGET_PARAMETERS, parameters.toEventData());
        }

        final Event event = new Event.Builder(EventName.PREFETCH_REQUEST, EventType.TARGET, EventSource.REQUEST_CONTENT)
                .setEventData(eventData).build();

        MobileCore.dispatchEventWithResponseCallback(event, DEFAULT_TIMEOUT_MS, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                if (callbackWithError != null) {
                    callbackWithError.fail(adobeError);
                }
            }

            @Override
            public void call(final Event event) {
                final Map<String, Object> eventData = event.getEventData();
                if (MapUtils.isNullOrEmpty(eventData)) {
                    if (callbackWithError != null) {
                        callbackWithError.fail(AdobeError.UNEXPECTED_ERROR);
                    }
                    return;
                }

                final String prefetchError = DataReader.optString(eventData, EventDataKeys.PREFETCH_ERROR, null);
                callback.call(prefetchError);
            }
        });
    }

    /**
     * Retrieves content for multiple Target mbox locations at once.
     * <p>
     * Executes a batch request to the configured Target server for multiple mbox locations. Any prefetched content
     * which matches a given mbox location is returned and not included in the batch request to the Target server.
     * Each object in the list contains a callback function, which will be invoked when content is available for
     * its given mbox location.
     * If a {@code} TargetRequest within the {@code List<TargetRequest>} contains an {@link AdobeCallbackWithError},
     * an {@link AdobeError} can be returned in the eventuality of an unexpected error or if the timeout (5 seconds) is met
     * before the location content is retrieved.
     * <p>
     * Note: If any mboxes have been prefetched before calling this method, please call one of the
     * {@code displayedLocations()} methods after the content returned by this method has been displayed.
     *
     * @param mboxRequestList a {@code List<TargetRequest>} to retrieve content for
     * @param parameters a {@link TargetParameters} object containing parameters for all mboxes in the request list
     */
    public static void retrieveLocationContent(@NonNull final List<TargetRequest> mboxRequestList,
                                               @Nullable final TargetParameters parameters) {
        if (mboxRequestList == null || mboxRequestList.isEmpty()) {
            Log.warning(LOG_TAG, CLASS_NAME,
                    "Failed to retrieve Target location content (%s).",
                    NULL_REQUEST_MESSAGE);
            return;
        }

        final List<TargetRequest> mboxRequestListCopy = new ArrayList<>(mboxRequestList);
        final List<Map<String, Object>> flattenedLocationRequests = new ArrayList<>();
        final Map<String, TargetRequest> tempIdToRequestMap = new HashMap<>();
        for (final TargetRequest request: mboxRequestListCopy) {
            if (request == null) {
                continue;
            }
            final AdobeCallback<String> callback = request.getContentCallback();
            final AdobeTargetDetailedCallback contentWithDataCallback = request.getContentWithDataCallback();

            // Skip the target request objects with null/empty mbox names
            final String mboxName = request.getMboxName();
            if (StringUtils.isNullOrEmpty(mboxName)) {
                Log.warning(LOG_TAG, CLASS_NAME, "Failed to retrieve Target location content (%s), returning default content.",
                        NULL_MBOX_MESSAGE);
                final String defaultContent = request.getDefaultContent();
                if (contentWithDataCallback != null) {
                    contentWithDataCallback.call(defaultContent, null);
                } else if (callback != null) {
                    callback.call(defaultContent);
                }
                continue;
            }

            final String responsePairId = UUID.randomUUID().toString();
            request.setResponsePairId(responsePairId);

            tempIdToRequestMap.put(responsePairId, request);
            flattenedLocationRequests.add(request.toEventData());
        }

        if (flattenedLocationRequests.isEmpty()) {
            Log.warning(LOG_TAG, CLASS_NAME, "Failed to retrieve Target location content (%s).",
                    NO_VALID_REQUEST_MESSAGE);
            return;
        }

        // Register the response content event listener
        registerResponseContentEventListener();

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.LOAD_REQUEST, flattenedLocationRequests);
        if (parameters != null) {
            eventData.put(EventDataKeys.TARGET_PARAMETERS, parameters.toEventData());
        }

        final Event event = new Event.Builder(EventName.LOAD_REQUEST,
                EventType.TARGET,
                EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        for (final Map.Entry<String,TargetRequest> entry : tempIdToRequestMap.entrySet()) {
            pendingTargetRequestsMap.put(event.getUniqueIdentifier()+ "-" +entry.getKey(), entry.getValue());
        }

        MobileCore.dispatchEvent(event);
    }

    /**
     * Sends a display notification to Target for a given prefetched mbox. This helps Target record location display events.
     * <p>
     * <b>Note:</b> if you're only using regular mboxes, and not prefetching any mbox content via {@code prefetchContent()}
     * this method should not be called.
     *
     * @param mboxNames (required) a list of displayed {@code String} location names
     * @param targetParameters {@link TargetParameters} for the displayed locations
     */
    public static void displayedLocations(@NonNull final List<String> mboxNames, @Nullable final TargetParameters targetParameters) {

        if (mboxNames == null || mboxNames.size() == 0) {
            Log.warning(LOG_TAG, CLASS_NAME,
                    "Failed to send display notification (%s).",
                    NULL_MBOXES_MESSAGE);
            return;
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.IS_LOCATION_DISPLAYED, true);
        eventData.put(EventDataKeys.MBOX_NAMES, mboxNames);
        if (targetParameters != null) {
            eventData.put(EventDataKeys.TARGET_PARAMETERS, targetParameters.toEventData());
        }

        final Event event = new Event.Builder(EventName.LOCATIONS_DISPLAYED,
                EventType.TARGET,
                EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event);
    }

    /**
     * Sends a click notification to Target if click metrics are enabled for the provided location name.
     * <p>
     * Click notification can be sent for a location, provided a load request has been executed for that prefetched or regular mbox
     * location before, indicating that the mbox was viewed. This request helps Target record the clicked event for the given location or mbox.
     *
     * @param mboxName  (required) a {@code String} representing the location name
     * @param parameters {@link TargetParameters} object for the location clicked
     */
    public static void clickedLocation(@NonNull final String mboxName, @Nullable final TargetParameters parameters) {
        if (StringUtils.isNullOrEmpty(mboxName)) {
            Log.warning(LOG_TAG, CLASS_NAME,
                    "Failed to send click notification (%s).",
                    NULL_MBOX_MESSAGE);
            return;
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.IS_LOCATION_CLICKED, true);
        eventData.put(EventDataKeys.MBOX_NAME, mboxName);
        if (parameters != null) {
            eventData.put(EventDataKeys.TARGET_PARAMETERS, parameters.toEventData());
        }

        final Event event = new Event.Builder(EventName.LOCATION_CLICKED,
                EventType.TARGET,
                EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event);
    }

    /**
     * Gets the custom visitor ID for Target.
     * <p>
     * This ID will be reset  when the {@link #resetExperience()} API is called.
     *
     * @param callback {@link AdobeCallback} the callback invoked with the third party id value {@code String}. The value will be null if no
     *                 third-party ID was set.
     *                 If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
     * 	         	   eventuality of an unexpected error or if the timeout (5 seconds) is met before the third party id is retrieved.
     * @see #setThirdPartyId(String)
     */
    public static void getThirdPartyId(@NonNull final AdobeCallback<String> callback) {
        if (callback == null) {
            Log.warning(LOG_TAG, CLASS_NAME,
                    "Failed to get Target session ID, provided AdobeCallback (callback) is null.");
            return;
        }

        final Event event = new Event.Builder(EventName.GET_THIRD_PARTY_ID,
                EventType.TARGET,
                EventSource.REQUEST_IDENTITY)
                .build();

        final AdobeCallbackWithError<String> callbackWithError = callback instanceof AdobeCallbackWithError ?
                (AdobeCallbackWithError<String>)callback : null;

        MobileCore.dispatchEventWithResponseCallback(event, DEFAULT_TIMEOUT_MS, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                if (callbackWithError != null) {
                    callbackWithError.fail(adobeError);
                }
            }

            @Override
            public void call(final Event event) {
                final Map<String, Object> eventData = event.getEventData();
                if (MapUtils.isNullOrEmpty(eventData)) {
                    if (callbackWithError != null) {
                        callbackWithError.fail(AdobeError.UNEXPECTED_ERROR);
                    }
                    return;
                }

                final String responseData = (String)eventData.get(EventDataKeys.THIRD_PARTY_ID);
                callback.call(responseData);
            }
        });
    }

    /**
     * Sets the third party ID for Target.
     * <p>
     * This ID will be persisted until either {@link #resetExperience()} is called or the app is uninstalled.
     *
     * @param thirdPartyId {@code String} containing the third party Id value.
     */
    public static void setThirdPartyId(@Nullable final String thirdPartyId) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.THIRD_PARTY_ID, thirdPartyId);

        final Event event = new Event.Builder(EventName.SET_THIRD_PARTY_ID,
                EventType.TARGET,
                EventSource.REQUEST_IDENTITY)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event);
    }

    /**
     * Gets the Target user identifier.
     * <p>
     * The tnt ID is returned in the network response from Target after a successful call to
     * {@link #prefetchContent(List, TargetParameters, AdobeCallback)} API or {@link #retrieveLocationContent(List, TargetParameters)} API,
     * which is then persisted in the SDK. The persisted tnt ID is used in subsequent Target requests until a different tnt ID is returned
     * from Target, or a new tnt ID is set using {@link #setTntId(String)} API.
     *
     * @param callback {@link AdobeCallback} the callback invoked with the tnt ID value as {@code String} or null if not available.
     *                 If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
     * 	         	   eventuality of an unexpected error, or if the timeout (5 seconds) is met before the tnt ID is retrieved.
     * @see #setTntId(String)
     */
    public static void getTntId(@NonNull final AdobeCallback<String> callback) {
        if (callback == null) {
            Log.warning(LOG_TAG, CLASS_NAME,
                    "Failed to get Target session ID, provided AdobeCallback (callback) is null.");
            return;
        }

        final Event event = new Event.Builder(EventName.GET_TNT_ID,
                EventType.TARGET,
                EventSource.REQUEST_IDENTITY)
                .build();

        final AdobeCallbackWithError<String> callbackWithError = callback instanceof AdobeCallbackWithError ?
                (AdobeCallbackWithError<String>)callback : null;

        MobileCore.dispatchEventWithResponseCallback(event, DEFAULT_TIMEOUT_MS, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                if (callbackWithError != null) {
                    callbackWithError.fail(adobeError);
                }
            }

            @Override
            public void call(final Event event) {
                final Map<String, Object> eventData = event.getEventData();
                if (MapUtils.isNullOrEmpty(eventData)) {
                    if (callbackWithError != null) {
                        callbackWithError.fail(AdobeError.UNEXPECTED_ERROR);
                    }
                    return;
                }

                final String responseData = (String)eventData.get(EventDataKeys.TNT_ID);
                callback.call(responseData);
            }
        });
    }

    /**
     * Sets the Target user identifier.
     * <p>
     * The provided tnt ID is persisted in the SDK and attached to subsequent Target requests. It is used to
     * derive the edge host value in the SDK, which is also persisted and used in future Target requests.
     * <p>
     * If the provided tnt ID is null or empty, or if the privacy status is opted out, the SDK will remove the tnt ID and edge host values from the persistence.
     * <p>
     * This ID is preserved between app upgrades, is saved and restored during the standard application backup process,
     * and is removed at uninstall, upon privacy status update to opted out, or when the {@link #resetExperience()} API is called.
     *
     * @param tntId {@link String} containing the tnt ID value to be set in the SDK.
     * @see #getTntId(AdobeCallback)
     */
    public static void setTntId(@Nullable final String tntId) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.TNT_ID, tntId);

        final Event event = new Event.Builder(EventName.SET_TNT_ID,
                EventType.TARGET,
                EventSource.REQUEST_IDENTITY)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event);
    }

    /**
     * Gets the Target session identifier.
     * <p>
     * The session ID is generated locally in the SDK upon initial Target request and persisted for a period defined by
     * {@code target.sessionTimeout} configuration setting. If the session timeout happens upon a subsequent Target request,
     * a new session ID will be generated for use in the request and persisted in the SDK.
     *
     * @param callback {@link AdobeCallback} the callback invoked with the session ID value as {@code String}.
     *                 If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
     * 	         	   eventuality of an unexpected error, or if the timeout (5 seconds) is met before the session ID is retrieved.
     * @see #setSessionId(String)
     */
    public static void getSessionId(@NonNull final AdobeCallback<String> callback) {
        if (callback == null) {
            Log.warning(LOG_TAG, CLASS_NAME,
                    "Failed to get Target session ID, provided AdobeCallback (callback) is null.");
            return;
        }

        final Event event = new Event.Builder(EventName.GET_SESSION_ID,
                EventType.TARGET,
                EventSource.REQUEST_IDENTITY)
                .build();

        final AdobeCallbackWithError<String> callbackWithError = callback instanceof AdobeCallbackWithError ?
                (AdobeCallbackWithError<String>)callback : null;

        MobileCore.dispatchEventWithResponseCallback(event, DEFAULT_TIMEOUT_MS, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                if (callbackWithError != null) {
                    callbackWithError.fail(adobeError);
                }
            }

            @Override
            public void call(final Event event) {
                final Map<String, Object> eventData = event.getEventData();
                if (MapUtils.isNullOrEmpty(eventData)) {
                    if (callbackWithError != null) {
                        callbackWithError.fail(AdobeError.UNEXPECTED_ERROR);
                    }
                    return;
                }

                final String responseData = (String)eventData.get(EventDataKeys.SESSION_ID);
                callback.call(responseData);
            }
        });
    }

    /**
     * Sets the Target session identifier.
     * <p>
     * The provided session ID is persisted in the SDK for a period defined by {@code target.sessionTimeout} configuration setting.
     * If the provided session ID is null or empty, or if the privacy status is opted out, the SDK will remove the session ID value
     * from the persistence.
     * <p>
     * This ID is preserved between app upgrades, is saved and restored during the standard application backup process,
     * and is removed at uninstall, upon privacy status update to opted out, or when the {@link #resetExperience()} API is called.
     *
     * @param sessionId {@link String} containing the Target session ID value to be set in the SDK.
     * @see #getSessionId(AdobeCallback)
     */
    public static void setSessionId(@Nullable final String sessionId) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.SESSION_ID, sessionId);

        final Event event = new Event.Builder(EventName.SET_SESSION_ID,
                EventType.TARGET,
                EventSource.REQUEST_IDENTITY)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event);
    }

    /**
     * Resets the user's experience
     * <p>
     * Resets the user's experience by removing the visitor identifiers. Removes previously set third-party and TnT IDs from persistent storage.
     *
     * @see #getThirdPartyId(AdobeCallback)
     * @see #getTntId(AdobeCallback)
     */
    public static void resetExperience() {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.RESET_EXPERIENCE, true);

        final Event event = new Event.Builder(EventName.REQUEST_RESET,
                EventType.TARGET,
                EventSource.REQUEST_RESET)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event);
    }

    /**
     * Clears the cached prefetched {@code TargetPrefetch} list.
     *
     * @see #prefetchContent(List, TargetParameters, AdobeCallback)
     */
    public static void clearPrefetchCache() {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.CLEAR_PREFETCH_CACHE, true);

        final Event event = new Event.Builder(EventName.CLEAR_PREFETCH_CACHE,
                EventType.TARGET,
                EventSource.REQUEST_RESET)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event);
    }

    /**
     * Sets the Target preview restart deep link.
     * <p>
     * Set the Target preview URL to be displayed when a new preview experience is loaded.
     *
     *  @param deepLink the {@link Uri} which will be set for preview restart
     */
    public static void setPreviewRestartDeepLink(@NonNull final Uri deepLink) {
        if (deepLink == null) {
            Log.warning(LOG_TAG, CLASS_NAME,
                    "Failed to set preview restart deeplink as the provided value is null.");
            return;
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.PREVIEW_RESTART_DEEP_LINK, deepLink.toString());

        final Event event = new Event.Builder(EventName.SET_PREVIEW_DEEPLINK,
                EventType.TARGET,
                EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event);
    }

    /**
     * Retrieves Target prefetch or execute response for a list of mbox locations.
     * <p>
     * It issues a request to the configured Target server for the provided mbox locations in the request.
     *
     * @param request a {@code Map<String, Object>} containing prefetch or execute request data in the Target v1 delivery API format.
     * @param callback  an {@code AdobeCallback<Map<String, Object>>} which will be called after the Target request is completed. The parameter
     *                     in the callback will contain the response data if the execute request completed successfully, or it will contain null otherwise.
     */
    public static void executeRawRequest(@NonNull final Map<String, Object> request,
                                         @NonNull final AdobeCallback<Map<String, Object>> callback) {

        final AdobeCallbackWithError<?> callbackWithError = callback instanceof AdobeCallbackWithError ?
                (AdobeCallbackWithError<?>) callback : null;

        if (MapUtils.isNullOrEmpty(request)) {
            Log.warning(LOG_TAG, CLASS_NAME,
                    "Failed to execute raw Target request (%s).", NULL_RAW_REQUEST_MESSAGE);

            if (callbackWithError != null) {
                callbackWithError.fail(AdobeError.UNEXPECTED_ERROR);
            } else if (callback != null) {
                callback.call(null);
            }
            return;
        }

        if (!request.containsKey(EventDataKeys.EXECUTE)
                && !request.containsKey(EventDataKeys.PREFETCH)) {
            Log.warning(LOG_TAG,CLASS_NAME,
                    "Failed to execute raw Target request, provided request doesn't contain prefetch or execute data.");

            if (callbackWithError != null) {
                callbackWithError.fail(AdobeError.UNEXPECTED_ERROR);
            } else if (callback != null) {
                callback.call(null);
            }
            return;
        }

        final Map<String, Object> eventData = new HashMap<>(request);
        eventData.put(EventDataKeys.IS_RAW_EVENT, true);

        final Event event = new Event.Builder(EventName.TARGET_RAW_REQUEST,
                EventType.TARGET,
                EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();


        MobileCore.dispatchEventWithResponseCallback(event, DEFAULT_TIMEOUT_MS, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                if (callbackWithError != null) {
                    callbackWithError.fail(adobeError);
                }
            }

            @Override
            public void call(final Event event) {
                final Map<String, Object> eventData = event.getEventData();
                if (MapUtils.isNullOrEmpty(eventData)) {
                    if (callbackWithError != null) {
                        callbackWithError.fail(AdobeError.UNEXPECTED_ERROR);
                    }
                    return;
                }

                final Map<String, Object> responseData = DataReader.optTypedMap(Object.class,
                        eventData, EventDataKeys.RESPONSE_DATA, null);
                callback.call(responseData);
            }
        });
    }

    /**
     * Sends a notification request to Target using the provided notification data in the request.
     * <p>
     * The display or click tokens, required for the Target notifications, can be retrieved from the
     * response of a previous `executeRawRequest` API call.
     *
     * @param request  (required) a {@code Map<String, Object>} containing notifications data in the Target v1 delivery API format.
     * @see #executeRawRequest(Map, AdobeCallback)
     */
    public static void sendRawNotifications(@NonNull final Map<String, Object> request) {
        if (MapUtils.isNullOrEmpty(request)) {
            Log.warning(LOG_TAG, CLASS_NAME,
                    "Failed to send raw Target notification(s) (%s).", NULL_RAW_REQUEST_MESSAGE);
            return;
        }

        if (!request.containsKey(EventDataKeys.NOTIFICATIONS)) {
            Log.warning(LOG_TAG, CLASS_NAME,
                    "Failed to send raw Target notification(s), provided request doesn't contain notifications data.");
            return;
        }

        final Map<String, Object> eventData = new HashMap<>(request);
        eventData.put(EventDataKeys.IS_RAW_EVENT, true);

        final Event event = new Event.Builder(EventName.TARGET_RAW_NOTIFICATIONS,
                EventType.TARGET,
                EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event);
    }

    /**
     * Registers the response content event listener
     */
    private static void registerResponseContentEventListener() {
        // Only register the listener once
        if (!isResponseListenerRegistered) {
            MobileCore.registerEventListener(EventType.TARGET, EventSource.RESPONSE_CONTENT, event -> {
                if (!event.getName().equals(EventName.TARGET_REQUEST_RESPONSE)) {
                    return;
                }

                final Map<String, Object> eventData = event.getEventData();
                if (MapUtils.isNullOrEmpty(eventData)) {
                    Log.debug(LOG_TAG, CLASS_NAME,  "Cannot find target request, response event data is null or empty.");
                    return;
                }

                String id = null;
                try {
                    id = DataReader.getString(eventData, EventDataKeys.TARGET_RESPONSE_EVENT_ID);
                } catch (final DataReaderException e) {
                    Log.debug(LOG_TAG, CLASS_NAME,  "Cannot find target request, responseEventId is invalid (%s).", e.getLocalizedMessage());
                }
                if (StringUtils.isNullOrEmpty(id)) {
                    Log.debug(LOG_TAG, CLASS_NAME,  "Cannot find target request, responseEventId is not available.");
                    return;
                }

                String responsePairId = null;
                try {
                    responsePairId = DataReader.getString(eventData, EventDataKeys.TARGET_RESPONSE_PAIR_ID);
                } catch (final DataReaderException e) {
                    Log.debug(LOG_TAG, CLASS_NAME,  "Cannot find target request, responsePairId is invalid (%s).", e.getLocalizedMessage());
                }
                if (StringUtils.isNullOrEmpty(responsePairId)) {
                    Log.debug(LOG_TAG, CLASS_NAME,  "Cannot find target request, responsePairId is not available.");
                    return;
                }

                final String requestSearchId = id+"-"+responsePairId;
                final TargetRequest request = pendingTargetRequestsMap.get(requestSearchId);
                if (request == null) {
                    Log.warning(LOG_TAG, CLASS_NAME,  "Missing target request for (%s)", requestSearchId);
                    return;
                }

                final AdobeCallback<String> callback = request.getContentCallback();
                final AdobeTargetDetailedCallback contentWithDataCallback = request.getContentWithDataCallback();

                if (contentWithDataCallback != null) {
                    final Map<String, Object> mboxPayloadMap = createMboxPayloadMap(DataReader.optTypedMap(Object.class,
                            eventData, EventDataKeys.TARGET_DATA_PAYLOAD, null), request);
                    final String content = DataReader.optString(eventData,
                            EventDataKeys.TARGET_CONTENT,
                            request.getDefaultContent());
                    contentWithDataCallback.call(content, mboxPayloadMap);
                } else if (callback != null) {
                    callback.call(DataReader.optString(eventData,
                            EventDataKeys.TARGET_CONTENT,
                            request.getDefaultContent()));
                }
            });
            isResponseListenerRegistered = true;
        }
    }

    /**
     * Retrieves the mbox values like A4t payload, response tokens, and click metric A4t payload from the provided {@code data} and
     * returns them as {@code Map<String, Object>}.
     * <p>
     * This method returns null if {@code data} map doesn't contain these values.
     *
     * @param data     a {@code Map<String, Object>} object.
     * @param request an object of {@link TargetRequest}.
     * @return a {@code Map<String, Object>} containing mbox values received in {@code data} map.
     */
    private static Map<String, Object> createMboxPayloadMap(final Map<String, Object> data, final TargetRequest request) {
        if (MapUtils.isNullOrEmpty(data)) {
            Log.debug(LOG_TAG, CLASS_NAME,
                    "The data payload map containing response tokens and analytics payload is not present for the mbox location (%s)", request.getMboxName());
            return null;
        }
        final Map<String, Object> mboxPayload = new HashMap<>();

        final Map<String, String> a4tParams = DataReader.optStringMap(data,
                EventDataKeys.ANALYTICS_PAYLOAD,
                null);
        if (a4tParams != null) {
            Log.trace(LOG_TAG, CLASS_NAME, "A4t params map is present for mbox location (%s)", request.getMboxName());
            mboxPayload.put(EventDataKeys.ANALYTICS_PAYLOAD, a4tParams);
        }

        final Map<String, Object> responseTokens = DataReader.optTypedMap(Object.class,
                data,
                EventDataKeys.RESPONSE_TOKENS,
                null);
        if (responseTokens != null) {
            Log.trace(LOG_TAG, CLASS_NAME, "Response tokens map is present for mbox location (%s)", request.getMboxName());
            mboxPayload.put(EventDataKeys.RESPONSE_TOKENS, responseTokens);
        }

        final Map<String, String> clickMetricA4TParams = DataReader.optStringMap(data,
                EventDataKeys.CLICK_METRIC_ANALYTICS_PAYLOAD,
                null);
        if (clickMetricA4TParams != null) {
            Log.trace(LOG_TAG, CLASS_NAME, "Click metrics map is present for mbox location (%s)", request.getMboxName());
            mboxPayload.put(EventDataKeys.CLICK_METRIC_ANALYTICS_PAYLOAD, clickMetricA4TParams);
        }

        if (mboxPayload.isEmpty()) {
            Log.debug(LOG_TAG, CLASS_NAME,
                    "Neither response tokens are activated on Target UI nor activity is A4T enabled, returning null data payload for mbox location (%s)",
                    request.getMboxName());
            return null;
        }

        return mboxPayload;
    }

    @VisibleForTesting
    static void resetListeners() {
        isResponseListenerRegistered = false;
    }
}