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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.Target;
import com.adobe.marketing.mobile.services.AppContextService;
import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.services.uri.UriOpening;
import com.adobe.marketing.mobile.util.DataReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings("unchecked")
public class TargetExtensionTests {

    // Mock Constants
    private static final String MOCKED_CLIENT_CODE = "clientCode";
    private static final String MOCKED_TARGET_SERVER = "targetServer";
    private static final String MOCK_EDGE_HOST = "mboxedge35.tt.omtrdc.net";
    private static final String MOCK_THIRD_PARTY_ID = "thirdPartyId";
    private static final String MOCK_THIRD_PARTY_ID_1 = "thirdPartyID_1";
    private static final String MOCK_TNT_ID = "66E5C681-4F70-41A2-86AE-F1E151443B10.35_0";
    private static final String MOCK_TNT_ID_1 = "66E5C681-4F70-41A2-86AE-F1E151443B10.32_0";
    private static final String MOCK_TNT_ID_2 = "4DBCC39D-4ACA-47D4-A7D2-A85C1C0CC382.32_0";
    private static final String MOCK_TNT_ID_INVALID = "66E5C681-4F70-41A2-86AE-F1E151443B10.a1a_0";
    private static final String MOCK_SESSION_ID = "mockSessionID";
    private static final Integer MOCK_NETWORK_TIMEOUT = 5;

    private static final HashMap<String, Object> targetParameters =
            new HashMap() {
                {
                    put("paramKey", "paramValue");
                }
            };

    private static final HashMap<String, Object> targetSharedState =
            new HashMap() {
                {
                    put("tntid", MOCK_TNT_ID);
                    put("sessionid", MOCK_SESSION_ID);
                }
            };

    private static final HashMap<String, String> lifecycleSharedState =
            new HashMap() {
                {
                    put("lifecycleKey", "lifecycleValue");
                }
            };

    private static final HashMap<String, Object> identitySharedState =
            new HashMap() {
                {
                    put("mid", "samplemid");
                    put("blob", "sampleBlob");
                    put("locationhint", "sampleLocationHint");
                }
            };

    private static final HashMap<String, Object> eventHubSharedState =
            new HashMap() {
                {
                    put("version", "x.y.z");
                    put(
                            "wrapper",
                            new HashMap<String, Object>() {
                                {
                                    put("friendlyName", "None");
                                    put("type", "N");
                                }
                            });
                }
            };

    private static final Map<String, String> responseTokens =
            new HashMap() {
                {
                    put("responseTokens.Key", "responseTokens.Value");
                }
            };

    private static final Map<String, String> clickMetricA4TParams =
            new HashMap() {
                {
                    put("pe", "tnt");
                    put("tnta", "1234|1234");
                }
            };

    private static final Map<String, String> a4tParams =
            new HashMap() {
                {
                    put("pe", "tnt");
                    put("tnta", "1234|1234");
                }
            };

    private TargetExtension extension;

    // common argument captors
    ArgumentCaptor<Event> eventArgumentCaptor;
    ArgumentCaptor<NetworkRequest> networkRequestCaptor;
    ArgumentCaptor<NetworkCallback> networkCallbackCaptor;

    // Mocks
    @Mock ServiceProvider mockServiceProvider;

    @Mock Networking networkService;

    @Mock DeviceInforming deviceInforming;

    @Mock DataStoring dataStoring;

    @Mock UIService uiService;

    @Mock UriOpening uriService;

    @Mock AppContextService appContextService;

    @Mock Context context;

    @Mock ExtensionApi mockExtensionApi;

    @Mock TargetRequestBuilder requestBuilder;

    @Mock TargetResponseParser responseParser;

    @Mock TargetPreviewManager targetPreviewManager;

    @Mock TargetState targetState;

    @Mock HttpConnecting connecting;

    @Mock NamedCollection datastore;

    @Before
    public void setup() {}

