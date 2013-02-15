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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

class QuantcastServiceUtility {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastServiceUtility.class);

    public static final String API_VERSION = "0_4_1";

    private static final long[] HASH_CONSTANTS = { 0x811c9dc5, 0xc9dc5118 };

    private static final String SHARED_PREFERENCES_NAME = "com.quantcast.measurement.service";
    private static final String APPLICATION_ID_PREF_NAME = "applicationId";

    private static final Object APPLICATION_ID_LOCK = new Object();

    private static final String HTTP_SCHEME = "http://";
    private static final String HTTPS_SCHEME = "https://";

    private static final char[] HEX_CHARS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    private static final byte HEX_CHAR_OFFSET_IN_BITS = 4;
    private static final byte HEX_CHAR_MASK = 0x0F;

    public static String applyHash(String string) {
        double[] hashedStrings = new double[HASH_CONSTANTS.length];
        for (int i = 0; i < hashedStrings.length; i++) {
            hashedStrings[i] = applyUserHash(HASH_CONSTANTS[i], string);
        }

        double product = 1;
        for (int i = 0; i < hashedStrings.length; i++) {
            product *= hashedStrings[i];
        }

        return Long.toHexString(Math.round(Math.abs(product)/65536d));
    }

    private static long applyUserHash(long hashConstant, String string) {
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
        synchronized (APPLICATION_ID_LOCK) {
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

    public static String addScheme(String schemelessUrl) {
        return (QuantcastClient.isUsingSecureConnections() ? HTTPS_SCHEME : HTTP_SCHEME) + schemelessUrl;
    }
    
    public static String applySha1Hash(String string) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            return QuantcastServiceUtility.toHexString(digest.digest(string.getBytes("iso-8859-1")));
        }
        catch (NoSuchAlgorithmException e) {
            QuantcastLog.e(TAG, "Cannot hash with SHA1 because the algorithm is not supported.");
        }
        catch (UnsupportedEncodingException e) {
            QuantcastLog.e(TAG, "Cannot hash with SHA1 because the encoding is not supported.");
        }
        
        return Long.toHexString(string.hashCode());
    }

    public static String toHexString(byte[] array) {
        StringBuilder stringBuilder = new StringBuilder(array.length);

        for (int i = 0; i < array.length; i++) {
            byte b = array[i];
            stringBuilder.append(HEX_CHARS[(b >>> HEX_CHAR_OFFSET_IN_BITS) & HEX_CHAR_MASK]);
            stringBuilder.append(HEX_CHARS[b & HEX_CHAR_MASK]);
        }
        
        return stringBuilder.toString();
    }

}
