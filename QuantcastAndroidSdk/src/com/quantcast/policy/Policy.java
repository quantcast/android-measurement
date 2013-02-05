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
    
    public boolean hasSessionTimeout();
    
    public Long getSessionTimeout();

}
