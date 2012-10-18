package com.quantcast.measurement.service;

class GeoInfo {
    String country;
    String state;
    String locality;

    GeoInfo(String c, String s, String l) {
        country = c;
        state = s;
        locality = l;
    }
}