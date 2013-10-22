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

import info.guardianproject.otr.OtrDataHandler;
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ContactListFilterView.ContactListListener;
import info.guardianproject.otr.app.im.app.adapter.ChatListenerAdapter;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.Imps.ProviderSettings.QueryMap;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.util.LogCleaner;
import info.guardianproject.util.SystemServices;
import info.guardianproject.util.SystemServices.FileInfo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;


public class NewChatActivity extends FragmentActivity implements View.OnCreateContextMenuListener {

    private static final String ICICLE_CHAT_PAGER_ADAPTER = "chatPagerAdapter";
    private static final String ICICLE_POSITION = "position";
    private static final int MENU_RESEND = Menu.FIRST;
    private static final int REQUEST_PICK_CONTACTS = RESULT_FIRST_USER + 1;
    private static final int REQUEST_SEND_IMAGE = REQUEST_PICK_CONTACTS + 1;
    private static final int REQUEST_SEND_FILE = REQUEST_SEND_IMAGE + 1;

    private ImApp mApp;
    private ViewPager mChatPager;
    private ChatViewPagerAdapter mChatPagerAdapter;
    
    private Cursor mCursorChats;
    
    private SimpleAlertHandler mHandler;
    
    private long mAccountId = -1;
    private long mLastProviderId = -1;
    
    private MessageContextMenuHandler mMessageContextMenuHandler;
    
    private ContactListFragment mContactList = null;
    private static final String TAG = "GB.NewChatActivity";

