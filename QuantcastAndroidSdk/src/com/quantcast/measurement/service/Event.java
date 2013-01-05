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


/**
 * Base class for Quantcast Events.
 *
 * An Event is an object that records data that the SDK wants to upload.
 *
 * Most parameters are simply stored in a map for genericity in the read/write code.
 */
@SuppressWarnings("serial")
class Event extends JsonMap {

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

