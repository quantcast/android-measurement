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

import android.content.Context;
import android.provider.Settings;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

enum QCMeasurement implements QCNotificationListener {
    INSTANCE;

    private static final QCLog.Tag TAG = new QCLog.Tag(QCMeasurement.class);
    static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    static final int DEFAULT_UPLOAD_EVENT_COUNT = 100;

    private boolean m_optedOut;

    private QCPolicy m_policy;
    private QCLocationManager m_locationManager;
    private QCDataManager m_manager;
    private Context m_context;

    private String m_apiKey;
    private String m_userId;
    private String m_sessionId;
    private String m_deviceId;

    private long m_lastPause;
    private int m_numActiveContext;
    private boolean m_locationEnabled;

    private boolean m_usesSecureConnection = false;


    private QCMeasurement() {
        QCNotificationCenter.INSTANCE.addListener(QCPolicy.QC_NOTIF_POLICY_UPDATE, this);
        QCNotificationCenter.INSTANCE.addListener(QCOptOutUtility.QC_NOTIF_OPT_OUT_CHANGED, this);
        m_numActiveContext = 0;

    }

    final String startUp(Context context, String apiKey, String userIdOrNull, String[] labelsOrNull) {
        //if this isn't the main activity then just count it and move on
        if (m_numActiveContext <= 0) {

            if (m_context == null && context == null) {
                throw new IllegalArgumentException("Context passed to Quantcast API cannot be null.");
            }

            //always get newest application context
            if (context.getApplicationContext() != null) {
                m_context = context.getApplicationContext();
            } else {
                m_context = context;
            }
            m_optedOut = QCOptOutUtility.isOptedOut(m_context);
            if (m_optedOut) setOptOutCookie(m_optedOut);

            //check if it is the very first time through
            if (!isMeasurementActive()) {
                QCLog.i(TAG, "First start of Quantcast");
                validateApiKey(apiKey);
                m_apiKey = apiKey;

                m_deviceId = getAndroidId(m_context);

                if (userIdOrNull != null) {
                    setUserIdentifier(userIdOrNull);
                }

                m_policy = QCPolicy.getQuantcastPolicy(m_context, apiKey);
                m_manager = new QCDataManager(m_context);
                logBeginSessionEvent(QCEvent.QC_BEGIN_LAUNCH_REASON, labelsOrNull);

                setUploadEventCount(DEFAULT_UPLOAD_EVENT_COUNT);
            } else {
                QCLog.i(TAG, "Resuming Quantcast");
                //otherwise just resume
                m_policy.updatePolicy(m_context);
                m_manager.postEvent(QCEvent.resumeSession(m_context, m_sessionId, labelsOrNull), m_policy);
                if (m_lastPause + getSessionTimeoutInMs() < System.currentTimeMillis()) {
                    QCLog.i(TAG, "Past session timeout.  Starting new session.");
                    logBeginSessionEvent(QCEvent.QC_BEGIN_RESUME_REASON, labelsOrNull);
                }
            }
            updateLocation();
        }
        m_numActiveContext++;
        return m_userId;
    }

    public final void logEvent(String name, String[] labels) {
        if (m_optedOut) return;
        if(isMeasurementActive()){
            m_manager.postEvent(QCEvent.logEvent(m_context, m_sessionId, name, labels), m_policy);
        }else{
            QCLog.e(TAG, "Log event called without first calling startActivity");
        }
    }

    public final void stop(String[] labels) {
        if (m_optedOut) return;

        if(isMeasurementActive()){
            m_numActiveContext = Math.max(0, m_numActiveContext - 1);
            if (m_numActiveContext == 0) {
                QCLog.i(TAG, "Last Activity stopped, pausing");
                m_lastPause = System.currentTimeMillis();
                m_manager.postEvent(QCEvent.pauseSession(m_context, m_sessionId, labels), m_policy);
                if (m_locationManager != null){
                    m_locationManager.stop();
                }
            }
        }else{
            QCLog.e(TAG, "Pause event called without first calling startActivity");
        }
    }

    public final boolean isMeasurementActive() {
        return m_sessionId != null;
    }

    public final String recordUserIdentifier(String userId, String[] labels) {
        if (m_optedOut) return null;

        //if not active just save it and send it on start
        if (!isMeasurementActive()) {
            setUserIdentifier(userId);
        } else {
            String ogUserId = m_userId;
            setUserIdentifier(userId);
            if ((m_userId == null && ogUserId != null) || (m_userId != null && !m_userId.equals(ogUserId))) {
                logBeginSessionEvent(QCEvent.QC_BEGIN_USERHASH_REASON, labels);
            }
        }
        return m_userId;
    }

    //apps don't really give us a chance to do this so its not used right now
    public final void end(String[] labels) {
        if (m_optedOut) return;
        if(isMeasurementActive()){
            QCLog.i(TAG, "Calling end.");
            m_manager.postEvent(QCEvent.closeSessionEvent(m_context, m_sessionId, labels), m_policy);
            m_sessionId = null;
            m_numActiveContext = 0;
        }else{
            QCLog.e(TAG, "End event called without first calling startActivity");
        }
    }

