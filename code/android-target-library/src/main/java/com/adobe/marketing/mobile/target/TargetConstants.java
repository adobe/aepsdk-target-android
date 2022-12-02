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

public final class TargetConstants {
    public static final String LOG_TAG = "Target";
    public static final String EXTENSION_VERSION = "2.0.0";
    static final String EXTENSION_NAME = "com.adobe.target";
    static final String FRIENDLY_NAME = "Target";

    static final String API_URL_HOST_BASE 		= "%s.tt.omtrdc.net";
    static final String EDGE_HOST_BASE = "mboxedge%s";
    static final String PREFETCH_API_URL_BASE 	= "https://%s/rest/v1/delivery/?client=%s&sessionId=%s";
    static final String REQUEST_CONTENT_TYPE  	= "application/json";
    static final String MBOX_AT_PROPERTY_KEY  		= "at_property";

    static final int DEFAULT_NETWORK_TIMEOUT = 2;
    static final int DEFAULT_TARGET_SESSION_TIMEOUT_SEC = 30 * 60; // 30 mins

    private TargetConstants() {}

    // datastore keys
    static final String DATA_STORE_KEY = "ADOBEMOBILE_TARGET";

    static class DataStoreKeys {
        static final String TNT_ID                        = "TNT_ID";
        static final String THIRD_PARTY_ID                = "THIRD_PARTY_ID";
        static final String SESSION_ID = "SESSION_ID";
        static final String SESSION_TIMESTAMP = "SESSION_TIMESTAMP";
        static final String EDGE_HOST = "EDGE_HOST";

        private DataStoreKeys() {}
    }

    public static final class EventName {
        public static final String PREFETCH_CONTENT = "TargetPrefetchContent";
        public static final String LOAD_REQUEST = "TargetLoadRequest";
        public static final String LOCATIONS_DISPLAYED = "TargetLocationsDisplayed";
        public static final String LOCATION_CLICKED = "TargetLocationClicked";
        public static final String TARGET_REQUEST_RESPONSE = "TargetRequestResponse";
        public static final String GET_THIRD_PARTY_ID = "TargetGetThirdPartyIdentifier";
        public static final String SET_THIRD_PARTY_ID = "TargetSetThirdPartyIdentifier";
        public static final String GET_TNT_ID = "TargetGetTnTIdentifier";
        public static final String SET_TNT_ID = "TargetSetTnTIdentifier";
        public static final String GET_SESSION_ID = "TargetGetSessionIdentifier";
        public static final String SET_SESSION_ID = "TargetSetSessionIdentifier";
        public static final String REQUEST_RESET = "TargetRequestReset";
        public static final String CLEAR_PREFETCH_CACHE = "TargetClearPrefetchCache";
        public static final String SET_PREVIEW_DEEPLINK = "TargetSetPreviewRestartDeeplink";
        public static final String TARGET_RAW_REQUEST = "TargetRawRequest";
        public static final String TARGET_RAW_NOTIFICATIONS = "TargetRawNotifications";
        public static final String IDENTITY_RESPONSE_EVENT_NAME = "TargetIdentity";
        public static final String ANALYTICS_FOR_TARGET_REQUEST_EVENT_NAME = "AnalyticsForTargetRequest";
        public static final String IDENTITY_RESET_COMPLETION_EVENT_NAME = "TargetReset";
        public static final String TARGET_RESPONSE_EVENT_NAME = "TargetResponse";
        public static final String TARGET_RAW_RESPONSE_EVENT_NAME = "TargetRawResponse";
        public static final String TARGET_VIEW_PREFETCH_RESPONSE_EVENT_NAME = "TargetViewPrefetchResponse";
        public static final String TARGET_PREVIEW_LIFECYCLE_EVENT_NAME = "TargetPreviewLifecycle";

        private EventName() {}
    }

    public static final class EventType {
        public static final String TARGET = "com.adobe.eventType.target";

        private EventType() {}
    }


    public static final class EventSource {
        public static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
        public static final String RESPONSE_CONTENT = "com.adobe.eventSource.responseContent";
        public static final String REQUEST_IDENTITY = "com.adobe.eventSource.requestIdentity";
        public static final String REQUEST_RESET = "com.adobe.eventSource.requestReset";
        
