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
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class NewChatActivity extends ThemeableActivity implements View.OnCreateContextMenuListener {

    private static final int MENU_RESEND = Menu.FIRST;
    private static final int REQUEST_PICK_CONTACTS = RESULT_FIRST_USER + 1;
    private static final int REQUEST_SEND_IMAGE = REQUEST_PICK_CONTACTS + 1;
    private static final int REQUEST_SEND_FILE = REQUEST_SEND_IMAGE + 1;

    ImApp mApp;
    ChatView mChatView;
    SimpleAlertHandler mHandler;
    MenuItem menuOtr, menuCall;

    private AlertDialog mSmileyDialog;
    private ChatSwitcher mChatSwitcher;
    private LayoutInflater mInflater;

    private long mAccountId = -1;
    private String mSipAccount = null;
    
    ContextMenuHandler mContextMenuHandler;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.chat_view);
        
        getSherlock().getActionBar().setHomeButtonEnabled(true);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(true);

        
        mChatView = (ChatView) findViewById(R.id.chatView);
        mHandler = mChatView.getHandler();
        mInflater = LayoutInflater.from(this);
        
        getSipAccount();
        
        
        
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
       
        

        mApp = (ImApp)getApplication();
        mChatSwitcher = new ChatSwitcher(this, mHandler, mApp, mInflater, null);

        mContextMenuHandler = new ContextMenuHandler();
        mChatView.getHistoryView().setOnCreateContextMenuListener(this);

        final Handler handler = new Handler();
        mApp.callWhenServiceConnected(handler, new Runnable() {
            public void run() {
                resolveIntent(getIntent());
            }
        });
    }
    
    protected void setHomeIcon (Drawable d)
    {
        getSherlock().getActionBar().setIcon(d);
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
        mChatView.onResume();
        getSipAccount();
    }

    @Override
    protected void onPause() {
        mChatView.onPause();
        super.onPause();
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
                mChatSwitcher.open();
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
                mChatView.bindSubscription(providerId, from);
            }
        } else {
            Uri data = intent.getData();
            String type = getContentResolver().getType(data);
            if (Imps.Chats.CONTENT_ITEM_TYPE.equals(type)) {
                mChatView.bindChat(ContentUris.parseId(data));
            } else if (Imps.Invitation.CONTENT_ITEM_TYPE.equals(type)) {
                mChatView.bindInvitation(ContentUris.parseId(data));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
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

        case R.id.menu_send_image:
            startImagePicker();
            return true;

        case R.id.menu_send_file:
            startFilePicker();
            return true;

        case R.id.menu_view_otr:
            switchOtrState();
            return true;
            
        case R.id.menu_secure_call:
            sendCallInvite ();
            return true;

        case R.id.menu_view_profile:
            mChatView.viewProfile();
            return true;

        case R.id.menu_end_conversation:
            mChatView.closeChatSession();
            return true;
         
        case R.id.menu_switch_chats:
            if (mChatSwitcher.isOpen()) {
                mChatSwitcher.close();
            } else {
                mChatSwitcher.open();
            }
            return true;

        case android.R.id.home:
            showChatList();
            return true;
            
        case R.id.menu_view_accounts:
            startActivity(new Intent(getBaseContext(), ChooseAccountActivity.class));
          //  finish();
            return true;
            
        case R.id.menu_prev_chat:
            switchChat(-1);
            return true;

        case R.id.menu_next_chat:
            switchChat(1);
            return true;

        case R.id.menu_quick_switch_0:
        case R.id.menu_quick_switch_1:
        case R.id.menu_quick_switch_2:
        case R.id.menu_quick_switch_3:
        case R.id.menu_quick_switch_4:
        case R.id.menu_quick_switch_5:
        case R.id.menu_quick_switch_6:
        case R.id.menu_quick_switch_7:
        case R.id.menu_quick_switch_8:
        case R.id.menu_quick_switch_9:
            mChatSwitcher.handleShortcut(item.getAlphabeticShortcut());
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
        
        mChatView.sendMessage("&#9742; Click to start call <a href=\"https://foo.com\">sip:" + this.mSipAccount + "</a>");
        
    }
    
    private void switchOtrState() {

        IOtrChatSession otrChatSession = mChatView.getOtrChatSession();
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
                    mChatView.updateWarningView();
                }
                updateOtrMenuState();
                
                Toast.makeText(this, getString(toastMsgId), Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                Log.d("Gibber", "error getting remote activity", e);
            }
        }
    }

    public void updateOtrMenuState() {
        
        if (menuOtr == null)
            return;

        IOtrChatSession otrChatSession = mChatView.getOtrChatSession();

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

   private void showRosterScreen() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, ContactListActivity.class);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mChatView.getAccountId());
        startActivity(intent);
    }

