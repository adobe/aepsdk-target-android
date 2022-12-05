/*
 * **********************************************************************
 *
 *   ADOBE CONFIDENTIAL
 *   ___________________
 *
 *  Copyright 2018 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 *  NOTICE:  All information contained herein is, and remains
 *  the property of Adobe Systems Incorporated and its suppliers,
 *  if any.  The intellectual and technical concepts contained
 *  herein are proprietary to Adobe Systems Incorporated and its
 *  suppliers and are protected by trade secret or copyright law.
 *  Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from Adobe Systems Incorporated.
 *
 *  **************************************************************************
 */

package com.adobe.marketing.mobile.target;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.VisitorID;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Log;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TargetRequestBuilderTest {
	static String ID = "id";
	static String TNT_ID = "tntId";
	static String THIRD_PARTY_ID = "thirdPartyId";
	static String MARKETING_CLOUD_ID = "marketingCloudVisitorId";
	static String EXPERIENCE_CLOUD = "experienceCloud";
	static String CUSTOMER_IDS = "customerIds";
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
	static String BLOB = "blob";
	static final String VIEWS = "views";
	static String PROPERTY = "property";
	static String TOKEN = "token";
	static String TOKENS = "tokens";
	static String LOCATION_HINT = "locationHint";
	static String INTEGRATION_CODE = "integrationCode";
	static String AUTHENTICATION_STATE = "authenticatedState";


	static String TIMESTAMP = "timestamp";
	static String CONTENT = "content";
	static String A4T = "clientSideAnalyticsLoggingPayload";
	static String ID_CUSTOMER_IDS = "customerIds";
	static String METRIC_TYPE = "type";
	static String METRIC_TYPE_DISPLAY = "display";
	static String METRIC_TYPE_CLICK = "click";
	static String PREVIEW_QA_MODE = "qaMode";

	@Mock
	Log fakeLoggingService;

	@Mock
	TargetPreviewManager mockTargetPreviewManager;

	@Mock
	DeviceInforming mockDeviceInfoService;

	TargetRequestBuilder targetRequestBuilder;

	@Before
	public void beforeEach() {
		targetRequestBuilder = new TargetRequestBuilder(mockDeviceInfoService, mockTargetPreviewManager);
	}

	// ===================================
	// Test setIdentityParameters
	// ===================================
	@Test
	public void setIdentityParameters_MidNotInJson_When_MidIsEmptyOrNull() {
		targetRequestBuilder.setIdentityParameters("", "", "", null);
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(ID));


		targetRequestBuilder.setIdentityParameters("", "", "", null);
		json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(ID));
	}

	@Test
	public void setIdentityParameters_MidInJson_When_MidIsValid() throws JsonException {
		targetRequestBuilder.setIdentityParameters("mid", "", "", null);
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertEquals(json.getJSONObject(ID).getString(MARKETING_CLOUD_ID), "mid");
	}

	@Test
	public void setIdentityParameters_BlobNotInJson_When_BlobIsEmptyOrNull() throws JsonException {
		targetRequestBuilder.setIdentityParameters("", "", "", null);
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS));

		targetRequestBuilder.setIdentityParameters("", null, "", null);
		json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS));
	}

	@Test
	public void setIdentityParameters_BlobInJson_When_BlobIsValid() throws JsonException {
		targetRequestBuilder.setIdentityParameters("", "blob", "", null);
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertEquals(json.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS).getString(BLOB), "blob");
	}

	@Test
	public void setIdentityParameters_LocationHintInJson_When_LocationHintIsValid() throws JsonException {
		targetRequestBuilder.setIdentityParameters("", "", "LocationHint", null);
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertEquals(json.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS).getString(LOCATION_HINT),
					 "LocationHint");
	}

	@Test
	public void setIdentityParameters_LocationHintNotInJson_When_LocationHintIsEmptyOrNull() throws JsonException {
		targetRequestBuilder.setIdentityParameters("", "", "", null);
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS));


		targetRequestBuilder.setIdentityParameters("", "", null, null);
		json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(EXPERIENCE_CLOUD).optJSONObject(AAM_PARAMETERS));

	}

	@Test
	public void setIdentityParameters_CustomerIdsInJson_When_CustomerIdsIsValid() throws JsonException {

		List<VisitorID> testVisitorIDList = new ArrayList<VisitorID>();
		testVisitorIDList.add(new VisitorID("d_cid_ic", "id1", "hodor",
											VisitorID.AuthenticationState.AUTHENTICATED));
		targetRequestBuilder.setIdentityParameters("", "", "", testVisitorIDList);
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertEquals(json.optJSONObject(ID).optJSONArray(ID_CUSTOMER_IDS).getJSONObject(0).get(INTEGRATION_CODE), "id1");
		assertEquals(json.optJSONObject(ID).optJSONArray(ID_CUSTOMER_IDS).getJSONObject(0).get(AUTHENTICATION_STATE),
					 "authenticated");
	}

	@Test
	public void setIdentityParameters_CustomerIdsNotInJson_When_CustomerIdsIsEmptyOrNull() throws JsonException {

		List<VisitorID> testVisitorIDList = new ArrayList<VisitorID>();
		targetRequestBuilder.setIdentityParameters("", "", "", testVisitorIDList);
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(ID));

		targetRequestBuilder.setIdentityParameters("", "", "", null);
		json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(ID));
	}


	// ===================================
	// Test setConfigParameters
	// ===================================
	@Test
	public void setConfigParameters_EnvironmentIdInJson_When_EnvironmentIdIsValid() {
		targetRequestBuilder.setConfigParameters(1234);
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertEquals(json.opt(ENVIRONMENT_ID), 1234l);
	}

	@Test
	public void setConfigParameters_EnvironmentIdNotInJson_When_EnvironmentIdIsZero() {
		targetRequestBuilder.setConfigParameters(0);
		JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.opt(ENVIRONMENT_ID));
	}

	// ===================================
	// Test setTargetInternalParameters
	// ===================================

	@Test
	public void seTargetInternalParameters_TntIdInJson_When_TntIdIsValid() {
		targetRequestBuilder.setTargetInternalParameters("tntid", "");
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertEquals(json.optJSONObject(ID).optString(TNT_ID, ""), "tntid");
	}

	@Test
	public void seTargetInternalParameters_TntIdNotInJson_When_TntIdIsEmptyOrNull() {
		targetRequestBuilder.setTargetInternalParameters("", "");
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(ID));


		targetRequestBuilder.setTargetInternalParameters(null, "");
		json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(ID));
	}

	@Test
	public void seTargetInternalParameters_ThirdPartyIdInJson_When_ThirdPartyIdIsValid() {
		targetRequestBuilder.setTargetInternalParameters("", "thirdPartyId");
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertEquals(json.optJSONObject(ID).optString(THIRD_PARTY_ID, ""), "thirdPartyId");
	}

	@Test
	public void seTargetInternalParameters_ThirdPartyIdNotInJson_When_ThirdPartyIdIsEmptyOrNull() {
		targetRequestBuilder.setTargetInternalParameters("", "");
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(ID));


		targetRequestBuilder.setTargetInternalParameters("", null);
		json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(ID));
	}

	// ===================================
	// Test getRequestPayload_notification
	// ===================================
	@Test
	public void getRequestPayload_NotificationInJson_When_NotificationListContainsOneObject() {

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null,
		new ArrayList<JsonUtilityService.JSONObject>() {
			{
				add(jsonUtilityService.createJSONObject("{}"));
			}
		}, null);
		assertEquals(1, json.optJSONArray(VISITED_MBOXES).length());
		assertEquals("{}", json.optJSONArray(VISITED_MBOXES).optJSONObject(0).toString());
	}

	@Test
	public void getRequestPayload_NotificationInJson_When_NotificationListContainsTwoObjects() {

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null,
		new ArrayList<JsonUtilityService.JSONObject>() {
			{
				add(jsonUtilityService.createJSONObject("{}"));
				add(jsonUtilityService.createJSONObject("{\"id\":1}"));
			}
		}, null);
		assertEquals(2, json.optJSONArray(VISITED_MBOXES).length());
		assertEquals("{}", json.optJSONArray(VISITED_MBOXES).optJSONObject(0).toString());
		assertEquals("{\"id\":1}", json.optJSONArray(VISITED_MBOXES).optJSONObject(1).toString());
	}

	@Test
	public void getRequestPayload_NotificationNotInJson_When_NotificationListIsEmpytOrNull() {

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null,
											 new ArrayList<JsonUtilityService.JSONObject>(), null);
		assertNull(json.optJSONArray(VISITED_MBOXES));

		json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONArray(VISITED_MBOXES));
	}

	// ===================================
	// Test getRequestPayload_preview
	// ===================================
	@Test
	public void getRequestPayload_PreviewInJson_When_NotificationListContainsOneObject() {
		mockTargetPreviewManager.getPreviewTokenReturnValue = "previewToken";
		mockTargetPreviewManager.getPreviewParametersReturnValue = "{\"qaMode\":{\"key\":\"value\"}}";

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertEquals("value", json.optJSONObject(PREVIEW_QA_MODE).opt("key"));
	}

	@Test
	public void getRequestPayload_PreviewInJson_When_NotificationListContainsOneObject_PreviewTokenIsEmpty() {
		mockTargetPreviewManager.getPreviewTokenReturnValue = "";
		mockTargetPreviewManager.getPreviewParametersReturnValue = "{\"qaMode\":{\"key\":\"value\"}}";

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertEquals("value", json.optJSONObject(PREVIEW_QA_MODE).opt("key"));
	}

	@Test
	public void getRequestPayload_PreviewNotInJson_When_PreviewTokenIsNull() {

		mockTargetPreviewManager.getPreviewTokenReturnValue = null;
		mockTargetPreviewManager.getPreviewParametersReturnValue = "{\"qaMode\":{\"key\":\"value\"}}";

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.optJSONObject(PREVIEW_QA_MODE));

	}

	// ===================================
	// Test getRequestPayload_orderParameters
	// ===================================
	@Test
	public void getRequestPayload_OrderParametersInJson_When_OrderParametersAreValid() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.setOrderParameters(new HashMap<String, Object>() {
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
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
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
	public void getRequestPayload_NoOrderIdInJson_When_OrderIdIsEmpty() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setOrderParameters(new HashMap<String, Object>() {
			{
				put(ID, "");
				put(ORDER_TOTAL, 0.1);
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).opt(ID));
		assertEquals(0.1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).opt(
						 ORDER_TOTAL));

	}

	@Test
	public void getRequestPayload_NoOrderIdInJson_When_OrderIdIsNull() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setOrderParameters(new HashMap<String, Object>() {
			{
				put(ID, null);
				put(ORDER_TOTAL, 0.1);
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
	}

	@Test
	public void getRequestPayload_NoOrderTotalInJson_When_OrderTotalIsEmpty() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setOrderParameters(new HashMap<String, Object>() {
			{
				put(ID, "orderId");
				put(ORDER_TOTAL, null);
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).opt(
					   ORDER_TOTAL));
	}

	@Test
	public void getRequestPayload_NoOrderTotalInJson_When_OrderTotalIsNull() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setOrderParameters(new HashMap<String, Object>() {
			{
				put(ID, "orderId");
				put(ORDER_TOTAL, null);
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).opt(
					   ORDER_TOTAL));
	}

	@Test
	public void getRequestPayload_NoOrderProductArrayInJson_When_ProductIdsIsEmpty() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setOrderParameters(new HashMap<String, Object>() {
			{
				put(ID, "orderId");
				put(ORDER_TOTAL, null);
				put(PURCHASED_PRODUCT_IDS, new ArrayList<Object>() {
					{
					}
				});
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).optJSONArray(
					   PURCHASED_PRODUCT_IDS));
	}

	@Test
	public void getRequestPayload_NoOrderProductArrayInJson_When_ProductIdsOnlyContainsNoneString() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setOrderParameters(new HashMap<String, Object>() {
			{
				put(ID, "orderId");
				put(ORDER_TOTAL, null);
				put(PURCHASED_PRODUCT_IDS, new ArrayList<Object>() {
					{
						add(new Object());
					}
				});
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertNotNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						  ORDER).optJSONArray(PURCHASED_PRODUCT_IDS));
	}

	@Test
	public void getRequestPayload_OrderParametersInJson_When_OrderTotalIsStringDouble() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.setOrderParameters(new HashMap<String, Object>() {
			{
				put(ID, "orderId");
				put(ORDER_TOTAL, "0.1");
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals(0.1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).opt(
						 ORDER_TOTAL));
		assertEquals("orderId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 ORDER).opt(ID));
	}

	@Test
	public void getRequestPayload_OrderParametersInJson_When_OrderTotalIsRandomString() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.setOrderParameters(new HashMap<String, Object>() {
			{
				put(ID, "orderId");
				put(ORDER_TOTAL, "total");
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER).opt(
					   ORDER_TOTAL));
		assertEquals("orderId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 ORDER).opt(ID));
	}

	// ===================================
	// Test getRequestPayload_ProductParameters
	// ===================================
	@Test
	public void getRequestPayload_ProductParametersInJson_When_ProductParametersIsValid() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();
		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setProductParameters(new HashMap<String, String>() {
			{
				put(ID, "productId");
				put(CATEGORY_ID, "categoryId");
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("productId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(ID));
		assertEquals("categoryId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(CATEGORY_ID));

	}

	@Test
	public void getRequestPayload_ProductIdNotInJson_When_ProductIdIsNull() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();
		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setProductParameters(new HashMap<String, String>() {
			{
				put(ID, null);
				put(CATEGORY_ID, "categoryId");
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT));

	}

	@Test
	public void getRequestPayload_ProductIdInJson_When_ProductIdIsEmpty() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();
		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setProductParameters(new HashMap<String, String>() {
			{
				put(ID, "");
				put(CATEGORY_ID, "categoryId");
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT).opt(
					   ID));
		assertEquals("categoryId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(CATEGORY_ID));

	}

	@Test
	public void getRequestPayload_ProductCategoryInJson_When_ProductCategoryIsEmpty() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();
		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setProductParameters(new HashMap<String, String>() {
			{
				put(ID, "productId");
				put(CATEGORY_ID, "");
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("productId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(ID));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT).opt(
					   CATEGORY_ID));

	}

	@Test
	public void getRequestPayload_ProductCategoryNotInJson_When_ProductCategoryIsNull() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();
		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.setProductParameters(new HashMap<String, String>() {
			{
				put(ID, "productId");
				put(CATEGORY_ID, null);
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
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
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");
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
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, prefetch, null, null,
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
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");
		final Map<String, Object> prefetch = new HashMap<String, Object>();

		// test
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, prefetch, null, null,
											 null);

		// verify
		assertNull(json.optJSONArray(PREFETCH_MBOXES));
	}

	@Test
	public void getRequestPayload_NoPrefetchInJson_When_NullPrefetch() {
		// setup
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");

		// test
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
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
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");
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
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, execute, null,
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
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");
		final Map<String, Object> execute = new HashMap<String, Object>();

		// test
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, execute, null,
											 null);

		// verify
		assertNull(json.optJSONArray(EXECUTE_MBOXES));
	}

	@Test
	public void getRequestPayload_NoExecuteInJson_When_NullExecute() {
		// setup
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");

		// test
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
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
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");
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
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, notifications,
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
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");
		final List<Map<String, Object>> notifications = new ArrayList<>();

		// test
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, notifications,
											 null);

		// verify
		assertNull(json.optJSONArray(VISITED_MBOXES));
	}

	@Test
	public void getRequestPayload_NoNotificationsInJson_When_NullNotifications() {
		// setup
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");

		// test
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
											 null);

		// verify
		assertNull(json.optJSONArray(VISITED_MBOXES));
	}

	// ===================================
	// Test getRequestPayload_property
	// ===================================
	@Test
	public void getRequestPayload_PropertyTokenInJson_When_validToken() throws JsonException {
		// setup
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");

		// test
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
											 "randomPropertyToken");

		// verify
		assertNotNull(json.optJSONObject(PROPERTY));
		assertEquals(1, json.optJSONObject(PROPERTY).length());
		assertEquals("randomPropertyToken", json.getJSONObject(PROPERTY).opt(TOKEN));
	}

	@Test
	public void getRequestPayload_NoPropertyTokenInJson_When_nullToken() {
		// setup
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");

		// test
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
											 null);

		// verify
		assertNull(json.optJSONObject(PROPERTY));
	}

	@Test
	public void getRequestPayload_NoPropertyTokenInJson_When_EmptyToken() {
		// setup
		final JsonUtilityService.JSONObject defaultJson = jsonUtilityService.createJSONObject("{}");

		// test
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(defaultJson, null, null, null,
											 "");

		// verify
		assertNull(json.optJSONObject(PROPERTY));
	}

	// ===================================
	// Test clean
	// ===================================
	@Test
	public void clean_cleanAllParameters() throws JsonException {
		List<VisitorID> testVisitorIDList = new ArrayList<VisitorID>();
		testVisitorIDList.add(new VisitorID("d_cid_ic", "id1", "hodor",
											VisitorID.AuthenticationState.AUTHENTICATED));
		targetRequestBuilder.setIdentityParameters("test", "test", "test", testVisitorIDList);
		targetRequestBuilder.setConfigParameters(1234);
		targetRequestBuilder.setLifecycleParameters(new HashMap<String, String>() {
			{
				put("key", "value");
			}
		});
		targetRequestBuilder.setTargetInternalParameters("test", "test");
		targetRequestBuilder.setConfigParameters(1234);

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNotNull(json.opt(ENVIRONMENT_ID));
		assertNotNull(json.getJSONObject(EXPERIENCE_CLOUD).opt(AAM_PARAMETERS));
		assertNotNull(json.opt(ID));

		targetRequestBuilder.clean();
		json = targetRequestBuilder.getRequestPayload(null, null, false, null, null, null);
		assertNull(json.opt(ENVIRONMENT_ID));
		assertNull(json.getJSONObject(EXPERIENCE_CLOUD).opt(AAM_PARAMETERS));
		assertNull(json.opt(ID));

	}

	@Test
	public void getRequestPayload_PropertyTokenNotInJson_When_TokenIsNull() {
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(1), false, null,
											 null, null);
		assertNull(json.optJSONObject(PROPERTY));
	}

	@Test
	public void getRequestPayload_PropertyTokenNotInJson_When_TokenIsEmpty() {
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(1), false, null,
											 null, "");
		assertNull(json.optJSONObject(PROPERTY));
	}

	@Test
	public void getRequestPayload_PropertyTokenInJson_When_TokenIsNotNull() throws JsonException {
		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, getTargetRequestList(1), false, null,
											 null, "randomPropertyToken");
		assertNotNull(json.optJSONObject(PROPERTY));
		assertEquals(1, json.optJSONObject(PROPERTY).length());
		assertEquals("randomPropertyToken", json.getJSONObject(PROPERTY).opt(TOKEN));
	}

	// ===================================
	// Test getDisplayNotificationJsonObject
	// ===================================
	@Test
	public void getDisplayNotificationJsonObject_ReturnNull_When_InputJsonIsNull() {
		JsonUtilityService.JSONObject json = targetRequestBuilder.getDisplayNotificationJsonObject(null, null, null, 123L);
		assertNull(json);
	}

	@Test
	public void getDisplayNotificationJsonObject_ReturnObjectWithTimeStampAndMetricType1_When_InputJsonIsNotNull() throws
		JsonException {
		JsonUtilityService.JSONObject json = jsonUtilityService.createJSONObject("{}");
		json.put(CONTENT, "content");
		json.put(A4T, "A4T");
		json.put("key", "value");
		JsonUtilityService.JSONObject visitedMboxNode = targetRequestBuilder.getDisplayNotificationJsonObject("mboxName", json,
				null, 123L);
		assertNull(visitedMboxNode.opt(CONTENT));
		assertNull(visitedMboxNode.opt(A4T));
		assertEquals(METRIC_TYPE_DISPLAY, visitedMboxNode.opt(METRIC_TYPE));
		assertEquals(123L, visitedMboxNode.opt(TIMESTAMP));
	}

	// ===================================
	// Test getClickNotificationJsonObject
	// ===================================
	@Test
	public void getClickNotificationJsonObject_ReturnObjectWithTimeStampAndMetricType_When_InputJsonIsNull() {
		JsonUtilityService.JSONObject json = targetRequestBuilder.getClickNotificationJsonObject((
				JsonUtilityService.JSONObject) null, null, 123L);
		assertNotNull(json);
		assertEquals(123L, json.opt(TIMESTAMP));
		assertEquals(METRIC_TYPE_CLICK, json.opt(METRIC_TYPE));
	}

	// ===================================
	// Test getViewNotifications
	// ===================================
	@Test
	public void getViewNotifications_Happy() {
		List<Object> notificationsList = new ArrayList<Object>();
		HashMap<String, Object> notification1 = new HashMap<String, Object>();
		notification1.put("id", "random1");
		notification1.put("timestamp", 123L);
		notification1.put("type", "display");
		HashMap<String, String> viewParameters1 = new HashMap<String, String>();
		viewParameters1.put("name", "view1");
		viewParameters1.put("id", "id1");
		viewParameters1.put("key", "key1");
		notification1.put("viewparameters", viewParameters1);
		List<String> tokenList1 = new ArrayList<String>();
		tokenList1.add("token1");
		notification1.put("tokens", tokenList1);
		HashMap<String, Object> notification2 = new HashMap<String, Object>();
		notification2.put("id", "random2");
		notification2.put("timestamp", 456L);
		notification2.put("type", "click");
		HashMap<String, String> viewParameters2 = new HashMap<String, String>();
		viewParameters2.put("name", "view2");
		viewParameters2.put("id", "id2");
		viewParameters2.put("key", "key2");
		notification2.put("viewparameters", viewParameters2);
		List<String> tokenList2 = new ArrayList<String>();
		tokenList2.add("token2");
		notification2.put("tokens", tokenList2);
		notificationsList.add(notification1);
		notificationsList.add(notification2);
		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(2, notificationJsonList.size());
	}

	@Test
	public void getViewNotifications_Return_Empty_List_when_notificationsList_is_empty() {
		List<Object> notificationsList = new ArrayList<Object>();
		notificationsList.add(jsonUtilityService.createJSONObject("{}"));

		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(0, notificationJsonList.size());
	}

	@Test
	public void getViewNotifications_Return_Empty_List_when_notificationsList_is_null() {
		List<Object> notificationsList = null;

		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(0, notificationJsonList.size());
	}

	@Test
	public void getViewNotifications_Return_Empty_List_when_notificationList_contains_missing_timestamp() {
		List<Object> notificationsList = new ArrayList<Object>();
		HashMap<String, Object> notification1 = new HashMap<String, Object>();
		notification1.put("id", "random1");
		notification1.put("type", "display");
		HashMap<String, String> viewParameters = new HashMap<String, String>();
		viewParameters.put("name", "view1");
		viewParameters.put("id", "id1");
		viewParameters.put("key", "key1");
		notification1.put("viewparameters", viewParameters);
		notificationsList.add(notification1);
		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(0, notificationJsonList.size());
	}

	@Test
	public void getViewNotifications_Return_Empty_List_when_notificationList_contains_invalid_timestamp() {
		List<Object> notificationsList = new ArrayList<Object>();
		HashMap<String, Object> notification1 = new HashMap<String, Object>();
		notification1.put("id", "random1");
		notification1.put("type", "display");
		notification1.put("timestamp", 123.45);
		HashMap<String, String> viewParameters = new HashMap<String, String>();
		viewParameters.put("name", "view1");
		viewParameters.put("id", "id1");
		viewParameters.put("key", "key1");
		notification1.put("viewparameters", viewParameters);
		notificationsList.add(notification1);
		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(0, notificationJsonList.size());
	}

	@Test
	public void getViewNotifications_Return_Empty_List_when_notificationList_contains_missing_id() {
		List<Object> notificationsList = new ArrayList<Object>();
		HashMap<String, Object> notification1 = new HashMap<String, Object>();
		notification1.put("type", "display");
		notification1.put("timestamp", 123L);
		HashMap<String, String> viewParameters = new HashMap<String, String>();
		viewParameters.put("name", "view1");
		viewParameters.put("id", "id1");
		viewParameters.put("key", "key1");
		notification1.put("viewparameters", viewParameters);
		notificationsList.add(notification1);
		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(0, notificationJsonList.size());
	}

	@Test
	public void getViewNotifications_Return_Empty_List_when_notificationList_contains_missing_type() {
		List<Object> notificationsList = new ArrayList<Object>();
		HashMap<String, Object> notification1 = new HashMap<String, Object>();
		notification1.put("id", "random1");
		notification1.put("timestamp", 123L);
		HashMap<String, String> viewParameters = new HashMap<String, String>();
		viewParameters.put("name", "view1");
		viewParameters.put("id", "id1");
		viewParameters.put("key", "key1");
		notification1.put("viewparameters", viewParameters);
		notificationsList.add(notification1);
		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(0, notificationJsonList.size());
	}

	@Test
	public void getViewNotifications_Return_Empty_List_when_notificationList_contains_invalid_type() {
		List<Object> notificationsList = new ArrayList<Object>();
		HashMap<String, Object> notification1 = new HashMap<String, Object>();
		notification1.put("id", "random1");
		notification1.put("type", "randomType");
		notification1.put("timestamp", 123L);
		HashMap<String, String> viewParameters = new HashMap<String, String>();
		viewParameters.put("name", "view1");
		viewParameters.put("id", "id1");
		viewParameters.put("key", "key1");
		notification1.put("viewparameters", viewParameters);
		notificationsList.add(notification1);
		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(0, notificationJsonList.size());
	}

	@Test
	public void getViewNotifications_Return_Empty_List_when_notificationList_contains_missing_viewparameters() {
		List<Object> notificationsList = new ArrayList<Object>();
		HashMap<String, Object> notification1 = new HashMap<String, Object>();
		notification1.put("id", "random1");
		notification1.put("type", "display");
		notification1.put("timestamp", 123L);
		notificationsList.add(notification1);
		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(0, notificationJsonList.size());
	}

	@Test
	public void getViewNotifications_Return_Empty_List_when_notificationList_contains_empty_viewparameters() {
		List<Object> notificationsList = new ArrayList<Object>();
		HashMap<String, Object> notification1 = new HashMap<String, Object>();
		notification1.put("id", "random1");
		notification1.put("type", "display");
		notification1.put("timestamp", 123L);
		HashMap<String, String> viewParameters = new HashMap<String, String>();
		notification1.put("viewparameters", viewParameters);
		notificationsList.add(notification1);
		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(0, notificationJsonList.size());
	}

	@Test
	public void getViewNotifications_Return_Empty_List_when_notificationList_contains_invalid_viewparameters() {
		List<Object> notificationsList = new ArrayList<Object>();
		HashMap<String, Object> notification1 = new HashMap<String, Object>();
		notification1.put("id", "random1");
		notification1.put("type", "display");
		notification1.put("timestamp", 123L);
		notification1.put("viewparameters", "randomViewParameters");
		notificationsList.add(notification1);
		List<JsonUtilityService.JSONObject> notificationJsonList = targetRequestBuilder.getViewNotifications(
					notificationsList);
		assertNotNull(notificationJsonList);
		assertEquals(0, notificationJsonList.size());
	}

	// ===================================
	// Test getRequestPayload_MBoxParameters
	// ===================================
	@Test
	public void getRequestPayload_MboxParametersInJson_When_MboxParametersAreValid() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.setMboxParameters(new HashMap<String, String>() {
			{
				put("mBox-parameter-Key1", "mBox-Value1");
				put("mBox-parameter-Key2", "mBox-Value2");
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("mBox-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PARAMETERS).opt("mBox-parameter-Key1"));
		assertEquals("mBox-Value2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PARAMETERS).opt("mBox-parameter-Key2"));
	}

	@Test
	public void getRequestPayload_Mboxparameter_Key1_NotInJson_When_parameter_Key1_ValueIsNull() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();
		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.setMboxParameters(new HashMap<String, String>() {
			{
				put("mBox-parameter-Key1", null);
				put("mBox-parameter-Key2", "mBox-Value2");
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
					   PARAMETERS).optString("mBox-parameter-Key1", null));
		assertEquals("mBox-Value2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PARAMETERS).opt("mBox-parameter-Key2"));
	}

	@Test
	public void getRequestPayload_Mboxparameter_When_Mboxparameters_Are_Empty() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();
		final String mboxName = "mbox";
		Map<String, String> mBoxParameters = new HashMap<String, String>();
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.setMboxParameters(mBoxParameters).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
					   PARAMETERS));
	}

	// ===================================
	// Test getRequestPayload_ProfileParameters
	// ===================================
	@Test
	public void getRequestPayload_ProfileParametersInJson_When_ProfileParametersAreValid() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.setProfileParameters(new HashMap<String, String>() {
			{
				put("profile-parameter-Key1", "profile-Value1");
				put("profile-parameter-Key2", "profile-Value2");
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertEquals("profile-Value2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
	}

	@Test
	public void getRequestPayload_Profileparameter_Key1_NotInJson_When_parameter_Key1_ValueIsNull() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();
		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.setProfileParameters(new HashMap<String, String>() {
			{
				put("profile-parameter-Key1", null);
				put("profile-parameter-Key2", "profile-Value2");
			}
		}).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
					   PROFILE_PARAMETERS).optString("profile-parameter-Key1", null));
		assertEquals("profile-Value2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
	}

	@Test
	public void getRequestPayload_Profileparameter_When_Profileparameters_are_Empty() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();
		final String mboxName = "mbox";
		Map<String, String> profileParameters = new HashMap<String, String>();
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.setProfileParameters(profileParameters).build();
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
					   PROFILE_PARAMETERS));
	}

	// ===================================
	// Test getRequestPayload_Target_Parameters
	// ===================================
	@Test
	public void getRequestPayload_ParametersInJson_When_TargetParametersWithMboxParameterAreValid() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<String, String>();
		mboxParameters.put("mBox-parameter-Key1", "mBox-Value1");
		mboxParameters.put("mBox-parameter-Key2", "mBox-Value2");
		TargetParameters targetParameters = new TargetParameters.Builder().parameters(
			mboxParameters).build();
		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.build();
		targetRequest.setTargetParameters(targetParameters);
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("mBox-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PARAMETERS).opt("mBox-parameter-Key1"));
		assertEquals("mBox-Value2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PARAMETERS).opt("mBox-parameter-Key2"));

	}

	@Test
	public void getRequestPayload_ParametersInJson_When_TargetParametersWithMboxAndOrderParameterAreValid() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<String, String>();
		mboxParameters.put("mBox-parameter-Key1", "mBox-Value1");
		mboxParameters.put("mBox-parameter-Key2", "mBox-Value2");
		TargetParameters targetParameters = new TargetParameters.Builder().parameters(
		mboxParameters).order(TargetOrder.fromMap(new HashMap<String, Object>() {
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
		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.build();
		targetRequest.setTargetParameters(targetParameters);
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
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
	public void getRequestPayload_ParametersInJson_When_TargetParametersWithProfileParameterAreValid() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		profileParameters.put("profile-parameter-Key2", "profile-Value2");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).build();
		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.build();
		targetRequest.setTargetParameters(targetParameters);
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertEquals("profile-Value2", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key2"));

	}

	@Test
	public void getRequestPayload_ParametersInJson_When_TargetParametersWithProductParameterAreValid() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		TargetParameters targetParameters = new TargetParameters.Builder().product(
		TargetProduct.fromMap(new HashMap<String, String>() {
			{
				put(ID, "productId");
				put(CATEGORY_ID, "categoryId");
			}
		})).build();
		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.build();
		targetRequest.setTargetParameters(targetParameters);
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("productId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(ID));
		assertEquals("categoryId", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(CATEGORY_ID));

	}


	@Test
	public void getRequestPayload_ParametersInJson_When_TargetParametersAreEmpty() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		TargetParameters targetParameters = new TargetParameters.Builder().build();
		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.build();
		targetRequest.setTargetParameters(targetParameters);
		targetRequestList.add(targetRequest);

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PROFILE_PARAMETERS));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
	}

	@Test
	public void getRequestPayload_ParametersInJson_When_TargetParametersAreNull() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		TargetRequest targetRequest = new TargetRequest.Builder("mbox", "default")
		.build();
		targetRequest.setTargetParameters(null);
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(ORDER));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PRODUCT));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PROFILE_PARAMETERS));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
	}

	@Test
	public void getRequestPayload_PropertyParametersInJson_When_ATPropertyInMboxParameters() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", "randomPropertyToken");

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();
		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.build();
		targetRequest.setTargetParameters(targetParameters);
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
		assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
		assertEquals("randomPropertyToken", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
	}

	@Test
	public void
	getRequestPayload_TargetRequestBuiltUsingConstructor_PropertyParametersNotInJson_When_NullATPropertyInMboxParameters() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", null);

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();
		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest(mboxName, targetParameters, "default", (AdobeCallback) null);
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
		assertNull(json.optJSONObject(TargetJson.PROPERTY));
	}

	@Test
	public void getRequestPayload_PropertyParametersNotInJson_When_NullATPropertyInMboxParameters() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", null);

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();
		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.build();
		targetRequest.setTargetParameters(targetParameters);
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
		assertNull(json.optJSONObject(TargetJson.PROPERTY));
	}

	@Test
	public void getRequestPayload_ForMultipleMboxes_PropertyParametersInJson_When_ATPropertyInMboxParameters() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", "randomPropertyToken");

		Map<String, String> mboxParameters2 = new HashMap<>();
		mboxParameters2.put("at_property", "randomPropertyToken2");

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");

		Map<String, String> profileParameters2 = new HashMap<String, String>();
		profileParameters2.put("profile-parameter-Key2", "profile-Value2");

		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();

		TargetParameters targetParameters2 = new TargetParameters.Builder().profileParameters(
			profileParameters2).parameters(mboxParameters2).build();

		final String mboxNames[] = { "mbox1", "mbox2" };
		TargetRequest targetRequest1 = new TargetRequest.Builder(mboxNames[0], "default")
		.build();
		targetRequest1.setTargetParameters(targetParameters);

		TargetRequest targetRequest2 = new TargetRequest.Builder(mboxNames[1], "default")
		.build();
		targetRequest2.setTargetParameters(targetParameters2);

		targetRequestList.add(targetRequest1);
		targetRequestList.add(targetRequest2);

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
		assertEquals("randomPropertyToken", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
		JsonUtilityService.JSONArray mboxesArray = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES);
		assertEquals(2, mboxesArray.length());
		JsonUtilityService.JSONObject mbox1 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0);
		assertEquals("profile-Value1", mbox1.optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(mbox1.optJSONObject(PARAMETERS));
		JsonUtilityService.JSONObject mbox2 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1);
		assertEquals("profile-Value2", mbox2.optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
		assertNull(mbox2.optJSONObject(PARAMETERS));
	}

	@Test
	public void
	getRequestPayload_ForMultipleMboxes_TargetRequestsBuiltUsingConstructor_PropertyParametersNotInJson_When_NullATPropertyInMboxParameters() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", null);

		Map<String, String> mboxParameters2 = new HashMap<>();
		mboxParameters2.put("at_property", null);

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");

		Map<String, String> profileParameters2 = new HashMap<String, String>();
		profileParameters2.put("profile-parameter-Key2", "profile-Value2");

		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();

		TargetParameters targetParameters2 = new TargetParameters.Builder().profileParameters(
			profileParameters2).parameters(mboxParameters2).build();

		final String mboxNames[] = { "mbox1", "mbox2" };
		TargetRequest targetRequest1 = new TargetRequest(mboxNames[0], targetParameters, "default", (AdobeCallback) null);
		TargetRequest targetRequest2 = new TargetRequest(mboxNames[1], targetParameters2, "default", (AdobeCallback) null);

		targetRequestList.add(targetRequest1);
		targetRequestList.add(targetRequest2);

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertNull(json.optJSONObject(TargetJson.PROPERTY));
		JsonUtilityService.JSONArray mboxesArray = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES);
		assertEquals(2, mboxesArray.length());
		JsonUtilityService.JSONObject mbox1 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0);
		assertEquals("profile-Value1", mbox1.optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(mbox1.optJSONObject(PARAMETERS));
		JsonUtilityService.JSONObject mbox2 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1);
		assertEquals("profile-Value2", mbox2.optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
		assertNull(mbox2.optJSONObject(PARAMETERS));
	}

	@Test
	public void getRequestPayload_ForMultipleMboxes_PropertyParametersNotInJson_When_NullATPropertyInMboxParameters() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", null);

		Map<String, String> mboxParameters2 = new HashMap<>();
		mboxParameters2.put("at_property", null);

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");

		Map<String, String> profileParameters2 = new HashMap<String, String>();
		profileParameters2.put("profile-parameter-Key2", "profile-Value2");

		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();

		TargetParameters targetParameters2 = new TargetParameters.Builder().profileParameters(
			profileParameters2).parameters(mboxParameters2).build();

		final String mboxNames[] = { "mbox1", "mbox2" };
		TargetRequest targetRequest1 = new TargetRequest.Builder(mboxNames[0], "default")
		.build();
		targetRequest1.setTargetParameters(targetParameters);

		TargetRequest targetRequest2 = new TargetRequest.Builder(mboxNames[1], "default")
		.build();
		targetRequest2.setTargetParameters(targetParameters2);

		targetRequestList.add(targetRequest1);
		targetRequestList.add(targetRequest2);

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertNull(json.optJSONObject(TargetJson.PROPERTY));
		JsonUtilityService.JSONArray mboxesArray = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES);
		assertEquals(2, mboxesArray.length());
		JsonUtilityService.JSONObject mbox1 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0);
		assertEquals("profile-Value1", mbox1.optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(mbox1.optJSONObject(PARAMETERS));
		JsonUtilityService.JSONObject mbox2 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1);
		assertEquals("profile-Value2", mbox2.optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
		assertNull(mbox2.optJSONObject(PARAMETERS));
	}

	@Test
	public void
	getRequestPayload_ForMultipleMboxes_TargetRequestsBuiltUsingConstructor_SecondMboxPropertyParametersInJson_When_NullATPropertyInFirstMboxParameters() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", null);

		Map<String, String> mboxParameters2 = new HashMap<>();
		mboxParameters2.put("at_property", "randomPropertyToken2");

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");

		Map<String, String> profileParameters2 = new HashMap<String, String>();
		profileParameters2.put("profile-parameter-Key2", "profile-Value2");

		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();

		TargetParameters targetParameters2 = new TargetParameters.Builder().profileParameters(
			profileParameters2).parameters(mboxParameters2).build();

		final String mboxNames[] = { "mbox1", "mbox2" };
		TargetRequest targetRequest1 = new TargetRequest(mboxNames[0], targetParameters, "default", (AdobeCallback) null);
		TargetRequest targetRequest2 = new TargetRequest(mboxNames[1], targetParameters2, "default", (AdobeCallback) null);

		targetRequestList.add(targetRequest1);
		targetRequestList.add(targetRequest2);

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
		assertEquals("randomPropertyToken2", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
		JsonUtilityService.JSONArray mboxesArray = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES);
		assertEquals(2, mboxesArray.length());
		JsonUtilityService.JSONObject mbox1 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0);
		assertEquals("profile-Value1", mbox1.optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(mbox1.optJSONObject(PARAMETERS));
		JsonUtilityService.JSONObject mbox2 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1);
		assertEquals("profile-Value2", mbox2.optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
		assertNull(mbox2.optJSONObject(PARAMETERS));
	}

	@Test
	public void
	getRequestPayload_ForMultipleMboxes_SecondMboxPropertyParametersInJson_When_NullATPropertyInFirstMboxParameters() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", null);

		Map<String, String> mboxParameters2 = new HashMap<>();
		mboxParameters2.put("at_property", "randomPropertyToken2");

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");

		Map<String, String> profileParameters2 = new HashMap<String, String>();
		profileParameters2.put("profile-parameter-Key2", "profile-Value2");

		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();

		TargetParameters targetParameters2 = new TargetParameters.Builder().profileParameters(
			profileParameters2).parameters(mboxParameters2).build();

		final String mboxNames[] = { "mbox1", "mbox2" };
		TargetRequest targetRequest1 = new TargetRequest.Builder(mboxNames[0], "default")
		.build();
		targetRequest1.setTargetParameters(targetParameters);

		TargetRequest targetRequest2 = new TargetRequest.Builder(mboxNames[1], "default")
		.build();
		targetRequest2.setTargetParameters(targetParameters2);

		targetRequestList.add(targetRequest1);
		targetRequestList.add(targetRequest2);

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
		assertEquals("randomPropertyToken2", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
		JsonUtilityService.JSONArray mboxesArray = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES);
		assertEquals(2, mboxesArray.length());
		JsonUtilityService.JSONObject mbox1 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0);
		assertEquals("profile-Value1", mbox1.optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(mbox1.optJSONObject(PARAMETERS));
		JsonUtilityService.JSONObject mbox2 = json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(1);
		assertEquals("profile-Value2", mbox2.optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
		assertNull(mbox2.optJSONObject(PARAMETERS));
	}

	@Test
	public void getRequestPayload_PropertyParametersInJson_When_ATPropertyInMboxParametersOverriddenByGlobalProperty() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", "randomPropertyToken");

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();
		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.build();
		targetRequest.setTargetParameters(targetParameters);
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 "GlobalPropertyToken");
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
		assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
		assertEquals("GlobalPropertyToken", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
	}

	@Test
	public void
	getRequestPayload_MboxPropertyParametersInJson_When_ATPropertyInMboxParametersOverriddenByNullGlobalProperty() {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", "randomPropertyToken");

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();
		final String mboxName = "mbox";
		TargetRequest targetRequest = new TargetRequest.Builder(mboxName, "default")
		.build();
		targetRequest.setTargetParameters(targetParameters);
		targetRequestList.add(targetRequest);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, targetRequestList, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(json.optJSONObject(EXECUTE_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
		assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
		assertEquals("randomPropertyToken", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
	}

	@Test
	public void getRequestPayloadWithPrefetchRequests_PropertyParametersInJson_When_ATPropertyInMboxParameters() {
		List<TargetPrefetch> targetPrefetchList = new ArrayList<TargetPrefetch>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", "randomPropertyToken");

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();
		final String mboxName = "mbox";
		TargetPrefetch targetPrefetch = new TargetPrefetch.Builder(mboxName).build();
		targetPrefetch.setTargetParameters(targetParameters);
		targetPrefetchList.add(targetPrefetch);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(targetPrefetchList, null, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
		assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
		assertEquals("randomPropertyToken", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
	}

	@Test
	public void
	getRequestPayloadWithPrefetchRequests_PropertyParametersInJson_When_ATPropertyInMboxParametersOverriddenByGlobalProperty() {
		List<TargetPrefetch> targetPrefetchList = new ArrayList<TargetPrefetch>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", "randomPropertyToken");

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();
		final String mboxName = "mbox";
		TargetPrefetch targetPrefetch = new TargetPrefetch.Builder(mboxName).build();
		targetPrefetch.setTargetParameters(targetParameters);
		targetPrefetchList.add(targetPrefetch);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(targetPrefetchList, null, false, null, null,
											 "GlobalPropertyToken");
		assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
		assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
		assertEquals("GlobalPropertyToken", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
	}

	@Test
	public void
	getRequestPayloadWithPrefetchRequests_MboxPropertyParametersInJson_When_ATPropertyInMboxParametersOverriddenByNullGlobalProperty() {
		List<TargetPrefetch> targetPrefetchList = new ArrayList<TargetPrefetch>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", "randomPropertyToken");

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();
		final String mboxName = "mbox";
		TargetPrefetch targetPrefetch = new TargetPrefetch.Builder(mboxName).build();
		targetPrefetch.setTargetParameters(targetParameters);
		targetPrefetchList.add(targetPrefetch);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(targetPrefetchList, null, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
		assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
		assertEquals("randomPropertyToken", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
	}

	@Test
	public void getRequestPayloadWithPrefetchRequests_PropertyParametersNotInJson_When_NullATPropertyInMboxParameters() {
		List<TargetPrefetch> targetPrefetchList = new ArrayList<TargetPrefetch>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", null);

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();
		final String mboxName = "mbox";
		TargetPrefetch targetPrefetch = new TargetPrefetch.Builder(mboxName).build();
		targetPrefetch.setTargetParameters(targetParameters);
		targetPrefetchList.add(targetPrefetch);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(targetPrefetchList, null, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
		assertNull(json.optJSONObject(TargetJson.PROPERTY));
	}

	@Test
	public void
	getRequestPayloadWithPrefetchRequests_PrefetchRequestBuiltUsingConstructor_PropertyParametersNotInJson_When_NullATPropertyInMboxParameters() {
		List<TargetPrefetch> targetPrefetchList = new ArrayList<TargetPrefetch>();

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("at_property", null);

		Map<String, String> profileParameters = new HashMap<String, String>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		TargetParameters targetParameters = new TargetParameters.Builder().profileParameters(
			profileParameters).parameters(mboxParameters).build();
		final String mboxName = "mbox";
		TargetPrefetch targetPrefetch = new TargetPrefetch(mboxName, targetParameters);
		targetPrefetchList.add(targetPrefetch);


		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(targetPrefetchList, null, false, null, null,
											 null);
		assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).length());
		assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertNull(json.optJSONObject(PREFETCH_MBOXES).optJSONArray(MBOXES).optJSONObject(0).optJSONObject(PARAMETERS));
		assertNull(json.optJSONObject(TargetJson.PROPERTY));
	}
	// ======================================================================
	// Test getRequestPayloadFor_PrefetchViews_With_TargetParameters
	// ======================================================================
	@Test
	public void getRequestPayloadForPrefetchViewsInJson_WithTargetParameters() {

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("mBox-parameter-Key1", "mBox-Value1");
		mboxParameters.put("mBox-parameter-Key2", "mBox-Value2");
		Map<String, String> profileParameters = new HashMap<>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		profileParameters.put("profile-parameter-Key2", "profile-Value2");
		Map<String, Object> orderParameters = new HashMap<>();
		orderParameters.put(ID, "orderId");
		orderParameters.put(ORDER_TOTAL, 0.1);
		orderParameters.put(PURCHASED_PRODUCT_IDS, new ArrayList<Object>() {
			{
				add("id1");
				add("id2");
			}
		});
		Map<String, String> productParameters = new HashMap<>();
		productParameters.put(ID, "productId");
		productParameters.put(CATEGORY_ID, "categoryId");
		TargetParameters targetParameters = new TargetParameters.Builder().parameters(
			mboxParameters).profileParameters(profileParameters).
		order(TargetOrder.fromMap(orderParameters)).product(TargetProduct.fromMap(productParameters)).build();

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, true,
											 targetParameters, null, null);
		assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).length());
		assertEquals("mBox-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PARAMETERS).opt("mBox-parameter-Key1"));
		assertEquals("mBox-Value2", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PARAMETERS).opt("mBox-parameter-Key2"));
		assertEquals("productId", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(ID));
		assertEquals("categoryId", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(CATEGORY_ID));
		assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertEquals("profile-Value2", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key2"));
		assertEquals(0.1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(ORDER).opt(
						 ORDER_TOTAL));
		assertEquals("orderId", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).opt(ID));
		assertEquals(2, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).optJSONArray(
						 PURCHASED_PRODUCT_IDS).length());
		assertEquals("id1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).optJSONArray(
						 PURCHASED_PRODUCT_IDS).opt(0));
		assertEquals("id2", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).optJSONArray(
						 PURCHASED_PRODUCT_IDS).opt(1));
	}

	@Test
	public void getRequestPayload_ForPrefetchViews_PropertyParametersInJson_When_ATPropertyInMboxParameters() {

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("mBox-parameter-Key1", "mBox-Value1");
		mboxParameters.put("at_property", "randomPropertyToken");
		Map<String, String> profileParameters = new HashMap<>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		Map<String, Object> orderParameters = new HashMap<>();
		orderParameters.put(ID, "orderId");
		orderParameters.put(ORDER_TOTAL, 0.1);
		orderParameters.put(PURCHASED_PRODUCT_IDS, new ArrayList<Object>() {
			{
				add("id1");
				add("id2");
			}
		});
		Map<String, String> productParameters = new HashMap<>();
		productParameters.put(ID, "productId");
		productParameters.put(CATEGORY_ID, "categoryId");
		TargetParameters targetParameters = new TargetParameters.Builder().parameters(
			mboxParameters).profileParameters(profileParameters).
		order(TargetOrder.fromMap(orderParameters)).product(TargetProduct.fromMap(productParameters)).build();

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, true,
											 targetParameters, null, null);
		assertEquals(1, json.optJSONObject(TargetJson.PROPERTY).length());
		assertEquals("randomPropertyToken", json.optJSONObject(TargetJson.PROPERTY).optString(TargetJson.TOKEN, ""));
		assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).length());
		assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PARAMETERS).length());
		assertEquals("mBox-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PARAMETERS).opt("mBox-parameter-Key1"));
		assertEquals("productId", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(ID));
		assertEquals("categoryId", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(CATEGORY_ID));
		assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertEquals(0.1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(ORDER).opt(
						 ORDER_TOTAL));
		assertEquals("orderId", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).opt(ID));
		assertEquals(2, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).optJSONArray(
						 PURCHASED_PRODUCT_IDS).length());
		assertEquals("id1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).optJSONArray(
						 PURCHASED_PRODUCT_IDS).opt(0));
		assertEquals("id2", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).optJSONArray(
						 PURCHASED_PRODUCT_IDS).opt(1));
	}

	@Test
	public void getRequestPayload_ForPrefetchViews_PropertyParametersNotInJson_When_NullATPropertyInMboxParameters() {

		Map<String, String> mboxParameters = new HashMap<>();
		mboxParameters.put("mBox-parameter-Key1", "mBox-Value1");
		mboxParameters.put("at_property", null);
		Map<String, String> profileParameters = new HashMap<>();
		profileParameters.put("profile-parameter-Key1", "profile-Value1");
		Map<String, Object> orderParameters = new HashMap<>();
		orderParameters.put(ID, "orderId");
		orderParameters.put(ORDER_TOTAL, 0.1);
		orderParameters.put(PURCHASED_PRODUCT_IDS, new ArrayList<Object>() {
			{
				add("id1");
				add("id2");
			}
		});
		Map<String, String> productParameters = new HashMap<>();
		productParameters.put(ID, "productId");
		productParameters.put(CATEGORY_ID, "categoryId");
		TargetParameters targetParameters = new TargetParameters.Builder().parameters(
			mboxParameters).profileParameters(profileParameters).
		order(TargetOrder.fromMap(orderParameters)).product(TargetProduct.fromMap(productParameters)).build();

		JsonUtilityService.JSONObject json = targetRequestBuilder.getRequestPayload(null, null, true,
											 targetParameters, null, null);
		assertNull(json.optJSONObject(TargetJson.PROPERTY));
		assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).length());
		assertEquals(1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PARAMETERS).length());
		assertEquals("mBox-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PARAMETERS).opt("mBox-parameter-Key1"));
		assertEquals("productId", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(ID));
		assertEquals("categoryId", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PRODUCT).opt(CATEGORY_ID));
		assertEquals("profile-Value1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 PROFILE_PARAMETERS).opt("profile-parameter-Key1"));
		assertEquals(0.1, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(ORDER).opt(
						 ORDER_TOTAL));
		assertEquals("orderId", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).opt(ID));
		assertEquals(2, json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).optJSONArray(
						 PURCHASED_PRODUCT_IDS).length());
		assertEquals("id1", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).optJSONArray(
						 PURCHASED_PRODUCT_IDS).opt(0));
		assertEquals("id2", json.optJSONObject(PREFETCH_MBOXES).optJSONArray(VIEWS).optJSONObject(0).optJSONObject(
						 ORDER).optJSONArray(
						 PURCHASED_PRODUCT_IDS).opt(1));
	}

	// ===================================
	// Helpers
	// ===================================
	List<TargetRequest> getTargetRequestList(int count) {
		List<TargetRequest> targetRequestList = new ArrayList<TargetRequest>();

		for (int i = 0; i < count; i++) {
			final String mboxName = "mbox" + i;
			TargetRequest targetRequest = new TargetRequest(mboxName, null, "default", new AdobeCallback<String>() {
				@Override
				public void call(String s) {

				}
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
}
