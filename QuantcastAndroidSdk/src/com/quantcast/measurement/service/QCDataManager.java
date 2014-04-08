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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;

import java.util.Arrays;
import java.util.List;

class QCDataManager {

    private static final QCLog.Tag TAG = new QCLog.Tag(QCDataManager.class);

    private static final int MAX_UPLOAD_SIZE = 200;
    private static final int MIN_UPLOAD_SIZE = 3;
    static final int DEFAULT_UPLOAD_EVENT_COUNT = 25;

    private long m_eventCount;
    private final QCDataUploader m_uploader;
    private int m_uploadCount;
    private int m_maxUploadCount;

    private boolean m_isUploading;

    private final QCDatabaseDAO m_database;


    QCDataManager(Context context) {
        m_database = new QCDatabaseDAO(context);
        m_uploader = new QCDataUploader();
        m_uploadCount = DEFAULT_UPLOAD_EVENT_COUNT;
        m_maxUploadCount = MAX_UPLOAD_SIZE;
        m_eventCount = m_database.numberOfEvents();
        m_isUploading = false;
    }

    void postEvent(QCEvent event, QCPolicy policy) {
        //if we are blacked out then we won't save anything
        if (policy != null && policy.isBlackedOut()) return;

        boolean forceUpload = event.shouldForceUpload();
        int written = 0;
        try {
            written = m_database.writeEvents(Arrays.asList(event));
        } catch (SQLiteDatabaseCorruptException dbc) {
            QCLog.e(TAG, "DB Write error", dbc);
            m_database.deleteDB(QCMeasurement.INSTANCE.getAppContext());
        } catch (OutOfMemoryError oom) {
            QCLog.e(TAG, "DB Write error", oom);
            System.gc();
        }
        if (written > 0) {
            m_eventCount += written;
            QCLog.i(TAG, "Successfully wrote " + written + " events! total: " + m_eventCount);
            if (policy != null && QCMeasurement.INSTANCE.isConnected() && (forceUpload || (m_eventCount >= m_uploadCount))) {
                uploadEvents(policy);
            }
        } else {
            QCLog.w(TAG, "DB Write canceled or nothing written");
        }
    }

    void uploadEvents(QCPolicy policy) {
        //if we don't have a policy or are blacked out then we cant send this data
        if (policy.policyIsLoaded() && !policy.isBlackedOut() && !m_isUploading) {
            m_isUploading = true;
            QCLog.i(TAG, "Starting upload...");
            long startTime = System.currentTimeMillis();
            int removed = 0;
            String uploadId = null;
            try {
                SQLiteDatabase db = m_database.getWritableDatabase();
                List<QCEvent> send = m_database.getEvents(db, m_maxUploadCount, policy);
                uploadId = m_uploader.synchronousUploadEvents(send);
                if (uploadId != null) {
                    boolean success = m_database.removeEvents(db, send);
                    if (success) {
                        removed = send.size();
                        QCLog.i(TAG, "Successfully upload " + removed + " events!");
                    } else {
                        QCLog.e(TAG, "Failed to remove " + send.size() + " events");
                    }
                } else {
                    QCLog.e(TAG, "Failed to upload " + send.size() + " events");
                }
            } catch (SQLiteDatabaseCorruptException dbc) {
                m_database.deleteDB(QCMeasurement.INSTANCE.getAppContext());
                QCLog.e(TAG, "DB upload error", dbc);
            } catch (OutOfMemoryError oom) {
                QCLog.e(TAG, "DB upload error", oom);
                System.gc();
            } catch (Throwable t) {
                //cancel this call and move on
                QCLog.e(TAG, "DB upload error", t);
            } finally {
                m_database.close();
            }

            if (removed > 0) {
                m_eventCount = Math.max(0, m_eventCount - removed);
                QCMeasurement.INSTANCE.logLatency(uploadId, System.currentTimeMillis() - startTime);
            } else {
                QCLog.w(TAG, "DB upload canceled or nothing removed");
            }
            m_isUploading = false;
        }
    }

    void setUploadCount(int uploadCount) {
        m_uploadCount = Math.max(MIN_UPLOAD_SIZE, Math.min(m_maxUploadCount, uploadCount));
    }

    void setMaxUploadCount(int maxUploadCount) {
        this.m_maxUploadCount = Math.max(MIN_UPLOAD_SIZE, maxUploadCount);
    }

    long getEventCount() {
        return m_eventCount;
    }

    QCDatabaseDAO getDataBase() {
        return m_database;
    }

}
