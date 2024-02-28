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

package com.adobe.marketing.mobile.target;

import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.services.NamedCollection;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TargetStateTests {

    @Mock NamedCollection mockedDataStore;

    TargetState targetState;

    @Before()
    public void beforeEach() {
        targetState = new TargetState(mockedDataStore);
    }

    // ===================================
    // Test updateConfigurationSharedState
    // ===================================
    @Test
    public void testUpdateConfigurationSharedState() {
        Assert.assertNull(targetState.getStoredConfigurationSharedState());

        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                    }
                };

        // test
        targetState.updateConfigurationSharedState(configuration);

        // verify
        Assert.assertEquals(configuration, targetState.getStoredConfigurationSharedState());
    }

    @Test
    public void testUpdateConfigurationSharedState_WhenNewClientCode() {
        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                    }
                };

        // test
        targetState.updateEdgeHost("edge-host-1");
        targetState.updateSessionTimestamp(false);
        targetState.updateConfigurationSharedState(configuration);

        // verify
        Assert.assertEquals(configuration, targetState.getStoredConfigurationSharedState());
        Assert.assertEquals("edge-host-1", targetState.getEdgeHost());
    }

    @Test
    public void testUpdateConfigurationSharedState_WhenNewClientCode_WithStoredConfiguration() {
        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                    }
                };
        Map<String, Object> newConfiguration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_456");
                        put("global.privacy", "optedin");
                    }
                };

        // test
        targetState.updateConfigurationSharedState(configuration);
        targetState.updateEdgeHost("edge-host-1");
        targetState.updateConfigurationSharedState(newConfiguration);

        // verify
        Assert.assertEquals(newConfiguration, targetState.getStoredConfigurationSharedState());
        Assert.assertNull(targetState.getEdgeHost());
    }

    @Test
    public void testUpdateConfigurationSharedState_WhenConfigurationNullOrEmpty() {
        // test
        targetState.updateConfigurationSharedState(null);

        // verify
        Assert.assertNull(targetState.getStoredConfigurationSharedState());

        // test
        targetState.updateConfigurationSharedState(new HashMap<>());

        // verify
        Assert.assertNull(targetState.getStoredConfigurationSharedState());
    }

    @Test
    public void
            testUpdateConfigurationSharedState_WhenConfigurationNullOrEmpty_WithStoredConfiguration() {
        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                    }
                };

        // test
        targetState.updateConfigurationSharedState(configuration);

        // verify
        Assert.assertEquals(configuration, targetState.getStoredConfigurationSharedState());

        // test
        targetState.updateConfigurationSharedState(null);

        // verify
        Assert.assertEquals(configuration, targetState.getStoredConfigurationSharedState());

        // test
        targetState.updateConfigurationSharedState(new HashMap<>());

        // verify
        Assert.assertEquals(configuration, targetState.getStoredConfigurationSharedState());
    }

    // ===================================
    // Test updateConfigurationSharedState
    // ===================================
    @Test
    public void testPrivacyStatus() {
        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                    }
                };

        // test
        targetState.updateConfigurationSharedState(configuration);

        // verify
        Assert.assertEquals(MobilePrivacyStatus.OPT_IN, targetState.getMobilePrivacyStatus());

        // setup
        Map<String, Object> newConfiguration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedout");
                    }
                };
        targetState.updateConfigurationSharedState(newConfiguration);

        // verify
        Assert.assertEquals(MobilePrivacyStatus.OPT_OUT, targetState.getMobilePrivacyStatus());
    }

    // ===================================
    // Test getSessionTimeout
    // ===================================
    @Test
    public void testSessionTimeout() {
        Assert.assertEquals(
                TargetTestConstants.DEFAULT_TARGET_SESSION_TIMEOUT_SEC,
                targetState.getSessionTimeout());
        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                        put("target.sessionTimeout", 1500);
                    }
                };

        // test
        targetState.updateConfigurationSharedState(configuration);

        // verify
        Assert.assertEquals(1500, targetState.getSessionTimeout());

        // setup
        Map<String, Object> newConfiguration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_456");
                        put("global.privacy", "optedout");
                        put("target.sessionTimeout", 1200);
                    }
                };
        targetState.updateConfigurationSharedState(newConfiguration);

        // verify
        Assert.assertEquals(1200, targetState.getSessionTimeout());
    }

    // ===================================
    // Test getClientCode
    // ===================================
    @Test
    public void testClientCode() {
        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                    }
                };

        // test
        targetState.updateConfigurationSharedState(configuration);

        // verify
        Assert.assertEquals("code_123", targetState.getClientCode());

        // setup
        Map<String, Object> newConfiguration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_456");
                        put("global.privacy", "optedout");
                    }
                };
        targetState.updateConfigurationSharedState(newConfiguration);

        // verify
        Assert.assertEquals("code_456", targetState.getClientCode());
    }

    // ===================================
    // Test getEnvironmentId
    // ===================================
    @Test
    public void testEnvironmentId() {
        Assert.assertEquals(0, targetState.getEnvironmentId());

        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                        put("target.environmentId", 45);
                    }
                };

        // test
        targetState.updateConfigurationSharedState(configuration);

        // verify
        Assert.assertEquals(45, targetState.getEnvironmentId());

        // setup
        Map<String, Object> newConfiguration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedout");
                        put("target.environmentId", 50);
                    }
                };
        targetState.updateConfigurationSharedState(newConfiguration);

        // verify
        Assert.assertEquals(50, targetState.getEnvironmentId());
    }

    // ===================================
    // Test getPropertyToken
    // ===================================
    @Test
    public void testPropertyToken() {
        Assert.assertEquals("", targetState.getPropertyToken());

        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                        put("target.environmentId", 45);
                        put("target.propertyToken", "configAtProperty");
                    }
                };

        // test
        targetState.updateConfigurationSharedState(configuration);

        // verify
        Assert.assertEquals("configAtProperty", targetState.getPropertyToken());

        // setup
        Map<String, Object> newConfiguration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedout");
                        put("target.environmentId", 50);
                        put("target.propertyToken", "configAtPropertyNew");
                    }
                };
        targetState.updateConfigurationSharedState(newConfiguration);

        // verify
        Assert.assertEquals("configAtPropertyNew", targetState.getPropertyToken());
    }

    // ===================================
    // Test getTargetServer
    // ===================================
    @Test
    public void testTargetServer() {
        Assert.assertEquals("", targetState.getTargetServer());

        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                        put("target.environmentId", 45);
                        put("target.propertyToken", "configAtProperty");
                    }
                };

        // test
        targetState.updateConfigurationSharedState(configuration);

        // verify
        Assert.assertEquals("configAtProperty", targetState.getPropertyToken());

        // setup
        Map<String, Object> newConfiguration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedout");
                        put("target.environmentId", 50);
                        put("target.propertyToken", "configAtPropertyNew");
                    }
                };
        targetState.updateConfigurationSharedState(newConfiguration);

        // verify
        Assert.assertEquals("configAtPropertyNew", targetState.getPropertyToken());
    }

    // ===================================
    // Test getNetworkTimeout
    // ===================================
    @Test
    public void testNetworkTimeout() {
        Assert.assertEquals(
                TargetTestConstants.DEFAULT_NETWORK_TIMEOUT, targetState.getNetworkTimeout());

        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                        put("target.timeout", 10);
                    }
                };

        // test
        targetState.updateConfigurationSharedState(configuration);

        // verify
        Assert.assertEquals(10, targetState.getNetworkTimeout());

        // setup
        Map<String, Object> newConfiguration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedout");
                        put("target.timeout", 5);
                    }
                };
        targetState.updateConfigurationSharedState(newConfiguration);

        // verify
        Assert.assertEquals(5, targetState.getNetworkTimeout());
    }

    // ===================================
    // Test getSessionId updateSessionId
    // ===================================
    @Test
    public void testSessionId() {
        Assert.assertEquals(
                TargetTestConstants.DEFAULT_NETWORK_TIMEOUT, targetState.getNetworkTimeout());

        // setup
        String sessionId = "mockSessionId";

        // test
        targetState.updateSessionId(sessionId);

        // verify
        Assert.assertEquals(sessionId, targetState.getSessionId());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID),
                        Mockito.eq(sessionId));
    }

    @Test
    public void testSessionId_WhenUpdatedSessionIdNull() {
        // setup
        String sessionId = "mockSessionId";

        // test
        targetState.updateSessionId(sessionId);

        // verify
        Assert.assertEquals(sessionId, targetState.getSessionId());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID),
                        Mockito.eq(sessionId));

        // test
        targetState.updateSessionId(null);

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID));
        Assert.assertNotNull(targetState.getSessionId());
        Assert.assertNotEquals("", targetState.getSessionId());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID),
                        AdditionalMatchers.not(Mockito.eq(sessionId)));
    }

    @Test
    public void testSessionId_WhenUpdatedSessionIdEmpty() {
        // setup
        String sessionId = "mockSessionId";

        // test
        targetState.updateSessionId(sessionId);

        // verify
        Assert.assertEquals(sessionId, targetState.getSessionId());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID),
                        Mockito.eq(sessionId));

        // test
        targetState.updateSessionId("");

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID));
        Assert.assertNotNull(targetState.getSessionId());
        Assert.assertNotEquals("", targetState.getSessionId());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID),
                        AdditionalMatchers.not(Mockito.eq(sessionId)));
    }

    @Test
    public void testSessionId_WhenSessionIsExpired() throws InterruptedException {
        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                        put("target.sessionTimeout", 2);
                    }
                };
        targetState.updateConfigurationSharedState(configuration);

        // test
        String firstSessionId = targetState.getSessionId();

        // verify
        Assert.assertNotNull(firstSessionId);
        Assert.assertNotEquals("", firstSessionId);
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID),
                        Mockito.eq(firstSessionId));

        Thread.sleep(3000L);

        // test
        String newSessionId = targetState.getSessionId();

        // verify
        Assert.assertNotNull(newSessionId);
        Assert.assertNotEquals("", newSessionId);
        Assert.assertNotEquals(firstSessionId, newSessionId);
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID),
                        Mockito.eq(newSessionId));
    }

    @Test
    public void testSessionId_WhenSessionIsNotExpired() throws InterruptedException {
        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                        put("target.sessionTimeout", 100);
                    }
                };
        targetState.updateConfigurationSharedState(configuration);

        // test
        String firstSessionId = targetState.getSessionId();

        // verify
        Assert.assertNotNull(firstSessionId);
        Assert.assertNotEquals("", firstSessionId);
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID),
                        Mockito.eq(firstSessionId));

        Thread.sleep(3000L);

        // test
        String newSessionId = targetState.getSessionId();

        // verify
        Assert.assertEquals(firstSessionId, newSessionId);
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID),
                        Mockito.eq(firstSessionId));
    }

    // ===================================
    // Test getEdgeHost updateEdgeHost
    // ===================================
    @Test
    public void testEdgeHost() {
        // verify
        targetState.updateSessionTimestamp(false);
        Assert.assertNull(targetState.getEdgeHost());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .getString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST),
                        Mockito.anyString());

        // setup
        String edgeHost = "edge-host-1";

        // test
        targetState.updateEdgeHost(edgeHost);

        // verify
        Assert.assertEquals(edgeHost, targetState.getEdgeHost());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST),
                        Mockito.eq(edgeHost));
    }

    @Test
    public void testEdgeHost_WhenUpdatedEdgeHostNull() {
        // setup
        String edgeHost = "edge-host-1";

        // test
        targetState.updateSessionTimestamp(false);
        targetState.updateEdgeHost(edgeHost);

        // verify
        Assert.assertEquals(edgeHost, targetState.getEdgeHost());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST),
                        Mockito.eq(edgeHost));

        // test
        targetState.updateEdgeHost(null);

        // verify
        Assert.assertNull(targetState.getEdgeHost());
        Mockito.verify(mockedDataStore, Mockito.times(2))
                .getString(Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST), Mockito.any());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST));
    }

    @Test
    public void testEdgeHost_WhenUpdatedEdgeHostEmpty() {
        // setup
        String edgeHost = "edge-host-1";

        // test
        targetState.updateSessionTimestamp(false);
        targetState.updateEdgeHost(edgeHost);

        // verify
        Assert.assertEquals(edgeHost, targetState.getEdgeHost());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST),
                        Mockito.eq(edgeHost));

        // test
        targetState.updateEdgeHost("");

        // verify
        Assert.assertNull(targetState.getEdgeHost());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .getString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST),
                        Mockito.anyString());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST));
    }

    @Test
    public void testEdgeHost_WhenSessionIsExpired() throws InterruptedException {
        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                        put("target.sessionTimeout", 2);
                    }
                };
        targetState.updateConfigurationSharedState(configuration);

        // setup
        String firstEdgeHost = "edge-host-1";

        // test
        targetState.updateSessionTimestamp(false);
        targetState.updateEdgeHost(firstEdgeHost);

        // verify
        Assert.assertEquals(firstEdgeHost, targetState.getEdgeHost());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST),
                        Mockito.eq(firstEdgeHost));

        Thread.sleep(3000L);

        // verify
        Assert.assertNull(targetState.getEdgeHost());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .getString(Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST), Mockito.any());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST));
    }

    @Test
    public void testEdgeHost_WhenSessionIsNotExpired() throws InterruptedException {
        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.clientCode", "code_123");
                        put("global.privacy", "optedin");
                        put("target.sessionTimeout", 100);
                    }
                };
        targetState.updateConfigurationSharedState(configuration);

        // setup
        String firstEdgeHost = "edge-host-1";

        // test
        targetState.updateSessionTimestamp(false);
        targetState.updateEdgeHost(firstEdgeHost);

        // verify
        Assert.assertEquals(firstEdgeHost, targetState.getEdgeHost());
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST),
                        Mockito.eq(firstEdgeHost));

        Thread.sleep(3000L);

        // verify
        String newEdgeHost = targetState.getEdgeHost();
        Assert.assertEquals(firstEdgeHost, newEdgeHost);
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .getString(Mockito.eq(TargetTestConstants.DataStoreKeys.EDGE_HOST), Mockito.any());
    }

    // ===================================
    // Test updateSessionTimestamp
    // ===================================
    @Test
    public void testSessionTimestamp() {
        // test
        targetState.updateSessionTimestamp(false);

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setLong(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_TIMESTAMP),
                        AdditionalMatchers.not(Mockito.eq(0)));
    }

    @Test
    public void testSessionTimestamp_WhenReset() {
        // test
        targetState.updateSessionTimestamp(true);

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_TIMESTAMP));
    }

    // ===================================
    // Test isPreviewEnabled
    // ===================================
    @Test
    public void testIsPreviewEnabled() {
        Assert.assertTrue(targetState.isPreviewEnabled());

        // setup
        Map<String, Object> configuration =
                new HashMap<String, Object>() {
                    {
                        put("target.previewEnabled", false);
                    }
                };
        targetState.updateConfigurationSharedState(configuration);

        // test and verify
        Assert.assertFalse(targetState.isPreviewEnabled());
    }

    // ===================================
    // Test getTntId updateTntId
    // ===================================
    @Test
    public void testGetTntId() {
        // setup
        String mockTntId = "mock-tntId";
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.TNT_ID),
                                Mockito.any()))
                .thenReturn(mockTntId);
        targetState = new TargetState(mockedDataStore);

        // test and verify
        Assert.assertEquals(mockTntId, targetState.getTntId());
    }

    @Test
    public void testUpdateTntId() {
        // setup
        String mockTntId = "mock-tntId";
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.TNT_ID),
                                Mockito.any()))
                .thenReturn(mockTntId);
        targetState = new TargetState(mockedDataStore);

        // verify
        Assert.assertEquals(mockTntId, targetState.getTntId());

        // test
        String newTntId = "new-tntId";
        targetState.updateTntId(newTntId);

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.TNT_ID), Mockito.eq(newTntId));
    }

    @Test
    public void testUpdateTntId_WhenNewTntIdNull() {
        // setup
        String mockTntId = "mock-tntId";
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.TNT_ID),
                                Mockito.any()))
                .thenReturn(mockTntId);
        targetState = new TargetState(mockedDataStore);

        // verify
        Assert.assertEquals(mockTntId, targetState.getTntId());

        // test
        targetState.updateTntId(null);

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.TNT_ID));
    }

    @Test
    public void testUpdateTntId_WhenNewTntIdEmpty() {
        // setup
        String mockTntId = "mock-tntId";
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.TNT_ID),
                                Mockito.any()))
                .thenReturn(mockTntId);
        targetState = new TargetState(mockedDataStore);

        // verify
        Assert.assertEquals(mockTntId, targetState.getTntId());

        // test
        targetState.updateTntId("");

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.TNT_ID));
    }

    // ===================================
    // Test getThirdPartyId updateThirdPartyId
    // ===================================
    @Test
    public void testGetThirdPartyId() {
        // setup
        String mockThirdPartyId = "mock-thirdPartyId";
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.THIRD_PARTY_ID),
                                Mockito.any()))
                .thenReturn(mockThirdPartyId);
        targetState = new TargetState(mockedDataStore);

        // test and verify
        Assert.assertEquals(mockThirdPartyId, targetState.getThirdPartyId());
    }

    @Test
    public void testUpdateThirdPartyId() {
        // setup
        String mockThirdPartyId = "mock-thirdPartyId";
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.THIRD_PARTY_ID),
                                Mockito.any()))
                .thenReturn(mockThirdPartyId);
        targetState = new TargetState(mockedDataStore);

        // verify
        Assert.assertEquals(mockThirdPartyId, targetState.getThirdPartyId());

        // test
        String newThirdPartyId = "new-thirdPartyId";
        targetState.updateThirdPartyId(newThirdPartyId);

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .setString(
                        Mockito.eq(TargetTestConstants.DataStoreKeys.THIRD_PARTY_ID),
                        Mockito.eq(newThirdPartyId));
    }

    @Test
    public void testUpdateThirdPartyId_WhenNewTntIdNull() {
        // setup
        String mockThirdPartyId = "mock-thirdPartyId";
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.THIRD_PARTY_ID),
                                Mockito.any()))
                .thenReturn(mockThirdPartyId);
        targetState = new TargetState(mockedDataStore);

        // verify
        Assert.assertEquals(mockThirdPartyId, targetState.getThirdPartyId());

        // test
        targetState.updateThirdPartyId(null);

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.THIRD_PARTY_ID));
    }

    @Test
    public void testUpdateThirdPartyId_WhenNewTntIdEmpty() {
        // setup
        String mockThirdPartyId = "mock-thirdPartyId";
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.THIRD_PARTY_ID),
                                Mockito.any()))
                .thenReturn(mockThirdPartyId);
        targetState = new TargetState(mockedDataStore);

        // verify
        Assert.assertEquals(mockThirdPartyId, targetState.getThirdPartyId());

        // test
        targetState.updateThirdPartyId("");

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.THIRD_PARTY_ID));
    }

    // ===================================
    // Test resetSession
    // ===================================
    @Test
    public void testResetSession() {
        // test
        targetState.resetSession();

        // verify
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_ID));
        Mockito.verify(mockedDataStore, Mockito.times(1))
                .remove(Mockito.eq(TargetTestConstants.DataStoreKeys.SESSION_TIMESTAMP));
    }

    // ===================================
    // Test generateSharedState
    // ===================================
    @Test
    public void testGenerateSharedState() {
        // setup
        String mockTntId = "mock-tntId";
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.TNT_ID),
                                Mockito.any()))
                .thenReturn(mockTntId);
        String mockThirdPartyId = "mock-thirdPartyId";
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.THIRD_PARTY_ID),
                                Mockito.any()))
                .thenReturn(mockThirdPartyId);
        targetState = new TargetState(mockedDataStore);

        // test
        Map<String, Object> sharedState = targetState.generateSharedState();

        // verify
        Assert.assertEquals(2, sharedState.size());
        Assert.assertEquals(mockTntId, sharedState.get(TargetTestConstants.EventDataKeys.TNT_ID));
        Assert.assertEquals(
                mockThirdPartyId,
                sharedState.get(TargetTestConstants.EventDataKeys.THIRD_PARTY_ID));
    }

    @Test
    public void testGenerateSharedState_WhenNullTntIdAndThirdPartyId() {
        // setup
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.TNT_ID),
                                Mockito.any()))
                .thenReturn(null);
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.THIRD_PARTY_ID),
                                Mockito.any()))
                .thenReturn(null);
        targetState = new TargetState(mockedDataStore);

        // test
        Map<String, Object> sharedState = targetState.generateSharedState();

        // verify
        Assert.assertEquals(0, sharedState.size());
        Assert.assertNull(sharedState.get(TargetTestConstants.EventDataKeys.TNT_ID));
        Assert.assertNull(sharedState.get(TargetTestConstants.EventDataKeys.THIRD_PARTY_ID));
    }

    @Test
    public void testGenerateSharedState_WhenEmptyTntIdAndThirdPartyId() {
        // setup
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.TNT_ID),
                                Mockito.any()))
                .thenReturn("");
        Mockito.when(
                        mockedDataStore.getString(
                                Mockito.eq(TargetTestConstants.DataStoreKeys.THIRD_PARTY_ID),
                                Mockito.any()))
                .thenReturn("");
        targetState = new TargetState(mockedDataStore);

        // test
        Map<String, Object> sharedState = targetState.generateSharedState();

        // verify
        Assert.assertTrue(sharedState.isEmpty());
    }

    // ===================================
    // Test getPrefetchedMbox mergePrefetchedMboxJson
    // ===================================
    @Test
    public void testPrefetchedMboxes() throws JSONException {
        Assert.assertTrue(targetState.getPrefetchedMbox().isEmpty());

        // setup
        String prefetchString =
                "{\n"
                        + "    \"name\" : \"mboxName\" ,  \n"
                        + "     \"options\" : [{\n"
                        + "      \"content\" : \"myContent\""
                        + "  }]\n"
                        + "  }";
        JSONObject prefetchJsonObject = new JSONObject(prefetchString);
        final Map<String, JSONObject> prefetchMbox =
                new HashMap<String, JSONObject>() {
                    {
                        put("mboxName", prefetchJsonObject);
                    }
                };

        // test
        targetState.mergePrefetchedMboxJson(prefetchMbox);

        // verify
        Assert.assertEquals(1, targetState.getPrefetchedMbox().size());
        Assert.assertEquals(prefetchMbox, targetState.getPrefetchedMbox());
        Assert.assertEquals(prefetchJsonObject, targetState.getPrefetchedMbox().get("mboxName"));

        // test
        targetState.clearPrefetchedMboxes();

        // verify
        Assert.assertTrue(targetState.getPrefetchedMbox().isEmpty());
    }

    @Test
    public void testPrefetchedMboxes_EmptyOrNullPrefetchedMbox() throws JSONException {
        // test
        targetState.mergePrefetchedMboxJson(new HashMap<>());

        // verify
        Assert.assertTrue(targetState.getPrefetchedMbox().isEmpty());

        // test
        targetState.mergePrefetchedMboxJson(null);

        // verify
        Assert.assertTrue(targetState.getPrefetchedMbox().isEmpty());
    }

    // ===================================
    // Test saveLoadedMbox getLoadedMbox
    // ===================================
    @Test
    public void testSaveLoadedMbox() throws JSONException {
        // setup
        String mBoxesResponseString =
                "{\n"
                        + "    \"name\" : \"mboxName\" ,  \n"
                        + "    \"metrics\" : \"myMetrics\" ,  \n"
                        + "     \"options\" : [{\n"
                        + "      \"content\" : \"myContent\""
                        + "  }]\n"
                        + "  }";
        JSONObject mboxResponseObject = new JSONObject(mBoxesResponseString);
        final Map<String, JSONObject> mboxResponses =
                new HashMap<String, JSONObject>() {
                    {
                        put("mboxName", mboxResponseObject);
                    }
                };

        // test
        targetState.saveLoadedMbox(mboxResponses);

        // verify
        Map<String, JSONObject> loadedMbox = targetState.getLoadedMbox();
        Assert.assertEquals(1, loadedMbox.size());
        JSONObject loadedMboxObject = loadedMbox.get("mboxName");
        Assert.assertEquals(2, loadedMboxObject.length());
        Assert.assertEquals("mboxName", loadedMboxObject.get("name"));
        Assert.assertEquals("myMetrics", loadedMboxObject.get("metrics"));
    }

    @Test
    public void testSaveLoadedMbox_WhenEmptyOrNullMboxName() throws JSONException {
        // setup
        String mBoxesResponseString =
                "{\n"
                        + "    \"name\" : \"mboxName\" ,  \n"
                        + "    \"metrics\" : \"myMetrics\" ,  \n"
                        + "     \"options\" : [{\n"
                        + "      \"content\" : \"myContent\""
                        + "  }]\n"
                        + "  }";
        JSONObject mboxResponseObject = new JSONObject(mBoxesResponseString);
        final Map<String, JSONObject> mboxResponses =
                new HashMap<String, JSONObject>() {
                    {
                        put("", mboxResponseObject);
                        put(null, mboxResponseObject);
                    }
                };

        // test
        targetState.saveLoadedMbox(mboxResponses);

        // verify
        Assert.assertTrue(targetState.getLoadedMbox().isEmpty());
    }

    @Test
    public void testSaveLoadedMbox_WhenEmptyOrNullMboxResponse() {
        // test
        targetState.saveLoadedMbox(null);

        // verify
        Assert.assertTrue(targetState.getLoadedMbox().isEmpty());

        // test
        targetState.saveLoadedMbox(new HashMap<>());

        // verify
        Assert.assertTrue(targetState.getLoadedMbox().isEmpty());
    }

    @Test
    public void testSaveLoadedMbox_WhenEmptyJsonInMboxResponse() {
        // setup
        final Map<String, JSONObject> mboxResponses =
                new HashMap<String, JSONObject>() {
                    {
                        put("mboxName", new JSONObject());
                    }
                };

        // test
        targetState.saveLoadedMbox(mboxResponses);

        // verify
        Assert.assertEquals(1, targetState.getLoadedMbox().size());
        Assert.assertEquals("{}", targetState.getLoadedMbox().get("mboxName").toString());
    }

    @Test
    public void testSaveLoadedMbox_WhenNullJsonInMboxResponse() {
        // setup
        final Map<String, JSONObject> mboxResponses =
                new HashMap<String, JSONObject>() {
                    {
                        put("mboxName", null);
                    }
                };

        // test
        targetState.saveLoadedMbox(mboxResponses);

        // verify
        Assert.assertTrue(targetState.getLoadedMbox().isEmpty());
    }

    @Test
    public void testSaveLoadedMbox_WithPrefetchedMbox() throws JSONException {
        // setup
        String prefetchString =
                "{\n"
                        + "    \"name\" : \"mboxName\" ,  \n"
                        + "    \"metrics\" : \"myMetrics\" ,  \n"
                        + "     \"options\" : [{\n"
                        + "      \"content\" : \"myContent\""
                        + "  }]\n"
                        + "  }";
        JSONObject prefetchJsonObject = new JSONObject(prefetchString);
        final Map<String, JSONObject> prefetchMbox =
                new HashMap<String, JSONObject>() {
                    {
                        put("mboxName", prefetchJsonObject);
                    }
                };
        targetState.mergePrefetchedMboxJson(prefetchMbox);

        String mBoxesResponseString =
                "{\n"
                        + "    \"name\" : \"mboxName1\" ,  \n"
                        + "    \"metrics\" : \"myMetrics1\" \n"
                        + "  }";
        JSONObject mboxResponseObject = new JSONObject(mBoxesResponseString);
        final Map<String, JSONObject> mboxResponses =
                new HashMap<String, JSONObject>() {
                    {
                        put("mboxName", prefetchJsonObject);
                        put("mboxName1", mboxResponseObject);
                    }
                };

        // test
        targetState.saveLoadedMbox(mboxResponses);

        // verify
        Map<String, JSONObject> loadedMbox = targetState.getLoadedMbox();
        Assert.assertEquals(1, loadedMbox.size());
        JSONObject loadedMboxObject = loadedMbox.get("mboxName1");
        Assert.assertEquals(2, loadedMboxObject.length());
        Assert.assertEquals("mboxName1", loadedMboxObject.get("name"));
        Assert.assertEquals("myMetrics1", loadedMboxObject.get("metrics"));
    }

    // ===================================
    // Test removeDuplicateLoadedMboxes
    // ===================================
    @Test
    public void testRemoveDuplicateLoadedMboxes() throws JSONException {
        // setup
        String mboxString =
                "{\n"
                        + "    \"name\" : \"mboxName\" ,  \n"
                        + "    \"metrics\" : \"myMetrics\" ,  \n"
                        + "     \"options\" : [{\n"
                        + "      \"content\" : \"myContent\""
                        + "  }]\n"
                        + "  }";
        JSONObject mboxJsonObject = new JSONObject(mboxString);

        final Map<String, JSONObject> mboxResponses =
                new HashMap<String, JSONObject>() {
                    {
                        put("mboxName", mboxJsonObject);
                    }
                };

        targetState.saveLoadedMbox(mboxResponses);

        final Map<String, JSONObject> prefetchMbox =
                new HashMap<String, JSONObject>() {
                    {
                        put("mboxName", mboxJsonObject);
                    }
                };
        targetState.mergePrefetchedMboxJson(prefetchMbox);

        // test
        targetState.removeDuplicateLoadedMboxes();

        // verify
        Assert.assertTrue(targetState.getLoadedMbox().isEmpty());
    }

    // ===================================
    // Test addNotification getNotifications clearNotifications
    // ===================================
    @Test
    public void testNotifications() throws JSONException {
        Assert.assertTrue(targetState.getNotifications().isEmpty());

        // setup
        String mboxString =
                "{\n"
                        + "    \"name\" : \"mboxName\" ,  \n"
                        + "    \"metrics\" : \"myMetrics\" ,  \n"
                        + "     \"options\" : [{\n"
                        + "      \"content\" : \"myContent\""
                        + "  }]\n"
                        + "  }";
        JSONObject mboxJsonObject = new JSONObject(mboxString);

        // test
        targetState.addNotification(mboxJsonObject);

        // verify
        Assert.assertEquals(
                mboxJsonObject.toString(), targetState.getNotifications().get(0).toString());

        // test
        targetState.clearNotifications();

        // verify
        Assert.assertTrue(targetState.getNotifications().isEmpty());
    }

    @Test
    public void testNotifications_WhenEmptyOrNullJsonObject() throws JSONException {
        // test
        targetState.addNotification(new JSONObject());

        // verify
        Assert.assertTrue(targetState.getNotifications().isEmpty());

        // test
        targetState.addNotification(null);

        // verify
        Assert.assertTrue(targetState.getNotifications().isEmpty());
    }
}
