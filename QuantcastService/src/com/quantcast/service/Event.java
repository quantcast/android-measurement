package com.quantcast.service;

import java.util.Map;

import com.quantcast.json.JsonMap;
import com.quantcast.json.JsonString;
import com.quantcast.json.Jsonifiable;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;


/**
 * Base class for Quantcast Events.
 *
 * An Event is an object that records data that the SDK wants to upload.
 *
 * Most parameters are simply stored in a map for genericity in the read/write code.
 * The base class Event has member variables for a couple of specific required parameters that are
 * frequently used, which are the timestamp and sid.
 */
@SuppressWarnings("serial")
class Event extends JsonMap {

    // Base event parameters
    protected static final String SESSION_ID_PARAMETER = "sid";
    protected static final String PARAMETER_ET = "et";
    protected static final String DEVICE_ID_PARAMETER = "did";
    protected static final String EVENT_TYPE_PARAMETER = "event";
    protected static final String LABELS_PARAMETER = "labels";
    protected static final String DEFAULT_MEDIA_PARAMETER = "app";
    
    private final EventType eventType;

    protected Event() {
        super();
        this.eventType = EventType.GENERIC;
    }

    Event(EventType eventType, Session session) {
        super();
        this.eventType = eventType;
        put(PARAMETER_ET, new JsonString(Long.toString(System.currentTimeMillis() / 1000)));
        put(SESSION_ID_PARAMETER, new JsonString(session.getId()));
        put(EVENT_TYPE_PARAMETER, new JsonString(eventType.getParameterValue()));
    }

    Event(EventType eventType, Session session, String labels) {
        this(eventType, session);
        if (labels != null) {
            put(LABELS_PARAMETER, new JsonString( labels));
        }
    }
    
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Write the Event and parameters to the db.
     *
     * There are two tables holding the event data:
     * - An index table EVENTS_TABLE that contains a list of primary keys
     * - A parameter table EVENT_TABLE that encodes each event parameter with three columns:
     * the event id (foreign key to EVENTS_TABLE, although that is not enforced in some Android versions),
     * a parameter name, and a parameter value.
     *
     * @param db        Writable database
     * @param values    ContentValues to use. This is reused for each row.
     */
    final void writeToDatabase(SQLiteDatabase db, ContentValues values) {
        // First write the index to the events table
        values.clear();
        values.put(EventsDatabaseHelper.EVENTS_COLUMN_DOH, false);

        long eventId = db.insert(EventsDatabaseHelper.EVENTS_TABLE, null, values);

        // now write name/values for the event
        writeToDatabase(db, eventId, values);
    }

    /**
     * Write a parameter to the db.
     *
     * @param db
     * @param eventId
     * @param values
     */
    void writeToDatabase(SQLiteDatabase db, long eventId, ContentValues values) {
        for (Map.Entry<String, Jsonifiable> entry : entrySet()) {
            writeParameter(db, eventId, entry.getKey(), entry.getValue().toJson(), values);
        }
    }
    
    final void writeParameter(SQLiteDatabase db, long eventId, String name, String value, ContentValues values) {
        values.clear();
        
        values.put(EventsDatabaseHelper.EVENT_COLUMN_ID, eventId);
        values.put(EventsDatabaseHelper.EVENT_COLUMN_NAME, name);
        values.put(EventsDatabaseHelper.EVENT_COLUMN_VALUE, value);

        db.insert(EventsDatabaseHelper.EVENT_TABLE, null, values);
    }
    
}

