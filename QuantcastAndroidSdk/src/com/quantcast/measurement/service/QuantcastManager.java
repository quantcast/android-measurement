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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;

import com.quantcast.measurement.event.Event;
import com.quantcast.measurement.event.EventDAO;
import com.quantcast.measurement.event.EventManager;
import com.quantcast.measurement.event.IdentifiableEvent;
import com.quantcast.policy.PolicyEnforcer;
import com.quantcast.settings.GlobalControl;
import com.quantcast.settings.GlobalControlListener;
import com.quantcast.settings.GlobalControlProvider;


/**
 * This class manages the Quantcast API.
 * It has state for initialization, knows how to receive events
 * from a client, and can launch a task to upload them.
 * It is internal to the Quantcast service.
 * It runs in its service thread, and must be created on that thread (not the main thread).
 */
class QuantcastManager implements GlobalControlListener, EventManager {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastManager.class);

    private static final int UPLOAD_WAIT_TIME_IN_MS = 30000; // 30 seconds
    
    private static final String DELETING_THREAD_NAME = QuantcastManager.class.getName() + "#deleting";

    private final Context context;
    private final EventDAO eventDAO;
    private final GlobalControlProvider globalControlProvider;
    private final Uploader uploader;

    private boolean withEvents;

    private final PolicyEnforcer policyEnforcer;

    private final AtomicBoolean uploadInProgress = new AtomicBoolean(false);
    private volatile long nextTimeUploadAllowed;
    
    private final int maxUploadSize;
    private int minUploadSize;

    /**
     * Constructor.
     * Must be called from service thread.
     *
     * @param apiKey            The apiKey provided by the developer
     * @param context           A context to use for resources
     */
    QuantcastManager(Context context, PolicyEnforcer policyEnforcer, int minUplaodSize, int maxUploadSize) {
        context = context.getApplicationContext();
        this.context = context;
        this.policyEnforcer = policyEnforcer;
        eventDAO = DatabaseEventDAO.getDatabaseHelper(context);

        withEvents = true;

        this.globalControlProvider = QuantcastGlobalControlProvider.getProvider(context);
        globalControlProvider.registerListener(this);
        
        this.maxUploadSize = maxUploadSize;
        this.minUploadSize = minUplaodSize;
        
        this.uploader = new QuantcastUploader();
    }
    
    /**
     * This shouldn't be called directly and is only meant to be used for testing
     */
    QuantcastManager(Context context, EventDAO eventDAO, GlobalControlProvider globalControlProvider, PolicyEnforcer policyEnforcer, Uploader uploader, int minUploadSize, int maxUploadSize) {
        this.context = context;
        this.eventDAO = eventDAO;
        this.globalControlProvider = globalControlProvider;
        this.policyEnforcer = policyEnforcer;
        this.uploader = uploader;
        this.minUploadSize = minUploadSize;
        this.maxUploadSize = maxUploadSize;
        withEvents = true;
    }
    
    @Override
    public void setMinUploadSize(int minUploadSize) {
        this.minUploadSize = minUploadSize;
    }

    @Override
    public void attemptEventsUpload(final boolean shouldForceUpload) {
        final long currentTime = System.currentTimeMillis();
        if (currentTime >= nextTimeUploadAllowed) {
            boolean shouldDoUpload = uploadInProgress.compareAndSet(false, true);

            if (shouldDoUpload) {
                QuantcastLog.i(TAG, "Queueing upload.");
                globalControlProvider.getControl(new GlobalControlListener() {

                    @Override
                    public void callback(GlobalControl control) {
                        if (!control.blockingEventCollection) {
                            List<? extends IdentifiableEvent> eventsToUpload = eventDAO.getEvents(maxUploadSize);

                            if (eventsToUpload.size() >= minUploadSize || shouldForceUpload) {
                                nextTimeUploadAllowed = currentTime + UPLOAD_WAIT_TIME_IN_MS;
                                
                                QuantcastLog.i(TAG, "Uploading " + eventsToUpload.size() + " events.");
                                if(uploader.uploadEvents(context, eventsToUpload, policyEnforcer)) {
                                    eventDAO.removeEvents(eventsToUpload);
                                }
                            }

                            uploadInProgress.set(false);
                            QuantcastLog.i(TAG, "Upload finished.");
                        }
                    }

                });
            } else {
                QuantcastLog.i(TAG, "Can't upload because another upload is queued.");
            } 
        } else {
            QuantcastLog.i(TAG, "Can't upload now (" + currentTime +"). Can upload at " + nextTimeUploadAllowed + ".");
        }
    }

    @Override
    public void saveEvents(Collection<? extends Event> events) {
        if (!events.isEmpty()) {
            if (globalControlProvider.isDelayed() || !globalControlProvider.getControl().blockingEventCollection) {
                QuantcastLog.i(TAG, "Saving " + events.size() + " events.");
                addAsyncParameters(events);
                eventDAO.writeEvents(events);
            }
        }
    }

    /**
     * Use this method to add any parameters to events that may be too costly to add in the main thread
     * 
     * @param events
     */
    private void addAsyncParameters(Collection<? extends Event> events) {
        for (Event event : events) {
            if (event instanceof BeginSessionEvent) {
                ((BeginSessionEvent) event).addAsyncParameters(context);
            }
        }
    }

    // Keep database operations asynchronous because this may be called without user provocation with the main thread
    private void deleteEvents() {
        if (withEvents) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    QuantcastLog.i(TAG, "Deleting logged events.");
                    eventDAO.removeAllEvents();
                    withEvents = false;
                }

            }, DELETING_THREAD_NAME).start();
        }
    }

    @Override
    public void destroy() {
        QuantcastLog.i(TAG, "Relinquishing resources.");
        globalControlProvider.unregisterListener(this);
    }

    @Override
    public void callback(GlobalControl control) {
        if (control.blockingEventCollection) {
            deleteEvents();
        }
    }

}
