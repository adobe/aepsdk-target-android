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

import java.util.HashMap;

class TargetTestConstants {

    static final String LOG_TAG = TargetExtension.class.getSimpleName();
    static final String A4T_ACTION_NAME = "AnalyticsForTarget";
    static final String OLD_API_COMPAT_PARAM = "__oldTargetSdkApiCompatParam__";

    static final String API_URL_HOST_BASE = "%s.tt.omtrdc.net";
    static final String PREFETCH_API_URL_BASE = "https://%s/rest/v1/delivery/?client=%s&sessionId=%s";
    static final String REQUEST_CONTENT_TYPE = "application/json";

    // Target preview Constants
    static final String PREVIEW_MESSAGE_ID = "target-preview-message-id";
    static final String PREVIEW_PARAMETERS = "at_preview_params";
    static final String PREVIEW_TOKEN = "at_preview_token";
    static final String PREVIEW_ENDPOINT = "at_preview_endpoint";
    static final String PREVIEW_QA_MODE = "qaMode";
    static final String DEFAULT_TARGET_PREVIEW_ENDPOINT = "hal.testandtarget.omniture.com";
    static final String DEEPLINK = "deeplink";
    static final String DEEPLINK_SCHEME = "adbinapp";
    static final String DEEPLINK_SCHEME_PATH_CANCEL = "cancel";
    static final String DEEPLINK_SCHEME_PATH_CONFIRM = "confirm";
    static final String MBOX_AT_PROPERTY_KEY = "at_property";

    static final HashMap<String, String> MAP_TO_CONTEXT_DATA_KEYS = createMap();

    static HashMap<String, String> createMap() {
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put(EventDataKeys.Identity.ADVERTISING_IDENTIFIER, ContextDataKeys.ADVERTISING_IDENTIFIER);
        map.put(EventDataKeys.Lifecycle.APP_ID, ContextDataKeys.APPLICATION_IDENTIFIER);
        map.put(EventDataKeys.Lifecycle.CARRIER_NAME, ContextDataKeys.CARRIER_NAME);
        map.put(EventDataKeys.Lifecycle.CRASH_EVENT, ContextDataKeys.CRASH_EVENT_KEY);
        map.put(EventDataKeys.Lifecycle.DAILY_ENGAGED_EVENT, ContextDataKeys.DAILY_ENGAGED_EVENT_KEY);
        map.put(EventDataKeys.Lifecycle.DAY_OF_WEEK, ContextDataKeys.DAY_OF_WEEK);
        map.put(EventDataKeys.Lifecycle.DAYS_SINCE_FIRST_LAUNCH, ContextDataKeys.DAYS_SINCE_FIRST_LAUNCH);
        map.put(EventDataKeys.Lifecycle.DAYS_SINCE_LAST_LAUNCH, ContextDataKeys.DAYS_SINCE_LAST_LAUNCH);
        map.put(EventDataKeys.Lifecycle.DAYS_SINCE_LAST_UPGRADE, ContextDataKeys.DAYS_SINCE_LAST_UPGRADE);
        map.put(EventDataKeys.Lifecycle.DEVICE_NAME, ContextDataKeys.DEVICE_NAME);
        map.put(EventDataKeys.Lifecycle.DEVICE_RESOLUTION, ContextDataKeys.DEVICE_RESOLUTION);
        map.put(EventDataKeys.Lifecycle.HOUR_OF_DAY, ContextDataKeys.HOUR_OF_DAY);
        map.put(EventDataKeys.Lifecycle.IGNORED_SESSION_LENGTH, ContextDataKeys.IGNORED_SESSION_LENGTH);
        map.put(EventDataKeys.Lifecycle.INSTALL_DATE, ContextDataKeys.INSTALL_DATE);
        map.put(EventDataKeys.Lifecycle.INSTALL_EVENT, ContextDataKeys.INSTALL_EVENT_KEY);
        map.put(EventDataKeys.Lifecycle.LAUNCH_EVENT, ContextDataKeys.LAUNCH_EVENT_KEY);
        map.put(EventDataKeys.Lifecycle.LAUNCHES, ContextDataKeys.LAUNCHES);
        map.put(EventDataKeys.Lifecycle.LAUNCHES_SINCE_UPGRADE, ContextDataKeys.LAUNCHES_SINCE_UPGRADE);
        map.put(EventDataKeys.Lifecycle.LOCALE, ContextDataKeys.LOCALE);
        map.put(EventDataKeys.Lifecycle.MONTHLY_ENGAGED_EVENT, ContextDataKeys.MONTHLY_ENGAGED_EVENT_KEY);
        map.put(EventDataKeys.Lifecycle.OPERATING_SYSTEM, ContextDataKeys.OPERATING_SYSTEM);
        map.put(EventDataKeys.Lifecycle.PREVIOUS_SESSION_LENGTH, ContextDataKeys.PREVIOUS_SESSION_LENGTH);
        map.put(EventDataKeys.Lifecycle.RUN_MODE, ContextDataKeys.RUN_MODE);
        map.put(EventDataKeys.Lifecycle.UPGRADE_EVENT, ContextDataKeys.UPGRADE_EVENT_KEY);

        return map;
    }

