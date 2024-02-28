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

public class TargetOrderTests {
    static final String ORDER_ID_HAPPY_PATH = "orderId";
    static final double ORDER_TOTAL_HAPPY_PATH = 100.34;
    protected final ArrayList<String> PURCHASED_PRODUCT_IDS_HAPPY_PATH =
            new ArrayList<String>() {
                {
                    add("ppId");
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

    @Test
    public void testTargetOrder_happy() {
        final TargetOrder targetOrder = ORDER_PARAMETERS_HAPPY_PATH;
        assertNotNull(targetOrder);
        assertEquals(ORDER_ID_HAPPY_PATH, targetOrder.getId());
        assertEquals(ORDER_TOTAL_HAPPY_PATH, targetOrder.getTotal(), 0.001);
        assertEquals(PURCHASED_PRODUCT_IDS_HAPPY_PATH, targetOrder.getPurchasedProductIds());
    }

    @Test
    public void testFromEventData_validOrder() throws Exception {
        TargetOrder targetOrder = TargetOrder.fromEventData(ORDER_PARAMETERS_HAPPY_PATH_MAP);
        assertNotNull(targetOrder);
        assertEquals(ORDER_ID_HAPPY_PATH, targetOrder.getId());
        assertEquals(ORDER_TOTAL_HAPPY_PATH, targetOrder.getTotal(), 0.001);
        assertEquals(PURCHASED_PRODUCT_IDS_HAPPY_PATH, targetOrder.getPurchasedProductIds());
    }

    @Test
    public void testFromEventData_nullOrder() throws Exception {
        final Map<String, Object> orderMap = null;
        TargetOrder targetOrder = TargetOrder.fromEventData(orderMap);
        assertNull(targetOrder);
    }

    @Test
    public void testFromEventData_emptyOrder() throws Exception {
        final Map<String, Object> orderMap = new HashMap<String, Object>();
        TargetOrder targetOrder = TargetOrder.fromEventData(orderMap);
        assertNull(targetOrder);
    }

    @Test
    public void testFromEventData_invalidOrderNullId() throws Exception {
        final Map<String, Object> orderMap =
                new HashMap<String, Object>() {
                    {
                        put("id", null);
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
        TargetOrder targetOrder = TargetOrder.fromEventData(orderMap);
        assertNull(targetOrder);
    }

    @Test
    public void testFromEventData_invalidOrderEmptyId() throws Exception {
        final Map<String, Object> orderMap =
                new HashMap<String, Object>() {
                    {
                        put("id", "");
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
        TargetOrder targetOrder = TargetOrder.fromEventData(orderMap);
        assertNull(targetOrder);
    }

    @Test
    public void testFromEventData_invalidOrderMissingId() throws Exception {
        final Map<String, Object> orderMap =
                new HashMap<String, Object>() {
                    {
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
        TargetOrder targetOrder = TargetOrder.fromEventData(orderMap);
        assertNull(targetOrder);
    }

    @Test
    public void testToEventData_happy() {
        final TargetOrder targetOrder = ORDER_PARAMETERS_HAPPY_PATH;
        final Map<String, Object> orderMap = targetOrder.toEventData();
        assertNotNull(orderMap);
        assertEquals(ORDER_PARAMETERS_HAPPY_PATH_MAP, orderMap);
    }
}
