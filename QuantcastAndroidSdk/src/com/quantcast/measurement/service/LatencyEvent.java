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

import com.quantcast.json.JsonMap;
import com.quantcast.json.JsonString;

@SuppressWarnings("serial")
class LatencyEvent extends BaseEvent {
    
    protected static final String PARAMETER_LATENCY = "latency";
    protected static final String PARAMETER_PREVIOUS_UPLOAD_LATENCY = "value";
    protected static final String PARAMETER_PREVIOUS_UPLOAD_ID = "uplid";
    
    public LatencyEvent(String sessionId, UploadLatency latency) {
        super(QuantcastEventType.LATENCY, sessionId);
        
        JsonMap latencyJson = new JsonMap();
        latencyJson.put(PARAMETER_PREVIOUS_UPLOAD_ID, new JsonString(latency.getUploadId()));
        latencyJson.put(PARAMETER_PREVIOUS_UPLOAD_LATENCY, new JsonString(latency.getUploadTime()));
        
        put(PARAMETER_LATENCY, latencyJson);
    }
    
}
