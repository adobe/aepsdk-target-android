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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.Silent.class)
public class TargetPreviewButtonListenerTest {

    @Mock
    private TargetPreviewManager mockTargetPreviewManager;

	TargetPreviewButtonListener previewButtonListener;


	@Before()
	public void beforeEach() {
		previewButtonListener = new TargetPreviewButtonListener(mockTargetPreviewManager);
	}

	// ===================================
	// Test OnTapDetected
	// ===================================
	@Test
	public void test_OnTapDetected() {
		// test
		previewButtonListener.onTapDetected();

		// verify
		verify(mockTargetPreviewManager, times(1)).fetchWebView();
	}

	// ===================================
	// Test onPanDetected
	// ===================================
	@Test
	public void test_OnPanDetected() {
		// test
		previewButtonListener.onPanDetected();

		// verify
		// nothing to verify for now
	}



}
