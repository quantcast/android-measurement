package com.quantcast.service;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;

import com.quantcast.settings.GlobalControl;
import com.quantcast.settings.GlobalControlAmbassador;
import com.quantcast.settings.GlobalControlDAO;
import com.quantcast.settings.GlobalControlListener;
import com.quantcast.settings.GlobalControlProvider;

class QuantcastGlobalControlProvider implements GlobalControlProvider {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastGlobalControlProvider.class);

    private static QuantcastGlobalControlProvider provider;

    public static GlobalControlProvider getProvider(Context context) {
        if (provider == null) {
            GlobalControlFileStructure globalControlFileStructure = new GlobalControlFileStructure(context);
            provider = new QuantcastGlobalControlProvider(globalControlFileStructure, globalControlFileStructure);
        }

        return provider;
    }

    private final GlobalControlDAO dao;
    private final GlobalControlAmbassador ambassador;
    private GlobalControl control;
    private volatile Boolean delayed = true;

    private Set<GlobalControlListener> listeners;

    public QuantcastGlobalControlProvider(GlobalControlDAO dao, GlobalControlAmbassador ambassador) {
        QuantcastLog.i(TAG, "Initializing new global control provider.");
        this.dao = dao;
        this.ambassador = ambassador;
        listeners = new HashSet<GlobalControlListener>();

        new Thread(new Runnable() {

            @Override
            public void run() {
                obtainInitialControl();
                delayed = false;
                notifyListeners();
                QuantcastLog.i(TAG, "Global control provider initialization complete.");
            }

        }).start();
    }

    private void obtainInitialControl() {
        if (ambassador.presenceAnnounced()) {
            control = dao.getLocal();
        } else {
            Context foreignContext = ambassador.getForeignContext();
            if (foreignContext != null) {
                control = dao.get(foreignContext);
            } else {
                control = GlobalControl.DEFAULT_CONTROL;
            }
            dao.saveLocal(control);
            ambassador.announcePresence();
        }
    }

    @Override
    public void refresh() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                boolean runRefresh = false;

                synchronized (delayed) {
                    if (!delayed) {
                        delayed = true;
                        runRefresh = true;
                    }
                }

                if (runRefresh) {
                    GlobalControl newControl = dao.getLocal();
                    delayed = false;
                    if (!control.equals(newControl)) {
                        control = newControl;
                    }
                    notifyListeners();
                }
            }

        }).start();
    }

    @Override
    public void saveControl(GlobalControl newControl) {
        if (!control.equals(newControl)) {
            control = newControl;

            for (Context foreignContext : ambassador.getForeignContexts()) {
                dao.save(foreignContext, control);
            }
            notifyListeners();
            dao.saveLocal(control);
        }
    }

    @Override
    public GlobalControl getControl() {
        return control;
    }

    @Override
    public void getControl(final GlobalControlListener listener) {
        final GlobalControlProvider globalControlProvider = this;
        if (isDelayed()) {
            this.registerListener(new GlobalControlListener() {

                @Override
                public void callback(GlobalControl control) {
                    listener.callback(control);
                    globalControlProvider.unregisterListener(this);
                }

            });
        } else {
            listener.callback(control);
        }
    }

    @Override
    public void registerListener(GlobalControlListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(GlobalControlListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        synchronized (listeners) {
            for (GlobalControlListener listener : new ArrayList<GlobalControlListener>(listeners)) {
                listener.callback(control);
            }
        }
    }

    @Override
    public boolean isDelayed() {
        synchronized (delayed) {
            return delayed;
        }
    }
}
