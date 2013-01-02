package com.quantcast.measurement.service;

import android.database.sqlite.SQLiteDatabase;

import com.quantcast.database.DatabaseCursor;
import com.quantcast.database.ReadableDatabase;

class QuantcastReadableDatabase implements ReadableDatabase {
    
    protected SQLiteDatabase sqliteDatabase;
    
    public QuantcastReadableDatabase(SQLiteDatabase sqliteDatabase) {
        this.sqliteDatabase = sqliteDatabase;
    }

    public DatabaseCursor query(String tableName, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
        return new QuantcastDatabaseCursor(sqliteDatabase.query(tableName, columns, selection, selectionArgs, groupBy, having, orderBy));
    }

    @Override
    public DatabaseCursor query(String tableName, String[] columns) {
        return query(tableName, columns, null, null, null, null, null);
    }

    @Override
    public DatabaseCursor query(String tableName, String[] columns, String selection, String[] selectionArgs) {
        return query(tableName, columns, selection, selectionArgs, null, null, null);
    }
}
