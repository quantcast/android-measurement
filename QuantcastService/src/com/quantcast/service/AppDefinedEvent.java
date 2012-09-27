package com.quantcast.service;

import com.quantcast.json.JsonString;


@SuppressWarnings("serial")
class AppDefinedEvent extends Event {

    private static final String EVENT_NAME_PARAMETER = "appevent";
    
    AppDefinedEvent(Session session, String name, String labels) {
        super(EventType.APP_DEFINED, session, labels);
        put(EVENT_NAME_PARAMETER, new JsonString(name));
    }
    
}
