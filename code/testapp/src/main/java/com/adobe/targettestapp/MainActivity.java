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

import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.target.AdobeTargetDetailedCallback;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Target;
import com.adobe.marketing.mobile.target.TargetOrder;
import com.adobe.marketing.mobile.target.TargetParameters;
import com.adobe.marketing.mobile.target.TargetPrefetch;
import com.adobe.marketing.mobile.target.TargetProduct;
import com.adobe.marketing.mobile.target.TargetRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
	private static Boolean contentPrefetched = false;
	private static final List<String> mboxList = new ArrayList<>();
	private static String tntId = "";
	private static String sessionId = "";
	private static String thirdPartyId = "";
	private static final List<Map<String, String>> notificationTokens = new ArrayList<>();

	private final String MBOX1 = "aep-loc-1", MBOX2 = "aep-loc-2", MBOX3 = "aep-loc-x";
	private final String DEFAULT1 = "DefaultValue1", DEFAULT2 = "DefaultValue2", DEFAULT3 = "DefaultValueX";

	private static final Map<String, String> mboxParameters = new HashMap<String, String>() {
		{
			put("mbox_parameter_key", "mbox_parameter_value");
			put("at_property", "mbox_parameter_property"); // this will be overridden by the property set via updateConfiguration
		}
	};

	private static final Map<String, String> profileParameters = new HashMap<String, String>() {
		{
			put("profile_parameter_key", "profile_parameter_value");
		}
	};

	private static final Map<String, Object> orderParameters;
	private static final Map<String, String> productParameters;

	static {
		orderParameters = new HashMap<>();
		orderParameters.put("id", "SomeOrderID");
		orderParameters.put("total", 4445.12);
	}

	static {
		productParameters = new HashMap<>();
		productParameters.put("id", "764334");
		productParameters.put("categoryId", "Online");
	}

	private static final TargetProduct targetProduct = new TargetProduct(productParameters.get("id"),
			productParameters.get("categoryId"));
	private static final TargetOrder targetOrder = new TargetOrder((String) orderParameters.get("id"),
			(double) orderParameters.get("total"),
			(List<String>) orderParameters.get("purchasedProductIds"));
	private static final TargetParameters targetParameters = new TargetParameters.Builder().product(targetProduct).order(
		targetOrder).parameters(mboxParameters).profileParameters(profileParameters).build();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("Target extension version: " + Target.extensionVersion());
		setContentView(R.layout.activity_main);
		final TextView prefetchStatusTextView = findViewById(R.id.prefetchStatus);

		// first run, initialize the activity
		if (savedInstanceState == null) {
			runOnUiThread(() -> prefetchStatusTextView.setText("Prefetch status"));
		} else {
			runOnUiThread(() -> prefetchStatusTextView.setText(savedInstanceState.getString("prefetchText")));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle savedState) {
		TextView prefetchStatusTextView = findViewById(R.id.prefetchStatus);
		savedState.putString("prefetchText", prefetchStatusTextView.getText().toString());
		super.onSaveInstanceState(savedState);
	}

	@Override
	public void onResume() {
		super.onResume();
		MobileCore.lifecycleStart(null);
	}

	@Override
	public void onPause() {
		super.onPause();
		MobileCore.lifecyclePause();
	}

	public void retrieveLocation(View view) throws Exception {
		final CountDownLatch latch = new CountDownLatch(3);
		final String[] callbackData = new String[3];

		AdobeCallback<String> adobeCallback = (AdobeCallback<String>) data -> {
			callbackData[0] = data;
			latch.countDown();
		};

		AdobeCallback<String> adobeCallback2 = (AdobeCallback<String>) data -> {
			callbackData[1] = data;
			latch.countDown();
		};

		AdobeCallback<String> adobeCallback3 = (AdobeCallback<String>) data -> {
			callbackData[2] = data;
			latch.countDown();
		};

		// load the requests using retrieve location content
		TargetRequest targetRequest = new TargetRequest(MBOX1, null, DEFAULT1, adobeCallback);

		TargetRequest targetRequest2 = new TargetRequest(MBOX2, null, DEFAULT2, adobeCallback2);

		TargetRequest targetRequest3 = new TargetRequest(MBOX3, targetParameters,
				DEFAULT3, adobeCallback3);

		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest);
		targetRequestList.add(targetRequest2);
		targetRequestList.add(targetRequest3);
		Target.retrieveLocationContent(targetRequestList, targetParameters);
		latch.await(5, TimeUnit.SECONDS);


		if (callbackData[0] != null) {
			if (callbackData[0].equals(DEFAULT1)) {
				System.out.println("Retrieve location content default content received: " + callbackData[0] + " for mbox: " + MBOX1);
			} else {
				System.out.println("Retrieve location content received: " + callbackData[0] + " for mbox: " + MBOX1);
				mboxList.add(MBOX1);
			}
		}

		if (callbackData[1] != null) {
			if (callbackData[1].equals(DEFAULT2)) {
				System.out.println("Retrieve location content default content received: " + callbackData[1] + " for mbox: " + MBOX2);
			} else {
				System.out.println("Retrieve location content default content received: " + callbackData[1] + " for mbox: " + MBOX2);
				mboxList.add(MBOX2);
			}
		}

		if (callbackData[2] != null) {
			if (callbackData[2].equals(DEFAULT3)) {
				System.out.println("Retrieve location content default content received: " + callbackData[2] + " for mbox: " + MBOX3);
			} else {
				System.out.println("testingClickMbox retrieve location content successful, content is: " + callbackData[2]);
				mboxList.add(MBOX3);
			}
		}
	}

	public void retrieveLocationWithPayloadCallback(View view) throws Exception {
		final CountDownLatch latch = new CountDownLatch(3);
		final String[] contents = new String[3];
		final Map<String, Object>[] callbackData = new Map[3];

		AdobeTargetDetailedCallback adobeTargetDetailedCallback1 = new AdobeTargetDetailedCallback() {
			@Override
			public void call(String content, Map<String, Object> data) {
				callbackData[0] = data;
				contents[0] = content;
				latch.countDown();
			}

			@Override
			public void fail(AdobeError error) {
				System.out.println(String.format("Error in fetching mbox content and data for (%s)", MBOX1));
			}
		};

		AdobeTargetDetailedCallback adobeTargetDetailedCallback2 = new AdobeTargetDetailedCallback() {
			@Override
			public void call(String content, Map<String, Object> data) {
				callbackData[1] = data;
				contents[1] = content;
				latch.countDown();
			}

			@Override
			public void fail(AdobeError error) {
				System.out.println(String.format("Error in fetching mbox content and data for (%s)", MBOX3));
			}
		};

		AdobeTargetDetailedCallback adobeTargetDetailedCallback3 = new AdobeTargetDetailedCallback() {
			@Override
			public void call(String content, Map<String, Object> data) {
				callbackData[2] = data;
				contents[2] = content;
				latch.countDown();
			}

			@Override
			public void fail(AdobeError error) {
				System.out.println(String.format("Error in fetching mbox content and data for (%s)", MBOX3));
			}
		};

		TargetRequest targetRequest1 = new TargetRequest(MBOX1, null, DEFAULT1, adobeTargetDetailedCallback1);

		TargetRequest targetRequest2 = new TargetRequest(MBOX2, null, DEFAULT2, adobeTargetDetailedCallback2);

		TargetRequest targetRequest3 = new TargetRequest(MBOX3, targetParameters, DEFAULT3, adobeTargetDetailedCallback3);

		List<TargetRequest> targetRequestList = new ArrayList<>();
		targetRequestList.add(targetRequest1);
		targetRequestList.add(targetRequest2);
		targetRequestList.add(targetRequest3);
		Target.retrieveLocationContent(targetRequestList, targetParameters);
		latch.await(5, TimeUnit.SECONDS);

		if (contents[0] != null) {
			MboxPayload mboxPayload = new MboxPayload(contents[0], callbackData[0]);

			if (DEFAULT1.equals(mboxPayload.getContent())) {
				System.out.println("Retrieve location content default content received: " + mboxPayload.getContent() + " for mbox: " +
						MBOX1);
			} else {
				mboxList.add(MBOX1);
				System.out.println(String.format("Mbox payload received for Mbox (%s) is (%s)", MBOX1, mboxPayload.toString()));
			}
		}

		if (contents[1] != null) {
			MboxPayload mboxPayload = new MboxPayload(contents[0], callbackData[1]);

			if (DEFAULT2.equals(mboxPayload.getContent())) {
				System.out.println("Retrieve location content default content received: " + mboxPayload.getContent() + " for mbox: " +
						MBOX2);
			} else {
				mboxList.add(MBOX2);
				System.out.println(String.format("Mbox payload received for Mbox (%s) is (%s)", MBOX2, mboxPayload.toString()));
			}
		}

		if (contents[2] != null) {
			MboxPayload mboxPayload = new MboxPayload(contents[2], callbackData[2]);

			if (DEFAULT3.equals(mboxPayload.getContent())) {
				System.out.println("Retrieve location content default content received: " + mboxPayload.getContent() + " for mbox: " +
						MBOX3);
			} else {
				System.out.println("testingClickMbox retrieve location content successful, content is: " + mboxPayload.getContent());
				mboxList.add(MBOX3);
				System.out.println(String.format("Mbox payload received for Mbox (%s) is (%s)", MBOX3, mboxPayload.toString()));
			}
		}
	}

	public void locationsClicked(View view) {
		if (mboxList.isEmpty()) {
			System.out.println("Mbox list is empty, cannot send location clicked request as no mboxes have been loaded yet.");
			return;
		}

		// click metrics are enabled for MBOX3 / testingClickMbox, Activity Name: Swarna Click Test - testingProperty, Property: testingProperty
		if (mboxList.contains(MBOX3)) {
			Target.locationClicked(MBOX3, targetParameters);
		}
	}

	public void locationsDisplayed(View view) {
		if (mboxList.isEmpty()) {
			System.out.println("Mbox list is empty, cannot send location displayed request as no mboxes have been loaded yet.");
			return;
		}

		Target.locationsDisplayed(mboxList, targetParameters);
	}

	public void prefetchRequests(View view) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final String prefetchStatus[] = new String[1];
		TargetPrefetch prefetch = new TargetPrefetch(MBOX1, null);
		TargetPrefetch prefetch2 = new TargetPrefetch(MBOX2, targetParameters);

		List<TargetPrefetch> prefetchList = new ArrayList<>();
		prefetchList.add(prefetch);
		prefetchList.add(prefetch2);
		AdobeCallback<String> adobeCallback = status -> {
			if (status == null) {
				System.out.println("New prefetch successful: true");
				contentPrefetched = true;
			} else {
				System.out.println("New prefetch failed, error: " + status);
				contentPrefetched = false;
				prefetchStatus[0] = status;
			}

			latch.countDown();
		};
		Target.prefetchContent(prefetchList, targetParameters, adobeCallback);
		latch.await(5, TimeUnit.SECONDS);
		setPrefetchStatusText(prefetchStatus[0]);
	}

	public void setTntId(View view) {
		final Button setTntIdButton = (Button)findViewById(R.id.setTntId);
		final EditText setTntIdInput   = (EditText)findViewById(R.id.setTntIdInput);

		setTntIdButton.setOnClickListener(view1 -> {
			tntId = setTntIdInput.getText().toString();

			System.out.println(String.format("Setting Tnt Id value to (%s).", tntId));
			Target.setTntId(tntId);
		});
	}

	public void getTntId(View view) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final String callbackData[] = new String[1];

		AdobeCallback<String> adobeCallback = (AdobeCallback<String>) data -> {
			callbackData[0] = data;
			latch.countDown();
		};
		Target.getTntId(adobeCallback);
		latch.await(5, TimeUnit.SECONDS);

		System.out.println(String.format("Retrieved Tnt Id value is (%s).", callbackData[0]));
	}

	public void setSessionId(View view) {
		final Button setSessionIdButton = (Button)findViewById(R.id.setSessionId);
		final EditText setSessionIdInput   = (EditText)findViewById(R.id.setSessionIdInput);

		setSessionIdButton.setOnClickListener(view1 -> {
			sessionId = setSessionIdInput.getText().toString();

			System.out.println(String.format("Setting Session Id value to (%s).", sessionId));
			Target.setSessionId(sessionId);
		});
	}

	public void getSessionId(View view) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final String callbackData[] = new String[1];

		AdobeCallback adobeCallback = (AdobeCallback<String>) data -> {
			callbackData[0] = data;
			latch.countDown();
		};
		Target.getSessionId(adobeCallback);
		latch.await(5, TimeUnit.SECONDS);

		System.out.println(String.format("Retrieved Session Id value is (%s).", callbackData[0]));
	}

	public void setThirdPartyId(View view) {
		final Button setTpIdButton = (Button)findViewById(R.id.setThirdPartyId);
		final EditText setTpIdInput   = (EditText)findViewById(R.id.setTpIdInput);

		setTpIdButton.setOnClickListener(view1 -> {
			thirdPartyId = setTpIdInput.getText().toString();

			System.out.println(String.format("Setting Third Party Id value to (%s).", thirdPartyId));
			Target.setThirdPartyId(thirdPartyId);
		});
	}

	public void getThirdPartyId(View view) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final String callbackData[] = new String[1];

		AdobeCallback adobeCallback = (AdobeCallback<String>) data -> {
			callbackData[0] = data;
			latch.countDown();
		};
		Target.getThirdPartyId(adobeCallback);
		latch.await(5, TimeUnit.SECONDS);

		System.out.println(String.format("Retrieved Third Party Id value is (%s).", callbackData[0]));
	}

	public void resetTarget(View view) {
		final TextView prefetchStatusTextView = findViewById(R.id.prefetchStatus);
		Target.resetExperience();
		Target.clearPrefetchCache();
		runOnUiThread(() -> prefetchStatusTextView.setText("Prefetch cache cleared"));
		contentPrefetched = false;
		mboxList.clear();
	}

	public void executeRawRequest(View view) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);

		final Map<String, Object> executeMbox1 = new HashMap<String, Object>() {
			{
				put("index", 0);
				put("name", "testmbox1");
				put("parameters", new HashMap<String, String>() {
					{
						put("mbox_parameter_key1", "mbox_parameter_value1");
					}
				});
			}
		};

		final Map<String, Object> executeMbox2 = new HashMap<String, Object>() {
			{
				put("index", 1);
				put("name", "testmbox2");
				put("parameters", new HashMap<String, String>() {
					{
						put("mbox_parameter_key2", "mbox_parameter_value2");
					}
				});
			}
		};

		final List<Map<String, Object>> executeMboxes = new ArrayList<>();
		executeMboxes.add(executeMbox1);
		executeMboxes.add(executeMbox2);

		final Map<String, Object> request = new HashMap<String, Object>() {
			{
				put("execute", new HashMap<String, Object>() {
					{
						put("mboxes", executeMboxes);
					}
				});
			}
		};

		Target.executeRawRequest(request, response -> {
			System.out.println("Received Target raw response.");

			if (response == null) {
				System.out.println("Null Target response!");
				return;
			}

			final Map<String, Object> execute = (Map<String, Object>)response.get("execute");

			if (execute == null || execute.isEmpty()) {
				return;
			}

			final List<Map<String, Object>> executeMboxes1 = (List<Map<String, Object>>)execute.get("mboxes");

			if (executeMboxes1 == null || executeMboxes1.isEmpty()) {
				return;
			}

			for (Map<String, Object> mbox : executeMboxes1) {
				final String mboxName = (String)mbox.get("name");

				if (mboxName == null || mboxName.isEmpty()) {
					continue;
				}

				final List<Map<String, Object>> metrics = (List<Map<String, Object>>)mbox.get("metrics");
				String eventToken = null;

				if (metrics != null && !metrics.isEmpty()) {
					eventToken = (String)metrics.get(0).get("eventToken");
				}

				if (eventToken != null && !eventToken.isEmpty()) {
					final String token = eventToken;
					notificationTokens.add(new HashMap<String, String>() {
						{
							put("name", mboxName);
							put("token", token);
						}

					});
				}
			}

			latch.countDown();
		});
		latch.await(5, TimeUnit.SECONDS);
	}

	public void sendRawNotifications(View view) {
		final List<Map<String, Object>> notifications = new ArrayList<>();
		int i = 0;

		for (final Map<String, String> tokenMap : notificationTokens) {
			final String id = String.valueOf(i);
			final Map<String, Object> notification = new HashMap<String, Object>() {
				{
					put("id", id);
					put("timestamp", (long)(System.currentTimeMillis()));
					put("type", "click");
					put("mbox", new HashMap<String, Object>() {
						{
							put("name", tokenMap.get("name"));
						}
					});
					put("tokens", new ArrayList<String>() {
						{
							add(tokenMap.get("token"));
						}
					});
					put("parameters", new HashMap<String, Object>() {
						{
							put("mbox_parameter_key3", "mbox_parameter_value3");
						}
					});
				}
			};
			notifications.add(notification);
			i += 1;
		}

		if (notifications.isEmpty()) {
			System.out.println("No notifications available to send!");
			return;
		}

		final Map<String, Object> request = new HashMap<>();
		request.put("notifications", notifications);
		Target.sendRawNotifications(request);
	}

	private void setPrefetchStatusText(final String status) {
		final TextView prefetchStatusTextView = findViewById(R.id.prefetchStatus);

		runOnUiThread(() -> {
			if (contentPrefetched) {
				prefetchStatusTextView.setText("Content prefetched");
			} else {
				if (status != null) {
					prefetchStatusTextView.setText("Prefetch failed with error: " + status);
				} else {
					prefetchStatusTextView.setText("Prefetch failed");
				}
			}
		});
	}
}