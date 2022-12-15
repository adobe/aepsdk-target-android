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

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

class TargetUtils {
    private static final long MILLISECONDS_PER_SECOND = 1000L;
    private static final double SECONDS_PER_MINUTE = 60;

    /**
     * Checks if the given {@code collection} is null or empty.
     *
     * @param collection input {@code Collection<?>} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code collection} is null or empty.
     */
    static boolean isNullOrEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Checks if the given {@code map} is null or empty.
     *
     * @param map input {@code Map<?, ?>} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code map} is null or empty.
     */
    static boolean isNullOrEmpty(final Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Gets the UTC time offset in minutes
     *
     * @return UTC time offset in minutes
     */
    static double getUTCTimeOffsetMinutes() {
        final TimeZone tz = TimeZone.getDefault();
        final Date now = new Date();
        return tz.getOffset(now.getTime()) / (double) MILLISECONDS_PER_SECOND / SECONDS_PER_MINUTE;
    }

    /**
     * Returns a {@code Map<String, String>} instance from a {@code JSONObject} instance if valid, null otherwise
     *
     * @param jsonObject {@link JSONObject} instance to be converted
     * @return {@code Map<String, String} if given object has valid format, null otherwise
     */
    @Nullable
    static Map<String, String> toStringMap(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        Iterator<String> keyItr = jsonObject.keys();
        Map<String, String> map = new HashMap<>();

        while (keyItr.hasNext()) {
            String name = keyItr.next();

            try {
                map.put(name, jsonObject.getString(name));
            } catch (JSONException e) {
                continue;
            }
        }

        return map;
    }
}
