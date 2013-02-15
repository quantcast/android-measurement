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

import java.util.Map;
import java.util.Set;

import com.quantcast.policy.Policy;

class QuantcastPolicy implements Policy {
    
    private static final String USE_NO_SALT = "MSG";
    private static final String EMPTY_SALT = "";

    private Set<String> blacklist;
    private String salt;
    private long blackoutUntil;
    private Long sessionTimeout;

    /**
     * @param blacklist this is expected to not be null
     * @param salt if this in null as salt of "null" will be users (use an empty String for no salt)
     */
    public QuantcastPolicy(Set<String> blacklist, String salt, long blackoutUntil, Long sessionTimeout) {
        this.blacklist = blacklist;
        if (USE_NO_SALT.equals(salt) || EMPTY_SALT.equals(salt)) {
            salt = null;
        }
        this.salt = salt;
        this.blackoutUntil = blackoutUntil;
        if (sessionTimeout == null || sessionTimeout < 0) {
            this.sessionTimeout = null;
        } else {
            this.sessionTimeout = sessionTimeout;
        }
    }

    @Override
    public String encodeDeviceId(String deviceId) {
        if (salt != null && deviceId != null) {
            return QuantcastServiceUtility.applySha1Hash(salt + deviceId);
        } else {
            return deviceId;
        }
    }

    @Override
    public boolean isBlackedOut() {
        // This will only hold true until the end of days
        return System.currentTimeMillis() <= blackoutUntil;
    }

    @Override
    public void applyBlacklist(Map<String, ? extends Object> parameters) {
        for (String key : blacklist) {
            parameters.remove(key);
        }
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }

    public String getSalt() {
        return salt;
    }

    public long getBlackout() {
        return blackoutUntil;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((blacklist == null) ? 0 : blacklist.hashCode());
        result = prime * result
                + (int) (blackoutUntil ^ (blackoutUntil >>> 32));
        result = prime * result + ((salt == null) ? 0 : salt.hashCode());
        result = prime * result
                + ((sessionTimeout == null) ? 0 : sessionTimeout.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QuantcastPolicy other = (QuantcastPolicy) obj;
        if (blacklist == null) {
            if (other.blacklist != null)
                return false;
        }
        else if (!blacklist.equals(other.blacklist))
            return false;
        if (blackoutUntil != other.blackoutUntil)
            return false;
        if (salt == null) {
            if (other.salt != null)
                return false;
        }
        else if (!salt.equals(other.salt))
            return false;
        if (sessionTimeout == null) {
            if (other.sessionTimeout != null)
                return false;
        }
        else if (!sessionTimeout.equals(other.sessionTimeout))
            return false;
        return true;
    }

    @Override
    public boolean hasSessionTimeout() {
        return sessionTimeout != null;
    }

    @Override
    public Long getSessionTimeout() {
        return sessionTimeout;
    }

    @Override
    public String toString() {
        return super.toString() +":\nblacklist: " + getBlacklist() + "\nsalt: \"" + getSalt() + "\"\nblackout: " + getBlackout() + "\nsession timeout: " + getSessionTimeout();
    }
    
}
