/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
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
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CursorTreeAdapter;
import android.widget.TextView;
import android.widget.ListAdapter;

public class ChatListAdapter implements ListAdapter, AbsListView.OnScrollListener {

    private static final String[] CONTACT_LIST_PROJECTION = { Imps.ContactList._ID,
                                                             Imps.ContactList.NAME, };

    private static final int COLUMN_CONTACT_LIST_ID = 0;
    private static final int COLUMN_CONTACT_LIST_NAME = 1;

    Activity mActivity;
    SimpleAlertHandler mHandler;
    private LayoutInflater mInflate;
    private long mProviderId = -1;
    long mAccountId = -1;
    Cursor mOngoingConversations;
    boolean mDataValid;
    ListTreeAdapter mAdapter;

    final MyContentObserver mContentObserver;
    final MyDataSetObserver mDataSetObserver;

    //   private static final int TOKEN_CONTACT_LISTS = -1;
    private static final int TOKEN_ONGOING_CONVERSATION = -2;
    //   private static final int TOKEN_SUBSCRIPTION = -3;

    /*
    private static final String NON_CHAT_AND_BLOCKED_CONTACTS = "("
                                                                + Imps.Contacts.LAST_MESSAGE_DATE
                                                                + " IS NULL) AND ("
                                                                + Imps.Contacts.TYPE + "!="
                                                                + Imps.Contacts.TYPE_BLOCKED + ")";

    
    private static final String CONTACTS_SELECTION = Imps.Contacts.CONTACTLIST + "=? AND "
                                                     + NON_CHAT_AND_BLOCKED_CONTACTS;

    
    private static final String ONLINE_CONTACT_SELECTION = CONTACTS_SELECTION + " AND "
                                                           + Imps.Contacts.PRESENCE_STATUS + " != "
                                                           + Imps.Presence.OFFLINE;
*/
    
    static final void log(String msg) {
        Log.d(ImApp.LOG_TAG, "<ContactListAdapter>" + msg);
    }

    static final String[] CONTACT_COUNT_PROJECTION = { Imps.Contacts.CONTACTLIST,
                                                      Imps.Contacts._COUNT, };

    ContentQueryMap mOnlineContactsCountMap;

