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
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.WindowManager;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

class QCEvent {

    private static final QCLog.Tag TAG = new QCLog.Tag(QCEvent.class);

    static final String QC_PARAMETER_LABEL = "labels";
    static final String QC_EVENT_KEY = "event";
    static final String QC_REASON_KEY = "nsr";
    static final String QC_APIKEY_KEY = "apikey";
    static final String QC_MEDIA_KEY = "media";
    static final String QC_MEDIA_VALUE = "app";
    static final String QC_CONNECTION_KEY = "ct";
    static final String QC_TIMESTAMP_KEY = "et";
    static final String QC_SESSIONID_KEY = "sid";
    static final String QC_DEVICEID_KEY = "did";
    static final String QC_APPID_KEY = "aid";
    static final String QC_APPNAME_KEY = "aname";
    static final String QC_PACKAGEID_KEY = "pkid";
    static final String QC_VERSION_KEY = "aver";
    static final String QC_BUILDNUM_KEY = "iver";
    static final String QC_USERHASH_KEY = "uh";
    static final String QC_SCREENRES_KEY = "sr";
    static final String QC_DST_KEY = "dst";
    static final String QC_TIMEZONE_KEY = "tz";
    static final String QC_MCC_KEY = "mcc";
    static final String QC_COUNTRYCODE_KEY = "icc";
    static final String QC_MNC_KEY = "mnc";
    static final String QC_CARRIERNAME_KEY = "mnn";
    static final String QC_DEVICETYPE_KEY = "dtype";
    static final String QC_DEVICEMODEL_KEY = "dmod";
    static final String QC_DEVICEOS_KEY = "dos";
    static final String QC_DEVICEOS_VALUE = "android";
    static final String QC_OSVERSION_KEY = "dosv";
    static final String QC_MANUFACTURER_KEY = "dm";
    static final String QC_LOCALECOUNTRY_KEY = "lc";
    static final String QC_LOCALELANG_KEY = "ll";
    static final String QC_INSTALLDATE_KEY = "inst";
    static final String QC_APPEVENT_KEY = "appevent";
    static final String QC_LATENCYVALUE_KEY = "latency-value";
    static final String QC_LATENCYID_KEY = "uplid";
    static final String QC_COUNTRY_KEY = "c";
    static final String QC_STATE_KEY = "st";
    static final String QC_CITY_KEY = "l";
    static final String QC_REFERRER_KEY = "r";
    static final String QC_ERRORTYPE_KEY = "error-type";
    static final String QC_ERRORDESC_KEY = "error-desc";
    static final String QC_ERRORPARAM_KEY = "error-param";


    static final String QC_EVENT_LOAD = "load";
    static final String QC_EVENT_FINISHED = "finished";
    static final String QC_EVENT_PAUSE = "pause";
    static final String QC_EVENT_RESUME = "resume";
    static final String QC_EVENT_APPEVENT = "appevent";
    static final String QC_EVENT_LATENCY = "latency";
    static final String QC_EVENT_LOCATION = "location";
    static final String QC_EVENT_SDKERROR = "sdkerror";

    protected static final String QC_BEGIN_LAUNCH_REASON = "launch";
    protected static final String QC_BEGIN_RESUME_REASON = "resume";
    protected static final String QC_BEGIN_USERHASH_REASON = "userhash";

