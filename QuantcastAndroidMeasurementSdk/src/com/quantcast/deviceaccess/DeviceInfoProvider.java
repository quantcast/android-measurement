package com.quantcast.deviceaccess;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.quantcast.measurement.service.QuantcastLog;

public class DeviceInfoProvider {
    
    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(DeviceInfoProvider.class);
    
    private Context context;
    
    private boolean deviceIdSet = false;
    private String deviceId = null;

    public DeviceInfoProvider(Context context) {
        this.context = context.getApplicationContext();
    }

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
            telephonyId = telephonyManager.getDeviceId();
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
