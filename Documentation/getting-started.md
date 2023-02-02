# Adobe Experience Platform Target extension

Adobe Target helps test, personalize, and optimize mobile app experiences based on user behavior and mobile context. You can deliver interactions that engage and convert through iterative testing and rules-based and AI-powered personalization.

To get started with Target, follow these steps:

1. Configure the Target extension in the Data Collection UI.
2. Add the Target Extension to your app.
3. Implement Target APIs to:
   * Request mbox offers.
   * Prefetch mbox offers.
   * Track mboxes.
   * Enter visual preview mode.

## Configure the Target extension in the Data Collection UI

![Adobe Target Extension Configuration](./assets/adobe-target-launch-options.png)

1. In the Data Collection UI, click the **Extensions** tab.
2. On the **Catalog** tab, locate the Adobe Target extension, and click **Install**.
3. Your **Target** client code will be detected automatically.
4. Optionally, provide your Environment ID.
5. Set the timeout value to at least 5 seconds.
6. Optionally, enter the Target workspace property token that was generated from Target UI.
7. Click **Save**.
8. Follow the publishing process to update SDK configuration.

## Add Target to your app

Add the Mobile Core and Target extensions to your project using the app's Gradle file.

   ```java
    implementation 'com.adobe.marketing.mobile:core:2.+'
    implementation 'com.adobe.marketing.mobile:target:2.+'
   ```
   
