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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

enum QCMeasurement implements QCNotificationListener {
    INSTANCE;

    private static final QCLog.Tag TAG = new QCLog.Tag(QCMeasurement.class);
    static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    static final int DEFAULT_UPLOAD_EVENT_COUNT = 100;

    public static final String QC_NOTIF_APP_START = "QC_START";
    public static final String QC_NOTIF_APP_STOP = "QC_STOP";

    private boolean m_optedOut;

    private QCPolicy m_policy;
    private QCDataManager m_manager;
    private Context m_context;

    private String m_apiKey;
    private String m_networkCode;
    private String m_userId;
    private String m_sessionId;
    private String m_deviceId;

    private int m_numActiveContext;

    private boolean m_usesSecureConnection = false;


    private QCMeasurement() {
        QCNotificationCenter.INSTANCE.addListener(QCPolicy.QC_NOTIF_POLICY_UPDATE, this);
        QCNotificationCenter.INSTANCE.addListener(QCOptOutUtility.QC_NOTIF_OPT_OUT_CHANGED, this);
        m_numActiveContext = 0;
        m_optedOut = true;

    }

    final String startUp(Context context, String apiKey, String userIdOrNull, String[] labelsOrNull) {
        return startUp(context, apiKey, null, userIdOrNull, labelsOrNull, null, false);
    }

    final String startUp(Context context, String apiKey, String networkCode, String userIdOrNull, String[] appLabelsOrNull,
                         String[] networkLabels, boolean isDirectedAtKids) {
        //if this isn't the main activity then just count it and move on
        if (m_numActiveContext <= 0) {

            if (m_context == null && context == null) {
                QCLog.e(TAG,"Context passed to Quantcast API cannot be null.");
                return null;
            }

            //always get newest application context
            if(context != null){
                if (context.getApplicationContext() != null) {
                    m_context = context.getApplicationContext();
                } else {
                    m_context = context;
                }
            }

            m_optedOut = QCOptOutUtility.isOptedOut(m_context);
            if (m_optedOut){
                setOptOutCookie(true);
            }

            //check if it is the very first time through
            if (!isMeasurementActive()) {
                QCLog.i(TAG, "First start of Quantcast");
                //read api key from manifest if there
                if(apiKey == null) {
                    apiKey = QCUtility.getAPIKey(m_context);
                }

                boolean valid = validateApiKeyAndNetworkCode(apiKey, networkCode);
                if(!valid)  return null;
                m_apiKey = apiKey;
                m_networkCode = networkCode;

                m_deviceId = getAndroidId(m_context);

                if (userIdOrNull != null) {
                    setUserIdentifier(userIdOrNull);
                }

                m_policy = QCPolicy.getQuantcastPolicy(m_context, m_apiKey, m_networkCode, m_context.getPackageName(), isDirectedAtKids);
                m_manager = new QCDataManager(m_context);
                setUploadEventCount(DEFAULT_UPLOAD_EVENT_COUNT);
                boolean newSession = checkSessionId(m_context);
                if(newSession){
                    logBeginSessionEvent(QCEvent.QC_BEGIN_LAUNCH_REASON, appLabelsOrNull, networkLabels);
                }else{
                    m_manager.postEvent(QCEvent.resumeSession(m_context, m_sessionId, appLabelsOrNull, networkLabels), m_policy);
                }
                QCNotificationCenter.INSTANCE.postNotification(QC_NOTIF_APP_START, m_context);
            } else {
                QCLog.i(TAG, "Resuming Quantcast");
                //otherwise just resume
                m_policy.updatePolicy(m_context);
                m_manager.postEvent(QCEvent.resumeSession(m_context, m_sessionId, appLabelsOrNull, networkLabels), m_policy);
                boolean newSession = checkSessionId(m_context);
                if (newSession) {
                    QCLog.i(TAG, "Past session timeout.  Starting new session.");
                    logBeginSessionEvent(QCEvent.QC_BEGIN_RESUME_REASON, appLabelsOrNull, networkLabels);
                }
            }
        }
        m_numActiveContext++;
        return m_userId;
    }

