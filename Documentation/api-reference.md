# Adobe Target API reference

## Prerequisites

Refer to the [Getting Started Guide](getting-started.md).

## API reference

- [clearPrefetchCache](#clearPrefetchCache)
- [clickedLocation](#clickedLocation)
- [displayedLocations](#displayedLocations)
- [extensionVersion](#extensionVersion)
- [getSessionId](#getSessionId)
- [getThirdPartyId](#getThirdPartyId)
- [prefetchContent](#prefetchContent)
- [registerExtension](#registerExtension)
- [resetExperience](#resetExperience)
- [retrieveLocationContent](#retrieveLocationContent)
- [setPreviewRestartDeepLink](#setPreviewRestartDeepLink)
- [setSessionId](#setSessionId)
- [setThirdPartyId](#setThirdPartyId)
- [setTntId](#setTntId)
- [Visual preview](#visualPreview)
- [executeRawRequest](#executeRawRequest)
- [sendRawNotifications](#sendRawNotifications)

## Public classes

- [TargetRequest](#TargetRequest)
- [TargetPrefetch](#TargetPrefetch)
- [TargetParameters](#TargetParameters)
- [TargetOrder](#TargetOrder)
- [TargetProduct](#TargetProduct)
- [AdobeTargetDetailedCallback](#AdobeTargetDetailedCallback)

## API reference

### clearPrefetchCache

This API clears the in-memory cache that contains the prefetched offers.

**Syntax**

```text
public static void clearPrefetchCache()
```

**Example**

```text
Target.clearPrefetchCache();
```

### clickedLocation

This API sends a location click notification for an mbox to the configured Target server and can be invoked in the following cases:

* For a prefetched mbox, after the mbox content is retrieved using the `retrieveLocationContent` API.
* For a regular mbox, where no previous prefetch request is made, and the mbox content is retrieved using the `retrieveLocationContent` API.

**Syntax**

```java
public static void clickedLocation(final String mboxName, final TargetParameters parameters)
```

* _mboxName_ is a String that contains the mbox location for which the click notification will be sent to Target.
* _parameters_ is the configured `TargetParameters` for the request.

**Example**

```java
// Mbox parameters
Map<String, String> mboxParameters = new HashMap<>();
mboxParameters.put("membership", "prime");

// Product parameters
TargetProduct targetProduct = new TargetProduct("CEDFJC", "Electronics");


// Order parameters
List<String> purchasedIds = new ArrayList<String>();
purchasedIds.add("81");
purchasedIds.add("123");
purchasedIds.add("190");
TargetOrder targetOrder = new TargetOrder("NJJICK", "650", purchasedIds);

// Profile parameters
Map<String, String> profileParameters = new HashMap<>();
profileParameters.put("ageGroup", "20-32");

// Create Target Parameters
TargetParameters targetParameters = new TargetParameters.Builder(mboxParameters)
                                .profileParameters(profileParameters)
                                .order(targetOrder)
                                .product(targetProduct)
                                .build();

Target.clickedLocation("cartLocation", targetParameters);
```

### displayedLocations

This API sends a location display notification for an mbox to the configured Target server. The API should be invoked for a prefetched mbox after the mbox content is retrieved using the `retrieveLocationContent` API. If no previous prefetch request is made, and the mbox content is retrieved using the `retrieveLocationContent` API, calling this API does not trigger a notification request to the Target server.

**Syntax**

```java
public static void displayedLocations(final List<String> mboxNames, final TargetParameters targetParameters)
```

* _mboxNames_ is a list of the mbox locations for which the display notification will be sent to Target.
* _targetParameters_ is the configured `TargetParameters` for the request.

**Example**

```java
List<String> purchasedProductIds = new ArrayList<String>();
purchasedProductIds.add("34");
purchasedProductIds.add("125"); 
TargetOrder targetOrder = new TargetOrder("123", 567.89, purchasedProductIds);

TargetProduct targetProduct = new TargetProduct("123", "Books");

TargetParameters targetParameters = new TargetParameters.Builder()
.parameters(new HashMap<String, String>())
.profileParameters(new HashMap<String, String>())
.product(targetProduct)
.order(targetOrder)
.build();

List<String> mboxList = new ArrayList<>();
mboxList.add("mboxName1");

Target.displayedLocations(mboxList, targetParameters);
```

### extensionVersion

Returns the running version of the Target extension.

**Syntax**

```java
public String extensionVersion()
```

**Example**

```java
Target.extensionVersion();
```

### getSessionId

This API gets the Target session identifier. 

The session ID is generated locally in the SDK upon initial Target request and persisted for a period defined by `target.sessionTimeout` configuration setting. If the session timeout happens upon a subsequent Target request, a new session ID will be generated for use in the request and persisted in the SDK.

**Syntax**

```java
public static void getSessionId(final AdobeCallback<String> callback)
```

* _callback_ is invoked with the `sessionId` value, or `null` if there was an error retrieving it.

**Example**

```java
Target.getSessionId(new AdobeCallback<String>() {                    
    @Override
    public void call(String sessionId) {
                // read Target sessionId
    }
});
```

### getThirdPartyId

This API gets the custom visitor ID for Target. If no `third-party` ID was previously set, or if the ID was reset by calling resetExperience API, it will have a `nil` value.

**Syntax**

```java
public static void getThirdPartyId(final AdobeCallback<String> callback)
```

* _callback_ is invoked with the `thirdPartyId` value. If no third-party ID was set, this value will be `null`.

**Example**

```java
Target.getThirdPartyId(new AdobeCallback<String>() {                    
    @Override
    public void call(String thirdPartyId) {
                // read Target thirdPartyId
    }
});
```

### getTntId

This API gets the Target user identifier (also known as the `tntId`). 

The tnt ID is returned in the network response from Target after a successful call to `prefetchContent` API or `retrieveLocationContent` API, which is then persisted in the SDK. The persisted tnt ID is used in subsequent Target requests until a different tnt ID is returned from Target, or a new tnt ID is set using `setTntId` API.

**Syntax**

```java
public static void getTntId(final AdobeCallback<String> callback)
```

* _callback_ is invoked with the `tntId` value, or `null` if there was an error retrieving it.

**Example**

```java
Target.getTntId(new AdobeCallback<String>() {
    @Override
    public void call(String tntId) {
        // read Target's tntid
    }
});
```

### prefetchContent

This API sends a prefetch request to your configured Target server. The prefetch request is sent with the prefetch objects array and the specified Target parameters.

**Syntax**

```java
public static void prefetchContent(final List<TargetPrefetch> mboxPrefetchList, final TargetParameters parameters, final AdobeCallback<String> callback)
```

* _mboxPrefetchList_ is a list of `TargetPrefetch` objects for various mbox locations.
* _parameters_ is the configured `TargetParameters` for the prefetch request.
* If the prefetch is successful, _callback_ is invoked with a `null` value. If the prefetch is not successful, an error message is returned.

**Example**

```java
// first prefetch request
Map<String, String> mboxParameters1 = new HashMap<>();
mboxParameters1.put("status", "platinum");

TargetParameters targetParameters1 = new TargetParameters.Builder()
.parameters(mboxParameters1)
.build();

TargetPrefetch prefetchRequest1 = new TargetPrefetch("mboxName1", targetParameters1);

// second prefetch request
Map<String, String> mboxParameters2 = new HashMap<>();
mboxParameters2.put("userType", "paid");

List<String> purchasedIds = new ArrayList<String>();
purchasedIds.add("34");
purchasedIds.add("125");
TargetOrder targetOrder = new TargetOrder("ADCKKIM", 344.30, purchasedIds);

TargetProduct targetProduct = new TargetProduct("24D3412", "Books");

TargetParameters targetParameters2 = new TargetParameters.Builder()
.parameters(mboxParameters2)
.product(targetProduct)
.order(targetOrder)
.build();

TargetPrefetch prefetchRequest2 = new TargetPrefetch("mboxName2", targetParameters2);

List<TargetPrefetch> prefetchMboxesList = new ArrayList<>();
prefetchMboxesList.add(prefetchRequest1);
prefetchMboxesList.add(prefetchRequest2);

// Call the prefetchContent API.
TargetParamters targetParameters = null;
Target.prefetchContent(prefetchMboxesList, targetParameters, prefetchStatusCallback);
```

### registerExtension

> **Warning**
> This API is deprecated from version 2.0.0. Please use Mobile Core's [registerExtensions](https://github.com/adobe/aepsdk-core-android/blob/main/Documentation/MobileCore/api-reference.md) API instead.

Registers the Target extension with the Mobile Core.

**Syntax**

```java
public static void registerExtension()
```

**Example**

```java
Target.registerExtension();
```

### resetExperience

This API resets the user's experience by removing the visitor identifiers and resetting the Target session. Invoking this API also removes previously set Target user ID and custom visitor IDs, Target Edge Host, and the session information from persistent storage.

**Syntax**

```java
public static void resetExperience()
```

**Example**

```java
Target.resetExperience();
```

### retrieveLocationContent

This API sends a batch request to the configured Target server for multiple mbox locations.

A request will be sent to the configured Target server for mbox locations in the requests array for Target requests that have not been previously prefetched. The content for the mbox locations that have been prefetched in a previous request are returned from the SDK, and no additional network request is made. Each Target request object in the list contains a callback function, which is invoked when content is available for its given mbox location.

When using `contentWithData` callback to instantiate TargetRequest object, the following keys can be used to read response tokens and Analytics for Target \(A4T\) info from the data payload, if available in the Target response.

* responseTokens \(Response tokens\)
* analytics.payload \(A4T payload\)
* clickmetric.analytics.payload \(Click tracking A4T payload\)

**Syntax**

```java
public static void retrieveLocationContent(final List<TargetRequest> targetRequestList, final TargetParameters parameters)
```

* _targetRequestList_ is a list of `TargetRequest` objects for various mbox locations.
* _parameters_ is the configured `TargetParameters` for the retrieve location request.

**Example**

```java
// define parameters for first request
Map<String, String> mboxParameters1 = new HashMap<>();
mboxParameters1.put("status", "platinum");

TargetParameters parameters1 = new TargetParameters.Builder().parameters(mboxParameters1).build();

TargetRequest request1 = new TargetRequest("mboxName1", parameters1, "defaultContent1",
                                            new AdobeCallback<String>() {
                                                @Override
                                                public void call(String value) {
                                                    // do something with target content.
                                                }
                                            });

// define parameters for second request
Map<String, String> mboxParameters2 = new HashMap<>();
mboxParameters2.put("userType", "paid");

List<String> purchasedIds = new ArrayList<String>();
purchasedIds.add("34");
purchasedIds.add("125");
TargetOrder targetOrder = new TargetOrder("ADCKKIM", 344.30, purchasedIds);

TargetProduct targetProduct = new TargetProduct("24D3412", "Books");

TargetParameters parameters2 = new TargetParameters.Builder()
                               .parameters(mboxParameters2)
                               .product(targetProduct)
                               .order(targetOrder)
                               .build();

TargetRequest request2 = new TargetRequest("mboxName2", parameters2, "defaultContent2",
                                            new AdobeTargetDetailedCallback() {
                                                @Override
                                                public void call(final String content, final Map<String, Object> data) {
                                                    if (content != null && !content.isEmpty()) {
                                                        // do something with the target content.
                                                    }

                                                    // Read the data Map containing one or more of response tokens, analytics payload 
                                                    // and click metric analytics payload, if available
                                                    if (data != null && !data.isEmpty()) {

                                                        Map<String, String> responseTokens = data.containsKey("responseTokens") ? 
                                                                                            (Map<String, String>) data.get("responseTokens") : 
                                                                                            null;

                                                        Map<String, String> analyticsPayload = data.containsKey("analytics.payload") ? 
                                                                                              (Map<String, String>) data.get("analytics.payload") : 
                                                                                              null;

                                                        Map<String, String> clickMetricAnalyticsPayload = data.containsKey("clickmetric.analytics.payload") ? 
                                                                                                          (Map<String, String>) data.get("clickmetric.analytics.payload") : 
                                                                                                          null;

                                                        ...
                                                    }
                                                }

                                                @Overrides
                                                void fail(final AdobeError error) {
                                                    // take appropriate action upon error.
                                                }
                                            });

// Creating Array of Request Objects
List<TargetRequest> locationRequests = new ArrayList<>();
locationRequests.add(request1);
locationRequests.add(request2);

 // Define the profile parameters map.
Map<String, String> profileParameters1 = new HashMap<>();
profileParameters1.put("ageGroup", "20-32");

TargetParameters parameters = new TargetParameters.Builder().profileParameters(profileParameters1).build();

// Call the targetRetrieveLocationContent API.
Target.retrieveLocationContent(locationRequests, parameters);
```

### setPreviewRestartDeepLink

This API sets a specific location in the app to be displayed when preview mode selections have been confirmed.

**Syntax**

```java
public static void setPreviewRestartDeepLink(final Uri deepLink)
```

* _deeplink_ is a Uri that contains the preview restart deeplink.

**Example**

```java
Target.setPreviewRestartDeepLink("myapp://HomePage");
```

### setSessionId

This API sets the Target session identifier.

The provided session ID is persisted in the SDK for a period defined by `target.sessionTimeout` configuration setting. If the provided session ID is null or empty, or if the privacy status is opted out, the SDK will remove the session ID value from the persistence.

This ID is preserved between app upgrades, is saved and restored during the standard application backup process, and is removed at uninstall, upon privacy status update to opted out, or when the resetExperience API is used.

**Syntax**

```java
public static void setSessionId(final String sessionId)
```

* _sessionId_ is a String that contains the Target session identifier to be set in the SDK.

**Example**

```java
Target.setSessionId("3f24b997-ea74-420c-81f8-96a8b92c3961");
```

### setThirdPartyId

This API sets the custom visitor ID for Target. This ID is preserved between app upgrades, is saved and restored during the standard application backup process, and is removed at uninstall or when the resetExperience API is used.

**Syntax**

```java
public static void setThirdPartyId(final String thirdPartyId)
```

* _thirdPartyId_ is a String that contains the custom visitor ID to be set in Target.

**Example**

```java
Target.setThirdPartyId("third-party-id");
```

### setTntId

This API sets the Target user identifier.

The provided tnt ID is persisted in the SDK and attached to subsequent Target requests. It is used to derive the edge host value in the SDK, which is also persisted and used in future Target requests. If the provided tnt ID is nil/null or empty, or if the privacy status is opted out, the SDK will remove the tnt ID and edge host values from the persistence.

This ID is preserved between app upgrades, is saved and restored during the standard application backup process, and is removed at uninstall, upon privacy status update to opted out, or when the `resetExperience` API is used.

**Syntax**

```java
public static void setTntId(final String tntId)
```

* _tntId_ is a String that contains the Target user identifier to be set in the SDK.

**Example**

```java
Target.setTntId("f741a5d5-09c0-4931-bf53-b9e568c5f782.35_0");
```

### Visual preview <a id="visualPreview"></a>

Target visual preview mode allows you to easily perform end-to-end QA activities by enrolling and previewing these activities on your device. This mode does not require a specialized testing set up. To get started, set up a URL scheme and generate the preview links.


On Android, when the application is launched as a result of a deep link, the `collectLaunchInfo` API is internally invoked, and the Target activity and deep link information is extracted from the Intent extras.

> **Note**
> The SDK can only collect information from the launching Activity if [`setApplication`](https://github.com/adobe/aepsdk-core-android/blob/main/Documentation/MobileCore/api-reference.md) has been called. Setting the Application is only necessary on an Activity that is also an entry point for your application. However, setting the Application on each Activity has no negative impact and ensures that the SDK always has the necessary reference to your Application. We recommend that you call `setApplication` in each of your Activities.

### executeRawRequest

This API can be used to retrieve prefetch or execute response for mbox locations from the configured Target server.

**Syntax**

```java
public static void executeRawRequest(final Map<String, Object> request, final AdobeCallback<Map<String, Object>> callback)
```

* request: a map containing prefetch or execute request data in the Target v1 delivery API request format.
* callback: an AdobeCallback instance which will be called after the Target requst is completed. The parameter in the callback will contain the response data if the request executed successfully, or it will contain null otherwise. 

**Example**

```java
final Map<String, Object> executeMbox1 = new HashMap<String, Object>() {
    {
        put("index", 0);
        put("name", "mbox1");
        put("parameters", new HashMap<String, String>() {
            {
                put("mbox_parameter_key1", "mbox_parameter_value1");
            }
        });
        put("profileParameters", new HashMap<String, String>() {
            {
                put("subscription", "premium");
            }
        });
        put("order", new HashMap<String, Object>() {
            {
                put("id", "id1");
                put("total", 100.34);
                put("purchasedProductIds", new ArrayList<String>() {
                    {
                        add("pId1");
                    }
                });
            }
        });
        put("product", new HashMap<String, String>() {
            {
                put("id", "pId1");
                put("categoryId", "cId1");
            }
        });
    }
};

final Map<String, Object> executeMbox2 = new HashMap<String, Object>() {
    {
        put("index", 1);
        put("name", "mbox2");
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

    // handle response
});
```

### sendRawNotifications

This API sends notification request(s) to the configured Target server for display or click notifications. The event tokens required for the Target display or click notifications can be retrieved from the response of a prior `executeRawRequest` API call for prefetch or execute.

**Syntax**

```java
public static void sendRawNotifications(final Map<String, Object> request)
```

* request: A map containing notifications data in the Target v1 delivery API request format.

**Example**

```java
final List<Map<String, Object>> notifications = new ArrayList<>();
final Map<String, Object> notification = new HashMap<String, Object>() {
    {
        put("id", "0");
        put("timestamp", (long)(System.currentTimeMillis()));
        put("type", "click");
        put("mbox", new HashMap<String, Object>() {
            {
                put("name", "mbox1");
            }
        });
        put("tokens", new ArrayList<String>() {
            {
                add("someClickToken");
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
final Map<String, Object> request = new HashMap<>();
request.put("notifications", notifications);
Target.sendRawNotifications(request);
```

## Public classes


### TargetRequest

Here is a code sample for this class in Android:

```java
public class TargetRequest extends TargetObject {

    /**
     * Instantiate a TargetRequest object
     * @param mboxName String mbox name for this request
     * @param targetParameters TargetParameters for this request
     * @param defaultContent String default content for this request
     * @param contentCallback AdobeCallback<String> which will get called with Target mbox content
     */
    public TargetRequest(final String mboxName,
                         final TargetParameters targetParameters,
                         final String defaultContent,
                         final AdobeCallback<String> contentCallback);

    /**
    * Instantiate a TargetRequest object.
    *
    * @param mboxName String mbox name for this request.
    * @param targetParameters TargetParameters for this request.
    * @param defaultContent String default content for this request.
    * @param contentWithDataCallback AdobeTargetDetailedCallback which will get called with Target mbox content and other optional data such as Target response tokens, analytics payload, click metric analytics payload if available.
    */
    public TargetRequest(final String mboxName, 
                         final TargetParameters targetParameters, 
                         final String defaultContent,
                         final AdobeTargetDetailedCallback contentWithDataCallback);
}
```

### TargetPrefetch

Here is a code sample for this class in Android:

```java
public class TargetPrefetch extends TargetObject {

    /**
     * Instantiate a TargetPrefetch object
     * @param mboxName String mbox name for this prefetch request
     * @param targetParameters TargetParameters for this prefetch request
     */
     public TargetPrefetch(final String mboxName, final TargetParameters targetParameters)
}
```

### TargetParameters

Here is a code sample for this class in Android:

```java
public class TargetParameters {

    private TargetParameters() {}

    /**
    * Builder used to construct a TargetParameters object
    */
    public static class Builder {
        private Map<String, String> parameters;
        private Map<String, String> profileParameters;
        private TargetProduct product;
        private TargetOrder order;

        /**
         * Create a TargetParameters object Builder
         */
        public Builder() {}

        /**
         * Create a TargetParameters object Builder
         *
         * @param parameters mbox parameters for the built TargetParameters
         */
        public Builder(final Map<String, String> parameters);

        /**
         * Set mbox parameters on the built TargetParameters
         *
         * @param parameters mbox parameters map
         * @return this builder
         */
        public Builder parameters(final Map<String, String> parameters);

        /**
         * Set profile parameters on the built TargetParameters
         *
         * @param profileParameters profile parameters map
         * @return this builder
         */
        public Builder profileParameters(final Map<String, String> profileParameters);

        /**
         * Set product parameters on the built TargetParameters
         *
         * @param product product parameters
         * @return this builder
         */
        public Builder product(final TargetProduct product);

        /**
         * Set order parameters on the built TargetParameters
         *
         * @param order order parameters
         * @return this builder
         */
        public Builder order(final TargetOrder order);

        /**
         * Build the TargetParameters object
         *
         * @return the built TargetParameters object
         */
        public TargetParameters build();
    }
}
```

### TargetOrder

Here is a code sample for this class in Android:

```java
public class TargetOrder {

    /**
     * Initialize a TargetOrder with an order id, order total and a list of purchasedProductIds
     *
     * @param id String order id
     * @param total double order total amount
     * @param purchasedProductIds a list of purchased product ids
     */
    public TargetOrder(final String id, final double total, final List<String> purchasedProductIds);
    /**
     * Get the order id
     *
     * @return order id
     */
    public String getId();

    /**
     * Get the order total
     *
     * @return order total
     */
    public double getTotal();

    /**
     * Get the order purchasedProductIds
     *
     * @return a list of this order's purchased product ids
     */
    public List<String> getPurchasedProductIds();
}
```

### TargetProduct

Here is a code sample for this class in Android:

```java
public class TargetProduct {

    /**
     * Initialize a TargetProduct with a product id and a productCategoryId categoryId
     *
     * @param id String product id
     * @param categoryId String product category id
     */
    public TargetProduct(final String id, final String categoryId);

    /**
     * Get the product id
     *
     * @return product id
     */
    public String getId();

    /**
     * Get the product categoryId
     *
     * @return product category id
     */
    public String getCategoryId();
}
```

### AdobeTargetDetailedCallback

A sample of this interface on Android can be seen below:

```java
public interface AdobeTargetDetailedCallback {

    /**
     * Callback function to pass the mbox content and other mbox payload values.
     *
     * @param content {@code String} mox content
     * @param data A {@code Map<String, Object>} of mbox payload values. It will be null if neither response tokens nor analytics payload is available.
     */
    void call(final String content, final Map<String, Object> data);

    /**
     * Callback function for notifying about the internal error in getting mbox details.
     *
     * @param error {@link AdobeError} represents the internal error occurred.
     */
    void fail(final AdobeError error);
}
```
