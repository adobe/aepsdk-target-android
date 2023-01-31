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

import com.adobe.marketing.mobile.services.HttpConnecting;
import androidx.annotation.Nullable;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.StreamUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class TargetResponseParser {

	private static final String CLASS_NAME = "TargetResponseParser";

	/**
	 * Parse the target server response to a json object if there is no error.
	 * <p>
	 * Do not pass a null connection object to this method.
	 * This method return null if
	 * <ol>
	 *   <li>There is any error occurred while reading the JSON response.</li>
	 *   <li>If the response code for the connection is not 200 (HTTP_OK).</li>
	 *   <li>If the response from the server is empty or not a valid JSON.</li>
	 * </ol>
	 *
	 * @param connection the network {@link com.adobe.marketing.mobile.services.HttpConnecting} object returned from server
	 * @return the {@link JSONObject} if the response is valid
	 */
	@Nullable
	JSONObject parseResponseToJson(final HttpConnecting connection) {
		try {
			if (connection != null) {
				final String responseString = StreamUtils.readAsString(connection.getInputStream());
				if (StringUtils.isNullOrEmpty(responseString)) {
					return null;
				}
				final JSONObject responseJson = new JSONObject(responseString);
				Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "Target Response was received : %s", responseString);
				return responseJson;
			}
		} catch (final JSONException e) {
			Log.error(TargetConstants.LOG_TAG, CLASS_NAME,"Unable to parse Target Response, Error (%s)", e);
		}
		return null;
	}

	/**
	 * Extracts the mboxes from the server response for a certain key.
	 * <p>
	 * Used by methods {@code #extractPrefetchedMboxes(JSONObject)} and {@code #extractBatchedMBoxes(JSONObject)}.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson A {@link JSONObject} server response
	 * @param key A {@link String} key name for which mbox need should the mboxes be extracted from
	 * @return all the mboxes for the given key
	 */
	private JSONArray getMboxesFromKey(final JSONObject serverResponseJson, final String key) {
		final JSONObject containerJson = serverResponseJson.optJSONObject(key);

		if (containerJson == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "getMboxesFromKey - Unable to retrieve mboxes from key, json is null");
			return null;
		}

		final JSONArray mboxJSONArray = containerJson.optJSONArray(TargetJson.MBOXES);

		if (mboxJSONArray == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "getMboxesFromKey - Unable to retrieve mboxes from key, mboxes array is null");
			return null;
		}

		return mboxJSONArray;
	}

	/**
	 * Extracts the batched mboxes from the server response and returns them as a {@code Map}, where the mbox name is the key
	 * and the {@code JSONObject} returned from the server is the value.
	 * <p>
	 * Returns null if there is no {@link TargetJson#MBOX_RESPONSES} key found in the server response.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson A {@link JSONObject} server response
	 * @return A {@link Map} of all the batched mboxes
	 */
	@Nullable
	Map<String, JSONObject> extractBatchedMBoxes(final JSONObject serverResponseJson) {
		final JSONArray batchedMboxes = getMboxesFromKey(serverResponseJson, TargetJson.EXECUTE);

		if (batchedMboxes == null) {
			return null;
		}

		final Map<String, JSONObject> mboxResponses = new HashMap<>();

		for (int i = 0; i < batchedMboxes.length(); i++) {
			final JSONObject mboxJson = batchedMboxes.optJSONObject(i);

			if (mboxJson == null) {
				continue;
			}

			final String mboxName = mboxJson.optString(TargetJson.Mbox.NAME, "");

			if (StringUtils.isNullOrEmpty(mboxName)) {
				continue;
			}

			mboxResponses.put(mboxName, mboxJson);
		}

		return mboxResponses;
	}

	/**
	 * Extracts the prefetched mboxes from the server response and returns them as a {@code Map}, where the mbox name is the key
	 * and the {@code JSONObject} returned from the server is the value.
	 * <p>
	 * Returns null if there is no {@code TargetJson#PREFETCH_MBOX_RESPONSES} key found in the server response.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson A {@link JSONObject} server response
	 * @return A {@link Map} of all the prefetched mboxes
	 */
	@Nullable
	Map<String, JSONObject> extractPrefetchedMboxes(final JSONObject serverResponseJson) {
		final JSONArray prefetchedMboxes = getMboxesFromKey(serverResponseJson, TargetJson.PREFETCH);

		if (prefetchedMboxes == null) {
			return null;
		}

		final Map<String, JSONObject> mboxResponses = new HashMap<String, JSONObject>();

		for (int i = 0; i < prefetchedMboxes.length(); i++) {
			final JSONObject mboxJson = prefetchedMboxes.optJSONObject(i);

			if (mboxJson == null) {
				continue;
			}

			final String mboxName = mboxJson.optString(TargetJson.Mbox.NAME, "");

			if (StringUtils.isNullOrEmpty(mboxName)) {
				continue;
			}

			final Iterator<String> keyIterator = mboxJson.keys();
			final List<String> keyCache = new ArrayList<String>();

			while (keyIterator.hasNext()) {
				keyCache.add(keyIterator.next());
			}

			for (final String key : keyCache) {
				if (!TargetJson.CACHED_MBOX_ACCEPTED_KEYS.contains(key)) {
					mboxJson.remove(key);
				}
			}

			mboxResponses.put(mboxName, mboxJson);
		}

		return mboxResponses;
	}

	/**
	 * Get the tnt id from the {@code JSONObject} server response.
	 * <p>
	 * Returns null if there is no {@code TargetJson#ID} key found in the server response.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson  A {@link JSONObject} server response
	 * @return A {@link String} tntid
	 */
	@Nullable
	String getTntId(final JSONObject serverResponseJson) {
		final JSONObject idJson = serverResponseJson.optJSONObject(TargetJson.ID);

		if (idJson == null) {
			return null;
		}

		return idJson.optString(TargetJson.ID_TNT_ID, "");
	}

	/**
	 * Get the edge host from the {@code JSONObject} server response
	 * <p>
	 * Returns an empty {@code String} if there is no {@code TargetJson#EDGE_HOST} key found in the server response.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson A {@link JSONObject} server response
	 * @return A {@link String} edge host
	 */
	String getEdgeHost(final JSONObject serverResponseJson) {
		return serverResponseJson.optString(TargetJson.EDGE_HOST, "");
	}


	/**
	 * Grabs the a4t payload from the target response and convert the keys to correct format
	 * <p>
	 * Returns null if there is no analytics payload that needs to be sent.
	 *
	 * @param mboxJson A prefetched mbox {@link JSONObject}
	 * @return A {@link Map} containing a4t payload
	 */
	@Nullable
	Map<String, String> getAnalyticsForTargetPayload(final JSONObject mboxJson, final String sessionId) {
		final Map<String, String> payload = getAnalyticsForTargetPayload(mboxJson);
		return preprocessAnalyticsForTargetPayload(payload, sessionId);
	}

	/**
	 * Converts the A4T params keys to correct format as required by the Analytics
	 * <p>
	 * Returns null if {@code payload} is null or empty
	 *
	 * @param payload {@code Map<String, String>} with A4t params
	 * @param sessionId Target session id
	 * @return {@code Map<String, String>} with processed keys
	 */
	@Nullable
	Map<String, String> preprocessAnalyticsForTargetPayload(final Map<String, String> payload, final String sessionId) {
		if (TargetUtils.isNullOrEmpty(payload)) {
			return null;
		}

		final Map<String, String> modifiedPayload = new HashMap<String, String>();

		for (Map.Entry<String, String> entry : payload.entrySet()) {
			modifiedPayload.put("&&" + entry.getKey(), entry.getValue());
		}

		if (!StringUtils.isNullOrEmpty(sessionId)) {
			modifiedPayload.put(TargetConstants.EventDataKeys.A4T_SESSION_ID, sessionId);
		}

		return modifiedPayload;
	}

	/**
	 * Parse the JSON object parameter to read A4T payload and returns it as a {@code Map<String, String>}.
	 * Returns null if {@code json} doesn't have A4T payload.
	 *
	 * @param json {@link JSONObject} containing analytics payload
	 * @return {@code Map<String, String>} containing A4T params
	 */
	@Nullable
	Map<String, String> getAnalyticsForTargetPayload(final JSONObject json) {
		if (json == null) {
			return null;
		}

		final JSONObject analyticsJson = json.optJSONObject(TargetJson.ANALYTICS_PARAMETERS);

		if (analyticsJson == null) {
			return null;
		}

		final JSONObject payloadJson = analyticsJson.optJSONObject(TargetJson.ANALYTICS_PAYLOAD);

		if (payloadJson == null) {
			return null;
		}

		return TargetUtils.toStringMap(payloadJson);
	}

	/**
	 * Parse the Mbox JSON object to read Response Tokens from the Options. The data will be read from the first option in the options list.
	 *
	 * @param mboxJson Mbox {@link JSONObject}
	 * @return Response Tokens from options payload as {@code Map<String, String>} OR null if Response Tokens are not activated on Target.
	 */
	Map<String, String> getResponseTokens(final JSONObject mboxJson) {
		if (mboxJson == null) {
			return null;
		}

		final JSONArray optionsArray = mboxJson.optJSONArray(TargetJson.OPTIONS);

		if (JSONUtils.isNullOrEmpty(optionsArray)) {
			return null;
		}

		// Mbox payload will have a single option object in options array, which is accessed using the index 0 and
		// further used to grab response tokens.
		final JSONObject option = optionsArray.optJSONObject(0);

		if (option == null) {
			return null;
		}

		final JSONObject responseTokens = option.optJSONObject(TargetJson.Option.RESPONSE_TOKENS);

		if (responseTokens == null) {
			return null;
		}

		return TargetUtils.toStringMap(responseTokens);
	}

	/**
	 * Extracts the click metric A4T params from the {@code mboxJson} and return them as a {@code Map<String, String>}.
	 * <p>
	 * Returns null if click metric analytics payload is missing in the mbox payload or there is any error in parsing {@code mboxJson}.
	 *
	 * @param mboxJson {@link JSONObject} of a mbox
	 * @return {@code Map<String, String>} containing click metric A4T params
	 */
	Map<String, String> extractClickMetricAnalyticsPayload(final JSONObject mboxJson) {
		final JSONObject clickMetric = getClickMetric(mboxJson);
		return getAnalyticsForTargetPayload(clickMetric);
	}

	/**
	 * Grab the click metric {@link JSONObject} and returns it
	 * <p>
	 * This method returns null if the input {@code mboxJson} is null,
	 * or if the metrics array is not present in the {@code mboxJson},
	 * or if a valid click metric object is not found in the metrics array,
	 * or if the eventToken is not found in the click metric object
	 *
	 * @param mboxJson {@code JSONObject} for mbox
	 * @return {@code JSONObject} for click metric
	 */
	JSONObject getClickMetric(final JSONObject mboxJson) {
		if (mboxJson == null) {
			return null;
		}

		final JSONArray metricsArray = mboxJson.optJSONArray(TargetJson.METRICS);

		if (JSONUtils.isNullOrEmpty(metricsArray)) {
			return null;
		}

		JSONObject clickMetric = null;

		for (int i = 0; i < metricsArray.length(); i++) {
			final JSONObject metric = metricsArray.optJSONObject(i);

			if (metric == null ||
					!TargetJson.MetricType.CLICK.equals(metric.optString(TargetJson.Metric.TYPE, null))
					|| StringUtils.isNullOrEmpty(metric.optString(TargetJson.Metric.EVENT_TOKEN, null))) {
				continue;
			}

			clickMetric = metric;
			break;
		}

		return clickMetric;
	}

	/**
	 * Return the Target error message, if any
	 *
	 * @param responseJson {@link JSONObject} Target response JSON
	 * @return {@link String} response error, if any
	 */
	String getErrorMessage(final JSONObject responseJson) {
		if (responseJson == null) {
			return null;
		}

		return responseJson.optString(TargetJson.MESSAGE, null);
	}

	/**
	 * Return Mbox content from mboxJson, if any
	 *
	 * @param mboxJson {@link JSONObject} Target response JSON
	 * @return {@link String} mbox content, if any otherwise returns null
	 */
	String extractMboxContent(final JSONObject mboxJson) {
		if (mboxJson == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
					"extractMboxContent - unable to extract mbox contents, mbox json is null");
			return null;
		}

		final JSONArray optionsArray = mboxJson.optJSONArray(TargetJson.OPTIONS);

		if (optionsArray == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
					"extractMboxContent - unable to extract mbox contents, options array is null");
			return null;
		}

		final StringBuilder contentBuilder = new StringBuilder();

		for (int i = 0; i < optionsArray.length(); i++) {
			final JSONObject option = optionsArray.optJSONObject(i);

			if (option == null || StringUtils.isNullOrEmpty(option.optString(TargetJson.Option.CONTENT, ""))) {
				continue;
			}

			final String optionType = option.optString(TargetJson.Option.TYPE, "");
			String optionContent = "";

			if (optionType.equals(TargetJson.HTML)) {
				optionContent = option.optString(TargetJson.Option.CONTENT, "");
			} else if (optionType.equals(TargetJson.JSON)) {
				final JSONObject contentJSON = option.optJSONObject(TargetJson.Option.CONTENT);

				if (contentJSON != null) {
					optionContent = contentJSON.toString();
				}
			}

			contentBuilder.append(optionContent);
		}

		return contentBuilder.toString();
	}
}