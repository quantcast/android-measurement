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

// TODO This class could use some refactoring / testing
// Database helper to read/write events, blacklist, and settings
class EventsDatabaseHelper extends SQLiteOpenHelper {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(EventsDatabaseHelper.class);

    private static final String NAME = "Quantcast.db";
    private static final int VERSION = 2;

    // Tables and columns

    // Table that indexes events, one per event.
    static final String EVENTS_TABLE = "events";
    static final String EVENTS_COLUMN_ID = "id";            // primary key
    static final String EVENTS_COLUMN_DOH = "doh";          // tables must have at least one column; this doesn't do anything other than that

    // Table of events, many per event
    // Each event has a unique ID, which is the primary key (rowid) in the events table.
    static final String EVENT_PARAMETERS_TABLE = "event";
    static final String EVENT_PARAMETERS_COLUMN_EVENT_ID = "eventid";
    static final String EVENT_PARAMETERS_COLUMN_NAME = "name";
    static final String EVENT_PARAMETERS_COLUMN_VALUE = "value";

    static final String EVENT_PARAMETERS_EVENT_ID_INDEX_NAME = "event_id_idx";

    private static int helperHolders;
    private int openDatabases;

    private static EventsDatabaseHelper databaseHelper;

    public static synchronized EventsDatabaseHelper checkoutDatabaseHelper(Context context) {
        if (databaseHelper == null) {
            databaseHelper = new EventsDatabaseHelper(context.getApplicationContext());
        }

        helperHolders++;
        return databaseHelper;
    }

    public static synchronized void checkinDatabaseHelper(EventsDatabaseHelper heldDatabaseHelper) {
        if (heldDatabaseHelper == databaseHelper && --helperHolders == 0) {
            databaseHelper = null;
        }
    }

    // Constructor
    private EventsDatabaseHelper(Context context) {
        super(context, NAME, null, VERSION);
    }

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
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

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
     */
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
    public synchronized SQLiteDatabase getReadableDatabase() {
        openDatabases++;
        return super.getReadableDatabase();
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        openDatabases++;
        return super.getWritableDatabase();
    }

    @Override
    public synchronized void close() {
        if (--openDatabases == 0) {
            super.close();
        }
    }

    /**
     * Read all events from db
     *
     * @param db            Readable db
     * @return              List of events
     */
    public synchronized List<DatabaseEvent> getEvents(SQLiteDatabase db, int maxToRead) {
        ArrayList<DatabaseEvent> events = new ArrayList<DatabaseEvent>();

        if (maxToRead > 0) {
            String [] columns = new String[] { EVENTS_COLUMN_ID };

            Cursor cursor = db.query(EVENTS_TABLE, columns, null, null, null, null, EVENTS_COLUMN_ID, Integer.toString(maxToRead));

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
    }

    public synchronized void removeEvents(SQLiteDatabase db, Collection<DatabaseEvent> events) {
        if (!events.isEmpty()) {
            List<String> eventIds = new ArrayList<String>(events.size());
            for (DatabaseEvent event : events) {
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
            }
        }
    }
    
    private static String generateDeleteClause(String table, String column, String eventIds) {
        return "delete from " + table + " where " + column + " in (" + eventIds + ");";
    }

    /**
     * Clear all events from the db
     */
    public void clearEvents() {
        clearEvents(getWritableDatabase());
        close();
    }

    /**
     * Clear all events from the db
     *
     * @param db            Writable db
     */
    public synchronized void clearEvents(SQLiteDatabase db) {
        db.beginTransaction();

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
    }

    /**
     * Write events to the db
     *
     * @param event             Events to write
     */
    public void writeEvents(Collection<Event> events) {
        writeEvents(getWritableDatabase(), events);
        close();
    }

    /**
     * Write events to the db
     *
     * @param db                Writable database to write to
     * @param event             Events to write
     */
    public synchronized void writeEvents(SQLiteDatabase db, Collection<Event> events) {
        if (!events.isEmpty()) {
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
        }
    }
}