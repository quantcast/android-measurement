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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import android.content.Context;

import com.quantcast.policy.PolicyJsonCache;

public class PolicyJsonCacheFile implements PolicyJsonCache {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(PolicyJsonCacheFile.class);

    private static final String FILENAME = "com.quantcast.measurement.service.PolicyJsonCacheFile";

    private final Context context;
    private String policyJsonString;
    private File file;
    
    public PolicyJsonCacheFile(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public synchronized String getPolicyJsonString() {
        if (file == null) {
            policyJsonString = getPolicyJsonStringFromFile();
        }

        return policyJsonString;
    }

    private String getPolicyJsonStringFromFile() {
        String jsonStringFromFile = null;

        file = getFile();

        if (file.exists()) {
            try {
                jsonStringFromFile = FileUtils.readFileToString(file);
            }
            catch (IOException e) {
                QuantcastLog.e(TAG, "There was an error reading the policy from the cache file.", e);
            }
        }

        return jsonStringFromFile;
    }
    
    private File getFile() {
        return new File(QuantcastServiceUtility.getBaseDirectory(context), FILENAME);
    }

    @Override
    public synchronized void savePolicyJsonString(String policyJsonString) {
        if (file == null) {
            file = getFile();
        }
        
        try {
            FileUtils.write(file, policyJsonString);
        }
        catch (IOException e) {
            QuantcastLog.e(TAG, "Error writing the policy to the cache file.");
        }
        
        this.policyJsonString = policyJsonString;
    }

}
