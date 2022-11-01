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

import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Target;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class TargetFunctionalTests {

    @Before
    public void setup() throws Exception {
        MobileCore.setApplication(ApplicationProvider.getApplicationContext());
        MobileCore.setLogLevel(LoggingMode.VERBOSE);
        final CountDownLatch latch = new CountDownLatch(1);

        List<Class<? extends Extension>> extensions = new ArrayList<>();
        extensions.add(Target.EXTENSION);
        // extensions.add(Identity.EXTENSION);

        MobileCore.registerExtensions(extensions, o -> latch.countDown());
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    }

    @After
    public void tearDown() {
    }

    //1
    @Test
    public void testExtensionVersion() {
        Assert.assertEquals(TargetTestConstants.EXTENSION_VERSION, Target.extensionVersion());
    }
}