    // Async QueryHandler
    private final class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("onQueryComplete:token=" + token);
            }

            if (token == TOKEN_ONGOING_CONVERSATION) {
                setOngoingConversations(c);
                mAdapter.notifyDataSetChanged();

            } else {
                int count = mAdapter.getGroupCount();
                for (int pos = 0; pos < count; pos++) {
                    long listId = mAdapter.getGroupId(pos);
                    if (listId == token) {
                        mAdapter.setChildrenCursor(pos, c);
                        break;
                    }
                }
            }
        }
    }

    private QueryHandler mQueryHandler;

    private int mScrollState;

    private boolean mAutoRequery;
    private boolean mRequeryPending;

    public ChatListAdapter(Activity activity) {
        mActivity = activity;
        mInflate = activity.getLayoutInflater();
        mHandler = new SimpleAlertHandler(activity);

        mAdapter = new ListTreeAdapter(null);

        mContentObserver = new MyContentObserver();
        mDataSetObserver = new MyDataSetObserver();
        mQueryHandler = new QueryHandler(activity);

    }

    public void changeConnection(IImConnection conn) {
        mQueryHandler.cancelOperation(TOKEN_ONGOING_CONVERSATION);

        synchronized (this) {
            if (mOngoingConversations != null) {
                mOngoingConversations.close();
                mOngoingConversations = null;
            }
            if (mOnlineContactsCountMap != null) {
                mOnlineContactsCountMap.close();
            }
        }

        mAdapter.notifyDataSetChanged();
        if (conn != null) {
            try {
                mProviderId = conn.getProviderId();
                mAccountId = conn.getAccountId();
                startQueryOngoingConversations();
            } catch (RemoteException e) {
                // Service died!
            }
        }
    }
    
    public void changeConnection() {
        mQueryHandler.cancelOperation(TOKEN_ONGOING_CONVERSATION);

        synchronized (this) {
            if (mOngoingConversations != null) {
                mOngoingConversations.close();
                mOngoingConversations = null;
            }
            if (mOnlineContactsCountMap != null) {
                mOnlineContactsCountMap.close();
            }
        }

        mAdapter.notifyDataSetChanged();
        
        mProviderId = -1;
        mAccountId = -1;
        startQueryOngoingConversations();
   
       
    }

    public void startAutoRequery() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("startAutoRequery()");
        }
        mAutoRequery = true;
        if (mRequeryPending) {
            mRequeryPending = false;
            startQueryOngoingConversations();
        }
    }

    void startQueryOngoingConversations() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("startQueryOngoingConversations()");
        }

        Uri uri = Imps.Contacts.CONTENT_URI_CHAT_CONTACTS_BY;
        
        if (mProviderId != -1)
            uri = ContentUris.withAppendedId(uri, mProviderId);
        
        if (mAccountId != -1)
            uri = ContentUris.withAppendedId(uri, mAccountId);
        
        
        mQueryHandler.startQuery(TOKEN_ONGOING_CONVERSATION, null, uri,
                ContactView.CONTACT_PROJECTION_FULL, null, null, Imps.Contacts.DEFAULT_SORT_ORDER);
    }
    
 

    /*
    void startQuerySubscriptions() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("startQuerySubscriptions()");
        }

        Uri uri = Imps.Contacts.CONTENT_URI_CONTACTS_BY;
        uri = ContentUris.withAppendedId(uri, mProviderId);
        uri = ContentUris.withAppendedId(uri, mAccountId);
    }
    */

    public long getChildId(int groupPosition, int childPosition) {
        if (isPosForOngoingConversation(groupPosition)) {
            // No cursor id exists for the "Empty" TextView item
            if (getOngoingConversationCount() == 0)
                return 0;
            return getId(getOngoingConversations(), childPosition);
        }

        return -1;
    }


    public int getChildrenCount(int groupPosition) {
        return 0;
    }

    public Object getGroup(int groupPosition) {
        if (isPosForOngoingConversation(groupPosition)) {
            return null;
        } else {
            return mAdapter.getGroup(getChildAdapterPosition(groupPosition));
        }
    }


    public boolean isChildSelectable(int groupPosition, int childPosition) {
        if (isPosForOngoingConversation(groupPosition)) {
            // "Empty" TextView is not selectable
            if (getOngoingConversationCount() == 0)
                return false;
            return true;
        }

        return mAdapter.isChildSelectable(getChildAdapterPosition(groupPosition), childPosition);
    }

    public boolean stableIds() {
        return true;
    }

    View newChildView(ViewGroup parent) {
        return mInflate.inflate(R.layout.contact_view, parent, false);
    }

    View newEmptyView(ViewGroup parent) {
        return mInflate.inflate(R.layout.empty_conversation_group_view, parent, false);
    }

    View newGroupView(ViewGroup parent) {
        return mInflate.inflate(R.layout.group_view, parent, false);
    }

    private synchronized Cursor getOngoingConversations() {
        if (mOngoingConversations == null) {
            startQueryOngoingConversations();
        }
        return mOngoingConversations;
    }

    synchronized void setOngoingConversations(Cursor c) {
        if (mOngoingConversations != null) {
            mOngoingConversations.unregisterContentObserver(mContentObserver);
            mOngoingConversations.unregisterDataSetObserver(mDataSetObserver);
            mOngoingConversations.close();
        }

        if (c != null) {
            c.registerContentObserver(mContentObserver);
            c.registerDataSetObserver(mDataSetObserver);
        }

        mOngoingConversations = c;

    }

    private int getOngoingConversationCount() {
        Cursor c = getOngoingConversations();
        return c == null ? 0 : c.getCount();
    }

    public boolean isPosForOngoingConversation(int groupPosition) {
        return groupPosition == 0;
    }

    private int getChildAdapterPosition(int groupPosition) {
        return groupPosition - 1;

    }

    private Cursor moveTo(Cursor cursor, int position) {
        if (cursor.moveToPosition(position)) {
            return cursor;
        }
        return null;
    }

    private long getId(Cursor cursor, int position) {
        if (cursor.moveToPosition(position)) {
            return cursor.getLong(ContactView.COLUMN_CONTACT_ID);
        }
        return 0;
    }

    class ListTreeAdapter extends CursorTreeAdapter {

        public ListTreeAdapter(Cursor cursor) {
            super(cursor, mActivity);
        }

        @Override
        protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
            // binding when child is text view for an empty group
            if (view instanceof TextView) {
                ((TextView) view).setText(mActivity.getText(R.string.empty_contact_group));
            } else {
                ((ContactView) view).bind(cursor, null, isScrolling());
            }
        }

        @Override
        protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
            TextView text1 = (TextView) view.findViewById(R.id.text1);
            TextView text2 = (TextView) view.findViewById(R.id.text2);
            Resources r = view.getResources();

            text1.setText(cursor.getString(COLUMN_CONTACT_LIST_NAME));
            text2.setVisibility(View.VISIBLE);
            text2.setText(r.getString(R.string.online_count, getOnlineChildCount(cursor)));
        }

        View newEmptyView(ViewGroup parent) {
            return mInflate.inflate(R.layout.empty_contact_group_view, parent, false);
        }

        // if the group is empty, provide a text view. The infrastructure provides a "convertView"
        // as a possible suggestion to reuse an existing view's data. It may be null, it may be a
        // TextView, or it may be a ContactView, so we need to test the possible cases.
        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            // Provide a TextView if the group is empty
            if (super.getChildrenCount(groupPosition) == 0) {
                if (convertView != null) {
                    if (convertView instanceof TextView) {
                        ((TextView) convertView).setText(mActivity
                                .getText(R.string.empty_contact_group));
                        return convertView;
                    }
                }
                return newEmptyView(parent);
            }
            if (!(convertView instanceof ContactView)) {
                convertView = null;
            }
            return super.getChildView(groupPosition, childPosition, isLastChild, convertView,
                    parent);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            return null;
        }

        // return a TextView for empty groups
        @Override
        protected View newChildView(Context context, Cursor cursor, boolean isLastChild,
                ViewGroup parent) {
            if (cursor.getCount() == 0) {
                return newEmptyView(parent);
            } else {
                return ChatListAdapter.this.newChildView(parent);
            }
        }

        @Override
        protected View newGroupView(Context context, Cursor cursor, boolean isExpanded,
                ViewGroup parent) {
            return ChatListAdapter.this.newGroupView(parent);
        }

        private int getOnlineChildCount(Cursor groupCursor) {
            long listId = groupCursor.getLong(COLUMN_CONTACT_LIST_ID);
            if (mOnlineContactsCountMap == null) {
                String where = Imps.Contacts.ACCOUNT + "=" + mAccountId;
                ContentResolver cr = mActivity.getContentResolver();

                Cursor c = cr.query(Imps.Contacts.CONTENT_URI_ONLINE_COUNT,
                        CONTACT_COUNT_PROJECTION, where, null, null);
                mOnlineContactsCountMap = new ContentQueryMap(c, Imps.Contacts.CONTACTLIST, true,
                        mHandler);
                mOnlineContactsCountMap.addObserver(new Observer() {
                    public void update(Observable observable, Object data) {
                        notifyDataSetChanged();
                    }
                });
            }
            ContentValues value = mOnlineContactsCountMap.getValues(String.valueOf(listId));
            return value == null ? 0 : value.getAsInteger(Imps.Contacts._COUNT);
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            int children = super.getChildrenCount(groupPosition);
            if (children == 0) {
                // Count the empty group text item as a child
                return 1;
            }
            return children;
        }

        // Don't allow the empty group text item to be selected
        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return (super.getChildrenCount(groupPosition) > 0);
        }
    }

    private class MyContentObserver extends ContentObserver {

        public MyContentObserver() {
            super(mHandler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("MyContentObserver.onChange() autoRequery=" + mAutoRequery);
            }
            // Don't requery when fling. We will schedule a requery when the fling is complete.
            if (isScrolling()) {
                return;
            }
            if (mAutoRequery) {
                startQueryOngoingConversations();
            } else {
                mRequeryPending = true;
            }
        }
    }

    private class MyDataSetObserver extends DataSetObserver {
        public MyDataSetObserver() {
        }

        @Override
        public void onChanged() {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("MyDataSetObserver.onChanged()");
            }
            mDataValid = true;
        }

        @Override
        public void onInvalidated() {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("MyDataSetObserver.onInvalidated()");
            }
            mDataValid = false;
        }
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        // no op
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        int oldState = mScrollState;

        mScrollState = scrollState;
        //  If we just finished a fling then some items may not have an icon
        //  So force a full redraw now that the fling is complete
        if (oldState == OnScrollListener.SCROLL_STATE_FLING) {
        }
    }

    public boolean isScrolling() {
        return mScrollState == OnScrollListener.SCROLL_STATE_FLING;
    }

    @Override
    public int getCount() {
        return this.getOngoingConversationCount();
    }

    @Override
    public Object getItem(int position) {

        if (getOngoingConversationCount() == 0)
            return null;
        return moveTo(getOngoingConversations(), position);
    }

    @Override
    public long getItemId(int position) {
        // No cursor id exists for the "Empty" TextView item
        if (getOngoingConversationCount() == 0)
            return 0;
        return getId(getOngoingConversations(), position);

    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // boolean isOngoingConversation = isPosForOngoingConversation(0);
        boolean displayEmpty = (getOngoingConversationCount() == 0);
        View view = null;
        if (convertView != null) {
            // use the convert view if it matches the type required by displayEmpty
            if (displayEmpty && (convertView instanceof TextView)) {
                view = convertView;
                ((TextView) view).setText(mActivity.getText(R.string.empty_conversation_group));
            } else if (!displayEmpty && (convertView instanceof ContactView)) {
                view = convertView;
            }
        }
        if (view == null) {
            if (displayEmpty) {
                view = newEmptyView(parent);
            } else {
                view = newChildView(parent);
            }
        }
        if (!displayEmpty) {
            Cursor cursor = getOngoingConversations();
            cursor.moveToPosition(position);

            String[] myColumnString = cursor.getColumnNames();
            for (int i = 0; i < myColumnString.length; i++) {
            }

            ((ContactView) view).bind(cursor, null, isScrolling());
        }
        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isEmpty() {

        return (this.getOngoingConversationCount() == 0);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mAdapter.registerDataSetObserver(observer);

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }
}
