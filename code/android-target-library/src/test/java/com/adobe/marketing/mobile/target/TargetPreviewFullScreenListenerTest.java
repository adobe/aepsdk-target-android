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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.adobe.marketing.mobile.services.ui.FullscreenMessage;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TargetPreviewFullScreenListenerTest {

	TargetPreviewFullscreenDelegate fullscreenListener;

    @Mock
	TargetPreviewManager mockTargetPreviewManager;

	@Mock
	FullscreenMessage fullScreenMessage;

	@Before
	public void beforeEach() {
		fullscreenListener = new TargetPreviewFullscreenDelegate(mockTargetPreviewManager);
	}

	// ===================================
	// Test onShow
	// ===================================
	@Test
	public void test_onShow() {
		// test no_op
		fullscreenListener.onShow(fullScreenMessage);
	}

	// ===================================
	// Test onDismiss
	// ===================================
	@Test
	public void test_onDismiss() {
		// test no op
		fullscreenListener.onDismiss(fullScreenMessage);
	}

	// ===================================
	// Test overrideUrlLoad
	// ===================================
	@Test
	public void test_overrideUrlLoad() {
		// test
		fullscreenListener.overrideUrlLoad(fullScreenMessage, "testdeeplink");

		// verify
		verify(mockTargetPreviewManager).previewConfirmedWithUrl(eq(fullScreenMessage), eq("testdeeplink"));
	}
}