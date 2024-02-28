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

import java.util.Arrays;
import java.util.List;

final class TargetJson {
    static final String MESSAGE = "message";
    static final String ID = "id";
    static final String ID_TNT_ID = "tntId";
    static final String ID_THIRD_PARTY_ID = "thirdPartyId";
    static final String ID_MARKETING_CLOUD_VISITOR_ID = "marketingCloudVisitorId";
    static final String ID_CUSTOMER_IDS = "customerIds";
    static final String ANALYTICS_LOGGING = "logging";
    static final String ANALYTICS_CLIENT_SIDE = "client_side";
    static final String AAM_PARAMETERS = "audienceManager";
    static final String EXPERIENCE_CLOUD = "experienceCloud";
    static final String CONTEXT_PARAMETERS = "context";
    static final String ANALYTICS_PARAMETERS = "analytics";
    static final String ANALYTICS_PAYLOAD = "payload";
    static final String OPTIONS = "options";
    static final String METRICS = "metrics";
    static final String ENVIRONMENT_ID = "environmentId"; // client environment id, optional
    static final String PREFETCH = "prefetch";
    static final String EXECUTE = "execute";
    static final String MBOX_RESPONSES = "mboxResponses";
    static final String PARAMETERS = "parameters";
    static final String PROFILE_PARAMETERS = "profileParameters";
    static final String PRODUCT = "product";
    static final String ORDER = "order";
    static final String NOTIFICATIONS = "notifications";
    static final String MBOXES = "mboxes";
    static final String EDGE_HOST = "edgeHost";
    static final String TOKEN = "token";
    static final String PROPERTY = "property";
    static final String HTML = "html";
    static final String JSON = "json";

    static final List<String> CACHED_MBOX_ACCEPTED_KEYS =
            Arrays.asList(Mbox.NAME, Mbox.STATE, OPTIONS, ANALYTICS_PARAMETERS, METRICS);

    static class CustomerIds {
        static final String ID = "id";
        static final String INTEGRATION_CODE = "integrationCode";
        static final String AUTHENTICATION_STATE = "authenticatedState";

        private CustomerIds() {}
    }

    static class Context {
        static final String CHANNEL = "channel";
        static final String CHANNEL_MOBILE = "mobile";
        static final String MOBILE_PLATFORM = "mobilePlatform";
        static final String APPLICATION = "application";
        static final String SCREEN = "screen";
        static final String USER_AGENT = "userAgent";
        static final String TIME_OFFSET = "timeOffsetInMinutes";
        static final String PLATFORM_TYPE = "platformType";
        static final String DEVICE_NAME = "deviceName";
        static final String DEVICE_TYPE = "deviceType";
        static final String APP_ID = "id";
        static final String APP_NAME = "name";
        static final String APP_VERSION = "version";
        static final String SCREEN_WIDTH = "width";
        static final String SCREEN_HEIGHT = "height";
        static final String SCREEN_COLOR_DEPTH = "colorDepth";
        static final int COLOR_DEPTH_32 = 32;
        static final String SCREEN_ORIENTATION = "orientation";
        static final String ORIENTATION_PORTRAIT = "portrait";
        static final String ORIENTATION_LANDSCAPE = "landscape";

        private Context() {}
    }

    static class AAMParameters {
        static final String BLOB = "blob";
        static final String LOCATION_HINT = "locationHint";

        private AAMParameters() {}
    }

    static class Mbox {
        static final String NAME = "name";
        static final String STATE = "state";
        static final String INDEX = "index";

        private Mbox() {}
    }

    static class Option {
        static final String TYPE = "type";
        static final String CONTENT = "content";
        static final String RESPONSE_TOKENS = "responseTokens";

        private Option() {}
    }

    static class Metric {
        static final String TYPE = "type";
        static final String EVENT_TOKEN = "eventToken";

        private Metric() {}
    }

    static class Notification {
        static final String ID = "id";
        static final String TIMESTAMP = "timestamp";
        static final String TOKENS = "tokens";
        static final String TYPE = "type";
        static final String MBOX = "mbox";

        private Notification() {}
    }

    static class Product {
        static final String ID = "id";
        static final String CATEGORY_ID = "categoryId";

        private Product() {}
    }

    static class Order {
        static final String ID = "id";
        static final String TOTAL = "total";
        static final String PURCHASED_PRODUCT_IDS = "purchasedProductIds";

        private Order() {}
    }

    static class MetricType {
        static final String DISPLAY = "display";
        static final String CLICK = "click";

        private MetricType() {}
    }

    private TargetJson() {}
}
