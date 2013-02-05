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
import com.quantcast.policy.PolicyGetter;

class MeasurementSession {
    
    protected static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    private final Context context;
    private final String apiKey;
    private final EventManager manager;
    private final EventQueue eventQueue;
    private final PolicyGetter policyGetter;

    private final String userId;

    private String sessionId;

    private boolean paused;
    private long lastPause;

    public MeasurementSession(String apiKey, String userId, Context context, String[] labels, int minUploadSize, int maxUploadSize) {
        QuantcastClient.validateApiKey(apiKey);

        this.userId = userId;
        this.context = context.getApplicationContext();
        this.apiKey = apiKey;
        
        QuantcastPolicyEnforcer quantcastPolicyEnforcer = new QuantcastPolicyEnforcer(context, QuantcastServiceUtility.API_VERSION, apiKey);
        this.policyGetter = quantcastPolicyEnforcer;
        manager = new QuantcastManager(context, quantcastPolicyEnforcer, minUploadSize, maxUploadSize);
        eventQueue = new ConcurrentEventQueue(manager);

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
        postEvent(new BeginSessionEvent(context, sessionId, reason, apiKey, userId, QuantcastClient.encodeLabelsForUpload(labels)));
    }

    public void logEvent(String name, String[] labels) {
        postEvent(new AppDefinedEvent(sessionId, name, QuantcastClient.encodeLabelsForUpload(labels)));
    }

    public void pause(String[] labels) {
        paused = true;
        lastPause = System.currentTimeMillis();
        postEvent(new BaseEvent(QuantcastEventType.PAUSE_SESSION, sessionId, QuantcastClient.encodeLabelsForUpload(labels)));
    }

    public void resume(String[] labels) {
        QuantcastGlobalControlProvider.getProvider(context).refresh();
        postEvent(new BaseEvent(QuantcastEventType.RESUME_SESSION, sessionId, QuantcastClient.encodeLabelsForUpload(labels)));
        if (paused && lastPause + getSessionTimeoutInMs() < System.currentTimeMillis()) {
            logBeginSessionEvent(BeginSessionEvent.Reason.RESUME, new String[0]);
        }
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
        postEvent(new BaseEvent(QuantcastEventType.END_SESSION, sessionId, QuantcastClient.encodeLabelsForUpload(labels)));
        eventQueue.terminate();
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
        QuantcastClient.startLocationGathering(context);
    }

    public void setUplaodEventCount(int uploadEventCount) {
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

}