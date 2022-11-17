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
 * Target parameter class, used for specifying custom parameters to be sent in Target requests,
 * such as location (former mbox) parameters, profile parameters, order/product parameters.<br>
 *
 * Use {@link TargetParameters.Builder} to build the desired parameters
 */

public class TargetParameters {
    private static final String CLASS_NAME = TargetParameters.class.getSimpleName();

    final private Map<String, String> parameters;
    final private Map<String, String> profileParameters;
    final private TargetProduct product;
    final private TargetOrder order;

    private TargetParameters(final Builder builder) {
        this.parameters = builder.parameters == null ? new HashMap<String, String>() : builder.parameters;
        this.profileParameters = builder.profileParameters == null ? new HashMap<String, String>() : builder.profileParameters;
        this.product = builder.product;
        this.order = builder.order;
    }

    /**
     * Get mbox {@link #parameters} for this {@link TargetParameters} object
     *
     * @return mbox parameters map
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Get {@link #profileParameters} for this {@link TargetParameters} object
     *
     * @return profile parameters map
     */
    public Map<String, String> getProfileParameters() {
        return profileParameters;
    }

    /**
     * Get {@link #product} parameters for this {@link TargetParameters} object
     *
     * @return {@link TargetProduct} parameters
     */
    public TargetProduct getProduct() {
        return product;
    }

    /**
     * Get {@link #order} parameters for this {@link TargetParameters} object
     *
     * @return {@link TargetOrder} parameters
     */
    public TargetOrder getOrder() {
        return order;
    }

    /**
     * Use this method to merge multiple {@link TargetParameters} objects
     *
     * @param parametersList list of {@code TargetParameters} to merge
     * @return merged {@link TargetParameters}
     */
    static TargetParameters merge(final List<TargetParameters> parametersList) {
        Builder builder = new Builder();

        Map<String, String> mergedParams = new HashMap<String, String>();
        Map<String, String> mergedProfileParams = new HashMap<String, String>();
        TargetProduct mergedProduct = null;
        TargetOrder mergedOrder = null;

        if (parametersList == null) {
            return builder.build();
        }

        for (final TargetParameters targetParams : parametersList) {
            if (targetParams == null) {
                continue;
            }

            try {
                if (targetParams.parameters != null && targetParams.parameters.size() > 0) {
                    mergedParams.putAll(targetParams.parameters);
                    mergedParams.remove("");
                }
            } catch (final Exception e) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "Failed to merge parameters, (%s)", e);
            }

            try {
                if (targetParams.profileParameters != null && targetParams.profileParameters.size() > 0) {
                    mergedProfileParams.putAll(targetParams.profileParameters);
                    mergedProfileParams.remove("");
                }
            } catch (final Exception e) {
                Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "Failed to merge profile parameters, (%s)", e);
            }

            if (targetParams.product != null) {
                mergedProduct = targetParams.product;
            }

            if (targetParams.order != null) {
                mergedOrder = targetParams.order;
            }
        }

        return builder.parameters(mergedParams)
                .profileParameters(mergedProfileParams)
                .product(mergedProduct)
                .order(mergedOrder)
                .build();
    }

    public static class Builder {
        private Map<String, String> parameters;
        private Map<String, String> profileParameters;
        private TargetProduct product;
        private TargetOrder order;

        /**
         * Create a {@link TargetParameters} object Builder
         */
        public Builder() {}

        /**
         * Create a {@link TargetParameters} object Builder
         *
         * @param parameters mbox parameters for the built {@link TargetParameters}
         */
        public Builder(final Map<String, String> parameters) {
            this.parameters = parameters;
        }

        /**
         * Set mbox parameters on the built {@link TargetParameters}
         *
         * @param parameters mbox parameters map
         * @return this {@link TargetParameters.Builder} instance
         */
        public Builder parameters(final Map<String, String> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Set profile parameters on the built {@link TargetParameters}
         *
         * @param profileParameters profile parameters map
         * @return this {@link TargetParameters.Builder} instance
         */
        public Builder profileParameters(final Map<String, String> profileParameters) {
            this.profileParameters = profileParameters;
            return this;
        }

        /**
         * Set product parameters on the built {@link TargetParameters}
         *
         * @param product product parameters
         * @return this {@link TargetParameters.Builder} instance
         */
        public Builder product(final TargetProduct product) {
            this.product = product;
            return this;
        }

        /**
         * Set order parameters on the built {@link TargetParameters}
         *
         * @param order order parameters
         * @return this {@link TargetParameters.Builder} instance
         */
        public Builder order(final TargetOrder order) {
            this.order = order;
            return this;
        }

        /**
         * Build the {@link TargetParameters} object
         *
         * @return the built {@link TargetParameters} object
         */
        public TargetParameters build() {
            return new TargetParameters(this);
        }
    }

    /**
     * Creates a {@code Map<String, Object>} using this {@code TargetParameters}'s attributes.
     *
     * @return {@code Map<String, Object>} containing {@link TargetParameters} data.
     */
    public Map<String, Object> toEventData() {
        final Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put(TargetConstants.EventDataKeys.MBOX_PARAMETERS, this.parameters);
        parametersMap.put(TargetConstants.EventDataKeys.PROFILE_PARAMETERS, this.profileParameters);
        if (this.order != null) {
            parametersMap.put(TargetConstants.EventDataKeys.ORDER_PARAMETERS, this.order.toEventData());
        }
        if (this.product != null) {
            parametersMap.put(TargetConstants.EventDataKeys.PRODUCT_PARAMETERS, this.product.toEventData());
        }
        return parametersMap;
    }

    /**
     * Creates a {@code TargetParameters} object using information provided in {@code data} map.
     * <p>
     * This method returns null if the provided {@code data} is null or empty, or if it does not
     * contain required info for creating a {@link TargetParameters} object.
     *
     * @param data {@code Map<String, Object>} containing Target parameters data.
     * @return {@code TargetParameters} object or null.
     */
    static TargetParameters fromEventData(final Map<String, Object> data) {
        if (TargetUtils.isNullOrEmpty(data)) {
            Log.debug(LOG_TAG, CLASS_NAME,"Cannot create TargetParameters object, provided data Map is empty or null.");
            return null;
        }

        try {
            final Map<String, String> parameters = DataReader.getStringMap(data, TargetConstants.EventDataKeys.MBOX_PARAMETERS);
            final Map<String, String> profileParameters = DataReader.getStringMap(data, TargetConstants.EventDataKeys.PROFILE_PARAMETERS);
            final Map<String, String> productParameters = DataReader.getStringMap(data, TargetConstants.EventDataKeys.PRODUCT_PARAMETERS);
            final Map<String, Object> orderParameters = DataReader.getTypedMap(Object.class, data, TargetConstants.EventDataKeys.ORDER_PARAMETERS);

            return new TargetParameters.Builder()
                    .parameters(parameters)
                    .profileParameters(profileParameters)
                    .order(TargetOrder.fromEventData(orderParameters))
                    .product(TargetProduct.fromEventData(productParameters))
                    .build();
        } catch (final DataReaderException e) {
            Log.warning(LOG_TAG, CLASS_NAME,"Cannot create TargetProduct object, provided data contains invalid fields.");
            return null;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TargetParameters that = (TargetParameters) o;
        if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;
        if (profileParameters != null ? !profileParameters.equals(that.profileParameters) : that.profileParameters != null) return false;
        if (order != null ? !order.equals(that.order) : that.order != null) return false;
        return product != null ? product.equals(that.product) : that.product == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters, profileParameters, order, product);
    }
}
