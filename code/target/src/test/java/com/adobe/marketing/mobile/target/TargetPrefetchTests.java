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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TargetPrefetchTests {
    protected final String MBOX_NAME_HAPPY_PATH = "name";

    protected final Map<String, String> MBOX_PARAMETERS_HAPPY_PATH =
            new HashMap<String, String>() {
                {
                    put("one", "uno");
                    put("two", "dos");
                    put("three", "tres");
                }
            };

    protected final Map<String, String> PROFILE_PARAMETERS_HAPPY_PATH =
            new HashMap<String, String>() {
                {
                    put("profileKey1", "profileValue1");
                    put("profileKey2", "profileValue2");
                }
            };

    protected final TargetProduct PRODUCT_PARAMETERS_HAPPY_PATH = new TargetProduct("pId", "cId");

    protected final Map<String, String> PRODUCT_PARAMETERS_HAPPY_PATH_MAP =
            new HashMap<String, String>() {
                {
                    put("id", "pId");
                    put("categoryId", "cId");
                }
            };

    protected final TargetOrder ORDER_PARAMETERS_HAPPY_PATH =
            new TargetOrder(
                    "orderId",
                    100.34,
                    new ArrayList<String>() {
                        {
                            add("ppId");
                        }
                    });

    protected final Map<String, Object> ORDER_PARAMETERS_HAPPY_PATH_MAP =
            new HashMap<String, Object>() {
                {
                    put("id", "orderId");
                    put(
                            "purchasedProductIds",
                            new ArrayList<Object>() {
                                {
                                    add("ppId");
                                }
                            });
                    put("total", 100.34);
                }
            };

    protected final TargetParameters TARGET_PARAMETERS_HAPPY_PATH =
            new TargetParameters.Builder()
                    .parameters(MBOX_PARAMETERS_HAPPY_PATH)
                    .profileParameters(PROFILE_PARAMETERS_HAPPY_PATH)
                    .order(ORDER_PARAMETERS_HAPPY_PATH)
                    .product(PRODUCT_PARAMETERS_HAPPY_PATH)
                    .build();

    protected final Map<String, Object> TARGET_PARAMETERS_HAPPY_PATH_MAP =
            new HashMap<String, Object>() {
                {
                    put("parameters", MBOX_PARAMETERS_HAPPY_PATH);
                    put("profileParameters", PROFILE_PARAMETERS_HAPPY_PATH);
                    put("order", ORDER_PARAMETERS_HAPPY_PATH_MAP);
                    put("product", PRODUCT_PARAMETERS_HAPPY_PATH_MAP);
                }
            };

    @Test
    public void testTargetPrefetch_happy() {
        final TargetPrefetch targetPrefetch =
                new TargetPrefetch(MBOX_NAME_HAPPY_PATH, TARGET_PARAMETERS_HAPPY_PATH);
        assertNotNull(targetPrefetch);
        assertEquals(MBOX_NAME_HAPPY_PATH, targetPrefetch.getMboxName());
        assertEquals(TARGET_PARAMETERS_HAPPY_PATH, targetPrefetch.getTargetParameters());
    }

    @Test
    public void testToEventData_validTargetPrefetchMap() {
        final TargetPrefetch targetPrefetch =
                new TargetPrefetch(MBOX_NAME_HAPPY_PATH, TARGET_PARAMETERS_HAPPY_PATH);

        final Map<String, Object> targetPrefetchMap = targetPrefetch.toEventData();
        assertNotNull(targetPrefetchMap);
        assertEquals(MBOX_NAME_HAPPY_PATH, targetPrefetchMap.get("name"));
        assertEquals(TARGET_PARAMETERS_HAPPY_PATH_MAP, targetPrefetchMap.get("targetparams"));
    }

    @Test
    public void testFromEventData_validTargetRequest() {
        final Map<String, Object> targetPrefetchMap = new HashMap<>();
        targetPrefetchMap.put("name", MBOX_NAME_HAPPY_PATH);
        targetPrefetchMap.put("targetparams", TARGET_PARAMETERS_HAPPY_PATH_MAP);

        final TargetPrefetch targetPrefetch = TargetPrefetch.fromEventData(targetPrefetchMap);
        assertNotNull(targetPrefetch);

        assertEquals(MBOX_NAME_HAPPY_PATH, targetPrefetch.getMboxName());
        assertEquals(TARGET_PARAMETERS_HAPPY_PATH, targetPrefetch.getTargetParameters());
    }

    @Test
    public void testFromEventData_nullOrEmptyMap() {
        assertNull(TargetPrefetch.fromEventData(null));

        assertNull(TargetPrefetch.fromEventData(new HashMap<>()));
    }
}
