package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.otr.TorProxyInfo;
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
import info.guardianproject.otr.app.im.plugin.XmppAddress;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.util.DNSUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

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
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.proxy.ProxyInfo.ProxyType;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.VCard;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class XmppConnection extends ImConnection implements CallbackHandler {

    final static String TAG = "GB.XmppConnection";
    private final static boolean DEBUG_ENABLED = false;
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

    private final static String TRUSTSTORE_TYPE = "BKS";
    private final static String TRUSTSTORE_PATH = "cacerts.bks";
    private final static String TRUSTSTORE_PASS = "changeit";
    private final static String KEYMANAGER_TYPE = "X509";
    private final static String SSLCONTEXT_TYPE = "TLS";

    private ServerTrustManager sTrustManager;
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
    
    private final Random rnd = new Random();
    
    // Maintains a sequence counting up to the user configured heartbeat interval
    private int heartbeatSequence = 0;

    public XmppConnection(Context context) {
        super(context);

        synchronized (XmppConnection.class) {
            mGlobalId = mGlobalCount++;
        }

        aContext = context;

        SmackConfiguration.setPacketReplyTimeout(SOTIMEOUT);

        // Create a single threaded executor.  This will serialize actions on the underlying connection.
        createExecutor();

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

    public VCard getVCard(String myJID) {

        VCard vCard = new VCard();

        try {
            // FIXME synchronize this to executor thread
            vCard.load(mConnection, myJID);

            // If VCard is loaded, then save the avatar to the personal folder.
            byte[] bytes = vCard.getAvatar();

            if (bytes != null) {
                try {
                    String filename = vCard.getAvatarHash() + ".jpg";
                    File sdCard = Environment.getExternalStorageDirectory();
                    File file = new File(sdCard, filename);
                    OutputStream output = new FileOutputStream(file);
                    output.write(bytes);
                    output.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (XMPPException ex) {
            ex.printStackTrace();
        }
        return vCard;
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
        // TODO chat groups
        return ImConnection.CAPABILITY_SESSION_REESTABLISHMENT;
    }

    @Override
    public ChatGroupManager getChatGroupManager() {
        // TODO chat groups
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
        // providerSettings is closed in initConnection()
        String userName = Imps.Account.getUserName(contentResolver, mAccountId);
        String password = Imps.Account.getPassword(contentResolver, mAccountId);

        if (mPasswordTemp != null)
            password = mPasswordTemp;

        mNeedReconnect = true;
        setState(LOGGING_IN, null);
        mUserPresence = new Presence(Presence.AVAILABLE, "", Presence.CLIENT_TYPE_MOBILE);

        try {
            if (userName.length() == 0)
                throw new XMPPException("empty username not allowed");
            initConnection(userName, password, providerSettings);

        } catch (Exception e) {
            Log.w(TAG, "login failed", e);
            mConnection = null;
            ImErrorInfo info = new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, e.getMessage());

            if (e == null || e.getMessage() == null) {
                Log.w(TAG, "NPE", e);
                info = new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "unknown error");
                disconnected(info);
                mRetryLogin = false;
            } else if (e.getMessage().contains("not-authorized")
                       || e.getMessage().contains("authentication failed")) {
                Log.w(TAG, "not authorized - will not retry");
                info = new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "invalid user/password");
                disconnected(info);
                mRetryLogin = false;
            } else if (mRetryLogin) {
                Log.w(TAG, "will retry");
                setState(LOGGING_IN, info);
            } else {
                Log.w(TAG, "will not retry");
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
                username = rnd.nextInt(100000)+"";
                password = rnd.nextInt(100000)+"";
                
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
    private void initConnection(String userName, final String password,
            Imps.ProviderSettings.QueryMap providerSettings) throws Exception {

        //android.os.Debug.waitForDebugger();

        boolean allowPlainAuth = providerSettings.getAllowPlainAuth();
        boolean requireTls = providerSettings.getRequireTls();
        boolean doDnsSrv = providerSettings.getDoDnsSrv();
        boolean tlsCertVerify = providerSettings.getTlsCertVerify();
        boolean allowSelfSignedCerts = !tlsCertVerify;
        boolean doVerifyDomain = tlsCertVerify;

        boolean useSASL = true;//!allowPlainAuth;

        String domain = providerSettings.getDomain();
        String requestedServer = providerSettings.getServer();
        if ("".equals(requestedServer))
            requestedServer = null;
        String xmppResource = providerSettings.getXmppResource();
        mPriority = providerSettings.getXmppResourcePrio();
        int serverPort = providerSettings.getPort();

        String server = requestedServer;

        providerSettings.close(); // close this, which was opened in do_login()

        debug(TAG, "TLS required? " + requireTls);
        debug(TAG, "Do SRV check? " + doDnsSrv);
        debug(TAG, "cert verification? " + tlsCertVerify);

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

        // No server requested and SRV lookup wasn't requested or returned nothing - use domain
        if (server == null) {
            debug(TAG, "(use domain) ConnectionConfiguration(" + domain + ", " + serverPort + ", "
                       + domain + ", mProxyInfo);");

            if (mProxyInfo == null)
                mConfig = new ConnectionConfiguration(domain, serverPort);
            else
                mConfig = new ConnectionConfiguration(domain, serverPort, mProxyInfo);

        } else {
            debug(TAG, "(use server) ConnectionConfiguration(" + server + ", " + serverPort + ", "
                       + domain + ", mProxyInfo);");

            if (mProxyInfo == null)
                mConfig = new ConnectionConfiguration(server, serverPort, domain);
            else
                mConfig = new ConnectionConfiguration(server, serverPort, domain, mProxyInfo);
        }

        mConfig.setDebuggerEnabled(DEBUG_ENABLED);
        mConfig.setSASLAuthenticationEnabled(useSASL);

        if (requireTls) {

            mConfig.setSecurityMode(SecurityMode.required);

            SASLAuthentication.supportSASLMechanism("PLAIN", 0);
            SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 1);

        } else {
            // if it finds a cert, still use it, but don't check anything since 
            // TLS errors are not expected by the user
            mConfig.setSecurityMode(SecurityMode.enabled);

            if (allowPlainAuth) {
                SASLAuthentication.supportSASLMechanism("PLAIN", 0);
                SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 1);

            } else {
                SASLAuthentication.unsupportSASLMechanism("PLAIN");
                SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 0);
            }

        }
        // Android has no support for Kerberos or GSSAPI, so disable completely
        SASLAuthentication.unregisterSASLMechanism("KERBEROS_V4");
        SASLAuthentication.unregisterSASLMechanism("GSSAPI");

        mConfig.setVerifyChainEnabled(tlsCertVerify);
        mConfig.setVerifyRootCAEnabled(tlsCertVerify);
        mConfig.setExpiredCertificatesCheckEnabled(tlsCertVerify);
        mConfig.setNotMatchingDomainCheckEnabled(doVerifyDomain && (!allowSelfSignedCerts));
        mConfig.setSelfSignedCertificateEnabled(allowSelfSignedCerts);

        mConfig.setTruststoreType(TRUSTSTORE_TYPE);
        mConfig.setTruststorePath(TRUSTSTORE_PATH);
        mConfig.setTruststorePassword(TRUSTSTORE_PASS);

        // Per XMPP specs, cert must match domain, not SRV lookup result.  Otherwise, DNS spoofing
        // can enable MITM.
        initSSLContext(domain, requestedServer, mConfig);

        // Don't use smack reconnection - not reliable
        mConfig.setReconnectionAllowed(false);
        mConfig.setSendPresence(false);
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
                ChatSession session = findOrCreateSession(address);
                DeliveryReceipts.DeliveryReceipt dr = (DeliveryReceipts.DeliveryReceipt) smackMessage
                        .getExtension("received", DeliveryReceipts.NAMESPACE);
                if (dr != null) {
                    debug(TAG, "got delivery receipt for " + dr.getId());
                    session.onMessageReceipt(dr.getId());
                }
                
                String body = smackMessage.getBody();
                
                if (smackMessage.getError() != null) {
                    if (body == null)
                        body = "";
                    body = body + " - " + smackMessage.getError().toString();
                }
                
                if (body == null)
                    return;
                
                Message rec = new Message(body);
                rec.setTo(mUser.getAddress());
                rec.setFrom(new XmppAddress(smackMessage.getFrom()));
                rec.setDateTime(new Date());
                session.onReceiveMessage(rec);
                if (smackMessage.getExtension("request", DeliveryReceipts.NAMESPACE) != null) {
                    debug(TAG, "got delivery receipt request");
                    // got XEP-0184 request, send receipt
                    sendReceipt(smackMessage);
                    session.onReceiptsExpected();
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

                    contact.setPresence(new Presence(type, presence.getStatus(), null, null,
                            Presence.CLIENT_TYPE_DEFAULT));

                }
            }
        }, new PacketTypeFilter(org.jivesoftware.smack.packet.Presence.class));

        mConnection.connect();

        initServiceDiscovery();

        mConnection.addConnectionListener(new ConnectionListener() {
            /**
             * Called from smack when connect() is fully successful
             * 
             * This is called on the executor thread while we are in reconnect()
             */
            @Override
            public void reconnectionSuccessful() {
                debug(TAG, "reconnection success");
                mNeedReconnect = false;
                setState(LOGGED_IN, null);
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
                Log.w(TAG, "reconnect on error", e);
                if (e.getMessage().contains("conflict")) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            disconnect();
                            disconnected(new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER,
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
        });

        if (server.contains(IS_GOOGLE)) {
            this.mUsername = userName + '@' + domain;
        } else {
            this.mUsername = userName;
        }

        this.mPassword = password;
        this.mResource = xmppResource;

        mStreamHandler = new XmppStreamHandler(mConnection);
        mConnection.login(mUsername, mPassword, mResource);
        mStreamHandler.notifyInitialLogin();

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
            ConnectionConfiguration config) throws Exception {

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

        sslContext = SSLContext.getInstance(SSLCONTEXT_TYPE);
        sTrustManager = new ServerTrustManager(aContext, domain, requestedServer, config);

        sslContext.init(kms, new javax.net.ssl.TrustManager[] { sTrustManager },
                new java.security.SecureRandom());

        config.setCustomSSLContext(sslContext);
        config.setCallbackHandler(this);

    }

    void sslCertificateError() {
        this.disconnect();
    }

    // We must release resources here, because we will not be reused
    void disconnected(ImErrorInfo info) {
        Log.w(TAG, "disconnected");
        join();
        setState(DISCONNECTED, info);
    }

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
        public void loadContactLists() {
            do_loadContactLists();
        }

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

                Contact contact = mContactListManager.getContact(xaddress.getFullName());

                if (contact == null)
                    contact = new Contact(xaddress, name);

                org.jivesoftware.smack.packet.Presence presence = roster.getPresence(address);

                contact.setPresence(new Presence(parsePresence(presence), presence.getStatus(),
                        null, null, Presence.CLIENT_TYPE_DEFAULT));

                contacts.add(contact);

                // getVCard(xaddress.getFullName());  // commented out to fix slow contact loading

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

                ContactList cl = new ContactList(mUser.getAddress(), group.getName(), group
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

            //			private Stack<String> entriesToAdd = new Stack<String>();
            //			private Stack<String> entriesToDel = new Stack<String>();

            @Override
            public void presenceChanged(org.jivesoftware.smack.packet.Presence presence) {

                handlePresenceChanged(presence);

            }

            @Override
            public void entriesUpdated(Collection<String> addresses) {

                debug(TAG, "roster entries updated");

                //entriesAdded(addresses);
            }

            @Override
            public void entriesDeleted(Collection<String> addresses) {

                debug(TAG, "roster entries deleted: " + addresses.size());

                /*
                if (addresses != null)
                	entriesToDel.addAll(addresses);
                
                if (mContactListManager.getState() == ContactListManager.LISTS_LOADED)
                {
                	

                	synchronized (entriesToDel)
                	{
                		while (!entriesToDel.empty())
                			try {
                				Contact contact = mContactListManager.getContact(entriesToDel.pop());
                				mContactListManager.removeContactFromListAsync(contact, mContactListManager.getDefaultContactList());
                			} catch (ImException e) {
                				Log.e(TAG,e.getMessage(),e);
                			}
                	}
                }
                else
                {
                	debug(TAG, "roster delete entries queued");
                }*/
            }

            @Override
            public void entriesAdded(Collection<String> addresses) {

                debug(TAG, "roster entries added: " + addresses.size());

                /*
                if (addresses != null)
                	entriesToAdd.addAll(addresses);
                
                if (mContactListManager.getState() == ContactListManager.LISTS_LOADED)
                {							
                	debug(TAG, "roster entries added");

                	while (!entriesToAdd.empty())
                		try {
                			mContactListManager.addContactToListAsync(entriesToAdd.pop(), mContactListManager.getDefaultContactList());
                		} catch (ImException e) {
                			Log.e(TAG,e.getMessage(),e);
                		}
                }
                else
                {
                	debug(TAG, "roster add entries queued");
                }*/
            }
        };

        private void handlePresenceChanged(org.jivesoftware.smack.packet.Presence presence) {
            String name = parseAddressName(presence.getFrom());
            String address = parseAddressBase(presence.getFrom());

            XmppAddress xaddress = new XmppAddress(name, address);

            if (mConnection == null)
                return;
            
            // Get presence from the Roster to handle priorities and such
            final Roster roster = mConnection.getRoster();
            if (roster != null) {
                presence = roster.getPresence(address);
            }
            int type = parsePresence(presence);

            Contact contact = getContact(xaddress.getFullName());

            if (contact == null) {
                contact = new Contact(xaddress, name);

                debug(TAG, "got presence updated for NEW user: "
                           + contact.getAddress().getFullName() + " presence:" + type);
                //store the latest presence notification for this user in this queue
                //unprocdPresence.put(user, presence);

            } else {
                debug(TAG, "Got present update for EXISTING user: "
                        + contact.getAddress().getFullName() + " presence:" + type);
            }

            Presence p = new Presence(type, presence.getStatus(), null, null,
                    Presence.CLIENT_TYPE_DEFAULT);
            contact.setPresence(p);

            Contact[] contacts = new Contact[] { contact };

            notifyContactsPresenceUpdated(contacts);
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
            String address = contact.getAddress().getFullName();
            try {
                RosterGroup group = roster.getGroup(list.getName());
                if (group == null) {
                    Log.e(TAG, "could not find group " + list.getName() + " in roster");
                    return;
                }
                RosterEntry entry = roster.getEntry(address);
                if (entry == null) {
                    Log.e(TAG, "could not find entry " + address + " in group " + list.getName());
                    return;
                }

                // Remove from Roster if this is the last group
                if (entry.getGroups().size() <= 1)
                    roster.removeEntry(entry);

                group.removeEntry(entry);
            } catch (XMPPException e) {
                Log.e(TAG, "remove entry failed", e);
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
                Log.e(TAG, "failed to add " + contact + " to default list");
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
        } else if (mConnection.isConnected() && getState() == LOGGED_IN) {
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
            if (result == null || result.getError() != null) {
                mPingCollector = null;
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
                    //initServiceDiscovery();
                } else {
                    debug(TAG, "no resume");
                    mConnection.connect();
                    //initServiceDiscovery();
                    if (!mConnection.isAuthenticated()) {
                        // This can happen if a reconnect failed and the smack connection now has wasAuthenticated = false.
                        // It can also happen if auth exception was swallowed by smack.
                        // Try to login manually.

                        Log.e(TAG, "authentication did not happen in connect() - login manually");
                        mConnection.login(mUsername, mPassword, mResource);

                        // Make sure
                        if (!mConnection.isAuthenticated())
                            throw new XMPPException("manual auth failed");

                        // Manually set the state since manual auth doesn't notify listeners
                        mNeedReconnect = false;
                        setState(LOGGED_IN, null);
                    }
                    sendPresencePacket();
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
        if (Log.isLoggable(TAG, Log.DEBUG)) {
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

        sdm.addFeature("http://jabber.org/protocol/disco#info");
        sdm.addFeature(DeliveryReceipts.NAMESPACE);
    }

}
