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
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.adapter.ChatListenerAdapter;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import net.java.otr4j.session.SessionStatus;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;


public class NewChatActivity extends FragmentActivity implements View.OnCreateContextMenuListener {

    private static final int MENU_RESEND = Menu.FIRST;
    private static final int REQUEST_PICK_CONTACTS = RESULT_FIRST_USER + 1;

    ImApp mApp;
    ViewPager mChatPager;
    ChatViewPagerAdapter mChatPagerAdapter;
   // ChatView mChatView;

    Cursor mCursorChats;
    
    SimpleAlertHandler mHandler;
    MenuItem menuOtr, menuCall;

   //private ChatSwitcher mChatSwitcher;
    private LayoutInflater mInflater;

    private long mAccountId = -1;
    private String mSipAccount = null;
    
    ContextMenuHandler mContextMenuHandler;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);        
        setContentView(R.layout.chat_pager);

        getSipAccount();

        mChatPager = (ViewPager) findViewById(R.id.chatpager);
        mChatPager.setOnPageChangeListener(new OnPageChangeListener ()
        {

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onPageSelected(int arg0) {
               
            }
            
        });
        
        
        mChatPagerAdapter = new ChatViewPagerAdapter(getSupportFragmentManager());
        mChatPager.setAdapter(mChatPagerAdapter);
        
        mApp = (ImApp)getApplication();
        mInflater = LayoutInflater.from(this);
      //  mChatSwitcher = new ChatSwitcher(this, mHandler, mApp, mInflater, null);

        mContextMenuHandler = new ContextMenuHandler();

        final Handler handler = new Handler();
        mApp.callWhenServiceConnected(handler, new Runnable() {
            public void run() {
                resolveIntent(getIntent());
            }
        });
    }
    
    
    
    private void initChatView ()
    {
     //   mChatView = (ChatView) findViewById(R.id.chatView);
     //  mHandler = mChatView.getHandler();
     //   mChatView.getHistoryView().setOnCreateContextMenuListener(this);

        /*
        EditText mCompose = (EditText)findViewById(R.id.composeMessage);
        mCompose.setOnLongClickListener(new OnLongClickListener ()
        {


            @Override
            public boolean onLongClick(View arg0) {
               
                if (getSherlock().getActionBar().isShowing())
                    getSherlock().getActionBar().hide();
                else
                    getSherlock().getActionBar().show();     
                
                return false;
            }

            
        });
       */
        
    }
    
    protected void setHomeIcon (Drawable d)
    {
      //  getSherlock().getActionBar().setIcon(d);
    }
    
    private void getSipAccount ()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mSipAccount = prefs.getString("pref_sip_account", null);

        if (menuCall != null)
        {
        if (mSipAccount != null && mSipAccount.length() > 0)
            menuCall.setVisible(true);
        else
            menuCall.setVisible(false);
        }
    }
    

    @Override
    protected void onResume() {
        super.onResume();
        
        getSipAccount();
    }

    @Override
    protected void onPause() {
        
        
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (mCursorChats != null)
            mCursorChats.close();
        
        super.onDestroy();
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
            }
        } else {
            Uri data = intent.getData();
            String type = getContentResolver().getType(data);
            if (Imps.Chats.CONTENT_ITEM_TYPE.equals(type)) {
                
                long requestedChatId = ContentUris.parseId(data);
                
                //chatView.bindChat(ContentUris.parseId(data));
                mCursorChats.moveToPosition(0);
                int posIdx = 0;
                while (mCursorChats.moveToNext())
                {
                    long chatId = mCursorChats.getLong(ChatView.CHAT_ID_COLUMN);
                    
                    if (chatId == requestedChatId)
                    {
                        mChatPager.setCurrentItem(posIdx+1);
                        break;
                    }
                    
                    posIdx++;
                }
                
                
            } else if (Imps.Invitation.CONTENT_ITEM_TYPE.equals(type)) {
                //chatView.bindInvitation(ContentUris.parseId(data));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_screen_menu, menu);

        menuOtr = menu.findItem(R.id.menu_view_otr);
        menuCall = menu.findItem(R.id.menu_secure_call);
        
        getSipAccount ();
        
        
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateOtrMenuState();
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case R.id.menu_view_otr:
            switchOtrState();
            return true;
            
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
    
    private void switchOtrState() {

        ChatView chatView = getCurrentChatView ();
        
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
                updateOtrMenuState();
                
                Toast.makeText(this, getString(toastMsgId), Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                Log.d("Gibber", "error getting remote activity", e);
            }
        }
    }

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
    }


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

                        chatView.bindChat(chatId);
                        showInvitationHasSent(mContact);
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

        AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        mContextMenuHandler.mPosition = info.position;
        Cursor cursor =  chatView.getMessageAtPosition(info.position);
        int type = cursor.getInt(cursor.getColumnIndexOrThrow(Imps.Messages.TYPE));
        if (type == Imps.MessageType.OUTGOING) {
            menu.add(0, MENU_RESEND, 0, R.string.menu_resend).setOnMenuItemClickListener(
                    mContextMenuHandler);
        }
    }

    final class ContextMenuHandler implements OnMenuItemClickListener {
        int mPosition;

     
        @Override
        public boolean onMenuItemClick(android.view.MenuItem item) {
            
            ChatView chatView = getCurrentChatView ();
            
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
    }
    
    

    public class ChatViewPagerAdapter extends FragmentStatePagerAdapter {
        
        
        public ChatViewPagerAdapter(FragmentManager fm) {
            super(fm);
            
            mCursorChats = managedQuery(Imps.Contacts.CONTENT_URI_CHAT_CONTACTS_BY, ChatView.CHAT_PROJECTION, null, null, null);
        }

        @Override
        public int getCount() {
            return mCursorChats.getCount();
        }

        @Override
        public Fragment getItem(int position) {
            
            long chatId = -1;
            
            try
            {
                mCursorChats.moveToPosition(position);            
                chatId = mCursorChats.getLong(ChatView.CHAT_ID_COLUMN);
            }
            catch (Exception e)
            {
                mCursorChats = managedQuery(Imps.Contacts.CONTENT_URI_CHAT_CONTACTS_BY, ChatView.CHAT_PROJECTION, null, null, null);

                mCursorChats.moveToPosition(position);       
                chatId = mCursorChats.getLong(ChatView.CHAT_ID_COLUMN);
            }
            
            return ChatViewFragment.newInstance(chatId);
        }

        @Override
        public CharSequence getPageTitle(int position) {
           
            try
            {
                       mCursorChats.moveToPosition(position);
                            return mCursorChats.getString(ChatView.USERNAME_COLUMN);
            }
            catch (Exception e)
            {
                mCursorChats = managedQuery(Imps.Contacts.CONTENT_URI_CHAT_CONTACTS_BY, ChatView.CHAT_PROJECTION, null, null, null);

                mCursorChats.moveToPosition(position);       
                return mCursorChats.getString(ChatView.USERNAME_COLUMN);
            }
        }

        @Override
        public Parcelable saveState() {
            return super.saveState();
        }
        
        
    }
    
    public static class ChatViewFragment extends Fragment {
        long mChatId;
        ChatView mChatView;
        /**
         * Create a new instance of CountingFragment, providing "num"
         * as an argument.
         */
        static ChatViewFragment newInstance(long chatId) {
            ChatViewFragment f = new ChatViewFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putLong("chatId", chatId);
            f.setArguments(args);

            return f;
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mChatId = getArguments() != null ? getArguments().getLong("chatId") : 1;
            
        }

        /**
         * The Fragment's UI is just a simple text view showing its
         * instance number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            
            mChatView = (ChatView)inflater.inflate(R.layout.chat_view, container, false);;
            mChatView.bindChat(mChatId);
            return mChatView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
           
        }

    }
    
    public ChatView getCurrentChatView ()
    {
        ChatView chatView = ((ChatViewFragment)mChatPagerAdapter.getItem(mChatPager.getCurrentItem())).mChatView;
        return chatView;
    }
    
    
}
