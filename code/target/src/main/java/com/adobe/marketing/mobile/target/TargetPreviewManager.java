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

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.NetworkingConstants;
import com.adobe.marketing.mobile.services.ui.FloatingButton;
import com.adobe.marketing.mobile.services.ui.InAppMessage;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.ui.floatingbutton.FloatingButtonSettings;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageSettings;
import com.adobe.marketing.mobile.services.uri.UriOpening;
import com.adobe.marketing.mobile.util.DefaultPresentationUtilityProvider;
import com.adobe.marketing.mobile.util.StreamUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.URLBuilder;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

class TargetPreviewManager {

    private static final String CLASS_NAME = "TargetPreviewManager";
    static final String CHARSET_UTF_8 = "UTF-8";

    // ========================================================================================
    // private fields
    // ========================================================================================

    private final Networking networkService;
    private final UIService uiService;
    private final UriOpening uriService;
    private final Context context;
    protected String previewParams;
    protected String token;
    protected String endPoint;
    protected String webViewHtml;
    protected String restartUrl;
    private Boolean fetchingWebView;
    private String clientCode;

    protected Presentable<FloatingButton> floatingButtonPresentable;

    /**
     * Constructor, returns an instance of the {@code TargetPreviewManager}.
     *
     * @param networkService an instance of {@link Networking} to be used by the extension
     * @param uiService an instance of {@link UIService} to be used by the extension
     * @param uriService an instance of {@link UriOpening} to be used by the extension
     * @see com.adobe.marketing.mobile.services.ServiceProvider
     */
    TargetPreviewManager(
            final Networking networkService,
            final UIService uiService,
            final UriOpening uriService,
            final Context context) {
        this.networkService = networkService;
        this.uiService = uiService;
        this.uriService = uriService;
        this.context = context;
        fetchingWebView = false;
    }

    // ========================================================================================
    // protected methods
    // ========================================================================================

    /**
     * Returns current preview parameters representing the json received from target servers as a
     * {@code String}.
     *
     * <p>Or null if preview mode was reset or not started.
     *
     * @return {@link String} preview parameters
     */
    protected String getPreviewParameters() {
        return previewParams;
    }

