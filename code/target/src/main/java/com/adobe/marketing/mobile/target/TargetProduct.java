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

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class for specifying Target product parameters
 */
public class TargetProduct {
    private static final String CLASS_NAME = "TargetProduct";

    final private String id;
    final private String categoryId;

    /**
     * Initialize a {@link TargetProduct} with a product {@link #id} and a productCategoryId {@link #categoryId}
     *
     * @param id {@link String} product id
     * @param categoryId {@link String} product category id
     */
    public TargetProduct(final String id, final String categoryId) {
        this.id = id;
        this.categoryId = categoryId;
    }

    /**
     * Get the product {@link #id}
     *
     * @return {@link String} containing the product id.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the product {@link #categoryId}
     *
     * @return {@link String} containing the product category id.
     */
    public String getCategoryId() {
        return categoryId;
    }

    /**
     * Creates a {@code Map<String, Object>} using this {@code TargetProduct}'s attributes.
     *
     * @return {@code Map<String, Object>} containing {@link TargetProduct} data.
     */
    Map<String, String> toEventData() {
        final Map<String, String> productMap = new HashMap<>();
        productMap.put(TargetConstants.EventDataKeys.Product.ID, this.id);
        productMap.put(TargetConstants.EventDataKeys.Product.CATEGORY_ID, this.categoryId);
        return productMap;
    }

    /**
     * Creates a {@code TargetProduct} object using information provided in {@code data} map.
     * <p>
     * This method returns null if the provided {@code data} is null or empty, or if it does not
     * contain required info for creating a {@link TargetProduct} object.
     *
     * @param data {@code Map<String, Object>} containing Target Product data.
     * @return {@code TargetProduct} object or null.
     */
    static TargetProduct fromEventData(final Map<String, String> data) {
        if (TargetUtils.isNullOrEmpty(data)) {
            Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,"Cannot create TargetProduct object, provided data Map is empty or null.");
            return null;
        }

        try {
            final String id = DataReader.getString(data, TargetConstants.EventDataKeys.Product.ID);
            if (StringUtils.isNullOrEmpty(id)) {
                Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "Cannot create TargetProduct object, provided data Map doesn't contain valid product ID.");
                return null;
            }
            final String categoryId = DataReader.getString(data, TargetConstants.EventDataKeys.Product.CATEGORY_ID);

            return new TargetProduct(id, categoryId);
        } catch (final DataReaderException e) {
            Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,"Cannot create TargetProduct object, provided data contains invalid fields.");
            return null;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TargetProduct that = (TargetProduct) o;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return categoryId != null ? categoryId.equals(that.categoryId) : that.categoryId == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, categoryId);
    }
}
