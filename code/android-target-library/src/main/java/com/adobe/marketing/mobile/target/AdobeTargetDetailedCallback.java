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

import com.adobe.marketing.mobile.AdobeError;

import java.util.Map;

/**
 * Callback Interface to pass the mbox content and other payload info of mbox.
 */
public interface AdobeTargetDetailedCallback {

	/**
	 * Callback function to pass the mbox content and other mbox payload values.
	 *
	 * @param content {@code String} mox content
	 * @param data A {@code Map<String, Object>} of mbox payload values.
	 *                {@code data} will be null if neither response tokens nor analytics payload is available.
	 */
	void call(final String content, final Map<String, Object> data);

	/**
	 * Callback function for notifying about the internal error in getting mbox details.
	 *
	 * @param error {@link AdobeError} represents the internal error occurred.
	 */
	void fail(final AdobeError error);
}
