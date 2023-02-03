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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.adobe.marketing.mobile.VisitorID;
import com.adobe.marketing.mobile.services.DeviceInforming;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TargetRequestBuilderTest {
	static final String METRICS = "metrics";
	static final String CONTEXT_PARAMETERS = "context";
	static String ID = "id";
	static String MARKETING_CLOUD_ID = "marketingCloudVisitorId";
	static String EXPERIENCE_CLOUD = "experienceCloud";
	static String CUSTOMER_IDS = "customerIds";
	static String LOCATION_HINT = "locationHint";
	static String BLOB = "blob";
	static String CATEGORY_ID = "categoryId";
	static String ORDER_TOTAL = "total";
	static String NAME = "name";
	static String PARAMETERS = "parameters";
	static String INDEX = "index";
	static String PRODUCT = "product";
	static String ORDER = "order";
	static String PURCHASED_PRODUCT_IDS = "purchasedProductIds";
	static String ENVIRONMENT_ID = "environmentId";
	static String PREFETCH_MBOXES = "prefetch";
	static String EXECUTE_MBOXES = "execute";
	static String PROFILE_PARAMETERS = "profileParameters";
	static String VISITED_MBOXES = "notifications";
	static String MBOX = "mbox";
	static String MBOXES = "mboxes";
	static String AAM_PARAMETERS = "audienceManager";
	static final String CUSTOMER_ID_ID = "id";
	static final String CUSTOMER_ID_INTEGRATION_CODE = "integrationCode";
	static final String CUSTOMER_ID_AUTHENTICATION_STATE = "authenticatedState";

	static final String VISITOR_ID_MID = "mid";
	static final String VISITOR_ID_BLOB = "blob";
	static final String VISITOR_ID_LOCATION_HINT = "locationhint";
	static final String VISITOR_IDS_LIST = "visitoridslist";
	static final String VISITOR_IDS_ID = "ID";
	static final String VISITOR_IDS_ID_TYPE = "ID_TYPE";
	static final String VISITOR_IDS_STATE = "STATE";
	static final String VISITOR_IDS_ID_ORIGIN = "ID_ORIGIN";

	static String PROPERTY = "property";
	static String TOKEN = "token";
	static String TOKENS = "tokens";
	static String AUTHENTICATION_STATE = "authenticatedState";

	static String TIMESTAMP = "timestamp";
	static String CONTENT = "content";
	static String A4T = "clientSideAnalyticsLoggingPayload";
	static String METRIC_TYPE = "type";
	static String METRIC_TYPE_DISPLAY = "display";
	static String METRIC_TYPE_CLICK = "click";
	static String PREVIEW_QA_MODE = "qaMode";
	static String STATE = "state";
	static String OPTIONS= "options";

	// setup
	static Map<String, String> lifecycleData = new HashMap<String, String>() {
		{
			put("a.OSVersion", "iOS 14.2");
			put("a.DaysSinceFirstUse", "0");
			put("a.CrashEvent", "CrashEvent");
			put("a.CarrierName", "(nil)");
			put("a.Resolution", "828x1792");
			put("a.RunMode", "Application");
			put("a.ignoredSessionLength", "-1605549540");
			put("a.HourOfDay", "11");
			put("a.AppID", "v5ManualTestApp 1.0 (1)");
			put("a.DayOfWeek", "2");
			put("a.DeviceName", "x86_64");
			put("a.LaunchEvent", "LaunchEvent");
			put("a.Launches", "2");
			put("a.DaysSinceLastUse", "0");
			put("a.locale", "en-US");
		}
	};

	@Mock
	DeviceInforming mockDeviceInfoService;

    @Mock
    TargetPreviewManager mockTargetPreviewManager;

    @Mock
    TargetState mockTargetState;

	TargetRequestBuilder targetRequestBuilder;

	@Before
	public void beforeEach() {
		targetRequestBuilder = new TargetRequestBuilder(mockDeviceInfoService, mockTargetPreviewManager, mockTargetState);
	}

    // ===================================
    // Test getRequestPayload_prefetch
    // ===================================
    @Test
    public void getRequestPayload_PrefetchInJson_When_PrefetchListContainsOneObject() {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(getTargetPrefetchList(1), null, null,
                null, null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
        assertEquals(0, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(INDEX));
        assertEquals("mbox0", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(NAME));
    }

    @Test
    public void getRequestPayload_PrefetchInJson_When_PrefetchListContainsTwoObjects() {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(getTargetPrefetchList(2), null, null,
                null, null, null, null);

		// verify
		assertEquals(2, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
        assertEquals(0, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(INDEX));
        assertEquals("mbox0", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(NAME));
        assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(1).opt(INDEX));
        assertEquals("mbox1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(1).opt(NAME));
    }


    @Test
    public void getRequestPayload_PrefetchInJson_When_PrefetchListIsEmptyOrNull() {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(getTargetPrefetchList(0), null, null,
                null, null, null, null);

		// verify
        assertNull(json.optJSONArray(PREFETCH_MBOXES));

		// test
        json = targetRequestBuilder.getRequestPayload(null, null, null, null, null, null, null);

		// verify
        assertNull(json.optJSONArray(PREFETCH_MBOXES));
    }

    // ===================================
    // Test getRequestPayload_profileParameters
    // ===================================
    @Test
    public void getRequestPayload_ProfileWhen_ProfileParametersIsValid() {
		// setup
        Map<String, String> map = new HashMap<String, String>() {
            {
                put("key", "value");
            }
        };
        TargetParameters params = new TargetParameters.Builder().profileParameters(map).build();

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, null, params, null, null, null, null);

		// verify
        assertNull(json.optJSONObject(EXECUTE_MBOXES));
        assertNull(json.optJSONObject(PREFETCH_MBOXES));
    }

    @Test
    public void getRequestPayload_ProfileParametersNotInJson_When_ProfileParametersIsEmptyOrNull() {
		// setup
        Map map = new HashMap<String, String>();
        TargetParameters params = new TargetParameters.Builder(map).build();

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, null, params, null, null, null, null);
		// verify
        assertNull(json.optJSONObject(PROFILE_PARAMETERS));

		// test
        json = targetRequestBuilder.getRequestPayload(null, null, params, null, null, null, null);
		// verify
        assertNull(json.optJSONObject(PROFILE_PARAMETERS));
    }

    // ===================================
    // Test getRequestPayload_batch
    // ===================================
    @Test
    public void getRequestPayload_BatchInJson_When_BatchListContainsOneObject() {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(1), null,
                null, null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertEquals(0, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(INDEX));
        assertEquals("mbox0", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(NAME));
    }

    @Test
    public void getRequestPayload_BatchInJson_When_BatchListContainsTwoObjects() {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(2), null,
                null, null, null, null);

		// verify
        assertEquals(2, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertEquals(0, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(INDEX));
        assertEquals("mbox0", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(NAME));
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1).opt(INDEX));
        assertEquals("mbox1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1).opt(NAME));
    }

    @Test
    public void getRequestPayload_BatchNotInJson_When_BatchListIsEmptyOrNull() {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(0), null,
                null, null, null, null);
		// verify
        assertNull(json.optJSONObject(EXECUTE_MBOXES));

		// test
        json = targetRequestBuilder.getRequestPayload(null, null, null, null, null, null, null);
		// verify
        assertNull(json.optJSONObject(EXECUTE_MBOXES));
    }

    // ===================================
    // Test getRequestPayload_notification
    // ===================================
    @Test
    public void getRequestPayload_NotificationInJson_When_NotificationListContainsOneObject() {
		// setup
        List<JSONObject> notifications = new ArrayList<JSONObject>() {
            {
                add(new JSONObject());
            }
        };

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null, notifications , null, null, null);

		// verify
        assertEquals(1, json.optJSONArray(VISITED_MBOXES).length());
        assertEquals("{}", json.optJSONArray(VISITED_MBOXES).optJSONObject(0).toString());
    }

    @Test
    public void getRequestPayload_NotificationInJson_When_NotificationListContainsTwoObjects() throws JSONException {
		// setup
        List<JSONObject> notifications = new ArrayList<JSONObject>() {
            {
                add(new JSONObject());
                add(new JSONObject("{\"id\":1}"));
            }
        };

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null,notifications, null, null, null);

		// verify
        assertEquals(2, json.optJSONArray(VISITED_MBOXES).length());
        assertEquals("{}", json.optJSONArray(VISITED_MBOXES).optJSONObject(0).toString());
        assertEquals("{\"id\":1}", json.optJSONArray(VISITED_MBOXES).optJSONObject(1).toString());
    }

    @Test
    public void getRequestPayload_NotificationNotInJson_When_NotificationListIsEmpytOrNull() {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null,
                new ArrayList<>(), null, null, null);
		// verify
        assertNull(json.optJSONArray(VISITED_MBOXES));

		// test
        json = targetRequestBuilder.getRequestPayload(null, null, null, null, null, null, null);
		// verify
        assertNull(json.optJSONArray(VISITED_MBOXES));
    }

    // ===================================
    // Test getRequestPayload_preview
    // ===================================
    @Test
    public void getRequestPayload_PreviewInJson_When_PreviewTokenAndPreviewParameterValid() {
		// setup
        Mockito.when(mockTargetPreviewManager.getPreviewToken()).thenReturn("previewToken");
        Mockito.when(mockTargetPreviewManager.getPreviewParameters()).thenReturn("{\"qaMode\":{\"key\":\"value\"}}");

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null, null, null, null, null);

		// verify
        assertEquals("value", json.optJSONObject(PREVIEW_QA_MODE).opt("key"));
    }

    @Test
    public void getRequestPayload_PreviewInJson_When_PreviewTokenIsEmpty() {
		// setup
        Mockito.when(mockTargetPreviewManager.getPreviewToken()).thenReturn("");
        Mockito.when(mockTargetPreviewManager.getPreviewParameters()).thenReturn("{\"qaMode\":{\"key\":\"value\"}}");

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null, null, null, null, null);

		// verify
		assertEquals("value", json.optJSONObject(PREVIEW_QA_MODE).opt("key"));
    }

    @Test
    public void getRequestPayload_PreviewNotInJson_When_PreviewTokenIsNull() {
		// setup
        Mockito.when(mockTargetPreviewManager.getPreviewToken()).thenReturn(null);
        Mockito.when(mockTargetPreviewManager.getPreviewParameters()).thenReturn("{\"qaMode\":{\"key\":\"value\"}}");

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null, null, null, null, null);

		// verify
        assertNull(json.optJSONObject(PREVIEW_QA_MODE));

    }

    @Test
    public void getRequestPayload_PreviewInJson_When_PreviewParameterIsEmpty() {
		// setup
        Mockito.when(mockTargetPreviewManager.getPreviewToken()).thenReturn("previewToken");
        Mockito.when(mockTargetPreviewManager.getPreviewParameters()).thenReturn("");

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null, null, null, null, null);

		// verify
        assertNull(json.optJSONObject(PREVIEW_QA_MODE));
    }

    @Test
    public void getRequestPayload_PreviewNotInJson_When_PreviewParameterIsNull() {
		// setup
        Mockito.when(mockTargetPreviewManager.getPreviewToken()).thenReturn("previewToken");
        Mockito.when(mockTargetPreviewManager.getPreviewParameters()).thenReturn(null);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null, null, null, null, null);

		// verify
        assertNull(json.optJSONObject(PREVIEW_QA_MODE));

    }

    // ===================================
    // Test getRequestPayload_orderParameters
    // ===================================
	@Test
	public void getRequestPayload_OrderWhen_OrderParametersAreValid() {
		// setup
		TargetOrder targetOrder = new TargetOrder("orderId", 0.1, new ArrayList<String>() {
			{
				add("id1");
				add("id2");
			}
		});
		TargetParameters targetParameters = new TargetParameters.Builder().order(targetOrder).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null, null, null, null);

		// verify
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals(0.1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).opt(
				ORDER_TOTAL));
		assertEquals("orderId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				ORDER).opt(ID));
		assertEquals(2, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				ORDER).optJSONArray(
				PURCHASED_PRODUCT_IDS).length());
		assertEquals("id1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				ORDER).optJSONArray(
				PURCHASED_PRODUCT_IDS).opt(0));
		assertEquals("id2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				ORDER).optJSONArray(
				PURCHASED_PRODUCT_IDS).opt(1));

	}

	@Test
	public void getRequestPayload_OrderIdNotInJson_When_OrderIdIsEmpty() {
		// setup
		TargetOrder targetOrder = new TargetOrder("", 0.1, null);
		TargetParameters targetParameters = new TargetParameters.Builder().order(targetOrder).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null, null, null, null);

		// verify
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

	@Test
	public void getRequestPayload_OrderIdNotInJson_When_OrderIdIsNull() {
		// setup
		TargetOrder targetOrder = new TargetOrder(null, 0.1, null);
		TargetParameters targetParameters = new TargetParameters.Builder().order(targetOrder).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null, null, null, null);

		// verify
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

	@Test
	public void getRequestPayload_NoOrderIdInJson_NoOrderInJson_When_PurchasedProductIdsIsEmpty() {
		// setup
		TargetOrder targetOrder = new TargetOrder("orderId", 0.1, new ArrayList<>());
		TargetParameters targetParameters = new TargetParameters.Builder().order(targetOrder).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null, null, null, null);

		// verify
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).optJSONArray(
				PURCHASED_PRODUCT_IDS));
	}

	@Test
	public void getRequestPayload_NoOrderIdInJson_NoOrderInJson_When_PurchasedProductIdsIsNull() {
		// setup
		TargetOrder targetOrder = new TargetOrder("orderId", 0.1, null);
		TargetParameters targetParameters = new TargetParameters.Builder().order(targetOrder).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null, null, null, null);

		// verify
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).optJSONArray(
				PURCHASED_PRODUCT_IDS));
	}

	@Test
	public void getRequestPayload_OrderParametersFromMap_When_OrderParametersAreValid() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.order(TargetOrder.fromEventData(new HashMap<String, Object>() {
					{
						put(ID, "orderId");
						put(ORDER_TOTAL, 0.1);
						put(PURCHASED_PRODUCT_IDS, new ArrayList<Object>() {
							{
								add("id1");
								add("id2");
							}
						});
					}
				})).build();
		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals(0.1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).opt(
				ORDER_TOTAL));
		assertEquals("orderId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				ORDER).opt(ID));
		assertEquals(2, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				ORDER).optJSONArray(
				PURCHASED_PRODUCT_IDS).length());
		assertEquals("id1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				ORDER).optJSONArray(
				PURCHASED_PRODUCT_IDS).opt(0));
		assertEquals("id2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				ORDER).optJSONArray(
				PURCHASED_PRODUCT_IDS).opt(1));
	}

	@Test
	public void getRequestPayload_OrderParametersFromMap_OrderNotInJson_When_OrderIdIsEmpty() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.order(TargetOrder.fromEventData(new HashMap<String, Object>() {
					{
						put(ID, "");
						put(ORDER_TOTAL, "");
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

	@Test
	public void getRequestPayload_OrderParametersFromMap_OrderNotInJson_When_OrderIdIsNull() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.order(TargetOrder.fromEventData(new HashMap<String, Object>() {
					{
						put(ID, null);
						put(ORDER_TOTAL, "");
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

	@Test
	public void getRequestPayload_OrderParametersFromMap_NoOrderInJson_When_OrderTotalIsEmpty() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.order(TargetOrder.fromEventData(new HashMap<String, Object>() {
					{
						put(ID, "orderId");
						put(ORDER_TOTAL, "");
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

	@Test
	public void getRequestPayload_OrderParametersFromMap_NoOrderInJson_When_OrderTotalIsNull() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.order(TargetOrder.fromEventData(new HashMap<String, Object>() {
					{
						put(ID, "orderId");
						put(ORDER_TOTAL, null);
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

	@Test
	public void getRequestPayload_OrderParametersFromMap_NoOrderInJson_When_OrderTotalIsStringDouble() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.order(TargetOrder.fromEventData(new HashMap<String, Object>() {
					{
						put(ID, "orderId");
						put(ORDER_TOTAL, "0.1");
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

	@Test
	public void getRequestPayload_OrderParametersFromMap_NoOrderInJson_When_OrderTotalIsRandomString() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.order(TargetOrder.fromEventData(new HashMap<String, Object>() {
					{
						put(ID, "orderId");
						put(ORDER_TOTAL, "total");
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

	@Test
	public void getRequestPayload_OrderParametersFromMap_NoOrderInJson_When_PurchasedProductIdsIsEmpty() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.order(TargetOrder.fromEventData(new HashMap<String, Object>() {
					{
						put(ID, "orderId");
						put(ORDER_TOTAL, null);
						put(PURCHASED_PRODUCT_IDS, new ArrayList<Object>() {
							{
							}
						});
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

	@Test
	public void getRequestPayload_OrderParametersFromMap_NoOrderInJson_When_PurchasedProductIdsOnlyContainsNoneString() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.order(TargetOrder.fromEventData(new HashMap<String, Object>() {
					{
						put(ID, "orderId");
						put(ORDER_TOTAL, null);
						put(PURCHASED_PRODUCT_IDS, new ArrayList<Object>() {
							{
								add(new Object());
							}
						});
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

    // ===================================
    // Test getRequestPayload_ProductParameters
    // ===================================
    @Test
    public void getRequestPayload_ProductWhen_ProductParametersIsValid() {
		// setup
        TargetProduct targetProduct = new TargetProduct("productId", "categoryId");
        TargetParameters targetParameters = new TargetParameters.Builder().product(targetProduct).build();
        TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
        List<TargetRequest> targetRequestList = new ArrayList<>();
        targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertEquals("productId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PRODUCT).opt(ID));
        assertEquals("categoryId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PRODUCT).opt(CATEGORY_ID));

    }

    @Test
    public void getRequestPayload_ProductIdNotInJson_When_ProductIdIsNull() {
		// setup
        TargetProduct targetProduct = new TargetProduct(null, "categoryId");
        TargetParameters targetParameters = new TargetParameters.Builder().product(targetProduct).build();
        TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
        List<TargetRequest> targetRequestList = new ArrayList<>();
        targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT));

    }

    @Test
    public void getRequestPayload_ProductIdNotInJson_When_ProductIdIsEmpty() {
		// setup
        TargetProduct targetProduct = new TargetProduct("", "categoryId");
        TargetParameters targetParameters = new TargetParameters.Builder().product(targetProduct).build();
        TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
        List<TargetRequest> targetRequestList = new ArrayList<>();
        targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT));
    }

    @Test
    public void getRequestPayload_NoProductInJson_When_ProductCategoryIsEmpty() {
		// setup
        TargetProduct targetProduct = new TargetProduct("productId", "");
        TargetParameters targetParameters = new TargetParameters.Builder().product(targetProduct).build();
        TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
        List<TargetRequest> targetRequestList = new ArrayList<>();
        targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("productId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				PRODUCT).opt(ID));
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT).opt(
				CATEGORY_ID));
	}

    @Test
    public void getRequestPayload_NoProductInJson_When_ProductCategoryIsNull() {
		// setup
        TargetProduct targetProduct = new TargetProduct("productId", null);
        TargetParameters targetParameters = new TargetParameters.Builder().product(targetProduct).build();
        TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
        List<TargetRequest> targetRequestList = new ArrayList<>();
        targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("productId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				PRODUCT).opt(ID));
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT).opt(
				CATEGORY_ID));
	}

	@Test
	public void getRequestPayload_ProductFromMap_When_ProductParameterAreValid() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.product(TargetProduct.fromEventData(new HashMap<String, String>() {
					{
						put(ID, "productId");
						put(CATEGORY_ID, "categoryId");
					}
				})).build();
		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("productId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				PRODUCT).opt(ID));
		assertEquals("categoryId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				PRODUCT).opt(CATEGORY_ID));
	}

	@Test
	public void getRequestPayload_ProductFromMap_ProductIdNotInJson_When_ProductIdIsNull() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.product(TargetProduct.fromEventData(new HashMap<String, String> () {
					{
						put(ID, null);
						put(CATEGORY_ID, "categoryId");
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT));

	}

	@Test
	public void getRequestPayload_ProductFromMap_ProductIdNotInJson_When_ProductIdIsEmpty() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.product(TargetProduct.fromEventData(new HashMap<String, String> () {
					{
						put(ID, "");
						put(CATEGORY_ID, "categoryId");
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT));
	}

	@Test
	public void getRequestPayload_ProductFromMap_NoProductInJson_When_ProductCategoryIsEmpty() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.product(TargetProduct.fromEventData(new HashMap<String, String> () {
					{
						put(ID, "productId");
						put(CATEGORY_ID, "");
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("productId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				PRODUCT).opt(ID));
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT).opt(
				CATEGORY_ID));
	}

	@Test
	public void getRequestPayload_ProductFromMap_NoProductInJson_When_ProductCategoryIsNull() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.product(TargetProduct.fromEventData(new HashMap<String, String> () {
					{
						put(ID, "productId");
						put(CATEGORY_ID, null);
					}
				})).build();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("productId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
				PRODUCT).opt(ID));
		 assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT).opt(
				CATEGORY_ID));
	}

	// ===================================
    // Test getRequestPayload_rawPrefetch
    // ===================================
    @Test
    public void getRequestPayload_PrefetchInJson() {
        // setup
        final JSONObject defaultJson = new JSONObject();;
        final Map<String, Object> prefetchMbox1 = new HashMap<String, Object>() {
            {
                put("index", 0);
                put("name", "mbox1");
            }
        };
        final Map<String, Object> prefetchMbox2 = new HashMap<String, Object>() {
            {
                put("index", 1);
                put("name", "mbox2");
            }
        };
        final List<Map<String, Object>> prefetchMboxes = new ArrayList<>();
        prefetchMboxes.add(prefetchMbox1);
        prefetchMboxes.add(prefetchMbox2);

        final Map<String, Object> prefetch = new HashMap<String, Object>() {
            {
                put("mboxes", prefetchMboxes);
            }
        };

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, prefetch, null, null,
                null);

        // verify
        assertEquals(2, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
        assertEquals(0, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(INDEX));
        assertEquals("mbox1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(NAME));
        assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(1).opt(INDEX));
        assertEquals("mbox2", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(1).opt(NAME));
    }

    @Test
    public void getRequestPayload_NoPrefetchInJson_When_PrefetchMapIsEmpty() {
        // setup
        final JSONObject defaultJson = new JSONObject();;
        final Map<String, Object> prefetch = new HashMap<String, Object>();

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, prefetch, null, null,
                null);

        // verify
        assertNull(json.optJSONArray(PREFETCH_MBOXES));
    }

    @Test
    public void getRequestPayload_NoPrefetchInJson_When_NullPrefetch() {
        // setup
        final JSONObject defaultJson = new JSONObject();;

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
                null);

        // verify
        assertNull(json.optJSONArray(PREFETCH_MBOXES));
    }

    // ===================================
    // Test getRequestPayload_rawExecute
    // ===================================
    @Test
    public void getRequestPayload_ExecuteInJson() {
        // setup
        final JSONObject defaultJson = new JSONObject();;
        final Map<String, Object> executeMbox1 = new HashMap<String, Object>() {
            {
                put("index", 0);
                put("name", "mbox1");
            }
        };
        final Map<String, Object> executeMbox2 = new HashMap<String, Object>() {
            {
                put("index", 1);
                put("name", "mbox2");
            }
        };
        final List<Map<String, Object>> executeMboxes = new ArrayList<>();
        executeMboxes.add(executeMbox1);
        executeMboxes.add(executeMbox2);

        final Map<String, Object> execute = new HashMap<String, Object>() {
            {
                put("mboxes", executeMboxes);
            }
        };

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, execute, null,
                null);

        // verify
        assertEquals(2, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertEquals(0, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(INDEX));
        assertEquals("mbox1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).opt(NAME));
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1).opt(INDEX));
        assertEquals("mbox2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1).opt(NAME));
    }

    @Test
    public void getRequestPayload_NoExecuteInJson_When_RawExecuteMapIsEmpty() {
        // setup
        final JSONObject defaultJson = new JSONObject();;
        final Map<String, Object> execute = new HashMap<String, Object>();

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, execute, null,
                null);

        // verify
        assertNull(json.optJSONArray(EXECUTE_MBOXES));
    }

    @Test
    public void getRequestPayload_NoExecuteInJson_When_NullExecute() {
        // setup
        final JSONObject defaultJson = new JSONObject();;

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
                null);

        // verify
        assertNull(json.optJSONArray(EXECUTE_MBOXES));
    }

    // ===================================
    // Test getRequestPayload_rawNotification
    // ===================================
    @Test
    public void getRequestPayload_NotificationsInJson() {
        // setup
        final JSONObject defaultJson = new JSONObject();;
        final Map<String, Object> notification1 = new HashMap<String, Object>() {
            {
                put("id", "0");
                put("timestamp", (long)(System.currentTimeMillis()));
                put("type", "display");
                put("mbox", new HashMap<String, Object>() {
                    {
                        put("name", "mbox1");
                    }
                });
                put("tokens", new ArrayList<String>() {
                    {
                        add("displayToken");
                    }
                });
            }
        };
        final Map<String, Object> notification2 = new HashMap<String, Object>() {
            {
                put("id", "1");
                put("timestamp", (long)(System.currentTimeMillis()));
                put("type", "click");
                put("mbox", new HashMap<String, Object>() {
                    {
                        put("name", "mbox2");
                    }
                });
                put("tokens", new ArrayList<String>() {
                    {
                        add("clickToken");
                    }
                });
            }
        };
        final List<Map<String, Object>> notifications = new ArrayList<Map<String, Object>>() {
            {
                add(notification1);
                add(notification2);
            }
        };

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, notifications,
                null);

        // verify
        assertEquals(2, json.optJSONArray(VISITED_MBOXES).length());
        assertEquals("0", json.optJSONArray(VISITED_MBOXES).optJSONObject(0).opt(ID));
        assertEquals("display", json.optJSONArray(VISITED_MBOXES).optJSONObject(0).opt(METRIC_TYPE));
        assertEquals("mbox1", json.optJSONArray(VISITED_MBOXES).optJSONObject(0).optJSONObject(MBOX).opt(NAME));
        assertEquals("displayToken", json.optJSONArray(VISITED_MBOXES).optJSONObject(0).optJSONArray(TOKENS).opt(0));
        assertNotEquals(0L, json.optJSONArray(VISITED_MBOXES).optJSONObject(0).opt(TIMESTAMP));
        assertEquals("1", json.optJSONArray(VISITED_MBOXES).optJSONObject(1).opt(ID));
        assertEquals("click", json.optJSONArray(VISITED_MBOXES).optJSONObject(1).opt(METRIC_TYPE));
        assertEquals("mbox2", json.optJSONArray(VISITED_MBOXES).optJSONObject(1).optJSONObject(MBOX).opt(NAME));
        assertEquals("clickToken", json.optJSONArray(VISITED_MBOXES).optJSONObject(1).optJSONArray(TOKENS).opt(0));
        assertNotEquals(0L, json.optJSONArray(VISITED_MBOXES).optJSONObject(1).opt(TIMESTAMP));
    }

    @Test
    public void getRequestPayload_NoNotificationsInJson_When_RawNotificationsListIsEmpty() {
        // setup
        final JSONObject defaultJson = new JSONObject();;
        final List<Map<String, Object>> notifications = new ArrayList<>();

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, notifications,
                null);

        // verify
        assertNull(json.optJSONArray(VISITED_MBOXES));
    }

    @Test
    public void getRequestPayload_NoNotificationsInJson_When_NullNotifications() {
        // setup
        final JSONObject defaultJson = new JSONObject();;

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
                null);

        // verify
        assertNull(json.optJSONArray(VISITED_MBOXES));
    }

    // ===================================
    // Test getRequestPayload_PropertyToken
    // ===================================
    @Test
    public void getRequestPayload_PropertyTokenInJson_When_validToken() throws JSONException {
        // setup
        final JSONObject defaultJson = new JSONObject();;

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
                "randomPropertyToken");

        // verify
        assertNotNull(json.optJSONObject(PROPERTY));
        assertEquals(1, json.optJSONObject(PROPERTY).length());
        assertEquals("randomPropertyToken", json.getJSONObject(PROPERTY).opt(TOKEN));
    }

    @Test
    public void getRequestPayload_NoPropertyTokenInJson_When_nullToken() {
        // setup
        final JSONObject defaultJson = new JSONObject();;

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
                null);

        // verify
        assertNull(json.optJSONObject(PROPERTY));
    }

    @Test
    public void getRequestPayload_NoPropertyTokenInJson_When_EmptyToken() {
        // setup
        final JSONObject defaultJson = new JSONObject();;

        // test
        JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
                "");

        // verify
        assertNull(json.optJSONObject(PROPERTY));
    }

    @Test
    public void getRequestPayload_PropertyTokenNotInJson_When_TokenIsNull() {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(1), null,
                null, null, null, null);

		// verify
        assertNull(json.optJSONObject(PROPERTY));
    }

    @Test
    public void getRequestPayload_PropertyTokenNotInJson_When_TokenIsEmpty() {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(1), null,
                null, "", null, null);

		// verify
        assertNull(json.optJSONObject(PROPERTY));
    }

    @Test
    public void getRequestPayload_PropertyTokenInJson_When_TokenIsNotNull() throws JSONException {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(1), null,
                null, "randomPropertyToken", null, null);

		// verify
        assertNotNull(json.optJSONObject(PROPERTY));
        assertEquals(1, json.optJSONObject(PROPERTY).length());
        assertEquals("randomPropertyToken", json.getJSONObject(PROPERTY).opt(TOKEN));
    }

    // ===================================
    // Test getRequestPayload_MBoxParameters
    // ===================================
    @Test
    public void getRequestPayload_MboxWhen_MboxParametersAreValid() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder().parameters(new HashMap<String, String>() {
			{
				put("mBox-parameter-Key1", "mBox-Value1");
				put("mBox-parameter-Key2", "mBox-Value2");
			}
		}).build();
        List<TargetRequest> targetRequestList = new ArrayList<>();
        TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
        targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertEquals("mBox-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PARAMETERS).opt("mBox-parameter-Key1"));
        assertEquals("mBox-Value2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PARAMETERS).opt("mBox-parameter-Key2"));
    }

    @Test
    public void getRequestPayload_Mboxparameter_Key1_NotInJson_When_parameter_Key1_ValueIsNull() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder().parameters(new HashMap<String, String>() {
			{
				put("mBox-parameter-Key1",  null);
				put("mBox-parameter-Key2", "mBox-Value2");
			}
		}).build();
		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PARAMETERS).optString("mBox-parameter-Key1", null));
        assertEquals("mBox-Value2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PARAMETERS).opt("mBox-parameter-Key2"));
    }

    @Test
    public void getRequestPayload_Mboxparameter_When_Mboxparameters_Are_Empty() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder().parameters(new HashMap<>()).build();
		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PARAMETERS));
    }

    // ===================================
    // Test getRequestPayload_ProfileParameters
    // ===================================
    @Test
    public void getRequestPayload_ProfileWhen_ProfileParametersAreValid() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(new HashMap<String, String>() {
			{
				put("profile-parameter-Key1", "profile-Value1");
				put("profile-parameter-Key2", "profile-Value2");
			}
		}).build();
		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
        assertEquals("profile-Value2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
    }

    @Test
    public void getRequestPayload_Profileparameter_Key1_NotInJson_When_parameter_Key1_ValueIsNull() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(new HashMap<String, String>() {
			{
				put("profile-parameter-Key1", null);
				put("profile-parameter-Key2", "profile-Value2");
			}
		}).build();
		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS).optString("profile-parameter-Key1", null));
        assertEquals("profile-Value2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
    }

    @Test
    public void getRequestPayload_Profileparameter_When_Profileparameters_are_Empty() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(new HashMap<>()).build();
		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS));
    }

    // ===================================
    // Test getRequestPayload_TargetParameters
    // ===================================
    @Test
    public void getRequestPayload_When_TargetParametersAreEmpty() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder().build();
		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT));
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PROFILE_PARAMETERS));
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
    }

    @Test
    public void getRequestPayload_When_TargetParametersAreNull() {
		// setup
		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", null, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT));
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PROFILE_PARAMETERS));
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
    }

	// ===================================
	// Test getRequestPayload_PropertyToken
	// ===================================
    @Test
    public void getRequestPayload_PropertyWhen_ATPropertyInMboxParameters() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", "randomPropertyToken");
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key1", "profile-Value1");
					}
				}).build();

        List<TargetRequest> targetRequestList = new ArrayList<>();
        TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
        targetRequestList.add(targetRequest);

		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
        assertNull(json.optJSONObject(TargetJson.PROPERTY));
    }

    @Test
    public void getRequestPayload_PropertyParametersNotInJson_When_NullATPropertyInMboxParameters() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", null);
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key1", "profile-Value1");
					}
				}).build();

		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
        assertNull(json.optJSONObject(TargetJson.PROPERTY));
    }

    @Test
    public void getRequestPayload_ForMultipleMboxes_PropertyWhen_ATPropertyInMboxParameters() {
		// setup
		TargetParameters targetParameters1 = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", "randomPropertyToken");
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key1", "profile-Value1");
					}
				}).build();

		TargetParameters targetParameters2 = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", "randomPropertyToken2");
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key2", "profile-Value2");
					}
				}).build();

		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest1 = new TargetRequest("mbox1", targetParameters1, "default", callback -> {});
		TargetRequest targetRequest2 = new TargetRequest("mbox2", targetParameters2, "default", callback -> {});
		targetRequestList.add(targetRequest1);
		targetRequestList.add(targetRequest2);

		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

        assertNull(json.optJSONObject(TargetJson.PROPERTY));
        JSONArray mboxesArray = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES);
        assertEquals(2, mboxesArray.length());
        JSONObject mbox1 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0);
        assertEquals("profile-Value1", mbox1.optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
        assertNull(mbox1.optJSONObject(PARAMETERS));
        JSONObject mbox2 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1);
        assertEquals("profile-Value2", mbox2.optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
        assertNull(mbox2.optJSONObject(PARAMETERS));
    }

    @Test
    public void getRequestPayload_ForMultipleMboxes_PropertyParametersNotInJson_When_NullATPropertyInMboxParameters() {
		// setup
		TargetParameters targetParameters1 = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", null);
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key1", "profile-Value1");
					}
				}).build();

		TargetParameters targetParameters2 = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", null);
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key2", "profile-Value2");
					}
				}).build();

		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest1 = new TargetRequest("mbox1", targetParameters1, "default", callback -> {});
		TargetRequest targetRequest2 = new TargetRequest("mbox2", targetParameters2, "default", callback -> {});
		targetRequestList.add(targetRequest1);
		targetRequestList.add(targetRequest2);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
        assertNull(json.optJSONObject(TargetJson.PROPERTY));
        JSONArray mboxesArray = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES);
        assertEquals(2, mboxesArray.length());
        JSONObject mbox1 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0);
        assertEquals("profile-Value1", mbox1.optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
        assertNull(mbox1.optJSONObject(PARAMETERS));
        JSONObject mbox2 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1);
        assertEquals("profile-Value2", mbox2.optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
        assertNull(mbox2.optJSONObject(PARAMETERS));
    }

    @Test
    public void
	getRequestPayload_ForMultipleMboxes_SecondMboxPropertyWhen_NullATPropertyInFirstMboxParameters() {
		// setup
		TargetParameters targetParameters1 = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", null);
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key1", "profile-Value1");
					}
				}).build();

		TargetParameters targetParameters2 = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", "randomPropertyToken2");
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key2", "profile-Value2");
					}
				}).build();

		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest1 = new TargetRequest("mbox1", targetParameters1, "default", callback -> {});
		TargetRequest targetRequest2 = new TargetRequest("mbox2", targetParameters2, "default", callback -> {});
		targetRequestList.add(targetRequest1);
		targetRequestList.add(targetRequest2);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				null, null, null);

		// verify
        assertNull(json.optJSONObject(TargetJson.PROPERTY));
        JSONArray mboxesArray = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES);
        assertEquals(2, mboxesArray.length());
        JSONObject mbox1 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0);
        assertEquals("profile-Value1", mbox1.optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
        assertNull(mbox1.optJSONObject(PARAMETERS));
        JSONObject mbox2 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1);
        assertEquals("profile-Value2", mbox2.optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
        assertNull(mbox2.optJSONObject(PARAMETERS));
    }

    @Test
    public void getRequestPayload_PropertyWhen_ATPropertyInMboxParametersOverriddenByGlobalProperty() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", "randomPropertyToken");
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key1", "profile-Value1");
					}
				}).build();

		List<TargetRequest> targetRequestList = new ArrayList<>();
		TargetRequest targetRequest = new TargetRequest("mbox", targetParameters, "default", callback -> {});
		targetRequestList.add(targetRequest);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, null, null,
				"GlobalPropertyToken", null, null);

		// verify
        assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
        assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
        assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
        assertEquals("GlobalPropertyToken", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
    }

    @Test
    public void getRequestPayloadWithPrefetchRequests_PropertyWhen_ATPropertyInMboxParameters() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", "randomPropertyToken");
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key1", "profile-Value1");
					}
				}).build();

		List<TargetPrefetch> targetPrefetchList = new ArrayList<TargetPrefetch>();
		TargetPrefetch targetRequest = new TargetPrefetch("mbox", targetParameters);
		targetPrefetchList.add(targetRequest);

		// setup
        JSONObject json = targetRequestBuilder.getRequestPayload(targetPrefetchList, null, null, null,
                null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
        assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
        assertNull(json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
        assertNull(json.optJSONObject(TargetJson.PROPERTY));
    }

    @Test
    public void
    getRequestPayloadWithPrefetchRequests_PropertyWhen_ATPropertyInMboxParametersOverriddenByGlobalProperty() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", "randomPropertyToken");
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key1", "profile-Value1");
					}
				}).build();

		List<TargetPrefetch> targetPrefetchList = new ArrayList<TargetPrefetch>();
		TargetPrefetch targetRequest = new TargetPrefetch("mbox", targetParameters);
		targetPrefetchList.add(targetRequest);

		// setup
		JSONObject json = targetRequestBuilder.getRequestPayload(targetPrefetchList, null, null, null,
				"GlobalPropertyToken", null, null);

		// verify
        assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
        assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
        assertNull(json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
        assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
        assertEquals("GlobalPropertyToken", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
    }

    @Test
    public void getRequestPayloadWithPrefetchRequests_PropertyParametersNotInJson_When_NullATPropertyInMboxParameters() {
		// setup
		TargetParameters targetParameters = new TargetParameters.Builder()
				.parameters(new HashMap<String, String>() {
					{
						put("at_property", null);
					}
				})
				.profileParameters(new HashMap<String, String>() {
					{
						put("profile-parameter-Key1", "profile-Value1");
					}
				}).build();

		List<TargetPrefetch> targetPrefetchList = new ArrayList<TargetPrefetch>();
		TargetPrefetch targetRequest = new TargetPrefetch("mbox", targetParameters);
		targetPrefetchList.add(targetRequest);

		// setup
		JSONObject json = targetRequestBuilder.getRequestPayload(targetPrefetchList, null, null, null,
				null, null, null);

		// verify
        assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
        assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
        assertNull(json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
        assertNull(json.optJSONObject(TargetJson.PROPERTY));
    }


	// ===================================
	// Test getRequestPayload_IdentityData
	// ===================================
	@Test
	public void getRequestPayload_MidNotInJson_When_MidIsEmptyOrNull() {
		// setup
		Map<String, Object> identitySharedState = getIdentitySharedState("", "", "", null);
		
		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null, null, null, identitySharedState, null);
		
		// test
		 assertNull(json.optJSONObject(ID));

		// setup
		Map<String, Object> identitySharedState2 = getIdentitySharedState(null, "", "", null);

		// test
		JSONObject json2 = targetRequestBuilder.getRequestPayload(null, null, null, null, null, identitySharedState2, null);

		// test
		 assertNull(json2.optJSONObject(ID));
	}

	@Test
	public void getRequestPayload_MidInJson_When_MidIsValid() throws JSONException {
		// setup
		Map<String, Object> identitySharedState = getIdentitySharedState("mid", "", "", null);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null,null, null, identitySharedState, null);

		// test
		assertEquals(json.getJSONObject(ID).getString(MARKETING_CLOUD_ID), "mid");
	}

	@Test
	public void getRequestPayload_BlobNotInJson_When_BlobIsEmptyOrNull() throws JSONException {
		// setup
		Map<String, Object> identitySharedState = getIdentitySharedState("", "", "", null);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null,null, null, identitySharedState, null);

		// test
		 assertNull(json.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS));

		// setup
		Map<String, Object> identitySharedState2 = getIdentitySharedState("", null, "", null);

		// test
		JSONObject json2 = targetRequestBuilder.getRequestPayload(null, null, null, null,null, identitySharedState2, null);

		// test
		 assertNull(json2.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS));
	}

	@Test
	public void getRequestPayload_BlobInJson_When_BlobIsValid() throws JSONException {
		// setup
		Map<String, Object> identitySharedState = getIdentitySharedState("", "blob", "", null);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null, null,null, identitySharedState, null);

		// test
		assertEquals(json.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS).getString(BLOB), "blob");
	}

	@Test
	public void getRequestPayload_LocationHintInJson_When_LocationHintIsValid() throws JSONException {
		// setup
		Map<String, Object> identitySharedState = getIdentitySharedState("", "", "LocationHint", null);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null, null,null, identitySharedState, null);

		// test
		assertEquals(json.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS).getString(LOCATION_HINT),
				"LocationHint");
	}

	@Test
	public void getRequestPayload_LocationHintNotInJson_When_LocationHintIsEmptyOrNull() throws JSONException {
		// setup
		Map<String, Object> identitySharedState = getIdentitySharedState("", "", "", null);

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null,null, null, identitySharedState, null);

		// test
		 assertNull(json.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS));

		// setup
		Map<String, Object> identitySharedState2 = getIdentitySharedState("", null, "", null);

		// test
		JSONObject json2 = targetRequestBuilder.getRequestPayload(null, null, null, null,null, identitySharedState2, null);

		// test
		 assertNull(json2.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS));
	}

	@Test
	public void getRequestPayload_CustomerIdsInJson_When_CustomerIdsIsValid() throws JSONException {
		// setup
		final Map<String, Object> customVisitorID = new HashMap<String, Object>(){{
			put(VISITOR_IDS_ID, "someID");
			put(VISITOR_IDS_ID_TYPE, "someType" );
			put(VISITOR_IDS_ID_ORIGIN, "someOrigin" );
			put(VISITOR_IDS_STATE, VisitorID.AuthenticationState.AUTHENTICATED.getValue());
		}};
		final Map<String, Object> identitySharedState = getIdentitySharedState("", "", "", Arrays.asList(customVisitorID));

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null,null, null, identitySharedState, null);

		// test
		assertEquals(json.optJSONObject(ID).optJSONArray(CUSTOMER_IDS).getJSONObject(0).get(AUTHENTICATION_STATE),
				"authenticated");
	}

	@Test
	public void getRequestPayload_CustomerIdsInJson_OnlyPicks_ValidCustomerIDs() throws JSONException {
		// setup
		final Map<String, Object> customVisitorID1 = new HashMap<String, Object>(){{
			put(VISITOR_IDS_ID, "someID1");
			put(VISITOR_IDS_ID_TYPE, "someType1" );
			put(VISITOR_IDS_ID_ORIGIN, "someOrigin1" );
			put(VISITOR_IDS_STATE, VisitorID.AuthenticationState.LOGGED_OUT.getValue());
		}};

		// without ID
		final Map<String, Object> customVisitorID2 = new HashMap<String, Object>(){{
			put(VISITOR_IDS_ID_TYPE, "someType2" );
			put(VISITOR_IDS_ID_ORIGIN, "someOrigin2" );
			put(VISITOR_IDS_STATE, VisitorID.AuthenticationState.AUTHENTICATED.getValue());
		}};

		// without integrationCode (type)
		final Map<String, Object> customVisitorID3 = new HashMap<String, Object>(){{
			put(VISITOR_IDS_ID, "someID2");
			put(VISITOR_IDS_ID_ORIGIN, "someOrigin2" );
			put(VISITOR_IDS_STATE, VisitorID.AuthenticationState.AUTHENTICATED.getValue());
		}};

		// nonString integrationCode value
		final Map<String, Object> customVisitorID4 = new HashMap<String, Object>(){{
			put(VISITOR_IDS_ID, 34);
			put(VISITOR_IDS_ID_TYPE, "someType2" );
			put(VISITOR_IDS_ID_ORIGIN, "someOrigin2" );
			put(VISITOR_IDS_STATE, VisitorID.AuthenticationState.LOGGED_OUT.getValue());
		}};
		Map<String, Object> identitySharedState = getIdentitySharedState("", "", "", Arrays.asList(customVisitorID1, customVisitorID2, customVisitorID3, customVisitorID4));

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null,null, null, identitySharedState, null);

		// verify
		JSONArray gatheredIds = json.optJSONObject(ID).optJSONArray(CUSTOMER_IDS);
		assertEquals("Length of customerIDs is correct", 1,gatheredIds.length());
		JSONObject gatheredVisitorID = gatheredIds.getJSONObject(0);
		assertEquals("Correct CustomerID id is set", gatheredVisitorID.get(CUSTOMER_ID_ID), "someID1");
		assertEquals("Correct CustomerID IntegrationCode is set", gatheredVisitorID.get(CUSTOMER_ID_INTEGRATION_CODE), "someType1");
		assertEquals("Correct CustomerID authentication state is set", gatheredVisitorID.get(CUSTOMER_ID_AUTHENTICATION_STATE), "logged_out");
	}


	@Test
	public void getRequestPayload_CustomerIdsInJson_When_CustomerIdsIsValid_AndLoggedOut() throws JSONException {
		// setup
		final Map<String, Object> customVisitorID = new HashMap<String, Object>(){{
			put(VISITOR_IDS_ID, "someID");
			put(VISITOR_IDS_ID_TYPE, "someType" );
			put(VISITOR_IDS_ID_ORIGIN, "someOrigin" );
			put(VISITOR_IDS_STATE, VisitorID.AuthenticationState.LOGGED_OUT.getValue());
		}};
		Map<String, Object> identitySharedState = getIdentitySharedState("", "", "", Arrays.asList(customVisitorID));

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null,null, null, identitySharedState, null);

		// test
		assertEquals(json.optJSONObject(ID).optJSONArray(CUSTOMER_IDS).getJSONObject(0).get(AUTHENTICATION_STATE),
				"logged_out");
	}


	@Test
	public void getRequestPayload_CustomerIdsInJson_When_CustomerIdsIsValid_AndUnknown() throws JSONException {
		// setup
		final Map<String, Object> customVisitorID = new HashMap<String, Object>(){{
			put(VISITOR_IDS_ID, "someID");
			put(VISITOR_IDS_ID_TYPE, "someType" );
			put(VISITOR_IDS_ID_ORIGIN, "someOrigin" );
			put(VISITOR_IDS_STATE, 5);
		}};

		List<VisitorID> testVisitorIDList = new ArrayList<>();
		testVisitorIDList.add(new VisitorID("d_cid_ic", "id1", "hodor",
				VisitorID.AuthenticationState.UNKNOWN));
		Map<String, Object> identitySharedState = getIdentitySharedState("", "", "", Arrays.asList(customVisitorID));

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null,null, null, identitySharedState, null);

		// test
		assertEquals(json.optJSONObject(ID).optJSONArray(CUSTOMER_IDS).getJSONObject(0).get(AUTHENTICATION_STATE),
				"unknown");
	}

	@Test
	public void getRequestPayload_CustomerIdsNotInJson_When_CustomerIdsIsEmptyOrNull() throws JSONException {
		// setup
		Map<String, Object> identitySharedState = getIdentitySharedState("", "", "", new ArrayList<>());

		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, null,null, null, identitySharedState, null);

		// test
		 assertNull(json.optJSONObject(ID));

		// setup
		Map<String, Object> identitySharedState2 = getIdentitySharedState("", null, "", null);

		// test
		JSONObject json2 = targetRequestBuilder.getRequestPayload(null, null, null, null,null, identitySharedState2, null);

		// test
		 assertNull(json.optJSONObject(ID));
	}


	// ===================================
	// Test getRequestPayload_LifecycleData
	// ===================================
    @Test
    public void getRequestPayload_LifecycleDataNotInJson_When_LifecycleIsEmptyOrNull() {
		// test
        JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(1), null,
                null, null, null, null);

		// verify
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));

		// test
		json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(1), null,
                null, null, null,  new HashMap<>());

		// verify
        assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));


    }

    @Test
    public void getRequestPayload_LifecycleDataInMboxParameters_When_LifecycleIsValid() {
		// test
		JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(1), null,
                null, null, null, lifecycleData);

		// verify
        assertEquals(new JSONObject(lifecycleData).toString(), json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
                PARAMETERS).toString());

    }

	// ===================================
	// Test getDisplayNotificationJsonObject
	// ===================================
	@Test
	public void getDisplayNotificationJsonObject_ReturnNull_When_InputJsonIsNull() {
		// test
		JSONObject json = targetRequestBuilder.getDisplayNotificationJsonObject(null, null, null, 123L, null);

		// verify
		 assertNull(json);
	}

	@Test
	public void getDisplayNotificationJsonObject_ReturnObjectWithTimeStampAndMetricType1_When_InputJsonIsNotNull() throws
			JSONException {
		// setup
		JSONObject json = new JSONObject();;
		json.put(CONTENT, "content");
		json.put(A4T, "A4T");
		json.put("key", "value");
		json.put(STATE, "somestate");
		JSONObject options1 = new JSONObject();
		options1.put("eventToken", "token1");
		JSONObject options2 = new JSONObject();
		JSONArray optionsArray = new JSONArray();
		optionsArray.put(options1);
		optionsArray.put(options2);
		json.put(OPTIONS, optionsArray);

		// test
		JSONObject visitedMboxNode = targetRequestBuilder.getDisplayNotificationJsonObject("mboxName", json,
				null, 123L, null);

		// verify
		 assertNull(visitedMboxNode.opt(CONTENT));
		 assertNull(visitedMboxNode.opt(A4T));
		assertEquals(METRIC_TYPE_DISPLAY, visitedMboxNode.opt(METRIC_TYPE));
		assertEquals(123L, visitedMboxNode.opt(TIMESTAMP));
		assertEquals("mboxName", visitedMboxNode.optJSONObject(MBOX).opt(NAME));
		assertEquals("somestate", visitedMboxNode.optJSONObject(MBOX).opt(STATE));
		assertEquals("token1", visitedMboxNode.optJSONArray(TOKENS).get(0));
	}

	// ===================================
	// Test getClickNotificationJsonObject
	// ===================================
	@Test
	public void getClickNotificationJsonObject_When_InputJsonIsNotNull() throws JSONException {
		// setup
		JSONObject mboxJson = new JSONObject();;
		mboxJson.put(NAME, "mboxName");
		JSONObject metric1 = new JSONObject();
		metric1.put("eventToken", "token1");
		metric1.put(METRIC_TYPE, METRIC_TYPE_CLICK);
		JSONObject metric2 = new JSONObject();
		JSONObject metric3 = new JSONObject();
		metric3.put(METRIC_TYPE, "custom");
		JSONArray metricsArray = new JSONArray();
		metricsArray.put(metric1);
		metricsArray.put(metric2);
		metricsArray.put(metric3);
		mboxJson.put(METRICS, metricsArray);

		// test
		JSONObject clickJson = targetRequestBuilder.getClickNotificationJsonObject(mboxJson,
				null, 123L, null);

		// verify
		assertEquals(123L, clickJson.opt(TIMESTAMP));
		assertEquals(METRIC_TYPE_CLICK, clickJson.opt(METRIC_TYPE));
		assertEquals("mboxName", clickJson.optJSONObject(MBOX).opt(NAME));
		assertEquals("token1", clickJson.optJSONArray(TOKENS).get(0));
	}

	@Test
	public void getClickNotificationJsonObject_ReturnObjectWithTimeStampAndMetricType_When_InputJsonIsNull() {
		// test
		JSONObject json = targetRequestBuilder.getClickNotificationJsonObject((
				JSONObject) null, null, 123L, null);

		// verify
		assertNotNull(json);
		assertEquals(123L, json.opt(TIMESTAMP));
		assertEquals(METRIC_TYPE_CLICK, json.opt(METRIC_TYPE));
	}

	// ===================================
	// Test getDefaultJsonObject
	// ===================================
	@Test
	public void getDefaultJsonObject_NullParameters() {
		// test
		Mockito.when(mockTargetState.getTntId()).thenReturn("tntId");
		Mockito.when(mockTargetState.getThirdPartyId()).thenReturn("thirdPartyId");

		JSONObject json = targetRequestBuilder.getDefaultJsonObject(null, null, null, 0L, null);

		// verify
		assertEquals(3, json.length());
		assertNotNull(json.optJSONObject(ID));
		assertNotNull(json.optJSONObject(CONTEXT_PARAMETERS));
		assertNotNull(json.optJSONObject(EXPERIENCE_CLOUD));
	}

	@Test
	public void getDefaultJsonObject_CustomParameters() {
		// setup
		Map<String, Object> idMap = new HashMap<String, Object>() {
			{
				put("id", "customId");
			}
		};

		Map<String, Object> contextMap = new HashMap<String, Object>() {
			{
				put("context", "customContext");
			}
		};

		Map<String, Object> experienceCloudId = new HashMap<String, Object>() {
			{
				put("ecid", "customEcid");
			}
		};

		// test
		JSONObject json = targetRequestBuilder.getDefaultJsonObject(idMap, contextMap, experienceCloudId,
				1234L, getIdentitySharedState("mcid", "blob", "locationHint", new ArrayList<>()));

		// verify
		assertEquals(new JSONObject(idMap).toString(), json.optJSONObject(ID).toString());
        assertEquals(new JSONObject(contextMap).toString(), json.optJSONObject(CONTEXT_PARAMETERS).toString());
		assertEquals(new JSONObject(experienceCloudId).toString(), json.optJSONObject(EXPERIENCE_CLOUD).toString());
		assertEquals(1234L, json.optLong(ENVIRONMENT_ID));
	}

	@Test
	public void getDefaultJsonObject_WithCustomVisitorIDs() throws JSONException {
		// test
		JSONObject json = targetRequestBuilder.getDefaultJsonObject(null, null, null, 1234L, getIdentitySharedState("mcid", "blob", "locationHint", new ArrayList<Map<String,Object>>(){{
			add(new HashMap<String, Object>(){{
				put(VISITOR_IDS_ID, "someID1" );
				put(VISITOR_IDS_ID_TYPE, "someType1" );
				put(VISITOR_IDS_ID_ORIGIN, "someOrigin1" );
				put(VISITOR_IDS_STATE, 1);
			}});
			add(new HashMap<String, Object>(){{
				put(VISITOR_IDS_ID, "someID2" );
				put(VISITOR_IDS_ID_TYPE, "someType2" );
				put(VISITOR_IDS_ID_ORIGIN, "someOrigin2" );
				put(VISITOR_IDS_STATE, 2);
			}});
			add(new HashMap<String, Object>(){{
				put(VISITOR_IDS_ID, "someID3" );
				put(VISITOR_IDS_ID_TYPE, "someType3" );
				put(VISITOR_IDS_STATE, 3);
			}});
		}}));

		// verify
		assertEquals(1234L, json.optLong(ENVIRONMENT_ID));
		JSONObject id  = json.optJSONObject(ID);
		JSONArray visitorIDList  = id.optJSONArray(CUSTOMER_IDS);
		JSONObject firstVisitorID = (JSONObject) visitorIDList.get(0);
		JSONObject secondVisitorID = (JSONObject) visitorIDList.get(1);

		assertEquals("someID1", firstVisitorID.get(CUSTOMER_ID_ID));
		assertEquals("someType1", firstVisitorID.get(CUSTOMER_ID_INTEGRATION_CODE));
		assertEquals("authenticated", firstVisitorID.get(CUSTOMER_ID_AUTHENTICATION_STATE));

		assertEquals("someID2", secondVisitorID.get(CUSTOMER_ID_ID));
		assertEquals("someType2", secondVisitorID.get(CUSTOMER_ID_INTEGRATION_CODE));
		assertEquals("logged_out", secondVisitorID.get(CUSTOMER_ID_AUTHENTICATION_STATE));
	}

	@Test
	public void getDefaultJsonObject_CustomVisitorIDs_withInvalidValueTypes() throws JSONException {
		// test
		JSONObject json = targetRequestBuilder.getDefaultJsonObject(null, null, null, 1234L, getIdentitySharedState("mcid", "blob", "locationHint", new ArrayList<Map<String,Object>>(){{
			add(new HashMap<String, Object>(){{
				put(VISITOR_IDS_ID, false);
				put(VISITOR_IDS_ID_TYPE, 788);
				put(VISITOR_IDS_STATE, "invalid");
			}});
		}}));

		// verify
		JSONObject id  = json.optJSONObject(ID);
		JSONArray visitorIDList  = id.optJSONArray(CUSTOMER_IDS);
		assertEquals(0, visitorIDList.length());
	}

    // ===================================
    // Helpers
    // ===================================
    List<TargetRequest> getTargetRequestList(int count) {
        List<TargetRequest> targetRequestList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final String mboxName = "mbox" + i;
            TargetRequest targetRequest = new TargetRequest(mboxName, null, "default", callback -> {});
            targetRequestList.add(targetRequest);
        }

        return targetRequestList;
    }

    List<TargetPrefetch> getTargetPrefetchList(int count) {
        List<TargetPrefetch> targetPrefetchList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final String mboxName = "mbox" + i;
            TargetPrefetch targetPrefetch = new TargetPrefetch(mboxName, null);
            targetPrefetchList.add(targetPrefetch);
        }

        return targetPrefetchList;
    }

    Map<String, Object> getIdentitySharedState(final String marketingCloudId,
                                               final String blob, final String locationHint,
                                               final List<Map<String,Object>> customerIds) {
        return new HashMap<String, Object>() {
            {
                put(VISITOR_ID_MID, marketingCloudId);
                put(VISITOR_ID_BLOB, blob);
                put(VISITOR_ID_LOCATION_HINT, locationHint);
                put(VISITOR_IDS_LIST, customerIds);
            }
        };
    }
}
