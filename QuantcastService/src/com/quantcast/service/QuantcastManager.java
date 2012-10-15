package com.quantcast.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.quantcast.json.JsonArray;
import com.quantcast.json.JsonMap;
import com.quantcast.json.JsonString;
import com.quantcast.settings.GlobalControl;
import com.quantcast.settings.GlobalControlListener;


/**
 * This class manages the Quantcast API.
 * It has state for initialization, knows how to receive events
 * from a client, and can launch a task to upload them.
 * It is internal to the Quantcast service.
 * It runs in its service thread, and must be created on that thread (not the main thread).
 */
class QuantcastManager implements GlobalControlListener {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastManager.class);

    private static final String UPLOAD_ID_PARAMETER = "uplid";
    private static final String PARAMETER_QCV = "qcv";
    private static final String PARAMETER_EVENTS = "events";

    private static final String UPLOAD_URL = "http://m.quantserve.com/mobile";
    private static final String API_VERSION = "0_2_1";
    private static final long UPLOAD_WAIT = 30000;

    private static final int MAX_UPLOAD_SIZE = 2000;
    private static final int MAX_EVENTS_TO_SAVE = 200;
    private static final int MIN_UPLOAD_SIZE = 100;

    private final Context context;
    private final EventsDatabaseHelper databaseHelper;
    private final PolicyEnforcer policyEnforcer;

    private boolean withEvents;
    private boolean overflow;

    private boolean timerStarted;
    private long nextUploadTime;

    /**
     * Constructor.
     * Must be called from service thread.
     *
     * @param publisherCode     P-code
     * @param context           A context to use for resources
     */
    QuantcastManager(Context context, String publisherCode) {
        context = context.getApplicationContext();
        this.context = context;
        databaseHelper = EventsDatabaseHelper.checkoutDatabaseHelper(context);
        policyEnforcer = new PolicyEnforcer(context, API_VERSION, publisherCode);

        withEvents = true;

        QuantcastGlobalControlProvider.getProvider(context).registerListener(this);
    }

    public Set<Event> handleEvents(Queue<Event> events, boolean forceUpload) {
        Set<Event> unsavedEvents = new HashSet<Event>(events);;

        synchronized (databaseHelper) {
            boolean delayed = QuantcastGlobalControlProvider.getProvider(context).isDelayed();
            if (delayed || !QuantcastGlobalControlProvider.getProvider(context).getControl().blockingEventCollection) {
                QuantcastLog.i(TAG, "Handling events.");
                SQLiteDatabase db = null;

                if (!delayed) {
                    boolean eventsUploaded = false;
                    if (isUploadTime() || forceUpload || events.size() > MAX_UPLOAD_SIZE || overflow) {
                        List<Event> eventsToUpload = new ArrayList<Event>(events);

                        if (eventsToUpload.size() > MAX_UPLOAD_SIZE) {
                            eventsToUpload = eventsToUpload.subList(0, MAX_UPLOAD_SIZE);
                        } else {
                            db = databaseHelper.getWritableDatabase();
                            eventsToUpload.addAll(EventsDatabaseHelper.getEvents(db, MAX_UPLOAD_SIZE - eventsToUpload.size()));
                        }
                        if (eventsToUpload.size() > MIN_UPLOAD_SIZE || forceUpload) {
                            overflow = eventsToUpload.size() == MAX_UPLOAD_SIZE;
                            QuantcastLog.i(TAG, "Uploading " + eventsToUpload.size() + " events.");
                            eventsUploaded = uploadEvents(context, eventsToUpload, policyEnforcer);
                        }

                        if (eventsUploaded) {
                            for (Event event : eventsToUpload) {
                                if (!unsavedEvents.remove(event)) {
                                    EventsDatabaseHelper.removeEvent(db, event);
                                }
                            }
                        }
                    }
                }

                Iterator<Event> iter = unsavedEvents.iterator();
                if (iter.hasNext()) {
                    QuantcastLog.i(TAG, "Attempting to save " + unsavedEvents.size() + " events.");

                    if (db == null) {
                        db = databaseHelper.getWritableDatabase();
                    }
                    int eventsSaved;
                    for (eventsSaved = 0; iter.hasNext() && eventsSaved < MAX_EVENTS_TO_SAVE; eventsSaved++) {
                        Event event = iter.next();
                        EventsDatabaseHelper.writeEvent(db, event);
                        iter.remove();
                    }
                    QuantcastLog.i(TAG, "Saved " + eventsSaved + " events.");
                }

                if (db != null) {
                    databaseHelper.close();
                }
                QuantcastLog.i(TAG, "Event handling complete.");
            }
        }

        return unsavedEvents;
    }

    // Keep database operations asynchronous because this may be called without user provocation with the main thread
    private void deleteEvents() {
        if (withEvents) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    QuantcastLog.i(TAG, "Deleting logged events.");
                    synchronized (databaseHelper) {
                        SQLiteDatabase db = databaseHelper.getWritableDatabase();
                        EventsDatabaseHelper.clearEvents(db);
                        databaseHelper.close();
                        withEvents = false;
                    }
                }

            }).start();
        }
    }

    public void destroy() {
        QuantcastLog.i(TAG, "Relinquishing resources.");
        QuantcastGlobalControlProvider.getProvider(context).unregisterListener(this);
        EventsDatabaseHelper.checkinDatabaseHelper(databaseHelper);
    }

    private boolean isUploadTime() {
        if (timerStarted) {
            if (System.currentTimeMillis() >= nextUploadTime) {
                timerStarted = false;
                return true;
            }
        } else {
            nextUploadTime = System.currentTimeMillis() + UPLOAD_WAIT;
            timerStarted = true;
        }

        return false;
    }

    @Override
    public void callback(GlobalControl control) {
        if (control.blockingEventCollection) {
            deleteEvents();
        }
    }

    // TODO This method could be refactored. It's large and hard to follow.
    /**
     * Upload any events in the db. On successful upload, clears the db
     * 
     * @return True if successful, false otherwise
     */
    private static boolean uploadEvents(Context context, List<Event> events, PolicyEnforcer policyEnforcer) {
        boolean policyEnforced = policyEnforcer.enforePolicy(events);
        boolean uploadSuccess = false;
        if (policyEnforced && events.size() > 0) {
            QuantcastLog.i(TAG, "Attempting to upload events.");

            JsonMap upload = new JsonMap();
            String uploadId = QuantcastServiceUtility.generateUniqueId();
            upload.put(UPLOAD_ID_PARAMETER, new JsonString(uploadId));
            upload.put(PARAMETER_QCV, new JsonString(API_VERSION));
            upload.put(PARAMETER_EVENTS, new JsonArray(events));

            // Track how long upload takes
            long startTime = System.currentTimeMillis();

            // Upload it
            HttpPost request = new HttpPost(UPLOAD_URL);
            final DefaultHttpClient httpClient = new DefaultHttpClient();
            final BasicHttpContext localContext = new BasicHttpContext();

            //            request.addHeader("Content-Type", "application/json");

            String jsonString = upload.toJson();
            try {
                StringEntity entity = new StringEntity(jsonString, HTTP.ASCII);

                //            entity.setContentType("application/json");
                request.setEntity(entity);

                HttpParams params = new BasicHttpParams();          
                params.setBooleanParameter("http.protocol.expect-continue", false);
                request.setParams(params);

                HttpResponse response = httpClient.execute(request, localContext);
                int returnStatus = response.getStatusLine().getStatusCode();
                if (returnStatus == HttpStatus.SC_OK) {
                    long elapsedTime = System.currentTimeMillis() - startTime;

                    QuantcastLog.i(TAG, "Upload successful.");
                    uploadSuccess = true;

                    QuantcastClient.logLatency(new UploadLatency(uploadId, elapsedTime));
                } else {
                    QuantcastLog.w(TAG, "Http upload returned code " + returnStatus);
                }
            }
            catch (UnsupportedEncodingException e) {
                QuantcastLog.e(TAG, "Error creating an entity for upload.", e);
            }
            catch (ClientProtocolException e) {
                QuantcastLog.e(TAG, "Unable to upload events.", e);
            }
            catch (IOException e) {
                QuantcastLog.e(TAG, "Unable to upload events.", e);
            }

        }

        if (!uploadSuccess) {
            QuantcastLog.w(TAG, "Upload failed");
        }

        return uploadSuccess;
    }

}
