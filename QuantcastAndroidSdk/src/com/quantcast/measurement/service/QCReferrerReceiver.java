package com.quantcast.measurement.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

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
public class QCReferrerReceiver extends BroadcastReceiver {

    protected static String referrer;

    private static final QCLog.Tag TAG = new QCLog.Tag(QCReferrerReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        if (extras != null) {
            referrer = extras.getString("referrer");
        }

        QCLog.i(TAG, "Referrer is: " + referrer);

        //get receiver info
        ActivityInfo ai;
        try {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                ai = pm.getReceiverInfo(new ComponentName(context, "com.quantcast.measurement.service.QCReferrerReceiver"), PackageManager.GET_META_DATA);
                if (ai != null) {
                    //extract meta-data
                    Bundle bundle = ai.metaData;
                    if (bundle != null) {
                        for (String k : bundle.keySet()) {
                            String v = bundle.getString(k);
                            try {
                                ((BroadcastReceiver) Class.forName(v).newInstance()).onReceive(context, intent); //send intent by dynamically creating instance of receiver
                                QCLog.i(TAG, "PASS REFERRER TO..." + v);
                            } catch (Exception e) {
                                QCLog.e(TAG, "Error when passing to referrer.  Class might not exist: " + v, e);
                            }

                        }
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            QCLog.e(TAG, "Could not find package Name for referrer", e);
        }
    }
}
