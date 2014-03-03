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

import android.util.Log;

class QCLog {

    private static int logLevel = Log.ERROR;

    static void setLogLevel(int logLevel) {
        QCLog.logLevel = logLevel;
    }

    public static void i(Tag tag, String message) {
        log(Log.INFO, tag, message);
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
        if (QCLog.logLevel <= logLevel && Log.isLoggable(tag.safeTag, logLevel)) {
            Log.println(logLevel, tag.safeTag, message);
        }
    }

    private static void log(int logLevel, Tag tag, String message, Throwable throwable) {
        if (QCLog.logLevel <= logLevel && Log.isLoggable(tag.safeTag, logLevel)) {
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
