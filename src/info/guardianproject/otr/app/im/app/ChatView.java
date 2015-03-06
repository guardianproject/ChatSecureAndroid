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
import info.guardianproject.otr.app.im.IChatListener;
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IContactList;
import info.guardianproject.otr.app.im.IContactListListener;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.MessageView.DeliveryState;
import info.guardianproject.otr.app.im.app.MessageView.EncryptionState;
import info.guardianproject.otr.app.im.app.adapter.ChatListenerAdapter;
import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.engine.Presence;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.ImpsAddressUtils;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.otr.app.im.ui.RoundedAvatarDrawable;
import info.guardianproject.util.LogCleaner;
import info.guardianproject.util.SystemServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionStatus;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Browser;
import android.support.v4.view.ViewPager;
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
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonSyntaxException;
import com.google.zxing.integration.android.IntentIntegrator;

import info.guardianproject.emoji.EmojiGroup;
import info.guardianproject.emoji.EmojiManager;
import info.guardianproject.emoji.EmojiPagerAdapter;
import info.guardianproject.otr.IOtrChatSession;
import info.guardianproject.otr.app.im.IChatListener;
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IContactList;
import info.guardianproject.otr.app.im.IContactListListener;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.MessageView.DeliveryState;
import info.guardianproject.otr.app.im.app.MessageView.EncryptionState;
import info.guardianproject.otr.app.im.app.adapter.ChatListenerAdapter;
import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.ImpsAddressUtils;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.otr.app.im.ui.RoundedAvatarDrawable;
import info.guardianproject.util.LogCleaner;
import info.guardianproject.util.SystemServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionStatus;

public class ChatView extends LinearLayout {
    // This projection and index are set for the query of active chats
    static final String[] CHAT_PROJECTION = { Imps.Contacts._ID, Imps.Contacts.ACCOUNT,
                                             Imps.Contacts.PROVIDER, Imps.Contacts.USERNAME,
                                             Imps.Contacts.NICKNAME, Imps.Contacts.TYPE,
                                             Imps.Presence.PRESENCE_STATUS,
                                             Imps.Chats.LAST_UNREAD_MESSAGE,
                                             Imps.Chats._ID,
                                             Imps.Contacts.SUBSCRIPTION_TYPE,
                                             Imps.Contacts.SUBSCRIPTION_STATUS,
                                             Imps.Contacts.AVATAR_DATA

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
    static final int SUBSCRIPTION_TYPE_COLUMN = 9;
    static final int SUBSCRIPTION_STATUS_COLUMN = 10;
    static final int AVATAR_COLUMN = 11;

    //static final int MIME_TYPE_COLUMN = 9;

    static final String[] INVITATION_PROJECT = { Imps.Invitation._ID, Imps.Invitation.PROVIDER,
                                                Imps.Invitation.SENDER, };
    static final int INVITATION_ID_COLUMN = 0;
    static final int INVITATION_PROVIDER_COLUMN = 1;
    static final int INVITATION_SENDER_COLUMN = 2;

    static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);
    static final StyleSpan STYLE_NORMAL = new StyleSpan(Typeface.NORMAL);

    Markup mMarkup;

    NewChatActivity mNewChatActivity;
    ImApp mApp;
    SimpleAlertHandler mHandler;
    IImConnection mConn;

    //private ImageView mStatusIcon;
   // private TextView mTitle;
    /*package*/ListView mHistory;
    EditText mComposeMessage;
    private ImageButton mSendButton;
    
    private ImageButton mButtonAttach;
    private View mViewAttach;
    
    private View mStatusWarningView;
    private TextView mWarningText;
    private ProgressBar mProgressTransfer;

    private ViewPager mEmojiPager;
   // private View mActionBox;

    private ImageView mDeliveryIcon;
    private boolean mExpectingDelivery;

    private boolean mIsSelected = false;

    private SessionStatus mLastSessionStatus = null;
    private boolean mIsStartingOtr = false;
    private boolean mIsVerified = false;

    public void setSelected (boolean isSelected)
    {
        mIsSelected = isSelected;

        if (mIsSelected)
        {
            bindChat(mLastChatId);
            setTitle();
            updateWarningView();
            mComposeMessage.requestFocus();
            userActionDetected();

            try
            {
                boolean isConnected = (mConn == null) ? false : mConn.getState() != ImConnection.SUSPENDED;

                        
                if (mLastSessionStatus == SessionStatus.PLAINTEXT && isConnected) {


                    boolean otrPolicyAuto = mNewChatActivity.getOtrPolicy() == OtrPolicy.OTRL_POLICY_ALWAYS
                            || this.mNewChatActivity.getOtrPolicy() == OtrPolicy.OPPORTUNISTIC;

                    if (mCurrentChatSession == null)
                        mCurrentChatSession = getChatSession();
                    if (mCurrentChatSession == null)
                        return;
                    IOtrChatSession otrChatSession = mCurrentChatSession.getOtrChatSession();
                    
                    if (otrChatSession != null)
                    {
                        String remoteJID = otrChatSession.getRemoteUserId();
                        
                        boolean isChatSecure = (remoteJID != null && remoteJID.contains("ChatSecure"));
                            
                        if (otrPolicyAuto && isChatSecure) //if set to auto, and is chatsecure, then start encryption
                        {
                               //automatically attempt to turn on OTR after 1 second
                                mHandler.postAtTime(new Runnable (){
                                    public void run (){  setOTRState(true);}
                                 },1000);
                        }
                    }

                }
            }
            catch (RemoteException re){}
        }


    }


    private boolean checkConnection () throws RemoteException
    {
        if (mConn == null)
        {
            mConn = mApp.createConnection(mProviderId,mAccountId);

            if (mConn != null)
                return false;

        }

        return true;


    }

