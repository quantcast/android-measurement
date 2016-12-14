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
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

enum QCMeasurement implements QCNotificationListener {
    INSTANCE;

    private static final QCLog.Tag TAG = new QCLog.Tag(QCMeasurement.class);
    static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    static final String QC_NOTIF_APP_START = "QC_START";
    static final String QC_NOTIF_APP_STOP = "QC_STOP";

    private String[] m_appLabels;
    private String[] m_netLabels;

    private boolean m_optedOut;
    private boolean adPrefChanged = false;

    private QCPolicy m_policy;
    private QCDataManager m_manager;
    private Context m_context;

    private String m_apiKey;
    private String m_networkCode;
    private String m_userId;
    private String m_sessionId;
    private String m_deviceId;

    private int m_numActiveContext;

    private int m_uploadCount;

    private boolean m_usesSecureConnection = false;

    private final QCEventHandler m_eventHandler;


    QCMeasurement() {
        m_eventHandler = new QCEventHandler();
        m_eventHandler.start();
        QCNotificationCenter.INSTANCE.addListener(QCPolicy.QC_NOTIF_POLICY_UPDATE, this);
        QCNotificationCenter.INSTANCE.addListener(QCOptOutUtility.QC_NOTIF_OPT_OUT_CHANGED, this);
        m_numActiveContext = 0;
        m_optedOut = false;
        m_uploadCount = QCDataManager.DEFAULT_UPLOAD_EVENT_COUNT;

    }

    final String startUp(Context context, String apiKey, String userIdOrNull, String[] labelsOrNull) {
        return startUp(context, apiKey, null, userIdOrNull, labelsOrNull, null, false);
    }

    final String startUp(final Context context, final String apiKey, final String networkCode, final String userIdOrNull, final String[] appLabelsOrNull,
                         final String[] networkLabels, final boolean isDirectedAtKids) {
        if (m_context == null && context == null) {
            QCLog.e(TAG, "Context passed to Quantcast API cannot be null.");
            return null;
        }

        //always get newest application context
        if (context != null) {
            if (context.getApplicationContext() != null) {
                m_context = context.getApplicationContext();
            } else {
                m_context = context;
            }
        }
        m_eventHandler.setContext(m_context);

        final String hashedId = QCUtility.applyHash(userIdOrNull);
        loadAdvertisingId(m_context);
        m_eventHandler.post(new Runnable() {
            @Override
            public void run() {
                //if this isn't the main activity then just count it and move on
                if (m_numActiveContext <= 0) {
                    m_optedOut = QCOptOutUtility.isOptedOut(m_context);
                    if (m_optedOut) {
                        setOptOutCookie(true);
                    }

                    boolean userIDChanged = false;
                    if (hashedId != null) {
                        userIDChanged = userIdentifierHasChanged(hashedId);
                        m_userId = hashedId;
                    }

                    //check if it is the very first time through
                    if (!isMeasurementActive()) {
                        QCLog.i(TAG, "First start of Quantcast " + m_optedOut);
                        //read api key from manifest if there
                        String key = apiKey;
                        if (key == null) {
                            key = QCUtility.getAPIKey(m_context);
                        }

                        boolean valid = validateApiKeyAndNetworkCode(key, networkCode);
                        if (valid) {
                            m_apiKey = apiKey;
                            m_networkCode = networkCode;

                            m_manager = new QCDataManager(m_context);
                            m_manager.setUploadCount(m_uploadCount);
                            m_policy = QCPolicy.getQuantcastPolicy(m_context, m_apiKey, m_networkCode, m_context.getPackageName(), isDirectedAtKids);

                            boolean newSession = checkSessionId(m_context);
                            if (adPrefChanged) {
                                logBeginSessionEvent(QCEvent.QC_BEGIN_ADPREF_REASON, appLabelsOrNull, networkLabels);
                            } else if (newSession) {
                                logBeginSessionEvent(QCEvent.QC_BEGIN_LAUNCH_REASON, appLabelsOrNull, networkLabels);
                            } else {
                                logResumeSessionEvent(appLabelsOrNull, networkLabels);
                            }
                            QCNotificationCenter.INSTANCE.postNotification(QC_NOTIF_APP_START, m_context);
                        }
                    } else {
                        QCLog.i(TAG, "Resuming Quantcast");
                        //otherwise just resume
                        m_policy.updatePolicy(m_context);
                        logResumeSessionEvent(appLabelsOrNull, networkLabels);
                        boolean newSession = checkSessionId(m_context);
                        if (adPrefChanged) {
                            QCLog.i(TAG, "Ad Preference changed.  Starting new session.");
                            logBeginSessionEvent(QCEvent.QC_BEGIN_ADPREF_REASON, appLabelsOrNull, networkLabels);
                        } else if (newSession) {
                            QCLog.i(TAG, "Past session timeout.  Starting new session.");
                            logBeginSessionEvent(QCEvent.QC_BEGIN_RESUME_REASON, appLabelsOrNull, networkLabels);
                        } else if (userIDChanged) {
                            logBeginSessionEvent(QCEvent.QC_BEGIN_USERHASH_REASON, appLabelsOrNull, networkLabels);
                        }
                    }
                } else if (hashedId != null && userIdentifierHasChanged(hashedId)) {
                    m_userId = hashedId;
                    logBeginSessionEvent(QCEvent.QC_BEGIN_USERHASH_REASON, appLabelsOrNull, networkLabels);
                }
                m_numActiveContext++;
            }
        });
        return hashedId;
    }

