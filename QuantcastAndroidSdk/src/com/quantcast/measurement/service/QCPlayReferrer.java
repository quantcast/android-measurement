package com.quantcast.measurement.service;

import android.content.Context;
import android.os.RemoteException;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

/**
 * © Copyright 2012-2014 Quantcast Corp.
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 * compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License. Unauthorized use of this file constitutes
 * copyright infringement and violation of law.
 */
class QCPlayReferrer {

    private static InstallReferrerClient referrerClient;
    private static String referrerUrl;
    private static long referrerClickTime;
    private static long appInstallTime;
    private static boolean instantExperienceLaunched;

    private static final QCLog.Tag TAG = new QCLog.Tag(QCPlayReferrer.class);

    protected static void connect(Context context){
        referrerClient = InstallReferrerClient.newBuilder(context).build();

        QCLog.i(TAG, "Connecting Play Install Referrer...");
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        QCLog.i(TAG, "Play Install Referrer connected");
                        getReferrer();
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        QCLog.i(TAG, "API not available on the current Play Store app.");
                        // API not available on the current Play Store app.
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        // Connection couldn't be established.
                        QCLog.i(TAG, "Play Install Referrer connection couldn't be established ");
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    private static void getReferrer(){
        try {
            ReferrerDetails response = referrerClient.getInstallReferrer();
            referrerUrl = response.getInstallReferrer();
            referrerClickTime = response.getReferrerClickTimestampSeconds();
            appInstallTime = response.getInstallBeginTimestampSeconds();
            instantExperienceLaunched = response.getGooglePlayInstantParam();

            referrerClient.endConnection();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected static String getReferrerURL(){
        QCLog.i(TAG, "Referrer url: " + referrerUrl);
        return referrerUrl;
    }

    protected static String getReferrerClickTime(){
        QCLog.i(TAG, "Referrer click time: " + referrerClickTime);
        return Long.toString(referrerClickTime);
    }

    protected static String getAppInstallTime(){
        QCLog.i(TAG, "Referrer app install time: " + appInstallTime);
        return Long.toString(appInstallTime);
    }

    protected static String getInstantExperienceLaunched(){
        QCLog.i(TAG, "Referrer Instant Experience Launched: " + instantExperienceLaunched);
        return Boolean.toString(instantExperienceLaunched);
    }
}
