/**
 * © Copyright 2012-2014 Quantcast Corp.
 *
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 * compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License. Unauthorized use of this file constitutes
 * copyright infringement and violation of law.
 */

package com.quantcast.measurement.service;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.UnknownHostException;
import java.util.Collection;

class QCDataUploader {

    private static final QCLog.Tag TAG = new QCLog.Tag(QCDataUploader.class);

    static final String QC_UPLOAD_ID_KEY = "uplid";
    static final String QC_QCV_KEY = "qcv";
    static final String QC_EVENTS_KEY = "events";

    private static final String UPLOAD_URL_WITHOUT_SCHEME = "m.quantcount.com/mobile";

    //this method is synchronous.  Be sure to call from an AsyncTask or other background Thread
    String synchronousUploadEvents(Collection<QCEvent> events) {
        if (events == null || events.isEmpty()) return null;

        String uploadId = QCUtility.generateUniqueId();

        JSONObject upload = new JSONObject();
        try {
            upload.put(QC_UPLOAD_ID_KEY, uploadId);
            upload.put(QC_QCV_KEY, QCUtility.API_VERSION);
            upload.put(QCEvent.QC_APIKEY_KEY, QCMeasurement.INSTANCE.getApiKey());
            upload.put(QCEvent.QC_NETWORKCODE_KEY, QCMeasurement.INSTANCE.getNetworkCode());
            upload.put(QCEvent.QC_DEVICEID_KEY, QCMeasurement.INSTANCE.getDeviceId());
            upload.put(QCEvent.QC_DEVICEOS_KEY, QCEvent.QC_DEVICEOS_VALUE);
            upload.put(QCEvent.QC_PACKAGEID_KEY, QCMeasurement.INSTANCE.getPackageId());

            JSONArray event = new JSONArray();
            for (QCEvent e : events) {
                event.put(new JSONObject(e.getParameters()));
            }
            upload.put(QC_EVENTS_KEY, event);
        } catch (JSONException e) {
            QCLog.e(TAG, "Error while encoding json.");
            return null;
        }

        int code;
        String url = QCUtility.addScheme(UPLOAD_URL_WITHOUT_SCHEME);
        final DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
        defaultHttpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
                System.getProperty("http.agent"));
        final BasicHttpContext localContext = new BasicHttpContext();

        try {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            StringEntity se = new StringEntity(upload.toString(), HTTP.UTF_8);
            post.setEntity(se);

            HttpParams params = new BasicHttpParams();
            params.setBooleanParameter("http.protocol.expect-continue", false);
            post.setParams(params);

            HttpResponse response = defaultHttpClient.execute(post, localContext);
            code = response.getStatusLine().getStatusCode();
        }catch (UnknownHostException uhe) {
            QCLog.e(TAG, "Not connected to Internet", uhe);
            //don't send this error because its ok if they don't have internet connection
            return null;
        } catch (Exception e) {
            QCLog.e(TAG, "Could not upload events", e);
            QCMeasurement.INSTANCE.logSDKError("json-upload-failure", e.toString(), null);
            return null;
        }

        if (!isSuccessful(code)) {
            uploadId = null;
            QCLog.e(TAG, "Events not sent to server. Response code: " + code);
            QCMeasurement.INSTANCE.logSDKError("json-upload-failure", "Bad response from server. Response code: " + code, null);
        }
        return uploadId;
    }

    private boolean isSuccessful(int code) {
        return (code >= 200) && (code <= 299);
    }


}
