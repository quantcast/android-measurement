package com.quantcast.database;

public interface DatabaseCursor {

    public boolean next();
    public String getString(int index);
    public long getLong(int index);
    public void close();

}
