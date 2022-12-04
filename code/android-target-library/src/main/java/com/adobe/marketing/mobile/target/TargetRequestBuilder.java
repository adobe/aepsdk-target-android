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
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class is used to create the json body for the target requests.
 */
class TargetRequestBuilder {
	private static final String CLASS_NAME = "TargetRequestBuilder";
	private final DeviceInforming deviceInfoService;
	private final TargetPreviewManager targetPreviewManager;
	private final TargetState targetState;

	private String visitorMarketingCloudId = "";
	private String visitorBlob = "";
	private String visitorLocationHint = "";
	private List<VisitorID> visitorCustomerIds = null;
	private Map<String, String> lifecycleData;

	private long environmentId;
	private String tntId;
	private String thirdPartyId;

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

		environmentId = targetState.getEnvironmentId();
		tntId = targetState.getTntId();
		thirdPartyId = targetState.getThirdPartyId();
	}

	/**
	 * Clean all the local variables.
	 */
	void clean() {
		this.environmentId = 0;
		this.visitorMarketingCloudId = null;
		this.visitorBlob = null;
		this.visitorLocationHint = null;
		this.visitorCustomerIds = null;
		this.lifecycleData = null;
		this.thirdPartyId = null;
		this.tntId = null;
	}

	/**
	 * Sets the parameters provided through shared state by the identity extension.
	 *
	 * @param identityData {@code Map<String, Object>} representing identity shared state
	 */
	void setIdentityData(final Map<String, Object> identityData) {
		if (identityData != null) {
			this.visitorMarketingCloudId = DataReader.optString(identityData, TargetConstants.Identity.VISITOR_ID_MID, "");
			this.visitorBlob = DataReader.optString(identityData, TargetConstants.Identity.VISITOR_ID_BLOB, "");
			this.visitorLocationHint = DataReader.optString(identityData, TargetConstants.Identity.VISITOR_ID_LOCATION_HINT, "");
			if (identityData.containsKey(TargetConstants.Identity.VISITOR_IDS_LIST)) {
				try {
					this.visitorCustomerIds = DataReader.getTypedList(VisitorID.class, identityData, TargetConstants.Identity.VISITOR_IDS_LIST);
				} catch (final DataReaderException exception) {
					Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "handleRawRequest - (%s) (%s)", TargetErrors.UNEXPECTED_VISITORIDS_LIST, exception);
				}
			}
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
				Iterator<String> keys = previewParameters.keys();
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
	 * Gets the default {@code JSONObject} attached to every Target API request.
	 *
	 * @param id {@code Map<String, Object>} containing the identifiers for the visitor.
	 * @param context {@code Map<String, Object>} specifying the context for the request.
	 * @param experienceCloud {@code Map<String, Object>} containing the Analytics and Audience Manager integration info.
	 * @param environmentId {@code long} containing the Target environmentId.
	 * @return {@code JSONObject} containing the default Target request payload.
	 */
	JSONObject getDefaultJsonObject(final Map<String, Object> id,
									final Map<String, Object> context,
									final Map<String, Object> experienceCloud,
									final long environmentId) {
		try {
			final JSONObject defaultParamJson = new JSONObject();

			// add id Node
			JSONObject idNode;

			if (id != null && !id.isEmpty()) {
				idNode = new JSONObject(id);
			} else {
				idNode = new JSONObject();

				if (!StringUtils.isNullOrEmpty(tntId)) {
					idNode.put(TargetJson.ID_TNT_ID, tntId);
				}

				if (!StringUtils.isNullOrEmpty(thirdPartyId)) {
					idNode.put(TargetJson.ID_THIRD_PARTY_ID, thirdPartyId);
				}

				if (!StringUtils.isNullOrEmpty(visitorMarketingCloudId)) {
					idNode.put(TargetJson.ID_MARKETING_CLOUD_VISITOR_ID, visitorMarketingCloudId);
				}

				if (visitorCustomerIds != null && !visitorCustomerIds.isEmpty()) {
					idNode.put(TargetJson.ID_CUSTOMER_IDS, getCustomerIDs(visitorCustomerIds));
				}
			}

			if (idNode.length() > 0) {
				defaultParamJson.put(TargetJson.ID, idNode);
			}

			// add context Node
			JSONObject contextNode;

			if (context != null && !context.isEmpty()) {
				contextNode = new JSONObject(context);
			} else {
				contextNode = getContextObject();
			}

			defaultParamJson.put(TargetJson.CONTEXT_PARAMETERS, contextNode);

			// add experienceCloud Node
			JSONObject experienceCloudNode;

			if (experienceCloud != null && !experienceCloud.isEmpty()) {
				experienceCloudNode = new JSONObject(experienceCloud);
			} else {
				experienceCloudNode = getExperienceCloudObject();
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

	private JSONObject getExperienceCloudObject() throws JSONException {
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
		JSONArray customerIDsArrayNode = new JSONArray();

		try {
			for (VisitorID visitorID : customerIDs) {
				JSONObject newVisitorIDNode = new JSONObject();
				newVisitorIDNode.put(TargetJson.CustomerIds.ID, visitorID.getId());
				newVisitorIDNode.put(TargetJson.CustomerIds.INTEGRATION_CODE, visitorID.getIdType());
				newVisitorIDNode.put(TargetJson.CustomerIds.AUTHENTICATION_STATE,
									 getAuthenticationStateToString(visitorID.getAuthenticationState()));
				customerIDsArrayNode.put(newVisitorIDNode);
			}
		} catch (JSONException exception) {
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
			} catch (JSONException e) {
				Log.warning(TargetConstants.LOG_TAG, CLASS_NAME,
							"getPreviewParameters - Could not compile the target preview params with the Target request (%s)", e.getMessage());
			}
		}

		return null;
	}
}
