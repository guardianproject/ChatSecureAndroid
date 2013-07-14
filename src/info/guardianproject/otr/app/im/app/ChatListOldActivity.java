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

import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ContactListFilterView.ContactListListener;
import info.guardianproject.otr.app.im.app.adapter.ConnectionListenerAdapter;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CursorAdapter;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
//  mScreen.finish();
//mContactListView.setAutoRefreshContacts(false);

public class ChatListOldActivity extends ThemeableActivity implements View.OnCreateContextMenuListener, ContactListListener {

    private static final int MENU_START_CONVERSATION = Menu.FIRST;
    private static final int MENU_VIEW_PROFILE = Menu.FIRST + 1;
    private static final int MENU_BLOCK_CONTACT = Menu.FIRST + 2;
    private static final int MENU_DELETE_CONTACT = Menu.FIRST + 3;
    private static final int MENU_END_CONVERSATION = Menu.FIRST + 4;

    private static final String FILTER_STATE_KEY = "Filtering";

    ImApp mApp;

   // long mProviderId;
   // long mAccountId;
   // IImConnection mConn;
    ActiveChatListView mActiveChatListView;
    ContactListFilterView mFilterView;
    
    SlidingMenu menu;
    
    ContextMenuHandler mContextMenuHandler;

    boolean mIsFiltering;
    UserPresenceView mPresenceView;
    Imps.ProviderSettings.QueryMap mGlobalSettingMap;
    boolean mDestroyed;

    private ConnectionListenerAdapter mConnectionListener;

    long[] mAccountIds;
    private long mLastProviderId = -1;
    
    Handler mHandler =  new Handler() {
      
        @Override
        public void handleMessage(Message msg) {
            /*
            long providerId = ((long) msg.arg1 << 32) | msg.arg2;
            if (providerId != mProviderId) {
                return;
            }
        */
            
            switch (msg.what) {

            case ImApp.EVENT_CONNECTION_LOGGED_IN:
                log("Connection resumed");
                //updateWarningView();
                return;
            case ImApp.EVENT_CONNECTION_SUSPENDED:
                log("Connection suspended");
               // updateWarningView();
                return;
            case ImApp.EVENT_CONNECTION_DISCONNECTED:
                log("Handle event connection disconnected.");
              //  updateWarningView();
              //  promptDisconnectedEvent(msg);
                return;
            }

            super.handleMessage(msg);
        }
    };
    
    
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        LayoutInflater inflate = getLayoutInflater();
        mActiveChatListView = (ActiveChatListView) inflate.inflate(R.layout.chat_list_view, null);
        setContentView(mActiveChatListView);
        
        getSherlock().getActionBar().setHomeButtonEnabled(true);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(true);
        
        
        mApp = (ImApp)getApplication();
        
        mGlobalSettingMap = new Imps.ProviderSettings.QueryMap(getContentResolver(), true, mHandler);

        mContextMenuHandler = new ContextMenuHandler();
        mActiveChatListView.getListView().setOnCreateContextMenuListener(this);

