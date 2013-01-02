package com.quantcast.measurement.service;

import java.util.HashSet;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;

import com.quantcast.database.DatabaseCursor;
import com.quantcast.database.DatabaseRetriever;
import com.quantcast.database.ReadableDatabase;
import com.quantcast.database.WritableDatabase;
import com.quantcast.measurement.service.QuantcastDatabaseRetriever.CreationHelper;
import com.quantcast.policy.Policy;
import com.quantcast.policy.PolicyDAO;

class QuantcastPolicyDAO implements PolicyDAO {
    
    private static final QuantcastLog.Tag TAG = new QuantcastLog.Tag(QuantcastPolicyDAO.class);

    private Policy currentPolicy;
    
    private final String apiVersion;

    private DatabaseRetriever databaseRetriever;

    public QuantcastPolicyDAO(Context context, String apiVersion) {
        this.apiVersion = apiVersion;

        databaseRetriever = new QuantcastDatabaseRetriever(context, CREATION_HELPER);
        databaseRetriever.close();
        
        currentPolicy = getCurrentPolicy();
    }

    public QuantcastPolicyDAO(String apiVersion, DatabaseRetriever databaseRetriever) {
        this.apiVersion = apiVersion;
        
        this.databaseRetriever = databaseRetriever;
        CREATION_HELPER.onCreate(databaseRetriever.getWritableDatabase());
        this.databaseRetriever.close();
        
        currentPolicy = getCurrentPolicy();
    }

    public Policy getCurrentPolicy() {
        Policy policy = null;

        ReadableDatabase db = databaseRetriever.getReadableDatabase();
        DatabaseCursor cursor = db.query(POLICIES_TABLE_NAME, new String[] { POLICIES_SALT_COLUMN_NAME, POLICIES_BLACKOUT_COLUMN_NAME },
                POLICIES_API_VERSION_COLUMN_NAME + " = ?", new String[] { apiVersion });

        if (cursor.next()) {
            Set<String> blacklist = getBlacklist(db);
            String salt = cursor.getString(0);
            long blackout = cursor.getLong(1);

            policy = new QuantcastPolicy(blacklist, salt, blackout);
        }
        
        cursor.close();
        databaseRetriever.close();
        QuantcastLog.i(TAG, "Policy retrieved from database:\n" + policy);
        return policy;
    }

    private Set<String> getBlacklist(ReadableDatabase db) {
        Set<String> blacklist = new HashSet<String>();

        DatabaseCursor cursor = db.query(BLACKLIST_TABLE_NAME, new String[] { BLACKLIST_PARAMETERS_COLUMN_NAME });
        while (cursor.next()) {
            blacklist.add(cursor.getString(0));
        }

        cursor.close();
        return blacklist;
    }

    @Override
    public Policy getPolicy() {
        return currentPolicy;
    }

    @Override
    public void savePolicy(Policy policy) {
        if (currentPolicy == null && policy != null || currentPolicy != null && !currentPolicy.equals(policy)) {
            savePolicyToDatabase(policy);
            currentPolicy = policy;
        }
    }

    private void savePolicyToDatabase(Policy policy) {
        QuantcastLog.i(TAG, "Saving policy:\n" + policy);
        
        WritableDatabase db = databaseRetriever.getWritableDatabase();

        deleteAllPolicyData(db);

        if (policy != null) {
            try {
                db.beginTransaction();
                boolean success = true;

                ContentValues values = new ContentValues();
                values.put(POLICIES_API_VERSION_COLUMN_NAME, apiVersion);
                values.put(POLICIES_SALT_COLUMN_NAME, policy.getSalt());
                values.put(POLICIES_BLACKOUT_COLUMN_NAME, policy.getBlackout());

                success = db.insert(POLICIES_TABLE_NAME, values) >= 0;

                if (success) {
                    success = saveBlacklist(db, policy.getBlacklist());
                }

                if (success) {
                    db.setTransactionSuccessful();
                }
            } finally {
                db.endTransaction();
            }
        }
        
        databaseRetriever.close();
    }

    private void deleteAllPolicyData(WritableDatabase db) {
        db.delete(POLICIES_TABLE_NAME);
        db.delete(BLACKLIST_TABLE_NAME);
    }

    /**
     * Only meant to be called from within savePolicyToDatabase
     * 
     * @param db
     * @param blacklist
     * @return
     */
    private boolean saveBlacklist(WritableDatabase db, Set<String> blacklist) {
        boolean success = true;

        for (String parameter : blacklist) {
            if (success) {
                ContentValues values = new ContentValues();
                values.put(BLACKLIST_PARAMETERS_COLUMN_NAME, parameter);

                success = db.insert(BLACKLIST_TABLE_NAME, values) >= 0;
            }
        }

        return success;
    }

    static final String POLICIES_TABLE_NAME = "policies";

    static final String POLICIES_API_VERSION_COLUMN_NAME = "apiVersion";
    static final String POLICIES_SALT_COLUMN_NAME = "salt";
    static final String POLICIES_BLACKOUT_COLUMN_NAME = "blackout";

    static final String CREATE_POLICIES_TABLE =
            "create table " + POLICIES_TABLE_NAME + " ("
                    + POLICIES_API_VERSION_COLUMN_NAME + " text not null unique" +
                    ", " + POLICIES_SALT_COLUMN_NAME + " text not null" +
                    ", " + POLICIES_BLACKOUT_COLUMN_NAME + " integer not null" +
                    ");";

    static final String BLACKLIST_TABLE_NAME = "blacklistEntries";

    static final String BLACKLIST_PARAMETERS_COLUMN_NAME = "parameter";

    static final String CREATE_BLACKLIST_ENTRIES_TABLE =
            "create table " + BLACKLIST_TABLE_NAME + " ("
                    + BLACKLIST_PARAMETERS_COLUMN_NAME + " text not null unique);";

    static final CreationHelper CREATION_HELPER = new CreationHelper() {

        @Override
        public void onCreate(WritableDatabase db) {
            db.beginTransaction();

            try {
                db.execSQL(CREATE_POLICIES_TABLE);
                db.execSQL(CREATE_BLACKLIST_ENTRIES_TABLE);

                db.setTransactionSuccessful();
            } catch (SQLException e) {
                // TODO log this
            } finally {
                db.endTransaction();
            }
        }

    };
}
