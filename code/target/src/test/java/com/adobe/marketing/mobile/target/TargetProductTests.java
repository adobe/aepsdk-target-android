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

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TargetProductTests {
    static final String PRODUCT_ID_HAPPY_PATH = "pId";
    static final String CATEGORY_ID_HAPPY_PATH = "cId";

    protected final TargetProduct PRODUCT_PARAMETERS_HAPPY_PATH = new TargetProduct("pId", "cId");

    protected final Map<String, String> PRODUCT_PARAMETERS_HAPPY_PATH_MAP =
            new HashMap<String, String>() {
                {
                    put("id", "pId");
                    put("categoryId", "cId");
                }
            };

    @Test
    public void testTargetProduct_happy() {
        final TargetProduct targetProduct = PRODUCT_PARAMETERS_HAPPY_PATH;
        assertNotNull(targetProduct);
        assertEquals(PRODUCT_ID_HAPPY_PATH, targetProduct.getId());
        assertEquals(CATEGORY_ID_HAPPY_PATH, targetProduct.getCategoryId());
    }

    @Test
    public void testFromEventData_validProduct() throws Exception {
        TargetProduct targetProduct =
                TargetProduct.fromEventData(PRODUCT_PARAMETERS_HAPPY_PATH_MAP);
        assertNotNull(targetProduct);
        assertEquals(PRODUCT_ID_HAPPY_PATH, targetProduct.getId());
        assertEquals(CATEGORY_ID_HAPPY_PATH, targetProduct.getCategoryId());
    }

    @Test
    public void testFromEventData_nullProduct() throws Exception {
        final Map<String, String> productMap = null;
        TargetProduct targetProduct = TargetProduct.fromEventData(productMap);
        assertNull(targetProduct);
    }

    @Test
    public void testFromEventData_emptyProduct() throws Exception {
        final Map<String, String> productMap = new HashMap<String, String>();
        TargetProduct targetProduct = TargetProduct.fromEventData(productMap);
        assertNull(targetProduct);
    }

    @Test
    public void testFromEventData_invalidProductNullId() throws Exception {
        Map<String, String> productMap =
                new HashMap<String, String>() {
                    {
                        put("id", null);
                        put("categoryId", "cId");
                    }
                };
        TargetProduct targetProduct = TargetProduct.fromEventData(productMap);
        assertNull(targetProduct);
    }

    @Test
    public void testFromEventData_invalidOrderEmptyId() throws Exception {
        Map<String, String> productMap =
                new HashMap<String, String>() {
                    {
                        put("id", "");
                        put("categoryId", "cId");
                    }
                };
        TargetProduct targetProduct = TargetProduct.fromEventData(productMap);
        assertNull(targetProduct);
    }

    @Test
    public void testFromEventData_invalidOrderMissingId() throws Exception {
        Map<String, String> productMap =
                new HashMap<String, String>() {
                    {
                        put("categoryId", "cId");
                    }
                };
        TargetProduct targetProduct = TargetProduct.fromEventData(productMap);
        assertNull(targetProduct);
    }

    @Test
    public void testToEventData_happy() {
        final TargetProduct targetProduct = PRODUCT_PARAMETERS_HAPPY_PATH;
        final Map<String, String> productMap = targetProduct.toEventData();
        assertNotNull(productMap);
        assertEquals(PRODUCT_PARAMETERS_HAPPY_PATH_MAP, productMap);
    }
}
