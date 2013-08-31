/**
 * Copyright 2012 Quantcast Corp.
 *
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 *  compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package com.quantcast.measurement.service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebView;

/**
 * Client API for Quantcast Measurement service.
 * <p/>
 * This exposes only those methods that may be called by developers using the Quantcast Measurement API.
 */
public class QuantcastClient {

    /**
     * Used when initially starting the SDK in the main activity.  This should be called in EVERY Activity's onStart() method.  The context and api key are required.
     *
     * @param context The activity context.  Note that the SDK will use the application context.
     * @param apiKey  The Quantcast API key that activity for this app should be reported under. Obtain this key from the Quantcast website.
     * @param userId  (Optional) A consistent identifier for the current user.
     *                Any user identifier recorded will be save for all future session until it a new user identifier is recorded.
     *                Record a user identifier of {@link null} should be used for a log out and will remove any saved user identifier.
     * @param labels  (Optional) A label is any arbitrary string that you want to be associated with this event, and will create a
     *                second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *                For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *                and one for users who have purchased an upgrade.
     */
    public static String activityStart(Context context, String apiKey, String userId, String[] labels) {
        return QCMeasurement.INSTANCE.startUp(context, apiKey, userId, labels);
    }

    /**
     * Convenience method for all subsequent Activity onStart() calls after the apiKey has been already given for an app
     *
     * @param context The activity context.  Note that the SDK will use the application context.
     */
    public static void activityStart(Context context) {
        activityStart(context, null, null, null);
    }

    /**
     * Convenience method for all subsequent Activity onStart() calls after the apiKey has been already given for an app
     *
     * @param context The activity context.  Note that the SDK will use the application context.
     * @param labels  (Optional) A label is any arbitrary string that you want to be associated with this event, and will create a
     *                second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *                For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *                and one for users who have purchased an upgrade.
     */
    public static void activityStart(Context context, String[] labels) {
        activityStart(context, null, null, labels);
    }

    /**
     * Cleans up any connections or data being collected by Quantcast SDK.  Should be called in every Activity's onStop()
     */
    public static void activityStop() {
        activityStop(null);
    }

    /**
     * Cleans up any connections or data being collected by Quantcast SDK.  Should be called in every Activity's onStop()
     *
     * @param labels (Optional) A label is any arbitrary string that you want to be associated with this event, and will create a
     *               second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *               For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *               and one for users who have purchased an upgrade.
     */
    public static void activityStop(String[] labels) {
        QCMeasurement.INSTANCE.stop(labels);
    }

    /**
     * Log a user identifier to the service. This will begin a new measurement session
     *
     * @param userId A consistent identifier for the current user.
     *               Any user identifier recorded will be save for all future session until it a new user identifier is recorded.
     *               Record a user identifier of {@link null} should be used for a log out and will remove any saved user identifier.
     */
    public static String recordUserIdentifier(String userId) {
        return recordUserIdentifier(userId, null);
    }

    public static String recordUserIdentifier(String userId, String[] labelsOrNull) {
        return QCMeasurement.INSTANCE.recordUserIdentifier(userId, labelsOrNull);
    }

    /**
     * Logs an app-defined event can be arbitrarily defined.
     *
     * @param name A string that identifies the event being logged. Hierarchical information can be indicated by using a left-to-right notation with a period as a separator.
     *             For example, logging one event named "button.left" and another named "button.right" will create three reportable items in Quantcast App Measurement:
     *             "button.left", "button.right", and "button".
     *             There is no limit on the cardinality that this hierarchical scheme can create,
     *             though low-frequency events may not have an audience report on due to the lack of a statistically significant population.
     */
    public static void logEvent(String name) {
        logEvent(name, new String[0]);
    }

    /**
     * Logs an app-defined event can be arbitrarily defined.
     *
     * @param name  A string that identifies the event being logged. Hierarchical information can be indicated by using a left-to-right notation with a period as a separator.
     *              For example, logging one event named "button.left" and another named "button.right" will create three reportable items in Quantcast App Measurement:
     *              "button.left", "button.right", and "button".
     *              There is no limit on the cardinality that this hierarchical scheme can create,
     *              though low-frequency events may not have an audience report on due to the lack of a statistically significant population.
     * @param label (Optional) A label is any arbitrary string that you want to be associated with this event, and will create a
     *              second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *              For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *              and one for users who have purchased an upgrade.
     */
    public static void logEvent(String name, String label) {
        logEvent(name, new String[]{label});
    }

