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
package com.quantcast.measurement.service;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.quantcast.settings.GlobalControl;
import com.quantcast.settings.GlobalControlAmbassador;
import com.quantcast.settings.GlobalControlDAO;

class GlobalControlFileStructure implements GlobalControlDAO, GlobalControlAmbassador {

    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(GlobalControlFileStructure.class);

    private static final String BASE_DIR = GlobalControlFileStructure.class.getName();
    private static final String PRESENCE_ANNOUNCEMENT_FILE_NAME = GlobalControlFileStructure.class.getName() + ".present";
    private static final String OPT_OUT_FILE_NAME = GlobalControl.class.getName() + ".blockEventCollection";

    private final Context context;

    public GlobalControlFileStructure(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public GlobalControl getLocal() {
        return get(context);
    }

    @Override
    public GlobalControl get(Context context) {
        return new GlobalControl(fileExists(context, OPT_OUT_FILE_NAME));
    }

    @Override
    public void saveLocal(GlobalControl control) {
        save(context, control);
    }

    @Override
    public void save(Context context, GlobalControl control) {
        File dir = getBaseDir(context);
        if (testDirIsValid(dir)) {
            File optOutFile = new File(dir, OPT_OUT_FILE_NAME);
            if (!control.blockingEventCollection) {
                optOutFile.delete();
            } else {
                try {
                    optOutFile.createNewFile();
                }
                catch (IOException e) {
                    QuantcastLog.e(TAG, "Unable to create opt-out file (" + optOutFile.getAbsolutePath() +  ".");
                }
            }
        }
    }

    @Override
    public boolean presenceAnnounced() {
        return presenceAnnounced(context);
    }

    @Override
    public void announcePresence() {
        File dir = getBaseDir(context);
        if (testDirIsValid(dir)) {
            File announcementFile = new File(dir, PRESENCE_ANNOUNCEMENT_FILE_NAME);
            try {
                announcementFile.createNewFile();
            }
            catch (IOException e) {
                QuantcastLog.e(TAG, "Unable to create presence file (" + announcementFile.getAbsolutePath() +  ".");
            }
        }
    }

    @Override
    public Context getForeignContext() {
        for (PackageInfo info : context.getPackageManager().getInstalledPackages(0)) {
            if (!info.packageName.equals(context.getPackageName())) {
                try {
                    Context foreignContext = context.createPackageContext(info.packageName, 0);
                    if (presenceAnnounced(foreignContext)) {
                        return foreignContext;
                    }
                }
                catch (NameNotFoundException e) {
                    QuantcastLog.w(TAG, "Unable to create context from package name.", e);
                }

            }
        }

        return null;
    }

    @Override
    public Queue<Context> getForeignContexts() {
        Queue<Context> foreignContexts = new LinkedList<Context>();

        for (PackageInfo info : context.getPackageManager().getInstalledPackages(0)) {
            if (!info.packageName.equals(context.getPackageName())) {
                try {
                    Context foreignContext = context.createPackageContext(info.packageName, 0);
                    if (presenceAnnounced(foreignContext)) {
                        foreignContexts.add(foreignContext);
                    }
                }
                catch (NameNotFoundException e) {
                    QuantcastLog.w(TAG, "Unable to create context from package name.", e);
                }

            }
        }

        return foreignContexts;
    }

    private static boolean presenceAnnounced(Context context) {
        return fileExists(context, PRESENCE_ANNOUNCEMENT_FILE_NAME);
    }

    private static boolean fileExists(Context context, String fileName) {
        boolean fileExists = false;

        File dir = getBaseDir(context);
        if (testValidDirExists(dir)) {
            File file = new File(dir, fileName);
            fileExists = file.exists();
        }

        return fileExists;
    }

    private static File getBaseDir(Context context) {
        return context.getDir(BASE_DIR, Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
    }

    private static boolean testValidDirExists(File dir) {
        boolean valid = true;
        if (dir == null || !dir.exists()) {
            valid = false;
        } else {
            valid = testDirIsValid(dir);
        }
        return valid;
    }

    private static boolean testDirIsValid(File dir) {
        boolean valid = true;
        if (!(dir != null && dir.exists() && dir.isDirectory() && dir.canRead() && dir.canWrite())) {
            valid = false;
            if (dir != null) { 
                QuantcastLog.e(TAG, "The directory (" + dir.getAbsolutePath() + ") cannot be accessed appropriately.");
            } else {
                QuantcastLog.e(TAG, "A null directory has could not be tested.");
            }
        }
        return valid;
    }

}
