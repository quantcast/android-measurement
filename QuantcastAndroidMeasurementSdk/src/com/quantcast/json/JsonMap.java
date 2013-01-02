package com.quantcast.json;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("serial")
public class JsonMap extends HashMap<String, Jsonifiable> implements Jsonifiable {

    public JsonMap() {
        super();
    }
    
    public JsonMap(Map<? extends String, ? extends Jsonifiable> map) {
        super(map);
    }

    @Override
    public String toJson() {
        String json = Jsonifiable.NULL;

        if (!isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder("{");
            
            Iterator<Entry<String, Jsonifiable>> iter = entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, Jsonifiable> entry = iter.next();
                stringBuilder.append(new JsonString(entry.getKey()).toJson() + ":" + entry.getValue().toJson());
                if (iter.hasNext()) {
                    stringBuilder.append(',');
                }
            }
            
            stringBuilder.append('}');
            json = stringBuilder.toString();
        }

        return json;
    }

}