    static QCEvent beginSessionEvent(Context context, String userhash,
                                            String reason, String session,
                                            String apiKey, String deviceId,
                                            String[] labels) {
        QCEvent e = new QCEvent(session);

        e.addParameter(QC_EVENT_KEY, QC_EVENT_LOAD);
        e.addParameter(QC_REASON_KEY, reason);
        e.addParameter(QC_APIKEY_KEY, apiKey);
        e.addParameter(QC_MEDIA_KEY, QC_MEDIA_VALUE);
        e.addParameter(QC_CONNECTION_KEY, QCReachability.networkType(context));

        String referrer = QCReferrerReceiver.referrer;
        if (referrer != null) {
            e.addParameter(QC_REFERRER_KEY, referrer);
        }

        if (deviceId != null) {
            e.addParameter(QC_DEVICEID_KEY, deviceId);
        }

        String appInstallId = QCUtility.getAppInstallId(context);
        if (appInstallId != null) {
            e.addParameter(QC_APPID_KEY, appInstallId);
        }

        String appName = QCUtility.getAppName(context);
        if (appName != null) {
            e.addParameter(QC_APPNAME_KEY, appName);
        }

        String packageName = context.getPackageName();
        if (packageName != null) {
            e.addParameter(QC_PACKAGEID_KEY, packageName);
        }

        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            if (packageManager != null) {
                packageInfo = packageManager.getPackageInfo(packageName, 0);
            }
        } catch (PackageManager.NameNotFoundException nnfe) {
            QCLog.e(TAG, "Unable to get application info for this app.", nnfe);
        }
        if (packageInfo != null) {
            e.addParameter(QC_VERSION_KEY, packageInfo.versionName);
            e.addParameter(QC_BUILDNUM_KEY, Integer.toString(packageInfo.versionCode));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                try {
                    Field field = PackageInfo.class.getField("firstInstallTime");
                    long timestamp = field.getLong(packageInfo);
                    e.addParameter(QC_INSTALLDATE_KEY, String.valueOf(timestamp));
                } catch (Exception e1) {
                    //error getting install time so get next best
                    e.addParameter(QC_INSTALLDATE_KEY, String.valueOf(context.getFilesDir().lastModified()));
                }
            } else {
                e.addParameter(QC_INSTALLDATE_KEY, String.valueOf(context.getFilesDir().lastModified()));
            }
        }

        e.addLabels(labels);

        if (userhash != null) {
            e.addParameter(QC_USERHASH_KEY, userhash);
        }

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            Display d = windowManager.getDefaultDisplay();
            String dims;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2){
                Point point = new Point();
                d.getSize(point);
                dims = String.format("%dx%dx32", point.x, point.y);
            }else{
                //noinspection deprecation
                dims = String.format("%dx%dx32", d.getWidth(), d.getHeight());
            }
            e.addParameter(QC_SCREENRES_KEY, dims);
        }


        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();

        e.addParameter(QC_DST_KEY, Boolean.toString(tz.inDaylightTime(now)));

        long tzo = tz.getOffset(now.getTime()) / 1000 / 60;

        e.addParameter(QC_TIMEZONE_KEY, Long.toString(tzo));

        TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tel != null) {
            String carrierInfo = tel.getNetworkOperator();
            //no network?  then try the sim
            if (carrierInfo == null || carrierInfo.length() <= 0) {
                carrierInfo = tel.getSimOperator();
            }
            if (carrierInfo != null && carrierInfo.length() > 0) {
                if (carrierInfo.length() <= 3) {
                    e.addParameter(QC_MCC_KEY, carrierInfo);
                } else {
                    e.addParameter(QC_MCC_KEY, carrierInfo.substring(0, 3));
                    e.addParameter(QC_MNC_KEY, carrierInfo.substring(3));
                }
            }

            String countryCode = tel.getNetworkCountryIso();
            if (countryCode == null || countryCode.length() == 0) {
                countryCode = tel.getSimCountryIso();
            }
            if (countryCode != null && countryCode.length() > 0) {
                e.addParameter(QC_COUNTRYCODE_KEY, countryCode);
            }

            String carrierName = tel.getNetworkOperatorName();
            if (carrierName == null || carrierName.length() == 0) {
                carrierName = tel.getSimOperatorName();
            }
            if (carrierName != null && carrierName.length() > 0) {
                e.addParameter(QC_CARRIERNAME_KEY, carrierName);
            }
        }
        int screenLayout = context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        boolean isTablet = screenLayout == 4 || screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE;
        e.addParameter(QC_DEVICETYPE_KEY, isTablet ? "Tablet" : "Handset");

        e.addParameter(QC_DEVICEOS_KEY, QC_DEVICEOS_VALUE);
        e.addParameter(QC_DEVICEMODEL_KEY, Build.MODEL);
        e.addParameter(QC_OSVERSION_KEY, Build.VERSION.RELEASE);
        e.addParameter(QC_MANUFACTURER_KEY, Build.MANUFACTURER);

        Locale locale = Locale.getDefault();
        e.addParameter(QC_LOCALECOUNTRY_KEY, locale.getISO3Country());
        e.addParameter(QC_LOCALELANG_KEY, locale.getISO3Language());

        return e;

    }

    static QCEvent closeSessionEvent(Context c, String sessionId, String[] labels) {
        QCEvent e = new QCEvent(sessionId);
        e.setForceUpload(true);
        e.addParameter(QC_EVENT_KEY, QC_EVENT_FINISHED);
        String appInstallId = QCUtility.getAppInstallId(c);
        if (appInstallId != null) {
            e.addParameter(QC_APPID_KEY, appInstallId);
        }
        e.addLabels(labels);
        return e;
    }

    static QCEvent pauseSession(Context c, String sessionId, String[] labels) {
        QCEvent e = new QCEvent(sessionId);
        e.setForceUpload(true);
        e.addParameter(QC_EVENT_KEY, QC_EVENT_PAUSE);
        String appInstallId = QCUtility.getAppInstallId(c);
        if (appInstallId != null) {
            e.addParameter(QC_APPID_KEY, appInstallId);
        }
        e.addLabels(labels);
        return e;
    }

    static QCEvent resumeSession(Context c, String sessionId, String[] labels) {
        QCEvent e = new QCEvent(sessionId);
        e.addParameter(QC_EVENT_KEY, QC_EVENT_RESUME);
        String appInstallId = QCUtility.getAppInstallId(c);
        if (appInstallId != null) {
            e.addParameter(QC_APPID_KEY, appInstallId);
        }
        e.addLabels(labels);
        return e;
    }

    static QCEvent logEvent(Context c, String sessionId, String eventName, String[] labels) {
        QCEvent e = new QCEvent(sessionId);
        e.addParameter(QC_EVENT_KEY, QC_EVENT_APPEVENT);
        e.addParameter(QC_APPEVENT_KEY, eventName);
        String appInstallId = QCUtility.getAppInstallId(c);
        if (appInstallId != null) {
            e.addParameter(QC_APPID_KEY, appInstallId);
        }
        e.addLabels(labels);
        return e;
    }

    //log latency need to check the policy now since it has an internal map
    static QCEvent logLatency(Context c, String sessionId, String latencyId, String latencyValue) {
        QCEvent e = new QCEvent(sessionId);
        e.addParameter(QC_EVENT_KEY, QC_EVENT_LATENCY);
        String appInstallId = QCUtility.getAppInstallId(c);
        if (appInstallId != null) {
            e.addParameter(QC_APPID_KEY, appInstallId);
        }
        e.addParameter(QC_LATENCYID_KEY, latencyId);
        e.addParameter(QC_LATENCYVALUE_KEY, latencyValue);

        return e;
    }

    static QCEvent logLocation(Context c, String sessionId, String country, String state, String city) {
        QCEvent e = new QCEvent(sessionId);
        e.addParameter(QC_EVENT_KEY, QC_EVENT_LOCATION);
        e.addParameter(QC_COUNTRY_KEY, country);
        e.addParameter(QC_STATE_KEY, state);
        e.addParameter(QC_CITY_KEY, city);
        String appInstallId = QCUtility.getAppInstallId(c);
        if (appInstallId != null) {
            e.addParameter(QC_APPID_KEY, appInstallId);
        }
        return e;
    }

    static QCEvent logSDKError(String sessionId, String errorType, String errorDesc, String errorParam) {
        QCEvent e = new QCEvent(sessionId);
        e.setForceUpload(true);
        e.addParameter(QC_EVENT_KEY, QC_EVENT_SDKERROR);
        e.addParameter(QC_ERRORTYPE_KEY, errorType);
        if (errorDesc != null) {
            e.addParameter(QC_ERRORDESC_KEY, errorDesc);
        }

        if (errorParam != null) {
            e.addParameter(QC_ERRORPARAM_KEY, errorParam);
        }
        return e;
    }

    static QCEvent dataBaseEventWithPolicyCheck(Long eventID, Map<String, String> params, QCPolicy policy) {
        //if we are blacked out or the event key is blacklisted, then we can't send any data to the servers
        if (policy == null || !policy.policyIsLoaded() || policy.isBlackedOut() || policy.isBlacklisted(QC_EVENT_KEY)){
            return null;
        }


        QCEvent e = new QCEvent(eventID);

        //salt if needed
        if (policy.getSalt() != null) {
            if (params.containsKey(QC_DEVICEID_KEY)) {
                String salted = params.get(QC_DEVICEID_KEY) + policy.getSalt();
                String hashed = QCUtility.applyHash(salted);
                params.put(QC_DEVICEID_KEY, hashed);
            }
            if (params.containsKey(QC_APPID_KEY)) {
                String salted = params.get(QC_APPID_KEY) + policy.getSalt();
                String hashed = QCUtility.applyHash(salted);
                params.put(QC_APPID_KEY, hashed);
            }
        }

        //loop through check against the policy
        for (String key : params.keySet()) {
            if (!policy.isBlacklisted(key)) {
                e.addParameter(key, params.get(key));
            }
        }
        return e;
    }

    private final Map<String, Object> m_parameters;
    private final String m_eventId;
    private boolean m_forceUpload;

    QCEvent(Long eventId) {
        m_parameters = new HashMap<String, Object>();
        m_eventId = Long.toString(eventId);
        m_forceUpload = false;
    }

    QCEvent(String sessionId) {
        m_parameters = new HashMap<String, Object>();
        addParameter(QC_TIMESTAMP_KEY, Long.toString(System.currentTimeMillis() / 1000));
        addParameter(QC_SESSIONID_KEY, sessionId);
        m_eventId = null;
    }

    void addParameter(String key, Object value) {
        if (value != null) {
            m_parameters.put(key, value);
        }
    }

    void addLabels(String[] labels) {
        if (labels == null || labels.length == 0) return;

        String labelString = null;
        for(String label : labels){
            try {
                String encodedLabel = URLEncoder.encode(label, "UTF-8");
                //encodes space with "+" so change it to %20
                encodedLabel = encodedLabel.replaceAll("\\+", "%20");
                if(labelString == null){
                    labelString = encodedLabel;
                }else{
                    labelString += "," + encodedLabel;
                }
            } catch (UnsupportedEncodingException ignored) { }
        }
        m_parameters.put(QC_PARAMETER_LABEL, labelString);

    }

    boolean shouldForceUpload() {
        return m_forceUpload;
    }

    void setForceUpload(boolean forceUpload) {
        m_forceUpload = forceUpload;
    }

    String getEventId() {
        return m_eventId;
    }

    Map<String, Object> getParameters() {
        return m_parameters;
    }

}
