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

import java.io.File;
import java.io.IOException;

import info.guardianproject.otr.OtrAndroidKeyManagerImpl;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.otr.app.im.ui.AboutActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class WelcomeActivity extends ThemeableActivity {
    
    private static final String TAG = "WelcomeActivity";
    private boolean mDidAutoLaunch = false;
    private Cursor mProviderCursor;
    private ImApp mApp;
    private SimpleAlertHandler mHandler;
    private String mDefaultLocale;
    private SignInHelper mSignInHelper;

    private boolean mDoSignIn = true;
    
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSignInHelper = new SignInHelper(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDefaultLocale = prefs.getString(getString(R.string.pref_default_locale), null);
        setContentView(R.layout.welcome_activity);
        
        this.getSupportActionBar().hide();
        
        Button getStarted = ((Button) findViewById(R.id.btnSplashAbout));

        getStarted.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                Intent intent = new Intent(getBaseContext(), AboutActivity.class);
                startActivity(intent);
            }
        });

        mDoSignIn = getIntent().getBooleanExtra("doSignIn", true);
      
    }
    

   
    @SuppressWarnings("deprecation")
    private boolean cursorUnlocked() {
        try {
            mApp = ImApp.getApplication(this);
            mHandler = new MyHandler(this);
            ImPluginHelper.getInstance(this).loadAvailablePlugins();

            mProviderCursor = managedQuery(Imps.Provider.CONTENT_URI_WITH_ACCOUNT,
                    PROVIDER_PROJECTION, Imps.Provider.CATEGORY + "=?" /* selection */,
                    new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                    Imps.Provider.DEFAULT_SORT_ORDER);

            mProviderCursor.moveToFirst();

            return true;

        } catch (Exception e) {
            Log.e(ImApp.LOG_TAG, e.getMessage(), e);
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
    }

    @Override
    protected void onDestroy() {
        mSignInHelper.stop();

        super.onDestroy();
    }
    

    @Override
    protected void onResume() {
        super.onResume();

        if (mDefaultLocale == null)
            showLocaleDialog();
        else {
            cursorUnlocked();
            doOnResume();
        }
    }

    private void doOnResume() {

        if (mApp == null) {
            mApp = ImApp.getApplication(this);
            mHandler = new MyHandler(this);
            ImPluginHelper.getInstance(this).loadAvailablePlugins();
        }

        mApp.setAppTheme(this);
        mHandler.registerForBroadcastEvents();

        int countSignedIn = accountsSignedIn();
        int countAvailable = accountsAvailable();
        int countConfigured = accountsConfigured();
        
        boolean doKeyStoreImport = false;
        
        if (getIntent().getData() != null)
        {
            Uri uriData = getIntent().getData();
            String path = null;
            
            if(uriData.getScheme() != null && uriData.getScheme().equals("file"))
            {
                path = uriData.toString().replace("file://", "");
            
                File file = new File(path);
                
                doKeyStoreImport = true;
                
                importOtrKeyStore(file);
            }
        }
        
        if (!doKeyStoreImport)
        {
            if (countAvailable == 1) {
                // If just one account is available for auto-signin, go there immediately after service starts trying
                // to connect.
                mSignInHelper.setSignInListener(new SignInHelper.Listener() {
                    public void connectedToService() {
                    }
                    public void stateChanged(int state, long accountId) {
                        if (state == ImConnection.LOGGING_IN) {
                            mSignInHelper.goToAccount(accountId);
                        }
                    }
                });
            } else {
                mSignInHelper.setSignInListener(null);
            }
            
            
            if (countSignedIn == 0 && countAvailable > 0 && !mDidAutoLaunch && mDoSignIn) {
                mDidAutoLaunch = true;
                signInAll();
                showAccounts();
            } else if (countSignedIn == 1) {
                showActiveAccount();
            } else if (countConfigured > 0) {
                showAccounts();
            }
            // Otherwise, stay on Getting Started view
        }
    }
    
    
    private void importOtrKeyStore (File file)
    {
        //ask user if they want to overwrite existing entries or just add
        try {
            OtrAndroidKeyManagerImpl oakm = OtrAndroidKeyManagerImpl.getInstance(null);
            boolean overWriteExisting = true;
            oakm.importKeyStore(file, overWriteExisting);
        } catch (IOException e) {
          Log.e(TAG,"error opening keystore",e);
        }
    }

    // Show signed in account
    protected boolean showActiveAccount() {
        if (!mProviderCursor.moveToFirst())
            return false;
        do {
            if (!mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN) && isSignedIn(mProviderCursor)) {
                gotoAccount();
                return true;
            }
        } while (mProviderCursor.moveToNext());
        return false;
    }

    protected void gotoAccount() {
        long accountId = mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);

        Intent intent = new Intent(this, ChatListActivity.class);
        // clear the back stack of the account setup
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, accountId);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_account_settings:
            finish();
            showAccounts();
            return true;

        case R.id.menu_about:
            showAbout();
            return true;

        case R.id.menu_locale:
            showLocaleDialog();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void signInAll() {

        Log.i(TAG, "signInAll");
        if (!mProviderCursor.moveToFirst())
            return;

        do {
            int position = mProviderCursor.getPosition();
            signInAccountAtPosition(position);

        } while (mProviderCursor.moveToNext());

    }

    private boolean signInAccountAtPosition(int position) {
        mProviderCursor.moveToPosition(position);

        if (!mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
            int state = mProviderCursor.getInt(ACCOUNT_CONNECTION_STATUS);
            long accountId = mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);

            if (state == Imps.ConnectionStatus.OFFLINE) {
                boolean isKeepSignedIn = mProviderCursor.getInt(ACTIVE_ACCOUNT_KEEP_SIGNED_IN) != 0;
                if (isKeepSignedIn) {
                    signIn(accountId);
                    return true;
                }

            }
        }

        return false;
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
    
    boolean isSigningIn(Cursor cursor) {
        int connectionStatus = cursor.getInt(ACCOUNT_CONNECTION_STATUS);
        return connectionStatus == Imps.ConnectionStatus.CONNECTING;
    }

    private boolean isSignedIn(Cursor cursor) {
        int connectionStatus = cursor.getInt(ACCOUNT_CONNECTION_STATUS);

        return connectionStatus == Imps.ConnectionStatus.ONLINE;
    }

    private int accountsSignedIn() {
        if (!mProviderCursor.moveToFirst()) {
            return 0;
        }
        int count = 0;
        do {
            if (isSignedIn(mProviderCursor)) {
                count++;
            }
        } while (mProviderCursor.moveToNext());

        return count;
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

    private int accountsConfigured() {
        if (!mProviderCursor.moveToFirst()) {
            return 0;
        }
        int count = 0;
        do {
            if (!mProviderCursor.isNull(ACTIVE_ACCOUNT_USERNAME_COLUMN) &&
                    !mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
                count++;
            }
        } while (mProviderCursor.moveToNext());

        return count;
    }

    //    private void showAccountSetup() {
    //        if (!mProviderCursor.moveToFirst() || mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
    //            // add account
    //            startActivity(getCreateAccountIntent());
    //        } else {
    //            // edit existing account
    //            startActivity(getEditAccountIntent());
    //        }
    //    }

    private void showAbout() {
        //TODO implement this about form
        Toast.makeText(this, "About Gibberbot\nhttps://guardianproject.info/apps/gibber",
                Toast.LENGTH_LONG).show();
    }

    //    private void signOutAll() {
    //        DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
    //            public void onClick(DialogInterface dialog, int whichButton) {
    //                do {
    //                    long accountId = mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
    //                    signOut(accountId);
    //                } while (mProviderCursor.moveToNext());
    //            }
    //        };
    //
    //        new AlertDialog.Builder(this).setTitle(R.string.confirm)
    //                .setMessage(R.string.signout_all_confirm_message)
    //                .setPositiveButton(R.string.yes, confirmListener) // default button
    //                .setNegativeButton(R.string.no, null).setCancelable(true).show();
    //    }

    //    private void signOut(long accountId) {
    //        if (accountId == 0) {
    //            Log.w(TAG, "signOut: account id is 0, bail");
    //            return;
    //        }
    //
    //        try {
    //            IImConnection conn = mApp.getConnectionByAccount(accountId);
    //            if (conn != null) {
    //                conn.logout();
    //            }
    //        } catch (RemoteException ex) {
    //            Log.e(TAG, "signOut failed", ex);
    //        }
    //    }

    void showAccounts() {
        startActivity(new Intent(getBaseContext(), AccountListActivity.class));
        finish();
    }
    
    /*
    Intent getCreateAccountIntent() {
        Intent intent = new Intent(getBaseContext(), AccountActivity.class);
        intent.setAction(Intent.ACTION_INSERT);

        // TODO fix for multiple account support
        long providerId = 1; // XMPP
        intent.setData(ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId));
        //TODO we probably need the ProviderCategory in the createAccountIntent, but currently it FC's on account creation
        return intent;
    }
    */
    
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

    private void showLocaleDialog() {
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setTitle(getResources().getString(R.string.KEY_PREF_LANGUAGE_TITLE));

        Configuration config = getResources().getConfiguration();
        String defaultLangName = config.locale.getDefault().getDisplayName();
        String defaultLangCode = config.locale.getDefault().getCountry();
        
        String[] langs = getResources().getStringArray(R.array.languages);
        langs[0] = langs[0] + " (" + defaultLangName + ")";
        
        ad.setItems(langs,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String[] locs = getResources().getStringArray(R.array.languages_values);

                        if (which < locs.length) {
                            ((ImApp)getApplication()).setNewLocale(WelcomeActivity.this.getBaseContext(), locs[which]);

                            Intent intent = getIntent();
                            finish();
                            startActivity(intent);
                        }
                    }
                });

        ad.show();
    }

}
