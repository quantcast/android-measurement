package com.quantcast.settings;


public class GlobalControl {
    
    public static final GlobalControl DEFAULT_CONTROL = new GlobalControl(false);
    
    public final boolean blockingEventCollection;
    
    public GlobalControl(boolean blockingEventCollection) {
        this.blockingEventCollection = blockingEventCollection;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (blockingEventCollection ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GlobalControl)) {
            return false;
        }
        GlobalControl other = (GlobalControl) obj;
        if (blockingEventCollection != other.blockingEventCollection) {
            return false;
        }
        return true;
    }
    
}
