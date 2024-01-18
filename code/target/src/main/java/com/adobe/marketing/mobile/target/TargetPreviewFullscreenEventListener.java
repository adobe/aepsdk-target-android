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

import androidx.annotation.NonNull;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ui.InAppMessage;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.PresentationError;
import com.adobe.marketing.mobile.services.ui.message.InAppMessageEventListener;

class TargetPreviewFullscreenEventListener implements InAppMessageEventListener {
	private static final String CLASS_NAME = "TargetPreviewFullscreenDelegate";
	final private TargetPreviewManager targetPreviewManager;

	TargetPreviewFullscreenEventListener(final TargetPreviewManager previewManager) {
		this.targetPreviewManager = previewManager;
	}

	@Override
	public void onDismiss(@NonNull final Presentable<InAppMessage> presentable) {
		Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "Target preview selection screen was dismissed");
	}

	@Override
	public void onError(@NonNull final Presentable<InAppMessage> presentable, @NonNull final PresentationError presentationError) {
		Log.debug(TargetConstants.LOG_TAG,  CLASS_NAME, "onError - Failed to display Target preview selection screen.");
	}

	@Override
	public void onHide(@NonNull final Presentable<InAppMessage> presentable) {}

	@Override
	public void onShow(@NonNull final Presentable<InAppMessage> presentable) {
		Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "Target preview selection screen was displayed");
	}

	@Override
	public void onBackPressed(@NonNull final Presentable<InAppMessage> presentable) {}

	@Override
	public boolean onUrlLoading(@NonNull final Presentable<InAppMessage> presentable, @NonNull final String url) {
		Log.debug(TargetConstants.LOG_TAG,  CLASS_NAME, String.format("Target preview override url received: %s", url));
		targetPreviewManager.previewConfirmedWithUrl(presentable, url);
		return true;
	}
}
