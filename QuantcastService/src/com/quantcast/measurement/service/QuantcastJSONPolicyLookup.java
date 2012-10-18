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
    
    public QuantcastJSONPolicyLookup(String pCode, String apiVersion) {
        policyRequestURL = generatePolicyRequestURL(pCode, apiVersion);
        httpClient = new DefaultHttpClient();
    }
    
    public QuantcastJSONPolicyLookup(String pCode, String apiVersion, HttpClient httpClient) {
        policyRequestURL = generatePolicyRequestURL(pCode, apiVersion);
        this.httpClient = httpClient;
    }

    @Override
    public String getPolicyJSONString() {
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
    private static final String POLICY_REQUEST_PCODE_PARAMETER = "a";
    private static final String POLICY_REQUEST_API_VERSION_PARAMETER = "v";
    private static final String POLICY_REQUEST_DEVICE_TYPE_PARAMTER = "t";
    private static final String POLICY_REQUEST_DEVICE_TYPE = "ANDROID";
    
    public static final String generatePolicyRequestURL(String pCode, String apiVersion) {
        Uri.Builder builder = Uri.parse(POLICY_REQUEST_BASE).buildUpon();
        builder.appendQueryParameter(POLICY_REQUEST_PCODE_PARAMETER, pCode);
        builder.appendQueryParameter(POLICY_REQUEST_API_VERSION_PARAMETER, apiVersion);
        builder.appendQueryParameter(POLICY_REQUEST_DEVICE_TYPE_PARAMTER, POLICY_REQUEST_DEVICE_TYPE);
        return builder.build().toString();
    }

}
