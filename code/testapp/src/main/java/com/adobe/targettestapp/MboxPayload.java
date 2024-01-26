/*
 Copyright 2023 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.targettestapp;

import java.util.Collections;
import java.util.Map;

class MboxPayload {

	private String content = "";
	private final Map<String, String> a4tParams;
	private final Map<String, String> clickMetricA4tParams;
	private final Map<String, Object> responseTokens;

	public MboxPayload(final String content, final Map<String, Object> payload) {
		if (content != null) {
			this.content = content;
		}

		if (payload != null && payload.size() > 0) {
			this.a4tParams = payload.containsKey("analytics.payload") ? (Map<String, String>) payload.get("analytics.payload") :
							 null;
			this.clickMetricA4tParams = payload.containsKey("clickmetric.analytics.payload") ? (Map<String, String>)
										payload.get("clickmetric.analytics.payload") :
										null;
			this.responseTokens = payload.containsKey("responseTokens") ? (Map<String, Object>) payload.get("responseTokens") :
								  null;
		} else {
			a4tParams = Collections.emptyMap();
			responseTokens = Collections.emptyMap();
			clickMetricA4tParams = Collections.emptyMap();
		}
	}

	public String getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "MboxPayload{" +
			   "content='" + content + '\'' +
			   ", a4tParams=" + a4tParams +
			   ", clickMetricA4TParams=" + clickMetricA4tParams +
			   ", responseTokens=" + responseTokens +
			   '}';
	}
}
