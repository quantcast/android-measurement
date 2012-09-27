package com.quantcast.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

class QuantcastServiceUtility {

    private static final long[] HASH_CONSTANTS = { 0x811c9dc5, 0xc9dc5118 };

    private static final Random random = new Random();
    private static final String UID_DELIMITER = "-";

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
        long randomLong = random.nextLong();
        if (randomLong < 0)
            randomLong = -randomLong;
        return Long.toString(System.currentTimeMillis()) + UID_DELIMITER + Long.toString(randomLong);
    }
    
}
