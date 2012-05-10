package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.otr.app.im.engine.Address;
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
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.Presence;
import info.guardianproject.otr.app.im.provider.Imps;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.harmony.javax.security.auth.callback.Callback;
import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.jivesoftware.smack.JmDNSService;
import org.jivesoftware.smack.LLChat;
import org.jivesoftware.smack.LLChatListener;
import org.jivesoftware.smack.LLMessageListener;
import org.jivesoftware.smack.LLPresence;
import org.jivesoftware.smack.LLPresence.Mode;
import org.jivesoftware.smack.LLPresenceListener;
import org.jivesoftware.smack.LLService;
import org.jivesoftware.smack.LLServiceStateListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.LLServiceDiscoveryManager;

import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Debug;
import android.os.Parcel;
import android.util.Log;

public class LLXmppConnection extends ImConnection implements CallbackHandler {

    final static String TAG = "Gibberbot.LLXmppConnection";

    private XmppContactList mContactListManager;
    private Contact mUser;

    private XmppChatSessionManager mSessionManager;

    private ThreadPoolExecutor mExecutor;

    private long mAccountId = -1;
    private long mProviderId = -1;

    private final static int SOTIMEOUT = 15000;

    private LLService mService;

    private MulticastLock mcLock;

    private WifiLock wifiLock;

    private InetAddress ipAddress;

    private String serviceName;
    
    static {
        LLServiceDiscoveryManager.addServiceListener();
    }

    public LLXmppConnection(Context context) {
        super(context);

        SmackConfiguration.setPacketReplyTimeout(SOTIMEOUT);

        // Create a single threaded executor. This will serialize actions on the
        // underlying connection.
        createExecutor();

        DeliveryReceipts.addExtensionProviders();

        LLServiceDiscoveryManager.setIdentityName("Gibberbot");
        LLServiceDiscoveryManager.setIdentityType("phone");
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

    public void join() throws InterruptedException {
        ExecutorService oldExecutor = mExecutor;
        createExecutor();
        oldExecutor.shutdown();
        oldExecutor.awaitTermination(10, TimeUnit.SECONDS);
    }

    public void sendPacket(final org.jivesoftware.smack.packet.Message message) {
        execute(new Runnable() {
            @Override
            public void run() {
                LLChat chat;
                try {
                    chat = mService.getChat(message.getTo());
                    chat.sendMessage(message);
                } catch (XMPPException e) {
                    Log.e(TAG, "Could not send message", e);
                }
            }
        });
    }

    @Override
    protected void doUpdateUserPresenceAsync(Presence presence) {

        String statusText = presence.getStatusText();
        Mode mode = Mode.avail;
        if (presence.getStatus() == Presence.AWAY) {
            mode = Mode.away;
        } else if (presence.getStatus() == Presence.IDLE) {
            mode = Mode.away;
        } else if (presence.getStatus() == Presence.DO_NOT_DISTURB) {
            mode = Mode.dnd;
        } else if (presence.getStatus() == Presence.OFFLINE) {
            statusText = "Offline";
        }
        mService.getLocalPresence().setStatus(mode);
        mService.getLocalPresence().setMsg(statusText);
        try {
            mService.updatePresence(mService.getLocalPresence());
        } catch (XMPPException e) {
            Log.e(TAG, "Could not update presence", e);
        }

        mUserPresence = presence;
        notifyUserPresenceUpdated();
    }

    @Override
    public int getCapability() {
        return ImConnection.CAPABILITY_SESSION_REESTABLISHMENT;
    }

    @Override
    public ChatGroupManager getChatGroupManager() {
        return null;
    }

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
        return new int[] { Presence.AVAILABLE, Presence.AWAY, Presence.DO_NOT_DISTURB, };
    }

    @Override
    public void loginAsync(long accountId, String passwordTemp, long providerId, boolean retry) {
        mAccountId = accountId;
        mProviderId = providerId;
        execute(new Runnable() {
            @Override
            public void run() {
                do_login();
            }
        });
    }

    public void do_login() {
        ContentResolver contentResolver = mContext.getContentResolver();
        Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                contentResolver, mProviderId, false, null);
        // providerSettings is closed in initConnection()
        String userName = Imps.Account.getUserName(contentResolver, mAccountId);
        String domain = providerSettings.getDomain();
        providerSettings.close(); // close this, which was opened in do_login()

