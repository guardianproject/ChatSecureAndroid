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

import info.guardianproject.emoji.EmojiGroup;
import info.guardianproject.emoji.EmojiManager;
import info.guardianproject.emoji.EmojiPagerAdapter;
import info.guardianproject.otr.IOtrChatSession;
import info.guardianproject.otr.OtrDebugLogger;
import info.guardianproject.otr.app.im.IChatListener;
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionListener;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IContactList;
import info.guardianproject.otr.app.im.IContactListListener;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IDataListener;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.MessageView.DeliveryState;
import info.guardianproject.otr.app.im.app.MessageView.EncryptionState;
import info.guardianproject.otr.app.im.app.adapter.ChatListenerAdapter;
import info.guardianproject.otr.app.im.app.adapter.ChatSessionListenerAdapter;
import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.ImpsAddressUtils;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.util.LogCleaner;
import info.guardianproject.util.SystemServices;
import info.guardianproject.util.SystemServices.Scanner;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import net.java.otr4j.session.SessionStatus;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AsyncQueryHandler;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DataSetObserver;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Browser;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonSyntaxException;

public class ChatView extends LinearLayout {
    // This projection and index are set for the query of active chats
    static final String[] CHAT_PROJECTION = { Imps.Contacts._ID, Imps.Contacts.ACCOUNT,
                                             Imps.Contacts.PROVIDER, Imps.Contacts.USERNAME,
                                             Imps.Contacts.NICKNAME, Imps.Contacts.TYPE,
                                             Imps.Presence.PRESENCE_STATUS,
                                             Imps.Chats.LAST_UNREAD_MESSAGE, 
                                             Imps.Chats._ID
    };
    
    static final int CONTACT_ID_COLUMN = 0;
    static final int ACCOUNT_COLUMN = 1;
    static final int PROVIDER_COLUMN = 2;
    static final int USERNAME_COLUMN = 3;
    static final int NICKNAME_COLUMN = 4;
    static final int TYPE_COLUMN = 5;
    static final int PRESENCE_STATUS_COLUMN = 6;
    static final int LAST_UNREAD_MESSAGE_COLUMN = 7;
    static final int CHAT_ID_COLUMN = 8;

    static final String[] INVITATION_PROJECT = { Imps.Invitation._ID, Imps.Invitation.PROVIDER,
                                                Imps.Invitation.SENDER, };
    static final int INVITATION_ID_COLUMN = 0;
    static final int INVITATION_PROVIDER_COLUMN = 1;
    static final int INVITATION_SENDER_COLUMN = 2;

