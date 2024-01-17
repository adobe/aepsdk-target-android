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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ui.FloatingButton;
import com.adobe.marketing.mobile.services.ui.Presentable;
import com.adobe.marketing.mobile.services.ui.PresentationError;


@RunWith(MockitoJUnitRunner.Silent.class)
public class TargetPreviewButtonEventListenerTest {

    @Mock
    private TargetPreviewManager mockTargetPreviewManager;

    @Mock
    private Presentable<FloatingButton> mockFloatingButton;

	@Mock
	private PresentationError mockPresentationError;

	TargetPreviewButtonEventListener previewButtonListener;


	@Before()
	public void beforeEach() {
		previewButtonListener = new TargetPreviewButtonEventListener(mockTargetPreviewManager);
	}

	// ===================================
	// Test OnTapDetected
	// ===================================
	@Test
	public void test_OnTapDetected() {
		// test
		previewButtonListener.onTapDetected(mockFloatingButton);

		// verify
		verify(mockTargetPreviewManager, times(1)).fetchWebView();
	}

	// ===================================
	// Test onPanDetected
	// ===================================
	@Test
	public void test_OnPanDetected() {
		// test
		previewButtonListener.onPanDetected(mockFloatingButton);

		// verify
		// nothing to verify for now
	}

	// ===================================
	// Test onDismiss
	// ===================================
	@Test
	public void test_OnDismiss() {
		try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
			// test
			previewButtonListener.onDismiss(mockFloatingButton);

			// verify
			logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()));
		}
	}

	// ===================================
	// Test onError
	// ===================================
	@Test
	public void test_OnError() {
		try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
			// test
			previewButtonListener.onError(mockFloatingButton, mockPresentationError);

			// verify
			logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()));
		}
	}

	// ===================================
	// Test onShow
	// ===================================
	@Test
	public void test_OnShow() {
		try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
			// test
			previewButtonListener.onShow(mockFloatingButton);

			// verify
			logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()));
		}
	}
}
