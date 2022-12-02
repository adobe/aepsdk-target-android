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
import java.util.Map;

public class TargetPrefetch {
    private static final String CLASS_NAME = "TargetPrefetch";

    final private String mboxName;
    final private TargetParameters targetParameters;

    /**
     * Get {@link #mboxName} for this request
     *
     * @return {@link String} containing this request's {@code mboxName}
     */
    String getMboxName() {
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
     * Instantiate a {@link TargetPrefetch} object
     * @param mboxName {@link String} mbox name for this prefetch
     * @param targetParameters {@link TargetParameters} for this prefetch
     */
    public TargetPrefetch(final String mboxName, final TargetParameters targetParameters) {
        this.mboxName = mboxName;
        this.targetParameters = targetParameters;
    }

    /**
     * Creates a {@code Map<String, Object>} using this {@code TargetPrefetch}'s attributes.
     *
     * @return {@code Map<String, Object>} containing {@link TargetPrefetch} data.
     */
    public Map<String, Object> toEventData() {
        final Map<String, Object> prefetchMap = new HashMap<>();
        prefetchMap.put(TargetConstants.EventDataKeys.MBOX_NAME, this.mboxName);
        if (this.targetParameters != null) {
            prefetchMap.put(TargetConstants.EventDataKeys.TARGET_PARAMETERS, this.targetParameters.toEventData());
        }
        return prefetchMap;
    }

    /**
     * Creates a {@code TargetPrefetch} object using information provided in {@code data} map.
     * <p>
     * This method returns null if the provided {@code data} is null or empty, or if it does not
     * contain required info for creating a {@link TargetPrefetch} object.
     *
     * @param data {@code Map<String, Object>} containing Target Prefetch data.
     * @return {@code TargetPrefetch} object or null.
     */
    static TargetPrefetch fromEventData(final Map<String, Object> data) {
        if (TargetUtils.isNullOrEmpty(data)) {
            Log.debug(LOG_TAG, CLASS_NAME,"Cannot create TargetPrefetch object, provided data Map is empty or null.");
            return null;
        }

        try {
            final String mboxName = DataReader.getString(data, TargetConstants.EventDataKeys.MBOX_NAME);
            final Map<String, Object> targetParameters = DataReader.getTypedMap(Object.class, data, TargetConstants.EventDataKeys.TARGET_PARAMETERS);

            return new TargetPrefetch(mboxName, TargetParameters.fromEventData(targetParameters));
        } catch (final DataReaderException e) {
            Log.warning(LOG_TAG, CLASS_NAME,"Cannot create TargetPrefetch object, provided data contains invalid fields.");
            return null;
        }
    }
}
