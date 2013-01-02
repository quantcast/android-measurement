package com.quantcast.database;


public interface ReadableDatabase {

    public DatabaseCursor query(String tableName, String[] columns);
    public DatabaseCursor query(String tableName, String[] columns, String selection, String[] selectionArgs);

}