    /**
     * Returns the current preview token if available.
     *
     * <p>Or returns null if preview mode was reset or not started.
     *
     * @return {@link String} preview token
     */
    protected String getPreviewToken() {
        return token;
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
     * Starts the preview mode by parsing the preview deep link, fetching the webView from target
     * server, displaying the preview button and creating a new custom message for the preview view.
     *
     * @param clientCode a {@link String} clientcode
     * @param deepLink a {@link String} target preview deeplink
     */
    protected void enterPreviewModeWithDeepLinkParams(
            final String clientCode, final String deepLink) {

        if (networkService == null) {
            Log.warning(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "enterPreviewModeWithDeepLinkParams - Unable to enter preview mode,"
                            + " NetworkServices is not available.");
            return;
        }

        if (uiService == null) {
            Log.warning(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "enterPreviewModeWithDeepLinkParams - Unable to enter preview mode, UIService"
                            + " is not available.");
            return;
        }

        if (uriService == null) {
            Log.warning(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "enterPreviewModeWithDeepLinkParams - Unable to enter preview mode, UriOpening"
                            + " is not available.");
            return;
        }

        this.clientCode = clientCode;

        // bail out on null/empty deepLink
        if (StringUtils.isNullOrEmpty(deepLink)) {
            Log.warning(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "enterPreviewModeWithDeepLinkParams - Unable to enter preview mode with"
                            + " empty/invalid url");
            return;
        }

        // bail out on malformed deepLink
        URI url;
        try {
            url = URI.create(deepLink);
        } catch (final IllegalArgumentException e) {
            Log.warning(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    String.format(
                            "enterPreviewModeWithDeepLinkParams - Unable to enter preview mode,"
                                    + " Invalid deep link provided, %s. Error (%s)",
                            deepLink, e.getMessage()));
            return;
        }

        // bail out on no preview parameters found in deepLink
        final String query = url.getRawQuery();
        final Map<String, String> queryParams = TargetUtils.extractQueryParameters(query);

        if (TargetUtils.isNullOrEmpty(queryParams)) {
            Log.warning(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    String.format(
                            "enterPreviewModeWithDeepLinkParams - Unable to enter preview mode."
                                    + " Cannot retrieve preview token from provided deeplink : %s",
                            deepLink));
            return;
        }

        // setup for preview
        if (setupTargetPreviewParameters(queryParams)) {
            createAndShowFloatingButton();
            fetchWebView();
        }
    }

    /**
     * This method will be called by the {@code TargetPreviewFullscreenEventListener} to process
     * given url.
     *
     * <p>If it is a cancel url, it dismisses the message and exits preview mode. If it is a confirm
     * url, it dismisses the message, updates preview parameters and starts a new view if preview
     * restart url is set.
     *
     * @param message A target preview {@link Presentable<InAppMessage>} instance
     * @param stringUrl A {@link String} url associated with the clicked button
     */
    protected void previewConfirmedWithUrl(
            final Presentable<InAppMessage> message, final String stringUrl) {

        // dismiss the message without exiting preview mode.
        // remove the message no matter what the clicked URL is.
        // This prevents us from showing a "NotFound" webPage on invalid URI.
        message.dismiss();

        // bail out on preview confirm/cancel deepLink
        URI url;

        try {
            url = URI.create(stringUrl);
        } catch (final Exception e) {
            Log.debug(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "previewConfirmedWithUrl - Invalid URL obtained from Target Preview Message %s",
                    stringUrl);
            return;
        }

        // bail out if the url scheme is wrong
        final String scheme = url.getScheme();

        if (!TargetConstants.PreviewKeys.DEEPLINK_SCHEME.equals(scheme)) {
            Log.debug(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "previewConfirmedWithUrl - Provided deeplink scheme is not equal to the target"
                            + " scheme");
            return;
        }

        // if the deepLink scheme path represents cancel,
        final String host = url.getHost();

        if (TargetConstants.PreviewKeys.DEEPLINK_SCHEME_PATH_CANCEL.equals(host)) {
            resetTargetPreviewProperties();

        } else if (TargetConstants.PreviewKeys.DEEPLINK_SCHEME_PATH_CONFIRM.equals(host)) {
            String query = url.getRawQuery();

            final Map<String, String> queryParams = TargetUtils.extractQueryParameters(query);

            if (TargetUtils.isNullOrEmpty(queryParams)) {
                Log.warning(
                        TargetConstants.LOG_TAG,
                        CLASS_NAME,
                        String.format(
                                "previewConfirmedWithUrl - Target Preview URL does not have preview"
                                        + " query parameter : URL : %s",
                                query));
                return;
            }

            final String previewParameters =
                    queryParams.get(TargetConstants.PreviewKeys.PREVIEW_PARAMETERS);

            try {
                if (!StringUtils.isNullOrEmpty(previewParameters)) {
                    this.previewParams = URLDecoder.decode(previewParameters, "UTF-8");
                }

            } catch (final UnsupportedEncodingException e) {
                Log.error(
                        TargetConstants.LOG_TAG,
                        CLASS_NAME,
                        "Unable to URL decode the preview parameters, Error %s",
                        e);
            }

            // open the restart url if it exists
            if (StringUtils.isNullOrEmpty(restartUrl)) {
                Log.debug(
                        TargetConstants.LOG_TAG,
                        CLASS_NAME,
                        "previewConfirmedWithUrl - Empty Preview restart url");
            } else if (!uriService.openUri(restartUrl)) {
                Log.debug(
                        TargetConstants.LOG_TAG,
                        CLASS_NAME,
                        "previewConfirmedWithUrl - Failed to load given preview restart url %s",
                        restartUrl);
            }
        }
    }

    /**
     * This method initiates a new async request to fetch the web view for the target preview.
     *
     * <p>If the connection is successful and a valid response is received, a {@code
     * UIService.UIFullScreenMessage} will be created and displayed. This method will be called on
     * preview deepLink or preview button tap detection. This method will not initiate a web view
     * fetch request, if there is a request already in process.
     */
    protected void fetchWebView() {

        // Do not proceed if fetching is already in progress.
        if (fetchingWebView) {
            Log.debug(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "fetchWebView - TargetPreview was already initialized. Fetching webView in"
                            + " progress.");
            return;
        }

        fetchingWebView = true;
        final String targetUrl = getUrlForTargetPreviewRequest();
        Log.debug(
                TargetConstants.LOG_TAG,
                CLASS_NAME,
                "fetchWebView - Sending preview request to url %s",
                targetUrl);

        // prepare the request headers
        final Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(
                NetworkingConstants.Headers.ACCEPT,
                NetworkingConstants.HeaderValues.ACCEPT_TEXT_HTML);
        requestHeaders.put(
                NetworkingConstants.Headers.CONTENT_TYPE,
                NetworkingConstants.HeaderValues.CONTENT_TYPE_URL_ENCODED);

        final NetworkRequest request =
                new NetworkRequest(
                        targetUrl,
                        HttpMethod.GET,
                        null,
                        requestHeaders,
                        TargetConstants.DEFAULT_NETWORK_TIMEOUT,
                        TargetConstants.DEFAULT_NETWORK_TIMEOUT);
        networkService.connectAsync(
                request,
                connection -> {
                    if (connection == null) {
                        Log.error(
                                TargetConstants.LOG_TAG,
                                CLASS_NAME,
                                "Target Preview unable to open connect to fetch webview");
                        fetchingWebView = false;
                        return;
                    }

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        final String serverResponse =
                                StreamUtils.readAsString(connection.getInputStream());
                        if (!StringUtils.isNullOrEmpty(serverResponse)) {
                            webViewHtml = serverResponse;
                            Log.debug(
                                    TargetConstants.LOG_TAG,
                                    CLASS_NAME,
                                    "Successfully fetched webview for preview mode, response body"
                                            + " %s",
                                    webViewHtml);
                            createAndShowMessage();
                        }
                    } else {
                        Log.error(
                                TargetConstants.LOG_TAG,
                                CLASS_NAME,
                                String.format(
                                        "Failed to fetch preview webview with connection status %s,"
                                                + " response body %s",
                                        connection.getResponseCode(),
                                        connection.getResponseMessage()));
                    }

                    connection.close();
                    fetchingWebView = false;
                });
    }

