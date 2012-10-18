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
