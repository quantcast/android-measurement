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

import java.util.Iterator;
import java.util.List;

import android.content.Context;

import com.quantcast.deviceaccess.DeviceInfoProvider;
import com.quantcast.json.JsonString;
import com.quantcast.measurement.event.Event;
import com.quantcast.policy.Policy;
import com.quantcast.policy.PolicyEnforcer;
import com.quantcast.policy.PolicyGetter;
import com.quantcast.policy.PolicyJsonCache;
import com.quantcast.policy.PolicyJsonLookup;

class QuantcastPolicyEnforcer implements PolicyEnforcer, PolicyGetter {
    
    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastPolicyEnforcer.class);
    
    private final PolicyJsonCache oldPolicyCache;
    private final PolicyJsonLookup newPolicyLookup;
    private final DeviceInfoProvider deviceInfoProvider;

    private Policy obtainedPolicy;
    
    private Policy currentPolicy;

    public QuantcastPolicyEnforcer(Context context, String apiVersion, String apiKey) {
        this(new PolicyJsonCacheFile(context), new HttpPolicyJsonLookup(apiKey, apiVersion), new QuantcastDeviceInfoProvider(context));
    }

    public QuantcastPolicyEnforcer(PolicyJsonCache oldPolicyCache, PolicyJsonLookup newPolicyLookup, DeviceInfoProvider deviceInfoProvider) {
        this.oldPolicyCache = oldPolicyCache;
        this.newPolicyLookup = newPolicyLookup;
        this.deviceInfoProvider = deviceInfoProvider;
    }


    /**
     * Attempt to enforce an up-to-date policy on a the provided events. This is done in place.
     * 
     * @param events
     * @return whether or not an up-to-date policy was enforced
     */
    @Override
    public boolean enforePolicy(List<? extends Event> events) {
        boolean policyEnforced = false;

        Policy savedPolicy = PolicyUtils.parsePolicy(oldPolicyCache.getPolicyJsonString());
        if (savedPolicy != null) {
            setPolicyForExternalConsumption(savedPolicy);
        }
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
            if (event.containsKey(BaseEvent.DEVICE_ID_PARAMETER)) {
                if (!deviceIdSet) {
                    String deviceId = deviceInfoProvider.getDeviceId();
                    if (deviceId != null) {
                        encodedDeviceId = policy.encodeDeviceId(deviceId);
                    }
                    deviceIdSet = true;
                }
                if (encodedDeviceId != null) {
                    event.put(BaseEvent.DEVICE_ID_PARAMETER, new JsonString(encodedDeviceId));
                } else {
                    event.remove(BaseEvent.DEVICE_ID_PARAMETER);
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
            String newPolicyJsonString = newPolicyLookup.getPolicyJsonString();
            obtainedPolicy = PolicyUtils.parsePolicy(newPolicyJsonString);
            if (obtainedPolicy != null) {
                setPolicyForExternalConsumption(obtainedPolicy);
                oldPolicyCache.savePolicyJsonString(newPolicyJsonString);
            }
        }
    }
    
    private synchronized void setPolicyForExternalConsumption(Policy policy) {
        this.currentPolicy = policy;
    }
    
    @Override
    public synchronized Policy getPolicy() {
        return currentPolicy;
    }
}
