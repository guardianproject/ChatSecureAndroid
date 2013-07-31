package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.otr.TorProxyInfo;
import info.guardianproject.otr.app.im.app.DatabaseUtils;
import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.ChatGroup;
import info.guardianproject.otr.app.im.engine.ChatGroupManager;
import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.ChatSessionManager;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ContactList;
import info.guardianproject.otr.app.im.engine.ContactListListener;
import info.guardianproject.otr.app.im.engine.ContactListManager;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.engine.ImException;
import info.guardianproject.otr.app.im.engine.Invitation;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.Presence;
import info.guardianproject.otr.app.im.plugin.xmpp.auth.GTalkOAuth2;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.ImpsErrorInfo;
import info.guardianproject.util.DNSUtil;
import info.guardianproject.util.Debug;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.harmony.javax.security.auth.callback.Callback;
import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.proxy.ProxyInfo.ProxyType;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;
import org.thoughtcrime.ssl.pinning.PinningTrustManager;
import org.thoughtcrime.ssl.pinning.SystemKeyStore;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;
import de.duenndns.ssl.MemorizingTrustManager;

public class XmppConnection extends ImConnection implements CallbackHandler {

    private static final String DISCO_FEATURE = "http://jabber.org/protocol/disco#info";
    final static String TAG = "GB.XmppConnection";
    private final static boolean PING_ENABLED = true;

    private XmppContactList mContactListManager;
    private Contact mUser;

    // watch out, this is a different XMPPConnection class than XmppConnection! ;)
    // Synchronized by executor thread
    private MyXMPPConnection mConnection;
    private XmppStreamHandler mStreamHandler;

    private XmppChatSessionManager mSessionManager;
    private ConnectionConfiguration mConfig;

    // True if we are in the process of reconnecting.  Reconnection is retried once per heartbeat.
    // Synchronized by executor thread.
    private boolean mNeedReconnect;

    private boolean mRetryLogin;
    private ThreadPoolExecutor mExecutor;

    private ProxyInfo mProxyInfo = null;

    private long mAccountId = -1;
    private long mProviderId = -1;
    private String mPasswordTemp;

    private boolean mIsGoogleAuth = false;
    
    private final static String TRUSTSTORE_TYPE = "BKS";
    private final static String TRUSTSTORE_PATH = "debiancacerts.bks";
    private final static String TRUSTSTORE_PASS = "changeit";
    private final static String KEYMANAGER_TYPE = "X509";
    private final static String SSLCONTEXT_TYPE = "TLS";

    private X509TrustManager mTrustManager;
    //private StrongTrustManager mStrongTrustManager;
    
    private SSLContext sslContext;
    
    private KeyStore ks = null;
    private KeyManager[] kms = null;
    private Context aContext;

    private final static String IS_GOOGLE = "google";

    private final static int SOTIMEOUT = 15000;

    private PacketCollector mPingCollector;
    private String mUsername;
    private String mPassword;
    private String mResource;
    private int mPriority;

    private int mGlobalId;
    private static int mGlobalCount;
    
    private final Random rndForTorCircuits = new Random();
    
    // Maintains a sequence counting up to the user configured heartbeat interval
    private int heartbeatSequence = 0;
    

    LinkedBlockingQueue<String> qAvatar = new LinkedBlockingQueue <String>();


    public XmppConnection(Context context) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        super(context);

        synchronized (XmppConnection.class) {
            mGlobalId = mGlobalCount++;
        }

        aContext = context;

        //mStrongTrustManager = new StrongTrustManager(aContext);
        //mStrongTrustManager.setNotifyVerificationSuccess(false);
        //mStrongTrustManager.setNotifyVerificationFail(false);
        
        //setup SSL managers
        SmackConfiguration.setPacketReplyTimeout(SOTIMEOUT);

        // Create a single threaded executor.  This will serialize actions on the underlying connection.
        createExecutor();

        addProviderManagerExtensions();
        
        XmppStreamHandler.addExtensionProviders();
        DeliveryReceipts.addExtensionProviders();

        ServiceDiscoveryManager.setIdentityName("Gibberbot");
        ServiceDiscoveryManager.setIdentityType("phone");

