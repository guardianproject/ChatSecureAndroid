/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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
package org.gitian.android.im.app;

import java.util.Observable;
import java.util.Observer;

import org.gitian.android.im.R;
import org.gitian.android.im.IImConnection;
import org.gitian.android.im.plugin.BrandingResourceIDs;
import org.gitian.android.im.provider.Imps;
import org.gitian.android.im.service.ImServiceConstants;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class ContactListActivity extends Activity implements View.OnCreateContextMenuListener{

    private static final int MENU_START_CONVERSATION = Menu.FIRST;
    private static final int MENU_VIEW_PROFILE       = Menu.FIRST + 1;
    private static final int MENU_BLOCK_CONTACT      = Menu.FIRST + 2;
    private static final int MENU_DELETE_CONTACT     = Menu.FIRST + 3;
    private static final int MENU_END_CONVERSATION   = Menu.FIRST + 4;

    private static final String FILTER_STATE_KEY = "Filtering";

    ImApp mApp;

    long mProviderId;
    long mAccountId;
    IImConnection mConn;
    ContactListView mContactListView;
    ContactListFilterView mFilterView;
    SimpleAlertHandler mHandler;

    ContextMenuHandler mContextMenuHandler;

    boolean mIsFiltering;

    Imps.ProviderSettings.QueryMap mSettingMap;
    boolean mDestroyed;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

        LayoutInflater inflate = getLayoutInflater();
        mContactListView = (ContactListView) inflate.inflate(
                R.layout.contact_list_view, null);

        setContentView(mContactListView);

        Intent intent = getIntent();
        mAccountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, -1);
        if (mAccountId == -1) {
            finish();
            return;
        }
        mApp = ImApp.getApplication(this);

        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(ContentUris.withAppendedId(Imps.Account.CONTENT_URI, mAccountId),
                null, null, null, null);
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

        BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
        setTitle(brandingRes.getString(
                BrandingResourceIDs.STRING_BUDDY_LIST_TITLE, username));
        getWindow().setFeatureDrawable(Window.FEATURE_LEFT_ICON,
                brandingRes.getDrawable(BrandingResourceIDs.DRAWABLE_LOGO));

        mSettingMap = new Imps.ProviderSettings.QueryMap(
                getContentResolver(), mProviderId, true, null);

        mApp.callWhenServiceConnected(mHandler, new Runnable(){
            public void run() {
                if (!mDestroyed) {
                    mApp.dismissNotifications(mProviderId);
                    mConn = mApp.getConnection(mProviderId);
                    if (mConn == null) {
                        Log.e(ImApp.LOG_TAG, "The connection has disappeared!");
                        clearConnectionStatus();
                        finish();
                    } else {
                        mContactListView.setConnection(mConn);
                        mContactListView.setHideOfflineContacts(
                                mSettingMap.getHideOfflineContacts());
                    }
                }
            }
        });

        mContextMenuHandler = new ContextMenuHandler();
        mContactListView.getListView().setOnCreateContextMenuListener(this);

        mSettingMap.addObserver(new Observer() {
            public void update(Observable observed, Object updateData) {
                if (!mDestroyed) {
                    mContactListView.setHideOfflineContacts(
                            mSettingMap.getHideOfflineContacts());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_list_menu, menu);

        BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
        menu.findItem(R.id.menu_invite_user).setTitle(
            brandingRes.getString(BrandingResourceIDs.STRING_MENU_ADD_CONTACT));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_invite_user:
                Intent i = new Intent(ContactListActivity.this, AddContactActivity.class);
                i.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
                i.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
                i.putExtra(ImServiceConstants.EXTRA_INTENT_LIST_NAME,
                        mContactListView.getSelectedContactList());
                startActivity(i);
                return true;

            case R.id.menu_blocked_contacts:
                Uri.Builder builder = Imps.BlockedList.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, mProviderId);
                ContentUris.appendId(builder, mAccountId);
                startActivity(new Intent(Intent.ACTION_VIEW, builder.build()));
                return true;

            case R.id.menu_view_accounts:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType(Imps.Provider.CONTENT_TYPE);
                startActivity(intent);
                finish();
                return true;

            case R.id.menu_settings:
                intent = new Intent(this, SettingActivity.class);
                intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
                startActivity(intent);
                return true;

            case R.id.menu_sign_out:
                try {
                    if (mConn != null) {
                        mConn.logout();
                    }
                } catch (RemoteException e) {
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                showContactListView();
                handled = true;
            }
        } else {
            handled = mContactListView.dispatchKeyEvent(event);
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
        if (mFilterView == null ) {
            mFilterView = (ContactListFilterView)getLayoutInflater().inflate(
                    R.layout.contact_list_filter_view, null);
            mFilterView.getListView().setOnCreateContextMenuListener(this);
        }
        Uri uri = mSettingMap.getHideOfflineContacts() ? Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY
                : Imps.Contacts.CONTENT_URI_CONTACTS_BY;
        uri = ContentUris.withAppendedId(uri, mProviderId);
        uri = ContentUris.withAppendedId(uri, mAccountId);
        mFilterView.doFilter(uri, null);

        setContentView(mFilterView);
        mFilterView.requestFocus();
        mIsFiltering = true;
    }

    void showContactListView() {
        if (mIsFiltering) {
            setContentView(mContactListView);
            mContactListView.requestFocus();
            mContactListView.invalidate();
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
        mApp.registerForConnEvents(mHandler);
        mContactListView.setAutoRefreshContacts(true);
    }

    @Override
    protected void onDestroy() {
        mDestroyed = true;
        // set connection to null to unregister listeners.
        mContactListView.setConnection(null);
        if (mSettingMap != null) {
            mSettingMap.close();
        }
        super.onDestroy();
    }

    static void log(String msg) {
        Log.d(ImApp.LOG_TAG, "<ContactListActivity> " +msg);
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
            chatSelected = mContactListView.isConversationAtPosition(info.packedPosition);
            contactCursor = mContactListView.getContactAtPosition(info.packedPosition);
        }

        boolean allowBlock = true;
        if (contactCursor != null) {
            //XXX HACK: Yahoo! doesn't allow to block a friend. We can only block a temporary contact.
            ProviderDef provider = mApp.getProvider(mProviderId);
            if (Imps.ProviderNames.YAHOO.equals(provider.mName)) {
                int type = contactCursor.getInt(contactCursor.getColumnIndexOrThrow(Imps.Contacts.TYPE));
                allowBlock = (type == Imps.Contacts.TYPE_TEMPORARY);
            }

            int nickNameIndex = contactCursor.getColumnIndexOrThrow(Imps.Contacts.NICKNAME);

            menu.setHeaderTitle(contactCursor.getString(nickNameIndex));
        }

        BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
        String menu_end_conversation = brandingRes.getString(
                BrandingResourceIDs.STRING_MENU_END_CHAT);
        String menu_view_profile = brandingRes.getString(
                BrandingResourceIDs.STRING_MENU_VIEW_PROFILE);
        String menu_block_contact = brandingRes.getString(
                BrandingResourceIDs.STRING_MENU_BLOCK_CONTACT);
        String menu_start_conversation = brandingRes.getString(
                BrandingResourceIDs.STRING_MENU_START_CHAT);
        String menu_delete_contact = brandingRes.getString(
                BrandingResourceIDs.STRING_MENU_DELETE_CONTACT);

        if (chatSelected) {
            menu.add(0, MENU_END_CONVERSATION, 0, menu_end_conversation)
                    //TODO .setIcon(org.gitian.android.internal.R.drawable.ic_menu_end_conversation)
                    .setOnMenuItemClickListener(mContextMenuHandler);
            menu.add(0, MENU_VIEW_PROFILE, 0, menu_view_profile)
                    .setIcon(R.drawable.ic_menu_my_profile)
                    .setOnMenuItemClickListener(mContextMenuHandler);
            if (allowBlock) {
                menu.add(0, MENU_BLOCK_CONTACT, 0, menu_block_contact)
                        //.setIcon(org.gitian.android.internal.R.drawable.ic_menu_block)
                        .setOnMenuItemClickListener(mContextMenuHandler);
            }
        } else if (contactSelected) {
            menu.add(0, MENU_START_CONVERSATION, 0, menu_start_conversation)
                    //.setIcon(org.gitian.android.internal.R.drawable.ic_menu_start_conversation)
                    .setOnMenuItemClickListener(mContextMenuHandler);
            menu.add(0, MENU_VIEW_PROFILE, 0, menu_view_profile)
                    .setIcon(R.drawable.ic_menu_view_profile)
                    .setOnMenuItemClickListener(mContextMenuHandler);
            if (allowBlock) {
                menu.add(0, MENU_BLOCK_CONTACT, 0, menu_block_contact)
                        //.setIcon(org.gitian.android.internal.R.drawable.ic_menu_block)
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

    final class ContextMenuHandler implements MenuItem.OnMenuItemClickListener {
        long mPosition;

        public boolean onMenuItemClick(MenuItem item) {
            Cursor c;
            if (mIsFiltering) {
                c = mFilterView.getContactAtPosition((int)mPosition);
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
                long providerId = ((long)msg.arg1 << 32) | msg.arg2;
                if (providerId == mProviderId) {
                    if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                        log("Current connection disconnected, finish");
                    }
                    finish();
                }
                return;
            }
            super.handleMessage(msg);
        }
    }
}