    final void logEvent(String name, String[] labels) {
        logEvent(name, labels, null);
    }

    final void logEvent(String name, String[] appLabels, String[] networkLabel) {
        if (m_optedOut) return;
        if(isMeasurementActive()){
            m_manager.postEvent(QCEvent.logEvent(m_context, m_sessionId, name, appLabels, networkLabel), m_policy);
        }else{
            QCLog.e(TAG, "Log event called without first calling startActivity");
        }
    }

    final void logOptionalEvent(Map<String, String> params, String[] appLabels, String[] networkLabels){
        if (m_optedOut) return;
        if(isMeasurementActive()){
            m_manager.postEvent(QCEvent.logOptionalEvent(m_context, m_sessionId, params, appLabels, networkLabels), m_policy);
        }else{
            QCLog.e(TAG, "Log event called without first calling startActivity");
        }
    }

    final void stop(String[] labels) {
        stop(labels, null);
    }

    final void stop(String[] appLabels, String[] networkLabels) {
        if (m_optedOut) return;

        if(isMeasurementActive()){
            m_numActiveContext = Math.max(0, m_numActiveContext - 1);
            if (m_numActiveContext == 0) {
                QCLog.i(TAG, "Last Activity stopped, pausing");
                updateSessionTimestamp();
                m_manager.postEvent(QCEvent.pauseSession(m_context, m_sessionId, appLabels, networkLabels), m_policy);
                QCNotificationCenter.INSTANCE.postNotification(QC_NOTIF_APP_STOP, m_context);
            }
        }else{
            QCLog.e(TAG, "Pause event called without first calling startActivity");
        }
    }

    public final boolean isMeasurementActive() {
        return m_sessionId != null;
    }

    final String recordUserIdentifier(String userId, String[] labels) {
        return recordUserIdentifier(userId, labels, null);
    }

    final String recordUserIdentifier(String userId, String[] appLabels, String[] networkLabels) {
        if (m_optedOut) return null;

        //if not active just save it and send it on start
        if (!isMeasurementActive()) {
            setUserIdentifier(userId);
        } else {
            String ogUserId = m_userId;
            setUserIdentifier(userId);
            if ((m_userId == null && ogUserId != null) || (m_userId != null && !m_userId.equals(ogUserId))) {
                logBeginSessionEvent(QCEvent.QC_BEGIN_USERHASH_REASON, appLabels, networkLabels);
            }
        }
        return m_userId;
    }

