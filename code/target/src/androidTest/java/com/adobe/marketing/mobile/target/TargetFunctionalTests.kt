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
package com.adobe.marketing.mobile.target

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adobe.marketing.mobile.AdobeCallback
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.SDKHelper
import com.adobe.marketing.mobile.Target
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.NetworkRequest
import com.adobe.marketing.mobile.services.Networking
import com.adobe.marketing.mobile.services.ServiceProvider
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

class Retry(private val numberOfTestAttempts: Int) : TestRule {
    private var currentTestRun = 0
    override fun apply(base: Statement, description: Description): Statement {
        return statement(base, description)
    }

    private fun statement(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                var caughtThrowable: Throwable? = null
                for (i in 0 until numberOfTestAttempts) {
                    currentTestRun = i + 1
                    try {
                        base.evaluate()
                        return
                    } catch (t: Throwable) {
                        caughtThrowable = t
                        System.err.println(
                            description.displayName + ": run " + currentTestRun + " failed, " +
                                    (numberOfTestAttempts - currentTestRun) + " retries remain."
                        )
                        System.err.println("test failure caused by: " + caughtThrowable.getLocalizedMessage())
                    }
                }
                System.err.println(description.displayName + ": giving up after " + numberOfTestAttempts + " failures")
                throw (caughtThrowable)!!
            }
        }
    }
}

private typealias NetworkMonitor = (request: NetworkRequest) -> Unit
private var targetResponse: InputStream? = null

@RunWith(AndroidJUnit4::class)
class TargetFunctionalTests {

    @get:Rule val totalTestCount = Retry(2)