        mUser = makeUser();
    }

    Contact makeUser() {
        ContentResolver contentResolver = mContext.getContentResolver();
        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                contentResolver, mProviderId, false, null);
        String userName = Imps.Account.getUserName(contentResolver, mAccountId);
        String domain = providerSettings.getDomain();
        String xmppName = userName + '@' + domain;
        providerSettings.close();
        
        return new Contact(new XmppAddress(userName, xmppName), xmppName);
    }

    private void createExecutor() {
        mExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    private boolean execute(Runnable runnable) {
        try {
            mExecutor.execute(runnable);
        } catch (RejectedExecutionException ex) {
            return false;
        }
        return true;
    }

    // Execute a runnable only if we are idle
    private boolean executeIfIdle(Runnable runnable) {
        if (mExecutor.getActiveCount() + mExecutor.getQueue().size() == 0) {
            return execute(runnable);
        }
        return false;
    }

    // This runs in executor thread, and since there is only one such thread, we will definitely
    // succeed in shutting down the executor if we get here.
    public void join() {
        final ExecutorService executor = mExecutor;
        mExecutor = null;
        // This will send us an interrupt, which we will ignore.  We will terminate
        // anyway after the caller is done.  This also drains the executor queue.
        if (executor != null)
            executor.shutdownNow();
    }

    // For testing
    boolean joinGracefully() throws InterruptedException {
        final ExecutorService executor = mExecutor;
        mExecutor = null;
        // This will send us an interrupt, which we will ignore.  We will terminate
        // anyway after the caller is done.  This also drains the executor queue.
        if (executor != null) {
            executor.shutdown();
            return executor.awaitTermination(1, TimeUnit.SECONDS);
        }
        
        return false;
    }

    public void sendPacket(final org.jivesoftware.smack.packet.Packet packet) {
        execute(new Runnable() {
            @Override
            public void run() {
                if (mConnection == null) {
                    Log.w(TAG, "postponed packet to " + packet.getTo()
                               + " because we are not connected");
                    postpone(packet);
                    return;
                }
                try {
                    mConnection.sendPacket(packet);
                } catch (IllegalStateException ex) {
                    postpone(packet);
                    Log.w(TAG, "postponed packet to " + packet.getTo()
                            + " because socket is disconnected");
                }
            }
        });
    }


    void postpone(final org.jivesoftware.smack.packet.Packet packet) {
        if (packet instanceof org.jivesoftware.smack.packet.Message) {
            ChatSession session = findOrCreateSession(parseAddressBase(packet.getTo()));
            session.onMessagePostponed(packet.getPacketID());
        }
    }
    
    /*
    private void loadVCardsAsync ()
    {
     // Using an AsyncTask to load the slow images in a background thread
        new AsyncTask<String, Void, String>() {
            
            @Override
            protected String doInBackground(String... params) {
                loadVCards();
                return "";
              }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
               
            }
        }.execute("");
    }
    
    private void loadVCards ()
    {
        String jid = null;
        ContentResolver resolver = mContext.getContentResolver();
        
      
        try
        {
            while ((jid = qAvatar.poll(1000, TimeUnit.MILLISECONDS)) != null)
            {
        
                loadVCard (resolver, jid);
               
               
            }
        }
        catch (Exception e) {}
       
        
        
    };*/
    
    private boolean loadVCard (ContentResolver resolver, String jid, boolean forceLoad)
    {
        try {
            
            
            if (forceLoad || (!DatabaseUtils.hasAvatarContact(resolver,  Imps.Avatars.CONTENT_URI, jid)))
            {
                debug(ImApp.LOG_TAG, "loading vcard for: " + jid);
                
                VCard vCard = new VCard();
                
                // FIXME synchronize this to executor thread
              
                    vCard.load(mConnection, jid);
               

                // If VCard is loaded, then save the avatar to the personal folder.
                String avatarHash = vCard.getAvatarHash();
                
                if (avatarHash != null)
                {
                    byte[] avatarBytes = vCard.getAvatar();
                    
                    if (avatarBytes != null)
                    {
                      
                        debug(ImApp.LOG_TAG, "found avatar image in vcard for: " + jid);
                        
                        DatabaseUtils.insertAvatarBlob(resolver, Imps.Avatars.CONTENT_URI, mProviderId, mAccountId, avatarBytes, avatarHash, jid);
                        
                        // int providerId, int accountId, byte[] data, String hash,String contact
                        return true;
                    }
                }
          
            }
            
        } catch (XMPPException e) {
            
            Log.d(ImApp.LOG_TAG,"err loading vcard",e);
            
            if (e.getStreamError() != null)
            {
                String streamErr = e.getStreamError().getCode();
                
                if (streamErr != null && (streamErr.contains("404") || streamErr.contains("503")))
                    {
                       return false;
                    }
            }
          
        }
        
        return false;
    }

    @Override
    protected void doUpdateUserPresenceAsync(Presence presence) {
        org.jivesoftware.smack.packet.Presence packet = makePresencePacket(presence);

        
        sendPacket(packet);
        mUserPresence = presence;
        notifyUserPresenceUpdated();
    }

    private org.jivesoftware.smack.packet.Presence makePresencePacket(Presence presence) {
        String statusText = presence.getStatusText();
        Type type = Type.available;
        Mode mode = Mode.available;
        int priority = mPriority;
        final int status = presence.getStatus();
        if (status == Presence.AWAY) {
            priority = 10;
            mode = Mode.away;
        } else if (status == Presence.IDLE) {
            priority = 15;
            mode = Mode.away;
        } else if (status == Presence.DO_NOT_DISTURB) {
            priority = 5;
            mode = Mode.dnd;
        } else if (status == Presence.OFFLINE) {
            priority = 0;
            type = Type.unavailable;
            statusText = "Offline";
        }

        // The user set priority is the maximum allowed
        if (priority > mPriority)
            priority = mPriority;

        org.jivesoftware.smack.packet.Presence packet = new org.jivesoftware.smack.packet.Presence(
                type, statusText, priority, mode);
        return packet;
    }

    @Override
    public int getCapability() {
        
        return ImConnection.CAPABILITY_SESSION_REESTABLISHMENT & ImConnection.CAPABILITY_GROUP_CHAT;
    }

    private XmppChatGroupManager mChatGroupManager = null;
    
    @Override
    public synchronized ChatGroupManager getChatGroupManager() {
       
        if (mChatGroupManager == null)
            mChatGroupManager = new XmppChatGroupManager();
        
        return mChatGroupManager;
    }

    public class XmppChatGroupManager extends ChatGroupManager
    {

        private Hashtable<String,MultiUserChat> mMUCs = new Hashtable<String,MultiUserChat>();
        
        public MultiUserChat getMultiUserChat (String chatRoomJid)
        {
            return mMUCs.get(chatRoomJid);
        }
        
        @Override
        public void createChatGroupAsync(String chatRoomJid) {
           
            RoomInfo roomInfo = null;

            Address address = new XmppAddress (chatRoomJid);
            
            try
            {
                roomInfo = MultiUserChat.getRoomInfo(mConnection, chatRoomJid);
            }
            catch (Exception e)
            {
                //who knows?
            }
            
            
            if (roomInfo == null)
            {
                //should be room@server
                String[] parts = chatRoomJid.split("@");
                String room = parts[0];
                String server = parts[1];
                String nickname = mUser.getName().split("@")[0];
                
                try {
                    
                    // Create a MultiUserChat using a Connection for a room
                    MultiUserChat muc = new MultiUserChat(mConnection, chatRoomJid);
    
                    // Create the room
                    muc.create(nickname);
                    
                    Form form = muc.getConfigurationForm();
                    Form submitForm = form.createAnswerForm();
                    for (Iterator fields = form.getFields();fields.hasNext();){
                      FormField field = (FormField) fields.next();
                        if(!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable()!= null){
                            submitForm.setDefaultAnswer(field.getVariable());
                        }
                    }               
                    submitForm.setAnswer("muc#roomconfig_publicroom", true);
                    muc.sendConfigurationForm(submitForm);
                    muc.join(nickname);
                    
                    ChatGroup chatGroup = new ChatGroup(address,room,this);
                    mGroups.put(address.getAddress(), chatGroup);                    
                    mMUCs.put(chatRoomJid, muc);
                    
                } catch (XMPPException e) {
                  
                    Log.e(ImApp.LOG_TAG,"error creating MUC",e);
                }
            }
            else
            {
                joinChatGroupAsync(address);
            }
            
        }

        @Override
        public void deleteChatGroupAsync(ChatGroup group) {
            
            String chatRoomJid = group.getAddress().getAddress();
         
            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);
             
                try {
                    muc.destroy("", null);
                    
                    mMUCs.remove(chatRoomJid);
                    
                } catch (XMPPException e) {
                    Log.e(ImApp.LOG_TAG,"error destroying MUC",e);
                }
                
            }
            
        }

        @Override
        protected void addGroupMemberAsync(ChatGroup group, Contact contact) {
            // TODO Auto-generated method stub
            
            
            
        }

        @Override
        protected void removeGroupMemberAsync(ChatGroup group, Contact contact) {
            // TODO Auto-generated method stub
            
            
        }

        @Override
        public void joinChatGroupAsync(Address address) {
            
           String chatRoomJid = address.getAddress();
           String[] parts = chatRoomJid.split("@");
           String room = parts[0];
           String server = parts[1];
           String nickname = mUser.getName().split("@")[0];
            
           try {
                              
               // Create a MultiUserChat using a Connection for a room
               MultiUserChat muc = new MultiUserChat(mConnection, chatRoomJid);

               // Create the room
               muc.join(nickname);
               
               ChatGroup chatGroup = new ChatGroup(address,room,this);
               mGroups.put(address.getAddress(), chatGroup);               
               mMUCs.put(chatRoomJid, muc);
               
               
               
            } catch (XMPPException e) {
                Log.e(ImApp.LOG_TAG,"error joining MUC",e);
            }
                
        }

        @Override
        public void leaveChatGroupAsync(ChatGroup group) {
            String chatRoomJid = group.getAddress().getAddress();
            
            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);               
                muc.leave();                
                mMUCs.remove(chatRoomJid);
                
            }
            
        }

        @Override
        public void inviteUserAsync(ChatGroup group, Contact invitee) {
            
            String chatRoomJid = group.getAddress().getAddress();
            
            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);               
            
                String reason = ""; //no reason for now
                muc.invite(invitee.getAddress().getAddress(),reason);
                
            }
            
        }

        @Override
        public void acceptInvitationAsync(Invitation invitation) {
            
            Address addressGroup = invitation.getGroupAddress();
            
            joinChatGroupAsync (addressGroup);
           
        }

        @Override
        public void rejectInvitationAsync(Invitation invitation) {
         
            Address addressGroup = invitation.getGroupAddress();
            
            String reason = ""; // no reason for now
            
            MultiUserChat.decline(mConnection, addressGroup.getAddress(),invitation.getSender().getAddress(),reason);            
             
            
        }
        
    };
    
    @Override
    public synchronized ChatSessionManager getChatSessionManager() {

        if (mSessionManager == null)
            mSessionManager = new XmppChatSessionManager();

        return mSessionManager;
    }

    @Override
    public synchronized XmppContactList getContactListManager() {

        if (mContactListManager == null)
            mContactListManager = new XmppContactList();

        return mContactListManager;
    }

    @Override
    public Contact getLoginUser() {
        return mUser;
    }

    @Override
    public Map<String, String> getSessionContext() {
        // Empty state for now (but must have at least one key)
        return Collections.singletonMap("state", "empty");
    }

    @Override
    public int[] getSupportedPresenceStatus() {
        return new int[] { Presence.AVAILABLE, Presence.AWAY, Presence.IDLE, Presence.OFFLINE,
                          Presence.DO_NOT_DISTURB, };
    }

    @Override
    public void loginAsync(long accountId, String passwordTemp, long providerId, boolean retry) {
        mAccountId = accountId;
        mPasswordTemp = passwordTemp;
        mProviderId = providerId;
        mRetryLogin = retry;

        mUser = makeUser();
        
        execute(new Runnable() {
            @Override
            public void run() {
                do_login();
            }
        });
    }

    // Runs in executor thread
    private void do_login() {
        
        if (mConnection != null) {
            setState(getState(), new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER,
                    "still trying..."));
            return;
        }
        
        ContentResolver contentResolver = mContext.getContentResolver();
        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                contentResolver, mProviderId, false, null);
        
        // providerSettings is closed in initConnection();
        String userName = Imps.Account.getUserName(contentResolver, mAccountId);
        String password = Imps.Account.getPassword(contentResolver, mAccountId);
        
        boolean createAccount = false;

        
        String defaultStatus = null;
        
        if (mPasswordTemp != null)
            password = mPasswordTemp;

        mNeedReconnect = true;
        setState(LOGGING_IN, null);
        
        mUserPresence = new Presence(Presence.AVAILABLE, defaultStatus, Presence.CLIENT_TYPE_MOBILE);

        try {
            if (userName.length() == 0)
                throw new XMPPException("empty username not allowed");
            initConnection(userName, password, providerSettings);
        } catch (Exception e) {
           debug(TAG, "login failed: " + e.getLocalizedMessage());
            mConnection = null;
            ImErrorInfo info = new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, e.getMessage());
            
            if (e == null || e.getMessage() == null) {
                debug(TAG, "NPE: " + e.getMessage());
                Log.e(TAG,"login error",e);
                info = new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "unknown error");
                disconnected(info);
                mRetryLogin = false;
            } else if (e.getMessage().contains("not-authorized")
                       || e.getMessage().contains("authentication failed")) {
                
                if (mIsGoogleAuth && password.contains(GTalkOAuth2.NAME))
                {
                    debug (TAG, "google failed; may need to refresh");

                    password = refreshGoogleToken (userName, password,providerSettings.getDomain());
                     
                    mRetryLogin = true;
                    setState(LOGGING_IN, info);

                }
                else
                {
                    debug(TAG, "not authorized - will not retry");
                    info = new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "invalid user/password");
                    disconnected(info);
                    mRetryLogin = false;
                }
            } else if (mRetryLogin) {
                debug(TAG, "will retry");
                setState(LOGGING_IN, info);
            } else {
                debug(TAG, "will not retry");
                mConnection = null;
                disconnected(info);
            }

            return;

        } finally {
            mNeedReconnect = false;
        }

        // TODO should we really be using the same name for both address and name?
        setState(LOGGED_IN, null);
        debug(TAG, "logged in");
        
      

    }
    
    private String refreshGoogleToken (String userName, String oldPassword, String domain)
    {
      //invalidate our old one, that is locally cached
        AccountManager.get(mContext.getApplicationContext()).invalidateAuthToken("com.google", oldPassword.split(":")[1]);
    
        //request a new one
        String password = GTalkOAuth2.getGoogleAuthToken(userName + '@' + domain, mContext.getApplicationContext());

        password = GTalkOAuth2.NAME + ':' + password;
        //now store the new one, for future use until it expires
        final long accountId = ImApp.insertOrUpdateAccount(mContext.getContentResolver(), mProviderId, userName,
               password );
        
        return password;

    }

    // TODO shouldn't setProxy be handled in Imps/settings?
    public void setProxy(String type, String host, int port) {
        if (type == null) {
            mProxyInfo = ProxyInfo.forNoProxy();
        } else {
            
            ProxyInfo.ProxyType pType = ProxyType.valueOf(type);
            String username = null;
            String password = null;
            
            if (type.equals(TorProxyInfo.PROXY_TYPE) //socks5
                    && host.equals(TorProxyInfo.PROXY_HOST) //127.0.0.1
                    && port == TorProxyInfo.PROXY_PORT) //9050
            {
                //if the proxy is for Orbot/Tor then generate random usr/pwd to isolate Tor streams
                username = rndForTorCircuits.nextInt(100000)+"";
                password = rndForTorCircuits.nextInt(100000)+"";
                
            }
            
            mProxyInfo = new ProxyInfo(pType, host, port, username, password);
            
        }
    }

    public void initConnection(MyXMPPConnection connection, Contact user, int state) {
        mConnection = connection;
        mUser = user;
        setState(state, null);
    }

    // Runs in executor thread
    private void initConnection(String userName, String password,
            Imps.ProviderSettings.QueryMap providerSettings) throws Exception {
        if (Debug.DEBUG_ENABLED) {
            Debug.onConnectionStart();
        }
     
        boolean allowPlainAuth = providerSettings.getAllowPlainAuth();
        boolean requireTls = providerSettings.getRequireTls();
        boolean doDnsSrv = providerSettings.getDoDnsSrv();
        boolean tlsCertVerify = providerSettings.getTlsCertVerify();

        boolean useSASL = true;//!allowPlainAuth;

        mIsGoogleAuth = password.startsWith(GTalkOAuth2.NAME);

        String domain = providerSettings.getDomain();
        String requestedServer = providerSettings.getServer();
        if ("".equals(requestedServer))
            requestedServer = null;
        String xmppResource = providerSettings.getXmppResource();
        mPriority = providerSettings.getXmppResourcePrio();
        int serverPort = providerSettings.getPort();


        if (mIsGoogleAuth)
        {
            password = refreshGoogleToken(userName, password, domain);
        }
        
        
        String server = requestedServer;

        providerSettings.close(); // close this, which was opened in do_login()

        debug(TAG, "TLS required? " + requireTls);
        debug(TAG, "cert verification? " + tlsCertVerify);

        if (providerSettings.getUseTor()) {
            setProxy(TorProxyInfo.PROXY_TYPE, TorProxyInfo.PROXY_HOST,
                    TorProxyInfo.PROXY_PORT);
        }
        else
        {
            setProxy(null, null, -1);
        }
        
        if (mProxyInfo == null)
            mProxyInfo = ProxyInfo.forNoProxy();

        // If user did not specify a server, and SRV requested then lookup SRV
        if (doDnsSrv && requestedServer == null) {

            //java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
            //java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");

            debug(TAG, "(DNS SRV) resolving: " + domain);
            DNSUtil.HostAddress srvHost = DNSUtil.resolveXMPPDomain(domain);
            server = srvHost.getHost();
            if (serverPort <= 0) {
                // If user did not override port, use port from SRV record
                serverPort = srvHost.getPort();
            }
            debug(TAG, "(DNS SRV) resolved: " + domain + "=" + server + ":" + serverPort);

        }
        
        if (serverPort == 0)
            serverPort = 5222;

        // No server requested and SRV lookup wasn't requested or returned nothing - use domain
        if (server == null) {
            debug(TAG, "(use domain) ConnectionConfiguration(" + domain + ", " + serverPort + ", "
                       + domain + ", mProxyInfo);");

            if (mProxyInfo == null)
                mConfig = new ConnectionConfiguration(domain, serverPort);
            else
                mConfig = new ConnectionConfiguration(domain, serverPort, mProxyInfo);
            
            server = domain;

        } else {
            debug(TAG, "(use server) ConnectionConfiguration(" + server + ", " + serverPort + ", "
                       + domain + ", mProxyInfo);");

            if (mProxyInfo == null)
                mConfig = new ConnectionConfiguration(server, serverPort, domain);
            else
                mConfig = new ConnectionConfiguration(server, serverPort, domain, mProxyInfo);
        }

        
        mConfig.setDebuggerEnabled(Debug.DEBUG_ENABLED);
        mConfig.setSASLAuthenticationEnabled(useSASL);

        // Android has no support for Kerberos or GSSAPI, so disable completely
        SASLAuthentication.unregisterSASLMechanism("KERBEROS_V4");
        SASLAuthentication.unregisterSASLMechanism("GSSAPI");

        //add gtalk auth in
        
        
        SASLAuthentication.supportSASLMechanism("PLAIN", 1);
        SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 2);

        if (requireTls) { 
            
            mConfig.setSecurityMode(SecurityMode.required);

        } else {
            // if it finds a cert, still use it, but don't check anything since 
            // TLS errors are not expected by the user
            mConfig.setSecurityMode(SecurityMode.enabled);

            if (!allowPlainAuth)
                SASLAuthentication.unsupportSASLMechanism("PLAIN");

        }

        if (mIsGoogleAuth)
        {
            mConfig.setSASLAuthenticationEnabled(true);
            password = password.split(":")[1];
            SASLAuthentication.registerSASLMechanism( GTalkOAuth2.NAME, GTalkOAuth2.class );
            SASLAuthentication.supportSASLMechanism( GTalkOAuth2.NAME, 0);     
        }
        else
        {
            SASLAuthentication.unregisterSASLMechanism( GTalkOAuth2.NAME);
            SASLAuthentication.unsupportSASLMechanism( GTalkOAuth2.NAME);     
        }
                
        mConfig.setVerifyChainEnabled(true);
        mConfig.setVerifyRootCAEnabled(true);
        mConfig.setExpiredCertificatesCheckEnabled(true);
        mConfig.setNotMatchingDomainCheckEnabled(true);
        mConfig.setSelfSignedCertificateEnabled(false);

        mConfig.setTruststoreType(TRUSTSTORE_TYPE);
        mConfig.setTruststorePath(TRUSTSTORE_PATH);
        mConfig.setTruststorePassword(TRUSTSTORE_PASS);
        
        // Per XMPP specs, cert must match domain, not SRV lookup result.  Otherwise, DNS spoofing
        // can enable MITM.
        initSSLContext(domain, requestedServer, mConfig);

        // Don't use smack reconnection - not reliable
        mConfig.setReconnectionAllowed(false);
        mConfig.setSendPresence(true);
        
        mConfig.setRosterLoadedAtLogin(true);
        

        mConnection = new MyXMPPConnection(mConfig);

        //debug(TAG,"is secure connection? " + mConnection.isSecureConnection());
        //debug(TAG,"is using TLS? " + mConnection.isUsingTLS());

        mConnection.addPacketListener(new PacketListener() {

            @Override
            public void processPacket(Packet packet) {
                debug(TAG, "receive message");
                org.jivesoftware.smack.packet.Message smackMessage = (org.jivesoftware.smack.packet.Message) packet;
                String address = parseAddressBase(smackMessage.getFrom());
                String body = smackMessage.getBody();
            
                DeliveryReceipts.DeliveryReceipt dr = (DeliveryReceipts.DeliveryReceipt) smackMessage
                        .getExtension("received", DeliveryReceipts.NAMESPACE);
                
                if (dr != null) {
                    
                    debug(TAG, "got delivery receipt for " + dr.getId());
                    ChatSession session = findOrCreateSession(address);
                    session.onMessageReceipt(dr.getId());
                } 
                
                if (body != null)
                {

                  ChatSession session = findOrCreateSession(address);
                   
                  if (!smackMessage.getFrom().equals(mUser.getAddress().getAddress()))
                  {
                        Message rec = new Message(body);
                        rec.setTo(mUser.getAddress());
                        rec.setFrom(new XmppAddress(smackMessage.getFrom()));
                        rec.setDateTime(new Date());
    
                        rec.setType(Imps.MessageType.INCOMING);
                        
                        smackMessage.getTo();
                        smackMessage.getThread();
                        smackMessage.getType();
                        
                        boolean good = session.onReceiveMessage(rec);
                        
                        if (smackMessage.getExtension("request", DeliveryReceipts.NAMESPACE) != null) {
                            if (good) {
                                debug(TAG, "sending delivery receipt");
                            // got XEP-0184 request, send receipt
                                sendReceipt(smackMessage);
                                session.onReceiptsExpected();
                                                    } else {
                            debug(TAG, "not sending delivery receipt due to processing error");
                            }
                            
                         } else if (!good) {
                                 debug(TAG, "packet processing error");
                         }
                  }
                }
                    
            }
        }, new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class));

        mConnection.addPacketListener(new PacketListener() {

            @Override
            public void processPacket(Packet packet) {

                org.jivesoftware.smack.packet.Presence presence = (org.jivesoftware.smack.packet.Presence) packet;
                String address = parseAddressBase(presence.getFrom());
                String name = parseAddressName(presence.getFrom());
                Contact contact = findOrCreateContact(name, address);

                if (presence.getType() == Type.subscribe) {
                    debug(TAG, "sub request from " + address);
                    mContactListManager.getSubscriptionRequestListener().onSubScriptionRequest(
                            contact);
                } else {
                    int type = parsePresence(presence);

                    
                    Presence p = new Presence(type, presence.getStatus(), null, null,
                            Presence.CLIENT_TYPE_DEFAULT);
                    
                    
                    String from = presence.getFrom();
                    String resource = null;
                    if (from != null && from.lastIndexOf("/") > 0) {
                        resource = from.substring(from.lastIndexOf("/") + 1);
                       
                        if (resource.indexOf('.')!=-1)
                            resource = resource.substring(0,resource.indexOf('.'));

                        p.setResource(resource);
                    }
                    
                    contact.setPresence(p);
                    
                    /*
                    Message rec = new Message(presence.getStatus());
                    rec.setTo(mUser.getAddress());
                    rec.setFrom(new XmppAddress(address, name));
                    rec.setDateTime(new Date());

                    rec.setType(Imps.MessageType.STATUS);
                    
                    ChatSession session = findOrCreateSession(address);
                    
                    boolean good = session.onReceiveMessage(rec);
                    */
                    

                }
            }
        }, new PacketTypeFilter(org.jivesoftware.smack.packet.Presence.class));

        ConnectionListener connectionListener = new ConnectionListener() {
            /**
             * Called from smack when connect() is fully successful
             * 
             * This is called on the executor thread while we are in reconnect()
             */
            @Override
            public void reconnectionSuccessful() {
                if (mStreamHandler == null || !mStreamHandler.isResumePending()) {
                    debug(TAG, "Reconnection success");
                    onReconnectionSuccessful();
                } else {
                    debug(TAG, "Ignoring reconnection callback due to pending resume");
                }
            }

            @Override
            public void reconnectionFailed(Exception e) {
                // We are not using the reconnection manager
                throw new UnsupportedOperationException();
            }

            @Override
            public void reconnectingIn(int seconds) {
                // We are not using the reconnection manager
                throw new UnsupportedOperationException();
            }

            @Override
            public void connectionClosedOnError(final Exception e) {
                /*
                 * This fires when:
                 * - Packet reader or writer detect an error
                 * - Stream compression failed
                 * - TLS fails but is required
                 * - Network error
                 * - We forced a socket shutdown
                 */
                debug(TAG, "reconnect on error: " + e.getMessage());
                if (e.getMessage().contains("conflict")) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            disconnect();
                            disconnected(new ImErrorInfo(ImpsErrorInfo.ALREADY_LOGGED,
                                    "logged in from another location"));
                        }
                    });
                } else if (!mNeedReconnect) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            if (getState() == LOGGED_IN)
                                setState(LOGGING_IN,
                                        new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
                            maybe_reconnect();
                        }
                    });
                }
            }

            @Override
            public void connectionClosed() {

                debug(TAG, "connection closed");

                /*
                 * This can be called in these cases:
                 * - Connection is shutting down
                 *   - because we are calling disconnect
                 *     - in do_logout
                 *     
                 * - NOT
                 *   - because server disconnected "normally"
                 *   - we were trying to log in (initConnection), but are failing
                 *   - due to network error
                 *   - due to login failing
                 */
            }
        };
        
        mConnection.addConnectionListener(connectionListener);

        mStreamHandler = new XmppStreamHandler(mConnection, connectionListener);
        
        mConnection.connect();

        if (server != null && server.contains(IS_GOOGLE)) {
            this.mUsername = userName + '@' + domain;
        } else {
            this.mUsername = userName;
        }

        this.mPassword = password;
        this.mResource = xmppResource;

        //disable compression based on statement by Ge0rg
        mConfig.setCompressionEnabled(false);
       
	/* 
        try
        {
            if (createAccount && mConnection.getAccountManager().supportsAccountCreation())
            {
                mConnection.getAccountManager().createAccount(mUsername, mPassword);
            
            }
        }
        catch (XMPPException e)
        {
            Log.w(TAG,"error creating account",e);
        }
	*/
            
        mConnection.login(mUsername, mPassword, mResource);
        
        mStreamHandler.notifyInitialLogin();
        initServiceDiscovery();

        sendPresencePacket();

        Roster roster = mConnection.getRoster();
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
        getContactListManager().listenToRoster(roster);
        

    }

    private void sendPresencePacket() {
        org.jivesoftware.smack.packet.Presence presence = makePresencePacket(mUserPresence);
        mConnection.sendPacket(presence);
    }

    public void sendReceipt(org.jivesoftware.smack.packet.Message msg) {
        debug(TAG, "sending XEP-0184 ack to " + msg.getFrom() + " id=" + msg.getPacketID());
        org.jivesoftware.smack.packet.Message ack = new org.jivesoftware.smack.packet.Message(
                msg.getFrom(), msg.getType());
        ack.addExtension(new DeliveryReceipts.DeliveryReceipt(msg.getPacketID()));
        mConnection.sendPacket(ack);
    }

    private void initSSLContext(String domain, String requestedServer,
            ConnectionConfiguration config) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException  {

        /*
        ks = KeyStore.getInstance(TRUSTSTORE_TYPE);
        
        try {
            ks.load(new FileInputStream(TRUSTSTORE_PATH), TRUSTSTORE_PASS.toCharArray());
        } catch (Exception e) {
            ks = null;
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEYMANAGER_TYPE);
        try {
            kmf.init(ks, TRUSTSTORE_PASS.toCharArray());
            kms = kmf.getKeyManagers();
        } catch (NullPointerException npe) {
            kms = null;
        }
*/
        
        sslContext = SSLContext.getInstance(SSLCONTEXT_TYPE);
        
        //mStrongTrustManager.setDomain(domain);
        //mStrongTrustManager.setServer(requestedServer);
        

        String[] PINLIST = {XmppCertPins.TALKGOOGLE, XmppCertPins.DUKGO, XmppCertPins.CHATFACEBOOK, XmppCertPins.JABBERCCCDE, XmppCertPins.BINARYPARADOX};
        PinningTrustManager trustPinning = new PinningTrustManager(SystemKeyStore.getInstance(aContext),PINLIST,
                                                        0);
        
        mTrustManager = new MemorizingTrustManager(aContext, trustPinning, null);

        SecureRandom mSecureRandom = new java.security.SecureRandom();
        
        sslContext.init(null, new javax.net.ssl.TrustManager[] { mTrustManager },
                mSecureRandom);

        config.setCustomSSLContext(sslContext);
        config.setCallbackHandler(this);

    }

    /*
     * this does nothing
    void sslCertificateError() {        
        disconnect();
        disconnected(new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER,
                "SSL Certificate Error"));
    }*/

    protected static int parsePresence(org.jivesoftware.smack.packet.Presence presence) {
        int type = Presence.AVAILABLE;
        Mode rmode = presence.getMode();
        Type rtype = presence.getType();
    
        if (rmode == Mode.away || rmode == Mode.xa)
            type = Presence.AWAY;
        else if (rmode == Mode.dnd)
            type = Presence.DO_NOT_DISTURB;
        else if (rtype == Type.unavailable || rtype == Type.error)
            type = Presence.OFFLINE;
    
        return type;
    }

    // We must release resources here, because we will not be reused
    void disconnected(ImErrorInfo info) {
        debug(TAG, "disconnected");
        join();
        setState(DISCONNECTED, info);
    }

    protected static String parseAddressBase(String from) {
        return from.replaceFirst("/.*", "");
    }

    protected static String parseAddressName(String from) {
        return from.replaceFirst("@.*", "");
    }

    @Override
    public void logoutAsync() {
        execute(new Runnable() {
            @Override
            public void run() {
                do_logout();
            }
        });
    }

    // Force immediate logout
    public void logout() {
        do_logout();
    }

    // Usually runs in executor thread, unless called from logout()
    private void do_logout() {
        Log.w(TAG, "logout");
        setState(LOGGING_OUT, null);
        disconnect();
        disconnected(null);
    }

    // Runs in executor thread
    private void disconnect() {

        clearPing();
        XMPPConnection conn = mConnection;
        mConnection = null;
        try {
            conn.disconnect();
        } catch (Throwable th) {
            // ignore
        }
        mNeedReconnect = false;
        mRetryLogin = false;
    }

    @Override
    public void reestablishSessionAsync(Map<String, String> sessionContext) {
        execute(new Runnable() {
            @Override
            public void run() {
                if (getState() == SUSPENDED) {
                    debug(TAG, "reestablish");
                    setState(LOGGING_IN, null);
                    maybe_reconnect();
                }
            }
        });
    }

    @Override
    public void suspend() {
        execute(new Runnable() {
            @Override
            public void run() {
                debug(TAG, "suspend");
                setState(SUSPENDED, null);
                mNeedReconnect = false;
                clearPing();
                // Do not try to reconnect anymore if we were asked to suspend
                mStreamHandler.quickShutdown();
            }
        });
    }

    private ChatSession findOrCreateSession(String address) {
        ChatSession session = mSessionManager.findSession(address);

        if (session == null) {
            Contact contact = findOrCreateContact(parseAddressName(address), address);
            session = mSessionManager.createChatSession(contact);
        }
        return session;
    }

    Contact findOrCreateContact(String name, String address) {
        Contact contact = mContactListManager.getContact(address);
        if (contact == null) {
            contact = makeContact(name, address);
        }

        return contact;
    }

    private static Contact makeContact(String name, String address) {
        Contact contact = new Contact(new XmppAddress(name, address), name);

        return contact;
    }

    private final class XmppChatSessionManager extends ChatSessionManager {
        @Override
        public void sendMessageAsync(ChatSession session, Message message) {
            
            
            String chatRoomJid = message.getTo().getAddress();
            MultiUserChat muc = ((XmppChatGroupManager)getChatGroupManager()).getMultiUserChat(chatRoomJid);
            
            if (muc != null)
            {
                org.jivesoftware.smack.packet.Message msg = muc.createMessage();
               
                msg.setBody(message.getBody());
                
                message.setID(msg.getPacketID());
                sendPacket(msg);
            }
            else
            {
                org.jivesoftware.smack.packet.Message msg = new org.jivesoftware.smack.packet.Message(
                        message.getTo().getAddress(), org.jivesoftware.smack.packet.Message.Type.chat);
                msg.addExtension(new DeliveryReceipts.DeliveryReceiptRequest());
                
                msg.setBody(message.getBody());
                
             //   debug(TAG, "sending packet ID " + msg.getPacketID());
                message.setID(msg.getPacketID());
                sendPacket(msg);
            }
        }

        ChatSession findSession(String address) {
            for (Iterator<ChatSession> iter = mSessions.iterator(); iter.hasNext();) {
                ChatSession session = iter.next();
                if (session.getParticipant().getAddress().getAddress().equals(address))
                    return session;
            }
            return null;
        }
        
    }
    

    public ChatSession findSession(String address) {
        return mSessionManager.findSession(address);
    }

    public ChatSession createChatSession(Contact contact) {
        return mSessionManager.createChatSession(contact);
    }

    public class XmppContactList extends ContactListManager {

        //private Hashtable<String, org.jivesoftware.smack.packet.Presence> unprocdPresence = new Hashtable<String, org.jivesoftware.smack.packet.Presence>();

        @Override
        protected void setListNameAsync(final String name, final ContactList list) {
            execute(new Runnable() {
                @Override
                public void run() {
                    do_setListName(name, list);
                }
            });
        }

        // Runs in executor thread
        private void do_setListName(String name, ContactList list) {
            debug(TAG, "set list name");
            mConnection.getRoster().getGroup(list.getName()).setName(name);
            notifyContactListNameUpdated(list, name);
        }

        @Override
        public String normalizeAddress(String address) {
            return address;
        }

        @Override
        public void loadContactListsAsync() {

            execute(new Runnable() {
                @Override
                public void run() {
                    do_loadContactLists();
                    
                }
            });

        }

        // For testing
        /*
        public void loadContactLists() {
            do_loadContactLists();
        }*/

        /**
         * Create new list of contacts from roster entries.
         * 
         * Runs in executor thread
         * 
         * @param entryIter iterator of roster entries to add to contact list
         * @param skipList list of contacts which should be omitted; new
         *            contacts are added to this list automatically
         * @return contacts from roster which were not present in skiplist.
         */
        private Collection<Contact> fillContacts(Collection<RosterEntry> entryIter,
                Set<String> skipList) {

            Roster roster = mConnection.getRoster();

            Collection<Contact> contacts = new ArrayList<Contact>();
            for (RosterEntry entry : entryIter) {
                
                String address = parseAddressBase(entry.getUser());

                /* Skip entries present in the skip list */
                if (skipList != null && !skipList.add(address))
                    continue;

                String name = entry.getName();
                if (name == null)
                    name = address;

                XmppAddress xaddress = new XmppAddress(name, address);
                
                org.jivesoftware.smack.packet.Presence presence = roster.getPresence(address);
                
                String status = presence.getStatus();
                String resource = null;
                
                Presence p = new Presence(parsePresence(presence), status,
                        null, null, Presence.CLIENT_TYPE_DEFAULT);
                
                String from = presence.getFrom();
                if (from != null && from.lastIndexOf("/") > 0) {
                    resource = from.substring(from.lastIndexOf("/") + 1);
                   
                    if (resource.indexOf('.')!=-1)
                        resource = resource.substring(0,resource.indexOf('.'));

                    p.setResource(resource);
                }
            
                Contact contact = mContactListManager.getContact(xaddress.getAddress());

                if (contact == null)
                    contact = new Contact(xaddress, xaddress.getScreenName());

                contact.setPresence(p);

                contacts.add(contact);


            }
            return contacts;
        }

        // Runs in executor thread
        private void do_loadContactLists() {
   
            debug(TAG, "load contact lists");

            if (mConnection == null)
                return;

            Roster roster = mConnection.getRoster();

            //Set<String> seen = new HashSet<String>();

            // This group will also contain all the unfiled contacts.  We will create it locally if it
            // does not exist.
            String generalGroupName = "Buddies";

            for (Iterator<RosterGroup> giter = roster.getGroups().iterator(); giter.hasNext();) {

                RosterGroup group = giter.next();

                debug(TAG, "loading group: " + group.getName() + " size:" + group.getEntryCount());

                Collection<Contact> contacts = fillContacts(group.getEntries(), null);

                if (group.getName().equals(generalGroupName) && roster.getUnfiledEntryCount() > 0) {
                    Collection<Contact> unfiled = fillContacts(roster.getUnfiledEntries(), null);
                    contacts.addAll(unfiled);
                }

                XmppAddress groupAddress = new XmppAddress(group.getName()); 
                ContactList cl = new ContactList(groupAddress, group.getName(), group
                        .getName().equals(generalGroupName), contacts, this);

                notifyContactListCreated(cl);

                notifyContactsPresenceUpdated(contacts.toArray(new Contact[contacts.size()]));
            }

            Collection<Contact> contacts;
            if (roster.getUnfiledEntryCount() > 0) {
                contacts = fillContacts(roster.getUnfiledEntries(), null);
            } else {
                contacts = new ArrayList<Contact>();
            }

            ContactList cl = getContactList(generalGroupName);

            // We might have already created the Buddies contact list above
            if (cl == null) {
                cl = new ContactList(mUser.getAddress(), generalGroupName, true, contacts, this);
                notifyContactListCreated(cl);

                notifyContactsPresenceUpdated(contacts.toArray(new Contact[contacts.size()]));
            }
            
            
            notifyContactListsLoaded();

        }

        /*
         * iterators through a list of contacts to see if there were any Presence
         * notifications sent before the contact was loaded
         */
        /*
        private void processQueuedPresenceNotifications (Collection<Contact> contacts)
        {
        	
        	Roster roster = mConnection.getRoster();
        	
        	//now iterate through the list of queued up unprocessed presence changes
        	for (Contact contact : contacts)
        	{
        		
        		String address = parseAddressBase(contact.getAddress().getFullName());
        	
        		org.jivesoftware.smack.packet.Presence presence = roster.getPresence(address);

        		if (presence != null)
        		{
        			debug(TAG, "processing queued presence: " + address + " - " + presence.getStatus());

        			unprocdPresence.remove(address);

        			contact.setPresence(new Presence(parsePresence(presence), presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT));
        			
        			Contact[] updatedContact = {contact};
        			notifyContactsPresenceUpdated(updatedContact);	
        		}
        		
        		

        	}
        }*/

        public void listenToRoster(final Roster roster) {

            roster.addRosterListener(rListener);
        }

        
        RosterListener rListener = new RosterListener() {

            		
            @Override
            public void presenceChanged(org.jivesoftware.smack.packet.Presence presence) {

                handlePresenceChanged(presence);

            }

            @Override
            public void entriesUpdated(Collection<String> addresses) {

                /*
                for (String address : addresses)
                    qAvatar.add(address);
                
                loadVCardsAsync();
                */
            }

            @Override
            public void entriesDeleted(Collection<String> addresses) {
               
            }

            @Override
            public void entriesAdded(Collection<String> addresses) {
                
                /*
                for (String address : addresses)
                    qAvatar.add(address);
                
               
                loadVCardsAsync();
                */
            }
        };

        private void handlePresenceChanged(org.jivesoftware.smack.packet.Presence presence) {
            
            
            String name = parseAddressName(presence.getFrom());
            String address = parseAddressBase(presence.getFrom());

            String status = presence.getStatus();
            String resource = null;
            
            String from = presence.getFrom();
            if (from != null && from.lastIndexOf("/") > 0) {
                resource = from.substring(from.lastIndexOf("/") + 1);
               
                if (resource.indexOf('.')!=-1)
                    resource = resource.substring(0,resource.indexOf('.'));
                
            }
            
            
            XmppAddress xaddress = new XmppAddress(name, address);
            
            if (mConnection == null)
                return;
            
            // Get presence from the Roster to handle priorities and such
            /*
            final Roster roster = mConnection.getRoster();
            if (roster != null) {
                presence = roster.getPresence(address);
            }
            int type = parsePresence(presence);
               */
            
            int type = parsePresence(presence);
            
            Contact contact = getContact(xaddress.getAddress());

            Presence p = new Presence(type, status, null, null,
                    Presence.CLIENT_TYPE_DEFAULT);
            p.setResource(resource);

            if (contact == null) {
                
                contact = new Contact(xaddress, xaddress.getScreenName());      
                
                debug(TAG, "got presence updated for NEW user: "
                           + contact.getAddress().getAddress() + " presence:" + type);
                //store the latest presence notification for this user in this queue
                //unprocdPresence.put(user, presence);

            } else {
                debug(TAG, "Got presence update for EXISTING user: "
                        + contact.getAddress().getAddress() + " presence:" + type);
              
            }

            contact.setPresence(p);

            Contact[] contacts = new Contact[] { contact };

            notifyContactsPresenceUpdated(contacts);
            
         //   loadVCard(mContext.getContentResolver(),contact.getAddress().getAddress());
            
            PacketExtension pe = presence.getExtension("x", NameSpace.VCARD_TEMP_X_UPDATE);
            if (pe != null) {
                DefaultPacketExtension dpe = (DefaultPacketExtension)pe;
                String hash = dpe.getValue("photo");
                
                if (hash != null)
                    loadVCard(mContext.getContentResolver(),contact.getAddress().getAddress(),true);
                
            }
        }

        @Override
        protected ImConnection getConnection() {
            return XmppConnection.this;
        }

        @Override
        protected void doRemoveContactFromListAsync(Contact contact, ContactList list) {
            // FIXME synchronize this to executor thread
            if (mConnection == null)
                return;
            Roster roster = mConnection.getRoster();
            String address = contact.getAddress().getAddress();
            try {
                RosterGroup group = roster.getGroup(list.getName());
                if (group == null) {
                    debug(TAG, "could not find group " + list.getName() + " in roster");
                    return;
                }
                RosterEntry entry = roster.getEntry(address);
                if (entry == null) {
                    debug(TAG, "could not find entry " + address + " in group " + list.getName());
                    return;
                }

                // Remove from Roster if this is the last group
                if (entry.getGroups().size() <= 1)
                    roster.removeEntry(entry);

                group.removeEntry(entry);
            } catch (XMPPException e) {
                debug(TAG, "remove entry failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
            org.jivesoftware.smack.packet.Presence response = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.unsubscribed);
            response.setTo(address);
            sendPacket(response);
            notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_REMOVED, contact);
        }

        @Override
        protected void doDeleteContactListAsync(ContactList list) {
            // TODO delete contact list
            debug(TAG, "delete contact list " + list.getName());
        }

        @Override
        protected void doCreateContactListAsync(String name, Collection<Contact> contacts,
                boolean isDefault) {
            // TODO create contact list
            debug(TAG, "create contact list " + name + " default " + isDefault);
        }

        @Override
        protected void doBlockContactAsync(String address, boolean block) {
            // TODO block contact

        }

        @Override
        protected void doAddContactToListAsync(String address, ContactList list) throws ImException {
            debug(TAG, "add contact to " + list.getName());
            org.jivesoftware.smack.packet.Presence response = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.subscribed);
            response.setTo(address);

            sendPacket(response);

            Roster roster = mConnection.getRoster();
            String[] groups = new String[] { list.getName() };
            try {
                final String name = parseAddressName(address);
                roster.createEntry(address, name, groups);

                // If contact exists locally, don't create another copy
                Contact contact = makeContact(name, address);
                if (!containsContact(contact))
                    notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_ADDED, contact);
                else
                    debug(TAG, "skip adding existing contact locally " + name);
            } catch (XMPPException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void declineSubscriptionRequest(String contact) {
            debug(TAG, "decline subscription");
            org.jivesoftware.smack.packet.Presence response = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.unsubscribed);
            response.setTo(contact);
            sendPacket(response);
            mContactListManager.getSubscriptionRequestListener().onSubscriptionDeclined(contact);
        }

        @Override
        public void approveSubscriptionRequest(String contact) {
            debug(TAG, "approve subscription");
            try {
                mContactListManager.doAddContactToListAsync(contact, getDefaultContactList());
            } catch (ImException e) {
                debug(TAG, "failed to add " + contact + " to default list");
            }
            mContactListManager.getSubscriptionRequestListener().onSubscriptionApproved(contact);
        }

        @Override
        public Contact createTemporaryContact(String address) {
            debug(TAG, "create temporary " + address);
            return makeContact(parseAddressName(address), address);
        }
    }

    public void sendHeartbeat(final long heartbeatInterval) {
        // Don't let heartbeats queue up if we have long running tasks - only
        // do the heartbeat if executor is idle.
        boolean success = executeIfIdle(new Runnable() {
            @Override
            public void run() {
                debug(TAG, "heartbeat state = " + getState());
                doHeartbeat(heartbeatInterval);
            }
        });

        if (!success) {
            debug(TAG, "failed to schedule heartbeat state = " + getState());
        }
    }

    // Runs in executor thread
    public void doHeartbeat(long heartbeatInterval) {
        heartbeatSequence++;
        
        if (mConnection == null && mRetryLogin) {
            debug(TAG, "reconnect with login");
            do_login();
        }

        if (mConnection == null)
            return;

        if (getState() == SUSPENDED) {
            debug(TAG, "heartbeat during suspend");
            return;
        }

        if (mNeedReconnect) {
            reconnect();
        } else if (!mConnection.isConnected() && getState() == LOGGED_IN) {
            // Smack failed to tell us about a disconnect
            Log.w(TAG, "reconnect on unreported state change");
            setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network disconnected"));
            force_reconnect();
        } else if (getState() == LOGGED_IN) {
            if (PING_ENABLED) {
                // Check ping on every heartbeat.  checkPing() will return true immediately if we already checked.
                if (!checkPing()) {
                    Log.w(TAG, "reconnect on ping failed");
                    setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network timeout"));
                    force_reconnect();
                } else {
                    // Send pings only at intervals configured by the user
                    if (heartbeatSequence >= heartbeatInterval) {
                        heartbeatSequence = 0;
                        debug(TAG, "ping");
                        sendPing();
                    }
                }
            }
        }
    }

    private void clearPing() {
        debug(TAG, "clear ping");
        mPingCollector = null;
        heartbeatSequence = 0;
    }

    // Runs in executor thread
    private void sendPing() {
        IQ req = new IQ() {
            public String getChildElementXML() {
                return "<ping xmlns='urn:xmpp:ping'/>";
            }
        };

        req.setType(IQ.Type.GET);
        PacketFilter filter = new AndFilter(new PacketIDFilter(req.getPacketID()),
                new PacketTypeFilter(IQ.class));
        mPingCollector = mConnection.createPacketCollector(filter);
        mConnection.sendPacket(req);
    }

    // Runs in executor thread
    private boolean checkPing() {
        if (mPingCollector != null) {
            IQ result = (IQ) mPingCollector.pollResult();
            mPingCollector.cancel();
            mPingCollector = null;
            if (result == null) {
                Log.e(TAG, "ping timeout");
                return false;
            }
        }
        return true;
    }

    // watch out, this is a different XMPPConnection class than XmppConnection! ;)
    // org.jivesoftware.smack.XMPPConnection
    //    - vs -
    // info.guardianproject.otr.app.im.plugin.xmpp.XmppConnection
    public static class MyXMPPConnection extends XMPPConnection {

        public MyXMPPConnection(ConnectionConfiguration config) {
            super(config);

            //this.getConfiguration().setSocketFactory(arg0)

        }

        public void shutdown() {

            try {
                // Be forceful in shutting down since SSL can get stuck
                try {
                    socket.shutdownInput();
                } catch (Exception e) {
                }
                socket.close();
                shutdown(new org.jivesoftware.smack.packet.Presence(
                        org.jivesoftware.smack.packet.Presence.Type.unavailable));

            } catch (Exception e) {
                Log.e(TAG, "error on shutdown()", e);
            }
        }
    }

    @Override
    public void networkTypeChanged() {
        super.networkTypeChanged();
    }

    /*
     * Force a shutdown and reconnect, unless we are already reconnecting.
     * 
     * Runs in executor thread
     */
    private void force_reconnect() {
        debug(TAG, "force_reconnect need=" + mNeedReconnect);
        if (mConnection == null)
            return;
        if (mNeedReconnect)
            return;

        mNeedReconnect = true;

        try {
            if (mConnection != null && mConnection.isConnected()) {
                mStreamHandler.quickShutdown();
            }
        } catch (Exception e) {
            Log.w(TAG, "problem disconnecting on force_reconnect: " + e.getMessage());
        }

        reconnect();
    }

    /*
     * Reconnect unless we are already in the process of doing so.
     * 
     * Runs in executor thread.
     */
    private void maybe_reconnect() {
        debug(TAG, "maybe_reconnect mNeedReconnect=" + mNeedReconnect + " state=" + getState()
                   + " connection?=" + (mConnection != null));

        // This is checking whether we are already in the process of reconnecting.  If we are,
        // doHeartbeat will take care of reconnecting.
        if (mNeedReconnect)
            return;

        if (getState() == SUSPENDED)
            return;

        if (mConnection == null)
            return;

        mNeedReconnect = true;
        reconnect();
    }

    /*
     * Retry connecting
     * 
     * Runs in executor thread
     */
    private void reconnect() {
        if (getState() == SUSPENDED) {
            debug(TAG, "reconnect during suspend, ignoring");
            return;
        }

        try {
            Thread.sleep(2000); // Wait for network to settle
        } catch (InterruptedException e) { /* ignore */
        }

        if (mConnection != null) {
            // It is safe to ask mConnection whether it is connected, because either:
            // - We detected an error using ping and called force_reconnect, which did a shutdown
            // - Smack detected an error, so it knows it is not connected
            // so there are no cases where mConnection can be confused about being connected here.
            // The only left over cases are reconnect() being called too many times due to errors
            // reported multiple times or errors reported during a forced reconnect.
            
            // The analysis above is incorrect in the case where Smack loses connectivity
            // while trying to log in.  This case is handled in a future heartbeat
            // by checking ping responses.
            if (mConnection.isConnected()) {
                Log.w(TAG, "reconnect while already connected, assuming good");
                mNeedReconnect = false;
                setState(LOGGED_IN, null);
                return;
            }
            Log.i(TAG, "reconnect");
            clearPing();
            try {
                if (mStreamHandler.isResumePossible()) {
                    // Connect without binding, will automatically trigger a resume
                    debug(TAG, "resume");
                    mConnection.connect(false);
                    initServiceDiscovery();
                } else {
                    
                   //mConnection.disconnect();
                   
                    mConnection = null;
                    
                    do_login();
                    /*
                    debug(TAG, "no resume");
                    mConnection.connect();

                    if (!mConnection.isAuthenticated()) {
                        // This can happen if a reconnect failed and the smack connection now has wasAuthenticated = false.
                        // It can also happen if auth exception was swallowed by smack.
                        // Try to login manually.

      //                  Log.e(TAG, "authentication did not happen in connect() - login manually");
    //                    mConnection.login(mUsername, mPassword, mResource);
                        
                        // Make sure
                        if (!mConnection.isAuthenticated())
                            throw new XMPPException("manual auth failed");

                        // Manually set the state since manual auth doesn't notify listeners
                        mNeedReconnect = false;
                        setState(LOGGED_IN, null);
                    }
                    mStreamHandler.notifyInitialLogin();
                    initServiceDiscovery();
                    sendPresencePacket();
                    */
                }
            } catch (Exception e) {
                mStreamHandler.quickShutdown();
                Log.w(TAG, "reconnection attempt failed", e);
                // Smack incorrectly notified us that reconnection was successful, reset in case it fails
                mNeedReconnect = true;
                setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
            }
        } else {
            mNeedReconnect = true;

            debug(TAG, "reconnection on network change failed");

            setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR,
                    "reconnection on network change failed"));
        }
    }

    @Override
    protected void setState(int state, ImErrorInfo error) {
        debug(TAG, "setState to " + state);
        super.setState(state, error);
    }

    public void debug(String tag, String msg) {
      //  if (Log.isLoggable(TAG, Log.DEBUG)) {
        if (Debug.DEBUG_ENABLED) {
            Log.d(tag, "" + mGlobalId + " : " + msg);
        }
    }

    @Override
    public void handle(Callback[] arg0) throws IOException {

        for (Callback cb : arg0) {
            debug(TAG, cb.toString());
        }

    }

    /*
    public class MySASLDigestMD5Mechanism extends SASLMechanism
    {
    	 
        public MySASLDigestMD5Mechanism(SASLAuthentication saslAuthentication)
        {
            super(saslAuthentication);
        }
     
        protected void authenticate()
            throws IOException, XMPPException
        {
            String mechanisms[] = {
                getName()
            };
            java.util.Map props = new HashMap();
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", hostname, props, this);
            super.authenticate();
        }
     
        public void authenticate(String username, String host, String password)
            throws IOException, XMPPException
        {
            authenticationId = username;
            this.password = password;
            hostname = host;
            String mechanisms[] = {
                getName()
            };
            java.util.Map props = new HashMap();
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, this);
            super.authenticate();
        }
     
        public void authenticate(String username, String host, CallbackHandler cbh)
            throws IOException, XMPPException
        {
            String mechanisms[] = {
                getName()
            };
            java.util.Map props = new HashMap();
            sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, cbh);
            super.authenticate();
        }
     
        protected String getName()
        {
            return "DIGEST-MD5";
        }
     
        public void challengeReceived(String challenge)
            throws IOException
        {
            //StringBuilder stanza = new StringBuilder();
            byte response[];
            if(challenge != null)
                response = sc.evaluateChallenge(Base64.decode(challenge));
            else
                //response = sc.evaluateChallenge(null);
                response = sc.evaluateChallenge(new byte[0]);
            //String authenticationText = "";
            Packet responseStanza;
            //if(response != null)
            //{
                //authenticationText = Base64.encodeBytes(response, 8);
                //if(authenticationText.equals(""))
                    //authenticationText = "=";
               
                if (response == null){
                    responseStanza = new Response();
                } else {
                    responseStanza = new Response(Base64.encodeBytes(response,Base64.DONT_BREAK_LINES));   
                }
            //}
            //stanza.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
            //stanza.append(authenticationText);
            //stanza.append("</response>");
            //getSASLAuthentication().send(stanza.toString());
            getSASLAuthentication().send(responseStanza);
        }
    }
     */
    private void initServiceDiscovery() {
        debug(TAG, "init service discovery");
        // register connection features
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mConnection);
        if (sdm == null)
            sdm = new ServiceDiscoveryManager(mConnection);

        if (!sdm.includesFeature(DISCO_FEATURE))
            sdm.addFeature(DISCO_FEATURE);
        if (!sdm.includesFeature(DeliveryReceipts.NAMESPACE))
            sdm.addFeature(DeliveryReceipts.NAMESPACE);
    }

    private void onReconnectionSuccessful() {
        mNeedReconnect = false;
        setState(LOGGED_IN, null);
    }
    
  
    private void addProviderManagerExtensions ()
    {

        ProviderManager pm = ProviderManager.getInstance();
        
    //  Private Data Storage
        pm.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());

        //  Time
        try {
            pm.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            Log.w("TestClient", "Can't load class for org.jivesoftware.smackx.packet.Time");
        }

        //  Roster Exchange
        pm.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider());

        //  Message Events
        pm.addExtensionProvider("x","jabber:x:event", new MessageEventProvider());

        //  Chat State
        pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider()); 
        pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

        //  XHTML
        pm.addExtensionProvider("html","http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());

        //  Group Chat Invitations
        pm.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());

        //  Service Discovery # Items    
        pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());

        //  Service Discovery # Info
        pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());

        //  Data Forms
        pm.addExtensionProvider("x","jabber:x:data", new DataFormProvider());

        //  MUC User
        pm.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());

        //  MUC Admin    
        pm.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider());

        //  MUC Owner    
        pm.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());

        
        //  Delayed Delivery
        pm.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider());
    
        //  Version
        try {
            pm.addIQProvider("query","jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException e) {
            //  Not sure what's happening here.
        }
    
        //  VCard
        pm.addIQProvider("vCard","vcard-temp", new VCardProvider());
    
        //  Offline Message Requests
        pm.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
    
        //  Offline Message Indicator
        pm.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
    
        //  Last Activity
        pm.addIQProvider("query","jabber:iq:last", new LastActivity.Provider());
    
        //  User Search
        pm.addIQProvider("query","jabber:iq:search", new UserSearch.Provider());
    
        //  SharedGroupsInfo
        pm.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());
    
        //  JEP-33: Extended Stanza Addressing
        pm.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddressesProvider());
    
        //   FileTransfer
        pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());
    
        pm.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
    
        //  Privacy
        pm.addIQProvider("query","jabber:iq:privacy", new PrivacyProvider());
        pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
        pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
        pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
        pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
        pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
        pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
        
    }        

    class NameSpace {

        public static final String DISCO_INFO = "http://jabber.org/protocol/disco#info";
        public static final String DISCO_ITEMS = "http://jabber.org/protocol/disco#items";
        public static final String IQ_GATEWAY = "jabber:iq:gateway";
        public static final String IQ_GATEWAY_REGISTER = "jabber:iq:gateway:register";
        public static final String IQ_LAST = "jabber:iq:last";
        public static final String IQ_REGISTER = "jabber:iq:register";
        public static final String IQ_REGISTERED = "jabber:iq:registered";
        public static final String IQ_ROSTER = "jabber:iq:roster";
        public static final String IQ_VERSION = "jabber:iq:version";
        public static final String CHATSTATES = "http://jabber.org/protocol/chatstates";
        public static final String XEVENT = "jabber:x:event";
        public static final String XDATA = "jabber:x:data";
        public static final String MUC = "http://jabber.org/protocol/muc";
        public static final String MUC_USER = MUC + "#user";
        public static final String MUC_ADMIN = MUC + "#admin";
        public static final String SPARKNS = "http://www.jivesoftware.com/spark";
        public static final String DELAY = "urn:xmpp:delay";
        public static final String OFFLINE = "http://jabber.org/protocol/offline";
        public static final String X_DELAY = "jabber:x:delay";
        public static final String VCARD_TEMP = "vcard-temp";
        public static final String VCARD_TEMP_X_UPDATE = "vcard-temp:x:update";
        public static final String ATTENTIONNS = "urn:xmpp:attention:0";

    }

    class XmppCertPins 
    {
        
/*
## Certificate 0 ##
Subject: CN=xmpp.binaryparadox.net
Issuer: CN=xmpp.binaryparadox.net
SHA1 FP: 0B93EB84CCBB7AA2CB92CF61A0348F63CCED14C1
SPKI Pin: B3A7C02FC620C25F3C395AB043BF3C7729CE3C41

Connecting to jabber.ccc.de [2 of 4 hosts]
There were 3 certs in chain.
*/
           public final static String BINARYPARADOX = "B3A7C02FC620C25F3C395AB043BF3C7729CE3C41";

           /*
## Certificate 0 ##
Subject: CN=jabber.ccc.de, O=Chaos Computer Club e.V., L=Hamburg, ST=Hamburg,
C=DE
Issuer: CN=CAcert Class 3 Root, OU=http://www.CAcert.org, O=CAcert Inc.
SHA1 FP: 8155CF376967A47417A7BEAA9B712AC63D161D50
SPKI Pin: ADE7618FE3BB26C20FC089F3EF9963D548D21457
*/
           
           public final static String JABBERCCCDE = "ADE7618FE3BB26C20FC089F3EF9963D548D21457";

           /*
## Certificate 1 ##
Subject: CN=CAcert Class 3 Root, OU=http://www.CAcert.org, O=CAcert Inc.
Issuer: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority,
OU=http://www.cacert.org, O=Root CA
SHA1 FP: DB4C4269073FE9C2A37D890A5C1B18C4184E2A2D
SPKI Pin: F061D83F958F4D78B147B31339978EA9C251BA9B
*/
           
           /*
## Certificate 2 ##
Subject: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority,
OU=http://www.cacert.org, O=Root CA
Issuer: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority,
OU=http://www.cacert.org, O=Root CA
SHA1 FP: 135CEC36F49CB8E93B1AB270CD80884676CE8F33
SPKI Pin: 10DA624DEF41A3046DCDBA3D018F19DF3DC9A07C
*/
           
           /*
Connecting to chat.facebook.com [3 of 4 hosts]
There were 2 certs in chain.

## Certificate 0 ##
Subject: CN=chat.facebook.com, O="Facebook, Inc.", L=Palo Alto, ST=California,
C=US
Issuer: CN=DigiCert High Assurance CA-3, OU=www.digicert.com, O=DigiCert Inc,
C=US
SHA1 FP: 22E50EEEAF2DAF8E440377196C4D95734DEE94D9
SPKI Pin: 1C5CC68C8ABE4AA0DBC7729BEA05A4EC756464B6

## Certificate 1 ##
Subject: CN=DigiCert High Assurance CA-3, OU=www.digicert.com, O=DigiCert Inc,
C=US
Issuer: CN=DigiCert High Assurance EV Root CA, OU=www.digicert.com, O=DigiCert
Inc, C=US
SHA1 FP: A2E32A1A2E9FAB6EAD6B05F64EA0641339E10011
SPKI Pin: 95F9D7434B1CE71DEF4211EE6BE3C0E0256FAD95
*/
           
           public final static String CHATFACEBOOK = "1C5CC68C8ABE4AA0DBC7729BEA05A4EC756464B6";

           /*
Connecting to dukgo.com [4 of 4 hosts]
There were 2 certs in chain.

## Certificate 0 ##
Subject: CN=*.dukgo.com, OU=EssentialSSL Wildcard, OU=Domain Control Validated
Issuer: CN=EssentialSSL CA, O=COMODO CA Limited, L=Salford, ST=Greater
Manchester, C=GB
SHA1 FP: 7727F3D42E00BDBFBEF697470F013B9E1C41A8CB
SPKI Pin: F44CF8786F4346082E18AB760CC49B6167B1B9D8

## Certificate 1 ##
Subject: CN=EssentialSSL CA, O=COMODO CA Limited, L=Salford, ST=Greater
Manchester, C=GB
Issuer: CN=COMODO Certification Authority, O=COMODO CA Limited, L=Salford,
ST=Greater Manchester, C=GB
SHA1 FP: 73820A20F8F47A457CD0B54CC4E4E31CEFA5C1E7
SPKI Pin: CA91EDBE3EEF0F1736BDA1BA53E48E79B8ED7389
*/
           public final static String DUKGO = "F44CF8786F4346082E18AB760CC49B6167B1B9D8";
     
           /* Gmail/ Gtalk
            * Calculating PIN for certificate: C=US, ST=California, L=Mountain View, O=Google Inc, CN=gmail.com
Pin Value: 4b09f2c32d093a31a175168346a459e2f0179d89

            */
           
           public final static String TALKGOOGLE = "4b09f2c32d093a31a175168346a459e2f0179d89";
    }
    
}
