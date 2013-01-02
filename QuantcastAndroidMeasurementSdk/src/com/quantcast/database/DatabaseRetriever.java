package com.quantcast.database;


public interface DatabaseRetriever {

    public ReadableDatabase getReadableDatabase();
    public WritableDatabase getWritableDatabase();
    public void close();
    public void destroy();
    
}
