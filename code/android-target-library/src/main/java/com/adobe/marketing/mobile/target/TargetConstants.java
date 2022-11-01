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

    private TargetConstants() {}

    static final class EventNames {

        private EventNames() {}
    }

    static final class EventType {
        static final String TARGET = "com.adobe.eventType.target";

        private EventType() {}
    }


    static final class EventSource {
        static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
        
        private EventSource() {}
    }

    static final class EventDataKeys {

        private EventDataKeys() {}
    }

    static final class Configuration {
        static final String EXTENSION_NAME = "com.adobe.module.configuration";

        private Configuration() {}
    }
}