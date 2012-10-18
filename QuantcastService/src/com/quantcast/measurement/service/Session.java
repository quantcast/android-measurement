package com.quantcast.measurement.service;

class Session {
    
    private final String id;
    
    Session() {
        this.id = QuantcastServiceUtility.generateUniqueId();
    }

    public String getId() {
        return id;
    }
}