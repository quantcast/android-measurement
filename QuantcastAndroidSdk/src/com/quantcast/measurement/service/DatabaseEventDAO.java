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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.quantcast.json.Jsonifiable;
import com.quantcast.measurement.event.Event;
import com.quantcast.measurement.event.EventDAO;
import com.quantcast.measurement.event.IdentifiableEvent;

class DatabaseEventDAO extends SQLiteOpenHelper implements EventDAO {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(DatabaseEventDAO.class);

    private static final String NAME = "Quantcast.db";
    private static final int VERSION = 2;

    // Table that indexes events, one per event.
    static final String EVENTS_TABLE = "events";
    static final String EVENTS_COLUMN_ID = "id";            // primary key
    static final String EVENTS_COLUMN_DOH = "doh";          // tables must have at least one column; this doesn't do anything other than that

    // Table of event parameters
    // Each event has a unique ID, which is the primary key (rowid) in the events table.
    static final String EVENT_PARAMETERS_TABLE = "event";
    static final String EVENT_PARAMETERS_COLUMN_EVENT_ID = "eventid";
    static final String EVENT_PARAMETERS_COLUMN_NAME = "name";
    static final String EVENT_PARAMETERS_COLUMN_VALUE = "value";

    static final String EVENT_PARAMETERS_EVENT_ID_INDEX_NAME = "event_id_idx";

    private static final Object DATABASE_EVENT_DAO_LOCK = new Object();
    private static volatile DatabaseEventDAO databaseEventDAO;
    
    public static synchronized DatabaseEventDAO getDatabaseHelper(Context context) {
        if (databaseEventDAO == null) {
            synchronized (DATABASE_EVENT_DAO_LOCK) {
                if (databaseEventDAO == null) {
                    databaseEventDAO = new DatabaseEventDAO(context);
                }
            }
        }
        
        return databaseEventDAO;
    }

    private DatabaseEventDAO(Context context) {
        super(context.getApplicationContext(), NAME, null, VERSION);
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
            QuantcastLog.e(TAG, "Unable to create events related tables", e);
        } finally {
            db.endTransaction();
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

    @Override
    public synchronized List<? extends IdentifiableEvent> getEvents(int maxToRetrieve) {
        SQLiteDatabase db = getReadableDatabase();
        try {
            ArrayList<DatabaseEvent> events = new ArrayList<DatabaseEvent>();

            if (maxToRetrieve > 0) {
                String [] columns = new String[] { EVENTS_COLUMN_ID };

                Cursor cursor = db.query(EVENTS_TABLE, columns, null, null, null, null, EVENTS_COLUMN_ID, Integer.toString(maxToRetrieve));

                if (cursor.moveToFirst()) {
                    do {
                        long eventId = cursor.getLong(0);
                        events.add(new DatabaseEvent(db, eventId));
                    } while (cursor.moveToNext());
                }

                QuantcastLog.i(TAG, "Got " + events.size() + " events from the database");

                cursor.close();
            }

            return events;
        } finally {
            close();
        }
    }

    @Override
    public synchronized void removeEvents(Collection<? extends IdentifiableEvent> events) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            if (!events.isEmpty()) {
                List<String> eventIds = new ArrayList<String>(events.size());
                for (IdentifiableEvent event : events) {
                    eventIds.add(event.getId());
                }
                String eventIdsString = TextUtils.join(",", eventIds);

                db.beginTransaction();
                try {
                    db.execSQL(generateDeleteClause(EVENTS_TABLE, EVENTS_COLUMN_ID, eventIdsString));
                    db.execSQL(generateDeleteClause(EVENT_PARAMETERS_TABLE, EVENT_PARAMETERS_COLUMN_EVENT_ID, eventIdsString));
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        } finally {
            close();
        }
    }

    private static String generateDeleteClause(String table, String column, String eventIds) {
        return "delete from " + table + " where " + column + " in (" + eventIds + ");";
    }

    @Override
    public synchronized void removeAllEvents() {
        SQLiteDatabase db = getWritableDatabase();
        try {
            try {
                // Remove all rows
                db.delete(EVENT_PARAMETERS_TABLE, null, null);
                db.delete(EVENTS_TABLE, null, null);

                db.setTransactionSuccessful();
            } catch (Exception e) {
                QuantcastLog.e(TAG, "Cannot clear events.", e);
            } finally {
                db.endTransaction();
            }
        } finally {
            close();
        }
    }

    @Override
    public synchronized void writeEvents(Collection<? extends Event> events) {
        if (!events.isEmpty()) {
            SQLiteDatabase db = getWritableDatabase();
            try {

                final InsertHelper eventsInsertHelper = new InsertHelper(db, EVENTS_TABLE);
                final int eventsPlaceholerColumn = eventsInsertHelper.getColumnIndex(EVENTS_COLUMN_DOH);

                final InsertHelper eventParametersInsertHelper = new InsertHelper(db, EVENT_PARAMETERS_TABLE);
                final int eventParametersEventIdColumn = eventParametersInsertHelper.getColumnIndex(EVENT_PARAMETERS_COLUMN_EVENT_ID);
                final int eventParametersNameColumn = eventParametersInsertHelper.getColumnIndex(EVENT_PARAMETERS_COLUMN_NAME);
                final int eventParametersValueColumn = eventParametersInsertHelper.getColumnIndex(EVENT_PARAMETERS_COLUMN_VALUE);

                try {
                    db.beginTransaction();

                    boolean success = false;
                    try {
                        for (Event event : events) {
                            eventsInsertHelper.prepareForInsert();
                            eventsInsertHelper.bind(eventsPlaceholerColumn, false);
                            long eventId = eventsInsertHelper.execute();

                            if (eventId < 0) {
                                QuantcastLog.e(TAG, "Unable to save " + event + ". See DatabseUtils logs for a detailed stack trace.");
                            } else {
                                for (Entry<String, Jsonifiable> entry : event.entrySet()) {
                                    String name = entry.getKey();
                                    String value = entry.getValue().toJson();
                                    eventParametersInsertHelper.prepareForInsert();
                                    eventParametersInsertHelper.bind(eventParametersEventIdColumn, eventId);
                                    eventParametersInsertHelper.bind(eventParametersNameColumn, name);
                                    eventParametersInsertHelper.bind(eventParametersValueColumn, value);
                                    if (eventParametersInsertHelper.execute() < 0) {
                                        QuantcastLog.e(TAG, "Unable to save parameter of name \"" + name + "\" with value " + value
                                                + " for event " + event + ". See DatabseUtils logs for a detailed stack trace.");
                                    }
                                }
                            }
                        }
                        success = true;
                    } finally {
                        eventsInsertHelper.close();
                        eventParametersInsertHelper.close();
                    }

                    if (success) {
                        db.setTransactionSuccessful();
                    }
                } finally {
                    db.endTransaction();
                }
            } finally {
                close();
            }
        }
    }
}
