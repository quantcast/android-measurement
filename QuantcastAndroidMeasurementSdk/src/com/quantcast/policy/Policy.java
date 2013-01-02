package com.quantcast.policy;

import java.util.Map;
import java.util.Set;

public interface Policy {

    public String encodeDeviceId(String deviceId);

    public boolean isBlackedOut();

    /**
     * Applies the policies blacklist to the even parameters in place
     * 
     * @param parameters
     */
    public void applyBlacklist(Map<String, ? extends Object> parameters);
    
    public Set<String> getBlacklist();
    
    public String getSalt();
    
    public long getBlackout();

}