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

import com.adobe.marketing.mobile.util.StringUtils
import java.io.InputStream
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal object TargetTestHelper {
    // ================================================================================
    // target constants
    // ================================================================================
    private const val TARGET_API_JSON_MBOX = "name"
    private const val TARGET_API_JSON_INDEX = "index"
    private const val TARGET_API_JSON_MBOXES = "mboxes"
    private const val PREFETCH_MBOX_RESPONSES = "prefetch"
    private const val MBOX_RESPONSES = "execute"
    private const val TARGET_API_JSON_NOTIFICATIONS = "notifications"
    private const val TARGET_API_JSON_VIEW_TIMESTAMP = "timestamp"
    const val TARGET_API_JSON_PROPERTY = "property"
    const val TARGET_API_JSON_TOKEN = "token"

    // ================================================================================
    // response json variable names
    // ================================================================================
    private const val TARGET_API_JSON_CONTENT = "content"
    private const val TARGET_API_JSON_OPTIONS = "options"
    private const val TARGET_API_JSON_ANALYTICS = "analytics"
    private const val TARGET_API_JSON_ANALYTICS_PAYLOAD_PE = "pe"
    private const val TARGET_API_JSON_ANALYTICS_PAYLOAD_PE_VALUE = "tnt"
    private const val TARGET_API_JSON_ANALYTICS_PAYLOAD_TNTA = "tnta"
    private const val TARGET_API_JSON_PAYLOAD = "payload"
    private const val TARGET_API_JSON_METRICS = "metrics"
    private const val TARGET_API_JSON_TYPE = "type"
    private const val TARGET_API_JSON_TYPE_HTML = "html"
    private const val TARGET_API_JSON_TYPE_JSON = "json"
    private const val TARGET_API_JSON_TYPE_CLICK = "click"
    private const val TARGET_API_JSON_EVENT_TOKEN = "eventToken"
    private const val TARGET_API_JSON_EVENT_TOKEN_DISPLAY = "RandomDisplayEventToken"
    private const val TARGET_API_JSON_EVENT_TOKEN_CLICK = "RandomClickTrackEventToken"
    private const val TARGET_API_JSON_RESPONSE_TOKEN = "responseTokens"

    // ================================================================================
    // request & response json variable names
    // ================================================================================
    private const val ID = "id"
    private const val ID_MARKETING_CLOUD_VISITOR_ID = "marketingCloudVisitorId"
    private const val TARGET_API_JSON_TNT_ID = "tntId"
    private const val TARGET_API_JSON_THIRD_PARTY_ID = "thirdPartyId"
    private const val TARGET_API_JSON_EDGE_HOST = "edgeHost"
    private const val TARGET_CLIENT = "client"
    private const val TARGET_REQUEST_ID = "requestId"
    private const val TARGET_ERROR_MESSAGE = "message"
    private const val TARGET_REQUEST_ID_VALUE = "95ba3643-ea07-4ed8-a422-c5d1a0c4b145"

    //======================================================================================================================================
    // target helper methods
    //======================================================================================================================================
    @JvmStatic
    fun getResponseForTarget(
        errorMessage: String?,
        mboxNames: Array<String>,
        clientCode: String?,
        content: String?,
        tntId: String?,
        edgeHost: String?,
        a4tPayloads: Array<JSONObject?>?,
        clickMetricAnalyticPayloads: Array<JSONObject?>?,
        isPrefetch: Boolean,
        isClickMetricAdded: Boolean
    ) : InputStream? {
        // val targetRequestMatcher = E2ERequestMatcher(".tt.omtrdc.net/rest/v1/delivery/")
        val responseString: String?
        var prefetchResponses: JSONArray? = JSONArray()
        var mboxResponses: JSONArray? = JSONArray()
        val mBoxResponseMap = getMBoxesResponseMap(content)

        //create mbox responses for each mbox loaded/prefetched. two paths are needed due to a4t payload.
        if (a4tPayloads != null) {
            for (i in mboxNames.indices) {
                val a4tToInsert = a4tPayloads[i]
                var contentObject: Any? = content
                var contentType: String? = null
                var responseTokens: JSONObject? = null
                if (mBoxResponseMap.size > 0 && mBoxResponseMap.containsKey(mboxNames[i])) {
                    val mBoxObject = mBoxResponseMap[mboxNames[i]]
                    val optionsArray = mBoxObject!!.optJSONArray(TARGET_API_JSON_OPTIONS)
                    var optionsObject: JSONObject? = null
                    if (optionsArray != null && optionsArray.length() > 0) {
                        optionsObject = optionsArray.optJSONObject(0)
                        if (optionsObject != null) {
                            contentObject = optionsObject.opt(TARGET_API_JSON_CONTENT)
                            contentType = optionsObject.optString(
                                TARGET_API_JSON_TYPE,
                                TARGET_API_JSON_TYPE_HTML
                            )
                            responseTokens = optionsObject.optJSONObject(
                                TARGET_API_JSON_RESPONSE_TOKEN
                            )
                        }
                    }
                }
                var clickMetricAnalyticsPayload: JSONObject? = null
                if (isClickMetricAdded && clickMetricAnalyticPayloads != null) {
                    clickMetricAnalyticsPayload = clickMetricAnalyticPayloads[i]
                }
                val mbox = createMBoxResponse(
                    mboxNames[i], contentObject, contentType, a4tToInsert, responseTokens,
                    isPrefetch, isClickMetricAdded, clickMetricAnalyticsPayload, i + 1
                )
                if (isPrefetch) {
                    prefetchResponses!!.put(mbox)
                } else {
                    mboxResponses!!.put(mbox)
                }
            }
        } else {
            for (i in mboxNames.indices) {
                var contentObject: Any? = content
                var contentType: String? = null
                var responseTokens: JSONObject? = null
                if (mBoxResponseMap.size > 0 && mBoxResponseMap.containsKey(mboxNames[i])) {
                    val mBoxObject = mBoxResponseMap[mboxNames[i]]
                    try {
                        contentObject =
                            mBoxObject!!.optJSONArray(TARGET_API_JSON_OPTIONS)?.getJSONObject(0)
                                ?.opt(
                                    TARGET_API_JSON_CONTENT
                                )
                        contentType =
                            mBoxObject.optJSONArray(TARGET_API_JSON_OPTIONS)?.getJSONObject(0)
                                ?.optString(
                                    TARGET_API_JSON_TYPE,
                                    TARGET_API_JSON_TYPE_HTML
                                )
                        responseTokens =
                            mBoxObject.optJSONArray(TARGET_API_JSON_OPTIONS)?.getJSONObject(0)
                                ?.optJSONObject(
                                    TARGET_API_JSON_RESPONSE_TOKEN
                                )
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                var clickMetricAnalyticsPayload: JSONObject? = null
                if (isClickMetricAdded && clickMetricAnalyticPayloads != null) {
                    clickMetricAnalyticsPayload = clickMetricAnalyticPayloads[i]
                }
                val mbox = createMBoxResponse(
                    mboxNames[i], contentObject, contentType, null, responseTokens, isPrefetch,
                    isClickMetricAdded, clickMetricAnalyticsPayload, i + 1
                )
                if (isPrefetch) {
                    prefetchResponses!!.put(mbox)
                } else {
                    mboxResponses!!.put(mbox)
                }
            }
        }

        //prevent creation of an empty prefetch or mbox node in the response string.
        if (prefetchResponses!!.length() == 0) {
            prefetchResponses = null
        }
        if (mboxResponses!!.length() == 0) {
            mboxResponses = null
        }

        //create the response string and set the response in the testable network service
        responseString = createTargetResponseString(
            errorMessage, clientCode, prefetchResponses, mboxResponses, edgeHost, tntId,
            null, null
        )
        return responseString?.byteInputStream() ?: null
    }

    private fun createMBoxResponse(
        mboxName: String?,
        content: Any?,
        contentType: String?,
        a4TPayload: JSONObject?,
        responseToken: JSONObject?,
        isPreFetch: Boolean,
        isClickMetricAdded: Boolean,
        clickMetricA4TPayload: JSONObject?,
        index: Int
    ): JSONObject? {
        return try {
            val response = JSONObject()
            if (mboxName != null) {
                response.put(TARGET_API_JSON_MBOX, mboxName)
                response.put(TARGET_API_JSON_INDEX, index)
            }
            if (content != null) {
                val options = JSONArray()
                val optionsContent = JSONObject()
                if (contentType != null && contentType == TARGET_API_JSON_TYPE_JSON) {
                    optionsContent.put(TARGET_API_JSON_CONTENT, content as JSONObject?)
                    optionsContent.put(TARGET_API_JSON_TYPE, TARGET_API_JSON_TYPE_JSON)
                } else {
                    optionsContent.put(TARGET_API_JSON_CONTENT, content.toString())
                    optionsContent.put(TARGET_API_JSON_TYPE, TARGET_API_JSON_TYPE_HTML)
                }
                if (isPreFetch) {
                    optionsContent.put(
                        TARGET_API_JSON_EVENT_TOKEN,
                        TARGET_API_JSON_EVENT_TOKEN_DISPLAY
                    )
                }
                if (responseToken != null) {
                    optionsContent.put(TARGET_API_JSON_RESPONSE_TOKEN, responseToken)
                }
                options.put(optionsContent)
                response.put(TARGET_API_JSON_OPTIONS, options)
            }
            if (isClickMetricAdded) {
                val metrics = JSONArray()
                val metricsContent = JSONObject()
                metricsContent.put(TARGET_API_JSON_TYPE, TARGET_API_JSON_TYPE_CLICK)
                metricsContent.put(TARGET_API_JSON_EVENT_TOKEN, TARGET_API_JSON_EVENT_TOKEN_CLICK)
                if (clickMetricA4TPayload != null) {
                    metricsContent.put(TARGET_API_JSON_ANALYTICS, clickMetricA4TPayload)
                }
                metrics.put(metricsContent)
                response.put(TARGET_API_JSON_METRICS, metrics)
            }
            if (a4TPayload != null) {
                val analyticsPayload = JSONObject()
                analyticsPayload.put(
                    TARGET_API_JSON_ANALYTICS_PAYLOAD_PE,
                    TARGET_API_JSON_ANALYTICS_PAYLOAD_PE_VALUE
                )
                analyticsPayload.put(TARGET_API_JSON_ANALYTICS_PAYLOAD_TNTA, a4TPayload)
                val analytics = JSONObject()
                analytics.put(TARGET_API_JSON_PAYLOAD, analyticsPayload)
                response.put(TARGET_API_JSON_ANALYTICS, a4TPayload)
            }
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createTargetResponseString(
        errorMessage: String?, clientCode: String?,
        prefetchArray: JSONArray?,
        mboxArray: JSONArray?,
        edgeHost: String?, tntId: String?, thirdPartyId: String?, marketingCloudID: String?
    ): String? {
        return try {
            val response = JSONObject()
            if (errorMessage != null && !errorMessage.isEmpty()) {
                response.put(TARGET_ERROR_MESSAGE, errorMessage)
                return response.toString()
            }
            response.put(TARGET_REQUEST_ID, TARGET_REQUEST_ID_VALUE)
            if (clientCode != null) {
                response.put(TARGET_CLIENT, clientCode)
            }

            // create id node
            val idNode = JSONObject()
            if (tntId != null) {
                idNode.put(TARGET_API_JSON_TNT_ID, tntId)
            }
            if (thirdPartyId != null) {
                idNode.put(TARGET_API_JSON_THIRD_PARTY_ID, thirdPartyId)
            }
            if (marketingCloudID != null) {
                idNode.put(ID_MARKETING_CLOUD_VISITOR_ID, marketingCloudID)
            }
            response.put(ID, idNode)
            if (edgeHost != null) {
                response.put(TARGET_API_JSON_EDGE_HOST, edgeHost)
            }

            // Create a prefetch Node if needed
            if (prefetchArray != null) {
                val prefetch = JSONObject()
                prefetch.put(TARGET_API_JSON_MBOXES, prefetchArray)
                response.put(PREFETCH_MBOX_RESPONSES, prefetch)
            }

            // Create a execute Node if needed
            if (mboxArray != null) {
                val execute = JSONObject()
                execute.put(TARGET_API_JSON_MBOXES, mboxArray)
                response.put(MBOX_RESPONSES, execute)
            }
            response.toString()
        } catch (ex: JSONException) {
            null
        }
    }

    private fun getMBoxesResponseMap(responseContent: String?): Map<String, JSONObject> {
        val mBoxResponseHashMap: MutableMap<String, JSONObject> = HashMap()
        if (responseContent == null) {
            return mBoxResponseHashMap
        }
        var contentJsonObject: JSONObject? = null
        contentJsonObject = try {
            JSONObject(responseContent)
        } catch (e: JSONException) {
            return mBoxResponseHashMap
        }
        if (contentJsonObject == null) {
            return mBoxResponseHashMap
        }
        val mboxJSONArray = contentJsonObject.optJSONArray(TARGET_API_JSON_MBOXES)
            ?: return mBoxResponseHashMap
        for (i in 0 until mboxJSONArray.length()) {
            val mboxNode = mboxJSONArray.optJSONObject(i)
            if (mboxNode == null || StringUtils.isNullOrEmpty(
                    mboxNode.optString(
                        TARGET_API_JSON_MBOX, ""
                    )
                )
            ) {
                continue
            }
            val mboxName = mboxNode.optString(TARGET_API_JSON_MBOX, "")
            mBoxResponseHashMap[mboxName] = mboxNode
        }
        return mBoxResponseHashMap
    }

    /**
     * Get the query from key : value pair in the map from url string
     */
    @JvmStatic
    fun getQueryMap(query: String): Map<String, String> {
        val params = query.split("&").toTypedArray()
        val map: MutableMap<String, String> = HashMap()
        for (param in params) {
            val name = param.split("=").toTypedArray()[0]
            val value = param.split("=").toTypedArray()[1]
            map[name] = value
        }
        return map
    }

    @JvmStatic
    fun readInputStreamFromFile(
        fileName: String,
    ) : InputStream? {
        return this::class.java.classLoader?.getResource("${fileName}.zip")
                ?.openStream()!!
    }
}
