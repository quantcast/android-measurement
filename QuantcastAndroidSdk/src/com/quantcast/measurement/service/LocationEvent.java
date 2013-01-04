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