    /**
     * Logs an app-defined event can be arbitrarily defined.
     *
     * @param name   A string that identifies the event being logged. Hierarchical information can be indicated by using a left-to-right notation with a period as a separator.
     *               For example, logging one event named "button.left" and another named "button.right" will create three reportable items in Quantcast App Measurement:
     *               "button.left", "button.right", and "button".
     *               There is no limit on the cardinality that this hierarchical scheme can create,
     *               though low-frequency events may not have an audience report on due to the lack of a statistically significant population.
     * @param labels (Optional) A label is any arbitrary string that you want to be associated with this event, and will create a
     *               second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *               For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *               and one for users who have purchased an upgrade.
     */
    public static void logEvent(String name, String[] labels) {
        QCMeasurement.INSTANCE.logEvent(name, labels);
    }

    /**
     * Set the number of events required to trigger an upload.
     * This is defaulted to 100
     *
     * @param uploadEventCount The number of events required to trigger an upload.
     *                         This must be greater than 1 and less than or equal to 200.
     */
    public static void setUploadEventCount(int uploadEventCount) {
        QCMeasurement.INSTANCE.setUploadEventCount(uploadEventCount);
    }

    /**
     * Use this to control whether or not the service should collect location data. You should only enabled location gathering if your app has some location-aware purpose.
     *
     * @param enableLocationGathering Set to true to enable location, false to disable
     */
    public static void setEnableLocationGathering(boolean enableLocationGathering) {
        QCMeasurement.INSTANCE.setLocationEnabled(enableLocationGathering);
    }

    /**
     * Control whether or not the SDK will secure data uploads using SSl/TLS.
     *
     * @param usingSecureConnections Whether or not the SDK will secure data uploads using SSl/TLS.
     */
    public static void setUsingSecureConnections(boolean usingSecureConnections) {
        QCMeasurement.INSTANCE.setUsesSecureConnection(usingSecureConnections);
    }

    public static boolean isUsingSecureConnections() {
        return QCMeasurement.INSTANCE.usesSecureConnection();
    }

    /**
     * Show the About Quantcast Screen via {@link Activity#startActivity(Intent)}.
     *
     * @param activity The activity to create the About Quantcast Screen Activity. This activity will be returned to when the user is finished.
     */
    public static void showAboutQuantcastScreen(Activity activity) {
        activity.startActivity(new Intent(activity, AboutQuantcastScreen.class));
    }

    /**
     * A webview that should be used when accessing a Quantcast tagged webpage.  This will prevent the web pixel as well as the app SDK from firing for the same visit.
     *
     * @param context The activity context.  Note that the SDK will use the application context.
     */
    public static WebView newDeduplicatedWebView(Context context) {
        return new QCDeduplicatedWebView(context);
    }


    /**
     * Allows you to change what logs will actually be reported by Quantcast Measurement Service classes
     *
     * @param logging If the SDk should display full logs or just error logs.   Logging should be turned off for Distribution builds.
     */
    public static void enableLogging(boolean logging) {
        if (logging) {
            QCLog.setLogLevel(Log.VERBOSE);
        } else {
            QCLog.setLogLevel(Log.ERROR);
        }
    }

    /**
     * Can be called to check the opt-out status of the Quantcast Service.
     * If collection is not enabled the user has opted-out.
     *
     * @param context Main Activity using the Quantcast Measurement API
     */
    public static boolean isCollectionEnabled(Context context) {
        return QCOptOutUtility.isOptedOut(context);
    }


    /**
     * Start a new measurement session. Should be called in the main activity's onCreate method.
     *
     * @param activity Main Activity using the Quantcast Measurement API
     * @param apiKey   The Quantcast API key that activity for this app should be reported under. Obtain this key from the Quantcast website.
     * @deprecated use {@link #activityStart(android.content.Context, String, String, String[])} instead.
     */
    @Deprecated
    public static void beginSessionWithApiKey(Activity activity, String apiKey) {
        beginSessionWithApiKeyAndWithUserId(activity, apiKey, null);
    }