    public void setOTRState(boolean otrEnabled) {


        try {

            boolean isConnected = (mConn == null) ? false : mConn.getState() != ImConnection.SUSPENDED;

            if (isConnected)
            {
                if (mCurrentChatSession == null)
                    mCurrentChatSession = getChatSession();

                if (mCurrentChatSession != null)
                {
                    IOtrChatSession otrChatSession = mCurrentChatSession.getOtrChatSession();

                    if (otrChatSession != null)
                    {

                        if (otrEnabled) {

                            otrChatSession.startChatEncryption();                  
                            mIsStartingOtr = true;
                            mProgressBarOtr.setVisibility(View.VISIBLE);
                            
                         //   Toast.makeText(getContext(),getResources().getString(R.string.starting_otr_chat), Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            otrChatSession.stopChatEncryption();
                           // Toast.makeText(getContext(),getResources().getString(R.string.stopping_otr_chat), Toast.LENGTH_LONG).show();

                        }


                    }
                }

            }


            updateWarningView();

        }
        catch (RemoteException e) {
            Log.d(ImApp.LOG_TAG, "error getting remote activity", e);
        }


    }

    private MessageAdapter mMessageAdapter;
    private boolean isServiceUp;
    private IChatSession mCurrentChatSession;

    long mLastChatId=-1;
    String mRemoteNickname;
    String mRemoteAddress;
    RoundedAvatarDrawable mRemoteAvatar = null;
    int mSubscriptionType;
    int mSubscriptionStatus;

    long mProviderId;
    long mAccountId;
    long mInvitationId;
    private Context mContext; // TODO
    private int mPresenceStatus;

    private int mViewType;

    private static final int VIEW_TYPE_CHAT = 1;
    private static final int VIEW_TYPE_INVITATION = 2;
    private static final int VIEW_TYPE_SUBSCRIPTION = 3;

    private static final long SHOW_TIME_STAMP_INTERVAL = 30 * 1000; // 15 seconds
    private static final long SHOW_DELIVERY_INTERVAL = 5 * 1000; // 5 seconds
    private static final long SHOW_MEDIA_DELIVERY_INTERVAL = 120 * 1000; // 2 minutes
    private static final long DEFAULT_QUERY_INTERVAL = 2000;
    private static final long FAST_QUERY_INTERVAL = 200;
    private static final int QUERY_TOKEN = 10;

    // Async QueryHandler
    private final class QueryHandler extends AsyncQueryHandler {

        private Cursor mLastCursor = null;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            mExpectingDelivery = false;


            if (c != null)
            {

                closeCursor ();
                
                mLastCursor = new DeltaCursor(c);

                if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                    log("onQueryComplete: cursor.count=" + mLastCursor.getCount());
                }