    final static class MyHandler extends SimpleAlertHandler {
        public MyHandler(NewChatActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ImApp.EVENT_SERVICE_CONNECTED) {
                ((NewChatActivity)mActivity).onServiceConnected();
                return;
            }
            super.handleMessage(msg);
        }
    }
    

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mApp = (ImApp)getApplication();
        mApp.maybeInit(this);
    
        requestWindowFeature(Window.FEATURE_NO_TITLE);        
        setContentView(R.layout.chat_pager);
        
        ThemeableActivity.setBackgroundImage(this);

        mHandler = new MyHandler(this);

        mChatPager = (ViewPager) findViewById(R.id.chatpager);
        mChatPager.setSaveEnabled(false);
        mChatPager.setOnPageChangeListener(new OnPageChangeListener () {
            
            private int lastPos = -1;
            
            @Override
            public void onPageScrollStateChanged(int arg0) {
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageSelected(int pos) {
                if (pos > 0) {
                    
                    if (lastPos != -1)
                    {
                        ChatViewFragment frag = (ChatViewFragment)mChatPagerAdapter.getItemAt(pos);
                        // Fragment isn't guaranteed to be initialized yet
                        if (frag != null)
                            frag.onDeselected(mApp);
                    }
                    
                    ChatViewFragment frag = (ChatViewFragment)mChatPagerAdapter.getItemAt(pos);
                    // Fragment isn't guaranteed to be initialized yet
                    if (frag != null)
                        frag.onSelected(mApp);
                    
                    lastPos = pos;
                }
            }

            
            
        });

        mMessageContextMenuHandler = new MessageContextMenuHandler();

        initSideBar ();
        
        mChatPagerAdapter = new ChatViewPagerAdapter(getSupportFragmentManager());
        mChatPager.setAdapter(mChatPagerAdapter);
        
        if (icicle != null) { 
            if (icicle.containsKey(ICICLE_CHAT_PAGER_ADAPTER)) {
                mChatPagerAdapter.restoreState(icicle.getParcelable(ICICLE_CHAT_PAGER_ADAPTER), getClassLoader());
            }
            if (icicle.containsKey(ICICLE_POSITION)) {
                int position = icicle.getInt(ICICLE_POSITION);
                if (position < mChatPagerAdapter.getCount())
                    mChatPager.setCurrentItem(position);
            }
        }
        
        mApp.registerForBroadcastEvent(ImApp.EVENT_SERVICE_CONNECTED, mHandler);
    }
    
    /*
     * We must have been thawed and the service was not previously connected, so our ChatViews are showing nothing.
     * Refresh them.
     */
    void onServiceConnected() {
        if (mChatPagerAdapter != null) {
            int size = mChatPagerAdapter.getCount();
            for (int i = 1; i < size ; i++) {
                ChatViewFragment frag = (ChatViewFragment)mChatPagerAdapter.getItemAt(i);
                if (frag != null) {
                    frag.onServiceConnected();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        mApp.unregisterForBroadcastEvent(ImApp.EVENT_SERVICE_CONNECTED, mHandler);
        mChatPagerAdapter.onDestroy();
        mChatPagerAdapter = null;
        super.onDestroy();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ICICLE_POSITION, mChatPager.getCurrentItem());
        outState.putParcelable(ICICLE_CHAT_PAGER_ADAPTER, mChatPagerAdapter.saveState());
    }
    
    @Override
    protected void onResume() {     
        super.onResume();
        
        if (getIntent() != null)
        {
            resolveIntent(getIntent());
            setIntent(null);
        }
        
        if (menu.isMenuShowing())
            menu.toggle();
        
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (menu.isMenuShowing()) {
            menu.showContent();
            return;
        }
        int currentPos = mChatPager.getCurrentItem();
        if (currentPos > 0) {
            mChatPager.setCurrentItem(0);
            return;
        }
        super.onBackPressed();
    }

    private SlidingMenu menu = null;
    
    private void initSideBar ()
    {
        menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);        
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.shadow);
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        menu.setFadeDegree(0.35f);
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
     
        menu.setMenu(R.layout.fragment_drawer);
        
        Button btnDrawerAccount = (Button) findViewById(R.id.btnDrawerAccount);
        Button btnDrawerSettings = (Button) findViewById(R.id.btnDrawerSettings);
        Button btnDrawerPanic = (Button) findViewById(R.id.btnDrawerPanic);
        Button btnDrawerGroupChat = (Button) findViewById(R.id.btnDrawerGroupChat);
        Button btnDrawerAddContact = (Button) findViewById(R.id.btnDrawerAddContact);
        Button btnDrawerFingerprint = (Button) findViewById(R.id.btnDrawerQRCode);
         
        
        btnDrawerAccount.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
                
                Intent intent = new Intent(NewChatActivity.this, AccountListActivity.class);
                startActivity(intent);
                finish();//we should clsoe this activity when we go to AccountList, in case we sign out
            }
            
            
        });
        
        btnDrawerSettings.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
                Intent sintent = new Intent(NewChatActivity.this, SettingActivity.class);
                startActivity(sintent);
                
            }
            
        });
        
        btnDrawerPanic.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
               
                /*
                Intent intent = new Intent(NewChatActivity.this, AccountListActivity.class);
                intent.putExtra("EXIT", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                */
                
                
                Uri packageURI = Uri.parse("package:info.guardianproject.otr.app.im");

                Intent intent = new Intent(Intent.ACTION_DELETE, packageURI);
                startActivity(intent);
                  
                
            }
            
        });
        
        btnDrawerGroupChat.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
               
                showGroupChatDialog();
                        
                if (menu.isMenuShowing())
                    menu.toggle();
            }
            
        });
        

        btnDrawerAddContact.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
               
                showInviteContactDialog();
                        
                
            }
            
        });
        
        
        
        btnDrawerFingerprint.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
               
                
                displayQRCode ();
            }
            
        });
    }
    
    private void showInviteContactDialog ()
    {
        if (mLastProviderId != -1 && mAccountId != -1)
        {
            Intent i = new Intent(this, AddContactActivity.class);
            i.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mLastProviderId);
            i.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
         //   i.putExtra(ImServiceConstants.EXTRA_INTENT_LIST_NAME,
           //         mContactListView.getSelectedContactList());
            startActivity(i);
        }
    }
    
    private void displayQRCode ()
    {
        
        try
        {
            if ( getCurrentChatSession() != null
                    &&  getCurrentChatSession().getOtrChatSession() != null)
            {
                
            
                String localFingerprint = getCurrentChatSession().getOtrChatSession().getLocalFingerprint();
                
                if (localFingerprint != null)
                 {
                    IntentIntegrator.shareText(this, localFingerprint);
                    return;
                 }
                
            }
            
        }
        catch (RemoteException re)
        {}
        
        //did not work
        Toast.makeText(this, "Please start a secure conversation before scanning codes", Toast.LENGTH_LONG).show();
     }
    
    void resolveIntent(Intent intent) {
        
        if (requireOpenDashboardOnStart(intent)) {
            long providerId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, -1L);
            mAccountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID,
                    -1L);
            if (providerId == -1L || mAccountId == -1L) {
                finish();
            } else {
             //   mChatSwitcher.open();
            }
            return;
        }

        if (ImServiceConstants.ACTION_MANAGE_SUBSCRIPTION.equals(intent.getAction())) {
            
            long providerId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, -1);
            mAccountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID,
                    -1L);
            String from = intent.getStringExtra(ImServiceConstants.EXTRA_INTENT_FROM_ADDRESS);
            
            if ((providerId == -1) || (from == null)) {
                finish();
            } else {
                //chatView.bindSubscription(providerId, from);
                
                showSubscriptionDialog (providerId, from);
                
            }
        } else {
            Uri data = intent.getData();
            
            if (data != null)
            {
                if (data.getScheme() != null && data.getScheme().equals("immu"))
                {
                    String user = data.getUserInfo();
                    String host = data.getHost();
                    String path = null;
                    
                    if (data.getPathSegments().size() > 0)
                        path = data.getPathSegments().get(0);
                         
                    if (host != null && path != null)
                    {
                        List<IImConnection> listConns = ((ImApp)getApplication()).getActiveConnections();
                        
                        if (!listConns.isEmpty())
                        {
                            
                             startGroupChat(path, host, listConns.get(0));
                            
                             setResult(RESULT_OK);
                        }
                    }
                    
                    
                } else {
                    String type = getContentResolver().getType(data);
                    if (Imps.Chats.CONTENT_ITEM_TYPE.equals(type)) {
                        
                        long requestedContactId = ContentUris.parseId(data);
                                           
                        mCursorChats.moveToPosition(-1);
                        int posIdx = 1;
                        boolean foundChatView = false;

                        while (mCursorChats.moveToNext())
                        {
                            long chatId = mCursorChats.getLong(ChatView.CONTACT_ID_COLUMN);

                            if (chatId == requestedContactId)
                            {
                                mChatPager.setCurrentItem(posIdx);
                                foundChatView = true;
                                break;
                            }

                            posIdx++;
                        }

                        if (!foundChatView)
                        {

                            Uri.Builder builder = Imps.Contacts.CONTENT_URI.buildUpon();
                            ContentUris.appendId(builder, requestedContactId);
                            Cursor cursor = getContentResolver().query(builder.build(), ChatView.CHAT_PROJECTION, null, null, null);
                            
                            try {
                                if (cursor.getCount() > 0)
                                { 
                                    cursor.moveToFirst();                            
                                    startChat(cursor);
                                }
                            } finally {
                                cursor.close();
                            }
                        }
                        
                   
                    } else if (Imps.Invitation.CONTENT_ITEM_TYPE.equals(type)) {
                        //chatView.bindInvitation(ContentUris.parseId(data));
                        
                        

                        
                    }
                }
            }
            else
            {
                mAccountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID,-1L);
                
                if (mContactList != null)
                {
                    mChatPager.setCurrentItem(0);
                    mContactList.refreshSpinners();
                    mContactList.initAccount(this, mAccountId);
                }

               
            }
        }
        
        if (mContactList != null)
        {
            mContactList.setSpinnerState(this);
        }
        
    }
    
    public void showChat (long requestedChatId)
    {
        mCursorChats.moveToPosition(-1);
        int posIdx = 1;
        
        while (mCursorChats.moveToNext())
        {
            long chatId = mCursorChats.getLong(ChatView.CONTACT_ID_COLUMN);
            
            if (chatId == requestedChatId)
            {
                mChatPager.setCurrentItem(posIdx);
                break;
            }
            
            posIdx++;
        }
    }

    public void refreshChatViews ()
    {
        mChatPagerAdapter.notifyDataSetChanged();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_screen_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        //updateOtrMenuState();
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case R.id.menu_send_image:
            startImagePicker();
            return true;

        case R.id.menu_send_file:
            startFilePicker();
            return true;

        case R.id.menu_view_profile:
            if (getCurrentChatView() != null)
                getCurrentChatView().viewProfile();
            return true;

        case R.id.menu_end_conversation:
            if (getCurrentChatView() != null)
                getCurrentChatView().closeChatSession();
            return true;
         
      

        case android.R.id.home:
            finish();// close this view and return to account list
            return true;
            
        case R.id.menu_view_accounts:
            startActivity(new Intent(getBaseContext(), ChooseAccountActivity.class));
          //  finish();
            return true;
            
      
        }

        return super.onOptionsItemSelected(item);
    }
    

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        /*
         * this behavior doesn't make sense to me. i hit the back key
         * and the chat disappears, as opposed to just going back to the message
         * list. i think the user should have it to use the 'end chat' button to really clear a chat
         * n8fr8 2012/09/26
         *
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
            && event.getAction() == KeyEvent.ACTION_DOWN) {
            mChatView.closeChatSessionIfInactive();
        }*/
        
        return super.dispatchKeyEvent(event);
    }

    /** Check whether we are asked to open Dashboard on startup. */
    private boolean requireOpenDashboardOnStart(Intent intent) {
        return intent.getBooleanExtra(ImServiceConstants.EXTRA_INTENT_SHOW_MULTIPLE, false);
    }

    private void sendCallInvite ()
    {
        
       // getChatView().sendMessage("&#9742; Click to start call <a href=\"https://foo.com\">sip:" + this.mSipAccount + "</a>");
        
    }
    
   

    /*
    public void updateOtrMenuState() {
        
        ChatView chatView = getCurrentChatView ();

        if (menuOtr == null || chatView == null)
            return;

        IOtrChatSession otrChatSession =  chatView.getOtrChatSession();

        if (otrChatSession != null) {
            try {
                SessionStatus sessionStatus = SessionStatus.values()[otrChatSession.getChatStatus()];

                if (sessionStatus != SessionStatus.PLAINTEXT) {
                    menuOtr.setTitle(R.string.menu_otr_stop);
                    menuOtr.setIcon(this.getResources().getDrawable(R.drawable.ic_menu_encrypt));
                    
                } else {
                    menuOtr.setTitle(R.string.menu_otr_start);
                    menuOtr.setIcon(this.getResources().getDrawable(R.drawable.ic_menu_unencrypt));

                }

            } catch (RemoteException e) {
                Log.d("NewChat", "Error accessing remote service", e);
            }
        } else {
            menuOtr.setTitle(R.string.menu_otr_start);

        }
    }*/


    /*
    private void switchChat(int delta) {
        
        ChatView chatView = getCurrentChatView ();
        long providerId =  chatView.getProviderId();
        long accountId =  chatView.getAccountId();
        String contact =  chatView.getUserName();

        mChatSwitcher.rotateChat(delta, contact, accountId, providerId);
    }*/

    /*
    private void startContactPicker() {
        Uri.Builder builder = Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY.buildUpon();
        ContentUris.appendId(builder,  getChatView().getProviderId());
        ContentUris.appendId(builder,  getChatView().getAccountId());
        Uri data = builder.build();

        try {
            Intent i = new Intent(Intent.ACTION_PICK, data);
            i.putExtra(ContactsPickerActivity.EXTRA_EXCLUDED_CONTACTS,  getChatView()
                    .getCurrentChatSession().getParticipants());
            startActivityForResult(i, REQUEST_PICK_CONTACTS);
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }
    }*/
    
    void startImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SEND_IMAGE);
    }
    
    void startFilePicker() {
        Intent selectFile = new Intent(Intent.ACTION_GET_CONTENT);
        selectFile.setType("file/*");
        startActivityForResult(Intent.createChooser(selectFile, "Select File"), REQUEST_SEND_FILE);
    }
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_SEND_IMAGE || requestCode == REQUEST_SEND_FILE) {
                Uri uri = resultIntent.getData() ;
                if( uri == null ) {
                    return ;
                }
                handleSend(uri);
            }