    /**
     * Start a new measurement session. Should be called in the main activity's onCreate method.
     *
     * @param activity Main Activity using the Quantcast Measurement API
     * @param apiKey   The Quantcast API key that activity for this app should be reported under. Obtain this key from the Quantcast website.
     * @param label    A label for the event.
     * @deprecated use {@link #activityStart(android.content.Context, String, String, String[])} instead.
     */
    @Deprecated
    public static void beginSessionWithApiKey(Activity activity, String apiKey, String label) {
        beginSessionWithApiKeyAndWithUserId(activity, apiKey, null, label);
    }

    /**
     * Start a new measurement session. Should be called in the main activity's onCreate method.
     *
     * @param activity Main Activity using the Quantcast Measurement API
     * @param apiKey   The Quantcast API key that activity for this app should be reported under. Obtain this key from the Quantcast website.
     * @param labels   An array of labels for the event.
     * @deprecated use {@link #activityStart(android.content.Context, String, String, String[])} instead.
     */
    @Deprecated
    public static void beginSessionWithApiKey(Activity activity, String apiKey, String[] labels) {
        beginSessionWithApiKeyAndWithUserId(activity, apiKey, null, labels);
    }

    /**
     * Start a new measurement session. Should be called in the main activity's onCreate method.
     *
     * @param activity Main Activity using the Quantcast Measurement API
     * @param apiKey   The Quantcast API key that activity for this app should be reported under. Obtain this key from the Quantcast website.
     * @param userId   A consistent identifier for the current user.
     *                 Any user identifier recorded will be save for all future session until it a new user identifier is recorded.
     *                 Record a user identifier of {@link null} should be used for a log out and will remove any saved user identifier.
     * @deprecated use {@link #activityStart(android.content.Context, String, String, String[])} instead.
     */
    @Deprecated
    public static void beginSessionWithApiKeyAndWithUserId(Activity activity, String apiKey, String userId) {
        beginSessionWithApiKeyAndWithUserId(activity, apiKey, userId, new String[0]);
    }

    /**
     * Start a new measurement session. Should be called in the main activity's onCreate method.
     *
     * @param activity Main Activity using the Quantcast Measurement API
     * @param apiKey   The Quantcast API key that activity for this app should be reported under. Obtain this key from the Quantcast website.
     * @param userId   A consistent identifier for the current user.
     *                 Any user identifier recorded will be save for all future session until it a new user identifier is recorded.
     *                 Record a user identifier of {@link null} should be used for a log out and will remove any saved user identifier.
     * @param label    A label for the event.
     * @deprecated use {@link #activityStart(android.content.Context, String, String, String[])} instead.
     */
    @Deprecated
    public static void beginSessionWithApiKeyAndWithUserId(Activity activity, String apiKey, String userId, String label) {
        beginSessionWithApiKeyAndWithUserId(activity, apiKey, userId, new String[]{label});
    }

    /**
     * Start a new measurement session. Should be called in the main activity's onCreate method.
     *
     * @param activity Main Activity using the Quantcast Measurement API
     * @param apiKey   The Quantcast API key that activity for this app should be reported under. Obtain this key from the Quantcast website.
     * @param userId   A consistent identifier for the current user.
     *                 Any user identifier recorded will be save for all future session until it a new user identifier is recorded.
     *                 Record a user identifier of {@link null} should be used for a log out and will remove any saved user identifier.
     * @param labels   An array of labels for the event.
     * @deprecated use {@link #activityStart(android.content.Context, String, String, String[])} instead.
     */
    @Deprecated
    public static void beginSessionWithApiKeyAndWithUserId(Activity activity, String apiKey, String userId, String[] labels) {
        activityStart(activity, apiKey, userId, labels);
    }

    /**
     * Logs a pause event as well as evoking some internal maintenance. This should be called in the main activity's onPause method
     *
     * @deprecated use {@link #activityStop()} instead.
     */
    @Deprecated
    public static void pauseSession() {
        pauseSession(new String[0]);
    }

    /**
     * Logs a pause event as well as evoking some internal maintenance. This should be called in the main activity's onPause method
     *
     * @param label A label for the event.
     * @deprecated use {@link #activityStop(String[])} instead.
     */
    @Deprecated
    public static void pauseSession(String label) {
        pauseSession(new String[]{label});
    }

