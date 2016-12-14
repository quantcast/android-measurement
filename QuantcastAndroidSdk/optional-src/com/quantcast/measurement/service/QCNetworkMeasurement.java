
/**
 * Copyright 2016 Quantcast Corp.
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 * compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.quantcast.measurement.service;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 *  @class QuantcastMeasurement+Networks
 *  @abstract   This optional addition to QuantcastClient allows app networks and platforms to quantify all of their network app traffic while still
 *              permitting individual apps to be independently quantify, even under a separate network.
 *  @discussion This class should only be used by app networks, most notably app platforms, app development shops, and companies with a large number of branded apps
 *              where they want to maintain the app's brand when quantifying but still have the app traffic attributed to a parent network. Entities quantifying
 *              a single app or a number of apps under the same network should not use this class. If what is described as a "network integration"
 *              doesn't make sense to you, it is likely that you should not use the Network integration.
 *
 *   The Networks extension adds the ability to identify a parent network, referred to as an "attributed network", for each app in addition to or instead of
 *   the app's API Key. However, only apps with an API Key will get a full profile on Quantcast.com. Apps without an API Key or an API Key from a different network
 *   will (additionally) have their activity attributed to the attributed (parent) network as "syndicated app traffic", contributing to the parent network's
 *   overall network traffic and demographics. Furthermore, the Networks extension allows the assignment of app-specific and network-specifc labels.
 *   App-specific labels will be used to create audience segments under the network of the API Key of the app. Network labels will be used to create audience
 *   segments under the attributed network of the app. If the API Key's network and the attributed network are the same, then app labels and network labels
 *   will both create audience segments under that network.
 */

/**
 Detailed Implementation Notes
 ----------------------------

 To implement the Networks extension, you should use the methods declared under this Networks category rather than their original form equivalents
 from QuantcastMeasurement.h. The mapping for the original form methods to the Networks replacement is:

    Original Form Method                                       -- Networks Extension Method

    activityStart(Context, apiKey, userId,labels)              -- activityStart(context, apiKey, networkCode, userId, appLabels, networkLabels, isDirectAtKids)
    activityStart(Context, labels)                             -- activityStart(context, apiKey, networkCode, userId, appLabels, networkLabels, isDirectAtKids)
    activityStart(Context)                                     -- activityStart(context, apiKey, networkCode, userId, appLabels, networkLabels, isDirectAtKids)
    activityStop(labels)                                       -- activityStop(appLabels, networkLabels)
    activityStop()                                             -- activityStop(appLabels, networkLabels)
    recordUserIdentifier(userId,labelsOrNull)                  -- recordUserIdentifier(userId, appLabels, networkLabels)
    logEvent(eventName, labels)                                -- logEvent(eventName, appLabels, networkLabels)

 All methods listed above will generate an error if you mix usage of Original Form Methods with Networks Methods.

 */
public class QCNetworkMeasurement {
    private static final QCLog.Tag TAG = new QCLog.Tag(QCNetworkMeasurement.class);
    static final String QC_NETEVENT_KEY = "netevent";
    static final String QC_EVENT_NETEVENT = "netevent";

    /**
     * Used to set static network labels.   When set, the label(s) will be automatically passed to all calls which take labels.
     * This is a convenience method for applications that segment their audience by a fairly static group of labels.
     * This property can be changed at any time.
     *
     * @param labels       An array of network segment labels
     */
    public static void setNetworkLabels(String... labels){
        QCMeasurement.INSTANCE.setNetworkLabels(labels);
    }

    /**
     * Used when initially starting the SDK in the main activity.  This should be called in EVERY Activity's onStart() method.  The context and api key are required.
     *
     * @param context       The activity context.  Note that the SDK will use the application context.
     * @param apiKey        (Optional) The Quantcast API key that activity for this app should be reported under. Obtain this key from the Quantcast website.
     *                      If there is no apikey given, then a network code is required.
     * @param networkCode   (Optional) The network code where this traffic should also be attributed.  Obtain this key from the Quantcast website.  If there is no
     *                      networkCode given then and apiKey is required, but please consider using QuantcastClient.activityStart instead.
     * @param userId        (Optional) A consistent identifier for the current user.
     *                      Any user identifier recorded will be save for all future session until it a new user identifier is recorded.
     *                      Record a user identifier of null should be used for a log out and will remove any saved user identifier.
     * @param appLabels     (Optional) A application label is any arbitrary string that you want to be associated with this event, and will create a
     *                      second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *                      For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *                      and one for users who have purchased an upgrade.  These labels are only seen by the apiKey.
     * @param networkLabels (Optional) networkLabels are the same as labels but will be only seen by the network instead of the apiKey.
     * @param isDirectAtKids Whether or not this app is known to be specifically targeted for children 13 and under.  In compliance with COPPA
     *                       and other international laws, if an app is directly targeted at children 13 and under certain measures must
     *                       be taken to stop tracking of this audience.
     */
    public static String activityStart(Context context, String apiKey, String networkCode, String userId, String[] appLabels, String[] networkLabels, boolean isDirectAtKids) {
        if(networkCode == null){
            QCLog.e(TAG, "Network p-code is null. If no network p-code is required, then please consider using the QuantcastClient.activityStart version.");
        }
        return QCMeasurement.INSTANCE.startUp(context, apiKey, networkCode, userId, appLabels, networkLabels, isDirectAtKids);
    }

