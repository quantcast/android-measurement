package com.quantcast.settings;

import android.content.Context;

public interface GlobalControlDAO {
    
    public GlobalControl getLocal();
    
    public GlobalControl get(Context foreignContext);

    public void saveLocal(GlobalControl control);
    
    public void save(Context foreignContext, GlobalControl control);

}
