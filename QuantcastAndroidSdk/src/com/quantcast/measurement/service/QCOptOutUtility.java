/*
 * Copyright 2012 Quantcast Corp.
 *
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 * compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.quantcast.measurement.service;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


class QCOptOutUtility {

    public static final String QC_NOTIF_OPT_OUT_CHANGED = "QC_OUC";

    private static final String QCMEASUREMENT_OPTOUT_STRING = "QC-OPT-OUT";


    static void saveOptOutStatus(Context appContext, boolean optedOut) {
        createOptOut(appContext, optedOut);
        askEveryone(appContext, optedOut, true);
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
            if (shouldAsk)
                askEveryone(appContext, false, false);
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
            stream = context.openFileOutput(QCMEASUREMENT_OPTOUT_STRING, Context.MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE);
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

    static void askEveryone(final Context context, boolean optedOut, boolean shouldUpdate) {
        new AsyncTask<Boolean, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Boolean... booleans) {
                boolean optedOut = booleans[0];
                boolean shouldUpdate = booleans[1];
                Boolean status = false;
                //now im gonna see whats installed
                PackageManager pm = context.getPackageManager();
                if (pm != null) {
                    for (PackageInfo info : pm.getInstalledPackages(0)) {
                        if (!info.packageName.equals(context.getPackageName())) {
                            try {
                                Context foreignContext = context.createPackageContext(info.packageName, 0);
                                if (shouldUpdate) {
                                    createOptOut(foreignContext, optedOut);
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
                return status;
            }

            @Override
            protected void onPostExecute(Boolean status) {
                if (status) {
                    QCNotificationCenter.INSTANCE.postNotification(QC_NOTIF_OPT_OUT_CHANGED, status);
                }
            }

        }.execute(optedOut, shouldUpdate);
    }
}