        private EventSource() {}
    }

    public static final class EventDataKeys {
        public static final String MBOX_NAME = "mboxname";
        public static final String MBOX_NAMES = "mboxnames";
        public static final String TARGET_PARAMETERS = "targetparams";
        static final String MBOX_PARAMETERS = "mboxparameters";
        static final String PROFILE_PARAMETERS = "profileparameters";
        static final String ORDER_PARAMETERS = "orderparameters";
        static final String PRODUCT_PARAMETERS = "productparameters";
        static final String DEFAULT_CONTENT = "defaultcontent";
        static final String RESPONSE_PAIR_ID = "responsepairid";
        public static final String EXECUTE = "execute";
        public static final String PREFETCH = "prefetch";
        public static final String LOAD_REQUEST = "request";
        public static final String PREFETCH_ERROR = "prefetcherror";
        public static final String IS_LOCATION_DISPLAYED = "islocationdisplayed";
        public static final String IS_LOCATION_CLICKED = "islocationclicked";
        public static final String THIRD_PARTY_ID = "thirdpartyid";
        public static final String TNT_ID         = "tntid";
        public static final String SESSION_ID = "sessionid";
        public static final String RESET_EXPERIENCE = "resetexperience";
        public static final String CLEAR_PREFETCH_CACHE = "clearcache";
        public static final String PREVIEW_RESTART_DEEP_LINK = "restartdeeplink";
        public static final String IS_RAW_EVENT = "israwevent";
        public static final String NOTIFICATIONS = "notifications";
        public static final String RESPONSE_DATA = "responsedata";
        public static final String TARGET_RESPONSE_EVENT_ID = "responseEventId";
        public static final String TARGET_RESPONSE_PAIR_ID = "responsePairId";
        public static final String ANALYTICS_PAYLOAD = "analytics.payload";
        public static final String RESPONSE_TOKENS = "responseTokens";
        public static final String CLICK_METRIC_ANALYTICS_PAYLOAD = "clickmetric.analytics.payload";
        public static final String TARGET_CONTENT = "content";
        public static final String TARGET_DATA_PAYLOAD = "data";
        static final String ID = "id";
        static final String TOKEN = "token";
        static final String CONTEXT = "context";
        static final String EXPERIENCE_CLOUD = "experienceCloud";
        static final String PROPERTY = "property";
        static final String ENVIRONMENT_ID = "environmentId";

        static final class Order {
            static final String ID = "id";
            static final String TOTAL = "total";
            static final String PURCHASED_PRODUCT_IDS = "purchasedProductIds";

            private Order() {}
        }

        static final class Product {
            static final String ID = "id";
            static final String CATEGORY_ID = "categoryId";

            private Product() {}
        }

        private EventDataKeys() {}
    }

    static final class Configuration {
        static final String EXTENSION_NAME       = "com.adobe.module.configuration";
        static final String GLOBAL_CONFIG_PRIVACY            = "global.privacy";
        static final String TARGET_CLIENT_CODE = "target.clientCode";
        static final String TARGET_PREVIEW_ENABLED = "target.previewEnabled";
        static final String TARGET_NETWORK_TIMEOUT = "target.timeout";
        static final String TARGET_ENVIRONMENT_ID = "target.environmentId";
        static final String TARGET_PROPERTY_TOKEN = "target.propertyToken";
        static final String TARGET_SESSION_TIMEOUT = "target.sessionTimeout";
        static final String TARGET_SERVER = "target.server";

        private Configuration() {}
    }

    static final class Identity {
        static final String EXTENSION_NAME = "com.adobe.module.identity";
        static final String VISITOR_ID_MID = "mid";
        static final String VISITOR_ID_BLOB = "blob";
        static final String VISITOR_ID_LOCATION_HINT = "locationhint";
        static final String VISITOR_IDS_LIST = "visitoridslist";
        static final String ADVERTISING_IDENTIFIER = "advertisingidentifier";

        private Identity() {}
    }
}