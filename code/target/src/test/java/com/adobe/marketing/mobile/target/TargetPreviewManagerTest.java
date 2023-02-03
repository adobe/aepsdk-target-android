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
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ui.FloatingButton;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;

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
	private FloatingButton floatingButton;

	@Mock
	private FullscreenMessage fullscreenMessage;

	@Mock
	private TargetPreviewFullscreenDelegate fullscreenDelegate;

	@Mock
	private HttpConnecting connecting;

	@Mock
	private TargetPreviewManager previewManager;

	private MockedConstruction<TargetPreviewButtonListener> buttonListenerMockedConstruction;

	@Before()
	public void beforeEach() {
		mockFullScreenAndFloatingButton();
		previewManager = new TargetPreviewManager(networkService, uiService);
	}


	void mockFullScreenAndFloatingButton() {
		when(uiService.createFloatingButton(any(TargetPreviewButtonListener.class))).thenReturn(floatingButton);
		when(uiService.createFullscreenMessage(any(String.class), any(TargetPreviewFullscreenDelegate.class), any(Boolean.class), any(MessageSettings.class))).thenReturn(fullscreenMessage);
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
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_NullDeepLink() {
		// test
		previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, null);

		// verify no FullScreenMessage or floating button is created
		verifyNoInteractions(uiService);

		// verify no network call
		verifyNoInteractions(networkService);

		// verify local variables
		assertNull(previewManager.getPreviewToken());
	}


	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_EmptyDeepLink() {
		// test
		previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, "");

		// verify no fullScreenMessage or floating button is created
		verifyNoInteractions(uiService);

		// verify no network call
		verifyNoInteractions(networkService);

		// verify local variables
		assertNull(previewManager.getPreviewToken());
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_ValidDeepLink_Without_Double_Decoding() {
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
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_DeepLinkMissingToken() {
		// setup
		String testDeeplink = "test://path?somethingelse=abcd&key1=val1";

		// test
		previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

		// verify no fullScreenMessage or floating button is created
		verifyNoInteractions(uiService);

		// verify no network call
		verifyNoInteractions(networkService);

		// verify local variables
		assertNull(previewManager.getPreviewToken());
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_InValidDeeplink() {
		// setup
		String testDeeplink = "NotAValidDeeplink";

		// test
		previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

		// verify no fullScreenMessage or floating button is created
		verifyNoInteractions(uiService);

		// verify no network call
		verifyNoInteractions(networkService);

		// verify local variables
		assertNull(previewManager.getPreviewToken());
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_NullUIService() {
		// setup
		String testDeeplink = "test://path?at_preview_token=abcd&key1=val1";
		previewManager = new TargetPreviewManager(networkService, null);

		// test
		previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

		// verify no fullScreenMessage or floating button is created
		verifyNoInteractions(uiService);

		// verify no network call
		verifyNoInteractions(networkService);

		// verify local variables
		assertNull(previewManager.getPreviewToken());
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_NullNetworkService() {
		// setup
		String testDeeplink = "test://path?at_preview_token=abcd&key1=val1";
		previewManager = new TargetPreviewManager(null, uiService);

		// test
		previewManager.enterPreviewModeWithDeepLinkParams(CLIENT_CODE, testDeeplink);

		// verify no fullScreenMessage or floating button is created
		verifyNoInteractions(uiService);

		// verify no network call
		verifyNoInteractions(networkService);

		// verify local variables
		assertNull(previewManager.getPreviewToken());
	}

	@Test
	public void test_EnterPreviewModeWithDeepLinkParams_DifferentEndpoint() {
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
	}

	// ===================================
	// Test resetTargetPreviewProperties
	// ===================================
	@Test
	public void test_resetTargetPreviewProperties() {
		// setup
		previewManager.token = "token";
		previewManager.webViewHtml = "html";
		previewManager.previewParams = "params";
		previewManager.endPoint = "endPoint";
		previewManager.restartUrl = "restartURL";
		previewManager.floatingButton = floatingButton;

		// test
		previewManager.resetTargetPreviewProperties();

		//verify
		assertNull(previewManager.token);
		assertNull(previewManager.webViewHtml);
		assertNull(previewManager.previewParams);
		assertNull(previewManager.endPoint);
		assertNull(previewManager.restartUrl);
		assertNull(previewManager.floatingButton);
		verify(floatingButton).remove();
	}

	// ===================================
	// Test previewConfirmedWithUrl
	// ===================================
	@Test
	public void test_previewConfirmedWithUrl_When_Confirm() {
		// setup
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		previewManager.floatingButton = floatingButton;

		// test
		previewManager.previewConfirmedWithUrl(fullscreenMessage, TEST_CONFIRM_DEEPLINK);

		//verify
		verify(fullscreenMessage).dismiss();
		assertEquals(JSON_PREVIEW_PARAMS, previewManager.getPreviewParameters().replaceAll("\\s+", ""));
	}

	@Test
	public void test_previewConfirmedWithUrl_When_Cancel() {
		// setup
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		previewManager.floatingButton = floatingButton;

		// test
		previewManager.previewConfirmedWithUrl(fullscreenMessage, "adbinapp://cancel");

		//verify
		verify(fullscreenMessage).dismiss();
		assertNull(previewManager.getPreviewParameters());
	}

	@Test
	public void test_previewConfirmedWithUrl_ConfirmPreviewParamsEmpty() {
		// setup
		previewManager.floatingButton = floatingButton;

		// test
		previewManager.previewConfirmedWithUrl(fullscreenMessage, "adbinapp://confirm?abc=def");

		//verify
		verify(fullscreenMessage).dismiss();
		assertNull(previewManager.getPreviewParameters());
	}

	@Test
	public void test_previewConfirmedWithUrl_InvalidURI() {
		// setup
		previewManager.floatingButton = floatingButton;

		// test
		previewManager.previewConfirmedWithUrl(fullscreenMessage, "Invalid");

		//verify
		verify(fullscreenMessage).dismiss();
		assertNull(previewManager.getPreviewParameters());
	}

	@Test
	public void test_previewConfirmedWithUrl_InvalidScheme() {
		// setup
		previewManager.floatingButton = floatingButton;

		// test
		previewManager.previewConfirmedWithUrl(fullscreenMessage, "notAppScheme://confirm?abc=def");

		//verify
		verify(fullscreenMessage).dismiss();
		assertNull(previewManager.getPreviewParameters());
	}

	@Test
	public void test_previewConfirmedWithUrl_When_RestartDeepLink() {
		// setup
		previewManager.floatingButton = floatingButton;
		previewManager.restartUrl = "restartURL";

		// test
		previewManager.previewConfirmedWithUrl(fullscreenMessage, TEST_CONFIRM_DEEPLINK);

		//verify
		verify(fullscreenMessage).dismiss();
		verify(uiService).showUrl("restartURL");
	}

	@Test
	public void test_previewConfirmedWithUrl_When_RestartDeepLinkUnAvailable() {
		// setup
		previewManager.floatingButton = floatingButton;
		previewManager.restartUrl = null;

		// test
		previewManager.previewConfirmedWithUrl(fullscreenMessage, TEST_CONFIRM_DEEPLINK);

		//verify
		verify(fullscreenMessage).dismiss();
		verify(uiService, times(0)).showUrl(any(String.class));
	}

	@Test
	public void test_previewConfirmedWithUrl_ConfirmPreviewParamsEmpty_Should_Not_Dispatch_TargetResponseEvent() {
		// setup
		previewManager.floatingButton = floatingButton;

		// test
		previewManager.previewConfirmedWithUrl(fullscreenMessage, "adbinapp://confirm?");

		//verify
		verify(fullscreenMessage).dismiss();
		assertNull(previewManager.getPreviewParameters());
	}


	// ===================================
	// Test fetchWebView
	// ===================================
	@Test
	public void test_fetchWebView_Works() {
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
	}

	@Test
	public void test_fetchWebView_EmptyServerResponse() {
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

	@Test
	public void test_fetchWebView_WhenConnectionNull() {
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
	}

	@Test
	public void test_fetchWebView_ResponseCodeNot200() {
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
	}

	@Test
	public void test_fetchWebView_WhenCreateFullScreenFails() {
		// setup
		previewManager.endPoint = "someEndpoint";
		when(uiService.createFullscreenMessage(any(String.class), any(TargetPreviewFullscreenDelegate.class), any(Boolean.class), any(MessageSettings.class))).thenReturn(null);
		setMockConnectionResponse("TestHTML", 200);

		// test
		previewManager.fetchWebView();

		// verify message not displayed
		verify(networkService, times(1)).connectAsync(any(), any());
		verifyNoInteractions(fullscreenMessage);
	}

	// ===================================
	// Test setRestartDeepLink
	// ===================================
	@Test
	public void test_setRestartDeepLink() {
		// test
		previewManager.setRestartDeepLink("restartURL");

		//verify
		assertEquals("restartURL", previewManager.restartUrl);
	}

	@Test
	public void test_setRestartDeepLink_Null() {
		// test
		previewManager.setRestartDeepLink(null);

		//verify
		assertNull(previewManager.restartUrl);
	}

	// ===================================
	// Test getPreviewParameters
	// ===================================
	@Test
	public void test_getPreviewParameters() {
		// setup
		previewManager.previewParams = "params";

		// test
		assertEquals("params", previewManager.getPreviewParameters());
	}


	private void verifyFullScreenMessageDisplayed() {
		verify(uiService,times(1)).createFullscreenMessage(eq("TestHTML"), any(TargetPreviewFullscreenDelegate.class), eq(false), any(MessageSettings.class));
		verify(fullscreenMessage).show();
	}

	private void verifyFloatingButtonDisplayed() {
		verify(uiService,times(1)).createFloatingButton(any(TargetPreviewButtonListener.class));
		verify(floatingButton).display();
	}

}