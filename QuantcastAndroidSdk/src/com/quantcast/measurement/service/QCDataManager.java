/*
 * Copyright 2012 Quantcast Corp.
 *
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 * compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.quantcast.measurement.service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.AsyncTask;

import java.util.Arrays;
import java.util.List;

class QCDataManager {

    private static final QCLog.Tag TAG = new QCLog.Tag(QCDataManager.class);

    private static final int MAX_UPLOAD_SIZE = 200;
    private static final int MIN_UPLOAD_SIZE = 2;
    private static final int DEFAULT_UPLOAD_EVENT_COUNT = 25;

    private long m_eventCount;
    private final QCDataUploader m_uploader;
    private int m_uploadCount;
    private int m_maxUploadCount;
    private boolean m_uploading;

    private final QCDatabaseDAO m_database;


    QCDataManager(Context context) {
        m_database = new QCDatabaseDAO(context);
        m_uploader = new QCDataUploader();
        m_uploadCount = DEFAULT_UPLOAD_EVENT_COUNT;
        m_maxUploadCount = MAX_UPLOAD_SIZE;
        m_eventCount = m_database.numberOfEvents();
        m_uploading = false;
    }

    void postEvent(QCEvent event, QCPolicy policy) {
        //if we are blacked out then we won't save anything
        if (policy.isBlackedOut()) return;
        AsyncTask<QCEvent, Void, Integer> task = newDBTask(policy);
        task.execute(event);
    }

    void uploadEvents(QCPolicy policy) {

        //if we don't have a policy or are blacked out then we cant send this data
        if (policy.policyIsLoaded() && !policy.isBlackedOut()) {
            AsyncTask<Void, Void, Integer> uploadTask = newUploadTask(policy);
            uploadTask.execute();
        }
    }

    void setUploadCount(int uploadCount) {
        m_uploadCount = Math.max(MIN_UPLOAD_SIZE, Math.min(m_maxUploadCount, uploadCount));
    }

    void setMaxUploadCount(int maxUploadCount) {
        this.m_maxUploadCount = Math.max(MIN_UPLOAD_SIZE, maxUploadCount);
    }

    AsyncTask<QCEvent, Void, Integer> newDBTask(final QCPolicy policy) {
        return new AsyncTask<QCEvent, Void, Integer>() {
            private boolean forceUpload;

            @Override
            protected Integer doInBackground(QCEvent... qcEvents) {
                forceUpload = qcEvents[0].shouldForceUpload();
                int written = 0;
                try {
                    written = m_database.writeEvents(Arrays.asList(qcEvents));
                } catch (SQLiteDatabaseCorruptException dbc) {
                    QCLog.e(TAG, "DB Write error", dbc);
                    m_database.deleteDB(QCMeasurement.INSTANCE.getAppContext());
                    this.cancel(true);
                } catch (OutOfMemoryError oom) {
                    QCLog.e(TAG, "DB Write error", oom);
                    System.gc();
                    this.cancel(true);
                } catch (Throwable t) {
                    QCLog.e(TAG, "DB Write error", t);
                    //cancel this call and move on
                    this.cancel(true);
                }
                return written;
            }

            @Override
            protected void onPostExecute(Integer written) {
                if (!this.isCancelled() && written > 0) {
                    m_eventCount += written;
                    QCLog.i(TAG, "Successfully wrote " + written + " events! total: " + m_eventCount);
                    if (policy != null && (forceUpload || (m_eventCount >= m_uploadCount && !m_uploading))) {
                        uploadEvents(policy);
                    }
                }else{
                    QCLog.w(TAG, "DB Write canceled or nothing written");
                }
            }
        };
    }

    AsyncTask<Void, Void, Integer> newUploadTask(final QCPolicy policy) {
        return new AsyncTask<Void, Void, Integer>() {
            private String uploadId;
            private long startTime;

            @Override
            protected void onPreExecute() {
                m_uploading = true;
                startTime = System.currentTimeMillis();
            }

            @Override
            protected Integer doInBackground(Void... voids) {
                QCLog.i(TAG, "Starting upload...");
                int removed = 0;
                synchronized (this) {
                    try {
                            SQLiteDatabase db = m_database.getWritableDatabase();
                            List<QCEvent> send = m_database.getEvents(db, m_maxUploadCount, policy);
                            uploadId = m_uploader.synchronousUploadEvents(send);
                            if (uploadId != null) {
                                boolean success = m_database.removeEvents(db, send);
                                if(success){
                                    removed = send.size();
                                    QCLog.i(TAG, "Successfully upload " + removed + " events!");
                                }else{
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
                }
                return removed;
            }

            @Override
            protected void onPostExecute(Integer removed) {
                if (!this.isCancelled() && removed > 0) {
                    m_eventCount = Math.max(0, m_eventCount - removed);
                    QCMeasurement.INSTANCE.logLatency(uploadId, System.currentTimeMillis() - startTime);
                }else{
                    QCLog.w(TAG, "DB upload canceled or nothing removed");
                }
                m_uploading = false;
            }
        };
    }

    long getEventCount() {
        return m_eventCount;
    }

    QCDatabaseDAO getDataBase() {
        return m_database;
    }


}
