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
