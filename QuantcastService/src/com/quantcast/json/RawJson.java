package com.quantcast.json;

public class RawJson implements Jsonifiable {
    
    private String json;
    
    public RawJson(String json) {
        this.json = json;
    }

    @Override
    public String toJson() {
        return json;
    }

}
