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

import android.database.Cursor;

import com.quantcast.database.DatabaseCursor;

class QuantcastDatabaseCursor implements DatabaseCursor {
    
    private Cursor cursor;
    
    private boolean preFirst = true;

    public QuantcastDatabaseCursor(Cursor cursor) {
        this.cursor = cursor;
    }
    
    @Override
    public boolean next() {
        if (preFirst) {
            preFirst = false;
            return cursor.moveToFirst();
        } else {
            return cursor.moveToNext();
        }
    }

    @Override
    public String getString(int index) {
        return cursor.getString(index);
    }

    @Override
    public long getLong(int index) {
        return cursor.getLong(index);
    }

    @Override
    public void close() {
        cursor.close();
    }
    
}
