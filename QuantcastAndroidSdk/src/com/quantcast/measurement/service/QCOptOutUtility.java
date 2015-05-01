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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


class QCOptOutUtility {

    public static final String QC_NOTIF_OPT_OUT_CHANGED = "QC_OUC";

    private static final String QCMEASUREMENT_OPTOUT_STRING = "QC-OPT-OUT";


    static void saveOptOutStatus(Context appContext, boolean optedOut) {
        createOptOut(appContext, optedOut);
        //askEveryone(appContext, optedOut, true);
        QCNotificationCenter.INSTANCE.postNotification(QC_NOTIF_OPT_OUT_CHANGED, optedOut);
    }

    static boolean isOptedOut(Context appContext) {
        return isOptedOut(appContext, true);
    }

    private static boolean isOptedOut(Context appContext, boolean shouldAsk) {
        boolean optedOut = false;
        FileInputStream in = null;
        try {
            in = appContext.openFileInput(QCMEASUREMENT_OPTOUT_STRING);
            optedOut = in.read() != 0;
        } catch (FileNotFoundException e) {
            optedOut = false;
            //if (shouldAsk)
            //    askEveryone(appContext, false, false);
        } catch (IOException ignored) {
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ignored) {
            }
        }

        return optedOut;

    }

    static void createOptOut(Context context, boolean optedOut) {
        FileOutputStream stream = null;
        try {
            stream = context.openFileOutput(QCMEASUREMENT_OPTOUT_STRING, Context.MODE_PRIVATE);
            stream.write(optedOut ? 1 : 0);
        } catch (Exception ignored) {
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ignored) {
            }
        }
    }


    /*We no longer want to leave our sandbox to see if a user is opted out elsewhere.
       1) it doesnt work very well anymore on newer versions
       2) it is pretty intrusive and we don't want to cause any security holes.
    static boolean isQuantified(Context appContext) {
        File quantified = appContext.getFileStreamPath(QCMEASUREMENT_OPTOUT_STRING);
        return quantified != null && quantified.exists();
    }

    static void askEveryone(final Context context, boolean optedOut, boolean shouldUpdate) {
        Boolean status = false;
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            for (PackageInfo info : pm.getInstalledPackages(0)) {
                if (!info.packageName.equals(context.getPackageName())) {
                    try {
                        Context foreignContext = context.createPackageContext(info.packageName, 0);
                        if (shouldUpdate) {
                            if (isQuantified(foreignContext)) {
                                createOptOut(foreignContext, optedOut);
                            }
                        } else {
                            status = isOptedOut(foreignContext, false);
                            if (status) {
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (!shouldUpdate) {
            createOptOut(context, status);
        }
    }
    */
}
