package com.quantcast.measurement.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.quantcast.settings.GlobalControl;
import com.quantcast.settings.GlobalControlListener;

/**
 * Client API for Quantcast Measurement service.
 *
 * This exposes only those methods that may be called by developers using the Quantcast Measurement API.
 */
public class QuantcastClient {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastClient.class);

    private static final long TIME_TO_NEW_SESSION_IN_MS = 10 * 60 * 1000; // 10 minutes

    private static MeasurementSession session;
    private static Object sessionLock = new Object();

    private static Boolean enableLocationGathering = false;

    public static Set<Integer> activeContexts;

    /**
     * Start a new measurement session. Should be called in the main activity's onCreate method.
     *
     * @param context               Main Activity using the Quantcast Measurement API
     * @param publisherCode         Developer's P-code, obtained from www.quantcast.com
     * @param labels                Any arbitrary string that you want to be associated with this event, and will create a second dimension in Quantcast Measurement reporting.
     *                              Nominally, this is a "user class" indicator.
     *                              For example, you might use one of two labels in your app: one for user who have not purchased an app upgrade, and one for users who have purchased an upgrade.
     */
    public static void beginSession(Activity activity, String publisherCode, String... labels) {
        synchronized (sessionLock) {
            if (activeContexts == null) {
                activeContexts = new HashSet<Integer>();
            }
            activeContexts.add(activity.hashCode());

            QuantcastLog.i(TAG, activeContexts.size() + " active contexts.");
            if (session == null) {
                QuantcastLog.i(TAG, "Initializing new session.");
                QuantcastGlobalControlProvider.getProvider(activity).refresh();
                session = new MeasurementSession(activity, publisherCode, labels);
                startLocationGathering(activity);
                QuantcastLog.i(TAG, "New session initialization complete.");
            }
        }
    }

    static void addActivity(Activity activity) {
        synchronized (sessionLock) {
            activeContexts.add(activity.hashCode());
        }
    }

    /**
     * Log a user identifier to the service. This will begin a new measurement session, but must be called during another measurement session in order to pull the necessary parameters.
     *
     * @param userId                A consistent identifier for the current user.
     *                              Any user identifier recorded will be save for all future session until it a new user identifier is recorded.
     *                              Record a user identifier of {@link null} should be used for a log out and will remove any saved user identifier.
     */
    public static void recordUserIdentifier(String userId) {
        synchronized (sessionLock) {
            if (session != null) {
                session.logUserId(userId);
            }
        }
    }

    /**
     * Logs an app-defined event can be arbitrarily defined.
     *
     * @param name                  A string that identifies the event being logged. Hierarchical information can be indicated by using a left-to-right notation with a period as a separator.
     *                              For example, logging one event named "button.left" and another named "button.right" will create three reportable items in Quantcast App Measurement:
     *                              "button.left", "button.right", and "button".
     *                              There is no limit on the cardinality that this hierarchical scheme can create,
     *                              though low-frequency events may not have an audience report on due to the lack of a statistically significant population.
     * @param labels                Any arbitrary string that you want to be associated with this event, and will create a second dimension in Quantcast Measurement reporting.
     *                              Nominally, this is a "user class" indicator.
     *                              For example, you might use one of two labels in your app: one for user who have not purchased an app upgrade, and one for users who have purchased an upgrade.
     */
    public static void logEvent(String name, String... labels) {
        synchronized (sessionLock) {
            if (session != null) {
                session.logEvent(name, labels);
            }
        }
    }

    /**
     * Logs a pause event as well as evoking some internal maintenance. This should be called in the main activity's onPause method
     * 
     * @param labels                Any arbitrary string that you want to be associated with this event, and will create a second dimension in Quantcast Measurement reporting.
     *                              Nominally, this is a "user class" indicator.
     *                              For example, you might use one of two labels in your app: one for user who have not purchased an app upgrade, and one for users who have purchased an upgrade.
     */
    public static void pauseSession(String... labels) {
        synchronized (sessionLock) {
            if (session != null) {
                session.pause(labels);
            }
        }
    }

    /**
     * Logs a resume event as well as evoking some internal maintenance. This should be called in the main activity's onResume method
     * 
     * @param labels                Any arbitrary string that you want to be associated with this event, and will create a second dimension in Quantcast Measurement reporting.
     *                              Nominally, this is a "user class" indicator.
     *                              For example, you might use one of two labels in your app: one for user who have not purchased an app upgrade, and one for users who have purchased an upgrade.
     */
    public static void resumeSession(String... labels) {
        synchronized (sessionLock) {
            if (session != null) {
                session.resume(labels);
            }
        }
    }

    /**
     * Ends the current measurement session. This will clean up all of the services resources. This should be called in the main activity's onDestroy method.
     * 
     * @param labels                Any arbitrary string that you want to be associated with this event, and will create a second dimension in Quantcast Measurement reporting.
     *                              Nominally, this is a "user class" indicator.
     *                              For example, you might use one of two labels in your app: one for user who have not purchased an app upgrade, and one for users who have purchased an upgrade.
     */
    public static void endSession(Activity activity, String... labels) {
        synchronized (sessionLock) {
            if (activeContexts != null) {
                activeContexts.remove(activity.hashCode());
                QuantcastLog.i(TAG, activeContexts.size() + " active contexts.");
            } else {
                QuantcastLog.i(TAG, "No active contexts.");
            }
            if (activeContexts == null || activeContexts.isEmpty()) {
                stopLocationGathering();
                if (session != null) {
                    session.end(labels);
                }
                session = null;
            }
        }
    }

    /**
     * Use this to control whether or not the service should collect location data. You should only enabled location gathering if your app has some location-aware purpose.
     * 
     * @param enableLocationGathering       Set to true to enable location, false to disable
     */
    public static void setEnableLocationGathering(boolean enableLocationGathering) {
        synchronized(QuantcastClient.enableLocationGathering) {
            QuantcastClient.enableLocationGathering = enableLocationGathering;

            if (enableLocationGathering) {
                synchronized (sessionLock) {
                    if (session != null) {
                        session.startLocationGathering();
                    }
                }
            } else {
                stopLocationGathering();
            }
        }
    }

    /**
     * Show the About Quantcast Screen via {@link Activity#startActivity(Intent)}.
     * 
     * @param activity              The activity to create the About Quantcast Screen Activity. This activity will be returned to when the user is finished.
     */
    public static void showAboutQuantcastScreen(Activity activity) {
        activity.startActivity(new Intent(activity, AboutQuantcastScreen.class));
    }

    /**
     * Can be called to check the opt-out status of the Quantcast Service.
     * If collection is not enabled the user has opted-out.
     * The opt-out status is not guaranteed to be available at the time of the call.
     * Therefore it must be communicated via callback.
     * 
     * @param context               Main Activity using the Quantcast Measurement API
     * @param callback              The action to be taken when the opt-out status is available
     */
    public static void isCollectionEnabled(Context context, final CollectionEnabledCallback callback) {
        QuantcastGlobalControlProvider.getProvider(context).getControl(new GlobalControlListener() {

            @Override
            public void callback(GlobalControl control) {
                callback.callback(control.blockingEventCollection);
            }

        });
    }

    /**
     * Helper callback class to allow for asynchronous reaction to requests for opt-out status
     */
    public static interface CollectionEnabledCallback {

        /**
         * Called when opt-out status is available
         * 
         * @param collectionEnabled             The current opt-out status. If collection is not enabled the user has opted-out.
         */
        public void callback(boolean collectionEnabled);

    }

    /**
     * Allows you to change what logs will actually be reported by Quantcast Measurement Service classes
     * 
     * @param logLevel          The log level for Quantcast Measurement Service classes. This should be one of Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR
     */
    public static void setLogLevel(int logLevel) {
        QuantcastLog.setLogLevel(logLevel);
    }

    /**
     * Legacy. No longer logs anything
     */
    @Deprecated
    public static void logRefresh() {
        // Do nothing
    }

    /**
     * Legacy. No longer logs anything
     */
    @Deprecated
    public static void logUpdate() {
        // Do nothing
    }

    private static String encodeLabelsForUpload(String... labels) {
        String labelsString = null;

        if (labels != null && labels.length > 0) {
            StringBuffer labelBuffer = new StringBuffer();

            try {
                for (String label : labels) {
                    String encoded = URLEncoder.encode(label, "UTF-8");
                    if (labelBuffer.length() == 0) {
                        labelBuffer.append(encoded);
                    } else {
                        labelBuffer.append("," + label);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                QuantcastLog.e(TAG, "Unable to encode event labels", e);
                return null;
            }

            labelsString = labelBuffer.toString();
        }

        return labelsString;
    }

    static final void logLatency(UploadLatency latency) {
        synchronized (sessionLock) {
            if (session != null) {
                session.logLatency(latency);
            }
        }
    }

    private static class MeasurementSession implements GlobalControlListener {

        private static String userId;

        private final Context context;
        private final String publisherCode;
        private final EventQueue eventQueue;

        private Session measurementSession;

        private boolean paused;
        private long lastPause;

        public MeasurementSession(Context context, String publisherCode, String... labels) {
            if (publisherCode == null || "".equals(publisherCode)) {
                throw new IllegalArgumentException("Publisher code must be set to a non-empty value. Please use the P-code provided to you by Quantcast.");
            }

            this.context = context.getApplicationContext();
            this.publisherCode = publisherCode;

            eventQueue = new EventQueue(new QuantcastManager(context, publisherCode));

            logBeginSessionEvent(BeginSessionEvent.Reason.LAUNCH, labels);

            QuantcastGlobalControlProvider.getProvider(context).registerListener(this);
        }

        @Override
        public void callback(GlobalControl control) {
            if (!control.blockingEventCollection) {
                logBeginSessionEvent(BeginSessionEvent.Reason.LAUNCH);
            }
        }

        public void logUserId(String userId) {
            MeasurementSession.userId = userId;
            logBeginSessionEvent(BeginSessionEvent.Reason.USERHHASH);
        }

        private void logBeginSessionEvent(BeginSessionEvent.Reason reason, String... labels) {
            measurementSession = new Session();
            postEvent(new BeginSessionEvent(context, measurementSession, reason, publisherCode, userId, encodeLabelsForUpload(labels)));
        }

        public void logEvent(String name, String... labels) {
            postEvent(new AppDefinedEvent(measurementSession, name, encodeLabelsForUpload(labels)));
        }

        public void pause(String... labels) {
            paused = true;
            lastPause = System.currentTimeMillis();
            postEvent(new Event(EventType.PAUSE_SESSION, measurementSession, encodeLabelsForUpload(labels)));
        }

        public void resume(String... labels) {
            QuantcastGlobalControlProvider.getProvider(context).refresh();
            // TODO this should be driven by JSON policy
            if (paused && lastPause + TIME_TO_NEW_SESSION_IN_MS < System.currentTimeMillis()) {
                logBeginSessionEvent(BeginSessionEvent.Reason.RESUME);
            }
            paused = false;
            postEvent(new Event(EventType.RESUME_SESSION, measurementSession, encodeLabelsForUpload(labels)));
        }

        public void end(String... labels) {
            postEvent(new Event(EventType.END_SESSION, measurementSession, encodeLabelsForUpload(labels)));
            QuantcastGlobalControlProvider.getProvider(context).unregisterListener(this);
            eventQueue.terminate();
        }

        public void logLocation(MeasurementLocation location) {
            postEvent(new LocationEvent(measurementSession, location));
        }

        public void logLatency(UploadLatency latency) {
            postEvent(new LatencyEvent(measurementSession, latency));
        }

        private void postEvent(Event event) {
            eventQueue.push(event);
        }
        
        public void startLocationGathering() {
            QuantcastClient.startLocationGathering(context);
        }

    }

    private static LocationMonitor locationMonitor;

    private static void startLocationGathering(Context context) {
        if (locationMonitor == null) {
            locationMonitor = new LocationMonitor(context);
            QuantcastGlobalControlProvider.getProvider(context).registerListener(locationMonitor);
        }

        QuantcastGlobalControlProvider.getProvider(context).getControl(new GlobalControlListener() {

            @Override
            public void callback(GlobalControl control) {
                if (!control.blockingEventCollection && enableLocationGathering) {
                    locationMonitor.startListening();
                }
            }

        });
    }

    private static void stopLocationGathering() {
        if (locationMonitor != null) {
            locationMonitor.stopListening();
        }
    }

    private static void logLocation(MeasurementLocation location) {
        synchronized (sessionLock) {
            if (session != null) {
                session.logLocation(location);
            }
        }
    }

    private static class LocationMonitor implements LocationListener, GlobalControlListener {

        private static final long MIN_LOC_UPDATE_TIME_IN_MS = 5 * 60 * 1000; // 5 minutes
        private static final float MIN_LOC_UPDATE_DISTANCE_IN_M = 8000; // 8km ~= 5 miles

        private final Context context;
        private final Geocoder geocoder;
        private volatile Boolean listening = false;

        public LocationMonitor(Context context) {
            this.context = context.getApplicationContext();
            geocoder = new Geocoder(context);
        }

        @Override
        public void onLocationChanged(Location location) {
            QuantcastLog.i(TAG, "Location changed");
            logLocation(location);
        }

        private void logLocation(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            QuantcastLog.i(TAG, "Looking for address.");
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude,longitude, 1);
                if(addresses != null && addresses.size() > 0) {
                    Address address = addresses.get(0);
                    QuantcastClient.logLocation(new MeasurementLocation(address.getCountryCode(), address.getAdminArea(), address.getLocality()));
                } else {
                    QuantcastLog.i(TAG, "Geocoder reverse lookup failed.");
                }
            }
            catch (IOException e) {
                QuantcastLog.i(TAG, "Geocoder API not available.");
                // call googles map api directly
                GeoInfo geoInfo = ReverseGeocoder.lookup(latitude, longitude);
                if(geoInfo != null) {
                    QuantcastClient.logLocation(new MeasurementLocation(geoInfo.country, geoInfo.state, geoInfo.locality));
                } else {
                    QuantcastLog.i(TAG, "Google Maps API reverse lookup failed.");
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Do nothing
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Do nothing
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Do nothing
        }

        public void startListening() {
            synchronized (listening) {
                if (!listening) {
                    listening = true;

                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                    Location mostRecent = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (mostRecent != null) {
                        logLocation(mostRecent);
                    } else {
                        mostRecent = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (mostRecent != null) {
                            logLocation(mostRecent);
                        }
                    }

                    if (locationManager != null) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_LOC_UPDATE_TIME_IN_MS, MIN_LOC_UPDATE_DISTANCE_IN_M, this);
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_LOC_UPDATE_TIME_IN_MS, MIN_LOC_UPDATE_DISTANCE_IN_M, this);
                    }
                }
            }
        }

        public void stopListening() {
            synchronized (listening) {
                if (listening) {
                    listening = false;

                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    locationManager.removeUpdates(this);
                }
            }
        }

        @Override
        public void callback(GlobalControl control) {
            if (control.blockingEventCollection) {
                stopListening();
            }
        }
    }
}
