package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.otr.TorProxyInfo;
import info.guardianproject.otr.app.im.R;
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
import info.guardianproject.otr.app.im.engine.ImEntity;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.engine.ImException;
import info.guardianproject.otr.app.im.engine.Invitation;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.Presence;
import info.guardianproject.otr.app.im.plugin.xmpp.auth.GTalkOAuth2;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.ImpsErrorInfo;
import info.guardianproject.otr.app.im.service.ChatSessionAdapter;
import info.guardianproject.util.DNSUtil;
import info.guardianproject.util.Debug;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
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
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message.Body;
import org.jivesoftware.smack.packet.Packet;
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

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import de.duenndns.ssl.MemorizingTrustManager;

public class XmppConnection extends ImConnection implements CallbackHandler {

    private static final String DISCO_FEATURE = "http://jabber.org/protocol/disco#info";
    final static String TAG = "GB.XmppConnection";
    private final static boolean PING_ENABLED = true;

    private XmppContactListManager mContactListManager;
    private Contact mUser;
    private boolean mUseTor;

    // watch out, this is a different XMPPConnection class than XmppConnection! ;)
    // Synchronized by executor thread
    private MyXMPPConnection mConnection;
    private XmppStreamHandler mStreamHandler;

    private Roster mRoster;

    private XmppChatSessionManager mSessionManager;
    private ConnectionConfiguration mConfig;

    // True if we are in the process of reconnecting.  Reconnection is retried once per heartbeat.
    // Synchronized by executor thread.
    private boolean mNeedReconnect;

    private boolean mRetryLogin;
    private ThreadPoolExecutor mExecutor;
    private Timer mTimerPresence;

    private ProxyInfo mProxyInfo = null;

    private long mAccountId = -1;
    private long mProviderId = -1;

    private boolean mIsGoogleAuth = false;

    private final static String SSLCONTEXT_TYPE = "TLS";

    private static SSLContext sslContext;

    private final static int SOTIMEOUT = 60000;

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

    private LinkedList<String> qAvatar = new LinkedList <String>();

    private LinkedList<org.jivesoftware.smack.packet.Presence> qPresence = new LinkedList<org.jivesoftware.smack.packet.Presence>();
    private LinkedList<org.jivesoftware.smack.packet.Packet> qPacket = new LinkedList<org.jivesoftware.smack.packet.Packet>();

    public XmppConnection(Context context) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        super(context);

        synchronized (XmppConnection.class) {
            mGlobalId = mGlobalCount++;
        }

        Debug.onConnectionStart();

        SmackConfiguration.setPacketReplyTimeout(SOTIMEOUT);

        // Create a single threaded executor.  This will serialize actions on the underlying connection.
        createExecutor();

        addProviderManagerExtensions();

        XmppStreamHandler.addExtensionProviders();
        DeliveryReceipts.addExtensionProviders();

