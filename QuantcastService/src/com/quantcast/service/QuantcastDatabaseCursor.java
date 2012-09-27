package com.quantcast.service;

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