           // setupSideBar();
        
    }
    
    /*
    private void setupSideBar ()
    {
        
        menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.shadow);
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        menu.setFadeDegree(0.35f);
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
     
        mFilterView = (ContactListFilterView) getLayoutInflater().inflate(
                R.layout.contact_list_filter_view, null);

        mFilterView.setListener(this);
        
        mPresenceView = (UserPresenceView) mFilterView.findViewById(R.id.userPresence);
     
        mConnectionListener = new ConnectionListenerAdapter(mHandler) {
            @Override
            public void onConnectionStateChange(IImConnection connection, int state,
                    ImErrorInfo error) {
                mPresenceView.loggingIn(state == ImConnection.LOGGING_IN);                        
            }  
        };
        
        mGlobalSettingMap.addObserver(new Observer() {
            public void update(Observable observed, Object updateData) {
                if (!mDestroyed) { 
                mConnectionListener = new ConnectionListenerAdapter(mHandler) {
                    @Override
                    public void onConnectionStateChange(IImConnection connection, int state,
                            ImErrorInfo error) {
                        mPresenceView.loggingIn(state == ImConnection.LOGGING_IN);                        
                    }  
                };

                }
            }
        });
        
       
        
        menu.setMenu(mFilterView);
        
       setupActionBarList();
        
        mApp.registerForConnEvents(mHandler);

    }
    */
    
    private void initAccount (long accountId)
    {

        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId), null,
                null, null, null);
      
        if (c == null) {
           // finish();
            return;
        }
        if (!c.moveToFirst()) {
            c.close();
          //  finish();
            return;
        }

        mLastProviderId = c.getLong(c.getColumnIndexOrThrow(Imps.Account.PROVIDER));
        
        mHandler = new MyHandler(this);
        
        initConnection (accountId, mLastProviderId);
        
        c.close();
    }
    
    private void initConnection (long accountId, long providerId)
    {
        mApp.dismissNotifications(providerId);
        IImConnection conn = mApp.getConnection(providerId);
      
        if (conn == null)
        {
            try {
             conn =  mApp.createConnection(providerId, accountId);
            } catch (RemoteException e) {
               Log.e(ImApp.LOG_TAG,"error creating connection",e);
            }
        }
        
        if (conn != null)
        {
            mActiveChatListView.setConnection(conn);     
            mPresenceView.setConnection(conn);

            try {
                mPresenceView.loggingIn(conn.getState() == ImConnection.LOGGING_IN);
            } catch (RemoteException e) {
                
                mPresenceView.loggingIn(false);
            //    mHandler.showServiceErrorAlert();
            }

            Uri uri = mGlobalSettingMap.getHideOfflineContacts() ? Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY
                                                                : Imps.Contacts.CONTENT_URI_CONTACTS_BY;
            uri = ContentUris.withAppendedId(uri, providerId);
            uri = ContentUris.withAppendedId(uri, accountId);
            mFilterView.doFilter(uri, null);
            
            
           
        }        
        
      
    }
    
    private void setupActionBarList ()
    {
        
        Cursor providerCursor = managedQuery(Imps.Provider.CONTENT_URI_WITH_ACCOUNT, PROVIDER_PROJECTION,
                Imps.Provider.CATEGORY + "=?" + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL",
        
                new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                Imps.Provider.DEFAULT_SORT_ORDER);
        

        //        + " AND " + Imps.Provider.ACCOUNT_CONNECTION_STATUS + " != 0"
        
                /* selection */
        mAccountIds = new long[providerCursor.getCount()];
        
        providerCursor.moveToFirst();
        int activeAccountIdColumn = providerCursor.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_ID);

       // int currentAccountIndex = -1;
        
        for (int i = 0; i < mAccountIds.length; i++)
        {
            mAccountIds[i] = providerCursor.getLong(activeAccountIdColumn);
            providerCursor.moveToNext();
            
        }

        providerCursor.moveToFirst();

        ProviderAdapter pAdapter = new ProviderAdapter(this, providerCursor);
        
        Spinner spinnerAccounts = (Spinner)mFilterView.findViewById(R.id.spinnerAccounts);
        spinnerAccounts.setAdapter(pAdapter);
        spinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener ()
        {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int itemPosition, long id) {
               
            //    mAccountId = mAccountIds[itemPosition];
                //update account list
                initAccount(mAccountIds[itemPosition]);
              
                
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
                
            }
            
        });
        
       

    }
    
    private class ProviderAdapter extends CursorAdapter {
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
    
    private class ProviderListItemFactory implements LayoutInflater.Factory {
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            if (name != null && name.equals(ProviderListItem.class.getName())) {
                return new ProviderListItem(context, ChatListOldActivity.this);
            }
            return null;
        }
    }
    

    private void signOut (long providerId)
    {
        IImConnection conn = ((ImApp)getApplication()).getConnection(providerId);
        
        try {
            if (conn != null)
                conn.logout();
          
        } catch (RemoteException e) {

            Log.e("ChatList","error signing out",e);
        }
    }
    
    private void showContactsList ()
    {
      //  Intent intent = new Intent (this, ContactListActivity.class);
       // intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
       // startActivity(intent);
        if (menu != null)
            menu.showMenu(true);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.chat_list_menu, menu);
        return true;
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
        case R.id.menu_new_chat:
            
            showContactsList ();
            
            return true;
        case R.id.menu_new_group_chat:
            
      //      showGroupChatDialog();
            
            return true;
      
        case R.id.menu_view_accounts:
            startActivity(new Intent(getBaseContext(), ChooseAccountActivity.class));
        //    finish();
            return true;

        case R.id.menu_settings:
            Intent intent = new Intent(this, SettingActivity.class);
            startActivityForResult(intent,1);
            return true;

