/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
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

import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.SearchView;
import android.support.v4.widget.SearchViewCompat;
import android.support.v4.widget.SearchViewCompat.OnQueryTextListenerCompat;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ContactListActivity extends SherlockActivity implements View.OnCreateContextMenuListener {

    private static final int MENU_START_CONVERSATION = Menu.FIRST;
    private static final int MENU_VIEW_PROFILE = Menu.FIRST + 1;
    private static final int MENU_BLOCK_CONTACT = Menu.FIRST + 2;
    private static final int MENU_DELETE_CONTACT = Menu.FIRST + 3;
    private static final int MENU_END_CONVERSATION = Menu.FIRST + 4;

    private static final String FILTER_STATE_KEY = "Filtering";

    ImApp mApp;

    ProviderAdapter mAdapter; 
    
    long mProviderId;
    long mAccountId;
    IImConnection mConn;
    ContactListView mContactListView;
    ContactListFilterView mFilterView;
    SimpleAlertHandler mHandler;

    ContextMenuHandler mContextMenuHandler;

    boolean mIsFiltering = true;

    Imps.ProviderSettings.QueryMap mGlobalSettingMap;
    boolean mDestroyed;
    
    View mSearchView;
    

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        LayoutInflater inflate = getLayoutInflater();

        mContactListView = (ContactListView) inflate.inflate(R.layout.contact_list_view, null);

        mFilterView = (ContactListFilterView) getLayoutInflater().inflate(
                R.layout.contact_list_filter_view, null);

        mFilterView.setActivity(this);

        mFilterView.getListView().setOnCreateContextMenuListener(this);

        Intent intent = getIntent();
        mAccountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, -1);
        if (mAccountId == -1) {
            finish();
            return;
        }
        
        setupActionBarList (mAccountId);
        

        mApp = ImApp.getApplication(this);
        
        initAccount ();
        

        // Get the intent, verify the action and get the query

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            if (mIsFiltering) {
                String filterText = intent.getStringExtra(SearchManager.QUERY);
                mFilterView.doFilter(filterText);
            }
        }
    }
    
    private void initAccount ()
    {

               

        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(ContentUris.withAppendedId(Imps.Account.CONTENT_URI, mAccountId), null,
                null, null, null);
        if (c == null) {
            finish();
            return;
        }
        if (!c.moveToFirst()) {
            c.close();
            finish();
            return;
        }

        mProviderId = c.getLong(c.getColumnIndexOrThrow(Imps.Account.PROVIDER));
        mHandler = new MyHandler(this);

        String username = c.getString(c.getColumnIndexOrThrow(Imps.Account.USERNAME));

        c.close();

       // BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
       // setTitle(brandingRes.getString(BrandingResourceIDs.STRING_BUDDY_LIST_TITLE, username));
        setTitle(username);
       // getWindow().setFeatureDrawable(Window.FEATURE_LEFT_ICON,
         //       brandingRes.getDrawable(BrandingResourceIDs.DRAWABLE_LOGO));

        mGlobalSettingMap = new Imps.ProviderSettings.QueryMap(getContentResolver(), true, null);

        mApp.callWhenServiceConnected(mHandler, new Runnable() {
            public void run() {
                if (!mDestroyed) {
                    mApp.dismissNotifications(mProviderId);
                    mConn = mApp.getConnection(mProviderId);
                    if (mConn == null) {
                        clearConnectionStatus();
                        try {
                            mConn = mApp.createConnection(mProviderId, mAccountId);
                        } catch (RemoteException e) {
                            Log.e(ImApp.LOG_TAG, "The connection cannot be created");
                            finish();
                        }
                    }
                  //  mFilterView.mPresenceView.setConnection(mConn);
                    mFilterView.setConnection(mConn);
                    mContactListView.setConnection(mConn);
                    mContactListView.setHideOfflineContacts(mGlobalSettingMap
                            .getHideOfflineContacts());
                }
            }
        });

        mContextMenuHandler = new ContextMenuHandler();
        mContactListView.getListView().setOnCreateContextMenuListener(this);

        mGlobalSettingMap.addObserver(new Observer() {
            public void update(Observable observed, Object updateData) {
                if (!mDestroyed) {
                    mContactListView.setHideOfflineContacts(mGlobalSettingMap
                            .getHideOfflineContacts());
                }
            }
        });

        showFilterView();

    }
    
    long[] mAccountIds;
    
    private void setupActionBarList (long accountId)
    {

        getSherlock().getActionBar().setHomeButtonEnabled(true);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlock().getActionBar().setTitle("");
        
        Cursor mProviderCursor = managedQuery(Imps.Provider.CONTENT_URI_WITH_ACCOUNT, PROVIDER_PROJECTION,
                Imps.Provider.CATEGORY + "=?" /* selection */,
                new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                Imps.Provider.DEFAULT_SORT_ORDER);
        
        mAccountIds = new long[mProviderCursor.getCount()];
        
        mProviderCursor.moveToFirst();
        int activeAccountIdColumn = mProviderCursor.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_ID);

        int currentAccountIndex = -1;
        
        for (int i = 0; i < mAccountIds.length; i++)
        {
            mAccountIds[i] = mProviderCursor.getLong(activeAccountIdColumn);
            mProviderCursor.moveToNext();
            
            if (mAccountIds[i] == mAccountId)
                currentAccountIndex = i;
        }

        mProviderCursor.moveToFirst();
        
        mAdapter = new ProviderAdapter(this, mProviderCursor);
        
        this.getSherlock().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        this.getSherlock().getActionBar().setListNavigationCallbacks(mAdapter, new OnNavigationListener () {

            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                
                if (mAccountIds[itemPosition] != mAccountId)
                {
                    mAccountId = mAccountIds[itemPosition];
                    initAccount ();
                    
                    
                }
                
                return false;
            }
            
        });
        
        
        getSherlock().getActionBar().setSelectedNavigationItem(currentAccountIndex);


    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.contact_list_menu, menu);
        
        mSearchView = SearchViewCompat.newSearchView(this);
        
        if (mSearchView != null)
        {
            MenuItem item = menu.add("Search")
                    .setIcon(android.R.drawable.ic_menu_search)
                    .setActionView(mSearchView);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
    
            SearchViewCompat.setOnQueryTextListener(mSearchView, new SearchViewCompat.OnQueryTextListenerCompat() {

                @Override
                public boolean onQueryTextChange(String newText) {
                    mFilterView.doFilter(newText);
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    mFilterView.doFilter(query);
                    return true;
                }
                
                
            });
        
            
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        //TODO make sure this works

        case R.id.menu_invite_user:
            Intent i = new Intent(ContactListActivity.this, AddContactActivity.class);
            i.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
            i.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
            i.putExtra(ImServiceConstants.EXTRA_INTENT_LIST_NAME,
                    mContactListView.getSelectedContactList());
            startActivity(i);
            return true;

        case android.R.id.home:
        case R.id.menu_view_accounts:
            startActivity(new Intent(getBaseContext(), ChooseAccountActivity.class));
            finish();
            return true;

        case R.id.menu_settings:
            Intent sintent = new Intent(this, SettingActivity.class);
            startActivity(sintent);
            return true;

        case R.id.menu_quit:
            handleQuit();
            return true;

            
        case R.id.menu_view_groups:
            if (mIsFiltering)
                showContactListView();
            else
                showFilterView();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    Intent getEditAccountIntent(boolean isSignedIn) {
        Uri uri = ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, mProviderId);

        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri,
                new String[] { Imps.Provider.CATEGORY }, null, null, null);
        cursor.moveToFirst();

        Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(
                Imps.Account.CONTENT_URI, mAccountId));
        intent.addCategory(cursor.getString(0));
        cursor.close();
        intent.putExtra("isSignedIn", isSignedIn);

        return intent;
    }

    private void handleQuit() {
        Intent intent = new Intent(ImApp.ACTION_QUIT);
        intent.setType(Imps.Provider.CONTENT_TYPE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(FILTER_STATE_KEY, mIsFiltering);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        boolean isFiltering = savedInstanceState.getBoolean(FILTER_STATE_KEY);
        if (isFiltering) {
            showFilterView();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        boolean handled = false;

        if (!mIsFiltering) {
            handled = mFilterView.dispatchKeyEvent(event);
            if (!handled && (KeyEvent.KEYCODE_BACK == keyCode)
                && (KeyEvent.ACTION_DOWN == event.getAction())) {
                showFilterView();
                handled = true;
            }
        } else {

            handled = mContactListView.dispatchKeyEvent(event);

            if (!handled && KeyEvent.KEYCODE_SEARCH == keyCode
                && (KeyEvent.ACTION_DOWN == event.getAction())) {
                InputMethodManager inputMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMgr.toggleSoftInput(0, 0);

                if (!mIsFiltering)
                    showFilterView();

                onSearchRequested();
            } else if (!handled && isReadable(keyCode, event)
                       && (KeyEvent.ACTION_DOWN == event.getAction())) {

                if (!mIsFiltering)
                    showFilterView();

                handled = mFilterView.dispatchKeyEvent(event);
            }

        }

        if (!handled) {
            handled = super.dispatchKeyEvent(event);
        }

        return handled;
    }


    @Override
    protected void onNewIntent(Intent intent) {

        // The user has probably entered a URL into "Go"

        String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {

            if (mIsFiltering) {
                String filterText = intent.getStringExtra(SearchManager.QUERY);

                mFilterView.doFilter(filterText);
            }
        }
    }

    private static boolean isReadable(int keyCode, KeyEvent event) {
        if (KeyEvent.isModifierKey(keyCode) || event.isSystem()) {
            return false;
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_ENTER:
            return false;
        }

        return true;
    }

    private void showFilterView() {

        Uri uri = mGlobalSettingMap.getHideOfflineContacts() ? Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY
                                                            : Imps.Contacts.CONTENT_URI_CONTACTS_BY;
        uri = ContentUris.withAppendedId(uri, mProviderId);
        uri = ContentUris.withAppendedId(uri, mAccountId);
        mFilterView.doFilter(uri, null);

        setContentView(mFilterView);
        mFilterView.requestFocus();
        mIsFiltering = true;
    }

    void showContactListView() {

        setContentView(mContactListView);
        mContactListView.requestFocus();
        mContactListView.invalidate();
        mIsFiltering = false;

    }

    @Override
    protected void onPause() {
        super.onPause();
        mApp.unregisterForConnEvents(mHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mApp.registerForConnEvents(mHandler);
        mContactListView.setAutoRefreshContacts(true);

    }

    @Override
    protected void onDestroy() {
        mDestroyed = true;
        // set connection to null to unregister listeners.
        mContactListView.setConnection(null);
        mFilterView.setConnection(null);
        if (mGlobalSettingMap != null) {
            mGlobalSettingMap.close();
        }
        super.onDestroy();
    }

    static void log(String msg) {
        Log.v(ImApp.LOG_TAG, "<ContactListActivity> " + msg);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        boolean chatSelected = false;
        boolean contactSelected = false;
        Cursor contactCursor;
        if (mIsFiltering) {
            AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            mContextMenuHandler.mPosition = info.position;
            contactSelected = true;
            contactCursor = mFilterView.getContactAtPosition(info.position);
        } else {
            ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
            mContextMenuHandler.mPosition = info.packedPosition;
            contactSelected = mContactListView.isContactAtPosition(info.packedPosition);
            contactCursor = mContactListView.getContactAtPosition(info.packedPosition);
        }

        boolean allowBlock = true;
        if (contactCursor != null) {
            //XXX HACK: Yahoo! doesn't allow to block a friend. We can only block a temporary contact.
            ProviderDef provider = mApp.getProvider(mProviderId);
            if (Imps.ProviderNames.YAHOO.equals(provider.mName)) {
                int type = contactCursor.getInt(contactCursor
                        .getColumnIndexOrThrow(Imps.Contacts.TYPE));
                allowBlock = (type == Imps.Contacts.TYPE_TEMPORARY);
            }

            int nickNameIndex = contactCursor.getColumnIndexOrThrow(Imps.Contacts.NICKNAME);

            menu.setHeaderTitle(contactCursor.getString(nickNameIndex));
        }

        BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
        String menu_end_conversation = brandingRes
                .getString(BrandingResourceIDs.STRING_MENU_END_CHAT);
        String menu_view_profile = brandingRes
                .getString(BrandingResourceIDs.STRING_MENU_VIEW_PROFILE);
        String menu_block_contact = brandingRes
                .getString(BrandingResourceIDs.STRING_MENU_BLOCK_CONTACT);
        String menu_start_conversation = brandingRes
                .getString(BrandingResourceIDs.STRING_MENU_START_CHAT);
        String menu_delete_contact = brandingRes
                .getString(BrandingResourceIDs.STRING_MENU_DELETE_CONTACT);

        if (chatSelected) {
            menu.add(0, MENU_END_CONVERSATION, 0, menu_end_conversation)
                    .setOnMenuItemClickListener(mContextMenuHandler);
            menu.add(0, MENU_VIEW_PROFILE, 0, menu_view_profile)
                    .setIcon(R.drawable.ic_menu_my_profile)
                    .setOnMenuItemClickListener(mContextMenuHandler);
            if (allowBlock) {
                menu.add(0, MENU_BLOCK_CONTACT, 0, menu_block_contact)
                        .setOnMenuItemClickListener(mContextMenuHandler);
            }
        } else if (contactSelected) {
            menu.add(0, MENU_START_CONVERSATION, 0, menu_start_conversation)
                    .setOnMenuItemClickListener(mContextMenuHandler);
            menu.add(0, MENU_VIEW_PROFILE, 0, menu_view_profile)
                    .setIcon(R.drawable.ic_menu_view_profile)
                    .setOnMenuItemClickListener(mContextMenuHandler);
            if (allowBlock) {
                menu.add(0, MENU_BLOCK_CONTACT, 0, menu_block_contact)
                        .setOnMenuItemClickListener(mContextMenuHandler);
            }
            menu.add(0, MENU_DELETE_CONTACT, 0, menu_delete_contact)
                    .setIcon(android.R.drawable.ic_menu_delete)
                    .setOnMenuItemClickListener(mContextMenuHandler);
        }
    }

    void clearConnectionStatus() {
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues(3);

        values.put(Imps.AccountStatus.ACCOUNT, mAccountId);
        values.put(Imps.AccountStatus.PRESENCE_STATUS, Imps.Presence.OFFLINE);
        values.put(Imps.AccountStatus.CONNECTION_STATUS, Imps.ConnectionStatus.OFFLINE);
        // insert on the "account_status" uri actually replaces the existing value 
        cr.insert(Imps.AccountStatus.CONTENT_URI, values);
    }

    final class ContextMenuHandler implements MenuItem.OnMenuItemClickListener, OnMenuItemClickListener {
        long mPosition;

        public boolean onMenuItemClick(MenuItem item) {
            Cursor c;
            if (mIsFiltering) {
                c = mFilterView.getContactAtPosition((int) mPosition);
            } else {
                c = mContactListView.getContactAtPosition(mPosition);
            }

            switch (item.getItemId()) {
            case MENU_START_CONVERSATION:
                mContactListView.startChat(c);
                break;
            case MENU_VIEW_PROFILE:
                mContactListView.viewContactPresence(c);
                break;
            case MENU_BLOCK_CONTACT:
                mContactListView.blockContact(c);
                break;
            case MENU_DELETE_CONTACT:
                mContactListView.removeContact(c);
                break;
            case MENU_END_CONVERSATION:
                mContactListView.endChat(c);
                break;
            default:
                return false;
            }

            if (mIsFiltering) {
                showContactListView();
            }
            return true;
        }

        @Override
        public boolean onMenuItemClick(android.view.MenuItem item) {
            Cursor c;
            if (mIsFiltering) {
                c = mFilterView.getContactAtPosition((int) mPosition);
            } else {
                c = mContactListView.getContactAtPosition(mPosition);
            }

            switch (item.getItemId()) {
            case MENU_START_CONVERSATION:
                mContactListView.startChat(c);
                break;
            case MENU_VIEW_PROFILE:
                mContactListView.viewContactPresence(c);
                break;
            case MENU_BLOCK_CONTACT:
                mContactListView.blockContact(c);
                break;
            case MENU_DELETE_CONTACT:
                mContactListView.removeContact(c);
                break;
            case MENU_END_CONVERSATION:
                mContactListView.endChat(c);
                break;
            default:
                return false;
            }

            if (mIsFiltering) {
                showContactListView();
            }
            return true;
        }
    }

    final class MyHandler extends SimpleAlertHandler {
        public MyHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ImApp.EVENT_CONNECTION_DISCONNECTED) {
                if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                    log("Handle event connection disconnected.");
                }
                promptDisconnectedEvent(msg);
                long providerId = ((long) msg.arg1 << 32) | msg.arg2;
                if (providerId == mProviderId) {
                    if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                        log("Current connection disconnected, finish");
                    }

                    startActivity(getEditAccountIntent(false));

                    finish();
                }
                return;
            }
            super.handleMessage(msg);
        }
    }
    
    
    private class ProviderListItemFactory implements LayoutInflater.Factory {
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            if (name != null && name.equals(ProviderListItem.class.getName())) {
                return new ProviderListItem(context, ContactListActivity.this);
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
            ProviderListItem view = (ProviderListItem) mInflater.inflate(R.layout.account_view_small,
                    parent, false);
            view.init(cursor);
            return view;
        }
        
        

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((ProviderListItem) view).bindView(cursor);
        }
    }

    private void setupSearchView(MenuItem searchItem) {
        
      
        
        
    }
  
    public boolean onClose() {
        
        return false;
    }
    protected boolean isAlwaysExpanded() {
        return false;
    }
    
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
}
