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
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class QCDatabaseDAO extends SQLiteOpenHelper {

    private static final QCLog.Tag TAG = new QCLog.Tag(QCDatabaseDAO.class);

    static final String NAME = "Quantcast.db";
    private static final int VERSION = 2;

    // Table that indexes events, one per event.
    static final String EVENTS_TABLE = "events";
    private static final String EVENTS_COLUMN_ID = "id";            // primary key
    private static final String EVENTS_COLUMN_DOH = "doh";          // tables must have at least one column; this doesn't do anything other than that

    // Table of event parameters
    // Each event has a unique ID, which is the primary key (rowid) in the events table.
    static final String EVENT_PARAMETERS_TABLE = "event";
    private static final String EVENT_PARAMETERS_COLUMN_EVENT_ID = "eventid";
    private static final String EVENT_PARAMETERS_COLUMN_NAME = "name";
    private static final String EVENT_PARAMETERS_COLUMN_VALUE = "value";

    private static final String EVENT_PARAMETERS_EVENT_ID_INDEX_NAME = "event_id_idx";

    private SQLiteDatabase m_openDB;
    private int m_numOpenDBs;

    QCDatabaseDAO(Context context) {
        super(context, NAME, null, VERSION);
        m_numOpenDBs = 0;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();

        try {
            db.execSQL("PRAGMA foreign_keys = ON;");

            db.execSQL("create table " + EVENTS_TABLE + " ("
                    + EVENTS_COLUMN_ID + " integer primary key autoincrement,"
                    + EVENTS_COLUMN_DOH + " integer"
                    + ");");

            db.execSQL("create table " + EVENT_PARAMETERS_TABLE + " ("
                    + EVENT_PARAMETERS_COLUMN_EVENT_ID + " integer,"
                    + EVENT_PARAMETERS_COLUMN_NAME + " varchar not null,"
                    + EVENT_PARAMETERS_COLUMN_VALUE + " varchar not null,"
                    + "FOREIGN KEY(" + EVENT_PARAMETERS_COLUMN_EVENT_ID + ") REFERENCES " + EVENTS_TABLE + "(" + EVENTS_COLUMN_ID + ")"
                    + ");");

            addEventIdIndex(db);

            db.setTransactionSuccessful();
        } catch (SQLException e) {
            QCLog.e(TAG, "Unable to create events related tables", e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        return getWritableDatabase();
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        if (m_openDB == null || !m_openDB.isOpen()) {
            m_numOpenDBs = 0;
            m_openDB = super.getWritableDatabase();
        }
        m_numOpenDBs++;
        return m_openDB;
    }

    @Override
    public void close() {
        m_numOpenDBs--;
        if (m_numOpenDBs == 0) {
            super.close();
            m_openDB = null;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            if (oldVersion <= 1) {
                addEventIdIndex(db);
            }
        } finally {
            db.endTransaction();
        }
    }

    private static void addEventIdIndex(SQLiteDatabase db) {
        String createIndex = "CREATE INDEX " + EVENT_PARAMETERS_EVENT_ID_INDEX_NAME + " ON " + EVENT_PARAMETERS_TABLE + " (" + EVENT_PARAMETERS_COLUMN_EVENT_ID + ")";
        db.execSQL(createIndex);
    }


    synchronized List<QCEvent> getEvents(int maxToRetrieve, QCPolicy policy) {
        SQLiteDatabase db = getReadableDatabase();
        List<QCEvent> retval = getEvents(db, maxToRetrieve, policy);
        close();
        return retval;
    }

    synchronized List<QCEvent> getEvents(SQLiteDatabase db, int maxToRetrieve, QCPolicy policy) {
        ArrayList<QCEvent> events = null;
        if (db != null && db.isOpen()) {
            events = new ArrayList<QCEvent>();

            if (maxToRetrieve > 0) {
                String[] columns = new String[]{EVENTS_COLUMN_ID};

                Cursor cursor = db.query(EVENTS_TABLE, columns, null, null, null, null, EVENTS_COLUMN_ID, Integer.toString(maxToRetrieve));

                if (cursor.moveToFirst()) {
                    do {
                        long eventId = cursor.getLong(0);
                        String[] eventColumns = new String[]{
                                QCDatabaseDAO.EVENT_PARAMETERS_COLUMN_NAME,
                                QCDatabaseDAO.EVENT_PARAMETERS_COLUMN_VALUE
                        };
                        String selection = QCDatabaseDAO.EVENT_PARAMETERS_COLUMN_EVENT_ID + "=?";
                        String[] selectionArgs = new String[]{Long.toString(eventId)};

                        Cursor eventCursor = db.query(QCDatabaseDAO.EVENT_PARAMETERS_TABLE,
                                eventColumns,
                                selection, selectionArgs,
                                null, null, null);

                        Map<String, String> params = new HashMap<String, String>();
                        if (eventCursor.moveToFirst()) {
                            do {
                                params.put(eventCursor.getString(0), eventCursor.getString(1));
                            } while (eventCursor.moveToNext());
                        }

                        eventCursor.close();
                        events.add(QCEvent.dataBaseEventWithPolicyCheck(eventId, params, policy));
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } else {
            QCLog.e(TAG, "Database could not be opened.(1)");
        }
        return events;
    }

    synchronized boolean removeEvents(Collection<QCEvent> events) {
        SQLiteDatabase db = getWritableDatabase();
        boolean success = removeEvents(db, events);
        close();
        return success;
    }

    synchronized boolean removeEvents(SQLiteDatabase db, Collection<QCEvent> events) {
        boolean removed = false;
        if (db != null && db.isOpen()) {
            if (!events.isEmpty()) {
                List<String> eventIds = new ArrayList<String>(events.size());
                for (QCEvent event : events) {
                    eventIds.add(event.getEventId());
                }
                String eventIdsString = TextUtils.join(",", eventIds);

                db.beginTransaction();
                try {
                    db.execSQL(generateDeleteClause(EVENTS_TABLE, EVENTS_COLUMN_ID, eventIdsString));
                    db.execSQL(generateDeleteClause(EVENT_PARAMETERS_TABLE, EVENT_PARAMETERS_COLUMN_EVENT_ID, eventIdsString));
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                    removed = true;
                }
            }
        } else {
            QCLog.e(TAG, "Database could not be opened.(2)");
        }
        return removed;
    }


    private static String generateDeleteClause(String table, String column, String eventIds) {
        return "delete from " + table + " where " + column + " in (" + eventIds + ");";
    }

    synchronized void removeAllEvents() {
        SQLiteDatabase db = getWritableDatabase();
        removeAllEvents(db);
        close();
    }

    synchronized void removeAllEvents(SQLiteDatabase db) {
        if (db != null) {
            try {
                db.beginTransaction();
                // Remove all rows
                db.delete(EVENT_PARAMETERS_TABLE, null, null);
                db.delete(EVENTS_TABLE, null, null);

                db.setTransactionSuccessful();
            } catch (Exception e) {
                QCLog.e(TAG, "Cannot clear events.", e);
            } finally {
                db.endTransaction();
            }
        } else {
            QCLog.e(TAG, "Database could not be opened.(3)");
        }
    }

    synchronized int writeEvents(Collection<QCEvent> events) {
        SQLiteDatabase db = getWritableDatabase();
        int retval = writeEvents(db, events);
        close();
        return retval;
    }

    synchronized int writeEvents(SQLiteDatabase db, Collection<QCEvent> events) {
        int numberWritten = 0;
        if (!events.isEmpty()) {
            if (db != null && db.isOpen()) {
                final SQLiteStatement statement = db.compileStatement("INSERT INTO " + EVENTS_TABLE + " ( " + EVENTS_COLUMN_DOH + " ) VALUES ( ? )");
                final SQLiteStatement paramsStatement = db.compileStatement("INSERT INTO " + EVENT_PARAMETERS_TABLE + " ( "
                        + EVENT_PARAMETERS_COLUMN_EVENT_ID + "," + EVENT_PARAMETERS_COLUMN_NAME + "," + EVENT_PARAMETERS_COLUMN_VALUE
                        + " ) VALUES ( ? , ? , ?)");


                if (statement != null && paramsStatement != null) {
                    db.beginTransaction();
                    try {
                        for (QCEvent event : events) {
                            statement.clearBindings();
                            statement.bindLong(1, 0);
                            // rest of bindings
                            long eventId = statement.executeInsert();
                            if (eventId < 0) {
                                QCLog.e(TAG, "Unable to save " + event + ". See DatabaseUtils logs for a detailed stack trace.");
                            } else {
                                for (Entry<String, String> entry : event.getParameters().entrySet()) {
                                    String name = entry.getKey();
                                    String value = entry.getValue();
                                    paramsStatement.clearBindings();
                                    paramsStatement.bindLong(1, eventId);
                                    paramsStatement.bindString(2, name);
                                    paramsStatement.bindString(3, value);
                                    paramsStatement.execute();
                                }
                                numberWritten++;
                            }
                        }
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                        statement.close();
                        paramsStatement.close();
                    }
                }
            } else {
                QCLog.e(TAG, "Database could not be opened.(4)");
            }
        }
        return numberWritten;
    }

    synchronized void deleteDB(Context context) {
        close();
        context.deleteDatabase(NAME);
    }

    long numberOfEvents() {
        SQLiteDatabase db = getReadableDatabase();
        long retval = rowCountForTable(db, EVENTS_TABLE);
        close();
        return retval;
    }

    long rowCountForTable(SQLiteDatabase db, String table) {
        long retval = 0;
        if (db != null && db.isOpen()) {
            retval = DatabaseUtils.queryNumEntries(db, table);
        } else {
            QCLog.e(TAG, "Database could not be opened.(6)");
        }
        return retval;
    }

}