    public final void setUploadEventCount(int uploadEventCount) {
        if(isMeasurementActive()){
            m_manager.setUploadCount(uploadEventCount);
        }
    }

    public String getApiKey() {
        return m_apiKey;
    }

    final boolean usesSecureConnection() {
        return m_usesSecureConnection;
    }

    final void setUsesSecureConnection(boolean usesSecureConnection) {
        m_usesSecureConnection = usesSecureConnection;
    }

    private void setUserIdentifier(String userId) {
        if (userId == null) {
            m_userId = null;
        } else {
            m_userId = QCUtility.applyHash(userId);
        }
    }

    final void logBeginSessionEvent(String reason, String[] labels) {
        if (m_optedOut) return;

        m_sessionId = QCUtility.generateUniqueId();
        m_manager.postEvent(QCEvent.beginSessionEvent(m_context, m_userId, reason, m_sessionId, m_apiKey, m_deviceId, labels), m_policy);
    }


    final long getSessionTimeoutInMs() {
        long sessionTimeoutInMs = DEFAULT_SESSION_TIMEOUT;

        if (m_policy.policyIsLoaded() && m_policy.hasSessionTimeout()) {
            sessionTimeoutInMs = m_policy.getSessionTimeout();
        }

        return sessionTimeoutInMs;
    }

    final void logLocation(String country, String state, String city) {
        if (m_optedOut) return;
        m_manager.postEvent(QCEvent.logLocation(m_context, m_sessionId, country, state, city), m_policy);
    }

    final void logLatency(String uploadId, long time) {
        if (m_optedOut || m_policy == null) return;
        m_manager.postEvent(QCEvent.logLatency(m_context, m_sessionId, uploadId, Long.toString(time)), m_policy);
    }

    final void logSDKError(String error, String desc, String param) {
        if (m_optedOut) return;
        m_manager.postEvent(QCEvent.logSDKError(m_sessionId, error, desc, param), m_policy);
    }

    final void setLocationEnabled(boolean enabled){
        if(enabled == m_locationEnabled) return;

        m_locationEnabled = enabled;
        if(isMeasurementActive()){
            if(m_locationEnabled){
                updateLocation();
            }else{
                stopLocationGathering();
            }
        }
    }

    final void updateLocation() {
        if (m_optedOut) return;
        if(m_locationEnabled){
            if (m_locationManager == null) {
                m_locationManager = new QCLocationManager(m_context);
            }
            m_locationManager.start();
        }
    }

    final void stopLocationGathering() {
        if (m_locationManager != null) {
            m_locationManager.stop();
            m_locationManager = null;
        }
    }

    final Context getAppContext() {
        return m_context;
    }

    final void validateApiKey(String apiKey) {
        if (apiKey == null) {
            throw new IllegalArgumentException("No Quantcast API Key was passed to the SDK. Please use the API Key provided to you by Quantcast.");
        }

        if (!apiKey.matches("[a-zA-Z0-9]{16}-[a-zA-Z0-9]{16}")) {
            throw new IllegalArgumentException("The Quantcast API Key passed to the SDK is malformed. Please use the API Key provided to you by Quantcast.");
        }
    }

    final String getAndroidId(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        //checks for the fake androidID returned on some Froyo devices
        if (androidId == null || androidId.equals("9774d56d682e549c")) {
            androidId = null;
        }

        return androidId;
    }

    @Override
    public void notificationCallback(String notificationName, Object o) {
        if (notificationName.equals(QCPolicy.QC_NOTIF_POLICY_UPDATE)) {


        } else if (notificationName.equals(QCOptOutUtility.QC_NOTIF_OPT_OUT_CHANGED)) {
            m_optedOut = (Boolean) o;
            //opted back in we need to set everything up
            if (!m_optedOut) {
                m_policy.updatePolicy(m_context);
                logBeginSessionEvent(QCEvent.QC_BEGIN_LAUNCH_REASON, new String[]{"_OPT-IN"});
            } else {
                stopLocationGathering();
                QCUtility.dumpAppInstallID(m_context);
                m_context.deleteDatabase(QCDatabaseDAO.NAME);
            }
            setOptOutCookie(m_optedOut);
        }
    }

    void setOptOutCookie(boolean add) {
        CookieSyncManager.createInstance(m_context);
        CookieManager cookieManager = CookieManager.getInstance();
        Calendar cal = GregorianCalendar.getInstance();
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd-MMM-yyyy H:m:s z");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (add) {
            cal.add(Calendar.YEAR, 10);
        } else {
            //just overwrite the old cookie with one that expires really soon
            cal.add(Calendar.SECOND, 1);
        }
        cookieManager.setCookie("quantserve.com", "qoo=OPT_OUT;domain=.quantserve.com;path=/;expires=" + fmt.format(cal.getTime()));
        CookieSyncManager csm = CookieSyncManager.getInstance();
        if (csm != null) {
            csm.sync();
        }
    }


    final void clearSession() {

        if (m_context != null) {
            m_context.deleteDatabase(QCDatabaseDAO.NAME);
        }
        m_numActiveContext = 0;
        m_sessionId = null;
        m_manager = null;
        m_policy = null;
        m_locationManager = null;
        m_context = null;
    }

    final QCDataManager getManager() {
        return m_manager;
    }

}