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
