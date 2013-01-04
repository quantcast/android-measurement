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


enum EventType {
    BEGIN_SESSION("load", false),
    END_SESSION("finished", false),
    PAUSE_SESSION("pause", true),
    RESUME_SESSION("resume", false),
    APP_DEFINED("appevent", false),
    LATENCY("latency", false),
    LOCATION("location", false), 
    GENERIC(null, false);
    
    private final String parameterValue;
    public final boolean forceUpload;

    /**
     * 
     * @param parameterValue            Value of the "event" parameter in the JSON representation of an {@link Event}
     * @param eventParametersFactory    An {@link EventParametersFactory} which returns an empty {@link EventParameters} implementation.
     *                                  This aids in passing event parameters through service layers.
     *                                  It is not necessary for events which are not initialized from the main thread. 
     */
    private EventType(String parameterValue, boolean forceUpload) {
        this.parameterValue = parameterValue;
        this.forceUpload = forceUpload;
    }

    public String getParameterValue() {
        return parameterValue;
    }
    
    public static EventType valueOf(int ordinal) {
        EventType eventType = null;
        
        if (ordinal >= 0 && ordinal < values().length) {
            eventType = values()[ordinal];
        }
        
        return eventType;
    }
}
