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
    BEGIN_SESSION("load", false, false, true),
    END_SESSION("finished", false, true, false),
    PAUSE_SESSION("pause", true, true, false),
    RESUME_SESSION("resume", false, false, true),
    APP_DEFINED("appevent", false, false, false),
    LATENCY("latency", false, false, false),
    LOCATION("location", false, false, false), 
    GENERIC(null, false, false, false);
    
    private final String parameterValue;
    private final boolean uploadForcing;
    private final boolean uploadPausing;
    private final boolean uploadResuming;

    private QuantcastEventType(String parameterValue, boolean uploadForcing, boolean uploadPausing, boolean uploadResuming) {
        this.parameterValue = parameterValue;
        this.uploadForcing = uploadForcing;
        this.uploadPausing = uploadPausing;
        this.uploadResuming = uploadResuming;
    }
    
    @Override
    public boolean isUploadForcing() {
        return uploadForcing;
    }
    
    @Override
    public boolean isUploadPausing() {
        return uploadPausing;
    }
    
    @Override
    public boolean isUploadResuming() {
        return uploadResuming;
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
