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

import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.adapter.ConnectionListenerAdapter;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.util.LogCleaner;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

public class ContactListFilterView extends LinearLayout {

    private AbsListView mFilterList;
    private Filter mFilter;
    private ContactAdapter mContactAdapter;

    private Uri mUri;
    private final Context mContext;
    private final SimpleAlertHandler mHandler;
    private final ConnectionListenerAdapter mConnectionListener;

    private IImConnection mConn;
    private EditText mEtSearch;
    
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
    public boolean isInEditMode() {
        return true;
    }


    @Override
    protected void onFinishInflate() {

        mFilterList = (AbsListView) findViewById(R.id.filteredList);
        mFilterList.setTextFilterEnabled(true);

        
        mFilterList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor c = (Cursor) mFilterList.getItemAtPosition(position);
                
                if (mListener != null)
                    mListener.startChat(c);

            }
        });
        
        mFilterList.setOnItemLongClickListener(new OnItemLongClickListener()
        {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {
                
                String[] contactOptions = {mContext.getString(R.string.contact_profile_title),mContext.getString(R.string.menu_remove_contact),mContext.getString(R.string.menu_block_contact)};
                
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setItems(contactOptions, new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int which) {
                           // The 'which' argument contains the index position
                           // of the selected item
                               
                               if (which == 0)
                                   mListener.showProfile((Cursor)mFilterList.getItemAtPosition(position));
                               else if (which == 1)
                                   removeContactAtPosition(position);
                               else if (which == 2)
                                   blockContactAtPosition(position);
                       }
                });

                builder.create().show();
                
                return true;


            }
            
        });
        
        mEtSearch = (EditText)findViewById(R.id.contactSearch);
        
        mEtSearch.addTextChangedListener(new TextWatcher()
        {

            @Override
            public void afterTextChanged(Editable s) {
                
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                
                ContactListFilterView.this.doFilter(mEtSearch.getText().toString());

            }
            
        });
        
        mEtSearch.setOnKeyListener(new OnKeyListener ()
        {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
               
                ContactListFilterView.this.doFilter(mEtSearch.getText().toString());
                return false;
            }
            
        });
        /*
        mFilterList.setItemActionListener(new ListView.OnActionClickListener() {

            @Override
            public void onClick(View listView, View buttonview, int position) {

                Cursor c = (Cursor) mFilterList.getItemAtPosition(position);
                if (mListener != null)
                    if (buttonview.getId() == R.id.btnExListChat)
                        mListener.startChat(c);
                    else if (buttonview.getId() == R.id.btnExListProfile)
                        mListener.showProfile(c);
                
            }
    }, R.id.btnExListChat, R.id.btnExListProfile);
    */
        
       // 
        
        //if (!isInEditMode())
          //  mPresenceView = (UserPresenceView) findViewById(R.id.userPresence);

    }

    public AbsListView getListView() {
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

            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "remote error",e);
        }
    }

    private void unregisterListeners() {
        try {
            mConn.unregisterConnectionListener(mConnectionListener);
        } catch (RemoteException e) {

            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "remote error",e);
        }
    }

    
    public void doFilter(Uri uri, String filterString) {
        if (!uri.equals(mUri)) {
            mUri = uri;

            if (mContactAdapter != null && mContactAdapter.getCursor() != null)
                mContactAdapter.getCursor() .close();
            
            Cursor contactCursor = runQuery(filterString);
            
            if (mContactAdapter == null) {
                
                if (mFilterList instanceof ListView)
                {
                    mContactAdapter = new ContactAdapter(mContext, R.layout.contact_view, contactCursor);
                    mFilter = mContactAdapter.getFilter();

                    ((ListView)mFilterList).setAdapter(mContactAdapter);
                }
                else if (mFilterList instanceof GridView)
                {

                    mContactAdapter = new ContactAdapter(mContext, R.layout.contact_view_grid_layout, contactCursor);
                    mFilter = mContactAdapter.getFilter();
                        
                    ((GridView)mFilterList).setAdapter(mContactAdapter);
                }
                    
                
            } else {
                mContactAdapter.changeCursor(contactCursor);
            }
            
            
        } else {
            mFilter.filter(filterString);
        }
    }

    public void doFilter(String filterString) {
        
        if (mFilter != null && filterString != null)
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

        private Timer timer = null;
        private boolean isUpdating = false;
        
        @SuppressWarnings("deprecation")
        public ContactAdapter(Context context, int view, Cursor cursor) {
            super(context, view, cursor);
            
            timer = new Timer();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
           

            return super.getView(position, convertView, parent);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ContactView v = (ContactView) view;
            v.bind(cursor, mSearchString, false);
            
        }

        @Override
        protected void onContentChanged() {
            
            if (!isUpdating)
            {

                isUpdating = true;
                
                timer.schedule(new TimerTask(){
    
                    @Override
                    public void run() {
                       
                        mHandler.post(new Runnable ()
                        {
                           public void run ()
                           {    
                               Cursor cursor = ContactListFilterView.this.runQuery(mSearchString);
                           
                               changeCursor(cursor);
                           }
                        });
                        
                        isUpdating = false;
                    }
                    
                    
                }, 1000l);
            }
                
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
        public void showProfile (Cursor c);
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

    public void removeContactAtPosition(int packedPosition) {
        removeContact((Cursor)mFilterList.getItemAtPosition(packedPosition));
    }

    void removeContact(Cursor c) {
        if (c == null || mConn == null) {
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


        }
    }

    

    public void blockContactAtPosition(int packedPosition) {
        blockContact((Cursor)mFilterList.getItemAtPosition(packedPosition));
    }

    void blockContact(Cursor c) {
        if (c == null || mConn == null) {
            mHandler.showAlert(R.string.error, R.string.select_contact);
        } else {
            String nickname = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.NICKNAME));
            final String address = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    try {
                        IContactListManager manager = mConn.getContactListManager();
                        
                        int res = -1;
                        
                        if (manager.isBlocked(address))
                            res = manager.unBlockContact(address);
                        else
                        {
                            res = manager.blockContact(address);
                            
                            if (res != ImErrorInfo.NO_ERROR) {
                                mHandler.showAlert(R.string.error,
                                        ErrorResUtils.getErrorRes(getResources(), res, address));
                            }
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
            
        }
    }
}
