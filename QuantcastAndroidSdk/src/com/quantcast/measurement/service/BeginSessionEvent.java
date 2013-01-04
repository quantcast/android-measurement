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

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.quantcast.json.JsonString;

@SuppressWarnings("serial")
class BeginSessionEvent extends Event {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(BeginSessionEvent.class);

    public static enum Reason {
        LAUNCH("launch"),
        RESUME("resume"),
        USERHHASH("userhash"),
        ;

        public final String parameterValue;

        private Reason(String parameterValue) {
            this.parameterValue = parameterValue;
        }

    }

    private static final int MCC_LENGTH = 3;

    private static final String API_KEY_PARAMETER = "apikey";
    private static final String PARAMETER_ANAME = "aname";
    private static final String PARAMETER_AVER = "aver";
    private static final String PARAMETER_UH = "uh";
    private static final String PARAMETER_CT = "ct";
    private static final String PARAMETER_DM = "dm";
    private static final String PARAMETER_DMOD = "dmod";
    private static final String PARAMETER_DOS = "dos";
    private static final String PARAMETER_DOSV = "dosv";
    private static final String PARAMETER_DST = "dst";
    private static final String PARAMETER_DTYPE = "dtype";
    private static final String PARAMETER_LC = "lc";
    private static final String PARAMETER_LL = "ll";
    private static final String PARAMETER_MCC = "mcc";
    private static final String PARAMETER_MEDIA = "media";
    private static final String PARAMETER_MNC = "mnc";
    private static final String PARAMETER_MNN = "mnn";
    private static final String PARAMETER_SR = "sr";
    private static final String PARAMETER_TZO = "tzo";
    private static final String PACKAGE_NAME_PARAMETER = "pkid";
    private static final String VERSION_CODE_PARAMETER = "iver";
    private static final String REASON_PARAMETER = "nsr";
    private static final String APPLICATION_ID_PARAMETER = "aid";
    private static final String ISO_COUNTRY_CODE_PARAMETER = "icc";

    BeginSessionEvent(Context context, Session session, Reason reason, String apiKey, String userId, String labels) {
        super(EventType.BEGIN_SESSION, session, labels);

        put(REASON_PARAMETER, new JsonString(reason.parameterValue));
        put(API_KEY_PARAMETER, new JsonString(apiKey));
        put(PARAMETER_MEDIA, new JsonString(DEFAULT_MEDIA_PARAMETER));
        if (userId != null) {
            put(PARAMETER_UH, new JsonString(QuantcastServiceUtility.applyHash(userId)));
        }
        // Put a placeholder here to be replaced by a hashed and salted DID at upload time (this sucks a little).
        put(Event.DEVICE_ID_PARAMETER, new JsonString(""));
    }

    public void addAsyncParameters(Context context) {
        String packageName = context.getPackageName();
        put(PACKAGE_NAME_PARAMETER, new JsonString(packageName));

        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, 0);
        }
        catch (NameNotFoundException e) {
            QuantcastLog.e(TAG, "Unable to get application info for this app.", e);
        }
        if (packageInfo != null) {
            put(PARAMETER_ANAME, new JsonString(packageManager.getApplicationLabel(packageInfo.applicationInfo)));
            put(PARAMETER_AVER, new JsonString(packageInfo.versionName));
            put(VERSION_CODE_PARAMETER, new JsonString(packageInfo.versionCode));
        }

        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conn != null) {
            NetworkInfo ni = conn.getActiveNetworkInfo();
            String ct;
            if (ni == null) {
                ct = "unknown";
            } else if (ni.isConnected()) {
                if (ni.getType() == ConnectivityManager.TYPE_WIFI)
                    ct = "WIFI";
                else
                    ct = ni.getSubtypeName();
            } else {
                ct = "disconnected";
            }
            put(PARAMETER_CT, new JsonString(ct));
        }

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);

            String sr = Integer.toString(metrics.widthPixels) + "x" + Integer.toString(metrics.heightPixels) + "x32";
            put(PARAMETER_SR, new JsonString(sr));
        }

        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();

        put(PARAMETER_DST, new JsonString(tz.inDaylightTime(now) ? Boolean.TRUE : Boolean.FALSE));

        // TZO is in minutes, as returned by the javascript function specified here:
        // https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Date/getTimezoneOffset
        // It has slightly odd behavior too.  From the docs:
        //   The time-zone offset is the difference, in minutes, between UTC and local time.
        //   Note that this means that the offset is positive if the local timezone is behind UTC and negative if it is ahead.
        //   For example, if your time zone is UTC+10 (Australian Eastern Standard Time), -600 will be returned.
        //   Daylight savings time prevents this value from being a constant even for a given locale
        long tzo = tz.getOffset(now.getTime()) / 1000 / 60;

        put(PARAMETER_TZO, new JsonString(tzo));

        TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tel != null) {
            String carrierInfo = tel.getNetworkOperator();
            if (carrierInfo != null && carrierInfo.length() > 0) {
                if (carrierInfo.length() <= MCC_LENGTH) {
                    put(PARAMETER_MCC, new JsonString(carrierInfo));
                } else {
                    put(PARAMETER_MCC, new JsonString(carrierInfo.substring(0, MCC_LENGTH)));
                    put(PARAMETER_MNC, new JsonString(carrierInfo.substring(MCC_LENGTH)));
                }
            }
            put(ISO_COUNTRY_CODE_PARAMETER, new JsonString(tel.getNetworkCountryIso()));
            put(PARAMETER_MNN, new JsonString(tel.getNetworkOperatorName()));

            put(PARAMETER_DTYPE, new JsonString(Integer.toString(tel.getPhoneType())));
        }
        
        put(PARAMETER_DOS, new JsonString("android"));
        put(PARAMETER_DMOD, new JsonString(Build.MODEL));
        put(PARAMETER_DOSV, new JsonString(Build.ID));
        put(PARAMETER_DM, new JsonString(Build.MANUFACTURER));

        Locale locale = Locale.getDefault();
        put(PARAMETER_LC, new JsonString(locale.getISO3Country()));
        put(PARAMETER_LL, new JsonString(locale.getISO3Language()));

        put(APPLICATION_ID_PARAMETER, new JsonString(QuantcastServiceUtility.getApplicationId(context)));
    }

}
