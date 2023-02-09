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

package com.adobe.marketing.mobile.target;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.FullscreenMessageDelegate;

class TargetPreviewFullscreenDelegate implements FullscreenMessageDelegate {
	private static final String CLASS_NAME = "TargetPreviewFullscreenDelegate";
	final private TargetPreviewManager targetPreviewManager;

	TargetPreviewFullscreenDelegate(final TargetPreviewManager previewManager) {
		this.targetPreviewManager = previewManager;
	}

	/**
	 * Invoked when a {@code UIFullScreenMessage} is displayed.
	 *
	 * @param message the {@link FullscreenMessage} being displayed
	 */
	@Override
	public void onShow(final FullscreenMessage message) {
		Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "Target preview selection screen was displayed");
	}

	@Override
	public void onDismiss(final FullscreenMessage message) {
		Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "Target preview selection screen was dismissed");
	}

	@Override
	public boolean overrideUrlLoad(final FullscreenMessage message, final String url) {
		Log.debug(TargetConstants.LOG_TAG,  CLASS_NAME, String.format("Target preview override url received: %s", url));
		targetPreviewManager.previewConfirmedWithUrl(message, url);
		return true;
	}

	@Override
	public void onShowFailure() {
		Log.debug(TargetConstants.LOG_TAG,  CLASS_NAME, "onShowFailure - Failed to display Target preview selection screen.");
	}
}
