package com.quantcast.measurement.service;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

class EventQueue {


    private static final long SLEEP_TIME_IN_MS = 500;
    private static final long UPLOAD_INTERVAL_IN_MS = 10 * 1000; // 10 seconds

    private volatile boolean continueThread;
    private final ConcurrentLinkedQueue<Event> events;
    private long nextUploadTime;

    public EventQueue(final QuantcastManager manager) {
        continueThread = true;
        events = new ConcurrentLinkedQueue<Event>();
        setNextUploadTime();
        new Thread(new Runnable() {

            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(SLEEP_TIME_IN_MS);
                    }
                    catch (InterruptedException e) {
                        // Do nothing
                    }

                    boolean forceUpload = false;
                    LinkedList<Event> eventsToSave = new LinkedList<Event>();
                    while (!events.isEmpty()) {
                        Event event = events.poll();
                        eventsToSave.add(event);
                        forceUpload |= event.getEventType().forceUpload;
                    }
                    manager.saveEvents(eventsToSave);
                    
                    if (forceUpload || System.currentTimeMillis() >= nextUploadTime) {
                        setNextUploadTime();
                        manager.uploadEvents(forceUpload);
                    }
                } while(continueThread || !events.isEmpty());

                manager.destroy();
            }
        }).start();
    }
    
    private void setNextUploadTime() {
        nextUploadTime = System.currentTimeMillis() + UPLOAD_INTERVAL_IN_MS;
    }

    public void terminate() {
        continueThread = false;
    }

    public void push(Event event) {
        events.add(event);
    }

}