    /** Called when the preview message is closed, it resets all the target preview properties. */
    protected void resetTargetPreviewProperties() {
        token = null;
        webViewHtml = null;
        endPoint = null;
        restartUrl = null;
        previewParams = null;

        // remove preview button
        if (floatingButtonPresentable != null) {
            floatingButtonPresentable.dismiss();
            floatingButtonPresentable = null;
        }
    }

    // ========================================================================================
    // private methods
    // ========================================================================================

    /**
     * Extracts token and endPoint parameters if present in the deepLink query params {@code Map}
     * and sets them in local variables for later user.
     *
     * <p>Tries to read the preview endpoint from the preview parameters, if unavailable assign
     * {@link #endPoint} with the default endpoint. Tries to read the preview token from the preview
     * parameters, if unavailable returns false.
     *
     * @param previewParameters {@link Map} representing preview parameters
     * @return {@code boolean} representing the success of reading the preview token from the
     *     previewParameters
     */
    private boolean setupTargetPreviewParameters(final Map<String, String> previewParameters) {
        // read and set endpoint
        try {
            final String previewEndPoint =
                    previewParameters.get(TargetConstants.PreviewKeys.PREVIEW_ENDPOINT);

            if (!StringUtils.isNullOrEmpty(previewEndPoint)) {
                this.endPoint = URLDecoder.decode(previewEndPoint, CHARSET_UTF_8);
            } else {
                Log.debug(
                        TargetConstants.LOG_TAG,
                        CLASS_NAME,
                        "setupTargetPreviewParameters - Using the Default endpoint");
                this.endPoint = TargetConstants.PreviewKeys.DEFAULT_TARGET_PREVIEW_ENDPOINT;
            }

        } catch (UnsupportedEncodingException e) {
            Log.debug(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "Decode error while extracting preview endpoint, Error %s",
                    e);
        }

        // read and set preview token
        try {
            final String encodedToken =
                    previewParameters.get(TargetConstants.PreviewKeys.PREVIEW_TOKEN);

            if (!StringUtils.isNullOrEmpty(encodedToken)) {
                token = URLDecoder.decode(encodedToken, CHARSET_UTF_8);
                return true;
            }

        } catch (final UnsupportedEncodingException e) {
            Log.debug(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "Decode error while extracting preview token, Error %s",
                    e);
        }

        return false;
    }

