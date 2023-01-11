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

import com.adobe.marketing.mobile.VisitorID;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class is used to create the json body for the target requests.
 */
class TargetRequestBuilder {
	private static final String CLASS_NAME = "TargetRequestBuilder";
	private final DeviceInforming deviceInfoService;
	private final TargetPreviewManager targetPreviewManager;
	private final TargetState targetState;

	/**
	 * Constructor for TargetRequestBuilder.
	 * @param deviceInfoService {@link DeviceInforming} instance
	 * @param targetPreviewManager {@link TargetPreviewManager} instance
	 */
	TargetRequestBuilder(final DeviceInforming deviceInfoService,
						 final TargetPreviewManager targetPreviewManager,
						 final TargetState targetState) {
		this.deviceInfoService = deviceInfoService;
		this.targetPreviewManager = targetPreviewManager;
		this.targetState = targetState;
	}

	/**
	 * Creates the target request {@code JSONObject} that we have to send to target APIs.
	 *
	 * @param prefetchArray     the list of {@link TargetPrefetch} objects with mboxes that we want to prefetch
	 * @param executeArray      the list of {@link TargetRequest} objects with mboxes that we want to execute
	 * @param parameters        {@link TargetParameters} object provided by the customer
	 * @param notifications     viewed mboxes that we cached
	 * @param propertyToken a {@link String} to be passed for all requests
	 * @param identitySharedState {@code Map<String, Object} shared state of Identity extension
	 * @param lifecycleData {@code Map<String, String} shared state of Lifecycle extension
	 * @return the pre-built {@link JSONObject} for target request
	 */
	JSONObject getRequestPayload(final List<TargetPrefetch> prefetchArray,
								 final List<TargetRequest> executeArray,
								 final TargetParameters parameters,
								 final List<JSONObject> notifications,
								 final String propertyToken,
								 final Map<String, Object> identitySharedState,
								 final Map<String, String> lifecycleData) {
		try {
			// add default parameters
			final JSONObject defaultParamJson = getDefaultJsonObject(null,
					null,
					null,
					targetState.getEnvironmentId(),
					identitySharedState);

			// add prefetch mBoxes
			final JSONArray prefetchMboxesNode = getPrefetchMboxes(prefetchArray, parameters, lifecycleData);

			if (prefetchMboxesNode != null && prefetchMboxesNode.length() > 0) {
				final JSONObject prefetchJson = new JSONObject();
				prefetchJson.put(TargetJson.MBOXES, prefetchMboxesNode);
				defaultParamJson.put(TargetJson.PREFETCH, prefetchJson);
			}

			// add notification mBoxes
			if (notifications != null && !notifications.isEmpty()) {
				defaultParamJson.put(TargetJson.NOTIFICATIONS, new JSONArray(notifications));
			}

			// add execute mBoxes
			final JSONArray executeMboxesNode = getExecuteMboxes(executeArray, parameters, lifecycleData);

			if (executeMboxesNode != null && executeMboxesNode.length() > 0) {
				final JSONObject executeJson = new JSONObject();
				executeJson.put(TargetJson.MBOXES, executeMboxesNode);
				defaultParamJson.put(TargetJson.EXECUTE, executeJson);
			}

			// Add property token
			if (!StringUtils.isNullOrEmpty(propertyToken)) {
				final JSONObject tokenJson = new JSONObject();
				tokenJson.put(TargetJson.TOKEN, propertyToken);
				defaultParamJson.put(TargetJson.PROPERTY, tokenJson);
			}

			// add preview parameters
			final JSONObject previewParameters = getPreviewParameters();

			if (previewParameters != null) {
				final Iterator<String> keys = previewParameters.keys();

				while (keys.hasNext()) {
					final String key = keys.next();
					defaultParamJson.put(key, previewParameters.get(key));
				}
			}

			return defaultParamJson;
		} catch (final JSONException e) {
			Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.REQUEST_GENERATION_FAILED, e);
			return null;
		}
	}

	/**
	 * Creates the {@code JSONObject} needed for the Target API requests.
	 *
	 * @param defaultJsonObject {@code JSONObject} containing the base request data attached to all Target requests.
	 * @param prefetch     the {@code Map<String, Object>} containing the request prefetch data.
	 * @param execute      the {@code Map<String, Object>} containing the request execute data.
	 * @param notifications    {@code List<Map<String, Object>>} containing the display or click notification data.
	 * @param propertyToken a {@link String} containing the configured Target {@code at_property}.
	 * @return the pre-built {@link JSONObject} for Target request.
	 */
	JSONObject getRequestPayload(final JSONObject defaultJsonObject,
								 final Map<String, Object> prefetch,
								 final Map<String, Object> execute,
								 final List<Map<String, Object>> notifications,
								 final String propertyToken) {
		try {
			// add default parameters
			final JSONObject defaultParamJson = defaultJsonObject == null ? new JSONObject() : defaultJsonObject;

			// add prefetch node
			if (prefetch != null && !prefetch.isEmpty()) {
				final JSONObject prefetchNode = new JSONObject(prefetch);
				defaultParamJson.put(TargetJson.PREFETCH, prefetchNode);
			}

			// add execute node
			if (execute != null && !execute.isEmpty()) {
				final JSONObject executeNode = new JSONObject(execute);
				defaultParamJson.put(TargetJson.EXECUTE, executeNode);
			}

			// add notifications node
			if (notifications != null && !notifications.isEmpty()) {
				final JSONArray notificationArrayNode = new JSONArray();
				for (Map<String, Object> notification : notifications) {
					final JSONObject notificationNode = new JSONObject(notification);
					notificationArrayNode.put(notificationNode);
				}
				defaultParamJson.put(TargetJson.NOTIFICATIONS, notificationArrayNode);
			}

			// Add property token
			if (!StringUtils.isNullOrEmpty(propertyToken)) {
				final JSONObject tokenJson = new JSONObject();
				tokenJson.put(TargetJson.TOKEN, propertyToken);
				defaultParamJson.put(TargetJson.PROPERTY, tokenJson);
			}

			// add preview parameters
			final JSONObject previewParameters = getPreviewParameters();
			if (previewParameters != null) {
				final Iterator<String> keys = previewParameters.keys();
				while (keys.hasNext()) {
					final String key = keys.next();
					defaultParamJson.put(key, previewParameters.get(key));
				}
			}

			return defaultParamJson;
		} catch (final JSONException e) {
			Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "getRequestPayload - (%s) (%s)", TargetErrors.REQUEST_GENERATION_FAILED, e);
			return null;
		}
	}

	/**
	 * Creates the display notification payload
	 *
	 * @param mboxName mbox name
	 * @param cachedMboxJson the cached mbox
	 * @param parameters {@link TargetParameters} object associated with the notification
	 * @param timestamp {@code long} timestamp associated with the event
	 * @param lifecycleData {@code Map<String, String>} shared state of lifecycle extension
	 * @return the {@link JSONObject} payload for notification
	 */
	JSONObject getDisplayNotificationJsonObject(final String mboxName, final JSONObject cachedMboxJson,
												final TargetParameters parameters, final long timestamp,
												final Map<String, String> lifecycleData) {
		try {
			final JSONObject notificationNode = new JSONObject();
			notificationNode.put(TargetJson.Notification.ID, UUID.randomUUID().toString());
			notificationNode.put(TargetJson.Notification.TIMESTAMP, timestamp);
			notificationNode.put(TargetJson.Metric.TYPE, TargetJson.MetricType.DISPLAY);
			setTargetParametersJson(notificationNode, parameters, lifecycleData);

			final JSONObject mboxNode = new JSONObject();
			mboxNode.put(TargetJson.Mbox.NAME, mboxName);

			if (cachedMboxJson == null) {
				return null;
			}

			final String mboxState = cachedMboxJson.optString(TargetJson.Mbox.STATE, "");

			if (!mboxState.isEmpty()) {
				mboxNode.put(TargetJson.Mbox.STATE, mboxState);
			}

			notificationNode.put(TargetJson.Notification.MBOX, mboxNode);

			//Gather display event tokens
			final JSONArray optionsArray = cachedMboxJson.optJSONArray(TargetJson.OPTIONS);

			if (optionsArray != null) {

				final JSONArray tokens = new JSONArray();

				for (int i = 0; i < optionsArray.length(); i++) {
					final JSONObject option = optionsArray.optJSONObject(i);

					if (option == null || StringUtils.isNullOrEmpty(option.optString(TargetJson.Metric.EVENT_TOKEN, ""))) {
						continue;
					}

					final String optionEventToken = option.optString(TargetJson.Metric.EVENT_TOKEN, "");
					tokens.put(optionEventToken);
				}

				if (tokens.length() == 0) {
					Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
							TargetErrors.DISPLAY_NOTIFICATION_TOKEN_EMPTY);
					return null;
				}

				notificationNode.put(TargetJson.Notification.TOKENS, tokens);
			}

			return notificationNode;
		} catch (final JSONException exception) {
			Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, TargetErrors.DISPLAY_NOTIFICATION_CREATE_FAILED, exception);
		}

		return null;
	}

	/**
	 * Creates the clicked mbox json object based on the cached mbox json node.
	 * It will contain the following params: mbox, parameters, order, product, clickToken , hit and timestamp.
	 * mbox order and product parameters of them might be missing if they are not provided from the public API.
	 *
	 * @param cachedMboxJson an {@link JSONObject} mbox node cached after prefetch/load request
	 * @param parameters {@link TargetParameters} corresponding to the clicked location
	 * @param timestamp {@code long} timestamp associated with the event
	 * @param lifecycleData {@code Map<String, String>} shared state of lifecycle extension
	 * @return mbox node for the click notification
	 */
	JSONObject getClickNotificationJsonObject(final JSONObject cachedMboxJson,
											  final TargetParameters parameters,
											  final long timestamp,
											  final Map<String, String> lifecycleData) {
		try {
			final JSONObject notificationNode = new JSONObject();
			notificationNode.put(TargetJson.Notification.ID, UUID.randomUUID().toString());
			notificationNode.put(TargetJson.Notification.TIMESTAMP, timestamp);
			notificationNode.put(TargetJson.Metric.TYPE, TargetJson.MetricType.CLICK);
			setTargetParametersJson(notificationNode, parameters, lifecycleData);

			if (cachedMboxJson == null) {
				return notificationNode;
			}

			final String mboxName = cachedMboxJson.getString(TargetJson.Mbox.NAME);

			final JSONObject mboxNode = new JSONObject();
			mboxNode.put(TargetJson.Mbox.NAME, mboxName);

			notificationNode.put(TargetJson.Notification.MBOX, mboxNode);

			final JSONArray metrics = cachedMboxJson.getJSONArray(TargetJson.METRICS);

			final JSONArray tokens = new JSONArray();

			for (int i = 0; i < metrics.length(); i++) {
				final JSONObject metric = metrics.optJSONObject(i);

				if (metric == null || !TargetJson.MetricType.CLICK.equals(metric.optString(TargetJson.Metric.TYPE, ""))
						|| metric.optString(TargetJson.Metric.EVENT_TOKEN, "").isEmpty()) {
					continue;
				}

				final String metricToken = metric.optString(TargetJson.Metric.EVENT_TOKEN, "");
				tokens.put(metricToken);
			}

			if (tokens.length() == 0) {
				throw new JSONException(TargetErrors.TOKEN_LIST_EMPTY_OR_NULL);
			}

			notificationNode.put(TargetJson.Notification.TOKENS, tokens);

			return notificationNode;
		} catch (final JSONException exception) {
			Log.warning(TargetConstants.LOG_TAG, TargetErrors.CLICK_NOTIFICATION_CREATE_FAILED,
					cachedMboxJson.toString());
		}

		return null;
	}

	/**
	 * Gets the default {@code JSONObject} attached to every Target API request.
	 *
	 * @param id {@code Map<String, Object>} containing the identifiers for the visitor.
	 * @param context {@code Map<String, Object>} specifying the context for the request.
	 * @param experienceCloud {@code Map<String, Object>} containing the Analytics and Audience Manager integration info.
	 * @param environmentId {@code long} containing the Target environmentId.
	 * @param identityData {@code Map<String, Object>} shared state of Identity extension
	 * @return {@code JSONObject} containing the default Target request payload.
	 */
	JSONObject getDefaultJsonObject(final Map<String, Object> id,
									final Map<String, Object> context,
									final Map<String, Object> experienceCloud,
									final long environmentId,
									final Map<String, Object> identityData) {
		try {
			final JSONObject defaultParamJson = new JSONObject();

			// add id Node
			final JSONObject idNode;

			if (id != null && !id.isEmpty()) {
				idNode = new JSONObject(id);
			} else {
				idNode = new JSONObject();

				if (!StringUtils.isNullOrEmpty(targetState.getTntId())) {
					idNode.put(TargetJson.ID_TNT_ID, targetState.getTntId());
				}

				if (!StringUtils.isNullOrEmpty(targetState.getThirdPartyId())) {
					idNode.put(TargetJson.ID_THIRD_PARTY_ID, targetState.getThirdPartyId());
				}

				final String visitorMarketingCloudId = DataReader.optString(identityData, TargetConstants.Identity.VISITOR_ID_MID, "");
				if (!StringUtils.isNullOrEmpty(visitorMarketingCloudId)) {
					idNode.put(TargetJson.ID_MARKETING_CLOUD_VISITOR_ID, visitorMarketingCloudId);
				}

				final List<VisitorID> visitorCustomerIds = DataReader.optTypedList(VisitorID.class, identityData, TargetConstants.Identity.VISITOR_IDS_LIST, null);
				if (visitorCustomerIds != null && !visitorCustomerIds.isEmpty()) {
					idNode.put(TargetJson.ID_CUSTOMER_IDS, getCustomerIDs(visitorCustomerIds));
				}
			}

			if (idNode.length() > 0) {
				defaultParamJson.put(TargetJson.ID, idNode);
			}

			// add context Node
			final JSONObject contextNode;

			if (context != null && !context.isEmpty()) {
				contextNode = new JSONObject(context);
			} else {
				contextNode = getContextObject();
			}

			defaultParamJson.put(TargetJson.CONTEXT_PARAMETERS, contextNode);

			// add experienceCloud Node
			final JSONObject experienceCloudNode;

			if (experienceCloud != null && !experienceCloud.isEmpty()) {
				experienceCloudNode = new JSONObject(experienceCloud);
			} else {
				final String visitorBlob = DataReader.optString(identityData, TargetConstants.Identity.VISITOR_ID_BLOB, "");
				final String visitorLocationHint = DataReader.optString(identityData, TargetConstants.Identity.VISITOR_ID_LOCATION_HINT, "");
				experienceCloudNode = getExperienceCloudObject(visitorBlob, visitorLocationHint);
			}

			defaultParamJson.put(TargetJson.EXPERIENCE_CLOUD, experienceCloudNode);

			// add environmentId
			if (environmentId != 0L) {
				defaultParamJson.put(TargetJson.ENVIRONMENT_ID, environmentId);
			}

			return defaultParamJson;

		} catch (final JSONException e) {
			Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "Failed to create base JSON object for Target request (%s)", e);
			return null;
		}
	}

	/**
	 * Creates a {@code JSONArray} containing all the batch mboxes, the mboxes will have an auto-incremental index.
	 *
	 * @param targetRequestList the {@code List<TargetRequest>} of batch request
	 * @param globalParameters global {@link TargetParameters} to be merged with per-mbox parameters
	 * @param lifecycleData {@code Map<String, String>} shared state of lifecycle extension
	 * @return the {@code JSONArray} generated from the input {@code targetRequestList}
	 */
	private JSONArray getExecuteMboxes(final List<TargetRequest> targetRequestList,
									   final TargetParameters globalParameters,
									   final Map<String, String> lifecycleData) {
		if (targetRequestList == null) {
			return null;
		}

		final JSONArray mBoxArrayNode = new JSONArray();
		int index = 0;

		for (TargetRequest currentMbox : targetRequestList) {
			try {
				mBoxArrayNode.put(createMboxJsonObject(currentMbox.getMboxName(),
						currentMbox.getTargetParameters(), index, globalParameters, lifecycleData));
				index++;
			} catch (final JSONException exception) {
				Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
						"getExecuteMboxes -Failed to create Json Node for mbox %s (%s)",
						currentMbox.getMboxName(), exception);
			}
		}

		return mBoxArrayNode;
	}

	private JSONObject getContextObject() throws JSONException {
		final JSONObject contextJson = new JSONObject();
		contextJson.put(TargetJson.Context.CHANNEL, TargetJson.Context.CHANNEL_MOBILE);
		contextJson.put(TargetJson.Context.MOBILE_PLATFORM, getPlatformContextObject());
		contextJson.put(TargetJson.Context.APPLICATION, getAppContextObject());
		contextJson.put(TargetJson.Context.SCREEN, getScreenContextObject());

		final String userAgent = deviceInfoService.getDefaultUserAgent();

		if (!StringUtils.isNullOrEmpty(userAgent)) {
			contextJson.put(TargetJson.Context.USER_AGENT, userAgent);
		}

		contextJson.put(TargetJson.Context.TIME_OFFSET, TargetUtils.getUTCTimeOffsetMinutes());

		return contextJson;
	}

	private JSONObject getScreenContextObject() throws JSONException {
		final JSONObject screenJson = new JSONObject();

		DeviceInforming.DisplayInformation displayInformation = deviceInfoService.getDisplayInformation();

		if (displayInformation != null) {
			screenJson.put(TargetJson.Context.SCREEN_WIDTH, displayInformation.getWidthPixels());
			screenJson.put(TargetJson.Context.SCREEN_HEIGHT, displayInformation.getHeightPixels());
		}

		screenJson.put(TargetJson.Context.SCREEN_COLOR_DEPTH, TargetJson.Context.COLOR_DEPTH_32);

		int orientation = deviceInfoService.getCurrentOrientation();

		if (orientation != 0) {
			screenJson.put(TargetJson.Context.SCREEN_ORIENTATION, orientation == 1
					? TargetJson.Context.ORIENTATION_PORTRAIT : TargetJson.Context.ORIENTATION_LANDSCAPE);
		}

		return screenJson;
	}

	private JSONObject getAppContextObject() throws JSONException {
		final JSONObject appJson = new JSONObject();

		final String appId = deviceInfoService.getApplicationPackageName();

		if (appId != null) {
			appJson.put(TargetJson.Context.APP_ID, appId);
		}

		final String appName = deviceInfoService.getApplicationName();

		if (appName != null) {
			appJson.put(TargetJson.Context.APP_NAME, appName);
		}

		final String appVersion = deviceInfoService.getApplicationVersion();

		if (appVersion != null) {
			appJson.put(TargetJson.Context.APP_VERSION, appVersion);
		}

		return appJson;
	}

	private JSONObject getPlatformContextObject() throws JSONException {
		final JSONObject platformJson = new JSONObject();

		platformJson.put(TargetJson.Context.PLATFORM_TYPE, deviceInfoService.getCanonicalPlatformName());

		final String deviceManufacturer = deviceInfoService.getDeviceManufacturer();
		final String deviceName = deviceInfoService.getDeviceName();

		if (deviceName != null) {
			platformJson.put(TargetJson.Context.DEVICE_NAME,
					(deviceManufacturer != null ? deviceManufacturer + " " : "") + deviceName);
		}

		final DeviceInforming.DeviceType deviceType = deviceInfoService.getDeviceType();

		if (deviceType != null && deviceType != DeviceInforming.DeviceType.UNKNOWN) {
			platformJson.put(TargetJson.Context.DEVICE_TYPE, deviceType.name().toLowerCase());
		}

		return platformJson;
	}

	private JSONObject getExperienceCloudObject(final String visitorBlob, final String visitorLocationHint) throws JSONException {
		final JSONObject analyticsJson = new JSONObject();
		analyticsJson.put(TargetJson.ANALYTICS_LOGGING, TargetJson.ANALYTICS_CLIENT_SIDE);

		final JSONObject experienceCloudJson = new JSONObject();
		experienceCloudJson.put(TargetJson.ANALYTICS_PARAMETERS, analyticsJson);

		final JSONObject audienceManagerJson = new JSONObject();
		// collect blob and location hint from visitor id service
		if (!StringUtils.isNullOrEmpty(visitorBlob)) {
			audienceManagerJson.put(TargetJson.AAMParameters.BLOB, visitorBlob);
		}

		if (!StringUtils.isNullOrEmpty(visitorLocationHint)) {
			audienceManagerJson.put(TargetJson.AAMParameters.LOCATION_HINT, visitorLocationHint);
		}

		if (audienceManagerJson.length() > 0) {
			experienceCloudJson.put(TargetJson.AAM_PARAMETERS, audienceManagerJson);
		}

		return experienceCloudJson;
	}

	/**
	 * Creates a {@code JSONArray} from the customer visitor ids.
	 *
	 * @param customerIDs the {@code List<VisitorID>} of customer visitor id
	 * @return the {@code JSONArray} generated
	 */
	private JSONArray getCustomerIDs(final List<VisitorID> customerIDs) {
		final JSONArray customerIDsArrayNode = new JSONArray();

		try {
			for (VisitorID visitorID : customerIDs) {
				JSONObject newVisitorIDNode = new JSONObject();
				newVisitorIDNode.put(TargetJson.CustomerIds.ID, visitorID.getId());
				newVisitorIDNode.put(TargetJson.CustomerIds.INTEGRATION_CODE, visitorID.getIdType());
				newVisitorIDNode.put(TargetJson.CustomerIds.AUTHENTICATION_STATE,
						getAuthenticationStateToString(visitorID.getAuthenticationState()));
				customerIDsArrayNode.put(newVisitorIDNode);
			}
		} catch (final JSONException exception) {
			Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "Failed to create json node for customer visitor ids (%s)", exception);

		}

		return customerIDsArrayNode;
	}

	/**
	 * Returns the authentication state string value for a given {@code AuthenticationState}.
	 *
	 * @param value The {@link VisitorID.AuthenticationState} value to be translated into string
	 * @return The corresponding string value.
	 */
	private String getAuthenticationStateToString(final VisitorID.AuthenticationState value) {
		switch (value) {
			case AUTHENTICATED:
				return "authenticated";

			case LOGGED_OUT:
				return "logged_out";

			default:
				return "unknown";
		}
	}

	/**
	 * Creates a {@code JSONArray} containing all the prefetch mboxes, the mboxes will be have an auto-incremental index.
	 *
	 * @param prefetchList the {@code List<TargetPrefetch>} of prefetch
	 * @param globalParameters global {@link TargetParameters} to be merged with per-mbox parameters
	 * @param lifecycleData {@code Map<String, String>} shared state of lifecycle extension
	 * @return the {@code JSONArray} generated from the input {@code prefetchList}
	 */
	private JSONArray getPrefetchMboxes(final List<TargetPrefetch> prefetchList,
										final TargetParameters globalParameters,
										final Map<String, String> lifecycleData) {
		if (prefetchList == null) {
			return null;
		}

		final JSONArray prefetchMboxesArrayNode = new JSONArray();
		int index = 0;

		for (final TargetPrefetch currentMbox : prefetchList) {
			try {
				prefetchMboxesArrayNode.put(createMboxJsonObject(currentMbox.getMboxName(),
						currentMbox.getTargetParameters(), index, globalParameters, lifecycleData));
				index++;
			} catch (final JSONException exception) {
				Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
						"getPrefetchMboxes - Failed to create json node for mbox %s (%s)",
						currentMbox.getMboxName(), exception);
			}
		}

		return prefetchMboxesArrayNode;
	}

	/**
	 * Creates a {@code JSONObject} with the provided target request. This json will contain the mbox name, mbox index,
	 * order parameters, product parameters, mbox parameters if they are present. This node will be added in
	 * the mboxes array in target network requests.
	 *
	 * @param mboxName {@link String} mbox name
	 * @param targetParameters {@link TargetParameters} per-mbox target parameters
	 * @param index        the index
	 * @param globalParameters global {@link TargetParameters} to be merged with per-mbox parameters
	 * @param lifecycleData {@code Map<String, String>} shared state of lifecycle extension
	 * @return {@link JSONObject} contains all the information provided for the mbox
	 * @throws JSONException json exception when it fails to add node to the json object
	 */
	private JSONObject createMboxJsonObject(final String mboxName,
											final TargetParameters targetParameters,
											final int index,
											final TargetParameters globalParameters,
											final Map<String, String> lifecycleData) throws JSONException {
		final JSONObject mboxNode = new JSONObject();

		mboxNode.put(TargetJson.Mbox.INDEX, index);

		mboxNode.put(TargetJson.Mbox.NAME, mboxName);

		final List<TargetParameters> targetParametersList = Arrays.asList(targetParameters, globalParameters);
		final TargetParameters parameters = TargetParameters.merge(targetParametersList);

		setTargetParametersJson(mboxNode, parameters, lifecycleData);

		return mboxNode;
	}

	private void setTargetParametersJson(final JSONObject jsonNode,
										 final TargetParameters parameters,
										 final Map<String, String> lifecycleData) throws JSONException {
		if (parameters == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
					"setTargetParametersJson - Unable to set the target parameters, TargetParamters are null");
			return;
		}

		// set mbox parameters
		final JSONObject mboxParametersNode = getMboxParameters(parameters.getParameters(), lifecycleData);

		if (mboxParametersNode.length() > 0) {
			jsonNode.put(TargetJson.PARAMETERS, mboxParametersNode);
		}

		// set profile parameters
		final JSONObject profileParamJson = new JSONObject(parameters.getProfileParameters());

		if (profileParamJson.length() > 0) {
			jsonNode.put(TargetJson.PROFILE_PARAMETERS, profileParamJson);
		}

		// set order details
		final JSONObject orderNode = getOrderParameters(parameters.getOrder());

		if (orderNode != null && orderNode.length() > 0) {
			jsonNode.put(TargetJson.ORDER, orderNode);
		}

		// set product details
		final JSONObject productNode = getProductParameters(parameters.getProduct());

		if (productNode != null && productNode.length() > 0) {
			jsonNode.put(TargetJson.PRODUCT, productNode);
		}
	}

	/**
	 * Creates the mbox parameters {@code JSONObject} with the provided data.
	 *
	 * @param mboxParameters the mbox parameters provided by the user
	 * @param lifecycleData {@code Map<String, String>} shared state of lifecycle extension
	 * @return {@link JSONObject} contains mbox data
	 */
	private JSONObject getMboxParameters(final Map<String, String> mboxParameters,
										 final Map<String, String> lifecycleData) {
		final HashMap<String, String> mboxParametersCopy = new HashMap<>(mboxParameters);

		// Remove at_property from mbox parameters which is no longer supported in v1 delivery API
		if (mboxParametersCopy.containsKey(TargetConstants.MBOX_AT_PROPERTY_KEY)) {
			if (!StringUtils.isNullOrEmpty(mboxParametersCopy.get(TargetConstants.MBOX_AT_PROPERTY_KEY))) {
				mboxParametersCopy.remove(TargetConstants.MBOX_AT_PROPERTY_KEY);
			}
		}

		final JSONObject mboxParametersJson = new JSONObject(mboxParametersCopy);

		try {
			if (lifecycleData != null && !lifecycleData.isEmpty()) {
				for (Map.Entry<String, String> param : lifecycleData.entrySet()) {
					mboxParametersJson.put(param.getKey(), param.getValue());
				}
			}

		} catch (final JSONException exception) {
			Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
					"getMboxParameters - Failed to append internal parameters to the target request json (%s)",
					exception);
		}

		return mboxParametersJson;
	}

	/**
	 * Creates the order parameters {@code JSONObject} with the provided data.
	 *
	 * @param order the {@link TargetOrder} parameters provided by the user
	 * @return {@link JSONObject} contains order data
	 */
	private JSONObject getOrderParameters(final TargetOrder order) {
		if (order == null || StringUtils.isNullOrEmpty(order.getId()) || order.getTotal() == 0
				|| TargetUtils.isNullOrEmpty(order.getPurchasedProductIds())) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "getOrderParameters - Unable to get the order parameters, TargetOrder is null");
			return null;
		}

		final JSONObject orderJson = new JSONObject();

		try {
			orderJson.put(TargetJson.Order.ID, order.getId());

			orderJson.put(TargetJson.Order.TOTAL, order.getTotal());

			final List<String> productIds = order.getPurchasedProductIds();
			final JSONArray productIdsJson = new JSONArray(productIds);
			orderJson.put(TargetJson.Order.PURCHASED_PRODUCT_IDS, productIdsJson);

			return orderJson;
		} catch (final JSONException ex) {
			Log.warning(TargetConstants.LOG_TAG, CLASS_NAME, "getOrderParameters - Failed to create target order parameters (%s)", ex);
		}

		return null;
	}

	/**
	 * Creates the product parameters {@code JSONObject} with the provided data.
	 *
	 * @param product {@link TargetProduct} provided by the user
	 * @return {@link JSONObject} contains product data
	 */
	private JSONObject getProductParameters(final TargetProduct product) {
		if (product == null || StringUtils.isNullOrEmpty(product.getId()) || StringUtils.isNullOrEmpty(product.getCategoryId())) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
					"getProductParameters - Unable to get the product parameters, TargetProduct is null");
			return null;
		}

		final JSONObject productNode = new JSONObject();

		try {
			productNode.put(TargetJson.Product.ID, product.getId());
			productNode.put(TargetJson.Product.CATEGORY_ID, product.getCategoryId());
		} catch (final JSONException exception) {
			Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
					"getProductParameters - Failed to append product parameters to the target request json (%s)",
					exception);
			return null;
		}

		return productNode;
	}

	/**
	 * Returns target preview parameters need to be appended to the current target {@code JSONObject}.
	 *
	 * @return target preview parameters in {@link JSONObject} format
	 */
	private JSONObject getPreviewParameters() {

		// Bail out early if previewManager instance is null
		if (targetPreviewManager == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
					"getPreviewParameters - Unable to get the preview parameters, target preview manager is null");
			return null;
		}

		if (targetPreviewManager.getPreviewToken() != null && targetPreviewManager.getPreviewParameters() != null) {
			try {
				return new JSONObject(targetPreviewManager.getPreviewParameters());
			} catch (final JSONException e) {
				Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
						"getPreviewParameters - Could not compile the target preview params with the Target request (%s)", e.getMessage());
			}
		}

		return null;
	}
}

