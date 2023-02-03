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

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.Target
import com.adobe.marketing.mobile.services.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

private const val TARGET_DATA_STORE = "ADOBEMOBILE_TARGET"
private const val CONFIG_DATA_STORE = "AdobeMobile_ConfigState"
private var dataStore: NamedCollection? = null

@RunWith(AndroidJUnit4::class)
class TargetFunctionalTests {

    @get:Rule val totalTestCount = Retry(2)

    companion object {
        // Tests will be run at most 2 times
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

        // network capture variables
        private var networkMonitor: NetworkMonitor? = null
        private var mockedNetworkResponse: InputStream? = null
        private var networkRequestUrl: String? = null
        private var networkRequestBody: String? = null

        // callback result captures
        private var retrievedLocationResponse: String? = null
        private var prefetchErrorStatus: String? = null

        // reusable latches
        private var waitForCallback: CountDownLatch? = null
        private var waitForNetworkCall: CountDownLatch? = null

        private val targetParameters = TargetParameters.Builder()
            .product(TargetProduct.fromEventData(productParameters))
            .order(TargetOrder.fromEventData(orderParameters))
            .parameters(mboxParameters)
            .profileParameters(profileParameters)
            .build()

        private fun setupNetwork() {
            ServiceProvider.getInstance().networkService = Networking { request, callback ->
                var connection: HttpConnecting? = null
                with(request.url) {
                    when {
                        startsWith("https://assets.adobedtm.com") && contains("rules.zip") -> {
                            connection =
                                MockedHttpConnecting(TargetTestHelper.readInputStreamFromFile("rules"))
                        }
                        contains(".tt.omtrdc.net/rest/v1/delivery/") -> {
                            connection = MockedHttpConnecting()
                            networkMonitor?.let { it(request) }
                        }
                        else -> {
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
            }
        }

        private fun resetLatches() {
            waitForCallback = CountDownLatch(1)
            waitForNetworkCall = CountDownLatch(1)
        }

        private fun resetNetworkVariables() {
            ServiceProvider.getInstance().networkService = null
            mockedNetworkResponse = null
            networkRequestUrl = null
            networkRequestBody = null
        }

        private fun resetCallbackResponses() {
            retrievedLocationResponse = null
            prefetchErrorStatus = null
        }

        private fun clearDatastore() {
            dataStore = ServiceProvider.getInstance().dataStoreService?.getNamedCollection(TARGET_DATA_STORE)
            dataStore?.removeAll()
            dataStore = ServiceProvider.getInstance().dataStoreService?.getNamedCollection(CONFIG_DATA_STORE)
            dataStore?.removeAll()
        }
    }

    @Before
    fun setup() {
        setupNetwork()
        MobileCore.setApplication(ApplicationProvider.getApplicationContext())
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        // Register required extensions
        val waitForRegistration = CountDownLatch(1)
        MobileCore.registerExtensions(
            listOf(
                Target.EXTENSION,
                Identity.EXTENSION,
                MonitorExtension::class.java
            )
        ) {
            waitForRegistration.countDown()
        }
        assertTrue(waitForRegistration.await(1000, TimeUnit.MILLISECONDS))

        // Configuration
        val configurationLatch = CountDownLatch(1)
        configurationAwareness { configurationLatch.countDown() }
        MobileCore.updateConfiguration(getConfigurationData())
        assertTrue(configurationLatch.await(1000, TimeUnit.MILLISECONDS))

        // set latches
        resetLatches()

        // setup network capturer
        networkMonitor = { request ->
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }
    }

    @After
    fun tearDown() {
        Target.clearPrefetchCache()
        Target.resetExperience()
        resetNetworkVariables()
        resetCallbackResponses()
        clearDatastore()
        TargetTestHelper.cleanCacheDir()
        SDKHelper.resetSDK()
        SDKHelper.resetTargetListener()
    }

    //1
    @Test
    fun testExtensionVersion() {
        assertEquals(TargetTestConstants.EXTENSION_VERSION, Target.extensionVersion())
    }

    // ---------------------------------------------------//
    // Happy Tests
    // ---------------------------------------------------//
    // Test Case No : 1
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_VerifyWithDefaultParameters() {
        // test
        val targetParameters = TargetParameters.Builder()
            .parameters(mboxParameters)
            .profileParameters(profileParameters)
            .build()
        val targetRequest = TargetRequest(mboxName, targetParameters , defaultContent) { data ->
            retrievedLocationResponse = data
            waitForCallback?.countDown()
        }
        Target.retrieveLocationContent(listOf(targetRequest), targetParameters)
        waitForCallback?.await(50, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(networkRequestBody)
        val mboxes: JSONArray = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length())
        val mbox: JSONObject = mboxes.getJSONObject(0)
        assertEquals(mboxName, mbox.getString("name"))
        val mboxParams = mbox.getJSONObject("parameters")
        assertEquals("mbox_parameter_value", mboxParams.getString("mbox_parameter_key"))
        val profileParams = mbox.getJSONObject("profileParameters")
        assertEquals("profile_parameter_value", profileParams.getString("profile_parameter_key"))
        assertEquals(defaultContent, retrievedLocationResponse)
    }

    // Test Case No : 2
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_VerifyWithOrderParameters() {
        // setup
        networkMonitor = { request ->
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }

        // test
        val targetParameters = TargetParameters.Builder()
            .order(TargetOrder.fromEventData(orderParameters))
            .build()
        val targetRequest = TargetRequest(mboxName, targetParameters , defaultContent) { data ->
            retrievedLocationResponse = data
            waitForCallback?.countDown()
        }
        Target.retrieveLocationContent(listOf(targetRequest), targetParameters)
        waitForCallback?.await(5, TimeUnit.SECONDS)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        val orderParams = mbox.getJSONObject("order")
        assertEquals(3, orderParams.length().toLong())
        assertEquals("SomeOrderID", orderParams.getString("id"))
        assertEquals(4445.12, orderParams.getDouble("total"), 0.001)
        assertEquals(
            "[\"no1\",\"no2\",\"no3\"]",
            orderParams.getString("purchasedProductIds")
        )
    }

    // Test Case No : 3
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_VerifyWithProductParameters() {
        // test
        val targetParameters = TargetParameters.Builder()
            .product(TargetProduct.fromEventData(productParameters))
            .build()
        val targetRequest = TargetRequest(mboxName, targetParameters , defaultContent) { data ->
            retrievedLocationResponse = data
            waitForCallback?.countDown()
        }
        Target.retrieveLocationContent(listOf(targetRequest), targetParameters)
        waitForCallback?.await(5, TimeUnit.SECONDS)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        val productParams = mbox.getJSONObject("product")
        assertEquals(2, productParams.length().toLong())
        assertEquals("764334", productParams.getString("id"))
        assertEquals("Online", productParams.getString("categoryId"))
        assertEquals(defaultContent, retrievedLocationResponse)
    }

    // Test Case No : 4
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_VerifyWithOrderAndProductParameters() {
        // setup
        networkMonitor = { request ->
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }

        val targetParameters = TargetParameters.Builder()
            .order(TargetOrder.fromEventData(orderParameters))
            .product(TargetProduct.fromEventData(productParameters))
            .build()
        val targetRequest = TargetRequest(mboxName, targetParameters , defaultContent) { data ->
            retrievedLocationResponse = data
            waitForCallback?.countDown()
        }
        Target.retrieveLocationContent(listOf(targetRequest), targetParameters)
        waitForCallback?.await(5, TimeUnit.SECONDS)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        val orderParams = mbox.getJSONObject("order")
        assertEquals(3, orderParams.length().toLong())
        assertEquals("SomeOrderID", orderParams.getString("id"))
        assertEquals(4445.12, orderParams.getDouble("total"), 0.001)
        assertEquals(
            "[\"no1\",\"no2\",\"no3\"]",
            orderParams.getString("purchasedProductIds")
        )
        val productParams = mbox.getJSONObject("product")
        assertEquals(2, productParams.length().toLong())
        assertEquals("764334", productParams.getString("id"))
        assertEquals("Online", productParams.getString("categoryId"))
    }

    // Test Case No : 5
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetSetThirdPartyId_VerifyIfThirdPartyIdGotSet() {
        // setup
        networkMonitor = { request ->
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }
        val thirdPartyID = "testID"
        Target.setThirdPartyId(thirdPartyID)
        val targetRequest = TargetRequest(mboxName, targetParameters , defaultContent) { data ->
            retrievedLocationResponse = data
            waitForCallback?.countDown()
        }

        // test
        Target.retrieveLocationContent(listOf(targetRequest), targetParameters)
        waitForCallback?.await(5, TimeUnit.SECONDS)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        assertEquals(mboxName, mbox.getString("name"))
        val id = json.getJSONObject("id")
        assertEquals(thirdPartyID, id.getString("thirdPartyId"))
        assertEquals(defaultContent, retrievedLocationResponse)
    }

    // Test Case No : 6
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetResetExperience_Check3rdPartyIdIsNotPresentAfterReset() {
        // setup
        networkMonitor = { request ->
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }

        val thirdPartyID = "testID"
        Target.setThirdPartyId(thirdPartyID)
        Target.resetExperience()
        val targetRequest = TargetRequest(mboxName, targetParameters , defaultContent) { data ->
            retrievedLocationResponse = data
            waitForCallback?.countDown()
        }

        // test
        Target.retrieveLocationContent(listOf(targetRequest), targetParameters)
        waitForCallback?.await(5, TimeUnit.SECONDS)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        assertEquals(mboxName, mbox.getString("name"))
        val id = json.getJSONObject("id")
        assertFalse(id.has("thirdPartyId"))
        assertEquals(defaultContent, retrievedLocationResponse)
    }

    // Test Case No : 7
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetPrefetchContent_ValidateThePrefetchParameter() {
        val mboxNames = arrayOf("mbox1", "mbox2")

        // setup network
        networkMonitor = { request ->
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null, mboxNames,
            targetClientCode, "prefetchedContent", null, null, null,
            null, true, true)


        val prefetchRequest1 = TargetPrefetch(mboxNames[0], targetParameters)
        val prefetchRequest2 = TargetPrefetch(mboxNames[1], targetParameters)

        // test
        Target.prefetchContent(listOf(prefetchRequest1, prefetchRequest2), targetParameters) { prefetchSuccess ->
            prefetchErrorStatus = prefetchSuccess
            waitForCallback?.countDown()
        }
        waitForCallback?.await(5, TimeUnit.SECONDS)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(networkRequestBody)
        val prefetch = json.getJSONObject("prefetch").getJSONArray("mboxes")
        val mbox1 = prefetch.getJSONObject(0)
        val mbox2 = prefetch.getJSONObject(1)
        assertEquals(2, prefetch.length().toLong())
        assertEquals("mbox1", mbox1.getString("name"))
        assertEquals(
            "mbox_parameter_value",
            mbox1.getJSONObject("parameters").getString("mbox_parameter_key")
        )
        assertEquals("mbox2", mbox2.getString("name"))
        assertEquals(
            "mbox_parameter_value",
            mbox2.getJSONObject("parameters").getString("mbox_parameter_key")
        )
        assertNull(prefetchErrorStatus)
    }

    // Test Case No : 8
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_Check3rdPartyIdIsNotPresentInTargetCallWhenNotSet() {
        // test
        retrieveLocationContent(mboxName)

        // verify
        val json = JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        assertEquals(mboxName, mbox.getString("name"))
        val id = json.getJSONObject("id")
        assertFalse(id.has("thirdPartyId"))
        assertEquals(defaultContent, retrievedLocationResponse)
    }

    // Test Case No : 9
    // test_Functional_Happy_Target_targetSetThirdPartyId_Check3rdPartyIdIsPresentInTargetCallWhenSet()
    // This test is covered by test_Functional_Happy_Target_targetSetThirdPartyId_VerifyIfThirdPartyIdGotSet() (test #5)

    // Test Case No : 10
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetGetThirdPartyId_IsNilWhenNotSet() {
        // setup
        val localLatch = CountDownLatch(1)
        var retrievedThirdPartyID: String? = ""

        // test
        Target.getThirdPartyId { data ->
            Log.error("Peaks Debug", data, data)
            retrievedThirdPartyID = data
            localLatch.countDown()
        }
        localLatch.await(5, TimeUnit.SECONDS)

        // verify
        assertNull(retrievedThirdPartyID)
    }

    // Test Case No : 11
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
        assertEquals(thirdPartyID, retrievedThirdPartyID)
    }

    // Test Case No : 12
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetLoadRequests_CheckForIdentityPresentInTargetCall() {
        // setup
        var mcid: String? = null
        waitForNetworkCall = CountDownLatch(3)
        networkMonitor = { request ->
            waitForNetworkCall?.countDown()
        }

        updateConfiguration(mapOf(
            "experienceCloud.org" to  "972C898555E9F7BC7F000101@AdobeOrg",
            "experienceCloud.server" to "identity.com"
        ))
        Identity.syncIdentifier(
            "type",
            "value",
            VisitorID.AuthenticationState.AUTHENTICATED
        )
        Identity.syncIdentifier(
            "type2",
            "value2",
            VisitorID.AuthenticationState.UNKNOWN
        )
        Identity.syncIdentifier(
            "type3",
            "value3",
            VisitorID.AuthenticationState.LOGGED_OUT
        )
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)


        // reset network capturer
        waitForNetworkCall = CountDownLatch(1)
        networkMonitor = { request ->
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }

        // test
        val targetRequestList = listOf(TargetRequest(mboxName, null, defaultContent) { data ->
            retrievedLocationResponse = data
            waitForCallback?.countDown() })
        Target.retrieveLocationContent(targetRequestList, null)
        waitForCallback?.await(5, TimeUnit.SECONDS)
        waitForNetworkCall?.await(5000, TimeUnit.SECONDS)

        val latch2 = CountDownLatch(1)
        val adobeCallback2: AdobeCallback<String?> =
            AdobeCallback<String?> { data ->
                mcid = data
                latch2.countDown()
            }
        Identity.getExperienceCloudId(adobeCallback2)
        latch2.await(5, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())
        val ids = json.getJSONObject("id")
        assertEquals(ids.getString("marketingCloudVisitorId").length.toLong(), 38)
        assertEquals(ids.getString("marketingCloudVisitorId"), mcid)
        assertEquals("authenticated", ids.getJSONArray("customerIds").getJSONObject(0).getString("authenticatedState"))
        assertEquals("type", ids.getJSONArray("customerIds").getJSONObject(0).getString("integrationCode"))
        assertEquals("value", ids.getJSONArray("customerIds").getJSONObject(0).getString("id"))
        assertEquals("unknown", ids.getJSONArray("customerIds").getJSONObject(1).getString("authenticatedState"))
        assertEquals("type2", ids.getJSONArray("customerIds").getJSONObject(1).getString("integrationCode"))
        assertEquals("value2", ids.getJSONArray("customerIds").getJSONObject(1).getString("id"))
        assertEquals("logged_out", ids.getJSONArray("customerIds").getJSONObject(2).getString("authenticatedState"))
        assertEquals("type3", ids.getJSONArray("customerIds").getJSONObject(2).getString("integrationCode"))
        assertEquals("value3", ids.getJSONArray("customerIds").getJSONObject(2).getString("id"))
        assertEquals(defaultContent, retrievedLocationResponse)
    }

    // Test Case No : 13
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetLoadRequests_CheckDefaultParametersAreTheOnesThatsActuallySet() {
        // setup
        retrieveLocationContent(mboxName)

        // verify
        val json = JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        assertEquals(mboxName, mbox.getString("name"))
        assertEquals("4455", json.getString("environmentId"))
        assertEquals(defaultContent, retrievedLocationResponse)
    }

    // Test Case No : 14
    // Duplicate of test case 24

    // Test Case No : 15
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetPrefetchContent_Prefetch_Smoke() {
        // test
        prefetchContent("mbox1")

        // verify prefetch results
        assertNull(prefetchErrorStatus)

        // verify the network request
        var json = JSONObject(networkRequestBody)
        val prefetch = json.getJSONObject("prefetch").getJSONArray("mboxes")
        assertEquals(1, prefetch.length().toLong())
        val mbox = prefetch.getJSONObject(0)
        assertEquals("mbox1", mbox.getString("name"))
        assertEquals("mbox_parameter_value", mbox.getJSONObject("parameters").getString("mbox_parameter_key"))


        // test
        retrieveLocationContent("mbox1")

        // verify
        assertNull(networkRequestBody);
        assertEquals("prefetchedContent", retrievedLocationResponse)

        // test
        // clear prefetch cache then load requests again (mboxes node should be present due to empty prefetch cache)
        Target.clearPrefetchCache()

        // test
        retrieveLocationContent("mbox1")
        json = JSONObject(networkRequestBody)
        val loadedRequests = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, loadedRequests.length().toLong())
        val loadedRequest = loadedRequests.getJSONObject(0)
        assertEquals("mbox1", loadedRequest.getString("name"))
        assertEquals("0", loadedRequest.getString("index"))
    }

