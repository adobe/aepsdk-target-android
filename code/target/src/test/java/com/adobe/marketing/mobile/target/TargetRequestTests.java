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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeError;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TargetRequestTests {
    protected final String MBOX_NAME_HAPPY_PATH = "name";

    private final String RESPONSE_PAIR_ID_HAPPY_PATH = "responsePairId";

    private final String DEFAULT_CONTENT_HAPPY_PATH = "defaultContent";

    protected final Map<String, String> MBOX_PARAMETERS_HAPPY_PATH = new HashMap<String, String>() {
        {
            put("one", "uno");
            put("two", "dos");
            put("three", "tres");
        }
    };

    protected final Map<String, String> PROFILE_PARAMETERS_HAPPY_PATH = new HashMap<String, String>() {
        {
            put("profileKey1", "profileValue1");
            put("profileKey2", "profileValue2");
        }
    };

    protected final TargetProduct PRODUCT_PARAMETERS_HAPPY_PATH = new TargetProduct("pId", "cId");

    protected final Map<String, String> PRODUCT_PARAMETERS_HAPPY_PATH_MAP = new HashMap<String, String>() {
        {
            put("id", "pId");
            put("categoryId", "cId");
        }
    };

    protected final TargetOrder ORDER_PARAMETERS_HAPPY_PATH = new TargetOrder("orderId", 100.34, new ArrayList<String>() {
        {
            add("ppId");
        }
    });

    protected final Map<String, Object> ORDER_PARAMETERS_HAPPY_PATH_MAP = new HashMap<String, Object>() {
        {
            put("id", "orderId");
            put("purchasedProductIds", new ArrayList<Object>() {
                {
                    add("ppId");
                }
            });
            put("total", 100.34);
        }
    };

    protected final TargetParameters TARGET_PARAMETERS_HAPPY_PATH = new TargetParameters.Builder()
            .parameters(MBOX_PARAMETERS_HAPPY_PATH)
            .profileParameters(PROFILE_PARAMETERS_HAPPY_PATH)
            .order(ORDER_PARAMETERS_HAPPY_PATH)
            .product(PRODUCT_PARAMETERS_HAPPY_PATH)
            .build();

    protected final Map<String, Object> TARGET_PARAMETERS_HAPPY_PATH_MAP = new HashMap<String, Object>() {
        {
            put("parameters", MBOX_PARAMETERS_HAPPY_PATH);
            put("profileParameters", PROFILE_PARAMETERS_HAPPY_PATH);
            put("order", ORDER_PARAMETERS_HAPPY_PATH_MAP);
            put("product", PRODUCT_PARAMETERS_HAPPY_PATH_MAP);
        }
    };

    final AdobeCallback<String> CONTENT_CALLBACK_HAPPY_PATH = new AdobeCallback<String>() {
        @Override
        public void call(String value) {

        }
    };

    final AdobeTargetDetailedCallback CONTENT_WITH_DATA_CALLBACK_HAPPY_PATH = new AdobeTargetDetailedCallback() {
        @Override
        public void call(String content, Map<String, Object> data) { }

        @Override
        public void fail(AdobeError var1) { }
    };

    @Test
    public void testTargetRequest_happy() {
        final TargetRequest targetRequest = new TargetRequest(MBOX_NAME_HAPPY_PATH, TARGET_PARAMETERS_HAPPY_PATH, DEFAULT_CONTENT_HAPPY_PATH, CONTENT_CALLBACK_HAPPY_PATH);
        assertNotNull(targetRequest);
        assertEquals(MBOX_NAME_HAPPY_PATH, targetRequest.getMboxName());
        assertEquals(TARGET_PARAMETERS_HAPPY_PATH, targetRequest.getTargetParameters());
        assertEquals(DEFAULT_CONTENT_HAPPY_PATH, targetRequest.getDefaultContent());
        assertEquals(CONTENT_CALLBACK_HAPPY_PATH, targetRequest.getContentCallback());
    }

    @Test
    public void testTargetRequest_withAdobeTargetDetailedCallback() {
        final TargetRequest targetRequest = new TargetRequest(MBOX_NAME_HAPPY_PATH, TARGET_PARAMETERS_HAPPY_PATH, DEFAULT_CONTENT_HAPPY_PATH, CONTENT_WITH_DATA_CALLBACK_HAPPY_PATH);
        assertNotNull(targetRequest);
        assertEquals(MBOX_NAME_HAPPY_PATH, targetRequest.getMboxName());
        assertEquals(TARGET_PARAMETERS_HAPPY_PATH, targetRequest.getTargetParameters());
        assertEquals(DEFAULT_CONTENT_HAPPY_PATH, targetRequest.getDefaultContent());
        assertEquals(CONTENT_WITH_DATA_CALLBACK_HAPPY_PATH, targetRequest.getContentWithDataCallback());
    }

    @Test
    public void testToEventData_validTargetRequestMap() {
        final TargetRequest targetRequest = new TargetRequest(MBOX_NAME_HAPPY_PATH, TARGET_PARAMETERS_HAPPY_PATH, DEFAULT_CONTENT_HAPPY_PATH, CONTENT_CALLBACK_HAPPY_PATH);
        targetRequest.setResponsePairId(RESPONSE_PAIR_ID_HAPPY_PATH);

        final Map<String, Object> targetRequestMap = targetRequest.toEventData();
        assertNotNull(targetRequestMap);
        assertEquals(MBOX_NAME_HAPPY_PATH, targetRequestMap.get("name"));
        assertEquals(TARGET_PARAMETERS_HAPPY_PATH_MAP, targetRequestMap.get("targetparams"));
        assertEquals(DEFAULT_CONTENT_HAPPY_PATH, targetRequestMap.get("defaultContent"));
        assertEquals(RESPONSE_PAIR_ID_HAPPY_PATH, targetRequestMap.get("responsePairId"));
    }

    @Test
    public void testFromEventData_validTargetRequest() {
        final Map<String, Object> targetRequestMap = new HashMap<>();
        targetRequestMap.put("name", MBOX_NAME_HAPPY_PATH);
        targetRequestMap.put("targetparams", TARGET_PARAMETERS_HAPPY_PATH_MAP);
        targetRequestMap.put("defaultContent", DEFAULT_CONTENT_HAPPY_PATH);
        targetRequestMap.put("responsePairId", RESPONSE_PAIR_ID_HAPPY_PATH);

        final TargetRequest targetRequest = TargetRequest.fromEventData(targetRequestMap);
        assertNotNull(targetRequest);

        assertEquals(MBOX_NAME_HAPPY_PATH, targetRequest.getMboxName());
        assertEquals(TARGET_PARAMETERS_HAPPY_PATH, targetRequest.getTargetParameters());
        assertEquals(DEFAULT_CONTENT_HAPPY_PATH, targetRequest.getDefaultContent());
        assertEquals(RESPONSE_PAIR_ID_HAPPY_PATH, targetRequest.getResponsePairId());
    }

    @Test
    public void testFromEventData_InvalidTargetRequest() {
        final Map<String, Object> targetRequestMap = new HashMap<>();
        targetRequestMap.put("name", MBOX_NAME_HAPPY_PATH);
        targetRequestMap.put("targetparams", TARGET_PARAMETERS_HAPPY_PATH_MAP);
        targetRequestMap.put("defaultContent", DEFAULT_CONTENT_HAPPY_PATH);
        targetRequestMap.put("responsePairId", new Object());

        final TargetRequest targetRequest = TargetRequest.fromEventData(targetRequestMap);
        assertNull(targetRequest);
    }

    @Test
    public void testFromEventData_nullOrEmptyTargetRequest() {
        assertNull(TargetRequest.fromEventData(null));

        assertNull( TargetRequest.fromEventData(new HashMap<>()));
    }
}