        try {
            initConnection(userName, domain);
        } catch (Exception e) {
            Log.w(TAG, "login failed", e);
            ImErrorInfo info = new ImErrorInfo(ImErrorInfo.UNKNOWN_ERROR, e.getMessage());
            setState(DISCONNECTED, info);
            mService = null;
        }
    }

    public void setProxy(String type, String host, int port) {
        // Ignore proxies for mDNS
    }

    // Runs in executor thread
    private void initConnection(String userName, String domain)
            throws Exception {
        setState(LOGGING_IN, null);
        mUserPresence = new Presence(Presence.AVAILABLE, "", null, null, Presence.CLIENT_TYPE_DEFAULT);

        domain = domain.replace('.', '_');
        serviceName = userName + "@" + domain;
        LLPresence presence = new LLPresence(serviceName);

        ipAddress = getMyAddress(serviceName, true);
        
        if (ipAddress == null) {
            ImErrorInfo info = new ImErrorInfo(ImErrorInfo.WIFI_NOT_CONNECTED_ERROR, "network connection is required");
            setState(DISCONNECTED, info);
            return;
        }
            
        mService = JmDNSService.create(presence, ipAddress);
        mService.addServiceStateListener(new LLServiceStateListener() {
            public void serviceNameChanged(String newName, String oldName) {
                debug(TAG, "Service named changed from " + oldName + " to " + newName + ".");
            }

            public void serviceClosed() {
                debug(TAG, "Service closed");
                if (getState() != SUSPENDED) {
                    setState(DISCONNECTED, null);
                }
                releaseLocks();
            }

            public void serviceClosedOnError(Exception e) {
                debug(TAG, "Service closed on error");
                ImErrorInfo info = new ImErrorInfo(ImErrorInfo.UNKNOWN_ERROR, e.getMessage());
                setState(DISCONNECTED, info);
                releaseLocks();
            }

            public void unknownOriginMessage(org.jivesoftware.smack.packet.Message m) {
                debug(TAG, "This message has unknown origin:");
                debug(TAG, m.toXML());
            }
        });

        // Adding presence listener.
        mService.addPresenceListener(new LLPresenceListener() {
            public void presenceRemove(final LLPresence presence) {
                execute(new Runnable() {
                    public void run() {
                        mContactListManager.handlePresenceChanged(presence, true);
                    }
                });
            }

            public void presenceNew(final LLPresence presence) {
                execute(new Runnable() {
                    public void run() {
                        mContactListManager.handlePresenceChanged(presence, false);
                    }
                });
            }
        });

        debug(TAG, "Preparing link-local service discovery");
        LLServiceDiscoveryManager disco = LLServiceDiscoveryManager.getInstanceFor(mService);

        disco.addFeature(DeliveryReceipts.NAMESPACE);

        // Start listen for Link-local chats
        mService.addLLChatListener(new LLChatListener() {
            public void newChat(LLChat chat) {
                chat.addMessageListener(new LLMessageListener() {
                    public void processMessage(LLChat chat,
                            org.jivesoftware.smack.packet.Message message) {
                        String address = message.getFrom();
                        ChatSession session = findOrCreateSession(address);
                        DeliveryReceipts.DeliveryReceipt dr =
                                (DeliveryReceipts.DeliveryReceipt)message.getExtension("received", DeliveryReceipts.NAMESPACE);
                        if (dr != null) {
                            debug(TAG, "got delivery receipt for " + dr.getId());
                            session.onMessageReceipt(dr.getId());
                        }
                        if (message.getBody() == null)
                            return;
                        Message rec = new Message(message.getBody());
                        rec.setTo(mUser.getAddress());
                        rec.setFrom(session.getParticipant().getAddress());
                        rec.setDateTime(new Date());
                        session.onReceiveMessage(rec);
                        
                        if (message.getExtension("request", DeliveryReceipts.NAMESPACE) != null) {
                            debug(TAG, "got delivery receipt request");
                            // got XEP-0184 request, send receipt
                            sendReceipt(message);
                            session.onReceiptsExpected();
                        }
                    }
                });
            }

            public void chatInvalidated(LLChat chat) {
                // TODO
            }
        });

        String xmppName = userName + '@' + domain;
        mUser = new Contact(new XmppAddress(userName, xmppName), xmppName);
        
        // Initiate Link-local message session
        mService.init();

        debug(TAG, "logged in");
        setState(LOGGED_IN, null);
    }

    private InetAddress getMyAddress(final String serviceName, boolean doLock) {
        WifiManager wifi = (WifiManager)mContext.getSystemService( Context.WIFI_SERVICE );

        WifiInfo connectionInfo = wifi.getConnectionInfo();
        if (connectionInfo == null || connectionInfo.getBSSID() == null) {
            Log.w(TAG, "Not connected to wifi.  This may not work.");
            // Get the IP the usual Java way
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            return inetAddress;
                        }
                    }
                }        
            } catch (SocketException e) {
                Log.e(TAG, "while enumerating interfaces", e);
                return null;
            }
        }
        
        int ip = connectionInfo.getIpAddress();
        InetAddress address;
        try {
            address = InetAddress.getByAddress(new byte[] { (byte)((ip)&0xff),
                                                  (byte)((ip>>8)&0xff),
                                                  (byte)((ip>>16)&0xff),
                                                  (byte)((ip>>24)&0xff)});
        } catch (UnknownHostException e) {
            Log.e(TAG, "unknown host exception when converting ip address");
            return null;
        }
        
        if (doLock) {
            mcLock = wifi.createMulticastLock(serviceName);
            mcLock.acquire();
            // HIGH_PERF is required for some devices to listen to multicast while screen is off
            wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, serviceName);
            wifiLock.acquire();
        }
        
        return address;
    }

    private void releaseLocks() {
        if (mcLock != null)
            mcLock.release();
        mcLock = null;
        if (wifiLock != null)
            wifiLock.release();
        wifiLock = null;
    }

    public void sendReceipt(org.jivesoftware.smack.packet.Message msg) {
        debug(TAG, "sending XEP-0184 ack to " + msg.getFrom() + " id=" + msg.getPacketID());
        org.jivesoftware.smack.packet.Message ack = new org.jivesoftware.smack.packet.Message(
                msg.getFrom(), msg.getType());
        ack.addExtension(new DeliveryReceipts.DeliveryReceipt(msg.getPacketID()));
        sendPacket(ack);
    }

    void disconnected(ImErrorInfo info) {
        Log.w(TAG, "disconnected");
        setState(DISCONNECTED, info);
    }

    protected static int parsePresence(LLPresence presence, boolean offline) {
        if (offline)
            return Presence.OFFLINE;
        int type = Presence.AVAILABLE;
        Mode rmode = presence.getStatus();

        if (rmode == Mode.away)
            type = Presence.AWAY;
        else if (rmode == Mode.dnd)
            type = Presence.DO_NOT_DISTURB;

        return type;
    }

    protected static String parseAddressBase(String from) {
        return from.replaceFirst("/.*", "");
    }

    protected static String parseAddressName(String from) {
        return from.replaceFirst("@.*", "");
    }

    @Override
    public void logoutAsync() {
        // TODO invoke join() here?
        execute(new Runnable() {
            @Override
            public void run() {
                logout();
            }
        });
    }

    // Force immediate logout
    public synchronized void logout() {
        if (mService != null) {
            mService.close();
            mService = null;
        }
    }

    @Override
    public void suspend() {
        execute(new Runnable() {
            @Override
            public void run() {
                do_suspend();
            }
        });
    }

    private void do_suspend() {
        debug(TAG, "suspend");
        setState(SUSPENDED, null);
        logout();
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
        Contact contact = new Contact(new XmppAddress(name, address), address);

        return contact;
    }

    private final class XmppChatSessionManager extends ChatSessionManager {
        @Override
        public void sendMessageAsync(ChatSession session, Message message) {
            org.jivesoftware.smack.packet.Message msg = new org.jivesoftware.smack.packet.Message(
                    message.getTo().getFullName(), org.jivesoftware.smack.packet.Message.Type.chat);
            msg.addExtension(new DeliveryReceipts.DeliveryReceiptRequest());
            msg.setBody(message.getBody());
            debug(TAG, "sending packet ID " + msg.getPacketID());
            message.setID(msg.getPacketID());
            sendPacket(msg);
        }

        ChatSession findSession(String address) {
            for (Iterator<ChatSession> iter = mSessions.iterator(); iter.hasNext();) {
                ChatSession session = iter.next();
                if (session.getParticipant().getAddress().getFullName().equals(address))
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
        private void do_loadContactLists() {
            String generalGroupName = "Buddies";

            Collection<Contact> contacts = new ArrayList<Contact>();
            ContactList cl = new ContactList(mUser.getAddress(), generalGroupName, true, contacts, this);
            notifyContactListCreated(cl);
            notifyContactListsLoaded();
        }
        
        @Override
        protected void setListNameAsync(final String name, final ContactList list) {
            execute(new Runnable() {
                @Override
                public void run() {
                    // TODO
                }
            });
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

        private void handlePresenceChanged(LLPresence presence, boolean offline) {
            // Create default lists on first presence received
            if (mContactListManager.getState() != ContactListManager.LISTS_LOADED) {
                do_loadContactLists();
            }
            
            String name = presence.getFirstName();
            String address = presence.getServiceName();

            mContactListManager.doAddContact(name, address);

            XmppAddress xaddress = new XmppAddress(name, address);

            Contact contact = getContact(xaddress.getFullName());

            int type = parsePresence(presence, offline);

            if (contact == null) {
                contact = new Contact(xaddress, name);

                debug(TAG, "got presence updated for NEW user: "
                           + contact.getAddress().getFullName() + " presence:" + type);
            } else {
                debug(TAG, "Got present update for EXISTING user: "
                           + contact.getAddress().getFullName() + " presence:" + type);

                Presence p = new Presence(type, presence.getMsg(), null, null,
                        Presence.CLIENT_TYPE_DEFAULT);
                contact.setPresence(p);

                Contact[] contacts = new Contact[] { contact };

                notifyContactsPresenceUpdated(contacts);
            }
        }

        @Override
        protected ImConnection getConnection() {
            return LLXmppConnection.this;
        }

        @Override
        protected void doRemoveContactFromListAsync(Contact contact, ContactList list) {
            // TODO
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

        private void doAddContact(String name, String address, ContactList list) {
            Contact contact = makeContact(name, address);
            if (!containsContact(contact))
                    notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_ADDED, contact);
        }

        private void doAddContact(String name, String address) {
            try {
                doAddContact(name, address, getDefaultContactList());
            } catch (ImException e) {
                Log.e(TAG, "Failed to add contact", e);
            }
        }
        
        @Override
        protected void doAddContactToListAsync(String address, ContactList list) throws ImException {
            debug(TAG, "add contact to " + list.getName());
            // TODO
        }

        @Override
        public void declineSubscriptionRequest(String contact) {
            debug(TAG, "decline subscription");
            // TODO
        }

        @Override
        public void approveSubscriptionRequest(String contact) {
            debug(TAG, "approve subscription");
            // TODO
        }

        @Override
        public Contact createTemporaryContact(String address) {
            debug(TAG, "create temporary " + address);
            return makeContact(parseAddressName(address), address);
        }
    }

    public static class XmppAddress extends Address {

        private String address;
        private String name;

        public XmppAddress() {
        }

        public XmppAddress(String name, String address) {
            this.name = name;
            this.address = address;
        }

        public XmppAddress(String address) {
            this.name = parseAddressName(address);
            this.address = address;
        }

        @Override
        public String getFullName() {
            return address;
        }

        @Override
        public String getScreenName() {
            return name;
        }

        @Override
        public void readFromParcel(Parcel source) {
            name = source.readString();
            address = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest) {
            dest.writeString(name);
            dest.writeString(address);
        }

    }

    @Override
    public void networkTypeChanged() {
        super.networkTypeChanged();
    }

    @Override
    protected void setState(int state, ImErrorInfo error) {
        debug(TAG, "setState to " + state);
        super.setState(state, error);
    }

    public static void debug(String tag, String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void handle(Callback[] arg0) throws IOException {

        for (Callback cb : arg0) {
            debug(TAG, cb.toString());
        }

    }

    @Override
    public void reestablishSessionAsync(Map<String, String> sessionContext) {
        execute(new Runnable() {
            public void run() {
                do_login();
            }
        });
    }

    @Override
    public void sendHeartbeat() {
        InetAddress newAddress = getMyAddress(serviceName, false);
        if (!ipAddress.equals(newAddress)) {
            debug(TAG, "new address, reconnect");
            execute(new Runnable() {
                public void run() {
                    do_suspend();
                    do_login();
                }
            });
        }
    }
}