//    private void showSmileyDialog() {
//        if (mSmileyDialog == null) {
//            long providerId = mChatView.getProviderId();
//
//            final BrandingResources brandingRes = mApp.getBrandingResource(providerId);
//            int[] icons = brandingRes.getSmileyIcons();
//            String[] names = brandingRes
//                    .getStringArray(BrandingResourceIDs.STRING_ARRAY_SMILEY_NAMES);
//            final String[] texts = brandingRes
//                    .getStringArray(BrandingResourceIDs.STRING_ARRAY_SMILEY_TEXTS);
//
//            final int N = names.length;
//
//            List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
//            for (int i = 0; i < N; i++) {
//                // We might have different ASCII for the same icon, skip it if
//                // the icon is already added.
//                boolean added = false;
//                for (int j = 0; j < i; j++) {
//                    if (icons[i] == icons[j]) {
//                        added = true;
//                        break;
//                    }
//                }
//                if (!added) {
//                    HashMap<String, Object> entry = new HashMap<String, Object>();
//
//                    entry.put("icon", icons[i]);
//                    entry.put("name", names[i]);
//                    entry.put("text", texts[i]);
//
//                    entries.add(entry);
//                }
//            }
//
//            final SimpleAdapter a = new SimpleAdapter(this, entries, R.layout.smiley_menu_item,
//                    new String[] { "icon", "name", "text" }, new int[] { R.id.smiley_icon,
//                                                                        R.id.smiley_name,
//                                                                        R.id.smiley_text });
//            SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
//                public boolean setViewValue(View view, Object data, String textRepresentation) {
//                    if (view instanceof ImageView) {
//                        Drawable img = brandingRes.getSmileyIcon((Integer) data);
//                        ((ImageView) view).setImageDrawable(img);
//                        return true;
//                    }
//                    return false;
//                }
//            };
//            a.setViewBinder(viewBinder);
//
//            AlertDialog.Builder b = new AlertDialog.Builder(this);
//
//            b.setTitle(brandingRes.getString(BrandingResourceIDs.STRING_MENU_INSERT_SMILEY));
//
//            b.setCancelable(true);
//            b.setAdapter(a, new DialogInterface.OnClickListener() {
//                public final void onClick(DialogInterface dialog, int which) {
//                    HashMap<String, Object> item = (HashMap<String, Object>) a.getItem(which);
//                    mChatView.insertSmiley((String) item.get("text"));
//                }
//            });
//
//            mSmileyDialog = b.create();
//        }
//
//        mSmileyDialog.show();
//    }

    private void switchChat(int delta) {
        long providerId = mChatView.getProviderId();
        long accountId = mChatView.getAccountId();
        String contact = mChatView.getUserName();

        mChatSwitcher.rotateChat(delta, contact, accountId, providerId);
    }

    private void startContactPicker() {
        Uri.Builder builder = Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY.buildUpon();
        ContentUris.appendId(builder, mChatView.getProviderId());
        ContentUris.appendId(builder, mChatView.getAccountId());
        Uri data = builder.build();

        try {
            Intent i = new Intent(Intent.ACTION_PICK, data);
            i.putExtra(ContactsPickerActivity.EXTRA_EXCLUDED_CONTACTS, mChatView
                    .getCurrentChatSession().getPariticipants());
            startActivityForResult(i, REQUEST_PICK_CONTACTS);
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }
    }
    
    private void startImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SEND_IMAGE);
    }
    
    private void startFilePicker() {
        Intent selectFile = new Intent(Intent.ACTION_GET_CONTENT);
        selectFile.setType("file/*");
        startActivityForResult(Intent.createChooser(selectFile, "Select File"), REQUEST_SEND_FILE);
    }
    
    public String getRealPathFromURI(Context aContext, Uri uri) {
        if (uri.getScheme().equals("file")) {
            return uri.getPath();
        }
        
        if (uri.toString().startsWith("content://org.openintents.filemanager/")) {
            // Work around URI escaping brokenness
            return uri.toString().replaceFirst("content://org.openintents.filemanager", "");
        }
        
        Cursor cursor = aContext.getContentResolver().query(uri, null, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_SEND_IMAGE || requestCode == REQUEST_SEND_FILE) {
                Uri uri = data.getData() ;
                if( uri == null ) {
                    return ;
                }
                try {
                    String localUri = getRealPathFromURI(this, uri);
                    mChatView.getCurrentChatSession().offerData( localUri );
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (requestCode == REQUEST_PICK_CONTACTS) {
                String username = data.getStringExtra(ContactsPickerActivity.EXTRA_RESULT_USERNAME);
                try {
                    IChatSession chatSession = mChatView.getCurrentChatSession();
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
                        mChatView.bindChat(chatId);
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
        AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        mContextMenuHandler.mPosition = info.position;
        Cursor cursor = mChatView.getMessageAtPosition(info.position);
        int type = cursor.getInt(cursor.getColumnIndexOrThrow(Imps.Messages.TYPE));
        if (type == Imps.MessageType.OUTGOING) {
            menu.add(0, MENU_RESEND, 0, R.string.menu_resend).setOnMenuItemClickListener(
                    mContextMenuHandler);
        }
    }

    final class ContextMenuHandler implements MenuItem.OnMenuItemClickListener, OnMenuItemClickListener {
        int mPosition;

        public boolean onMenuItemClick(MenuItem item) {
            Cursor c;
            c = mChatView.getMessageAtPosition(mPosition);

            switch (item.getItemId()) {
            case MENU_RESEND:
                String text = c.getString(c.getColumnIndexOrThrow(Imps.Messages.BODY));
                mChatView.getComposedMessage().setText(text);
                break;
            default:
                return false;
            }

            return true;
        }

        @Override
        public boolean onMenuItemClick(android.view.MenuItem item) {
            Cursor c;
            c = mChatView.getMessageAtPosition(mPosition);

            switch (item.getItemId()) {
            case MENU_RESEND:
                String text = c.getString(c.getColumnIndexOrThrow(Imps.Messages.BODY));
                mChatView.getComposedMessage().setText(text);
                break;
            default:
                return false;
            }            return false;
        }
    }

}
