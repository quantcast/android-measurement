# Quantcast Android SDK Changelog #

## Version 1.4.0 ##
- Remove fallback to android device identifier.  If Google Play services is not linked, a null is returned.

## Version 1.3.1 ##
- Minor update to upload packet

## Version 1.3.0 ##
- Added new one-step application level tracking for apps targeting 14 and above
- Dropped support for all deprecated methods.
- minor bug fixes

## Version 1.2.7 ##
- Changed Opt Out function to be more concise.
- No Connection errors no longer logged
- minor bug fixes

## Version 1.2.5 ##
- Fixes #8 Fixes race condition in QCLocation
- Fixes #9 Remove BuildConfig.Debug from library file

## Version 1.2.4 ##
- Fixes #7 fileInputStream not closed properly
- small code refactor

## Version 1.2.3 ##
- Fix bug in opt out called before start

## Version 1.2.2 ##
- Update documentation for Google's Advertising ID
- fixes #5 - Exception handled
- minor bug fixes 

## Version 1.2.1 ##
- Updated documentation for Gradle support
- minor bug fixes

## Version 1.2.0 ##
- Added support for Google Play Advertising Identifier
- added static App and Network label segments
- Swapped AsyncTask for background thread handler
- Added programmatic way of opting out.

## Version 1.1.2 ##
-  fixed event enqueuing for older android devices.
-  bug fix for missing permissions on specific devices
-  updated sessioning
-  fixed opt out

## Version 1.1.1 ##
-  Added "network events" for the Network extension.

## Version 1.1.0 ##
-  Refactored location to be completely optional in src-optional directory
-  Added optional calls specifically for periodicals and magazine measurement
-  Added optional calls for Quantcast integration into 3rd party platforms.

## Version 1.0.1 ##
-  Fixed buffered reader calls.  Github issue #3

## Version 1.0.0 ##

- 	Simplified integration by adding `activityStart` and `activityStop`
-       Deprecated `beginSessionWithApiKey`, `pauseSession`, `resumeSession`, and `endSession`
-       Reduced library size


### Implementation Changes ###

- 	In every activity you now call:

	``` java
	QuantcastClient.activityStart(this, <*Insert your API Key Here*>, userIdOrNull, labelsOrNull);
	```
	
	or every subsequent activity after a session is already started from another activity
	
	``` java
	QuantcastClient.activityStart(this);
        ```

-       In every activity you must call
        ``` java
        QuantcastClient.activityStop
        ```

## Version 0.4.6 ##

- Github issue #1 fix: Null check for location

## Version 0.4.5 ##

- Stability improvement

## Version 0.4.4 ##

- Stability improvements and bug fixes

## Version 0.4.3 ##

- Performance Improvements

## Version 0.4.2 ##

-	Fixed file access issue

## Version 0.4.1 ##

-	Smaller SDK footprint

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
## Version 0.3.0 ##

-	Various optimizations and bug fixes
-	Renamed project and built JAR

### Implementation Changes ###

-	The project JAR has been moved from `QuantcastService.jar` to `QuantcastAndroidSdk.jar`
-	The project directory has been moved from `QuantcastService` to `QuantcastAndroidSdk`

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
	


