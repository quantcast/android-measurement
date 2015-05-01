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
import android.net.Uri;
import android.telephony.TelephonyManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

class QCPolicy {

    public static final String QC_NOTIF_POLICY_UPDATE = "QC_PU";
    static long POLICY_CACHE_LENGTH = 1000 * 60 * 30;  //30 mins

    private static final QCLog.Tag TAG = new QCLog.Tag(QCPolicy.class);

    private static final String USE_NO_SALT = "MSG";

    private Set<String> m_blacklist;
    private String m_salt;
    private long m_blackoutUntil;
    private Long m_sessionTimeout;

    private boolean m_policyIsLoaded;

    private final String m_policyURL;

    private static final String BLACKLIST_KEY = "blacklist";
    private static final String SALT_KEY = "salt";
    private static final String BLACKOUT_KEY = "blackout";
    private static final String SESSION_TIMEOUT_KEY = "sessionTimeOutSeconds";
    private static final String POLICY_REQUEST_BASE_WITHOUT_SCHEME = "m.quantcount.com/policy.json";
    private static final String POLICY_REQUEST_API_KEY_PARAMETER = "a";
    private static final String POLICY_REQUEST_API_VERSION_PARAMETER = "v";
    private static final String POLICY_REQUEST_DEVICE_TYPE_PARAMETER = "t";
    private static final String POLICY_REQUEST_PACKAGE_PARAMETER = "p";
    private static final String POLICY_REQUEST_NETWORK_CODE_PARAMETER = "n";
    private static final String POLICY_REQUEST_KID_DIRECTED_PARAMETER = "k";
    private static final String POLICY_REQUEST_DEVICE_COUNTRY = "c";
    private static final String POLICY_REQUEST_DEVICE_TYPE = "ANDROID";

    public static QCPolicy getQuantcastPolicy(Context context, String apiKey, String networkCode, String packageName, boolean kidDirected) {
        Uri.Builder builder = Uri.parse(QCUtility.addScheme(POLICY_REQUEST_BASE_WITHOUT_SCHEME)).buildUpon();
        builder.appendQueryParameter(POLICY_REQUEST_API_VERSION_PARAMETER, QCUtility.API_VERSION);
        builder.appendQueryParameter(POLICY_REQUEST_DEVICE_TYPE_PARAMETER, POLICY_REQUEST_DEVICE_TYPE);

        String mcc = null;
        try {
            TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tel != null) {
                mcc = tel.getNetworkCountryIso();
                if (mcc == null) {
                    mcc = tel.getSimCountryIso();
                }
            }
        } catch (SecurityException ignored) {
        }

        if (mcc == null) {
            mcc = Locale.getDefault().getCountry();
        }
        if (mcc != null) {
            builder.appendQueryParameter(POLICY_REQUEST_DEVICE_COUNTRY, mcc);
        }

        if (apiKey != null) {
            builder.appendQueryParameter(POLICY_REQUEST_API_KEY_PARAMETER, apiKey);
        } else {
            builder.appendQueryParameter(POLICY_REQUEST_NETWORK_CODE_PARAMETER, networkCode);
            builder.appendQueryParameter(POLICY_REQUEST_PACKAGE_PARAMETER, packageName);
        }

        if (kidDirected) {
            builder.appendQueryParameter(POLICY_REQUEST_KID_DIRECTED_PARAMETER, "YES");
        }

