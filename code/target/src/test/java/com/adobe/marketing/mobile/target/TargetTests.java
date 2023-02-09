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

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Target;
import com.adobe.marketing.mobile.services.Log;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings("unchecked")
public class TargetTests {
    private String response;
    private Map<String, Object> responseMap;
    private AdobeError responseError;

    @Mock
    private Uri uri;

    @Before
    public void setup() throws Exception {
    }

    @After
    public void teardown() {
        response = null;
        responseMap = null;
        responseError = null;
    }

    @Test
    public void test_extensionVersion() {
        // test
        final String extensionVersion = Target.extensionVersion();
        assertEquals("extensionVersion API should return the correct version string.", "2.0.0",
                extensionVersion);
    }

    @Test
    public void testPrefetchContent_validprefetchList() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            final List<TargetPrefetch> prefetchList = new ArrayList<>();
            prefetchList.add(new TargetPrefetch("mbox1", null));
            prefetchList.add(new TargetPrefetch("mbox2", null));

            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.prefetchContent(prefetchList, targetParameters, new AdobeCallback<String>() {
                @Override
                public void call(String value) {
                    response = value;
                }
            });

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEventWithResponseCallback(eventCaptor.capture(), anyLong(), callbackCaptor.capture()));
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetPrefetchRequest", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            List<Map<String, Object>> prefetchData = (List<Map<String, Object>>) eventData.get("prefetch");
            assertNotNull(prefetchData);
            final Map<String, Object> prefetchMbox1 = prefetchData.get(0);
            assertNotNull(prefetchMbox1);
            assertEquals("mbox1", prefetchMbox1.get("name"));
            final Map<String, Object> prefetchMbox2 = prefetchData.get(1);
            assertNotNull(prefetchMbox2);
            assertEquals("mbox2", prefetchMbox2.get("name"));
            final Map<String, Object> targetParams = (Map<String, Object>) eventData.get("targetparams");
            assertNotNull(targetParams);
            final Map<String, String> mboxParameters = (Map<String, String>) targetParams.get("parameters");
            assertNotNull(mboxParameters);
            assertEquals("mbox_parameter_value", mboxParameters.get("mbox_parameter_key"));

            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put("prefetcherror", null);
            final Event responseEvent = new Event.Builder("TargetRequestResponse", "com.adobe.eventType.target", "com.adobe.eventSource.responseContent")
                    .setEventData(responseEventData).build();
            callbackWithError.call(responseEvent);

            assertNull(response);
        }
    }

    @Test
    public void testPrefetchContent_invalidPrefetchList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<TargetPrefetch> prefetchList = new ArrayList<>();
            prefetchList.add(null);

            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.prefetchContent(prefetchList, targetParameters, new AdobeCallback<String>() {
                @Override
                public void call(String value) {
                    response = value;
                }
            });

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
            assertNotNull(response);
        }
    }

    @Test
    public void testPrefetchContent_invalidPrefetchListWithErrorCallback() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<TargetPrefetch> prefetchList = new ArrayList<>();
            prefetchList.add(null);

            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.prefetchContent(prefetchList, targetParameters, new AdobeCallbackWithError<String>() {
                @Override
                public void fail(AdobeError error) {
                    responseError = error;
                }

                @Override
                public void call(String value) {
                    response = value;
                }
            });

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
            assertNotNull(responseError);
        }
    }

    @Test
    public void testPrefetchContent_emptyPrefetchList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<TargetPrefetch> prefetchList = new ArrayList<>();
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.prefetchContent(prefetchList, targetParameters, new AdobeCallback<String>() {
                @Override
                public void call(String value) {
                    response = value;
                }
            });

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
            assertNotNull(response);
        }
    }

    @Test
    public void testPrefetchContent_emptyPrefetchListWithErrorCallback() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<TargetPrefetch> prefetchList = new ArrayList<>();
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.prefetchContent(prefetchList, targetParameters, new AdobeCallbackWithError<String>() {
                @Override
                public void fail(AdobeError error) {
                    responseError = error;
                }

                @Override
                public void call(String value) {
                    response = value;
                }
            });

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
            assertNotNull(responseError);
        }
    }

    @Test
    public void testRetrieveLocationContent_validRequestList() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            final List<TargetRequest> requestList = new ArrayList<>();
            requestList.add(new TargetRequest("mbox1", null, "defaultContent1", new AdobeCallback<String>() {
                @Override
                public void call(String value) {
                }
            }));
            requestList.add(new TargetRequest("mbox2", null, "defaultContent2", new AdobeCallback<String>() {
                @Override
                public void call(String value) {
                }
            }));

            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.retrieveLocationContent(requestList, targetParameters);

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event event = eventCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetLoadRequest", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            List<Map<String, Object>> requestData = (List<Map<String, Object>>) eventData.get("request");
            assertNotNull(requestData);
            final Map<String, Object> requestMbox1 = requestData.get(0);
            assertNotNull(requestMbox1);
            assertEquals("mbox1", requestMbox1.get("name"));
            assertEquals("defaultContent1", requestMbox1.get("defaultContent"));
            assertNotNull(requestMbox1.get("responsePairId"));
            assertNull(requestMbox1.get("targetparams"));
            final Map<String, Object> requestMbox2 = requestData.get(1);
            assertNotNull(requestMbox2);
            assertEquals("mbox2", requestMbox2.get("name"));
            assertEquals("defaultContent2", requestMbox2.get("defaultContent"));
            assertNotNull(requestMbox2.get("responsePairId"));
            assertNull(requestMbox2.get("targetparams"));
            final Map<String, Object> targetParams = (Map<String, Object>) eventData.get("targetparams");
            assertNotNull(targetParams);
            final Map<String, String> mboxParameters = (Map<String, String>) targetParams.get("parameters");
            assertNotNull(mboxParameters);
            assertEquals("mbox_parameter_value", mboxParameters.get("mbox_parameter_key"));
        }
    }

    @Test
    public void testRetrieveLocationContent_invalidRequestList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<TargetRequest> requestList = new ArrayList<>();
            requestList.add(null);
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.retrieveLocationContent(requestList, targetParameters);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
        }
    }

    @Test
    public void testRetrieveLocationContent_emptyRequestList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<TargetRequest> requestList = new ArrayList<>();
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.retrieveLocationContent(requestList, targetParameters);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
        }
    }

    @Test
    public void testRetrieveLocationContent_emptyMboxNameInRequestList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<TargetRequest> requestList = new ArrayList<>();
            requestList.add(new TargetRequest("", null, "defaultContent", new AdobeCallback<String>() {
                @Override
                public void call(String value) {
                    response = value;
                }
            }));
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.retrieveLocationContent(requestList, targetParameters);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()),
                    Mockito.times(2));
            assertEquals("defaultContent", response);
        }
    }

    @Test
    public void testRetrieveLocationContent_emptyMboxNameInRequestListWithErrorCallback() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<TargetRequest> requestList = new ArrayList<>();
            requestList.add(new TargetRequest("", null, "defaultContent", new AdobeCallbackWithError<String>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError = adobeError;
                }

                @Override
                public void call(String value) {
                    response = value;
                }
            }));
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.retrieveLocationContent(requestList, targetParameters);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()),
                    Mockito.times(2));
            assertNull(responseError);
            assertEquals("defaultContent", response);
        }
    }

    @Test
    public void testRetrieveLocationContent_emptyMboxNameInRequestListWithDataCallback() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<TargetRequest> requestList = new ArrayList<>();
            requestList.add(new TargetRequest("", null, "defaultContent", new AdobeTargetDetailedCallback() {
                @Override
                public void call(String content, Map<String, Object> data) {
                    response = content;
                    responseMap = data;
                }

                @Override
                public void fail(AdobeError error) {
                    responseError = error;
                }
            }));
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();

            Target.retrieveLocationContent(requestList, targetParameters);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()),
                    Mockito.times(2));
            assertNull(responseError);
            assertNull(responseMap);
            assertEquals("defaultContent", response);
        }
    }

    @Test
    public void testDisplayedLocations_validMboxesList() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            final List<String> mboxes = new ArrayList<String>();
            mboxes.add("mbox1");
            mboxes.add("mbox2");

            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();
            Target.displayedLocations(mboxes, targetParameters);

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event event = eventCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetLocationsDisplayed", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            assertEquals(true, eventData.get("islocationdisplayed"));
            final List<String> mboxesList = (List<String>) eventData.get("names");
            assertEquals(2, mboxesList.size());
            assertEquals("mbox1", mboxesList.get(0));
            assertEquals("mbox2", mboxesList.get(1));
            final Map<String, Object> targetParams = (Map<String, Object>) eventData.get("targetparams");
            assertNotNull(targetParams);
            final Map<String, String> mboxParameters = (Map<String, String>) targetParams.get("parameters");
            assertNotNull(mboxParameters);
            assertEquals(1, mboxParameters.size());
            assertEquals("mbox_parameter_value", mboxParameters.get("mbox_parameter_key"));
        }
    }

    @Test
    public void testDisplayedLocations_nullMboxesList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<String> mboxes = null;
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();
            Target.displayedLocations(mboxes, targetParameters);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
        }
    }

    @Test
    public void testDisplayedLocations_emptyMboxesList() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final List<String> mboxes = new ArrayList<>();
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();
            Target.displayedLocations(mboxes, targetParameters);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
        }
    }

    @Test
    public void testClickedLocation_validMbox() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            final String mbox = "mbox1";
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();
            Target.clickedLocation(mbox, targetParameters);

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event event = eventCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetLocationClicked", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            assertEquals(true, eventData.get("islocationclicked"));
            final String mboxName = (String) eventData.get("name");
            assertNotNull(mboxName);
            assertEquals("mbox1", mboxName);
            final Map<String, Object> targetParams = (Map<String, Object>) eventData.get("targetparams");
            assertNotNull(targetParams);
            final Map<String, String> mboxParameters = (Map<String, String>) targetParams.get("parameters");
            assertNotNull(mboxParameters);
            assertEquals(1, mboxParameters.size());
            assertEquals("mbox_parameter_value", mboxParameters.get("mbox_parameter_key"));
        }
    }

    @Test
    public void testClickedLocation_nullMbox() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final String mbox = null;
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();
            Target.clickedLocation(mbox, targetParameters);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
        }
    }

    @Test
    public void testClickedLocation_emptyMbox() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final String mbox = "";
            final TargetParameters targetParameters = new TargetParameters.Builder()
                    .parameters(new HashMap<String, String>() {
                        {
                            put("mbox_parameter_key", "mbox_parameter_value");
                        }
                    }).build();
            Target.clickedLocation(mbox, targetParameters);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
        }
    }

    @Test
    public void testGetThirdPartyId() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            Target.getThirdPartyId(new AdobeCallbackWithError<String>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError = adobeError;
                }

                @Override
                public void call(String value) {
                    response = value;
                }
            });

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEventWithResponseCallback(eventCaptor.capture(), anyLong(), callbackCaptor.capture()));
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetGetThirdPartyIdentifier", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestIdentity", event.getSource());

            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put("thirdpartyid", "someThirdPartryId");
            final Event responseEvent = new Event.Builder("TargetIdentity", "com.adobe.eventType.target", "com.adobe.eventSource.responseIdentity")
                    .setEventData(responseEventData).build();
            callbackWithError.call(responseEvent);

            assertNull(responseError);
            assertEquals("someThirdPartryId", response);
        }
    }

    @Test
    public void testSetThirdPartyId() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            final String thirdPartyId = "myThirdPartyId";
            Target.setThirdPartyId(thirdPartyId);

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event event = eventCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetSetThirdPartyIdentifier", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestIdentity", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            assertEquals("myThirdPartyId", eventData.get("thirdpartyid"));
        }
    }

    @Test
    public void testGetTntId() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            Target.getTntId(new AdobeCallbackWithError<String>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError = adobeError;
                }

                @Override
                public void call(String value) {
                    response = value;
                }
            });

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEventWithResponseCallback(eventCaptor.capture(), anyLong(), callbackCaptor.capture()));
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetGetTnTIdentifier", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestIdentity", event.getSource());

            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put("tntid", "someTntId");
            final Event responseEvent = new Event.Builder("TargetIdentity", "com.adobe.eventType.target", "com.adobe.eventSource.responseIdentity")
                    .setEventData(responseEventData).build();
            callbackWithError.call(responseEvent);

            assertNull(responseError);
            assertEquals("someTntId", response);
        }
    }

    @Test
    public void testSetTntId() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            final String tntId = "myTntId";
            Target.setTntId(tntId);

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event event = eventCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetSetTnTIdentifier", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestIdentity", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            assertEquals("myTntId", eventData.get("tntid"));
        }
    }

    @Test
    public void testGetSessionId() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            Target.getSessionId(new AdobeCallbackWithError<String>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError = adobeError;
                }

                @Override
                public void call(String value) {
                    response = value;
                }
            });

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEventWithResponseCallback(eventCaptor.capture(), anyLong(), callbackCaptor.capture()));
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetGetSessionIdentifier", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestIdentity", event.getSource());

            final Map<String, Object> responseEventData = new HashMap<>();
            responseEventData.put("sessionid", "someSessionId");
            final Event responseEvent = new Event.Builder("TargetIdentity", "com.adobe.eventType.target", "com.adobe.eventSource.responseIdentity")
                    .setEventData(responseEventData).build();
            callbackWithError.call(responseEvent);

            assertNull(responseError);
            assertEquals("someSessionId", response);
        }
    }

    @Test
    public void testSetSessionId() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            final String sessionId = "mySessionId";
            Target.setSessionId(sessionId);

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event event = eventCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetSetSessionIdentifier", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestIdentity", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            assertEquals("mySessionId", eventData.get("sessionid"));
        }
    }

    @Test
    public void testResetExperience() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            Target.resetExperience();

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event event = eventCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetRequestReset", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestReset", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            assertEquals(true, eventData.get("resetexperience"));
        }
    }

    @Test
    public void testClearPrefetchCache() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            Target.clearPrefetchCache();

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event event = eventCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetClearPrefetchCache", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestReset", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            assertEquals(true, eventData.get("clearcache"));
        }
    }

    @Test
    public void testSetPreviewRestartDeepLink() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            // test
            when(uri.toString()).thenReturn("my://deeplink");
            Target.setPreviewRestartDeepLink(uri);

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
            final Event event = eventCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetSetPreviewRestartDeeplink", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> eventData = event.getEventData();
            assertEquals("my://deeplink", eventData.get("restartdeeplink"));
        }
    }

    @Test
    public void testExecuteRawRequest_validRequest() throws IOException {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            final Map<String, Object> executeMbox = new HashMap<>();
            executeMbox.put("index", 0);

            executeMbox.put("mbox", new HashMap<String, Object>() {
                {
                    put("name", "mbox1");
                }
            });

            final Map<String, String> mboxParameters = new HashMap<>();
            mboxParameters.put("mbox_parameter_key", "mbox_parameter_value");
            executeMbox.put("parameters", mboxParameters);

            final Map<String, String> profileParameters = new HashMap<>();
            profileParameters.put("profile_parameter_key", "profile_parameter_value");
            executeMbox.put("profileParameters", profileParameters);

            final Map<String, String> productParameters = new HashMap<>();
            productParameters.put("id", "pId");
            productParameters.put("categoryId", "cId");
            executeMbox.put("product", productParameters);

            final Map<String, Object> orderParameters = new HashMap<>();
            orderParameters.put("id", "oId");
            orderParameters.put("total", 100.34);
            orderParameters.put("purchasedProductIds", new ArrayList<String>() {
                {
                    add("pId");
                }
            });
            executeMbox.put("order", orderParameters);

            final List<Map<String, Object>> executeMboxes = new ArrayList<Map<String, Object>>();
            executeMboxes.add(executeMbox);

            final Map<String, Object> request = new HashMap<String, Object>();
            request.put("execute", new HashMap<String, Object>() {
                {
                    put("mboxes", executeMboxes);
                }
            });

            // test
            Target.executeRawRequest(request, new AdobeCallbackWithError<Map<String, Object>>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError = adobeError;
                }

                @Override
                public void call(Map<String, Object> value) {
                    responseMap = value;
                }
            });

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEventWithResponseCallback(eventCaptor.capture(), anyLong(), callbackCaptor.capture()));
            final Event event = eventCaptor.getValue();
            final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetRawRequest", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> actualRequest = event.getEventData();
            final Map<String, Object> expectedRequest = new HashMap<String, Object>(request);
            expectedRequest.put("israwevent", true);
            assertTrue(actualRequest.equals(expectedRequest));

            final Map<String, Object> responseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/TARGET_RAW_RESPONSE_EXECUTE.json"), HashMap.class);
            final Event responseEvent = new Event.Builder("TargetRawResponse", "com.adobe.eventType.target", "com.adobe.eventSource.responseContent")
                    .setEventData(responseData).build();
            callbackWithError.call(responseEvent);

            assertNull(responseError);
            assertNotNull(responseMap);
            final Map<String, Object> execute = (Map<String, Object>) responseMap.get("execute");
            assertNotNull(execute);
            final List<Map<String, Object>> mboxes = (List<Map<String, Object>>) execute.get("mboxes");
            assertNotNull(mboxes);
            assertEquals(1, mboxes.size());
            final Map<String, Object> mbox1 = mboxes.get(0);
            assertNotNull(mbox1);
            assertEquals("mbox1", mbox1.get("name"));
            final List<Map<String, Object>> options = (List<Map<String, Object>>) mbox1.get("options");
            assertNotNull(options);
            final Map<String, Object> mbox1Options = options.get(0);
            assertEquals("html", mbox1Options.get("type"));
            assertEquals("Good Morning!", mbox1Options.get("content"));
        }
    }

    @Test
    public void testExecuteRawRequest_invalidRequest() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            final Map<String, Object> request = new HashMap<String, Object>();
            request.put("someKey", "someValue");

            // test
            Target.executeRawRequest(request, new AdobeCallback<Map<String, Object>>() {
                @Override
                public void call(Map<String, Object> value) {
                    responseMap = value;
                }
            });

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString()));
            assertNull(responseMap);
        }
    }

    @Test
    public void testExecuteRawRequest_invalidRequestWithErrorCallback() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            final Map<String, Object> request = new HashMap<String, Object>();
            request.put("someKey", "someValue");

            // test
            Target.executeRawRequest(request, new AdobeCallbackWithError<Map<String, Object>>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError = adobeError;
                }

                @Override
                public void call(Map<String, Object> value) {
                    responseMap = value;
                }
            });

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString()));
            assertEquals(AdobeError.UNEXPECTED_ERROR, responseError);
        }
    }

    @Test
    public void testExecuteRawRequest_emptyRequest() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            final Map<String, Object> request = new HashMap<String, Object>();

            // test
            Target.executeRawRequest(request, new AdobeCallback<Map<String, Object>>() {
                @Override
                public void call(Map<String, Object> value) {
                    responseMap = value;
                }
            });

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
            assertNull(responseMap);
        }
    }

    @Test
    public void testExecuteRawRequest_emptyRequestWithErrorCallback() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            final Map<String, Object> request = new HashMap<String, Object>();

            // test
            Target.executeRawRequest(request, new AdobeCallbackWithError<Map<String, Object>>() {
                @Override
                public void fail(AdobeError adobeError) {
                    responseError = adobeError;
                }

                @Override
                public void call(Map<String, Object> value) {
                    responseMap = value;
                }
            });

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
            assertEquals(AdobeError.UNEXPECTED_ERROR, responseError);
        }
    }

    @Test
    public void testSendRawNotifications_validRequest() {
        try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
            final Map<String, Object> notification = new HashMap<>();
            notification.put("id", "0");
            notification.put("timestamp", (long) (System.currentTimeMillis()));
            notification.put("type", "click");
            notification.put("mbox", new HashMap<String, Object>() {
                {
                    put("name", "mbox1");
                }
            });
            notification.put("tokens", new ArrayList<String>() {
                {
                    add("LgG0+YDMHn4X5HqGJVoZ5g==");
                }
            });

            final Map<String, String> parameters = new HashMap<>();
            parameters.put("mbox_parameter_key", "mbox_parameter_value");
            notification.put("parameters", parameters);

            final Map<String, String> profileParameters = new HashMap<>();
            profileParameters.put("profile_parameter_key", "profile_parameter_value");
            notification.put("profileParameters", profileParameters);

            final Map<String, String> productParameters = new HashMap<>();
            productParameters.put("id", "pId");
            productParameters.put("categoryId", "cId");
            notification.put("product", productParameters);

            final Map<String, Object> orderParameters = new HashMap<>();
            orderParameters.put("id", "oId");
            orderParameters.put("total", 100.34);
            orderParameters.put("purchasedProductIds", new ArrayList<String>() {
                {
                    add("pId");
                }
            });
            notification.put("order", orderParameters);

            final List<Map<String, Object>> notifications = new ArrayList<>();
            notifications.add(notification);

            final Map<String, Object> request = new HashMap<String, Object>() {
                {
                    put("notifications", notifications);
                }
            };
            // test
            Target.sendRawNotifications(request);

            // verify
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));

            final Event event = eventCaptor.getValue();

            assertNotNull(event);
            assertEquals("TargetRawNotifications", event.getName());
            assertEquals("com.adobe.eventType.target", event.getType());
            assertEquals("com.adobe.eventSource.requestContent", event.getSource());

            final Map<String, Object> actualRequest = event.getEventData();
            final Map<String, Object> expectedRequest = new HashMap<String, Object>(request);
            expectedRequest.put("israwevent", true);
            assertTrue(actualRequest.equals(expectedRequest));
        }
    }

    @Test
    public void testSendRawNotifications_invalidRequest() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final Map<String, Object> request = new HashMap<String, Object>() {
                {
                    put("someKey", "someValue");
                }
            };
            Target.sendRawNotifications(request);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString()));
        }
    }

    @Test
    public void testSendRawNotifications_emptyRequest() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            final Map<String, Object> request = new HashMap<String, Object>();
            Target.sendRawNotifications(request);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()));
        }
    }
}
