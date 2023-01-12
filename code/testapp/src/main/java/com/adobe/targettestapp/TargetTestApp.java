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

package com.adobe.targettestapp;
import com.adobe.marketing.mobile.Lifecycle;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Target;
import com.adobe.marketing.mobile.Identity;
import com.adobe.marketing.mobile.LoggingMode;

import android.app.Application;
import android.content.Context;

import java.util.Arrays;

public class TargetTestApp extends Application {

	private static Application application;

	@Override
	public void onCreate() {
		super.onCreate();
		application = this;
		MobileCore.setApplication(this);
		MobileCore.setLogLevel(LoggingMode.VERBOSE);
		MobileCore.registerExtensions(Arrays.asList(Target.EXTENSION, Identity.EXTENSION, Lifecycle.EXTENSION), null);
		MobileCore.configureWithAppID("");
	}

	public static Application getApplication() {
		return application;
	}

	public static Context getContext() {
		return getApplication().getApplicationContext();
	}
}
