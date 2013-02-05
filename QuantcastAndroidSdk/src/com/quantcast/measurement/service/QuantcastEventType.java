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

import com.quantcast.measurement.event.EventType;

enum QuantcastEventType implements EventType {
    BEGIN_SESSION("load", false),
    END_SESSION("finished", false),
    PAUSE_SESSION("pause", true),
    RESUME_SESSION("resume", false),
    APP_DEFINED("appevent", false),
    LATENCY("latency", false),
    LOCATION("location", false), 
    GENERIC(null, false);
    
    private final String parameterValue;
    public final boolean shouldForceUpload;

    private QuantcastEventType(String parameterValue, boolean shouldForceUpload) {
        this.parameterValue = parameterValue;
        this.shouldForceUpload = shouldForceUpload;
    }
    
    @Override
    public boolean shouldForceUpload() {
        return shouldForceUpload;
    }

    @Override
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
