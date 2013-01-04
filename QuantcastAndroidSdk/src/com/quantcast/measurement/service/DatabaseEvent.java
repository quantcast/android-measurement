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

import com.quantcast.json.RawJson;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

@SuppressWarnings("serial")
class DatabaseEvent extends Event {
    
    private final String eventId;
    
    public DatabaseEvent(SQLiteDatabase db, long eventIdNum) {
        super();
        
        this.eventId = Long.toString(eventIdNum);
        
        String [] columns = new String[] {
                EventsDatabaseHelper.EVENT_PARAMETERS_COLUMN_NAME,
                EventsDatabaseHelper.EVENT_PARAMETERS_COLUMN_VALUE
        };
        String selection = EventsDatabaseHelper.EVENT_PARAMETERS_COLUMN_EVENT_ID + "=?";
        String[] selectionArgs = new String[] { eventId };

        Cursor cursor = db.query(EventsDatabaseHelper.EVENT_PARAMETERS_TABLE,
                columns,
                selection, selectionArgs,
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                put(cursor.getString(0), new RawJson(cursor.getString(1)));
            } while (cursor.moveToNext());
        }

        cursor.close();
    }
    
    public String getEventId() {
        return eventId;
    }

}
