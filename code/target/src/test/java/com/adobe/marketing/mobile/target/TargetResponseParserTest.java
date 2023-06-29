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

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.util.StreamUtils;

public class TargetResponseParserTest {
	private static final String FAKE_SESSION_ID = "session_id";

	private TargetResponseParser responseParser;

	@Before()
	public void beforeEach() {
		responseParser = new TargetResponseParser();
	}

	// ===================================
	// Test parseResponseToJson
	// ===================================
	@Test
	public void testParseResponseToJson_Happy() throws Exception {
		// setup
        try (MockedStatic<StreamUtils> streamUtilsMock = Mockito.mockStatic(StreamUtils.class)) {
            streamUtilsMock.when(() -> StreamUtils.readAsString(Mockito.any()))
                    .thenReturn("{\n" +
                            "  \"prefetch\" : {\n" +
                            "    \"mboxes\" : [{\n" +
                            "      \"name\" : \"mboxName\" ,  \n" +
                            "       \"options\" : [{\n" +
                            "        \"content\" : \"myContent\"" +
                            "    }]\n" +
                            "    }]\n" + "  }\n" +
                            "}"
                    );
            HttpConnecting mockedHttpConnecting = Mockito.mock(HttpConnecting.class);

            // test
            JSONObject jsonObject = responseParser.parseResponseToJson(mockedHttpConnecting);

            // verify
            assertNotNull(jsonObject);
            assertNotNull(jsonObject.get("prefetch"));
        }
	}

	@Test
	public void testParseResponseToJson_WhenEmptyResponse() {
        // setup
        try (MockedStatic<StreamUtils> streamUtilsMock = Mockito.mockStatic(StreamUtils.class)) {
            streamUtilsMock.when(() -> StreamUtils.readAsString(Mockito.any()))
                    .thenReturn("");
            HttpConnecting mockedHttpConnecting = Mockito.mock(HttpConnecting.class);

            // test
            assertNull(responseParser.parseResponseToJson(mockedHttpConnecting));
        }
    }

    @Test
    public void testParseResponseToJson_When_ResponseInvalidJSON() {
        // setup
        try (MockedStatic<StreamUtils> streamUtilsMock = Mockito.mockStatic(StreamUtils.class)) {
            streamUtilsMock.when(() -> StreamUtils.readAsString(Mockito.any()))
                    .thenReturn("InvalidJSON");
            HttpConnecting mockedHttpConnecting = Mockito.mock(HttpConnecting.class);

            // test
            assertNull(responseParser.parseResponseToJson(mockedHttpConnecting));
        }
    }

	@Test
	public void testParseResponseToJson_When_ConnectionNull() {
		// test
		assertNull(responseParser.parseResponseToJson(null));
	}

	// ===================================
	// Test ExtractPrefetchedMboxes
	// ===================================
	@Test
	public void testExtractPrefetchedMboxes_Happy() throws Exception {
		// setup
		String serverResponse = "{\n" +
								"  \"prefetch\" : {\n" +
								"  \"mboxes\" : [{\n" +
								"    \"name\" : \"mboxName\" ,  \n" +
								"     \"options\" : [{\n" +
								"      \"content\" : \"myContent\"" +
								"  }]\n" +
								"  }]\n" + "  }\n" +
								"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, JSONObject> prefetchedMboxes = responseParser.extractPrefetchedMboxes(jsonObject);

		// verify
		assertEquals(1, prefetchedMboxes.size());
		assertEquals("mboxName", prefetchedMboxes.get("mboxName").getString("name"));
		assertEquals("myContent", prefetchedMboxes.get("mboxName").getJSONArray("options").getJSONObject(
						 0).getString("content"));
	}

	@Test
	public void testExtractPrefetchedMboxes_When_NoPrefetchMboxKeys() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"noprefetch\" : {\n" +
				"  \"mboxes\" : [{\n" +
				"    \"name\" : \"mboxName\" ,  \n" +
				"    \"options\" : [{\n" +
				"    \"content\" : \"myContent\"" +
				"  }]\n" +
				"  }]\n" + "  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, JSONObject> prefetchedMboxes = responseParser.extractPrefetchedMboxes(jsonObject);

