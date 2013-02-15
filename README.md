# Quantcast Android SDK #

Thank you for downloading the Quantcast Android SDK! This implementation guide provides steps for integrating the SDK, so you can take advantage of valuable, actionable insights:

* **Traffic Stats & Return Usage** - see your audience visitation trends
* **Combined Web / App Audiences** - understand your aggregate audience across all screens by taking a few additional steps outlined in this guide.
* **Labels** – segment your traffic by customizing views within your app.

If you have any implementation questions, please email mobilesupport@quantcast.com. We're here to help.

## Integrating Quantcast Measure for Mobile Apps ##

### Project Setup ###

There are two ways to integrate the Quantcast's SDK into your Android app. You can integrate via **external JAR** or you can integrate via **library project**.

Whichever way you choose to integrate you must first clone the Quantcast Android SDK by issuing the following command:

``` bash
git clone https://github.com/quantcast/android-measurement.git quantcast-android-measurement
```

#### Integrate via External JAR ####

Once you have the repository cloned, add the `QuantcastAndroidMeasurement.jar` within to your project by copying the file into your project's `libs/` directory. If you would like to keep the JAR external and are using Eclipse you can follow [this guide](http://developer.android.com/guide/faq/commontasks.html#addexternallibrary).

#### Integrate via Library Project ####

##### With Eclipse #####

Import the `QuantcastAndroidSdk` project into your workspace with the following steps:

1.	Go to **File > Import…**
2.	Select **Android > Existing Android Code Into Workspace** and click **Next >**
3.	For the **Root Directory** browse to `<repo cloning directory>/quantcast-android-measurement/QuantcastAndroidSdk`
4.	Make sure Copy projects into workspace is not checked
5.	Click **Finish**