> **Warning**
> Using dynamic dependency versions is not recommended for production apps. Refer to this [page](https://github.com/adobe/aepsdk-core-android/blob/main/Documentation/MobileCore/gradle-dependencies.md) for managing gradle dependencies.

### Register Target with Mobile Core

#### Java
```java
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Target;
import com.adobe.marketing.mobile.AdobeCallback;

public class MainApp extends Application {

  private final String ENVIRONMENT_FILE_ID = "YOUR_APP_ENVIRONMENT_ID";

    @Override
    public void onCreate() {
        super.onCreate();

        MobileCore.setApplication(this);
        MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);

        MobileCore.registerExtensions(
            Arrays.asList(Target.EXTENSION),
            o -> Log.d("MainApp", "Adobe Target Mobile SDK was initialized.")
        );
    }
}
```

#### Kotlin
```kotlin
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.Target
import com.adobe.marketing.mobile.AdobeCallback

class MainApp : Application() {

  private var ENVIRONMENT_FILE_ID: String = "YOUR_APP_ENVIRONMENT_ID"

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID)

        MobileCore.registerExtensions(
          listOf(Target.EXTENSION)
        ) {
          Log.d("MainApp", "Adobe Target Mobile SDK was initialized")
        }
    }

}
```

## Parameters in a Target request

Here is some information about the parameters in a Target request:

### Target Order class

The `TargetOrder` class encapsulates the order ID, the order total, and the purchased product IDs. You can instantiate this class to create order parameters. For more information about Target Order parameters, see [Create an Order Confirmation mbox - mbox.js](https://experienceleague.adobe.com/docs/target/using/implement-target/client-side/implement-target-for-client-side-web.html?lang=en).

#### Syntax

```java
public TargetOrder(final String id, final double total, final List<String> purchasedProductIds)
```

#### Example

```java
List<String> purchasedProductIds = new ArrayList<String>();
purchasedProductIds.add("34");
purchasedProductIds.add("125");
TargetOrder targetOrder = new TargetOrder("123", 567.89, purchasedProductIds);
```

### Target Product class

The `TargetProduct` class encapsulates the product ID and the product category ID, and you can instantiate this class to create order parameters. For more information about Target Product parameters, see [Entity attributes](https://experienceleague.adobe.com/docs/target/using/recommendations/entities/entity-attributes.html?lang=en)

#### Syntax

```java
public TargetProduct(final String id, final String categoryId)
```

#### Example

```java
TargetProduct targetProduct = new TargetProduct("123", "Books");
```

### Target Parameters

`TargetParameters` encapsulates `mboxParameters`, `profileParameters`, `orderParameters`, and `productParameters` and allows you easily pass these parameters in a Target request.

#### Syntax

```java
TargetParameters targetParameters = new TargetParameters.Builder()
.parameters(new HashMap<String, String>())
.profileParameters(new HashMap<String, String>())
.product(new TargetProduct("productId", "productCategoryId"))
.order(new TargetOrder("orderId", 0.0, new ArrayList<String>()))
.build();
```

#### Example

```java
List<String> purchasedProductIds = new ArrayList<String>();
purchasedProductIds.add("34");
purchasedProductIds.add("125");
TargetOrder targetOrder = new TargetOrder("123", 567.89, purchasedProductIds);

TargetProduct targetProduct = new TargetProduct("123", "Books");

Map<String, String> mboxParameters = new HashMap<String, String>();
mboxParameters1.put("status", "platinum");

Map<String, String> profileParameters = new HashMap<String, String>();
profileParameters1.put("gender", "male");

TargetParameters targetParameters = new TargetParameters.Builder()
.parameters(mboxParameters)
.profileParameters(profileParameters)
.product(targetProduct)
.order(targetOrder)
.build();
```

### Merge behavior of Target parameters

`TargetParameters`, such as `mboxParameters`, `profileParameters`, `orderParameters`, and `productParameters`, can be passed in the Target APIs and can also be passed in when you create `TargetPrefetch` or `TargetRequest` objects. The `TargetParameters` that are passed in the public APIs are global parameters and are merged with the corresponding parameters in the individual `TargetRequest` or `TargetPrefetch` objects.

When merging, the new keys in the mbox parameters or the profile parameters are appended to the final dictionary, and the keys with the same name are overwritten in each `TargetRequest` or `TargetPrefetch` object by the keys from the global parameters. For `TargetOrder` or `TargetProduct` objects, the object that is passed to the global parameters replaces the corresponding object in the `TargetRequest` or `TargetPrefetch` objects.

## Target sessions

The Target extension now supports persistent sessions. When a Target request is received, a new session ID is generated and is sent in the request, if it does not exist. This ID, with the Edge Host that is returned from Target, is kept in persistent storage for the configured `target.sessionTimeout` period. If the timeout value is not configured, the default value is 30 minutes.

If no Target request is received during the configured `target.sessionTimeout` or if the [resetExperience](./api-reference.md#resetExperience) API is called, these variables are reset and removed from persistent storage.

### Cross-channel session support

The Target extension now supports additional getter and setter APIs for Target tnt IDs and session IDs to enable cross-channel sessions by using these APIs in conjunction with Target APIs on another channel e.g. `at.js` Javascript library. 

The session ID and tnt ID should be set in the Mobile SDK prior to issuing any Target prefetch or execute requests. This will allow the SDK to do the following:

* Persist and use the provided session ID in the subsequent Target request(s) instead of generating one locally and using it. The session expiry will be governed by the `target.sessionTimeout` SDK configuration setting.
* Persist and use the provided tnt ID in the subsequent Target request(s).
* Persist and use the Target edge host value, derived from the profile hint supplied in the tnt ID, in the subsequent Target request(s). For example: if the provided tnt ID value is `66E5C681-4F70-41A2-86AE-F1E151443B10.35_0`, the Mobile SDK will set the Target edge host value to `mboxedge35.tt.omtrdc.net` (based on the supplied profile hint `35_0`).

Additionally, the current Target session ID and tnt ID values can be retrieved from the mobile SDK by using the corresponding getter APIs.

## Visual preview

The visual preview mode allows you to easily perform end-to-end QA activities by enrolling and previewing these activities on your device. This mode does not require a specialized testing set up. To get started, set up a URL scheme and generate the preview links. For more information about setting up Target visual preview, see [Target mobile preview](https://experienceleague.adobe.com/docs/target/using/implement-target/mobile-apps/target-mobile-preview.html?lang=en). For more information about setting URL schemes for Android, see [Create Deep Links to App Content](https://developer.android.com/training/app-links/deep-linking).

You can also set an application deep link that can be triggered when selections are made in the preview mode by using the [setPreviewRestartDeeplink](https://aep-sdks.gitbook.io/docs/using-mobile-extensions/adobe-target/target-api-reference#setPreviewRestartDeeplink) API.

To enter the preview visual mode, use the `collectLaunchInfo` API to enable the mode and click the red floating button that appears on the app screen. For more information, see [collectLaunchInfo](https://aep-sdks.gitbook.io/docs/using-mobile-extensions/mobile-core/mobile-core-api-reference#collect-launch-information).

> **Note**
> After making preview mode selections, the first mbox request made may fail due to a caching issue on the Target server. For more information see [the known issues and resolved issues document](https://experienceleague.adobe.com/docs/target/using/release-notes/known-issues-resolved-issues.html?lang=en). The mbox request that failed can be retried to successfully retrieve the test offer content.

On Android, when the application is launched as a result of a deep link, the `collectLaunchInfo` API is internally invoked, and the Target activity and deep link information is extracted from the Intent extras.

> **Note**
> The SDK can only collect information from the launching Activity if [`setApplication`](https://aep-sdks.gitbook.io/docs/using-mobile-extensions/mobile-core/mobile-core-api-reference#application-reference-android-only) has been called. Setting the Application is only necessary on an Activity that is also an entry point for your application. However, setting the Application on each Activity has no negative impact and ensures that the SDK always has the necessary reference to your Application. We recommend that you call `setApplication` in each of your Activities.

## Offer Prefetch

The SDK can minimize the number of times it reaches out to Target servers to fetch offers by caching server responses. With a successful prefetch call for mbox locations, offer content is retrieved and cached in the SDK. This content is retrieved from the cache for all future [retrieveLocationContent](https://aep-sdks.gitbook.io/docs/using-mobile-extensions/adobe-target/target-api-reference#retrieveLocationContent) API calls for the specified mbox names. This prefetch process reduces the offer load time and network calls that were made to the Target servers, and the prrocess allows Target to be notified which mbox was visited by the mobile app user.

> **Warning**
> Prefetched offer content does not persist across application launches. The prefetch content is cached as long as the application lives in memory or until the API to clear the cache is called. For more information, see [clearPrefetchCache](https://aep-sdks.gitbook.io/docs/using-mobile-extensions/target-api-reference#clearPrefetchCache).

> **Warning**
> Offer prefetch is not available while visual preview mode is enabled.

## Target with Analytics \(A4T\) <a id="integrating-adobe-target-with-analytics-a-4-t"></a>

To track the performance of your Target activities for certain segments, set up the Analytics for Target \(A4T\) cross-solution integration by enabling the A4T campaigns. This integration allows you to use Analytics reports to examine your results. If you use Analytics as the reporting source for an activity, all reporting and segmentation for that activity is based on Analytics data collection. For more information, see [Adobe Analytics for Adobe Target \(A4T\)](https://experienceleague.adobe.com/docs/target/using/integrate/a4t/a4t.html?lang=en).

Once Analytics is listed as the reporting source for an activity on Target UI, A4T works out of the box in the Target SDK. The Target SDK extension extracts the A4T payload from the Target server response, dispatches an event for Analytics SDK extension to send an internal track action request to the configured Analytics server.

The A4T payload returned from Target servers is sent to Adobe Analytics in the following cases:

* When one or more locations are retrieved using [retrieveLocationContent](https://aep-sdks.gitbook.io/docs/using-mobile-extensions/adobe-target/target-api-reference#retrievelocationcontent) API call.
* When one or more prefetched locations are loaded and a subsequent [locationsDisplayed](https://aep-sdks.gitbook.io/docs/using-mobile-extensions/adobe-target/target-api-reference#locationsdisplayed) API call is made for the location\(s\).

> **Warning**
> For A4T data to be sent to Adobe Analytics client-side, make sure Analytics SDK extension is installed and registered in your mobile application. For more information, see [Adobe Analytics](https://aep-sdks.gitbook.io/docs/using-mobile-extensions/adobe-analytics).

## Configuration keys

To programmatically update SDK configuration, use the following information to change your Target configuration values:

For more information, see [Programmatic updates to Configuration](https://github.com/adobe/aepsdk-core-android/blob/main/Documentation/MobileCore/api-reference.md#updating-the-configuration-programmatically).

| Key | Description | Data Type |
| :--- | :--- | :--- |
| target.clientcode | Client code for your account. | String |
| target.timeout | Time, in seconds, to wait for a response from Target servers before timing out. | Integer |
| target.environmentId | Environment ID you want to use. If the value is left blank, the default production environment will be used. | Integer |
| target.propertyToken | `at_property` token value, which is generated from the Target UI. If this value is left blank, no token is sent in the Target network calls. | String |
| target.previewEnabled | Boolean parameter, which can be used to enable/disable Target Preview. If not specified, then Preview will be enabled by default. | Boolean |
| target.sessionTimeout | The duration, in seconds, during which the Target session ID and Edge Host are persisted. If this value is not specified, the default timeout value is 30 minutes. | Integer |
| target.server | _Optional_. If provided, all Target requests will be sent to this host. e.g. - `mytargetdomain.com` | String |

> **Warning**
> We recommend that, instead of passing the property token as a mbox parameter, you use an Experience Platform Launch configuration so that Target can pass the token. If the token is passed both in an Experience Platform Launch configuration, and as a mbox parameter, the token that was provided as the mbox parameter is discarded.

> **Warning**
> Currently, the `target.sessiontimeout` value can only be configured programmatically. For more information, see [updateConfiguration](https://github.com/adobe/aepsdk-core-android/blob/main/Documentation/MobileCore/api-reference.md#updating-the-configuration-programmatically).

## Additional information

* Get familiar with the various APIs offered by the AEP SDK by checking out the [Adobe Target API reference](./api-reference.md).
* Want to get your Target client code? See the **Client** row in [Configure mbox.js](https://experienceleague.adobe.com/docs/target/using/implement-target/client-side/implement-target-for-client-side-web.html?lang=en).
* What is an mbox? See [How Target works in mobile apps](https://experienceleague.adobe.com/docs/target/using/implement-target/mobile-apps/mobile-how-target-works-mobile-apps.html?lang=en).
* What is Analytics for Target \(A4T\)? See [Adobe Analytics as the reporting source for Adobe Target \(A4T\)](https://experienceleague.adobe.com/docs/target/using/integrate/a4t/a4t.html?lang=en).
