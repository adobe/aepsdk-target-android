/* ***********************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2018 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/

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