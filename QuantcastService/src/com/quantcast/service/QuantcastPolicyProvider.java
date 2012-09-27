package com.quantcast.service;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.quantcast.policy.JSONPolicyLookup;
import com.quantcast.policy.Policy;
import com.quantcast.policy.PolicyProvider;

class QuantcastPolicyProvider implements PolicyProvider {
    
    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastPolicyProvider.class);

    static final String BLACKLIST_KEY = "blacklist";
    static final String SALT_KEY = "salt";
    static final String BLACKOUT_KEY = "blackout";

    private JSONPolicyLookup lookup;

    public QuantcastPolicyProvider(String pCode, String apiVersion) {
        lookup = new QuantcastJSONPolicyLookup(pCode, apiVersion);
    }

    public QuantcastPolicyProvider(JSONPolicyLookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public Policy getPolicy() {
        String policyJSONString = lookup.getPolicyJSONString();
        if (policyJSONString == null) {
            return null;
        }
        
        Set<String> blacklist = new HashSet<String>();
        String salt = "";
        long blackout = 0;

        if (!"".equals(policyJSONString)) {
            try {
                JSONObject policyJSON = new JSONObject(policyJSONString);
                
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
            } catch (JSONException e) {
                QuantcastLog.w(TAG, "Failed to parse JSON from string: " + policyJSONString);
            }
        }

        Policy policy = new QuantcastPolicy(blacklist, salt, blackout);
        QuantcastLog.i(TAG, "Generated policy from JSON:\n" + policy);
        return policy;
    }
}