    static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);
    static final StyleSpan STYLE_NORMAL = new StyleSpan(Typeface.NORMAL);

    Markup mMarkup;

    NewChatActivity mActivity;
    ImApp mApp;
    SimpleAlertHandler mHandler;
    Cursor mCursor;

    //private ImageView mStatusIcon;
   // private TextView mTitle;
    /*package*/ListView mHistory;
    EditText mComposeMessage;
    private ImageButton mSendButton;
    private View mStatusWarningView;
    private ImageView mWarningIcon;
    private TextView mWarningText;
    
    private ViewPager mEmojiPager;
    private View mActionBox;

    private ImageView mDeliveryIcon;
    private boolean mExpectingDelivery;
    
    private CompoundButton mOtrSwitch;
    private boolean mOtrSwitchTouched = false;
    private OnCheckedChangeListener mOtrListener = new OnCheckedChangeListener ()
    {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mActivity.setOTRState(ChatView.this, ChatView.this.getOtrChatSession(), isChecked);
         
            mOtrSwitchTouched = true;
        }
        
    };
    
    private MessageAdapter mMessageAdapter;
    private IChatSessionManager mChatSessionManager;
    private IChatSessionListener mChatSessionListener;

    private IChatSession mCurrentChatSession;
    private IOtrChatSession mOtrChatSession;

    private DataAdapter mDataListenerAdapter;
    
    
    long mLastChatId=-1;
    int mType;
    String mRemoteNickname;
    String mRemoteAddressString;
    Address mRemoteAddress;
    
    long mProviderId;
    long mAccountId;
    long mInvitationId;
    private Context mContext; // TODO
    private int mPresenceStatus;

    private int mViewType;

    private static final int VIEW_TYPE_CHAT = 1;
    private static final int VIEW_TYPE_INVITATION = 2;
    private static final int VIEW_TYPE_SUBSCRIPTION = 3;

    private static final long SHOW_TIME_STAMP_INTERVAL = 30 * 1000; // 1 minute
    private static final long SHOW_DELIVERY_INTERVAL = 5 * 1000; // 10 seconds
    private static final long DEFAULT_QUERY_INTERVAL = 1000;
    private static final long FAST_QUERY_INTERVAL = 100;
    private static final int QUERY_TOKEN = 10;

    // Async QueryHandler
    private final class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            mExpectingDelivery = false;
            setDeliveryIcon();
            
            if (c != null)
            {
                Cursor cursor = new DeltaCursor(c);
    
                if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                    log("onQueryComplete: cursor.count=" + cursor.getCount());
                }
    
                if (mMessageAdapter != null && cursor != null)
                    mMessageAdapter.changeCursor(cursor);
            }
        }
    }

    private QueryHandler mQueryHandler;

    public SimpleAlertHandler getHandler() {
        return mHandler;
    }

    public int getType() {
        return mViewType;
    }

    private class RequeryCallback implements Runnable {
        public void run() {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
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
            
            URLSpan[] links = ((MessageView) view).getMessageLinks();
            if (links.length > 0) {
                

                final ArrayList<String> linkUrls = new ArrayList<String>(links.length);
                for (URLSpan u : links) {
                    linkUrls.add(u.getURL());
                }
                ArrayAdapter<String> a = new ArrayAdapter<String>(mActivity,
                        android.R.layout.select_dialog_item, linkUrls);
                AlertDialog.Builder b = new AlertDialog.Builder(mActivity);
                b.setTitle(R.string.select_link_title);
                b.setCancelable(true);
                b.setAdapter(a, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse(linkUrls.get(which));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
                        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
                        intent.putExtra(Browser.EXTRA_APPLICATION_ID, mActivity.getPackageName());
                        mActivity.startActivity(intent);
                    }
                });
                b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                b.show();
            }
            else
            {
                viewProfile();
            }
        }
    };

    private IChatListener mChatListener = new ChatListenerAdapter() {
        @Override
        public void onIncomingMessage(IChatSession ses,
                info.guardianproject.otr.app.im.engine.Message msg) {
            scheduleRequery(FAST_QUERY_INTERVAL);

            mRemoteAddress = msg.getFrom();
          //  mRemoteAddressString = msg.getFrom().getAddress();
        }

        @Override
        public void onContactJoined(IChatSession ses, Contact contact) {
            scheduleRequery(DEFAULT_QUERY_INTERVAL);
        }

        @Override
        public void onContactLeft(IChatSession ses, Contact contact) {
            scheduleRequery(DEFAULT_QUERY_INTERVAL);
        }

        @Override
        public void onSendMessageError(IChatSession ses,
                info.guardianproject.otr.app.im.engine.Message msg, ImErrorInfo error) {
            scheduleRequery(FAST_QUERY_INTERVAL);
        }

        @Override
        public void onIncomingReceipt(IChatSession ses, String packetId) throws RemoteException {
            scheduleRequery(FAST_QUERY_INTERVAL);
        }

        @Override
        public void onStatusChanged(IChatSession ses) throws RemoteException {
            scheduleRequery(DEFAULT_QUERY_INTERVAL);
        };
        
        @Override
        public void onIncomingData(IChatSession ses, byte[] data) {
            try {
                Log.i("OTR_DATA", "incoming data " + new String(data, "UTF8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        };
    };


    private Runnable mUpdateChatCallback = new Runnable() {
        public void run() {
            if (mCursor.requery() && mCursor.moveToFirst()) {
                updateChat();
            }
        }
    };
    
    private IContactListListener mContactListListener = new IContactListListener.Stub() {
        public void onAllContactListsLoaded() {
        }

        public void onContactChange(int type, IContactList list, Contact contact) {
        }

        public void onContactError(int errorType, ImErrorInfo error, String listName,
                Contact contact) {
        }

        public void onContactsPresenceUpdate(Contact[] contacts) {
            
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("onContactsPresenceUpdate()");
            }
            for (Contact c : contacts) {
                if (Address.stripResource(c.getAddress().getAddress()).equals(Address.stripResource(mRemoteAddressString))) {
                    mHandler.post(mUpdateChatCallback);
                    scheduleRequery(DEFAULT_QUERY_INTERVAL);
                    break;
                }
            }
        }
    };

    private boolean mIsListening;

    static final void log(String msg) {
        Log.d(ImApp.LOG_TAG, "<ChatView> " + msg);
    }

    public ChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (NewChatActivity) context;
        mApp = (ImApp)mActivity.getApplication();
        mHandler = new ChatViewHandler(mActivity);
        mContext = context;

        ThemeableActivity.setBackgroundImage(this, mActivity);
    }
    
    void registerForConnEvents() {
        mApp.registerForConnEvents(mHandler);
    }

    void unregisterForConnEvents() {
        mApp.unregisterForConnEvents(mHandler);
    }
    
    @Override
    protected void onFinishInflate() {
      //  mStatusIcon = (ImageView) findViewById(R.id.statusIcon);
        mDeliveryIcon = (ImageView) findViewById(R.id.deliveryIcon);
       // mTitle = (TextView) findViewById(R.id.title);
        mHistory = (ListView) findViewById(R.id.history);
        mComposeMessage = (EditText) findViewById(R.id.composeMessage);
        mSendButton = (ImageButton) findViewById(R.id.btnSend);
        mHistory.setOnItemClickListener(mOnItemClickListener);

        mStatusWarningView = findViewById(R.id.warning);
        mWarningIcon = (ImageView) findViewById(R.id.warningIcon);
        mWarningText = (TextView) findViewById(R.id.warningText);
     
        mOtrSwitch = (CompoundButton)findViewById(R.id.otrSwitch);
       
        mHistory.setOnItemLongClickListener(new OnItemLongClickListener ()
        {

            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                
                
             if (arg1 instanceof MessageView)
             {

                 // Gets a handle to the clipboard service.
                 ClipboardManager clipboard = (ClipboardManager)
                         mActivity.getSystemService(Context.CLIPBOARD_SERVICE);

                 
                 String textToCopy = ((MessageView)arg1).getLastMessage();
                 
                 ClipData clip = ClipData.newPlainText("chat",textToCopy);
    
                 clipboard.setPrimaryClip(clip);
                 
                 Toast.makeText(mActivity, "message copied to the clipboard", Toast.LENGTH_SHORT).show();
                 
                 return true;
                 
             }
                
                return false;
            }
            
        });
        
        mWarningText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
               viewProfile();
                
            }

           

        });
        
        mOtrSwitch.setOnCheckedChangeListener(mOtrListener);
        
        
        
        mComposeMessage.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                        sendMessage();
                        return true;

                    case KeyEvent.KEYCODE_ENTER:
                        if (event.isAltPressed()) {
                            mComposeMessage.append("\n");
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        mComposeMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null) {
                    if (event.isAltPressed()) {
                        return false;
                    }
                }

                InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && imm.isActive(v)) {
                    imm.hideSoftInputFromWindow(getWindowToken(), 0);
                }
                sendMessage();
                return true;
            }
        });

        // TODO: this is a hack to implement BUG #1611278, when dispatchKeyEvent() works with
        // the soft keyboard, we should remove this hack.
        mComposeMessage.addTextChangedListener(new TextWatcher() {
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

        mActionBox = (View)findViewById(R.id.actionBox);
        ImageButton btnActionBox = (ImageButton)findViewById(R.id.btnActionBox);
        btnActionBox.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
                
                mEmojiPager.setVisibility(View.GONE);
                
                 
                if (mActionBox.getVisibility() == View.GONE)
                    mActionBox.setVisibility(View.VISIBLE);
                else
                    mActionBox.setVisibility(View.GONE);
            }
            
        });
        
        View btnEndChat = findViewById(R.id.btnEndChat);
        btnEndChat.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
                 
                ChatView.this.closeChatSession();
            }
            
        });
        
        View btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
                 
                viewProfile();
            }
            
        });
        
        View btnSharePicture = findViewById(R.id.btnSendPicture);
        btnSharePicture.setOnClickListener(new OnClickListener ()
        {
            
            @Override
            public void onClick(View v) {
                mActivity.startImagePicker();
            }
            
        });
        
        View btnShareFile = findViewById(R.id.btnSendFile);
        btnShareFile.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
                mActivity.startFilePicker();
            }
            
        });
        
        
        initEmoji();
        
        
        
    }

    private static EmojiManager emojiManager = null;
    
    private synchronized void initEmoji ()
    {
        if (emojiManager == null)
        {
            emojiManager = EmojiManager.getInstance(mContext);

            try
            {
                emojiManager.addJsonDefinitions("emoji/phantom.json", "emoji/phantom", "png");
             
                emojiManager.addJsonPlugins();
                
            }
            catch (JsonSyntaxException jse)
            {
                    Log.e(ImApp.LOG_TAG,"could not parse json", jse);
            }
            catch (IOException fe)
            {
                    Log.e(ImApp.LOG_TAG,"could not load emoji definition",fe);
            }       
            catch (Exception fe)
            {
                    Log.e(ImApp.LOG_TAG,"could not load emoji definition",fe);
            }      
        }
        
        
        mEmojiPager = (ViewPager)this.findViewById(R.id.emojiPager);
            
        Collection<EmojiGroup> emojiGroups = emojiManager.getEmojiGroups();
        
        EmojiPagerAdapter emojiPagerAdapter = new EmojiPagerAdapter(mActivity, mComposeMessage, new ArrayList<EmojiGroup>(emojiGroups));
      
        mEmojiPager.setAdapter(emojiPagerAdapter);
        
        ImageView btnEmoji = (ImageView)findViewById(R.id.btnEmoji);
        btnEmoji.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
                 

                mActionBox.setVisibility(View.GONE);
                
                if (mEmojiPager.getVisibility() == View.GONE)
                    mEmojiPager.setVisibility(View.VISIBLE);
                else
                    mEmojiPager.setVisibility(View.GONE);
            }
            
        });

           
        
    }
    
  
    @Override
    public boolean onTouchEvent(MotionEvent event) {
       
        
        if (event.getPointerCount() > 1 && event.getAction() == MotionEvent.ACTION_DOWN)
        {
            

            ChatView.this.closeChatSession();
            
            return true;
        }
        
        return false;
        
    }

    public void startListening() {
        mIsListening = true;
        if (mViewType == VIEW_TYPE_CHAT) {
            Cursor cursor = getMessageCursor();
            if (cursor == null) {
                long chatId = getChatId();
                if (chatId != -1)
                    startQuery(chatId);
            } else {
                requeryCursor();
            }
        }
        registerChatListener();
        registerForConnEvents();

        updateWarningView();
    }

    public void stopListening() {
        //Cursor cursor = getMessageCursor();
        //if (cursor != null && (!cursor.isClosed())) {
         //   cursor.close();
       // }
        
        cancelRequery();
        unregisterChatListener();
        unregisterForConnEvents();
        unregisterChatSessionListener();
        mIsListening = false;
    }

    
    
    void updateChat() {
        setViewType(VIEW_TYPE_CHAT);

        updateContactInfo();

        setStatusIcon();
        
        IImConnection conn = mApp.getConnection(mProviderId);
        if (conn == null) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG))
                log("Connection has been signed out");
          
            return;
        }
        
        mMessageAdapter = new MessageAdapter(mActivity, null);
        mHistory.setAdapter(mMessageAdapter);
        mHistory.invalidate();
        
        startQuery(getChatId());
        // This is not needed, now that there is a ChatView per fragment.  It also causes a spurious detection of user action
        // on fragments adjacent to the current one, when they get initialized.
        //mComposeMessage.setText("");
        mOtrChatSession = null;
    
        updateWarningView();
        setDeliveryIcon();
    }

    private void updateContactInfo() {
       // mChatId = mCursor.getLong(CONTACT_ID_COLUMN);
        mProviderId = mCursor.getLong(PROVIDER_COLUMN);
        mAccountId = mCursor.getLong(ACCOUNT_COLUMN);
        mPresenceStatus = mCursor.getInt(PRESENCE_STATUS_COLUMN);
        mType = mCursor.getInt(TYPE_COLUMN);
        mRemoteAddressString = mCursor.getString(USERNAME_COLUMN);
        mRemoteNickname = mCursor.getString(NICKNAME_COLUMN);
        
        
    }

    /*
    private void setTitle() {
        
        if (mType == Imps.Contacts.TYPE_GROUP) {
            final String[] projection = { Imps.GroupMembers.NICKNAME };
            Uri memberUri = ContentUris.withAppendedId(Imps.GroupMembers.CONTENT_URI, mChatId);
            ContentResolver cr = mActivity.getContentResolver();
            Cursor c = cr.query(memberUri, projection, null, null, null);
            StringBuilder buf = new StringBuilder();
            BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);

            if (c != null) {
                while (c.moveToNext()) {

                    String nickname = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.NICKNAME));
                    int status = c.getInt(c.getColumnIndexOrThrow(Imps.Contacts.PRESENCE_STATUS));
                    buf.append(nickname);
                    buf.append(" (");
                    buf.append(brandingRes.getString(PresenceUtils.getStatusStringRes(this.mPresenceStatus)));
                    buf.append(")");
                    if (!c.isLast()) {
                        buf.append(',');
                    }
                }
              
            }
            
            mActivity.setTitle(buf.toString());
            
        } else {
            
        
            StringBuilder buf = new StringBuilder();
           
            BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
           
            buf.append(this.mNickName);
            buf.append(" (");
            buf.append(brandingRes.getString(PresenceUtils.getStatusStringRes(this.mPresenceStatus)));
            buf.append(")");
            
            mActivity.setTitle(buf.toString());
       
            Drawable avatar = loadAvatar(mUserName);
            
           // if (avatar != null)
           // mActivity.setHomeIcon(avatar);
            
       // }
    }*/
    
    private Drawable loadAvatar (String jid)
    {
        try
        {
            //String filename = Base64.encodeBase64String(jid.getBytes()) + ".jpg";
            String fileName = Base64.encodeToString(jid.getBytes(), Base64.NO_WRAP) + ".jpg";
            File sdCard = new File(mActivity.getCacheDir(),"avatars");
            File fileAvatar = new File(sdCard, fileName);
            
            if (fileAvatar.exists())
            {
                return new BitmapDrawable(BitmapFactory.decodeFile(fileAvatar.getCanonicalPath()));
            }
            else
                return null;
        }
        catch (IOException ioe)
        {
            Log.e("Contacts","error loading avatar",ioe);
            return null;
        }
    }

    private void setStatusIcon() {
        if (mType == Imps.Contacts.TYPE_GROUP) {
            // hide the status icon for group chat.
         //   mStatusIcon.setVisibility(GONE);
        } else {
          //  mStatusIcon.setVisibility(VISIBLE);
            BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
            int presenceResId = PresenceUtils.getStatusIconId(mPresenceStatus);
            //mStatusIcon.setImageDrawable(brandingRes.getDrawable(presenceResId));
            
        }
    }

    private void setDeliveryIcon() {
        if (mExpectingDelivery) {
            mDeliveryIcon.setVisibility(VISIBLE);
        } else {
            mDeliveryIcon.setVisibility(GONE);
        }
    }

    public void rebind ()
    {
        bindChat(mLastChatId);
    }
    
    private void deleteChat ()
    {
        Uri chatUri = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, mLastChatId);
        mActivity.getContentResolver().delete(chatUri,null,null);
    }
    
   
    public void bindChat(long contactId) {
        
        mLastChatId = contactId;
        
        if (mCursor != null && (!mCursor.isClosed())) {
            mCursor.close();
        }
        
        Uri contactUri = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, contactId);
        mCursor = mActivity.managedQuery(contactUri, CHAT_PROJECTION, null, null, null);
        
        if (mCursor == null || !mCursor.moveToFirst()) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("Failed to query chat: " + contactId);
            }
            mLastChatId = -1;
        } else {
            
            mCurrentChatSession = getChatSession(mCursor);
            
            updateChat();
            
            if (mCurrentChatSession != null)
            {
                // This will save the current chatId and providerId in the relevant fields.
                // getChatSessionManager depends on mProviderId getting the cursor value of providerId.
                
                registerChatListener();
                
                try
                {
                    if (mDataListenerAdapter == null)
                        mDataListenerAdapter = new DataAdapter();
                   
                    mCurrentChatSession.setDataListener(mDataListenerAdapter);
                }
                catch (RemoteException re)
                {
                    if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG))
                        log("error setting remote data listener: " + re.getLocalizedMessage());
                }
            }
        }
        
        updateWarningView();
        
        
    }
    
    private IChatSession getChatSession ()
    {
        return getChatSession(false);
    }
    
    private IChatSession getChatSession (boolean autoInit)
    {
        if (mCurrentChatSession == null && autoInit)
            bindChat(mLastChatId);
        
        return mCurrentChatSession;
    }

    private void initOtr()  {

        if (mOtrChatSession == null)
        {
            try
            {
                
                if (getChatSession() != null)
                    mOtrChatSession = getChatSession ().getOtrChatSession();
             
            }
            catch (Exception e)
            {
                Log.e(ImApp.LOG_TAG,"error setting up OTR session",e);
            }
        }

    }

    public void bindInvitation(long invitationId) {
        Uri uri = ContentUris.withAppendedId(Imps.Invitation.CONTENT_URI, invitationId);
        ContentResolver cr = mActivity.getContentResolver();
        Cursor cursor = cr.query(uri, INVITATION_PROJECT, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("Failed to query invitation: " + invitationId);
            }
          //  mActivity.finish();
        } else {
            setViewType(VIEW_TYPE_INVITATION);

            mInvitationId = cursor.getLong(INVITATION_ID_COLUMN);
            mProviderId = cursor.getLong(INVITATION_PROVIDER_COLUMN);
            String sender = cursor.getString(INVITATION_SENDER_COLUMN);

            TextView mInvitationText = (TextView) findViewById(R.id.txtInvitation);
            mInvitationText.setText(mContext.getString(R.string.invitation_prompt, sender));
            mActivity.setTitle(mContext.getString(R.string.chat_with, sender));
        }

       
    }

    /*
    public void bindSubscription(long providerId, String from) {
        mProviderId = providerId;
        
        mRemoteAddressString = from;

        setViewType(VIEW_TYPE_SUBSCRIPTION);

        TextView text = (TextView) findViewById(R.id.txtSubscription);
        String displayableAddr = ImpsAddressUtils.getDisplayableAddress(from);
        text.setText(mContext.getString(R.string.subscription_prompt, displayableAddr));
        mActivity.setTitle(mContext.getString(R.string.chat_with, displayableAddr));

        mApp.dismissChatNotification(providerId, from);
    }*/

    

    private void setViewType(int type) {
        mViewType = type;
        if (type == VIEW_TYPE_CHAT) {
            findViewById(R.id.invitationPanel).setVisibility(GONE);
            findViewById(R.id.subscription).setVisibility(GONE);
            setChatViewEnabled(true);
        } else if (type == VIEW_TYPE_INVITATION) {
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
        mComposeMessage.setEnabled(enabled);
        mSendButton.setEnabled(enabled);
        if (enabled) {
            // This can steal focus from the fragment that's in front of the user
            //mComposeMessage.requestFocus();
        } else {
            mHistory.setAdapter(null);
        }

    }

    ListView getHistoryView() {
        return mHistory;
    }

    private void startQuery(long chatId) {
        if (mQueryHandler == null) {
            mQueryHandler = new QueryHandler(mContext);
        } else {
            // Cancel any pending queries
            mQueryHandler.cancelOperation(QUERY_TOKEN);
        }

        Uri uri = Imps.Messages.getContentUriByThreadId(chatId);

        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("queryCursor: uri=" + uri);
        }

        mQueryHandler.startQuery(QUERY_TOKEN, null, uri, null, null /* selection */,
                null /* selection args */, "date");
    }

    void scheduleRequery(long interval) {
        if (mRequeryCallback == null) {
            mRequeryCallback = new RequeryCallback();
        } else {
            mHandler.removeCallbacks(mRequeryCallback);
        }

        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("scheduleRequery");
        }
        mHandler.postDelayed(mRequeryCallback, interval);
    }

    void cancelRequery() {
        if (mRequeryCallback != null) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
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

        // This is redundant if there are messages in view, because the cursor requery will update everything.
        // However, if there are no messages, no update will trigger below, and we still want this to update.
        updateWarningView(true);

        // TODO: async query?
        Cursor cursor = getMessageCursor();
        if (cursor != null) {
            cursor.requery();
        }
    }

    private Cursor getMessageCursor() {
        return mMessageAdapter == null ? null : mMessageAdapter.getCursor();
    }

    public void closeChatSession() {
        if (getChatSession() != null) {
            try {
                getChatSession().leave();
                
            } catch (RemoteException e) {
                
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
            }
        } 
        
        deleteChat();
                
    }

    public void closeChatSessionIfInactive() {
        if (getChatSession() != null) {
            try {
                getChatSession().leaveIfInactive();
            } catch (RemoteException e) {
                
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
            }
        }
        
        deleteChat();
    
    }

    public void viewProfile() {
        if (getChatId() == -1)
            return;
        
        Uri data = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, getChatId());

        Intent intent = new Intent(Intent.ACTION_VIEW, data);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
        
            if (mRemoteAddress != null)
                intent.putExtra("jid", mRemoteAddress.getAddress());
        
        mActivity.startActivity(intent);

    }

    public void blockContact() {
        // TODO: unify with codes in ContactListView
        DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    IImConnection conn = mApp.getConnection(mProviderId);
                    IContactListManager manager = conn.getContactListManager();
                    manager.blockContact(mRemoteAddressString);
                  //  mActivity.finish();
                } catch (RemoteException e) {

                    mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                    LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
                }
            }
        };

        Resources r = getResources();

        // The positive button is deliberately set as no so that
        // the no is the default value
        new AlertDialog.Builder(mContext).setTitle(R.string.confirm)
                .setMessage(r.getString(R.string.confirm_block_contact, mRemoteNickname))
                .setPositiveButton(R.string.yes, confirmListener) // default button
                .setNegativeButton(R.string.no, null).setCancelable(false).show();
    }

    public long getProviderId() {
        return mProviderId;
    }

    public long getAccountId() {
        return mAccountId;
    }

    public String getUserName() {
        return mRemoteAddressString;
    }

    public long getChatId() {
        try {
            return getChatSession() == null ? -1 : getChatSession().getId();
        } catch (RemoteException e) {
            
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
            return -1;
        }
    }

    public IChatSession getCurrentChatSession() {
        return mCurrentChatSession;
    }

    private IChatSessionManager getChatSessionManager(long providerId) {
        if (mChatSessionManager == null || mProviderId != providerId) {

            IImConnection conn = mApp.getConnection(providerId);

            if (conn != null) {
                try {
                    mChatSessionManager = conn.getChatSessionManager();
                } catch (RemoteException e) {
                    
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
                }
            }
        }

        return mChatSessionManager;
    }


    public IOtrChatSession getOtrChatSession() {
        initOtr();

        return mOtrChatSession;
    }

    private IChatSession getChatSession(Cursor cursor) {
        long providerId = cursor.getLong(PROVIDER_COLUMN);
        String username = cursor.getString(USERNAME_COLUMN);

        IChatSessionManager sessionMgr = getChatSessionManager(providerId);
        if (sessionMgr != null) {
            try {
                IChatSession session = sessionMgr.getChatSession(username);
                
                if (session == null)
                    session = sessionMgr.createChatSession(username);
              
                return session;
            } catch (RemoteException e) {
                
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
            }
        }

        return null;
    }

    boolean isGroupChat() {
        
        boolean isGroupChat = false;
        
        if (mCurrentChatSession != null)
        {
            try {
                isGroupChat = mCurrentChatSession.isGroupChatSession();
            }
            catch (Exception e){}
            
        }
           
        return isGroupChat;
    }

    void sendMessage() {
        
        mEmojiPager.setVisibility(View.GONE);
        mActionBox.setVisibility(View.GONE);
        
        String msg = mComposeMessage.getText().toString();

        if (TextUtils.isEmpty(msg.trim())) {
            return;
        }
        
        IChatSession session = getChatSession(true);
        
        if (session != null) {
            try {
                session.sendMessage(msg);
                mComposeMessage.setText("");
                mComposeMessage.requestFocus();
                requeryCursor();
            } catch (RemoteException e) {
                
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
            } catch (Exception e) {
                
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
            }
        }
    }
    
    void sendMessage(String msg) {

        if (TextUtils.isEmpty(msg.trim())) {
            return;
        }

        if (getChatSession() != null) {
            try {
                getChatSession().sendMessage(msg);
                requeryCursor();
            } catch (Exception e) {
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);    
            }
        }
    }

    void registerChatListener() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("registerChatListener");
        }
        try {
            if (getChatSession() != null) {
                getChatSession().registerChatListener(mChatListener);
            }
            IImConnection conn = mApp.getConnection(mProviderId);
            if (conn != null) {
                IContactListManager listMgr = conn.getContactListManager();
                listMgr.registerContactListListener(mContactListListener);
            }
        } catch (RemoteException e) {
            Log.w(ImApp.LOG_TAG, "<ChatView> registerChatListener fail:" + e.getMessage());
        }
    }

    void unregisterChatListener() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("unregisterChatListener");
        }
        try {
            if (getChatSession() != null) {
                getChatSession().unregisterChatListener(mChatListener);
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
                
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
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
                
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
            }
        }
    }

    void updateWarningView()
    {
        updateWarningView(false);
    }
    
    void updateWarningView(boolean overrideUserTouch) {
        int visibility = View.GONE;
        int iconVisibility = View.GONE;
        String message = null;
        boolean isConnected;

        SessionStatus sessionStatus = null;
        
        if (overrideUserTouch)
            mOtrSwitchTouched = false;

        if (this.isGroupChat())
        {
            //no OTR in group chat
            mStatusWarningView.setVisibility(View.GONE);
            return;
        }
        
        initOtr();

        //check if the chat is otr or not
        if (mOtrChatSession != null) {
            try {
                sessionStatus = SessionStatus.values()[mOtrChatSession.getChatStatus()];
            } catch (RemoteException e) {
                Log.w("Gibber", "Unable to call remote OtrChatSession from ChatView", e);
            }
        }

        try {
            IImConnection conn = mApp.getConnection(mProviderId);
            isConnected = (conn == null) ? false : conn.getState() != ImConnection.SUSPENDED;
        } catch (RemoteException e) {
           
            isConnected = false;
        }

        if (isConnected) {

            if (mType == Imps.Contacts.TYPE_GROUP) {
                visibility = View.GONE;
                message = "";
            }
            else if (mType == Imps.Contacts.TYPE_TEMPORARY) {
                visibility = View.VISIBLE;
                message = mContext.getString(R.string.contact_not_in_list_warning, mRemoteNickname);
            } else if (mPresenceStatus == Imps.Presence.OFFLINE) {
                visibility = View.VISIBLE;
                message = mContext.getString(R.string.contact_offline_warning, mRemoteNickname);
            } else {

                visibility = View.VISIBLE;

            }

            if (mPresenceStatus == Imps.Presence.OFFLINE)
            {
                mWarningText.setTextColor(Color.WHITE);
                mStatusWarningView.setBackgroundColor(Color.DKGRAY);
                message = mContext.getString(R.string.presence_offline);
                
                /*
                if (!mOtrSwitchTouched)
                {
                    mOtrSwitch.setOnCheckedChangeListener(null);
                    mOtrSwitch.setChecked(false);
                    mOtrSwitch.setOnCheckedChangeListener(mOtrListener);
                }
                */
                
            }
            else if (sessionStatus == SessionStatus.ENCRYPTED) {
                try {

                    if (mOtrChatSession == null)
                        initOtr();

                    if (!mOtrSwitchTouched)
                    {                    
                        mOtrSwitch.setOnCheckedChangeListener(null);
                        mOtrSwitch.setChecked(true);
                        mOtrSwitch.setOnCheckedChangeListener(mOtrListener);
                    }
                    
                    String rFingerprint = mOtrChatSession.getRemoteFingerprint();
                    boolean rVerified = mOtrChatSession.isKeyVerified(mRemoteAddressString);

                    if (rFingerprint != null) {
                        if (!rVerified) {
                            message = mContext.getString(R.string.otr_session_status_encrypted);

                            mWarningText.setTextColor(Color.BLACK);
                            mStatusWarningView.setBackgroundResource(R.color.otr_yellow);
                        } else {
                            message = mContext.getString(R.string.otr_session_status_verified);

                            mWarningText.setTextColor(Color.BLACK);
                            mStatusWarningView.setBackgroundResource(R.color.otr_green);
                        }
                    } else {
                        mWarningText.setTextColor(Color.WHITE);
                        mStatusWarningView.setBackgroundResource(R.color.otr_red);
                        message = mContext.getString(R.string.otr_session_status_plaintext);
                    }

                  //  ImageView imgSec = (ImageView) findViewById(R.id.composeSecureIcon);
//                    imgSec.setImageResource(R.drawable.ic_menu_encrypt);             
                 //   mSendButton.setCompoundDrawablesWithIntrinsicBounds( getContext().getResources().getDrawable(R.drawable.ic_menu_encrypt ), null, null, null );
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else if (sessionStatus == SessionStatus.FINISHED) {
            //    mSendButton.setCompoundDrawablesWithIntrinsicBounds( getContext().getResources().getDrawable(R.drawable.ic_menu_unencrypt ), null, null, null );

                if (!mOtrSwitchTouched)
                { 
                    mOtrSwitch.setOnCheckedChangeListener(null);
                    mOtrSwitch.setChecked(true);
                    mOtrSwitch.setOnCheckedChangeListener(mOtrListener);
                }
                
                mWarningText.setTextColor(Color.WHITE);
                mStatusWarningView.setBackgroundColor(Color.DKGRAY);
                message = mContext.getString(R.string.otr_session_status_finished);
                
                mOtrChatSession = null;
            }  
            else if (sessionStatus == SessionStatus.PLAINTEXT) {

            //    mOtrSwitch.setChecked(false);

                if (!mOtrSwitchTouched)
                { 
                    mOtrSwitch.setOnCheckedChangeListener(null);
                    mOtrSwitch.setChecked(false);
                    mOtrSwitch.setOnCheckedChangeListener(mOtrListener);
                }
                
//                ImageView imgSec = (ImageView) findViewById(R.id.composeSecureIcon);
  //              imgSec.setImageResource(R.drawable.ic_menu_unencrypt);

            //    mSendButton.setCompoundDrawablesWithIntrinsicBounds( getContext().getResources().getDrawable(R.drawable.ic_menu_unencrypt ), null, null, null );
                
                mWarningText.setTextColor(Color.WHITE);
                mStatusWarningView.setBackgroundResource(R.color.otr_red);
                message = mContext.getString(R.string.otr_session_status_plaintext);
            }

        } else {
            

            mOtrSwitch.setOnCheckedChangeListener(null);
            mOtrSwitch.setChecked(false);
            mOtrSwitch.setOnCheckedChangeListener(mOtrListener);
            
            
            visibility = View.VISIBLE;
            iconVisibility = View.VISIBLE;
            mWarningText.setTextColor(Color.WHITE);
            mWarningText.setBackgroundColor(Color.DKGRAY);
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
        // Check that we have a chat session and that our fragment is resumed
        // The latter filters out bogus TextWatcher events on restore from saved
        if (getChatSession() != null && mIsListening) {
            try {
                getChatSession().markAsRead();
              
              //  updateWarningView();

            } catch (RemoteException e) {
                
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
            }
        }
    }

    private final class ChatViewHandler extends SimpleAlertHandler {
      

        public ChatViewHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            long providerId = ((long) msg.arg1 << 32) | msg.arg2;
            if (providerId != mProviderId) {
                return;
            }

            switch (msg.what) {

            case ImApp.EVENT_CONNECTION_DISCONNECTED:
                log("Handle event connection disconnected.");
                updateWarningView();
                promptDisconnectedEvent(msg);
                return;
             default:
                 updateWarningView();
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
                        }
                    });
                }

                updateWarningView();

            } catch (RemoteException e) {

                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "on chat session created error",e);    
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

            for (int i = 0; i < len; i++) {
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
                return (short) getDeltaValue();
            }

            return mInnerCursor.getShort(column);
        }

        public int getInt(int column) {
            checkPosition();

            if (column == mDeltaColumn) {
                return (int) getDeltaValue();
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

            if (pos == getCount() - 1) {
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

        public int getType(int arg0) {
            // TODO Auto-generated method stub
            return 0;
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
        private int mDeliveredColumn;

        private LayoutInflater mInflater;

        public MessageAdapter(Activity context, Cursor c) {
            super(context, c, false);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
           
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
            mDeliveredColumn = c.getColumnIndexOrThrow(Imps.Messages.IS_DELIVERED);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            
            if (getCursor() != null && (!getCursor().isClosed()))
                getCursor().close();
            
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
            MessageView messageView = (MessageView) view;

            mType = cursor.getInt(mTypeColumn);
            
            String nickname = isGroupChat() ? cursor.getString(mNicknameColumn) : mRemoteNickname;
            String body = cursor.getString(mBodyColumn);
            long delta = cursor.getLong(mDeltaColumn);
            boolean showTimeStamp = (delta > SHOW_TIME_STAMP_INTERVAL);
            long timestamp = cursor.getLong(mDateColumn);
            
            Date date = showTimeStamp ? new Date(timestamp) : null;
            boolean isDelivered = cursor.getLong(mDeliveredColumn) > 0;
            boolean showDelivery = ((System.currentTimeMillis() - timestamp) > SHOW_DELIVERY_INTERVAL);
            
            DeliveryState deliveryState = DeliveryState.NEUTRAL;
            if (showDelivery && !isDelivered && mExpectingDelivery) {
                deliveryState = DeliveryState.UNDELIVERED;
            }
            
            EncryptionState encState = EncryptionState.NONE;
            if (mType == Imps.MessageType.INCOMING_ENCRYPTED)
            {
                mType = Imps.MessageType.INCOMING;
                encState = EncryptionState.ENCRYPTED;
            }
            else if (mType == Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED)
            {
                 mType = Imps.MessageType.INCOMING;
                 encState = EncryptionState.ENCRYPTED_AND_VERIFIED;
            }
            else if (mType == Imps.MessageType.OUTGOING_ENCRYPTED)
            {
                mType = Imps.MessageType.OUTGOING;
                encState = EncryptionState.ENCRYPTED;
            }
            else if (mType == Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED)
            {
                 mType = Imps.MessageType.OUTGOING;
                 encState = EncryptionState.ENCRYPTED_AND_VERIFIED;
            }
            
            switch (mType) {
            case Imps.MessageType.INCOMING:
                if (body != null)
                {
                   messageView.bindIncomingMessage(mRemoteAddressString, nickname, body, date, mMarkup, isScrolling(), encState, isGroupChat());
                }

                break;

            case Imps.MessageType.OUTGOING:
            case Imps.MessageType.POSTPONED:
                
                int errCode = cursor.getInt(mErrCodeColumn);
                if (errCode != 0) {
                    messageView.bindErrorMessage(errCode);
                } else {
                    messageView.bindOutgoingMessage(null, body, date, mMarkup, isScrolling(),
                            deliveryState, encState);
                }
                
                break;

            default:
                messageView.bindPresenceMessage(mRemoteAddressString, mType, isGroupChat(), isScrolling());
            }

           // updateWarningView();

            if (!mExpectingDelivery && isDelivered) {
                log("Setting delivery icon");
                mExpectingDelivery = true;
                setDeliveryIcon();
                scheduleRequery(DEFAULT_QUERY_INTERVAL); // FIXME workaround to no refresh
            } else if (cursor.getPosition() == cursor.getCount() - 1) {
                // if showTimeStamp is false for the latest message, then set a timer to query the
                // cursor again in a minute, so we can update the last message timestamp if no new
                // message is received
                if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                    log("delta = " + delta + ", showTs=" + showTimeStamp);
                }
                if (!showDelivery) {
                    scheduleRequery(SHOW_DELIVERY_INTERVAL);
                } else if (!showTimeStamp) {
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

            if (getChatSession() != null) {
                try {
                    getChatSession().markAsRead();
                } catch (RemoteException e) {
                    
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e); 
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

    Cursor getMessageAtPosition(int position) {
        Object item = mMessageAdapter.getItem(position);
        return (Cursor) item;
    }

    EditText getComposedMessage() {
        return mComposeMessage;
    }
  
    class DataAdapter extends IDataListener.Stub {
        
        @Override
        public void onTransferComplete(String from, String url, String type, String filePath) {
            // TODO have a specific notifier for files / data
            //String username = from.getScreenName();
           
            
            File file = new File(filePath);
            
            try {
                Message msg = Message.obtain(mTransferHandler, 3);            
                msg.getData().putString("path", file.getCanonicalPath());
                msg.getData().putString("type", type);
                
                mTransferHandler.sendMessage(msg);
            } catch (IOException e) {
                mHandler.showAlert("Transfer Error", "Unable to read file to storage");
                OtrDebugLogger.log("error reading file", e);
            }
            

        }

        @Override
        public void onTransferFailed(String from, String url, String reason) {
            

            String[] path = url.split("/"); 
            String sanitizedPath = SystemServices.sanitize(path[path.length - 1]);
         

            Message msg = Message.obtain(mTransferHandler, 2);
            msg.getData().putInt("progress", (int)0);
            msg.getData().putString("status", sanitizedPath + " transfer failed: " + reason);
            
            mTransferHandler.sendMessage(msg);
        }

        @Override
        public void onTransferProgress(String from, String url, float percentF) {
            
            long percent = (long)(100.00*percentF);
            
            String[] path = url.split("/"); 
            String sanitizedPath = SystemServices.sanitize(path[path.length - 1]);
            

            Message msg = Message.obtain(mTransferHandler, 2);
            msg.getData().putInt("progress", (int)percent);
            msg.getData().putString("status", sanitizedPath);
            
            mTransferHandler.sendMessage(msg);
        }

        private boolean mAcceptTransfer = false;
        private boolean mWaitingForResponse = false;
        
        @Override
        public boolean onTransferRequested(String from, String to, String transferUrl) {
            
            mAcceptTransfer = false;            
            mWaitingForResponse = true;
            
            Message msg = Message.obtain(mTransferHandler, 1);
            msg.getData().putString("from", from);
            msg.getData().putString("url", transferUrl);
            
            mTransferHandler.sendMessage(msg);
            
            while (mWaitingForResponse)
            {
                try { Thread.sleep(500);} catch (Exception e){}
            }
            
            return mAcceptTransfer;
            
        }
        
        private Handler mTransferHandler = new Handler ()
        {

            @Override
            public void handleMessage(Message msg) {
            
                if (msg.what == 1)
                {
                    String transferUrl = msg.getData().getString("url");
                    String transferFrom = msg.getData().getString("from");
    
                    String[] path = transferUrl.split("/"); 
                    String sanitizedPath = SystemServices.sanitize(path[path.length - 1]);
                    
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    
                    builder.setTitle("Incoming File");
                    builder.setMessage(transferFrom + " wants to send you the file '" + sanitizedPath + "'. Accept transfer?");
    
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
    
                        public void onClick(DialogInterface dialog, int which) {
                            mAcceptTransfer = true;
                            mWaitingForResponse = false;
                            NOTIFY_DOWNLOAD_ID++;
                            
                            dialog.dismiss();
                        }
    
                    });
    
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
    
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mAcceptTransfer = false;
                            mWaitingForResponse = false;
    
                            
                            // Do nothing
                            dialog.dismiss();
                        }
                    });
    
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else if (msg.what == 2) //progress update
                {
                    int progressValue = msg.getData().getInt("progress");
                    String progressText = msg.getData().getString("status");
                    
                    if (mNotifyManager == null)
                    {
                        mNotifyManager =
                                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                        mBuilder = new NotificationCompat.Builder(mContext);
                    
                        mBuilder.setContentTitle("ChatSecure Transfer");
                        mBuilder.setTicker("Transfer in progress: " + progressText);
                   
                        mBuilder .setSmallIcon(R.drawable.ic_secure_xfer);                    
                        
                    }
                    
                    
                   
                    mBuilder.setContentText("Transfer in progress: " + progressText);
                    mBuilder.setProgress(100, progressValue, false);
                    
                    
                    mNotifyManager.notify(NOTIFY_DOWNLOAD_ID, mBuilder.build());
                    
                }
                else if (msg.what == 3)
                {
                    String filePath = msg.getData().getString("path");
                    String fileType = msg.getData().getString("type");
                    
                    if (mNotifyManager == null)
                    {
                        mNotifyManager =
                                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                        mBuilder = new NotificationCompat.Builder(mContext);
                        mBuilder.setContentTitle("ChatSecure Transfer");
                   
                        mBuilder .setSmallIcon(R.drawable.ic_secure_xfer);  
                        
                    }

                    String[] path = filePath.split("/"); 
                    String sanitizedPath = SystemServices.sanitize(path[path.length - 1]);
                   
                    Uri fileUri = Scanner.scan(mContext, filePath);
                    
                    if (fileType == null)
                    {
                        String fileExt = null;
                        String[] fileParts = filePath.split("\\.");
                        
                        if (fileParts.length > 0)
                        {
                        
                            fileExt = fileParts[fileParts.length-1];
                            
                            MimeTypeMap mimeTypeMap =
                                MimeTypeMap.getSingleton();

                            fileType = mimeTypeMap.getMimeTypeFromExtension(fileExt);
                        }
                    }
                    
                    Intent intentView = new Intent(Intent.ACTION_VIEW);                    
                    
                    if (fileType != null)
                    {
                       // String generalType = fileType.split("/")[0] + "/*";                        
                        intentView.setDataAndType(fileUri,fileType);                        
                    }
                    else
                        intentView.setDataAndType(fileUri,"*/*");
                    
                    PendingIntent contentIntent = 
                            PendingIntent.getActivity(mActivity, 0, intentView, 0);
                  
                    mBuilder.setContentIntent(contentIntent);
                    mBuilder.setLights(0xff00ff00, 300, 1000);
                    
                    String status = "Transfer Complete: " + sanitizedPath;
                    
                    mBuilder.setContentText(status)                    
                    // Removes the progress bar
                            .setProgress(0,0,false)
                            .setTicker(status)
                              .setWhen(System.currentTimeMillis());                              
             
                    mNotifyManager.notify(NOTIFY_DOWNLOAD_ID, mBuilder.build());
                    
                }
                
                super.handleMessage(msg);
            }
            
        };
        
        NotificationManager mNotifyManager;
        NotificationCompat.Builder mBuilder;
        int NOTIFY_DOWNLOAD_ID = 898989;
        
        
        
    }

}
