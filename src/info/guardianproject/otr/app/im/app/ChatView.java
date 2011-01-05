/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.app.adapter.ChatListenerAdapter;
import info.guardianproject.otr.app.im.app.adapter.ChatSessionListenerAdapter;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.ArrayList;
import java.util.Date;

import info.guardianproject.otr.app.im.IChatListener;
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionListener;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IContactList;
import info.guardianproject.otr.app.im.IContactListListener;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Browser;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class ChatView extends LinearLayout {
	
    // This projection and index are set for the query of active chats
    static final String[] CHAT_PROJECTION = {
        Imps.Contacts._ID,
        Imps.Contacts.ACCOUNT,
        Imps.Contacts.PROVIDER,
        Imps.Contacts.USERNAME,
        Imps.Contacts.NICKNAME,
        Imps.Contacts.TYPE,
        Imps.Presence.PRESENCE_STATUS,
        Imps.Chats.LAST_UNREAD_MESSAGE,
    };
    static final int CONTACT_ID_COLUMN             = 0;
    static final int ACCOUNT_COLUMN                = 1;
    static final int PROVIDER_COLUMN               = 2;
    static final int USERNAME_COLUMN               = 3;
    static final int NICKNAME_COLUMN               = 4;
    static final int TYPE_COLUMN                   = 5;
    static final int PRESENCE_STATUS_COLUMN        = 6;
    static final int LAST_UNREAD_MESSAGE_COLUMN    = 7;

    static final String[] INVITATION_PROJECT = {
        Imps.Invitation._ID,
        Imps.Invitation.PROVIDER,
        Imps.Invitation.SENDER,
    };
    static final int INVITATION_ID_COLUMN = 0;
    static final int INVITATION_PROVIDER_COLUMN = 1;
    static final int INVITATION_SENDER_COLUMN = 2;

    static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    Markup mMarkup;

    Activity mScreen;
    ImApp mApp;
    SimpleAlertHandler mHandler;
    Cursor mCursor;

    private ImageView   mStatusIcon;
    private TextView    mTitle;
    /*package*/ListView    mHistory;
    EditText    mEdtInput;
    private Button      mSendButton;
    private View mStatusWarningView;
    private ImageView mWarningIcon;
    private TextView mWarningText;

    private MessageAdapter mMessageAdapter;
    private IChatSessionManager mChatSessionMgr;
    private IChatSessionListener mChatSessionListener;

    private IChatSession mChatSession;
    private long mChatId;
    int mType;
    String mNickName;
    String mUserName;
    long mProviderId;
    long mAccountId;
    long mInvitationId;
    private Context mContext; // TODO
    private int mPresenceStatus;

    private int mViewType;

    private static final int VIEW_TYPE_CHAT = 1;
    private static final int VIEW_TYPE_INVITATION = 2;
    private static final int VIEW_TYPE_SUBSCRIPTION = 3;

    private static final long SHOW_TIME_STAMP_INTERVAL = 60 * 1000;     // 1 minute
    private static final int QUERY_TOKEN = 10;

    // Async QueryHandler
    private final class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            Cursor cursor = new DeltaCursor(c);

            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
                log("onQueryComplete: cursor.count=" + cursor.getCount());
            }

            mMessageAdapter.changeCursor(cursor);
        }
    }
    private QueryHandler mQueryHandler;


	public SimpleAlertHandler getHandler ()
	{
		return mHandler;
	}
	
	public int getType ()
	{
		return mViewType;
	}
	
    private class RequeryCallback implements Runnable {
        public void run() {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
                log("RequeryCallback");
            }
            requeryCursor();
        }
    }
    private RequeryCallback mRequeryCallback = null;

    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!(view instanceof MessageView)) {
                return;
            }
            URLSpan[] links = ((MessageView)view).getMessageLinks();
            if (links.length == 0){
                return;
            }

            final ArrayList<String> linkUrls = new ArrayList<String>(links.length);
            for (URLSpan u : links) {
                linkUrls.add(u.getURL());
            }
            ArrayAdapter<String> a = new ArrayAdapter<String>(mScreen,
                    android.R.layout.select_dialog_item, linkUrls);
            AlertDialog.Builder b = new AlertDialog.Builder(mScreen);
            b.setTitle(R.string.select_link_title);
            b.setCancelable(true);
            b.setAdapter(a, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Uri uri = Uri.parse(linkUrls.get(which));
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, mScreen.getPackageName());
                    mScreen.startActivity(intent);
                }
            });
            b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            b.show();
        }
    };

    private IChatListener mChatListener = new ChatListenerAdapter() {
        @Override
        public void onIncomingMessage(IChatSession ses,
                info.guardianproject.otr.app.im.engine.Message msg) {
            scheduleRequery(0);
            

        }

        @Override
        public void onContactJoined(IChatSession ses, Contact contact) {
            scheduleRequery(0);
        }

        @Override
        public void onContactLeft(IChatSession ses, Contact contact) {
            scheduleRequery(0);
        }

        @Override
        public void onSendMessageError(IChatSession ses,
                info.guardianproject.otr.app.im.engine.Message msg, ImErrorInfo error) {
            scheduleRequery(0);
        }
    };

    private Runnable mUpdateChatCallback = new Runnable() {
        public void run() {
            if (mCursor.requery() && mCursor.moveToFirst()) {
                updateChat();
            }
        }
    };
    private IContactListListener mContactListListener = new IContactListListener.Stub () {
        public void onAllContactListsLoaded() {
        }

        public void onContactChange(int type, IContactList list, Contact contact){
        }

        public void onContactError(int errorType, ImErrorInfo error,
                String listName, Contact contact) {
        }

        public void onContactsPresenceUpdate(Contact[] contacts) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("onContactsPresenceUpdate()");
            }
            for (Contact c : contacts) {
                if (c.getAddress().getFullName().equals(mUserName)) {
                    mHandler.post(mUpdateChatCallback);
                    scheduleRequery(0);
                    break;
                }
            }
        }
    };

    static final void log(String msg) {
        Log.d(ImApp.LOG_TAG, "<ChatView> " +msg);
    }

    public ChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScreen = (Activity) context;
        mApp = ImApp.getApplication(mScreen);
        mHandler = new ChatViewHandler();
        mContext = context;
    }

    void registerForConnEvents() {
        mApp.registerForConnEvents(mHandler);
    }

    void unregisterForConnEvents() {
        mApp.unregisterForConnEvents(mHandler);
    }

    @Override
    protected void onFinishInflate() {
        mStatusIcon     = (ImageView) findViewById(R.id.statusIcon);
        mTitle          = (TextView) findViewById(R.id.title);
        mHistory        = (ListView) findViewById(R.id.history);
        mEdtInput       = (EditText) findViewById(R.id.edtInput);
        mSendButton     = (Button)findViewById(R.id.btnSend);
        mHistory.setOnItemClickListener(mOnItemClickListener);

        mStatusWarningView = findViewById(R.id.warning);
        mWarningIcon = (ImageView)findViewById(R.id.warningIcon);
        mWarningText = (TextView)findViewById(R.id.warningText);

        Button acceptInvitation = (Button)findViewById(R.id.btnAccept);
        Button declineInvitation= (Button)findViewById(R.id.btnDecline);

        Button approveSubscription = (Button)findViewById(R.id.btnApproveSubscription);
        Button declineSubscription = (Button)findViewById(R.id.btnDeclineSubscription);

        acceptInvitation.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                acceptInvitation();
            }
        });
        declineInvitation.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                declineInvitation();
            }
        });

        approveSubscription.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                approveSubscription();
            }
        });
        declineSubscription.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                declineSubscription();
            }
        });

        mEdtInput.setOnKeyListener(new OnKeyListener(){
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                            sendMessage();
                            return true;

                        case KeyEvent.KEYCODE_ENTER:
                            if (event.isAltPressed()) {
                                mEdtInput.append("\n");
                                return true;
                            }
                    }
                }
                return false;
            }
        });

        mEdtInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null) {
                    if (event.isAltPressed()) {
                        return false;
                    }
                }

                sendMessage();
                return true;
            }
        });

        // TODO: this is a hack to implement BUG #1611278, when dispatchKeyEvent() works with
        // the soft keyboard, we should remove this hack.
        mEdtInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int before, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int after) {
                //log("TextWatcher: " + s);
                userActionDetected();
            }

            public void afterTextChanged(Editable s) {
            }
        });

        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    public void onResume(){
        if (mViewType == VIEW_TYPE_CHAT) {
            Cursor cursor = getMessageCursor();
            if (cursor == null) {
                startQuery();
            } else {
                requeryCursor();
            }
            updateWarningView();
        }
        registerChatListener();
        registerForConnEvents();
    }

    public void onPause(){
        Cursor cursor = getMessageCursor();
        if (cursor != null) {
            cursor.deactivate();
        }
        cancelRequery();
        if (mViewType == VIEW_TYPE_CHAT && mChatSession != null) {
            try {
                mChatSession.markAsRead();
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
        }
        unregisterChatListener();
        unregisterForConnEvents();
        unregisterChatSessionListener();
    }

    private void closeSoftKeyboard() {
        InputMethodManager inputMethodManager =
            (InputMethodManager)mApp.getSystemService(Context.INPUT_METHOD_SERVICE);

        inputMethodManager.hideSoftInputFromWindow(mEdtInput.getWindowToken(), 0);
    }

    void updateChat() {
        setViewType(VIEW_TYPE_CHAT);

        long oldChatId = mChatId;

        updateContactInfo();

        setStatusIcon();
        setTitle();

        IImConnection conn = mApp.getConnection(mProviderId);
        if (conn == null) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) log("Connection has been signed out");
            mScreen.finish();
            return;
        }

        BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
        mHistory.setBackgroundDrawable(
                brandingRes.getDrawable(BrandingResourceIDs.DRAWABLE_CHAT_WATERMARK));

        if (mMarkup == null) {
            mMarkup = new Markup(brandingRes);
        }

        if (mMessageAdapter == null) {
            mMessageAdapter = new MessageAdapter(mScreen, null);
            mHistory.setAdapter(mMessageAdapter);
        }

        // only change the message adapter when we switch to another chat
        if (mChatId != oldChatId) {
            startQuery();
            mEdtInput.setText("");
        }

        updateWarningView();
    }

    private void updateContactInfo() {
        mChatId = mCursor.getLong(CONTACT_ID_COLUMN);
        mProviderId = mCursor.getLong(PROVIDER_COLUMN);
        mAccountId = mCursor.getLong(ACCOUNT_COLUMN);
        mPresenceStatus = mCursor.getInt(PRESENCE_STATUS_COLUMN);
        mType = mCursor.getInt(TYPE_COLUMN);
        mUserName = mCursor.getString(USERNAME_COLUMN);
        mNickName = mCursor.getString(NICKNAME_COLUMN);
    }

    private void setTitle() {
        if (mType == Imps.Contacts.TYPE_GROUP) {
            final String[] projection = {Imps.GroupMembers.NICKNAME};
            Uri memberUri = ContentUris.withAppendedId(Imps.GroupMembers.CONTENT_URI, mChatId);
            ContentResolver cr = mScreen.getContentResolver();
            Cursor c = cr.query(memberUri, projection, null, null, null);
            StringBuilder buf = new StringBuilder();
            if(c != null) {
                while(c.moveToNext()) {
                    buf.append(c.getString(0));
                    if(!c.isLast()) {
                        buf.append(',');
                    }
                }
                c.close();
            }
            mTitle.setText(mContext.getString(R.string.chat_with, buf.toString()));
        } else {
            mTitle.setText(mContext.getString(R.string.chat_with, mNickName));
        }
    }

    private void setStatusIcon() {
        if (mType == Imps.Contacts.TYPE_GROUP) {
            // hide the status icon for group chat.
            mStatusIcon.setVisibility(GONE);
        } else {
            mStatusIcon.setVisibility(VISIBLE);
            BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
            int presenceResId = PresenceUtils.getStatusIconId(mPresenceStatus);
            mStatusIcon.setImageDrawable(brandingRes.getDrawable(presenceResId));
        }
    }

    public void bindChat(long chatId) {
        if (mCursor != null) {
            mCursor.deactivate();
        }
        Uri contactUri = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, chatId);
        mCursor = mScreen.managedQuery(contactUri, CHAT_PROJECTION, null, null, null);
        if (mCursor == null || !mCursor.moveToFirst()) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
                log("Failed to query chat: " + chatId);
            }
            mScreen.finish();
            return;
        } else {
            mChatSession = getChatSession(mCursor);
            updateChat();
            registerChatListener();
        }
    }

    public void bindInvitation(long invitationId) {
        Uri uri = ContentUris.withAppendedId(Imps.Invitation.CONTENT_URI, invitationId);
        ContentResolver cr = mScreen.getContentResolver();
        Cursor cursor = cr.query(uri, INVITATION_PROJECT, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
                log("Failed to query invitation: " + invitationId);
            }
            mScreen.finish();
        } else {
            setViewType(VIEW_TYPE_INVITATION);

            mInvitationId = cursor.getLong(INVITATION_ID_COLUMN);
            mProviderId = cursor.getLong(INVITATION_PROVIDER_COLUMN);
            String sender = cursor.getString(INVITATION_SENDER_COLUMN);

            TextView mInvitationText = (TextView)findViewById(R.id.txtInvitation);
            mInvitationText.setText(mContext.getString(R.string.invitation_prompt, sender));
            mTitle.setText(mContext.getString(R.string.chat_with, sender));
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    public void bindSubscription(long providerId, String from) {
        mProviderId = providerId;
        mUserName = from;

        setViewType(VIEW_TYPE_SUBSCRIPTION);

        TextView text =  (TextView)findViewById(R.id.txtSubscription);
        String displayableAddr = ImpsAddressUtils.getDisplayableAddress(from);
        text.setText(mContext.getString(R.string.subscription_prompt, displayableAddr));
        mTitle.setText(mContext.getString(R.string.chat_with, displayableAddr));

        mApp.dismissChatNotification(providerId, from);
    }

    void acceptInvitation() {
        try {

            IImConnection conn = mApp.getConnection(mProviderId);
            if (conn != null) {
                // register a chat session listener and wait for a group chat
                // session to be created after we accept the invitation.
                registerChatSessionListener();
                conn.acceptInvitation(mInvitationId);
            }
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }
    }

    void declineInvitation() {
        try {
            IImConnection conn = mApp.getConnection(mProviderId);
            if (conn != null) {
                conn.rejectInvitation(mInvitationId);
            }
            mScreen.finish();
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }
    }

    void approveSubscription() {
        IImConnection conn = mApp.getConnection(mProviderId);
        try {
            IContactListManager manager = conn.getContactListManager();
            manager.approveSubscription(mUserName);
        } catch (RemoteException ex) {
            mHandler.showServiceErrorAlert();
        }
        mScreen.finish();
    }

    void declineSubscription() {
        IImConnection conn = mApp.getConnection(mProviderId);
        try {
            IContactListManager manager = conn.getContactListManager();
            manager.declineSubscription(mUserName);
        } catch (RemoteException ex) {
            mHandler.showServiceErrorAlert();
        }
        mScreen.finish();
    }

    private void setViewType(int type) {
        mViewType = type;
        if (type == VIEW_TYPE_CHAT) {
            findViewById(R.id.invitationPanel).setVisibility(GONE);
            findViewById(R.id.subscription).setVisibility(GONE);
            setChatViewEnabled(true);
        }  else if (type == VIEW_TYPE_INVITATION) {
            setChatViewEnabled(false);
            findViewById(R.id.invitationPanel).setVisibility(VISIBLE);
            findViewById(R.id.btnAccept).requestFocus();
        } else if (type == VIEW_TYPE_SUBSCRIPTION) {
            setChatViewEnabled(false);
            findViewById(R.id.subscription).setVisibility(VISIBLE);
            findViewById(R.id.btnApproveSubscription).requestFocus();
        }
    }

    private void setChatViewEnabled(boolean enabled) {
        mEdtInput.setEnabled(enabled);
        mSendButton.setEnabled(enabled);
        if (enabled) {
            mEdtInput.requestFocus();
        } else {
            mHistory.setAdapter(null);
        }
        
        updateSecureWarning ();
    }

    private void startQuery() {
        if (mQueryHandler == null) {
            mQueryHandler = new QueryHandler(mContext);
        } else {
            // Cancel any pending queries
            mQueryHandler.cancelOperation(QUERY_TOKEN);
        }

        Uri uri = Imps.Messages.getContentUriByThreadId(mChatId);

        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
            log("queryCursor: uri=" + uri);
        }

        mQueryHandler.startQuery(QUERY_TOKEN, null,
                uri,
                null,
                null /* selection */,
                null /* selection args */,
                null);
    }

    void scheduleRequery(long interval) {
        if (mRequeryCallback == null) {
            mRequeryCallback = new RequeryCallback();
        } else {
            mHandler.removeCallbacks(mRequeryCallback);
        }

        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
            log("scheduleRequery");
        }
        mHandler.postDelayed(mRequeryCallback, interval);
        
       
    }

    void cancelRequery() {
        if (mRequeryCallback != null) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
                log("cancelRequery");
            }
            mHandler.removeCallbacks(mRequeryCallback);
            mRequeryCallback = null;
        }
    }

    void requeryCursor() {
        if (mMessageAdapter.isScrolling()) {
            mMessageAdapter.setNeedRequeryCursor(true);
            return;
        }
        

        updateSecureWarning ();
        
        // TODO: async query?
        Cursor cursor = getMessageCursor();
        if (cursor != null) {
            cursor.requery();
        }
        
    }

    private Cursor getMessageCursor() {
        return mMessageAdapter == null ? null : mMessageAdapter.getCursor();
    }

    public void insertSmiley(String smiley) {
        mEdtInput.append(mMarkup.applyEmoticons(smiley));
    }

    public void closeChatSession() {
        if (mChatSession != null) {
            try {
                mChatSession.leave();
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
        } else {
            // the conversation is already closed, clear data in database
            ContentResolver cr = mContext.getContentResolver();
            cr.delete(ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, mChatId),
                    null, null);
        }
        mScreen.finish();
    }

    public void closeChatSessionIfInactive() {
        if (mChatSession != null) {
            try {
                mChatSession.leaveIfInactive();
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
        }
    }

    public void viewProfile()  {
    	
    	/*
        Uri data = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, mChatId);
        Intent intent = new Intent(Intent.ACTION_VIEW, data);
        mScreen.startActivity(intent);
        */
    	
    	//we should launch a new Intent here to handle this and display the progress
    	
    	try
    	{
    		
    		if (mChatSessionMgr.isEncryptedSession(mUserName))
    		{
    			mChatSessionMgr.unencryptChat(mUserName);
				
				 mStatusWarningView.setVisibility(View.VISIBLE);
			     mWarningIcon.setVisibility(View.VISIBLE);
			     mWarningText.setTextColor(Color.RED);
			     mWarningText.setTextSize(10);
			      mWarningText.setText("stopping encrypted chat...");
    		}
    		else
    		{
	    		
				mChatSessionMgr.encryptChat(mUserName);
				
				 mStatusWarningView.setVisibility(View.VISIBLE);
			     mWarningIcon.setVisibility(View.VISIBLE);
			     mWarningText.setTextSize(10);

			      mWarningText.setText("initiating encrypted chat...");
    		}
    		
    	}
    	catch (RemoteException re)
    	{
    		Log.e("ChatView","error: " + re);
    		re.printStackTrace();
    	}
    	
    }
    

    public void blockContact() {
        // TODO: unify with codes in ContactListView
        DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    IImConnection conn = mApp.getConnection(mProviderId);
                    IContactListManager manager = conn.getContactListManager();
                    manager.blockContact(mUserName);
                    mScreen.finish();
                } catch (RemoteException e) {
                    mHandler.showServiceErrorAlert();
                }
            }
        };

        Resources r = getResources();

        // The positive button is deliberately set as no so that
        // the no is the default value
        new AlertDialog.Builder(mContext)
            .setTitle(R.string.confirm)
            .setMessage(r.getString(R.string.confirm_block_contact, mNickName))
            .setPositiveButton(R.string.yes, confirmListener) // default button
            .setNegativeButton(R.string.no, null)
            .setCancelable(false)
            .show();
    }

    public long getProviderId() {
        return mProviderId;
    }

    public long getAccountId() {
        return mAccountId;
    }

    public String getUserName() {
        return mUserName;
    }

    public long getChatId () {
        try {
            return mChatSession == null ? -1 : mChatSession.getId();
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
            return -1;
        }
    }

    public IChatSession getCurrentChatSession() {
        return mChatSession;
    }

    private IChatSessionManager getChatSessionManager(long providerId) {
        if (mChatSessionMgr == null) {
            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                try {
                    mChatSessionMgr = conn.getChatSessionManager();
                } catch (RemoteException e) {
                    mHandler.showServiceErrorAlert();
                }
            }
        }
        return mChatSessionMgr;
    }

    private IChatSession getChatSession(Cursor cursor) {
        long providerId = cursor.getLong(PROVIDER_COLUMN);
        String username = cursor.getString(USERNAME_COLUMN);

        IChatSessionManager sessionMgr = getChatSessionManager(providerId);
        if (sessionMgr != null) {
            try {
                return sessionMgr.getChatSession(username);
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
        }
        return null;
    }

    boolean isGroupChat() {
        return Imps.Contacts.TYPE_GROUP == mType;
    }

    void sendMessage() {
        String msg = mEdtInput.getText().toString();

        if (TextUtils.isEmpty(msg.trim())) {
            return;
        }
        

        if (mChatSession != null) {
            try {
                mChatSession.sendMessage(msg);
                mEdtInput.setText("");
                mEdtInput.requestFocus();
                requeryCursor();
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
        }

        // Close the soft on-screen keyboard if we're in landscape mode so the user can see the
        // conversation.
        Configuration config = getResources().getConfiguration();
        if (config.orientation == config.ORIENTATION_LANDSCAPE) {
            closeSoftKeyboard();
        }
        
        updateSecureWarning();
        
    }
    
    boolean securityTextSet = false;
    
    void updateSecureWarning ()
    {

    	try
    	{
    		
    	
	        if (mChatSessionMgr!=null)
	        {
	            try {
					if (mChatSessionMgr.isEncryptedSession(mUserName))
					{
						if (!securityTextSet)
						{
					
							String localKeyFingerprint = mChatSessionMgr.getLocalKeyFingerprint(mUserName);
							String remoteKeyFingerprint = mChatSessionMgr.getRemoteKeyFingerprint(mUserName);
		
							StringBuffer statusText = new StringBuffer();
							statusText.append("ENCRYPTED CHAT SESSION (key fingerprints below)");
							statusText.append("\n");
							statusText.append("You: " + localKeyFingerprint);
							statusText.append("\n");
							statusText.append("Them: " + remoteKeyFingerprint);
							
							mStatusWarningView.setVisibility(View.VISIBLE);
							mWarningIcon.setVisibility(View.INVISIBLE);
						    mWarningText.setText(statusText.toString());
						     mWarningText.setTextSize(10);
	
						    mWarningText.setTextColor(Color.parseColor("#005500"));
						    
						    securityTextSet = true;
						}
					    
					}
					else
					{
						mStatusWarningView.setVisibility(View.VISIBLE);
						mWarningIcon.setVisibility(View.VISIBLE);
						mWarningText.setTextColor(Color.RED);
					    
					    mWarningText.setText("WARNING: Your chat is *NOT SECURE*");
					    
					    securityTextSet = false;
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
	        }
    	}
    	catch (Exception e)
    	{
    		Log.e("ChatView","Error checking security status: " + e);
    		
    	}
	           
    }

    void registerChatListener() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
            log("registerChatListener");
        }
        try {
            if (mChatSession != null) {
                mChatSession.registerChatListener(mChatListener);
            }
            IImConnection conn = mApp.getConnection(mProviderId);
            if (conn != null) {
                IContactListManager listMgr = conn.getContactListManager();
                listMgr.registerContactListListener(mContactListListener);
            }
            mApp.dismissChatNotification(mProviderId, mUserName);
        } catch (RemoteException e) {
            Log.w(ImApp.LOG_TAG, "<ChatView> registerChatListener fail:" + e.getMessage());
        }
    }

    void unregisterChatListener() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
            log("unregisterChatListener");
        }
        try {
            if (mChatSession != null) {
                mChatSession.unregisterChatListener(mChatListener);
            }
            IImConnection conn = mApp.getConnection(mProviderId);
            if (conn != null) {
                IContactListManager listMgr = conn.getContactListManager();
                listMgr.unregisterContactListListener(mContactListListener);
            }
        } catch (RemoteException e) {
            Log.w(ImApp.LOG_TAG, "<ChatView> unregisterChatListener fail:" + e.getMessage());
        }
    }

    void registerChatSessionListener() {
        IChatSessionManager sessionMgr = getChatSessionManager(mProviderId);
        if (sessionMgr != null) {
            mChatSessionListener = new ChatSessionListener();
            try {
                sessionMgr.registerChatSessionListener(mChatSessionListener);
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
        }
    }

    void unregisterChatSessionListener() {
        if (mChatSessionListener != null) {
            try {
                IChatSessionManager sessionMgr = getChatSessionManager(mProviderId);
                sessionMgr.unregisterChatSessionListener(mChatSessionListener);
                // We unregister the listener when the chat session we are
                // waiting for has been created or the activity is stopped.
                // Clear the listener so that we won't unregister the listener
                // twice.
                mChatSessionListener = null;
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
        }
    }

    void updateWarningView() {
        int visibility = View.GONE;
        int iconVisibility = View.GONE;
        String message = null;
        boolean isConnected;

        try {
            IImConnection conn = mApp.getConnection(mProviderId);
            isConnected = (conn == null) ? false
                    : conn.getState() != ImConnection.SUSPENDED;
        } catch (RemoteException e) {
            // do nothing
            return;
        }

        if (isConnected) {
            if (mType == Imps.Contacts.TYPE_TEMPORARY) {
                visibility = View.VISIBLE;
                message = mContext.getString(R.string.contact_not_in_list_warning, mNickName);
            } else if (mPresenceStatus == Imps.Presence.OFFLINE) {
                visibility = View.VISIBLE;
                message = mContext.getString(R.string.contact_offline_warning, mNickName);
            }
        } else {
            visibility = View.VISIBLE;
            iconVisibility = View.VISIBLE;
            message = mContext.getString(R.string.disconnected_warning);
        }

        mStatusWarningView.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            mWarningIcon.setVisibility(iconVisibility);
            mWarningText.setText(message);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        userActionDetected();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        userActionDetected();
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        userActionDetected();
        return super.dispatchTrackballEvent(ev);
    }

    private void userActionDetected() {
        if (mChatSession != null) {
            try {
                mChatSession.markAsRead();
                
                updateSecureWarning();

             
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
        }
    }

    private final class ChatViewHandler extends SimpleAlertHandler {
        public ChatViewHandler() {
            super(mScreen);
        }

        @Override
        public void handleMessage(Message msg) {
            long providerId = ((long)msg.arg1 << 32) | msg.arg2;
            if (providerId != mProviderId) {
                return;
            }

            switch(msg.what) {
            case ImApp.EVENT_CONNECTION_LOGGED_IN:
                log("Connection resumed");
                updateWarningView();
                return;
            case ImApp.EVENT_CONNECTION_SUSPENDED:
                log("Connection suspended");
                updateWarningView();
                return;
            }

            super.handleMessage(msg);
        }
    }

    class ChatSessionListener extends ChatSessionListenerAdapter {
        @Override
        public void onChatSessionCreated(IChatSession session) {
            try {
                if (session.isGroupChatSession()) {
                    final long id = session.getId();
                    unregisterChatSessionListener();
                    mHandler.post(new Runnable() {
                        public void run() {
                            bindChat(id);
                        }});
                }
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert();
            }
        }
    }

    public static class DeltaCursor implements Cursor {
        static final String DELTA_COLUMN_NAME = "delta";

        private Cursor mInnerCursor;
        private String[] mColumnNames;
        private int mDateColumn = -1;
        private int mDeltaColumn = -1;

        DeltaCursor(Cursor cursor) {
            mInnerCursor = cursor;

            String[] columnNames = cursor.getColumnNames();
            int len = columnNames.length;

            mColumnNames = new String[len + 1];

            for (int i = 0 ; i < len ; i++) {
                mColumnNames[i] = columnNames[i];
                if (mColumnNames[i].equals(Imps.Messages.DATE)) {
                    mDateColumn = i;
                }
            }

            mDeltaColumn = len;
            mColumnNames[mDeltaColumn] = DELTA_COLUMN_NAME;

            //if (DBG) log("##### DeltaCursor constructor: mDeltaColumn=" +
            //        mDeltaColumn + ", columnName=" + mColumnNames[mDeltaColumn]);
        }

        public int getCount() {
            return mInnerCursor.getCount();
        }

        public int getPosition() {
            return mInnerCursor.getPosition();
        }

        public boolean move(int offset) {
            return mInnerCursor.move(offset);
        }

        public boolean moveToPosition(int position) {
            return mInnerCursor.moveToPosition(position);
        }

        public boolean moveToFirst() {
            return mInnerCursor.moveToFirst();
        }

        public boolean moveToLast() {
            return mInnerCursor.moveToLast();
        }

        public boolean moveToNext() {
            return mInnerCursor.moveToNext();
        }

        public boolean moveToPrevious() {
            return mInnerCursor.moveToPrevious();
        }

        public boolean isFirst() {
            return mInnerCursor.isFirst();
        }

        public boolean isLast() {
            return mInnerCursor.isLast();
        }

        public boolean isBeforeFirst() {
            return mInnerCursor.isBeforeFirst();
        }

        public boolean isAfterLast() {
            return mInnerCursor.isAfterLast();
        }

        public int getColumnIndex(String columnName) {
            if (DELTA_COLUMN_NAME.equals(columnName)) {
                return mDeltaColumn;
            }

            int columnIndex = mInnerCursor.getColumnIndex(columnName);
            return columnIndex;
        }

        public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
            if (DELTA_COLUMN_NAME.equals(columnName)) {
                return mDeltaColumn;
            }

            return mInnerCursor.getColumnIndexOrThrow(columnName);
        }

        public String getColumnName(int columnIndex) {
            if (columnIndex == mDeltaColumn) {
                return DELTA_COLUMN_NAME;
            }

            return mInnerCursor.getColumnName(columnIndex);
        }

        public int getColumnCount() {
            return mInnerCursor.getColumnCount() + 1;
        }

        public void deactivate() {
            mInnerCursor.deactivate();
        }

        public boolean requery() {
            return mInnerCursor.requery();
        }

        public void close() {
            mInnerCursor.close();
        }

        public boolean isClosed() {
            return mInnerCursor.isClosed();
        }

        public void registerContentObserver(ContentObserver observer) {
            mInnerCursor.registerContentObserver(observer);
        }

        public void unregisterContentObserver(ContentObserver observer) {
            mInnerCursor.unregisterContentObserver(observer);
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            mInnerCursor.registerDataSetObserver(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            mInnerCursor.unregisterDataSetObserver(observer);
        }

        public void setNotificationUri(ContentResolver cr, Uri uri) {
            mInnerCursor.setNotificationUri(cr, uri);
        }

        public boolean getWantsAllOnMoveCalls() {
            return mInnerCursor.getWantsAllOnMoveCalls();
        }

        public Bundle getExtras() {
            return mInnerCursor.getExtras();
        }

        public Bundle respond(Bundle extras) {
            return mInnerCursor.respond(extras);
        }

        public String[] getColumnNames() {
            return mColumnNames;
        }

        private void checkPosition() {
            int pos = mInnerCursor.getPosition();
            int count = mInnerCursor.getCount();

            if (-1 == pos || count == pos) {
                throw new CursorIndexOutOfBoundsException(pos, count);
            }
        }

        public byte[] getBlob(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return null;
            }

            return mInnerCursor.getBlob(column);
        }

        public String getString(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                long value = getDeltaValue();
                return Long.toString(value);
            }

            return mInnerCursor.getString(column);
        }

        public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
            checkPosition();

            if (columnIndex == mDeltaColumn) {
                long value = getDeltaValue();
                String strValue = Long.toString(value);
                int len = strValue.length();
                char[] data = buffer.data;
                if (data == null || data.length < len) {
                    buffer.data = strValue.toCharArray();
                } else {
                    strValue.getChars(0, len, data, 0);
                }
                buffer.sizeCopied = strValue.length();
            } else {
                mInnerCursor.copyStringToBuffer(columnIndex, buffer);
            }
        }

        public short getShort(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return (short)getDeltaValue();
            }

            return mInnerCursor.getShort(column);
        }

        public int getInt(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return (int)getDeltaValue();
            }

            return mInnerCursor.getInt(column);
        }

        public long getLong(int column) {
        //if (DBG) log("DeltaCursor.getLong: column=" + column + ", mDeltaColumn=" + mDeltaColumn);
            checkPosition();

            if (column == mDeltaColumn) {
                return getDeltaValue();
            }

            return mInnerCursor.getLong(column);
        }

        public float getFloat(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return getDeltaValue();
            }

            return mInnerCursor.getFloat(column);
        }

        public double getDouble(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return getDeltaValue();
            }

            return mInnerCursor.getDouble(column);
        }

        public boolean isNull(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return false;
            }

            return mInnerCursor.isNull(column);
        }

        private long getDeltaValue() {
            int pos = mInnerCursor.getPosition();
            //Log.i(LOG_TAG, "getDeltaValue: mPos=" + mPos);

            long t2, t1;

            if (pos == getCount()-1) {
                t1 = mInnerCursor.getLong(mDateColumn);
                t2 = System.currentTimeMillis();
            } else {
                mInnerCursor.moveToPosition(pos + 1);
                t2 = mInnerCursor.getLong(mDateColumn);
                mInnerCursor.moveToPosition(pos);
                t1 = mInnerCursor.getLong(mDateColumn);
            }

            return t2 - t1;
        }
    }

    private class MessageAdapter extends CursorAdapter implements AbsListView.OnScrollListener {
        private int mScrollState;
        private boolean mNeedRequeryCursor;

        private int mNicknameColumn;
        private int mBodyColumn;
        private int mDateColumn;
        private int mTypeColumn;
        private int mErrCodeColumn;
        private int mDeltaColumn;
        private ChatBackgroundMaker mBgMaker;

        private LayoutInflater mInflater;

        public MessageAdapter(Activity context, Cursor c) {
            super(context, c, false);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mBgMaker = new ChatBackgroundMaker(context);
            if (c != null) {
                resolveColumnIndex(c);
            }
        }

        private void resolveColumnIndex(Cursor c) {
            mNicknameColumn = c.getColumnIndexOrThrow(Imps.Messages.NICKNAME);
            mBodyColumn = c.getColumnIndexOrThrow(Imps.Messages.BODY);
            mDateColumn = c.getColumnIndexOrThrow(Imps.Messages.DATE);
            mTypeColumn = c.getColumnIndexOrThrow(Imps.Messages.TYPE);
            mErrCodeColumn = c.getColumnIndexOrThrow(Imps.Messages.ERROR_CODE);
            mDeltaColumn = c.getColumnIndexOrThrow(DeltaCursor.DELTA_COLUMN_NAME);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            if (cursor != null) {
                resolveColumnIndex(cursor);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.new_message_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            MessageView chatMsgView = (MessageView) view;

            int type = cursor.getInt(mTypeColumn);
            String contact = isGroupChat() ? cursor.getString(mNicknameColumn) : mNickName;
            String body = cursor.getString(mBodyColumn);
            long delta = cursor.getLong(mDeltaColumn);
            boolean showTimeStamp = (delta > SHOW_TIME_STAMP_INTERVAL);
            Date date = showTimeStamp ? new Date(cursor.getLong(mDateColumn)) : null;

            switch (type) {
                case Imps.MessageType.INCOMING:
                    chatMsgView.bindIncomingMessage(contact, body, date, mMarkup, isScrolling());
                    break;

                case Imps.MessageType.OUTGOING:
                case Imps.MessageType.POSTPONED:
                    int errCode = cursor.getInt(mErrCodeColumn);
                    if (errCode != 0) {
                        chatMsgView.bindErrorMessage(errCode);
                    } else {
                        chatMsgView.bindOutgoingMessage(body, date, mMarkup, isScrolling());
                    }
                    break;

                default:
                    chatMsgView.bindPresenceMessage(contact, type, isGroupChat(), isScrolling());
            }
            if (!isScrolling()) {
                mBgMaker.setBackground(chatMsgView, contact, type);
            }

            // if showTimeStamp is false for the latest message, then set a timer to query the
            // cursor again in a minute, so we can update the last message timestamp if no new
            // message is received
            if (cursor.getPosition() == cursor.getCount()-1) {
                if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)){
                    log("delta = " + delta + ", showTs=" + showTimeStamp);
                }
                if (!showTimeStamp) {
                    scheduleRequery(SHOW_TIME_STAMP_INTERVAL);
                } else {
                    cancelRequery();
                }
            }
        }

        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            // do nothing
        }

        public void onScrollStateChanged(AbsListView view, int scrollState) {
            int oldState = mScrollState;
            mScrollState = scrollState;

            if (mChatSession != null) {
                try {
                    mChatSession.markAsRead();
                } catch (RemoteException e) {
                    mHandler.showServiceErrorAlert();
                }
            }

            if (oldState == OnScrollListener.SCROLL_STATE_FLING) {
                if (mNeedRequeryCursor) {
                    requeryCursor();
                } else {
                    notifyDataSetChanged();
                }
            }
        }

        boolean isScrolling() {
            return mScrollState == OnScrollListener.SCROLL_STATE_FLING;
        }

        void setNeedRequeryCursor(boolean requeryCursor) {
            mNeedRequeryCursor = requeryCursor;
        }
    }
}
