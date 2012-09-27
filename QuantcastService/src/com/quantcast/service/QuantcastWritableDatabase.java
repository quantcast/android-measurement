package com.quantcast.service;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.quantcast.database.WritableDatabase;

class QuantcastWritableDatabase extends QuantcastReadableDatabase implements WritableDatabase {
    
    public QuantcastWritableDatabase(SQLiteDatabase sqliteDatabase) {
        super(sqliteDatabase);
        
        if (sqliteDatabase.isReadOnly()) {
            throw new IllegalArgumentException("The provided SQLiteDatabase is read only.");
        }
    }
    
    @Override
    public void beginTransaction() {
        sqliteDatabase.beginTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        sqliteDatabase.setTransactionSuccessful();
    }

    @Override
    public void endTransaction() {
        sqliteDatabase.endTransaction();
    }

    @Override
    public long insert(String tableName, ContentValues values) {
        return sqliteDatabase.insert(tableName, null, values);
    }

    @Override
    public long delete(String tableName) {
        return sqliteDatabase.delete(tableName, null, null);
    }

    @Override
    public void execSQL(String sql) {
        sqliteDatabase.execSQL(sql);
    }
    
}