//        case R.id.menu_sign_out:
           // signOut();
  //          return true;
            
        }
        
        return super.onOptionsItemSelected(item);
    }

    Intent getEditAccountIntent() {

        Cursor mProviderCursor = managedQuery(Imps.Provider.CONTENT_URI_WITH_ACCOUNT,
                PROVIDER_PROJECTION, Imps.Provider.CATEGORY + "=?" /* selection */,
                new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                Imps.Provider.DEFAULT_SORT_ORDER);
        mProviderCursor.moveToFirst();

        Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(
                Imps.Account.CONTENT_URI, mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN)));
        intent.addCategory(mProviderCursor.getString(PROVIDER_CATEGORY_COLUMN));
        intent.putExtra("isSignedIn", true);

        return intent;
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

    static final int PROVIDER_CATEGORY_COLUMN = 3;
    static final int ACTIVE_ACCOUNT_ID_COLUMN = 4;

   
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        
        //this is bad code for weird locale switching stuff
        /**
        
        if (requestCode == 1 && resultCode == 2)
        {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
            
        }*/
        
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

        if (mIsFiltering) {
            handled = mFilterView.dispatchKeyEvent(event);
            if (!handled && (KeyEvent.KEYCODE_BACK == keyCode)
                && (KeyEvent.ACTION_DOWN == event.getAction())) {
                showChatListView();
                handled = true;
            }
        } else {
            handled = mActiveChatListView.dispatchKeyEvent(event);
            if (!handled && isReadable(keyCode, event)
                && (KeyEvent.ACTION_DOWN == event.getAction())) {
                showFilterView();
                handled = mFilterView.dispatchKeyEvent(event);
            }
        }

        if (!handled) {
            handled = super.dispatchKeyEvent(event);
        }

        return handled;
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

        if (mFilterView == null) {
            mFilterView = (ContactListFilterView) getLayoutInflater().inflate(
                    R.layout.contact_list_filter_view, null);
            mFilterView.getListView().setOnCreateContextMenuListener(this);
        }
        Uri uri = mGlobalSettingMap.getHideOfflineContacts() ? Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY
                                                            : Imps.Contacts.CONTENT_URI_CONTACTS_BY;
        //uri = ContentUris.withAppendedId(uri, mProviderId);
        //uri = ContentUris.withAppendedId(uri, mAccountId);
        
        mFilterView.doFilter(uri, null);

        try
        {
            setContentView(mFilterView);
            mFilterView.requestFocus();
        }
        catch (Exception e) {
           Log.d(ImApp.LOG_TAG,"error switching view",e);
        }
        
        mIsFiltering = true;
    }

    void showChatListView() {
        if (mIsFiltering) {
            setContentView(mActiveChatListView);
            mActiveChatListView.requestFocus();
            mActiveChatListView.invalidate();
            mIsFiltering = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApp.unregisterForConnEvents(mHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        ((ImApp)getApplication()).startImServiceIfNeed(true);
        
        mApp.registerForConnEvents(mHandler);
        //mActiveChatListView.setAutoRefreshContacts(true);
        
    }

    @Override
    protected void onDestroy() {
        mDestroyed = true;
        // set connection to null to unregister listeners.
        mActiveChatListView.setConnection(null);
        if (mGlobalSettingMap != null) {
            mGlobalSettingMap.close();
        }
        super.onDestroy();
    }

    static void log(String msg) {
        Log.d(ImApp.LOG_TAG, "<ContactListActivity> " + msg);
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
            chatSelected = true;
        } else {

            if (menuInfo instanceof ExpandableListContextMenuInfo) {
                ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
                mContextMenuHandler.mPosition = info.packedPosition;
                contactSelected = false;
                chatSelected = mActiveChatListView.isConversationAtPosition(info.packedPosition);
                contactCursor = null;
            } else if (menuInfo instanceof AdapterContextMenuInfo) {
                AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
                mContextMenuHandler.mPosition = info.position;
                contactSelected = false;
                contactCursor = null;
                chatSelected = mActiveChatListView.isConversationAtPosition(info.position);

            } else
                contactCursor = null;
        }

        /*
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
*/
        
        /*
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
        }*/
    }
    
    /*
    void clearConnectionStatus() {
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues(3);

        values.put(Imps.AccountStatus.ACCOUNT, mAccountId);
        values.put(Imps.AccountStatus.PRESENCE_STATUS, Imps.Presence.OFFLINE);
        values.put(Imps.AccountStatus.CONNECTION_STATUS, Imps.ConnectionStatus.OFFLINE);
        // insert on the "account_status" uri actually replaces the existing value 
        cr.insert(Imps.AccountStatus.CONTENT_URI, values);
    }*/

    final class ContextMenuHandler implements android.view.MenuItem.OnMenuItemClickListener {
        long mPosition;

        @Override
        public boolean onMenuItemClick(android.view.MenuItem item) {
            
            if (item.getItemId() == MENU_END_CONVERSATION)
            {
                
                Cursor c = (Cursor)mActiveChatListView.getListView().getAdapter().getItem((int) mPosition);

                long providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Account.PROVIDER));
                IImConnection conn = ((ImApp)getApplication()).getConnection(providerId);
                
                mActiveChatListView.endChat(c, conn);
                
            }
            return false;
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
            }
            super.handleMessage(msg);
        }
    }
    

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        if (mActiveChatListView != null && mActiveChatListView.getListView().getCount() == 0)
        {
                
            View empty = findViewById(R.id.empty);
            
            if (empty != null)
            {
                empty.setOnClickListener(new OnClickListener (){
        
                    @Override
                    public void onClick(View arg0) {
                        showContactsList ();
                        
                    }
                
                });
                
                ListView list = (ListView) findViewById(R.id.chatsList);
                list.setEmptyView(empty);
            }
        }
    }
    

    @Override
    public void startChat(Cursor c) {

        if (c != null) {
            long id = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts._ID));
            String username = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            
            long providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts.PROVIDER));
            IImConnection conn = ((ImApp)getApplication()).getConnection(providerId);
            
            try {
                IChatSessionManager manager = conn.getChatSessionManager();
                IChatSession session = manager.getChatSession(username);
                if (session == null) {
                    manager.createChatSession(username);
                }

                Uri data = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, id);
                Intent i = new Intent(Intent.ACTION_VIEW, data);
                i.addCategory(ImApp.IMPS_CATEGORY);
                
                if (menu.isShown())
                    menu.toggle();
                
                startActivity(i);
                
            } catch (RemoteException e) {
              //  mHandler.showServiceErrorAlert();
            }
           
        }
        
        
    }
}
