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

import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
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
import android.os.Message;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class LandingPage extends SherlockListActivity implements View.OnCreateContextMenuListener {
    private static final String TAG = ImApp.LOG_TAG;

    private static final int ID_SIGN_IN = Menu.FIRST + 1;
    private static final int ID_SIGN_OUT = Menu.FIRST + 2;
    private static final int ID_EDIT_ACCOUNT = Menu.FIRST + 3;
    private static final int ID_REMOVE_ACCOUNT = Menu.FIRST + 4;
//    private static final int ID_SIGN_OUT_ALL = Menu.FIRST + 5;
    private static final int ID_ADD_ACCOUNT = Menu.FIRST + 6;
    private static final int ID_VIEW_CONTACT_LIST = Menu.FIRST + 7;

    private ProviderAdapter mAdapter;
    private Cursor mProviderCursor;
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

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setTitle(R.string.landing_page_title);
        this.
        mApp = ImApp.getApplication(this);
        mHandler = new MyHandler(this);
        mSignInHelper = new SignInHelper(this);

        ImPluginHelper.getInstance(this).loadAvailablePlugins();

        mProviderCursor = managedQuery(Imps.Provider.CONTENT_URI_WITH_ACCOUNT, PROVIDER_PROJECTION,
                Imps.Provider.CATEGORY + "=?" /* selection */,
                new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                Imps.Provider.DEFAULT_SORT_ORDER);
        Intent intent = getIntent();

        if (ImApp.ACTION_QUIT.equals(intent.getAction())) {
            quit();
            return;
        }
        
        mAdapter = new ProviderAdapter(this, mProviderCursor);
        setListAdapter(mAdapter);

        registerForContextMenu(getListView());
    }

    @Override
    protected void onPause() {
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

        mHandler.registerForBroadcastEvents();
    }

    private void signInAccountAtPosition(int position) {
        Intent intent = null;
        mProviderCursor.moveToPosition(position);

        if (mProviderCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
            // add account
            intent = getCreateAccountIntent();
        } else {
            int state = mProviderCursor.getInt(ACCOUNT_CONNECTION_STATUS);
            long accountId = mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);

            if (state == Imps.ConnectionStatus.OFFLINE) {
                boolean isKeepSignedIn = mProviderCursor.getInt(ACTIVE_ACCOUNT_KEEP_SIGNED_IN) != 0;
                boolean isAccountEditible = mProviderCursor.getInt(ACTIVE_ACCOUNT_LOCKED) == 0;
                if (isKeepSignedIn) {
                    signIn(accountId);
                } else if (isAccountEditible) {
                    intent = getEditAccountIntent();
                }
            } else if (state == Imps.ConnectionStatus.CONNECTING) {
                gotoAccount();
            } else {
                intent = getViewChatsIntent();
            }
        }

        if (intent != null) {
            startActivity(intent);
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
            if (Log.isLoggable(TAG, Log.DEBUG))
                log("no pw for account " + accountId);
            Intent intent = getEditAccountIntent();
            startActivity(intent);
            return;
        }

        // Remember that the user signed in.
        setKeepSignedIn(accountId, true);


        long providerId = mProviderCursor.getLong(PROVIDER_ID_COLUMN);
        String password = mProviderCursor.getString(ACTIVE_ACCOUNT_PW_COLUMN);
        boolean isActive = false; // TODO(miron)
        mSignInHelper.signIn(password, providerId, accountId, isActive);
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

    boolean isSigningIn(Cursor cursor) {
        int connectionStatus = cursor.getInt(ACCOUNT_CONNECTION_STATUS);
        return connectionStatus == Imps.ConnectionStatus.CONNECTING;
    }

    private boolean isSignedIn(Cursor cursor) {
        int connectionStatus = cursor.getInt(ACCOUNT_CONNECTION_STATUS);
        return connectionStatus == Imps.ConnectionStatus.ONLINE;
    }

    private boolean allAccountsSignedOut() {
        if (!mProviderCursor.moveToFirst()) {
            return false;
        }
        do {
            if (isSignedIn(mProviderCursor)) {
                return false;
            }
        } while (mProviderCursor.moveToNext());

        return true;
    }

    private void quit() {
        if (!mProviderCursor.moveToFirst())
            return;
        
        do {
            long accountId = mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
            signOut(accountId);
        } while (mProviderCursor.moveToNext());

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    private void signOutAll() {
        DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                do {
                    long accountId = mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
                    signOut(accountId);
                } while (mProviderCursor.moveToNext());
            }
        };

        new AlertDialog.Builder(this).setTitle(R.string.confirm)
                .setMessage(R.string.signout_all_confirm_message)
                .setPositiveButton(R.string.yes, confirmListener) // default button
                .setNegativeButton(R.string.no, null).setCancelable(true).show();
    }

    private void signOut(final long accountId) {
        if (accountId == 0) {
            Log.w(TAG, "signOut: account id is 0, bail");
            return;
        }

        // Remember that the user signed out and do not auto sign in until they
        // explicitly do so
        setKeepSignedIn(accountId, false);

        // Sign out
        mApp.callWhenServiceConnected(mHandler, new Runnable() {
            public void run() {
                try {
                    IImConnection conn = mApp.getConnectionByAccount(accountId);
                    if (conn != null) {
                        conn.logout();
                    }
                } catch (RemoteException ex) {
                    Log.e(TAG, "signOut failed", ex);
                }
            }
        });
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
        menu.findItem(R.id.menu_sign_out_all).setVisible(!allAccountsSignedOut());
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
            signOutAll();
            return true;
        case R.id.menu_new_account:
            createAccount();
            return true;
        case R.id.menu_settings:
            Intent sintent = new Intent(this, SettingActivity.class);
            startActivity(sintent);
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void createAccount() {
        final ImPluginHelper helper = ImPluginHelper.getInstance(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.account_select_type);
        final String[] items = helper.getProviderNames().toArray(new String[0]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
                helper.createAdditionalProvider(items[pos]);
                mApp.resetProviderSettings();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
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
        menu.add(0, ID_VIEW_CONTACT_LIST, 0,
                brandingRes.getString(BrandingResourceIDs.STRING_MENU_CONTACT_LIST));
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
        }
    }

    
    @SuppressWarnings("deprecation")
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
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
            startActivity(getCreateAccountIntent());
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

        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        signInAccountAtPosition(position);
    }

    Intent getCreateAccountIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_INSERT);

        long providerId = mProviderCursor.getLong(PROVIDER_ID_COLUMN);
        intent.setData(ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId));
        intent.addCategory(getProviderCategory(mProviderCursor));
        return intent;
    }

    Intent getEditAccountIntent() {
        Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(
                Imps.Account.CONTENT_URI, mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN)));
        intent.addCategory(getProviderCategory(mProviderCursor));
        return intent;
    }

    Intent getViewContactsIntent() {
        Intent intent = new Intent(this, ContactListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN));
        return intent;
    }
    
    Intent getViewChatsIntent() {
        Intent intent = new Intent(this, ChatListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN));
        return intent;
    }

    private String getProviderCategory(Cursor cursor) {
        return cursor.getString(PROVIDER_CATEGORY_COLUMN);
    }

    static void log(String msg) {
        Log.d(TAG, "[LandingPage]" + msg);
    }

    private class ProviderListItemFactory implements LayoutInflater.Factory {
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            if (name != null && name.equals(ProviderListItem.class.getName())) {
                return new ProviderListItem(context, LandingPage.this);
            }
            return null;
        }
    }

    private final class ProviderAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        @SuppressWarnings("deprecation")
        public ProviderAdapter(Context context, Cursor c) {
            super(context, c);
            mInflater = LayoutInflater.from(context).cloneInContext(context);
            mInflater.setFactory(new ProviderListItemFactory());
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            // create a custom view, so we can manage it ourselves. Mainly, we want to
            // initialize the widget views (by calling getViewById()) in newView() instead of in
            // bindView(), which can be called more often.
            ProviderListItem view = (ProviderListItem) mInflater.inflate(R.layout.account_view,
                    parent, false);
            view.init(cursor);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((ProviderListItem) view).bindView(cursor);
        }
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
