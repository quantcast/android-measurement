# Quantcast Android SDK Changelog #

## Version 0.2.3 ##

-	READ_PHONE_STATE permission is now optional
-	SDK updated to handle multiple activities
-	Enable/disable location gathering handled by method instead of attribute
-	Package restructure

### Implementation changes ###

-	The following line is no longer required in your app's `AndroidManifest.xml`:
	
	``` xml
	<uses-permission android:name="android.permission.READ_PHONE_STATE" />
	```
	
-	To allow the `AboutQuantcastScreen` to show you must now use:
	
	``` xml
	<activity android:name="com.quantcast.measurement.service.AboutQuantcastScreen" >
	</activity>
	```
	
-	The import line has changed to:

	``` java
	import com.quantcast.measurement.service.QuantcastClient;
	```
	
-	Required calls to the `QuantcastClient` should be done in every `Activity` of your project rather than just your project's main `Activity`
-	`QuantcastClient.beginSession()` now requires an `Activity` instead of just a `Context`
-	The required call in the `onDestroy()` method of an `Activity` has changed to:
	
	``` java
	QuantcastClient.endSession(this);
	```
	
-	To enable location gathering you must now call:
	
	``` java
	QuantcastClient.setEnableLocationGathering(true);
	```
	
## Version 0.2.4 ##

- 	Added API key implementation
- 	The option to record a user identifier at the beginning of a session has been added.
- 	Remover String vararg for labels

### Implementation Changes ###

- 	To start a session you now call:

	``` java
	QuantcastClient.beginSessionWithApiKey(this, <*Insert your API Key Here*>);
	```
	
	or
	
	``` java
	QuantcastClient.beginSessionWithApiKeyAndWithUserId(this, <*Insert your API Key Here*>, userId);
	```
	
-	To log an event with labels you must provide a single `String` label or and array of `String` labels.

## Version 0.3.0 ##

-	Various optimizations and bug fixes
-	Renamed project and built JAR

### Implementation Changes ###

-	The project JAR has been moved from `QuantcastService.jar` to `QuantcastAndroidSdk.jar`
-	The project directory has been moved from `QuantcastService` to `QuantcastAndroidSdk`

## Version 0.4.0 ##

-	Bug fixes
-	Added the capability to change the minimum event upload count
-	Added the capability to enable secure data uploads

### Implementation Changes ###

-	To change the minimum event upload count call:

	```java
	QuantcastClient.setUploadEventCount(20)
	```

-	To enable secure data uploads call:

	```java
	QuantcastClient.setUsingSecureConnections(true);
	```
	
## Version 0.4.1 ##

-	Smaller SDK footprint




