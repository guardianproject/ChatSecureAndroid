
package info.guardianproject.otr.app.im.provider;

import info.guardianproject.cacheword.Constants;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * This hook handles the v2 -> v3 migration for SQLCipher databases
 */
public class SQLCipherV3MigrationHook implements SQLiteDatabaseHook {
    private Context mContext;

    public SQLCipherV3MigrationHook(Context context) {
        mContext = context;
    }

    @Override
    public void preKey(SQLiteDatabase database) {
        // nop for now
    }

    @Override
    public void postKey(SQLiteDatabase database) {
        /* V2 - V3 migration */
        if (!isMigratedV3(mContext, database)) {
            database.rawExecSQL("PRAGMA cipher_migrate;");
            setMigratedV3(mContext, database, true);
        }

    }

    public static void setMigratedV3(Context context, SQLiteDatabase database, boolean migrated) {
        SharedPreferences prefs = context.getSharedPreferences(
                Constants.SHARED_PREFS_SQLCIPHER_V3_MIGRATE, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(database.getPath(), migrated).commit();
    }

    public static boolean isMigratedV3(Context context, SQLiteDatabase database) {
        SharedPreferences prefs = context.getSharedPreferences(
                Constants.SHARED_PREFS_SQLCIPHER_V3_MIGRATE, Context.MODE_PRIVATE);
        return prefs.getBoolean(database.getPath(), false);
    }
}
