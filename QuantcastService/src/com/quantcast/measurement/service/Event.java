package com.quantcast.measurement.service;

import com.quantcast.json.JsonMap;
import com.quantcast.json.JsonString;


/**
 * Base class for Quantcast Events.
 *
 * An Event is an object that records data that the SDK wants to upload.
 *
 * Most parameters are simply stored in a map for genericity in the read/write code.
 * The base class Event has member variables for a couple of specific required parameters that are
 * frequently used, which are the timestamp and sid.
 */
@SuppressWarnings("serial")
class Event extends JsonMap {

    // Base event parameters
    protected static final String SESSION_ID_PARAMETER = "sid";
    protected static final String PARAMETER_ET = "et";
    protected static final String DEVICE_ID_PARAMETER = "did";
    protected static final String EVENT_TYPE_PARAMETER = "event";
    protected static final String LABELS_PARAMETER = "labels";
    protected static final String DEFAULT_MEDIA_PARAMETER = "app";
    
    private final EventType eventType;

    protected Event() {
        super();
        this.eventType = EventType.GENERIC;
    }

    Event(EventType eventType, Session session) {
        super();
        this.eventType = eventType;
        put(PARAMETER_ET, new JsonString(Long.toString(System.currentTimeMillis() / 1000)));
        put(SESSION_ID_PARAMETER, new JsonString(session.getId()));
        put(EVENT_TYPE_PARAMETER, new JsonString(eventType.getParameterValue()));
    }

    Event(EventType eventType, Session session, String labels) {
        this(eventType, session);
        if (labels != null) {
            put(LABELS_PARAMETER, new JsonString( labels));
        }
    }
    
    public EventType getEventType() {
        return eventType;
    }
    
}

