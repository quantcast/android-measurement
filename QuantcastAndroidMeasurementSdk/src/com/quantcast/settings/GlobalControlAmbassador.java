package com.quantcast.settings;

import java.util.Queue;

import android.content.Context;

public interface GlobalControlAmbassador {

    public Context getForeignContext();

    public Queue<Context> getForeignContexts();

    public boolean presenceAnnounced();

    public void announcePresence();

}
