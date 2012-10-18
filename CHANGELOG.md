# Quantcast Android Measurement SDK Changelog #

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