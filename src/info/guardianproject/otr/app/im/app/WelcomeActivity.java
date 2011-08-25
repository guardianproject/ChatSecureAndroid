/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.otr.app.im.app;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.otr.app.im.ui.AboutActivity;
import info.guardianproject.otr.app.im.ui.TabbedContainer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;

public class WelcomeActivity extends Activity {
    private static final String TAG = "WelcomeActivity";

    private boolean mDidAutoLaunch = false;

    private Cursor mProviderCursor;
    private ImApp mApp;
    private SimpleAlertHandler mHandler;

    private static final String[] PROVIDER_PROJECTION = {
            Imps.Provider._ID,
            Imps.Provider.NAME,
            Imps.Provider.FULLNAME,
            Imps.Provider.CATEGORY,
            Imps.Provider.ACTIVE_ACCOUNT_ID,
            Imps.Provider.ACTIVE_ACCOUNT_USERNAME,
            Imps.Provider.ACTIVE_ACCOUNT_PW,
            Imps.Provider.ACTIVE_ACCOUNT_LOCKED,
            Imps.Provider.ACTIVE_ACCOUNT_KEEP_SIGNED_IN,
            Imps.Provider.ACCOUNT_PRESENCE_STATUS,
            Imps.Provider.ACCOUNT_CONNECTION_STATUS,
    };

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
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        SQLiteDatabase.loadLibs(this);


        setContentView(R.layout.welcome_activity);
        
        Button btnSplashAbout = ((Button)findViewById(R.id.btnSplashAbout));
        btnSplashAbout.setAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
        
