package com.quantcast.measurement.service;

import java.util.Map;
import java.util.Set;

import com.quantcast.policy.Policy;

class QuantcastPolicy implements Policy {

    private static final String USE_NO_SALT = "MSG";

    private Set<String> blacklist;
    private String salt;
    private long blackoutUntil;

    /**
     * @param blacklist this is expected to not be null
     * @param salt if this in null as salt of "null" will be users (use an empty String for no salt)
     */
    public QuantcastPolicy(Set<String> blacklist, String salt, long blackoutUntil) {
        this.blacklist = blacklist;
        if (USE_NO_SALT.equals(salt)) {
            salt = "";
        }
        this.salt = salt;
        this.blackoutUntil = blackoutUntil;
    }

    @Override
    public String encodeDeviceId(String deviceId) {
        if (deviceId != null) {
            return applyHash(salt + deviceId);
        } else {
            return null;
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

    public static String applyHash(String string) {
        return QuantcastServiceUtility.applyHash(string);
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
    public boolean equals(Object o) {
        if (o instanceof QuantcastPolicy) {
            QuantcastPolicy that = (QuantcastPolicy) o;
            return this.salt.equals(that.salt)
                    && this.blackoutUntil == that.blackoutUntil
                    && this.blacklist.equals(that.blacklist);
        }

        return false;
    }

    @Override
    public String toString() {
        return super.toString() +":\nblacklist: " + getBlacklist() + "\nsalt: \"" + getSalt() + "\"\nblackout: " + getBlackout();
    }
}
