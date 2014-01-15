/*
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package info.guardianproject.otr.app.im.provider;

import info.guardianproject.otr.OtrAndroidKeyManagerImpl;
import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.util.Debug;
import info.guardianproject.util.LogCleaner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import net.sqlcipher.database.SQLiteConstraintException;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import net.sqlcipher.database.SQLiteQueryBuilder;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

/** A content provider for IM */
public class ImpsProvider extends ContentProvider {
    private static final String PREV_DATABASE_OPEN_TRAIL_TAG = "prev_database_open";
    private static final String EMPTY_KEY_TRAIL_TAG = "empty_key";
    private static final String DATABASE_OPEN_TRAIL_TAG = "database_open";
    private static final String LOG_TAG = "imProvider";
    private static final boolean DBG = false;

    private static final String AUTHORITY = "info.guardianproject.otr.app.im.provider.Imps";

    private static final String TABLE_ACCOUNTS = "accounts";
    private static final String TABLE_PROVIDERS = "providers";
    private static final String TABLE_PROVIDER_SETTINGS = "providerSettings";

    private static final String TABLE_CONTACTS = "contacts";
    private static final String TABLE_CONTACTS_ETAG = "contactsEtag";
    private static final String TABLE_BLOCKED_LIST = "blockedList";
    private static final String TABLE_CONTACT_LIST = "contactList";
    private static final String TABLE_INVITATIONS = "invitations";
    private static final String TABLE_GROUP_MEMBERS = "groupMembers";
    private static final String TABLE_PRESENCE = "presence";
    private static final String USERNAME = "username";
    private static final String TABLE_CHATS = "chats";
    private static final String TABLE_AVATARS = "avatars";
    private static final String TABLE_SESSION_COOKIES = "sessionCookies";
    private static final String TABLE_MESSAGES = "messages";
    private static final String TABLE_IN_MEMORY_MESSAGES = "inMemoryMessages";
    private static final String TABLE_ACCOUNT_STATUS = "accountStatus";
    private static final String TABLE_BRANDING_RESOURCE_MAP_CACHE = "brandingResMapCache";

    // tables for mcs and rmq
    private static final String TABLE_OUTGOING_RMQ_MESSAGES = "outgoingRmqMessages";
    private static final String TABLE_LAST_RMQ_ID = "lastrmqid";
    private static final String TABLE_S2D_RMQ_IDS = "s2dRmqIds";

    private static final String ENCRYPTED_DATABASE_NAME = "impsenc.db";
    private static final String UNENCRYPTED_DATABASE_NAME = "imps.db";

    private static final int DATABASE_VERSION = 103;

    protected static final int MATCH_PROVIDERS = 1;
    protected static final int MATCH_PROVIDERS_BY_ID = 2;
    protected static final int MATCH_PROVIDERS_WITH_ACCOUNT = 3;
    protected static final int MATCH_ACCOUNTS = 10;
    protected static final int MATCH_ACCOUNTS_BY_ID = 11;
    protected static final int MATCH_ACCOUNTS_WITH_DOMAIN = 12;
    protected static final int MATCH_CONTACTS = 18;
    protected static final int MATCH_CONTACTS_JOIN_PRESENCE = 19;
    protected static final int MATCH_CONTACTS_BAREBONE = 20;
    protected static final int MATCH_CHATTING_CONTACTS = 21;
    protected static final int MATCH_CONTACTS_BY_PROVIDER = 22;
    protected static final int MATCH_CHATTING_CONTACTS_BY_PROVIDER = 23;
    protected static final int MATCH_NO_CHATTING_CONTACTS_BY_PROVIDER = 24;
    protected static final int MATCH_ONLINE_CONTACTS_BY_PROVIDER = 25;
    protected static final int MATCH_OFFLINE_CONTACTS_BY_PROVIDER = 26;
    protected static final int MATCH_CONTACT = 27;
    protected static final int MATCH_CONTACTS_BULK = 28;
    protected static final int MATCH_ONLINE_CONTACT_COUNT = 30;
    protected static final int MATCH_BLOCKED_CONTACTS = 31;
    protected static final int MATCH_CONTACTLISTS = 32;
    protected static final int MATCH_CONTACTLISTS_BY_PROVIDER = 33;
    protected static final int MATCH_CONTACTLIST = 34;
    protected static final int MATCH_BLOCKEDLIST = 35;
    protected static final int MATCH_BLOCKEDLIST_BY_PROVIDER = 36;
    protected static final int MATCH_CONTACTS_ETAGS = 37;
    protected static final int MATCH_CONTACTS_ETAG = 38;
    protected static final int MATCH_PRESENCE = 40;
    protected static final int MATCH_PRESENCE_ID = 41;
    protected static final int MATCH_PRESENCE_BY_ACCOUNT = 42;
    protected static final int MATCH_PRESENCE_SEED_BY_ACCOUNT = 43;
    protected static final int MATCH_PRESENCE_BULK = 44;

    protected static final int MATCH_MESSAGES = 50;
    protected static final int MATCH_MESSAGES_BY_CONTACT = 51;
    protected static final int MATCH_MESSAGES_BY_THREAD_ID = 52;
    protected static final int MATCH_MESSAGES_BY_PROVIDER = 53;
    protected static final int MATCH_MESSAGES_BY_ACCOUNT = 54;
    protected static final int MATCH_MESSAGE = 55;
    protected static final int MATCH_OTR_MESSAGES = 56;
    protected static final int MATCH_OTR_MESSAGES_BY_CONTACT = 57;
    protected static final int MATCH_OTR_MESSAGES_BY_THREAD_ID = 58;
    protected static final int MATCH_OTR_MESSAGES_BY_PROVIDER = 59;
    protected static final int MATCH_OTR_MESSAGES_BY_ACCOUNT = 60;
    protected static final int MATCH_OTR_MESSAGE = 61;
    protected static final int MATCH_OTR_MESSAGES_BY_PACKET_ID = 62;

    protected static final int MATCH_GROUP_MEMBERS = 65;
    protected static final int MATCH_GROUP_MEMBERS_BY_GROUP = 66;
    protected static final int MATCH_AVATARS = 70;
    protected static final int MATCH_AVATAR = 71;
    protected static final int MATCH_AVATAR_BY_PROVIDER = 72;
    protected static final int MATCH_CHATS = 80;
    protected static final int MATCH_CHATS_BY_ACCOUNT = 81;
    protected static final int MATCH_CHATS_ID = 82;
    protected static final int MATCH_SESSIONS = 83;
    protected static final int MATCH_SESSIONS_BY_PROVIDER = 84;
    protected static final int MATCH_PROVIDER_SETTINGS = 90;
    protected static final int MATCH_PROVIDER_SETTINGS_BY_ID = 91;
    protected static final int MATCH_PROVIDER_SETTINGS_BY_ID_AND_NAME = 92;
    protected static final int MATCH_INVITATIONS = 100;
    protected static final int MATCH_INVITATION = 101;
    protected static final int MATCH_ACCOUNTS_STATUS = 104;
    protected static final int MATCH_ACCOUNT_STATUS = 105;
    protected static final int MATCH_BRANDING_RESOURCE_MAP_CACHE = 106;

    // mcs url matcher
    protected static final int MATCH_OUTGOING_RMQ_MESSAGES = 200;
    protected static final int MATCH_OUTGOING_RMQ_MESSAGE = 201;
    protected static final int MATCH_OUTGOING_HIGHEST_RMQ_ID = 202;
    protected static final int MATCH_LAST_RMQ_ID = 203;
    protected static final int MATCH_S2D_RMQ_IDS = 204;

    protected final UriMatcher mUrlMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private String mTransientDbName;

    private static final HashMap<String, String> sProviderAccountsProjectionMap;
    private static final HashMap<String, String> sAccountsByDomainProjectionMap;
    private static final HashMap<String, String> sContactsProjectionMap;
    private static final HashMap<String, String> sContactListProjectionMap;
    private static final HashMap<String, String> sBlockedListProjectionMap;
    private static final HashMap<String, String> sMessagesProjectionMap;
    private static final HashMap<String, String> sInMemoryMessagesProjectionMap;

    private static final String PROVIDER_JOIN_ACCOUNT_TABLE = "providers LEFT OUTER JOIN accounts ON "
                                                              + "(providers._id = accounts.provider AND accounts.active = 1) "
                                                              + "LEFT OUTER JOIN accountStatus ON (accounts._id = accountStatus.account)";

    private static final String DOMAIN_JOIN_ACCOUNT_TABLE = "providerSettings JOIN accounts ON "
            + "(providerSettings.provider = accounts.provider AND providerSettings.name = '" + Imps.ProviderSettings.DOMAIN + "' AND accounts.active = 1) "
            + "LEFT OUTER JOIN accountStatus ON (accounts._id = accountStatus.account)";


    private static final String CONTACT_JOIN_PRESENCE_TABLE = "contacts LEFT OUTER JOIN presence ON (contacts._id = presence.contact_id)";

    private static final String CONTACT_JOIN_PRESENCE_CHAT_TABLE = CONTACT_JOIN_PRESENCE_TABLE
                                                                   + " LEFT OUTER JOIN chats ON (contacts._id = chats.contact_id)";

    private static final String CONTACT_JOIN_PRESENCE_CHAT_AVATAR_TABLE = CONTACT_JOIN_PRESENCE_CHAT_TABLE
                                                                          + " LEFT OUTER JOIN avatars ON (contacts.username = avatars.contact"
                                                                          + " AND contacts.account = avatars.account_id)";

    private static final String BLOCKEDLIST_JOIN_AVATAR_TABLE = "blockedList LEFT OUTER JOIN avatars ON (blockedList.username = avatars.contact"
                                                                + " AND blockedList.account = avatars.account_id)";

    private static final String MESSAGE_JOIN_CONTACT_TABLE = "messages LEFT OUTER JOIN contacts ON (contacts._id = messages.thread_id)";

    private static final String IN_MEMORY_MESSAGES_JOIN_CONTACT_TABLE = "inMemoryMessages LEFT OUTER JOIN contacts ON "
                                                                        + "(contacts._id = inMemoryMessages.thread_id)";

    /** The where clause for filtering out blocked contacts */
    private static final String NON_BLOCKED_CONTACTS_WHERE_CLAUSE = "("
                                                                    + Imps.Contacts.TYPE
                                                                    + " IS NULL OR "
                                                                    + Imps.Contacts.TYPE
                                                                    + "!="
                                                                    + String.valueOf(Imps.Contacts.TYPE_BLOCKED)
                                                                    + ")";

    private static final String BLOCKED_CONTACTS_WHERE_CLAUSE = "(contacts." + Imps.Contacts.TYPE
                                                                + "=" + Imps.Contacts.TYPE_BLOCKED
                                                                + ")";

    private static final String CONTACT_ID = TABLE_CONTACTS + '.' + Imps.Contacts._ID;
    private static final String PRESENCE_CONTACT_ID = TABLE_PRESENCE + '.'
                                                      + Imps.Presence.CONTACT_ID;

    protected static DatabaseHelper mDbHelper;
    private String mDatabaseName;
    private final int mDatabaseVersion;
    

    private final String[] BACKFILL_PROJECTION = { Imps.Chats._ID, Imps.Chats.SHORTCUT,
                                                  Imps.Chats.LAST_MESSAGE_DATE };

    private final String[] FIND_SHORTCUT_PROJECTION = { Imps.Chats._ID, Imps.Chats.SHORTCUT };

    // contact id query projection
    private static final String[] CONTACT_ID_PROJECTION = new String[] { Imps.Contacts._ID, // 0
    };
    private static final int CONTACT_ID_COLUMN = 0;

    // contact id query selection for "seed presence" operation
    private static final String CONTACTS_WITH_NO_PRESENCE_SELECTION = Imps.Contacts.ACCOUNT + "=?"
                                                                      + " AND " + Imps.Contacts._ID
                                                                      + " in (select " + CONTACT_ID
                                                                      + " from " + TABLE_CONTACTS
                                                                      + " left outer join "
                                                                      + TABLE_PRESENCE + " on "
                                                                      + CONTACT_ID + '='
                                                                      + PRESENCE_CONTACT_ID
                                                                      + " where "
                                                                      + PRESENCE_CONTACT_ID
                                                                      + " IS NULL)";

    // contact id query selection args 1
    private String[] mQueryContactIdSelectionArgs1 = new String[1];

    // contact id query selection for getContactId()
    private static final String CONTACT_ID_QUERY_SELECTION = Imps.Contacts.ACCOUNT + "=? AND "
                                                             + Imps.Contacts.USERNAME + "=?";

    // contact id query selection args 2
    private String[] mQueryContactIdSelectionArgs2 = new String[2];
    

    private class DatabaseHelper extends SQLiteOpenHelper {

        private SQLiteDatabase dbRead;
        private SQLiteDatabase dbWrite;

        boolean mInMemoryDB = false;
        String mKey = null;
        
        DatabaseHelper(Context context, String key, boolean inMemoryDb) throws Exception {
            super(context, mDatabaseName, null, mDatabaseVersion);
            
            mInMemoryDB = inMemoryDb;
            mKey = key;
        }

        public SQLiteDatabase getReadableDatabase() {
            if (dbRead == null)
                dbRead = super.getReadableDatabase(mKey);

            return dbRead;
        }

        public SQLiteDatabase getWritableDatabase() {
            if (dbWrite == null)
                dbWrite = super.getWritableDatabase(mKey);

            return dbWrite;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            if (DBG)
                log("DatabaseHelper.onCreate");

            db.execSQL("CREATE TABLE " + TABLE_PROVIDERS + " (" + "_id INTEGER PRIMARY KEY,"
                       + "name TEXT," + // eg AIM
                       "fullname TEXT," + // eg AOL Instance Messenger
                       "category TEXT," + // a category used for forming intent
                       "signup_url TEXT" + // web url to visit to create a new account
                       ");");

            db.execSQL("CREATE TABLE " + TABLE_ACCOUNTS + " (" + "_id INTEGER PRIMARY KEY,"
                       + "name TEXT," + "provider INTEGER," + "username TEXT," + "pw TEXT,"
                       + "active INTEGER NOT NULL DEFAULT 0,"
                       + "locked INTEGER NOT NULL DEFAULT 0,"
                       + "keep_signed_in INTEGER NOT NULL DEFAULT 0,"
                       + "last_login_state INTEGER NOT NULL DEFAULT 0,"
                       + "UNIQUE (provider, username)" + ");");

            createContactsTables(db);
            createMessageChatTables(db, true /* create show_ts column */);

            db.execSQL("CREATE TABLE " + TABLE_AVATARS + " (" + "_id INTEGER PRIMARY KEY,"
                       + "contact TEXT," + "provider_id INTEGER," + "account_id INTEGER,"
                       + "hash TEXT," + "data BLOB," + // raw image data
                       "UNIQUE (account_id, contact)" + ");");

            db.execSQL("CREATE TABLE " + TABLE_PROVIDER_SETTINGS + " ("
                       + "_id INTEGER PRIMARY KEY," + "provider INTEGER," + "name TEXT,"
                       + "value TEXT," + "UNIQUE (provider, name)" + ");");

            db.execSQL("create TABLE " + TABLE_BRANDING_RESOURCE_MAP_CACHE + " ("
                       + "_id INTEGER PRIMARY KEY," + "provider_id INTEGER,"
                       + "app_res_id INTEGER," + "plugin_res_id INTEGER" + ");");

            // clean up account specific data when an account is deleted.
            db.execSQL("CREATE TRIGGER account_cleanup " + "DELETE ON " + TABLE_ACCOUNTS
                       + " BEGIN " + "DELETE FROM " + TABLE_AVATARS + " WHERE account_id= OLD._id;"
                       + "END");

            // add a database trigger to clean up associated provider settings
            // while deleting a provider
            db.execSQL("CREATE TRIGGER provider_cleanup " + "DELETE ON " + TABLE_PROVIDERS
                       + " BEGIN " + "DELETE FROM " + TABLE_PROVIDER_SETTINGS
                       + " WHERE provider= OLD._id;" + "END");

            // the following are tables for mcs
            db.execSQL("create TABLE " + TABLE_OUTGOING_RMQ_MESSAGES + " ("
                       + "_id INTEGER PRIMARY KEY," + "rmq_id INTEGER," + "type INTEGER,"
                       + "ts INTEGER," + "data TEXT" + ");");

            db.execSQL("create TABLE " + TABLE_LAST_RMQ_ID + " (" + "_id INTEGER PRIMARY KEY,"
                       + "rmq_id INTEGER" + ");");

            db.execSQL("create TABLE " + TABLE_S2D_RMQ_IDS + " (" + "_id INTEGER PRIMARY KEY,"
                       + "rmq_id INTEGER" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            LogCleaner.debug(LOG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

            switch (oldVersion) {
            case 43: // this is the db version shipped in Dream 1.0
                // no-op: no schema changed from 43 to 44. The db version was changed to flush
                // old provider settings, so new provider setting (including new name/value
                // pairs) could be inserted by the plugins.

                // follow thru.
            case 44:
                if (newVersion <= 44) {
                    return;
                }

                db.beginTransaction();
                try {
                    // add category column to the providers table
                    db.execSQL("ALTER TABLE " + TABLE_PROVIDERS + " ADD COLUMN category TEXT;");
                    // add otr column to the contacts table
                    db.execSQL("ALTER TABLE " + TABLE_CONTACTS + " ADD COLUMN otr INTEGER;");

                    db.setTransactionSuccessful();
                } catch (Throwable ex) {
                    LogCleaner.error(LOG_TAG, ex.getMessage(), ex);
                    break; // force to destroy all old data;
                } finally {
                    db.endTransaction();
                }

            case 45:
                if (newVersion <= 45) {
                    return;
                }

                db.beginTransaction();
                try {
                    // add an otr_etag column to contact etag table
                    db.execSQL("ALTER TABLE " + TABLE_CONTACTS_ETAG + " ADD COLUMN otr_etag TEXT;");
                    db.setTransactionSuccessful();
                } catch (Throwable ex) {
                    LogCleaner.error(LOG_TAG, ex.getMessage(), ex);
                    break; // force to destroy all old data;
                } finally {
                    db.endTransaction();
                }

            case 46:
                if (newVersion <= 46) {
                    return;
                }

                db.beginTransaction();
                try {
                    // add branding resource map cache table
                    db.execSQL("create TABLE " + TABLE_BRANDING_RESOURCE_MAP_CACHE + " ("
                               + "_id INTEGER PRIMARY KEY," + "provider_id INTEGER,"
                               + "app_res_id INTEGER," + "plugin_res_id INTEGER" + ");");
                    db.setTransactionSuccessful();
                } catch (Throwable ex) {
                    LogCleaner.error(LOG_TAG, ex.getMessage(), ex);
                    break; // force to destroy all old data;
                } finally {
                    db.endTransaction();
                }

            case 47:
                if (newVersion <= 47) {
                    return;
                }

                db.beginTransaction();
                try {
                    // when upgrading from version 47, don't create the show_ts column
                    // here. The upgrade step in 51 will add the show_ts column to the
                    // messages table. If we created the messages table with show_ts here,
                    // we'll get a duplicate column error later.
                    createMessageChatTables(db, false /* don't create show_ts column */);
                    db.setTransactionSuccessful();
                } catch (Throwable ex) {
                    LogCleaner.error(LOG_TAG, ex.getMessage(), ex);
                    break; // force to destroy all old data;
                } finally {
                    db.endTransaction();
                }

                // fall thru.

            case 48:
            case 49:
            case 50:
                if (newVersion <= 50) {
                    return;
                }

                db.beginTransaction();
                try {
                    // add rmq2 s2d ids table
                    db.execSQL("create TABLE " + TABLE_S2D_RMQ_IDS + " ("
                               + "_id INTEGER PRIMARY KEY," + "rmq_id INTEGER" + ");");
                    db.setTransactionSuccessful();
                } catch (Throwable ex) {
                    LogCleaner.error(LOG_TAG, ex.getMessage(), ex);
                    break; // force to destroy all old data;
                } finally {
                    db.endTransaction();
                }

            case 51:
                if (newVersion <= 51) {
                    return;
                }

                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN show_ts INTEGER;");
                    db.setTransactionSuccessful();
                } catch (Throwable ex) {
                    LogCleaner.error(LOG_TAG, ex.getMessage(), ex);
                    break; // force to destroy all old data;
                } finally {
                    db.endTransaction();
                }

                return;
            case 101:
                // This was a no-op upgrade when we added the encrypted DB option
                return;
            case 102:
                if (newVersion <= 102) {
                    return;
                }
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + TABLE_MESSAGES
                               + " ADD COLUMN mime_type TEXT;");
                    if (!mInMemoryDB) {
                        db.execSQL("ALTER TABLE " + TABLE_IN_MEMORY_MESSAGES
                                + " ADD COLUMN mime_type TEXT;");
                    }
                    db.setTransactionSuccessful();
                } catch (Throwable ex) {
                    LogCleaner.error(LOG_TAG, ex.getMessage(), ex);
                } finally {
                    db.endTransaction();
                }

                return;
            case 1:
                if (newVersion <= 100) {
                    return;
                }

                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + TABLE_MESSAGES
                               + " ADD COLUMN is_delivered INTEGER;");
                    db.setTransactionSuccessful();
                } catch (Throwable ex) {
                    LogCleaner.error(LOG_TAG, ex.getMessage(), ex);
                } finally {
                    db.endTransaction();
                }

