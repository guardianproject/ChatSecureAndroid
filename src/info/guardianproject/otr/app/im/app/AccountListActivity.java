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

import info.guardianproject.otr.OtrAndroidKeyManagerImpl;
import info.guardianproject.otr.OtrDebugLogger;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.plugin.xmpp.auth.GTalkOAuth2;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;

import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.zxing.integration.android.IntentIntegrator;

public class AccountListActivity extends SherlockFragmentActivity implements View.OnCreateContextMenuListener, ProviderListItem.SignInManager {

    private static final String TAG = ImApp.LOG_TAG;

    private AccountAdapter mAdapter;
    private ImApp mApp;
    private SimpleAlertHandler mHandler;

    private SignInHelper mSignInHelper;

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
                                                         Imps.Provider.ACCOUNT_CONNECTION_STATUS
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

    private static final int ACCOUNT_LOADER_ID = 1000;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        mApp = (ImApp)getApplication();
        mApp.maybeInit(this);

        mApp.setAppTheme(this);
        ThemeableActivity.setBackgroundImage(this);
        
        if (!Imps.isUnlocked(this)) {
            onDBLocked();
            return;
        }

        mHandler = new MyHandler(this);
        mSignInHelper = new SignInHelper(this);

        initProviderCursor();
        setContentView(R.layout.account_list_activity);
        
        
    }

    AccountAdapter getAdapter() {
        return mAdapter;
    }
    
    @Override
    protected void onPause() {
        mHandler.unregisterForBroadcastEvents();
        
        super.onPause();
        
    }

    @Override
    protected void onDestroy() {
        if (mSignInHelper != null) // if !Imps.isUnlocked(this)
            mSignInHelper.stop();
        
        if (mAdapter != null)
            mAdapter.swapCursor(null);
        
        super.onDestroy();
    }
    

    
    @Override
    protected void onResume() {

        super.onResume();

        ThemeableActivity.setBackgroundImage(this);
        
        mHandler.registerForBroadcastEvents();
        
        mApp.checkForCrashes(this);
        
    }
    
    

    protected void openAccountAtPosition(int position) {
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);

        if (cursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
            showExistingAccountListDialog();
            
        } else {

            int state = cursor.getInt(ACCOUNT_CONNECTION_STATUS);
            long accountId = cursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);

           
            if (state == Imps.ConnectionStatus.OFFLINE) {
             
                Intent intent = getEditAccountIntent();
                startActivity(intent);
                
            }
            else
            {
                gotoAccount(accountId);
            }
            
            
        }
        

    }
    
    public void signIn(long accountId) {
        if (accountId <= 0) {
            Log.w(TAG, "signIn: account id is 0, bail");
            return;
        }
        Cursor cursor = mAdapter.getCursor();

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            long cAccountId = cursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
            
            if (cAccountId == accountId)
                break;
            
            cursor.moveToNext();
        }

        // Remember that the user signed in.
        setKeepSignedIn(accountId, true);

        long providerId = cursor.getLong(PROVIDER_ID_COLUMN);
        String password = cursor.getString(ACTIVE_ACCOUNT_PW_COLUMN);
        
        boolean isActive = false; // TODO(miron)
        mSignInHelper.signIn(password, providerId, accountId, isActive);
        
        cursor.moveToPosition(-1);
    }

    
    protected void gotoAccount(long accountId)
    {

        Intent intent = new Intent(this, NewChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, accountId);
        
        startActivity(intent);
    
    }

    private void doHardShutdown() {
        
        for (IImConnection conn : mApp.getActiveConnections())
        {
               try {
                conn.logout();                
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
        // Request lock
        intent.putExtra("doLock", true);
        // Clear the backstack
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
   }
    public void signOut(final long accountId) {
        // Remember that the user signed out and do not auto sign in until they
        // explicitly do so
        setKeepSignedIn(accountId, false);

        try {
            IImConnection conn = mApp.getConnectionByAccount(accountId);
            if (conn != null) {
                conn.logout();
            }
        } catch (Exception ex) {
            Log.e(TAG, "signOut failed", ex);
        }
    }

    private void setKeepSignedIn(final long accountId, boolean signin) {
        Uri mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
        ContentValues values = new ContentValues();
        values.put(Imps.Account.KEEP_SIGNED_IN, signin);
        getContentResolver().update(mAccountUri, values, null, null);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.accounts_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_sign_out_all:
            doHardShutdown();
            return true;
        case R.id.menu_existing_account:
            showExistingAccountListDialog();
            return true;
        case R.id.menu_create_account:
            showSetupAccountForm(helper.getProviderNames().get(0), null, null, true, null,false);
            return true;
        case R.id.menu_settings:
            Intent sintent = new Intent(this, SettingActivity.class);
            startActivityForResult(sintent,0);
            return true;
        case R.id.menu_import_keys:
            importKeyStore();
            return true;
       // case R.id.menu_exit:
      //      signOutAndKillProcess();
            
          //  return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String[] mAccountList;
    private String mNewUser;
    
    private ImPluginHelper helper = ImPluginHelper.getInstance(this);

    private void importKeyStore ()
    {
        boolean doKeyStoreImport = OtrAndroidKeyManagerImpl.checkForKeyImport(getIntent(), this);

    }
    
    private void exportKeyStore ()
    {
        //boolean doKeyStoreExport = OtrAndroidKeyManagerImpl.getInstance(this).doKeyStoreExport(password);

    }
    
    void showExistingAccountListDialog() {
      
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.account_select_type);
        
        List<String> listProviders = helper.getProviderNames();
        
        mAccountList = new String[listProviders.size()+1];
        
        int i = 0;
        mAccountList[i] = getString(R.string.google_account);
        i++;
        
        for (String providerName : listProviders)
            mAccountList[i++] = providerName;
                
        
        builder.setItems(mAccountList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {

                if (pos == 0) //google accounts based on xmpp
                {           
                    showGoogleAccountListDialog();
                }
                else if (pos == 1) //xmpp
                {
                    //otherwise support the actual plugin-type
                    showSetupAccountForm(mAccountList[pos],null, null, false,mAccountList[pos],false);
                }
                else if (pos == 2) //zeroconf
                {
                    String username = "";
                    String passwordPlaceholder = "password";//zeroconf doesn't need a password
                    showSetupAccountForm(mAccountList[pos],username,passwordPlaceholder, false,mAccountList[pos],true);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }
    

private Handler mHandlerGoogleAuth = new Handler ()
{

    @Override
    public void handleMessage(Message msg) {
       
        super.handleMessage(msg);
        
        Log.d(TAG,"Got handler callback from auth: " + msg.what);
    }
        
};

    
    private void showGoogleAccountListDialog() {
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.account_select_type);
        
        Account[] accounts = AccountManager.get(this).getAccountsByType(GTalkOAuth2.TYPE_GOOGLE_ACCT);
        
        mAccountList = new String[accounts.length];
        
        for (int i = 0; i < mAccountList.length; i++)
            mAccountList[i] = accounts[i].name;
        
        builder.setItems(mAccountList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
   
                    mNewUser = mAccountList[pos];                     
                    Thread thread = new Thread ()
                    {
                        public void run ()
                        {
                            //get the oauth token
                            
                          //don't store anything just make sure it works!
                           String password = GTalkOAuth2.NAME + ':' + GTalkOAuth2.getGoogleAuthTokenAllow(mNewUser, getApplicationContext(), AccountListActivity.this,mHandlerGoogleAuth);
                   
                           //use the XMPP type plugin for google accounts, and the .NAME "X-GOOGLE-TOKEN" as the password
                            showSetupAccountForm(helper.getProviderNames().get(0), mNewUser,password, false, getString(R.string.google_account),false);
                        }
                    };
                    thread.start();
              
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }
    
    public void showSetupAccountForm (String providerType, String username, String token, boolean createAccount, String formTitle, boolean hideTor)
    {
        long providerId = helper.createAdditionalProvider(providerType);//xmpp
        mApp.resetProviderSettings(); //clear cached provider list
        
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_INSERT);
        
        intent.setData(ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId));
        intent.addCategory(ImApp.IMPS_CATEGORY);
        
        if (username != null)
            intent.putExtra("newuser", username);
        
        if (token != null)
            intent.putExtra("newpass", token);
        
        if (formTitle != null)
            intent.putExtra("title", formTitle);
        
        intent.putExtra("hideTor", hideTor);
        
        intent.putExtra("register", createAccount);
        
        startActivity(intent);
    }
    
   
    

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        /*
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor providerCursor = (Cursor) getListAdapter().getItem(info.position);
        menu.setHeaderTitle(providerCursor.getString(PROVIDER_FULLNAME_COLUMN));

        if (providerCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
            menu.add(0, ID_ADD_ACCOUNT, 0, R.string.menu_edit_account);
            menu.add(0, ID_REMOVE_ACCOUNT, 0, R.string.menu_remove_account).setIcon(
                    android.R.drawable.ic_menu_delete);
            return;
        }

        long providerId = providerCursor.getLong(PROVIDER_ID_COLUMN);
        boolean isLoggingIn = isSigningIn(providerCursor);
        boolean isLoggedIn = isSignedIn(providerCursor);

        BrandingResources brandingRes = mApp.getBrandingResource(providerId);
        //menu.add(0, ID_VIEW_CONTACT_LIST, 0,
          //      brandingRes.getString(BrandingResourceIDs.STRING_MENU_CONTACT_LIST));
        
        if (isLoggedIn) {
            menu.add(0, ID_SIGN_OUT, 0, R.string.menu_sign_out).setIcon(
                    android.R.drawable.ic_menu_close_clear_cancel);
        } else if (isLoggingIn) {
            menu.add(0, ID_SIGN_OUT, 0, R.string.menu_cancel_signin).setIcon(
                    android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            menu.add(0, ID_SIGN_IN, 0, R.string.sign_in)
            // TODO .setIcon(info.guardianproject.otr.app.internal.R.drawable.ic_menu_login)
            ;
        }

        boolean isAccountEditable = providerCursor.getInt(ACTIVE_ACCOUNT_LOCKED) == 0;
        if (isAccountEditable && !isLoggingIn && !isLoggedIn) {
            menu.add(0, ID_EDIT_ACCOUNT, 0, R.string.menu_edit_account).setIcon(
                    android.R.drawable.ic_menu_edit);
            menu.add(0, ID_REMOVE_ACCOUNT, 0, R.string.menu_remove_account).setIcon(
                    android.R.drawable.ic_menu_delete);
        }*/
    }

    
    @SuppressWarnings("deprecation")
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        /*
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        
        long providerId = info.id;
        Cursor providerCursor = (Cursor) getListAdapter().getItem(info.position);
        long accountId = providerCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);

        mProviderCursor.moveToPosition(info.position);
                    Intent intent = new Intent(getContext(), NewChatActivity.class);
                    intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
                    getContext().startActivity(intent);
        
        switch (item.getItemId()) {
        case ID_EDIT_ACCOUNT: {
            startActivity(getEditAccountIntent());
            return true;
        }

        case ID_REMOVE_ACCOUNT: {
            Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
            getContentResolver().delete(accountUri, null, null);
            Uri providerUri = ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId);
            getContentResolver().delete(providerUri, null, null);
            // Requery the cursor to force refreshing screen
            providerCursor.requery();
            return true;
        }

        case ID_VIEW_CONTACT_LIST: {
            Intent intent = getViewContactsIntent();
            startActivity(intent);
            return true;
        }
        case ID_ADD_ACCOUNT: {
           
            showNewAccountListDialog();
           
            return true;
        }

        case ID_SIGN_IN: {
            signIn(accountId);
            return true;
        }

        case ID_SIGN_OUT: {
            // TODO: progress bar
            signOut(accountId);
            return true;
        }

        }
    */
        
        return false;
    }

    /*
    Intent getCreateAccountIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_INSERT);

        long providerId = mProviderCursor.getLong(PROVIDER_ID_COLUMN);
        intent.setData(ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId));
        intent.addCategory(getProviderCategory(mProviderCursor));
        return intent;
    }*/

    Intent getEditAccountIntent() {
        Cursor cursor = mAdapter.getCursor();
        Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(
                Imps.Account.CONTENT_URI, cursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN)));
        intent.addCategory(ImApp.IMPS_CATEGORY);
        return intent;
    }

    Intent getViewContactsIntent() {
        Cursor cursor = mAdapter.getCursor();
        Intent intent = new Intent(this, ContactListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, cursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN));
        return intent;
    }
    
    /*
    Intent getViewChatsIntent() {
        Intent intent = new Intent(this, ChatListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN));
        return intent;
    }*/

    /*
    private String getProviderCategory(Cursor cursor) {
        return cursor.getString(PROVIDER_CATEGORY_COLUMN);
    }*/

    static void log(String msg) {
        Log.d(TAG, "[LandingPage]" + msg);
    }

    private class ProviderListItemFactory implements LayoutInflater.Factory {
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            if (name != null && name.equals(ProviderListItem.class.getName())) {
                return new ProviderListItem(context, AccountListActivity.this, AccountListActivity.this);
            }
            return null;
        }
    }

    private final class MyHandler extends SimpleAlertHandler {

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == IntentIntegrator.REQUEST_CODE)
        {
            
          Object keyMan = null;
          boolean keyStoreImported = false;
            
            try {
         
                keyStoreImported = OtrAndroidKeyManagerImpl.handleKeyScanResult(requestCode, resultCode, data, this);
                
            } catch (Exception e) {
                OtrDebugLogger.log("error importing keystore",e);
            }
            
            if (keyStoreImported)
            {
                Toast.makeText(this, R.string.successfully_imported_otr_keyring, Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, R.string.otr_keyring_not_imported_please_check_the_file_exists_in_the_proper_format_and_location, Toast.LENGTH_SHORT).show();
    
            }
            
        }
    }


    public void onDBLocked() {
     
        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void initProviderCursor()
    {
        final Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;
        getSupportLoaderManager().initLoader(ACCOUNT_LOADER_ID, null, new LoaderCallbacks<Cursor>() {

            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                CursorLoader loader = new CursorLoader(AccountListActivity.this, uri, PROVIDER_PROJECTION,
                        Imps.Provider.CATEGORY + "=?" + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL" /* selection */,
                        new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                        Imps.Provider.DEFAULT_SORT_ORDER);

                return loader;
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {
                mAdapter.swapCursor(newCursor);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mAdapter.swapCursor(null);
                
            }
        });

        mAdapter = new AccountAdapter(this, new ProviderListItemFactory(), R.layout.account_view);
    }
}
