package com.quantcast.json;

public class JsonString implements Jsonifiable {
    
    private String string;

    public JsonString(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("The provided object cannot be null.");
        }
        this.string = object.toString();
    }

    @Override
    public String toJson() {
        return "\"" + string.replace("\"", "\\\"") + "\"";
    }
    
    @Override
    public String toString() {
        return string;
    }

}
