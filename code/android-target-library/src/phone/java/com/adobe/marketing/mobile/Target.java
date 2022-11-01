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

import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.target.TargetConstants;
import com.adobe.marketing.mobile.target.TargetExtension;

import static com.adobe.marketing.mobile.target.TargetConstants.LOG_TAG;

/**
 * Public class containing APIs for the Target extension.
 */
public class Target {
    public static final Class<? extends Extension> EXTENSION = TargetExtension.class;
    private static final String CLASS_NAME = "Target";

    private Target() {}

    /**
     * Returns the version of the {@code Target} extension.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    public static String extensionVersion() {
        return TargetConstants.EXTENSION_VERSION;
    }

    /**
     * Registers the extension with the Mobile Core.
     * <p>
     * Note: This method should be called only once in your application class.
     */
    @Deprecated
    public static void registerExtension() {
        MobileCore.registerExtension(TargetExtension.class, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                Log.warning(LOG_TAG, CLASS_NAME,
                        "An error occurred while registering the Target extension: " + extensionError.getErrorName());
            }
        });
    }
}