        ServiceDiscoveryManager.setIdentityName("ChatSecure");
        ServiceDiscoveryManager.setIdentityType("phone");
    }

    public void initUser(long providerId, long accountId) throws ImException
    {
        ContentResolver contentResolver = mContext.getContentResolver();

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(providerId)},null);

        if (cursor == null)
            throw new ImException("unable to query settings");

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, providerId, false, null);

        mProviderId = providerId;
        mAccountId = accountId;
        mUser = makeUser(providerSettings, contentResolver);
        mUseTor = providerSettings.getUseTor();

        providerSettings.close();
    }

    private Contact makeUser(Imps.ProviderSettings.QueryMap providerSettings, ContentResolver contentResolver) {

        String userName = Imps.Account.getUserName(contentResolver, mAccountId);
        String domain = providerSettings.getDomain();
        String xmppName = userName + '@' + domain + '/' + providerSettings.getXmppResource();

        return new Contact(new XmppAddress(xmppName), userName);
    }

    private void createExecutor() {
       mExecutor = new ThreadPoolExecutor(1, 1, 1L, TimeUnit.SECONDS,
              new LinkedBlockingQueue<Runnable>());

    }

    private boolean execute(Runnable runnable) {

        if (mExecutor == null)
            createExecutor (); //if we disconnected, will need to recreate executor here, because join() made it null

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
        qPacket.add(packet);
       
    }


    void postpone(final org.jivesoftware.smack.packet.Packet packet) {
        if (packet instanceof org.jivesoftware.smack.packet.Message) {
            boolean groupChat = ((org.jivesoftware.smack.packet.Message) packet).getType().equals( org.jivesoftware.smack.packet.Message.Type.groupchat);
            ChatSession session = findOrCreateSession(packet.getTo(), groupChat);
            session.onMessagePostponed(packet.getPacketID());
        }
    }


    private boolean mLoadingAvatars = false;

    private void loadVCardsAsync ()
    {
        if (!mLoadingAvatars)
        {
            execute(new AvatarLoader());
        }
    }

    private class AvatarLoader implements Runnable
    {
        @Override
        public void run () {

            mLoadingAvatars = true;

            ContentResolver resolver = mContext.getContentResolver();

            try
            {
                while (qAvatar.size()>0)
                {

                    loadVCard (resolver, qAvatar.pop(), null);

                }
            }
            catch (Exception e) {}

            mLoadingAvatars = false;
        }
    }

    private boolean loadVCard (ContentResolver resolver, String jid, String hash)
    {
        try {

            boolean loadAvatar = false;

            if (hash != null)
                loadAvatar = (!DatabaseUtils.doesAvatarHashExist(resolver,  Imps.Avatars.CONTENT_URI, jid, hash));
            else
            {
                loadAvatar = DatabaseUtils.hasAvatarContact(resolver, Imps.Avatars.CONTENT_URI, jid);
            }

            if (!loadAvatar)
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
                        debug(ImApp.LOG_TAG, "start avatar length: " + avatarBytes.length);

                        int width = ImApp.DEFAULT_AVATAR_WIDTH;
                        int height = ImApp.DEFAULT_AVATAR_HEIGHT;

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length,options);
                        options.inSampleSize = DatabaseUtils.calculateInSampleSize(options, width, height);
                        options.inJustDecodeBounds = false;

                        Bitmap b = BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length,options);
                        b = Bitmap.createScaledBitmap(b, ImApp.DEFAULT_AVATAR_WIDTH, ImApp.DEFAULT_AVATAR_HEIGHT, false);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        b.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                        byte[] avatarBytesCompressed = stream.toByteArray();

                        debug(ImApp.LOG_TAG, "compressed avatar length: " + avatarBytesCompressed.length);

                        DatabaseUtils.insertAvatarBlob(resolver, Imps.Avatars.CONTENT_URI, mProviderId, mAccountId, avatarBytesCompressed, avatarHash, XmppAddress.stripResource(jid));

                        // int providerId, int accountId, byte[] data, String hash,String contact
                        return true;
                    }
                }

            }

        } catch (XMPPException e) {

            // Log.d(ImApp.LOG_TAG,"err loading vcard");

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

        return ImConnection.CAPABILITY_SESSION_REESTABLISHMENT | ImConnection.CAPABILITY_GROUP_CHAT;
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
        
        public void reconnectAll ()
        {
            for (MultiUserChat muc : mMUCs.values())
            {
                if (!muc.isJoined())
                {
                    try {
                        muc.join(muc.getNickname());
                    } catch (XMPPException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public boolean createChatGroupAsync(String chatRoomJid, String nickname) throws Exception {

            if (mConnection == null || getState() != ImConnection.LOGGED_IN)
                return false;
            
            RoomInfo roomInfo = null;

            if (chatRoomJid.indexOf("@")==-1)
            {
                //let's add a host to that!

                Collection<String> servers = MultiUserChat.getServiceNames(mConnection);
                chatRoomJid += '@' + servers.iterator().next();
                
            }
            
            Address address = new XmppAddress (chatRoomJid);

            try
            {
                //first check if the room already exists
                roomInfo = MultiUserChat.getRoomInfo(mConnection, chatRoomJid);
            }
            catch (Exception e)
            {
                //who knows?
            }

            if (roomInfo == null)
            {
                //if the room does not exist, then create one

                //should be room@server
                String[] parts = chatRoomJid.split("@");
                String room = parts[0];
                String server = parts[1];

                if (nickname == null || nickname.length() == 0)
                {
                    nickname = mUsername;
                }
                
                try {

                    // Create a MultiUserChat using a Connection for a room
                    MultiUserChat muc = new MultiUserChat(mConnection, chatRoomJid);

                    try
                    {
                        // Create the room
                        muc.create(nickname);

                    }
                    catch (XMPPException iae)
                    {
                        if (iae.getMessage().contains("Creation failed"))
                        {
                            //some server's don't return the proper 201 create code, so we can just assume the room was created!
                        }
                        else
                        {
                            throw iae;
                        }
                    }

                    try
                    {
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
                    }
                    catch (XMPPException xe)
                    {
                        if (Debug.DEBUG_ENABLED)
                            Log.w(ImApp.LOG_TAG,"(ignoring) got an error configuring MUC room: " + xe.getLocalizedMessage());
                    }

                    muc.join(nickname);

                    ChatGroup chatGroup = new ChatGroup(address,room,this);
                    
                    mGroups.put(address.getAddress(), chatGroup);
                    mMUCs.put(chatRoomJid, muc);

                    return true;

                } catch (XMPPException e) {

                    Log.e(ImApp.LOG_TAG,"error creating MUC",e);
                    return false;
                }
            }
            else
            {
                //otherwise, join the room!

                joinChatGroupAsync(address);
                return true;
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

            String chatRoomJid = group.getAddress().getAddress();

            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);
                muc.invite(contact.getAddress().getBareAddress(),"");
            }


        }

        @Override
        protected void removeGroupMemberAsync(ChatGroup group, Contact contact) {


            String chatRoomJid = group.getAddress().getAddress();

            if (mMUCs.containsKey(chatRoomJid))
            {
                MultiUserChat muc = mMUCs.get(chatRoomJid);
                try {
                    muc.kickParticipant(chatRoomJid, contact.getAddress().getBareAddress());
                } catch (XMPPException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public String getDefaultMultiUserChatServer ()
        {
            try
            {
                if (mConnection == null)
                    return null;
                
                Collection<String> servers = MultiUserChat.getServiceNames(mConnection);
                
                if (servers == null || servers.isEmpty())
                    return null;
                else
                    return servers.iterator().next();
            }
            catch (XMPPException e)
            {
                Log.e(ImApp.LOG_TAG,"error finding MUC",e);
                
            }
            
            return null;
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
    public synchronized XmppContactListManager getContactListManager() {

        if (mContactListManager == null)
            mContactListManager = new XmppContactListManager();

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
    public boolean isUsingTor() {
        return mUseTor;
    }

    @Override
    public void loginAsync(long accountId, String passwordTemp, long providerId, boolean retry) {

        mAccountId = accountId;
        mPassword = passwordTemp;
        mProviderId = providerId;
        mRetryLogin = retry;

        ContentResolver contentResolver = mContext.getContentResolver();

        if (mPassword == null)
            mPassword = Imps.Account.getPassword(contentResolver, mAccountId);

        mIsGoogleAuth = mPassword.startsWith(GTalkOAuth2.NAME);

        if (mIsGoogleAuth)
        {
            mPassword = mPassword.split(":")[1];
        }

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(mProviderId)},null);

        if (cursor == null)
            return;

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, mProviderId, false, null);

        mUser = makeUser(providerSettings, contentResolver);

        providerSettings.close();

        execute(new Runnable() {
            @Override
            public void run() {
                do_login();
            }
        });
    }

    // Runs in executor thread
    private void do_login() {

        /*
        if (mConnection != null) {
            setState(getState(), new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER,
                    "still trying..."));
            return;
        }*/

        ContentResolver contentResolver = mContext.getContentResolver();

        Cursor cursor = contentResolver.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(mProviderId)},null);

        if (cursor == null)
            return; //not going to work

        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                cursor, contentResolver, mProviderId, false, null);


        // providerSettings is closed in initConnection();
        String userName = Imps.Account.getUserName(contentResolver, mAccountId);

        String defaultStatus = null;

        mNeedReconnect = true;
        setState(LOGGING_IN, null);

        mUserPresence = new Presence(Presence.AVAILABLE, defaultStatus, Presence.CLIENT_TYPE_MOBILE);

        try {
            if (userName == null || userName.length() == 0)
                throw new XMPPException("empty username not allowed");

            initConnectionAndLogin(providerSettings, userName);

            setState(LOGGED_IN, null);
            debug(TAG, "logged in");
            mNeedReconnect = false;



        } catch (XMPPException e) {
            debug(TAG, "exception thrown on connection",e);


            ImErrorInfo info = new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, e.getMessage());
            mRetryLogin = true; // our default behavior is to retry

            if (mConnection != null && mConnection.isConnected() && (!mConnection.isAuthenticated())) {

                if (mIsGoogleAuth)
                {
                    debug (TAG, "google failed; may need to refresh");

                    String newPassword = refreshGoogleToken (userName, mPassword,providerSettings.getDomain());

                    if (newPassword != null)
                        mPassword = newPassword;

                    mRetryLogin = true;


                }
                else
                {
                    debug(TAG, "not authorized - will not retry");
                    info = new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "invalid user/password");
                    mRetryLogin = false;
                    mNeedReconnect = false;
                }
            }


            if (mRetryLogin && getState() != SUSPENDED) {
                debug(TAG, "will retry");
                setState(LOGGING_IN, info);
                maybe_reconnect();

            } else {
               //debug(TAG, "will not retry"); //WE MUST ALWAYS RETRY!
               // disconnect();
               // disconnected(info);
            }


        } catch (Exception e) {

            debug(TAG, "login failed",e);
            mRetryLogin = true;
            mNeedReconnect = true;

            debug(TAG, "will retry");
            ImErrorInfo info = new ImErrorInfo(ImErrorInfo.UNKNOWN_ERROR, "keymanagement exception");
            setState(LOGGING_IN, info);

        }
        finally {
            providerSettings.close();
            
            if (!cursor.isClosed())
                cursor.close();
        }

    }

    private String refreshGoogleToken (String userName, String expiredToken, String domain)
    {
        
        //invalidate our old one, that is locally cached
        AccountManager.get(mContext.getApplicationContext()).invalidateAuthToken("com.google", expiredToken);

        //request a new one
        String newToken = GTalkOAuth2.getGoogleAuthToken(userName + '@' + domain, mContext.getApplicationContext());

        if (newToken != null)
        {
            //now store the new one, for future use until it expires
            ImApp.insertOrUpdateAccount(mContext.getContentResolver(), mProviderId, userName,
                    GTalkOAuth2.NAME + ':' + newToken );
        }

        return newToken;

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
        mRoster = mConnection.getRoster();
        mUser = user;
        setState(state, null);
    }

    private void initConnectionAndLogin (Imps.ProviderSettings.QueryMap providerSettings,String userName) throws XMPPException, KeyManagementException, NoSuchAlgorithmException, IllegalStateException, RuntimeException
    {
        Debug.onConnectionStart(); //only activates if Debug TRUE is set, so you can leave this in!

        initConnection(providerSettings, userName);

        mResource = providerSettings.getXmppResource();

        //disable compression based on statement by Ge0rg
        mConfig.setCompressionEnabled(false);

        if (mConnection.isConnected())
        {
            
            mConnection.login(mUsername, mPassword, mResource);
            
            String fullJid = mConnection.getUser();
            XmppAddress xa = new XmppAddress(fullJid);
            mUser = new Contact(xa, xa.getUser());

            mStreamHandler.notifyInitialLogin();
            initServiceDiscovery();

            sendPresencePacket();

            mRoster = mConnection.getRoster();
            mRoster.setSubscriptionMode(Roster.SubscriptionMode.manual);

            getContactListManager().listenToRoster(mRoster);

        }
        

    }


    // Runs in executor thread
    private void initConnection(Imps.ProviderSettings.QueryMap providerSettings, String userName) throws NoSuchAlgorithmException, KeyManagementException, XMPPException  {


        boolean allowPlainAuth = providerSettings.getAllowPlainAuth();
        boolean requireTls = providerSettings.getRequireTls();
        boolean doDnsSrv = providerSettings.getDoDnsSrv();
        boolean tlsCertVerify = providerSettings.getTlsCertVerify();

        boolean useSASL = true;//!allowPlainAuth;

        String domain = providerSettings.getDomain();

        mPriority = providerSettings.getXmppResourcePrio();
        int serverPort = providerSettings.getPort();

        String server = providerSettings.getServer();
        if ("".equals(server))
            server = null;

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
        if (doDnsSrv) {

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

        if (server != null && server.contains("google.com"))
        {
            mUsername = userName + '@' + domain;
        }
        else if (domain.contains("gmail.com"))
        {
            mUsername = userName + '@' + domain;
        }
        else if (mIsGoogleAuth)
        {
            mUsername = userName + '@' + domain;
        }
        else
        {
            mUsername = userName;
        }


        if (serverPort == 0) //if serverPort is set to 0 then use 5222 as default
            serverPort = 5222;

        // No server requested and SRV lookup wasn't requested or returned nothing - use domain
        if (server == null) {
            debug(TAG, "(use domain) ConnectionConfiguration(" + domain + ", " + serverPort + ", "
                    + domain + ", mProxyInfo);");

            if (mProxyInfo == null)
                mConfig = new ConnectionConfiguration(domain, serverPort);
            else
                mConfig = new ConnectionConfiguration(domain, serverPort, mProxyInfo);

            //server = domain;

        } else {
            debug(TAG, "(use server) ConnectionConfiguration(" + server + ", " + serverPort + ", "
                    + domain + ", mProxyInfo);");

            //String serviceName = domain;

            //if (server != null && (!server.endsWith(".onion"))) //if a connect server was manually entered, and is not an .onion address
              //  serviceName = server;

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

        SASLAuthentication.registerSASLMechanism( GTalkOAuth2.NAME, GTalkOAuth2.class );

        if (mIsGoogleAuth) //if using google auth enable sasl
            SASLAuthentication.supportSASLMechanism( GTalkOAuth2.NAME, 0);
        else if (domain.contains("google.com")||domain.contains("gmail.com")) //if not google auth, disable if doing direct google auth
            SASLAuthentication.unsupportSASLMechanism( GTalkOAuth2.NAME);

        SASLAuthentication.supportSASLMechanism("PLAIN", 1);
        SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 2);


        if (requireTls) {

            MemorizingTrustManager trustManager = ImApp.sImApp.getTrustManager();

            if (sslContext == null)
            {

                sslContext = SSLContext.getInstance(SSLCONTEXT_TYPE);
                SecureRandom secureRandom = new java.security.SecureRandom();
                sslContext.init(null, new javax.net.ssl.TrustManager[] { trustManager },
                        secureRandom);

                sslContext.getDefaultSSLParameters().getCipherSuites();

                if (Build.VERSION.SDK_INT >= 20) {

                    sslContext.getDefaultSSLParameters().setCipherSuites(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES_API_20);

                }
                else
                {
                    sslContext.getDefaultSSLParameters().setCipherSuites(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES);
                }


            }


            int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= 16){
                // Enable TLS1.2 and TLS1.1 on supported versions of android
                // http://stackoverflow.com/questions/16531807/android-client-server-on-tls-v1-2

               //mConfig.setEnabledProtocols(new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" });
                sslContext.getDefaultSSLParameters().setProtocols(new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" });

            }

            if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
                mConfig.setEnabledCipherSuites(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES);
            }


            HostnameVerifier hv = trustManager.wrapHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());

            mConfig.setHostnameVerifier(hv);
            mConfig.setCustomSSLContext(sslContext);

            mConfig.setSecurityMode(SecurityMode.required);

            mConfig.setVerifyChainEnabled(true);
            mConfig.setVerifyRootCAEnabled(true);
            mConfig.setExpiredCertificatesCheckEnabled(true);

            mConfig.setNotMatchingDomainCheckEnabled(true);

            mConfig.setSelfSignedCertificateEnabled(false);

            mConfig.setCallbackHandler(this);

        } else {
            // if it finds a cert, still use it, but don't check anything since
            // TLS errors are not expected by the user
            mConfig.setSecurityMode(SecurityMode.enabled);

            if (sslContext == null)
            {
                sslContext = SSLContext.getInstance(SSLCONTEXT_TYPE);

                SecureRandom mSecureRandom = new java.security.SecureRandom();

                sslContext.init(null, new javax.net.ssl.TrustManager[] {  getDummyTrustManager () },
                        mSecureRandom);

                sslContext.getDefaultSSLParameters().setCipherSuites(XMPPCertPins.SSL_IDEAL_CIPHER_SUITES);
            }
            mConfig.setCustomSSLContext(sslContext);

            if (!allowPlainAuth)
                SASLAuthentication.unsupportSASLMechanism("PLAIN");

            mConfig.setVerifyChainEnabled(false);
            mConfig.setVerifyRootCAEnabled(false);
            mConfig.setExpiredCertificatesCheckEnabled(false);
            mConfig.setNotMatchingDomainCheckEnabled(false);
            mConfig.setSelfSignedCertificateEnabled(true);
        }

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
                debug(TAG, "receive message: " + packet.getFrom() + " to " + packet.getTo());

                org.jivesoftware.smack.packet.Message smackMessage = (org.jivesoftware.smack.packet.Message) packet;
                
                String address = smackMessage.getFrom();
                String body = smackMessage.getBody();

                if (smackMessage.getError() != null)
                {
                  //  smackMessage.getError().getCode();
                    
                    String error = "Error " + smackMessage.getError().getCode() + " (" + smackMessage.getError().getCondition() + "): " + smackMessage.getError().getMessage();
                    
                    debug (TAG, error);
                    
                    return;
                    
                }
                
                
                if (body == null)
                {

                    Collection<Body> mColl = smackMessage.getBodies();
                    for (Body bodyPart : mColl)
                    {
                        String msg = bodyPart.getMessage();
                        if (msg != null)
                        {
                            body = msg;
                            break;
                        }
                    }

                }

                DeliveryReceipts.DeliveryReceipt drIncoming = (DeliveryReceipts.DeliveryReceipt) smackMessage
                        .getExtension("received", DeliveryReceipts.NAMESPACE);

                if (drIncoming != null) {

                    debug(TAG, "got delivery receipt for " + drIncoming.getId());
                    boolean groupMessage = smackMessage.getType() == org.jivesoftware.smack.packet.Message.Type.groupchat;
                    ChatSession session = findOrCreateSession(address, groupMessage);
                    session.onMessageReceipt(drIncoming.getId());
                    
                }

                if (body != null)
                {
                    XmppAddress aFrom = new XmppAddress(smackMessage.getFrom());
                    XmppAddress aTo = new XmppAddress(smackMessage.getTo());
                    
                    boolean isGroupMessage = smackMessage.getType() == org.jivesoftware.smack.packet.Message.Type.groupchat;

                    ChatSession session = findOrCreateSession(address, isGroupMessage);

                    Message rec = new Message(body);
                    rec.setTo(aTo);
                    rec.setFrom(aFrom);
                    rec.setDateTime(new Date());

                    rec.setType(Imps.MessageType.INCOMING);

                    /*
                    // Detect if this was said by us, and mark message as outgoing
                    if (isGroupMessage && rec.getFrom().getResource().equals(rec.getTo().getUser())) {
                        rec.setType(Imps.MessageType.OUTGOING);
                    }*/

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
        }, new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class));

        mConnection.addPacketListener(new PacketListener() {

            @Override
            public void processPacket(Packet packet) {

                org.jivesoftware.smack.packet.Presence presence = (org.jivesoftware.smack.packet.Presence) packet;
                qPresence.push(presence);

            }
        }, new PacketTypeFilter(org.jivesoftware.smack.packet.Presence.class));

        if (mTimerPackets == null)
            initPacketProcessor();
        
        if (mTimerPresence == null)
            initPresenceProcessor ();
        
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
                    mRoster = mConnection.getRoster();
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
                // // We are not using the reconnection manager
                // throw new UnsupportedOperationException();
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

                        public void run() {
                            if (getState() == LOGGED_IN)
                            {
                                //Thread.sleep(1000);
                                mNeedReconnect = true;
                                setState(LOGGING_IN,
                                      new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network error"));
                                reconnect();
                            }
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

        for (int i = 0; i < 3; i++)
        {
            try
            {
                mConnection.connect();
                break;
            }
            catch (Exception uhe)
            {
                //sometimes DNS fails.. let's wait and try again a few times
                try { Thread.sleep(500);} catch (Exception e){}

            }

        }

        if (!mConnection.isConnected())
            throw new XMPPException("Unable to connect to host");


    }

    private void sendPresencePacket() {        
        qPacket.add(makePresencePacket(mUserPresence));        
    }

    public void sendReceipt(org.jivesoftware.smack.packet.Message msg) {
        debug(TAG, "sending XEP-0184 ack to " + msg.getFrom() + " id=" + msg.getPacketID());
        org.jivesoftware.smack.packet.Message ack = new org.jivesoftware.smack.packet.Message(
                msg.getFrom(), msg.getType());
        ack.addExtension(new DeliveryReceipts.DeliveryReceipt(msg.getPacketID()));
        sendPacket(ack);
    }



    public X509TrustManager getDummyTrustManager ()
    {

        return new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };


    }


    protected int parsePresence(org.jivesoftware.smack.packet.Presence presence) {
        int type = Imps.Presence.AVAILABLE;
        Mode rmode = presence.getMode();
        Type rtype = presence.getType();

        //if a device sends something other than available, check if there is a higher priority one available on the server
        /*
        if (rmode != Mode.available)
        {
            if (mRoster != null)
            {
                org.jivesoftware.smack.packet.Presence npresence = mRoster.getPresence(XmppAddress.stripResource(presence.getFrom()));
                rmode = npresence.getMode();
                rtype = npresence.getType();

                if (rmode == Mode.away || rmode == Mode.xa)
                    type = Presence.AWAY;
                else if (rmode == Mode.dnd)
                    type = Presence.DO_NOT_DISTURB;
                else if (rtype == Type.unavailable || rtype == Type.error)
                    type = Presence.OFFLINE;
            }
        }*/
                
        if (rmode == Mode.chat)
            type = Imps.Presence.AVAILABLE;
        else if (rmode == Mode.away || rmode == Mode.xa)
            type = Imps.Presence.AWAY;
        else if (rmode == Mode.dnd)
            type = Imps.Presence.DO_NOT_DISTURB;
        else if (rtype == Type.unavailable || rtype == Type.error)
            type = Imps.Presence.OFFLINE;
        else if (rtype == Type.unsubscribed)
            type = Imps.Presence.OFFLINE;
        
        return type;
    }

    // We must release resources here, because we will not be reused
    void disconnected(ImErrorInfo info) {
        debug(TAG, "disconnected");
        join();
        setState(DISCONNECTED, info);
    }


    @Override
    public void logoutAsync() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                do_logout();
            }
        }).start();

    }

    // Force immediate logout
    public void logout() {
        logoutAsync();
    }

    // Usually runs in executor thread, unless called from logout()
    private void do_logout() {
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
                    mNeedReconnect = false;
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

                if (mStreamHandler != null)
                    mStreamHandler.quickShutdown();

            }
        });
    }

    private ChatSession findOrCreateSession(String address, boolean groupChat) {
        ChatSession session = mSessionManager.findSession(address);

        if (session == null) {
            ImEntity participant = findOrCreateParticipant(address, groupChat);
            session = mSessionManager.createChatSession(participant,false);

        }

        return session;
    }

    ImEntity findOrCreateParticipant(String address, boolean groupChat) {
        ImEntity participant = mContactListManager.getContact(address);
        if (participant == null) {
            if (!groupChat) {
                participant = makeContact(address);
            }
            else {
                try {
                    mChatGroupManager.createChatGroupAsync(address, mUser.getName());

                    Address xmppAddress = new XmppAddress(address);

                    participant = mChatGroupManager.getChatGroup(xmppAddress);
                }
                catch (Exception e) {
                    Log.e(ImApp.LOG_TAG,"unable to join group chat",e);
                }
            }
        }

        return participant;
    }

    Contact findOrCreateContact(String address) {
        return (Contact) findOrCreateParticipant(address, false);
    }

    private Contact makeContact(String address) {

        Contact contact = null;

        //load from roster if we don't have the contact
        RosterEntry rEntry = null;

        if (mConnection != null)
            rEntry = mConnection.getRoster().getEntry(address);

        if (rEntry != null)
        {
            XmppAddress xAddress = new XmppAddress(address);

            String name = rEntry.getName();
            if (name == null)
                name = xAddress.getUser();

            contact = new Contact(xAddress, name);
        }
        else
        {
            XmppAddress xAddress = new XmppAddress(address);

            contact = new Contact(xAddress, xAddress.getUser());
        }

        return contact;
    }

    private final class XmppChatSessionManager extends ChatSessionManager {
        @Override
        public void sendMessageAsync(ChatSession session, Message message) {

            String chatRoomJid = message.getTo().getAddress();
            MultiUserChat muc = ((XmppChatGroupManager)getChatGroupManager()).getMultiUserChat(chatRoomJid);

            org.jivesoftware.smack.packet.Message msgXmpp = null;
            
            if (muc != null)
            {
                msgXmpp = muc.createMessage();

            }
            else
            {
                msgXmpp = new org.jivesoftware.smack.packet.Message(
                        message.getTo().getAddress(), org.jivesoftware.smack.packet.Message.Type.chat);
                msgXmpp.addExtension(new DeliveryReceipts.DeliveryReceiptRequest());
                
                Contact contact = mContactListManager.getContact(message.getTo().getBareAddress());
                
                if (contact != null && contact.getPresence() !=null && (!contact.getPresence().isOnline()))
                    requestPresenceRefresh(message.getTo().getBareAddress());
                
            }
            
            if (message.getFrom() == null)
                msgXmpp.setFrom(mUser.getAddress().getAddress());
            else
                msgXmpp.setFrom(message.getFrom().getAddress());
            
            msgXmpp.setBody(message.getBody());

            if (message.getID() != null)
                msgXmpp.setPacketID(message.getID());
            else
                message.setID(msgXmpp.getPacketID());
            
            sendPacket(msgXmpp);            

        }

        ChatSession findSession(String address) {

            return mSessions.get(Address.stripResource(address));
        }

        @Override
        public ChatSession createChatSession(ImEntity participant, boolean isNewSession) {

            requestPresenceRefresh(participant.getAddress().getAddress());
            
            ChatSession session = super.createChatSession(participant,isNewSession);

            //do avatar check if we have a no dominant presence
            qAvatar.push(participant.getAddress().getAddress());

         //   mSessions.put(Address.stripResource(participant.getAddress().getAddress()),session);
            return session;
        }

    }

    
    private void requestPresenceRefresh (String address)
    {
        org.jivesoftware.smack.packet.Presence p = new org.jivesoftware.smack.packet.Presence(Type.error);
        p.setFrom(address);
        qPresence.push(p);
    }

    public class XmppContactListManager extends ContactListManager {

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
            return Address.stripResource(address);
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
        /*
        private Collection<Contact> fillContacts(Collection<RosterEntry> entryIter,
                Set<String> skipList) {

            Roster roster = mConnection.getRoster();

            Collection<Contact> contacts = new ArrayList<Contact>();
            for (RosterEntry entry : entryIter) {

                String address = entry.getUser();
                if (skipList != null && !skipList.add(address))
                    continue;

                String name = entry.getName();
                if (name == null)
                    name = address;

                XmppAddress xaddress = new XmppAddress(address);

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

                Contact contact = mContactListManager.getContact(xaddress.getBareAddress());

                if (contact == null)
                    contact = new Contact(xaddress, name);

                contact.setPresence(p);

                contacts.add(contact);


            }
            return contacts;
        }
         */

        // Runs in executor thread
        private void do_loadContactLists() {

            debug(TAG, "load contact lists");

            if (mConnection == null)
                return;

            Roster roster = mConnection.getRoster();

            //Set<String> seen = new HashSet<String>();

            // This group will also contain all the unfiled contacts.  We will create it locally if it
            // does not exist.
            /*
            String generalGroupName = mContext.getString(R.string.buddies);

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
                cl = new ContactList(groupAddress, group.getName(), group
                        .getName().equals(generalGroupName), contacts, this);

            // We might have already created the Buddies contact list above
            if (cl == null) {
                cl = new ContactList(mUser.getAddress(), generalGroupName, true, contacts, this);
                notifyContactListCreated(cl);

                notifyContactsPresenceUpdated(contacts.toArray(new Contact[contacts.size()]));
            }
             */

            //since we don't show lists anymore, let's just load all entries together


            ContactList cl;

            try {
                cl = mContactListManager.getDefaultContactList();
            } catch (ImException e1) {
                debug(TAG,"couldn't read default list");
                cl = null;
            }

            if (cl == null)
            {
                String generalGroupName = mContext.getString(R.string.buddies);

                Collection<Contact> contacts = new ArrayList<Contact>();
                XmppAddress groupAddress = new XmppAddress(generalGroupName);

                cl = new ContactList(groupAddress,generalGroupName, true, contacts, this);

                notifyContactListCreated(cl);
            }

            for (RosterEntry rEntry : roster.getEntries())
            {
                String address = rEntry.getUser();
                String name = rEntry.getName();

                if (mUser.getAddress().getBareAddress().equals(address)) //don't load a roster for yourself
                    continue;

                Contact contact = getContact(address);

                if (contact == null)
                {
                    XmppAddress xAddr = new XmppAddress(address);

                    if (name == null || name.length() == 0)
                        name = xAddr.getUser();

                    contact = new Contact(xAddr,name);

                }
                
                requestPresenceRefresh(address);
                                
                if (!cl.containsContact(contact))
                {
                    try {
                        cl.addExistingContact(contact);
                    } catch (ImException e) {
                        debug(TAG,"could not add contact to list: " + e.getLocalizedMessage());
                    }
                }
                

            }

            notifyContactListLoaded(cl);
            notifyContactListsLoaded();

        }

     // Runs in executor thread
        public void addContactsToList(Collection<String> addresses) {

            debug(TAG, "add contacts to lists");

            if (mConnection == null)
                return;

            ContactList cl;

            try {
                cl = mContactListManager.getDefaultContactList();
            } catch (ImException e1) {
                debug(TAG,"couldn't read default list");
                cl = null;
            }

            if (cl == null)
            {
                String generalGroupName = mContext.getString(R.string.buddies);

                Collection<Contact> contacts = new ArrayList<Contact>();
                XmppAddress groupAddress = new XmppAddress(generalGroupName);

                cl = new ContactList(groupAddress,generalGroupName, true, contacts, this);

                notifyContactListCreated(cl);
            }

            for (String address : addresses)
            {

                if (mUser.getAddress().getBareAddress().equals(address)) //don't load a roster for yourself
                    continue;

                Contact contact = getContact(address);

                if (contact == null)
                {
                    XmppAddress xAddr = new XmppAddress(address);

                    contact = new Contact(xAddr,xAddr.getUser());

                }

                //org.jivesoftware.smack.packet.Presence p = roster.getPresence(contact.getAddress().getBareAddress());
                //qPresence.push(p);

                if (!cl.containsContact(contact))
                {
                    try {
                        cl.addExistingContact(contact);
                    } catch (ImException e) {
                        debug(TAG,"could not add contact to list: " + e.getLocalizedMessage());
                    }
                }

            }

            notifyContactListLoaded(cl);
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
                
                qPresence.push(presence);
                 
            }

            @Override
            public void entriesUpdated(Collection<String> addresses) {

                
                for (String address :addresses)
                {

                    requestPresenceRefresh(address);
                    
                }
            }

            @Override
            public void entriesDeleted(Collection<String> addresses) {

                ContactList cl;
                try {
                    cl = mContactListManager.getDefaultContactList();

                    for (String address : addresses)
                    {
                        requestPresenceRefresh(address);

                        Contact contact = mContactListManager.getContact(XmppAddress.stripResource(address));
                        mContactListManager.notifyContactListUpdated(cl, ContactListListener.LIST_CONTACT_REMOVED, contact);
                    }
                    

                } catch (ImException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            @Override
            public void entriesAdded(Collection<String> addresses) {

                try
                {
                    if (mContactListManager.getState() == LISTS_LOADED)
                    {
                        
                        for (String address : addresses)
                        {
        
                            Contact contact = getContact(address);
                            
                            requestPresenceRefresh(address);

                            if (contact == null)
                            {
                                XmppAddress xAddr = new XmppAddress(address);
                                contact = new Contact(xAddr,xAddr.getUser());
        
                            }
        
                            try
                            {
                                ContactList cl = mContactListManager.getDefaultContactList();
                                if (!cl.containsContact(contact))
                                    cl.addExistingContact(contact);
                                 
                            }
                            catch (Exception e)
                            {
                                debug(TAG,"could not add contact to list: " + e.getLocalizedMessage());

                            }
                        
        
                        }
                        
                    }
                }
                catch (Exception e)
                {
                    Log.d(ImApp.LOG_TAG,"error adding contacts",e);
                }
            }
        };




        @Override
        protected ImConnection getConnection() {
            return XmppConnection.this;
        }

        @Override
        protected void doRemoveContactFromListAsync(Contact contact, ContactList list) {
            // FIXME synchronize this to executor thread
            if (mConnection == null)
                return;
            
            String address = contact.getAddress().getAddress();

            //otherwise, send unsub message and delete from local contact database
            org.jivesoftware.smack.packet.Presence presence = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.unsubscribe);            
            presence.setTo(address);
            sendPacket(presence);
            
            presence = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.unsubscribed);            
            presence.setTo(address);
            sendPacket(presence);
            
            try {
                RosterEntry entry = mRoster.getEntry(address);
                RosterGroup group = mRoster.getGroup(list.getName());

                if (group == null) {
                    debug(TAG, "could not find group " + list.getName() + " in roster");
                    if (mRoster != null)
                        mRoster.removeEntry(entry);
                }
                else
                {
                    group.removeEntry(entry);
                    entry = mRoster.getEntry(address);
                    
                    // Remove from Roster if this is the last group
                    if (entry != null && entry.getGroups() != null && entry.getGroups().size() <= 1)
                        mRoster.removeEntry(entry);

                }

            } catch (XMPPException e) {
                debug(TAG, "remove entry failed: " + e.getMessage());
                throw new RuntimeException(e);
            }


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
        protected void doAddContactToListAsync(Contact contact, ContactList list) throws ImException {
            debug(TAG, "add contact to " + list.getName());

            if (mConnection.isConnected())
            {
                org.jivesoftware.smack.packet.Presence reqSubscribe = new org.jivesoftware.smack.packet.Presence(
                        org.jivesoftware.smack.packet.Presence.Type.subscribe);
                reqSubscribe.setTo(contact.getAddress().getBareAddress());
                sendPacket(reqSubscribe);
                
                org.jivesoftware.smack.packet.Presence reqSubscribed = new org.jivesoftware.smack.packet.Presence(
                        org.jivesoftware.smack.packet.Presence.Type.subscribed);
                reqSubscribed.setTo(contact.getAddress().getBareAddress());
                sendPacket(reqSubscribed);

               
                String[] groups = new String[] { list.getName() };
                try {
                    RosterEntry rEntry = mRoster.getEntry(contact.getAddress().getBareAddress());
                    RosterGroup rGroup = mRoster.getGroup(list.getName());

                    if (rGroup == null)
                    {
                        if (rEntry == null)
                            mRoster.createEntry (contact.getAddress().getBareAddress(), contact.getName(), null);

                    }
                    else if (rEntry == null)
                    {
                        mRoster.createEntry(contact.getAddress().getBareAddress(), contact.getName(), groups);

                    }

                } catch (XMPPException e) {

                    debug(TAG,"error updating remote roster",e);
                    throw new ImException("error updating remote roster");
                } catch (IllegalStateException e) {
                    String msg = "Not logged in to server while updating remote roster";
                    debug(TAG, msg, e);
                    throw new ImException(msg);
                }

                do_loadContactLists();
                notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_ADDED, contact);

            }
        }

        @Override
        public void declineSubscriptionRequest(Contact contact) {
            debug(TAG, "decline subscription");
            org.jivesoftware.smack.packet.Presence response = new org.jivesoftware.smack.packet.Presence(
                    org.jivesoftware.smack.packet.Presence.Type.unsubscribed);
            response.setTo(contact.getAddress().getBareAddress());
            sendPacket(response);
            try {
                mContactListManager.getSubscriptionRequestListener().onSubscriptionDeclined(contact, mProviderId, mAccountId);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void approveSubscriptionRequest(final Contact contact) {
            
            
            new Thread(new Runnable()
            {
                
                public void run ()
                {
                    debug(TAG, "approve subscription: " + contact.getAddress().getAddress());
                    
                    try
                    {
     
                        doAddContactToListAsync(contact, getContactListManager().getDefaultContactList());
                        mContactListManager.getSubscriptionRequestListener().onSubscriptionApproved(contact, mProviderId, mAccountId);
       
                        
                    } catch (ImException e) {
                        debug (TAG, "error responding to subscription approval: " + e.getLocalizedMessage());
        
                    }
                    catch (RemoteException e) {
                        debug (TAG, "error responding to subscription approval: " + e.getLocalizedMessage());
        
                    }
                }
            }).start();
                
        }

        @Override
        public Contact[] createTemporaryContacts(String[] addresses) {
            // debug(TAG, "create temporary " + address);

            Contact[] contacts = new Contact[addresses.length];

            int i = 0;

            for (String address : addresses)
            {
                contacts[i++] = makeContact(address);
            }

            notifyContactsPresenceUpdated(contacts);
            return contacts;
        }

        @Override
        protected void doSetContactName(String address, String name) throws ImException {
            Roster roster = mConnection.getRoster();
            RosterEntry entry = roster.getEntry(address);
            // confirm entry still exists
            if (entry == null) {
                return;
            }
            // set name
            entry.setName(name);
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

        if (getState() == SUSPENDED) {
            debug(TAG, "heartbeat during suspend");
            return;
        }

        if (mConnection == null && mRetryLogin) {
            debug(TAG, "reconnect with login");
            do_login();
            return;
        }

        if (mConnection == null)
            return;

        if (mNeedReconnect) {
            reconnect();
        } else if (!mConnection.isConnected() && getState() == LOGGED_IN) {
            // Smack failed to tell us about a disconnect
            debug(TAG, "reconnect on unreported state change");
            setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network disconnected"));
            force_reconnect();
        } else if (getState() == LOGGED_IN) {
            if (PING_ENABLED) {
                // Check ping on every heartbeat.  checkPing() will return true immediately if we already checked.
                if (!checkPing()) {
                    debug(TAG, "reconnect on ping failed: " + mUser.getAddress().getAddress());
                    setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network timeout"));
                    maybe_reconnect();
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

        }


        public void shutdown() {
            if (socket != null)
            {
                try {
                    // Be forceful in shutting down since SSL can get stuck
                    try {
                        socket.shutdownInput();
                     } catch (Exception e) {   }

                    socket.close();
                    shutdown(new org.jivesoftware.smack.packet.Presence(
                        org.jivesoftware.smack.packet.Presence.Type.unavailable));


                } catch (Exception e) {
                    Log.e(TAG, "error on shutdown()", e);
                }
            }
        }
    }

    @Override
    public void networkTypeChanged() {

        super.networkTypeChanged();

        execute(new Runnable() {
            @Override
            public void run() {
                if (mState == SUSPENDED || mState == SUSPENDING)
                {
                    debug(TAG, "network type changed");
                    mNeedReconnect = false;
                    setState(LOGGING_IN, null);
                    reconnect();
                }
            }
        });

    }

    /*
     * Force a shutdown and reconnect, unless we are already reconnecting.
     *
     * Runs in executor thread
     */
    private void force_reconnect() {
        debug(TAG, "force_reconnect mNeedReconnect=" + mNeedReconnect + " state=" + getState()
                + " connection?=" + (mConnection != null));

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
            clearPing();
            if (mConnection.isConnected()) {
                debug(TAG,"reconnect while already connected, assuming good: " + mConnection);
                mNeedReconnect = false;
                setState(LOGGED_IN, null);
                return;
            }
            debug(TAG, "reconnect");

            try {
                if (mStreamHandler.isResumePossible()) {
                    // Connect without binding, will automatically trigger a resume
                    debug(TAG, "mStreamHandler resume");
                    mConnection.connect(false);
                    initServiceDiscovery();
                } else {
                    debug(TAG, "reconnection on network change failed: " + mUser.getAddress().getAddress());

                    mConnection = null;
                    mNeedReconnect = true;
                    setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, null));

                    while (mNeedReconnect)
                    {
                        do_login();
                        
                        if (mNeedReconnect)
                            try { Thread.sleep(3000);}
                            catch (Exception e){}
                    }
                    
                }
            } catch (Exception e) {
                if (mStreamHandler != null)
                    mStreamHandler.quickShutdown();

                mConnection = null;
                debug(TAG, "reconnection attempt failed", e);
                // Smack incorrectly notified us that reconnection was successful, reset in case it fails
                mNeedReconnect = false;
                setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));

                //while (mNeedReconnect)
                  //  do_login();

            }
        } else {
            mNeedReconnect = false;
            mConnection = null;
            debug(TAG, "reconnection on network change failed");

            setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR,
                    "reconnection on network change failed"));

            //while (mNeedReconnect)
              //  do_login();

        }
    }

    @Override
    protected void setState(int state, ImErrorInfo error) {
        debug(TAG, "setState to " + state);
        super.setState(state, error);
        
        if (state == LOGGED_IN)
        {
            //update and send new presence packet out
            mUserPresence = new Presence(Presence.AVAILABLE, "", Presence.CLIENT_TYPE_MOBILE);
            sendPresencePacket();  
            
            //request presence of remote contact for all existing sessions 
            for (ChatSessionAdapter session : mSessionManager.getAdapter().getActiveChatSessions())
            {
                requestPresenceRefresh(session.getAddress());
            }

            mChatGroupManager.reconnectAll();
        }
    }    
    
    public void debug(String tag, String msg) {
        //  if (Log.isLoggable(TAG, Log.DEBUG)) {
        if (Debug.DEBUG_ENABLED) {
            Log.d(tag, "" + mGlobalId + " : " + msg);
        }
    }

    public void debug(String tag, String msg, Exception e) {
        if (Debug.DEBUG_ENABLED) {
            Log.e(tag, "" + mGlobalId + " : " + msg,e);
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


    public boolean registerAccount (Imps.ProviderSettings.QueryMap providerSettings, String username, String password, Map<String,String> params) throws Exception
    {

        initConnection(providerSettings, username);

        if (mConnection.getAccountManager().supportsAccountCreation())
        {
            mConnection.getAccountManager().createAccount(username, password, params);

            return true;

        }
        else
        {
            return false;//not supported
        }


    }

    private Contact handlePresenceChanged(org.jivesoftware.smack.packet.Presence presence) {

        if (presence == null || presence.getFrom() == null) //our presence isn't really valid
            return null;

        String from = presence.getFrom();
        
        if (presence.getType() == Type.error)
        {            
            if (mRoster == null)
                return null;
            
            presence = mRoster.getPresence(from);
        }
        
        if (TextUtils.isEmpty(from))
            return null;
        
        XmppAddress xaddress = new XmppAddress(from);

        if (mUser.getAddress().getBareAddress().equals(xaddress.getBareAddress())) //ignore presence from yourself
            return null;

        String status = presence.getStatus();

        Presence p = new Presence(parsePresence(presence), status, null, null,
                Presence.CLIENT_TYPE_DEFAULT);

        //this is only persisted in memory
        p.setPriority(presence.getPriority());
        
        // Get presence from the Roster to handle priorities and such
        // TODO: this causes bad network and performance issues
        //   if (presence.getType() == Type.available) //get the latest presence for the highest priority
        Contact contact = mContactListManager.getContact(xaddress.getBareAddress());
        
        String[] presenceParts = presence.getFrom().split("/");
        if (presenceParts.length > 1)
            p.setResource(presenceParts[1]);

        if (contact == null && presence.getType() == Type.subscribe) {

            XmppAddress xAddr = new XmppAddress(presence.getFrom());

            if (mRoster == null)
                return null;
            
            RosterEntry rEntry = mRoster.getEntry(xAddr.getBareAddress());

            String name = null;

            if (rEntry != null)
                name = rEntry.getName();

            if (name == null || name.length() == 0)
                name = xAddr.getUser();

            contact = new Contact(xAddr,name);

            try {
                if (!mContactListManager.getDefaultContactList().containsContact(contact.getAddress()))
                {
                    mContactListManager.getDefaultContactList().addExistingContact(contact);

                }
            } catch (ImException e) {

                debug(TAG,"unable to add new contact to default list: " + e.getLocalizedMessage());

            }


        }
        else if (contact == null)
        {
            return null; //do nothing if we don't have a contact
        }

        if (presence.getType() == Type.subscribe) {
            debug(TAG,"got subscribe request: " + presence.getFrom());

            try
            {
                mContactListManager.getSubscriptionRequestListener().onSubScriptionRequest(contact, mProviderId, mAccountId);
            }
            catch (RemoteException e)
            {
                Log.e(TAG,"remote exception on subscription handling",e);
            }
        }
        else if (presence.getType() == Type.subscribed) {
            debug(TAG,"got subscribed confirmation request: " + presence.getFrom());
            try
            {
                mContactListManager.getSubscriptionRequestListener().onSubscriptionApproved(contact, mProviderId, mAccountId);
            }
            catch (RemoteException e)
            {
                Log.e(TAG,"remote exception on subscription handling",e);
            }
        }
        else if (presence.getType() == Type.unsubscribe) {
            debug(TAG,"got unsubscribe request: " + presence.getFrom());

            //TBD how to handle this
            //     mContactListManager.getSubscriptionRequestListener().onUnSubScriptionRequest(contact);
        }
        else if (presence.getType() == Type.unsubscribed) {
            debug(TAG,"got unsubscribe request: " + presence.getFrom());
            try
            {
                mContactListManager.getSubscriptionRequestListener().onSubscriptionDeclined(contact, mProviderId, mAccountId);

            }
            catch (RemoteException e)
            {
                Log.e(TAG,"remote exception on subscription handling",e);
            }

        }
        else 
        {
            //this is typical presence, let's get the latest/highest priority
            debug(TAG,"got presence:: " + presence.getFrom() + "=" + p.getStatusText());
            
            if (contact.getPresence() != null)
            {                
                Presence pOld = contact.getPresence();

                if (pOld.getResource() != null && pOld.getResource().equals(p.getResource())) //if the same resource as the existing one, then update it
                {                    
                    contact.setPresence(p);
                }
                else if (p.getPriority() >= pOld.getPriority()) //if priority is higher, then override    
                {
                    contact.setPresence(p);                   
                    
                }
                
                if (p.getStatus() != Imps.Presence.AVAILABLE)
                {
                    //if offline, let's check for another online presence
                    presence = mRoster.getPresence(presence.getFrom());
                    p = new Presence(parsePresence(presence), status, null, null,
                            Presence.CLIENT_TYPE_DEFAULT);

                    //this is only persisted in memory
                    p.setPriority(presence.getPriority());
                    contact.setPresence(p);
                    
                }
                

            }
            else
            {

                //we don't have a presence yet so set one
                contact.setPresence(p);
            }
            
        }
        

        
        return contact;
    }

    private void initPresenceProcessor ()
    {
        mTimerPresence = new Timer();

        mTimerPresence.scheduleAtFixedRate(new TimerTask() {

            public void run() {


                if (qPresence.size() > 0)
                {
                    ArrayList<Contact> alUpdate = new ArrayList<Contact>();
                    
                    org.jivesoftware.smack.packet.Presence p = null;
                    Contact contact = null;

                    while (qPresence.peek() != null)
                    {
                        p = qPresence.pop();
                        contact = handlePresenceChanged(p);
                        if (contact != null)
                        {
                            alUpdate.add(contact);
                        }

                    }
                    
                    //Log.d(ImApp.LOG_TAG,"XMPP processed presence q=" + alUpdate.size());                    
                    mContactListManager.notifyContactsPresenceUpdated(alUpdate.toArray(new Contact[alUpdate.size()]));
                    loadVCardsAsync();

                }
                
             }

          }, 1000, 5000);
    }
    
    Timer mTimerPackets = null;
    
    private void initPacketProcessor ()
    {
        mTimerPackets = new Timer();

        mTimerPackets.scheduleAtFixedRate(new TimerTask() {

            public void run() {

                try
                {
                    org.jivesoftware.smack.packet.Packet packet = null;
                    
                    if (qPacket.size() > 0)
                        while (qPacket.peek()!=null)
                        {
                            packet = qPacket.poll();
                                    
                            if (mConnection == null || (!mConnection.isConnected())) {
                                debug(TAG, "postponed packet to " + packet.getTo()
                                        + " because we are not connected");
                                postpone(packet);
                                return;
                            }
                            try {
                                mConnection.sendPacket(packet);
                            } catch (IllegalStateException ex) {
                                postpone(packet);
                               debug(TAG, "postponed packet to " + packet.getTo()
                                        + " because socket is disconnected");
                            }
                        }


                }
                catch (Exception e)
                {
                    Log.e(ImApp.LOG_TAG,"error processing presence",e);
                }


             }

          }, 500, 500);
    }

}