                return;
            }

            LogCleaner.warn(LOG_TAG, "Couldn't upgrade db to " + newVersion + ". Destroying old data.");
            destroyOldTables(db);
            onCreate(db);
        }

        private void destroyOldTables(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROVIDERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACT_LIST);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLOCKED_LIST);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS_ETAG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AVATARS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROVIDER_SETTINGS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BRANDING_RESOURCE_MAP_CACHE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHATS);

            // mcs/rmq stuff
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_OUTGOING_RMQ_MESSAGES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LAST_RMQ_ID);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_S2D_RMQ_IDS);
        }

        private void createContactsTables(SQLiteDatabase db) {
            if (DBG)
                log("createContactsTables");

            StringBuilder buf = new StringBuilder();
            String contactsTableName = TABLE_CONTACTS;

            // creating the "contacts" table
            buf.append("CREATE TABLE IF NOT EXISTS ");
            buf.append(contactsTableName);
            buf.append(" (");
            buf.append("_id INTEGER PRIMARY KEY,");
            buf.append("username TEXT,");
            buf.append("nickname TEXT,");

            buf.append("provider INTEGER,");
            buf.append("account INTEGER,");
            buf.append("contactList INTEGER,");
            buf.append("type INTEGER,");
            buf.append("subscriptionStatus INTEGER,");
            buf.append("subscriptionType INTEGER,");

            // the following are derived from Google Contact Extension, we don't include all
            // the attributes, just the ones we can use.
            // (see http://code.google.com/apis/talk/jep_extensions/roster_attributes.html)
            //
            // qc: quick contact (derived from message count)
            // rejected: if the contact has ever been rejected by the user
            buf.append("qc INTEGER,");
            buf.append("rejected INTEGER,");

            // Off the record status
            buf.append("otr INTEGER");

            buf.append(");");

            db.execSQL(buf.toString());

            buf.delete(0, buf.length());

            // creating contact etag table
            buf.append("CREATE TABLE IF NOT EXISTS ");
            buf.append(TABLE_CONTACTS_ETAG);
            buf.append(" (");
            buf.append("_id INTEGER PRIMARY KEY,");
            buf.append("etag TEXT,");
            buf.append("otr_etag TEXT,");
            buf.append("account INTEGER UNIQUE");
            buf.append(");");

            db.execSQL(buf.toString());

            buf.delete(0, buf.length());

            // creating the "contactList" table
            buf.append("CREATE TABLE IF NOT EXISTS ");
            buf.append(TABLE_CONTACT_LIST);
            buf.append(" (");
            buf.append("_id INTEGER PRIMARY KEY,");
            buf.append("name TEXT,");
            buf.append("provider INTEGER,");
            buf.append("account INTEGER");
            buf.append(");");

            db.execSQL(buf.toString());

            buf.delete(0, buf.length());

            // creating the "blockedList" table
            buf.append("CREATE TABLE IF NOT EXISTS ");
            buf.append(TABLE_BLOCKED_LIST);
            buf.append(" (");
            buf.append("_id INTEGER PRIMARY KEY,");
            buf.append("username TEXT,");
            buf.append("nickname TEXT,");
            buf.append("provider INTEGER,");
            buf.append("account INTEGER");
            buf.append(");");

            db.execSQL(buf.toString());
        }

        private void createMessageChatTables(SQLiteDatabase db,
                boolean addShowTsColumnForMessagesTable) {
            if (DBG)
                log("createMessageChatTables");

            // message table
            StringBuilder buf = new StringBuilder();
            buf.append("CREATE TABLE IF NOT EXISTS ");
            buf.append(TABLE_MESSAGES);
            buf.append(" (_id INTEGER PRIMARY KEY,");
            buf.append("thread_id INTEGER,");
            buf.append("nickname TEXT,");
            buf.append("body TEXT,");
            buf.append("date INTEGER,");
            buf.append("type INTEGER,");
            buf.append("packet_id TEXT UNIQUE,");
            buf.append("err_code INTEGER NOT NULL DEFAULT 0,");
            buf.append("err_msg TEXT,");
            buf.append("is_muc INTEGER");

            if (addShowTsColumnForMessagesTable) {
                buf.append(",show_ts INTEGER");
            }
            buf.append(",is_delivered INTEGER");
            buf.append(",mime_type TEXT");

            buf.append(");");

            String sqlStatement = buf.toString();

            if (DBG)
                log("create message table: " + sqlStatement);
            db.execSQL(sqlStatement);

            buf.delete(0, buf.length());
            buf.append("CREATE TABLE IF NOT EXISTS ");
            buf.append(TABLE_CHATS);
            buf.append(" (_id INTEGER PRIMARY KEY,");
            buf.append("contact_id INTEGER UNIQUE,");
            buf.append("jid_resource TEXT,"); // the JID resource for the user, for non-group chats
            buf.append("groupchat INTEGER,"); // 1 if group chat, 0 if not TODO: remove this column
            buf.append("last_unread_message TEXT,"); // the last unread message
            buf.append("last_message_date INTEGER,"); // in seconds
            buf.append("unsent_composed_message TEXT,"); // a composed, but not sent message
            buf.append("shortcut INTEGER);"); // which of 10 slots (if any) this chat occupies

            // chat sessions, including single person chats and group chats
            sqlStatement = buf.toString();

            if (DBG)
                log("create chat table: " + sqlStatement);
            db.execSQL(sqlStatement);

            buf.delete(0, buf.length());
            buf.append("CREATE TRIGGER IF NOT EXISTS contact_cleanup ");
            buf.append("DELETE ON contacts ");
            buf.append("BEGIN ");
            buf.append("DELETE FROM ").append(TABLE_CHATS).append(" WHERE contact_id = OLD._id;");
            buf.append("DELETE FROM ").append(TABLE_MESSAGES).append(" WHERE thread_id = OLD._id;");
            buf.append("END");

            sqlStatement = buf.toString();

            if (DBG)
                log("create trigger: " + sqlStatement);
            db.execSQL(sqlStatement);
        }

        private void createInMemoryMessageTables(SQLiteDatabase db, String tablePrefix) {
            String tableName = (tablePrefix != null) ? tablePrefix + TABLE_IN_MEMORY_MESSAGES
                                                    : TABLE_IN_MEMORY_MESSAGES;

            db.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " ("
                       + "_id INTEGER PRIMARY KEY," + "thread_id INTEGER," + "nickname TEXT,"
                       + "body TEXT,"
                       + "date INTEGER,"
                       + // in millisec
                       "type INTEGER," + "packet_id TEXT UNIQUE,"
                       + "err_code INTEGER NOT NULL DEFAULT 0," + "err_msg TEXT,"
                       + "is_muc INTEGER," + "show_ts INTEGER," +
                       "is_delivered INTEGER," +
                       "mime_type TEXT" +
                       ");");

        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (db.isReadOnly()) {
                LogCleaner.warn(LOG_TAG, "ImProvider database opened in read only mode.");
                LogCleaner.warn(LOG_TAG, "Transient tables not created.");
                return;
            }

            if (DBG)
                log("##### createTransientTables");

            
            // Create transient tables
            String cpDbName;
            
            if (mInMemoryDB)
            {
                db.execSQL("ATTACH DATABASE ':memory:' AS " + mTransientDbName + ";");
                cpDbName = mTransientDbName + ".";
            }
            else
            {
               cpDbName = "";
            }
            
            // in-memory message table
            createInMemoryMessageTables(db, cpDbName);

            // presence
            db.execSQL("CREATE TABLE IF NOT EXISTS " + cpDbName + TABLE_PRESENCE + " ("
                       + "_id INTEGER PRIMARY KEY," + "contact_id INTEGER UNIQUE,"
                       + "jid_resource TEXT," + // jid resource for the presence
                       "client_type INTEGER," + // client type
                       "priority INTEGER," + // presence priority (XMPP)
                       "mode INTEGER," + // presence mode
                       "status TEXT" + // custom status
                       ");");

            // group chat invitations
            db.execSQL("CREATE TABLE IF NOT EXISTS " + cpDbName + TABLE_INVITATIONS + " ("
                       + "_id INTEGER PRIMARY KEY," + "providerId INTEGER," + "accountId INTEGER,"
                       + "inviteId TEXT," + "sender TEXT," + "groupName TEXT," + "note TEXT,"
                       + "status INTEGER" + ");");

            // group chat members
            db.execSQL("CREATE TABLE IF NOT EXISTS " + cpDbName + TABLE_GROUP_MEMBERS + " ("
                       + "_id INTEGER PRIMARY KEY," + "groupId INTEGER," + "username TEXT,"
                       + "nickname TEXT" + ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + cpDbName + TABLE_ACCOUNT_STATUS + " ("
                       + "_id INTEGER PRIMARY KEY," + "account INTEGER UNIQUE,"
                       + "presenceStatus INTEGER," + "connStatus INTEGER" + ");");

            /* when we moved the contact table out of transient_db and into the main db, the
               presence and groupchat cleanup triggers don't work anymore. It seems we can't
               create triggers that reference objects in a different database!

            // Insert a default presence for newly inserted contact
            db.execSQL("CREATE TRIGGER IF NOT EXISTS contact_create_presence " +
                    "AFTER INSERT ON " + contactsTableName +
                        " WHEN NEW.type != " + Im.Contacts.TYPE_GROUP +
                        " BEGIN " +
                            "INSERT INTO presence (contact_id) VALUES (NEW._id);" +
                        " END");

            // Remove the presence when the contact is removed.
            db.execSQL("CREATE TRIGGER IF NOT EXISTS contact_presence_cleanup " +
                    "DELETE ON " + contactsTableName +
                       " BEGIN " +
                           "DELETE FROM presence WHERE contact_id = OLD._id;" +
                       "END");

            // Cleans up group members and group messages when a group chat is deleted
            db.execSQL("CREATE TRIGGER IF NOT EXISTS " + cpDbName + "group_cleanup " +
                    "DELETE ON " + cpDbName + contactsTableName +
                       " FOR EACH ROW WHEN OLD.type = " + Im.Contacts.TYPE_GROUP +
                       " BEGIN " +
                           "DELETE FROM groupMembers WHERE groupId = OLD._id;" +
                           "DELETE FROM groupMessages WHERE groupId = OLD._id;" +
                       " END");
            */

            // only store the session cookies in memory right now. This means
            // that we don't persist them across device reboot
            db.execSQL("CREATE TABLE IF NOT EXISTS " + cpDbName + TABLE_SESSION_COOKIES + " ("
                       + "_id INTEGER PRIMARY KEY," + "provider INTEGER," + "account INTEGER,"
                       + "name TEXT," + "value TEXT" + ");");

        }

        @Override
        public synchronized void close() {

            if (dbRead != null && dbRead.isOpen())
                dbRead.close();

            if (dbWrite != null && dbWrite.isOpen())
                dbWrite.close();

            super.close();
        }
    }

    static {
        sProviderAccountsProjectionMap = new HashMap<String, String>();
        sProviderAccountsProjectionMap.put(Imps.Provider._ID, "providers._id AS _id");
        sProviderAccountsProjectionMap.put(Imps.Provider._COUNT, "COUNT(*) AS _account");
        sProviderAccountsProjectionMap.put(Imps.Provider.NAME, "providers.name AS name");
        sProviderAccountsProjectionMap
                .put(Imps.Provider.FULLNAME, "providers.fullname AS fullname");
        sProviderAccountsProjectionMap
                .put(Imps.Provider.CATEGORY, "providers.category AS category");
        sProviderAccountsProjectionMap.put(Imps.Provider.ACTIVE_ACCOUNT_ID,
                "accounts._id AS account_id");
        sProviderAccountsProjectionMap.put(Imps.Provider.ACTIVE_ACCOUNT_USERNAME,
                "accounts.username AS account_username");
        sProviderAccountsProjectionMap.put(Imps.Provider.ACTIVE_ACCOUNT_PW,
                "accounts.pw AS account_pw");
        sProviderAccountsProjectionMap.put(Imps.Provider.ACTIVE_ACCOUNT_LOCKED,
                "accounts.locked AS account_locked");
        sProviderAccountsProjectionMap.put(Imps.Provider.ACTIVE_ACCOUNT_KEEP_SIGNED_IN,
                "accounts.keep_signed_in AS account_keepSignedIn");
        sProviderAccountsProjectionMap.put(Imps.Provider.ACCOUNT_PRESENCE_STATUS,
                "accountStatus.presenceStatus AS account_presenceStatus");
        sProviderAccountsProjectionMap.put(Imps.Provider.ACCOUNT_CONNECTION_STATUS,
                "accountStatus.connStatus AS account_connStatus");

        sAccountsByDomainProjectionMap = new HashMap<String, String>();
        sAccountsByDomainProjectionMap.put(Imps.Account._ID, "accounts._id AS _id");

        // contacts projection map
        sContactsProjectionMap = new HashMap<String, String>();

        // Base column
        sContactsProjectionMap.put(Imps.Contacts._ID, "contacts._id AS _id");
        sContactsProjectionMap.put(Imps.Contacts._COUNT, "COUNT(*) AS _count");

        // contacts column
        sContactsProjectionMap.put(Imps.Contacts._ID, "contacts._id as _id");
        sContactsProjectionMap.put(Imps.Contacts.USERNAME, "contacts.username as username");
        sContactsProjectionMap.put(Imps.Contacts.NICKNAME, "contacts.nickname as nickname");
        sContactsProjectionMap.put(Imps.Contacts.PROVIDER, "contacts.provider as provider");
        sContactsProjectionMap.put(Imps.Contacts.ACCOUNT, "contacts.account as account");
        sContactsProjectionMap
                .put(Imps.Contacts.CONTACTLIST, "contacts.contactList as contactList");
        sContactsProjectionMap.put(Imps.Contacts.TYPE, "contacts.type as type");
        sContactsProjectionMap.put(Imps.Contacts.SUBSCRIPTION_STATUS,
                "contacts.subscriptionStatus as subscriptionStatus");
        sContactsProjectionMap.put(Imps.Contacts.SUBSCRIPTION_TYPE,
                "contacts.subscriptionType as subscriptionType");
        sContactsProjectionMap.put(Imps.Contacts.QUICK_CONTACT, "contacts.qc as qc");
        sContactsProjectionMap.put(Imps.Contacts.REJECTED, "contacts.rejected as rejected");

        // Presence columns
        sContactsProjectionMap.put(Imps.Presence.CONTACT_ID, "presence.contact_id AS contact_id");
        sContactsProjectionMap.put(Imps.Contacts.PRESENCE_STATUS, "presence.mode AS mode");
        sContactsProjectionMap.put(Imps.Contacts.PRESENCE_CUSTOM_STATUS,
                "presence.status AS status");
        sContactsProjectionMap
                .put(Imps.Contacts.CLIENT_TYPE, "presence.client_type AS client_type");

        // Chats columns
        sContactsProjectionMap.put(Imps.Contacts.CHATS_CONTACT,
                "chats.contact_id AS chats_contact_id");
        sContactsProjectionMap.put(Imps.Chats.JID_RESOURCE, "chats.jid_resource AS jid_resource");
        sContactsProjectionMap.put(Imps.Chats.GROUP_CHAT, "chats.groupchat AS groupchat");
        sContactsProjectionMap.put(Imps.Contacts.LAST_UNREAD_MESSAGE,
                "chats.last_unread_message AS last_unread_message");
        sContactsProjectionMap.put(Imps.Contacts.LAST_MESSAGE_DATE,
                "chats.last_message_date AS last_message_date");
        sContactsProjectionMap.put(Imps.Contacts.UNSENT_COMPOSED_MESSAGE,
                "chats.unsent_composed_message AS unsent_composed_message");
        sContactsProjectionMap.put(Imps.Contacts.SHORTCUT, "chats.SHORTCUT AS shortcut");

        // Avatars columns
        sContactsProjectionMap.put(Imps.Contacts.AVATAR_HASH, "avatars.hash AS avatars_hash");
        sContactsProjectionMap.put(Imps.Contacts.AVATAR_DATA, "quote(avatars.data) AS avatars_data");

        // contactList projection map
        sContactListProjectionMap = new HashMap<String, String>();
        sContactListProjectionMap.put(Imps.ContactList._ID, "contactList._id AS _id");
        sContactListProjectionMap.put(Imps.ContactList._COUNT, "COUNT(*) AS _count");
        sContactListProjectionMap.put(Imps.ContactList.NAME, "name");
        sContactListProjectionMap.put(Imps.ContactList.PROVIDER, "provider");
        sContactListProjectionMap.put(Imps.ContactList.ACCOUNT, "account");

        // blockedList projection map
        sBlockedListProjectionMap = new HashMap<String, String>();
        sBlockedListProjectionMap.put(Imps.BlockedList._ID, "blockedList._id AS _id");
        sBlockedListProjectionMap.put(Imps.BlockedList._COUNT, "COUNT(*) AS _count");
        sBlockedListProjectionMap.put(Imps.BlockedList.USERNAME, "username");
        sBlockedListProjectionMap.put(Imps.BlockedList.NICKNAME, "nickname");
        sBlockedListProjectionMap.put(Imps.BlockedList.PROVIDER, "provider");
        sBlockedListProjectionMap.put(Imps.BlockedList.ACCOUNT, "account");
        sBlockedListProjectionMap.put(Imps.BlockedList.AVATAR_DATA, "quote(avatars.data) AS avatars_data");

        // messages projection map
        sMessagesProjectionMap = new HashMap<String, String>();
        sMessagesProjectionMap.put(Imps.Messages._ID, "messages._id AS _id");
        sMessagesProjectionMap.put(Imps.Messages._COUNT, "COUNT(*) AS _count");
        sMessagesProjectionMap.put(Imps.Messages.THREAD_ID, "messages.thread_id AS thread_id");
        sMessagesProjectionMap.put(Imps.Messages.PACKET_ID, "messages.packet_id AS packet_id");
        sMessagesProjectionMap.put(Imps.Messages.NICKNAME, "messages.nickname AS nickname");
        sMessagesProjectionMap.put(Imps.Messages.BODY, "messages.body AS body");
        sMessagesProjectionMap.put(Imps.Messages.DATE, "messages.date AS date");
        sMessagesProjectionMap.put(Imps.Messages.TYPE, "messages.type AS type");
        sMessagesProjectionMap.put(Imps.Messages.ERROR_CODE, "messages.err_code AS err_code");
        sMessagesProjectionMap.put(Imps.Messages.ERROR_MESSAGE, "messages.err_msg AS err_msg");
        sMessagesProjectionMap.put(Imps.Messages.IS_GROUP_CHAT, "messages.is_muc AS is_muc");
        sMessagesProjectionMap.put(Imps.Messages.DISPLAY_SENT_TIME, "messages.show_ts AS show_ts");
        sMessagesProjectionMap.put(Imps.Messages.IS_DELIVERED,
                "messages.is_delivered AS is_delivered");
        sMessagesProjectionMap.put(Imps.Messages.MIME_TYPE,
                "messages.mime_type AS mime_type");
        // contacts columns
        sMessagesProjectionMap.put(Imps.Messages.CONTACT, "contacts.username AS contact");
        sMessagesProjectionMap.put(Imps.Contacts.PROVIDER, "contacts.provider AS provider");
        sMessagesProjectionMap.put(Imps.Contacts.ACCOUNT, "contacts.account AS account");
        sMessagesProjectionMap.put("contact_type", "contacts.type AS contact_type");

        sInMemoryMessagesProjectionMap = new HashMap<String, String>();
        sInMemoryMessagesProjectionMap.put(Imps.Messages._ID, "inMemoryMessages._id AS _id");
        sInMemoryMessagesProjectionMap.put(Imps.Messages._COUNT, "COUNT(*) AS _count");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.THREAD_ID,
                "inMemoryMessages.thread_id AS thread_id");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.PACKET_ID,
                "inMemoryMessages.packet_id AS packet_id");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.NICKNAME,
                "inMemoryMessages.nickname AS nickname");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.BODY, "inMemoryMessages.body AS body");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.DATE, "inMemoryMessages.date AS date");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.TYPE, "inMemoryMessages.type AS type");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.ERROR_CODE,
                "inMemoryMessages.err_code AS err_code");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.ERROR_MESSAGE,
                "inMemoryMessages.err_msg AS err_msg");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.IS_GROUP_CHAT,
                "inMemoryMessages.is_muc AS is_muc");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.DISPLAY_SENT_TIME,
                "inMemoryMessages.show_ts AS show_ts");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.IS_DELIVERED,
                "inMemoryMessages.is_delivered AS is_delivered");
        sInMemoryMessagesProjectionMap.put(Imps.Messages.MIME_TYPE,
                "inMemoryMessages.mime_type AS mime_type");
        // contacts columns
        sInMemoryMessagesProjectionMap.put(Imps.Messages.CONTACT, "contacts.username AS contact");
        sInMemoryMessagesProjectionMap.put(Imps.Contacts.PROVIDER, "contacts.provider AS provider");
        sInMemoryMessagesProjectionMap.put(Imps.Contacts.ACCOUNT, "contacts.account AS account");
        sInMemoryMessagesProjectionMap.put("contact_type", "contacts.type AS contact_type");
    }

    public ImpsProvider() {
        this(DATABASE_VERSION);

    
        setupImUrlMatchers(AUTHORITY);
        setupMcsUrlMatchers(AUTHORITY);
    }

    protected ImpsProvider(int dbVersion) {
        mDatabaseVersion = dbVersion;
    }

    private void setupImUrlMatchers(String authority) {
        mUrlMatcher.addURI(authority, "providers", MATCH_PROVIDERS);
        mUrlMatcher.addURI(authority, "providers/#", MATCH_PROVIDERS_BY_ID);
        mUrlMatcher.addURI(authority, "providers/account", MATCH_PROVIDERS_WITH_ACCOUNT);

        mUrlMatcher.addURI(authority, "accounts", MATCH_ACCOUNTS);
        mUrlMatcher.addURI(authority, "domainAccounts", MATCH_ACCOUNTS_WITH_DOMAIN);
        mUrlMatcher.addURI(authority, "accounts/#", MATCH_ACCOUNTS_BY_ID);

        mUrlMatcher.addURI(authority, "contacts", MATCH_CONTACTS);
        mUrlMatcher.addURI(authority, "contactsWithPresence", MATCH_CONTACTS_JOIN_PRESENCE);
        mUrlMatcher.addURI(authority, "contactsBarebone", MATCH_CONTACTS_BAREBONE);
        mUrlMatcher.addURI(authority, "contacts/#/#", MATCH_CONTACTS_BY_PROVIDER);
        mUrlMatcher.addURI(authority, "contacts/chatting", MATCH_CHATTING_CONTACTS);
        mUrlMatcher.addURI(authority, "contacts/chatting/#/#", MATCH_CHATTING_CONTACTS_BY_PROVIDER);
        mUrlMatcher.addURI(authority, "contacts/online/#/#", MATCH_ONLINE_CONTACTS_BY_PROVIDER);
        mUrlMatcher.addURI(authority, "contacts/offline/#/#", MATCH_OFFLINE_CONTACTS_BY_PROVIDER);
        mUrlMatcher.addURI(authority, "contacts/#", MATCH_CONTACT);
        mUrlMatcher.addURI(authority, "contacts/blocked", MATCH_BLOCKED_CONTACTS);
        mUrlMatcher.addURI(authority, "bulk_contacts", MATCH_CONTACTS_BULK);
        mUrlMatcher.addURI(authority, "contacts/onlineCount", MATCH_ONLINE_CONTACT_COUNT);

        mUrlMatcher.addURI(authority, "contactLists", MATCH_CONTACTLISTS);
        mUrlMatcher.addURI(authority, "contactLists/#/#", MATCH_CONTACTLISTS_BY_PROVIDER);
        mUrlMatcher.addURI(authority, "contactLists/#", MATCH_CONTACTLIST);
        mUrlMatcher.addURI(authority, "blockedList", MATCH_BLOCKEDLIST);
        mUrlMatcher.addURI(authority, "blockedList/#/#", MATCH_BLOCKEDLIST_BY_PROVIDER);

        mUrlMatcher.addURI(authority, "contactsEtag", MATCH_CONTACTS_ETAGS);
        mUrlMatcher.addURI(authority, "contactsEtag/#", MATCH_CONTACTS_ETAG);

        mUrlMatcher.addURI(authority, "presence", MATCH_PRESENCE);
        mUrlMatcher.addURI(authority, "presence/#", MATCH_PRESENCE_ID);
        mUrlMatcher.addURI(authority, "presence/account/#", MATCH_PRESENCE_BY_ACCOUNT);
        mUrlMatcher.addURI(authority, "seed_presence/account/#", MATCH_PRESENCE_SEED_BY_ACCOUNT);
        mUrlMatcher.addURI(authority, "bulk_presence", MATCH_PRESENCE_BULK);

        mUrlMatcher.addURI(authority, "messages", MATCH_MESSAGES);
        mUrlMatcher.addURI(authority, "messagesByAcctAndContact/#/*", MATCH_MESSAGES_BY_CONTACT);
        mUrlMatcher.addURI(authority, "messagesByThreadId/#", MATCH_MESSAGES_BY_THREAD_ID);
        mUrlMatcher.addURI(authority, "messagesByProvider/#", MATCH_MESSAGES_BY_PROVIDER);
        mUrlMatcher.addURI(authority, "messagesByAccount/#", MATCH_MESSAGES_BY_ACCOUNT);
        mUrlMatcher.addURI(authority, "messages/#", MATCH_MESSAGE);

        mUrlMatcher.addURI(authority, "otrMessages", MATCH_OTR_MESSAGES);
        mUrlMatcher.addURI(authority, "otrMessagesByAcctAndContact/#/*",
                MATCH_OTR_MESSAGES_BY_CONTACT);
        mUrlMatcher.addURI(authority, "otrMessagesByThreadId/#", MATCH_OTR_MESSAGES_BY_THREAD_ID);
        mUrlMatcher.addURI(authority, "otrMessagesByProvider/#", MATCH_OTR_MESSAGES_BY_PROVIDER);
        mUrlMatcher.addURI(authority, "otrMessagesByAccount/#", MATCH_OTR_MESSAGES_BY_ACCOUNT);
        mUrlMatcher.addURI(authority, "otrMessagesByPacketId/*", MATCH_OTR_MESSAGES_BY_PACKET_ID);
        mUrlMatcher.addURI(authority, "otrMessages/#", MATCH_OTR_MESSAGE);

        mUrlMatcher.addURI(authority, "groupMembers", MATCH_GROUP_MEMBERS);
        mUrlMatcher.addURI(authority, "groupMembers/#", MATCH_GROUP_MEMBERS_BY_GROUP);

        mUrlMatcher.addURI(authority, "avatars", MATCH_AVATARS);
        mUrlMatcher.addURI(authority, "avatars/#", MATCH_AVATAR);
        mUrlMatcher.addURI(authority, "avatarsBy/#/#", MATCH_AVATAR_BY_PROVIDER);
        mUrlMatcher.addURI(authority, "chats", MATCH_CHATS);
        mUrlMatcher.addURI(authority, "chats/account/#", MATCH_CHATS_BY_ACCOUNT);
        mUrlMatcher.addURI(authority, "chats/#", MATCH_CHATS_ID);

        mUrlMatcher.addURI(authority, "sessionCookies", MATCH_SESSIONS);
        mUrlMatcher.addURI(authority, "sessionCookiesBy/#/#", MATCH_SESSIONS_BY_PROVIDER);
        mUrlMatcher.addURI(authority, "providerSettings", MATCH_PROVIDER_SETTINGS);
        mUrlMatcher.addURI(authority, "providerSettings/#", MATCH_PROVIDER_SETTINGS_BY_ID);
        mUrlMatcher.addURI(authority, "providerSettings/#/*",
                MATCH_PROVIDER_SETTINGS_BY_ID_AND_NAME);

        mUrlMatcher.addURI(authority, "invitations", MATCH_INVITATIONS);
        mUrlMatcher.addURI(authority, "invitations/#", MATCH_INVITATION);

        mUrlMatcher.addURI(authority, "accountStatus", MATCH_ACCOUNTS_STATUS);
        mUrlMatcher.addURI(authority, "accountStatus/#", MATCH_ACCOUNT_STATUS);

        mUrlMatcher.addURI(authority, "brandingResMapCache", MATCH_BRANDING_RESOURCE_MAP_CACHE);
    }

    private void setupMcsUrlMatchers(String authority) {
        mUrlMatcher.addURI(authority, "outgoingRmqMessages", MATCH_OUTGOING_RMQ_MESSAGES);
        mUrlMatcher.addURI(authority, "outgoingRmqMessages/#", MATCH_OUTGOING_RMQ_MESSAGE);
        mUrlMatcher.addURI(authority, "outgoingHighestRmqId", MATCH_OUTGOING_HIGHEST_RMQ_ID);
        mUrlMatcher.addURI(authority, "lastRmqId", MATCH_LAST_RMQ_ID);
        mUrlMatcher.addURI(authority, "s2dids", MATCH_S2D_RMQ_IDS);
    }

    @Override
    public boolean onCreate() {

        return true;
    }

    private void setDatabaseName(boolean isEncrypted) {
        mDatabaseName = isEncrypted ? ENCRYPTED_DATABASE_NAME : UNENCRYPTED_DATABASE_NAME;
        mTransientDbName = "transient_" + mDatabaseName.replace(".", "_");
    }
    
    private synchronized DatabaseHelper initDBHelper(String pkey, boolean noCreate) throws Exception {
        if (mDbHelper == null) {
            if (pkey != null) {
                setDatabaseName(!pkey.isEmpty());
                Context ctx = getContext();
                String path = ctx.getDatabasePath(mDatabaseName).getPath();
                if (noCreate && !new File(path).exists()) {
                    LogCleaner.debug(ImApp.LOG_TAG, "no DB exists at " + path);
                    return null;
                }

                boolean inMemoryDb = false;

                mDbHelper = new DatabaseHelper(ctx, pkey, inMemoryDb);
                OtrAndroidKeyManagerImpl.setKeyStorePassword(pkey);
                LogCleaner.debug(LOG_TAG, "Opened DB with key - empty=" + pkey.isEmpty());
                
                Debug.recordTrail(getContext(), EMPTY_KEY_TRAIL_TAG, "" + pkey.isEmpty());
                String prevOpen = Debug.getTrail(getContext(), DATABASE_OPEN_TRAIL_TAG);
                if (prevOpen != null) {
                    Debug.recordTrail(getContext(), PREV_DATABASE_OPEN_TRAIL_TAG, prevOpen);
                }
                Debug.recordTrail(getContext(), DATABASE_OPEN_TRAIL_TAG, new Date());
            } else {
                LogCleaner.warn(ImApp.LOG_TAG, "DB not open and no password provided");
            }
        }

        return mDbHelper;

    }

    private DatabaseHelper getDBHelper() {
        
        return mDbHelper;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public final int update(final Uri url, final ContentValues values, final String selection,
            final String[] selectionArgs) {

        DatabaseHelper dbHelper = getDBHelper();
        
        
        int result = 0;
        
        if (dbHelper != null)
        {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            synchronized (db)
            {
                if (db.isOpen())
                {
                    db.beginTransaction();
                    try {
                        result = updateInternal(url, values, selection, selectionArgs);
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                    if (result > 0) {
                        getContext().getContentResolver()
                                .notifyChange(url, null /* observer */, false /* sync */);
                    }
                }
            }
        }
        
        return result;
    }

    @Override
    public final int delete(final Uri url, final String selection, final String[] selectionArgs) {
        
        int result = -1;
        
        if (getDBHelper() != null)
        {
            SQLiteDatabase db = getDBHelper().getWritableDatabase();
            
            if (db.isOpen()) //db can be closed if service sign out takes longer than app/cacheword lock
            {
                db.beginTransaction();
                try {
                    result = deleteInternal(url, selection, selectionArgs);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (result > 0) {
                    getContext().getContentResolver()
                            .notifyChange(url, null /* observer */, false /* sync */);
                }
            }
        }
        
        return result;
    }

    @Override
    public final Uri insert(final Uri url, final ContentValues values) {
        Uri result = null;
        
        if (getDBHelper() != null)
        {
            try
            {
                SQLiteDatabase db = getDBHelper().getWritableDatabase();
                synchronized (db)
                {
                    if (db.isOpen())
                    {
                        db.beginTransaction();
                        try {
                            result = insertInternal(url, values);
                            db.setTransactionSuccessful();
                        } finally {
                            db.endTransaction();
                        }
                        if (result != null) {
                            getContext().getContentResolver()
                                    .notifyChange(url, null /* observer */, false /* sync */);
                        }
                    }
                }
            }
            catch (IllegalStateException ise)
            {
                log("database closed when insert attempted: " + url.toString());
            }
        }
        return result;
    }

    @Override
    public final Cursor query(final Uri url, final String[] projection, final String selection,
            final String[] selectionArgs, final String sortOrder) {
        return queryInternal(url, projection, selection, selectionArgs, sortOrder);
    }

    boolean mLoadedLibs = false;
    
    public Cursor queryInternal(Uri url, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        
        Debug.onServiceStart();
        
        if (!mLoadedLibs)
        {
            SQLiteDatabase.loadLibs(this.getContext().getApplicationContext());
            mLoadedLibs = true;
        }
        
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        StringBuilder whereClause = new StringBuilder();
        if (selection != null) {
            whereClause.append(selection);
        }
        String groupBy = null;
        String limit = null;

        String pkey = url.getQueryParameter(ImApp.CACHEWORD_PASSWORD_KEY);
        boolean noCreate = "1".equals(url.getQueryParameter(ImApp.NO_CREATE_KEY));
        boolean clearKey = "1".equals(url.getQueryParameter(ImApp.CLEAR_PASSWORD_KEY));
        
        if (clearKey) {
            if (mDbHelper != null) {
                mDbHelper.close();
                mDbHelper = null;
            }
            return null;
        }
        
        try {
            initDBHelper(pkey, noCreate);
        } catch (Exception e) {
            LogCleaner.error(ImApp.LOG_TAG, e.getMessage(), e);
            return null;
        }
        
        if (mDbHelper == null) {
            // Failed to open
            return null;
        }
        
        if (pkey != null)
        {
            OtrAndroidKeyManagerImpl.setKeyStorePassword(pkey);
            
        }
    

        /*
         * String dbKey = null;
        if (sort != null && sort.startsWith("key="))
        {
        	if (sort.length() > 4)
        	{
        		dbKey = sort.substring(4);
        		sort = Imps.Provider.DEFAULT_SORT_ORDER;
        	
        		try
        		{
        			initDBHelper(dbKey);
        		
        		}
        		catch (Exception se)
        		{
        			//passcode must be incorrect or other db issue
        			return null;
        		}
        	}
        	else
        	{
        		return null;
        	}
        }
        else
        {
        	return null;
        }
        */

        // Generate the body of the query
        int match = mUrlMatcher.match(url);

        if (DBG) {
            //log("query " + url + ", match " + match + ", where " + selection);
            if (selectionArgs != null) {
                for (String selectionArg : selectionArgs) {
                    log("     selectionArg: " + selectionArg);
                }
            }
        }

        switch (match) {
        case MATCH_PROVIDERS_BY_ID:
            appendWhere(whereClause, Imps.Provider._ID, "=", url.getPathSegments().get(1));
            // fall thru.

        case MATCH_PROVIDERS:
            qb.setTables(TABLE_PROVIDERS);
            break;

        case MATCH_PROVIDERS_WITH_ACCOUNT:
            qb.setTables(PROVIDER_JOIN_ACCOUNT_TABLE);
            qb.setProjectionMap(sProviderAccountsProjectionMap);
            break;

        case MATCH_ACCOUNTS_WITH_DOMAIN:
            qb.setTables(DOMAIN_JOIN_ACCOUNT_TABLE);
            qb.setProjectionMap(sAccountsByDomainProjectionMap);
            break;

        case MATCH_ACCOUNTS_BY_ID:
            appendWhere(whereClause, Imps.Account._ID, "=", url.getPathSegments().get(1));
            // falls down
        case MATCH_ACCOUNTS:
            qb.setTables(TABLE_ACCOUNTS);
            break;

        case MATCH_CONTACTS:
            qb.setTables(CONTACT_JOIN_PRESENCE_CHAT_AVATAR_TABLE);
            qb.setProjectionMap(sContactsProjectionMap);
            break;

        case MATCH_CONTACTS_JOIN_PRESENCE:
            qb.setTables(CONTACT_JOIN_PRESENCE_TABLE);
            qb.setProjectionMap(sContactsProjectionMap);
            break;

        case MATCH_CONTACTS_BAREBONE:
            qb.setTables(TABLE_CONTACTS);
            break;

        case MATCH_CHATTING_CONTACTS:
            qb.setTables(CONTACT_JOIN_PRESENCE_CHAT_AVATAR_TABLE);
            qb.setProjectionMap(sContactsProjectionMap);
            appendWhere(whereClause, "chats.last_message_date IS NOT NULL");
            // no need to add the non blocked contacts clause because
            // blocked contacts can't have conversations.
            break;

        case MATCH_CONTACTS_BY_PROVIDER:
            buildQueryContactsByProvider(qb, whereClause, url);
            appendWhere(whereClause, NON_BLOCKED_CONTACTS_WHERE_CLAUSE);
            break;

        case MATCH_CHATTING_CONTACTS_BY_PROVIDER:
            buildQueryContactsByProvider(qb, whereClause, url);
            appendWhere(whereClause, "chats.last_message_date IS NOT NULL");
            // no need to add the non blocked contacts clause because
            // blocked contacts can't have conversations.
            break;

        case MATCH_NO_CHATTING_CONTACTS_BY_PROVIDER:
            buildQueryContactsByProvider(qb, whereClause, url);
            appendWhere(whereClause, "chats.last_message_date IS NULL");
            appendWhere(whereClause, NON_BLOCKED_CONTACTS_WHERE_CLAUSE);
            break;

        case MATCH_ONLINE_CONTACTS_BY_PROVIDER:
            buildQueryContactsByProvider(qb, whereClause, url);
            appendWhere(whereClause, Imps.Contacts.PRESENCE_STATUS, "!=", Imps.Presence.OFFLINE);
            appendWhere(whereClause, NON_BLOCKED_CONTACTS_WHERE_CLAUSE);
            break;

        case MATCH_OFFLINE_CONTACTS_BY_PROVIDER:
            buildQueryContactsByProvider(qb, whereClause, url);
            appendWhere(whereClause, Imps.Contacts.PRESENCE_STATUS, "=", Imps.Presence.OFFLINE);
            appendWhere(whereClause, NON_BLOCKED_CONTACTS_WHERE_CLAUSE);
            break;

        case MATCH_BLOCKED_CONTACTS:
            qb.setTables(CONTACT_JOIN_PRESENCE_CHAT_AVATAR_TABLE);
            qb.setProjectionMap(sContactsProjectionMap);
            appendWhere(whereClause, BLOCKED_CONTACTS_WHERE_CLAUSE);
            break;

        case MATCH_CONTACT:
            qb.setTables(CONTACT_JOIN_PRESENCE_CHAT_AVATAR_TABLE);
            qb.setProjectionMap(sContactsProjectionMap);
            appendWhere(whereClause, "contacts._id", "=", url.getPathSegments().get(1));
            break;

        case MATCH_ONLINE_CONTACT_COUNT:
            qb.setTables(CONTACT_JOIN_PRESENCE_CHAT_TABLE);
            qb.setProjectionMap(sContactsProjectionMap);
            appendWhere(whereClause, Imps.Contacts.PRESENCE_STATUS, "!=", Imps.Presence.OFFLINE);
            appendWhere(whereClause, "chats.last_message_date IS NULL");
            appendWhere(whereClause, NON_BLOCKED_CONTACTS_WHERE_CLAUSE);
            groupBy = Imps.Contacts.CONTACTLIST;
            break;

        case MATCH_CONTACTLISTS_BY_PROVIDER:
            appendWhere(whereClause, Imps.ContactList.ACCOUNT, "=", url.getPathSegments().get(2));
            // fall through
        case MATCH_CONTACTLISTS:
            qb.setTables(TABLE_CONTACT_LIST);
            qb.setProjectionMap(sContactListProjectionMap);
            break;

        case MATCH_CONTACTLIST:
            qb.setTables(TABLE_CONTACT_LIST);
            appendWhere(whereClause, Imps.ContactList._ID, "=", url.getPathSegments().get(1));
            break;

        case MATCH_BLOCKEDLIST:
            qb.setTables(BLOCKEDLIST_JOIN_AVATAR_TABLE);
            qb.setProjectionMap(sBlockedListProjectionMap);
            break;

        case MATCH_BLOCKEDLIST_BY_PROVIDER:
            qb.setTables(BLOCKEDLIST_JOIN_AVATAR_TABLE);
            qb.setProjectionMap(sBlockedListProjectionMap);
            appendWhere(whereClause, Imps.BlockedList.ACCOUNT, "=", url.getPathSegments().get(2));
            break;

        case MATCH_CONTACTS_ETAGS:
            qb.setTables(TABLE_CONTACTS_ETAG);
            break;

        case MATCH_CONTACTS_ETAG:
            qb.setTables(TABLE_CONTACTS_ETAG);
            appendWhere(whereClause, "_id", "=", url.getPathSegments().get(1));
            break;

        case MATCH_MESSAGES_BY_THREAD_ID:
            appendWhere(whereClause, Imps.Messages.THREAD_ID, "=", url.getPathSegments().get(1));
            // fall thru.

        case MATCH_MESSAGES:
            qb.setTables(TABLE_MESSAGES);

            final String selectionClause = whereClause.toString();
            final String query1 = qb.buildQuery(projectionIn, selectionClause, null, null, null,
                    null, null /* limit */);

            // Build the second query for frequent
            qb = new SQLiteQueryBuilder();
            qb.setTables(TABLE_IN_MEMORY_MESSAGES);
            final String query2 = qb.buildQuery(projectionIn, selectionClause, null, null, null,
                    null, null /* limit */);

            // Put them together
            final String query = qb.buildUnionQuery(new String[] { query1, query2 }, sort, null);
            final SQLiteDatabase db = getDBHelper().getWritableDatabase();
            String[] doubleArgs = null;
            if (selectionArgs != null) {
                
                doubleArgs = new String[ selectionArgs.length * 2];//Arrays.copyOf(selectionArgs, selectionArgs.length * 2);
                System.arraycopy(selectionArgs, 0, doubleArgs, 0, selectionArgs.length);
                System.arraycopy(selectionArgs, 0, doubleArgs, selectionArgs.length, selectionArgs.length);
            }
            
            Cursor c = db.rawQueryWithFactory(null, query, doubleArgs, TABLE_MESSAGES);
            if ((c != null) && !isTemporary()) {
                c.setNotificationUri(getContext().getContentResolver(), url);
            }
            return c;

        case MATCH_MESSAGE:
            qb.setTables(TABLE_MESSAGES);
            appendWhere(whereClause, Imps.Messages._ID, "=", url.getPathSegments().get(1));
            break;

        case MATCH_MESSAGES_BY_CONTACT:
            qb.setTables(MESSAGE_JOIN_CONTACT_TABLE);
            qb.setProjectionMap(sMessagesProjectionMap);

            appendWhere(whereClause, Imps.Contacts.ACCOUNT, "=", url.getPathSegments().get(1));
            appendWhere(whereClause, "contacts.username", "=", decodeURLSegment(url
                    .getPathSegments().get(2)));

            final String sel = whereClause.toString();
            final String q1 = qb.buildQuery(projectionIn, sel, null, null, null, null, null);

            // Build the second query for frequent
            qb = new SQLiteQueryBuilder();
            qb.setTables(IN_MEMORY_MESSAGES_JOIN_CONTACT_TABLE);
            qb.setProjectionMap(sInMemoryMessagesProjectionMap);
            final String q2 = qb.buildQuery(projectionIn, sel, null, null, null, null, null);

            // Put them together
            final String q3 = qb.buildUnionQuery(new String[] { q1, q2 }, sort, null);
            final SQLiteDatabase db2 = getDBHelper().getWritableDatabase();
            Cursor c2 = db2.rawQueryWithFactory(null, q3, null, MESSAGE_JOIN_CONTACT_TABLE);
            if ((c2 != null) && !isTemporary()) {
                c2.setNotificationUri(getContext().getContentResolver(), url);
            }
            return c2;

        case MATCH_INVITATIONS:
            qb.setTables(TABLE_INVITATIONS);
            break;

        case MATCH_INVITATION:
            qb.setTables(TABLE_INVITATIONS);
            appendWhere(whereClause, Imps.Invitation._ID, "=", url.getPathSegments().get(1));
            break;

        case MATCH_GROUP_MEMBERS:
            qb.setTables(TABLE_GROUP_MEMBERS);
            break;

        case MATCH_GROUP_MEMBERS_BY_GROUP:
            qb.setTables(TABLE_GROUP_MEMBERS);
            appendWhere(whereClause, Imps.GroupMembers.GROUP, "=", url.getPathSegments().get(1));
            break;

        case MATCH_AVATARS:
            qb.setTables(TABLE_AVATARS);
            break;

        case MATCH_AVATAR_BY_PROVIDER:
            qb.setTables(TABLE_AVATARS);
            appendWhere(whereClause, Imps.Avatars.ACCOUNT, "=", url.getPathSegments().get(2));
            break;

        case MATCH_CHATS:
            qb.setTables(TABLE_CHATS);
            break;

        case MATCH_CHATS_ID:
            qb.setTables(TABLE_CHATS);
            appendWhere(whereClause, Imps.Chats.CONTACT_ID, "=", url.getPathSegments().get(1));
            break;

        case MATCH_CHATS_BY_ACCOUNT:
            qb.setTables(TABLE_CHATS);
            String accountStr = decodeURLSegment(url.getLastPathSegment());
            appendWhere(
                    whereClause,
                    buildContactIdSelection(Imps.Chats.CONTACT_ID, Imps.Contacts.ACCOUNT + "='"
                                                                   + accountStr + "'"));
            break;

        case MATCH_PRESENCE:
            qb.setTables(TABLE_PRESENCE);
            break;

        case MATCH_PRESENCE_ID:
            qb.setTables(TABLE_PRESENCE);
            appendWhere(whereClause, Imps.Presence.CONTACT_ID, "=", url.getPathSegments().get(1));
            break;

        case MATCH_SESSIONS:
            qb.setTables(TABLE_SESSION_COOKIES);
            break;

        case MATCH_SESSIONS_BY_PROVIDER:
            qb.setTables(TABLE_SESSION_COOKIES);
            appendWhere(whereClause, Imps.SessionCookies.ACCOUNT, "=", url.getPathSegments().get(2));
            break;

        case MATCH_PROVIDER_SETTINGS_BY_ID_AND_NAME:
            appendWhere(whereClause, Imps.ProviderSettings.NAME, "=", url.getPathSegments().get(2));
            // fall through
        case MATCH_PROVIDER_SETTINGS_BY_ID:
            appendWhere(whereClause, Imps.ProviderSettings.PROVIDER, "=", url.getPathSegments()
                    .get(1));
            // fall through
        case MATCH_PROVIDER_SETTINGS:
            qb.setTables(TABLE_PROVIDER_SETTINGS);
            break;

        case MATCH_ACCOUNTS_STATUS:
            qb.setTables(TABLE_ACCOUNT_STATUS);
            break;

        case MATCH_ACCOUNT_STATUS:
            qb.setTables(TABLE_ACCOUNT_STATUS);
            appendWhere(whereClause, Imps.AccountStatus.ACCOUNT, "=", url.getPathSegments().get(1));
            break;

        case MATCH_BRANDING_RESOURCE_MAP_CACHE:
            qb.setTables(TABLE_BRANDING_RESOURCE_MAP_CACHE);
            break;

        // mcs and rmq queries
        case MATCH_OUTGOING_RMQ_MESSAGES:
            qb.setTables(TABLE_OUTGOING_RMQ_MESSAGES);
            break;

        case MATCH_OUTGOING_HIGHEST_RMQ_ID:
            qb.setTables(TABLE_OUTGOING_RMQ_MESSAGES);
            sort = "rmq_id DESC";
            limit = "1";
            break;

        case MATCH_LAST_RMQ_ID:
            qb.setTables(TABLE_LAST_RMQ_ID);
            limit = "1";
            break;

        case MATCH_S2D_RMQ_IDS:
            qb.setTables(TABLE_S2D_RMQ_IDS);
            break;

        default:
            throw new IllegalArgumentException("Unknown URL " + url);
        }

        if (getDBHelper() == null)
            return null;

        // run the query
        SQLiteDatabase db;
        try {
            db = getDBHelper().getReadableDatabase();
        } catch (net.sqlcipher.database.SQLiteException e) {
            // Failed to actually open - the passphrase must have been wrong - reset the helper
            mDbHelper = null;
            throw e;
        }
        net.sqlcipher.Cursor c = null;

        try {
            c = qb.query(db, projectionIn, whereClause.toString(), selectionArgs, groupBy, null,
                    sort, limit);
            if (c != null) {
                switch (match) {
                case MATCH_CHATTING_CONTACTS:
                case MATCH_CONTACTS_BY_PROVIDER:
                case MATCH_CHATTING_CONTACTS_BY_PROVIDER:
                case MATCH_ONLINE_CONTACTS_BY_PROVIDER:
                case MATCH_OFFLINE_CONTACTS_BY_PROVIDER:
                case MATCH_CONTACTS_BAREBONE:
                case MATCH_CONTACTS_JOIN_PRESENCE:
                case MATCH_ONLINE_CONTACT_COUNT:
                    url = Imps.Contacts.CONTENT_URI;
                    break;
                }
                if (DBG)
                    log("set notify url " + url);
                
                
                c.setNotificationUri(getContext().getContentResolver(), url);
            }
        } catch (Exception ex) {
            LogCleaner.error(LOG_TAG, "query exc db caught ", ex);
        }
        catch (Error ex) {
            LogCleaner.error(LOG_TAG, "query error db caught ", ex);
        }
        

        c = new MyCrossProcessCursorWrapper(c);
        return c;
    }
    
    static class MyCrossProcessCursorWrapper extends net.sqlcipher.CrossProcessCursorWrapper {
        public MyCrossProcessCursorWrapper(net.sqlcipher.Cursor cursor) {
            super(cursor);
        }
        
        @Override
        public void fillWindow(int position, CursorWindow window) {
            if (position < 0 || position > getCount()) {
                return;
            }
            window.acquireReference();
            try {
                moveToPosition(position - 1);
                window.clear();
                window.setStartPosition(position);
                int columnNum = getColumnCount();
                window.setNumColumns(columnNum);
                boolean isFull = false;
                int numRows = 10;
                
                while (!isFull && --numRows > 0 && moveToNext() && window.allocRow()) {
                    for (int i = 0; i < columnNum; i++) {
                        String field = getString(i);
                        if (field != null) {
                            if (!window.putString(field, getPosition(), i)) {
                                window.freeLastRow();
                                isFull = true;
                                break;
                            }
                        } else {
                            if (!window.putNull(getPosition(), i)) {
                                window.freeLastRow();
                                isFull = true;
                                break;
                            }
                        }
                    }
                }
            } catch (IllegalStateException e) {
                // simply ignore it
            } finally {
                window.releaseReference();
            }
        }
    }

    private void buildQueryContactsByProvider(SQLiteQueryBuilder qb, StringBuilder whereClause,
            Uri url) {
        qb.setTables(CONTACT_JOIN_PRESENCE_CHAT_AVATAR_TABLE);
        qb.setProjectionMap(sContactsProjectionMap);
        // we don't really need the provider id in query. account id is enough.
        appendWhere(whereClause, Imps.Contacts.ACCOUNT, "=", url.getLastPathSegment());
    }

    @Override
    public String getType(Uri url) {
        int match = mUrlMatcher.match(url);
        switch (match) {
        case MATCH_PROVIDERS:
            return Imps.Provider.CONTENT_TYPE;

        case MATCH_PROVIDERS_BY_ID:
            return Imps.Provider.CONTENT_ITEM_TYPE;

        case MATCH_ACCOUNTS:
            return Imps.Account.CONTENT_TYPE;

        case MATCH_ACCOUNTS_BY_ID:
            return Imps.Account.CONTENT_ITEM_TYPE;

        case MATCH_CONTACTS:
        case MATCH_CONTACTS_BY_PROVIDER:
        case MATCH_ONLINE_CONTACTS_BY_PROVIDER:
        case MATCH_OFFLINE_CONTACTS_BY_PROVIDER:
        case MATCH_CONTACTS_BULK:
        case MATCH_CONTACTS_BAREBONE:
        case MATCH_CONTACTS_JOIN_PRESENCE:
            return Imps.Contacts.CONTENT_TYPE;

        case MATCH_CONTACT:
            return Imps.Contacts.CONTENT_ITEM_TYPE;

        case MATCH_CONTACTLISTS:
        case MATCH_CONTACTLISTS_BY_PROVIDER:
            return Imps.ContactList.CONTENT_TYPE;

        case MATCH_CONTACTLIST:
            return Imps.ContactList.CONTENT_ITEM_TYPE;

        case MATCH_BLOCKEDLIST:
        case MATCH_BLOCKEDLIST_BY_PROVIDER:
            return Imps.BlockedList.CONTENT_TYPE;

        case MATCH_CONTACTS_ETAGS:
        case MATCH_CONTACTS_ETAG:
            return Imps.ContactsEtag.CONTENT_TYPE;

        case MATCH_MESSAGES:
        case MATCH_MESSAGES_BY_CONTACT:
        case MATCH_MESSAGES_BY_THREAD_ID:
        case MATCH_MESSAGES_BY_PROVIDER:
        case MATCH_MESSAGES_BY_ACCOUNT:
        case MATCH_OTR_MESSAGES:
        case MATCH_OTR_MESSAGES_BY_CONTACT:
        case MATCH_OTR_MESSAGES_BY_THREAD_ID:
        case MATCH_OTR_MESSAGES_BY_PROVIDER:
        case MATCH_OTR_MESSAGES_BY_ACCOUNT:
            return Imps.Messages.CONTENT_TYPE;

        case MATCH_MESSAGE:
        case MATCH_OTR_MESSAGE:
            return Imps.Messages.CONTENT_ITEM_TYPE;

        case MATCH_PRESENCE:
        case MATCH_PRESENCE_BULK:
            return Imps.Presence.CONTENT_TYPE;

        case MATCH_AVATARS:
            return Imps.Avatars.CONTENT_TYPE;

        case MATCH_AVATAR:
            return Imps.Avatars.CONTENT_ITEM_TYPE;

        case MATCH_CHATS:
            return Imps.Chats.CONTENT_TYPE;

        case MATCH_CHATS_ID:
            return Imps.Chats.CONTENT_ITEM_TYPE;

        case MATCH_INVITATIONS:
            return Imps.Invitation.CONTENT_TYPE;

        case MATCH_INVITATION:
            return Imps.Invitation.CONTENT_ITEM_TYPE;

        case MATCH_GROUP_MEMBERS:
        case MATCH_GROUP_MEMBERS_BY_GROUP:
            return Imps.GroupMembers.CONTENT_TYPE;

        case MATCH_SESSIONS:
        case MATCH_SESSIONS_BY_PROVIDER:
            return Imps.SessionCookies.CONTENT_TYPE;

        case MATCH_PROVIDER_SETTINGS:
            return Imps.ProviderSettings.CONTENT_TYPE;

        case MATCH_ACCOUNTS_STATUS:
            return Imps.AccountStatus.CONTENT_TYPE;

        case MATCH_ACCOUNT_STATUS:
            return Imps.AccountStatus.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URL");
        }
    }

    // package scope for testing.
    boolean insertBulkContacts(ContentValues values) {
        //if (DBG) log("insertBulkContacts: begin");

        ArrayList<String> usernames = getStringArrayList(values, Imps.Contacts.USERNAME);
        ArrayList<String> nicknames = getStringArrayList(values, Imps.Contacts.NICKNAME);
        int usernameCount = usernames.size();
        int nicknameCount = nicknames.size();

        if (usernameCount != nicknameCount) {
            LogCleaner.warn(LOG_TAG, "[ImProvider] insertBulkContacts: input bundle "
                           + "username & nickname lists have diff. length!");
            return false;
        }

        ArrayList<String> contactTypeArray = getStringArrayList(values, Imps.Contacts.TYPE);
        ArrayList<String> subscriptionStatusArray = getStringArrayList(values,
                Imps.Contacts.SUBSCRIPTION_STATUS);
        ArrayList<String> subscriptionTypeArray = getStringArrayList(values,
                Imps.Contacts.SUBSCRIPTION_TYPE);
        ArrayList<String> quickContactArray = getStringArrayList(values,
                Imps.Contacts.QUICK_CONTACT);
        ArrayList<String> rejectedArray = getStringArrayList(values, Imps.Contacts.REJECTED);
        int sum = 0;

        final SQLiteDatabase db = getDBHelper().getWritableDatabase();

        db.beginTransaction();
        try {
            Long provider = values.getAsLong(Imps.Contacts.PROVIDER);
            Long account = values.getAsLong(Imps.Contacts.ACCOUNT);
            Long listId = values.getAsLong(Imps.Contacts.CONTACTLIST);

            ContentValues contactValues = new ContentValues();
            contactValues.put(Imps.Contacts.PROVIDER, provider);
            contactValues.put(Imps.Contacts.ACCOUNT, account);
            contactValues.put(Imps.Contacts.CONTACTLIST, listId);
            ContentValues presenceValues = new ContentValues();
            presenceValues.put(Imps.Presence.PRESENCE_STATUS, Imps.Presence.OFFLINE);

            for (int i = 0; i < usernameCount; i++) {
                String username = usernames.get(i);
                String nickname = nicknames.get(i);
                int type = 0;
                int subscriptionStatus = 0;
                int subscriptionType = 0;
                int quickContact = 0;
                int rejected = 0;

                try {
                    type = Integer.parseInt(contactTypeArray.get(i));
                    if (subscriptionStatusArray != null) {
                        subscriptionStatus = Integer.parseInt(subscriptionStatusArray.get(i));
                    }
                    if (subscriptionTypeArray != null) {
                        subscriptionType = Integer.parseInt(subscriptionTypeArray.get(i));
                    }
                    if (quickContactArray != null) {
                        quickContact = Integer.parseInt(quickContactArray.get(i));
                    }
                    if (rejectedArray != null) {
                        rejected = Integer.parseInt(rejectedArray.get(i));
                    }
                } catch (NumberFormatException ex) {
                    LogCleaner.error(LOG_TAG, "insertBulkContacts: caught ", ex);
                }

                /*
                if (DBG) log("insertBulkContacts[" + i + "] username=" +
                        username + ", nickname=" + nickname + ", type=" + type +
                        ", subscriptionStatus=" + subscriptionStatus + ", subscriptionType=" +
                        subscriptionType + ", qc=" + quickContact);
                */

                contactValues.put(Imps.Contacts.USERNAME, username);
                contactValues.put(Imps.Contacts.NICKNAME, nickname);
                contactValues.put(Imps.Contacts.TYPE, type);
                if (subscriptionStatusArray != null) {
                    contactValues.put(Imps.Contacts.SUBSCRIPTION_STATUS, subscriptionStatus);
                }
                if (subscriptionTypeArray != null) {
                    contactValues.put(Imps.Contacts.SUBSCRIPTION_TYPE, subscriptionType);
                }
                if (quickContactArray != null) {
                    contactValues.put(Imps.Contacts.QUICK_CONTACT, quickContact);
                }
                if (rejectedArray != null) {
                    contactValues.put(Imps.Contacts.REJECTED, rejected);
                }

                long rowId;

                /* save this code for when we add constraint (account, username) to the contacts
                   table
                try {
                    rowId = db.insertOrThrow(TABLE_CONTACTS, USERNAME, contactValues);
                } catch (android.database.sqlite.SQLiteConstraintException ex) {
                    if (DBG) log("insertBulkContacts: insert " + username + " caught " + ex);

                    // append username to the selection clause
                    updateSelection.delete(0, updateSelection.length());
                    updateSelection.append(Im.Contacts.USERNAME);
                    updateSelection.append("=?");
                    updateSelectionArgs[0] = username;

                    int updated = db.update(TABLE_CONTACTS, contactValues,
                            updateSelection.toString(), updateSelectionArgs);

                    if (DBG && updated != 1) {
                        log("insertBulkContacts: update " + username + " failed!");
                    }
                }
                */

                rowId = db.insert(TABLE_CONTACTS, USERNAME, contactValues);
                if (rowId > 0) {
                    sum++;

                    // seed the presence for the new contact
                    if (DBG)
                        log("### seedPresence for contact id " + rowId);
                    presenceValues.put(Imps.Presence.CONTACT_ID, rowId);

                    try {
                        db.insert(TABLE_PRESENCE, null, presenceValues);
                    } catch (android.database.sqlite.SQLiteConstraintException ex) {
                        LogCleaner.warn(LOG_TAG, "insertBulkContacts: seeding presence caught " + ex);
                    }
                }

                // yield the lock if anyone else is trying to
                // perform a db operation here.
                db.yieldIfContended();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // We know that we succeeded becuase endTransaction throws if the transaction failed.
        if (DBG)
            log("insertBulkContacts: added " + sum + " contacts!");
        return true;
    }

    private ArrayList<String> getStringArrayList(ContentValues values, String key) {
        byte[] stored = values.getAsByteArray(key);
        if (stored == null)
            return null;
        ByteArrayInputStream bis = new ByteArrayInputStream(stored);
        try {
            ObjectInputStream is = new ObjectInputStream(bis);
            ArrayList<String> res = (ArrayList<String>) is.readObject();
            is.close();
            return res;
        } catch (StreamCorruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // package scope for testing.
    int updateBulkContacts(ContentValues values, String userWhere) {
        ArrayList<String> usernames = getStringArrayList(values, Imps.Contacts.USERNAME);
        ArrayList<String> nicknames = getStringArrayList(values, Imps.Contacts.NICKNAME);

        int usernameCount = usernames.size();
        int nicknameCount = nicknames.size();

        if (usernameCount != nicknameCount) {
            LogCleaner.warn(LOG_TAG, "[ImProvider] updateBulkContacts: input bundle "
                           + "username & nickname lists have diff. length!");
            return 0;
        }

        ArrayList<String> contactTypeArray = getStringArrayList(values, Imps.Contacts.TYPE);
        ArrayList<String> subscriptionStatusArray = getStringArrayList(values,
                Imps.Contacts.SUBSCRIPTION_STATUS);
        ArrayList<String> subscriptionTypeArray = getStringArrayList(values,
                Imps.Contacts.SUBSCRIPTION_TYPE);
        ArrayList<String> quickContactArray = getStringArrayList(values,
                Imps.Contacts.QUICK_CONTACT);
        ArrayList<String> rejectedArray = getStringArrayList(values, Imps.Contacts.REJECTED);
        final SQLiteDatabase db = getDBHelper().getWritableDatabase();

        db.beginTransaction();
        int sum = 0;

        try {
            Long provider = values.getAsLong(Imps.Contacts.PROVIDER);
            Long account = values.getAsLong(Imps.Contacts.ACCOUNT);

            ContentValues contactValues = new ContentValues();
            contactValues.put(Imps.Contacts.PROVIDER, provider);
            contactValues.put(Imps.Contacts.ACCOUNT, account);

            StringBuilder updateSelection = new StringBuilder();
            String[] updateSelectionArgs = new String[1];

            for (int i = 0; i < usernameCount; i++) {
                String username = usernames.get(i);
                String nickname = nicknames.get(i);
                int type = 0;
                int subscriptionStatus = 0;
                int subscriptionType = 0;
                int quickContact = 0;
                int rejected = 0;

                try {
                    type = Integer.parseInt(contactTypeArray.get(i));
                    subscriptionStatus = Integer.parseInt(subscriptionStatusArray.get(i));
                    subscriptionType = Integer.parseInt(subscriptionTypeArray.get(i));
                    quickContact = Integer.parseInt(quickContactArray.get(i));
                    rejected = Integer.parseInt(rejectedArray.get(i));
                } catch (NumberFormatException ex) {
                    LogCleaner.error(LOG_TAG, "insertBulkContacts: caught ",ex);
                }

                if (DBG)
                    log("updateBulkContacts[" + i + "] username=" + username + ", nickname="
                        + nickname + ", type=" + type + ", subscriptionStatus="
                        + subscriptionStatus + ", subscriptionType=" + subscriptionType + ", qc="
                        + quickContact);

                contactValues.put(Imps.Contacts.USERNAME, username);
                contactValues.put(Imps.Contacts.NICKNAME, nickname);
                contactValues.put(Imps.Contacts.TYPE, type);
                contactValues.put(Imps.Contacts.SUBSCRIPTION_STATUS, subscriptionStatus);
                contactValues.put(Imps.Contacts.SUBSCRIPTION_TYPE, subscriptionType);
                contactValues.put(Imps.Contacts.QUICK_CONTACT, quickContact);
                contactValues.put(Imps.Contacts.REJECTED, rejected);

                // append username to the selection clause
                updateSelection.delete(0, updateSelection.length());
                updateSelection.append(userWhere);
                updateSelection.append(" AND ");
                updateSelection.append(Imps.Contacts.USERNAME);
                updateSelection.append("=?");

                updateSelectionArgs[0] = username;

                int numUpdated = db.update(TABLE_CONTACTS, contactValues,
                        updateSelection.toString(), updateSelectionArgs);
                if (numUpdated == 0) {
                    LogCleaner.warn(LOG_TAG, "[ImProvider] updateBulkContacts: "
                                   + " update failed for selection = " + updateSelection);
                } else {
                    sum += numUpdated;
                }

                // yield the lock if anyone else is trying to
                // perform a db operation here.
                db.yieldIfContended();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (DBG)
            log("updateBulkContacts: " + sum + " entries updated");
        return sum;
    }

    /**
     * make sure the presence for all contacts of a given account is set to
     * offline, and each contact has a presence row associated with it. However,
     * this method does not remove presences for which the corresponding
     * contacts no longer exist. That's probably ok since presence is kept in
     * memory, so it won't stay around for too long. Here is the algorithm.
     * 
     * 1. for all presence that have a corresponding contact, make it OFFLINE.
     * This is one sqlite call. 2. query for all the contacts that don't have a
     * presence, and add a presence row for them.
     * 
     * TODO simplify the presence management! The desire is to have a presence
     * row for each TODO contact in the database, so later we can just call
     * update() on the presence rows TODO instead of checking for the existence
     * of presence first. The assumption is we get TODO presence updates much
     * more frequently. However, the logic to maintain that goal is TODO overly
     * complicated. One possible solution is to use insert_or_replace the
     * presence rows TODO when updating the presence. That way we don't always
     * need to maintain an empty presence TODO row for each contact.
     * 
     * @param account the account of the contacts for which we want to create
     *            seed presence rows.
     */
    private void seedInitialPresenceByAccount(long account) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_CONTACTS);
        qb.setProjectionMap(sContactsProjectionMap);

        mQueryContactIdSelectionArgs1[0] = String.valueOf(account);

        final SQLiteDatabase db = getDBHelper().getWritableDatabase();
        db.beginTransaction();

        Cursor c = null;

        try {
            ContentValues presenceValues = new ContentValues();
            presenceValues.put(Imps.Presence.PRESENCE_STATUS, Imps.Presence.OFFLINE);
            presenceValues.put(Imps.Presence.PRESENCE_CUSTOM_STATUS, "");

            // update all the presence for the account so they are offline
            StringBuilder buf = new StringBuilder();
            buf.append(Imps.Presence.CONTACT_ID);
            buf.append(" in (select ");
            buf.append(Imps.Contacts._ID);
            buf.append(" from ");
            buf.append(TABLE_CONTACTS);
            buf.append(" where ");
            buf.append(Imps.Contacts.ACCOUNT);
            buf.append("=?) ");

            String selection = buf.toString();
            if (DBG)
                log("seedInitialPresence: reset presence selection=" + selection);

            int count = db.update(TABLE_PRESENCE, presenceValues, selection,
                    mQueryContactIdSelectionArgs1);
            if (DBG)
                log("seedInitialPresence: reset " + count + " presence rows to OFFLINE");

            // for in-memory presence table, add a presence row for each contact that
            // doesn't have a presence. in-memory presence table isn't reliable, and goes away
            // when device reboot or IMProvider process dies, so we can't rely on each contact
            // have a corresponding presence.
            if (DBG) {
                log("seedInitialPresence: contacts_with_no_presence_selection => "
                    + CONTACTS_WITH_NO_PRESENCE_SELECTION);
            }

            c = qb.query(db, CONTACT_ID_PROJECTION, CONTACTS_WITH_NO_PRESENCE_SELECTION,
                    mQueryContactIdSelectionArgs1, null, null, null, null);

            if (DBG)
                log("seedInitialPresence: found " + c.getCount() + " contacts w/o presence");

            count = 0;

            while (c.moveToNext()) {
                long id = c.getLong(CONTACT_ID_COLUMN);
                presenceValues.put(Imps.Presence.CONTACT_ID, id);

                try {
                    if (db.insert(TABLE_PRESENCE, null, presenceValues) > 0) {
                        count++;
                    }
                } catch (SQLiteConstraintException ex) {
                    // we could possibly catch this exception, since there could be a presence
                    // row with the same contact_id. That's fine, just ignore the error
                    if (DBG)
                        log("seedInitialPresence: insert presence for contact_id " + id
                            + " failed, caught " + ex);
                }
            }

            if (DBG)
                log("seedInitialPresence: added " + count + " new presence rows");

            db.setTransactionSuccessful();
        } finally {
            if (c != null) {
                c.close();
            }
            db.endTransaction();
        }
    }

    private int updateBulkPresence(ContentValues values, String userWhere, String[] whereArgs) {
        ArrayList<String> usernames = getStringArrayList(values, Imps.Contacts.USERNAME);
        int count = usernames.size();
        Long account = values.getAsLong(Imps.Contacts.ACCOUNT);

        ArrayList<String> priorityArray = getStringArrayList(values, Imps.Presence.PRIORITY);
        ArrayList<String> modeArray = getStringArrayList(values, Imps.Presence.PRESENCE_STATUS);
        ArrayList<String> statusArray = getStringArrayList(values,
                Imps.Presence.PRESENCE_CUSTOM_STATUS);
        ArrayList<String> clientTypeArray = getStringArrayList(values, Imps.Presence.CLIENT_TYPE);
        ArrayList<String> resourceArray = getStringArrayList(values, Imps.Presence.JID_RESOURCE);

        // append username to the selection clause
        StringBuilder buf = new StringBuilder();

        if (!TextUtils.isEmpty(userWhere)) {
            buf.append(userWhere);
            buf.append(" AND ");
        }

        buf.append(Imps.Presence.CONTACT_ID);
        buf.append(" in (select ");
        buf.append(Imps.Contacts._ID);
        buf.append(" from ");
        buf.append(TABLE_CONTACTS);
        buf.append(" where ");
        buf.append(Imps.Contacts.ACCOUNT);
        buf.append("=? AND ");

        // use username LIKE ? for case insensitive comparison
        buf.append(Imps.Contacts.USERNAME);
        buf.append(" LIKE ?) AND (");

        buf.append(Imps.Presence.PRIORITY);
        buf.append("<=? OR ");
        buf.append(Imps.Presence.PRIORITY);
        buf.append(" IS NULL OR ");
        buf.append(Imps.Presence.JID_RESOURCE);
        buf.append("=?)");

        String selection = buf.toString();

        if (DBG)
            log("updateBulkPresence: selection => " + selection);

        int numArgs = (whereArgs != null ? whereArgs.length + 4 : 4);
        String[] selectionArgs = new String[numArgs];
        int selArgsIndex = 0;

        if (whereArgs != null) {
            for (selArgsIndex = 0; selArgsIndex < numArgs - 1; selArgsIndex++) {
                selectionArgs[selArgsIndex] = whereArgs[selArgsIndex];
            }
        }

        final SQLiteDatabase db = getDBHelper().getWritableDatabase();

        db.beginTransaction();
        int sum = 0;

        try {
            ContentValues presenceValues = new ContentValues();

            for (int i = 0; i < count; i++) {
                String username = usernames.get(i);
                int priority = 0;
                int mode = 0;
                String status = statusArray.get(i);
                String jidResource = resourceArray == null ? "" : resourceArray.get(i);
                int clientType = Imps.Presence.CLIENT_TYPE_DEFAULT;

                try {
                    if (priorityArray != null) {
                        priority = Integer.parseInt(priorityArray.get(i));
                    }
                    if (modeArray != null) {
                        mode = Integer.parseInt(modeArray.get(i));
                    }
                    if (clientTypeArray != null) {
                        clientType = Integer.parseInt(clientTypeArray.get(i));
                    }
                } catch (NumberFormatException ex) {
                    LogCleaner.error(LOG_TAG, "[ImProvider] updateBulkPresence: caught",ex);
                }

                if (DBG) {
                    log("updateBulkPresence[" + i + "] username=" + username + ", priority="
                        + priority + ", mode=" + mode + ", status=" + status + ", resource="
                        + jidResource + ", clientType=" + clientType);
                }

                if (modeArray != null) {
                    presenceValues.put(Imps.Presence.PRESENCE_STATUS, mode);
                }
                if (priorityArray != null) {
                    presenceValues.put(Imps.Presence.PRIORITY, priority);
                }
                presenceValues.put(Imps.Presence.PRESENCE_CUSTOM_STATUS, status);
                if (clientTypeArray != null) {
                    presenceValues.put(Imps.Presence.CLIENT_TYPE, clientType);
                }

                if (!TextUtils.isEmpty(jidResource)) {
                    presenceValues.put(Imps.Presence.JID_RESOURCE, jidResource);
                }

                // fill in the selection args
                int idx = selArgsIndex;
                selectionArgs[idx++] = String.valueOf(account);
                selectionArgs[idx++] = username;
                selectionArgs[idx++] = String.valueOf(priority);
                selectionArgs[idx] = jidResource;

                int numUpdated = db
                        .update(TABLE_PRESENCE, presenceValues, selection, selectionArgs);
                if (numUpdated == 0) {
                    // this is really generating a lot of log output that doesn't seem necessary
                   // LogCleaner.warn(LOG_TAG, "[ImProvider] updateBulkPresence: failed for " + username);
                } else {
                    sum += numUpdated;
                }

                // yield the lock if anyone else is trying to
                // perform a db operation here.
                db.yieldIfContended();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (DBG)
            log("updateBulkPresence: " + sum + " entries updated");
        return sum;
    }

    private Uri insertInternal(Uri url, ContentValues initialValues) {
        Uri resultUri = null;
        long rowID = 0;
        long account = 0;
        String contact = null;
        long threadId = 0;

        boolean notifyContactListContentUri = false;
        boolean notifyContactContentUri = false;
        boolean notifyMessagesContentUri = false;
        boolean notifyMessagesByContactContentUri = false;
        boolean notifyMessagesByThreadIdContentUri = false;
        boolean notifyProviderAccountContentUri = false;

        final SQLiteDatabase db = getDBHelper().getWritableDatabase();
        int match = mUrlMatcher.match(url);

        if (DBG)
            log("insert to " + url + ", match " + match);
        switch (match) {
        case MATCH_PROVIDERS:
            // Insert into the providers table
            rowID = db.insert(TABLE_PROVIDERS, "name", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Provider.CONTENT_URI + "/" + rowID);
            }
            notifyProviderAccountContentUri = true;
            break;

        case MATCH_ACCOUNTS:
            // Insert into the accounts table
            rowID = db.insert(TABLE_ACCOUNTS, "name", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Account.CONTENT_URI + "/" + rowID);
            }
            notifyProviderAccountContentUri = true;
            break;

        case MATCH_CONTACTS_BY_PROVIDER:
            appendValuesFromUrl(initialValues, url, Imps.Contacts.PROVIDER, Imps.Contacts.ACCOUNT);
            // fall through
        case MATCH_CONTACTS:
        case MATCH_CONTACTS_BAREBONE:
            // Insert into the contacts table
            rowID = db.insert(TABLE_CONTACTS, "username", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Contacts.CONTENT_URI + "/" + rowID);
            }

            notifyContactContentUri = true;
            break;

        case MATCH_CONTACTS_BULK:
            if (insertBulkContacts(initialValues)) {
                // notify change using the "content://im/contacts" url,
                // so the change will be observed by listeners interested
                // in contacts changes.
                resultUri = Imps.Contacts.CONTENT_URI;
            }
            notifyContactContentUri = true;
            break;

        case MATCH_CONTACTLISTS_BY_PROVIDER:
            appendValuesFromUrl(initialValues, url, Imps.ContactList.PROVIDER,
                    Imps.ContactList.ACCOUNT);
            // fall through
        case MATCH_CONTACTLISTS:
            // Insert into the contactList table
            rowID = db.insert(TABLE_CONTACT_LIST, "name", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.ContactList.CONTENT_URI + "/" + rowID);
            }
            notifyContactListContentUri = true;
            break;

        case MATCH_BLOCKEDLIST_BY_PROVIDER:
            appendValuesFromUrl(initialValues, url, Imps.BlockedList.PROVIDER,
                    Imps.BlockedList.ACCOUNT);
            // fall through
        case MATCH_BLOCKEDLIST:
            // Insert into the blockedList table
            rowID = db.insert(TABLE_BLOCKED_LIST, "username", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.BlockedList.CONTENT_URI + "/" + rowID);
            }

            break;

        case MATCH_CONTACTS_ETAGS:
            rowID = db.replace(TABLE_CONTACTS_ETAG, Imps.ContactsEtag.ETAG, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.ContactsEtag.CONTENT_URI + "/" + rowID);
            }
            break;

        case MATCH_MESSAGES_BY_CONTACT:
            String accountStr = decodeURLSegment(url.getPathSegments().get(1));
            try {
                account = Long.parseLong(accountStr);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            contact = decodeURLSegment(url.getPathSegments().get(2));
            initialValues.put(Imps.Messages.THREAD_ID, getContactId(db, accountStr, contact));

            notifyMessagesContentUri = true;

            // Insert into the messages table.
            rowID = db.insert(TABLE_MESSAGES, "thread_id", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Messages.CONTENT_URI + "/" + rowID);
            }

            break;

        case MATCH_MESSAGES_BY_THREAD_ID:
            appendValuesFromUrl(initialValues, url, Imps.Messages.THREAD_ID);
            // fall through

        case MATCH_MESSAGES:
            // Insert into the messages table.
            notifyMessagesContentUri = true;
            rowID = db.insert(TABLE_MESSAGES, "thread_id", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Messages.CONTENT_URI + "/" + rowID);
            }

            break;

        case MATCH_OTR_MESSAGES_BY_CONTACT:
            String accountStr2 = decodeURLSegment(url.getPathSegments().get(1));

            try {
                account = Long.parseLong(accountStr2);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            contact = decodeURLSegment(url.getPathSegments().get(2));
            initialValues.put(Imps.Messages.THREAD_ID, getContactId(db, accountStr2, contact));

            notifyMessagesByContactContentUri = true;

            // Insert into the in-memory messages table.
            rowID = db.insert(TABLE_IN_MEMORY_MESSAGES, "thread_id", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Messages.OTR_MESSAGES_CONTENT_URI + "/" + rowID);
            }

            break;

        case MATCH_OTR_MESSAGES_BY_THREAD_ID:
            try {
                threadId = Long.parseLong(decodeURLSegment(url.getPathSegments().get(1)));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            initialValues.put(Imps.Messages.THREAD_ID, threadId);

            notifyMessagesByThreadIdContentUri = true;
            // fall through

        case MATCH_OTR_MESSAGES:
            // Insert into the messages table.
            rowID = db.insert(TABLE_IN_MEMORY_MESSAGES, "thread_id", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Messages.OTR_MESSAGES_CONTENT_URI + "/" + rowID);
            }

            break;

        case MATCH_INVITATIONS:
            rowID = db.insert(TABLE_INVITATIONS, null, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Invitation.CONTENT_URI + "/" + rowID);
            }
            break;

        case MATCH_GROUP_MEMBERS:
            rowID = db.insert(TABLE_GROUP_MEMBERS, "nickname", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.GroupMembers.CONTENT_URI + "/" + rowID);
            }
            break;

        case MATCH_GROUP_MEMBERS_BY_GROUP:
            appendValuesFromUrl(initialValues, url, Imps.GroupMembers.GROUP);
            rowID = db.insert(TABLE_GROUP_MEMBERS, "nickname", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.GroupMembers.CONTENT_URI + "/" + rowID);
            }
            break;

        case MATCH_AVATAR_BY_PROVIDER:
            appendValuesFromUrl(initialValues, url, Imps.Avatars.PROVIDER, Imps.Avatars.ACCOUNT);
            // fall through
        case MATCH_AVATARS:
            // Insert into the avatars table
            rowID = db.replace(TABLE_AVATARS, "contact", initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Avatars.CONTENT_URI + "/" + rowID);
            }
            break;

        case MATCH_CHATS_ID:
            appendValuesFromUrl(initialValues, url, Imps.Chats.CONTACT_ID);
            // fall through
        case MATCH_CHATS:
            // Insert into the chats table
            initialValues.put(Imps.Chats.SHORTCUT, -1);
            rowID = db.replace(TABLE_CHATS, Imps.Chats.CONTACT_ID, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Chats.CONTENT_URI + "/" + rowID);
                addToQuickSwitch(rowID);
            }
            notifyContactContentUri = true;
            notifyProviderAccountContentUri = true; // For updating account stats in account list
            break;

        case MATCH_PRESENCE:
            rowID = db.replace(TABLE_PRESENCE, null, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.Presence.CONTENT_URI + "/" + rowID);
            }
            notifyContactContentUri = true;
            break;

        case MATCH_PRESENCE_SEED_BY_ACCOUNT:
            try {
                seedInitialPresenceByAccount(Long.parseLong(url.getLastPathSegment()));
                resultUri = Imps.Presence.CONTENT_URI;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }
            break;

        case MATCH_SESSIONS_BY_PROVIDER:
            appendValuesFromUrl(initialValues, url, Imps.SessionCookies.PROVIDER,
                    Imps.SessionCookies.ACCOUNT);
            // fall through
        case MATCH_SESSIONS:
            rowID = db.insert(TABLE_SESSION_COOKIES, null, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.SessionCookies.CONTENT_URI + "/" + rowID);
            }
            break;

        case MATCH_PROVIDER_SETTINGS:
            rowID = db.replace(TABLE_PROVIDER_SETTINGS, null, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.ProviderSettings.CONTENT_URI + "/" + rowID);
            }
            break;

        case MATCH_ACCOUNTS_STATUS:
            rowID = db.replace(TABLE_ACCOUNT_STATUS, null, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.AccountStatus.CONTENT_URI + "/" + rowID);
            }
            notifyProviderAccountContentUri = true;
            break;

        case MATCH_BRANDING_RESOURCE_MAP_CACHE:
            rowID = db.insert(TABLE_BRANDING_RESOURCE_MAP_CACHE, null, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.BrandingResourceMapCache.CONTENT_URI + "/" + rowID);
            }
            break;

        // mcs/rmq stuff
        case MATCH_OUTGOING_RMQ_MESSAGES:
            rowID = db.insert(TABLE_OUTGOING_RMQ_MESSAGES, null, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.OutgoingRmq.CONTENT_URI + "/" + rowID);
            }
            break;

        case MATCH_LAST_RMQ_ID:
            rowID = db.replace(TABLE_LAST_RMQ_ID, null, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.LastRmqId.CONTENT_URI + "/" + rowID);
            }
            break;

        case MATCH_S2D_RMQ_IDS:
            rowID = db.insert(TABLE_S2D_RMQ_IDS, null, initialValues);
            if (rowID > 0) {
                resultUri = Uri.parse(Imps.ServerToDeviceRmqIds.CONTENT_URI + "/" + rowID);
            }
            break;

        default:
            throw new UnsupportedOperationException("Cannot insert into URL: " + url);
        }
        // TODO: notify the data change observer?

        if (resultUri != null) {
            ContentResolver resolver = getContext().getContentResolver();

            // In most case, we query contacts with presence and chats joined, thus
            // we should also notify that contacts changes when presence or chats changed.
            if (notifyContactContentUri) {
                resolver.notifyChange(Imps.Contacts.CONTENT_URI, null);
            }

            if (notifyContactListContentUri) {
                resolver.notifyChange(Imps.ContactList.CONTENT_URI, null);
            }

            if (notifyMessagesContentUri) {
                resolver.notifyChange(Imps.Messages.CONTENT_URI, null);
            }

            if (notifyMessagesByContactContentUri) {
                resolver.notifyChange(Imps.Messages.CONTENT_URI, null);
                resolver.notifyChange(Imps.Messages.getContentUriByContact(account, contact), null);
            }

            if (notifyMessagesByThreadIdContentUri) {
                resolver.notifyChange(Imps.Messages.CONTENT_URI, null);
                resolver.notifyChange(Imps.Messages.getContentUriByThreadId(threadId), null);
            }

            if (notifyProviderAccountContentUri) {
                if (DBG)
                    log("notify insert for " + Imps.Provider.CONTENT_URI_WITH_ACCOUNT);
                resolver.notifyChange(Imps.Provider.CONTENT_URI_WITH_ACCOUNT, null);
            }
        }
        return resultUri;
    }

    private void appendValuesFromUrl(ContentValues values, Uri url, String... columns) {
        if (url.getPathSegments().size() <= columns.length) {
            throw new IllegalArgumentException("Not enough values in url");
        }
        for (int i = 0; i < columns.length; i++) {
            if (values.containsKey(columns[i])) {
                throw new UnsupportedOperationException("Cannot override the value for "
                                                        + columns[i]);
            }
            values.put(columns[i], decodeURLSegment(url.getPathSegments().get(i + 1)));
        }
    }

    private long getContactId(final SQLiteDatabase db, final String accountId, final String contact) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_CONTACTS);
        qb.setProjectionMap(sContactsProjectionMap);

        mQueryContactIdSelectionArgs2[0] = accountId;
        mQueryContactIdSelectionArgs2[1] = contact;

        Cursor c = qb.query(db, CONTACT_ID_PROJECTION, CONTACT_ID_QUERY_SELECTION,
                mQueryContactIdSelectionArgs2, null, null, null, null);

        long contactId = 0;

        try {
            if (c.moveToFirst()) {
                contactId = c.getLong(CONTACT_ID_COLUMN);
            }
        } finally {
            c.close();
        }

        return contactId;
    }

    //  Quick-switch management
    //  The chat UI provides slots (0, 9, .., 1) for the first 10 chats.  This allows you to
    //  quickly switch between these chats by chording menu+#.  We number from the right end of
    //  the number row and move leftward to make an easier two-hand chord with the menu button
    //  on the left side of the keyboard.
    private void addToQuickSwitch(long newRow) {
        //  Since there are fewer than 10, there must be an empty slot.  Let's find it.
        int slot = findEmptyQuickSwitchSlot();

        if (slot == -1) {
            return;
        }

        updateSlotForChat(newRow, slot);
    }

    //  If there are more than 10 chats and one with a quick switch slot ends then pick a chat
    //  that doesn't have a slot and have it inhabit the newly emptied slot.
    private void backfillQuickSwitchSlots() {
        //  Find all the chats without a quick switch slot, and order
        Cursor c = query(Imps.Chats.CONTENT_URI, BACKFILL_PROJECTION, Imps.Chats.SHORTCUT + "=-1",
                null, Imps.Chats.LAST_MESSAGE_DATE + " DESC");

        try {
            if (c.getCount() < 1) {
                return;
            }

            int slot = findEmptyQuickSwitchSlot();

            if (slot != -1) {
                c.moveToFirst();

                long id = c.getLong(c.getColumnIndex(Imps.Chats._ID));

                updateSlotForChat(id, slot);
            }
        } finally {
            c.close();
        }
    }

    private int updateSlotForChat(long chatId, int slot) {
        ContentValues values = new ContentValues();

        values.put(Imps.Chats.SHORTCUT, slot);

        return update(Imps.Chats.CONTENT_URI, values, Imps.Chats._ID + "=?",
                new String[] { Long.toString(chatId) });
    }

    private int findEmptyQuickSwitchSlot() {
        Cursor c = queryInternal(Imps.Chats.CONTENT_URI, FIND_SHORTCUT_PROJECTION, null, null, null);
        final int N = c.getCount();

        try {
            //  If there are 10 or more chats then all the quick switch slots are already filled
            if (N >= 10) {
                return -1;
            }

            int slots = 0;
            int column = c.getColumnIndex(Imps.Chats.SHORTCUT);

            //  The map is here because numbers go from 0-9, but we want to assign slots in
            //  0, 9, 8, ..., 1 order to match the right-to-left reading of the number row
            //  on the keyboard.
            int[] map = new int[] { 0, 9, 8, 7, 6, 5, 4, 3, 2, 1 };

            //  Mark all the slots that are in use
            //  The shortcuts represent actual keyboard number row keys, and not ordinals.
            //  So 7 would mean the shortcut is the 7 key on the keyboard and NOT the 7th
            //  shortcut.  The passing of slot through map[] below maps these keyboard key
            //  shortcuts into an ordinal bit position in the 'slots' bitfield.
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                int slot = c.getInt(column);

                if (slot != -1) {
                    slots |= (1 << map[slot]);
                }
            }

            //  Try to find an empty one
            //  As we exit this, the push of i through map[] maps the ordinal bit position
            //  in the 'slots' bitfield onto a key on the number row of the device keyboard.
            //  The keyboard key is what is used to designate the shortcut.
            for (int i = 0; i < 10; i++) {
                if ((slots & (1 << i)) == 0) {
                    return map[i];
                }
            }

            return -1;
        } finally {
            c.close();
        }
    }

    /** manual trigger for deleting contacts */
    private static final String DELETE_PRESENCE_SELECTION = Imps.Presence.CONTACT_ID
                                                            + " in (select " + PRESENCE_CONTACT_ID
                                                            + " from " + TABLE_PRESENCE
                                                            + " left outer join " + TABLE_CONTACTS
                                                            + " on " + PRESENCE_CONTACT_ID + '='
                                                            + CONTACT_ID + " where " + CONTACT_ID
                                                            + " IS NULL)";

    private static final String CHATS_CONTACT_ID = TABLE_CHATS + '.' + Imps.Chats.CONTACT_ID;
    private static final String DELETE_CHATS_SELECTION = Imps.Chats.CONTACT_ID + " in (select "
                                                         + CHATS_CONTACT_ID + " from "
                                                         + TABLE_CHATS + " left outer join "
                                                         + TABLE_CONTACTS + " on "
                                                         + CHATS_CONTACT_ID + '=' + CONTACT_ID
                                                         + " where " + CONTACT_ID + " IS NULL)";

    private static final String GROUP_MEMBER_ID = TABLE_GROUP_MEMBERS + '.'
                                                  + Imps.GroupMembers.GROUP;
    private static final String DELETE_GROUP_MEMBER_SELECTION = Imps.GroupMembers.GROUP
                                                                + " in (select " + GROUP_MEMBER_ID
                                                                + " from " + TABLE_GROUP_MEMBERS
                                                                + " left outer join "
                                                                + TABLE_CONTACTS + " on "
                                                                + GROUP_MEMBER_ID + '='
                                                                + CONTACT_ID + " where "
                                                                + CONTACT_ID + " IS NULL)";

    private static final String GROUP_MESSAGES_ID = TABLE_MESSAGES + '.' + Imps.Messages.THREAD_ID;
    private static final String DELETE_GROUP_MESSAGES_SELECTION = Imps.Messages.THREAD_ID
                                                                  + " in (select "
                                                                  + GROUP_MESSAGES_ID + " from "
                                                                  + TABLE_MESSAGES
                                                                  + " left outer join "
                                                                  + TABLE_CONTACTS + " on "
                                                                  + GROUP_MESSAGES_ID + '='
                                                                  + CONTACT_ID + " where "
                                                                  + CONTACT_ID + " IS NULL)";

    private void performContactRemovalCleanup(long contactId) {
        final SQLiteDatabase db = getDBHelper().getWritableDatabase();

        if (contactId > 0) {
            StringBuilder buf = new StringBuilder();

            // delete presence
            buf.append(Imps.Presence.CONTACT_ID).append('=').append(contactId);
            deleteWithSelection(db, TABLE_PRESENCE, buf.toString(), null);

            // delete group memebers
            buf.delete(0, buf.length());
            buf.append(Imps.GroupMembers.GROUP).append('=').append(contactId);
            deleteWithSelection(db, TABLE_GROUP_MEMBERS, buf.toString(), null);
        } else {
            // delete presence
            deleteWithSelection(db, TABLE_PRESENCE, DELETE_PRESENCE_SELECTION, null);

            // delete group members
            deleteWithSelection(db, TABLE_GROUP_MEMBERS, DELETE_GROUP_MEMBER_SELECTION, null);
        }
    }

    private void deleteWithSelection(SQLiteDatabase db, String tableName, String selection,
            String[] selectionArgs) {
        if (DBG)
            log("deleteWithSelection: table " + tableName + ", selection => " + selection);
        int count = db.delete(tableName, selection, selectionArgs);
        if (DBG)
            log("deleteWithSelection: deleted " + count + " rows");
    }

    private String buildContactIdSelection(String columnName, String contactSelection) {
        StringBuilder buf = new StringBuilder();

        buf.append(columnName);
        buf.append(" in (select ");
        buf.append(Imps.Contacts._ID);
        buf.append(" from ");
        buf.append(TABLE_CONTACTS);
        buf.append(" where ");
        buf.append(contactSelection);
        buf.append(")");

        return buf.toString();
    }

    private int deleteInternal(Uri url, String userWhere, String[] whereArgs) {
        String tableToChange;

        // In some cases a given url requires that we delete rows from more than one
        // table.  The motivating example is deleting messages from both the on disk
        // and in memory messages tables.
        String tableToChange2 = null;
        String idColumnName = null;
        String changedItemId = null;
        String provider = null;
        String accountStr = null;
        long account = 0;
        String contact = null;
        long threadId = 0;

        StringBuilder whereClause = new StringBuilder();
        if (userWhere != null) {
            whereClause.append(userWhere);
        }

        boolean notifyMessagesContentUri = false;
        boolean notifyMessagesByContactContentUri = false;
        boolean notifyMessagesByThreadIdContentUri = false;
        boolean notifyContactListContentUri = false;
        boolean notifyProviderAccountContentUri = false;
        int match = mUrlMatcher.match(url);

        boolean contactDeleted = false;
        long deletedContactId = 0;

        boolean backfillQuickSwitchSlots = false;

        final SQLiteDatabase db = getDBHelper().getWritableDatabase();

        switch (match) {
        case MATCH_PROVIDERS_BY_ID:
            changedItemId = url.getPathSegments().get(1);
            // fall through
        case MATCH_PROVIDERS:
            tableToChange = TABLE_PROVIDERS;
            notifyProviderAccountContentUri = true;
            break;

        case MATCH_ACCOUNTS_BY_ID:
            changedItemId = url.getPathSegments().get(1);
            // fall through
        case MATCH_ACCOUNTS:
            tableToChange = TABLE_ACCOUNTS;
            notifyProviderAccountContentUri = true;
            break;

        case MATCH_ACCOUNT_STATUS:
            changedItemId = url.getPathSegments().get(1);
            // fall through
        case MATCH_ACCOUNTS_STATUS:
            tableToChange = TABLE_ACCOUNT_STATUS;
            notifyProviderAccountContentUri = true;
            break;

        case MATCH_CONTACTS:
        case MATCH_CONTACTS_BAREBONE:
            tableToChange = TABLE_CONTACTS;
            contactDeleted = true;
            break;

        case MATCH_CONTACT:
            tableToChange = TABLE_CONTACTS;
            changedItemId = url.getPathSegments().get(1);

            try {
                deletedContactId = Long.parseLong(changedItemId);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            contactDeleted = true;
            break;

        case MATCH_CONTACTS_BY_PROVIDER:
            tableToChange = TABLE_CONTACTS;
            appendWhere(whereClause, Imps.Contacts.ACCOUNT, "=", url.getPathSegments().get(2));
            contactDeleted = true;
            break;

        case MATCH_CONTACTLISTS_BY_PROVIDER:
            appendWhere(whereClause, Imps.ContactList.ACCOUNT, "=", url.getPathSegments().get(2));
            // fall through
        case MATCH_CONTACTLISTS:
            tableToChange = TABLE_CONTACT_LIST;
            notifyContactListContentUri = true;
            break;

        case MATCH_CONTACTLIST:
            tableToChange = TABLE_CONTACT_LIST;
            changedItemId = url.getPathSegments().get(1);
            break;

        case MATCH_BLOCKEDLIST:
            tableToChange = TABLE_BLOCKED_LIST;
            break;

        case MATCH_BLOCKEDLIST_BY_PROVIDER:
            tableToChange = TABLE_BLOCKED_LIST;
            appendWhere(whereClause, Imps.BlockedList.ACCOUNT, "=", url.getPathSegments().get(2));
            break;

        case MATCH_CONTACTS_ETAGS:
            tableToChange = TABLE_CONTACTS_ETAG;
            break;

        case MATCH_CONTACTS_ETAG:
            tableToChange = TABLE_CONTACTS_ETAG;
            changedItemId = url.getPathSegments().get(1);
            break;

        case MATCH_MESSAGES:
            tableToChange = TABLE_MESSAGES;
            break;

        case MATCH_MESSAGES_BY_CONTACT:
            tableToChange = TABLE_MESSAGES;
            tableToChange2 = TABLE_IN_MEMORY_MESSAGES;

            accountStr = decodeURLSegment(url.getPathSegments().get(1));
            try {
                account = Long.parseLong(accountStr);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            contact = decodeURLSegment(url.getPathSegments().get(2));
            appendWhere(whereClause, Imps.Messages.THREAD_ID, "=",
                    getContactId(db, accountStr, contact));

            notifyMessagesContentUri = true;
            break;

        case MATCH_MESSAGES_BY_THREAD_ID:
            tableToChange = TABLE_MESSAGES;
            tableToChange2 = TABLE_IN_MEMORY_MESSAGES;

            try {
                threadId = Long.parseLong(decodeURLSegment(url.getPathSegments().get(1)));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            appendWhere(whereClause, Imps.Messages.THREAD_ID, "=", threadId);

            notifyMessagesContentUri = true;
            break;

        case MATCH_MESSAGES_BY_PROVIDER:
            tableToChange = TABLE_MESSAGES;

            provider = decodeURLSegment(url.getPathSegments().get(1));
            appendWhere(
                    whereClause,
                    buildContactIdSelection(Imps.Messages.THREAD_ID, Imps.Contacts.PROVIDER + "='"
                                                                     + provider + "'"));

            notifyMessagesContentUri = true;
            break;

        case MATCH_MESSAGES_BY_ACCOUNT:
            tableToChange = TABLE_MESSAGES;

            accountStr = decodeURLSegment(url.getPathSegments().get(1));
            appendWhere(
                    whereClause,
                    buildContactIdSelection(Imps.Messages.THREAD_ID, Imps.Contacts.ACCOUNT + "='"
                                                                     + accountStr + "'"));

            notifyMessagesContentUri = true;
            break;

        case MATCH_MESSAGE:
            tableToChange = TABLE_MESSAGES;
            changedItemId = url.getPathSegments().get(1);
            notifyMessagesContentUri = true;
            break;

        case MATCH_OTR_MESSAGES:
            tableToChange = TABLE_IN_MEMORY_MESSAGES;
            break;

        case MATCH_OTR_MESSAGES_BY_CONTACT:
            tableToChange = TABLE_IN_MEMORY_MESSAGES;

            accountStr = decodeURLSegment(url.getPathSegments().get(1));
            try {
                account = Long.parseLong(accountStr);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            contact = decodeURLSegment(url.getPathSegments().get(2));
            appendWhere(whereClause, Imps.Messages.THREAD_ID, "=",
                    getContactId(db, accountStr, contact));

            notifyMessagesByContactContentUri = true;
            break;

        case MATCH_OTR_MESSAGES_BY_THREAD_ID:
            tableToChange = TABLE_IN_MEMORY_MESSAGES;

            try {
                threadId = Long.parseLong(decodeURLSegment(url.getPathSegments().get(1)));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            appendWhere(whereClause, Imps.Messages.THREAD_ID, "=", threadId);

            notifyMessagesByThreadIdContentUri = true;
            break;

        case MATCH_OTR_MESSAGES_BY_PROVIDER:
            tableToChange = TABLE_IN_MEMORY_MESSAGES;

            provider = decodeURLSegment(url.getPathSegments().get(1));
            appendWhere(
                    whereClause,
                    buildContactIdSelection(Imps.Messages.THREAD_ID, Imps.Contacts.PROVIDER + "='"
                                                                     + provider + "'"));

            if (DBG)
                log("delete (MATCH_OTR_MESSAGES_BY_PROVIDER) sel => " + whereClause);
            notifyMessagesContentUri = true;
            break;

        case MATCH_OTR_MESSAGES_BY_ACCOUNT:
            tableToChange = TABLE_IN_MEMORY_MESSAGES;

            accountStr = decodeURLSegment(url.getPathSegments().get(1));
            appendWhere(
                    whereClause,
                    buildContactIdSelection(Imps.Messages.THREAD_ID, Imps.Contacts.ACCOUNT + "='"
                                                                     + accountStr + "'"));

            if (DBG)
                log("delete (MATCH_OTR_MESSAGES_BY_ACCOUNT) sel => " + whereClause);
            notifyMessagesContentUri = true;
            break;

        case MATCH_OTR_MESSAGE:
            tableToChange = TABLE_IN_MEMORY_MESSAGES;
            changedItemId = url.getPathSegments().get(1);
            notifyMessagesContentUri = true;
            break;

        case MATCH_GROUP_MEMBERS:
            tableToChange = TABLE_GROUP_MEMBERS;
            break;

        case MATCH_GROUP_MEMBERS_BY_GROUP:
            tableToChange = TABLE_GROUP_MEMBERS;
            appendWhere(whereClause, Imps.GroupMembers.GROUP, "=", url.getPathSegments().get(1));
            break;

        case MATCH_INVITATIONS:
            tableToChange = TABLE_INVITATIONS;
            break;

        case MATCH_INVITATION:
            tableToChange = TABLE_INVITATIONS;
            changedItemId = url.getPathSegments().get(1);
            break;

        case MATCH_AVATARS:
            tableToChange = TABLE_AVATARS;
            break;

        case MATCH_AVATAR:
            tableToChange = TABLE_AVATARS;
            changedItemId = url.getPathSegments().get(1);
            break;

        case MATCH_AVATAR_BY_PROVIDER:
            tableToChange = TABLE_AVATARS;
            changedItemId = url.getPathSegments().get(2);
            idColumnName = Imps.Avatars.ACCOUNT;
            break;

        case MATCH_CHATS:
            tableToChange = TABLE_CHATS;
            backfillQuickSwitchSlots = true;
            notifyProviderAccountContentUri = true; // For updating account stats in account list
            break;

        case MATCH_CHATS_BY_ACCOUNT:
            tableToChange = TABLE_CHATS;

            accountStr = decodeURLSegment(url.getLastPathSegment());
            appendWhere(
                    whereClause,
                    buildContactIdSelection(Imps.Chats.CONTACT_ID, Imps.Contacts.ACCOUNT + "='"
                                                                   + accountStr + "'"));

            if (DBG)
                log("delete (MATCH_CHATS_BY_ACCOUNT) sel => " + whereClause);

            changedItemId = null;
            notifyProviderAccountContentUri = true; // For updating account stats in account list
            break;

        case MATCH_CHATS_ID:
            tableToChange = TABLE_CHATS;
            changedItemId = url.getPathSegments().get(1);
            idColumnName = Imps.Chats.CONTACT_ID;
            notifyProviderAccountContentUri = true; // For updating account stats in account list
            break;

        case MATCH_PRESENCE:
            tableToChange = TABLE_PRESENCE;
            break;

        case MATCH_PRESENCE_ID:
            tableToChange = TABLE_PRESENCE;
            changedItemId = url.getPathSegments().get(1);
            idColumnName = Imps.Presence.CONTACT_ID;
            break;

        case MATCH_PRESENCE_BY_ACCOUNT:
            tableToChange = TABLE_PRESENCE;

            accountStr = decodeURLSegment(url.getLastPathSegment());
            appendWhere(
                    whereClause,
                    buildContactIdSelection(Imps.Presence.CONTACT_ID, Imps.Contacts.ACCOUNT + "='"
                                                                      + accountStr + "'"));

            if (DBG)
                log("delete (MATCH_PRESENCE_BY_ACCOUNT): sel => " + whereClause);
            changedItemId = null;
            break;

        case MATCH_SESSIONS:
            tableToChange = TABLE_SESSION_COOKIES;
            break;

        case MATCH_SESSIONS_BY_PROVIDER:
            tableToChange = TABLE_SESSION_COOKIES;
            changedItemId = url.getPathSegments().get(2);
            idColumnName = Imps.SessionCookies.ACCOUNT;
            break;

        case MATCH_PROVIDER_SETTINGS_BY_ID:
            tableToChange = TABLE_PROVIDER_SETTINGS;
            changedItemId = url.getPathSegments().get(1);
            idColumnName = Imps.ProviderSettings.PROVIDER;
            break;

        case MATCH_PROVIDER_SETTINGS_BY_ID_AND_NAME:
            tableToChange = TABLE_PROVIDER_SETTINGS;

            String providerId = url.getPathSegments().get(1);
            String name = url.getPathSegments().get(2);

            appendWhere(whereClause, Imps.ProviderSettings.PROVIDER, "=", providerId);
            appendWhere(whereClause, Imps.ProviderSettings.NAME, "=", name);
            break;

        case MATCH_BRANDING_RESOURCE_MAP_CACHE:
            tableToChange = TABLE_BRANDING_RESOURCE_MAP_CACHE;
            break;

        // mcs/rmq stuff
        case MATCH_OUTGOING_RMQ_MESSAGES:
            tableToChange = TABLE_OUTGOING_RMQ_MESSAGES;
            break;

        case MATCH_LAST_RMQ_ID:
            tableToChange = TABLE_LAST_RMQ_ID;
            break;

        case MATCH_S2D_RMQ_IDS:
            tableToChange = TABLE_S2D_RMQ_IDS;
            break;

        default:
            throw new UnsupportedOperationException("Cannot delete that URL: " + url);
        }

        if (idColumnName == null) {
            idColumnName = "_id";
        }

        if (changedItemId != null) {
            appendWhere(whereClause, idColumnName, "=", changedItemId);
        }

        if (DBG)
            log("delete from " + url + " WHERE  " + whereClause);

        int count = db.delete(tableToChange, whereClause.toString(), whereArgs);

        // see the comment at the declaration of tableToChange2 for an explanation
        if (tableToChange2 != null) {
            count += db.delete(tableToChange2, whereClause.toString(), whereArgs);
        }

        if (contactDeleted && count > 0) {
            // since the contact cleanup triggers no longer work for cross database tables,
            // we have to do it by hand here.
            performContactRemovalCleanup(deletedContactId);
        }

        if (count > 0) {
            ContentResolver resolver = getContext().getContentResolver();

            // In most case, we query contacts with presence and chats joined, thus
            // we should also notify that contacts changes when presence or chats changed.
            if (match == MATCH_CHATS || match == MATCH_CHATS_ID || match == MATCH_PRESENCE
                || match == MATCH_PRESENCE_ID || match == MATCH_CONTACTS_BAREBONE) {
                resolver.notifyChange(Imps.Contacts.CONTENT_URI, null);
            }

            if (notifyMessagesContentUri) {
                resolver.notifyChange(Imps.Messages.CONTENT_URI, null);
            }

            if (notifyMessagesByContactContentUri) {
                resolver.notifyChange(Imps.Messages.CONTENT_URI, null);
                resolver.notifyChange(Imps.Messages.getContentUriByContact(account, contact), null);
            }

            if (notifyMessagesByThreadIdContentUri) {
                resolver.notifyChange(Imps.Messages.CONTENT_URI, null);
                resolver.notifyChange(Imps.Messages.getContentUriByThreadId(threadId), null);
            }

            if (notifyContactListContentUri) {
                resolver.notifyChange(Imps.ContactList.CONTENT_URI, null);
            }

            if (notifyProviderAccountContentUri) {
                if (DBG)
                    log("notify delete for " + Imps.Provider.CONTENT_URI_WITH_ACCOUNT);
                resolver.notifyChange(Imps.Provider.CONTENT_URI_WITH_ACCOUNT, null);
            }

            if (backfillQuickSwitchSlots) {
                backfillQuickSwitchSlots();
            }
        }

        return count;
    }

    private int updateInternal(Uri url, ContentValues values, String userWhere, String[] whereArgs) {
        String tableToChange;
        String idColumnName = null;
        String changedItemId = null;
        String accountStr = null;
        long account = 0;
        String contact = null;
        String packetId = null;
        long threadId = 0;
        int count = 0;

        StringBuilder whereClause = new StringBuilder();
        if (userWhere != null) {
            whereClause.append(userWhere);
        }

        boolean notifyMessagesContentUri = false;
        boolean notifyMessagesByContactContentUri = false;
        boolean notifyMessagesByThreadIdContentUri = false;
        boolean notifyContactListContentUri = false;
        boolean notifyProviderAccountContentUri = false;

        int match = mUrlMatcher.match(url);
        final SQLiteDatabase db = getDBHelper().getWritableDatabase();

        switch (match) {
        case MATCH_PROVIDERS_BY_ID:
            changedItemId = url.getPathSegments().get(1);
            // fall through
        case MATCH_PROVIDERS:
            tableToChange = TABLE_PROVIDERS;
            break;

        case MATCH_ACCOUNTS_BY_ID:
            changedItemId = url.getPathSegments().get(1);
            // fall through
        case MATCH_ACCOUNTS:
            tableToChange = TABLE_ACCOUNTS;
            notifyProviderAccountContentUri = true;
            break;

        case MATCH_ACCOUNT_STATUS:
            changedItemId = url.getPathSegments().get(1);
            // fall through
        case MATCH_ACCOUNTS_STATUS:
            tableToChange = TABLE_ACCOUNT_STATUS;
            notifyProviderAccountContentUri = true;
            break;

        case MATCH_CONTACTS:
        case MATCH_CONTACTS_BAREBONE:
            tableToChange = TABLE_CONTACTS;
            break;

        case MATCH_CONTACTS_BY_PROVIDER:
            tableToChange = TABLE_CONTACTS;
            changedItemId = url.getPathSegments().get(2);
            idColumnName = Imps.Contacts.ACCOUNT;
            break;

        case MATCH_CONTACT:
            tableToChange = TABLE_CONTACTS;
            changedItemId = url.getPathSegments().get(1);
            break;

        case MATCH_CONTACTS_BULK:
            count = updateBulkContacts(values, userWhere);
            // notify change using the "content://im/contacts" url,
            // so the change will be observed by listeners interested
            // in contacts changes.
            if (count > 0) {
                getContext().getContentResolver().notifyChange(Imps.Contacts.CONTENT_URI, null);
            }
            return count;

        case MATCH_CONTACTLIST:
            tableToChange = TABLE_CONTACT_LIST;
            changedItemId = url.getPathSegments().get(1);
            notifyContactListContentUri = true;
            break;

        case MATCH_CONTACTS_ETAGS:
            tableToChange = TABLE_CONTACTS_ETAG;
            break;

        case MATCH_CONTACTS_ETAG:
            tableToChange = TABLE_CONTACTS_ETAG;
            changedItemId = url.getPathSegments().get(1);
            break;

        case MATCH_MESSAGES:
            tableToChange = TABLE_MESSAGES;
            break;

        case MATCH_MESSAGES_BY_CONTACT:
            tableToChange = TABLE_MESSAGES;

            accountStr = decodeURLSegment(url.getPathSegments().get(1));
            try {
                account = Long.parseLong(accountStr);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            contact = decodeURLSegment(url.getPathSegments().get(2));
            appendWhere(whereClause, Imps.Messages.THREAD_ID, "=",
                    getContactId(db, accountStr, contact));

            notifyMessagesContentUri = true;
            break;

        case MATCH_MESSAGES_BY_THREAD_ID:
            tableToChange = TABLE_MESSAGES;

            try {
                threadId = Long.parseLong(decodeURLSegment(url.getPathSegments().get(1)));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            appendWhere(whereClause, Imps.Messages.THREAD_ID, "=", threadId);

            notifyMessagesContentUri = true;
            break;

        case MATCH_MESSAGE:
            tableToChange = TABLE_MESSAGES;
            changedItemId = url.getPathSegments().get(1);
            notifyMessagesContentUri = true;
            break;

        case MATCH_OTR_MESSAGES:
            tableToChange = TABLE_IN_MEMORY_MESSAGES;
            break;

        case MATCH_OTR_MESSAGES_BY_CONTACT:
            tableToChange = TABLE_IN_MEMORY_MESSAGES;

            accountStr = decodeURLSegment(url.getPathSegments().get(1));
            try {
                account = Long.parseLong(accountStr);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            contact = decodeURLSegment(url.getPathSegments().get(2));
            appendWhere(whereClause, Imps.Messages.THREAD_ID, "=",
                    getContactId(db, accountStr, contact));

            notifyMessagesByContactContentUri = true;
            break;

        case MATCH_OTR_MESSAGES_BY_THREAD_ID:
            tableToChange = TABLE_IN_MEMORY_MESSAGES;

            try {
                threadId = Long.parseLong(decodeURLSegment(url.getPathSegments().get(1)));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            appendWhere(whereClause, Imps.Messages.THREAD_ID, "=", threadId);

            notifyMessagesByThreadIdContentUri = true;
            break;

        case MATCH_OTR_MESSAGES_BY_PACKET_ID:
            packetId = decodeURLSegment(url.getPathSegments().get(1));
            tableToChange = TABLE_MESSAGES; // FIXME these should be going to memory but they do not
            appendWhere(whereClause, Imps.Messages.PACKET_ID, "=", packetId);
            notifyMessagesContentUri = true;

            // Try updating OTR message
            count += db.update(TABLE_IN_MEMORY_MESSAGES, values, whereClause.toString(), whereArgs);
            break;

        case MATCH_OTR_MESSAGE:
            tableToChange = TABLE_IN_MEMORY_MESSAGES;
            changedItemId = url.getPathSegments().get(1);
            notifyMessagesContentUri = true;
            break;

        case MATCH_AVATARS:
            tableToChange = TABLE_AVATARS;
            break;

        case MATCH_AVATAR:
            tableToChange = TABLE_AVATARS;
            changedItemId = url.getPathSegments().get(1);
            break;

        case MATCH_AVATAR_BY_PROVIDER:
            tableToChange = TABLE_AVATARS;
            changedItemId = url.getPathSegments().get(2);
            idColumnName = Imps.Avatars.ACCOUNT;
            break;

        case MATCH_CHATS:
            tableToChange = TABLE_CHATS;
            notifyProviderAccountContentUri = true; // For updating account stats in account list
            break;

        case MATCH_CHATS_ID:
            tableToChange = TABLE_CHATS;
            changedItemId = url.getPathSegments().get(1);
            idColumnName = Imps.Chats.CONTACT_ID;
            notifyProviderAccountContentUri = true; // For updating account stats in account list
            break;

        case MATCH_PRESENCE:
            //if (DBG) log("update presence: where='" + userWhere + "'");
            tableToChange = TABLE_PRESENCE;
            break;

        case MATCH_PRESENCE_ID:
            tableToChange = TABLE_PRESENCE;
            changedItemId = url.getPathSegments().get(1);
            idColumnName = Imps.Presence.CONTACT_ID;
            break;

        case MATCH_PRESENCE_BULK:
            count = updateBulkPresence(values, userWhere, whereArgs);
            // notify change using the "content://im/contacts" url,
            // so the change will be observed by listeners interested
            // in contacts changes.
            if (count > 0) {
                getContext().getContentResolver().notifyChange(Imps.Contacts.CONTENT_URI, null);
            }

            return count;

        case MATCH_INVITATION:
            tableToChange = TABLE_INVITATIONS;
            changedItemId = url.getPathSegments().get(1);
            break;

        case MATCH_SESSIONS:
            tableToChange = TABLE_SESSION_COOKIES;
            break;

        case MATCH_PROVIDER_SETTINGS_BY_ID_AND_NAME:
            tableToChange = TABLE_PROVIDER_SETTINGS;

            String providerId = url.getPathSegments().get(1);
            String name = url.getPathSegments().get(2);

            if (values.containsKey(Imps.ProviderSettings.PROVIDER)
                || values.containsKey(Imps.ProviderSettings.NAME)) {
                throw new SecurityException("Cannot override the value for provider|name");
            }

            appendWhere(whereClause, Imps.ProviderSettings.PROVIDER, "=", providerId);
            appendWhere(whereClause, Imps.ProviderSettings.NAME, "=", name);

            break;

        case MATCH_OUTGOING_RMQ_MESSAGES:
            tableToChange = TABLE_OUTGOING_RMQ_MESSAGES;
            break;

        case MATCH_LAST_RMQ_ID:
            tableToChange = TABLE_LAST_RMQ_ID;
            break;

        case MATCH_S2D_RMQ_IDS:
            tableToChange = TABLE_S2D_RMQ_IDS;
            break;

        default:
            throw new UnsupportedOperationException("Cannot update URL: " + url);
        }

        if (idColumnName == null) {
            idColumnName = "_id";
        }
        if (changedItemId != null) {
            appendWhere(whereClause, idColumnName, "=", changedItemId);
        }

        if (DBG)
            log("update " + url + " WHERE " + whereClause);

        count += db.update(tableToChange, values, whereClause.toString(), whereArgs);

        if (count > 0) {
            ContentResolver resolver = getContext().getContentResolver();

            // In most case, we query contacts with presence and chats joined, thus
            // we should also notify that contacts changes when presence or chats changed.
            if (match == MATCH_CHATS || match == MATCH_CHATS_ID || match == MATCH_PRESENCE
                || match == MATCH_PRESENCE_ID || match == MATCH_CONTACTS_BAREBONE) {
                resolver.notifyChange(Imps.Contacts.CONTENT_URI, null);
            }

            if (notifyMessagesContentUri) {
                if (DBG)
                    log("notify change for " + Imps.Messages.CONTENT_URI);
                resolver.notifyChange(Imps.Messages.CONTENT_URI, null);
            }

            if (notifyMessagesByContactContentUri) {
                resolver.notifyChange(Imps.Messages.CONTENT_URI, null);
                resolver.notifyChange(Imps.Messages.getContentUriByContact(account, contact), null);
            }

            if (notifyMessagesByThreadIdContentUri) {
                resolver.notifyChange(Imps.Messages.CONTENT_URI, null);
                resolver.notifyChange(Imps.Messages.getContentUriByThreadId(threadId), null);
            }

            if (notifyContactListContentUri) {
                resolver.notifyChange(Imps.ContactList.CONTENT_URI, null);
            }

            if (notifyProviderAccountContentUri) {
                if (DBG)
                    log("notify change for " + Imps.Provider.CONTENT_URI_WITH_ACCOUNT);
                resolver.notifyChange(Imps.Provider.CONTENT_URI_WITH_ACCOUNT, null);
            }
        }

        return count;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return openFileHelper(uri, mode);
    }

    private static void appendWhere(StringBuilder where, String columnName, String condition,
            Object value) {
        if (where.length() > 0) {
            where.append(" AND ");
        }
        where.append(columnName).append(condition);
        if (value != null) {
            DatabaseUtils.appendValueToSql(where, value);
        }
    }

    private static void appendWhere(StringBuilder where, String clause) {
        if (where.length() > 0) {
            where.append(" AND ");
        }
        where.append(clause);
    }

    private static String decodeURLSegment(String segment) {
        try {
            return URLDecoder.decode(segment, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // impossible
            return segment;
        }
    }

    static void log(String message) {
           LogCleaner.debug(LOG_TAG, message);
    }

}