        btnSplashAbout.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) {
				finish();
				Intent intent = new Intent(getBaseContext(), AboutActivity.class);
				startActivity(intent);
			}
        });
        
        /*
        Button btnSplashSetup = ((Button)findViewById(R.id.btnSplashSetup));
        btnSplashSetup.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) {
				// TODO change for multiple account support	        
		        if (! mProviderCursor.moveToFirst() || mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
		            // add account
					startActivity(getCreateAccountIntent());
		        } else {
		        	// edit existing account
					startActivity(getEditAccountIntent());
		        }
			}
        });*/

        mProviderCursor = managedQuery(Imps.Provider.CONTENT_URI_WITH_ACCOUNT,
                PROVIDER_PROJECTION,
                Imps.Provider.CATEGORY + "=?" /* selection */,
                new String[]{ ImApp.IMPS_CATEGORY } /* selection args */,
                Imps.Provider.DEFAULT_SORT_ORDER);
        
        mApp = ImApp.getApplication(this);
        mHandler = new MyHandler(this);

        ImPluginHelper.getInstance(this).loadAvailablePlugins();
        

    }

    @Override
    protected void onPause() {
        mHandler.unregisterForBroadcastEvents();
        
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        Toast.makeText(this, getString(R.string.warning_alpha), Toast.LENGTH_LONG).show();


        mHandler.registerForBroadcastEvents();
        
        if (allAccountsSignedOut() && ! mDidAutoLaunch) {
        	mDidAutoLaunch = true;
        	finish();
        	showAccountSetup();
        	signInAll();
        } else {
        	showActiveAccount();
        }
    }

    // Show active account if signed in
    protected boolean showActiveAccount() {
    	if (! mProviderCursor.moveToFirst() || mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN))
    		return false;
    	if (!isSignedIn(mProviderCursor))
    		return false;

        long accountId = mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);

        finish();
        Intent intent = new Intent(this, TabbedContainer.class);
        // clear the back stack of the account setup
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, accountId);
        startActivity(intent);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_account_settings:
                finish();
                showAccountSetup();                
                return true;
                
            case R.id.menu_about:
                finish();
                showAbout();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }
    
    private void signInAll() {
    	Log.i(TAG, "signInAll");
    	mProviderCursor.moveToFirst();
    	do {
    		int position = mProviderCursor.getPosition();
    		signInAccountAtPosition(position);
    	} while (mProviderCursor.moveToNext()) ;
    }
    
    private void signInAccountAtPosition(int position) {
        mProviderCursor.moveToPosition(position);

        if (!mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
            int state = mProviderCursor.getInt(ACCOUNT_CONNECTION_STATUS);
            long accountId = mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);

            if (state == Imps.ConnectionStatus.OFFLINE) {
                boolean isKeepSignedIn = mProviderCursor.getInt(ACTIVE_ACCOUNT_KEEP_SIGNED_IN) != 0;
                if (isKeepSignedIn) {
                    signIn(accountId);
                }
            } else if (state == Imps.ConnectionStatus.CONNECTING) {
                signIn(accountId);
            }
        }
    }

    private void signIn(long accountId) {
        if (accountId == 0) {
            Log.w(TAG, "signIn: account id is 0, bail");
            return;
        }

        boolean isAccountEditible = mProviderCursor.getInt(ACTIVE_ACCOUNT_LOCKED) == 0;
        if (isAccountEditible && mProviderCursor.isNull(ACTIVE_ACCOUNT_PW_COLUMN)) {
            // no password, edit the account
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.i(TAG, "no pw for account " + accountId);
            Intent intent = getEditAccountIntent();
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(this, SigningInActivity.class);
        intent.setData(ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId));
        finish();
        startActivity(intent);
    }

    boolean isSigningIn(Cursor cursor) {
        int connectionStatus = cursor.getInt(ACCOUNT_CONNECTION_STATUS);
        return connectionStatus == Imps.ConnectionStatus.CONNECTING;
    }

    private boolean isSignedIn(Cursor cursor) {
        int connectionStatus = cursor.getInt(ACCOUNT_CONNECTION_STATUS);
        return connectionStatus == Imps.ConnectionStatus.ONLINE;
    }

    private boolean allAccountsSignedOut() {
        if(!mProviderCursor.moveToFirst()) {
            return false;
        }
        do {
            if (isSignedIn(mProviderCursor)) {
                return false;
            }
        } while (mProviderCursor.moveToNext()) ;

        return true;
    }

    private void showAccountSetup ()
    {
    	if (! mProviderCursor.moveToFirst() || mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
            // add account
			startActivity(getCreateAccountIntent());
        } else {
        	// edit existing account
			startActivity(getEditAccountIntent());
        }
    }
    
    private void showAbout ()
    {
    	//TODO implement this about form
    	Toast.makeText(this, "About Gibberbot\nhttps://guardianproject.info/apps/gibber", Toast.LENGTH_LONG).show();
    }
    
    private void signOutAll() {
        DialogInterface.OnClickListener confirmListener
                = new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton) {
                do {
                    long accountId = mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
                    signOut(accountId);
                } while (mProviderCursor.moveToNext()) ;
            }
        };

        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm)
            .setMessage(R.string.signout_all_confirm_message)
            .setPositiveButton(R.string.yes, confirmListener) // default button
            .setNegativeButton(R.string.no, null)
            .setCancelable(true)
            .show();
    }

    private void signOut(long accountId) {
        if (accountId == 0) {
            Log.w(TAG, "signOut: account id is 0, bail");
            return;
        }

        try {
            IImConnection conn = mApp.getConnectionByAccount(accountId);
            if (conn != null) {
                conn.logout();
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "signOut failed", ex);
        }
    }

    Intent getCreateAccountIntent() {
    	Intent intent = new Intent(getBaseContext(), AccountActivity.class);
    	intent.setAction(Intent.ACTION_INSERT);

    	// TODO fix for multiple account support
    	//long providerId = mProviderCursor.getLong(PROVIDER_ID_COLUMN);
    	long providerId = 1; // XMPP
    	intent.setData(ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId));
    	//TODO we probably need the ProviderCategory in the createAccountIntent, but currently it FC's on account creation
    	//intent.addCategory(getProviderCategory(mProviderCursor));
    	return intent;
    }

    Intent getEditAccountIntent() {
        Intent intent = new Intent(Intent.ACTION_EDIT,
                ContentUris.withAppendedId(Imps.Account.CONTENT_URI,
                        mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN)));
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
}