    //apps don't really give us a chance to do this so its not used
    final void end(String[] appLabels, String[] networkLabels) {
        if (m_optedOut) return;
        if(isMeasurementActive()){
            QCLog.i(TAG, "Calling end.");
            updateSessionTimestamp();
            m_manager.postEvent(QCEvent.closeSessionEvent(m_context, m_sessionId, appLabels, networkLabels), m_policy);
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

    final void logBeginSessionEvent(String reason, String[] appLabels, String[] networkLabels) {
        if (m_optedOut) return;

        m_sessionId = createSessionId();
        m_manager.postEvent(QCEvent.beginSessionEvent(m_context, m_userId, reason, m_sessionId, m_apiKey, m_networkCode, m_deviceId, appLabels, networkLabels), m_policy);
    }

    private static final String QC_SESSION_FILE = "QC-SessionId";

    final String createSessionId(){
        String sessionId = QCUtility.generateUniqueId();
        saveSessionID(sessionId);
        return sessionId;
    }

    final boolean checkSessionId(Context context){
        boolean newSession = false;
        File session =  context.getFileStreamPath(QC_SESSION_FILE);
        if(session.exists() ){
            long modified = session.lastModified();
            //check if we are over the timeout, if so then create a new session
            if ((System.currentTimeMillis() - modified) > getSessionTimeoutInMs()){
                newSession = true;
            }
            //still time left and we dont have an id in memory?  read it
            else if(m_sessionId == null){
                FileInputStream fis = null;
                try {
                    byte buffer[] = new byte[256];
                    fis = context.openFileInput(QC_SESSION_FILE);
                    int length = context.openFileInput(QC_SESSION_FILE).read(buffer);
                    m_sessionId = new String(buffer,0, length);
                } catch (IOException e) {
                    //last resort create a new one
                    newSession = true;
                }finally {
                    if(fis != null){
                        try {
                            fis.close();
                        } catch (IOException ignored) { }
                    }
                }
            }
        }else{
            newSession = true;
        }
        return newSession;
    }

    private void saveSessionID(String sessionId){
        FileOutputStream fos = null;
        try {
            fos = m_context.openFileOutput(QC_SESSION_FILE, Context.MODE_PRIVATE);
            fos.write(sessionId.getBytes());
        } catch(IOException ignored){
            //not much we can do
        }finally {
            if(fos != null){
                try {
                    fos.close();
                } catch (IOException ignored) { }
            }
        }
    }

    private void updateSessionTimestamp(){
        File session =  m_context.getFileStreamPath(QC_SESSION_FILE);
        if(session != null){
            //noinspection ResultOfMethodCallIgnored
            session.setLastModified(System.currentTimeMillis());
        }
    }


    final long getSessionTimeoutInMs() {
        long sessionTimeoutInMs = DEFAULT_SESSION_TIMEOUT;

        if (m_policy != null && m_policy.policyIsLoaded() && m_policy.hasSessionTimeout()) {
            sessionTimeoutInMs = m_policy.getSessionTimeout()*1000;
        }

        return sessionTimeoutInMs;
    }

    final void logLatency(String uploadId, long time) {
        if (m_optedOut || m_manager == null) return;
        m_manager.postEvent(QCEvent.logLatency(m_context, m_sessionId, uploadId, Long.toString(time)), m_policy);
    }

    final void logSDKError(String error, String desc, String param) {
        if (m_optedOut || m_manager == null) return;
        m_manager.postEvent(QCEvent.logSDKError(m_sessionId, error, desc, param), m_policy);
    }

    final Context getAppContext() {
        return m_context;
    }

    final boolean hasNetworkCode(){
        return m_networkCode != null;
    }

    final boolean validateApiKeyAndNetworkCode(String apiKey, String networkCode) {
        boolean valid = true;
        if(apiKey == null && networkCode == null){
            QCLog.e(TAG, "No Quantcast API Key was passed to the SDK. Please use the API Key provided to you by Quantcast.");
            valid = false;
        }

        if (apiKey != null && !apiKey.matches("[a-zA-Z0-9]{16}-[a-zA-Z0-9]{16}")) {
            QCLog.e(TAG, "The Quantcast API Key passed to the SDK is malformed. Please use the API Key provided to you by Quantcast.");
            valid = false;
        }

        if (networkCode != null && !networkCode.matches("p-[-_a-zA-Z0-9]{13}")) {
            QCLog.e(TAG, "The Quantcast network p-code passed to the SDK is malformed. Please use the network p-code found on Quantcast.com.");
            valid = false;
        }

        return valid;
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
        if (notificationName.equals(QCOptOutUtility.QC_NOTIF_OPT_OUT_CHANGED)) {
            m_optedOut = (Boolean) o;
            //opted back in we need to set everything up
            if (!m_optedOut) {
                m_policy.updatePolicy(m_context);
                logBeginSessionEvent(QCEvent.QC_BEGIN_LAUNCH_REASON, new String[]{"_OPT-IN"}, null);
            } else {
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
        m_context = null;
    }

    final QCDataManager getManager() {
        return m_manager;
    }
}