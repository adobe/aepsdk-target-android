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

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Target request object.
 */
public class TargetRequest {
    private static final String CLASS_NAME = TargetProduct.class.getSimpleName();

    final private String mboxName;
    final private TargetParameters targetParameters;
    final private String defaultContent;
    private String responsePairId;
    private AdobeCallback<String> contentCallback;
    private AdobeTargetDetailedCallback contentWithDataCallback;

    /**
     * Instantiate a {@link TargetRequest} object
     *
     * @param mboxName         {@link String} mbox name for this request
     * @param targetParameters {@link TargetParameters} for this request
     * @param defaultContent   {@code String} default content for this request
     * @param contentCallback  {@code  AdobeCallback<String>} which will get called with Target mbox content
     */
    public TargetRequest(final String mboxName, final TargetParameters targetParameters,
                         final String defaultContent, final AdobeCallback<String> contentCallback) {
        this.mboxName = mboxName;
        this.targetParameters = targetParameters;
        this.defaultContent = defaultContent;
        this.contentCallback = contentCallback;
    }

    /**
     * Instantiate a {@link TargetRequest} object.
     *
     * @param mboxName         {@link String} mbox name for this request.
     * @param targetParameters {@link TargetParameters} for this request.
     * @param defaultContent   {@link String} default content for this request.
     * @param contentWithDataCallback  {@link AdobeTargetDetailedCallback} which will get called with Target mbox content and other optional data such as Target response tokens,
     *                                                                    analytics payload, click metric analytics payload if available.
     */
    public TargetRequest(final String mboxName, final TargetParameters targetParameters, final String defaultContent,
                         final AdobeTargetDetailedCallback contentWithDataCallback) {
        this.mboxName = mboxName;
        this.targetParameters = targetParameters;
        this.defaultContent = defaultContent;
        this.contentWithDataCallback = contentWithDataCallback;
    }

    /**
     * Get {@link #mboxName} for this request
     *
     * @return {@link String} containing this request's {@code mboxName}
     */
    public String getMboxName() {
        return mboxName;
    }

    /**
     * Get {@link #targetParameters} for this request
     *
     * @return {@link TargetParameters} for this request
     */
    TargetParameters getTargetParameters() {
        return targetParameters;
    }

    /**
     * Get {@link #defaultContent} for this request
     *
     * @return {@link String} containing this {@code defaultContent}
     */
    public String getDefaultContent() {
        return defaultContent;
    }

    /**
     * Get {@link #responsePairId} for this request
     *
     * @return {@link String} containing this {@code responsePairId}
     */
    String getResponsePairId() {
        return this.responsePairId;
    }

    /**
     * Get {@link #contentCallback} for this request
     *
     * @return {@link AdobeCallback<String>} containing this {@code contentCallback}
     */
    public AdobeCallback<String> getContentCallback() {
        return this.contentCallback;
    }

    /**
     * Get {@link #contentWithDataCallback} for this request
     *
     * @return {@link AdobeTargetDetailedCallback} containing this {@code contentWithDataCallback}
     */
    public AdobeTargetDetailedCallback getContentWithDataCallback() {
        return contentWithDataCallback;
    }

    /**
     * Set {@link #responsePairId} for this request
     *
     * @param pairId {@link String} containing the response pair Id.
     */
    public void setResponsePairId(final String pairId) {
        this.responsePairId = pairId;
    }

    /**
     * Creates a {@code Map<String, Object>} using this {@code TargetRequest}'s attributes.
     *
     * @return {@code Map<String, Object>} containing {@link TargetRequest} data.
     */
    public Map<String, Object> toEventData() {
        final Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(TargetConstants.EventDataKeys.MBOX_NAME, this.mboxName);
        requestMap.put(TargetConstants.EventDataKeys.DEFAULT_CONTENT, this.defaultContent);
        requestMap.put(TargetConstants.EventDataKeys.RESPONSE_PAIR_ID, this.responsePairId);
        if (this.targetParameters != null) {
            requestMap.put(TargetConstants.EventDataKeys.TARGET_PARAMETERS, this.targetParameters.toEventData());
        }
        return requestMap;
    }

    /**
     * Creates a {@code TargetRequest} object using information provided in {@code data} map.
     * <p>
     * This method returns null if the provided {@code data} is null or empty, or if it does not
     * contain required info for creating a {@link TargetRequest} object.
     *
     * @param data {@code Map<String, Object>} containing Target Request data.
     * @return {@code TargetRequest} object or null.
     */
    static TargetRequest fromEventData(final Map<String, Object> data) {
        if (TargetUtils.isNullOrEmpty(data)) {
            Log.debug(LOG_TAG, CLASS_NAME,"Cannot create TargetRequest object, provided data Map is empty or null.");
            return null;
        }

        try {
            final String mboxName = DataReader.getString(data, TargetConstants.EventDataKeys.MBOX_NAME);
            final Map<String, Object> targetParameters = DataReader.getTypedMap(Object.class, data, TargetConstants.EventDataKeys.TARGET_PARAMETERS);
            final String defaultContent = DataReader.getString(data, TargetConstants.EventDataKeys.DEFAULT_CONTENT);
            final String responsePairId = DataReader.getString(data, TargetConstants.EventDataKeys.RESPONSE_PAIR_ID);

            TargetRequest targetRequest = new TargetRequest(mboxName, TargetParameters.fromEventData(targetParameters), defaultContent, (AdobeCallback) null);
            targetRequest.setResponsePairId(responsePairId);

            return targetRequest;
        } catch (final DataReaderException e) {
            Log.warning(LOG_TAG, CLASS_NAME,"Cannot create TargetRequest object, provided data contains invalid fields.");
            return null;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TargetRequest that = (TargetRequest) o;
        if (mboxName != null ? !mboxName.equals(that.mboxName) : that.mboxName != null) return false;
        if (targetParameters != null ? !targetParameters.equals(that.targetParameters) : that.targetParameters != null) return false;
        if (defaultContent != null ? !defaultContent.equals(that.defaultContent) : that.defaultContent != null) return false;
        if (responsePairId != null ? !responsePairId.equals(that.responsePairId) : that.responsePairId != null) return false;
        if (contentCallback != null ? !contentCallback.equals(that.contentCallback) : that.contentCallback != null) return false;
        return contentWithDataCallback != null ? contentWithDataCallback.equals(that.contentWithDataCallback) : that.contentWithDataCallback == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mboxName, targetParameters, contentCallback, contentWithDataCallback, defaultContent, responsePairId);
    }
}
