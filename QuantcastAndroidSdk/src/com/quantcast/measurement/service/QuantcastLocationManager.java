/**
 * Copyright 2013 Quantcast Corp.
 *
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 *  compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package com.quantcast.measurement.service;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;

import java.io.IOException;
import java.util.List;


public class QuantcastLocationManager {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastLocationManager.class);

    private static final int STALE_LIMIT = 1000 * 60 * 20;  //20 minutes
    LocationManager _locManager;

    private String _myProvider;
    private Location _location;
    private Context _context;
    private MeasurementSession _session;
    private AsyncTask _geoTask;

    public QuantcastLocationManager(Context appContext, MeasurementSession session)
    {
        _session = session;
        _context = appContext;
        //loop through every provider and try to find a location
        _locManager= (LocationManager) _context.getSystemService(Context.LOCATION_SERVICE);

        if(_locManager != null){
            //specifically set our Criteria. All we need is a general location
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
            criteria.setSpeedRequired(false);

            _myProvider = _locManager.getBestProvider(criteria, true);

            findBestLocation();
        }
    }

    //check all providers for any recently cached data see if we can save a call
    private void findBestLocation(){
        long acceptableTimestamp = System.currentTimeMillis() - STALE_LIMIT;
        if(_location != null && _location.getTime() > acceptableTimestamp) return;

        _location = null;
        float bestAccuracy = Float.MAX_VALUE;
        List<String> matchingProviders = _locManager.getAllProviders();
        for (String provider: matchingProviders) {
            Location location = _locManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();

                if ((time > acceptableTimestamp && accuracy < bestAccuracy)) {
                    _location = location;
                    bestAccuracy = accuracy;
                }
            }
        }
    }

    public void start(){

        findBestLocation();
        if(_location != null){
            sendLocation();
        }else if (_myProvider != null){
            try{
                if(_locManager.isProviderEnabled(_myProvider)){
                    _locManager.requestLocationUpdates(_myProvider, 0, 0, singleUpdateListener, _context.getMainLooper());
                }
            }catch (Exception e){
                QuantcastLog.e(TAG, "Available location provider not found.  Skipping Location Event", e);
            }
        }else{
            QuantcastLog.i(TAG, "Available location provider not found.  Skipping Location Event");
        }

    }

    public void stop(){
        if( null != _geoTask && _geoTask.getStatus() != AsyncTask.Status.FINISHED ){
            _geoTask.cancel(true);
        }
        _geoTask = null;
    }

    private void sendLocation(){
        Double lat = _location.getLatitude();
        Double longTemp = _location.getLongitude();

        _geoTask = new AsyncTask<Double, Void, MeasurementLocation>() {
            @Override
            protected MeasurementLocation doInBackground(Double... params)
            {
                MeasurementLocation retval = null;
                double latitude = params[0];
                double longitude = params[1];
                QuantcastLog.i(TAG, "Looking for address.");
                try {
                    QuantcastLog.i(TAG, "Geocoder.");
                    List<Address> addresses = new Geocoder(_context).getFromLocation(latitude,longitude, 1);
                    if(addresses != null && addresses.size() > 0) {
                        Address address = addresses.get(0);
                        retval = new MeasurementLocation(address.getCountryCode(), address.getAdminArea(), address.getLocality());
                    } else {
                        QuantcastLog.i(TAG, "Geocoder reverse lookup failed.");
                        GeoInfo geoInfo = ReverseGeocoder.lookup(latitude, longitude);
                        if(geoInfo != null) {
                            retval = new MeasurementLocation(geoInfo.country, geoInfo.state, geoInfo.locality);
                        } else {
                            QuantcastLog.i(TAG, "Google Maps API reverse lookup failed.");
                            retval = this.fallbackGeoLocate(latitude, longitude);
                        }
                    }
                }
                catch (Exception e) {
                    QuantcastLog.i(TAG, "Geocoder API not available.");
                    retval = this.fallbackGeoLocate(latitude, longitude);
                }
                return retval;

            }

            protected MeasurementLocation fallbackGeoLocate(double latitude, double longitude){
                MeasurementLocation retval = null;
                // call googles map api directly
                GeoInfo geoInfo = ReverseGeocoder.lookup(latitude, longitude);
                if(geoInfo != null && !_geoTask.isCancelled()) {
                    retval = new MeasurementLocation(geoInfo.country, geoInfo.state, geoInfo.locality);
                } else {
                    QuantcastLog.i(TAG, "Google Maps API reverse lookup failed.");
                }
                return retval;
            }

            @Override
            protected void onPostExecute(MeasurementLocation address)
            {
                if(null != address && null != _session){
                    QuantcastLog.i(TAG, "Got address and sending..." + address.getCountry() + " " +address.getState() + " "+ address.getLocality());
                    _session.logLocation(address);
                }
            }
        }.execute(lat, longTemp);
    }

    protected LocationListener singleUpdateListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            _location = location;
            _locManager.removeUpdates(singleUpdateListener);
            if(_location != null){
                sendLocation();
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) {}
    };

}
