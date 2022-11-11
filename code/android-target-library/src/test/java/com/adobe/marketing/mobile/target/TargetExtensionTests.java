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

import android.app.Application;
import android.content.Context;

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;

import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings("unchecked")
public class TargetExtensionTests {
    private TargetExtension extension;

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;

    @Mock
    Application mockApplication;

    @Mock
    Context mockContext;

    @Before
    public void setup() {
        extension = new TargetExtension(mockExtensionApi);
    }

    @After
    public void teardown() {
    }

    @Test
    public void test_getName() {
        // test
        final String extensionName = extension.getName();
        assertEquals("getName should return the correct extension name.", "com.adobe.target", extensionName);
    }

    @Test
    public void test_getFriendlyName() {
        // test
        final String extensionName = extension.getFriendlyName();
        assertEquals("getFriendlyName should return the correct extension friendly name.", "Target", extensionName);
    }

    @Test
    public void test_getVersion() {
        // test
        final String extensionVersion = extension.getVersion();
        assertEquals("getVersion should return the correct extension version.", "2.0.0", extensionVersion);
    }
}

