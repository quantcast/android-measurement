package com.quantcast.service;

class Session {
    
    private final String id;
    
    Session() {
        // TODO Actually set id
        this.id = QuantcastServiceUtility.generateUniqueId();
    }

    public String getId() {
        return id;
    }
}