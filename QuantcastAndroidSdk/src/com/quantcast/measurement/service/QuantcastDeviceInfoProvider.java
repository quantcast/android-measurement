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

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.quantcast.deviceaccess.DeviceInfoProvider;

class QuantcastDeviceInfoProvider implements DeviceInfoProvider {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastDeviceInfoProvider.class);

    private Context context;

    private boolean deviceIdSet = false;
    private String deviceId = null;

    public QuantcastDeviceInfoProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String getDeviceId() {
        if (!deviceIdSet) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
                deviceId = getAndroidId();
            } else {
                deviceId = getTelephonyId();
            }

            deviceIdSet = true;
            QuantcastLog.i(TAG, "Generated deviceId: " + deviceId);
        }

        return deviceId;
    }

    private String getTelephonyId() {
        String telephonyId = null;

        PackageManager packageManager = context.getPackageManager();
        if (packageManager.checkPermission(permission.READ_PHONE_STATE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                telephonyId = telephonyManager.getDeviceId();
            }
        }

        return telephonyId;
    }

    private String getAndroidId() {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null) {
            androidId = "";
        }

        return androidId;
    }

}
