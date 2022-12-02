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

import static com.adobe.marketing.mobile.target.TargetConstants.LOG_TAG;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Class for specifying Target order parameters
 */
public class TargetOrder {
    private static final String CLASS_NAME = "TargetOrder";

    final private String id;
    final private double total;
    final private List<String> purchasedProductIds;

    /**
     * Initialize a {@link TargetOrder} with an order {@link #id}, order {@link #total} and a list
     * of {@link #purchasedProductIds}
     *
     * @param id {@link String} order id
     * @param total {@code double} order total amount
     * @param purchasedProductIds a list of purchased product ids
     */
    public TargetOrder(final String id, final double total, final List<String> purchasedProductIds) {
        this.id = id;
        this.total = total;
        this.purchasedProductIds = purchasedProductIds;
    }

    /**
     * Get the order {@link #id}
     *
     * @return {@link String} containing the order id.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the order {@link #total}
     *
     * @return {@code double} order total.
     */
    public double getTotal() {
        return total;
    }

    /**
     * Get the order {@link #purchasedProductIds}
     *
     * @return a {@code List<String>} of this order's purchased product ids.
     */
    public List<String> getPurchasedProductIds() {
        return purchasedProductIds;
    }

    /**
     * Creates a {@code Map<String, Object>} using this {@code TargetOrder}'s attributes.
     *
     * @return {@code Map<String, Object>} containing {@link TargetOrder} data.
     */
    Map<String, Object> toEventData() {
        final Map<String, Object> orderMap = new HashMap<>();
        orderMap.put(TargetConstants.EventDataKeys.Order.ID, this.id);
        orderMap.put(TargetConstants.EventDataKeys.Order.TOTAL, this.total);
        orderMap.put(TargetConstants.EventDataKeys.Order.PURCHASED_PRODUCT_IDS, this.purchasedProductIds);
        return orderMap;
    }

    /**
     * Creates a {@code TargetOrder} object using information provided in {@code data} map.
     * <p>
     * This method returns null if the provided {@code data} is null or empty, or if it does not
     * contain required info for creating a {@link TargetOrder} object.
     *
     * @param data {@code Map<String, Object>} containing Target Order data.
     * @return {@code TargetOrder} object or null.
     */
    static TargetOrder fromEventData(final Map<String, Object> data) {
        if (TargetUtils.isNullOrEmpty(data)) {
            Log.debug(LOG_TAG, CLASS_NAME,"Cannot create TargetOrder object, provided data Map is empty or null.");
            return null;
        }

        try {
            final String id = DataReader.getString(data, TargetConstants.EventDataKeys.Order.ID);
            if (TargetUtils.isNullOrEmpty(id)) {
                Log.debug(LOG_TAG, CLASS_NAME, "Cannot create TargetOrder object, provided data Map doesn't contain valid order ID.");
                return null;
            }
            final double total = DataReader.getDouble(data, TargetConstants.EventDataKeys.Order.TOTAL);
            final List<String> purchasedProductIds = DataReader.getStringList(data, TargetConstants.EventDataKeys.Order.PURCHASED_PRODUCT_IDS);

            return new TargetOrder(id, total, purchasedProductIds);
        } catch (final DataReaderException e) {
            Log.warning(LOG_TAG, CLASS_NAME,"Cannot create TargetOrder object, provided data contains invalid fields (%s).", e.getLocalizedMessage());
            return null;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TargetOrder that = (TargetOrder) o;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (total != that.total) return false;
        return purchasedProductIds != null ? purchasedProductIds.equals(that.purchasedProductIds) : that.purchasedProductIds == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, total, purchasedProductIds);
    }
}