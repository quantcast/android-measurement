package com.quantcast.measurement.service;

import java.util.Iterator;
import java.util.List;

import android.content.Context;

import com.quantcast.deviceaccess.DeviceInfoProvider;
import com.quantcast.json.JsonString;
import com.quantcast.policy.Policy;
import com.quantcast.policy.PolicyDAO;
import com.quantcast.policy.PolicyProvider;

class PolicyEnforcer {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(PolicyEnforcer.class);

    private final PolicyDAO policyDAO;
    private final PolicyProvider policyProvider;
    private final DeviceInfoProvider deviceInfoProvider;

    private Policy obtainedPolicy;

    public PolicyEnforcer(Context context, String apiVersion, String apiKey) {
        this(new QuantcastPolicyDAO(context, apiVersion), new QuantcastPolicyProvider(apiKey, apiVersion), new DeviceInfoProvider(context));
    }

    public PolicyEnforcer(PolicyDAO policyDAO, PolicyProvider policyProvider, DeviceInfoProvider deviceInfoProvider) {
        this.policyDAO = policyDAO;
        this.policyProvider = policyProvider;
        this.deviceInfoProvider = deviceInfoProvider;
    }

    /**
     * Attempt to enforce an up-to-date policy on a the provided events. This is done in place.
     * 
     * @param events
     * @return whether or not an up-to-date policy was enforced
     */
    public boolean enforePolicy(List<? extends Event> events) {
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

    private void applyPolicy(List<? extends Event> events, Policy policy) {
        QuantcastLog.i(TAG, "Applying policy:\n" + policy);

        boolean deviceIdSet = false;
        String encodedDeviceId = null;

        Iterator<? extends Event> iter = events.iterator();
        while (iter.hasNext()) {
            Event event = iter.next();
            if (event.containsKey(Event.DEVICE_ID_PARAMETER)) {
                if (!deviceIdSet) {
                    String deviceId = deviceInfoProvider.getDeviceId();
                    if (deviceId != null) {
                        encodedDeviceId = policy.encodeDeviceId(deviceId);
                    }
                    deviceIdSet = true;
                }
                if (encodedDeviceId != null) {
                    event.put(Event.DEVICE_ID_PARAMETER, new JsonString(encodedDeviceId));
                } else {
                    event.remove(Event.DEVICE_ID_PARAMETER);
                }
            }
            policy.applyBlacklist(event);

            if (event.size() == 0) {
                iter.remove();
            }
        }
    }

    private void obtainPolicy() {
        if (obtainedPolicy == null) {
            obtainedPolicy = policyProvider.getPolicy();
            if (obtainedPolicy != null) {
                policyDAO.savePolicy(obtainedPolicy);
            }
        }
    }
}