		// verify
		assertNull(prefetchedMboxes);
	}

	@Test
	public void testExtractPrefetchedMboxes_Will_ReturnNull_When_PrefetchMboxInvalidType() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"prefetch\": \"ItsAString\"\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, JSONObject> prefetchedMboxes = responseParser.extractPrefetchedMboxes(jsonObject);

		// verify
		assertNull(prefetchedMboxes);
	}

	@Test
	public void testExtractPrefetchedMboxes_Will_ReturnEmptyMap_When_PrefetchResponseContainsOneItemWithEmptyMbox() throws
			Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"prefetch\" : {\n" +
				"  \"mboxes\" : [{\n" +
				"    \"name\" : \"\" ,  \n" +
				"   \"options\" : [{\n" +
				"    \"content\" : \"myContent\"" +
				"  }]\n" +
				"  }]\n" + "  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, JSONObject> prefetchedMboxes = responseParser.extractPrefetchedMboxes(jsonObject);

		// verify
		assertEquals(0, prefetchedMboxes.size());
	}

	@Test
	public void testExtractPrefetchedMboxes_Will_IgnoreUnwantedKeys() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"prefetch\" : {\n" +
				"  \"mboxes\" : [{\n" +
				"    \"name\" : \"mboxName\" ,  \n" +
				"    \"unwantedKey\" : \"someValue\" ,  \n" +
				"     \"options\" : [{\n" +
				"    \"content\" : \"myContent\"" +
				"  }]\n" +
				"  }]\n" + "  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, JSONObject> prefetchedMboxes = responseParser.extractPrefetchedMboxes(jsonObject);
		// verify
		assertEquals(1, prefetchedMboxes.size());
		assertEquals(2, prefetchedMboxes.get("mboxName").length());
		assertEquals("mboxName", prefetchedMboxes.get("mboxName").getString("name"));
		assertEquals("myContent", prefetchedMboxes.get("mboxName").getJSONArray("options").getJSONObject(
				0).getString("content"));
	}

	// ===================================
	// Test ExtractBatchedMBoxes
	// ===================================

	@Test
	public void testExtractBatchedMBoxes_Happy() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"execute\": {\n" +
				"  \"mboxes\": [\n" +
				"  {\n" +
				"  \"name\": \"mboxName\",\n" +
				"   \"index\": 0,\n" +
				"  }]\n" + "  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, JSONObject> batchedMBoxes = responseParser.extractBatchedMBoxes(jsonObject);

		// verify
		assertEquals(1, batchedMBoxes.size());
		assertEquals("mboxName", batchedMBoxes.get("mboxName").getString("name"));
		assertEquals(0, batchedMBoxes.get("mboxName").getInt("index"));
	}

	@Test
	public void testExtractBatchedMBoxes_When_NoMboxKey() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"execute\": {\n" +
				"  \"nomboxesResponse\": [\n" +
				"  {\n" +
				"  \"name\": \"mboxName\",\n" +
				"   \"index\": 0,\n" +
				"  }]\n" + "  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, JSONObject> batchedMBoxes = responseParser.extractBatchedMBoxes(jsonObject);

		// verify
		assertNull(batchedMBoxes);
	}

	@Test
	public void testExtractBatchedMBoxes_Will_ReturnNull_When_MboxResponseInvalidType() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"execute\": {\n" +
				"  \"mboxes\": \"ItsAString\"" +
				"  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, JSONObject> batchedMBoxes = responseParser.extractBatchedMBoxes(jsonObject);

		// verify
		assertNull(batchedMBoxes);
	}

	@Test
	public void testExtractBatchedMBoxes_Will_ReturnEmptyMap_When_MboxResponseContainsOneItemWithEmptyMbox() throws
			Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"execute\": {\n" +
				"  \"mboxes\": [\n" +
				"  {\n" +
				"  \"name\": \"\",\n" +
				"   \"index\": 0,\n" +
				"  }]\n" + "  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, JSONObject> batchedMBoxes = responseParser.extractBatchedMBoxes(jsonObject);

		// verify
		assertEquals(0, batchedMBoxes.size());
	}

	@Test
	public void testExtractBatchedMBoxes_InvalidMboxResponse() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"execute\" : [2]\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, JSONObject> batchedMBoxes = responseParser.extractBatchedMBoxes(jsonObject);

		// verify
		assertNull(batchedMBoxes);
	}


	// ===================================
	// Test GetAnalyticsForTargetPayload
	// ===================================
	@Test
	public void testGetAnalyticsForTargetPayload_Happy() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"analytics\" : {\n" +
				"  \"payload\" : {\n" +
				"    \"key1\" : \"value1\" ,  \n" +
				"    \"key2\" : \"value2\"  \n" +
				"  }\n" + "  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, String> a4tPayloadMap = responseParser.getAnalyticsForTargetPayload(jsonObject, FAKE_SESSION_ID);

		// verify
		assertEquals(3, a4tPayloadMap.size());
		assertEquals("value1", a4tPayloadMap.get("&&key1"));
		assertEquals("value2", a4tPayloadMap.get("&&key2"));
		assertTrue(a4tPayloadMap.containsKey(TargetTestConstants.EventDataKeys.A4T_SESSION_ID));
		assertEquals(a4tPayloadMap.get(TargetTestConstants.EventDataKeys.A4T_SESSION_ID), FAKE_SESSION_ID);
	}

	@Test
	public void testGetAnalyticsForTargetPayload_Happy_With_Null_Session_Id() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"analytics\" : {\n" +
				"  \"payload\" : {\n" +
				"    \"key1\" : \"value1\" ,  \n" +
				"    \"key2\" : \"value2\"  \n" +
				"  }\n" + "  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, String> a4tPayloadMap = responseParser.getAnalyticsForTargetPayload(jsonObject, null);

		// verify
		assertEquals(2, a4tPayloadMap.size());
		assertEquals("value1", a4tPayloadMap.get("&&key1"));
		assertEquals("value2", a4tPayloadMap.get("&&key2"));
		assertFalse(a4tPayloadMap.containsKey(TargetTestConstants.EventDataKeys.SESSION_ID));
	}

	@Test
	public void testGetAnalyticsForTargetPayload_Happy_With_Empty_Session_Id() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"analytics\" : {\n" +
				"  \"payload\" : {\n" +
				"    \"key1\" : \"value1\" ,  \n" +
				"    \"key2\" : \"value2\"  \n" +
				"  }\n" + "  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, String> a4tPayloadMap = responseParser.getAnalyticsForTargetPayload(jsonObject, "");

		// verify
		assertEquals(2, a4tPayloadMap.size());
		assertEquals("value1", a4tPayloadMap.get("&&key1"));
		assertEquals("value2", a4tPayloadMap.get("&&key2"));
		assertFalse(a4tPayloadMap.containsKey(TargetTestConstants.EventDataKeys.SESSION_ID));
	}

	@Test
	public void testGetAnalyticsForTargetPayload_A4TPayloadEmpty() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"analytics\" : {} }";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, String> a4tPayloadMap = responseParser.getAnalyticsForTargetPayload(jsonObject, FAKE_SESSION_ID);

		// verify
		assertNull(a4tPayloadMap);
	}

	@Test
	public void testGetAnalyticsForTargetPayload_A4TPayloadIsAInvalidType() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"analytics\" : \"ShouldNotBeAString\" }";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, String> a4tPayloadMap = responseParser.getAnalyticsForTargetPayload(jsonObject, FAKE_SESSION_ID);

		// verify
		assertNull(a4tPayloadMap);
	}

	@Test
	public void testGetAnalyticsForTargetPayload_EmptyJSON() throws Exception {
		// setup
		String serverResponse = "{}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		Map<String, String> a4tPayloadMap = responseParser.getAnalyticsForTargetPayload(jsonObject, FAKE_SESSION_ID);

		// verify
		assertNull(a4tPayloadMap);
	}


	// ===================================
	// Test getTntId
	// ===================================
	@Test
	public void testGetTntId_Happy() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"id\": {\n" +
				"    \"tntId\": \"tntIDValue\"\n" +
				"  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		String actualTntID = responseParser.getTntId(jsonObject);

		// verify
		assertEquals("tntIDValue", actualTntID);
	}

	@Test
	public void testGetTntId_When_TntIDUnavailable() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"id\": {\n" +
				"    \"nottntID\": \"tntIDValue\"\n" +
				"  }\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		String actualTntID = responseParser.getTntId(jsonObject);

		// verify
		assertEquals("",actualTntID);
	}

	@Test
	public void testGetTntId_When_InvalidServerJSONResponse() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"id\": \"nonJSON\"\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		String actualTntID = responseParser.getTntId(jsonObject);

		// verify
		assertNull(actualTntID);
	}


	// ===================================
	// Test getEdgeHost
	// ===================================
	@Test
	public void testGetEdgeHost_Happy() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"edgeHost\": \"superHost\"\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		String edgeHost = responseParser.getEdgeHost(jsonObject);

		// verify
		assertEquals("superHost", edgeHost);
	}

	@Test
	public void testGetEdgeHost_When_EdgeHostUnavailable() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"  \"notedgeHost\": \"superHost\"\n" +
				"}";
		JSONObject jsonObject = new JSONObject(serverResponse);

		// test
		String edgeHost = responseParser.getEdgeHost(jsonObject);

		// verify
		assertEquals("", edgeHost);
	}


	// ===================================
	// Test GetErrorMessage
	// ===================================
	@Test
	public void testGetErrorMessage_Happy() throws Exception {
		// setup
		String errorString = "{\n" +
				"  \"message\" : \"Error message\",\n" +
				"}";
		JSONObject jsonObject = new JSONObject(errorString);
		// test
		String errorMessage = responseParser.getErrorMessage(jsonObject);

		// verify
		assertEquals(1, jsonObject.length());
		assertEquals("Error message", errorMessage);
	}

	@Test
	public void testGetErrorMessage_EmptyJSON() throws Exception {
		// setup
		JSONObject jsonObject = new JSONObject();

		// test
		String errorMessage = responseParser.getErrorMessage(jsonObject);

		// verify
		assertNull(errorMessage);
	}

	@Test
	public void testGetErrorMessage_nullJson() throws Exception {
		// setup

		JSONObject jsonObject = null;

		// test
		String errorMessage = responseParser.getErrorMessage(jsonObject);

		// verify
		assertNull(errorMessage);
	}

	// ===================================
	// Test extractMboxContent
	// ===================================
	@Test
	public void testExtractMboxContent_Happy() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"     \"index\" : \"1\" ,  \n" +
				"     \"options\" : [{\n" +
				"      \"content\" : \"AAA\" , \n" +
				"      \"type\" : \"html\"" +
				"  }]\n" +
				"}";
		// test
		JSONObject jsonObject = new JSONObject(serverResponse);
		String extractMboxContent = responseParser.extractMboxContent(jsonObject);
		// verify
		assertNotNull(extractMboxContent);
		assertEquals("AAA", extractMboxContent);
	}

	@Test
	public void testExtractMboxContent_JsonArrayContent() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"     \"index\" : \"1\" ,  \n" +
				"     \"options\" : [{\n" +
				"      \"content\" : [\"one\", \"two\", \"three\"] , \n" +
				"      \"type\" : \"json\"" +
				"  }]\n" +
				"}";
		// test
		JSONObject jsonObject = new JSONObject(serverResponse);
		String extractMboxContent = responseParser.extractMboxContent(jsonObject);
		// verify
		assertNotNull(extractMboxContent);
		assertEquals("[\"one\",\"two\",\"three\"]", extractMboxContent);
	}

	@Test
	public void testExtractMboxContent_MixedJsonArrayContent() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"     \"index\" : \"1\" ,  \n" +
				"     \"options\" : [{\n" +
				"      \"content\" : [[\"one\", 1], true, 23] , \n" +
				"      \"type\" : \"json\"" +
				"  }]\n" +
				"}";
		// test
		JSONObject jsonObject = new JSONObject(serverResponse);
		String extractMboxContent = responseParser.extractMboxContent(jsonObject);
		// verify
		assertNotNull(extractMboxContent);
		assertEquals("[[\"one\",1],true,23]", extractMboxContent);
	}

	@Test
	public void testExtractMboxContentHappy_For_StringContent() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"     \"index\" : \"1\" ,  \n" +
				"     \"options\" : [{\n" +
				"      \"content\" : \"AAA\" , \n" +
				"      \"type\" : \"html\"" +
				"  }]\n" +
				"}";
		// test
		JSONObject jsonObject = new JSONObject(serverResponse);
		String extractMboxContent = responseParser.extractMboxContent(jsonObject);
		assertEquals("AAA", extractMboxContent);
	}

	@Test
	public void testExtractMboxContentHappy_For_JsonContent() throws Exception { // setup
		String serverResponse = "{\n" +
				"     \"index\" : \"1\" ,  \n" +
				"     \"options\" : [{\n" +
				"      \"content\" : {\"key\": \"value\"} , \n" +
				"      \"type\" : \"json\"" +
				"  }]\n" +
				"}";
		// test
		JSONObject jsonObject = new JSONObject(serverResponse);
		String extractMboxContent = responseParser.extractMboxContent(jsonObject);
		assertEquals("{\"key\":\"value\"}", extractMboxContent);
	}

	@Test
	public void testExtractMboxContent_When_NoMboxContentKeys() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"     \"index\" : \"1\" ,  \n" +
				"     \"options\" : [{\n" +
				"  }]\n" +
				"}";
		// test
		JSONObject jsonObject = new JSONObject(serverResponse);
		String extractMboxContent = responseParser.extractMboxContent(jsonObject);
		assertEquals("", extractMboxContent);
	}

	@Test
	public void testExtractMboxContent_Will_ReturnEmpty_When_ExtractMboxContentInvalidType() throws Exception {
		// setup
		String serverResponse = "{\n" +
				"     \"index\" : \"1\" ,  \n" +
				"     \"options\" : [{\n" +
				"      \"content\" : \"\"" +
				"  }]\n" +
				"}";
		// test
		JSONObject jsonObject = new JSONObject(serverResponse);
		String extractMboxContent = responseParser.extractMboxContent(jsonObject);
		assertEquals("", extractMboxContent);
	}

	// ===================================
	// Test extract A4T Params
	// ===================================

	@Test
	public void testExtractA4TParams_Will_Return_Valid_Map_When_Analytics_Payload_Is_Present() throws JSONException {
		//setup
		final String mboxPayload = "{\n" +
				"   \"index\":0,\n" +
				"   \"name\":\"ryan_a4t2\",\n" +
				"   \"options\":[\n" +
				"      {\n" +
				"         \"content\":{\n" +
				"            \"key2\":\"value2\"\n" +
				"         },\n" +
				"         \"type\":\"json\",\n" +
				"         \"responseTokens\":{\n" +
				"            \"geo.connectionSpeed\":\"broadband\",\n" +
				"            \"geo.state\":\"california\",\n" +
				"         },\n" +
				"         \"sourceType\":\"target\"\n" +
				"      }\n" +
				"   ],\n" +
				"   \"analytics\":{\n" +
				"      \"payload\":{\n" +
				"         \"pe\":\"tnt\",\n" +
				"         \"tnta\":\"333911:0:0:0|2|4445.12,333911:0:0:0|1|4445.12\"\n" +
				"      }\n" +
				"   }\n" +
				"}";

		final JSONObject mBoxPayloadObject = new JSONObject(mboxPayload);

		//Assertions
		Map<String, String> a4tParams = responseParser.getAnalyticsForTargetPayload(mBoxPayloadObject);

		assertNotNull(a4tParams);
		assertEquals(2, a4tParams.size());

		assertEquals("tnt", a4tParams.get("pe"));
		assertEquals("333911:0:0:0|2|4445.12,333911:0:0:0|1|4445.12", a4tParams.get("tnta"));
	}

	@Test
	public void testExtractA4TParams_Will_Return_Null_When_Analytics_Payload_Is_Missing() throws JSONException {
		//setup
		final String mboxPayload = "{\n" +
				"   \"index\":0,\n" +
				"   \"name\":\"ryan_a4t2\",\n" +
				"   \"options\":[\n" +
				"      {\n" +
				"         \"content\":{\n" +
				"            \"key2\":\"value2\"\n" +
				"         },\n" +
				"         \"type\":\"json\",\n" +
				"         \"responseTokens\":{\n" +
				"            \"geo.connectionSpeed\":\"broadband\",\n" +
				"            \"geo.state\":\"california\",\n" +
				"         },\n" +
				"         \"sourceType\":\"target\"\n" +
				"      }\n" +
				"   ]\n" +
				"}";

		final JSONObject mBoxPayloadObject = new JSONObject(mboxPayload);

		//Assertions
		Map<String, String> a4tParams = responseParser.getAnalyticsForTargetPayload(mBoxPayloadObject);
		assertNull(a4tParams);
	}

	// ===================================
	// Test extract Response Tokens
	// ===================================

	@Test
	public void testExtractResponseTokens_Will_Return_Valid_Map_When_Response_Tokens_Payload_Is_Present() throws JSONException {
		//setup
		final String mboxPayload = "{\n" +
				"   \"index\":0,\n" +
				"   \"name\":\"ryan_a4t2\",\n" +
				"   \"options\":[\n" +
				"      {\n" +
				"         \"content\":{\n" +
				"            \"key2\":\"value2\"\n" +
				"         },\n" +
				"         \"type\":\"json\",\n" +
				"         \"responseTokens\":{\n" +
				"            \"geo.connectionSpeed\":\"broadband\",\n" +
				"            \"geo.state\":\"california\",\n" +
				"         },\n" +
				"         \"sourceType\":\"target\"\n" +
				"      }\n" +
				"   ],\n" +
				"   \"analytics\":{\n" +
				"      \"payload\":{\n" +
				"         \"pe\":\"tnt\",\n" +
				"         \"tnta\":\"333911:0:0:0|2|4445.12,333911:0:0:0|1|4445.12\"\n" +
				"      }\n" +
				"   }\n" +
				"}";

		final JSONObject mBoxPayloadObject = new JSONObject(mboxPayload);

		//Assertions
		Map<String, String> responseTokens = responseParser.getResponseTokens(mBoxPayloadObject);

		assertNotNull(responseTokens);
		assertEquals(2, responseTokens.size());
		assertEquals("broadband", responseTokens.get("geo.connectionSpeed"));
		assertEquals("california", responseTokens.get("geo.state"));
	}

	@Test
	public void testExtractResponseTokens_Will_Return_Null_When_response_Tokens_Payload_Is_Missing() throws JSONException {
		//setup
		final String mboxPayload = "{\n" +
				"   \"index\":0,\n" +
				"   \"name\":\"ryan_a4t2\",\n" +
				"   \"options\":[\n" +
				"      {\n" +
				"         \"content\":{\n" +
				"            \"key2\":\"value2\"\n" +
				"         },\n" +
				"         \"type\":\"json\",\n" +
				"         \"sourceType\":\"target\"\n" +
				"      }\n" +
				"   ],\n" +
				"   \"analytics\":{\n" +
				"      \"payload\":{\n" +
				"         \"pe\":\"tnt\",\n" +
				"         \"tnta\":\"333911:0:0:0|2|4445.12,333911:0:0:0|1|4445.12\"\n" +
				"      }\n" +
				"   }\n" +
				"}";

		final JSONObject mBoxPayloadObject = new JSONObject(mboxPayload);

		//Assertions
		Map<String, String> responseTokens = responseParser.getResponseTokens(mBoxPayloadObject);
		assertNull(responseTokens);
	}

	// =====================================
	// Test extract Click Metric A4T Params
	// =====================================

	@Test
	public void testExtractClickMetricAnalyticsPayload_Will_Return_Valid_Map_When_Analytics_Payload_Is_Present() throws JSONException {
		//setup
		final String mboxPayload = "{\n" +
				"   \"index\":0,\n" +
				"   \"name\":\"ryan_a4t2\",\n" +
				"   \"options\":[\n" +
				"      {\n" +
				"         \"content\":{\n" +
				"            \"key2\":\"value2\"\n" +
				"         },\n" +
				"         \"type\":\"json\",\n" +
				"         \"sourceType\":\"target\"\n" +
				"      }\n" +
				"   ],\n" +
				"\"metrics\":[\n" +
				"               {\n" +
				"                  \"type\":\"click\",\n" +
				"                  \"eventToken\":\"ABPi/uih7s0vo6/8kqyxjA==\",\n" +
				"                  \"analytics\":{\n" +
				"                     \"payload\":{\n" +
				"                        \"pe\":\"tnt\",\n" +
				"                        \"tnta\":\"409277:0:0|32767\"\n" +
				"                     }\n" +
				"                  }\n" +
				"               }\n" +
				"            ]," +
				"   \"analytics\":{\n" +
				"      \"payload\":{\n" +
				"         \"pe\":\"tnt\",\n" +
				"         \"tnta\":\"333911:0:0:0|2|4445.12,333911:0:0:0|1|4445.12\"\n" +
				"      }\n" +
				"   }\n" +
				"}";

		final JSONObject mBoxPayloadObject = new JSONObject(mboxPayload);

		//Assertions
		Map<String, String> clickMetricsA4TParams = responseParser.extractClickMetricAnalyticsPayload(mBoxPayloadObject);

		assertNotNull(clickMetricsA4TParams);
		assertEquals(2, clickMetricsA4TParams.size());

		assertEquals("tnt", clickMetricsA4TParams.get("pe"));
		assertEquals("409277:0:0|32767", clickMetricsA4TParams.get("tnta"));
	}

	@Test
	public void tesExtractClickMetricAnalyticsPayload_Will_Return_Null_When_Analytics_Payload_Is_Missing() throws JSONException {
		//setup
		final String mboxPayload = "{\n" +
				"   \"index\":0,\n" +
				"   \"name\":\"ryan_a4t2\",\n" +
				"   \"options\":[\n" +
				"      {\n" +
				"         \"content\":{\n" +
				"            \"key2\":\"value2\"\n" +
				"         },\n" +
				"         \"type\":\"json\",\n" +
				"         \"responseTokens\":{\n" +
				"            \"geo.connectionSpeed\":\"broadband\",\n" +
				"            \"geo.state\":\"california\",\n" +
				"         },\n" +
				"         \"sourceType\":\"target\"\n" +
				"      }\n" +
				"   ]\n" +
				"}";

		final JSONObject mBoxPayloadObject = new JSONObject(mboxPayload);

		//Assertions
		Map<String, String> clickMetricsA4TParams = responseParser.extractClickMetricAnalyticsPayload(mBoxPayloadObject);
		assertNull(clickMetricsA4TParams);
	}

	@Test
	public void testExtractClickMetricAnalyticsPayload_Will_Return_Null_When_EventTokenIsMissing() throws JSONException {
		//setup
		final String mboxPayload = "{\n" +
				"   \"index\":0,\n" +
				"   \"name\":\"ryan_a4t2\",\n" +
				"   \"options\":[\n" +
				"      {\n" +
				"         \"content\":{\n" +
				"            \"key2\":\"value2\"\n" +
				"         },\n" +
				"         \"type\":\"json\",\n" +
				"         \"sourceType\":\"target\"\n" +
				"      }\n" +
				"   ],\n" +
				"\"metrics\":[\n" +
				"               {\n" +
				"                  \"type\":\"click\",\n" +
				"                  \"analytics\":{\n" +
				"                     \"payload\":{\n" +
				"                        \"pe\":\"tnt\",\n" +
				"                        \"tnta\":\"409277:0:0|32767\"\n" +
				"                     }\n" +
				"                  }\n" +
				"               }\n" +
				"            ]," +
				"   \"analytics\":{\n" +
				"      \"payload\":{\n" +
				"         \"pe\":\"tnt\",\n" +
				"         \"tnta\":\"333911:0:0:0|2|4445.12,333911:0:0:0|1|4445.12\"\n" +
				"      }\n" +
				"   }\n" +
				"}";

		final JSONObject mBoxPayloadObject = new JSONObject(mboxPayload);

		//Assertions
		Map<String, String> clickMetricsA4TParams = responseParser.extractClickMetricAnalyticsPayload(mBoxPayloadObject);
		assertNull(clickMetricsA4TParams);
	}

	@Test
	public void test_preprocessAnalyticsForTargetPayload_Should_Return_Correctly_Formatted_Map() {
		//Setup
		final String pe = "tnt";
		final String tnta = "333911:0:0:0|32767";
		final String sessionId = "fake_sessionId";
		Map<String, String> a4tParams = new HashMap<String, String>() {
			{
				put("pe", pe);
				put("tnta", tnta);
			}
		};

		//Action
		Map<String, String> processedMap = responseParser.preprocessAnalyticsForTargetPayload(a4tParams, sessionId);

		//Assert
		assertEquals(3, processedMap.size());
		assertEquals("tnt", processedMap.get("&&pe"));
		assertEquals("333911:0:0:0|32767", processedMap.get("&&tnta"));
		assertEquals(sessionId, processedMap.get(TargetTestConstants.EventDataKeys.A4T_SESSION_ID));
	}

	@Test
	public void test_preprocessAnalyticsForTargetPayload_Should_Return_Null() {
		//Setup
		final String sessionId = "fake_sessionId";

		//Action
		Map<String, String> processedMap = responseParser.preprocessAnalyticsForTargetPayload(null, sessionId);

		//Assert
		assertNull(processedMap);
	}

	@Test
	public void test_getClickMetric_ShouldReturn_ClickMetricObject() throws JSONException {
		final String mboxJsonString = "{\n" +
				"            \"index\":0,\n" +
				"            \"name\":\"ryan_a4t2\",\n" +
				"            \"options\":[\n" +
				"               {\n" +
				"                  \"content\":{\n" +
				"                     \"key2\":\"value2\"\n" +
				"                  },\n" +
				"                  \"type\":\"json\",\n" +
				"                  \"responseTokens\":{\n" +
				"                     \"activity.name\":\"ryan json offer a4t test 2\"\n" +
				"                  },\n" +
				"                  \"sourceType\":\"target\"\n" +
				"               }\n" +
				"            ],\n" +
				"            \"metrics\":[\n" +
				"               {\n" +
				"                  \"type\":\"click\",\n" +
				"                  \"eventToken\":\"1iqiEBgD3uMVv+V1rBrSug==\",\n" +
				"                  \"analytics\":{\n" +
				"                     \"payload\":{\n" +
				"                        \"pe\":\"tnt\",\n" +
				"                        \"tnta\":\"333911:0:0:0|32767\"\n" +
				"                     }\n" +
				"                  }\n" +
				"               }\n" +
				"            ],\n" +
				"            \"analytics\":{\n" +
				"               \"payload\":{\n" +
				"                  \"pe\":\"tnt\",\n" +
				"                  \"tnta\":\"333911:0:0:0|2\"\n" +
				"               }\n" +
				"            }\n" +
				"         }";

		JSONObject mboxJson = new JSONObject(mboxJsonString);
		JSONObject clickMetric = responseParser.getClickMetric(mboxJson);
		assertNotNull(clickMetric);
		assertEquals("click", clickMetric.getString("type"));
		assertEquals("1iqiEBgD3uMVv+V1rBrSug==", clickMetric.getString("eventToken"));
		assertEquals("tnt", clickMetric.getJSONObject("analytics").getJSONObject("payload").getString("pe"));
		assertEquals("333911:0:0:0|32767", clickMetric.getJSONObject("analytics").getJSONObject("payload").getString("tnta"));
	}

	@Test
	public void test_getClickMetric_ShouldReturnNull_WhenClickMetricIsMissing() throws JSONException {
		final String mboxJsonString = "{\n" +
				"            \"index\":0,\n" +
				"            \"name\":\"ryan_a4t2\",\n" +
				"            \"options\":[\n" +
				"               {\n" +
				"                  \"content\":{\n" +
				"                     \"key2\":\"value2\"\n" +
				"                  },\n" +
				"                  \"type\":\"json\",\n" +
				"                  \"responseTokens\":{\n" +
				"                     \"activity.name\":\"ryan json offer a4t test 2\"\n" +
				"                  },\n" +
				"                  \"sourceType\":\"target\"\n" +
				"               }\n" +
				"            ],\n" +
				"            \"metrics\":[\n" +
				"               {\n" +
				"                  \"type\":\"display\",\n" +
				"                  \"eventToken\":\"1iqiEBgD3uMVv+V1rBrSug==\"\n" +
				"               }\n" +
				"            ],\n" +
				"            \"analytics\":{\n" +
				"               \"payload\":{\n" +
				"                  \"pe\":\"tnt\",\n" +
				"                  \"tnta\":\"333911:0:0:0|2\"\n" +
				"               }\n" +
				"            }\n" +
				"         }";

		JSONObject mboxJson = new JSONObject(mboxJsonString);
		JSONObject clickMetric = responseParser.getClickMetric(mboxJson);
		assertNull(clickMetric);
	}

	@Test
	public void test_getClickMetric_ShouldReturnNull_WhenEventTokenIsMissing() throws JSONException {
		final String mboxJsonString = "{\n" +
				"            \"index\":0,\n" +
				"            \"name\":\"ryan_a4t2\",\n" +
				"            \"options\":[\n" +
				"               {\n" +
				"                  \"content\":{\n" +
				"                     \"key2\":\"value2\"\n" +
				"                  },\n" +
				"                  \"type\":\"json\",\n" +
				"                  \"responseTokens\":{\n" +
				"                     \"activity.name\":\"ryan json offer a4t test 2\"\n" +
				"                  },\n" +
				"                  \"sourceType\":\"target\"\n" +
				"               }\n" +
				"            ],\n" +
				"            \"metrics\":[\n" +
				"               {\n" +
				"                  \"type\":\"click\",\n" +
				"                  \"analytics\":{\n" +
				"                     \"payload\":{\n" +
				"                        \"pe\":\"tnt\",\n" +
				"                        \"tnta\":\"333911:0:0:0|32767\"\n" +
				"                     }\n" +
				"                  }\n" +
				"               }\n" +
				"            ],\n" +
				"            \"analytics\":{\n" +
				"               \"payload\":{\n" +
				"                  \"pe\":\"tnt\",\n" +
				"                  \"tnta\":\"333911:0:0:0|2\"\n" +
				"               }\n" +
				"            }\n" +
				"         }";

		JSONObject mboxJson = new JSONObject(mboxJsonString);
		JSONObject clickMetric = responseParser.getClickMetric(mboxJson);
		assertNull(clickMetric);
	}
}