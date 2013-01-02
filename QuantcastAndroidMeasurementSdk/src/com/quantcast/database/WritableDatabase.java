package com.quantcast.database;

import android.content.ContentValues;

public interface WritableDatabase extends ReadableDatabase {
    
    public void beginTransaction();
    public void setTransactionSuccessful();
    public void endTransaction();
    
    public long insert(String tableName, ContentValues values);
    public long delete(String tableName);
    
    public void execSQL(String sql);

}
