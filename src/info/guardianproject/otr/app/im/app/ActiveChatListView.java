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
import info.guardianproject.otr.app.im.IChatSessionListener;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.adapter.ChatSessionListenerAdapter;
import info.guardianproject.otr.app.im.app.adapter.ConnectionListenerAdapter;
import info.guardianproject.otr.app.im.engine.ContactListManager;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

public class ActiveChatListView extends LinearLayout {

    Activity mScreen;
  //  IImConnection mConn;
    SimpleAlertHandler mHandler;
    Context mContext;
    private final IChatSessionListener mChatListListener;
    
    ListView mChatList;
    private ChatListAdapter mAdapter;
    private boolean mAutoRefresh = true;
    private final ConnectionListenerAdapter mConnectionListener;

    public ActiveChatListView(Context screen, AttributeSet attrs) {
        super(screen, attrs);
        mContext = screen;
        mScreen = (Activity) screen;
        mHandler = new SimpleAlertHandler(mScreen);
        mChatListListener = new MyChatSessionListener(mHandler);
        
        mConnectionListener = new ConnectionListenerAdapter(mHandler) {
            @Override
            public void onConnectionStateChange(IImConnection connection, int state,
                    ImErrorInfo error) {
                    
            }  
        };
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }



    private class MyChatSessionListener extends ChatSessionListenerAdapter {
        public MyChatSessionListener(SimpleAlertHandler handler) {
            super();
        }

        @Override
        public void onChatSessionCreated(IChatSession session) {
            super.onChatSessionCreated(session);
            mAdapter.startAutoRequery();
        }

        @Override
        public void onChatSessionCreateError(String name, ImErrorInfo error) {
            super.onChatSessionCreateError(name, error);
        }

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        mChatList = (ListView) findViewById(R.id.chatsList);
        mChatList.setOnItemClickListener(mOnClickListener);
    }

    public ListView getListView() {
        return mChatList;
    }

    public void setConnection(IImConnection conn) {
        
            if (conn != null) {
                unregisterListeners(conn);
            }

            if (conn != null) {

                registerListeners(conn);

                if (mAdapter == null) {
                    mAdapter = new ChatListAdapter(mScreen);
                    mAdapter.changeConnection();
                    mChatList.setAdapter(mAdapter);
                    mChatList.setOnScrollListener(mAdapter);
                } 
                
           //     mAdapter.changeConnection();
                
                
//                try {
//                    IChatSessionManager listMgr = conn.getChatSessionManager();
                    mAdapter.startAutoRequery();

//                } catch (RemoteException e) {
//                    Log.e(ImApp.LOG_TAG, "Service died!");
//                }
            }
       
    }


    
    void startChat(Cursor c) {
        if (c != null) {
            long id = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts._ID));
            String username = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            
            long providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Account.PROVIDER));
            IImConnection conn = ((ImApp)mScreen.getApplication()).getConnection(providerId);
            
            
                try {
                    
                    if (conn != null)
                    {
                        IChatSessionManager manager = conn.getChatSessionManager();
                        IChatSession session = manager.getChatSession(username);
                        if (session == null) {
                            manager.createChatSession(username);
                        }
                    }
    
                    Uri data = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, id);
                    Intent intent = new Intent(Intent.ACTION_VIEW, data);
                    intent.addCategory(ImApp.IMPS_CATEGORY);
                    mScreen.startActivity(intent);
                    setAutoRefreshContacts(false);
                } catch (RemoteException e) {
                    mHandler.showServiceErrorAlert();
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


    public void endChat(Cursor c, IImConnection conn) {
        if (c != null) {
            String username = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            
            try {
                IChatSessionManager manager = conn.getChatSessionManager();
                IChatSession session = manager.getChatSession(username);
                if (session != null) {
                    session.leave();
                }
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
            
            clearFocusIfEmpty(c);
        }
    }


    public void viewContactPresence(Cursor c) {
        if (c != null) {
            long id = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts._ID));
            Uri data = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, id);
            Intent i = new Intent(Intent.ACTION_VIEW, data);
            mScreen.startActivity(i);
        }
    }


    public boolean isConversationAtPosition(long packedPosition) {
        /*
        int type = ExpandableListView.getPackedPositionType(packedPosition);
        int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
        return (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
               && mAdapter.isPosForOngoingConversation(groupPosition);*/
               return true;
    }

    public boolean isConversationSelected() {
        long pos = mChatList.getSelectedItemPosition();
        return isConversationAtPosition(pos);
    }

    /*
    public boolean isContactsLoaded() {
        
        if (mConn != null)
        {
            try {
                IContactListManager manager = mConn.getContactListManager();
                return (manager.getState() == ContactListManager.LISTS_LOADED);
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
                return false;
            }
        }
        return false;
    }*/

    /*
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
                        mHandler.showServiceErrorAlert();
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
    }*/


    /*
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
                        mHandler.showServiceErrorAlert();
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
    }*/


    private void registerListeners(IImConnection conn) {
        
       
            try {
                IChatSessionManager chatManager = conn.getChatSessionManager();
                chatManager.registerChatSessionListener(mChatListListener);
                
    
            } 
            catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
            
            try {
                conn.registerConnectionListener(mConnectionListener);
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
           }
        
    }

    private void unregisterListeners(IImConnection conn) {
       
        try {
            IChatSessionManager chatManager = conn.getChatSessionManager();
            chatManager.unregisterChatSessionListener(mChatListListener);
        } 
        catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }
        
    }

    private final OnItemClickListener mOnClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> view, View itemView, int position, long id) {
            ListView chatList = (ListView) view;

            Cursor cursor = (Cursor) chatList.getAdapter().getItem(position);

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
        return superState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state); 
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
