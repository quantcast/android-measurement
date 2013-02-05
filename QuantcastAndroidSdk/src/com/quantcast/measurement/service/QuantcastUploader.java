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

import com.quantcast.json.JsonArray;
import com.quantcast.json.JsonMap;
import com.quantcast.json.JsonString;
import com.quantcast.measurement.event.Event;
import com.quantcast.policy.PolicyEnforcer;

class QuantcastUploader implements Uploader {
    
    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastUploader.class);
    
    private static final String UPLOAD_ID_PARAMETER = "uplid";
    private static final String PARAMETER_QCV = "qcv";
    private static final String PARAMETER_EVENTS = "events";

    private static final String UPLOAD_URL_WITHOUT_SCHEME = "m.quantserve.com/mobile";

    @Override
    public boolean uploadEvents(Context context, List<? extends Event> events, PolicyEnforcer policyEnforcer) {
        boolean policyEnforced = policyEnforcer.enforePolicy(events);
        boolean uploadSuccess = false;
        if (policyEnforced && events.size() > 0) {
            QuantcastLog.i(TAG, "Attempting to upload events.");

            JsonMap upload = new JsonMap();
            String uploadId = QuantcastServiceUtility.generateUniqueId();
            upload.put(UPLOAD_ID_PARAMETER, new JsonString(uploadId));
            upload.put(PARAMETER_QCV, new JsonString(QuantcastServiceUtility.API_VERSION));
            upload.put(PARAMETER_EVENTS, new JsonArray(events));

            // Track how long upload takes
            long startTime = System.currentTimeMillis();

            // Upload it
            HttpPost request = new HttpPost(QuantcastServiceUtility.addScheme(UPLOAD_URL_WITHOUT_SCHEME));
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
