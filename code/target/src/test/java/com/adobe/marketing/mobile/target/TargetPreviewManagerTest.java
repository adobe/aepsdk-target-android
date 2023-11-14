/*
 Copyright 2023 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.target;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ui.FloatingButton;
import com.adobe.marketing.mobile.services.ui.InAppMessage;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.PresentationUtilityProvider;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.uri.UriOpening;
import com.adobe.marketing.mobile.util.StreamUtils;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TargetPreviewManagerTest  {

	private static String PREVIEW_PARAMETERS   = "at_preview_params";
	private static String PREVIEW_TOKEN        = "at_preview_token";
	private static String PREVIEW_ENDPOINT     = "at_preview_endpoint";
	private static String PREVIEW_DEFAULT_EP   = "hal.testandtarget.omniture.com";
	private static String QA_MODE_NODE         = "qaMode";
	private static String CLIENT_CODE          = "test_client_code";


	String ENCODED_PREVIEW_PARAMS =
			"%7B%22qaMode%22%3A%7B%0D%0A%22token%22%3A%22abcd%22%2C%0D%0A%22bypassEntryAudience%22%3Atrue%2C%0D%0A%22listedActivitiesOnly%22%3Atrue%2C%0D%0A%22evaluateAsTrueAudienceIds%22%3A%5B%22audienceId1%22%2C%22audienceId2%22%5D%2C%0D%0A%22evaluateAsFalseAudienceIds%22%3A%5B%22audienceId3%22%2C%22audienceId4%22%5D%2C%0D%0A%22previewIndexes%22%3A%5B%0D%0A%7B%0D%0A%22activityIndex%22%3A1%2C%0D%0A%22experienceIndex%22%3A1%0D%0A%7D%5D%7D%7D";

	String JSON_PREVIEW_PARAMS = "{\"qaMode\":{\"token\":\"abcd\","
			+ "\"bypassEntryAudience\":true,"
			+ "\"listedActivitiesOnly\":true,"
			+ "\"evaluateAsTrueAudienceIds\":[\"audienceId1\",\"audienceId2\"],"
			+ "\"evaluateAsFalseAudienceIds\":[\"audienceId3\",\"audienceId4\"],"
			+ "\"previewIndexes\":["
			+ "{"
			+ "\"activityIndex\":1,"
			+ "\"experienceIndex\":1"
			+ "}"
			+ "]"
			+ "}"
			+ "}";

	String TEST_QUERY_PARAMS       = "at_preview_params=" + ENCODED_PREVIEW_PARAMS + "&extraKey=extraValue";
	String TEST_CONFIRM_DEEPLINK   = "adbinapp://confirm?" + TEST_QUERY_PARAMS;
	String TEST_RESTART_URL        = "adbinapp://somepage";

	@Mock
	private Networking networkService;

	@Mock
	private UIService uiService;

    @Mock
    private UriOpening uriService;

	@Mock
	private Context context;

	@Mock
	private AssetManager assetManager;

	@Mock
	private Bitmap floatingButtonImage;

	@Mock
	private Presentable<FloatingButton> floatingButton;

	@Mock
	private Presentable<InAppMessage> fullscreenMessage;

	@Mock
	private HttpConnecting connecting;

	@Mock
	private TargetPreviewManager previewManager;

	@Before()
	public void beforeEach() {
		mockFullScreenAndFloatingButton();
		previewManager = new TargetPreviewManager(networkService, uiService, uriService, context);
	}

	void runUsingMockedServiceProvider(final Runnable runnable) {
		try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class);
			 MockedStatic<BitmapFactory> bitmapFactoryMockedStatic = Mockito.mockStatic(BitmapFactory.class);
			 MockedStatic<StreamUtils> streamUtilsMockedStatic = Mockito.mockStatic(StreamUtils.class)) {
			base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
					.thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));
			bitmapFactoryMockedStatic.when(() -> BitmapFactory.decodeStream(any(InputStream.class)))
					.thenReturn(floatingButtonImage);
			streamUtilsMockedStatic.when(() -> StreamUtils.readAsString(any()))
					.thenReturn(TargetTestConstants.ENCODED_BUTTON_BACKGROUND_PNG);
			runnable.run();
		}
	}

	void mockFullScreenAndFloatingButton() {
        when(uiService.create(any(FloatingButton.class), any(PresentationUtilityProvider.class))).thenReturn(floatingButton);
        when(uiService.create(any(InAppMessage.class), any(PresentationUtilityProvider.class))).thenReturn(fullscreenMessage);
		when(context.getAssets()).thenReturn(assetManager);
	}

	void setMockConnectionResponse(final String response, final int responseCode) {
		when(connecting.getResponseCode()).thenReturn(responseCode);
		when(connecting.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
		doAnswer(invocation -> {
			((NetworkCallback) invocation.getArguments()[1]).call(connecting);
			return null;
		}).when(networkService).connectAsync(any(), any());
	}

	// ===================================
	// Test enterPreviewModeWithDeepLinkParams
	// ===================================
	@Test
	public void test_enterPreviewModeWithDeepLinkParams_ValidDeepLink() {
		runUsingMockedServiceProvider(() -> {
			// setup
			ArgumentCaptor<NetworkRequest> networkResponseCapture = ArgumentCaptor.forClass(NetworkRequest.class);
			String testDeeplink = "test://path?at_preview_token=abcd&key1=val1";
			String expectedUrl = "https://" + PREVIEW_DEFAULT_EP + "/ui/admin/" + CLIENT_CODE + "/preview?token=abcd";
			setMockConnectionResponse("TestHTML", 200);

			// test
			previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

			// verify floating button created and displayed
			verifyFloatingButtonDisplayed();

			// verify network call
			verify(networkService, times(1)).connectAsync(networkResponseCapture.capture(), any());
			assertEquals("network request has the correct httpcommand", HttpMethod.GET,
					networkResponseCapture.getValue().getMethod());
			assertEquals("network request has the correct read timeout", 2,
					networkResponseCapture.getValue().getReadTimeout());
			assertEquals("network request has the correct URL",
					expectedUrl, networkResponseCapture.getValue().getUrl());
			assertEquals("network request has the correct request property", 2, networkResponseCapture.getValue().getHeaders().size());
			assertEquals("network request has the correct request property", "text/html", networkResponseCapture.getValue().getHeaders().get("Accept"));
			assertEquals("network request has the correct request property", "application/x-www-form-urlencoded", networkResponseCapture.getValue().getHeaders().get("Content-Type"));


			// verify fullscreen message created and displayed
			verifyFullScreenMessageDisplayed();

			// verify local variables
			assertEquals("abcd", previewManager.getPreviewToken());
		});
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_NullDeepLink() {
		runUsingMockedServiceProvider(() -> {
			// test
			previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, null);

			// verify no FullScreenMessage or floating button is created
			verifyNoInteractions(uiService);

			// verify no network call
			verifyNoInteractions(networkService);

			// verify local variables
			assertNull(previewManager.getPreviewToken());
		});
	}


	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_EmptyDeepLink() {
		runUsingMockedServiceProvider(() -> {
			// test
			previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, "");

			// verify no fullScreenMessage or floating button is created
			verifyNoInteractions(uiService);

			// verify no network call
			verifyNoInteractions(networkService);

			// verify local variables
			assertNull(previewManager.getPreviewToken());
		});
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_ValidDeepLink_Without_Double_Decoding() {
		runUsingMockedServiceProvider(() -> {
			// Validate fix for AMSDK-8272 Preview Link getting re-encoded from %2B to %20
			// setup
			ArgumentCaptor<NetworkRequest> networkResponseCapture = ArgumentCaptor.forClass(NetworkRequest.class);
			String testDeeplink = "test://path?at_preview_token=olDho0%2Blkfr8LaHURjiCAnu1xIVRkvN3UqdXwawRz3E%3D&key1=val1";
			String expectedUrl = "https://" + PREVIEW_DEFAULT_EP + "/ui/admin/" + CLIENT_CODE +
					"/preview?token=olDho0%2Blkfr8LaHURjiCAnu1xIVRkvN3UqdXwawRz3E%3D";
			setMockConnectionResponse("TestHTML", 200);

			// test
			previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

			// verify floating button
			verifyFloatingButtonDisplayed();

			// verify network call
			verify(networkService, times(1)).connectAsync(networkResponseCapture.capture(), any());
			assertEquals("network request has the correct URL",
					expectedUrl, networkResponseCapture.getValue().getUrl());

			// verify fullscreen message created and displayed
			verifyFullScreenMessageDisplayed();

			// verify local variables
			assertEquals("olDho0+lkfr8LaHURjiCAnu1xIVRkvN3UqdXwawRz3E=", previewManager.getPreviewToken());
		});
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_DeepLinkMissingToken() {
		runUsingMockedServiceProvider(() -> {
			// setup
			String testDeeplink = "test://path?somethingelse=abcd&key1=val1";

			// test
			previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

			// verify no fullScreenMessage or floating button is created
			verifyNoInteractions(uiService);

			// verify no network call
			verifyNoInteractions(networkService);

			// verify no deeplink opened
			verifyNoInteractions(uriService);

			// verify local variables
			assertNull(previewManager.getPreviewToken());
		});
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_InValidDeeplink() {
		runUsingMockedServiceProvider(() -> {
			// setup
			String testDeeplink = "NotAValidDeeplink";

			// test
			previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

			// verify no fullScreenMessage or floating button is created
			verifyNoInteractions(uiService);

			// verify no network call
			verifyNoInteractions(networkService);

			// verify no deeplink opened
			verifyNoInteractions(uriService);

			// verify local variables
			assertNull(previewManager.getPreviewToken());
		});
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_NullUIService() {
		runUsingMockedServiceProvider(() -> {
			// setup
			String testDeeplink = "test://path?at_preview_token=abcd&key1=val1";
			previewManager = new TargetPreviewManager(networkService, null, uriService, context);

			// test
			previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

			// verify no fullScreenMessage or floating button is created
			verifyNoInteractions(uiService);

			// verify no network call
			verifyNoInteractions(networkService);

			// verify no deeplink opened
			verifyNoInteractions(uriService);

			// verify local variables
			assertNull(previewManager.getPreviewToken());
		});
	}

    @Test
    public void test_EnterPreviewModeWithDeepLinkParams_NullUriService() {
		runUsingMockedServiceProvider(() -> {
			// setup
			String testDeeplink = "test://path?at_preview_token=abcd&key1=val1";
			previewManager = new TargetPreviewManager(networkService, uiService, null, context);

			// test
			previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

			// verify no fullScreenMessage or floating button is created
			verifyNoInteractions(uiService);

			// verify no network call
			verifyNoInteractions(networkService);

			// verify no deeplink opened
			verifyNoInteractions(uriService);

			// verify local variables
			assertNull(previewManager.getPreviewToken());
		});
    }

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_NullNetworkService() {
		runUsingMockedServiceProvider(() -> {
			// setup
			String testDeeplink = "test://path?at_preview_token=abcd&key1=val1";
			previewManager = new TargetPreviewManager(null, uiService, uriService, context);

			// test
			previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

			// verify no fullScreenMessage or floating button is created
			verifyNoInteractions(uiService);

			// verify no network call
			verifyNoInteractions(networkService);

			// verify no deeplink opened
			verifyNoInteractions(uriService);

			// verify local variables
			assertNull(previewManager.getPreviewToken());
		});
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_DifferentEndpoint() {
		runUsingMockedServiceProvider(() -> {
			// setup
			ArgumentCaptor<NetworkRequest> networkResponseCapture = ArgumentCaptor.forClass(NetworkRequest.class);
			String testDeeplink = "test://path?at_preview_token=abcd&key1=val1&" + PREVIEW_ENDPOINT + "=newEndPoint";
			String expectedUrl = "https://newEndPoint/ui/admin/" + CLIENT_CODE + "/preview?token=abcd";
			setMockConnectionResponse("TestHTML", 200);

			// test
			previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

			// verify floating button
			verifyFloatingButtonDisplayed();

			// verify message displayed
			verifyFullScreenMessageDisplayed();

			// verify network call
			verify(networkService, times(1)).connectAsync(networkResponseCapture.capture(), any());
			assertEquals("network request has the correct URL",
					expectedUrl, networkResponseCapture.getValue().getUrl());
		});
	}

	// ===================================
	// Test resetTargetPreviewProperties
	// ===================================
	@Test
	public void test_resetTargetPreviewProperties() {
		runUsingMockedServiceProvider(() -> {
			// setup
			previewManager.token = "token";
			previewManager.webViewHtml = "html";
			previewManager.previewParams = "params";
			previewManager.endPoint = "endPoint";
			previewManager.restartUrl = "restartURL";
			previewManager.floatingButtonPresentable = floatingButton;

			// test
			previewManager.resetTargetPreviewProperties();

			//verify
			assertNull(previewManager.token);
			assertNull(previewManager.webViewHtml);
			assertNull(previewManager.previewParams);
			assertNull(previewManager.endPoint);
			assertNull(previewManager.restartUrl);
			assertNull(previewManager.floatingButtonPresentable);
			verify(floatingButton).dismiss();
		});
	}

	// ===================================
	// Test previewConfirmedWithUrl
	// ===================================
	@Test
	public void test_previewConfirmedWithUrl_When_Confirm() {
		runUsingMockedServiceProvider(() -> {
			// setup
			ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
			previewManager.floatingButtonPresentable = floatingButton;

			// test
			previewManager.previewConfirmedWithUrl(fullscreenMessage, TEST_CONFIRM_DEEPLINK);

			//verify
			verify(fullscreenMessage).dismiss();
			assertEquals(JSON_PREVIEW_PARAMS, previewManager.getPreviewParameters().replaceAll("\\s+", ""));
		});
	}

	@Test
	public void test_previewConfirmedWithUrl_When_Cancel() {
		runUsingMockedServiceProvider(() -> {
			// setup
			ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
			previewManager.floatingButtonPresentable = floatingButton;

			// test
			previewManager.previewConfirmedWithUrl(fullscreenMessage, "adbinapp://cancel");

			//verify
			verify(fullscreenMessage).dismiss();
			assertNull(previewManager.getPreviewParameters());
		});
	}

	@Test
	public void test_previewConfirmedWithUrl_ConfirmPreviewParamsEmpty() {
		runUsingMockedServiceProvider(() -> {
			// setup
			previewManager.floatingButtonPresentable = floatingButton;

			// test
			previewManager.previewConfirmedWithUrl(fullscreenMessage, "adbinapp://confirm?abc=def");

			//verify
			verify(fullscreenMessage).dismiss();
			assertNull(previewManager.getPreviewParameters());
		});
	}

	@Test
	public void test_previewConfirmedWithUrl_InvalidURI() {
		runUsingMockedServiceProvider(() -> {
			// setup
			previewManager.floatingButtonPresentable = floatingButton;

			// test
			previewManager.previewConfirmedWithUrl(fullscreenMessage, "Invalid");

			//verify
			verify(fullscreenMessage).dismiss();
			assertNull(previewManager.getPreviewParameters());
		});
	}

	@Test
	public void test_previewConfirmedWithUrl_InvalidScheme() {
		runUsingMockedServiceProvider(() -> {
			// setup
			previewManager.floatingButtonPresentable = floatingButton;

			// test
			previewManager.previewConfirmedWithUrl(fullscreenMessage, "notAppScheme://confirm?abc=def");

			//verify
			verify(fullscreenMessage).dismiss();
			assertNull(previewManager.getPreviewParameters());
		});
	}

	@Test
	public void test_previewConfirmedWithUrl_When_RestartDeepLink() {
		runUsingMockedServiceProvider(() -> {
			// setup
			previewManager.floatingButtonPresentable = floatingButton;
			previewManager.restartUrl = "restartURL";

			// test
			previewManager.previewConfirmedWithUrl(fullscreenMessage, TEST_CONFIRM_DEEPLINK);

			//verify
			verify(fullscreenMessage).dismiss();
			verify(uriService).openUri("restartURL");
		});
	}

	@Test
	public void test_previewConfirmedWithUrl_When_RestartDeepLinkUnAvailable() {
		runUsingMockedServiceProvider(() -> {

			// setup
			previewManager.floatingButtonPresentable = floatingButton;
			previewManager.restartUrl = null;

			// test
			previewManager.previewConfirmedWithUrl(fullscreenMessage, TEST_CONFIRM_DEEPLINK);

			//verify
			verify(fullscreenMessage).dismiss();
			verify(uriService, times(0)).openUri(any(String.class));
		});
	}

	@Test
	public void test_previewConfirmedWithUrl_ConfirmPreviewParamsEmpty_Should_Not_Dispatch_TargetResponseEvent() {
		runUsingMockedServiceProvider(() -> {
			// setup
			previewManager.floatingButtonPresentable = floatingButton;

			// test
			previewManager.previewConfirmedWithUrl(fullscreenMessage, "adbinapp://confirm?");

			//verify
			verify(fullscreenMessage).dismiss();
			assertNull(previewManager.getPreviewParameters());
		});
	}


	// ===================================
	// Test fetchWebView
	// ===================================
	@Test
	public void test_fetchWebView_Works() {
		runUsingMockedServiceProvider(() -> {
			// setup
			ArgumentCaptor<NetworkRequest> networkResponseCapture = ArgumentCaptor.forClass(NetworkRequest.class);
			previewManager.endPoint = "someEndpoint";
			setMockConnectionResponse("TestHTML", 200);

			// test
			previewManager.fetchWebView();

			// verify network call
			verify(networkService, times(1)).connectAsync(networkResponseCapture.capture(), any());
			assertEquals("network request has the correct httpcommand", HttpMethod.GET,
					networkResponseCapture.getValue().getMethod());
			assertEquals("network request has the correct read timeout", 2,
					networkResponseCapture.getValue().getReadTimeout());
			assertEquals("network request has the correct URL",
					"https://someEndpoint/ui/admin/preview", networkResponseCapture.getValue().getUrl());
			assertEquals("network request has the correct request property", 2, networkResponseCapture.getValue().getHeaders().size());
			assertEquals("network request has the correct request property", "text/html", networkResponseCapture.getValue().getHeaders().get("Accept"));
			assertEquals("network request has the correct request property", "application/x-www-form-urlencoded", networkResponseCapture.getValue().getHeaders().get("Content-Type"));


			// verify fullscreen message created and displayed
			verifyFullScreenMessageDisplayed();
		});
	}

	@Test
	public void test_fetchWebView_EmptyServerResponse() {
		try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class);
			 MockedStatic<BitmapFactory> bitmapFactoryMockedStatic = Mockito.mockStatic(BitmapFactory.class);
			 MockedStatic<StreamUtils> streamUtilsMockedStatic = Mockito.mockStatic(StreamUtils.class)) {
			base64MockedStatic.when(() -> Base64.decode(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
					.thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));
			bitmapFactoryMockedStatic.when(() -> BitmapFactory.decodeStream(any(InputStream.class)))
					.thenReturn(floatingButtonImage);
			streamUtilsMockedStatic.when(() -> StreamUtils.readAsString(any())).thenReturn(null);
			// setup
			ArgumentCaptor<NetworkRequest> networkResponseCapture = ArgumentCaptor.forClass(NetworkRequest.class);
			previewManager.endPoint = "someEndpoint";
			setMockConnectionResponse("", 200);

			// test
			previewManager.fetchWebView();

			// verify network call
			verify(networkService, times(1)).connectAsync(networkResponseCapture.capture(), any());
			assertEquals("network request has the correct URL",
					"https://someEndpoint/ui/admin/preview", networkResponseCapture.getValue().getUrl());

			// verify message not displayed
			verifyNoInteractions(uiService);
		}
	}

	@Test
	public void test_fetchWebView_WhenConnectionNull() {
		runUsingMockedServiceProvider(() -> {
			// setup
			ArgumentCaptor<NetworkRequest> networkResponseCapture = ArgumentCaptor.forClass(NetworkRequest.class);
			previewManager.endPoint = "someEndpoint";
			doAnswer(invocation -> {
				((NetworkCallback) invocation.getArguments()[1]).call(null);
				return null;
			}).when(networkService).connectAsync(any(), any());

			// test
			previewManager.fetchWebView();

			// verify network call
			verify(networkService, times(1)).connectAsync(networkResponseCapture.capture(), any());
			assertEquals("network request has the correct URL",
					"https://someEndpoint/ui/admin/preview", networkResponseCapture.getValue().getUrl());

			// verify message not displayed
			verifyNoInteractions(uiService);
		});
	}

	@Test
	public void test_fetchWebView_ResponseCodeNot200() {
		runUsingMockedServiceProvider(() -> {
			// setup
			ArgumentCaptor<NetworkRequest> networkResponseCapture = ArgumentCaptor.forClass(NetworkRequest.class);
			previewManager.endPoint = "someEndpoint";
			setMockConnectionResponse("TestHTML", 202);

			// test
			previewManager.fetchWebView();

			// verify network call
			verify(networkService, times(1)).connectAsync(networkResponseCapture.capture(), any());
			assertEquals("network request has the correct URL",
					"https://someEndpoint/ui/admin/preview", networkResponseCapture.getValue().getUrl());

			// verify message not displayed
			verifyNoInteractions(uiService);
		});
	}

	// ===================================
	// Test setRestartDeepLink
	// ===================================
	@Test
	public void test_setRestartDeepLink() {
		runUsingMockedServiceProvider(() -> {
			// test
			previewManager.setRestartDeepLink("restartURL");

			//verify
			assertEquals("restartURL", previewManager.restartUrl);
		});
	}

	@Test
	public void test_setRestartDeepLink_Null() {
		runUsingMockedServiceProvider(() -> {
			// test
			previewManager.setRestartDeepLink(null);

			//verify
			assertNull(previewManager.restartUrl);
		});
	}

	// ===================================
	// Test getPreviewParameters
	// ===================================
	@Test
	public void test_getPreviewParameters() {
		runUsingMockedServiceProvider(() -> {
			// setup
			previewManager.previewParams = "params";

			// test
			assertEquals("params", previewManager.getPreviewParameters());
		});
	}


	private void verifyFullScreenMessageDisplayed() {
		verify(uiService,times(1)).create(any(InAppMessage.class), any(PresentationUtilityProvider.class));
		verify(fullscreenMessage).show();
	}

	private void verifyFloatingButtonDisplayed() {
		verify(uiService,times(1)).create(any(FloatingButton.class), any(PresentationUtilityProvider.class));
		verify(floatingButton).show();
	}

}