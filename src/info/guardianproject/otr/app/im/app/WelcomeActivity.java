/*
 * Copyright (C) 2008 The Android Open Source Project
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

package info.guardianproject.otr.app.im.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import info.guardianproject.cacheword.CacheWordActivityHandler;
import info.guardianproject.cacheword.CacheWordService;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.otr.OtrAndroidKeyManagerImpl;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.SQLCipherOpenHelper;

import java.io.File;

import net.hockeyapp.android.UpdateManager;

public class WelcomeActivity extends ThemeableActivity implements ICacheWordSubscriber  {

    private static final String TAG = "WelcomeActivity";
    private Cursor mProviderCursor;
    private ImApp mApp;
    private SimpleAlertHandler mHandler;
    private SignInHelper mSignInHelper;

    private boolean mDoSignIn = true;

    private ProgressDialog dialog;

    static final String[] PROVIDER_PROJECTION = { Imps.Provider._ID, Imps.Provider.NAME,
                                                 Imps.Provider.FULLNAME, Imps.Provider.CATEGORY,
                                                 Imps.Provider.ACTIVE_ACCOUNT_ID,
                                                 Imps.Provider.ACTIVE_ACCOUNT_USERNAME,
                                                 Imps.Provider.ACTIVE_ACCOUNT_PW,
                                                 Imps.Provider.ACTIVE_ACCOUNT_LOCKED,
                                                 Imps.Provider.ACTIVE_ACCOUNT_KEEP_SIGNED_IN,
                                                 Imps.Provider.ACCOUNT_PRESENCE_STATUS,
                                                 Imps.Provider.ACCOUNT_CONNECTION_STATUS, };

    static final int PROVIDER_ID_COLUMN = 0;
    static final int PROVIDER_NAME_COLUMN = 1;
    static final int PROVIDER_FULLNAME_COLUMN = 2;
    static final int PROVIDER_CATEGORY_COLUMN = 3;
    static final int ACTIVE_ACCOUNT_ID_COLUMN = 4;
    static final int ACTIVE_ACCOUNT_USERNAME_COLUMN = 5;
    static final int ACTIVE_ACCOUNT_PW_COLUMN = 6;
    static final int ACTIVE_ACCOUNT_LOCKED = 7;
    static final int ACTIVE_ACCOUNT_KEEP_SIGNED_IN = 8;
    static final int ACCOUNT_PRESENCE_STATUS = 9;
    static final int ACCOUNT_CONNECTION_STATUS = 10;

    private CacheWordActivityHandler mCacheWord = null;
    private boolean mDoLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApp = (ImApp)getApplication();
        mHandler = new MyHandler(this);

        mSignInHelper = new SignInHelper(this);

        Intent intent = getIntent();
        mDoSignIn = intent.getBooleanExtra("doSignIn", true);
        mDoLock = intent.getBooleanExtra("doLock", false);

        if (!mDoLock)
        {
            if (checkMediaStoreFile()) {
                return;
            }

            mApp.maybeInit(this);
        }

        if (ImApp.mUsingCacheword)
            connectToCacheWord();
        else
        {
           if (openEncryptedStores(null, false)) {
               try
               {
                   ChatFileStore.initWithoutPassword(this);
               }
               catch (Exception e)
               {
                   Log.d(ImApp.LOG_TAG,"unable to mount VFS store"); //but let's not crash the whole app right now
               }
           } else {
               connectToCacheWord(); //first time setup
           }
        }

        // if we have an incoming contact, send it to the right place
        String scheme = intent.getScheme();
        if(TextUtils.equals(scheme, "xmpp"))
        {
            intent.setClass(this, AddContactActivity.class);
            startActivity(intent);
            finish();
            return;
        }
    }

    private void connectToCacheWord ()
    {

        mCacheWord = new CacheWordActivityHandler(this, (ICacheWordSubscriber)this);

        mCacheWord.connectToService();


    }



    @SuppressWarnings("deprecation")
    private boolean cursorUnlocked(String pKey, boolean allowCreate) {
        try {
            Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;

            Builder builder = uri.buildUpon();
            if (pKey != null)
                builder.appendQueryParameter(ImApp.CACHEWORD_PASSWORD_KEY, pKey);
            if (!allowCreate)
                builder = builder.appendQueryParameter(ImApp.NO_CREATE_KEY, "1");
            uri = builder.build();

            mProviderCursor = managedQuery(uri,
                    PROVIDER_PROJECTION, Imps.Provider.CATEGORY + "=?" /* selection */,
                    new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                    Imps.Provider.DEFAULT_SORT_ORDER);

            if (mProviderCursor != null)
            {
                ImPluginHelper.getInstance(this).loadAvailablePlugins();

                mProviderCursor.moveToFirst();

                return true;
            }
            else
            {
                return false;
            }

        } catch (Exception e) {
            // Only complain if we thought this password should succeed
            if (allowCreate) {
                Log.e(ImApp.LOG_TAG, e.getMessage(), e);

                Toast.makeText(this, getString(R.string.error_welcome_database), Toast.LENGTH_LONG).show();
                finish();
            }

            // needs to be unlocked
            return false;
        }
    }

