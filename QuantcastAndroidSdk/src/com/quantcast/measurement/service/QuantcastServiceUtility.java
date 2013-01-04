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

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

class QuantcastServiceUtility {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastServiceUtility.class);

    private static final long[] HASH_CONSTANTS = { 0x811c9dc5, 0xc9dc5118 };

    private static final String SHARED_PREFERENCES_NAME = "com.quantcast.measurement.service";
    private static final String APPLICATION_ID_PREF_NAME = "applicationId";

    private static Object applicationIdLock = new Object();

    public static String applyHash(String string) {
        double[] hashedStrings = new double[HASH_CONSTANTS.length];
        for (int i = 0; i < hashedStrings.length; i++) {
            hashedStrings[i] = applyHash(HASH_CONSTANTS[i], string);
        }

        double product = 1;
        for (int i = 0; i < hashedStrings.length; i++) {
            product *= hashedStrings[i];
        }

        return Long.toHexString(Math.round(Math.abs(product)/65536d));
    }

    private static long applyHash(long hashConstant, String string) {
        for(int i = 0; i < string.length(); i++){
            int h32 = (int) hashConstant; // javascript only does bit shifting on 32 bits
            h32 ^= string.charAt(i);
            hashConstant = h32;
            hashConstant += (long) (h32 << 1) + (h32 << 4) + (h32 << 7) + (h32 << 8) + (h32 << 24);
        }
        return hashConstant;
    }

    public static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }

    /**
     * This method could result in file I/O and should be considered too heavy for the main thread.
     * 
     * @param context
     * @return
     */
    public static String getApplicationId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String applicationId = sharedPreferences.getString(APPLICATION_ID_PREF_NAME, null);

        if (applicationId == null) {
            applicationId = generateAndSaveApplicationId(sharedPreferences);
        }

        return applicationId;
    }

    private static String generateAndSaveApplicationId(SharedPreferences sharedPreferences) {
        synchronized (applicationIdLock) {
            String applicationId = sharedPreferences.getString(APPLICATION_ID_PREF_NAME, null);
            
            if (applicationId == null) {
                applicationId = generateUniqueId();
                QuantcastLog.i(TAG, "Saving application id:" + applicationId + ".");
                Editor editor = sharedPreferences.edit();
                editor.putString(APPLICATION_ID_PREF_NAME, applicationId);
                editor.commit();
            }
            
            return applicationId;
        }
    }

}
