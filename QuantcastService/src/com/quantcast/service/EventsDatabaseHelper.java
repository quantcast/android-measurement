package com.quantcast.service;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
    static final String EVENT_TABLE = "event";
    static final String EVENT_COLUMN_ID = "eventid";
    static final String EVENT_COLUMN_NAME = "name";
    static final String EVENT_COLUMN_VALUE = "value";
    
    static final String EVENT_ID_INDEX_NAME = "event_id_idx";

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

            String create;

            create = "create table " + EVENTS_TABLE + " ("
                    + EVENTS_COLUMN_ID + " integer primary key autoincrement,"
                    + EVENTS_COLUMN_DOH + " integer"
                    + ");";

            db.execSQL(create);

            create = "create table " + EVENT_TABLE + " ("
                    + EVENT_COLUMN_ID + " integer,"
                    + EVENT_COLUMN_NAME + " varchar not null,"
                    + EVENT_COLUMN_VALUE + " varchar not null,"
                    + "FOREIGN KEY(" + EVENT_COLUMN_ID + ") REFERENCES " + EVENTS_TABLE + "(" + EVENTS_COLUMN_ID + ")"
                    + ");";

            db.execSQL(create);

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
        String createIndex = "CREATE INDEX " + EVENT_ID_INDEX_NAME + " ON " + EVENT_TABLE + " (" + EVENT_COLUMN_ID + ")";
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
    public static List<Event> getEvents(SQLiteDatabase db, int maxToRead) {
        ArrayList<Event> events = new ArrayList<Event>();

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

    public static void removeEvent(SQLiteDatabase db, Event event) {
        if (event instanceof DatabaseEvent) {
            String eventWhereClause = EVENT_COLUMN_ID + "=?";
            String eventsWhereClause = EVENTS_COLUMN_ID + "=?";
            String[] whereArgs = new String[] { ((DatabaseEvent) event).getEventId() };

            db.beginTransaction();
            try {
                db.delete(EVENT_TABLE, eventWhereClause, whereArgs);
                db.delete(EVENTS_TABLE, eventsWhereClause, whereArgs);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    /**
     * Clear all events from db
     *
     * @param db            Writable db
     */
    public static void clearEvents(SQLiteDatabase db) {
        db.beginTransaction();

        try {

            // Remove all rows
            db.delete(EVENT_TABLE, null, null);
            db.delete(EVENTS_TABLE, null, null);

            db.setTransactionSuccessful();
        } catch (Exception e) {
            QuantcastLog.e(TAG, "Cannot clear events.", e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Write an event to the db
     * This is a wrapper that opens a writable db and catches exceptions
     *
     * @param event             Event to write
     * @return                  True if write succeeded
     */
    static boolean writeEvent(SQLiteDatabase db, Event event) {
        boolean result = false;

        try {
            ContentValues values = new ContentValues();
            event.writeToDatabase(db, values);
            result = true;
        } catch (Exception e) {
            QuantcastLog.e(TAG, "Cannot write event " + event + ".", e);
        }

        return result;
    }
}