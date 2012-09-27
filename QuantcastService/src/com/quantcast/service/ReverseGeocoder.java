package com.quantcast.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ReverseGeocoder {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(ReverseGeocoder.class);

    private static final String STATUS = "status";
    private static final String OK = "OK";
    private static final String URL = "https://maps.googleapis.com/maps/api/geocode/json?sensor=true&latlng=";
    private static final String ADDRESS = "address_components";
    private static final String RESULTS = "results";
    private static final String LOCALITY = "locality";
    private static final String SHORT_NAME = "short_name";
    private static final String STATE = "administrative_area_level_1";
    private static final String COUNTRY = "country";
    private static final String TYPES = "types";

    static GeoInfo lookup(double lat, double lng) {
        String urlStr = URL + lat + "," + lng;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
            try {
                return parseJson(builder.toString());
            }
            catch (JSONException jsonException) {
                QuantcastLog.e(TAG, "Unable to get address from JSON", jsonException);
            }
        }
        catch (Exception mapsException) {
            QuantcastLog.e(TAG, "Exception thrown by Google Maps", mapsException);
        }
        return null;
    }

    private static GeoInfo parseJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        if (OK.equals(json.optString(STATUS))) {
            JSONArray resultsArray = json.optJSONArray(RESULTS);
            if (resultsArray != null) {
                JSONArray addressComponents = null;
                for (int i = 0; i < resultsArray.length(); i++) {
                    addressComponents = resultsArray.getJSONObject(i).optJSONArray(ADDRESS);
                    if (addressComponents != null) {
                        String country = "", locality = "", state = "";
                        for (int j = 0; j < addressComponents.length(); j++) {
                            JSONObject obj = addressComponents.getJSONObject(j);
                            JSONArray types = obj.optJSONArray(TYPES);
                            if (types != null) {
                                if (containsString(types, LOCALITY))
                                    locality = obj.getString(SHORT_NAME);
                                if (containsString(types, STATE))
                                    state = obj.getString(SHORT_NAME);
                                if (containsString(types, COUNTRY))
                                    country = obj.getString(SHORT_NAME);
                            }
                        }
                        return new GeoInfo(country, locality, state);
                    }
                }
            }
        }
        return null;
    }

    private static boolean containsString(JSONArray arr, String s) throws JSONException {
        String tmp;
        for (int i = 0; i < arr.length(); i++) {
            tmp = arr.optString(i);
            if (tmp != null && tmp.equals(s))
                return true;
        }
        return false;
    }
}
