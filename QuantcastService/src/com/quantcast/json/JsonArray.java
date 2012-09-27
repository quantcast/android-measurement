package com.quantcast.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("serial")
public class JsonArray extends ArrayList<Jsonifiable> implements Jsonifiable {
    
    public JsonArray() {
        super();
    }

    public JsonArray(List<? extends Jsonifiable> json) {
        super(json);
    }

    @Override
    public String toJson() {
        String json = Jsonifiable.NULL;

        if (!isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder("[");

            Iterator<Jsonifiable> iter = iterator();
            while(iter.hasNext()) {
                stringBuilder.append(iter.next().toJson());
                if (iter.hasNext()) {
                    stringBuilder.append(',');
                }
            }

            stringBuilder.append(']');
            json = stringBuilder.toString();
        }
        
        return json;
    }

}
