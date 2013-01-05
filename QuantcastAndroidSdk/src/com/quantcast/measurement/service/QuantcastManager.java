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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;

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
    private static final String API_VERSION = "0_3_1";

    private static final int MAX_UPLOAD_SIZE = 200;
    private static final int MIN_UPLOAD_SIZE = 100;
    private static final int UPLOAD_WAIT_TIME_IN_MS = 30000; // 30 seconds

    private final Context context;
    private final EventsDatabaseHelper databaseHelper;

    private boolean withEvents;

    private final String apiKey;
    private final Object policyEnforcerLock = new Object();
    private volatile PolicyEnforcer policyEnforcer;

    private volatile Boolean uploadQueued = false;
    private volatile long nextTimeUploadAllowed;

    /**
     * Constructor.
     * Must be called from service thread.
     *
     * @param apiKey            The apiKey provided by the developer
     * @param context           A context to use for resources
     */
    QuantcastManager(Context context, String apiKey) {
        context = context.getApplicationContext();
        this.context = context;
        databaseHelper = EventsDatabaseHelper.checkoutDatabaseHelper(context);
        this.apiKey = apiKey;

        withEvents = true;

        QuantcastGlobalControlProvider.getProvider(context).registerListener(this);
    }

    public void uploadEvents(final boolean forceUpload) {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= nextTimeUploadAllowed) {
            boolean uploadNeeded = false;
            if (!uploadQueued) {
                synchronized (uploadQueued) {
                    if (!uploadQueued) {
                        uploadQueued = true;
                        uploadNeeded = true;
                    }
                }
            }

            if (uploadNeeded) {
                nextTimeUploadAllowed = currentTime + UPLOAD_WAIT_TIME_IN_MS;
                QuantcastLog.i(TAG, "Queueing upload.");
                QuantcastGlobalControlProvider.getProvider(context).getControl(new GlobalControlListener() {

                    @Override
                    public void callback(GlobalControl control) {
                        if (!control.blockingEventCollection) {
                            SQLiteDatabase db = databaseHelper.getWritableDatabase();
                            List<DatabaseEvent> eventsToUpload = databaseHelper.getEvents(db, MAX_UPLOAD_SIZE);

                            if (eventsToUpload.size() > MIN_UPLOAD_SIZE || forceUpload) {
                                QuantcastLog.i(TAG, "Uploading " + eventsToUpload.size() + " events.");

                                if (policyEnforcer == null) {
                                    synchronized (policyEnforcerLock) {
                                        if (policyEnforcer == null) {
                                            policyEnforcer = new PolicyEnforcer(context, API_VERSION, apiKey);
                                        }
                                    }
                                }

                                if(uploadEvents(context, eventsToUpload, policyEnforcer)) {
                                    databaseHelper.removeEvents(db, eventsToUpload);
                                }
                            }

                            databaseHelper.close();

                            uploadQueued = false;
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

    public void saveEvents(Collection<Event> events) {
        if (!events.isEmpty()) {
            if (QuantcastGlobalControlProvider.getProvider(context).isDelayed()
                    || !QuantcastGlobalControlProvider.getProvider(context).getControl().blockingEventCollection) {
                QuantcastLog.i(TAG, "Saving " + events.size() + " events.");
                addAsyncParameters(events);
                databaseHelper.writeEvents(events);
            }
        }
    }

    /**
     * Use this method to add any parameters to events that may be too costly to add in the main thread
     * 
     * @param events
     */
    private void addAsyncParameters(Collection<Event> events) {
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
                    databaseHelper.clearEvents();
                    withEvents = false;
                }

            }).start();
        }
    }

    public void destroy() {
        QuantcastLog.i(TAG, "Relinquishing resources.");
        QuantcastGlobalControlProvider.getProvider(context).unregisterListener(this);
        EventsDatabaseHelper.checkinDatabaseHelper(databaseHelper);
    }

    @Override
    public void callback(GlobalControl control) {
        if (control.blockingEventCollection) {
            deleteEvents();
        }
    }

    /**
     * Upload any events in the db.
     * 
     * @return True if successful, false otherwise
     */
    private static boolean uploadEvents(Context context, List<? extends Event> events, PolicyEnforcer policyEnforcer) {
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
