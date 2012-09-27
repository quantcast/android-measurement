package com.quantcast.settings;

import android.content.Context;
import android.content.SharedPreferences.Editor;

public interface SettingsEditorRetriever {

    Editor getEditor(Context context);
    
}