    public static String activityStart(Context context){
        if(!QCMeasurement.INSTANCE.hasNetworkCode()){
            QCLog.e(TAG, "Network labels will be ignored without starting with a network code.  Call QCNetworkMeasurement.activityStart on Activity onStart to set a network code");
        }
        return QCMeasurement.INSTANCE.startUp(context, null, null, null, null, null, false);
    }

    /**
     * Cleans up any connections or data being collected by Quantcast SDK.  Should be called in every Activity's onStop()
     *
     * @param appLabels     (Optional) A application Label is any arbitrary string that you want to be associated with this event, and will create a
     *                      second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *                      For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *                      and one for users who have purchased an upgrade.
     * @param networkLabels (Optional) networkLabels are the same as labels but will be only seen by the network instead of the apiKey.
     */
    public static void activityStop(String[] appLabels, String[] networkLabels) {
        if(!QCMeasurement.INSTANCE.hasNetworkCode()){
            QCLog.e(TAG, "Network labels will be ignored without starting with a network code.  Call QCNetworkMeasurement.activityStart on Activity onStart to set a network code");
        }
        QCMeasurement.INSTANCE.stop(appLabels, networkLabels);
    }

    /**
     * Log a user identifier to the service. This will begin a new measurement session
     *
     * @param userId       A consistent identifier for the current user.
     *                     Any user identifier recorded will be save for all future session until it a new user identifier is recorded.
     *                     Record a user identifier of null should be used for a log out and will remove any saved user identifier.
     * @param appLabels    (Optional) An application label is any arbitrary string that you want to be associated with this event, and will create a
     *                      second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *                      For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *                      and one for users who have purchased an upgrade.
     * @param networkLabels (Optional) networkLabels are the same as labels but will be only seen by the network instead of the apiKey.
     */
    public static String recordUserIdentifier(String userId, String[] appLabels, String[] networkLabels) {
        if(!QCMeasurement.INSTANCE.hasNetworkCode()){
            QCLog.e(TAG, "Network labels will be ignored without starting with a network code.  Call QCNetworkMeasurement.activityStart on Activity onStart to set a network code");
        }
        return QCMeasurement.INSTANCE.recordUserIdentifier(userId, appLabels, networkLabels);
    }

    /**
     * Logs an app-defined event can be arbitrarily defined.
     *
     * @param name   A string that identifies the event being logged. Hierarchical information can be indicated by using a left-to-right notation with a period as a separator.
     *               For example, logging one event named "button.left" and another named "button.right" will create three reportable items in Quantcast App Measurement:
     *               "button.left", "button.right", and "button".
     *               There is no limit on the cardinality that this hierarchical scheme can create,
     *               though low-frequency events may not have an audience report on due to the lack of a statistically significant population.
     * @param appLabels (Optional) An application label is any arbitrary string that you want to be associated with this event, and will create a
     *               second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *               For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *               and one for users who have purchased an upgrade.
     * @param networkLabels (Optional) networkLabels are the same as labels but will be only seen by the network instead of the apiKey.
     */
    public static void logEvent(String name, String[] appLabels, String[] networkLabels) {
        if(!QCMeasurement.INSTANCE.hasNetworkCode()){
            QCLog.e(TAG, "Network labels will be ignored without starting with a network code.  Call QCNetworkMeasurement.activityStart on Activity onStart to set a network code");
        }
        QCMeasurement.INSTANCE.logEvent(name, appLabels, networkLabels);
    }

    /**
     * Logs an network-defined event can be arbitrarily defined.
     *
     * @param name   A string that identifies the event being logged. Hierarchical information can be indicated by using a left-to-right notation with a period as a separator.
     *               For example, logging one event named "button.left" and another named "button.right" will create three reportable items in Quantcast App Measurement:
     *               "button.left", "button.right", and "button".
     *               There is no limit on the cardinality that this hierarchical scheme can create,
     *               though low-frequency events may not have an audience report on due to the lack of a statistically significant population.
     * @param networkLabels (Optional) An application label is any arbitrary string that you want to be associated with this event, and will create a
     *               second dimension in Quantcast Measurement reporting. Nominally, this is a "user class" indicator.
     *               For example, you might use one of two labels in your app: one for user who ave not purchased an app upgrade,
     *               and one for users who have purchased an upgrade.
     */

    public static void logNetworkEvent(String name, String[] networkLabels){
        if(!QCMeasurement.INSTANCE.hasNetworkCode()){
            QCLog.e(TAG, "Network labels will be ignored without starting with a network code.  Call QCNetworkMeasurement.activityStart on Activity onStart to set a network code");
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put(QCEvent.QC_EVENT_KEY, QC_EVENT_NETEVENT);
        params.put(QC_NETEVENT_KEY, name);
        QCMeasurement.INSTANCE.logOptionalEvent(params, null, networkLabels);

    }
}
