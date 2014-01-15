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
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.util.LogCleaner;
import info.guardianproject.util.SystemServices;
import info.guardianproject.util.SystemServices.FileInfo;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.java.otr4j.session.SessionStatus;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class ImUrlActivity extends Activity {
    
    private static final int REQUEST_PICK_CONTACTS = RESULT_FIRST_USER + 1;
    private static final int REQUEST_CREATE_ACCOUNT = RESULT_FIRST_USER + 2;
    private static final int REQUEST_SIGNIN_ACCOUNT = RESULT_FIRST_USER + 3;
    private static final int REQUEST_START_MUC =  RESULT_FIRST_USER + 4;
    
    private String mProviderName;
    private String mToAddress;
    private String mFromAddress;
    private String mHost;
    
    private IImConnection mConn;
    private IChatSessionManager mChatSessionManager;
    
    private String mSendUrl;
    private String mSendType;
    private String mSendText;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        doOnCreate();
    }

    
    
    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        setIntent(intent);
    }

    public void onDBLocked() {
        
        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    void handleIntent() {
        ContentResolver cr = getContentResolver();
        
        long providerId = -1;
        long accountId = -1;
        
        Collection<IImConnection> listConns = ((ImApp)getApplication()).getActiveConnections();
        
        //look for active connections that match the host we need
        for (IImConnection conn : listConns)
        {            
   
            
            try {
                long connProviderId = conn.getProviderId();

                Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                        cr, connProviderId, false /* don't keep updated */, null /* no handler */);
                
                try {
                    String domainToCheck = settings.getDomain();
                    
                    if (domainToCheck != null && domainToCheck.length() > 0 && mHost.contains(domainToCheck))
                    {
                        mConn = conn;
                        providerId = connProviderId;
                        accountId = conn.getAccountId();
                        
                        break;
                    }
                } finally {
                    settings.close();
                }
                
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            
            
        }

        //nothing active, let's see if non-active connections match
        if (mConn == null) {
            
            Cursor cursorProvider = initProviderCursor();
            
            if (cursorProvider == null || cursorProvider.getCount() == 0) {                                

                createNewAccount();
                return;
            } else {
                
                
                while (cursorProvider.moveToNext())
                {                
                    //make sure there is a stored password
                    if (!cursorProvider.isNull(ACTIVE_ACCOUNT_PW_COLUMN)) {
                            
                        long cProviderId = cursorProvider.getLong(PROVIDER_ID_COLUMN);
                        
                        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                                cr, cProviderId, false /* don't keep updated */, null /* no handler */);
                        
                        //does the conference host we need, match the settings domain for a logged in account
                        String domainToCheck = settings.getDomain(); 
                        
                        if (domainToCheck != null && domainToCheck.length() > 0 && mHost.contains(domainToCheck))
                        {
                            providerId = cProviderId;
                            accountId = cursorProvider.getLong(ACTIVE_ACCOUNT_ID_COLUMN);
                            mConn = ((ImApp)getApplication()).getConnection(providerId);
                            
                            //now sign in
                            signInAccount(accountId, providerId, cursorProvider.getString(ACTIVE_ACCOUNT_PW_COLUMN));
                            
                            break; //do sign in, the rest of the process will happen later
                           
                        }
                        
                        settings.close();
                        cursorProvider.close();
                        
                    }
                }
                
                
                
                
            }
            
        }
            
        if (mConn != null)
        {
            try {
                int state = mConn.getState();
                accountId = mConn.getAccountId();
                providerId = mConn.getProviderId();
                
                if (state < ImConnection.LOGGED_IN) {                
                    
                    Cursor cursorProvider = initProviderCursor();
                    
                    while(cursorProvider.moveToNext())
                    {
                        if (cursorProvider.getLong(ACTIVE_ACCOUNT_ID_COLUMN) == accountId)
                        {
                            signInAccount(accountId, providerId, cursorProvider.getString(ACTIVE_ACCOUNT_PW_COLUMN));

                            try {
                                Thread.sleep (500);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }//wait here for three seconds
                            mConn = ((ImApp)getApplication()).getConnection(providerId);

                            break;
                        }
                    }
                    
                    cursorProvider.close();
                } 
                
                if (state == ImConnection.LOGGED_IN || state == ImConnection.SUSPENDED) {
                    
                    Uri data = getIntent().getData();
                    
                    if (data.getScheme().equals("immu"))
                    {
                        this.openMultiUserChat(data);
                     
                    }  
                    else if (!isValidToAddress()) {
                        showContactList(accountId);
                    } else {
                        openChat(providerId, accountId);
                    }
                    
    
    
                }
            } catch (RemoteException e) {
                // Ouch!  Service died!  We'll just disappear.
                Log.w("ImUrlActivity", "Connection disappeared!");
                finish();
            }
        }
        else
        {
            createNewAccount();
            return;
        }
    }

    /*
    private void addAccount(long providerId) {
        Intent intent = new Intent(this, AccountActivity.class);
        intent.setAction(Intent.ACTION_INSERT);
        intent.setData(ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId));
//        intent.putExtra(ImApp.EXTRA_INTENT_SEND_TO_USER, mToAddress);

        if (mFromAddress != null)
            intent.putExtra("newuser", mFromAddress + '@' + mHost);
        
        startActivity(intent);
    }*/

    private void editAccount(long accountId) {
        Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
        Intent intent = new Intent(this, AccountActivity.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.setData(accountUri);
        intent.putExtra(ImApp.EXTRA_INTENT_SEND_TO_USER, mToAddress);
        startActivityForResult(intent,REQUEST_SIGNIN_ACCOUNT);
    }

    private void signInAccount(long accountId, long providerId, String password) {
        //editAccount(accountId);        
        // TODO sign in?  security implications?        
        SignInHelper signInHelper = new SignInHelper(this);
        signInHelper.setSignInListener(new SignInHelper.SignInListener() {
            public void connectedToService() {
            }
            public void stateChanged(int state, long accountId) {
                if (state == ImConnection.LOGGED_IN) {
                    handleIntent();
                }
                else if (state == ImConnection.LOGGING_IN)
                {
                    Toast.makeText(ImUrlActivity.this, R.string.signing_in_wait, Toast.LENGTH_LONG).show();
                }
                
            }
        });
        
        signInHelper.signIn(password, providerId, accountId, true);
    }

    private void showContactList(long accountId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Imps.Contacts.CONTENT_URI);
        intent.addCategory(ImApp.IMPS_CATEGORY);
        intent.putExtra("accountId", accountId);

        startActivity(intent);
    }

    private void openChat(long provider, long account) {
        try {
            IChatSessionManager manager = mConn.getChatSessionManager();
            IChatSession session = manager.getChatSession(mToAddress);
            if (session == null) {
                session = manager.createChatSession(mToAddress);
            }

            Uri data = ContentUris.withAppendedId(Imps.Chats.CONTENT_URI, session.getId());
            Intent intent = new Intent(Intent.ACTION_VIEW, data);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_FROM_ADDRESS, mToAddress);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, provider);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, account);
            intent.addCategory(ImApp.IMPS_CATEGORY);
            startActivity(intent);
        } catch (RemoteException e) {
            // Ouch!  Service died!  We'll just disappear.
            Log.w("ImUrlActivity", "Connection disappeared!");
        }
    }

    private boolean resolveInsertIntent(Intent intent) {
        Uri data = intent.getData();
        
        if (data.getScheme().equals("ima"))
        {
            createNewAccount();
         
            return true;
        }
        return false;
    }
    
    private boolean resolveIntent(Intent intent) {
        Uri data = intent.getData();
        mHost = data.getHost();
        
        if (data.getScheme().equals("immu")) {
            mFromAddress = data.getUserInfo();
            String chatRoom = null;
            
            if (data.getPathSegments().size() > 0)
                chatRoom = data.getPathSegments().get(0);
           
            mToAddress = chatRoom + '@' + mHost;
            
            mProviderName = findMatchingProvider(mHost);
            
            return true;
        }

        if (data.getScheme().equals("otr-in-band")) {
            this.openOtrInBand(data, intent.getType());
         
            return true;
        }

        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("resolveIntent: host=" + mHost);
        }

        if (TextUtils.isEmpty(mHost)) {
            Set<String> categories = intent.getCategories();
            if (categories != null) {
                Iterator<String> iter = categories.iterator();
                if (iter.hasNext()) {
                    String category = iter.next();
                    String providerName = getProviderNameForCategory(category);
                    mProviderName = findMatchingProvider(providerName);
                    if (mProviderName == null) {
                        Log.w(ImApp.LOG_TAG, "resolveIntent: IM provider " + category
                                             + " not supported");
                        return false;
                    }
                }
            }
            
            mToAddress = data.getSchemeSpecificPart();
        } else {
            mProviderName = findMatchingProvider(mHost);

            if (mProviderName == null) {
                Log.w(ImApp.LOG_TAG, "resolveIntent: IM provider " + mHost + " not supported");
                return false;
            }

            String path = data.getPath();

            if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG))
                log("resolveIntent: path=" + path);

            if (!TextUtils.isEmpty(path)) {
                int index;
                if ((index = path.indexOf('/')) != -1) {
                    mToAddress = path.substring(index + 1);
                }
            }
        }

        if (Log.isLoggable(ImApp.LOG_TAG, Log.DEBUG)) {
            log("resolveIntent: provider=" + mProviderName + ", to=" + mToAddress);
        }

        return true;
    }

    private String getProviderNameForCategory(String providerCategory) {
        return Imps.ProviderNames.XMPP;
    }

    private String findMatchingProvider(String provider) {
        if (TextUtils.isEmpty(provider)) {
            return null;
        }

//        if (provider.equalsIgnoreCase("xmpp"))
  //          return Imps.ProviderNames.XMPP;
    
        
        return "Jabber (XMPP)";
        //return Imps.ProviderNames.XMPP;
    }

    private boolean isValidToAddress() {
        if (TextUtils.isEmpty(mToAddress)) {
            return false;
        }

        if (mToAddress.indexOf('/') != -1) {
            return false;
        }

        return true;
    }

    private static void log(String msg) {
        Log.d(ImApp.LOG_TAG, "<ImUrlActivity> " + msg);
    }
    

    void openMultiUserChat(final Uri data) {
        
        new AlertDialog.Builder(this)            
        .setTitle("Join Chat Room?")
        .setMessage("An external app is attempting to connect you to a chatroom. Allow?")
        .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                Intent intent = new Intent(ImUrlActivity.this, NewChatActivity.class);        
                intent.setData(data);
                ImUrlActivity.this.startActivityForResult(intent, REQUEST_START_MUC);
                
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                /* User clicked cancel so do some stuff */
                finish();
            }
        })
        .create().show();
        
       
    }

    void createNewAccount() {
        
        String username = getIntent().getData().getUserInfo();
        String appCreateAcct = String.format(getString(R.string.allow_s_to_create_a_new_chat_account_for_s_),username);
        
        new AlertDialog.Builder(this)            
        .setTitle(R.string.prompt_create_new_account_)
        .setMessage(appCreateAcct)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
             
              
                mHandlerRouter.sendEmptyMessage(1);
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        })
        .create().show();
    }

    Handler mHandlerRouter = new Handler ()
    {

        @Override
        public void handleMessage(Message msg) {
            
            if (msg.what == 1)
            {
                Uri uriAccountData = getIntent().getData();
                
                if (uriAccountData.getScheme().equals("immu"))
                {
                    //need to generate proper IMA url for account setup
                    String randomJid = ((int)(Math.random()*1000))+"";
                    String regUser = mFromAddress + randomJid;
                    String regPass =  UUID.randomUUID().toString().substring(0,16);
                    String regDomain = mHost.replace("conference.", "");                
                    uriAccountData = Uri.parse("ima://" + regUser + ':' + regPass + '@' + regDomain);
                }
                    
                Intent intent = new Intent(ImUrlActivity.this, AccountActivity.class);
                intent.setAction(Intent.ACTION_INSERT);
                intent.setData(uriAccountData);
                startActivityForResult(intent,REQUEST_CREATE_ACCOUNT);
            }
            else if (msg.what == 2)
            {
                doOnCreate();
            }
        }
        
    };
    
    public String getRealPathFromURI(Uri contentUri, String type) {
        
        String[] proj = { MediaColumns.DATA };
        
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }
    
    void openOtrInBand(final Uri data, final String type) {
        
        String localUrl = null;
        
        try
        {
            localUrl = getRealPathFromURI(data, type);
        }
        catch (Exception e)
        {
            LogCleaner.warn(ImApp.LOG_TAG, "unable to get path from URI");
        }
        
        if (localUrl == null)
            localUrl = data.toString();
        
        if (localUrl != null ) {
            
            if (localUrl.startsWith(OtrDataHandler.URI_PREFIX_OTR_IN_BAND))
                localUrl = localUrl.replaceFirst(OtrDataHandler.URI_PREFIX_OTR_IN_BAND, "");
       
          
            FileInfo info = SystemServices.getFileInfoFromURI(ImUrlActivity.this, data);
            
            if (info != null && info.path != null)
            {
                mSendUrl = info.path;
                mSendType = type != null ? type : info.type;
    
                startContactPicker();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_CONTACTS) {
                String username = resultIntent.getExtras().getString(ContactsPickerActivity.EXTRA_RESULT_USERNAME);
                sendOtrInBand(username);
                finish();
            }
            else if (requestCode == REQUEST_SIGNIN_ACCOUNT || requestCode == REQUEST_CREATE_ACCOUNT)
            {
                mHandlerRouter.postDelayed(new Runnable()
                {
                    public void run ()
                    {
                        doOnCreate();
                    }
                }, 500);
              
            }
            
        } else {
            finish();
        }
    }
    
    private void sendOtrInBand(String username) {
        
        IChatSession session = getChatSession(username);
        
        try
        {
            if (session.getOtrChatSession() != null)
            {
            
                if (session.getOtrChatSession().getChatStatus() != SessionStatus.ENCRYPTED.ordinal())
                {
                    //can't do OTR transfer
                    Toast.makeText(this, R.string.err_otr_share_no_encryption, Toast.LENGTH_LONG).show();
                }
                else 
                {
                    try {
                    
                        if (mSendUrl != null) {
                            String offerId = UUID.randomUUID().toString();
                            session.offerData(offerId, mSendUrl, mSendType );
                            
                            
                            Imps.insertMessageInDb(
                                    getContentResolver(), false, session.getId(), true, null, mSendUrl.toString(),
                                    System.currentTimeMillis(), Imps.MessageType.OUTGOING_ENCRYPTED, // TODO show verified status
                                    0, offerId, mSendType);
                        }
                        else if (mSendText != null)
                            session.sendMessage(mSendText);
                        
                        
                        
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
        
    }
    
    private IChatSession getChatSession(String username) {
        IChatSessionManager sessionMgr = mChatSessionManager;
        if (sessionMgr != null) {
            try {
                IChatSession session = sessionMgr.getChatSession(username);
                
                if (session == null)
                    session = sessionMgr.createChatSession(username);
              
                return session;
            } catch (RemoteException e) {
                LogCleaner.error(ImApp.LOG_TAG, "send message error",e);
            }
        }
        return null;
    }

    private void startContactPicker() {
        
        Uri.Builder builder = Imps.Contacts.CONTENT_URI_ONLINE_CONTACTS_BY.buildUpon();   
        Collection<IImConnection> listConns = ((ImApp)getApplication()).getActiveConnections();
        
        for (IImConnection conn : listConns)
        {
            try
            {
                if (conn.getState() == ImConnection.LOGGED_IN)
                {
                    try {
                        mChatSessionManager = conn.getChatSessionManager();
                        long mProviderId = conn.getProviderId();
                        long mAccountId = conn.getAccountId();
                   
                        ContentUris.appendId(builder,  mProviderId);
                        ContentUris.appendId(builder,  mAccountId);
                        Uri data = builder.build();
                
                        Intent i = new Intent(Intent.ACTION_PICK, data);
                        startActivityForResult(i, REQUEST_PICK_CONTACTS);
                        break;
                        
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            catch (RemoteException re){}
        }
    }

    void showLockScreen() {
        Intent intent = new Intent(this, LockScreenActivity.class);
      //  intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("originalIntent", getIntent());
        startActivity(intent);
      
    }
    
    private void doOnCreate ()
    {
        Intent intent = getIntent();
        if (Intent.ACTION_INSERT.equals(intent.getAction())) {
            if (!resolveInsertIntent(intent)) {
                finish();
                return;
            }
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
        
            Uri streamUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            String mimeType = intent.getType();
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            
            if (streamUri != null)
                openOtrInBand(streamUri, mimeType);
            else if (intent.getData() != null)
                openOtrInBand(intent.getData(), mimeType);
            else if (sharedText != null)
            {
                //do nothing for now :(
                mSendText = sharedText;

                startContactPicker();
                
            }
            else
                finish();
        
        } else if (Intent.ACTION_SENDTO.equals(intent.getAction())) {
            if (!resolveIntent(intent)) {
                finish();
                return;
            }

            if (TextUtils.isEmpty(mToAddress)) {
                LogCleaner.warn(ImApp.LOG_TAG, "<ImUrlActivity>Invalid to address");
              //  finish();
                return;
            }
            
            ImApp mApp = (ImApp)getApplication();
            
            if (mApp.serviceConnected())
                handleIntent();
            else
            {
                mApp.callWhenServiceConnected(new Handler(), new Runnable() {
                    public void run() {
                        
                       handleIntent();
                    }
                });
                Toast.makeText(ImUrlActivity.this, R.string.starting_the_chatsecure_service_, Toast.LENGTH_LONG).show();

            }
        } else {
            finish();
        }
    }
    
    private Cursor initProviderCursor ()
    {
        Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;
       // uri = uri.buildUpon().appendQueryParameter(ImApp.CACHEWORD_PASSWORD_KEY, pkey).build();
      
        //just init the contentprovider db
        return getContentResolver().query(uri, PROVIDER_PROJECTION,
                Imps.Provider.CATEGORY + "=?" + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL" /* selection */,
                new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                Imps.Provider.DEFAULT_SORT_ORDER);
        
    }
    
    

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
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
                                                         Imps.Provider.ACCOUNT_CONNECTION_STATUS
                                                        };
    

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
}
