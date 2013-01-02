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

    private static Boolean applicationIdSet = false;
    private static String applicationId;

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
        synchronized (applicationIdSet) {
            if (!applicationIdSet) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                applicationId = sharedPreferences.getString(APPLICATION_ID_PREF_NAME, null);

                if (applicationId == null) {
                    applicationId = generateUniqueId();
                    saveApplicationId(applicationId, sharedPreferences);
                }

                applicationIdSet = true;
            }
        }

        return applicationId;
    }

    /**
     * This method is not naturally safe for concurrency.
     * 
     * @param applicationId
     * @param sharedPreferences
     */
    private static void saveApplicationId(String applicationId, SharedPreferences sharedPreferences) {
        QuantcastLog.i(TAG, "Saving application id:" + applicationId + ".");
        Editor editor = sharedPreferences.edit();
        editor.putString(APPLICATION_ID_PREF_NAME, applicationId);
        editor.commit();
    }

}