    /**
     * Creates and displays a {@code UIService.FloatingButton}.
     *
     * <p>Also attaches a {@code TargetPreviewButtonListener} to the {@code #floatingButton} to
     * listen to the events on that {@code #floatingButton}. Doesn't create a {@code
     * UIService.FloatingButton} instance, if the {@code UIService} is unavailable.
     */
    private void createAndShowFloatingButton() {
        if (floatingButtonPresentable != null) {
            Log.debug(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "createAndShowFloatingButton - Floating button already exists");
            return;
        }

        final AssetManager assetManager = context.getAssets();
        if (assetManager == null) {
            Log.debug(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "createAndShowFloatingButton - Unable to instantiate the floating button for"
                            + " target preview, cannot find button image");
            return;
        }

        try {
            final String backgroundImageString =
                    StreamUtils.readAsString(
                            assetManager.open(TargetConstants.PREVIEW_FLOATING_BUTTON_ASSET_FILE));
            final byte[] backgroundImage = Base64.decode(backgroundImageString, Base64.DEFAULT);
            final Bitmap floatingButtonImage =
                    BitmapFactory.decodeStream(new ByteArrayInputStream(backgroundImage));

            floatingButtonPresentable =
                    uiService.create(
                            new FloatingButton(
                                    new FloatingButtonSettings.Builder()
                                            .initialGraphic(floatingButtonImage)
                                            .build(),
                                    new TargetPreviewButtonEventListener(this)),
                            new DefaultPresentationUtilityProvider());
            floatingButtonPresentable.show();
        } catch (final Exception e) {
            Log.debug(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "createAndShowFloatingButton - Unable to instantiate the floating button for"
                            + " target preview %s",
                    e.getMessage());
        }
    }

    /**
     * Creates and displays a {@code UIService.UIFullScreenMessage}.
     *
     * <p>Also attaches a {@code TargetPreviewFullscreenDelegate} to the {@code
     * UIService.UIFullScreenMessage} to listen to the button clicks on that message. Doesn't create
     * a {@code UIService.UIFullScreenMessage} instance, if the {@link UIService} is unavailable or
     * if there is another {@code UIService.UIFullScreenMessage} already being displayed.
     */
    private void createAndShowMessage() {
        // Target fullscreen messages are displayed at 100% scale
        final InAppMessageSettings inAppMessageSettings =
                new InAppMessageSettings.Builder()
                        .content(webViewHtml)
                        .backgroundColor("#FFFFFFF")
                        .backdropOpacity(1)
                        .build();

        try {
            final Presentable<InAppMessage> inAppMessagePresentable =
                    uiService.create(
                            new InAppMessage(
                                    inAppMessageSettings,
                                    new TargetPreviewFullscreenEventListener(this)),
                            new DefaultPresentationUtilityProvider());
            inAppMessagePresentable.show();
        } catch (final Exception e) {
            Log.debug(
                    TargetConstants.LOG_TAG,
                    CLASS_NAME,
                    "createAndShowMessage - Unable to instantiate the full screen message for"
                            + " target preview %s",
                    e.getMessage());
        }
    }

    /**
     * Creates a url for the target preview request.
     *
     * @return {@link String} the preview request url with {@link #endPoint}, {@link #clientCode}
     *     and {@link #token}
     */
    private String getUrlForTargetPreviewRequest() {
        return new URLBuilder()
                .enableSSL(true)
                .setServer(endPoint)
                .addPath("ui")
                .addPath("admin")
                .addPath(clientCode)
                .addPath("preview")
                .addQueryParameter("token", token)
                .build();
    }
}
