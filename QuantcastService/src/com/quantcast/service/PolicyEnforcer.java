package com.quantcast.service;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.quantcast.json.JsonString;
import com.quantcast.policy.Policy;
import com.quantcast.policy.PolicyDAO;
import com.quantcast.policy.PolicyProvider;

class PolicyEnforcer {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(PolicyEnforcer.class);

    private final Context context;
    private final PolicyDAO policyDAO;
    private final PolicyProvider policyProvider;
    
    private Policy obtainedPolicy;

    public PolicyEnforcer(Context context, String apiVersion, String pCode) {
        this.context = context;
        this.policyDAO = new QuantcastPolicyDAO(context, apiVersion);
        this.policyProvider = new QuantcastPolicyProvider(pCode, apiVersion);
    }

    public PolicyEnforcer(Context context, PolicyDAO policyDAO, PolicyProvider policyProvider) {
        this.context = context;
        this.policyDAO = policyDAO;
        this.policyProvider = policyProvider;
    }

    /**
     * Attempt to enforce an up-to-date policy on a the provided events. This is done in place.
     * 
     * @param events
     * @return whether or not an up-to-date policy was enforced
     */
    public boolean enforePolicy(List<Event> events) {
        boolean policyEnforced = false;
        
        Policy savedPolicy = policyDAO.getPolicy();
        if (savedPolicy != null && savedPolicy.isBlackedOut()) {
            events.clear();
            policyEnforced = true;
            QuantcastLog.i(TAG, "Blackedout based on saved policy:\n" + savedPolicy);
        } else {
            obtainPolicy();
            if (obtainedPolicy != null) {
                if (obtainedPolicy.isBlackedOut()) {
                    events.clear();
                    QuantcastLog.i(TAG, "Blackedout based on newly aquired policy:\n" + obtainedPolicy);
                } else {
                    applyPolicy(events, obtainedPolicy);
                }
                policyEnforced = true;
            }
        }

        return policyEnforced;
    }

    private void applyPolicy(List<Event> events, Policy policy) {
        QuantcastLog.i(TAG, "Applying policy:\n" + policy);
        
        String deviceId = null;
        
        Iterator<Event> iter = events.iterator();
        while (iter.hasNext()) {
            Event event = iter.next();
            if (event.containsKey(Event.DEVICE_ID_PARAMETER)) {
                if (deviceId == null) {
                    deviceId = policy.encodeDeviceId(generateDeviceId(context));
                }
                event.put(Event.DEVICE_ID_PARAMETER, new JsonString(deviceId));
            }
            policy.applyBlacklist(event);

            if (event.size() == 0) {
                iter.remove();
            }
        }
    }
    
    public boolean hasPolicy() {
        boolean hasPolicy = false;
        
        if (policyDAO.getPolicy() != null) {
            hasPolicy = true;
        } else {
            obtainPolicy();
            if (obtainedPolicy != null) {
                hasPolicy = true;
            }
        }
        
        return hasPolicy;
    }
    
    private void obtainPolicy() {
        if (obtainedPolicy == null) {
            obtainedPolicy = policyProvider.getPolicy();
            if (obtainedPolicy != null) {
                policyDAO.savePolicy(obtainedPolicy);
            }
        }
    }
    
    public static String generateDeviceId(Context context) {
        String deviceId = getTelephonyId(context) + getAndroidId(context);
        QuantcastLog.i(TAG, "Generated deviceId: " + deviceId);
        return deviceId;
    }

    private static String getTelephonyId(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String telephonyId = telephonyManager.getDeviceId();
        if (telephonyId == null) {
            telephonyId = "";
        }

        return telephonyId;
    }

    private static String getAndroidId(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null) {
            androidId = "";
        }

        return androidId;
    }
}