    private void runWithMockedServiceProvider(final Runnable testRunnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(deviceInforming);
            when(mockServiceProvider.getDataStoreService()).thenReturn(dataStoring);
            when(dataStoring.getNamedCollection(Mockito.anyString())).thenReturn(datastore);
            when(mockServiceProvider.getNetworkService()).thenReturn(networkService);
            when(mockServiceProvider.getUIService()).thenReturn(uiService);
            when(mockServiceProvider.getUriService()).thenReturn(uriService);
            when(mockServiceProvider.getAppContextService()).thenReturn(appContextService);
            when(appContextService.getApplicationContext()).thenReturn(context);

            when(targetState.getClientCode()).thenReturn(MOCKED_CLIENT_CODE);
            when(targetState.getNetworkTimeout()).thenReturn(MOCK_NETWORK_TIMEOUT);
            when(targetState.getTargetServer()).thenReturn(MOCKED_TARGET_SERVER);
            when(targetState.getTntId()).thenReturn(MOCK_TNT_ID);
            when(targetState.getThirdPartyId()).thenReturn(MOCK_THIRD_PARTY_ID);
            when(targetState.getSessionId()).thenReturn(MOCK_SESSION_ID);
            when(targetState.getMobilePrivacyStatus()).thenReturn(MobilePrivacyStatus.OPT_IN);
            when(targetState.generateSharedState()).thenReturn(targetSharedState);

            eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
            networkRequestCaptor = ArgumentCaptor.forClass(NetworkRequest.class);
            networkCallbackCaptor = ArgumentCaptor.forClass(NetworkCallback.class);

            when(requestBuilder.getRequestPayload(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(validJSONObject());
            when(requestBuilder.getRequestPayload(any(), any(), any(), any(), any()))
                    .thenReturn(validJSONObject());
            when(requestBuilder.getDisplayNotificationJsonObject(
                            any(), any(), any(), anyLong(), any()))
                    .thenReturn(validJSONObject());

            final JSONObject validMboxResponse =
                    new JSONObject(
                            "{\"options\": [{\"content\": \"mbox0content\", \"type\": \"html\"}]}");
            when(responseParser.parseResponseToJson(any())).thenReturn(validJSONObject());
            when(responseParser.parseResponseToJson(any())).thenReturn(validJSONObject());
            when(responseParser.getTntId(any())).thenReturn(MOCK_TNT_ID);
            when(responseParser.getEdgeHost(any())).thenReturn(MOCK_EDGE_HOST);
            when(responseParser.extractBatchedMBoxes(any()))
                    .thenReturn(
                            new HashMap<String, JSONObject>() {
                                {
                                    put("mbox0", validMboxResponse);
                                }
                            });
            when(responseParser.extractMboxContent(eq(validMboxResponse)))
                    .thenReturn("mbox0content");
            when(responseParser.getResponseTokens(eq(validMboxResponse)))
                    .thenReturn(responseTokens);
            when(responseParser.extractClickMetricAnalyticsPayload(eq(validMboxResponse)))
                    .thenReturn(clickMetricA4TParams);
            when(responseParser.getAnalyticsForTargetPayload(any())).thenReturn(a4tParams);
            when(connecting.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

            extension =
                    new TargetExtension(
                            mockExtensionApi,
                            targetState,
                            targetPreviewManager,
                            requestBuilder,
                            responseParser);
            testRunnable.run();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void tearDown() {
        reset(mockServiceProvider);
        reset(networkService);
        reset(deviceInforming);
        reset(dataStoring);
        reset(uiService);
        reset(uriService);
        reset(appContextService);
        reset(context);
        reset(mockExtensionApi);
        reset(requestBuilder);
        reset(responseParser);
        reset(targetPreviewManager);
        reset(targetState);
        reset(connecting);
        reset(datastore);
    }

    @Test
    public void test_getName() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    final String extensionName = extension.getName();
                    assertEquals(
                            "getName should return the correct extension name.",
                            "com.adobe.module.target",
                            extensionName);
                });
    }

    @Test
    public void test_getFriendlyName() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    final String extensionName = extension.getFriendlyName();
                    assertEquals(
                            "getFriendlyName should return the correct extension friendly name.",
                            "Target",
                            extensionName);
                });
    }

    @Test
    public void test_getVersion() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    final String extensionVersion = extension.getVersion();
                    assertEquals(
                            "getVersion should return the correct extension version.",
                            Target.EXTENSION_VERSION,
                            extensionVersion);
                });
    }

    // **********************************************************************************************
    // Constructor
    // **********************************************************************************************

    @Test
    public void test_Constructor() {
        // verify that the targetExtension extension does not throw any exception
        new TargetExtension(mockExtensionApi);
    }

    // **********************************************************************************************
    // onRegister Tests
    // **********************************************************************************************

    @Test
    public void test_onRegister() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.onRegistered();

                    // verify that five listeners are registered
                    verify(mockExtensionApi, times(5)).registerEventListener(any(), any(), any());
                });
    }

    // **********************************************************************************************
    // readyForEvent tests
    // **********************************************************************************************

    @Test
    public void test_readyForEvent() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    setConfigurationSharedState(
                            MOCKED_CLIENT_CODE, 5, "optedin", false, "targetserver");

                    // test
                    extension.readyForEvent(noEventDataEvent());

                    // verify target state is not updated with correct configuration
                    verify(targetState).updateConfigurationSharedState(notNull());
                });
    }

    @Test
    public void test_readyForEvent_whenConfigurationIsNotSet() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.readyForEvent(noEventDataEvent());

                    // verify that the target state is not updated with latest configuration
                    verify(targetState).updateConfigurationSharedState(null);
                });
    }

    @Test
    public void test_readyForEvent_returnsTrueWhenConfigurationIsSet() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    setConfigurationSharedState(
                            MOCKED_CLIENT_CODE, 5, "optedin", false, "targetServer");
                    extension =
                            new TargetExtension(
                                    mockExtensionApi,
                                    targetState,
                                    targetPreviewManager,
                                    requestBuilder,
                                    responseParser);

                    // test
                    assertTrue(extension.readyForEvent(noEventDataEvent()));
                });
    }

    // **********************************************************************************************
    // LoadRequests tests
    // **********************************************************************************************

    @Test
    public void testLoadRequests_NoRequest_When_OptOut() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_OUT);
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);

                    // test
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "default", extractMboxContentFromEvent(eventArgumentCaptor.getValue()));
                });
    }

    @Test
    public void testLoadRequests_NoRequest_When_ClientCodeIsEmpty() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getClientCode()).thenReturn("");
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);

                    // test
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verifyNoInteractions(requestBuilder);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "default", extractMboxContentFromEvent(eventArgumentCaptor.getValue()));
                });
    }

    @Test
    public void testLoadRequests_NoRequest_When_TargetRequestListIsEmpty() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    final Event event = loadRequestEvent(getTargetRequestList(0), null);

                    // test
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verifyNoInteractions(requestBuilder);
                });
    }

    @Test
    public void testLoadRequests_NoRequest_When_NetworkServiceIsNotAvailable() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockServiceProvider.getNetworkService()).thenReturn(null);
                    extension =
                            new TargetExtension(
                                    mockExtensionApi,
                                    targetState,
                                    targetPreviewManager,
                                    requestBuilder,
                                    responseParser);
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);

                    // test
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "default", extractMboxContentFromEvent(eventArgumentCaptor.getValue()));
                });
    }

    @Test
    public void testLoadRequests_NoRequest_When_EventWithNoData() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.handleTargetRequestContentEvent(noEventDataEvent());

                    // verify
                    verifyNoInteractions(networkService);
                    verifyNoInteractions(requestBuilder);
                });
    }

    @Test
    public void testLoadRequests_when_invalidLoadRequestEventData() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.handleTargetRequestContentEvent(noEventDataEvent());

                    // verify
                    verifyNoInteractions(networkService);
                    verifyNoInteractions(requestBuilder);
                });
    }

    @Test
    public void testLoadRequests_whenEmptyJSONObject() {
        runWithMockedServiceProvider(
                () -> {
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);
                    try {
                        when(requestBuilder.getRequestPayload(
                                        any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(new JSONObject("{}"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    // test
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verify(requestBuilder)
                            .getRequestPayload(
                                    eq(null), anyList(), eq(null), anyList(), eq(""), eq(null),
                                    eq(null));
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "default", extractMboxContentFromEvent(eventArgumentCaptor.getValue()));
                });
    }

    @Test
    public void testLoadRequests_attachesLifecycleAndIdentityData() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    setLifecycleSharedState();
                    setIdentitySharedState();

                    // test
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verify(requestBuilder)
                            .getRequestPayload(
                                    eq(null),
                                    anyList(),
                                    eq(null),
                                    anyList(),
                                    eq(""),
                                    eq(identitySharedState),
                                    eq(lifecycleSharedState));
                });
    }

    @Test
    public void testLoadRequests_makesCorrectNetworkRequest() {
        runWithMockedServiceProvider(
                () -> {
                    setEventHubSharedState();
                    final JSONObject jsonObject;
                    try {
                        jsonObject =
                                new JSONObject(
                                        "{\n"
                                                + "\"name\": \"mbox1\",\n"
                                                + "\"options\":"
                                                + " [{\"eventToken\":\"displayEventToken\"}]\n"
                                                + "}\n");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    when(requestBuilder.getRequestPayload(
                                    any(), any(), any(), any(), any(), any(), any()))
                            .thenReturn(jsonObject);

                    // test
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verify(networkService)
                            .connectAsync(
                                    networkRequestCaptor.capture(),
                                    networkCallbackCaptor.capture());
                    assertEquals(
                            "https://"
                                    + MOCKED_TARGET_SERVER
                                    + "/rest/v1/delivery/?client="
                                    + MOCKED_CLIENT_CODE
                                    + "&sessionId="
                                    + MOCK_SESSION_ID,
                            networkRequestCaptor.getValue().getUrl());
                    assertEquals(HttpMethod.POST, networkRequestCaptor.getValue().getMethod());
                    final Map<String, String> headers =
                            networkRequestCaptor.getValue().getHeaders();
                    assertEquals(3, headers.size());
                    assertEquals("application/json", headers.get("Content-Type"));
                    assertEquals("AdobeTargetMobile-Android", headers.get("X-EXC-SDK"));
                    assertEquals(
                            String.format("%s+%s", "x.y.z", extension.getVersion()),
                            headers.get("X-EXC-SDK-Version"));
                    assertEquals(
                            MOCK_NETWORK_TIMEOUT,
                            networkRequestCaptor.getValue().getReadTimeout(),
                            0);
                    assertEquals(
                            MOCK_NETWORK_TIMEOUT,
                            networkRequestCaptor.getValue().getConnectTimeout(),
                            0);
                });
    }

    @Test
    public void testLoadRequests_makesNetworkRequest_wrapperInfoAvailable() {
        runWithMockedServiceProvider(
                () -> {
                    final Map<String, Object> eventHubState =
                            new HashMap<String, Object>() {
                                {
                                    put("version", "x.y.z");
                                    put(
                                            "wrapper",
                                            new HashMap<String, Object>() {
                                                {
                                                    put("friendlyName", "Flutter");
                                                    put("type", "F");
                                                }
                                            });
                                }
                            };
                    setEventHubSharedState(eventHubState);
                    final JSONObject jsonObject;
                    try {
                        jsonObject =
                                new JSONObject(
                                        "{\n"
                                                + "\"name\": \"mbox1\",\n"
                                                + "\"options\":"
                                                + " [{\"eventToken\":\"displayEventToken\"}]\n"
                                                + "}\n");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    when(requestBuilder.getRequestPayload(
                                    any(), any(), any(), any(), any(), any(), any()))
                            .thenReturn(jsonObject);

                    // test
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verify(networkService)
                            .connectAsync(
                                    networkRequestCaptor.capture(),
                                    networkCallbackCaptor.capture());
                    assertEquals(
                            "https://"
                                    + MOCKED_TARGET_SERVER
                                    + "/rest/v1/delivery/?client="
                                    + MOCKED_CLIENT_CODE
                                    + "&sessionId="
                                    + MOCK_SESSION_ID,
                            networkRequestCaptor.getValue().getUrl());
                    assertEquals(HttpMethod.POST, networkRequestCaptor.getValue().getMethod());
                    final Map<String, String> headers =
                            networkRequestCaptor.getValue().getHeaders();
                    assertEquals(3, headers.size());
                    assertEquals("application/json", headers.get("Content-Type"));
                    assertEquals("AdobeTargetMobile-Android-Flutter", headers.get("X-EXC-SDK"));
                    assertEquals(
                            String.format("%s+%s", "x.y.z", extension.getVersion()),
                            headers.get("X-EXC-SDK-Version"));
                    assertEquals(
                            MOCK_NETWORK_TIMEOUT,
                            networkRequestCaptor.getValue().getReadTimeout(),
                            0);
                    assertEquals(
                            MOCK_NETWORK_TIMEOUT,
                            networkRequestCaptor.getValue().getConnectTimeout(),
                            0);
                });
    }

    @Test
    public void testLoadRequests_makesNetworkRequest_eventHubSharedStateNotAvailable() {
        runWithMockedServiceProvider(
                () -> {
                    setEventHubSharedState(null);
                    final JSONObject jsonObject;
                    try {
                        jsonObject =
                                new JSONObject(
                                        "{\n"
                                                + "\"name\": \"mbox1\",\n"
                                                + "\"options\":"
                                                + " [{\"eventToken\":\"displayEventToken\"}]\n"
                                                + "}\n");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    when(requestBuilder.getRequestPayload(
                                    any(), any(), any(), any(), any(), any(), any()))
                            .thenReturn(jsonObject);

                    // test
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verify(networkService)
                            .connectAsync(
                                    networkRequestCaptor.capture(),
                                    networkCallbackCaptor.capture());
                    assertEquals(
                            "https://"
                                    + MOCKED_TARGET_SERVER
                                    + "/rest/v1/delivery/?client="
                                    + MOCKED_CLIENT_CODE
                                    + "&sessionId="
                                    + MOCK_SESSION_ID,
                            networkRequestCaptor.getValue().getUrl());
                    assertEquals(HttpMethod.POST, networkRequestCaptor.getValue().getMethod());
                    final Map<String, String> headers =
                            networkRequestCaptor.getValue().getHeaders();
                    assertEquals(3, headers.size());
                    assertEquals("application/json", headers.get("Content-Type"));
                    assertEquals("AdobeTargetMobile-Android", headers.get("X-EXC-SDK"));
                    assertEquals("", headers.get("X-EXC-SDK-Version"));
                    assertEquals(
                            MOCK_NETWORK_TIMEOUT,
                            networkRequestCaptor.getValue().getReadTimeout(),
                            0);
                    assertEquals(
                            MOCK_NETWORK_TIMEOUT,
                            networkRequestCaptor.getValue().getConnectTimeout(),
                            0);
                });
    }

    @Test
    public void testLoadRequests_withTargetEdgeHost() {
        runWithMockedServiceProvider(
                () -> {
                    when(targetState.getTargetServer()).thenReturn("");
                    when(targetState.getEdgeHost()).thenReturn(MOCK_EDGE_HOST);

                    // test
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verify(networkService, times(1))
                            .connectAsync(networkRequestCaptor.capture(), any());
                    assertEquals(
                            "https://"
                                    + MOCK_EDGE_HOST
                                    + "/rest/v1/delivery/?client="
                                    + MOCKED_CLIENT_CODE
                                    + "&sessionId="
                                    + MOCK_SESSION_ID,
                            networkRequestCaptor.getValue().getUrl());
                });
    }

    @Test
    public void testLoadRequests_ResponseNotProcessed_When_ConnectionIsNull() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());

                    // test
                    networkCallbackCaptor.getValue().call(null);

                    // verify the dispatched response event
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "default", extractMboxContentFromEvent(eventArgumentCaptor.getValue()));
                });
    }

    @Test
    public void testLoadRequests_ReturnDefaultContent_When_ResponseJsonIsNull() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.parseResponseToJson(any())).thenReturn(null);

                    // test
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);
                    extension.handleTargetRequestContentEvent(event);
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify default response event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "default", extractMboxContentFromEvent(eventArgumentCaptor.getValue()));
                });
    }

    @Test
    public void testLoadRequests_ReturnDefaultContent_When_ResponseNot200OK() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(connecting.getResponseCode())
                            .thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

                    // test
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);
                    extension.handleTargetRequestContentEvent(event);
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify default response event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "default", extractMboxContentFromEvent(eventArgumentCaptor.getValue()));
                });
    }

    @Test
    public void testLoadRequests_ReturnDefaultContent_When_ResponseError() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.getErrorMessage(any())).thenReturn("anyError");

                    // test
                    final Event event = loadRequestEvent(getTargetRequestList(1), null);
                    extension.handleTargetRequestContentEvent(event);
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify default response event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "default", extractMboxContentFromEvent(eventArgumentCaptor.getValue()));
                });
    }

    @Test
    public void testLoadRequests_whenValidResponse() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    final JSONObject validMboxResponse;
                    try {
                        validMboxResponse =
                                new JSONObject(
                                        "{\"options\": [{\"content\": \"mbox0content\", \"type\":"
                                                + " \"html\"}]}");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        when(responseParser.parseResponseToJson(any()))
                                .thenReturn(validJSONObject());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    when(responseParser.getTntId(any())).thenReturn(MOCK_TNT_ID_1);
                    when(responseParser.getEdgeHost(any())).thenReturn(MOCK_EDGE_HOST);
                    when(responseParser.extractBatchedMBoxes(any()))
                            .thenReturn(
                                    new HashMap<String, JSONObject>() {
                                        {
                                            put("mbox0", validMboxResponse);
                                        }
                                    });
                    when(responseParser.extractMboxContent(eq(validMboxResponse)))
                            .thenReturn("mbox0content");
                    when(responseParser.extractMboxContent(eq(validMboxResponse)))
                            .thenReturn("mbox0content");
                    when(responseParser.getResponseTokens(eq(validMboxResponse)))
                            .thenReturn(responseTokens);
                    when(responseParser.extractClickMetricAnalyticsPayload(eq(validMboxResponse)))
                            .thenReturn(clickMetricA4TParams);
                    when(responseParser.getAnalyticsForTargetPayload(eq(validMboxResponse)))
                            .thenReturn(a4tParams);
                    when(responseParser.getAnalyticsForTargetPayload(any(), any()))
                            .thenReturn(a4tParams);

                    // test
                    Event event = loadRequestEvent(getTargetRequestList(1), null);
                    extension.handleTargetRequestContentEvent(event);
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify the target state are correctly set from response
                    verify(targetState).clearNotifications();
                    verify(targetState).updateSessionTimestamp(eq(false));
                    verify(targetState).updateEdgeHost(MOCK_EDGE_HOST);
                    verify(targetState).updateTntId(MOCK_TNT_ID_1);
                    verify(mockExtensionApi).createSharedState(eq(targetSharedState), eq(event));
                    verify(mockExtensionApi, times(2)).dispatch(eventArgumentCaptor.capture());
                    Event a4tEvent = eventArgumentCaptor.getAllValues().get(0);
                    Event mboxContentEvent = eventArgumentCaptor.getAllValues().get(1);

                    // verify the a4t event
                    assertEquals(EventType.ANALYTICS, a4tEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, a4tEvent.getSource());
                    assertEquals(true, a4tEvent.getEventData().get("trackinternal"));
                    assertEquals("AnalyticsForTarget", a4tEvent.getEventData().get("action"));
                    assertEquals(a4tParams, a4tEvent.getEventData().get("contextdata"));

                    // verify the dispatched mbox content event
                    assertEquals("mbox0content", extractMboxContentFromEvent(mboxContentEvent));
                    assertEquals(responseTokens, extractResponseToken(mboxContentEvent));
                    assertEquals(a4tParams, extractAnalyticsPayload(mboxContentEvent));
                    assertEquals(clickMetricA4TParams, extractClickMetric(mboxContentEvent));
                });
    }

    @Test
    public void testLoadRequests_ReturnDefaultContent_When_ResponseJsonNotContainMbox() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.extractMboxContent(any())).thenReturn(null);

                    // test
                    extension.handleTargetRequestContentEvent(
                            loadRequestEvent(getTargetRequestList(1), null));
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify default response event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "default", extractMboxContentFromEvent(eventArgumentCaptor.getValue()));
                });
    }

    @Test
    public void
            testLoadRequests_ReturnDefaultContent_When_ResponseJsonContainMboxWithEmptyContent() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.extractMboxContent(any())).thenReturn("");

                    // test
                    extension.handleTargetRequestContentEvent(
                            loadRequestEvent(getTargetRequestList(1), null));
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify default response event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "default", extractMboxContentFromEvent(eventArgumentCaptor.getValue()));
                });
    }

    @Test
    public void
            testLoadRequests_NotSendAnalyticsRequest_When_ResponseJsonDoesNotContainA4TPayload() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.getAnalyticsForTargetPayload(any(), any()))
                            .thenReturn(null);

                    // test
                    extension.handleTargetRequestContentEvent(
                            loadRequestEvent(getTargetRequestList(1), null));
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify only target response event is dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(EventType.TARGET, eventArgumentCaptor.getValue().getType());
                });
    }

    @Test
    public void
            testLoadRequests_SetTntIdAndEdgeHostToNull_When_ResponseJsonNotContainTntIdAndEdgeHost() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.getEdgeHost(any())).thenReturn(null);
                    when(responseParser.getTntId(any())).thenReturn(null);

                    // test
                    extension.handleTargetRequestContentEvent(
                            loadRequestEvent(getTargetRequestList(1), null));
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify
                    verify(targetState).updateTntId(null);
                    verify(targetState, times(2)).updateEdgeHost(null);
                });
    }

    // **********************************************************************************************
    // setThirdPartyId
    // **********************************************************************************************

    @Test
    public void testSetThirdPartyId_validInput() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getThirdPartyId()).thenReturn(null);

                    // test
                    extension.handleTargetRequestIdentityEvent(
                            setThirdPartyIdEvent(MOCK_THIRD_PARTY_ID));

                    // validate
                    verify(targetState).updateThirdPartyId(MOCK_THIRD_PARTY_ID);
                });
    }

    @Test
    public void testSetThirdPartyId_NullInput() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getThirdPartyId()).thenReturn(MOCK_THIRD_PARTY_ID);

                    // test
                    extension.handleTargetRequestIdentityEvent(setThirdPartyIdEvent(null));

                    // validate
                    verify(targetState).updateThirdPartyId(null);
                });
    }

    @Test
    public void testSetThirdPartyId_NewID() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getThirdPartyId()).thenReturn(MOCK_THIRD_PARTY_ID);

                    // test
                    extension.handleTargetRequestIdentityEvent(
                            setThirdPartyIdEvent(MOCK_THIRD_PARTY_ID_1));

                    // validate
                    verify(targetState).updateThirdPartyId(MOCK_THIRD_PARTY_ID_1);
                });
    }

    @Test
    public void testSetThirdPartyId_sameID_Then_NoOp() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getThirdPartyId()).thenReturn(MOCK_THIRD_PARTY_ID);

                    // test
                    extension.handleTargetRequestIdentityEvent(
                            setThirdPartyIdEvent(MOCK_THIRD_PARTY_ID));

                    // validate
                    verify(targetState, times(0)).updateThirdPartyId(any());
                });
    }

    @Test
    public void testSetThirdPartyId_Creates_SharedState_Privacy_OptIn() {
        runWithMockedServiceProvider(
                () -> {
                    // set privacy status optedIn
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_IN);

                    // test
                    final Event event = setThirdPartyIdEvent(MOCK_THIRD_PARTY_ID);
                    extension.handleTargetRequestIdentityEvent(event);

                    // validate
                    verify(mockExtensionApi).createSharedState(targetSharedState, event);
                });
    }

    @Test
    public void testSetThirdPartyId_Creates_SharedState_Privacy_OptUnknown() {
        runWithMockedServiceProvider(
                () -> {
                    // set privacy status optUnknown
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.UNKNOWN);

                    // test
                    final Event event = setThirdPartyIdEvent(MOCK_THIRD_PARTY_ID);
                    extension.handleTargetRequestIdentityEvent(event);

                    // validate
                    verify(mockExtensionApi).createSharedState(targetSharedState, event);
                });
    }

    @Test
    public void testSetThirdPartyId_On_Privacy_OptOut() {
        runWithMockedServiceProvider(
                () -> {
                    // set privacy status optedOut
                    final HashMap<String, Object> emptySharedState = new HashMap();
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_OUT);
                    when(targetState.generateSharedState()).thenReturn(emptySharedState);

                    // test
                    final Event event = setThirdPartyIdEvent(MOCK_THIRD_PARTY_ID);
                    extension.handleTargetRequestIdentityEvent(event);

                    // validate that the shared state is updated with empty map and the third party
                    // Id is not set
                    verify(mockExtensionApi).createSharedState(emptySharedState, event);
                    verify(targetState, times(0)).updateThirdPartyId(any());
                });
    }

    // **********************************************************************************************
    // setTntId
    // **********************************************************************************************

    @Test
    public void testSetTntId_validInput() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getTntId()).thenReturn(null);

                    // test
                    final Event event = setTntIdEvent(MOCK_TNT_ID);
                    extension.handleTargetRequestIdentityEvent(event);

                    // validate
                    verify(targetState).updateTntId(MOCK_TNT_ID);
                    verify(targetState).updateEdgeHost("mboxedge35.tt.omtrdc.net");
                    // verify that a new shared state is generated
                    verify(mockExtensionApi).createSharedState(any(), eq(event));
                });
    }

    @Test
    public void testSetTntId_null_whenAlreadySet() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getTntId()).thenReturn(MOCK_TNT_ID);

                    // test
                    final Event event = setTntIdEvent(null);
                    extension.handleTargetRequestIdentityEvent(event);

                    // validate
                    verify(targetState).updateTntId(null);
                    verify(targetState).updateEdgeHost(null);
                    // verify that a new shared state is generated
                    verify(mockExtensionApi).createSharedState(any(), eq(event));
                });
    }

    @Test
    public void testSetTntId_null_whenAlreadyNull() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getTntId()).thenReturn(null);

                    // test
                    extension.handleTargetRequestIdentityEvent(setTntIdEvent(null));

                    // validate that the state is not updated
                    verify(targetState, times(0)).updateTntId(any());
                    verify(targetState, times(0)).updateEdgeHost(any());
                });
    }

    @Test
    public void testSetTntId_EmptyInput() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getTntId()).thenReturn(MOCK_TNT_ID);

                    // test
                    extension.handleTargetRequestIdentityEvent(setTntIdEvent(""));

                    // validate that the state is not updated
                    verify(targetState).updateTntId(eq(""));
                    verify(targetState).updateEdgeHost(null);
                });
    }

    @Test
    public void testSetTntId_newIdHasDifferentHint() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    // MOCK_TNT_ID  =  66E5C681-4F70-41A2-86AE-F1E151443B10.35_0
                    // MOCK_TNT_ID1 =  66E5C681-4F70-41A2-86AE-F1E151443B10.32_0
                    when(targetState.getTntId()).thenReturn(MOCK_TNT_ID);

                    // test
                    extension.handleTargetRequestIdentityEvent(setTntIdEvent(MOCK_TNT_ID_1));

                    // verify
                    verify(targetState).updateTntId(MOCK_TNT_ID_1);
                    verify(targetState).updateEdgeHost("mboxedge32.tt.omtrdc.net");
                });
    }

    @Test
    public void testSetTntId_newIdHasDifferentUUID() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    // MOCK_TNT_ID_1 =  66E5C681-4F70-41A2-86AE-F1E151443B10.32_0
                    // MOCK_TNT_ID_2 =  4DBCC39D-4ACA-47D4-A7D2-A85C1C0CC382.32_0
                    when(targetState.getTntId()).thenReturn(MOCK_TNT_ID_1);

                    // test
                    extension.handleTargetRequestIdentityEvent(setTntIdEvent(MOCK_TNT_ID_2));

                    // verify
                    verify(targetState).updateTntId(MOCK_TNT_ID_2);
                    verify(targetState).updateEdgeHost("mboxedge32.tt.omtrdc.net");
                });
    }

    @Test
    public void testSetTntId_newIdHasDifferentUUIDAndHint() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    // MOCK_TNT_ID  =  66E5C681-4F70-41A2-86AE-F1E151443B10.35_0
                    // MOCK_TNT_ID_2 =  4DBCC39D-4ACA-47D4-A7D2-A85C1C0CC382.32_0
                    when(targetState.getTntId()).thenReturn(MOCK_TNT_ID);

                    // test
                    extension.handleTargetRequestIdentityEvent(setTntIdEvent(MOCK_TNT_ID_2));

                    // verify
                    verify(targetState).updateTntId(MOCK_TNT_ID_2);
                    verify(targetState).updateEdgeHost("mboxedge32.tt.omtrdc.net");
                });
    }

    @Test
    public void testSetTntId_newIdHasInvalidHint() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.handleTargetRequestIdentityEvent(setTntIdEvent(MOCK_TNT_ID_INVALID));

                    // verify
                    verify(targetState).updateTntId(MOCK_TNT_ID_INVALID);
                    verify(targetState).updateEdgeHost(null);
                });
    }

    @Test
    public void testSetTntId_Fails_Privacy_OptOut() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_OUT);

                    // test
                    extension.handleTargetRequestIdentityEvent(setTntIdEvent(MOCK_TNT_ID_1));

                    // validate that the state is not updated
                    verify(targetState, times(0)).updateTntId(any());
                    verify(targetState, times(0)).updateEdgeHost(any());
                });
    }

    @Test
    public void testSetTntId_Succeeds_Privacy_Unknown() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.UNKNOWN);

                    // test
                    extension.handleTargetRequestIdentityEvent(setTntIdEvent(MOCK_TNT_ID_1));

                    // validate that the state is updated
                    verify(targetState).updateTntId(any());
                    verify(targetState).updateEdgeHost(any());
                });
    }

    // **********************************************************************************************
    // setSessionId
    // **********************************************************************************************

    @Test
    public void testSetSessionId_validInput() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getSessionId()).thenReturn(null);

                    // test
                    extension.handleTargetRequestIdentityEvent(setSessionIdEvent(MOCK_SESSION_ID));

                    // validate
                    verify(targetState).updateSessionId(MOCK_SESSION_ID);
                    verify(targetState).updateSessionTimestamp(eq(false));
                });
    }

    @Test
    public void testSetSessionId_nullInput() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.handleTargetRequestIdentityEvent(setSessionIdEvent(null));

                    // verify
                    verify(targetState).resetSession();
                });
    }

    @Test
    public void testSetSessionId_privacyOptedOut() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_OUT);

                    // test
                    extension.handleTargetRequestIdentityEvent(setSessionIdEvent(MOCK_SESSION_ID));

                    // validate that the state is not updated
                    verify(targetState, times(0)).updateSessionId(any());
                });
    }

    @Test
    public void testSetSessionId_newSessionIdValueIsUnchanged() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    // the setup step mocks with current session to value MOCK_SESSION_ID

                    // test
                    extension.handleTargetRequestIdentityEvent(setSessionIdEvent(MOCK_SESSION_ID));

                    // validate
                    verify(targetState, times(0)).updateSessionId(any());
                    verify(targetState).updateSessionTimestamp(eq(false));
                });
    }

    // **********************************************************************************************
    // ClearPrefetchCache
    // **********************************************************************************************

    @Test
    public void test_onClearPrefetchCacheEvent() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.handleTargetRequestResetEvent(clearPrefetchCacheEvent());

                    // verify
                    verify(targetState).clearPrefetchedMboxes();
                });
    }

    // **********************************************************************************************
    // ResetExperience
    // **********************************************************************************************

    @Test
    public void test_onResetExperienceEvent() {
        runWithMockedServiceProvider(
                () -> {
                    when(targetState.generateSharedState()).thenReturn(null);
                    when(targetState.getEdgeHost()).thenReturn("hst");
                    when(targetState.getTntId()).thenReturn("sampleId");
                    when(targetState.getThirdPartyId()).thenReturn("sampleId");
                    Event event = resetExperienceEvent();

                    // test
                    extension.handleTargetRequestResetEvent(event);

                    // verify
                    verify(targetState).resetSession();
                    verify(targetState).updateTntId(eq(null));
                    verify(targetState).updateThirdPartyId(eq(null));
                    verify(targetState, times(2)).updateEdgeHost(eq(null));
                    verify(mockExtensionApi).createSharedState(eq(null), eq(event));
                });
    }

    @Test
    public void test_RequestResetEvent_when_noEventData() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    HashMap<String, Object> eventData = new HashMap<>();
                    eventData.put(TargetConstants.EventDataKeys.LOAD_REQUEST, "invalid");
                    Event event =
                            new Event.Builder("Test", EventType.TARGET, EventSource.NONE)
                                    .setEventData(eventData)
                                    .build();
                    extension.handleTargetRequestResetEvent(event);

                    // verify
                    verifyNoInteractions(targetState);
                    verifyNoInteractions(mockExtensionApi);
                });
    }

    // **********************************************************************************************
    // HandleRawEvent
    // **********************************************************************************************

    @Test
    public void testHandleRawRequest_NoRequest_When_NoConfiguration() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getClientCode()).thenReturn("");

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify
                    verifyNoInteractions(networkService);
                    verifyNoInteractions(requestBuilder);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertNull(
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(EventDataKeys.RESPONSE_DATA));
                });
    }

    @Test
    public void testHandleRawRequest_NoRequest_When_OptOut() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_OUT);

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify
                    verifyNoInteractions(networkService);
                    verifyNoInteractions(requestBuilder);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertNull(
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(EventDataKeys.RESPONSE_DATA));
                });
    }

    @Test
    public void testHandleRawRequest_NoRequest_When_NetworkServiceIsNotAvailable() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockServiceProvider.getNetworkService()).thenReturn(networkService);
                    when(mockServiceProvider.getNetworkService()).thenReturn(null);
                    extension =
                            new TargetExtension(
                                    mockExtensionApi,
                                    targetState,
                                    targetPreviewManager,
                                    requestBuilder,
                                    responseParser);

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertNull(
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(EventDataKeys.RESPONSE_DATA));
                });
    }

    @Test
    public void testHandleRawRequest_TargetRequestBuilder_isNull() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockServiceProvider.getDeviceInfoService()).thenReturn(null);
                    extension =
                            new TargetExtension(
                                    mockExtensionApi,
                                    targetState,
                                    targetPreviewManager,
                                    null,
                                    responseParser);

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify
                    verifyNoInteractions(networkService);
                    verifyNoInteractions(requestBuilder);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertNull(
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(EventDataKeys.RESPONSE_DATA));
                });
    }

    @Test
    public void testHandleRawRequest_NoRequest_When_RequestPayloadIsEmpty() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    JSONObject json = null;
                    try {
                        json = new JSONObject("{}");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    when(requestBuilder.getRequestPayload(any(), any(), any(), any(), any()))
                            .thenReturn(json);

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify no network request is made
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertNull(
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(EventDataKeys.RESPONSE_DATA));
                });
    }

    @Test
    public void
            testHandleRawRequest_SendRequest_When_RequestPayloadIsValid_sendsCorrectNetworkRequest()
                    throws Exception {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    setEventHubSharedState();
                    JSONObject json = null;
                    try {
                        json = new JSONObject("{\"test\":\"value\"}");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    when(requestBuilder.getRequestPayload(any(), any(), any(), any(), any()))
                            .thenReturn(json);

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify
                    verify(networkService)
                            .connectAsync(
                                    networkRequestCaptor.capture(),
                                    networkCallbackCaptor.capture());
                    assertEquals(
                            "https://"
                                    + MOCKED_TARGET_SERVER
                                    + "/rest/v1/delivery/?client="
                                    + MOCKED_CLIENT_CODE
                                    + "&sessionId="
                                    + MOCK_SESSION_ID,
                            networkRequestCaptor.getValue().getUrl());
                    assertEquals(HttpMethod.POST, networkRequestCaptor.getValue().getMethod());
                    final Map<String, String> headers =
                            networkRequestCaptor.getValue().getHeaders();
                    assertEquals(3, headers.size());
                    assertEquals("application/json", headers.get("Content-Type"));
                    assertEquals("AdobeTargetMobile-Android", headers.get("X-EXC-SDK"));
                    assertEquals(
                            String.format("%s+%s", "x.y.z", extension.getVersion()),
                            headers.get("X-EXC-SDK-Version"));
                    assertEquals(
                            json.toString(),
                            new String(
                                    networkRequestCaptor.getValue().getBody(),
                                    StandardCharsets.UTF_8));
                    assertEquals(
                            MOCK_NETWORK_TIMEOUT,
                            networkRequestCaptor.getValue().getReadTimeout(),
                            0);
                    assertEquals(
                            MOCK_NETWORK_TIMEOUT,
                            networkRequestCaptor.getValue().getConnectTimeout(),
                            0);
                });
    }

    @Test
    public void testHandleRawRequest_ResponseNotProcessed_When_ConnectionIsNull() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify and call callback
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(null);

                    // verify the dispatched response event
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertNull(
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(EventDataKeys.RESPONSE_DATA));
                });
    }

    @Test
    public void testHandleRawRequest_DispatchRawResponse_When_ResponseJsonIsNull() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.parseResponseToJson(any())).thenReturn(null);

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify and call callback
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify the dispatched response event
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertNull(
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(EventDataKeys.RESPONSE_DATA));
                });
    }

    @Test
    public void testHandleRawRequest_DispatchRawResponse_When_NetworkResponseNot200() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(connecting.getResponseCode())
                            .thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
                    when(responseParser.getErrorMessage(any())).thenReturn("SomeErrorMessage");

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify and call callback
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify the dispatched response event
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertNull(
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(EventDataKeys.RESPONSE_DATA));
                });
    }

    @Test
    public void
            testHandleRawRequest_SetTntIdAndEdgeHost_When_ResponseJsonContainValidTntIdAndEdgeHost() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.getTntId(any())).thenReturn(MOCK_TNT_ID_1);
                    when(responseParser.getEdgeHost(any())).thenReturn(MOCK_EDGE_HOST);

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify and call callback
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify
                    verify(targetState).updateSessionTimestamp(eq(false));
                    verify(targetState).updateEdgeHost(eq(MOCK_EDGE_HOST));
                    verify(targetState).updateTntId(eq(MOCK_TNT_ID_1));
                });
    }

    @Test
    public void
            testHandleRawRequest_SetTntIdAndEdgeHostToNull_When_ResponseJsonNotContainTntIdAndEdgeHost() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.getTntId(any())).thenReturn(null);
                    when(responseParser.getEdgeHost(any())).thenReturn(null);

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify and call callback
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify
                    verify(targetState).updateSessionTimestamp(eq(false));
                    verify(targetState, times(2)).updateEdgeHost(eq(null));
                    verify(targetState).updateTntId(eq(null));
                });
    }

    @Test
    public void
            testHandleRawRequest_DispatchExecuteResponse_When_ResponseJsonContainsMboxWithValidContent() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> executeMbox =
                            new HashMap<String, Object>() {
                                {
                                    put("name", "mbox0");
                                    put(
                                            "options",
                                            new ArrayList<Map<String, Object>>() {
                                                {
                                                    add(
                                                            new HashMap<String, Object>() {
                                                                {
                                                                    put("type", "html");
                                                                    put(
                                                                            "content",
                                                                            "<html><body>Hello</body></html>");
                                                                }
                                                            });
                                                }
                                            });
                                    put(
                                            "metrics",
                                            new ArrayList<Map<String, Object>>() {
                                                {
                                                    add(
                                                            new HashMap<String, Object>() {
                                                                {
                                                                    put("type", "click");
                                                                    put(
                                                                            "eventToken",
                                                                            "LgG0+YDMHn4X5HqGJVoZ5g==");
                                                                }
                                                            });
                                                }
                                            });
                                }
                            };

                    List<Map<String, Object>> executeMboxes =
                            new ArrayList<Map<String, Object>>() {
                                {
                                    add(executeMbox);
                                }
                            };

                    Map<String, Object> responseMap =
                            new HashMap<String, Object>() {
                                {
                                    put("status", 200);
                                    put("requestId", "01d4a408-6978-48f7-95c6-03f04160b257");
                                    put("client", "adobe");
                                    put("edgeHost", "mboxedge35.tt.omtrdc.net");
                                    put(
                                            "id",
                                            new HashMap<String, Object>() {
                                                {
                                                    put(
                                                            "tntId",
                                                            "DE03D4AD-1FFE-421F-B2F2-303BF26822C1.35_0");
                                                    put(
                                                            "marketingCloudVisitorId",
                                                            "61055260263379929267175387965071996926");
                                                }
                                            });
                                    put(
                                            "execute",
                                            new HashMap<String, Object>() {
                                                {
                                                    put("mboxes", executeMboxes);
                                                }
                                            });
                                }
                            };
                    when(responseParser.parseResponseToJson(any()))
                            .thenReturn(new JSONObject(responseMap));

                    // test
                    extension.handleTargetRequestContentEvent(rawRequestExecuteEvent(1));

                    // verify and call callback
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify the dispatched response event contains the response data parsed by
                    // responseParser
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            responseMap,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(EventDataKeys.RESPONSE_DATA));
                });
    }

    @Test
    public void testHandleRawRequest_SendNotification_When_RequestPayloadIsValid() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    JSONObject json = null;
                    try {
                        json =
                                new JSONObject(
                                        "{\"mbox\": {\"name\": \"mbox1\"},"
                                                + " \"tokens\":[\"someToken\"]}");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    when(requestBuilder.getRequestPayload(any(), any(), any(), any(), any()))
                            .thenReturn(json);

                    // test
                    extension.handleTargetRequestContentEvent(
                            getTargetRawRequestForNotificationsEvent(1));

                    // verify
                    verify(networkService)
                            .connectAsync(
                                    networkRequestCaptor.capture(),
                                    networkCallbackCaptor.capture());
                });
    }

    // ========================================================================================
    // HandleGenericDataOSEvent
    // ========================================================================================

    @Test
    public void testHandleGenericDataOSEvent_withValidDeeplink() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    final String deeplink = "deeplink://something";
                    when(targetState.isPreviewEnabled()).thenReturn(true);

                    // test
                    extension.handleGenericDataOSEvent(previewDeeplinkEvent(deeplink));

                    // verify
                    verify(targetPreviewManager)
                            .enterPreviewModeWithDeepLinkParams(
                                    eq(MOCKED_CLIENT_CODE), eq(deeplink));
                });
    }

    @Test
    public void testHandleGenericDataOSEvent_whenPreviewNotEnabledInConfiguration() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    final String deeplink = "deeplink://something";
                    when(targetState.isPreviewEnabled()).thenReturn(false);

                    // test
                    extension.handleGenericDataOSEvent(previewDeeplinkEvent(deeplink));

                    // verify
                    verifyNoInteractions(targetPreviewManager);
                });
    }

    @Test
    public void testHandleGenericDataOSEvent_whenNullDeeplink() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.isPreviewEnabled()).thenReturn(true);

                    // test
                    extension.handleGenericDataOSEvent(previewDeeplinkEvent(null));

                    // verify
                    verifyNoInteractions(targetPreviewManager);
                });
    }

    @Test
    public void testHandleGenericDataOSEvent_whenEmptyDeeplink() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.isPreviewEnabled()).thenReturn(true);

                    // test
                    extension.handleGenericDataOSEvent(previewDeeplinkEvent(""));

                    // verify
                    verifyNoInteractions(targetPreviewManager);
                });
    }

    @Test
    public void testHandleGenericDataOSEvent_when_targetNotConfigured() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getClientCode()).thenReturn("");
                    when(targetState.isPreviewEnabled()).thenReturn(true);

                    // test
                    extension.handleGenericDataOSEvent(previewDeeplinkEvent("target:deeplink//"));

                    // verify
                    verifyNoInteractions(targetPreviewManager);
                });
    }

    @Test
    public void testHandleGenericDataOSEvent_when_emptyEventData() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.isPreviewEnabled()).thenReturn(true);

                    // test
                    extension.handleGenericDataOSEvent(noEventDataEvent());

                    // verify
                    verifyNoInteractions(targetPreviewManager);
                });
    }

    // ========================================================================================
    // setPreviewRestartDeeplink
    // ========================================================================================

    @Test
    public void testSetPreviewRestartDeepLink_when_emptyDeeplink() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.handleTargetRequestContentEvent(previewRestartDeeplinkEvent(""));

                    // verify
                    verifyNoInteractions(targetPreviewManager);
                });
    }

    @Test
    public void testSetPreviewRestartDeepLink_when_nullDeeplink() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.handleTargetRequestContentEvent(previewRestartDeeplinkEvent(null));

                    // verify
                    verifyNoInteractions(targetPreviewManager);
                });
    }

    @Test
    public void testSetPreviewRestartDeepLink_when_validDeeplink() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    final String restartDeeplink = "deeplink://";
                    extension.handleTargetRequestContentEvent(
                            previewRestartDeeplinkEvent(restartDeeplink));

                    // verify
                    verify(targetPreviewManager).setRestartDeepLink(eq(restartDeeplink));
                });
    }

    // **********************************************************************************************
    // HandlePrefetchContent
    // **********************************************************************************************

    @Test
    public void testHandlePrefetchContent_NoRequest_When_OptOut() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_OUT);

                    // test
                    final Event event = prefetchContentEvent(getTargetPrefetchList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Privacy status is not opted in",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_NoRequest_When_ClientCodeIsEmpty() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getClientCode()).thenReturn("");

                    // test
                    final Event event = prefetchContentEvent(getTargetPrefetchList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Missing client code",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_NoRequest_When_NoPrefetchList() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getClientCode()).thenReturn("");

                    // test
                    final Event event = prefetchContentEvent(getTargetPrefetchList(0), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Empty or null prefetch requests list",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_NoRequest_When_PreviewModeInOn() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetPreviewManager.getPreviewParameters()).thenReturn("someParameter");

                    // test
                    final Event event = prefetchContentEvent(getTargetPrefetchList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Target prefetch can't be used while in preview mode",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_NoRequest_When_NetworkServiceIsNotAvailable() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockServiceProvider.getNetworkService()).thenReturn(null);
                    extension =
                            new TargetExtension(
                                    mockExtensionApi,
                                    targetState,
                                    targetPreviewManager,
                                    requestBuilder,
                                    responseParser);

                    // test
                    final Event event = prefetchContentEvent(getTargetPrefetchList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Unable to send target request, Network service is not available",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_NoRequest_When_TargetRequestBuilderIsNotAvailable() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockServiceProvider.getDeviceInfoService()).thenReturn(null);
                    extension =
                            new TargetExtension(
                                    mockExtensionApi,
                                    targetState,
                                    targetPreviewManager,
                                    null,
                                    responseParser);

                    // test
                    final Event event = prefetchContentEvent(getTargetPrefetchList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Couldn't initialize the target request builder for this request",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_NoRequest_When_RequestPayloadIsNotGenerated() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(requestBuilder.getRequestPayload(
                                    any(), any(), any(), any(), any(), any(), any()))
                            .thenReturn(null);

                    // test
                    final Event event = prefetchContentEvent(getTargetPrefetchList(1), null);
                    extension.handleTargetRequestContentEvent(event);

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Failed to generate the Target request payload",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_attachesLifecycleAndIdentityData() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    setLifecycleSharedState();
                    setIdentitySharedState();

                    // test
                    extension.handleTargetRequestContentEvent(
                            prefetchContentEvent(getTargetPrefetchList(1), null));

                    // verify
                    verify(requestBuilder)
                            .getRequestPayload(
                                    anyList(),
                                    eq(null),
                                    eq(null),
                                    anyList(),
                                    eq(""),
                                    eq(identitySharedState),
                                    eq(lifecycleSharedState));
                });
    }

    @Test
    public void testHandlePrefetchContent_when_ConnectionIsNull() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.handleTargetRequestContentEvent(
                            prefetchContentEvent(getTargetPrefetchList(1), null));

                    // verify
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());

                    // test
                    networkCallbackCaptor.getValue().call(null);

                    // verify
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Unable to open connection",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_makesCorrectNetworkRequest() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    setEventHubSharedState();

                    // test
                    extension.handleTargetRequestContentEvent(
                            prefetchContentEvent(getTargetPrefetchList(1), null));

                    // verify
                    verify(networkService)
                            .connectAsync(
                                    networkRequestCaptor.capture(),
                                    networkCallbackCaptor.capture());
                    assertEquals(
                            "https://"
                                    + MOCKED_TARGET_SERVER
                                    + "/rest/v1/delivery/?client="
                                    + MOCKED_CLIENT_CODE
                                    + "&sessionId="
                                    + MOCK_SESSION_ID,
                            networkRequestCaptor.getValue().getUrl());
                    assertEquals(HttpMethod.POST, networkRequestCaptor.getValue().getMethod());
                    final Map<String, String> headers =
                            networkRequestCaptor.getValue().getHeaders();
                    assertEquals(3, headers.size());
                    assertEquals("application/json", headers.get("Content-Type"));
                    assertEquals("AdobeTargetMobile-Android", headers.get("X-EXC-SDK"));
                    assertEquals(
                            String.format("%s+%s", "x.y.z", extension.getVersion()),
                            headers.get("X-EXC-SDK-Version"));
                    assertEquals(
                            MOCK_NETWORK_TIMEOUT,
                            networkRequestCaptor.getValue().getReadTimeout(),
                            0);
                    assertEquals(
                            MOCK_NETWORK_TIMEOUT,
                            networkRequestCaptor.getValue().getConnectTimeout(),
                            0);
                });
    }

    @Test
    public void testHandlePrefetchContent_ReturnDefaultContent_When_ResponseNot200OK() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(connecting.getResponseCode())
                            .thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

                    // test
                    extension.handleTargetRequestContentEvent(
                            prefetchContentEvent(getTargetPrefetchList(1), null));
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify default response event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Errors returned in Target response: ",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_ReturnError_when_ResponseJsonPrefetchError() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.getErrorMessage(any())).thenReturn("<error_message>");

                    // test
                    extension.handleTargetRequestContentEvent(
                            prefetchContentEvent(getTargetPrefetchList(1), null));
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Errors returned in Target response: <error_message>",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_ReturnError_when_ResponseIsNull() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(responseParser.parseResponseToJson(any())).thenReturn(null);
                    when(responseParser.getErrorMessage(any())).thenReturn("<error_message>");

                    // test
                    extension.handleTargetRequestContentEvent(
                            prefetchContentEvent(getTargetPrefetchList(1), null));
                    verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                    networkCallbackCaptor.getValue().call(connecting);

                    // verify
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    assertEquals(
                            "Null response Json <error_message>",
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_ERROR));
                    assertEquals(
                            false,
                            eventArgumentCaptor
                                    .getValue()
                                    .getEventData()
                                    .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));
                });
    }

    @Test
    public void testHandlePrefetchContent_ReturnTrue_When_ValidResponse() {
        runWithMockedServiceProvider(
                () -> {
                    final JSONObject validMboxResponse;
                    try {
                        validMboxResponse =
                                new JSONObject(
                                        "{\"options\": [{\"content\": \"mbox0content\", \"type\":"
                                                + " \"html\"}]}");
                        when(responseParser.extractPrefetchedMboxes(any()))
                                .thenReturn(
                                        new HashMap<String, JSONObject>() {
                                            {
                                                put("mbox0", validMboxResponse);
                                            }
                                        });
                        String serverResponse =
                                "{\n"
                                    + "  \"prefetch\" : {\n"
                                    + "  \"views\" : [{\n"
                                    + "     \"options\" : [{\n"
                                    + "     \"content\" : [{\n"
                                    + "     \"id\" : \"1\" ,  \n"
                                    + "      \"type\" : \"setHtml\" , \n"
                                    + "      \"selector\" : \"toolBar\", \n"
                                    + "      \"content\" : \"AAA\"  }], \n"
                                    + "    \"type\" : \"actions\" ,  \n"
                                    + "    \"eventToken\" : \"1zIHw4w+hPht4NV5MyssK4usK1YQ\"  \n"
                                    + "  }]\n"
                                    + "  }]\n"
                                    + "  }\n"
                                    + "}";
                        when(responseParser.parseResponseToJson(any()))
                                .thenReturn(new JSONObject(serverResponse));

                        // test
                        final Event event = prefetchContentEvent(getTargetPrefetchList(1), null);
                        extension.handleTargetRequestContentEvent(event);
                        verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                        networkCallbackCaptor.getValue().call(connecting);

                        // verify
                        verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                        assertEquals(
                                true,
                                eventArgumentCaptor
                                        .getValue()
                                        .getEventData()
                                        .get(TargetConstants.EventDataKeys.PREFETCH_RESULT));

                        // verify other interaction with targetState
                        verify(targetState).clearNotifications();
                        verify(targetState).updateSessionTimestamp(eq(false));
                        verify(targetState).updateEdgeHost(MOCK_EDGE_HOST);
                        verify(mockExtensionApi)
                                .createSharedState(eq(targetSharedState), eq(event));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    // **********************************************************************************************
    // handleLocationsDisplayed
    // **********************************************************************************************

    @Test
    public void testHandleLocationsDisplayed_NoRequest_When_OptOut() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_OUT);

                    // test
                    extension.handleTargetRequestContentEvent(locationsDisplayedEvent(1));

                    // verify
                    verifyNoInteractions(networkService);
                    verifyNoInteractions(mockExtensionApi);
                });
    }

    @Test
    public void testHandleLocationsDisplayed_NoRequest_When_ClientCodeIsEmpty() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getClientCode()).thenReturn("");

                    // test
                    extension.handleTargetRequestContentEvent(locationsDisplayedEvent(1));

                    // verify
                    verifyNoInteractions(networkService);
                    verifyNoInteractions(mockExtensionApi);
                });
    }

    @Test
    public void testHandleLocationsDisplayed_NoRequest_When_networkServicesIsNotAvailable() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    extension =
                            new TargetExtension(
                                    mockExtensionApi,
                                    targetState,
                                    targetPreviewManager,
                                    requestBuilder,
                                    responseParser);

                    // test
                    extension.handleTargetRequestContentEvent(locationsDisplayedEvent(1));

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, never()).createSharedState(any(), any());
                    verify(mockExtensionApi, never()).dispatch(any());
                });
    }

    @Test
    public void testHandleLocationsDisplayed_NoRequest_When_TargetRequestBuilderIsNotAvailable() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    extension =
                            new TargetExtension(
                                    mockExtensionApi,
                                    targetState,
                                    targetPreviewManager,
                                    null,
                                    responseParser);

                    // test
                    extension.handleTargetRequestContentEvent(locationsDisplayedEvent(1));

                    // verify
                    verifyNoInteractions(networkService);
                    verify(mockExtensionApi, never()).createSharedState(any(), any());
                    verify(mockExtensionApi, never()).dispatch(any());
                });
    }

    @Test
    public void testHandleLocationsDisplayed_NoRequest_When_NoMboxes() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    extension.handleTargetRequestContentEvent(locationsDisplayedEvent(0));

                    // verify
                    verifyNoInteractions(networkService);
                    verifyNoInteractions(mockExtensionApi);
                });
    }

    @Test
    public void testHandleLocationsDisplayed_sendsCorrectNetworkRequest() throws JSONException {
        runWithMockedServiceProvider(
                () -> {
                    try {
                        // setup
                        setEventHubSharedState();
                        when(targetState.getPrefetchedMbox()).thenReturn(getMboxData(3));
                        when(targetState.getLoadedMbox()).thenReturn(getMboxData(1));
                        when(responseParser.getAnalyticsForTargetPayload(any(), any()))
                                .thenReturn(a4tParams);
                        when(targetState.getNotifications())
                                .thenReturn(
                                        new ArrayList<JSONObject>() {
                                            {
                                                add(validJSONObject());
                                            }
                                        });

                        // test
                        extension.handleTargetRequestContentEvent(locationsDisplayedEvent(3));

                        // verify
                        verify(targetState, times(2)).addNotification(any());
                        verify(mockExtensionApi, times(2)).dispatch(any());
                        verify(networkService)
                                .connectAsync(
                                        networkRequestCaptor.capture(),
                                        networkCallbackCaptor.capture());
                        assertEquals(
                                "https://"
                                        + MOCKED_TARGET_SERVER
                                        + "/rest/v1/delivery/?client="
                                        + MOCKED_CLIENT_CODE
                                        + "&sessionId="
                                        + MOCK_SESSION_ID,
                                networkRequestCaptor.getValue().getUrl());
                        assertEquals(HttpMethod.POST, networkRequestCaptor.getValue().getMethod());
                        final Map<String, String> headers =
                                networkRequestCaptor.getValue().getHeaders();
                        assertEquals(3, headers.size());
                        assertEquals("application/json", headers.get("Content-Type"));
                        assertEquals("AdobeTargetMobile-Android", headers.get("X-EXC-SDK"));
                        assertEquals(
                                String.format("%s+%s", "x.y.z", extension.getVersion()),
                                headers.get("X-EXC-SDK-Version"));
                        assertEquals(
                                MOCK_NETWORK_TIMEOUT,
                                networkRequestCaptor.getValue().getReadTimeout(),
                                0);
                        assertEquals(
                                MOCK_NETWORK_TIMEOUT,
                                networkRequestCaptor.getValue().getConnectTimeout(),
                                0);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testHandleLocationsDisplayed_notificationsNotCleared_When_ConnectionIsNull()
            throws JSONException {
        runWithMockedServiceProvider(
                () -> {
                    try {
                        // test
                        when(targetState.getPrefetchedMbox()).thenReturn(getMboxData(1));
                        when(targetState.getNotifications())
                                .thenReturn(
                                        new ArrayList<JSONObject>() {
                                            {
                                                add(validJSONObject());
                                            }
                                        });

                        // verify
                        extension.handleTargetRequestContentEvent(locationsDisplayedEvent(1));
                        verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                        networkCallbackCaptor.getValue().call(null);

                        // verify that the notifications are not cleared
                        verify(targetState, never()).clearNotifications();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testHandleLocationsDisplayed_notificationsNotCleared_When_ResponseJsonIsNull()
            throws JSONException {
        runWithMockedServiceProvider(
                () -> {
                    try {
                        // test
                        when(targetState.getPrefetchedMbox()).thenReturn(getMboxData(1));
                        when(targetState.getNotifications())
                                .thenReturn(
                                        new ArrayList<JSONObject>() {
                                            {
                                                add(validJSONObject());
                                            }
                                        });
                        when(responseParser.parseResponseToJson(any())).thenReturn(null);

                        // verify
                        extension.handleTargetRequestContentEvent(locationsDisplayedEvent(1));
                        verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                        networkCallbackCaptor.getValue().call(connecting);

                        // verify that the notifications are not cleared
                        verify(targetState, never()).clearNotifications();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testHandleLocationsDisplayed_notificationsNotCleared_When_ResponseNot200OK()
            throws JSONException {
        runWithMockedServiceProvider(
                () -> {
                    try {
                        // test
                        when(targetState.getPrefetchedMbox()).thenReturn(getMboxData(1));
                        when(targetState.getNotifications())
                                .thenReturn(
                                        new ArrayList<JSONObject>() {
                                            {
                                                add(validJSONObject());
                                            }
                                        });
                        when(connecting.getResponseCode())
                                .thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

                        // verify
                        extension.handleTargetRequestContentEvent(locationsDisplayedEvent(1));
                        verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                        networkCallbackCaptor.getValue().call(connecting);

                        // verify that the notifications are not cleared
                        verify(targetState, never()).clearNotifications();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testHandleLocationsDisplayed_notificationsNotCleared_When_ResponseError()
            throws JSONException {
        runWithMockedServiceProvider(
                () -> {
                    try {
                        // test
                        when(targetState.getPrefetchedMbox()).thenReturn(getMboxData(1));
                        when(targetState.getNotifications())
                                .thenReturn(
                                        new ArrayList<JSONObject>() {
                                            {
                                                add(validJSONObject());
                                            }
                                        });
                        when(responseParser.getErrorMessage(any())).thenReturn("anyError");

                        // verify
                        extension.handleTargetRequestContentEvent(locationsDisplayedEvent(1));
                        verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                        networkCallbackCaptor.getValue().call(connecting);

                        // verify that the notifications are not cleared
                        verify(targetState, never()).clearNotifications();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testHandleLocationsDisplayed_notificationsCleared_When_validResponse()
            throws JSONException {
        runWithMockedServiceProvider(
                () -> {
                    try {
                        // test
                        when(targetState.getPrefetchedMbox()).thenReturn(getMboxData(1));
                        when(targetState.getNotifications())
                                .thenReturn(
                                        new ArrayList<JSONObject>() {
                                            {
                                                add(validJSONObject());
                                            }
                                        });

                        // verify
                        extension.handleTargetRequestContentEvent(locationsDisplayedEvent(1));
                        verify(networkService).connectAsync(any(), networkCallbackCaptor.capture());
                        networkCallbackCaptor.getValue().call(connecting);

                        // verify that the notifications are not cleared
                        verify(targetState).clearNotifications();
                        verify(targetState).updateEdgeHost(any());
                        verify(targetState).updateSessionTimestamp(eq(false));
                        verify(mockExtensionApi).createSharedState(any(), any());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    // **********************************************************************************************
    // handleLocationClicked
    // **********************************************************************************************
    @Test
    public void testHandleLocationsClicked_NoRequest_When_OptOut() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_OUT);

                    // verify
                    extension.handleTargetRequestContentEvent(locationsClickedEvent("mbox1"));

                    // verify network call not made
                    verifyNoInteractions(networkService);
                });
    }

    @Test
    public void testHandleLocationsClicked_NoRequest_When_ClientCodeIsEmpty() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    when(targetState.getClientCode()).thenReturn("");

                    // verify
                    extension.handleTargetRequestContentEvent(locationsClickedEvent("mbox1"));

                    // verify network call not made
                    verifyNoInteractions(networkService);
                });
    }

    @Test
    public void testHandleLocationsClicked_NoRequest_When_mboxNull() {
        runWithMockedServiceProvider(
                () -> {
                    // verify
                    extension.handleTargetRequestContentEvent(locationsClickedEvent(null));

                    // verify network call not made
                    verifyNoInteractions(networkService);
                });
    }

    @Test
    public void testHandleLocationsClicked_NoRequest_When_mboxNameEmpty() {
        runWithMockedServiceProvider(
                () -> {
                    // verify
                    extension.handleTargetRequestContentEvent(locationsClickedEvent(""));

                    // verify network call not made
                    verifyNoInteractions(networkService);
                });
    }

    @Test
    public void testHandleLocationsClicked_NoRequest_When_mboxNotPresentInPrefetchedOrLoadedMbox() {
        runWithMockedServiceProvider(
                () -> {
                    // verify
                    extension.handleTargetRequestContentEvent(locationsClickedEvent("mbox1"));

                    // verify network call not made
                    verifyNoInteractions(networkService);
                });
    }

    @Test
    public void testHandleLocationsClicked_NoRequest_when_clickMetricNotAvailable()
            throws JSONException {
        runWithMockedServiceProvider(
                () -> {
                    try {
                        // setup
                        when(targetState.getPrefetchedMbox()).thenReturn(getMboxData(1));
                        when(responseParser.getClickMetric(any())).thenReturn(null);

                        // verify
                        extension.handleTargetRequestContentEvent(locationsClickedEvent("mbox1"));

                        // verify network call not made
                        verifyNoInteractions(networkService);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testHandleLocationsClicked_sendsCorrectData() throws JSONException {
        runWithMockedServiceProvider(
                () -> {
                    try {
                        // setup
                        setEventHubSharedState();
                        when(targetState.getPrefetchedMbox()).thenReturn(getMboxData(1));
                        when(responseParser.getClickMetric(any())).thenReturn(validJSONObject());
                        when(requestBuilder.getClickNotificationJsonObject(
                                        any(), any(), anyLong(), any()))
                                .thenReturn(validJSONObject());
                        when(responseParser.getAnalyticsForTargetPayload(any(), any()))
                                .thenReturn(a4tParams);
                        when(responseParser.preprocessAnalyticsForTargetPayload(any(), any()))
                                .thenReturn(a4tParams);

                        // verify
                        extension.handleTargetRequestContentEvent(locationsClickedEvent("mbox0"));

                        // verify network
                        // verify notifications are added
                        // verify analytics4target event is dispatched
                        verify(targetState, times(1)).addNotification(any());
                        verify(mockExtensionApi, times(1)).dispatch(any());
                        verify(networkService)
                                .connectAsync(
                                        networkRequestCaptor.capture(),
                                        networkCallbackCaptor.capture());
                        assertEquals(
                                "https://"
                                        + MOCKED_TARGET_SERVER
                                        + "/rest/v1/delivery/?client="
                                        + MOCKED_CLIENT_CODE
                                        + "&sessionId="
                                        + MOCK_SESSION_ID,
                                networkRequestCaptor.getValue().getUrl());
                        assertEquals(HttpMethod.POST, networkRequestCaptor.getValue().getMethod());
                        final Map<String, String> headers =
                                networkRequestCaptor.getValue().getHeaders();
                        assertEquals(3, headers.size());
                        assertEquals("application/json", headers.get("Content-Type"));
                        assertEquals("AdobeTargetMobile-Android", headers.get("X-EXC-SDK"));
                        assertEquals(
                                String.format("%s+%s", "x.y.z", extension.getVersion()),
                                headers.get("X-EXC-SDK-Version"));
                        assertEquals(
                                MOCK_NETWORK_TIMEOUT,
                                networkRequestCaptor.getValue().getReadTimeout(),
                                0);
                        assertEquals(
                                MOCK_NETWORK_TIMEOUT,
                                networkRequestCaptor.getValue().getConnectTimeout(),
                                0);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    // **********************************************************************************************
    // handleConfigurationResponseContentEvent
    // **********************************************************************************************

    @Test
    public void testHandleConfigurationResponseContentEvent_whenPrivacyOptedOut() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_OUT);

                    // test
                    final Event event = noEventDataEvent();
                    extension.handleConfigurationResponseContentEvent(event);

                    // verify
                    verify(targetState, times(2)).updateEdgeHost(null);
                    verify(targetState).resetSession();
                    ;
                    verify(targetState).updateTntId(null);
                    verify(targetState).updateThirdPartyId(null);
                    verify(mockExtensionApi).createSharedState(eq(targetSharedState), eq(event));
                });
    }

    @Test
    public void testHandleConfigurationResponseContentEvent_whenPrivacyOptedIn() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getMobilePrivacyStatus())
                            .thenReturn(MobilePrivacyStatus.OPT_IN);

                    // test
                    extension.handleConfigurationResponseContentEvent(noEventDataEvent());

                    // verify
                    verify(targetState, never()).updateEdgeHost(any());
                    verify(targetState, never()).resetSession();
                    verify(targetState, never()).updateTntId(any());
                    verify(targetState, never()).updateThirdPartyId(any());
                    verify(mockExtensionApi, never()).createSharedState(any(), any());
                });
    }

    // **********************************************************************************************
    // TargetIdentitiesGetter
    // **********************************************************************************************
    @Test
    public void testGetTargetIdentities_whenAllIDPresent() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getThirdPartyId()).thenReturn(MOCK_THIRD_PARTY_ID);
                    when(targetState.getTntId()).thenReturn(MOCK_TNT_ID);
                    when(targetState.getSessionId()).thenReturn(MOCK_SESSION_ID);

                    // test
                    extension.handleTargetRequestIdentityEvent(noEventDataEvent());

                    // verify
                    verify(mockExtensionApi).dispatch(eventArgumentCaptor.capture());
                    final Event capturedEvent = eventArgumentCaptor.getValue();
                    assertEquals(EventType.TARGET, capturedEvent.getType());
                    assertEquals(EventSource.RESPONSE_IDENTITY, capturedEvent.getSource());
                    assertEquals(EventType.TARGET, capturedEvent.getType());
                    assertEquals(
                            MOCK_THIRD_PARTY_ID,
                            capturedEvent.getEventData().get(EventDataKeys.THIRD_PARTY_ID));
                    assertEquals(
                            MOCK_TNT_ID, capturedEvent.getEventData().get(EventDataKeys.TNT_ID));
                    assertEquals(
                            MOCK_SESSION_ID,
                            capturedEvent.getEventData().get(EventDataKeys.SESSION_ID));
                });
    }

    @Test
    public void testGetTargetIdentities_whenNullTntIDAndThirdPartyID() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(targetState.getThirdPartyId()).thenReturn(null);
                    when(targetState.getTntId()).thenReturn(null);
                    when(targetState.getSessionId()).thenReturn(null);

                    // test
                    extension.handleTargetRequestIdentityEvent(noEventDataEvent());

                    // verify
                    verify(mockExtensionApi).dispatch(eventArgumentCaptor.capture());
                    final Event capturedEvent = eventArgumentCaptor.getValue();
                    assertEquals(EventType.TARGET, capturedEvent.getType());
                    assertEquals(EventSource.RESPONSE_IDENTITY, capturedEvent.getSource());
                    assertEquals(EventType.TARGET, capturedEvent.getType());
                    assertFalse(
                            capturedEvent.getEventData().containsKey(EventDataKeys.THIRD_PARTY_ID));
                    assertFalse(capturedEvent.getEventData().containsKey(EventDataKeys.TNT_ID));
                    assertEquals(
                            targetState.getSessionId(),
                            capturedEvent.getEventData().get(EventDataKeys.SESSION_ID));
                });
    }

    // ========================================================================================
    // Private Helper methods
    // ========================================================================================
    private void setConfigurationSharedState(
            final String clientCode,
            final Integer timeout,
            final String privacyStatus,
            final boolean previewEnabled,
            final String targetServer) {
        HashMap<String, Object> configData = new HashMap<>();
        configData.put("target.clientCode", clientCode);
        configData.put("target.timeout", timeout);
        configData.put("global.privacy", MobilePrivacyStatus.fromString(privacyStatus).getValue());
        configData.put("target.previewEnabled", previewEnabled);
        configData.put("target.server", targetServer);

        when(mockExtensionApi.getSharedState(
                        eq("com.adobe.module.configuration"), any(), anyBoolean(), any()))
                .thenReturn(new SharedStateResult(SharedStateStatus.SET, configData));
    }

    private void setLifecycleSharedState() {
        HashMap<String, Object> lifecycleData = new HashMap<>();
        lifecycleData.put("lifecyclecontextdata", lifecycleSharedState);
        when(mockExtensionApi.getSharedState(
                        eq("com.adobe.module.lifecycle"), any(), anyBoolean(), any()))
                .thenReturn(new SharedStateResult(SharedStateStatus.SET, lifecycleData));
    }

    private void setIdentitySharedState() {
        when(mockExtensionApi.getSharedState(
                        eq("com.adobe.module.identity"), any(), anyBoolean(), any()))
                .thenReturn(new SharedStateResult(SharedStateStatus.SET, identitySharedState));
    }

    private void setEventHubSharedState() {
        setEventHubSharedState(eventHubSharedState);
    }

    private void setEventHubSharedState(final Map<String, Object> sharedState) {
        when(mockExtensionApi.getSharedState(
                        eq("com.adobe.module.eventhub"), any(), anyBoolean(), any()))
                .thenReturn(new SharedStateResult(SharedStateStatus.SET, sharedState));
    }

    Map<String, Object> getTargetRawRequestForExecute(final int count) {
        if (count == 0) {
            return new HashMap<>();
        }

        final List<Map<String, Object>> executeMboxes = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final int mboxIndex = i;
            final String mboxName = "mbox" + i;

            final Map<String, Object> executeMbox =
                    new HashMap<String, Object>() {
                        {
                            put("index", mboxIndex);
                            put("name", mboxName);
                        }
                    };
            executeMboxes.add(executeMbox);
        }

        final Map<String, Object> request =
                new HashMap<String, Object>() {
                    {
                        put(
                                "execute",
                                new HashMap<String, Object>() {
                                    {
                                        put("mboxes", executeMboxes);
                                    }
                                });
                    }
                };
        return request;
    }

    Map<String, Object> getTargetRawRequestForPrefetch(int count) {
        final List<Map<String, Object>> executeMboxes = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final int mboxIndex = i;
            final String mboxName = "mbox" + i;

            final Map<String, Object> executeMbox =
                    new HashMap<String, Object>() {
                        {
                            put("index", mboxIndex);
                            put("name", mboxName);
                        }
                    };
            executeMboxes.add(executeMbox);
        }

        final Map<String, Object> request =
                new HashMap<String, Object>() {
                    {
                        put(
                                "prefetch",
                                new HashMap<String, Object>() {
                                    {
                                        put("mboxes", executeMboxes);
                                    }
                                });
                    }
                };
        return request;
    }

    Map<String, Object> getTargetRawRequestForNotifications(int count) {
        final List<Map<String, Object>> notifications = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final String notificationId = String.valueOf(i);
            ;
            final String mboxName = "mbox" + i;
            final Map<String, Object> notification =
                    new HashMap<String, Object>() {
                        {
                            put("id", notificationId);
                            put("timestamp", (long) (System.currentTimeMillis()));
                            put("type", "click");
                            put(
                                    "mbox",
                                    new HashMap<String, Object>() {
                                        {
                                            put("name", mboxName);
                                        }
                                    });
                            put(
                                    "tokens",
                                    new ArrayList<String>() {
                                        {
                                            add("randomToken");
                                        }
                                    });
                        }
                    };
            notifications.add(notification);
        }

        final Map<String, Object> request =
                new HashMap<String, Object>() {
                    {
                        put("notifications", notifications);
                    }
                };
        return request;
    }

    List<TargetRequest> getTargetRequestList(int count) {
        List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

        for (int i = 0; i < count; i++) {
            final String mboxName = "mbox" + i;
            TargetRequest targetRequest =
                    new TargetRequest(
                            mboxName,
                            null,
                            "default",
                            new AdobeCallback<String>() {
                                @Override
                                public void call(String value) {}
                            });
            targetRequestList.add(targetRequest);
        }

        return targetRequestList;
    }

    List<TargetRequest> getTargetRequestListWithContentWithDataCallback(int count) {
        List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

        for (int i = 0; i < count; i++) {
            final String mboxName = "mbox" + i;
            TargetRequest targetRequest =
                    new TargetRequest(
                            mboxName,
                            null,
                            "default",
                            new AdobeTargetDetailedCallback() {
                                @Override
                                public void call(String content, Map<String, Object> data) {}

                                @Override
                                public void fail(AdobeError var1) {}
                            });
            targetRequestList.add(targetRequest);
        }

        return targetRequestList;
    }

    List<TargetPrefetch> getTargetPrefetchList(int count) {
        List<TargetPrefetch> targetPrefetchList = new ArrayList<TargetPrefetch>();

        for (int i = 0; i < count; i++) {
            final String mboxName = "mbox" + i;
            TargetPrefetch targetPrefetch = new TargetPrefetch(mboxName, null);
            targetPrefetchList.add(targetPrefetch);
        }

        return targetPrefetchList;
    }

    // ========================================================================================
    // Sample Events
    // ========================================================================================

    private Event noEventDataEvent() {
        Event testEvent = new Event.Builder("Test", EventType.TARGET, EventSource.NONE).build();
        return testEvent;
    }

    private Event loadRequestEvent(
            final List<TargetRequest> targetRequestList, final TargetParameters parameters) {
        final List<TargetRequest> mboxRequestListCopy = new ArrayList<>(targetRequestList);
        final List<Map<String, Object>> flattenedLocationRequests = new ArrayList<>();
        final Map<String, TargetRequest> tempIdToRequestMap = new HashMap<>();
        for (final TargetRequest request : mboxRequestListCopy) {
            if (request == null) {
                continue;
            }
            final String responsePairId = UUID.randomUUID().toString();
            request.setResponsePairId(responsePairId);

            tempIdToRequestMap.put(responsePairId, request);
            flattenedLocationRequests.add(request.toEventData());
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.LOAD_REQUEST, flattenedLocationRequests);
        if (parameters != null) {
            eventData.put(EventDataKeys.TARGET_PARAMETERS, parameters.toEventData());
        }

        final Event event =
                new Event.Builder(
                                EventName.LOAD_REQUEST,
                                EventType.TARGET,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        return event;
    }

    private Event prefetchContentEvent(
            final List<TargetPrefetch> targetPrefetchList, final TargetParameters parameters) {
        final List<TargetPrefetch> prefetchRequestListCopy = new ArrayList<>(targetPrefetchList);
        final List<Map<String, Object>> flattenedPrefetchRequests = new ArrayList<>();
        for (final TargetPrefetch request : prefetchRequestListCopy) {
            if (request == null) {
                continue;
            }
            flattenedPrefetchRequests.add(request.toEventData());
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.PREFETCH, flattenedPrefetchRequests);
        if (parameters != null) {
            eventData.put(EventDataKeys.TARGET_PARAMETERS, parameters.toEventData());
        }

        final Event event =
                new Event.Builder(
                                EventName.PREFETCH_REQUEST,
                                EventType.TARGET,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        return event;
    }

    private Event rawRequestExecuteEvent(final int count) {
        final Map<String, Object> eventData = new HashMap<>(getTargetRawRequestForExecute(count));
        eventData.put(EventDataKeys.IS_RAW_EVENT, true);

        final Event event =
                new Event.Builder(
                                EventName.TARGET_RAW_REQUEST,
                                EventType.TARGET,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        return event;
    }

    private Event getTargetRawRequestForNotificationsEvent(final int count) {
        final Map<String, Object> eventData =
                new HashMap<>(getTargetRawRequestForNotifications(count));
        eventData.put(EventDataKeys.IS_RAW_EVENT, true);

        final Event event =
                new Event.Builder(
                                EventName.TARGET_RAW_REQUEST,
                                EventType.TARGET,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        return event;
    }

    private Event setThirdPartyIdEvent(final String thirdPartyId) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.THIRD_PARTY_ID, thirdPartyId);

        return new Event.Builder(
                        EventName.SET_THIRD_PARTY_ID,
                        EventType.TARGET,
                        EventSource.REQUEST_IDENTITY)
                .setEventData(eventData)
                .build();
    }

    private Event setTntIdEvent(final String tntId) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.TNT_ID, tntId);

        return new Event.Builder(
                        EventName.SET_TNT_ID, EventType.TARGET, EventSource.REQUEST_IDENTITY)
                .setEventData(eventData)
                .build();
    }

    private Event setSessionIdEvent(final String sessionId) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.SESSION_ID, sessionId);

        return new Event.Builder(
                        EventName.SET_SESSION_ID, EventType.TARGET, EventSource.REQUEST_IDENTITY)
                .setEventData(eventData)
                .build();
    }

    private Event previewDeeplinkEvent(final String deeplink) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.DEEPLINK, deeplink);

        return new Event.Builder("OS Deeplink Event", EventType.GENERIC_DATA, EventSource.OS)
                .setEventData(eventData)
                .build();
    }

    public static Event previewRestartDeeplinkEvent(final String deepLink) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.PREVIEW_RESTART_DEEP_LINK, deepLink);

        return new Event.Builder(
                        EventName.SET_PREVIEW_DEEPLINK,
                        EventType.TARGET,
                        EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();
    }

    private Event resetExperienceEvent() {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.RESET_EXPERIENCE, true);

        final Event event =
                new Event.Builder(
                                EventName.REQUEST_RESET,
                                EventType.TARGET,
                                EventSource.REQUEST_RESET)
                        .setEventData(eventData)
                        .build();
        return event;
    }

    private Event clearPrefetchCacheEvent() {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.CLEAR_PREFETCH_CACHE, true);

        final Event event =
                new Event.Builder(
                                EventName.CLEAR_PREFETCH_CACHE,
                                EventType.TARGET,
                                EventSource.REQUEST_RESET)
                        .setEventData(eventData)
                        .build();

        return event;
    }

    private Event locationsDisplayedEvent(final int mboxCount) {
        ArrayList<String> mboxes = new ArrayList<>();
        for (int i = 0; i < mboxCount; i++) {
            mboxes.add("mbox" + i);
        }
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.IS_LOCATION_DISPLAYED, true);
        eventData.put(EventDataKeys.MBOX_NAMES, mboxes);
        eventData.put(EventDataKeys.TARGET_PARAMETERS, targetParameters);

        return new Event.Builder(
                        EventName.LOCATIONS_DISPLAYED,
                        EventType.TARGET,
                        EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();
    }

    private Event locationsClickedEvent(final String mboxName) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(EventDataKeys.IS_LOCATION_CLICKED, true);
        eventData.put(EventDataKeys.MBOX_NAME, mboxName);
        eventData.put(EventDataKeys.TARGET_PARAMETERS, targetParameters);

        return new Event.Builder(
                        EventName.LOCATION_CLICKED, EventType.TARGET, EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();
    }

    // ========================================================================================
    // Private Helper methods
    // ========================================================================================

    private String extractMboxContentFromEvent(final Event event) {
        return DataReader.optString(event.getEventData(), EventDataKeys.TARGET_CONTENT, "");
    }

    private Map<String, String> extractResponseToken(final Event event) {
        Map<String, Map> data =
                DataReader.optTypedMap(
                        Map.class, event.getEventData(), EventDataKeys.TARGET_DATA_PAYLOAD, null);
        return data.get(EventDataKeys.RESPONSE_TOKENS);
    }

    private Map<String, String> extractAnalyticsPayload(final Event event) {
        Map<String, Map> data =
                DataReader.optTypedMap(
                        Map.class, event.getEventData(), EventDataKeys.TARGET_DATA_PAYLOAD, null);
        return data.get(EventDataKeys.ANALYTICS_PAYLOAD);
    }

    private Map<String, String> extractClickMetric(final Event event) {
        Map<String, Map> data =
                DataReader.optTypedMap(
                        Map.class, event.getEventData(), EventDataKeys.TARGET_DATA_PAYLOAD, null);
        return data.get(EventDataKeys.CLICK_METRIC_ANALYTICS_PAYLOAD);
    }

    private Map<String, JSONObject> getMboxData(final int count) throws JSONException {
        Map<String, JSONObject> mboxData = new HashMap<>();
        for (int i = 0; i < count; i++) {
            mboxData.put("mbox" + i, validJSONObject());
        }
        return mboxData;
    }

    private JSONObject validJSONObject() throws JSONException {
        return new JSONObject("{\"test\":\"value\"}");
    }

    static final class EventDataKeys {
        static final String MBOX_NAME = "name";
        static final String MBOX_NAMES = "names";
        static final String TARGET_PARAMETERS = "targetparams";
        static final String EXECUTE = "execute";
        static final String PREFETCH = "prefetch";
        static final String LOAD_REQUEST = "request";
        static final String PREFETCH_ERROR = "prefetcherror";
        static final String IS_LOCATION_DISPLAYED = "islocationdisplayed";
        static final String IS_LOCATION_CLICKED = "islocationclicked";
        static final String THIRD_PARTY_ID = "thirdpartyid";
        static final String TNT_ID = "tntid";
        static final String SESSION_ID = "sessionid";
        static final String RESET_EXPERIENCE = "resetexperience";
        static final String CLEAR_PREFETCH_CACHE = "clearcache";
        static final String PREVIEW_RESTART_DEEP_LINK = "restartdeeplink";
        static final String DEEPLINK = "deeplink";
        static final String IS_RAW_EVENT = "israwevent";
        static final String NOTIFICATIONS = "notifications";
        static final String RESPONSE_DATA = "responsedata";
        static final String TARGET_RESPONSE_EVENT_ID = "responseEventId";
        static final String TARGET_RESPONSE_PAIR_ID = "responsePairId";
        static final String ANALYTICS_PAYLOAD = "analytics.payload";
        static final String RESPONSE_TOKENS = "responseTokens";
        static final String CLICK_METRIC_ANALYTICS_PAYLOAD = "clickmetric.analytics.payload";
        static final String TARGET_CONTENT = "content";
        static final String TARGET_DATA_PAYLOAD = "data";

        private EventDataKeys() {}
    }

    static final class EventName {
        static final String PREFETCH_REQUEST = "TargetPrefetchRequest";
        static final String LOAD_REQUEST = "TargetLoadRequest";
        static final String LOCATIONS_DISPLAYED = "TargetLocationsDisplayed";
        static final String LOCATION_CLICKED = "TargetLocationClicked";
        static final String TARGET_REQUEST_RESPONSE = "TargetRequestResponse";
        static final String GET_THIRD_PARTY_ID = "TargetGetThirdPartyIdentifier";
        static final String SET_THIRD_PARTY_ID = "TargetSetThirdPartyIdentifier";
        static final String GET_TNT_ID = "TargetGetTnTIdentifier";
        static final String SET_TNT_ID = "TargetSetTnTIdentifier";
        static final String GET_SESSION_ID = "TargetGetSessionIdentifier";
        static final String SET_SESSION_ID = "TargetSetSessionIdentifier";
        static final String REQUEST_RESET = "TargetRequestReset";
        static final String CLEAR_PREFETCH_CACHE = "TargetClearPrefetchCache";
        static final String SET_PREVIEW_DEEPLINK = "TargetSetPreviewRestartDeeplink";
        static final String TARGET_RAW_REQUEST = "TargetRawRequest";
        static final String TARGET_RAW_NOTIFICATIONS = "TargetRawNotifications";

        private EventName() {}
    }
}
