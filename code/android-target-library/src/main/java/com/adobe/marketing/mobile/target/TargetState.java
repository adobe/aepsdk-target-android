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

import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.TimeUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class TargetState {

    private static final String CLASS_NAME = "TargetState";

    private final NamedCollection dataStore;
    private Map<String, JSONObject> prefetchedMbox = new HashMap<>();
    private Map<String, JSONObject> loadedMbox = new HashMap<>();
    private List<JSONObject> notifications = new ArrayList<>();

    private Map<String, Object> storedConfigurationSharedState = null;
    private String tntId = "";
    private String thirdPartyId = "";
    private String edgeHost = "";
    private String sessionId = "";
    private long sessionTimestampInSeconds = 0L;

    TargetState(final NamedCollection dataStore) {
        this.dataStore = dataStore;
        if (dataStore == null) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "Unable to initialize TargetState, datastore is null");
            return;
        }
        tntId = dataStore.getString(TargetConstants.DataStoreKeys.TNT_ID, "");
        thirdPartyId = dataStore.getString(TargetConstants.DataStoreKeys.THIRD_PARTY_ID, "");
        edgeHost = dataStore.getString(TargetConstants.DataStoreKeys.EDGE_HOST, "");
        sessionId = dataStore.getString(TargetConstants.DataStoreKeys.SESSION_ID, "");
        sessionTimestampInSeconds =  dataStore.getLong(TargetConstants.DataStoreKeys.SESSION_TIMESTAMP, 0L);

    }

    /**
     *  Updates the stored configuration shared state if the given one is not null.
     *  If the given configuration shared state contains a new client code, the stored `edge host` will be set with null.
     * @param configuration {@code Map<String, Object} the shared state of the `Configuration`
     */
    void updateConfigurationSharedState(final Map<String, Object> configuration) {
        if (TargetUtils.isNullOrEmpty(configuration)) {
            return;
        }

        final String newClientCode = DataReader.optString(configuration, TargetConstants.Configuration.TARGET_CLIENT_CODE, "");
        if (storedConfigurationSharedState != null && !newClientCode.equals(getClientCode())) {
            updateEdgeHost(null);
        }
        storedConfigurationSharedState = configuration;
    }

    /**
     * Get {@code MobilePrivacyStatus} for this Target extension.
     *
     * @return {@link MobilePrivacyStatus} {@link TargetConstants.Configuration#GLOBAL_CONFIG_PRIVACY} value from the last known Configuration state
     */
    MobilePrivacyStatus getMobilePrivacyStatus() {
        final String privacyString = DataReader.optString(storedConfigurationSharedState,
                TargetConstants.Configuration.GLOBAL_CONFIG_PRIVACY, MobilePrivacyStatus.UNKNOWN.getValue());
        return MobilePrivacyStatus.fromString(privacyString);
    }

    /**
     * Get the session timeout from config or default session timeout
     *
     * @return {@code int} session timeout from config or default session timeout {@code int} TargetConstants#DEFAULT_TARGET_SESSION_TIMEOUT_SEC
     */
    int getSessionTimeout() {
        return DataReader.optInt(storedConfigurationSharedState,
                TargetConstants.Configuration.TARGET_SESSION_TIMEOUT,
                TargetConstants.DEFAULT_TARGET_SESSION_TIMEOUT_SEC);
    }

    /**
     * Get {@code String} client code for this Target extension.
     *
     * @return {@link String} {@link TargetConstants.Configuration#TARGET_CLIENT_CODE} value from the last known Configuration state
     */
    String getClientCode() {
        return DataReader.optString(storedConfigurationSharedState,
                TargetConstants.Configuration.TARGET_CLIENT_CODE, "");
    }

    /**
     * Get {@code String} environmentId for this Target extension.
     *
     * @return {@link String} {@link TargetConstants.Configuration#TARGET_ENVIRONMENT_ID} value from the last known Configuration state
     */
    long getEnvironmentId() {
        return DataReader.optLong(storedConfigurationSharedState,
                TargetConstants.Configuration.TARGET_ENVIRONMENT_ID, 0L);
    }

    /**
     * Get {@code String} property token for this Target extension.
     *
     * @return {@link String}{@link TargetConstants.Configuration#TARGET_PROPERTY_TOKEN} value from the last known Configuration state
     */
    String getPropertyToken() {
        return DataReader.optString(storedConfigurationSharedState,
                TargetConstants.Configuration.TARGET_PROPERTY_TOKEN, "");
    }

    /**
     * Get {@code String} target server for this Target extension.
     *
     * @return {@link String}{@link TargetConstants.Configuration#TARGET_SERVER} value from the last known Configuration state
     */
    String getTargetServer() {
        return DataReader.optString(storedConfigurationSharedState,
                TargetConstants.Configuration.TARGET_SERVER, "");
    }

    /**
     * Get {@code String} network timeout for this Target extension.
     *
     * @return {@link String}{@link TargetConstants.Configuration#TARGET_SERVER} value from the last known Configuration state
     */
    int getNetworkTimeout() {
        return DataReader.optInt(storedConfigurationSharedState, TargetConstants.Configuration.TARGET_NETWORK_TIMEOUT, TargetConstants.DEFAULT_NETWORK_TIMEOUT);
    }

    List<JSONObject> getNotifications() {
        return notifications;
    }

    /**
     * Get the sessionId either from memory or from the datastore if session is not expired.
     *
     * <p>
     * Context: AMSDK-8217
     * Retrieves the sessionId from memory. If no value in memory, it generates a random UUID,
     * sets current timestamp for {@code long} sessionTimestampInSeconds and saves them in persistence.
     * The sessionId is refreshed after
     * {@link TargetConstants.Configuration#TARGET_SESSION_TIMEOUT} (secs) of inactivity
     * (from the last successful target request).
     * <p>
     * The sessionId is sent in URL query parameters for each Target Network call; based on this Target will
     * route all requests from a session to the same edge to prevent overwriting the profiles.
     * <p>
     *
     * @return the session ID value as {@link String}
     */
    String getSessionId() {
        // if there is no session id persisted in local data store or if the session id is expired
        // because there was no activity for more than certain amount of time
        // (from the last successful network request), then generate a new session id, save it in persistence storage
        if (StringUtils.isNullOrEmpty(sessionId) || isSessionExpired()) {
            sessionId = UUID.randomUUID().toString();

            if (dataStore != null) {
                dataStore.setString(TargetConstants.DataStoreKeys.SESSION_ID, sessionId);
            }

            // update session id timestamp when the new session id is generated
            resetSessionTimestamp(false);
        }

        return sessionId;
    }

    /**
     * Returns edgeHost in memory.
     * <p>
     * If current session expired, the edge host is reset to null (in memory and persistence), so
     * the next network call will use the target client code.
     * <p>
     *
     * @return the edgeHost {@link String} value
     */
    String getEdgeHost() {
        // If the session expired reset the edge host
        if (isSessionExpired()) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "getEdgeHost - Resetting edge host to null as session id expired.");
            updateEdgeHost(null);
        }
        return edgeHost;
    }


    /**
     * Get {@code String} tntId for this Target extension.
     *
     * @return {@link String} tntId in memory
     */
    String getTntId() {
        return tntId;
    }

    /**
     * Get {@code String} thirdPartyId for this Target extension.
     *
     * @return {@link String} thirdPartyId in memory
     */
    String getThirdPartyId() {
        return thirdPartyId;
    }

    /**
     * Get configuration shared state in memory
     * @return {@code Map<String, Object} configuration shared state in memory
     */
    Map<String, Object> getStoredConfigurationSharedState() {
        return storedConfigurationSharedState;
    }

    /**
     * Updates {@code long} session timestamp in memory and in datastore.
     * If session timestamp needs to be reset, locally stored session timestamp is set to 0 and the value is removed from persistence.
     * If not, sets locally stored session timestamp to the current timestamp and stores it in persistence.
     *
     * <p>
     * Note: this method needs to be called after each successful target network request in order
     * to compute the session id expiration date properly.
     * <p>
     * @param shouldSessionTimestampReset {@link Boolean} representing if session timestamp needs to be reset
     */
    void resetSessionTimestamp(final boolean shouldSessionTimestampReset) {
        if(shouldSessionTimestampReset) {
            sessionTimestampInSeconds = 0L;
            if (dataStore != null) {
                Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "updateSessionTimestamp - Attempting to remove the session timestamp");
                dataStore.remove(TargetConstants.DataStoreKeys.SESSION_TIMESTAMP);
            }
            return;
        }
        sessionTimestampInSeconds = TimeUtils.getUnixTimeInSeconds();
        if (dataStore != null) {
            Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "updateSessionTimestamp - Attempting to update the session timestamp");
            dataStore.setLong(TargetConstants.DataStoreKeys.SESSION_TIMESTAMP, sessionTimestampInSeconds);
        }
    }

    /**
     * Updates {@code String} sessionId in memory and in the datastore.
     * If provided sessionId is null or empty, removes the value in persistence.
     * If not, stores the new sessionId in persistence.
     *
     * @param updatedSessionId {@link String} containing the new sessionId to be set
     */
    void updateSessionId(final String updatedSessionId) {
        sessionId = updatedSessionId;
        if (dataStore != null) {
            if (StringUtils.isNullOrEmpty(sessionId)) {
                Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "updateSessionId - Attempting to remove the session id");
                dataStore.remove(TargetConstants.DataStoreKeys.SESSION_ID);
            } else {
                Log.trace(TargetConstants.LOG_TAG, CLASS_NAME, "updateSessionId - Attempting to update the session id");
                dataStore.setString(TargetConstants.DataStoreKeys.SESSION_ID, updatedSessionId);
            }
        }
    }

    /**
     * Updates {@code String} tntId in memory and in the datastore.
     * If provided tntId is null or empty value, removes the value in persistence.
     * If not, stores the new tntId in persistence.
     *
     * @param updatedTntId {@link String} containing new tntId that needs to be set.
     */
    void setTntId(final String updatedTntId) {
        tntId = updatedTntId;

        if (dataStore != null) {
            if (StringUtils.isNullOrEmpty(updatedTntId)) {
                Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                        "setTntIdInternal - Removed tntId from the data store, provided tntId value is null or empty.");
                dataStore.remove(TargetConstants.DataStoreKeys.TNT_ID);
            } else {
                Log.debug(TargetConstants.LOG_TAG, "setTntIdInternal - Persisted new tntId (%s) in the data store.", updatedTntId);
                dataStore.setString(TargetConstants.DataStoreKeys.TNT_ID, updatedTntId);
            }
        } else {
            Log.debug(TargetConstants.LOG_TAG, "setTntIdInternal - " + TargetErrors.TARGET_TNT_ID_NOT_PERSISTED,
                    "Data store is not available.");
        }
    }

    /**
     * Updates {@code String} thirdPartyId in memory and in the datastore.
     * If provided thirdPartyId is null or empty value, removes the value in persistence.
     * If not, stores the new thirdPartyId in persistence.
     *
     * @param updatedThirdPartyId {@link String} containing new thirdPartyId that needs to be set.
     */
    void setThirdPartyId(final String updatedThirdPartyId) {
        thirdPartyId = updatedThirdPartyId;
        if (dataStore != null) {
            if (StringUtils.isNullOrEmpty(thirdPartyId)) {
                dataStore.remove(TargetConstants.DataStoreKeys.THIRD_PARTY_ID);
            } else {
                dataStore.setString(TargetConstants.DataStoreKeys.THIRD_PARTY_ID, thirdPartyId);
            }
        }
    }

    /**
     * Updates {@code String} edge host in memory and in the datastore.
     * If provided edge host is null or empty, removes the value in persistence.
     * If not, stores the new edge host in persistence.
     *
     * @param updatedEdgeHost {@link String} containing the new edge host to be set
     */
    void updateEdgeHost(final String updatedEdgeHost) {
        if ((edgeHost == null && updatedEdgeHost == null) || (edgeHost != null && edgeHost.equals(updatedEdgeHost))) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
                    "updateEdgeHost - Data store is not updated as the provided edge host is same as the existing edgeHost");
            return;
        }

        edgeHost = updatedEdgeHost;

        if (dataStore != null) {
            if (StringUtils.isNullOrEmpty(edgeHost)) {
                dataStore.remove(TargetConstants.DataStoreKeys.EDGE_HOST);
            } else {
                dataStore.setString(TargetConstants.DataStoreKeys.EDGE_HOST, edgeHost);
            }
        }
    }

    /**
     * Generates target extension's state data for sharing.
     *
     * @return {@code Map<String, Object>} of this extension's state data
     */
    Map<String, Object> generateSharedState() {
        final Map<String, Object> data = new HashMap<>();

        if (!StringUtils.isNullOrEmpty(tntId)) {
            data.put(TargetConstants.EventDataKeys.TNT_ID, tntId);
        }

        if (!StringUtils.isNullOrEmpty(thirdPartyId)) {
            data.put(TargetConstants.EventDataKeys.THIRD_PARTY_ID, thirdPartyId);
        }

        return data;
    }

    void mergePrefetchedMboxJson(final Map<String, JSONObject> mboxMap) {
        prefetchedMbox.putAll(mboxMap);
    }

    /**
     * Removes mboxes from loadedMboxes if they are also present in the prefetchedMboxes cache
     */
    void removeDuplicateLoadedMboxes() {
        for (String mboxName : prefetchedMbox.keySet()) {
            if (mboxName != null) {
                loadedMbox.remove(mboxName);
            }
        }
    }

    Map<String, JSONObject> getPrefetchedMbox() {
        return prefetchedMbox;
    }

    Map<String, JSONObject> getLoadedMbox() {
        return loadedMbox;
    }

    void clearNotifications() {
        notifications.clear();
    }

    void addNotification(final JSONObject notification) {
        notifications.add(notification);
    }

    /**
     * Verifies if current target session is expired.
     *
     * @return {@code boolean} indicating whether Target session has expired
     */
    private boolean isSessionExpired() {
        final long currentTimeSeconds = TimeUtils.getUnixTimeInSeconds();

        return (sessionTimestampInSeconds > 0) && ((currentTimeSeconds - sessionTimestampInSeconds) > getSessionTimeout());
    }
}
