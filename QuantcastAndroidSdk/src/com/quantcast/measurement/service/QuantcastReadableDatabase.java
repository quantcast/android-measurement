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
