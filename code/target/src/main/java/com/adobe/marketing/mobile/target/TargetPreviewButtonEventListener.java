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
import com.adobe.marketing.mobile.services.ui.FloatingButton;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.PresentationError;
import com.adobe.marketing.mobile.services.ui.floatingbutton.FloatingButtonEventListener;

class TargetPreviewButtonEventListener implements FloatingButtonEventListener {

	private static final String CLASS_NAME = "TargetPreviewButtonEventListener";
	private final TargetPreviewManager previewManager;

	TargetPreviewButtonEventListener(final TargetPreviewManager targetPreviewManager) {
		this.previewManager = targetPreviewManager;
	}

	@Override
	public void onTapDetected(@NonNull Presentable<FloatingButton> presentable) {
		Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "Target preview button was clicked");
		previewManager.fetchWebView();
	}

	@Override
	public void onPanDetected(@NonNull Presentable<FloatingButton> presentable) {

	}

	@Override
	public void onDismiss(@NonNull Presentable<FloatingButton> presentable) {
		Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "Target preview button was dismissed");
	}

	@Override
	public void onError(@NonNull Presentable<FloatingButton> presentable, @NonNull PresentationError presentationError) {
		Log.debug(TargetConstants.LOG_TAG,  CLASS_NAME, "onError - Failed to display Target preview button");
	}

	@Override
	public void onHide(@NonNull Presentable<FloatingButton> presentable) {

	}

	@Override
	public void onShow(@NonNull Presentable<FloatingButton> presentable) {
		Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "Target preview button was shown");
	}
}
