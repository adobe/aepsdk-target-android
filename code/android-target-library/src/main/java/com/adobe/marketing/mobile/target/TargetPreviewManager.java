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

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ui.UIService;

public class TargetPreviewManager {

    // ========================================================================================
    // private fields
    // ========================================================================================

    private Networking networkService;
    private UIService uiService;
    protected String previewParams;
    protected String token;
    protected String endPoint;
    protected String webViewHtml;
    protected String restartUrl;
    private Boolean fetchingWebView;
    private String clientCode;
    private ExtensionApi extensionApi;

    /**
     * Constructor, returns an instance of the {@code TargetPreviewManager}.
     *
     * @param networkService an instance of {@link Networking} to be used by the extension
     * @param uiService an instance of {@link UIService} to be used by the extension
     * @see com.adobe.marketing.mobile.services.ServiceProvider
     */
    TargetPreviewManager(final Networking networkService, final UIService uiService,
                         final ExtensionApi extensionApi) {
        this.networkService = networkService;
        this.uiService = uiService;
        fetchingWebView = false;
        this.extensionApi = extensionApi;
    }

    protected void enterPreviewModeWithDeepLinkParams(final String clientCode, final String deepLink) {
        // todo
    }

    /**
     * Sets the local variable {@code #restartUrl} with the provided value.
     *
     * @param restartDeepLink {@link String} deeplink
     */
    protected void setRestartDeepLink(final String restartDeepLink) {
        restartUrl = restartDeepLink;
    }

    /**
     * Returns current preview parameters representing the json received from target servers as a {@code String}.
     * <p>
     * Or null if preview mode was reset or not started.
     *
     * @return {@link String} preview parameters
     */
    protected String getPreviewParameters() {
        return previewParams;
    }

    /**
     * Returns the current preview token if available.
     * <p>
     * Or returns null if preview mode was reset or not started.
     *
     * @return {@link String}  preview token
     */
    protected String getPreviewToken() {
        return token;
    }
}