    // contextdata keys
    static final class ContextDataKeys {
        static final String INSTALL_EVENT_KEY = "a.InstallEvent";
        static final String LAUNCH_EVENT_KEY = "a.LaunchEvent";
        static final String CRASH_EVENT_KEY = "a.CrashEvent";
        static final String UPGRADE_EVENT_KEY = "a.UpgradeEvent";
        static final String DAILY_ENGAGED_EVENT_KEY = "a.DailyEngUserEvent";
        static final String MONTHLY_ENGAGED_EVENT_KEY = "a.MonthlyEngUserEvent";
        static final String INSTALL_DATE = "a.InstallDate";
        static final String LAUNCHES = "a.Launches";
        static final String PREVIOUS_SESSION_LENGTH = "a.PrevSessionLength";
        static final String DAYS_SINCE_FIRST_LAUNCH = "a.DaysSinceFirstUse";
        static final String DAYS_SINCE_LAST_LAUNCH = "a.DaysSinceLastUse";
        static final String HOUR_OF_DAY = "a.HourOfDay";
        static final String DAY_OF_WEEK = "a.DayOfWeek";
        static final String OPERATING_SYSTEM = "a.OSVersion";
        static final String APPLICATION_IDENTIFIER = "a.AppID";
        static final String DAYS_SINCE_LAST_UPGRADE = "a.DaysSinceLastUpgrade";
        static final String LAUNCHES_SINCE_UPGRADE = "a.LaunchesSinceUpgrade";
        static final String ADVERTISING_IDENTIFIER = "a.adid";
        static final String DEVICE_NAME = "a.DeviceName";
        static final String DEVICE_RESOLUTION = "a.Resolution";
        static final String CARRIER_NAME = "a.CarrierName";
        static final String LOCALE = "a.locale";
        static final String RUN_MODE = "a.RunMode";
        static final String IGNORED_SESSION_LENGTH = "a.ignoredSessionLength";

        private ContextDataKeys() {
        }
    }

    // datastore keys
    static final String DATA_STORE_KEY = "ADOBEMOBILE_TARGET";

    static class DataStoreKeys {
        static final String TNT_ID = "TNT_ID";
        static final String THIRD_PARTY_ID = "THIRD_PARTY_ID";
        static final String SESSION_ID = "SESSION_ID";
        static final String SESSION_TIMESTAMP = "SESSION_TIMESTAMP";
        static final String EDGE_HOST = "EDGE_HOST";

        private DataStoreKeys() {
        }
    }

    static final int DEFAULT_NETWORK_TIMEOUT = 2;
    static final int DEFAULT_TARGET_SESSION_TIMEOUT_SEC = 30 * 60; // 30 mins

    private TargetTestConstants() {
    }

    static final class Events {
        static final String TARGET_VIEW_PREFETCH = "TargetViewPrefetchRequest";
        static final String TARGET_PREVIEW_LIFECYCLE = "TargetPreviewLifecycle";
        static final String TARGET_RAW_REQUEST = "TargetRawRequest";
        static final String TARGET_RAW_NOTIFICATIONS = "TargetRawNotifications";

        private Events() {
        }
    }

    static final class EventDataKeys {

        private EventDataKeys() {
        }

        static final class Analytics {
            static final String TRACK_INTERNAL = "trackinternal";
            static final String TRACK_ACTION = "action";
            static final String CONTEXT_DATA = "contextdata";

            private Analytics() {
            }
        }

        static final class Audience {
            static final String EXTENSION_NAME = "com.adobe.module.audience";
            static final String DPID = "dpid";
            static final String DPUUID = "dpuuid";
            static final String UUID = "uuid";

            private Audience() {
            }
        }

        static final class Configuration {
            static final String EXTENSION_NAME = "com.adobe.module.configuration";
            static final String GLOBAL_CONFIG_PRIVACY = "global.privacy";
            static final String TARGET_CLIENT_CODE = "target.clientCode";
            static final String TARGET_NETWORK_TIMEOUT = "target.timeout";
            static final String TARGET_ENVIRONMENT_ID = "target.environmentId";
            static final String TARGET_SERVER = "target.server";

            private Configuration() {
            }
        }