                if (mMessageAdapter != null)
                    mMessageAdapter.changeCursor(mLastCursor);
            }
        }

        public void closeCursor ()
        {
            if (mLastCursor != null && (!mLastCursor.isClosed()))
                mLastCursor.close();

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
                ArrayAdapter<String> a = new ArrayAdapter<String>(mNewChatActivity,
                        android.R.layout.select_dialog_item, linkUrls);
                AlertDialog.Builder b = new AlertDialog.Builder(mNewChatActivity);
                b.setTitle(R.string.select_link_title);
                b.setCancelable(true);
                b.setAdapter(a, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse(linkUrls.get(which));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
                        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
                        intent.putExtra(Browser.EXTRA_APPLICATION_ID, mNewChatActivity.getPackageName());
                        mNewChatActivity.startActivity(intent);
                    }
                });
                b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                b.show();
            }
        }
    };

    private final static int PROMPT_FOR_DATA_TRANSFER = 9999;
    private final static int SHOW_DATA_PROGRESS = 9998;
    private final static int SHOW_DATA_ERROR = 9997;


    private IChatListener mChatListener = new ChatListenerAdapter() {
        @Override
        public boolean onIncomingMessage(IChatSession ses,
                info.guardianproject.otr.app.im.engine.Message msg) {
            scheduleRequery(FAST_QUERY_INTERVAL);
            updatePresenceDisplay();
            
            return mIsSelected;
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
            updatePresenceDisplay();

        };


        @Override
        public void onIncomingFileTransfer(String transferFrom, String transferUrl) throws RemoteException {

            String[] path = transferUrl.split("/");
            String sanitizedPath = SystemServices.sanitize(path[path.length - 1]);

            android.os.Message message = android.os.Message.obtain(null, PROMPT_FOR_DATA_TRANSFER, (int) (mProviderId >> 32),
                    (int) mProviderId, -1);
            message.getData().putString("from", transferFrom);
            message.getData().putString("file", sanitizedPath);
            mHandler.sendMessage(message);


        }

        @Override
        public void onIncomingFileTransferProgress(String file, int percent)
                throws RemoteException {

            android.os.Message message = android.os.Message.obtain(null, SHOW_DATA_PROGRESS, (int) (mProviderId >> 32),
                    (int) mProviderId, -1);
            message.getData().putString("file", file);
            message.getData().putInt("progress", percent);
            
            scheduleRequery(FAST_QUERY_INTERVAL);
            
            mHandler.sendMessage(message);
            
            
        }

        @Override
        public void onIncomingFileTransferError(String file, String err) throws RemoteException {


            android.os.Message message = android.os.Message.obtain(null, SHOW_DATA_ERROR, (int) (mProviderId >> 32),
                    (int) mProviderId, -1);
            message.getData().putString("file", file);
            message.getData().putString("err", err);

            mHandler.sendMessage(message);
        }


    };

    private void showPromptForData (String transferFrom, String filePath)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(mNewChatActivity);

        builder.setTitle(mContext.getString(R.string.file_transfer));
        builder.setMessage(transferFrom + ' ' + mNewChatActivity.getString(R.string.wants_to_send_you_the_file)
        + " '" + filePath + "'. " + mNewChatActivity.getString(R.string.accept_transfer_));

        builder.setNeutralButton(R.string.button_yes_accept_all,new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                try {
                    mCurrentChatSession.setIncomingFileResponse(true, true);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                dialog.dismiss();
            }

        });

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                try {
                    mCurrentChatSession.setIncomingFileResponse(true, false);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                dialog.dismiss();
            }

        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                try {
                    mCurrentChatSession.setIncomingFileResponse(false, false);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }


                // Do nothing
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    private Runnable mUpdateChatCallback = new Runnable() {
        public void run() {
           // if (mCursor != null && mCursor.requery() && mCursor.moveToFirst()) {
                updateChat();
           // }
        }
    };

    private IContactListListener mContactListListener = new IContactListListener.Stub() {
        public void onAllContactListsLoaded() {
        }

        public void onContactChange(int type, IContactList list, Contact contact) {
            
           if (contact != null && contact.getPresence() != null)
               mPresenceStatus = contact.getPresence().getStatus();
           
        }

        public void onContactError(int errorType, ImErrorInfo error, String listName,
                Contact contact) {
        }

        public void onContactsPresenceUpdate(Contact[] contacts) {

            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("onContactsPresenceUpdate()");
            }
            
            for (Contact c : contacts) {
                if (c.getAddress().getBareAddress().equals(Address.stripResource(mRemoteAddress))) {
                    
                    if (c != null && c.getPresence() != null)
                    {
                        mPresenceStatus = c.getPresence().getStatus();
                        updatePresenceDisplay();
                    }
                        
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
        mNewChatActivity = (NewChatActivity) context;
        mApp = (ImApp)mNewChatActivity.getApplication();
        mHandler = new ChatViewHandler(mNewChatActivity);
        mContext = context;

        ThemeableActivity.setBackgroundImage(this, mNewChatActivity);
    }

    void registerForConnEvents() {
        mApp.registerForConnEvents(mHandler);
    }

    void unregisterForConnEvents() {
        mApp.unregisterForConnEvents(mHandler);
    }

    ProgressBar mProgressBarOtr;
    
    @Override
    protected void onFinishInflate() {
      //  mStatusIcon = (ImageView) findViewById(R.id.statusIcon);
     //   mDeliveryIcon = (ImageView) findViewById(R.id.deliveryIcon);
       // mTitle = (TextView) findViewById(R.id.title);
        mHistory = (ListView) findViewById(R.id.history);
        mComposeMessage = (EditText) findViewById(R.id.composeMessage);
        mSendButton = (ImageButton) findViewById(R.id.btnSend);
        mHistory.setOnItemClickListener(mOnItemClickListener);
        mButtonAttach = (ImageButton) findViewById(R.id.btnAttach);
        mViewAttach = findViewById(R.id.attachPanel);
        
        mStatusWarningView = findViewById(R.id.warning);
        mWarningText = (TextView) findViewById(R.id.warningText);

        mProgressTransfer = (ProgressBar)findViewById(R.id.progressTransfer);
       // mOtrSwitch = (CompoundButton)findViewById(R.id.otrSwitch);
        mProgressBarOtr = (ProgressBar)findViewById(R.id.progressBarOtr);
        
        mButtonAttach.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
                
                if (mViewAttach.getVisibility() == View.GONE)
                    mViewAttach.setVisibility(View.VISIBLE);
                else
                    mViewAttach.setVisibility(View.GONE);
            }
            
        });
        
        ((ImageButton) findViewById(R.id.btnAttachAudio)).setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v) {
                mNewChatActivity.startAudioPicker();
            }
            
        });
        
        ((ImageButton) findViewById(R.id.btnAttachPicture)).setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v) {
                mNewChatActivity.startImagePicker();
            }
            
        });
        
        ((ImageButton) findViewById(R.id.btnTakePicture)).setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v) {
                mNewChatActivity.startPhotoTaker();
            }
            
        });
        
        ((ImageButton) findViewById(R.id.btnAttachFile)).setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v) {
                mNewChatActivity.startFilePicker();
            }
            
        });
        
        
        mHistory.setOnItemLongClickListener(new OnItemLongClickListener ()
        {

            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {


             if (arg1 instanceof MessageView)
             {

                 String textToCopy = ((MessageView)arg1).getLastMessage();

                 int sdk = android.os.Build.VERSION.SDK_INT;
                 if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                     android.text.ClipboardManager clipboard = (android.text.ClipboardManager) mNewChatActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                     clipboard.setText(textToCopy); //
                 } else {
                     android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mNewChatActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                     android.content.ClipData clip = android.content.ClipData.newPlainText("chat",textToCopy);
                     clipboard.setPrimaryClip(clip); //
                 }

                 Toast.makeText(mNewChatActivity, mContext.getString(R.string.toast_chat_copied_to_clipboard), Toast.LENGTH_SHORT).show();

                 return true;

             }

                return false;
            }

        });

        mWarningText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showVerifyDialog();
            }
        });

        //mOtrSwitch.setOnCheckedChangeListener(mOtrListener);

        mComposeMessage.setOnKeyListener(new OnKeyListener() {
            @Override
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

        Button btnApproveSubscription = (Button)findViewById(R.id.btnApproveSubscription);
        btnApproveSubscription.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v) {

                mNewChatActivity.approveSubscription(mProviderId, mRemoteAddress);
                
                mHandler.postDelayed(new Runnable () { public void run () {bindChat(mLastChatId); } }, 2000);
                

            }

        });

        Button btnDeclineSubscription = (Button)findViewById(R.id.btnDeclineSubscription);
        btnDeclineSubscription.setOnClickListener(new OnClickListener()
        {

            @Override

            public void onClick(View v) {

                mHandler.postDelayed(new Runnable () { public void run () {
                    mNewChatActivity.declineSubscription(mProviderId, mRemoteAddress);
                    
                } }, 500);

                


            }

        });

        /*
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

                if (mLastSessionStatus != null && mLastSessionStatus == SessionStatus.ENCRYPTED)
                {
                    mNewChatActivity.startImagePicker();
                }
                else
                {
                    mHandler.showServiceErrorAlert(getContext().getString(R.string.please_enable_chat_encryption_to_share_files));
                }
            }

        });

        View btnShareFile = findViewById(R.id.btnSendFile);
        btnShareFile.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {

                if (mLastSessionStatus != null && mLastSessionStatus == SessionStatus.ENCRYPTED)
                {
                    mNewChatActivity.startFilePicker();
                }
                else
                {
                    mHandler.showServiceErrorAlert(getContext().getString(R.string.please_enable_chat_encryption_to_share_files));

                }
            }

        });
        */

        initEmoji();


        mMessageAdapter = new MessageAdapter(mNewChatActivity, null);
        mHistory.setAdapter(mMessageAdapter);
    }

    private static EmojiManager emojiManager = null;

    private synchronized void initEmoji ()
    {
        if (emojiManager == null)
        {
            emojiManager = EmojiManager.getInstance(mContext);

            try
            {

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
        ImageView btnEmoji = (ImageView)findViewById(R.id.btnEmoji);

        Collection<EmojiGroup> emojiGroups = emojiManager.getEmojiGroups();

        if (emojiGroups.size() > 0)
        {
            btnEmoji.setVisibility(View.VISIBLE);
            
            EmojiPagerAdapter emojiPagerAdapter = new EmojiPagerAdapter(mNewChatActivity, mComposeMessage, new ArrayList<EmojiGroup>(emojiGroups));

            mEmojiPager.setAdapter(emojiPagerAdapter);

            btnEmoji.setOnClickListener(new OnClickListener ()
            {

                @Override
                public void onClick(View v) {


              //     mActionBox.setVisibility(View.GONE);

                    if (mEmojiPager.getVisibility() == View.GONE)
                        mEmojiPager.setVisibility(View.VISIBLE);
                    else
                        mEmojiPager.setVisibility(View.GONE);
                }

            });
        }
        else
        {
            btnEmoji.setVisibility(View.GONE);
            
            btnEmoji.setOnClickListener(new OnClickListener ()
            {

                @Override
                public void onClick(View v) {

                    //what? prompt to install?
                }

            });
        }


    }

    public void startListening() {
        if (!isServiceUp)
            return;
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
        mIsListening = false;
    }

    public void unbind() {

        if (mQueryHandler != null)
            mQueryHandler.closeCursor();

    }


    void updateChat() {
        setViewType(VIEW_TYPE_CHAT);

//        updateSessionInfo();

        setStatusIcon();

        //n8fr8 + devrandom: commented out on 15 Oct 2013: we really do want the chat to update w/o a connection
        //so we can show message history in offline mode
        /*
        *
        if (!isServiceUp)
            return;

        IImConnection conn = mApp.getConnection(mProviderId);
        if (conn == null) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG))
                log("Connection has been signed out");

            return;
        }*/

        mHistory.invalidate();

        startQuery(getChatId());
        // This is not needed, now that there is a ChatView per fragment.  It also causes a spurious detection of user action
        // on fragments adjacent to the current one, when they get initialized.
        //mComposeMessage.setText("");

        updateWarningView();
    }

    int mContactType = -1;

    private void updateSessionInfo(Cursor c) {

        if (c != null && (!c.isClosed()))
        {
            mProviderId = c.getLong(PROVIDER_COLUMN);
            mAccountId = c.getLong(ACCOUNT_COLUMN);
            mPresenceStatus = c.getInt(PRESENCE_STATUS_COLUMN);
            mContactType = c.getInt(TYPE_COLUMN);

            mRemoteNickname = c.getString(NICKNAME_COLUMN);
            mRemoteAddress = c.getString(USERNAME_COLUMN);

            mSubscriptionType = c.getInt(SUBSCRIPTION_TYPE_COLUMN);

            mSubscriptionStatus = c.getInt(SUBSCRIPTION_STATUS_COLUMN);
            if ((mSubscriptionType == Imps.Contacts.SUBSCRIPTION_TYPE_FROM)
                && (mSubscriptionStatus == Imps.Contacts.SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING)) {
                bindSubscription(mProviderId, mRemoteAddress);
            }
        }

    }

    public void setTitle ()
    {
        if (mIsSelected)
        {
            mNewChatActivity.setTitle(mRemoteNickname,mRemoteAvatar);

        }
    }

    private void updatePresenceDisplay ()
    {        
        if (mRemoteAvatar == null)
            return;
        
        switch (mPresenceStatus) {
        case Presence.AVAILABLE:
            mRemoteAvatar.setBorderColor(getResources().getColor(R.color.holo_green_light));
            mRemoteAvatar.setAlpha(255);
            break;

        case Presence.IDLE:
            mRemoteAvatar.setBorderColor(getResources().getColor(R.color.holo_green_dark));
            mRemoteAvatar.setAlpha(255);
            break;

        case Presence.AWAY:
            mRemoteAvatar.setBorderColor(getResources().getColor(R.color.holo_orange_light));
            mRemoteAvatar.setAlpha(255);
            break;

        case Presence.DO_NOT_DISTURB:
            mRemoteAvatar.setBorderColor(getResources().getColor(R.color.holo_red_dark));
            mRemoteAvatar.setAlpha(255);
            break;

        case Presence.OFFLINE:
            mRemoteAvatar.setBorderColor(getResources().getColor(R.color.holo_grey_light));
            mRemoteAvatar.setAlpha(100);
            break;


        default:
        }
    }

    /*
    private void setTitle() {

        if (mType == Imps.Contacts.TYPE_GROUP) {
            final String[] projection = { Imps.GroupMembers.NICKNAME };
            Uri memberUri = ContentUris.withAppendedId(Imps.GroupMembers.CONTENT_URI, mChatId);
            ContentResolver cr = mNewChatActivity.getContentResolver();
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

            mNewChatActivity.setTitle(buf.toString());

        } else {


            StringBuilder buf = new StringBuilder();

            BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);

            buf.append(this.mNickName);
            buf.append(" (");
            buf.append(brandingRes.getString(PresenceUtils.getStatusStringRes(this.mPresenceStatus)));
            buf.append(")");

            mNewChatActivity.setTitle(buf.toString());

            Drawable avatar = loadAvatar(mUserName);

           // if (avatar != null)
           // mNewChatActivity.setHomeIcon(avatar);

       // }
    }*/


    private void setStatusIcon() {
        if (mContactType == Imps.Contacts.TYPE_GROUP) {
            // hide the status icon for group chat.
         //   mStatusIcon.setVisibility(GONE);
        } else {
          //  mStatusIcon.setVisibility(VISIBLE);
            BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);
            int presenceResId = PresenceUtils.getStatusIconId(mPresenceStatus);
            //mStatusIcon.setImageDrawable(brandingRes.getDrawable(presenceResId));

        }
    }

    private void deleteChat ()
    {
        Uri chatUri = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, mLastChatId);
        mNewChatActivity.getContentResolver().delete(chatUri,null,null);

    }

    public void bindChat(long chatId) {
        log("bind " + this + " " + chatId);
        mLastChatId = chatId;

        Uri contactUri = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, chatId);
        Cursor c = mNewChatActivity.getContentResolver().query(contactUri, CHAT_PROJECTION, null, null, null);

        if (c == null)
            return;

        if (!c.moveToFirst()) {
            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                log("Failed to query chat: " + chatId);
            }
            mLastChatId = -1;

            c.close();

        } else {

            updateSessionInfo(c);

            if (mRemoteAvatar == null)
            {
                try {mRemoteAvatar =DatabaseUtils.getAvatarFromCursor(c, AVATAR_COLUMN, ImApp.DEFAULT_AVATAR_WIDTH,ImApp.DEFAULT_AVATAR_HEIGHT);}
                catch (Exception e){}

                if (mRemoteAvatar == null)
                {
                    mRemoteAvatar = new RoundedAvatarDrawable(BitmapFactory.decodeResource(getResources(),
                            R.drawable.avatar_unknown));

                }

                updatePresenceDisplay();

            }


            c.close();

            mCurrentChatSession = getChatSession();

            if (mCurrentChatSession == null)
                mCurrentChatSession = createChatSession();

            if (mCurrentChatSession != null) {
                isServiceUp = true;

            }

            updateChat();
        }

    }

    public void bindInvitation(long invitationId) {
        Uri uri = ContentUris.withAppendedId(Imps.Invitation.CONTENT_URI, invitationId);
        ContentResolver cr = mNewChatActivity.getContentResolver();
        Cursor cursor = cr.query(uri, INVITATION_PROJECT, null, null, null);
        try {
            if (!cursor.moveToFirst()) {
                if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
                    log("Failed to query invitation: " + invitationId);
                }
                //  mNewChatActivity.finish();
            } else {
                setViewType(VIEW_TYPE_INVITATION);

                mInvitationId = cursor.getLong(INVITATION_ID_COLUMN);
                mProviderId = cursor.getLong(INVITATION_PROVIDER_COLUMN);
                String sender = cursor.getString(INVITATION_SENDER_COLUMN);

                TextView mInvitationText = (TextView) findViewById(R.id.txtInvitation);
                mInvitationText.setText(mContext.getString(R.string.invitation_prompt, sender));
              //  mNewChatActivity.setTitle(mContext.getString(R.string.chat_with, sender));
            }
        } finally {
            cursor.close();
        }


    }


    public void bindSubscription(long providerId, String from) {
        mProviderId = providerId;

      //  mRemoteAddressString = from;

        setViewType(VIEW_TYPE_SUBSCRIPTION);

        TextView text = (TextView) findViewById(R.id.txtSubscription);
        String displayableAddr = ImpsAddressUtils.getDisplayableAddress(from);
        text.setText(mContext.getString(R.string.subscription_prompt, displayableAddr));
    //.displayableAdd    mNewChatActivity.setTitle(mContext.getString(R.string.chat_with, displayableAddr));

        mApp.dismissChatNotification(providerId, from);
    }


    private void setViewType(int type) {
        mViewType = type;
        if (type == VIEW_TYPE_CHAT) {
            findViewById(R.id.invitationPanel).setVisibility(GONE);
            findViewById(R.id.subscription).setVisibility(GONE);
            setChatViewEnabled(true);
        } else if (type == VIEW_TYPE_INVITATION) {
            //setChatViewEnabled(false);

            findViewById(R.id.invitationPanel).setVisibility(VISIBLE);
            findViewById(R.id.btnAccept).requestFocus();
        } else if (type == VIEW_TYPE_SUBSCRIPTION) {
            //setChatViewEnabled(false);
            findViewById(R.id.subscription).setVisibility(VISIBLE);

            findViewById(R.id.btnApproveSubscription).requestFocus();
        }
    }

    private void setChatViewEnabled(boolean enabled) {
        mComposeMessage.setEnabled(enabled);
        mSendButton.setEnabled(enabled);
        if (enabled) {
            // This can steal focus from the fragment that's i n front of the user
            //mComposeMessage.requestFocus();
        } else {
            mHistory.setAdapter(null);
        }

    }

    ListView getHistoryView() {
        return mHistory;
    }

    private synchronized void startQuery(long chatId) {
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
        updateWarningView();

        // TODO: async query?
        Cursor cursor = getMessageCursor();
        if (cursor != null) {
            cursor.requery();
        }
    }

    private Cursor getMessageCursor() {
        return mMessageAdapter == null ? null : mMessageAdapter.getCursor();
    }

    public void closeChatSession(boolean doDelete) {
        if (getChatSession() != null) {
            try {

                if (doDelete)
                    setOTRState(false);

                updateWarningView();
                getChatSession().leave();

            } catch (RemoteException e) {

                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            }
        }

        if (doDelete)
            deleteChat();

    }

    public void verifyScannedFingerprint (String scannedFingerprint)
    {
        try
        {
            IOtrChatSession otrChatSession = mCurrentChatSession.getOtrChatSession();

            if (scannedFingerprint != null && scannedFingerprint.equalsIgnoreCase(otrChatSession.getRemoteFingerprint())) {
                verifyRemoteFingerprint();
            }
        }
        catch (RemoteException e)
        {
            LogCleaner.error(ImApp.LOG_TAG, "unable to perform manual key verification", e);
        }
    }

    public void showVerifyDialog() {
        if (getChatId() == -1)
            return;

        try {
            IOtrChatSession otrChatSession = mCurrentChatSession.getOtrChatSession();
            if (otrChatSession == null) {
                return;
            }

            String localFingerprint = otrChatSession.getLocalFingerprint();
            String remoteFingerprint = otrChatSession.getRemoteFingerprint();
            if (TextUtils.isEmpty(localFingerprint) || TextUtils.isEmpty(remoteFingerprint)) {
                return;
            }

            StringBuffer message = new StringBuffer();
            message.append(mContext.getString(R.string.fingerprint_for_you)).append("\n")
                    .append(prettyPrintFingerprint(localFingerprint)).append("\n\n");
            message.append(mContext.getString(R.string.fingerprint_for_))
                    .append(otrChatSession.getRemoteUserId()).append("\n")
                    .append(prettyPrintFingerprint(remoteFingerprint)).append("\n\n");

            message.append(mContext.getString(R.string.are_you_sure_you_want_to_confirm_this_key_));

            new AlertDialog.Builder(mContext)
                    .setTitle(R.string.verify_key_)
                    .setMessage(message.toString())
                    .setPositiveButton(R.string.menu_verify_fingerprint,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    verifyRemoteFingerprint();
                                }
                            })
                    .setNegativeButton(R.string.menu_verify_secret,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    initSmpUI();
                                }
                            })
                    .setNeutralButton(R.string.menu_scan, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            new IntentIntegrator(mNewChatActivity).initiateScan();

                        }
                    }).show();
        } catch (RemoteException e) {
            LogCleaner.error(ImApp.LOG_TAG, "unable to perform manual key verification", e);
        }
    }

    private void initSmpUI() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View viewSmp = inflater.inflate(R.layout.smp_question_dialog, null, false);

        if (viewSmp != null)
        {
            new AlertDialog.Builder(mContext).setTitle(mContext.getString(R.string.otr_qa_title)).setView(viewSmp)
                    .setPositiveButton(mContext.getString(R.string.otr_qa_send), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            EditText eiQuestion = (EditText) viewSmp.findViewById(R.id.editSmpQuestion);
                            EditText eiAnswer = (EditText) viewSmp.findViewById(R.id.editSmpAnswer);
                            String question = eiQuestion.getText().toString();
                            String answer = eiAnswer.getText().toString();
                            initSmp(question, answer);
                        }
                    }).setNegativeButton(mContext.getString(R.string.otr_qa_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();
        }
    }

    private void initSmp(String question, String answer) {
        try {

            if (mCurrentChatSession != null)
            {
                IOtrChatSession iOtrSession = mCurrentChatSession.getOtrChatSession();
                iOtrSession.initSmpVerification(question, answer);
            }

        } catch (RemoteException e) {
            Log.e(ImApp.LOG_TAG, "error init SMP", e);

        }
    }

    private void verifyRemoteFingerprint() {


        try {

            IOtrChatSession otrChatSession = mCurrentChatSession.getOtrChatSession();
            otrChatSession.verifyKey(otrChatSession.getRemoteUserId());


        } catch (RemoteException e) {
            Log.e(ImApp.LOG_TAG, "error init otr", e);

        }

        updateWarningView();


    }


    private static String prettyPrintFingerprint (String fingerprint)
    {
        StringBuffer spacedFingerprint = new StringBuffer();

        for (int i = 0; i + 8 <= fingerprint.length(); i+=8)
        {
            spacedFingerprint.append(fingerprint.subSequence(i,i+8));
            spacedFingerprint.append(' ');
        }

        return spacedFingerprint.toString();
    }

    public void blockContact() {
        // TODO: unify with codes in ContactListView
        DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    checkConnection();
                    mConn = mApp.getConnection(mProviderId);
                    IContactListManager manager = mConn.getContactListManager();
                    manager.blockContact(Address.stripResource(mRemoteAddress));
                  //  mNewChatActivity.finish();
                } catch (Exception e) {

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

    public long getChatId() {
        return mLastChatId;
    }

    private IChatSession createChatSession() {

        try
        {
            checkConnection ();

            if (mConn != null) {
                    IChatSessionManager sessionMgr = mConn.getChatSessionManager();
                    if (sessionMgr != null) {

                        String remoteAddress = mRemoteAddress;
                        IChatSession session = null;
                        
                        if (mContactType == Imps.Contacts.TYPE_GROUP)
                        {
                            session = sessionMgr.createMultiUserChatSession(remoteAddress,null, false);
                        }
                        else
                        {
                            remoteAddress = Address.stripResource(mRemoteAddress);
                       
                            session = sessionMgr.createChatSession(remoteAddress,false);
                        }

                        return session;

                    }
            }

        } catch (Exception e) {

            //mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "issue getting chat session",e);
        }

        return null;
    }

    private IChatSession getChatSession() {

        try {

            if ( checkConnection ()) {

                if (mConn != null)
                {
                    IChatSessionManager sessionMgr = mConn.getChatSessionManager();
                    if (sessionMgr != null) {

                            IChatSession session = sessionMgr.getChatSession(Address.stripResource(mRemoteAddress));

                            return session;

                    }
                }
            }

        } catch (Exception e) {

            //mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "error getting chat session",e);
        }

        return null;
    }

    boolean isGroupChat() {
        return this.mContactType == Imps.Contacts.TYPE_GROUP;
    }

    void sendMessage() {

        mEmojiPager.setVisibility(View.GONE);

        String msg = mComposeMessage.getText().toString();

        if (TextUtils.isEmpty(msg.trim())) {
            return;
        }

        IChatSession session = getChatSession();

        if (session == null)
            session = createChatSession();
        
        if (session != null) {
            try {
                session.sendMessage(msg);
                mComposeMessage.setText("");
                mComposeMessage.requestFocus();
                requeryCursor();
            } catch (RemoteException e) {

              //  mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            } catch (Exception e) {

              //  mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            }
        }
    }

    void registerChatListener() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("registerChatListener " + mLastChatId);
        }
        try {
            if (getChatSession() != null) {
                getChatSession().registerChatListener(mChatListener);
            }

            checkConnection();

            if (mConn != null)
            {
                IContactListManager listMgr = mConn.getContactListManager();
                listMgr.registerContactListListener(mContactListListener);
            }

        } catch (Exception e) {
            Log.w(ImApp.LOG_TAG, "<ChatView> registerChatListener fail:" + e.getMessage());
        }
    }

    void unregisterChatListener() {
        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("unregisterChatListener " + mLastChatId);
        }
        try {
            if (getChatSession() != null) {
                getChatSession().unregisterChatListener(mChatListener);
            }
            checkConnection ();

            if (mConn != null) {
                IContactListManager listMgr = mConn.getContactListManager();
                listMgr.unregisterContactListListener(mContactListListener);
            }
        } catch (Exception e) {
            Log.w(ImApp.LOG_TAG, "<ChatView> unregisterChatListener fail:" + e.getMessage());
        }
    }

    void updateWarningView() {

        int visibility = View.GONE;
        int iconVisibility = View.GONE;
        String message = null;
        boolean isConnected;


        try {
            checkConnection();
            isConnected = (mConn == null) ? false : mConn.getState() == ImConnection.LOGGED_IN;

        } catch (Exception e) {

            isConnected = false;
        }

        if (this.isGroupChat())
        {
            //anything to do here?
            /*
            visibility = View.VISIBLE;
            message = getContext().getString(R.string.this_is_a_group_chat);
            mWarningText.setTextColor(Color.WHITE);
            mStatusWarningView.setBackgroundColor(Color.LTGRAY);
            */
            
            mButtonAttach.setVisibility(View.GONE);
            
            mSendButton.setImageResource(R.drawable.ic_send_holo_light);
            
            mComposeMessage.setHint(R.string.this_is_a_group_chat);                


        }
        else if (mCurrentChatSession != null) {
            IOtrChatSession otrChatSession = null;

            try {
                otrChatSession = mCurrentChatSession.getOtrChatSession();

                //check if the chat is otr or not
                if (otrChatSession != null) {
                    try {
                        mLastSessionStatus = SessionStatus.values()[otrChatSession.getChatStatus()];
                    } catch (RemoteException e) {
                        Log.w("Gibber", "Unable to call remote OtrChatSession from ChatView", e);
                    }
                }


            } catch (RemoteException e) {
                LogCleaner.error(ImApp.LOG_TAG, "error getting OTR session in ChatView", e);
            }

            if (mContactType == Imps.Contacts.TYPE_GROUP) {
                message = "";
            }
            else if ((mSubscriptionType == Imps.Contacts.SUBSCRIPTION_TYPE_FROM)) {
                bindSubscription(mProviderId, mRemoteAddress);            
                visibility = View.VISIBLE;                
                //message = mContext.getString(R.string.contact_not_in_list_warning, mRemoteNickname);
                //mWarningText.setTextColor(Color.WHITE);
                //mStatusWarningView.setBackgroundColor(Color.DKGRAY);

            } else {

                visibility = View.GONE;

            }


            if (mLastSessionStatus == SessionStatus.PLAINTEXT) {

                mSendButton.setImageResource(R.drawable.ic_send_holo_light);
                mComposeMessage.setHint(R.string.compose_hint);


            }
            else if (mLastSessionStatus == SessionStatus.ENCRYPTED) {

                if (mIsStartingOtr)
                {
                    mIsStartingOtr = false; //it's started!
                    mProgressBarOtr.setVisibility(View.GONE);
                }
                
                mComposeMessage.setHint(R.string.compose_hint_secure);                
                mSendButton.setImageResource(R.drawable.ic_send_secure);

                try
                {
                    String rFingerprint = otrChatSession.getRemoteFingerprint();
                    mIsVerified = otrChatSession.isKeyVerified(mRemoteAddress);

                }
                catch (RemoteException re){}


            } else if (mLastSessionStatus == SessionStatus.FINISHED) {

                mSendButton.setImageResource(R.drawable.ic_send_holo_light);
                mComposeMessage.setHint(R.string.compose_hint);

                mWarningText.setTextColor(Color.WHITE);
                mStatusWarningView.setBackgroundColor(Color.DKGRAY);
                message = mContext.getString(R.string.otr_session_status_finished);

                visibility = View.VISIBLE;
            }

        }

        if (!isConnected)
        {
          //  visibility = View.VISIBLE;
         //   iconVisibility = View.VISIBLE;
           // mWarningText.setTextColor(Color.WHITE);
           // mStatusWarningView.setBackgroundColor(Color.DKGRAY);
           // message = mContext.getString(R.string.disconnected_warning);
              mComposeMessage.setHint(R.string.error_suspended_connection);                

        }

        mStatusWarningView.setVisibility(visibility);

        if (visibility == View.VISIBLE) {
            if (message != null && message.length() > 0)
            {
                mWarningText.setText(message);            
                mWarningText.setVisibility(View.VISIBLE);
            }
            else
            {
                mWarningText.setVisibility(View.GONE);
            }
        }

        mNewChatActivity.updateEncryptionMenuState();

    }

    public SessionStatus getOtrSessionStatus ()
    {
        return mLastSessionStatus;
    }

    public boolean isOtrSessionVerified ()
    {
        return mIsVerified;
    }

    public int getRemotePresence ()
    {
        return mPresenceStatus;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        userActionDetected();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        try {
            userActionDetected();
            return super.dispatchTouchEvent(ev);
        } catch (ActivityNotFoundException e) {
            /* if the user clicked a link, e.g. geo:60.17,24.829, and there is
             * no app to handle that kind of link, catch the exception */
            Toast.makeText(getContext(), R.string.error_no_app_to_handle_url, Toast.LENGTH_SHORT)
                    .show();
            return true;
        }
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
            case PROMPT_FOR_DATA_TRANSFER:
                showPromptForData(msg.getData().getString("from"),msg.getData().getString("file"));
                break;
            case SHOW_DATA_ERROR:

                String fileName = msg.getData().getString("file");
                String error = msg.getData().getString("err");

                Toast.makeText(mContext, "Error transferring file: " + error, Toast.LENGTH_LONG).show();
                mProgressTransfer.setVisibility(View.GONE);
                break;
            case SHOW_DATA_PROGRESS:

                int percent = msg.getData().getInt("progress");
                
                mProgressTransfer.setVisibility(View.VISIBLE);
                mProgressTransfer.setProgress(percent);
                mProgressTransfer.setMax(100);

                if (percent > 95)
                {
                    mProgressTransfer.setVisibility(View.GONE);
                    requeryCursor();
                    mMessageAdapter.notifyDataSetChanged();
                    
                }

                break;
             default:
                 updateWarningView();
            }

            super.handleMessage(msg);
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

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
		public int getType(int arg0) {
            return mInnerCursor.getType(arg0);
        }

        @TargetApi(19)
		@Override
        public Uri getNotificationUri() {
            return mInnerCursor.getNotificationUri();
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
        private int mMimeTypeColumn;
        private int mIdColumn;


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
            mMimeTypeColumn = c.getColumnIndexOrThrow(Imps.Messages.MIME_TYPE);
            mIdColumn = c.getColumnIndexOrThrow(Imps.Messages._ID);
        }

        @Override
        public void changeCursor(Cursor cursor) {

            super.changeCursor(cursor);
            if (cursor != null) {
                resolveColumnIndex(cursor);
            }
        }

        @Override
        public int getItemViewType(int position) {

            Cursor c = getCursor();
            c.moveToPosition(position);
            int type = c.getInt(mTypeColumn);
            boolean isLeft = (type == Imps.MessageType.INCOMING_ENCRYPTED)||(type == Imps.MessageType.INCOMING)||(type == Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED);

            if (isLeft)
                return 0;
            else
                return 1;

        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        void setLinkifyForMessageView(MessageView messageView) {
            try {
                
                if (messageView == null)
                    return;
                
                ContentResolver cr = getContext().getContentResolver();
                Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI,
                        new String[] { Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE },
                        Imps.ProviderSettings.PROVIDER + "=?", new String[] { Long
                                .toString(Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS) },
                        null);
                Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                        pCursor, cr, Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS,
                        false /* keep updated */, null /* no handler */);
                
                if (settings != null)
                {
                    if (mConn !=null)
                        messageView.setLinkify(!mConn.isUsingTor() || settings.getLinkifyOnTor());
                    
                    settings.close();
                }
                
                if (pCursor != null)
                    pCursor.close();
                
            } catch (RemoteException e) {
                e.printStackTrace();
                messageView.setLinkify(false);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {

            View result;

            int type = getItemViewType(cursor.getPosition());

            if (type == 0)
                result     = mInflater.inflate(R.layout.message_view_left, null);
            else
                result     = mInflater.inflate(R.layout.message_view_right, null);

            return result;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            MessageView messageView = (MessageView) view;

            setLinkifyForMessageView(messageView);

            if (mApp.isThemeDark())
            {
                messageView.setMessageBackground(getResources().getDrawable(R.drawable.message_view_rounded_dark));
            }
            else
            {
                messageView.setMessageBackground(getResources().getDrawable(R.drawable.message_view_rounded_light));

            }

            int messageType = cursor.getInt(mTypeColumn);

            String nickname = isGroupChat() ? cursor.getString(mNicknameColumn) : mRemoteNickname;
            String mimeType = cursor.getString(mMimeTypeColumn);
            int id = cursor.getInt(mIdColumn);
            String body = cursor.getString(mBodyColumn);
            long delta = cursor.getLong(mDeltaColumn);
            boolean showTimeStamp = true;//(delta > SHOW_TIME_STAMP_INTERVAL);
            long timestamp = cursor.getLong(mDateColumn);

            Date date = showTimeStamp ? new Date(timestamp) : null;
            boolean isDelivered = cursor.getLong(mDeliveredColumn) > 0;
            long showDeliveryInterval = (mimeType == null) ? SHOW_DELIVERY_INTERVAL : SHOW_MEDIA_DELIVERY_INTERVAL;
            boolean showDelivery = ((System.currentTimeMillis() - timestamp) > showDeliveryInterval);

            DeliveryState deliveryState = DeliveryState.NEUTRAL;

            if (showDelivery && !isDelivered && mExpectingDelivery) {
                deliveryState = DeliveryState.UNDELIVERED;
            }
            else if (isDelivered)
            {
                deliveryState = DeliveryState.DELIVERED;
            }

            EncryptionState encState = EncryptionState.NONE;
            if (messageType == Imps.MessageType.INCOMING_ENCRYPTED)
            {
                messageType = Imps.MessageType.INCOMING;
                encState = EncryptionState.ENCRYPTED;
            }
            else if (messageType == Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED)
            {
                messageType = Imps.MessageType.INCOMING;
                 encState = EncryptionState.ENCRYPTED_AND_VERIFIED;
            }
            else if (messageType == Imps.MessageType.OUTGOING_ENCRYPTED)
            {
                messageType = Imps.MessageType.OUTGOING;
                encState = EncryptionState.ENCRYPTED;
            }
            else if (messageType == Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED)
            {
                messageType = Imps.MessageType.OUTGOING;
                 encState = EncryptionState.ENCRYPTED_AND_VERIFIED;
            }

            switch (messageType) {
            case Imps.MessageType.INCOMING:
                messageView.bindIncomingMessage(id, messageType, mRemoteAddress, nickname, mimeType, body, date, mMarkup, isScrolling(), encState, isGroupChat(), mPresenceStatus);

                break;

            case Imps.MessageType.OUTGOING:
            case Imps.MessageType.POSTPONED:

                int errCode = cursor.getInt(mErrCodeColumn);
                if (errCode != 0) {
                    messageView.bindErrorMessage(errCode);
                } else {
                    messageView.bindOutgoingMessage(id, messageType, null, mimeType, body, date, mMarkup, isScrolling(),
                            deliveryState, encState);
                }

                break;

            default:
                messageView.bindPresenceMessage(mRemoteAddress, messageType, isGroupChat(), isScrolling());
            }

           // updateWarningView();

            if (!mExpectingDelivery && isDelivered) {
                log("Setting delivery icon");
                mExpectingDelivery = true;
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

    public void onServiceConnected() {
        if (!isServiceUp) {
            bindChat(mLastChatId);
            startListening();
        }

    }

}
