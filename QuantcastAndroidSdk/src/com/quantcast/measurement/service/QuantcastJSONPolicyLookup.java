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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.Uri;

import com.quantcast.policy.JSONPolicyLookup;

class QuantcastJSONPolicyLookup implements JSONPolicyLookup {
    
    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastJSONPolicyLookup.class);
    
    private final String policyRequestURL;
    private final HttpClient httpClient;
    
    public QuantcastJSONPolicyLookup(String apiKey, String apiVersion) {
        policyRequestURL = generatePolicyRequestURL(apiKey, apiVersion);
        httpClient = new DefaultHttpClient();
    }
    
    public QuantcastJSONPolicyLookup(String apiKey, String apiVersion, HttpClient httpClient) {
        policyRequestURL = generatePolicyRequestURL(apiKey, apiVersion);
        this.httpClient = httpClient;
    }

    @Override
    public synchronized String getPolicyJSONString() {
        String policyJSONString = null;
        
        try {
            HttpResponse response = httpClient.execute(new HttpGet(policyRequestURL));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                OutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                policyJSONString = out.toString();
                QuantcastLog.i(TAG, "Recieved the following policy from request\n" + policyRequestURL + "\n" + policyJSONString);
            } else {
                QuantcastLog.e(TAG, "Witnessed an HTTP response of " + statusLine.getStatusCode() + " on request " + policyRequestURL);
            }
        } catch (ClientProtocolException e) {
            QuantcastLog.e(TAG, "Exception witnessed on request " + policyRequestURL, e);
        } catch (IOException e) {
            QuantcastLog.i(TAG, "Exception witnessed on request " + policyRequestURL, e);
        }
        
        return policyJSONString;
    }
    
    private static final String POLICY_REQUEST_BASE = "http://m.quantserve.com/policy.json";
    private static final String POLICY_REQUEST_API_KEY_PARAMETER = "a";
    private static final String POLICY_REQUEST_API_VERSION_PARAMETER = "v";
    private static final String POLICY_REQUEST_DEVICE_TYPE_PARAMTER = "t";
    private static final String POLICY_REQUEST_DEVICE_TYPE = "ANDROID";
    
    public static final String generatePolicyRequestURL(String apiKey, String apiVersion) {
        Uri.Builder builder = Uri.parse(POLICY_REQUEST_BASE).buildUpon();
        builder.appendQueryParameter(POLICY_REQUEST_API_KEY_PARAMETER, apiKey);
        builder.appendQueryParameter(POLICY_REQUEST_API_VERSION_PARAMETER, apiVersion);
        builder.appendQueryParameter(POLICY_REQUEST_DEVICE_TYPE_PARAMTER, POLICY_REQUEST_DEVICE_TYPE);
        return builder.build().toString();
    }

}
