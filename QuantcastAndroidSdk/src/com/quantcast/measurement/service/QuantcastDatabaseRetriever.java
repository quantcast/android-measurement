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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.quantcast.database.DatabaseRetriever;
import com.quantcast.database.ReadableDatabase;
import com.quantcast.database.WritableDatabase;

class QuantcastDatabaseRetriever implements DatabaseRetriever {
    
    static final String DATABASE_NAME = "quantcastService";
    static final int SQLITE_VERSION = 2;
    
    private SQLiteOpenHelper openHelper;
    
    public QuantcastDatabaseRetriever(Context context, final CreationHelper creationHelper) {
        openHelper = new SQLiteOpenHelper(context, DATABASE_NAME, null, SQLITE_VERSION) {

            @Override
            public void onCreate(SQLiteDatabase db) {
                creationHelper.onCreate(new QuantcastWritableDatabase(db));
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                // do nothing
            }
            
        };
    }
    
    public QuantcastDatabaseRetriever(Context context, final CreationHelper creationHelper, final UpgradeHelper upgradeHelper) {
        openHelper = new SQLiteOpenHelper(context, DATABASE_NAME, null, SQLITE_VERSION) {

            @Override
            public void onCreate(SQLiteDatabase db) {
                creationHelper.onCreate(new QuantcastWritableDatabase(db));
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                upgradeHelper.onUpgrade(new QuantcastWritableDatabase(db), oldVersion, newVersion);
            }
            
        };
    }
    
    public static interface CreationHelper {
        public void onCreate(WritableDatabase db);
    }
    
    public static interface UpgradeHelper {
        public void onUpgrade(WritableDatabase db, int oldVersion, int newVersion);
    }

    @Override
    public ReadableDatabase getReadableDatabase() {
        return new QuantcastReadableDatabase(openHelper.getReadableDatabase());
    }

    @Override
    public WritableDatabase getWritableDatabase() {
        return new QuantcastWritableDatabase(openHelper.getWritableDatabase());
    }

    @Override
    public void close() {
        openHelper.close();
    }

    @Override
    public void destroy() {
        openHelper.close();
        openHelper = null;
    }
    
}
