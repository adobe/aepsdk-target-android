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