        Uri builtURL = builder.build();
        if (builtURL != null) {
            return new QCPolicy(context, builtURL.toString());
        } else {
            QCLog.e(TAG, "Policy URL was not built correctly for some reason.  Should not happen");
            return null;
        }
    }


    private QCPolicy(Context context, String policyURL) {
        m_policyURL = policyURL;
        m_policyIsLoaded = false;
        boolean optedOut = QCOptOutUtility.isOptedOut(context);
        if (optedOut) {
            m_policyIsLoaded = false;
        } else {
            if (QCReachability.isConnected(context)) {
                getPolicy(context);
            } else {
                QCLog.i(TAG, "No connection.  Policy could not be downloaded. Using cache");
                m_policyIsLoaded = checkPolicy(context, true);
            }
        }
    }

    public void updatePolicy(Context context) {
        if (QCReachability.isConnected(context)) {
            getPolicy(context);
        } else {
            QCLog.i(TAG, "No connection.  Policy could not be updated. Using cache.");
            m_policyIsLoaded = checkPolicy(context, true);
        }
    }

    public boolean policyIsLoaded() {
        return m_policyIsLoaded;
    }


    private void getPolicy(Context context) {

        //if we are blacked out we cant go get the policy yet
        if (isBlackedOut()) return;

        boolean loadedPolicy = checkPolicy(context, false);
        QCLog.i(TAG, "checking load policy: " + loadedPolicy);
        if (!loadedPolicy) {
            String jsonString = null;
            DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
            defaultHttpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
                    System.getProperty("http.agent"));
            InputStream inputStream = null;
            try {
                HttpGet method = new HttpGet(m_policyURL);
                HttpResponse response = defaultHttpClient.execute(method);
                inputStream = response.getEntity().getContent();
                jsonString = readStreamToString(inputStream);
            } catch (Exception e) {
                QCLog.e(TAG, "Could not download policy", e);
                QCMeasurement.INSTANCE.logSDKError("policy-download-failure", e.getMessage(), null);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            if (jsonString != null) {
                savePolicy(context, jsonString);
                loadedPolicy = parsePolicy(jsonString);
            }
        }
        m_policyIsLoaded = loadedPolicy;
    }


    private boolean parsePolicy(String policyJsonString) {

        boolean successful = true;
        m_blacklist = null;
        m_salt = null;
        m_blackoutUntil = 0;
        m_sessionTimeout = null;

        if (!"".equals(policyJsonString)) {
            try {
                JSONObject policyJSON = new JSONObject(policyJsonString);

                if (policyJSON.has(BLACKLIST_KEY)) {
                    try {
                        JSONArray blacklistJSON = policyJSON.getJSONArray(BLACKLIST_KEY);
                        if (blacklistJSON.length() > 0) {
                            if (m_blacklist == null) {
                                m_blacklist = new HashSet<String>(blacklistJSON.length());
                            }

                            for (int i = 0; i < blacklistJSON.length(); i++) {
                                m_blacklist.add(blacklistJSON.getString(i));
                            }
                        }
                    } catch (JSONException e) {
                        QCLog.w(TAG, "Failed to parse blacklist from JSON.", e);
                    }
                }

                if (policyJSON.has(SALT_KEY)) {
                    try {
                        m_salt = policyJSON.getString(SALT_KEY);
                        if (USE_NO_SALT.equals(m_salt)) {
                            m_salt = null;
                        }
                    } catch (JSONException e) {
                        QCLog.w(TAG, "Failed to parse salt from JSON.", e);
                    }
                }

                if (policyJSON.has(BLACKOUT_KEY)) {
                    try {
                        m_blackoutUntil = policyJSON.getLong(BLACKOUT_KEY);
                    } catch (JSONException e) {
                        QCLog.w(TAG, "Failed to parse blackout from JSON.", e);
                    }
                }

                if (policyJSON.has(SESSION_TIMEOUT_KEY)) {
                    try {
                        m_sessionTimeout = policyJSON.getLong(SESSION_TIMEOUT_KEY);
                        if (m_sessionTimeout <= 0) {
                            m_sessionTimeout = null;
                        }
                    } catch (JSONException e) {
                        QCLog.w(TAG, "Failed to parse session timeout from JSON.", e);
                    }
                }
            } catch (JSONException e) {
                QCLog.w(TAG, "Failed to parse JSON from string: " + policyJsonString);
                successful = false;
            }
        }
        return successful;
    }

    static final String POLICY_DIRECTORY = "com.quantcast";
    static final String POLICY_FILENAME = "qc-policy.json";

    private void savePolicy(Context context, String policy) {
        File base = context.getDir(POLICY_DIRECTORY, Context.MODE_PRIVATE);
        File policyFile = new File(base, POLICY_FILENAME);
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(policyFile);
            stream.write(policy.getBytes());
        } catch (Exception e) {
            QCLog.e(TAG, "Could not write policy", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private boolean checkPolicy(Context context, boolean force) {
        boolean retval = false;
        File base = context.getDir(POLICY_DIRECTORY, Context.MODE_PRIVATE);
        File policyFile = new File(base, POLICY_FILENAME);
        if (policyFile.exists()) {
            //check how old it is
            long date = policyFile.lastModified();
            FileInputStream input = null;
            try {
                input = new FileInputStream(policyFile);
                String policy = readStreamToString(input);
                retval = parsePolicy(policy);
                //check if it should be updated
                retval = retval && (force || ((System.currentTimeMillis() - date) < POLICY_CACHE_LENGTH));
            } catch (Exception e) {
                QCLog.e(TAG, "Could not read from policy cache", e);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return retval;

    }


    boolean isBlackedOut() {
        return policyIsLoaded() && System.currentTimeMillis() <= m_blackoutUntil;

    }

    boolean isBlacklisted(String key) {
        if (key == null) return true;

        boolean retval = false;
        if (m_blacklist != null) {
            retval = m_blacklist.contains(key);
        }
        return retval;
    }

    String getSalt() {
        return m_salt;
    }

    boolean hasSessionTimeout() {
        return m_sessionTimeout != null;
    }

    Long getSessionTimeout() {
        return m_sessionTimeout;
    }

    private String readStreamToString(InputStream input) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return stringBuilder.toString();
    }

}