/*            if (requestCode == REQUEST_PICK_CONTACTS) {
                String username = data.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);
                try {
                    IChatSession chatSession =  getChatView().getCurrentChatSession();
                    if (chatSession.isGroupChatSession()) {
                        chatSession.inviteContact(username);
                        showInvitationHasSent(username);
                    } else {
                        chatSession.convertToGroupChat();
                        new ContactInvitor(chatSession, username).start();
                    }
                } catch (RemoteException e) {
                    mHandler.showServiceErrorAlert();
                }
            }
*/        }
    }
    
    private void testSendIntent(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        try {
            String url = OtrDataHandler.URI_PREFIX_OTR_IN_BAND + URLEncoder.encode(uri.toString(), "UTF-8");
            intent.setData(Uri.parse(url));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        startActivity(intent);
    }

    IChatSession getCurrentChatSession() {
        int currentPos = mChatPager.getCurrentItem();
        if (currentPos == 0)
            return null;
        mCursorChats.moveToPosition(currentPos - 1);
        long providerId = mCursorChats.getLong(ChatView.PROVIDER_COLUMN);
        String username = mCursorChats.getString(ChatView.USERNAME_COLUMN);
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
    
    private IChatSessionManager getChatSessionManager(long providerId) {
        IImConnection conn = mApp.getConnection(providerId);

        if (conn != null) {
            try {
                return conn.getChatSessionManager();
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            }
        }
        return null;
    }


    private void handleSend(Uri uri) {
        
        
        try {
            FileInfo info = SystemServices.getFileInfoFromURI(this, uri);
            
            if (info != null)
            {
                IChatSession session = getCurrentChatSession();
           
                if (session != null)
                    session.offerData( info.path, info.type );
            }
            else
            {
                Toast.makeText(this, R.string.sorry_we_cannot_share_that_file_type, Toast.LENGTH_LONG).show();
            }
        } catch (RemoteException e) {
           Log.e(ImApp.LOG_TAG,"error sending file",e);
        }
    }

    void showInvitationHasSent(String contact) {
        Toast.makeText(NewChatActivity.this, getString(R.string.invitation_sent_prompt, contact),
                Toast.LENGTH_SHORT).show();
    }

    private class ContactInvitor extends ChatListenerAdapter {
        private final IChatSession mChatSession;
        String mContact;

        public ContactInvitor(IChatSession session, String data) {
            mChatSession = session;
            mContact = data;
        }

        @Override
        public void onConvertedToGroupChat(IChatSession ses) {
            try {
                final long chatId = mChatSession.getId();
                mChatSession.inviteContact(mContact);
                mHandler.post(new Runnable() {
                    public void run() {
                        
                        ChatView chatView = getCurrentChatView ();

                        if (chatView != null)
                        {
                            chatView.bindChat(chatId);
                            showInvitationHasSent(mContact);
                        }
                    }
                });
                mChatSession.unregisterChatListener(this);
            } catch (RemoteException e) {

                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "group chat error",e); 
            }
        }

        public void start() throws RemoteException {
            mChatSession.registerChatListener(this);
        }
    }

    /** Show the context menu on a history item. */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        
        ChatView chatView = getCurrentChatView ();

        if (chatView != null)
        {
            AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            mMessageContextMenuHandler.mPosition = info.position;
            Cursor cursor =  chatView.getMessageAtPosition(info.position);
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(Imps.Messages.TYPE));
            if (type == Imps.MessageType.OUTGOING) {
                menu.add(0, MENU_RESEND, 0, R.string.menu_resend).setOnMenuItemClickListener(
                        mMessageContextMenuHandler);
            }
            
           
        }
    }

    final class MessageContextMenuHandler implements OnMenuItemClickListener {
        int mPosition;

     
        @Override
        public boolean onMenuItemClick(android.view.MenuItem item) {
            
            ChatView chatView = getCurrentChatView ();
            
            if (chatView != null)
            {
                Cursor c;
                c =  chatView.getMessageAtPosition(mPosition);
    
                switch (item.getItemId()) {
                case MENU_RESEND:
                    String text = c.getString(c.getColumnIndexOrThrow(Imps.Messages.BODY));
                    chatView.getComposedMessage().setText(text);
                    break;
                default:
                    return false;
                }            return false;
            }
            else
                return false;
        }
    }
    
    private class MyContentObserver extends ContentObserver {

        public MyContentObserver() {
            super(mHandler);
        }

        @Override
        public void onChange(boolean selfChange) {
            // Although we get this on our main thread, the dispatch is async and can happen after onDestroy
            // Ensure we are still alive
            if (mCursorChats != null)
                refreshChatViews();
            else
                Log.w(TAG, "got onChange after onDestroy");
        }
    }

    public class ChatViewPagerAdapter extends DynamicPagerAdapter {
        private MyContentObserver mCursorObserver;

        public ChatViewPagerAdapter(FragmentManager fm) {
            super(fm);
            mCursorChats = getContentResolver().query(Imps.Contacts.CONTENT_URI_CHAT_CONTACTS, ChatView.CHAT_PROJECTION, null, null, null);
            
            mCursorObserver = new MyContentObserver();
            mCursorChats.registerContentObserver(mCursorObserver);
        }
        
        public void onDestroy() {
            mCursorChats.unregisterContentObserver(mCursorObserver);
            mCursorChats.close();
            mCursorChats = null;
        }

        @Override
        public void notifyDataSetChanged() {
            // In case that onDestroy was called first
            // FIXME check if this can actually happen
            if (mCursorChats == null)
                return;
            mCursorChats.unregisterContentObserver(mCursorObserver);
            mCursorChats.close();

            mCursorChats = getContentResolver().query(Imps.Contacts.CONTENT_URI_CHAT_CONTACTS, ChatView.CHAT_PROJECTION, null, null, null);
            mCursorChats.registerContentObserver(mCursorObserver);
            
            super.notifyDataSetChanged();
        }


        @Override
        public int getCount() {
            if (mCursorChats != null)
                return mCursorChats.getCount() + 1;
            else
                return 0;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0)
            {
                return (mContactList = new ContactListFragment());
            }
            else
            {
                int positionMod = position - 1;
                
                mCursorChats.moveToPosition(positionMod);            
                long contactChatId = mCursorChats.getLong(ChatView.CONTACT_ID_COLUMN);
                String contactName = mCursorChats.getString(ChatView.USERNAME_COLUMN); 
                long providerId = mCursorChats.getLong(ChatView.PROVIDER_COLUMN); 
                
                return ChatViewFragment.newInstance(contactChatId, contactName, providerId);
            }
        }

        @Override
        public int getItemPosition(Object object) {
           
            if (object instanceof ChatViewFragment)
            {
                ChatViewFragment cvFrag = (ChatViewFragment)object;
                ChatView view = cvFrag.getChatView();
                long viewChatId = view.mLastChatId;
                int position = PagerAdapter.POSITION_NONE;
                
                // TODO: cache positions so we don't scan the cursor every time
                if (mCursorChats.getCount() > 0)
                {
                    mCursorChats.moveToFirst();
                    
                    int posIdx = 1;
                    
                    do {
                        long chatId = mCursorChats.getLong(ChatView.CHAT_ID_COLUMN);
                        
                        if (chatId == viewChatId)                        
                        {
                            position = posIdx;
                            break;
                        }
                        
                        posIdx++;
                    }
                    while (mCursorChats.moveToNext());
                    
                }
                
               //` Log.d(TAG, "position of " + cvFrag.getArguments().getString("contactName") + " = " + position);
                return position;
                
            }
            else if (object instanceof ContactListFragment)
            {
                return 0;
                
            }
            else {
                throw new RuntimeException("got asked about an unknown fragment");
            }
        }


        @Override
        public CharSequence getPageTitle(int position) {
           
            if (position == 0)
            {
                return getString(R.string.app_name);
            }
            else
            {
                int positionMod = position - 1;

                mCursorChats.moveToPosition(positionMod);
                if (!mCursorChats.isAfterLast())
                    return mCursorChats.getString(ChatView.NICKNAME_COLUMN);
                else
                    return "";//unknown title
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int pos) {
            Object item = super.instantiateItem(container, pos);
            if (pos > 0) {
                ChatViewFragment frag = (ChatViewFragment)item;
            }
            return item;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int pos, Object object) {
            super.destroyItem(container, pos, object);
        }
        
        public ChatView getChatViewAt(int pos) {
            if (pos > 0)
            {
                ChatViewFragment frag = ((ChatViewFragment)getItemAt(pos));
            
                if (frag != null)
                    return frag.getChatView();
            }
            
            throw new RuntimeException("could not get chat view at " + pos);
        }
    }
    
    
    public static class ContactListFragment extends Fragment implements ContactListListener, ProviderListItem.SignInManager
    {
        

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

        
        
        static final int PROVIDER_ID_COLUMN = 0;
        static final int PROVIDER_NAME_COLUMN = 1;
        static final int PROVIDER_FULLNAME_COLUMN = 2;
        static final int PROVIDER_CATEGORY_COLUMN = 3;
        static final int ACTIVE_ACCOUNT_ID_COLUMN = 4;
        static final int ACTIVE_ACCOUNT_USERNAME_COLUMN = 5;
        static final int ACTIVE_ACCOUNT_PW_COLUMN = 6;
        static final int ACTIVE_ACCOUNT_LOCKED = 7;
        static final int ACTIVE_ACCOUNT_KEEP_SIGNED_IN = 8;
        static final int ACCOUNT_PRESENCE_STATUS = 9;
        static final int ACCOUNT_CONNECTION_STATUS = 10;
         
        long[] mAccountIds = null;
        ContactListFilterView mFilterView = null;
        UserPresenceView mPresenceView = null;
        
        Cursor mProviderCursor = null;
    
        Spinner mSpinnerAccounts;

        boolean showGrid = true;
        
        ImApp mApp = null;
        

        private Handler mPresenceHandler = new Handler()
        {
            
            @Override
            public void handleMessage(Message msg) {
               
                
                mPresenceView.refreshLogginInStatus();

                super.handleMessage(msg);
            } 
        };



        public ContactListFragment() {
        }

        /**
          * When creating, retrieve this instance's number from its arguments.
          */
         @Override
         public void onCreate(Bundle savedInstanceState) {
           

             super.onCreate(savedInstanceState);

             
         }

         
         


        /**
          * The Fragment's UI is just a simple text view showing its
          * instance number.
          */
         @Override
         public View onCreateView(LayoutInflater inflater, ViewGroup container,
                 Bundle savedInstanceState) {
             
             if (showGrid)
                 mFilterView = (ContactListFilterView) inflater.inflate(
                         R.layout.contact_grid_filter_view, null);
             else
              mFilterView = (ContactListFilterView) inflater.inflate(
                     R.layout.contact_list_filter_view, null);
              
              mPresenceView = (UserPresenceView) mFilterView.findViewById(R.id.userPresence);

             mFilterView.setListener(this);
             
             TextView txtEmpty = (TextView)mFilterView.findViewById(R.id.empty);
             
             txtEmpty.setOnClickListener(new OnClickListener ()
             {

                @Override
                public void onClick(View v) {
                  
                    ((NewChatActivity)getActivity()).showInviteContactDialog();
                }
                 
             });
             
             ((AbsListView)mFilterView.findViewById(R.id.filteredList)).setEmptyView(txtEmpty);
             
             
             
             
            //  QueryMap mGlobalSettingMap = new Imps.ProviderSettings.QueryMap(getContext().getContentResolver(), true, mHandler);
             
         //    Uri uri = mGlobalSettingMap.getHideOfflineContacts() ? Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY
                                                                 // : Imps.Contacts.CONTENT_URI_CONTACTS_BY;
           //   uri = ContentUris.withAppendedId(uri, providerId);
            //  uri = ContentUris.withAppendedId(uri, accountId);
         //     mFilterView.doFilter( Imps.Contacts.CONTENT_URI_CONTACTS_BY, null);
              
              setupSpinners(mFilterView);
              
              setSpinnerState(getActivity());
              
              return mFilterView;
           
         }

         
        public void refreshSpinners ()
        {
            if (mProviderCursor != null && (!mProviderCursor.isClosed()))
                mProviderCursor.close();
            setupSpinners(mFilterView);
        }
         
         @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            
            mApp = ((ImApp)activity.getApplication()); 
            mApp.registerForConnEvents(mPresenceHandler);

        }

        @Override
        public void onDetach() {
            super.onDetach();
            
            mApp.unregisterForConnEvents(mPresenceHandler);
            mApp = null;            
            
        }

        @Override
        public void onDestroyView() {
            if (mProviderCursor != null && (!mProviderCursor.isClosed()))
                mProviderCursor.close();
            mFilterView.setConnection(null);

            super.onDestroyView();
        }

         private void setupSpinners (ContactListFilterView filterView)
         {
             
             mProviderCursor = getActivity().getContentResolver().query(Imps.Provider.CONTENT_URI_WITH_ACCOUNT, PROVIDER_PROJECTION,
                     Imps.Provider.CATEGORY + "=?" + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL",
             
                     new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                     Imps.Provider.DEFAULT_SORT_ORDER);

             if (mProviderCursor == null)
             {
                 getActivity().finish();
                 return;

                 }
           
             //        + " AND " + Imps.Provider.ACCOUNT_CONNECTION_STATUS + " != 0"
             
                     /* selection */
             mAccountIds = new long[mProviderCursor.getCount()];
             
          
             mProviderCursor.moveToFirst();

             ProviderAdapter pAdapter = new ProviderAdapter(getActivity(), mProviderCursor);
             
             mSpinnerAccounts = (Spinner)filterView.findViewById(R.id.spinnerAccounts);
             
             mSpinnerAccounts.setAdapter(pAdapter);
             mSpinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener ()
             {

                 @Override
                 public void onItemSelected(AdapterView<?> parent, View view, int itemPosition, long id) {
                    
                 //    mAccountId = mAccountIds[itemPosition];
                     //update account list
                     initAccount(getActivity(),mAccountIds[itemPosition]);
                   
                     
                 }

                 @Override
                 public void onNothingSelected(AdapterView<?> arg0) {
                     // TODO Auto-generated method stub
                     
                 }
                 
             });
             
             mProviderCursor.moveToFirst();
             int activeAccountIdColumn = mProviderCursor.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_ID);

             for (int i = 0; i < mAccountIds.length; i++)
             {
                 mAccountIds[i] = mProviderCursor.getLong(activeAccountIdColumn);              
                 mProviderCursor.moveToNext();
                 
             }
         }
         
         public void setSpinnerState (Activity activity)
         {

             NewChatActivity chatActivity = (NewChatActivity)activity;
             
             if (mAccountIds == null)
                 return;
             
             if (mAccountIds.length == 1) //only one account, hide the spinner
             {
                 mSpinnerAccounts.setVisibility(View.GONE);
                 initAccount(activity,mAccountIds[0]);
             }
             else if (chatActivity.getAccountId() != -1) //multiple accounts, so select a spinner based on user input
             {

                 mSpinnerAccounts.setVisibility(View.VISIBLE);
                 
                 int selIdx = 0;
                 
                 for (long accountId : mAccountIds)
                 {
                     if (accountId == chatActivity.getAccountId())
                     {
                         mSpinnerAccounts.setSelection(selIdx);   
                         break;
                     }
                     
                     selIdx++;
                 }
                 
             }
             else if (getActivity() != null) //nothing from the user, show show an active account
             {
                 List<IImConnection> listConns = ((ImApp)getActivity().getApplication()).getActiveConnections();
                 
                 for (IImConnection conn : listConns)
                 {
                     try
                     {
                         long activeAccountId = conn.getAccountId();
                         int spinnerIdx = -1;
                         for (long accountId : mAccountIds )
                         {
                             spinnerIdx++;
                             
                             if (accountId == activeAccountId)
                             {
                                 mSpinnerAccounts.setSelection(spinnerIdx);
                                 break;
                             }
                         }
                         
                     }
                     catch (Exception e){}
                 }
             }
             
            
         }
         
         public void initAccount (Activity activity, long accountId)
         {

             if (accountId == -1)
                 return;
             
             ContentResolver cr = activity.getContentResolver();
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

             long providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Account.PROVIDER));
             ((NewChatActivity)activity).setLastProviderId(providerId);
             // FIXME doesn't mAccountId need to be set here?
             
             initConnection (activity, accountId, providerId);
             
             c.close();
         }
         
         private void initConnection (Activity activity, long accountId, long providerId)
         {
             IImConnection conn = ((ImApp)activity.getApplication()).getConnection(providerId);
           
             if (conn == null)
             {
                 try {
                  conn =  ((ImApp)getActivity().getApplication()).createConnection(providerId, accountId);
                 } catch (RemoteException e) {
                    Log.e(ImApp.LOG_TAG,"error creating connection",e);
                 }

             }
             
             if (conn != null)
             {
                 mFilterView.setConnection(conn);
                 mPresenceView.setConnection(conn);

                 try {
                     mPresenceView.loggingIn(conn.getState() == ImConnection.LOGGING_IN);
                 } catch (RemoteException e) {        
                     mPresenceView.loggingIn(false);
                 //    mHandler.showServiceErrorAlert();
                 }

                 QueryMap settingMap = new Imps.ProviderSettings.QueryMap(activity.getContentResolver(), false, null);
                 
                 Uri baseUri = settingMap.getHideOfflineContacts() ? Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY
                                                                     : Imps.Contacts.CONTENT_URI_CONTACTS_BY;
                                                                
                 settingMap.close();
                 
                 Uri.Builder builder = baseUri.buildUpon();
                 ContentUris.appendId(builder, providerId);
                 ContentUris.appendId(builder, accountId);
                 mFilterView.doFilter(builder.build(), null);
             }        
             
         }

        @Override
        public void startChat(Cursor c) {
            
            NewChatActivity activity = (NewChatActivity)getActivity();
            
            activity.startChat(c);
            
        }
        
        public void showProfile (Cursor c)
        {
            if (c != null) {
                long chatContactId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts._ID));
           
                long providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts.PROVIDER));
                long accountId = c.getLong(c.getColumnIndex(Imps.Contacts.ACCOUNT));
                
                Uri data = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, chatContactId);

                Intent intent = new Intent(Intent.ACTION_VIEW, data);
                intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, providerId);
                intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, accountId);

                startActivity(intent);
             
            }
        }
        
        public class ProviderAdapter extends CursorAdapter {
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
                view.init(cursor, true);
                return view;
            }
            
            

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                ((ProviderListItem) view).bindView(cursor);
            }
            
            
            
        }
        
        public class ProviderListItemFactory implements LayoutInflater.Factory {
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                if (name != null && name.equals(ProviderListItem.class.getName())) {
                    return new ProviderListItem(context, getActivity(), ContactListFragment.this);
                }
                return null;
            }
            
            
        }

        @Override
        public void signIn(long accountId) {
           throw new UnsupportedOperationException("not implemented");
            
        }

        @Override
        public void signOut(long accountId) {

            throw new UnsupportedOperationException("not implemented");
        }
        
        
    
    }
    
   
    
    
    private void startChat(Cursor c) {
        if (c != null && (!  c.isAfterLast())) {
            long chatContactId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts._ID));
            String username = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            
            long providerId = mLastProviderId;//FIXME c.getLong(c.getColumnIndexOrThrow(Imps.Contacts.PROVIDER));
            IImConnection conn = mApp.getConnection(providerId);
            
            if (conn != null)
            {
                try {
                    IChatSessionManager manager = conn.getChatSessionManager();
                    IChatSession session = manager.getChatSession(username);
                    if (session == null) {
                        manager.createChatSession(username);
                    }

                    refreshChatViews(); // Refresh early so that we can jump to the chat
                    showChat(chatContactId);
                } catch (RemoteException e) {
                  //  mHandler.showServiceErrorAlert(e.getMessage());
                    LogCleaner.debug(ImApp.LOG_TAG, "remote exception starting chat");
    
                }
           
            }
            else
            {
                LogCleaner.debug(ImApp.LOG_TAG, "could not start chat as connection was null");
            }
        }
    }

    public static class ChatViewFragment extends Fragment {
        
         ChatView mChatView;
         
        /**
         * Create a new instance of CountingFragment, providing "num"
         * as an argument.
         * @param providerId 
         * @param contactName 
         */
        static ChatViewFragment newInstance(long chatContactId, String contactName, long providerId) {
            
            ChatViewFragment f = new ChatViewFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putLong("contactChatId", chatContactId);
            args.putString("contactName", contactName);
            args.putLong("providerId", providerId);
            f.setArguments(args);

//            Log.d(TAG, "CVF new " + contactName);
            return f;
        }
        
        public ChatViewFragment() {
//            Log.d(TAG, "CVF construct " + super.toString());
        }
        
        public String toString() {
            return super.toString() + " -> " + getArguments().getString("contactName"); 
        }

        public void onSelected(ImApp app) {
            app.dismissChatNotification(getArguments().getLong("providerId"), getArguments().getString("contactName"));
            if (mChatView != null)
                mChatView.setSelected(true);
        }
        
        public void onDeselected(ImApp app) {
            if (mChatView != null)
                mChatView.setSelected(false);
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
          
//            Log.d(TAG, "CVF create " + getArguments().getString("contactName"));
        }

        /**
         * The Fragment's UI is just a simple text view showing its
         * instance number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            
            long chatContactId = getArguments().getLong("contactChatId");
            mChatView = (ChatView)inflater.inflate(R.layout.chat_view, container, false);
            mChatView.bindChat(chatContactId);                       
            
            return mChatView;
        }

        public void onServiceConnected() {
            if (isResumed()) {
                mChatView.onServiceConnected();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            
            mChatView.startListening();
//            Log.d(TAG, "CVF resume " + getArguments().getString("contactName") + " " + this);
        }
        
        @Override
        public void onPause() {
            super.onPause();
            
            mChatView.stopListening();
//            Log.d(TAG, "CVF pause " + getArguments().getString("contactName") + " " + this);
        }

        @Override
        public void onDestroy() {
            mChatView.unbind();
            super.onDestroy();
//            Log.d(TAG, "CVF destroy " + getArguments().getString("contactName") + " " + this);
        }
        
        public ChatView getChatView() {
            return mChatView;
        }
    }
    
    public ChatView getCurrentChatView ()
    {
        int cItemIdx;
        
        if ((cItemIdx = mChatPager.getCurrentItem()) > 0)
        {
            return mChatPagerAdapter.getChatViewAt(cItemIdx);            
        }
        else
            return null;
    }
    
    

    
    private void showGroupChatDialog ()
    {
        ContentResolver cr = getContentResolver();

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                cr, mLastProviderId, false /* don't keep updated */, null /* no handler */);

        String chatDomain = "conference." + settings.getDomain();
        
        settings.close();
        
        
        
     // This example shows how to add a custom layout to an AlertDialog
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.alert_dialog_group_chat, null);
        final TextView tvServer = (TextView) textEntryView.findViewById(R.id.chat_server);
        
        tvServer.setText(chatDomain);
        
        new AlertDialog.Builder(this)            
            .setTitle(R.string.create_or_join_group_chat)
            .setView(textEntryView)
            .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked OK so do some stuff */
                    
                    String chatRoom = null;
                    String chatServer = null;
                    
                    TextView tv = (TextView)textEntryView.findViewById(R.id.chat_room);
                    
                    chatRoom = tv.getText().toString();
                    
                    tv = (TextView) textEntryView.findViewById(R.id.chat_server);
                    
                    chatServer = tv.getText().toString();
                    
                    startGroupChat (chatRoom, chatServer, ((ImApp)getApplication()).getConnection(mLastProviderId));
                    
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked cancel so do some stuff */
                }
            })
            .create().show();
        
        
        
    }
    
    public void startGroupChat (String room, String server, IImConnection conn)
    {
        String roomAddress = room + '@' + server;
        
        try {
            IChatSessionManager manager = conn.getChatSessionManager();
            IChatSession session = manager.getChatSession(roomAddress);
            if (session == null) {
                session = manager.createMultiUserChatSession(roomAddress);
            }

            if (session != null)
            {
                long id = session.getId();
                showChat(id);
                //Uri data = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, id);                
                //Intent i = new Intent(Intent.ACTION_VIEW, data);
                //i.addCategory(ImApp.IMPS_CATEGORY);            
                //startActivity(i);
            }
            else
            {
                mHandler.showServiceErrorAlert(getString(R.string.unable_to_create_or_join_group_chat));
                
            }
            
        } catch (RemoteException e) {
           mHandler.showServiceErrorAlert(getString(R.string.unable_to_create_or_join_group_chat));
        }
       
    }
    
    void acceptInvitation(long providerId, long invitationId) {
        try {

            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                conn.acceptInvitation(invitationId);
            }
        } catch (RemoteException e) {

            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "accept invite error",e);
        }
    }

    void declineInvitation(long providerId, long invitationId) {
        try {
            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                conn.rejectInvitation(invitationId);
            }
        } catch (RemoteException e) {

            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "decline invite error",e); 
        }
    }
    
    void showSubscriptionDialog (final long subProviderId, final String subFrom)
    {
        new AlertDialog.Builder(this)            
        .setTitle(getString(R.string.subscriptions))
        .setMessage(getString(R.string.subscription_prompt,subFrom))
        .setPositiveButton(R.string.approve_subscription, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                approveSubscription(subProviderId, subFrom);
            }
        })
        .setNegativeButton(R.string.decline_subscription, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                declineSubscription(subProviderId, subFrom);
            }
        })
        .create().show();
    }

    void approveSubscription(long providerId, String userName) {
        IImConnection conn = mApp.getConnection(providerId);

        try {
            IContactListManager manager = conn.getContactListManager();
            manager.approveSubscription(userName);
        } catch (RemoteException e) {

            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "approve sub error",e);
        }
    }

    void declineSubscription(long providerId, String userName) {
        IImConnection conn = mApp.getConnection(providerId);

        if (conn != null)
        {
            try {
                IContactListManager manager = conn.getContactListManager();
                manager.declineSubscription(userName);
            } catch (RemoteException e) {
                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "decline sub error",e);
            }
        }
    }
    

    long getAccountId() {
        return mAccountId;
    }
    
    long getLastProviderId() {
        return mLastProviderId;
    }
    
    void setAccountId(long mAccountId) {
        this.mAccountId = mAccountId;
    }
    
    void setLastProviderId(long mLastProviderId) {
        this.mLastProviderId = mLastProviderId;
    }
}
