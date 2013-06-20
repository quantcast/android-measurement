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
package com.quantcast.measurement.service;

import android.content.Context;

import com.quantcast.measurement.event.EventManager;
import com.quantcast.measurement.event.EventQueue;
import com.quantcast.policy.Policy;
import com.quantcast.policy.PolicyEnforcer;
import com.quantcast.policy.PolicyGetter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

class MeasurementSession {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(MeasurementSession.class);
    protected static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    
    private final Context context;
    private final String apiKey;
    private final EventManager manager;
    private final EventQueue eventQueue;
    private final PolicyGetter policyGetter;

    private QuantcastLocationManager _locationManager;

    private final String userId;

    private String sessionId;

    private boolean paused;
    private long lastPause;

    private static final Pattern apiKeyPattern = Pattern.compile("[a-zA-Z0-9]{16}-[a-zA-Z0-9]{16}");
    private static ConcurrentEventQueue eventQueueSingleton;
    
    private static ConcurrentEventQueue getConcurrentEventQueue(Context context, PolicyEnforcer policyEnforcer, int minUploadSize, int maxUploadSize) {
        if (eventQueueSingleton == null) {
            eventQueueSingleton = new ConcurrentEventQueue(new QuantcastManager(context, policyEnforcer, minUploadSize, maxUploadSize));
        }
        return eventQueueSingleton;
    }

    /**
     * This constructor is not safe for concurrent calls
     * 
     * @param apiKey
     * @param userId
     * @param context
     * @param labels
     * @param minUploadSize
     * @param maxUploadSize
     */
    public MeasurementSession(String apiKey, String userId, Context context, String[] labels, int minUploadSize, int maxUploadSize) {
        validateApiKey(apiKey);

        this.userId = userId;
        this.context = context.getApplicationContext();
        this.apiKey = apiKey;
        
        QuantcastPolicyEnforcer quantcastPolicyEnforcer = new QuantcastPolicyEnforcer(context, QuantcastServiceUtility.API_VERSION, apiKey);
        this.policyGetter = quantcastPolicyEnforcer;
        
        ConcurrentEventQueue concurrentEventQueue = getConcurrentEventQueue(context, quantcastPolicyEnforcer, minUploadSize, maxUploadSize);
        eventQueue = concurrentEventQueue;
        manager = concurrentEventQueue.getEventManager();

        logBeginSessionEvent(BeginSessionEvent.Reason.LAUNCH, labels);
    }

    public MeasurementSession(MeasurementSession previousSession, String userId) {
        this.context = previousSession.context;
        this.userId = userId;
        this.apiKey = previousSession.apiKey;
        this.manager = previousSession.manager;
        this.eventQueue = previousSession.eventQueue;
        this.policyGetter = previousSession.policyGetter;
        this.paused = previousSession.paused;
        this.lastPause = previousSession.lastPause;

        logBeginSessionEvent(BeginSessionEvent.Reason.USERHHASH, null);
    }

    /**
     * This is only meant for testing
     * 
     * @param context
     * @param apiKey
     * @param manager
     * @param eventQueue
     * @param policyGetter
     * @param userId
     */
    public MeasurementSession(Context context, String apiKey, EventManager manager, EventQueue eventQueue, PolicyGetter policyGetter, String userId) {
        this.context = context;
        this.apiKey = apiKey;
        this.manager = manager;
        this.eventQueue = eventQueue;
        this.policyGetter = policyGetter;
        this.userId = userId;
    }

    private void logBeginSessionEvent(BeginSessionEvent.Reason reason, String[] labels) {
        sessionId = QuantcastServiceUtility.generateUniqueId();;
        postEvent(new BeginSessionEvent(context, sessionId, reason, apiKey, userId, encodeLabelsForUpload(labels)));
    }

    public void logEvent(String name, String[] labels) {
        postEvent(new AppDefinedEvent(sessionId, name, encodeLabelsForUpload(labels)));
    }

    public void pause(String[] labels) {
        paused = true;
        lastPause = System.currentTimeMillis();
        postEvent(new BaseEvent(QuantcastEventType.PAUSE_SESSION, sessionId, encodeLabelsForUpload(labels)));
    }

    public void resume(String[] labels) {
        QuantcastGlobalControlProvider.getProvider(context).refresh();
        postEvent(new BaseEvent(QuantcastEventType.RESUME_SESSION, sessionId, encodeLabelsForUpload(labels)));
        if (paused && lastPause + getSessionTimeoutInMs() < System.currentTimeMillis()) {
            logBeginSessionEvent(BeginSessionEvent.Reason.RESUME, new String[0]);
        }
        this.updateLocation();
        paused = false;
    }
    
    protected long getSessionTimeoutInMs() {
        long sessionTimeoutInMs = DEFAULT_SESSION_TIMEOUT;
        
        Policy policy = policyGetter.getPolicy();
        if (policy != null && policy.hasSessionTimeout()) {
            sessionTimeoutInMs = policy.getSessionTimeout();
        }
        
        return sessionTimeoutInMs;
    }

    public void end(String[] labels) {
        postEvent(new BaseEvent(QuantcastEventType.END_SESSION, sessionId, encodeLabelsForUpload(labels)));
    }

    public void logLocation(MeasurementLocation location) {
        postEvent(new LocationEvent(sessionId, location));
    }

    public void logLatency(UploadLatency latency) {
        postEvent(new LatencyEvent(sessionId, latency));
    }

    private void postEvent(BaseEvent event) {
        eventQueue.push(event);
    }

    public void startLocationGathering() {
        //make the location manager.  it will kick in on start.
        if( null == _locationManager ){
            _locationManager = new QuantcastLocationManager(context, this);
        }
    }

    private void updateLocation(){
        if( null != _locationManager){
            _locationManager.start();
        }
    }

    public void stopLocationGathering(){
        if(null != _locationManager){
            _locationManager.stop();
            _locationManager = null;
        }
    }

    public void setUploadEventCount(int uploadEventCount) {
        manager.setMinUploadSize(uploadEventCount);
    }

    String getSessionId() {
        return sessionId;
    }

    void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    boolean isPaused() {
        return paused;
    }

    void setPaused(boolean paused) {
        this.paused = paused;
    }

    long getLastPause() {
        return lastPause;
    }

    void setLastPause(long lastPause) {
        this.lastPause = lastPause;
    }

    String getUserId() {
        return userId;
    }

    private String encodeLabelsForUpload(String[] labels) {
        String labelsString = null;

        if (labels != null && labels.length > 0) {
            StringBuilder labelBuffer = new StringBuilder();

            try {
                for (String label : labels) {
                    String encoded = URLEncoder.encode(label, "UTF-8");
                    if (labelBuffer.length() == 0) {
                        labelBuffer.append(encoded);
                    } else {
                        labelBuffer.append(",").append(label);
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


    private void validateApiKey(String apiKey) {
        if (apiKey == null) {
            throw new IllegalArgumentException("No Quantcast API Key was passed to the SDK. Please use the API Key provided to you by Quantcast.");
        }

        if (!apiKeyPattern.matcher(apiKey).matches()) {
            throw new IllegalArgumentException("The Quantcast API Key passed to the SDK is malformed. Please use the API Key provided to you by Quantcast.");
        }
    }
}