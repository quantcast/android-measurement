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