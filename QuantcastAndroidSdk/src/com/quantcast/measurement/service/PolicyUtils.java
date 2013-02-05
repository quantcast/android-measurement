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

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.quantcast.policy.Policy;

class PolicyUtils {
    
    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(PolicyUtils.class);

    static final String BLACKLIST_KEY = "blacklist";
    static final String SALT_KEY = "salt";
    static final String BLACKOUT_KEY = "blackout";
    static final String SESSION_TIMEOUT_KEY = "sessionTimeOutSeconds";

    public static Policy parsePolicy(String policyJsonString) {
        if (policyJsonString == null) {
            return null;
        }
        
        Set<String> blacklist = new HashSet<String>();
        String salt = "";
        long blackout = 0;
        Long sessionTimeout = null;

        if (!"".equals(policyJsonString)) {
            try {
                JSONObject policyJSON = new JSONObject(policyJsonString);
                
                if (policyJSON.has(BLACKLIST_KEY)) {
                    try {
                        JSONArray blacklistJSON = policyJSON.getJSONArray(BLACKLIST_KEY);

                        for (int i = 0; i < blacklistJSON.length(); i++) {
                            blacklist.add(blacklistJSON.getString(i));
                        }
                    } catch (JSONException e) {
                        QuantcastLog.w(TAG, "Failed to parse blacklist from JSON.", e);
                    }
                }

                if (policyJSON.has(SALT_KEY)) {
                    try {
                        salt = policyJSON.getString(SALT_KEY);
                    } catch (JSONException e) {
                        QuantcastLog.w(TAG, "Failed to parse salt from JSON.", e);
                    }
                }

                if (policyJSON.has(BLACKOUT_KEY)) {
                    try {
                        blackout = policyJSON.getLong(BLACKOUT_KEY);
                    } catch (JSONException e) {
                        QuantcastLog.w(TAG, "Failed to parse blackout from JSON.", e);
                    }
                }
                
                if (policyJSON.has(SESSION_TIMEOUT_KEY)) {
                    try {
                        sessionTimeout = policyJSON.getLong(SESSION_TIMEOUT_KEY);
                    } catch (JSONException e) {
                        QuantcastLog.w(TAG, "Failed to parse session timeout from JSON.", e);
                    }
                }
            } catch (JSONException e) {
                QuantcastLog.w(TAG, "Failed to parse JSON from string: " + policyJsonString);
            }
        }

        Policy policy = new QuantcastPolicy(blacklist, salt, blackout, sessionTimeout);
        QuantcastLog.i(TAG, "Generated policy from JSON:\n" + policy);
        return policy;
    }
}