    final void logEvent(String name, String[] labels) {
        logEvent(name, labels, null);
    }

    final void logEvent(final String name, final String[] appLabels, final String[] networkLabel) {
        if (m_optedOut) return;
        m_eventHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isMeasurementActive()) {
                    String[] combinedAppLabels = QCUtility.combineLabels(m_appLabels, appLabels);
                    String[] combinedNetLabels = QCUtility.combineLabels(m_netLabels, networkLabel);
                    m_manager.postEvent(QCEvent.logEvent(m_context, m_sessionId, name, combinedAppLabels, combinedNetLabels), m_policy);
                } else {
                    QCLog.e(TAG, "Log event called without first calling startActivity");
                }
            }
        });

    }

    final void logOptionalEvent(final Map<String, String> params, final String[] appLabels, final String[] networkLabels) {
        if (m_optedOut) return;
        m_eventHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isMeasurementActive()) {
                    String[] combinedAppLabels = QCUtility.combineLabels(m_appLabels, appLabels);
                    String[] combinedNetLabels = QCUtility.combineLabels(m_netLabels, networkLabels);
                    m_manager.postEvent(QCEvent.logOptionalEvent(m_context, m_sessionId, params, combinedAppLabels, combinedNetLabels), m_policy);
                } else {
                    QCLog.e(TAG, "Log event called without first calling startActivity");
                }
            }
        });

    }

    final void stop(String[] labels) {
        stop(labels, null);
    }

    final void stop(final String[] appLabels, final String[] networkLabels) {
        QCLog.i(TAG, "Stoping check opt out " + m_optedOut);
        if (m_optedOut) return;

        m_eventHandler.post(new Runnable() {
            @Override
            public void run() {
                m_numActiveContext = Math.max(0, m_numActiveContext - 1);
                QCLog.i(TAG, "Activity stopped, count: " + m_numActiveContext);
                if (isMeasurementActive()) {
                    if (m_numActiveContext == 0) {
                        QCLog.i(TAG, "Last Activity stopped, pausing");
                        updateSessionTimestamp();
                        String[] combinedAppLabels = QCUtility.combineLabels(m_appLabels, appLabels);
                        String[] combinedNetLabels = QCUtility.combineLabels(m_netLabels, networkLabels);
                        m_manager.postEvent(QCEvent.pauseSession(m_context, m_sessionId, combinedAppLabels, combinedNetLabels), m_policy);
                        QCNotificationCenter.INSTANCE.postNotification(QC_NOTIF_APP_STOP, m_context);
                    }
                } else {
                    QCLog.e(TAG, "Pause event called without first calling startActivity");
                }
            }
        });
    }

    public final boolean isMeasurementActive() {
        return m_sessionId != null;
    }

    final boolean userIdentifierHasChanged(String userId){
        return  ((m_userId != null && userId == null) || (userId != null && !userId.equals(m_userId)));
    }

    final String recordUserIdentifier(String userId, String[] labels) {
        return recordUserIdentifier(userId, labels, null);
    }

    final String recordUserIdentifier(String userId, final String[] appLabels, final String[] networkLabels) {
        if (m_optedOut) return null;

        final String hashedId = QCUtility.applyHash(userId);
        m_eventHandler.post(new Runnable() {
            @Override
            public void run() {
                //if not active just save it and send it on start
                if (!isMeasurementActive()) {
                    m_userId = hashedId;
                } else if (userIdentifierHasChanged(hashedId)){
                    m_userId = hashedId;
                    logBeginSessionEvent(QCEvent.QC_BEGIN_USERHASH_REASON, appLabels, networkLabels);
                }
            }
        });

        return hashedId;
    }

    //apps don't really give us a chance to do this so its not used
    final void end(final String[] appLabels, final String[] networkLabels) {
        if (m_optedOut) return;

        m_eventHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isMeasurementActive()) {
                    QCLog.i(TAG, "Calling end.");
                    updateSessionTimestamp();
                    String[] combinedAppLabels = QCUtility.combineLabels(m_appLabels, appLabels);
                    String[] combinedNetLabels = QCUtility.combineLabels(m_netLabels, networkLabels);
                    m_manager.postEvent(QCEvent.closeSessionEvent(m_context, m_sessionId, combinedAppLabels, combinedNetLabels), m_policy);
                    m_sessionId = null;
                    m_numActiveContext = 0;
                } else {
                    QCLog.e(TAG, "End event called without first calling startActivity");
                }
            }
        });

    }

    public final void setUploadEventCount(int uploadEventCount) {
        m_uploadCount = uploadEventCount;
        if (isMeasurementActive()) {
            m_manager.setUploadCount(uploadEventCount);
        }
    }

    final void setAppLabels(String[] labels){
        m_appLabels = labels;
    }

    final void setNetworkLabels(String[] labels){
        m_netLabels = labels;
    }

    String getApiKey() {
        return m_apiKey;
    }

    String getNetworkCode() {
        return m_networkCode;
    }

    String getPackageId(){
        return m_context != null ? m_context.getPackageName() : null;
    }

    String getDeviceId() {
        if(m_policy != null && m_policy.policyIsLoaded() && !m_policy.isBlacklisted(QCEvent.QC_DEVICEID_KEY)) {
            return m_deviceId;
        }
        return null;
    }

    final boolean usesSecureConnection() {
        return m_usesSecureConnection;
    }

    final void setUsesSecureConnection(boolean usesSecureConnection) {
        m_usesSecureConnection = usesSecureConnection;
    }

    final boolean isConnected(){
        return QCReachability.isConnected(m_context);
    }

    final void logResumeSessionEvent(String[] appLabels, String[] networkLabels) {
        String[] combinedAppLabels = QCUtility.combineLabels(m_appLabels, appLabels);
        String[] combinedNetLabels = QCUtility.combineLabels(m_netLabels, networkLabels);
        m_manager.postEvent(QCEvent.resumeSession(m_context, m_sessionId, combinedAppLabels, combinedNetLabels), m_policy);
    }

    final void logBeginSessionEvent(String reason, String[] appLabels, String[] networkLabels) {
        if (m_optedOut) return;

        m_sessionId = createSessionId();
        String[] combinedAppLabels = QCUtility.combineLabels(m_appLabels, appLabels);
        String[] combinedNetLabels = QCUtility.combineLabels(m_netLabels, networkLabels);
        m_manager.postEvent(QCEvent.beginSessionEvent(m_context, m_userId, reason, m_sessionId, m_apiKey, m_networkCode, m_deviceId, combinedAppLabels, combinedNetLabels), m_policy);
    }

    private static final String QC_SESSION_FILE = "QC-SessionId";

    final String createSessionId() {
        String sessionId = QCUtility.generateUniqueId();
        saveSessionID(sessionId);
        return sessionId;
    }

    final boolean checkSessionId(Context context) {
        boolean newSession = false;
        File session = context.getFileStreamPath(QC_SESSION_FILE);
        if (session.exists()) {
            long modified = session.lastModified();
            //check if we are over the timeout, if so then create a new session
            if ((System.currentTimeMillis() - modified) > getSessionTimeoutInMs()) {
                newSession = true;
            }
            //still time left and we dont have an id in memory?  read it
            else if (m_sessionId == null) {
                FileInputStream fis = null;
                try {
                    byte buffer[] = new byte[256];
                    fis = context.openFileInput(QC_SESSION_FILE);
                    int length = fis.read(buffer);
                    m_sessionId = new String(buffer, 0, length);
                } catch (Exception e) {
                    QCLog.e(TAG, "Error reading session file ", e);
                    logSDKError("session-read-failure", e.toString(), null);
                    //last resort create a new one
                    newSession = true;
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        } else {
            newSession = true;
        }
        return newSession;
    }

    private void saveSessionID(String sessionId) {
        FileOutputStream fos = null;
        try {
            fos = m_context.openFileOutput(QC_SESSION_FILE, Context.MODE_PRIVATE);
            fos.write(sessionId.getBytes());
        } catch (IOException ignored) {
            //not much we can do
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void updateSessionTimestamp() {
        File session = m_context.getFileStreamPath(QC_SESSION_FILE);
        if (session != null) {
            //noinspection ResultOfMethodCallIgnored
            session.setLastModified(System.currentTimeMillis());
        }
    }


    final long getSessionTimeoutInMs() {
        long sessionTimeoutInMs = DEFAULT_SESSION_TIMEOUT;

        if (m_policy != null && m_policy.policyIsLoaded() && m_policy.hasSessionTimeout()) {
            sessionTimeoutInMs = m_policy.getSessionTimeout() * 1000;
        }

        return sessionTimeoutInMs;
    }

    final void logLatency(final String uploadId, final long time) {
        if (m_optedOut || m_manager == null) return;
        m_eventHandler.post(new Runnable() {
            @Override
            public void run() {
                m_manager.postEvent(QCEvent.logLatency(m_context, m_sessionId, uploadId, Long.toString(time)), m_policy);
            }
        });
    }

    final void logSDKError(final String error, final String desc, final String param) {
        if (m_optedOut || m_manager == null) return;
        m_eventHandler.post(new Runnable() {
            @Override
            public void run() {
                m_manager.postEvent(QCEvent.logSDKError(m_sessionId, error, desc, param), m_policy);
            }
        });
    }

    final Context getAppContext() {
        return m_context;
    }

    final boolean hasNetworkCode() {
        return m_networkCode != null;
    }

    final boolean validateApiKeyAndNetworkCode(String apiKey, String networkCode) {
        boolean valid = true;
        if (apiKey == null && networkCode == null) {
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

    final boolean hasUserAdPrefChanged(Context context, boolean currentAdPref) {
        boolean lastAdPref = QCUtility.getUserAdPref(context);
        return currentAdPref ^ lastAdPref;
    }

    private boolean hasAdvertisingId(){
        boolean exists = true;
        try {
            Class.forName( "com.google.android.gms.ads.identifier.AdvertisingIdClient" );
        } catch( ClassNotFoundException e ) {
            exists = false;
            QCLog.i(TAG, "Could not find advertising ID.  Please link with Google Play Service 4.0.30 or greater.");
        }
        return exists;
    }

    final void loadAdvertisingId(final Context context) {
        if(hasAdvertisingId()){

         m_eventHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        com.google.android.gms.ads.identifier.AdvertisingIdClient.Info adInfo;
                        adInfo = com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(context);
                        //check if they turned off advertising tracking.  If so return null
                        boolean limitedAdTracking = adInfo.isLimitAdTrackingEnabled();
                        if (hasUserAdPrefChanged(context, limitedAdTracking)) {
                            QCUtility.dumpAppInstallID(context);
                            adPrefChanged = true;
                        }
                        QCUtility.saveUserAdPref(context, limitedAdTracking);
                        if (limitedAdTracking) {
                            m_deviceId = null;
                        } else {
                            m_deviceId = adInfo.getId();
                        }
                    } catch (Throwable t) {
                        //whatever else could happen
                        m_deviceId = null;
                        QCLog.e(TAG, "Exception thrown while getting Advertising Id.  Please make sure the Play Services 4.0+ library is linked properly and added to the application's manifest.", t);
                    }
                }
            });
        }else{
            m_deviceId = null;
            QCLog.e(TAG, "Quantcast strongly recommends using the Google Advertising Identifier to ensure user privacy.  Please link to the Play Services 4.0+ library and add it to the application's manifest. ");
        }
    }

    void setOptOut(final Context c, final boolean optOut) {
        m_eventHandler.post(new Runnable() {
            @Override
            public void run() {
                QCOptOutUtility.saveOptOutStatus(m_context != null ? m_context : c, optOut);

            }
        });
    }

    @Override
    public void notificationCallback(String notificationName, Object o) {
        if (notificationName.equals(QCOptOutUtility.QC_NOTIF_OPT_OUT_CHANGED)) {
            m_optedOut = (Boolean) o;
            //opted back in we need to set everything up
            if (!m_optedOut && (m_apiKey != null || m_networkCode != null)) {
                m_policy.updatePolicy(m_context);
                //only auto start if we have the api key
                if(m_apiKey != null) {
                    logBeginSessionEvent(QCEvent.QC_BEGIN_LAUNCH_REASON, new String[]{"_OPT-IN"}, null);
                }
            } else if (m_optedOut && isMeasurementActive()) {
                QCUtility.dumpAppInstallID(m_context);
                m_context.deleteDatabase(QCDatabaseDAO.NAME);
            }
            setOptOutCookie(m_optedOut);
        }
    }

    void setOptOutCookie(boolean add) {
        if(m_context == null) return;

        CookieSyncManager.createInstance(m_context);
        CookieManager cookieManager = CookieManager.getInstance();
        Calendar cal = GregorianCalendar.getInstance();
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd-MMM-yyyy H:m:s z", Locale.US);
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
            File session = m_context.getFileStreamPath(QC_SESSION_FILE);
            if (session.exists()) { session.delete(); }
        }
        m_numActiveContext = 0;
        m_sessionId = null;
        m_manager = null;
        m_policy = null;
        m_context = null;
        m_userId = null;
    }

    final QCDataManager getManager() {
        return m_manager;
    }

    final QCEventHandler getHandler() {
        return m_eventHandler;
    }
}