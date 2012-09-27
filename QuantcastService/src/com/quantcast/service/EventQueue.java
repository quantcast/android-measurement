package com.quantcast.service;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

class EventQueue {
    
    private static final long SLEEP_TIME_IN_MS = 10 * 1000; // 10 seconds
    
    private Thread managementThread;
    private volatile boolean continueThread;
    private Queue<Event> events;
    private volatile boolean shouldUpload;
    
    private EventQueue eventQueue = this;
    
    public EventQueue(final QuantcastManager manager) {
        continueThread = true;
        events = generateNewEventsQueue();
        managementThread = new Thread(new Runnable() {
            
            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(SLEEP_TIME_IN_MS);
                    }
                    catch (InterruptedException e) {
                        // Do nothing
                    }
                    
                    Queue<Event> eventsToHandle;
                    boolean forceUpload = false;
                    synchronized (eventQueue) {
                        eventsToHandle = events;
                        forceUpload = shouldUpload;
                        events = generateNewEventsQueue();
                    }
                    Set<Event> unsavedEvents = manager.handleEvents(eventsToHandle, forceUpload);
                    if (unsavedEvents.size() > 0) {
                        synchronized (eventQueue) {
                            events.addAll(unsavedEvents);
                        }
                    }
                } while(continueThread || !events.isEmpty());
                
                manager.destroy();
            }
        });
        managementThread.start();
    }
    
    private Queue<Event> generateNewEventsQueue() {
        shouldUpload = false;
        return new LinkedList<Event>();
    }
    
    public void terminate() {
        continueThread = false;
    }
    
    public synchronized void push(Event event) {
        events.add(event);
        if (event.getEventType().forceUpload) {
            shouldUpload = true;
        }
    }

}
