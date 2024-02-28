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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class TargetParametersTests {
    protected final Map<String, String> MBOX_PARAMETERS_HAPPY_PATH =
            new HashMap<String, String>() {
                {
                    put("one", "uno");
                    put("two", "dos");
                    put("three", "tres");
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

    protected final Map<String, String> PROFILE_PARAMETERS_HAPPY_PATH =
            new HashMap<String, String>() {
                {
                    put("profileKey1", "profileValue1");
                    put("profileKey2", "profileValue2");
                }
            };

    @Test
    public void testTargetParametersBuilder_happy() {
        final TargetParameters targetParameters =
                new TargetParameters.Builder()
                        .product(PRODUCT_PARAMETERS_HAPPY_PATH)
                        .order(ORDER_PARAMETERS_HAPPY_PATH)
                        .profileParameters(PROFILE_PARAMETERS_HAPPY_PATH)
                        .parameters(MBOX_PARAMETERS_HAPPY_PATH)
                        .build();
        assertNotNull(targetParameters);
        assertEquals(PROFILE_PARAMETERS_HAPPY_PATH, targetParameters.getProfileParameters());
        assertEquals(MBOX_PARAMETERS_HAPPY_PATH, targetParameters.getParameters());
        assertEquals(PRODUCT_PARAMETERS_HAPPY_PATH, targetParameters.getProduct());
        assertEquals(ORDER_PARAMETERS_HAPPY_PATH, targetParameters.getOrder());
    }

    @Test
    public void testTargetParametersBuilder_noMboxParameters() {
        final TargetParameters targetParameters =
                new TargetParameters.Builder()
                        .product(PRODUCT_PARAMETERS_HAPPY_PATH)
                        .order(ORDER_PARAMETERS_HAPPY_PATH)
                        .profileParameters(PROFILE_PARAMETERS_HAPPY_PATH)
                        .build();
        assertNotNull(targetParameters);
        assertEquals(0, targetParameters.getParameters().size());
        assertEquals(PROFILE_PARAMETERS_HAPPY_PATH, targetParameters.getProfileParameters());
        assertEquals(PRODUCT_PARAMETERS_HAPPY_PATH, targetParameters.getProduct());
        assertEquals(ORDER_PARAMETERS_HAPPY_PATH, targetParameters.getOrder());
    }

    @Test
    public void testTargetParametersBuilder_noProfileParameters() {
        final TargetParameters targetParameters =
                new TargetParameters.Builder()
                        .product(PRODUCT_PARAMETERS_HAPPY_PATH)
                        .order(ORDER_PARAMETERS_HAPPY_PATH)
                        .parameters(MBOX_PARAMETERS_HAPPY_PATH)
                        .build();
        assertNotNull(targetParameters);
        assertEquals(0, targetParameters.getProfileParameters().size());
        assertEquals(MBOX_PARAMETERS_HAPPY_PATH, targetParameters.getParameters());
        assertEquals(PRODUCT_PARAMETERS_HAPPY_PATH, targetParameters.getProduct());
        assertEquals(ORDER_PARAMETERS_HAPPY_PATH, targetParameters.getOrder());
    }

    @Test
    public void testTargetParametersBuilder_noOrderParameters() {
        final TargetParameters targetParameters =
                new TargetParameters.Builder()
                        .product(PRODUCT_PARAMETERS_HAPPY_PATH)
                        .profileParameters(PROFILE_PARAMETERS_HAPPY_PATH)
                        .parameters(MBOX_PARAMETERS_HAPPY_PATH)
                        .build();
        assertNotNull(targetParameters);
        assertNull(targetParameters.getOrder());
        assertEquals(PROFILE_PARAMETERS_HAPPY_PATH, targetParameters.getProfileParameters());
        assertEquals(MBOX_PARAMETERS_HAPPY_PATH, targetParameters.getParameters());
        assertEquals(PRODUCT_PARAMETERS_HAPPY_PATH, targetParameters.getProduct());
    }

    @Test
    public void testTargetParametersBuilder_noProductParameters() {
        final TargetParameters targetParameters =
                new TargetParameters.Builder()
                        .profileParameters(PROFILE_PARAMETERS_HAPPY_PATH)
                        .order(ORDER_PARAMETERS_HAPPY_PATH)
                        .parameters(MBOX_PARAMETERS_HAPPY_PATH)
                        .build();
        assertNotNull(targetParameters);
        assertNull(targetParameters.getProduct());
        assertEquals(PROFILE_PARAMETERS_HAPPY_PATH, targetParameters.getProfileParameters());
        assertEquals(MBOX_PARAMETERS_HAPPY_PATH, targetParameters.getParameters());
        assertEquals(ORDER_PARAMETERS_HAPPY_PATH, targetParameters.getOrder());
    }

    @Test
    public void testTargetParametersBuilder_default() {
        final TargetParameters targetParameters = new TargetParameters.Builder().build();
        assertNotNull(targetParameters);
        assertNull(targetParameters.getProduct());
        assertNull(targetParameters.getOrder());
        assertEquals(0, targetParameters.getProfileParameters().size());
        assertEquals(0, targetParameters.getParameters().size());
    }

    @Test
    public void testToEventData_happy() {
        final TargetParameters targetParameters =
                new TargetParameters.Builder()
                        .product(PRODUCT_PARAMETERS_HAPPY_PATH)
                        .order(ORDER_PARAMETERS_HAPPY_PATH)
                        .profileParameters(PROFILE_PARAMETERS_HAPPY_PATH)
                        .parameters(MBOX_PARAMETERS_HAPPY_PATH)
                        .build();
        final Map<String, Object> targetParametersMap = targetParameters.toEventData();
        assertNotNull(targetParametersMap);
        assertEquals(MBOX_PARAMETERS_HAPPY_PATH, targetParametersMap.get("parameters"));
        assertEquals(PROFILE_PARAMETERS_HAPPY_PATH, targetParametersMap.get("profileParameters"));
        assertEquals(ORDER_PARAMETERS_HAPPY_PATH_MAP, targetParametersMap.get("order"));
        assertEquals(PRODUCT_PARAMETERS_HAPPY_PATH_MAP, targetParametersMap.get("product"));
    }

    @Test
    public void testFromEventData_happy() {
        final Map<String, Object> targetParametersMap = new HashMap<>();
        targetParametersMap.put("parameters", MBOX_PARAMETERS_HAPPY_PATH);
        targetParametersMap.put("profileParameters", PROFILE_PARAMETERS_HAPPY_PATH);
        targetParametersMap.put("order", ORDER_PARAMETERS_HAPPY_PATH_MAP);
        targetParametersMap.put("product", PRODUCT_PARAMETERS_HAPPY_PATH_MAP);

        final TargetParameters targetParameters =
                TargetParameters.fromEventData(targetParametersMap);
        assertNotNull(targetParameters);
        assertEquals(MBOX_PARAMETERS_HAPPY_PATH, targetParameters.getParameters());
        assertEquals(PROFILE_PARAMETERS_HAPPY_PATH, targetParameters.getProfileParameters());
        assertEquals(ORDER_PARAMETERS_HAPPY_PATH, targetParameters.getOrder());
        assertEquals(PRODUCT_PARAMETERS_HAPPY_PATH, targetParameters.getProduct());
    }

    @Test
    public void testFromEventData_nullOrEmptyMap() {
        assertNull(TargetParameters.fromEventData(null));

        assertNull(TargetParameters.fromEventData(new HashMap<>()));
    }

    @Test
    public void testMerge_happy() {
        final List<TargetParameters> targetParametersList = new ArrayList<>();
        final TargetParameters targetParamsWithProduct =
                new TargetParameters.Builder().product(PRODUCT_PARAMETERS_HAPPY_PATH).build();
        final TargetParameters targetParamsWithOrder =
                new TargetParameters.Builder().order(ORDER_PARAMETERS_HAPPY_PATH).build();
        final TargetParameters targetParamsWithProfileAndMboxParameters =
                new TargetParameters.Builder()
                        .profileParameters(PROFILE_PARAMETERS_HAPPY_PATH)
                        .parameters(MBOX_PARAMETERS_HAPPY_PATH)
                        .build();
        targetParametersList.add(targetParamsWithProduct);
        targetParametersList.add(targetParamsWithOrder);
        targetParametersList.add(targetParamsWithProfileAndMboxParameters);

        final TargetParameters mergedTargetParameters =
                TargetParameters.merge(targetParametersList);
        assertNotNull(mergedTargetParameters);
        assertEquals(PROFILE_PARAMETERS_HAPPY_PATH, mergedTargetParameters.getProfileParameters());
        assertEquals(MBOX_PARAMETERS_HAPPY_PATH, mergedTargetParameters.getParameters());
        assertEquals(ORDER_PARAMETERS_HAPPY_PATH, mergedTargetParameters.getOrder());
        assertEquals(PRODUCT_PARAMETERS_HAPPY_PATH, mergedTargetParameters.getProduct());
    }

    @Test
    public void testMerge_differentMboxParametersInList() {
        final List<TargetParameters> targetParametersList = new ArrayList<>();
        final TargetParameters targetParamsWithProfileAndMboxParameters =
                new TargetParameters.Builder()
                        .profileParameters(PROFILE_PARAMETERS_HAPPY_PATH)
                        .parameters(MBOX_PARAMETERS_HAPPY_PATH)
                        .build();
        targetParametersList.add(targetParamsWithProfileAndMboxParameters);

        final TargetParameters mergedTargetParameters =
                TargetParameters.merge(targetParametersList);
        assertNotNull(mergedTargetParameters);
        assertEquals(PROFILE_PARAMETERS_HAPPY_PATH, mergedTargetParameters.getProfileParameters());
        assertEquals(MBOX_PARAMETERS_HAPPY_PATH, mergedTargetParameters.getParameters());

        final TargetParameters targetParametersWithMboxParameters =
                new TargetParameters.Builder()
                        .parameters(
                                new HashMap<String, String>() {
                                    {
                                        put("mboxkey", "mboxvalue");
                                        put("mboxkey2", "mboxvalue2");
                                    }
                                })
                        .build();
        targetParametersList.add(targetParametersWithMboxParameters);

        final TargetParameters mergedTargetParameters1 =
                TargetParameters.merge(targetParametersList);

        assertNotEquals(MBOX_PARAMETERS_HAPPY_PATH, mergedTargetParameters1.getParameters());
        assertEquals(5, mergedTargetParameters1.getParameters().size());
        assertEquals("mboxvalue", mergedTargetParameters1.getParameters().get("mboxkey"));
        assertEquals("mboxvalue2", mergedTargetParameters1.getParameters().get("mboxkey2"));
        assertEquals("uno", mergedTargetParameters1.getParameters().get("one"));
        assertEquals("dos", mergedTargetParameters1.getParameters().get("two"));
        assertEquals("tres", mergedTargetParameters1.getParameters().get("three"));
    }

    @Test
    public void testMerge_emptyList() {
        List<TargetParameters> targetParametersList = new ArrayList<>();
        TargetParameters mergedTargetParameters = TargetParameters.merge(targetParametersList);
        assertNotNull(mergedTargetParameters);
    }

    @Test
    public void testMerge_nullList() {
        List<TargetParameters> targetParametersList = null;
        TargetParameters mergedTargetParameters = TargetParameters.merge(targetParametersList);
        assertNotNull(mergedTargetParameters);
    }
}