    /**
     * Logs a pause event as well as evoking some internal maintenance. This should be called in the main activity's onPause method
     *
     * @param labels An array of labels for the event.
     * @deprecated use {@link #activityStop(String[])} instead.
     */
    @Deprecated
    public static void pauseSession(String[] labels) {
        activityStop(labels);
    }

    /**
     * Logs a resume event as well as evoking some internal maintenance. This should be called in the main activity's onResume method
     *
     * @deprecated use {@link #activityStart(android.content.Context)} instead.
     */
    @Deprecated
    public static void resumeSession() {
        resumeSession(new String[0]);
    }

    /**
     * Logs a resume event as well as evoking some internal maintenance. This should be called in the main activity's onResume method
     *
     * @param label A label for the event.
     * @deprecated use {@link #activityStart(android.content.Context, String[])} instead.
     */
    @Deprecated
    public static void resumeSession(String label) {
        resumeSession(new String[]{label});
    }

    /**
     * Logs a resume event as well as evoking some internal maintenance. This should be called in the main activity's onResume method
     *
     * @param labels An array of labels for the event.
     * @deprecated use {@link #activityStart(android.content.Context, String[])} instead.
     */
    @Deprecated
    public static void resumeSession(String[] labels) {
        activityStart(null, labels);
    }

    /**
     * Ends the current measurement session. This will clean up all of the services resources. This should be called in the main activity's onDestroy method.
     *
     * @deprecated use {@link #activityStop()}  instead.
     */
    @Deprecated
    public static void endSession(Activity activity) {
        endSession(activity, new String[0]);
    }

    /**
     * Ends the current measurement session. This will clean up all of the services resources. This should be called in the main activity's onDestroy method.
     *
     * @param label A label for the event.
     * @deprecated use {@link #activityStop(String[])}  instead.
     */
    @Deprecated
    public static void endSession(Activity activity, String label) {
        endSession(activity, new String[]{label});
    }

    /**
     * Ends the current measurement session. This will clean up all of the services resources. This should be called in the main activity's onDestroy method.
     *
     * @param labels An array of labels for the event.
     * @deprecated use {@link #activityStop(String[])}  instead.
     */
    @Deprecated
    public static void endSession(Activity activity, String[] labels) {
        activityStop(labels);
    }

    /**
     * Can be called to check the opt-out status of the Quantcast Service.
     * If collection is not enabled the user has opted-out.
     * The opt-out status is not guaranteed to be available at the time of the call.
     * Therefore it must be communicated via callback.
     *
     * @param context  Main Activity using the Quantcast Measurement API
     * @param callback The action to be taken when the opt-out status is available
     * @deprecated Callback no longer required.  use {@link #isCollectionEnabled(android.content.Context)}  instead.
     */
    @Deprecated
    public static void isCollectionEnabled(Context context, final CollectionEnabledCallback callback) {
        callback.callback(QCOptOutUtility.isOptedOut(context));
    }

    /**
     * Helper callback class to allow for asynchronous reaction to requests for opt-out status
     *
     * @deprecated Callback no longer required.  use {@link #isCollectionEnabled(android.content.Context)}  instead.
     */
    @Deprecated
    public static interface CollectionEnabledCallback {

        /**
         * Called when opt-out status is available
         *
         * @param collectionEnabled The current opt-out status. If collection is not enabled the user has opted-out.
         */
        public void callback(boolean collectionEnabled);

    }

    /**
     * Allows you to change what logs will actually be reported by Quantcast Measurement Service classes
     *
     * @param logLevel The log level for Quantcast Measurement Service classes. This should be one of Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR
     * @deprecated For easier use logs are just on or off.  use {@link #enableLogging(boolean)}  instead.
     */
    @Deprecated
    public static void setLogLevel(int logLevel) {
        QCLog.setLogLevel(logLevel);
    }

    /**
     * Legacy. No longer logs anything
     *
     * @deprecated For easier use logs are just on or off.  use {@link #enableLogging(boolean)}  instead.
     */
    @Deprecated
    public static void logRefresh() {
        // Do nothing
    }

    /**
     * Legacy. No longer logs anything
     *
     * @deprecated For easier use logs are just on or off.  use {@link #enableLogging(boolean)}  instead.
     */
    @Deprecated
    public static void logUpdate() {
        // Do nothing
    }
}