Add a reference to the `QuantcastAndroidSdk` library project to your project with [this guide](http://developer.android.com/tools/projects/projects-eclipse.html#ReferencingLibraryProject).

##### Without Eclipse #####

Setup the `QuantcastAndroidSdk` project to be buildable with the following command:

``` bash
android update lib-project -p <repo cloning directory>/quantcast-android-measurement/QuantcastAndroidSdk/
```

Add a reference to the `QuantcastAndroidSdk` library project to your project with [this guide](http://developer.android.com/tools/projects/projects-cmdline.html#ReferencingLibraryProject)

Note: for the `android update project` command described in the guide be sure to make the `-library` option a relative bath to the project or else your project will not be able to build.

### Required Code Integration ###

1. 	In your project's `AndroidManifest.xml` you must ask for the required permissions by adding the following lines within the `<manifest>` tag before the `<application>` tag:

	``` xml
	<uses-permission android:name="android.permission.INTERNET" />
	<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
	```
	
	You can optionally add the following permissions to gather more information about your user base:
	
	``` xml
	<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
	```
	
	and
	
	``` xml
	<uses-permission android:name="android.permission.READ_PHONE_STATE" />
	```

	Also add the following lines within the `<application>` tag to allow the `AboutQuantcastScreen` to show:

	``` xml
	<activity android:name="com.quantcast.measurement.service.AboutQuantcastScreen" >
	</activity>
	```
2.	Import the `QuantcastClient` into every `Activity` in your project by adding the following import:

	``` java
	import com.quantcast.measurement.service.QuantcastClient;
	```
3.	In the `onCreate()` method of every `Activity` in your project, place the following to initialize the measurement service:

	``` java
	QuantcastClient.beginSessionWithApiKeyAndWithUserId(this, <*Insert your API Key Here*>, userIdentifier);
	```
	Replace "<\*Insert your API Key Here\*>" with your Quantcast API Key, which can be generated in your Quantcast account homepage on [the Quantcast website](http://www.quantcast.com "Quantcast.com"). The API Key is used as the basic reporting entity for Quantcast Measure. The same API Key can be used across multiple apps (i.e. AppName Free / AppName Paid) and/or app platforms (i.e. iOS / Android). For all apps under each unique API Key, Quantcast will report the aggregate audience among them all, and also identify/report on the individual app versions.
	
	The `userIdentifier` parameter is a `String` that uniquely identifies an individual user, such as an account login. Passing this information allows Quantcast to provide reports on your combined audience across all your properties: online, mobile web and mobile app. This parameter may be `null` if your app does not have a user identifier. If the user identifier is not known at the time the `onCreate()` method is called, the user identifier can be recorded at a later time. Please see the [Combined Web/App Audiences](#combined-webapp-audiences) section for more information.
	
4.	In the `onDestroy()` method of every `Activity` in your project place the following to clean up the measurement service:

	``` java
	QuantcastClient.endSession(this);
	```
	
5.	In the `onPause()` method of every `Activity` in your project place the following:

	``` java
	QuantcastClient.pauseSession();
	```
	
6.	In the `onResume()` method of every `Activity` in your project place the following:

	``` java
	QuantcastClient.resumeSession();
	```
### User Privacy ###

#### Privacy Notification ####
Quantcast believes in informing users of how their data is being used.  We recommend that you disclose in your privacy policy that you use Quantcast to understand your audiences. You may link to Quantcast's privacy policy [here](https://www.quantcast.com/privacy).

#### User Opt-Out ####

You can give users the option to opt out of Quantcast Measure by providing access to the About Quantcast Screen. This should be a button in your app's preferences `Activity` with the title "About Quantcast". When the user taps the button you provide, call `AboutQuantcastScreen` with the following:

``` java
QuantcastClient.showAboutQuantcastScreen(activity);
```
	
`activity` is your project's preference `Activity`.
	
Note: when a user opts out of Quantcast Measure, the Quantcast Android SDK immediately stops transmitting information to or from the user's device and deletes any cached information that may have retained. Furthermore, when a user opts out of a single app on a device, the action affects all other apps on the device that are integrated with Quantcast Measure the next time they are launched.

### Optional Code Integrations ###

#### Tracking App Events ####

Quantcast Measure can be used to measure audiences that engage in certain activities within your app. To log the occurrence of an app event or activity, call the following method:

``` java
QuantcastClient.logEvent(eventName);
```

`eventName` is the `String` that is associated with the event you are logging. Hierarchical information can be indicated by using a left-to-right notation with a period as a separator. For example, logging one event named "button.left" and another named "button.right" will create three reportable items in Quantcast Measure: "button.left", "button.right", and "button". There is no limit on the cardinality that this hierarchal scheme can create, though low-frequency events may not have an audience report on due to the lack of a statistically significant population.

#### Event Labels ####

Most of Quantcast's public methods have an option to provide a single `String` label or a `String[]` of labels. A label is any arbitrary string that you want associated with an event. The label will create a second dimension in Quantcast Measure audience reporting. Normally, this dimension is a "user class" indicator. For example, use one of two labels in your app: one for user who have not purchased an app upgrade, and one for users who have purchased an upgrade.

While there is no constraint on the intended use of the label dimension, it is not recommended that you use it to indicate discrete events; in these cases, use the `logEvent(eventName)` method.

#### Geo-Location Measurement ####

To get geo-location aware reporting, turn on geo-tracking in the `onCreate()` method of every `Activity` in your project before you call `beginSessionWithApiKey()` with the following:

``` java
QuantcastClient.setEnableLocationGathering(true);
```

Note: only enable geo-tracking if your app has some location-aware purpose.

The Quantcast Android SDK will automatically pause geo-tracking while your app is in the background. This is done for both battery-life and privacy considerations.

#### Combined Web/App Audiences ####

Quantcast Measure enables you to measure your combined web and mobile app audiences, allowing you to understand the differences and similarities of your online and mobile app audiences, or even the combined audiences of your different apps. To enable this feature, you will need to provide a user identifier, which Quantcast will always anonymize with a 1-way hash before it is transmitted from the user's device. This user identifier should also be provided for your website(s); please see Quantcast's web measurement documentation for instructions.

Normally, your app user identifier would be provided in the `onCreate()` method of any `Activity` of your project via the `QuantcastClient.beginSessionWithApiKeyAndWithUserId()` method as described in the [Required Code Integration](#required-code-integration) section above. If the app's active user identifier changes later in the app's life cycle, you can update the user identifier using the following method call:

``` java
QuantcastClient.recordUserIdentifier(userIdentifier);
```
The `userIdentifier` parameter is a `String` containing the user identifier.

Note that in all cases, the Quantcast Android SDK will immediately 1-way hash the passed app user identifier, and return the hashed value for your reference. You do not need to take any action with the hashed value.

### SDK Customization ###

#### Logging and Debugging ####

You may enable logging within the Quantcast Android SDK for debugging purposes. By default, logging is turned off. To enable logging, set the log level of the Quantcast Android SDK by calling:

``` java
QuantcastClient.setLogLevel(Log.VERBOSE);
```

The log level should be one of `Log.VERBOSE`, `Log.DEBUG`, `Log.INFO`, `Log.WARN`, `Log.ERROR`. The default log level for the Quantcast Android SDK is `Log.ERROR`.

Everything logged by the Quantcast Android SDK will have a tag beginning with "q.".

##### Event Upload Frequency #####

The Quantcast Android SDK will upload the events it collects to Quantcast's server periodically. Uploads that occur too often will drain the device's battery. Uploads that don't occur often enough will cause significant delays in Quantcast receiving the data needed for analysis and reporting. By default, these uploads occur when at least 100 events have been collected or when your application pauses (that is, it switched into the background). You can alter this default behavior via `QuantcastClient.setUploadEventCount()`. For example, if you wish to upload your app's events after 20 events have been collected, you would make the following call:

```java
QuantcastClient.setUploadEventCount(20)
```

You may change this property multiple times throughout your app's execution.

##### Secure Data Uploads #####

The Quantcast Android SDK can support secure data uploads using SSL/TLS. In order to enable secure data uploads you must make the following call:

```java
QuantcastClient.setUsingSecureConnections(true);
```

Note that using secure data uploads causes your app to use encryption technology. Various jurisdictions have laws controlling the export of software applications that use encryption. Please review your jurisdiction's laws concerning exporting software that uses encryption before enabling secure data uploads in the Quantcast Android SDK.

### License ###

This Quantcast Measurement SDK is Copyright 2012 Quantcast Corp. This SDK is licensed under the Quantcast Mobile App Measurement Terms of Service, found at [the Quantcast website here](https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos "Quantcast's Measurement SDK Terms of Service") (the "License"). You may not use this SDK unless (1) you sign up for an account at [Quantcast.com](https://www.quantcast.com "Quantcast.com") and click your agreement to the License and (2) are in compliance with the License. See the License for the specific language governing permissions and limitations under the License.