//    private void initCursor(String dbKey) {
//
//        mProviderCursor = managedQuery(Imps.Provider.CONTENT_URI_WITH_ACCOUNT, PROVIDER_PROJECTION,
//                Imps.Provider.CATEGORY + "=?" /* selection */,
//                new String[] { ImApp.IMPS_CATEGORY } /* selection args */, null);
//        doOnResume();
//    }

    @Override
    protected void onPause() {
        if (mHandler != null)
            mHandler.unregisterForBroadcastEvents();

        super.onPause();
        if (mCacheWord != null)
            mCacheWord.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCacheWord != null)
            mCacheWord.disconnect();

        if (dialog != null)
            dialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCacheWord != null)
            mCacheWord.onResume();
    }

    private void doOnResume() {
        mHandler.registerForBroadcastEvents();

        int countAvailable = accountsAvailable();

        if (countAvailable == 1) {
            // If just one account is available for auto-signin, go there immediately after service starts trying
            // to connect.
            mSignInHelper.setSignInListener(new SignInHelper.SignInListener() {
                @Override
                public void connectedToService() {
                }
                @Override
                public void stateChanged(int state, long accountId) {
                    if (state == ImConnection.LOGGING_IN) {
                        mSignInHelper.goToAccount(accountId);
                    }
                }
            });
        } else {
            mSignInHelper.setSignInListener(null);
        }

        Intent intent = getIntent();

        if (intent != null && intent.getAction() != null && (!intent.getAction().equals(Intent.ACTION_MAIN)))
        {
            handleIntentAPILaunch(intent);
        }
        else
        {
            if (countAvailable > 0 && mDoSignIn && mProviderCursor.moveToFirst()) {
                do {
                    if (!mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
                        int state = mProviderCursor.getInt(ACCOUNT_CONNECTION_STATUS);
                        long accountId = mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
                        if (mProviderCursor.getInt(ACTIVE_ACCOUNT_KEEP_SIGNED_IN) != 0) {
                            signIn(accountId);
                        }
                    }
                } while (mProviderCursor.moveToNext());
            }
            startActivity(new Intent(getBaseContext(), NewChatActivity.class));
            finish();
        }
    }

    private void signIn(long accountId) {
        if (accountId == 0) {
            Log.w(TAG, "signIn: account id is 0, bail");
            return;
        }

        boolean isAccountEditable = mProviderCursor.getInt(ACTIVE_ACCOUNT_LOCKED) == 0;
        if (isAccountEditable && mProviderCursor.isNull(ACTIVE_ACCOUNT_PW_COLUMN)) {
            // no password, edit the account
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.i(TAG, "no pw for account " + accountId);
            Intent intent = getEditAccountIntent();
            startActivity(intent);
            finish();
            return;
        }

        long providerId = mProviderCursor.getLong(PROVIDER_ID_COLUMN);
        String password = mProviderCursor.getString(ACTIVE_ACCOUNT_PW_COLUMN);
        boolean isActive = false; // TODO(miron)
        mSignInHelper.signIn(password, providerId, accountId, isActive);
    }

    private boolean isSignedIn(Cursor cursor) {
        int connectionStatus = cursor.getInt(ACCOUNT_CONNECTION_STATUS);

        return connectionStatus == Imps.ConnectionStatus.ONLINE;
    }

    private int accountsAvailable() {
        if (!mProviderCursor.moveToFirst()) {
            return 0;
        }
        int count = 0;
        do {
            if (!mProviderCursor.isNull(ACTIVE_ACCOUNT_PW_COLUMN) &&
                    !mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN) &&
                    mProviderCursor.getInt(ACTIVE_ACCOUNT_KEEP_SIGNED_IN) != 0) {
                count++;
            }
        } while (mProviderCursor.moveToNext());

        return count;
    }

    void handleIntentAPILaunch (Intent srcIntent)
    {
        Intent intent = new Intent(this, ImUrlActivity.class);
        intent.setAction(srcIntent.getAction());

        if (srcIntent.getData() != null)
            intent.setData(srcIntent.getData());

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (srcIntent.getExtras()!= null)
            intent.putExtras(srcIntent.getExtras());
        startActivity(intent);

        setIntent(null);
        finish();
    }

    Intent getEditAccountIntent() {
        Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(
                Imps.Account.CONTENT_URI, mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN)));
        intent.putExtra("isSignedIn", isSignedIn(mProviderCursor));
        intent.addCategory(getProviderCategory(mProviderCursor));
        return intent;
    }


    private String getProviderCategory(Cursor cursor) {
        return cursor.getString(PROVIDER_CATEGORY_COLUMN);
    }

    private final static class MyHandler extends SimpleAlertHandler {

        public MyHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ImApp.EVENT_CONNECTION_DISCONNECTED) {
                promptDisconnectedEvent(msg);
            }
            super.handleMessage(msg);
        }
    }

    @Override
    public void onCacheWordUninitialized() {
        Log.d(ImApp.LOG_TAG,"cache word uninit");

        if (mDoLock) {
            completeShutdown();
        } else {
            showLockScreen();
        }
        finish();
    }

    void showLockScreen() {
        Intent intent = new Intent(this, LockScreenActivity.class);
        Intent returnIntent = getIntent();
        returnIntent.putExtra("doSignIn", mDoSignIn);
        intent.putExtra("originalIntent", returnIntent);
        startActivity(intent);

    }

    @Override
    public void onCacheWordLocked() {
        if (mDoLock) {
            Log.d(ImApp.LOG_TAG, "cacheword lock requested but already locked");

        } else {
            showLockScreen();
        }
        finish();
    }

    @Override
    public void onCacheWordOpened() {
        if (mDoLock) {
            completeShutdown();
            return;
        }

       byte[] encryptionKey = mCacheWord.getEncryptionKey();
       openEncryptedStores(encryptionKey, true);

       ChatFileStore.init(this, mCacheWord.getEncryptionKey());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void shutdownAndLock(Activity activity) {
        ImApp app = (ImApp) activity.getApplication();
        if (app != null) {
            for (IImConnection conn : app.getActiveConnections()) {
                try {
                    conn.logout();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        Intent intent = new Intent(activity, WelcomeActivity.class);
        // Request lock
        intent.putExtra("doLock", true);
        // Clear the backstack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 11)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private void completeShutdown ()
    {
        /* ignore unmount errors and quit ASAP. Threads actively using the VFS will
         * cause IOCipher's VirtualFileSystem.unmount() to throw an IllegalStateException */
        try {
            ChatFileStore.unmount();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
           new AsyncTask<String, Void, String>() {

            @Override
            protected void onPreExecute() {
                if (mApp.getActiveConnections().size() > 0)
                {
                    dialog = new ProgressDialog(WelcomeActivity.this);
                    dialog.setCancelable(true);
                    dialog.setMessage(getString(R.string.signing_out_wait));
                    dialog.show();
                }
            }

            @Override
            protected String doInBackground(String... params) {

                boolean stillConnected = true;

                while (stillConnected)
                {

                       try{
                           IImConnection conn = mApp.getActiveConnections().iterator().next();

                           if (conn.getState() == ImConnection.DISCONNECTED || conn.getState() == ImConnection.LOGGING_OUT)
                           {
                               stillConnected = false;
                           }
                           else
                           {
                               conn.logout();
                               stillConnected = true;
                           }


                           Thread.sleep(500);
                       }catch(Exception e){}


                }

                return "";
              }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                if (dialog != null)
                    dialog.dismiss();

                mApp.forceStopImService();

                Imps.clearPassphrase(mApp);

                if (mCacheWord != null)
                {
                    mCacheWord.manuallyLock();
                }

                Intent cacheWordIntent = CacheWordService
                        .getBlankServiceIntent(getApplicationContext());
                stopService(cacheWordIntent);
                finish();
            }
        }.execute();
    }

    private boolean checkMediaStoreFile() {
        /* First set location based on pref, then override based on where the file is.
         * This crazy logic is necessary to support old installs that used logic that
         * is not really predictable, since it was based on whether the SD card was
         * present or not. */
        File internalDbFile = new File(ChatFileStore.getInternalDbFilePath(this));
        boolean internalDbFileUsabe = internalDbFile.isFile() && internalDbFile.canWrite();

        boolean externalDbFileUsable = false;
        java.io.File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null) {
            File externalDbFile = new File(ChatFileStore.getExternalDbFilePath(this));
            externalDbFileUsable = externalDbFile.isFile() && externalDbFile.canWrite();
        }
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isPrefSet = settings.contains(
                getString(R.string.key_store_media_on_external_storage_pref));
        boolean storeMediaOnExternalStorage;
        if (isPrefSet) {
            storeMediaOnExternalStorage = settings.getBoolean(
                    getString(R.string.key_store_media_on_external_storage_pref), false);
            if (storeMediaOnExternalStorage && !externalDbFileUsable) {
                Intent i = new Intent(this, MissingChatFileStoreActivity.class);
                startActivity(i);
                finish();
                return true;
            }
        } else {
            /* only use external if file already exists only there or internal is almost full */
            boolean forceExternalStorage = !enoughSpaceInInternalStorage(internalDbFile);
            if (!internalDbFileUsabe && (externalDbFileUsable || forceExternalStorage)) {
                storeMediaOnExternalStorage = true;
            } else {
                storeMediaOnExternalStorage = false;
            }
            Editor editor = settings.edit();
            editor.putBoolean(getString(R.string.key_store_media_on_external_storage_pref),
                    storeMediaOnExternalStorage);
            editor.apply();
        }
        return false;
    }

    private static boolean enoughSpaceInInternalStorage(File f) {
        StatFs stat = new StatFs(f.getParent());
        long freeSizeInBytes = stat.getAvailableBlocks() * (long) stat.getBlockSize();
        return freeSizeInBytes > 536870912; // 512 MB
    }

    private boolean openEncryptedStores(byte[] key, boolean allowCreate) {
        String pkey = (key != null) ? new String(SQLCipherOpenHelper.encodeRawKey(key)) : "";
        OtrAndroidKeyManagerImpl.setKeyStorePassword(pkey);

        if (cursorUnlocked(pkey, allowCreate)) {

            if (mDoLock)
                completeShutdown();
            else
                doOnResume();

            return true;
        } else {
            return false;
        }
    }


    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this, ImApp.HOCKEY_APP_ID);
    }
}
