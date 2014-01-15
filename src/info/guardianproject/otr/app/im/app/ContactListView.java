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
import info.guardianproject.otr.app.im.IContactListListener;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.ISubscriptionListener;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.adapter.ContactListListenerAdapter;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ContactListManager;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.util.LogCleaner;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;

public class ContactListView extends LinearLayout {

    Activity mScreen;
    IImConnection mConn;
    SimpleAlertHandler mHandler;
    Context mContext;
    private final IContactListListener mContactListListener;

    ExpandableListView mContactsList;
    private ContactListTreeAdapter mAdapter;
    private boolean mHideOfflineContacts;
    private SavedState mSavedState;
    private boolean mAutoRefresh = true;

    public ContactListView(Context screen, AttributeSet attrs) {
        super(screen, attrs);
        mContext = screen;
        mScreen = (Activity) screen;
        mHandler = new SimpleAlertHandler(mScreen);
        mContactListListener = new MyContactListListener(mHandler);
    }

    private class MyContactListListener extends ContactListListenerAdapter {
        public MyContactListListener(SimpleAlertHandler handler) {
            super(handler);
        }

        @Override
        public void onAllContactListsLoaded() {
            if (mAdapter != null) {
                mAdapter.startAutoRequery();
            }
        }
    }

    private final ISubscriptionListener.Stub mSubscriptionListener = new ISubscriptionListener.Stub() {

        public void onSubScriptionRequest(Contact from, long providerId, long accountId) {
            querySubscription();
        }

        public void onSubscriptionApproved(Contact contact, long providerId, long accountId) {
            querySubscription();
        }

        public void onSubscriptionDeclined(Contact contact, long providerId, long accountId) {
            querySubscription();
        }

        private void querySubscription() {
            if (mAdapter != null) {
                mAdapter.startQuerySubscriptions();
            }
        }
    };

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContactsList = (ExpandableListView) findViewById(R.id.contactsList);
        mContactsList.setOnChildClickListener(mOnChildClickListener);
    }

    public ExpandableListView getListView() {
        return mContactsList;
    }

    public void setConnection(IImConnection conn) {
        if (mConn != conn) {
            if (mConn != null) {
                unregisterListeners();
            }
            mConn = conn;

            if (conn != null) {
                registerListeners();

                if (mAdapter == null) {
                    mAdapter = new ContactListTreeAdapter(conn, mScreen);
                    mAdapter.setHideOfflineContacts(mHideOfflineContacts);
                    mContactsList.setAdapter(mAdapter);
                    mContactsList.setOnScrollListener(mAdapter);
                    if (mSavedState != null) {
                        int[] expandedGroups = mSavedState.mExpandedGroups;
                        if (expandedGroups != null) {
                            for (int group : expandedGroups) {
                                mContactsList.expandGroup(group);
                            }
                        }
                    }
                } else {
                    mAdapter.changeConnection(conn);
                }
                try {
                    IContactListManager listMgr = conn.getContactListManager();
                    if (listMgr.getState() == ContactListManager.LISTS_LOADED) {
                        mAdapter.startAutoRequery();
                    }
                } catch (RemoteException e) {
                    Log.e(ImApp.LOG_TAG, "Service died!");
                }
            }
        } else {
            mContactsList.invalidateViews();
        }
    }

    public void setHideOfflineContacts(boolean hide) {
        if (mAdapter != null) {
            mAdapter.setHideOfflineContacts(hide);
        } else {
            mHideOfflineContacts = hide;
        }
    }

    public void startChat() {
        startChat(getSelectedContact());
    }

    public void startChatAtPosition(long packedPosition) {
        startChat(getContactAtPosition(packedPosition));
    }

    void startChat(Cursor c) {
        if (c != null) {
            long id = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts._ID));
            String username = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            try {
                IChatSessionManager manager = mConn.getChatSessionManager();
                IChatSession session = manager.getChatSession(username);
                if (session == null) {
                    manager.createChatSession(username);
                }

                Uri data = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, id);
                Intent i = new Intent(Intent.ACTION_VIEW, data);
                i.addCategory(ImApp.IMPS_CATEGORY);
                
                mScreen.startActivity(i);
              //  mScreen.finish();
                
                setAutoRefreshContacts(false);
            } catch (RemoteException e) {
                
            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "remote error",e);
            }
            clearFocusIfEmpty(c);
        }
    }

    private void clearFocusIfEmpty(Cursor c) {
        // clear focus if there's only one item so that it would focus on the
        // "empty" item after the contact removed.
        if (c.getCount() == 1) {
            clearFocus();
        }
    }

    public void endChat() {
        endChat(getSelectedContact());
    }

    public void endChatAtPosition(long packedPosition) {
        endChat(getContactAtPosition(packedPosition));
    }

    void endChat(Cursor c) {
        if (c != null) {
            String username = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            try {
                IChatSessionManager manager = mConn.getChatSessionManager();
                IChatSession session = manager.getChatSession(username);
                if (session != null) {
                    session.leave();
                }
            } catch (RemoteException e) {
                
            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "remote error",e);
            }
            clearFocusIfEmpty(c);
        }
    }

    public void viewContactPresence() {
        viewContactPresence(getSelectedContact());
    }

    public void viewContactPresenceAtPostion(long packedPosition) {
        viewContactPresence(getContactAtPosition(packedPosition));
    }

    public void viewContactPresence(Cursor c) {
        if (c != null) {
            long id = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts._ID));
            Uri data = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, id);
            Intent i = new Intent(Intent.ACTION_VIEW, data);
            mScreen.startActivity(i);
        }
    }

    public boolean isContactAtPosition(long packedPosition) {
        int type = ExpandableListView.getPackedPositionType(packedPosition);
        int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
        return (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
               && !mAdapter.isPosForSubscription(groupPosition);
    }

    public boolean isContactSelected() {
        long pos = mContactsList.getSelectedPosition();
        return isContactAtPosition(pos);
    }

    public boolean isContactsLoaded() {
        try {
            IContactListManager manager = mConn.getContactListManager();
            return (manager.getState() == ContactListManager.LISTS_LOADED);
        } catch (RemoteException e) {
            
            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "remote error",e);
            return false;
        }
    }

    public void removeContact() {
        removeContact(getSelectedContact());
    }

    public void removeContactAtPosition(long packedPosition) {
        removeContact(getContactAtPosition(packedPosition));
    }

    void removeContact(Cursor c) {
        if (c == null) {
            mHandler.showAlert(R.string.error, R.string.select_contact);
        } else {
            String nickname = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.NICKNAME));
            final String address = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    try {
                        IContactListManager manager = mConn.getContactListManager();
                        int res = manager.removeContact(address);
                        if (res != ImErrorInfo.NO_ERROR) {
                            mHandler.showAlert(R.string.error,
                                    ErrorResUtils.getErrorRes(getResources(), res, address));
                        }
                    } catch (RemoteException e) {
                        
            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "remote error",e);
                    }
                }
            };
            Resources r = getResources();

            new AlertDialog.Builder(mContext).setTitle(R.string.confirm)
                    .setMessage(r.getString(R.string.confirm_delete_contact, nickname))
                    .setPositiveButton(R.string.yes, confirmListener) // default button
                    .setNegativeButton(R.string.no, null).setCancelable(false).show();

            clearFocusIfEmpty(c);
        }
    }

    public void blockContact() {
        blockContact(getSelectedContact());
    }

    public void blockContactAtPosition(long packedPosition) {
        blockContact(getContactAtPosition(packedPosition));
    }

    void blockContact(Cursor c) {
        if (c == null) {
            mHandler.showAlert(R.string.error, R.string.select_contact);
        } else {
            String nickname = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.NICKNAME));
            final String address = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    try {
                        IContactListManager manager = mConn.getContactListManager();
                        int res = manager.blockContact(address);
                        
                        if (res != ImErrorInfo.NO_ERROR) {
                            mHandler.showAlert(R.string.error,
                                    ErrorResUtils.getErrorRes(getResources(), res, address));
                        }
                    } catch (RemoteException e) {
                        
            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "remote error",e);
                    }
                }
            };

            Resources r = getResources();

            new AlertDialog.Builder(mContext).setTitle(R.string.confirm)
                    .setMessage(r.getString(R.string.confirm_block_contact, nickname))
                    .setPositiveButton(R.string.yes, confirmListener) // default button
                    .setNegativeButton(R.string.no, null).setCancelable(false).show();
            clearFocusIfEmpty(c);
        }
    }

    public Cursor getContactAtPosition(long packedPosition) {
        int type = ExpandableListView.getPackedPositionType(packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
            int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
            return (Cursor) mAdapter.getChild(groupPosition, childPosition);
        }
        return null;
    }

    public Cursor getSelectedContact() {
        long pos = mContactsList.getSelectedPosition();
        if (ExpandableListView.getPackedPositionType(pos) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            return (Cursor) mContactsList.getSelectedItem();
        }
        return null;
    }

    public String getSelectedContactList() {
        long pos = mContactsList.getSelectedPosition();
        int groupPos = ExpandableListView.getPackedPositionGroup(pos);
        if (groupPos == -1) {
            return null;
        }

        Cursor cursor = (Cursor) mAdapter.getGroup(groupPos);
        if (cursor == null) {
            return null;
        }
        return cursor.getString(cursor.getColumnIndexOrThrow(Imps.ContactList.NAME));
    }

    private void registerListeners() {
        try {
            IContactListManager listManager = mConn.getContactListManager();
            listManager.registerContactListListener(mContactListListener);
            listManager.registerSubscriptionListener(mSubscriptionListener);
        } catch (RemoteException e) {
            
            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "remote error",e);
        }
    }

    private void unregisterListeners() {
        try {
            IContactListManager listManager = mConn.getContactListManager();
            listManager.unregisterContactListListener(mContactListListener);
            listManager.unregisterSubscriptionListener(mSubscriptionListener);
        } catch (RemoteException e) {
            
            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "remote error",e);
        }
    }

    private final OnChildClickListener mOnChildClickListener = new OnChildClickListener() {
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                int childPosition, long id) {
            Cursor cursor = (Cursor) parent.getExpandableListAdapter().getChild(groupPosition,
                    childPosition);
            if (cursor == null) {
                Log.w(ImApp.LOG_TAG,
                        "[ContactListView.OnChildClickListener.onChildClick] cursor null! groupPos="
                                + groupPosition + ", childPos=" + childPosition,
                        new RuntimeException());
                return false;
            }

            int subscriptionType = cursor.getInt(ContactView.COLUMN_SUBSCRIPTION_TYPE);
            int subscriptionStatus = cursor.getInt(ContactView.COLUMN_SUBSCRIPTION_STATUS);
            if ((subscriptionType == Imps.Contacts.SUBSCRIPTION_TYPE_FROM)
                && (subscriptionStatus == Imps.Contacts.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING)) {
                long providerId = cursor.getLong(ContactView.COLUMN_CONTACT_PROVIDER);
                String username = cursor.getString(ContactView.COLUMN_CONTACT_USERNAME);
                Intent intent = new Intent(ImServiceConstants.ACTION_MANAGE_SUBSCRIPTION,
                        ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, id));
                intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, providerId);
                intent.putExtra(ImServiceConstants.EXTRA_INTENT_FROM_ADDRESS, username);
                mScreen.startActivity(intent);
            } else {
                startChat(cursor);
            }
            return true;
        }
    };

    static class SavedState extends BaseSavedState {
        int[] mExpandedGroups;

        SavedState(Parcelable superState, int[] expandedGroups) {
            super(superState);
            mExpandedGroups = expandedGroups;
        }

        private SavedState(Parcel in) {
            super(in);
            mExpandedGroups = in.createIntArray();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeIntArray(mExpandedGroups);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        int[] expandedGroups = mAdapter == null ? null : mAdapter.getExpandedGroups();
        return new SavedState(superState, expandedGroups);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mSavedState = ss;
    }

    protected void setAutoRefreshContacts(boolean isRefresh) {
        mAutoRefresh = isRefresh;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mAutoRefresh) {
            super.onLayout(changed, l, t, r, b);
        }
    }
}
