/* ************************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2019 Adobe
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Adobe and its suppliers, if any. The intellectual
 * and technical concepts contained herein are proprietary to Adobe
 * and its suppliers and are protected by all applicable intellectual
 * property laws, including trade secret and copyright laws.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe.
 **************************************************************************/

package com.adobe.marketing.mobile.target;

final class TargetErrors {
	static final String NO_CLIENT_CODE = "Missing client code";
	static final String CONFIG_NULL = "Missing shared configuration state";
	static final String OPTED_OUT = "Privacy status is opted out";
	static final String NO_PREFETCH_REQUESTS = "Empty or null prefetch requests list";
	static final String NO_CONNECTION = "Unable to open connection";
	static final String RESPONSE_PARSER_INIT_FAILED = "Target response parser initialization failed";
	static final String NULL_RESPONSE_JSON = "Null response Json";
	static final String ERROR_RESPONSE = "Errors returned in Target response: ";
	static final String NOTIFICATION_ERROR_TAG = "Notification";
	static final String NO_PREFETCH_MBOXES = "No prefetch mbox content in Target response";
	static final String NO_PREFETCH_VIEWS = "No prefetch view content in Target response";
	static final String MBOX_NAME_NULL_OR_EMPTY = "MboxName is either null or empty";
	static final String MBOX_NAMES_NULL_OR_EMPTY = "MboxNames List is either null or empty";
	static final String NO_TARGET_REQUESTS = "No valid Target Request found.";
	static final String PARAMS_SERIALIZATION_FAILED = "TargetParameters serialization failed";
	static final String PLATFORM_SERVICES_UNAVAILABLE =
		"Unable to send target request, Platform services are not available";
	static final String JSON_UTILITY_UNAVAILABLE = "Unable to send target request, Json utility service is not available";
	static final String NETWORK_SERVICE_UNAVAILABLE = "Unable to send target request, Network service is not available";
	static final String REQUEST_BUILDER_INIT_FAILED = "Couldn't initialize the target request builder for this request";
	static final String REQUEST_BUILDER_INIT_FAILED_NOTIFICATIONS =
		"Couldn't initialize the target request builder to extract the notifications";
	static final String REQUEST_GENERATION_FAILED = "Failed to generate the Target request payload (%s)";
	static final String UNEXPECTED_VISITORIDS_LIST = "The serialized visitorIdsList received as parameter is not a list %s";
	static final String NO_CACHED_MBOX_FOUND = "No cached mbox found for %s";
	static final String VIEW_NOTIFICATION_INVALID = "Some fields are missing in view notification";
	static final String VIEW_NOTIFICATION_SEND_FAILED = "Unable to send view notifications: %s";
	static final String VIEW_NOTIFICATION_ERROR = "Failed to parse view notification objects %s";
	static final String TOKEN_LIST_EMPTY_OR_NULL = "Tokens list is null or empty in the view notification object";
	static final String VIEW_PARAMETERS_NOT_PRESENT = "View parameters are not present in the view notification object";
	static final String NO_VALID_VIEW_NOTIFICATIONS = "No valid view notifications found";
	static final String DISPLAY_NOTIFICATION_SEND_FAILED = "Unable to send display notification: %s";
	static final String DISPLAY_NOTIFICATION_CREATE_FAILED = "Failed to create display notification Json(%s)";
	static final String DISPLAY_NOTIFICATION_TOKEN_EMPTY =
		"Unable to create display notification as token is null or empty";
	static final String DISPLAY_NOTIFICATION_NOT_SENT = "No display notifications are available to send";
	static final String DISPLAY_NOTIFICATION_NULL_FOR_MBOX = "No display notifications are available to send for mbox %s";
	static final String CLICK_NOTIFICATION_SEND_FAILED = "Unable to send click notification: ";
	static final String CLICK_NOTIFICATION_NOT_SENT = "No click notifications are available to send";
	static final String NO_CLICK_METRICS = "No click metrics set on mbox: %s";
	static final String NO_CLICK_METRIC_FOUND = "No click metric found on mbox: %s";
	static final String CLICK_NOTIFICATION_CREATE_FAILED = "Failed to create click notification Json(%s)";
	static final String VIEW_NOTIFICATION_NOT_SENT = "No view notifications are available to send";
	static final String NO_PREFETCH_IN_PREVIEW = "Target prefetch can't be used while in preview mode";
	static final String PREVIEW_MANAGER_INIT_FAILED = "Couldn't initialize the Target preview manager";
	static final String TARGET_NOT_ENABLED_FOR_PREVIEW = "Target is not enabled, cannot enter in preview mode: %s";
	static final String TARGET_PREVIEW_DISABLED =
		"Target Preview is disabled, please change the configuration and try again";
	static final String TARGET_THIRD_PARTY_ID_NOT_PERSISTED = "Failed to persist thirdPartyId, %s";
	static final String TARGET_TNT_ID_NOT_PERSISTED = "Failed to persist tntID, %s";
	static final String TARGET_SESSION_ID_NOT_PERSISTED = "Failed to persist session id, %s";

	private TargetErrors() {}
}
