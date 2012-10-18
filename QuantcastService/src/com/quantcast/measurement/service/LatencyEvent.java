package com.quantcast.measurement.service;

import com.quantcast.json.JsonMap;
import com.quantcast.json.JsonString;

@SuppressWarnings("serial")
class LatencyEvent extends Event {
    
    protected static final String PARAMETER_LATENCY = "latency";
    protected static final String PARAMETER_PREVIOUS_UPLOAD_LATENCY = "value";
    protected static final String PARAMETER_PREVIOUS_UPLOAD_ID = "uplid";
    
    public LatencyEvent(Session session, UploadLatency latency) {
        super(EventType.LATENCY, session);
        
        JsonMap latencyJson = new JsonMap();
        latencyJson.put(PARAMETER_PREVIOUS_UPLOAD_ID, new JsonString(latency.getUploadId()));
        latencyJson.put(PARAMETER_PREVIOUS_UPLOAD_LATENCY, new JsonString(latency.getUploadTime()));
        
        put(PARAMETER_LATENCY, latencyJson);
    }
    
}
