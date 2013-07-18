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
import info.guardianproject.otr.app.im.app.adapter.ConnectionListenerAdapter;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.provider.Imps;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

public class ContactListFilterView extends LinearLayout {

    private ListView mFilterList;
    private Filter mFilter;
    private ContactAdapter mContactAdapter;

    private Uri mUri;
    private final Context mContext;
    private final SimpleAlertHandler mHandler;
    private final ConnectionListenerAdapter mConnectionListener;

    private IImConnection mConn;

    public ContactListFilterView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mHandler = new SimpleAlertHandler((Activity)context);
        mConnectionListener = new ConnectionListenerAdapter(mHandler) {
            @Override
            public void onConnectionStateChange(IImConnection connection, int state,
                    ImErrorInfo error) {
                
            }

            @Override
            public void onUpdateSelfPresenceError(IImConnection connection, ImErrorInfo error) {
                super.onUpdateSelfPresenceError(connection, error);
            }

            @Override
            public void onSelfPresenceUpdated(IImConnection connection) {
                super.onSelfPresenceUpdated(connection);
            }  
            
            
        };
    }

    @Override
    protected void onFinishInflate() {

        mFilterList = (ListView) findViewById(R.id.filteredList);
        mFilterList.setTextFilterEnabled(true);

        mFilterList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor c = (Cursor) mFilterList.getItemAtPosition(position);
                
                if (mListener != null)
                    mListener.startChat(c);

            }
        });

        //if (!isInEditMode())
          //  mPresenceView = (UserPresenceView) findViewById(R.id.userPresence);

    }

    public ListView getListView() {
        return mFilterList;
    }

    public Cursor getContactAtPosition(int position) {
        return (Cursor) mContactAdapter.getItem(position);
    }

    public void setConnection(IImConnection conn) {
        if (mConn != conn) {
            if (mConn != null) {
                unregisterListeners();
            }
            
            mConn = conn;

            if (conn != null) {
                /*
                try {
                    //mPresenceView.loggingIn(mConn.getState() == ImConnection.LOGGING_IN);
                } catch (RemoteException e) {
                    //mPresenceView.loggingIn(false);
                    mHandler.showServiceErrorAlert();
                }
                */
                
                registerListeners();
            }
        }
    }

    private void registerListeners() {
        try {
            mConn.registerConnectionListener(mConnectionListener);
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }
    }

    private void unregisterListeners() {
        try {
            mConn.unregisterConnectionListener(mConnectionListener);
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }
    }

    
    public void doFilter(Uri uri, String filterString) {
        if (!uri.equals(mUri)) {
            mUri = uri;

            if (mContactAdapter != null && mContactAdapter.getCursor() != null)
                mContactAdapter.getCursor() .close();
            
            Cursor contactCursor = runQuery(filterString);
            
            if (mContactAdapter == null) {
                mContactAdapter = new ContactAdapter(mContext, contactCursor);
                mFilter = mContactAdapter.getFilter();
                mFilterList.setAdapter(mContactAdapter);
            } else {
                mContactAdapter.changeCursor(contactCursor);
            }
            
            
        } else {
            mFilter.filter(filterString);
        }
    }

    public void doFilter(String filterString) {
        mFilter.filter(filterString);

    }

    Cursor runQuery(CharSequence constraint) {
        StringBuilder buf = new StringBuilder();

        if (constraint != null) {

            buf.append(Imps.Contacts.NICKNAME);
            buf.append(" LIKE ");
            DatabaseUtils.appendValueToSql(buf, "%" + constraint + "%");
        }

        ContentResolver cr = mContext.getContentResolver();
        
        Cursor cursor = cr.query(mUri, ContactView.CONTACT_PROJECTION,
                buf == null ? null : buf.toString(), null, Imps.Contacts.DEFAULT_SORT_ORDER);
        
        
        
        return cursor;
    }

    private class ContactAdapter extends ResourceCursorAdapter {
        private String mSearchString;

        @SuppressWarnings("deprecation")
        public ContactAdapter(Context context, Cursor cursor) {
            super(context, R.layout.contact_view, cursor);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ContactView v = (ContactView) view;
          //  v.setPadding(0, 0, 0, 0);
            v.bind(cursor, mSearchString, false);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (constraint != null) {
                mSearchString = constraint.toString();
            }
            return ContactListFilterView.this.runQuery(constraint);
        }
    }

    public interface ContactListListener {
     
        public void startChat (Cursor c);
    }
    
    
    private ContactListListener mListener = null;
    
    public void setListener (ContactListListener listener)
    {
        mListener = listener;
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
       
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        
       
        if (mContactAdapter != null && mContactAdapter.getCursor() != null)
        {
            mContactAdapter.getCursor().close();
            mContactAdapter = null;
        }
        
    }
}
