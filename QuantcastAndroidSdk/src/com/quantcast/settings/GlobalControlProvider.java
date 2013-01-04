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
package com.quantcast.settings;


public interface GlobalControlProvider {
    
    /**
     * @return Whether the global control has changed
     */
    public void refresh();

    /**
     * This should only be used when the provider is known to not be delayed
     * 
     * @param control
     */
    public void saveControl(GlobalControl control);
    
    /**
     * This should only be used when the provider is known to not be delayed
     * 
     * @return
     */
    public GlobalControl getControl();

    public void getControl(GlobalControlListener listener);
    
    public void registerListener(GlobalControlListener listener);
    
    public void unregisterListener(GlobalControlListener listener);
    
    public boolean isDelayed();

}
