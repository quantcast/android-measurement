# Quantcast Android Measurement SDK #

## Integrating Quantcast Measurement ##

### Project Setup ###

There are two ways to integrate the Quantcast Measurement SDK into your Android app. You can intergrate via **external JAR** or you can integrate via **library project**.

Whichever way you choose to integrate you must first clone the Quantcast Android Measurement SDK by issueing the following command:

``` bash
git clone https://github.com/quantcast/android-measurement.git quantcast-android-measurement
```

#### Integrate via External JAR ####

Once you have the repository cloned you need to add the `QuantcastService.jar` within to your project. You can do this by simply copying the file into your project's `libs/` directory or if you would like to keep the JAR external and are using Eclipse you can follow [this guide](http://developer.android.com/guide/faq/commontasks.html#addexternallibrary).

#### Integrate via Library Project ####

##### With Eclipse #####

Import the `QuantcastService` project into your workspace with the following steps:

1.	Go to **File > Import…**
2.	Select **Android > Existing Android Code Into Workspace** and click **Next >**
3.	For the **Root Directory** browse to `<repo cloning directory>/quantcast-android-measurement/QuantcastService`
4.	Make sure Copy projects into workspace is not checked
5.	Click **Finish**

Add a reference to the `QuantcastService` library project to your project with [this guide](http://developer.android.com/tools/projects/projects-eclipse.html#ReferencingLibraryProject).

##### Without Eclipse #####

Setup the `QuantcastService` project to be buildable with the following command:

``` bash
android update lib-project -p <repo cloning directory>/quantcast-android-measurement/QuantcastService/
```

Add a reference to the `QuantcastService` library project to your project with [this guide](http://developer.android.com/tools/projects/projects-cmdline.html#ReferencingLibraryProject)

Note: For the `android update project` command described in the guide be sure to make the `-library` option a relative bath to the project or else your project will not be able to build.

### Required Code Integration ###

1. 	In your project's `AndroidManifest.xml` you must ask for the required permissions by adding the following lines within the `<manifest>` tag before the `<application>` tag:

	``` xml
	<uses-permission android:name="android.permission.INTERNET" />
	<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
	<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
	```
	
	You can optionaly add the following permissions to gather more information about your userbase:
	
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
2.	Import the `QuantcastClient` into every `Acitivity` in your project by adding the following import:

	``` java
	import com.quantcast.measurement.service.QuantcastClient;
	```
3.	In the `onCreate()` method of every `Activity` in your project place the following to initialize the measurement service:

	``` java
	QuantcastClient.beginSessionWithApiKey(this, <*Insert your API Key Here*>);
	```
	Replacing "<\*Insert your API Key Here\*>" with your Quantcast API Key, which you can generate in your Quantcast account homepage on [the Quantcast website](http://www.quantcast.com "Quantcast.com"). 
	
Note that the API Key is used as basic reporting entity for Quantcast Measurement. You can use the same API Key across multiple apps across multiple platforms, and Quantcast will report the aggregate audience amongst them all. Quantcast will identify and report on the individual app versions seen under the API Key, but the intent is that the API Key is used for a logical grouping of apps. For example, you may have a "lite" and "full" version of an app that you group together with the same API Key.
	
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

### Optional Code Integrations ###

#### User Opt-Out ####

You may offer your app users the ability to opt-out of Quantcast Measurement. This is done by providing your users a means to access the About Quantcast Screen. This should be a button in your app's preferences `Activity` with the title "About Quantcast". When the user taps the button you provide, you should call the Quantcast's Measurement SDK's `AboutQuantcastScreen` with the following:

``` java
QuantcastClient.showAboutQuantcastScreen(activity);
```
	
Where `activity` is your project's preference `Activity`.
	
Note that when a user opts-out of Quantcast Measurement, it causes the SDK to immediately stop transmitting information to or from the user's device and it deletes any cached information that the SDK may have retained. Furthermore, when a user opts-out of a single app on a device, it affects all apps on the device that are using Quantcast Measurement.

#### Tracking App Events ####

You may use Quantcast App Measurement to measure the audiences that engage in certain activities within your app. In order to log the occurrence of an app event or activity, simply call the following method:

``` java
QuantcastClient.logEvent(eventName);
```

Here `eventName` is a `String` that is meaningful to you and is associated with the event you are logging. Note that hierarchical information can be indicated by using a left-to-right notation with a period as a seperator. For example, logging one event named "button.left" and another named "button.right" will create three reportable items in Quantcast App Measurement: "button.left", "button.right", and "button". There is no limit on the cardinality that this hierarchal scheme can create, though low-frequency events may not have an audience report on due to the lack of a statistically significant population.

#### Event Labels ####

Most of Quantcast Measurement SDK's public methods have an option to provide a comma separated list of labels. A label is any arbitrary string that you want to be ascociated with an event, and will create a second dimension in Quantcast Measurement audience reporting. Normally, this dimension is a "user class" indicator. For example, you might use one of two labels in your app: one for user who have not purchased an app upgrade, and one for users who have purchased an upgrade.

While there is no specific constraint on the intended use of the label dimension, it is not recommended that you use it to indicate discrete events. You should use the `logEvent(eventName)` method to do that.

#### Geo-Location Measurement ####

If you would like to get geo-location aware reporting, you must turn on geo-tracking in the Measurement SDK. You do this in the `onCreate()` method of every `Activity` in your project before you call `beginSessionWithApiKey()` with the following:

``` java
QuantcastClient.setEnableLocationGathering(true);
```

Note that you should only enable geo-tracking if your app has some location-aware purpose.

The Quantcast Measurement SDK will automatically pause geo-tracking while your app is in the background. This is done for both battery-life and privacy considerations.

#### Combined Web/App Audiences ####

Quantcast App Measurement enables you to measure your web and app audience. This allows you to use Quantcast Measurement to understand the differences and similarities of your online and app audiences, or even between different apps that you publish. In order to enable this feature, your will need to provide a user identifier, which Quantcast will always anonymize with a 1-way hash before it is transmitted off the user's device. This user identifier also needs to be provided in your website(s); please see Quantcast's web measurement documentation for specific instructions on how to provide an user identifier for your website.

In order to provide Quantcast Measurement with the user identifier, call the following method:

``` java
QuantcastClient.recordUserIdentifier(userId);
```
Where `userId` is a `String` containing the user identifier that you use. The SDK will immediately 1-way hash the passed identifier. A `null` `userId` indicates that the current user is uknown.

When starting a Quantcast Measurement session, if you already know the user identifier (e.g., it was saved in the apps preferences) when the `onCreate()` method of any `Activity` is called, you may call the alternate version of the `beginSessionWithApiKey()` method:

``` java
QuantcastClient.beginSessionWithApiKeyAndWithUserId(this, <*Insert your API Key Here*>, userId);
```

*Important*: Use of this feature requires certain notice and disclosures to your website and app users. Please see Quantcast's terms of service for more details.

### Logging and Debugging ###

You can set the log level of the Quantcast Measurement SDK by calling:

``` java
QuantcastClient.setLogLevel(Log.VERBOSE);
```

The log level should be one of `Log.VERBOSE`, `Log.DEBUG`, `Log.INFO`, `Log.WARN`, `Log.ERROR`. The default log level for the Quantcast Measurement SDK is `Log.ERROR`.

Everything logged by the Quantcast Measurement SDK will have a tag beginning with "q.".

### License ###

This software is licensed under the Quantcast Mobile API Beta Evaluation Agreement and may not be used except as permitted thereunder or copied, modified, or distributed in any case.