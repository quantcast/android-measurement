package com.quantcast.measurement.service;

import android.util.Log;

public class QuantcastLog {

    private static int logLevel = Log.ERROR;

    static void setLogLevel(int logLevel) {
        QuantcastLog.logLevel = logLevel;
    }

    public static void v(Tag tag, String message) {
        log(Log.VERBOSE, tag, message);
    }

    public static void d(Tag tag, String message) {
        log(Log.DEBUG, tag, message);
    }

    public static void i(Tag tag, String message) {
        log(Log.INFO, tag, message);
    }

    public static void i(Tag tag, String message, Throwable throwable) {
        log(Log.INFO, tag, message, throwable);
    }

    public static void w(Tag tag, String message) {
        log(Log.WARN, tag, message);
    }

    public static void w(Tag tag, String message, Throwable throwable) {
        log(Log.WARN, tag, message, throwable);
    }

    public static void e(Tag tag, String message) {
        log(Log.ERROR, tag, message);
    }

    public static void e(Tag tag, String message, Throwable throwable) {
        log(Log.ERROR, tag, message, throwable);
    }

    private static void log(int logLevel, Tag tag, String message) {
        if (QuantcastLog.logLevel <= logLevel && Log.isLoggable(tag.safeTag, logLevel)) {
            Log.println(logLevel, tag.safeTag, message);
        }
    }

    private static void log(int logLevel, Tag tag, String message, Throwable throwable) {
        if (QuantcastLog.logLevel <= logLevel && Log.isLoggable(tag.safeTag, logLevel)) {
            Log.println(logLevel, tag.safeTag, message + '\n' + Log.getStackTraceString(throwable));
        }
    }
    
    /**
     * For use with Android's obscure logging system. Safely compresses a class into a loggable tag.
     */
    public static class Tag {
        
        public final String safeTag;
        
        public Tag(Class<?> clazz) {
            String classTag = clazz.getSimpleName();
            if (classTag.length() > 21) {
                classTag = classTag.substring(classTag.length() - 21);
            }
            
            safeTag = "q." + classTag;
        }
        
    }

}