    // Test Case No : 18
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
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null, mboxNames,
            targetClientCode, "prefetchedContent", null, null, null,
            null, true, true)

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
        assertEquals(1, prefetch.length().toLong())
        assertEquals(mboxName, mbox.getString("name"))
        assertEquals(
            "mbox_parameter_value",
            mbox.getJSONObject("parameters").getString("mbox_parameter_key")
        )
        assertEquals(
            "profile_parameter_value",
            mbox.getJSONObject("profileParameters").getString("profile_parameter_key")
        )
        assertEquals("SomeOrderID", mbox.getJSONObject("order").getString("id"))
        assertEquals(4445.12, mbox.getJSONObject("order").getDouble("total"), 0.001)
        assertEquals(
            "[\"no1\",\"no2\",\"no3\"]",
            mbox.getJSONObject("order").getString("purchasedProductIds")
        )
        assertEquals("764334", mbox.getJSONObject("product").getString("id"))
        assertEquals("Online", mbox.getJSONObject("product").getString("categoryId"))
        assertNull(prefetchErrorStatus)
    }

    // Test Case No : 19
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
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null, mboxNames,
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
        assertEquals(1, prefetch.length().toLong())
        val mbox = prefetch.getJSONObject(0)
        assertEquals("mbox1", mbox.getString("name"))
        assertEquals(
            "mbox_parameter_value",
            mbox.getJSONObject("parameters").getString("mbox_parameter_key")
        )
        assertEquals(
            "profile_parameter_value",
            mbox.getJSONObject("profileParameters").getString("profile_parameter_key")
        )
        assertEquals("SomeOrderID", mbox.getJSONObject("order").getString("id"))
        assertEquals(4445.12, mbox.getJSONObject("order").getDouble("total"), 0.001)
        assertEquals("764334", mbox.getJSONObject("product").getString("id"))
        assertEquals("Online", mbox.getJSONObject("product").getString("categoryId"))
        assertNull(callbackErrorStatus)

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
        assertFalse(json.has("notifications"))
        val loadedRequests = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, loadedRequests.length().toLong())
        val loadedRequest = loadedRequests.getJSONObject(0)
        assertEquals(mboxName, loadedRequest.getString("name"))
        assertEquals("0", loadedRequest.getString("index"))
        assertEquals(
            "mbox_parameter_value2",
            loadedRequest.getJSONObject("parameters").getString("mbox_parameter_key2")
        )
        assertEquals(
            "profile_parameter_value2",
            loadedRequest.getJSONObject("profileParameters").getString("profile_parameter_key2")
        )
        assertEquals("SomeOrderID2", loadedRequest.getJSONObject("order").getString("id"))
        assertEquals(4567.89, loadedRequest.getJSONObject("order").getDouble("total"), 0.001)
        assertEquals("765432", loadedRequest.getJSONObject("product").getString("id"))
        assertEquals(
            "Offline",
            loadedRequest.getJSONObject("product").getString("categoryId")
        )
    }

    // Test Case No : 24
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetRetrieveLocationContent_CheckIndexIdsAreCorrectForMultipleMboxes() {
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

        val networkCountDownLatch = CountDownLatch(1)
        networkMonitor = { request ->
            networkRequestBody = String(request.body, Charsets.UTF_8)
            networkCountDownLatch.countDown()
        }

        // test
        Target.retrieveLocationContent(targetRequestList, null)
        networkCountDownLatch.await(3, TimeUnit.SECONDS)
        retrieveLocationCountdownLatch1.await(3, TimeUnit.SECONDS)
        retrieveLocationCountdownLatch2.await(3, TimeUnit.SECONDS)

        // verify
        val json = JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(3, mboxes.length().toLong())
        val mbox1 = mboxes.getJSONObject(0)
        val mbox2 = mboxes.getJSONObject(1)
        val mbox3 = mboxes.getJSONObject(2)
        assertEquals("0", mbox1.getString("index"))
        assertEquals("1", mbox2.getString("index"))
        assertEquals("2", mbox3.getString("index"))
        assertEquals(defaultContent, retrieveLocationRequestString1)
        assertEquals(defaultContent, retrieveLocationRequestString2)
    }

    // Test Case No : 25
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
        assertEquals(1, mboxes.length().toLong())
        val mbox = mboxes.getJSONObject(0)
        assertEquals(mboxName, mbox.getString("name"))
        assertFalse(mbox.has("parameters"))
        assertFalse(mbox.has("order"))
        assertFalse(mbox.has("product"))
        assertFalse(mbox.has("profileParameters"))
    }

    //**********************************************************************************************
    // Location Clicked Tests
    //**********************************************************************************************
    // Test Case No : 26
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetLocationClicked() {
        // setup
        prefetchContent("mbox1")

        // verify prefetched mboxes
        val prefetchJson = JSONObject(networkRequestBody)
        assertNotNull(prefetchJson)
        val prefetch = prefetchJson.getJSONObject("prefetch").getJSONArray("mboxes")
        assertEquals(1, prefetch.length().toLong())
        val mbox = prefetch.getJSONObject(0)
        assertEquals("mbox1", mbox.getString("name"))
        assertNull(prefetchErrorStatus)

        // test
        resetNetworkMonitor()
        Target.locationClicked("mbox1", targetParameters)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify location click
        val json = JSONObject(networkRequestBody)
        assertNotNull(json)
        val clickNotifications = json.getJSONArray("notifications")
        val notificationObject = clickNotifications.getJSONObject(0)
        assertEquals(1, clickNotifications.length().toLong())
        assertEquals("click", notificationObject.getString("type"))
        assertNotNull(notificationObject.getString("timestamp"))
        assertNotNull(notificationObject.getString("id"))
        val tokenArray = notificationObject.getJSONArray("tokens")
        assertNotNull(tokenArray)
        assertEquals(1, tokenArray.length().toLong())
        assertEquals("RandomClickTrackEventToken", tokenArray.getString(0))
        var orderParams: org.json.JSONObject? = mbox.getJSONObject("order")
        orderParams = notificationObject.getJSONObject("order")
        assertEquals(3, orderParams.length().toLong())
        assertEquals("SomeOrderID", orderParams.getString("id"))
        assertEquals(4445.12, orderParams.getDouble("total"), 0.001)
        assertEquals("[\"no1\",\"no2\",\"no3\"]", orderParams.getString("purchasedProductIds"))
        var productParams = mbox.getJSONObject("product")
        productParams = notificationObject.getJSONObject("product")
        assertEquals(2, productParams.length().toLong())
        assertEquals("764334", productParams.getString("id"))
        assertEquals("Online", productParams.getString("categoryId"))
        var mboxParams = mbox.getJSONObject("parameters")
        mboxParams = notificationObject.getJSONObject("parameters")
        assertEquals("mbox_parameter_value", mboxParams.getString("mbox_parameter_key"))
        var profileParams = mbox.getJSONObject("profileParameters")
        profileParams = notificationObject.getJSONObject("profileParameters")
        assertEquals("profile_parameter_value", profileParams.getString("profile_parameter_key"))
    }

    // Test Case No : 29
    @Test
    @Throws(Exception::class)
    fun test_Functional_Target_targetLocationClicked_WithoutTargetParameters() {
        // setup
        prefetchContent("mbox1")

        // verify prefetched mboxes
        val prefetchJson = JSONObject(networkRequestBody)
        assertNotNull(prefetchJson)
        assertNull(prefetchErrorStatus)

        // test
        resetNetworkMonitor()
        Target.locationClicked("mbox1", null)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify location click network request
        val json = JSONObject(networkRequestBody)
        assertNotNull(json)
        val clickNotifications = json.getJSONArray("notifications")
        val notificationObject = clickNotifications.getJSONObject(0)
        assertEquals(1, clickNotifications.length().toLong())
        assertEquals("click", notificationObject.getString("type"))
        assertNotNull(notificationObject.getString("timestamp"))
        assertNotNull(notificationObject.getString("id"))
        assertFalse(notificationObject.has("parameters"))
        assertFalse(notificationObject.has("order"))
        assertFalse(notificationObject.has("product"))
        assertFalse(notificationObject.has("profileParameters"))
    }


    //**********************************************************************************************
    // Location Displayed Tests
    //**********************************************************************************************
    // Test Case No : 32
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetLocationDisplayed() {
        prefetchContent("mbox1")

        val prefetchJson= JSONObject(networkRequestBody)
        assertNotNull(prefetchJson)
        val prefetch = prefetchJson.getJSONObject("prefetch").getJSONArray("mboxes")
        assertEquals(1, prefetch.length().toLong())
        val mbox = prefetch.getJSONObject(0)
        assertEquals("mbox1", mbox.getString("name"))
        var orderParams = mbox.getJSONObject("order")
        assertEquals(3, orderParams.length().toLong())
        assertEquals("SomeOrderID", orderParams.getString("id"))
        assertEquals(4445.12, orderParams.getDouble("total"), 0.001)
        assertEquals("[\"no1\",\"no2\",\"no3\"]", orderParams.getString("purchasedProductIds"))
        var productParams = mbox.getJSONObject("product")
        assertEquals(2, productParams.length().toLong())
        assertEquals("764334", productParams.getString("id"))
        assertEquals("Online", productParams.getString("categoryId"))
        var mboxParams = mbox.getJSONObject("parameters")
        assertEquals("mbox_parameter_value", mboxParams.getString("mbox_parameter_key"))
        var profileParams = mbox.getJSONObject("profileParameters")
        assertEquals("profile_parameter_value", profileParams.getString("profile_parameter_key"))
        assertNull(prefetchErrorStatus)

        // reset
        resetNetworkMonitor()

        // test
        Target.locationsDisplayed(arrayOf("mbox1").toMutableList(), targetParameters)
        waitForNetworkCall?.await(3, TimeUnit.SECONDS)

        // verify location display network call
        val json = JSONObject(networkRequestBody)
        assertNotNull(json)
        val displayNotification = json.getJSONArray("notifications")
        val notificationObject = displayNotification.getJSONObject(0)
        assertEquals(1, displayNotification.length().toLong())
        assertEquals("display", notificationObject.getString("type"))
        assertNotNull(notificationObject.getString("timestamp"))
        assertNotNull(notificationObject.getString("id"))
        val tokenArray = notificationObject.getJSONArray("tokens")
        assertNotNull(tokenArray)
        assertEquals(1, tokenArray.length().toLong())
        assertEquals("RandomDisplayEventToken", tokenArray.getString(0))
        orderParams = notificationObject.getJSONObject("order")
        assertEquals(3, orderParams.length().toLong())
        assertEquals("SomeOrderID", orderParams.getString("id"))
        assertEquals(4445.12, orderParams.getDouble("total"), 0.001)
        assertEquals("[\"no1\",\"no2\",\"no3\"]", orderParams.getString("purchasedProductIds"))
        productParams = notificationObject.getJSONObject("product")
        assertEquals(2, productParams.length().toLong())
        assertEquals("764334", productParams.getString("id"))
        assertEquals("Online", productParams.getString("categoryId"))
        mboxParams = notificationObject.getJSONObject("parameters")
        assertEquals("mbox_parameter_value", mboxParams.getString("mbox_parameter_key"))
        profileParams = notificationObject.getJSONObject("profileParameters")
        assertEquals("profile_parameter_value", profileParams.getString("profile_parameter_key"))
    }

    // Test Case No : 38
    @Test
    @Throws(Exception::class)
    fun test_Functional_Target_targetPrefetchContentWith_targetLocationDisplayed_For_Different_MBox() {
        // setup
        prefetchContent("mbox1")
        val prefetchJson= JSONObject(networkRequestBody)
        assertNotNull(prefetchJson)

        // reset
        resetNetworkMonitor()

        // test
        Target.locationsDisplayed(arrayOf("mbox2").toMutableList(), targetParameters)
        waitForNetworkCall?.await(2, TimeUnit.SECONDS)

        // verify location display network call is not made
        assertNull(networkRequestBody)
    }

    // Test Case No : 39
    @Test
    fun test_Functional_targetLocationDisplayed_After_RetrieveLocationContent_WithTargetParameter() {
        // setup
        retrieveLocationContent("mbox2")
        val networkResponse = JSONObject(networkRequestBody)
        assertNotNull(networkResponse)

        // reset
        resetNetworkMonitor()

        // test
        Target.locationsDisplayed(arrayOf("mbox2").toMutableList(), targetParameters)
        waitForNetworkCall?.await(2, TimeUnit.SECONDS)

        // verify location display network call is not made
        assertNull(networkRequestBody)
    }


    //**********************************************************************************************
    // Setters And Getters test
    //**********************************************************************************************
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetSetAndGetSessionId() {
        // setup
        val sessionId = "66E5C681-4F70-41A2-86AE-F1E151443B10"
        var retrievedSessionId = ""

        Target.getSessionId() { sessionId ->
            retrievedSessionId = sessionId
            waitForCallback?.countDown()
        }
        waitForCallback?.await(5, TimeUnit.SECONDS)

        // verify
        val oldSessionId = retrievedSessionId
        assertNotNull(retrievedSessionId)
        assertNotEquals("", retrievedSessionId)


        // Set a new session Id
        Target.setSessionId(sessionId)

        // verify the newly set sessionId
        waitForCallback = CountDownLatch(1)
        Target.getSessionId() { sessionId ->
            retrievedSessionId = sessionId
            waitForCallback?.countDown()
        }
        waitForCallback?.await(5, TimeUnit.SECONDS)

        // verify
        assertNotNull(retrievedSessionId)
        assertEquals(sessionId, retrievedSessionId)
        assertNotEquals(oldSessionId, retrievedSessionId)
    }


    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetSetAndGetTntId() {
        // setup
        var retrievedTntId : String? = null
        val newTntId= "66E5C681-4F70-41A2-86AE-F1E151443B10.35_0"

        // test
        Target.getTntId() { tntId ->
            retrievedTntId = tntId
            waitForCallback?.countDown()
        }
        waitForCallback?.await(5, TimeUnit.SECONDS)

        // verify
        val oldTntId = retrievedTntId
        assertNull(retrievedTntId)

        // Set a new Tnt Id
        Target.setTntId(newTntId)

        // test
        waitForCallback = CountDownLatch(1)
        Target.getTntId() { tntId ->
            retrievedTntId = tntId
            waitForCallback?.countDown()
        }
        waitForCallback?.await(5, TimeUnit.SECONDS)

        // verify
        assertNotNull(retrievedTntId)
        assertEquals(newTntId, retrievedTntId)
        assertNotEquals(oldTntId, retrievedTntId)
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetSetAndGetThirdPartyId() {
        // setup
        var retrievedThirdPartyId : String? = null
        val newThirdPartyId= "shiningNewId"

        // test
        Target.getThirdPartyId() { thirdPartyId ->
            retrievedThirdPartyId = thirdPartyId
            waitForCallback?.countDown()
        }
        waitForCallback?.await(5, TimeUnit.SECONDS)

        // verify
        val oldThirdPartyId = retrievedThirdPartyId
        assertNull(retrievedThirdPartyId)

        // Set a new Tnt Id
        Target.setThirdPartyId(newThirdPartyId)

        // test
        waitForCallback = CountDownLatch(1)
        Target.getThirdPartyId() { thirdPartyId ->
            retrievedThirdPartyId = thirdPartyId
            waitForCallback?.countDown()
        }
        waitForCallback?.await(5, TimeUnit.SECONDS)

        // verify
        assertNotNull(retrievedThirdPartyId)
        assertEquals(newThirdPartyId, retrievedThirdPartyId)
        assertNotEquals(oldThirdPartyId, retrievedThirdPartyId)
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Session_Target_VerifySetSessionIdAndTntIdAndEdgeHostUsedWhenSendingTargetRequest() {
        // setup
        var retrievedTntId : String? = null
        var retrievedSessionId : String? = null
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null, arrayOf(mboxName),
            targetClientCode, "prefetchedContent",
            "f741a5d5-09c0-4931-bf53-b9e568c5f782.35_0",
            "mboxedge35.tt.omtrdc.net", null,
            null, false, true)
        retrieveLocationContent(mboxName)

        // verify
        assertNotNull(networkRequestUrl)
        val uri = Uri.parse(networkRequestUrl)
        val sessionId1 = uri.getQueryParameter("sessionId")
        assertNotNull(sessionId1)


        // Get and verify tntId
        waitForCallback = CountDownLatch(1)
        Target.getTntId() { tntId ->
            retrievedTntId = tntId
            waitForCallback?.countDown()
        }
        waitForCallback?.await(5, TimeUnit.SECONDS)
        assertEquals("f741a5d5-09c0-4931-bf53-b9e568c5f782.35_0", retrievedTntId)


        // Get and verify sessionId
        waitForCallback = CountDownLatch(1)
        Target.getSessionId() { sessionId ->
            retrievedSessionId = sessionId
            waitForCallback?.countDown()
        }
        waitForCallback?.await(5, TimeUnit.SECONDS)
        assertEquals(sessionId1, retrievedSessionId)

        // test
        Target.setSessionId("5568c1a2-ece1-42d1-b807-930623998ec3")
        Target.setTntId("9093c11c-accd-41a1-9fa7-ea8c50882c41.32_0")

        // make another target call
        resetNetworkMonitor()
        retrieveLocationContent("mbox2")

        //verify if the network request contains the latest ids
        assertNotNull(networkRequestUrl)
        val uri2 = Uri.parse(networkRequestUrl)
        val sessionId2 = uri2.getQueryParameter("sessionId")
        assertNotNull(sessionId2)
        assertEquals("5568c1a2-ece1-42d1-b807-930623998ec3", sessionId2)
        var json = JSONObject(networkRequestBody)
        assertNotNull(json)
        assertEquals("9093c11c-accd-41a1-9fa7-ea8c50882c41.32_0", json.getJSONObject("id").getString("tntId"))
    }

    //**********************************************************************************************
    // Target Raw Request Tests
    //**********************************************************************************************
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetExecuteRawRequest_batch() {
        // setup
        val localLatch = CountDownLatch(1)
        val responseDataList: MutableList<Map<String, Any>> = ArrayList()
        val mboxNames = arrayOf("mbox1")
        val executeMbox1 = mapOf(
            "index" to 0,
            "name" to "mbox1",
            "parameters" to  mboxParameters,
            "profileParameters" to profileParameters,
            "order" to orderParameters,
            "product" to productParameters)
        val executeMboxes: MutableList<Map<String, Any>> = ArrayList()
        executeMboxes.add(executeMbox1)
        val request = mapOf(
            "execute" to mapOf(
                "mboxes" to executeMboxes
            ))

        val a4tPayload = "{\"payload\":{\"pe\":\"tnt\",\"tnta\":\"333911:0:0:0|2|4445.12\"}}"
        val a4tPayloadObject = JSONObject(a4tPayload)
        val mboxClickMetricAnalyticsPayloadString =
            "{\"payload\":{\"pe\":\"tnt\",\"tnta\":\"333911:0:0|32767\"}}"
        val mboxClickMetricPayload = JSONObject(mboxClickMetricAnalyticsPayloadString)

        networkMonitor = { request ->
            networkRequestUrl = request.url
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }


        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null, mboxNames,
            targetClientCode,
            "targetContent",
            "f741a5d5-09c0-4931-bf53-b9e568c5f782.35_0",
            "mboxedge35.tt.omtrdc.net",
            arrayOf(a4tPayloadObject),
            arrayOf(mboxClickMetricPayload),
            false,
            true)

        // test
        Target.executeRawRequest(
            request
        ) { responseData ->
            responseDataList.add(responseData)
            localLatch.countDown()
        }
        localLatch.await(5, TimeUnit.SECONDS)

        // verify request
        assertTrue(networkRequestUrl!!.startsWith("https://adobeobumobile5targe.tt.omtrdc.net/"))
        assertTrue(networkRequestUrl!!.contains("sessionId"))
        var json = JSONObject(networkRequestBody)
        assertNotNull(json)
        assertNotNull(json.optJSONObject("id"))
        assertNotNull(json.optJSONObject("context"))
        assertNotNull(json.optJSONObject("experienceCloud"))
        assertNotNull(json.optString("environmentId", null))
        assertNull(json.optJSONObject("property"))
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())

        // Asserting for mbox1
        val mbox = mboxes.getJSONObject(0)
        assertEquals("mbox1", mbox.getString("name"))
        val parameters = mbox.getJSONObject("parameters")
        assertEquals("mbox_parameter_value", parameters.getString("mbox_parameter_key"))
        val profileParameters = mbox.getJSONObject("profileParameters")
        assertEquals(
            "profile_parameter_value",
            profileParameters.getString("profile_parameter_key")
        )
        val orderParameters = mbox.getJSONObject("order")
        assertEquals(3, orderParameters.length().toLong())
        assertEquals("SomeOrderID", orderParameters.getString("id"))
        assertEquals(4445.12, orderParameters.getDouble("total"), 0.001)
        assertEquals(
            "[\"no1\",\"no2\",\"no3\"]",
            orderParameters.getString("purchasedProductIds")
        )
        val productParameters = mbox.getJSONObject("product")
        assertEquals(2, productParameters.length().toLong())
        assertEquals("764334", productParameters.getString("id"))
        assertEquals("Online", productParameters.getString("categoryId"))

        // Verify server response
        assertEquals(1, responseDataList.size.toLong())
        val responseData = responseDataList[0]
        assertNotNull(responseData)
        assertNotNull(responseData["requestId"] as String?)
        assertEquals("mboxedge35.tt.omtrdc.net", responseData["edgeHost"] as String?)
        assertEquals("adobeobumobile5targe", responseData["client"] as String?)
        val id = responseData["id"] as Map<String, Any>?
        assertNotNull(id)
        assertEquals(1, id!!.size.toLong())
        assertEquals("f741a5d5-09c0-4931-bf53-b9e568c5f782.35_0", id["tntId"] as String?)
        val execute = responseData["execute"] as Map<String, Any>?
        assertNotNull(execute)
        val executeMboxesResponse = execute!!["mboxes"] as List<Map<String, Any>>?
        assertNotNull(executeMboxesResponse)
        assertEquals(1, executeMboxesResponse!!.size.toLong())
        val executeResponseForMbox1 = executeMboxesResponse[0]
        assertEquals(1, executeResponseForMbox1["index"] as Int)
        assertEquals("mbox1", executeResponseForMbox1["name"] as String?)
        val optionsArray = executeResponseForMbox1["options"] as List<Map<String, Any>>?
        assertNotNull(optionsArray)
        assertEquals(1, optionsArray!!.size.toLong())
        val optionsForMbox1 = optionsArray[0]
        assertEquals(2, optionsForMbox1.size.toLong())
        assertEquals("targetContent", optionsForMbox1["content"])
        assertEquals("html", optionsForMbox1["type"])
        val analytics = executeResponseForMbox1["analytics"] as Map<String, Any>?
        assertNotNull(analytics)
        assertEquals(1, analytics!!.size.toLong())
        val analyticsPayloadForMbox1 = analytics["payload"] as Map<String, String>?
        assertEquals(2, analyticsPayloadForMbox1!!.size.toLong())
        assertEquals("tnt", analyticsPayloadForMbox1["pe"])
        assertEquals("333911:0:0:0|2|4445.12", analyticsPayloadForMbox1["tnta"])
        val metricsArray = executeResponseForMbox1["metrics"] as List<Map<String, Any>>?
        assertNotNull(metricsArray)
        assertEquals(1, metricsArray!!.size.toLong())
        val metricsForMbox1 = metricsArray[0]
        assertEquals(3, metricsForMbox1.size.toLong())
        assertEquals("click", metricsForMbox1["type"])
        assertEquals("RandomClickTrackEventToken", metricsForMbox1["eventToken"])
        val metricsAnalytics = metricsForMbox1["analytics"] as Map<String, Any>?
        assertNotNull(metricsAnalytics)
        assertEquals(1, metricsAnalytics!!.size.toLong())
        val metricsAnalyticsPayloadForMbox1 = metricsAnalytics["payload"] as Map<String, String>?
        assertEquals(2, metricsAnalyticsPayloadForMbox1!!.size.toLong())
        assertEquals("tnt", metricsAnalyticsPayloadForMbox1["pe"])
        assertEquals("333911:0:0|32767", metricsAnalyticsPayloadForMbox1["tnta"])
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Target_targetExecuteRawRequest_emptyRequest() {
        val localLatch = CountDownLatch(1)
        var responseData: Map<String, Any>? = emptyMap<String, String>()
        val mboxNames = arrayOf("mbox1")
        val request: Map<String, Any> = HashMap()
        networkMonitor = { request ->
            networkRequestUrl = request.url
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }

        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null, mboxNames,
            targetClientCode,
            "targetContent",
            "f741a5d5-09c0-4931-bf53-b9e568c5f782.35_0",
            "mboxedge35.tt.omtrdc.net",
            null,
            null,
            false,
            true)

        // test
        Target.executeRawRequest(
            request
        ) { response ->
            responseData = response
            localLatch.countDown()
        }
        localLatch.await(5, TimeUnit.SECONDS)

        // verify
        assertNull(responseData)
        assertNull(networkRequestUrl)
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Target_targetExecuteRawRequest_privacyOptedOut() {
        // setup
        val configData: Map<String, Any> = mapOf(
            "global.privacy" to "optedout"
        )
        MobileCore.updateConfiguration(configData)
        Thread.sleep(2000)
        val localLatch = CountDownLatch(1)
        var responseData: Map<String, Any>? = emptyMap<String, String>()
        val mboxNames = arrayOf("mbox1")
        val executeMbox1 = mapOf(
            "index" to 0,
            "name" to "mbox1",
            "parameters" to  mboxParameters,
            "profileParameters" to profileParameters,
            "order" to orderParameters,
            "product" to productParameters)
        val executeMboxes: MutableList<Map<String, Any>> = ArrayList()
        executeMboxes.add(executeMbox1)
        val request = mapOf(
            "execute" to mapOf(
                "mboxes" to executeMboxes
            ))

        // setup network response
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null,
            mboxNames,
            targetClientCode,
            "targetContent",
            "f741a5d5-09c0-4931-bf53-b9e568c5f782.35_0",
            "mboxedge35.tt.omtrdc.net",
            null,
            null,
            false,
            true)
        networkMonitor = { request ->
            networkRequestUrl = request.url
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }

        // test
        Target.executeRawRequest(
            request
        ) { response ->
            responseData = response
            localLatch.countDown()
        }
        localLatch.await(5, TimeUnit.SECONDS)

        // Verify server response
        assertNull(responseData)
        assertNull(networkRequestUrl)
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetSendRawNotifications_forDisplay() {
        // setup
        val localLatch = CountDownLatch(1)
        val responseDataList: MutableList<Map<String, Any>> = ArrayList()
        val mboxNames = arrayOf("mbox1")
        val prefetchMbox1 = mapOf(
            "index" to 0,
            "name" to "mbox1",
            "parameters" to  mboxParameters,
            "profileParameters" to profileParameters)
        val prefetchMboxes: MutableList<Map<String, Any>> = ArrayList()
        prefetchMboxes.add(prefetchMbox1)
        val request = mapOf(
            "prefetch" to mapOf(
                "mboxes" to prefetchMboxes
            ))

        // setup network response
        resetNetworkMonitor()
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null,
            mboxNames,
            targetClientCode,
            "targetContent",
            "f741a5d5-09c0-4931-bf53-b9e568c5f782.35_0",
            "mboxedge35.tt.omtrdc.net",
            null,
            null,
            true,
            true)

        // test
        Target.executeRawRequest(
            request
        ) { responseData ->
            responseDataList.add(responseData)
            localLatch.countDown()
        }
        localLatch.await(5, TimeUnit.SECONDS)

        // verify prefetch raw request
        assertTrue(networkRequestUrl!!.startsWith("https://adobeobumobile5targe.tt.omtrdc.net/"))
        assertTrue(networkRequestUrl!!.contains("sessionId"))
        var json = JSONObject(networkRequestBody)
        assertNotNull(json)
        assertNotNull(json.optJSONObject("id"))
        assertNotNull(json.optJSONObject("context"))
        assertNotNull(json.optJSONObject("experienceCloud"))
        assertNotNull(json.optString("environmentId", null))
        assertNull(json.optJSONObject("property"))
        val mboxes = json.getJSONObject("prefetch").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())

        // Asserting for mbox1
        val mbox = mboxes.getJSONObject(0)
        assertEquals("mbox1", mbox.getString("name"))
        val parameters = mbox.getJSONObject("parameters")
        assertEquals("mbox_parameter_value", parameters.getString("mbox_parameter_key"))
        val profileParameters = mbox.getJSONObject("profileParameters")
        assertEquals(
            "profile_parameter_value",
            profileParameters.getString("profile_parameter_key")
        )

        // Verify server response
        assertEquals(1, responseDataList.size.toLong())
        val responseData = responseDataList[0]
        assertNotNull(responseData)
        val prefetch = responseData["prefetch"] as Map<String, Any>?
        assertNotNull(prefetch)
        val prefetchMboxesResponse = prefetch!!["mboxes"] as List<Map<String, Any>>?
        assertNotNull(prefetchMboxesResponse)
        assertEquals(1, prefetchMboxesResponse!!.size.toLong())
        val prefetchResponseForMbox1 = prefetchMboxesResponse[0]
        assertEquals(1, prefetchResponseForMbox1["index"] as Int)
        assertEquals("mbox1", prefetchResponseForMbox1["name"] as String?)
        val optionsArray = prefetchResponseForMbox1["options"] as List<Map<String, Any>>?
        assertNotNull(optionsArray)
        assertEquals(1, optionsArray!!.size.toLong())
        val optionsForMbox1 = optionsArray[0]
        assertEquals(3, optionsForMbox1.size.toLong())
        assertEquals("targetContent", optionsForMbox1["content"])
        assertEquals("html", optionsForMbox1["type"])
        val displayToken = optionsForMbox1["eventToken"] as String?
        assertEquals("RandomDisplayEventToken", displayToken)

        resetNetworkMonitor()
        val notification = mapOf(
            "id" to "0",
            "timestamp" to System.currentTimeMillis(),
            "type" to "display",
            "mbox" to mapOf(
                "name" to "mbox1"
            ),
            "tokens" to listOf(displayToken),
            "parameters" to  mboxParameters2,
            "profileParameters" to profileParameters2,
            "order" to orderParameters2,
            "product" to productParameters2
        )
        val notificationRequest = mapOf(
            "notifications" to listOf(notification)
        )
        // test
        Target.sendRawNotifications(notificationRequest)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify send raw notification
        json = JSONObject(networkRequestBody)
        assertNotNull(json)
        assertNotNull(json.optJSONObject("id"))
        assertNotNull(json.optJSONObject("context"))
        assertNotNull(json.optJSONObject("experienceCloud"))
        assertNotNull(json.optString("environmentId", null))
        assertNull(json.optJSONObject("property"))
        val clickNotifications = json.getJSONArray("notifications")
        val notificationObject = clickNotifications.getJSONObject(0)
        assertEquals(1, clickNotifications.length().toLong())
        assertEquals("display", notificationObject.getString("type"))
        assertNotNull(notificationObject.getString("timestamp"))
        assertNotNull(notificationObject.getString("id"))
        val tokenArray = notificationObject.getJSONArray("tokens")
        assertNotNull(tokenArray)
        assertEquals(1, tokenArray.length().toLong())
        assertEquals("RandomDisplayEventToken", tokenArray.getString(0))
        val parameters2 = notificationObject.getJSONObject("parameters")
        assertEquals("mbox_parameter_value2", parameters2.getString("mbox_parameter_key2"))
        val profileParameters2 = notificationObject.getJSONObject("profileParameters")
        assertEquals(
            "profile_parameter_value2",
            profileParameters2.getString("profile_parameter_key2")
        )
        val orderParameters2 = notificationObject.getJSONObject("order")
        assertEquals(3, orderParameters2.length().toLong())
        assertEquals("SomeOrderID2", orderParameters2.getString("id"))
        assertEquals(4567.89, orderParameters2.getDouble("total"), 0.001)
        assertEquals("[\"a1\",\"a2\",\"a3\"]", orderParameters2.getString("purchasedProductIds"))
        val productParameters2 = notificationObject.getJSONObject("product")
        assertEquals(2, productParameters2.length().toLong())
        assertEquals("765432", productParameters2.getString("id"))
        assertEquals("Offline", productParameters2.getString("categoryId"))
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_targetSendRawNotifications_forClick() {
        // setup
        val localLatch = CountDownLatch(1)
        val responseDataList: MutableList<Map<String, Any>> = ArrayList()
        val mboxNames = arrayOf("mbox1")
        val executeMbox1 = mapOf(
            "index" to 0,
            "name" to "mbox1",
            "parameters" to  mboxParameters,
            "profileParameters" to profileParameters,
            "order" to orderParameters,
            "product" to productParameters)
        val executeMboxes: MutableList<Map<String, Any>> = ArrayList()
        executeMboxes.add(executeMbox1)
        val request = mapOf(
            "execute" to mapOf(
                "mboxes" to executeMboxes
            ))

        // setup network response
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null,
            mboxNames,
            targetClientCode,
            "targetContent",
            "f741a5d5-09c0-4931-bf53-b9e568c5f782.35_0",
            "mboxedge35.tt.omtrdc.net",
            null,
            null,
            false,
            true)
        networkMonitor = { request ->
            networkRequestUrl = request.url
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }

        // test
        Target.executeRawRequest(
            request
        ) { responseData ->
            responseDataList.add(responseData)
            localLatch.countDown()
        }
        localLatch.await(5, TimeUnit.SECONDS)

        // verify execute raw request
        assertTrue(networkRequestUrl!!.startsWith("https://adobeobumobile5targe.tt.omtrdc.net/"))
        assertTrue(networkRequestUrl!!.contains("sessionId"))
        var json = JSONObject(networkRequestBody)
        assertNotNull(json)
        assertNotNull(json.optJSONObject("id"))
        assertNotNull(json.optJSONObject("context"))
        assertNotNull(json.optJSONObject("experienceCloud"))
        assertNotNull(json.optString("environmentId", null))
        assertNull(json.optJSONObject("property"))
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        assertEquals(1, mboxes.length().toLong())

        // Asserting for mbox1
        val mbox = mboxes.getJSONObject(0)
        assertEquals("mbox1", mbox.getString("name"))
        val parameters = mbox.getJSONObject("parameters")
        assertEquals("mbox_parameter_value", parameters.getString("mbox_parameter_key"))
        val profileParameters = mbox.getJSONObject("profileParameters")
        assertEquals(
            "profile_parameter_value",
            profileParameters.getString("profile_parameter_key")
        )

        // Verify server response
        assertEquals(1, responseDataList.size.toLong())
        val responseData = responseDataList[0]
        assertNotNull(responseData)
        val execute = responseData["execute"] as Map<String, Any>?
        assertNotNull(execute)
        val executeMboxesResponse = execute!!["mboxes"] as List<Map<String, Any>>?
        assertNotNull(executeMboxesResponse)
        assertEquals(1, executeMboxesResponse!!.size.toLong())
        val executeResponseForMbox1 = executeMboxesResponse[0]
        assertEquals(1, executeResponseForMbox1["index"] as Int)
        assertEquals("mbox1", executeResponseForMbox1["name"] as String?)
        val metricsArray = executeResponseForMbox1["metrics"] as List<Map<String, Any>>?
        assertNotNull(metricsArray)
        assertEquals(1, metricsArray!!.size.toLong())
        val metricsForMbox1 = metricsArray[0]
        assertEquals(2, metricsForMbox1.size.toLong())
        assertEquals("click", metricsForMbox1["type"])
        val clickToken = metricsForMbox1["eventToken"] as String?
        assertEquals("RandomClickTrackEventToken", clickToken)

        resetNetworkMonitor()
        val notification = mapOf(
            "id" to "0",
            "timestamp" to System.currentTimeMillis(),
            "type" to "click",
            "mbox" to mapOf(
                "name" to "mbox1"
            ),
            "tokens" to listOf(clickToken),
            "parameters" to  mboxParameters2,
            "profileParameters" to profileParameters2,
            "order" to orderParameters2,
            "product" to productParameters2
        )
        val notificationRequest = mapOf(
            "notifications" to listOf(notification)
        )
        // test
        Target.sendRawNotifications(notificationRequest)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify send raw notification
        json = JSONObject(networkRequestBody)
        assertNotNull(json)
        assertNotNull(json.optJSONObject("id"))
        assertNotNull(json.optJSONObject("context"))
        assertNotNull(json.optJSONObject("experienceCloud"))
        assertNotNull(json.optString("environmentId", null))
        assertNull(json.optJSONObject("property"))
        val clickNotifications = json.getJSONArray("notifications")
        val notificationObject = clickNotifications.getJSONObject(0)
        assertEquals(1, clickNotifications.length().toLong())
        assertEquals("click", notificationObject.getString("type"))
        assertNotNull(notificationObject.getString("timestamp"))
        assertNotNull(notificationObject.getString("id"))
        val tokenArray = notificationObject.getJSONArray("tokens")
        assertNotNull(tokenArray)
        assertEquals(1, tokenArray.length().toLong())
        assertEquals("RandomClickTrackEventToken", tokenArray.getString(0))
        val parameters2 = notificationObject.getJSONObject("parameters")
        assertEquals("mbox_parameter_value2", parameters2.getString("mbox_parameter_key2"))
        val profileParameters2 = notificationObject.getJSONObject("profileParameters")
        assertEquals(
            "profile_parameter_value2",
            profileParameters2.getString("profile_parameter_key2")
        )
        val orderParameters2 = notificationObject.getJSONObject("order")
        assertEquals(3, orderParameters2.length().toLong())
        assertEquals("SomeOrderID2", orderParameters2.getString("id"))
        assertEquals(4567.89, orderParameters2.getDouble("total"), 0.001)
        assertEquals("[\"a1\",\"a2\",\"a3\"]", orderParameters2.getString("purchasedProductIds"))
        val productParameters2 = notificationObject.getJSONObject("product")
        assertEquals(2, productParameters2.length().toLong())
        assertEquals("765432", productParameters2.getString("id"))
        assertEquals("Offline", productParameters2.getString("categoryId"))
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Target_targetSendRawNotifications_noClientCodeConfigured() {
        // setup
        updateConfiguration(mapOf(
            "target.clientCode" to ""
        ))

        val notification = mapOf(
            "id" to "0",
            "timestamp" to System.currentTimeMillis(),
            "type" to "click",
            "mbox" to mapOf(
                "name" to "mbox1"
            ),
            "tokens" to listOf("RandomClickTrackEventToken"),
            "parameters" to  mboxParameters2,
            "profileParameters" to profileParameters2,
            "order" to orderParameters2,
            "product" to productParameters2
        )
        val notificationRequest = mapOf(
            "notifications" to listOf(notification)
        )
        // test
        Target.sendRawNotifications(notificationRequest)

        // verify no request is sent
        assertNull(networkRequestBody)
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Target_targetSendRawNotifications_emptyRequest() {
        // setup
        Target.sendRawNotifications(HashMap())
        waitForNetworkCall?.await(2, TimeUnit.SECONDS)

        // verify
        assertNull(networkRequestBody)
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Target_targetSendRawNotifications_withPropertyTokenInRequest() {
        // setup
        resetNetworkMonitor()
        val notification = mapOf(
            "id" to "0",
            "timestamp" to System.currentTimeMillis(),
            "type" to "click",
            "mbox" to mapOf(
                "name" to "mbox1"
            ),
            "tokens" to listOf("RandomClickTrackEventToken"),
            "parameters" to  mboxParameters2,
            "profileParameters" to profileParameters2,
            "order" to orderParameters2,
            "product" to productParameters2
        )

        val notificationRequest = mapOf(
            "notifications" to listOf(notification),
            "property" to mapOf(
                "token" to "requestPropertyToken"
            ))

        // test
        Target.sendRawNotifications(notificationRequest)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify send raw notification
        val json = JSONObject(networkRequestBody)
        assertNotNull(json)
        val propertyObject = json.getJSONObject("property")
        assertEquals(1, propertyObject.length().toLong())
        assertEquals("requestPropertyToken", propertyObject.getString("token"))
        val clickNotifications = json.getJSONArray("notifications")
        val notificationObject = clickNotifications.getJSONObject(0)
        assertEquals(1, clickNotifications.length().toLong())
        assertEquals("click", notificationObject.getString("type"))
        assertNotNull(notificationObject.getString("timestamp"))
        assertNotNull(notificationObject.getString("id"))
        val tokenArray = notificationObject.getJSONArray("tokens")
        assertNotNull(tokenArray)
        assertEquals(1, tokenArray.length().toLong())
        assertEquals("RandomClickTrackEventToken", tokenArray.getString(0))
        val parameters2 = notificationObject.getJSONObject("parameters")
        assertEquals("mbox_parameter_value2", parameters2.getString("mbox_parameter_key2"))
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Target_targetSendRawNotifications_withPropertyTokenInRequestAndInConfiguration() {
        // setup
        resetNetworkMonitor()
        updateConfiguration(mapOf(
            "target.propertyToken" to "configPropertyToken"
        ))
        val notification = mapOf(
            "id" to "0",
            "timestamp" to System.currentTimeMillis(),
            "type" to "click",
            "mbox" to mapOf(
                "name" to "mbox1"
            ),
            "tokens" to listOf("RandomClickTrackEventToken"),
            "parameters" to  mboxParameters2,
            "profileParameters" to profileParameters2,
            "order" to orderParameters2,
            "product" to productParameters2
        )

        val notificationRequest = mapOf(
            "notifications" to listOf(notification),
            "property" to mapOf(
                "token" to "requestPropertyToken"
            ))
        Thread.sleep(1000)

        // test
        Target.sendRawNotifications(notificationRequest)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)

        // verify send raw notification
        val json = JSONObject(networkRequestBody)
        assertNotNull(json)
        val propertyObject = json.getJSONObject("property")
        assertEquals(1, propertyObject.length().toLong())
        assertEquals("configPropertyToken", propertyObject.getString("token"))

        // reset the updated configuration
        // so that it doesn't get carry forwarded to other tests
        updateConfiguration(mapOf(
            "target.propertyToken" to null
        ))
    }

    //**********************************************************************************************
    // Attach Data Tests
    //**********************************************************************************************
    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_VerifyDataAttachedToRetrieveLocationContentRequest() {
        // setup
        updateConfiguration(mapOf(
            "rules.url" to "https://assets.adobedtm.com/94f571f308d5/36109c91f05a/launch-025ab77d2925-development-rules.zip"
        ))
        Thread.sleep(1000)

        // test
        resetNetworkMonitor()
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null, arrayOf(mboxName),
            targetClientCode,
            "targetContent",
            null,
            null,
            null,
            null,
            false,
            true)
        retrieveLocationContent(mboxName)

        // verify
        val json= JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        val mbox = mboxes.getJSONObject(0)
        val profileParams = mbox.getJSONObject("profileParameters")
        assertEquals("male", profileParams.getString("gender"))
        assertEquals("targetContent", retrievedLocationResponse)

        // clear the rules
        resetRules()
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Target_VerifyDataNotAttachedIfKeyAlreadyExists() {
        // setup
        updateConfiguration(mapOf(
            "rules.url" to "https://assets.adobedtm.com/94f571f308d5/36109c91f05a/launch-025ab77d2925-development-rules.zip"
        ))
        Thread.sleep(1000)

        // test
        resetNetworkMonitor()
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null, arrayOf(mboxName),
            targetClientCode,
            "targetContent",
            null,
            null,
            null,
            null,
            false,
            true)
        val targetParameters = TargetParameters.Builder()
            .profileParameters(mapOf("gender" to "female"))
            .build()
        retrieveLocationContent(mboxName, targetParameters)

        // verify
        val json= JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("execute").getJSONArray("mboxes")
        val mbox = mboxes.getJSONObject(0)
        val profileParams = mbox.getJSONObject("profileParameters")
        assertEquals("female", profileParams.getString("gender"))
        assertEquals("targetContent", retrievedLocationResponse)

        // clear the rules
        resetRules()
    }

    @Test
    @Throws(Exception::class)
    fun test_Functional_Happy_Target_VerifyDataAttachedToPrefetchRequest() {
        // setup
        updateConfiguration(mapOf(
            "rules.url" to "https://assets.adobedtm.com/94f571f308d5/36109c91f05a/launch-025ab77d2925-development-rules.zip"
        ))
        Thread.sleep(1000)

        // test
        resetNetworkMonitor()
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null, arrayOf(mboxName),
            targetClientCode,
            "targetContent",
            null,
            null,
            null,
            null,
            true,
            true)
        prefetchContent(mboxName)

        // verify
        val json= JSONObject(networkRequestBody)
        val mboxes = json.getJSONObject("prefetch").getJSONArray("mboxes")
        val mbox = mboxes.getJSONObject(0)
        val profileParams = mbox.getJSONObject("profileParameters")
        assertEquals("male", profileParams.getString("gender"))
        assertNull(prefetchErrorStatus)

        // clear the rules
        resetRules()
    }

    //**********************************************************************************************
    // Private methods
    //**********************************************************************************************
    private fun updateConfiguration(config:  Map<String,Any?>) {
        val configurationLatch = CountDownLatch(1)
        configurationAwareness { configurationLatch.countDown() }
        MobileCore.updateConfiguration(config)
        assertTrue(configurationLatch.await(5000, TimeUnit.MILLISECONDS))
    }

    private fun resetRules() {
        updateConfiguration(mapOf(
            "rules.url" to ""
        ))
    }

    private fun retrieveLocationContent(mboxName: String, targetParams: TargetParameters = targetParameters) {
        waitForCallback = CountDownLatch(1)
        retrievedLocationResponse = null

        resetNetworkMonitor()

        val targetRequest = TargetRequest(mboxName, targetParams , defaultContent) { data ->
            retrievedLocationResponse = data
            waitForCallback?.countDown()
        }

        // test
        Target.retrieveLocationContent(listOf(targetRequest), targetParams)
        waitForCallback?.await(5, TimeUnit.SECONDS)
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)
    }

    private fun prefetchContent(mboxName: String) {
        waitForCallback = CountDownLatch(1)
        prefetchErrorStatus = null
        resetNetworkMonitor()
        mockedNetworkResponse = TargetTestHelper.getResponseForTarget(null, arrayOf(mboxName),
            targetClientCode, "prefetchedContent", null, null, null,
            null, true, true)

        val targetPrefetchList = listOf(TargetPrefetch(mboxName, targetParameters))
        Target.prefetchContent(targetPrefetchList, targetParameters) { status ->
            prefetchErrorStatus = status
            waitForCallback?.countDown()
        }
        waitForNetworkCall?.await(5, TimeUnit.SECONDS)
        waitForCallback?.await(5, TimeUnit.SECONDS)

        assertNull(prefetchErrorStatus)
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
            "target.timeout" to 5,
            "experienceCloud.org" to "972C898555E9F7BC7F000101@AdobeOrg",
            "experienceCloud.server" to "identity.net"
        )
    }

    private fun configurationAwareness(callback: ConfigurationMonitor) {
        MonitorExtension.configurationAwareness(callback)
    }

    private fun resetNetworkMonitor() {
        waitForNetworkCall = CountDownLatch(1)
        networkRequestBody = null
        networkRequestUrl = null

        networkMonitor = { request ->
            networkRequestUrl = request.url
            networkRequestBody = String(request.body, Charsets.UTF_8)
            waitForNetworkCall?.countDown()
        }
    }

    private class MockedHttpConnecting(val responseStream: InputStream? = mockedNetworkResponse) :
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