        static final class Identity {
            static final String EXTENSION_NAME = "com.adobe.module.identity";
            static final String VISITOR_ID_MID = "mid";
            static final String VISITOR_ID_BLOB = "blob";
            static final String VISITOR_ID_LOCATION_HINT = "locationhint";
            static final String VISITOR_IDS_LIST = "visitoridslist";
            static final String ADVERTISING_IDENTIFIER = "advertisingidentifier";

            private Identity() {
            }
        }

        static final class Lifecycle {
            static final String EXTENSION_NAME = "com.adobe.module.lifecycle";

            static final String APP_ID = "appid";
            static final String CARRIER_NAME = "carriername";
            static final String CRASH_EVENT = "crashevent";
            static final String DAILY_ENGAGED_EVENT = "dailyenguserevent";
            static final String DAY_OF_WEEK = "dayofweek";
            static final String DAYS_SINCE_FIRST_LAUNCH = "dayssincefirstuse";
            static final String DAYS_SINCE_LAST_LAUNCH = "dayssincelastuse";
            static final String DAYS_SINCE_LAST_UPGRADE = "dayssincelastupgrade";
            static final String DEVICE_NAME = "devicename";
            static final String DEVICE_RESOLUTION = "resolution";
            static final String HOUR_OF_DAY = "hourofday";
            static final String IGNORED_SESSION_LENGTH = "ignoredsessionlength";
            static final String INSTALL_DATE = "installdate";
            static final String INSTALL_EVENT = "installevent";
            static final String LAUNCH_EVENT = "launchevent";
            static final String LAUNCHES = "launches";
            static final String LAUNCHES_SINCE_UPGRADE = "launchessinceupgrade";
            static final String LIFECYCLE_CONTEXT_DATA = "lifecyclecontextdata";
            static final String LOCALE = "locale";
            static final String MONTHLY_ENGAGED_EVENT = "monthlyenguserevent";
            static final String OPERATING_SYSTEM = "osversion";
            static final String PREVIOUS_SESSION_LENGTH = "prevsessionlength";
            static final String RUN_MODE = "runmode";
            static final String UPGRADE_EVENT = "upgradeevent";

            private Lifecycle() {
            }
        }

        static final String EXTENSION_NAME = "com.adobe.module.target";

        static final String MBOX_NAME = "mboxname";
        static final String MBOX_NAMES = "mboxnames";
        static final String TNT_ID = "tntid";
        static final String THIRD_PARTY_ID = "thirdpartyid";
        static final String SESSION_ID = "sessionid";
        static final String LOAD_REQUESTS = "request";
        static final String TARGET_CONTENT = "content";
        static final String PREFETCH_REQUESTS = "prefetch";
        static final String PREFETCH_ERROR = "prefetcherror";
        static final String PREFETCH_RESULT = "prefetchresult";
        static final String PREFETCH_VIEWS = "prefetchviews";
        static final String PREFETCH_CACHE_ONLY = "cacheonly";
        static final String RESET_EXPERIENCE = "resetexperience";
        static final String CLEAR_PREFETCH_CACHE = "clearcache";
        static final String PROFILE_PARAMETERS = "profileparams";
        static final String MBOX_PARAMETERS = "mboxparameters";
        static final String ORDER_PARAMETERS = "orderparameters";
        static final String PRODUCT_PARAMETERS = "productparameters";
        static final String PREVIEW_RESTART_DEEP_LINK = "restartdeeplink";
        static final String IS_LOCATION_CLICKED = "islocationclicked";
        static final String IS_LOCATION_DISPLAYED = "islocationdisplayed";
        static final String LOCATION_CLICKED = "TargetLocationClicked";
        static final String LOCATIONS_DISPLAYED = "TargetLocationsDisplayed";
        static final String TARGET_VIEW_NOTIFICATION = "TargetViewNotificationRequest";
        static final String TARGET_VIEW_PREFETCH = "TargetViewPrefetchRequest";
        static final String DEFAULT_CONTENT = "defaultcontent";
        static final String BASE_CLASS = "baseclass";
        static final String RESPONSE_PAIR_ID = "responsepairid";
        static final String TARGET_PARAMETERS = "targetparams";
        static final String VIEW_NOTIFICATIONS = "viewnotifications";
        static final String PROPERTY_TOKEN = "propertytoken";
        static final String PREVIEW_INITIATED = "ispreviewinitiated";
        static final String A4T_SESSION_ID = "a.target.sessionId"; // For A4T requests event data.
        static final String ANALYTICS_PAYLOAD = "analytics.payload";
        static final String RESPONSE_TOKENS = "responseTokens";
        static final String CLICK_METRIC_ANALYTICS_PAYLOAD = "clickmetric.analytics.payload";
        static final String IS_RAW_EVENT = "israwevent";
        static final String NOTIFICATION = "notification";
        static final String RESPONSE_DATA = "responsedata";

    }
}

