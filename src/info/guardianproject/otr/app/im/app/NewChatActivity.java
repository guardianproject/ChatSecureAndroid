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

import info.guardianproject.otr.IOtrChatSession;
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ContactListFilterView.ContactListListener;
import info.guardianproject.otr.app.im.app.adapter.ChatListenerAdapter;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import net.java.otr4j.session.SessionStatus;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
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
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;


public class NewChatActivity extends FragmentActivity implements View.OnCreateContextMenuListener {

    private static final int MENU_RESEND = Menu.FIRST;
    private static final int REQUEST_PICK_CONTACTS = RESULT_FIRST_USER + 1;

    private ImApp mApp;
    private static ViewPager mChatPager;
    private  static ChatViewPagerAdapter mChatPagerAdapter;

    private Cursor mCursorChats;
    
    private SimpleAlertHandler mHandler;
    private MenuItem menuOtr, menuCall;
    
    private LayoutInflater mInflater;

    private static long mAccountId = -1;
    private static long mLastProviderId = -1;
    
    private String mSipAccount = null;
    
    private MessageContextMenuHandler mMessageContextMenuHandler;
    
    private ContactListFragment mContactList = null;

    
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);        
        setContentView(R.layout.chat_pager);


        mChatPager = (ViewPager) findViewById(R.id.chatpager);
        
        mApp = (ImApp)getApplication();
        mInflater = LayoutInflater.from(this);
    
        mMessageContextMenuHandler = new MessageContextMenuHandler();

       
        initSideBar ();
        
        mChatPagerAdapter = new ChatViewPagerAdapter(getSupportFragmentManager());
        mChatPager.setAdapter(mChatPagerAdapter);
        resolveIntent(getIntent());
       
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
         
        
        btnDrawerAccount.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
                
                Intent intent = new Intent(NewChatActivity.this, AccountListActivity.class);
                startActivity(intent);
                
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
               
                Intent intent = new Intent(NewChatActivity.this, AccountListActivity.class);
                intent.putExtra("EXIT", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                
                /*
                Uri packageURI = Uri.parse("package:info.guardianproject.otr.app.im");

                intent = new Intent(Intent.ACTION_DELETE, packageURI);
                startActivity(intent);
                */
                        
                
            }
            
        });
        
        btnDrawerGroupChat.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
               
                showGroupChatDialog();
                        
                
            }
            
        });
        

        btnDrawerAddContact.setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {
               
                showInviteContactDialog();
                        
                
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


    @Override
    protected void onNewIntent(Intent intent) {
        resolveIntent(intent);
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
                String type = getContentResolver().getType(data);
                if (Imps.Chats.CONTENT_ITEM_TYPE.equals(type)) {
                    
                    long requestedChatId = ContentUris.parseId(data);
                                        
                    mCursorChats.moveToPosition(0);
                    int posIdx = 0;
                    while (mCursorChats.moveToNext())
                    {
                        long chatId = mCursorChats.getLong(ChatView.CHAT_ID_COLUMN);
                        
                        if (chatId == requestedChatId)
                        {
                            mChatPager.setCurrentItem(posIdx+2);
                            break;
                        }
                        
                        posIdx++;
                    }
                    
               
                } else if (Imps.Invitation.CONTENT_ITEM_TYPE.equals(type)) {
                    //chatView.bindInvitation(ContentUris.parseId(data));
                }
            }
            else
            {
                mAccountId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID,-1L);
                
                if (mContactList != null)
                    mContactList.initAccount(mAccountId);
            }
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
        
        mChatPagerAdapter = new ChatViewPagerAdapter(getSupportFragmentManager());
        mChatPager.setAdapter(mChatPagerAdapter);
     
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_screen_menu, menu);

        menuOtr = menu.findItem(R.id.menu_view_otr);
        menuCall = menu.findItem(R.id.menu_secure_call);
        
        
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

        case R.id.menu_secure_call:
            sendCallInvite ();
            return true;

        case R.id.menu_view_profile:
            //getChatView().viewProfile();
            return true;

        case R.id.menu_end_conversation:
            //getChatView().closeChatSession();
            return true;
         
      

        case android.R.id.home:
            showChatList();
            return true;
            
        case R.id.menu_view_accounts:
            startActivity(new Intent(getBaseContext(), ChooseAccountActivity.class));
          //  finish();
            return true;
            
      
        }

        return super.onOptionsItemSelected(item);
    }
    
    private void showChatList ()
    {
     //   Intent intent = new Intent (this, ChatListActivity.class);
      //  intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
       // startActivity(intent);
         finish();
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
    
    public void switchOtrState(ChatView chatView) {

        
        IOtrChatSession otrChatSession =  chatView.getOtrChatSession();
        int toastMsgId;
        
        if (SessionStatus.values() != null && otrChatSession != null)
        {
            try {
                SessionStatus sessionStatus = SessionStatus.values()[otrChatSession.getChatStatus()];
                if (sessionStatus == SessionStatus.PLAINTEXT) {
                    otrChatSession.startChatEncryption();
                    toastMsgId = R.string.starting_otr_chat;
    
                } else {
                    otrChatSession.stopChatEncryption();
                    toastMsgId = R.string.stopping_otr_chat;
                    chatView.updateWarningView();
                }
               // updateOtrMenuState();
                
                Toast.makeText(this, getString(toastMsgId), Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                Log.d("Gibber", "error getting remote activity", e);
            }
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      
        /*
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_CONTACTS) {
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
        }*/
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
                mHandler.showServiceErrorAlert();
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
    
    

    public class ChatViewPagerAdapter extends FragmentStatePagerAdapter {
        
        
        public ChatViewPagerAdapter(FragmentManager fm) {
            super(fm);
            
           // if (mCursorChats != null && (!mCursorChats.isClosed()))
            //    mCursorChats.close();
            
            if (mCursorChats == null)
                mCursorChats = getContentResolver().query(Imps.Contacts.CONTENT_URI_CHAT_CONTACTS, ChatView.CHAT_PROJECTION, null, null, null);
            else
                mCursorChats.requery();
        }
        
        
        
        @Override
        public void notifyDataSetChanged() {

            
            mCursorChats.requery();
            
            super.notifyDataSetChanged();

            for (int i = 1; i < getCount(); i++)
            {
                ChatViewFragment frag = ((ChatViewFragment)getItem(i));
                View fragView = frag.getView();
                
                if (frag != null && fragView != null && fragView instanceof ChatView)
                {
                    ((ChatView)fragView).rebind();
                    fragView.invalidate();
                }
            }
            
            
        }


        @Override
        public int getCount() {
            if (mCursorChats != null && (!mCursorChats.isClosed()))
                return mCursorChats.getCount() + 1;
            else
                return 1;
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
                
                long contactChatId = -1;
                
                mCursorChats.moveToPosition(positionMod);            
                contactChatId = mCursorChats.getLong(ChatView.CONTACT_ID_COLUMN);
                
                return ChatViewFragment.newInstance(contactChatId);
            }
        }

        @Override
        public int getItemPosition(Object object) {
           
            if (object instanceof ChatViewFragment)
            {
                ChatViewFragment cvFrag = (ChatViewFragment)object;
                int position = -1;
                
                mCursorChats.moveToFirst();
                
                int posIdx = 1;
                
                do {
                    long chatId = mCursorChats.getLong(ChatView.CHAT_ID_COLUMN);
                    
                    View view = cvFrag.getView();
                    
                    if (view instanceof ChatView && chatId == ((ChatView)view).mLastChatId)                        
                    {
                        position = posIdx;
                        
                        break;
                    }
                    
                    posIdx++;
                }
                while (mCursorChats.moveToNext());
                
                
                return position;
                
            }
            else if (object instanceof ContactListFragment)
            {
                return 0;
                
            }
            
            return POSITION_NONE;
            
            
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

                try
                {
                           mCursorChats.moveToPosition(positionMod);
                           return mCursorChats.getString(ChatView.NICKNAME_COLUMN);
                }
                catch (Exception e)
                {
                    mChatPagerAdapter.notifyDataSetChanged();
                    
                    if (mCursorChats == null)
                    {
                        Log.e(ImApp.LOG_TAG,"error getting chat",e);
                        return "";
                    }
                    else
                    {
                        mCursorChats.moveToPosition(positionMod);       
                        return mCursorChats.getString(ChatView.NICKNAME_COLUMN);
                    }
                }
            }
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
        SignInHelper mSignInHelper = null;
        
        private Handler mPresenceHandler = new Handler()
        {
            
            @Override
            public void handleMessage(Message msg) {
               
                
                mPresenceView.refreshLogginInStatus();

                super.handleMessage(msg);
            } 
        };
        
         /**
          * When creating, retrieve this instance's number from its arguments.
          */
         @Override
         public void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);
             
             mSignInHelper = new SignInHelper(getActivity());
             
         }

         /**
          * The Fragment's UI is just a simple text view showing its
          * instance number.
          */
         @Override
         public View onCreateView(LayoutInflater inflater, ViewGroup container,
                 Bundle savedInstanceState) {
             

              mFilterView = (ContactListFilterView) inflater.inflate(
                     R.layout.contact_list_filter_view, null);
              
              mPresenceView = (UserPresenceView) mFilterView.findViewById(R.id.userPresence);


             mFilterView.setListener(this);
             
             
             ((ImApp)getActivity().getApplication()).registerForConnEvents(mPresenceHandler);
             
             
            
             
            //  QueryMap mGlobalSettingMap = new Imps.ProviderSettings.QueryMap(getContext().getContentResolver(), true, mHandler);
             
         //    Uri uri = mGlobalSettingMap.getHideOfflineContacts() ? Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY
                                                                 // : Imps.Contacts.CONTENT_URI_CONTACTS_BY;
           //   uri = ContentUris.withAppendedId(uri, providerId);
            //  uri = ContentUris.withAppendedId(uri, accountId);
         //     mFilterView.doFilter( Imps.Contacts.CONTENT_URI_CONTACTS_BY, null);
              
              setupSpinners(mFilterView);
              return mFilterView;
           
         }

         
         
         @Override
        public void onDestroy() {           
            super.onDestroy();
            
            if (mProviderCursor != null && (!mProviderCursor.isClosed()))
                    mProviderCursor.close();
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
             
             Spinner spinnerAccounts = (Spinner)filterView.findViewById(R.id.spinnerAccounts);
             
           
             spinnerAccounts.setAdapter(pAdapter);
             spinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener ()
             {

                 @Override
                 public void onItemSelected(AdapterView<?> parent, View view, int itemPosition, long id) {
                    
                 //    mAccountId = mAccountIds[itemPosition];
                     //update account list
                     initAccount(mAccountIds[itemPosition]);
                   
                     
                 }

                 @Override
                 public void onNothingSelected(AdapterView<?> arg0) {
                     // TODO Auto-generated method stub
                     
                 }
                 
             });
             
             mProviderCursor.moveToFirst();
             int activeAccountIdColumn = mProviderCursor.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_ID);

            // int currentAccountIndex = -1;
             int selIdx = -1;
             
             for (int i = 0; i < mAccountIds.length; i++)
             {
                 mAccountIds[i] = mProviderCursor.getLong(activeAccountIdColumn);
                 
                 if (mAccountIds[i] == mAccountId)
                     selIdx = i;
                 
                 mProviderCursor.moveToNext();
                 
             }

             if (selIdx != -1)
                 spinnerAccounts.setSelection(selIdx);
             
             if (mAccountIds.length == 1)
             {
                 spinnerAccounts.setVisibility(View.GONE);
                 initAccount(mAccountIds[0]);
             }
             
            
         }
         
         public void initAccount (long accountId)
         {

             ContentResolver cr = getActivity().getContentResolver();
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

             mLastProviderId = c.getLong(c.getColumnIndexOrThrow(Imps.Account.PROVIDER));
             
             initConnection (accountId, mLastProviderId);
             
             c.close();
         }
         
         private void initConnection (long accountId, long providerId)
         {
             IImConnection conn = ((ImApp)getActivity().getApplication()).getConnection(providerId);
           
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
                 //mActiveChatListView.setConnection(conn);     
                 
                 mPresenceView.setConnection(conn);

                 try {
                     mPresenceView.loggingIn(conn.getState() == ImConnection.LOGGING_IN);
                 } catch (RemoteException e) {
                     
                     mPresenceView.loggingIn(false);
                 //    mHandler.showServiceErrorAlert();
                 }

                 Uri uri =Imps.Contacts.CONTENT_URI_CONTACTS_BY ;
                 
                 //mGlobalSettingMap.getHideOfflineContacts() ? Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY
                   //                                                  : Imps.Contacts.CONTENT_URI_CONTACTS_BY;
                 uri = ContentUris.withAppendedId(uri, providerId);
                 uri = ContentUris.withAppendedId(uri, accountId);
                 mFilterView.doFilter(uri, null);

                 mChatPagerAdapter.notifyDataSetChanged();
                
             }        
             
           
         }

        @Override
        public void startChat(Cursor c) {
            
            NewChatActivity activity = (NewChatActivity)getActivity();
            
            if (c != null && activity != null) {
                long chatContactId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts._ID));
                String username = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
                
                long providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts.PROVIDER));
                IImConnection conn = ((ImApp)activity.getApplication()).getConnection(providerId);
                
                try {
                    IChatSessionManager manager = conn.getChatSessionManager();
                    IChatSession session = manager.getChatSession(username);
                    if (session == null) {
                        manager.createChatSession(username);
                    }

                    activity.refreshChatViews();
                    
                    activity.showChat(chatContactId);
                    
                  
                    
                } catch (RemoteException e) {
                  //  mHandler.showServiceErrorAlert();
                }
               
            }
            
            
        }
        
        public void showProfile (Cursor c)
        {
            String remoteFingerprint = null;
            String localFingerprint = null;
            boolean isVerified = false;

            if (c != null) {
                long chatContactId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts._ID));
           

                long providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts.PROVIDER));
                    Uri data = ContentUris.withAppendedId(Imps.Contacts.CONTENT_URI, chatContactId);

                    Intent intent = new Intent(Intent.ACTION_VIEW, data);
                    intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, providerId);
                    intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);

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
           
            
            long providerId = mProviderCursor.getLong(PROVIDER_ID_COLUMN);
            String password = mProviderCursor.getString(ACTIVE_ACCOUNT_PW_COLUMN);
            
            boolean isActive = false; // TODO(miron)
            mSignInHelper.signIn(password, mLastProviderId, accountId, isActive);
            
            
        }

        @Override
        public void signOut(long accountId) {
         
            IImConnection conn = ((ImApp)getActivity().getApplication()).getConnection(mLastProviderId);
            try {
                conn.logout();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        
    
    }
    
   
    
    
    public static class ChatViewFragment extends Fragment {
        
         ChatView mChatView;
         
        /**
         * Create a new instance of CountingFragment, providing "num"
         * as an argument.
         */
        static ChatViewFragment newInstance(long chatContactId) {
            
            ChatViewFragment f = new ChatViewFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putLong("contactChatId", chatContactId);
            f.setArguments(args);

            return f;
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
            
            long chatContactId = getArguments().getLong("contactChatId");
            mChatView = (ChatView)inflater.inflate(R.layout.chat_view, container, false);
            mChatView.bindChat(chatContactId);                       
            
            return mChatView;
        }

        @Override
        public void onPause() {
            super.onPause();
            
            if (mChatView != null)
                mChatView.stopListening();
            
        }

        @Override
        public void onResume() {
            super.onResume();
            
            if (mChatView != null)
                mChatView.startListening();
        }
        

    }
    
    public ChatView getCurrentChatView ()
    {
        int cItemIdx;
        
        if ((cItemIdx = mChatPager.getCurrentItem()) > 0)
        {
            return (ChatView)((ChatViewFragment)mChatPagerAdapter.getItem(cItemIdx)).getView();            
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
                    
                    startGroupChat (chatRoom, chatServer, mLastProviderId);
                    
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked cancel so do some stuff */
                }
            })
            .create().show();
        
        
        
    }
    
    public void startGroupChat (String room, String server, long providerId)
    {
        IImConnection conn = ((ImApp)getApplication()).getConnection(providerId);
        
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
            
                Uri data = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, id);
                Intent i = new Intent(Intent.ACTION_VIEW, data);
                i.addCategory(ImApp.IMPS_CATEGORY);
            
                if (menu.isShown())
                    menu.toggle();
            
                startActivity(i);
            }
            else
            {
               // mHandler.showServiceErrorAlert();
            }
            
        } catch (RemoteException e) {
          //  mHandler.showServiceErrorAlert();
        }
       
    }
    
    void acceptInvitation(long providerId, long invitationId) {
        try {

            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                conn.acceptInvitation(invitationId);
            }
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }
    }

    void declineInvitation(long providerId, long invitationId) {
        try {
            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                conn.rejectInvitation(invitationId);
            }
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
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
        } catch (RemoteException ex) {
            mHandler.showServiceErrorAlert();
        }
    }

    void declineSubscription(long providerId, String userName) {
        IImConnection conn = mApp.getConnection(providerId);

        if (conn != null)
        {
            try {
                IContactListManager manager = conn.getContactListManager();
                manager.declineSubscription(userName);
            } catch (RemoteException ex) {
                mHandler.showServiceErrorAlert();
            }
        }
    }
    

}
