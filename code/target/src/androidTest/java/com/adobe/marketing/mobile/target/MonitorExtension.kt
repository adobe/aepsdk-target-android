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

import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.Extension
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.SharedStateResolution

internal typealias StateMonitor = (validState: Map<String, Any>) -> Unit

internal class MonitorExtension(extensionApi: ExtensionApi) : Extension(extensionApi) {
    companion object {
        private var stateMonitor: StateMonitor? = null
        private var stateOwner: String? = null
        internal fun stateAwareness(stateOwner:String, callback: StateMonitor) {
            this.stateOwner = stateOwner
            stateMonitor = callback
        }

        internal fun reset() {
            stateOwner = null
            stateMonitor = null
        }
    }

    override fun getName(): String {
        return "MonitorExtension"
    }

    override fun onRegistered() {
        api.registerEventListener(EventType.WILDCARD, EventSource.WILDCARD) { event ->
            val result = stateOwner?.let {
                api.getSharedState(
                        it,
                    event,
                    false,
                    SharedStateResolution.ANY
                )
            }
            val state = result?.value
            state?.let {
                stateMonitor?.let { it(state) }
            }
        }
    }
}