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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.services.Log;

import static com.adobe.marketing.mobile.target.TargetConstants.EXTENSION_NAME;
import static com.adobe.marketing.mobile.target.TargetConstants.FRIENDLY_NAME;
import static com.adobe.marketing.mobile.target.TargetConstants.EXTENSION_VERSION;

import java.util.Map;

public class TargetExtension extends Extension {
    private static final String CLASS_NAME = TargetExtension.class.getSimpleName();
    /**
     * Constructor for {@code TargetExtension}.
     * <p>
     * It is invoked during the extension registration to retrieve the extension's details such as name and version.
     *
     * @param extensionApi {@link ExtensionApi} instance.
     */
    protected TargetExtension(final ExtensionApi extensionApi) {
        super(extensionApi);
    }

    /**
     * Retrieve the extension name.
     *
     * @return {@link String} containing the unique name for this extension.
     */
    @Override
    protected String getName() {
        return EXTENSION_NAME;
    }

    /**
     * Retrieve the extension friendly name.
     *
     * @return {@link String} containing the friendly name for this extension.
     */
    @Override
    protected String getFriendlyName() { return FRIENDLY_NAME; }

    /**
     * Retrieve the extension version.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    @Override
    protected String getVersion() {
        return EXTENSION_VERSION;
    }

    @Override
    protected void onRegistered() {
         getApi().registerEventListener(TargetConstants.EventType.TARGET, TargetConstants.EventSource.REQUEST_CONTENT,
                 this::handleTargetRequestEvent
         );
    }

    void handleTargetRequestEvent(final Event event) {
    }
}