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

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.database.sqlite.SQLiteDatabaseCorruptException;
import com.quantcast.measurement.event.Event;
import com.quantcast.measurement.event.EventManager;
import com.quantcast.measurement.event.EventQueue;

class ConcurrentEventQueue extends Thread implements EventQueue {

    private static final long SLEEP_TIME_IN_MS = 500;
    private static final long UPLOAD_INTERVAL_IN_MS = 10 * 1000; // 10 seconds
    
    private static final String THREAD_NAME = ConcurrentEventQueue.class.getName();

    private final ConcurrentLinkedQueue<Event> eventQueue;
    private final EventManager eventManager;
    private long nextUploadTime;
    private volatile boolean uploadingPaused;

    public ConcurrentEventQueue(EventManager manager) {
        super(THREAD_NAME);
        eventQueue = new ConcurrentLinkedQueue<Event>();
        this.eventManager = manager;
        setNextUploadTime();
        start();
    }
    
    private void setNextUploadTime() {
        nextUploadTime = System.currentTimeMillis() + UPLOAD_INTERVAL_IN_MS;
    }

    @Override
    public void push(Event event) {
        eventQueue.add(event);
        synchronized (this) {
            notifyAll();
        } 
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
    
    @Override
    public void run() {
        do {
            try {
                Thread.sleep(SLEEP_TIME_IN_MS);
                if (uploadingPaused && eventQueue.isEmpty()) {
                    synchronized (this) {
                        wait();
                    }
                }
            }
            catch (InterruptedException e) {
                // Do nothing
            }

            try{
                boolean savedUploadForcingEvent = false;
                boolean uploadingShouldBePaused = uploadingPaused;
                LinkedList<Event> eventsToSave = new LinkedList<Event>();
                while (!eventQueue.isEmpty()) {
                    Event event = eventQueue.poll();
                    eventsToSave.add(event);
                    savedUploadForcingEvent |= event.getEventType().isUploadForcing();
                    if (event.getEventType().isUploadPausing()) {
                        uploadingShouldBePaused = true;
                    } else if (event.getEventType().isUploadResuming()) {
                        uploadingShouldBePaused = false;
                    }
                }
                eventManager.saveEvents(eventsToSave);

                if (!(uploadingPaused && uploadingShouldBePaused) && (savedUploadForcingEvent || System.currentTimeMillis() >= nextUploadTime)) {
                    setNextUploadTime();
                    eventManager.attemptEventsUpload(savedUploadForcingEvent);
                }
                uploadingPaused = uploadingShouldBePaused;
            } catch(SQLiteDatabaseCorruptException e) {
                //delete entire DB
                eventQueue.clear();
                eventManager.deleteDB();
            } catch (Exception e) {
                //clean up last calls and keep the loop going
                eventQueue.clear();

            }
        } while(true);
    }

}