    companion object {
        // Tests will be run at most 2 times
        private var networkMonitor: NetworkMonitor? = null

        private val mboxParameters: Map<String, String> =
            mapOf("mbox_parameter_key" to "mbox_parameter_value")
        private val mboxParameters2: Map<String, String> =
            mapOf("mbox_parameter_key2" to "mbox_parameter_value2")
        private val emptyMboxParameters: Map<String, String> = mapOf("" to "")
        private val profileParameters: Map<String, String> =
            mapOf("profile_parameter_key" to "profile_parameter_value")
        private val profileParameters2: Map<String, String> =
            mapOf("profile_parameter_key2" to "profile_parameter_value2")
        private val emptyProfileParameters: Map<String, String> = mapOf("" to "")
        private val mboxParametersWithPropertyToken: Map<String, String> = mapOf(
            "mbox_parameter_key" to "mbox_parameter_value",
            "at_property" to "mbox_property_token"
        )
        private val mboxParametersWithPropertyToken2: Map<String, String> = mapOf(
            "mbox_parameter_key2" to "mbox_parameter_value2",
            "at_property" to "mbox_property_token2"
        )
        private val mboxParametersWithEmptyPropertyToken: Map<String, String> = mapOf(
            "mbox_parameter_key3" to "mbox_parameter_value3",
            "at_property" to ""
        )
        private val orderParameters: Map<String, Any> = mapOf(
            "id" to "SomeOrderID",
            "total" to 4445.12, "purchasedProductIds" to listOf("no1", "no2", "no3")
        )
        private val orderParameters2: Map<String, Any> = mapOf(
            "id" to "SomeOrderID2",
            "total" to 4567.89, "purchasedProductIds" to listOf("a1", "a2", "a3")
        )
        private val orderParameters3: Map<String, Any> = mapOf(
            "id" to "SomeOrderID3",
            "total" to 1234.56, "purchasedProductIds" to listOf("b1", "b2", "b3")
        )
        private val emptyOrderParameters: Map<String, Any> = mapOf("" to "")
        private val productParameters: Map<String, String> =
            mapOf("id" to "764334", "categoryId" to "Online")
        private val productParameters2: Map<String, String> =
            mapOf("id" to "765432", "categoryId" to "Offline")
        private val emptyProductParameters: Map<String, String> = mapOf("" to "")
        private val defaultContent = "Default Content String"
        private val mboxName = "Title of mbox"
        private val targetClientCode = "adobeobumobile5targe"

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            SDKHelper.resetSDK()
            ServiceProvider.getInstance().networkService = Networking { request, callback ->
                var connection: HttpConnecting? = null
                with(request.url) {
                    when {
                        startsWith("https://adobe.com") && contains("rules.zip") -> {
                            connection =
                                MockedHttpConnecting(TargetTestHelper.readInputStreamFromFile("rules"))
                        }
                        contains(".tt.omtrdc.net/rest/v1/delivery/") -> {
                            connection = MockedHttpConnecting()
                        }
                    }
                }
                if (callback != null && connection != null) {
                    callback.call(connection)
                } else {
                    // If no callback is passed by the client, close the connection.
                    connection?.close()
                }
                networkMonitor?.let { it(request) }


            }
        }
    }

    @Before
    fun setup() {
        MobileCore.setApplication(ApplicationProvider.getApplicationContext())
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        val countDownLatch = CountDownLatch(1)
        MobileCore.registerExtensions(
            listOf(
                Target.EXTENSION,
                MonitorExtension::class.java
            )
        ) {
            countDownLatch.countDown()
        }
        Assert.assertTrue(countDownLatch.await(1000, TimeUnit.MILLISECONDS))

        val configurationLatch = CountDownLatch(1)
        configurationAwareness { configurationLatch.countDown() }
        MobileCore.updateConfiguration(getConfigurationData())
        Assert.assertTrue(configurationLatch.await(1000, TimeUnit.MILLISECONDS))
    }

    @After
    fun tearDown() {
        targetResponse = null
        Target.resetExperience()
        Target.clearPrefetchCache()
    }

    //1
    @Test
    fun testExtensionVersion() {
        Assert.assertEquals(TargetTestConstants.EXTENSION_VERSION, Target.extensionVersion())
    }

    // ---------------------------------------------------//
    // Happy Tests
    // ---------------------------------------------------//
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetGetThirdPartyId_Check3rdPartyIdIsNilWhenNotSet() {
        // setup
        val localLatch = CountDownLatch(1)
        var retrievedThirdPartyID: String? = null

        // test
        Target.getThirdPartyId { data ->
            retrievedThirdPartyID = data
            localLatch.countDown()
        }
        localLatch.await(5, TimeUnit.SECONDS)

        // verify
        Assert.assertNull(retrievedThirdPartyID)
    }

    @Test
    fun test_Functional_Happy_Target_targetSetThirdPartyId_TargetGetThirdPartyId_SetAndTestThe3rdPartyId() {
        // setup
        val localLatch = CountDownLatch(1)
        var retrievedThirdPartyID: String? = null
        val thirdPartyID = "testThirdPartyID"
        Target.setThirdPartyId(thirdPartyID)

        // test
        Target.getThirdPartyId { data ->
            retrievedThirdPartyID = data
            localLatch.countDown()
        }
        localLatch.await(5, TimeUnit.SECONDS)

        // verify
        Assert.assertEquals(thirdPartyID, retrievedThirdPartyID)
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetPrefetchContent_ValidateTheTargetParameters() {
        // setup
        val mboxNames = arrayOf(mboxName)
        val targetParameters: TargetParameters = TargetParameters.Builder()
            .product(TargetProduct.fromEventData(productParameters))
            .order(TargetOrder.fromEventData(orderParameters))
            .parameters(mboxParameters)
            .profileParameters(profileParameters)
            .build()
        val targetPrefetchList = listOf(TargetPrefetch(mboxName, targetParameters))
        targetResponse = TargetTestHelper.getResponseForTarget(null, mboxNames,
            targetClientCode, "prefetchedContent", null, null, null,
            null, true, true)

        var prefetchErrorStatus: String? = null
        var requestString = ""
        val networkCountDownLatch = CountDownLatch(1)
        networkMonitor = { request ->
            requestString = String(request.body, Charsets.UTF_8)
            networkCountDownLatch.countDown()
        }

        // test
        val callbackCountdownLatch = CountDownLatch(1)
        Target.prefetchContent(targetPrefetchList, targetParameters) { status ->
            prefetchErrorStatus = status
            callbackCountdownLatch.countDown()
        }
        callbackCountdownLatch.await(5, TimeUnit.SECONDS)
        networkCountDownLatch.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(requestString)
        val prefetch = json.getJSONObject("prefetch").getJSONArray("mboxes")
        val mbox = prefetch.getJSONObject(0)
        Assert.assertEquals(1, prefetch.length().toLong())
        Assert.assertEquals(mboxName, mbox.getString("name"))
        Assert.assertEquals(
            "mbox_parameter_value",
            mbox.getJSONObject("parameters").getString("mbox_parameter_key")
        )
        Assert.assertEquals(
            "profile_parameter_value",
            mbox.getJSONObject("profileParameters").getString("profile_parameter_key")
        )
        Assert.assertEquals("SomeOrderID", mbox.getJSONObject("order").getString("id"))
        Assert.assertEquals(4445.12, mbox.getJSONObject("order").getDouble("total"), 0.001)
        Assert.assertEquals(
            "[\"no1\",\"no2\",\"no3\"]",
            mbox.getJSONObject("order").getString("purchasedProductIds")
        )
        Assert.assertEquals("764334", mbox.getJSONObject("product").getString("id"))
        Assert.assertEquals("Online", mbox.getJSONObject("product").getString("categoryId"))
        Assert.assertNull(prefetchErrorStatus)
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetPrefetchContentWithTargetParameters_Prefetch_Smoke() {
        // setup
        val mboxNames = arrayOf("mbox1")
        val targetParameters: TargetParameters = TargetParameters.Builder()
            .product(TargetProduct.fromEventData(productParameters))
            .order(TargetOrder.fromEventData(orderParameters))
            .parameters(mboxParameters)
            .profileParameters(profileParameters)
            .build()
        val targetPrefetchList = listOf(TargetPrefetch("mbox1", targetParameters))
        targetResponse = TargetTestHelper.getResponseForTarget(null, mboxNames,
            targetClientCode, "prefetchedContent", null, null, null,
            null, true, true)

        var callbackErrorStatus: String? = null
        var prefetchRequestString = ""
        val networkCountDownLatch = CountDownLatch(1)
        networkMonitor = { request ->
            prefetchRequestString = String(request.body, Charsets.UTF_8)
            networkCountDownLatch.countDown()
        }

        // test
        val prefetchCountdownLatch = CountDownLatch(1)
        Target.prefetchContent(targetPrefetchList, targetParameters) { status ->
            callbackErrorStatus = status
            prefetchCountdownLatch.countDown()
        }
        networkCountDownLatch.await(5, TimeUnit.SECONDS)
        prefetchCountdownLatch.await(5, TimeUnit.SECONDS)

        // verify
        var json = JSONObject(prefetchRequestString)
        val prefetch = json.getJSONObject("prefetch").getJSONArray("mboxes")
        Assert.assertEquals(1, prefetch.length().toLong())
        val mbox = prefetch.getJSONObject(0)
        Assert.assertEquals("mbox1", mbox.getString("name"))
        Assert.assertEquals(
            "mbox_parameter_value",
            mbox.getJSONObject("parameters").getString("mbox_parameter_key")
        )
        Assert.assertEquals(
            "profile_parameter_value",
            mbox.getJSONObject("profileParameters").getString("profile_parameter_key")
        )
        Assert.assertEquals("SomeOrderID", mbox.getJSONObject("order").getString("id"))
        Assert.assertEquals(4445.12, mbox.getJSONObject("order").getDouble("total"), 0.001)
        Assert.assertEquals("764334", mbox.getJSONObject("product").getString("id"))
        Assert.assertEquals("Online", mbox.getJSONObject("product").getString("categoryId"))
        Assert.assertNull(callbackErrorStatus)

        // setup
        val retrieveLocationCountdownLatch = CountDownLatch(1)
        val targetRequestList = listOf(TargetRequest(mboxName, null, defaultContent) { status ->
            retrieveLocationCountdownLatch.countDown()
        })
        var retrieveLocationRequestString = ""
        val locationContentTargetParameters: TargetParameters = TargetParameters.Builder()
            .product(TargetProduct.fromEventData(productParameters2))
            .order(TargetOrder.fromEventData(orderParameters2))
            .parameters(mboxParameters2)
            .profileParameters(profileParameters2)
            .build()
        val networkCountDownLatch2 = CountDownLatch(1)
        networkMonitor = { request ->
            retrieveLocationRequestString = String(request.body, Charsets.UTF_8)
            networkCountDownLatch2.countDown()
        }

        // test
        Target.retrieveLocationContent(targetRequestList, locationContentTargetParameters)
        networkCountDownLatch2.await(5, TimeUnit.SECONDS)
        retrieveLocationCountdownLatch.await(5, TimeUnit.SECONDS)

        // verify
        json = JSONObject(retrieveLocationRequestString)
        Assert.assertFalse(json.has("notifications"))
        val loadedRequests = json.getJSONObject("execute").getJSONArray("mboxes")
        Assert.assertEquals(1, loadedRequests.length().toLong())
        val loadedRequest = loadedRequests.getJSONObject(0)
        Assert.assertEquals(mboxName, loadedRequest.getString("name"))
        Assert.assertEquals("0", loadedRequest.getString("index"))
        Assert.assertEquals(
            "mbox_parameter_value2",
            loadedRequest.getJSONObject("parameters").getString("mbox_parameter_key2")
        )
        Assert.assertEquals(
            "profile_parameter_value2",
            loadedRequest.getJSONObject("profileParameters").getString("profile_parameter_key2")
        )
        Assert.assertEquals("SomeOrderID2", loadedRequest.getJSONObject("order").getString("id"))
        Assert.assertEquals(4567.89, loadedRequest.getJSONObject("order").getDouble("total"), 0.001)
        Assert.assertEquals("765432", loadedRequest.getJSONObject("product").getString("id"))
        Assert.assertEquals(
            "Offline",
            loadedRequest.getJSONObject("product").getString("categoryId")
        )
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_VerifyWithDefaultParameters() {
        // setup
        val retrieveLocationCountdownLatch = CountDownLatch(1)
        val targetParameters: TargetParameters = TargetParameters.Builder()
            .parameters(mboxParameters)
            .profileParameters(profileParameters)
            .build()
        var callbackData: String? = null
        val targetRequestList = listOf(TargetRequest(mboxName, targetParameters, defaultContent) { data ->
            callbackData = data
            retrieveLocationCountdownLatch.countDown()
        })
        var retrieveLocationRequestString = ""
        val networkCountDownLatch = CountDownLatch(1)
        networkMonitor = { request ->
            retrieveLocationRequestString = String(request.body, Charsets.UTF_8)
            networkCountDownLatch.countDown()
        }

        // test
        Target.retrieveLocationContent(targetRequestList, targetParameters)
        networkCountDownLatch.await(5, TimeUnit.SECONDS)
        retrieveLocationCountdownLatch.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(retrieveLocationRequestString)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        Assert.assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        Assert.assertEquals(mboxName, mbox.getString("name"))
        val mboxParams = mbox.getJSONObject("parameters")
        Assert.assertEquals("mbox_parameter_value", mboxParams.getString("mbox_parameter_key"))
        val profileParams = mbox.getJSONObject("profileParameters")
        Assert.assertEquals(
            "profile_parameter_value",
            profileParams.getString("profile_parameter_key")
        )
        Assert.assertEquals(defaultContent, callbackData)
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_VerifyWithOrderParameters() {
        // setup
        val retrieveLocationCountdownLatch = CountDownLatch(1)
        val targetParameters: TargetParameters = TargetParameters.Builder()
            .order(TargetOrder.fromEventData(orderParameters))
            .build()
        val targetRequestList = listOf(TargetRequest(mboxName, targetParameters, defaultContent) { data ->
            retrieveLocationCountdownLatch.countDown()
        })
        var retrieveLocationRequestString = ""
        val networkCountDownLatch = CountDownLatch(1)
        networkMonitor = { request ->
            retrieveLocationRequestString = String(request.body, Charsets.UTF_8)
            networkCountDownLatch.countDown()
        }

        // test
        Target.retrieveLocationContent(targetRequestList, targetParameters)
        networkCountDownLatch.await(5, TimeUnit.SECONDS)
        retrieveLocationCountdownLatch.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(retrieveLocationRequestString)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        Assert.assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        val orderParams = mbox.getJSONObject("order")
        Assert.assertEquals(3, orderParams.length().toLong())
        Assert.assertEquals("SomeOrderID", orderParams.getString("id"))
        Assert.assertEquals(4445.12, orderParams.getDouble("total"), 0.001)
        Assert.assertEquals(
            "[\"no1\",\"no2\",\"no3\"]",
            orderParams.getString("purchasedProductIds")
        )
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_VerifyWithProductParameters() {
        // setup
        val retrieveLocationCountdownLatch = CountDownLatch(1)
        val targetParameters: TargetParameters = TargetParameters.Builder()
            .product(TargetProduct.fromEventData(productParameters))
            .build()
        val targetRequestList = listOf(TargetRequest(mboxName, targetParameters, defaultContent) { data ->
            retrieveLocationCountdownLatch.countDown()
        })
        var retrieveLocationRequestString = ""
        val networkCountDownLatch = CountDownLatch(1)
        networkMonitor = { request ->
            retrieveLocationRequestString = String(request.body, Charsets.UTF_8)
            networkCountDownLatch.countDown()
        }

        // test
        Target.retrieveLocationContent(targetRequestList, targetParameters)
        networkCountDownLatch.await(5, TimeUnit.SECONDS)
        retrieveLocationCountdownLatch.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(retrieveLocationRequestString)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        Assert.assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        val productParams = mbox.getJSONObject("product")
        Assert.assertEquals(2, productParams.length().toLong())
        Assert.assertEquals("764334", productParams.getString("id"))
        Assert.assertEquals("Online", productParams.getString("categoryId"))
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_VerifyWithOrderAndProductParameters() {
        // setup
        val retrieveLocationCountdownLatch = CountDownLatch(1)
        val targetParameters: TargetParameters = TargetParameters.Builder()
            .order(TargetOrder.fromEventData(orderParameters))
            .product(TargetProduct.fromEventData(productParameters))
            .build()
        val targetRequestList = listOf(TargetRequest(mboxName, targetParameters, defaultContent) { data ->
            retrieveLocationCountdownLatch.countDown()
        })
        var retrieveLocationRequestString = ""
        val networkCountDownLatch = CountDownLatch(1)
        networkMonitor = { request ->
            retrieveLocationRequestString = String(request.body, Charsets.UTF_8)
            networkCountDownLatch.countDown()
        }

        // test
        Target.retrieveLocationContent(targetRequestList, targetParameters)
        networkCountDownLatch.await(5, TimeUnit.SECONDS)
        retrieveLocationCountdownLatch.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(retrieveLocationRequestString)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        Assert.assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        val orderParams = mbox.getJSONObject("order")
        Assert.assertEquals(3, orderParams.length().toLong())
        Assert.assertEquals("SomeOrderID", orderParams.getString("id"))
        Assert.assertEquals(4445.12, orderParams.getDouble("total"), 0.001)
        Assert.assertEquals(
            "[\"no1\",\"no2\",\"no3\"]",
            orderParams.getString("purchasedProductIds")
        )
        val productParams = mbox.getJSONObject("product")
        Assert.assertEquals(2, productParams.length().toLong())
        Assert.assertEquals("764334", productParams.getString("id"))
        Assert.assertEquals("Online", productParams.getString("categoryId"))
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_CheckindexIdsAreCorrectForMultipleMboxes() {
        // setup
        val retrieveLocationCountdownLatch1 = CountDownLatch(1)
        var retrieveLocationRequestString1 = ""
        val retrieveLocationCountdownLatch2 = CountDownLatch(1)
        var retrieveLocationRequestString2 = ""
        val targetRequestList = listOf(
            TargetRequest(mboxName, null, defaultContent) { data ->
                retrieveLocationRequestString1 = data
                retrieveLocationCountdownLatch1.countDown()
            },
            TargetRequest("mbox2", null, defaultContent) { data ->
                retrieveLocationRequestString2 = data
                retrieveLocationCountdownLatch2.countDown()
            },
            TargetRequest("mbox3", null, defaultContent, null as AdobeCallback<String?>?)
        )
        var retrieveLocationRequestString = ""
        val networkCountDownLatch = CountDownLatch(1)
        networkMonitor = { request ->
            retrieveLocationRequestString = String(request.body, Charsets.UTF_8)
            networkCountDownLatch.countDown()
        }

        // test
        Target.retrieveLocationContent(targetRequestList, null)
        networkCountDownLatch.await(5, TimeUnit.SECONDS)
        retrieveLocationCountdownLatch1.await(5, TimeUnit.SECONDS)
        retrieveLocationCountdownLatch2.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(retrieveLocationRequestString)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        Assert.assertEquals(3, mboxes.length().toLong())
        val mbox1 = mboxes.getJSONObject(0)
        val mbox2 = mboxes.getJSONObject(1)
        val mbox3 = mboxes.getJSONObject(2)
        Assert.assertEquals("0", mbox1.getString("index"))
        Assert.assertEquals("1", mbox2.getString("index"))
        Assert.assertEquals("2", mbox3.getString("index"))
        Assert.assertEquals(defaultContent, retrieveLocationRequestString1)
        Assert.assertEquals(defaultContent, retrieveLocationRequestString2)
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_VerifyWithoutTargetParameters() {
        // setup
        val retrieveLocationCountdownLatch = CountDownLatch(1)
        val targetRequestList = listOf(TargetRequest(mboxName, null, defaultContent) { data ->
            retrieveLocationCountdownLatch.countDown()
        })
        var retrieveLocationRequestString = ""
        val networkCountDownLatch = CountDownLatch(1)
        networkMonitor = { request ->
            retrieveLocationRequestString = String(request.body, Charsets.UTF_8)
            networkCountDownLatch.countDown()
        }

        // test
        Target.retrieveLocationContent(targetRequestList, null)
        networkCountDownLatch.await(5, TimeUnit.SECONDS)
        retrieveLocationCountdownLatch.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(retrieveLocationRequestString)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        Assert.assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        Assert.assertEquals(mboxName, mbox.getString("name"))
        Assert.assertFalse(mbox.has("parameters"))
        Assert.assertFalse(mbox.has("order"))
        Assert.assertFalse(mbox.has("product"))
        Assert.assertFalse(mbox.has("profileParameters"))
    }

    private fun getConfigurationData(): Map<String, Any> {
        return mapOf(
            "lifecycle.sessionTimeout" to 1,
            "global.privacy" to "optedin",
            "identity.adidEnabled" to true,
            "global.ssl" to true,
            "global.timezone" to "PDT",
            "global.timeoneOffset" to -420,
            "target.clientCode" to targetClientCode,
            "target.environmentId" to 4455,
            "target.timeout" to 5
        )
    }

    private fun configurationAwareness(callback: ConfigurationMonitor) {
        MonitorExtension.configurationAwareness(callback)
    }

    private class MockedHttpConnecting(val responseStream: InputStream? = targetResponse) :
        HttpConnecting {

        override fun getInputStream(): InputStream? {
            return responseStream
        }

        override fun getErrorStream(): InputStream? {
            return null
        }

        override fun getResponseCode(): Int {
            return 200
        }

        override fun getResponseMessage(): String {
            return ""
        }

        override fun getResponsePropertyValue(responsePropertyKey: String?): String {
            return ""
        }

        override fun close() {
            responseStream?.close()
        }
    }
}