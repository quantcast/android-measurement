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
