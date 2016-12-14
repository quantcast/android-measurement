/**
 * © Copyright 2016 Quantcast Corp.
 *
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 * compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License. Unauthorized use of this file constitutes
 * copyright infringement and violation of law.
 */
package com.quantcast.measurement.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

/**
 * Client API for Quantcast Measurement service.
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
     *                Record a user identifier of null should be used for a log out and will remove any saved user identifier.
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
        activityStart(context, null);
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
        activityStop((String) null);
    }

    /**
     * Cleans up any connections or data being collected by Quantcast SDK.  Should be called in every Activity's onStop()
     *
     * @param labels (Optional) A label is any arbitrary string that you want to be associated with this event, and will create a
     *               second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *               For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *               and one for users who have purchased an upgrade.
     */
    public static void activityStop(String... labels) {
        QCMeasurement.INSTANCE.stop(labels);
    }

    /**
     * Used when initially starting the SDK in the Application file.  This only needs to be called once in the OnCreate() method of the Application
     * When using this method, you do not ever need to call activityStart() and activityStop().  Also be sure that you declare your Application subclass in the Android manifest.
     * The Application object and and api key are required.
     *
     * @param app     The Application object of your app.
     * @param apiKey  The Quantcast API key that activity for this app should be reported under. Obtain this key from the Quantcast website.
     * @param userId  (Optional) A consistent identifier for the current user.
     *                Any user identifier recorded will be save for all future session until it a new user identifier is recorded.
     *                Record a user identifier of null should be used for a log out and will remove any saved user identifier.
     * @param labels  (Optional) A label is any arbitrary string that you want to be associated with this event, and will create a
     *                second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *                For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *                and one for users who have purchased an upgrade.
     */
    @TargetApi(14)
    public static void startQuantcast(Application app, final String apiKey, final String userId, final String[] labels) {
        if (Build.VERSION.SDK_INT < 14) {
            QCLog.Tag t = new QCLog.Tag(QuantcastClient.class);
            QCLog.e(t, "This method requires Android API level of 14 or above. You must use activityStart instead.");
        } else {
            app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                private boolean hasInstanceData = false;
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    hasInstanceData = savedInstanceState != null;
                }

                @Override
                public void onActivityStarted(Activity activity) {
                    if(!hasInstanceData) {
                        QuantcastClient.activityStart(activity, apiKey, userId, labels);
                    }
                }

                @Override
                public void onActivityResumed(Activity activity) {
                }

                @Override
                public void onActivityPaused(Activity activity) {
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    if(!activity.isChangingConfigurations()) {
                        QuantcastClient.activityStop(labels);
                    }
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                }
            });
        }
    }

    /**
     * Log a user identifier to the service. This will begin a new measurement session
     *
     * @param userId A consistent identifier for the current user.
     *               Any user identifier recorded will be save for all future session until it a new user identifier is recorded.
     *               Record a user identifier of null should be used for a log out and will remove any saved user identifier.
     */
    public static String recordUserIdentifier(String userId) {
        return recordUserIdentifier(userId, (String)null);
    }

    public static String recordUserIdentifier(String userId, String... labelsOrNull) {
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
    public static void logEvent(String name, String... labels) {
        QCMeasurement.INSTANCE.logEvent(name, labels);
    }

    /**
     * Used to set static application labels.   When set, the label(s) will be automatically passed to all calls which take labels.
     * This is a convenience method for applications that segment their audience by a fairly static group of labels.
     * This property can be changed at any time.
     *
     * @param labels       An array of app segment labels
     */
    public static void setAppLabels(String... labels){
        QCMeasurement.INSTANCE.setAppLabels(labels);
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
     * Show the About Quantcast Screen.
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
     * Call this to show the Quantcast Privacy Policy.   This should be used if the application is manually setting the opt out status instead
     * of using the default {@link #showAboutQuantcastScreen(android.app.Activity)}
     *
     * @param activity The activity to create the Quantcast Privacy Policy Activity.
     */
    public static void showQuantcastPrivacyPolicy(Activity activity){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.quantcast.com/privacy/"));
        activity.startActivity(browserIntent);
    }

    /**
     * Can be called to check the opt-out status of the Quantcast Service.
     * If collection is not enabled the user has opted-out.
     *
     * @param context Main Activity using the Quantcast Measurement API
     */
    public static boolean isOptedOut(Context context) {
        return QCOptOutUtility.isOptedOut(context);
    }

    /**
     * Can be called to set the opt-out status of the Quantcast Service.
     * If collection is not enabled the user has opted-out.
     *
     * @param context Application Context using the Quantcast Measurement API
     * @param optOut true if the user is opted out, otherwise false.
     */
    public static void setOptOut(Context context, boolean optOut) {
        QCMeasurement.INSTANCE.setOptOut(context, optOut);
    }
}
