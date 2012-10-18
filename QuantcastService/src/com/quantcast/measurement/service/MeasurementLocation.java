package com.quantcast.measurement.service;

class MeasurementLocation {

    private String country;
    private String state;
    private String locality;
    
    public MeasurementLocation(String country, String state, String locality) {
        this.country = country;
        this.state = state;
        this.locality = locality;
    }

    public String getCountry() {
        return country;
    }

    public String getState() {
        return state;
    }

    public String getLocality() {
        return locality;
    }
}
