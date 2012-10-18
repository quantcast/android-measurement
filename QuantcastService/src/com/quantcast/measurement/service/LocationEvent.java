package com.quantcast.measurement.service;

import com.quantcast.json.JsonString;


@SuppressWarnings("serial")
class LocationEvent extends Event {
    
    protected static final String LOCALITY_PARAMETER = "l";
    protected static final String PARAMETER_COUNTRY = "c";
    protected static final String PARAMETER_STATE = "st";
    
    public LocationEvent(Session session, MeasurementLocation location) {
        super(EventType.LOCATION, session);
        
        put(PARAMETER_COUNTRY, new JsonString(location.getCountry()));
        put(PARAMETER_STATE, new JsonString(location.getState()));
        put(LOCALITY_PARAMETER, new JsonString(location.getLocality()));
    }

}
