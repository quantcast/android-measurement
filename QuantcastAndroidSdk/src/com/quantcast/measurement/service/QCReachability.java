/**
 * © Copyright 2012-2014 Quantcast Corp.
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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

class QCReachability {

    static boolean isConnected(Context context) {
        if(context == null) return true;
        boolean retval = false;
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conn != null) {
            NetworkInfo ni = conn.getActiveNetworkInfo();
            retval = ni != null && ni.isConnected();
        }
        return retval;
    }

    static String networkType(Context context) {
        String retval = "unknown";
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conn != null) {
            NetworkInfo ni = conn.getActiveNetworkInfo();
            if (ni != null && ni.isConnected()) {
                if (ni.getType() == ConnectivityManager.TYPE_MOBILE) {
                    retval = ni.getSubtypeName();
                    if (retval == null) {
                        retval = "wwan";
                    }
                } else if (ni.getType() == ConnectivityManager.TYPE_WIFI) {
                    retval = "wifi";
                }
            } else {
                retval = "disconnected";
            }
        }
        return retval;
    }

}
