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
import info.guardianproject.util.DNSUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.proxy.ProxyInfo.ProxyType;
import org.jivesoftware.smackx.packet.VCard;

import android.R;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Environment;
import android.os.Parcel;
import android.util.Log;

public class XmppConnection extends ImConnection {

	private final static String TAG = "Gibberbot.XmppConnection";
	private XmppContactList mContactListManager;
	private Contact mUser;
	
	// watch out, this is a different XMPPConnection class than XmppConnection! ;)
	private MyXMPPConnection mConnection;
	private XmppChatSessionManager mSessionManager;
	private ConnectionConfiguration mConfig;
	private boolean mNeedReconnect;
	private boolean mRetryLogin;
	private Executor mExecutor;

	private PacketCollector mPingCollector;

	private ProxyInfo mProxyInfo = null;
	
	private long mAccountId = -1;
	private long mProviderId = -1;
	private String mPasswordTemp;
	
	private final static int SOTIMEOUT = 15000;
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
	
	public XmppConnection(Context context) {
		super(context);
		aContext = context;
		
		SmackConfiguration.setPacketReplyTimeout(SOTIMEOUT);
		
		mExecutor = Executors.newCachedThreadPool();
	}
	
	public void sendMessage(org.jivesoftware.smack.packet.Message msg) {
	
		if (mConnection != null)
			mConnection.sendPacket(msg);		
	}
	
	 public VCard getVCard(String myJID) {
	        

		// android.os.Debug.waitForDebugger();
		 
	        VCard vCard = new VCard();
	        
	        try {       
	            vCard.load(mConnection, myJID);
	            
	            // If VCard is loaded, then save the avatar to the personal folder.
	            byte[] bytes = vCard.getAvatar();
	            
	            if (bytes != null)
	            {
	            	
	            
	            try {
	            	String filename = vCard.getAvatarHash() + ".jpg";
	                InputStream in = new ByteArrayInputStream(bytes);
	                File sdCard = Environment.getExternalStorageDirectory();
	                File file = new File(sdCard, filename);
	                new FileOutputStream(file).write(bytes);
	                /*
	                BufferedImage bi = javax.imageio.ImageIO.read(in);
	                File outputfile = new File("C://Avatar.jpg");
	                ImageIO.write(bi, "jpg", outputfile);
	                */
	                }
	                catch (Exception e){
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
		
		String statusText = presence.getStatusText();
        Type type = Type.available;
        Mode mode = Mode.available;
        int priority = 20;
        if (presence.getStatus() == Presence.AWAY) {
        	priority = 10;
        	mode = Mode.away;
        }
        else if (presence.getStatus() == Presence.IDLE) {
        	priority = 15;
        	mode = Mode.away;
        }
        else if (presence.getStatus() == Presence.DO_NOT_DISTURB) {
        	priority = 5;
        	mode = Mode.dnd;
        }
        else if (presence.getStatus() == Presence.OFFLINE) {
        	priority = 0;
        	type = Type.unavailable;
        	statusText = "Offline";
        }
		org.jivesoftware.smack.packet.Presence packet = 
        	new org.jivesoftware.smack.packet.Presence(type, statusText, priority, mode);
        mConnection.sendPacket(packet);
		mUserPresence = presence;
        notifyUserPresenceUpdated();
	}

	@Override
	public int getCapability() {
		// TODO chat groups
		return 0;
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
	public HashMap<String, String> getSessionContext() {
		return null;
	}

	@Override
	public int[] getSupportedPresenceStatus() {
		return new int[] {
				Presence.AVAILABLE,
				Presence.AWAY,
				Presence.IDLE,
				Presence.OFFLINE,
				Presence.DO_NOT_DISTURB,
		};
	}

	@Override
	public void loginAsync(long accountId, String passwordTemp, long providerId, boolean retry) {
		mAccountId = accountId;
		mPasswordTemp = passwordTemp;
		mProviderId = providerId;
		mRetryLogin = retry;
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				do_login();
			}
		});
	}
	
	private void do_login() {
		if (mConnection != null) {
			setState(getState(), new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, "still trying..."));
			return;
		}
		ContentResolver contentResolver = mContext.getContentResolver();
		Imps.ProviderSettings.QueryMap providerSettings = 
			new Imps.ProviderSettings.QueryMap(contentResolver,
					mProviderId, false, null);
		// providerSettings is closed in initConnection()
		String userName = Imps.Account.getUserName(contentResolver, mAccountId);
		String password = Imps.Account.getPassword(contentResolver, mAccountId);
		String domain = providerSettings.getDomain();
		
		if (mPasswordTemp != null)
			password = mPasswordTemp;
		
		mNeedReconnect = true;
		setState(LOGGING_IN, null);
		mUserPresence = new Presence(Presence.AVAILABLE, "", null, null, Presence.CLIENT_TYPE_DEFAULT);

		try {
			if (userName.length() == 0)
				throw new XMPPException("empty username not allowed");
			initConnection(userName, password, providerSettings);
			
		} catch (Exception e) {
			Log.w(TAG, "login failed");
			mConnection = null;
			ImErrorInfo info = new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, e.getMessage());
			
			if (e == null || e.getMessage() == null)
			{
				Log.w(TAG, "NPE", e);
				info = new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "unknown error");
				disconnected(info);
				mRetryLogin = false;
			}
			else if (e.getMessage().contains("not-authorized")) {
				Log.w(TAG, "not authorized - will not retry");
				info = new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "invalid user/password");
				disconnected(info);
				mRetryLogin = false;
			}
			else if (mRetryLogin) {
				Log.w(TAG, "will retry");
				setState(LOGGING_IN, info);
			}
			else {
				Log.w(TAG, "will not retry");
				mConnection = null;
				disconnected(info);
			}
			return;
		} finally {
			mNeedReconnect = false;
		}
		
		// TODO should we really be using the same name for both address and name?
		String xmppName = userName + '@' + domain;
		mUser = new Contact(new XmppAddress(userName, xmppName), xmppName);
		setState(LOGGED_IN, null);
		debug(TAG, "logged in");
		
	}

	// TODO shouldn't setProxy be handled in Imps/settings?
	public void setProxy (String type, String host, int port)
	{

		if (type == null)
		{
			 mProxyInfo = ProxyInfo.forNoProxy();
		}
		else
		{
			ProxyInfo.ProxyType pType = ProxyType.valueOf(type);
			mProxyInfo = new ProxyInfo(pType, host, port,"","");
		}
	}
	
	private void initConnection(String userName, final String password, 
			Imps.ProviderSettings.QueryMap providerSettings) throws Exception {

		boolean allowPlainAuth = providerSettings.getAllowPlainAuth();
		boolean requireTls = providerSettings.getRequireTls();
		boolean doDnsSrv = providerSettings.getDoDnsSrv();
		boolean tlsCertVerify = providerSettings.getTlsCertVerify();
		boolean allowSelfSignedCerts = !tlsCertVerify;
		boolean doVerifyDomain = tlsCertVerify;

		// TODO this should be reorged as well as the gmail.com section below
		String domain = providerSettings.getDomain();
		String server = providerSettings.getServer();
		String xmppResource = providerSettings.getXmppResource();
		int serverPort = providerSettings.getPort();
		
		providerSettings.close(); // close this, which was opened in do_login()
		
		
		debug(TAG, "TLS required? " + requireTls);
		debug(TAG, "Do SRV check? " + doDnsSrv);
		debug(TAG, "cert verification? " + tlsCertVerify);
    	
    	if (mProxyInfo == null)
    		 mProxyInfo = ProxyInfo.forNoProxy();

    	// TODO try getting a connection without DNS SRV first, and if that doesn't work and the prefs allow it, use DNS SRV
    	if (doDnsSrv) {
    		
    		//java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
    		//java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
    		
    		debug(TAG, "(DNS SRV) resolving: "+domain);
    	//	mConfig = new ConnectionConfiguration(domain, mProxyInfo);
    		DNSUtil.HostAddress srvHost = DNSUtil.resolveXMPPDomain(domain);
    		server = srvHost.getHost();
    		//serverPort = srvHost.getPort();
    		debug(TAG, "(DNS SRV) resolved: "+domain+"=" + server + ":" + serverPort);
    		
    	}

    	if (server == null) { // no server specified in prefs, use the domain
    		debug(TAG, "(use domain) ConnectionConfiguration("+domain+", "+serverPort+", "+domain+", mProxyInfo);");
    		mConfig = new ConnectionConfiguration(domain, serverPort, domain, mProxyInfo);
  		
    		
    	} else {	
    		debug(TAG, "(use server) ConnectionConfiguration("+server+", "+serverPort+", "+domain+", mProxyInfo);");
    		mConfig = new ConnectionConfiguration(server, serverPort, domain, mProxyInfo);

    		//if domain of login user is the same as server
    		doVerifyDomain = (domain.equals(server));
    		
        	
    	}

    	//mConfig.setDebuggerEnabled(true);
    	mConfig.setSASLAuthenticationEnabled(true);
    	if (requireTls) {
    		mConfig.setSecurityMode(SecurityMode.required);
    		
    		
    		if(allowPlainAuth)
    		{
    			mConfig.setSASLAuthenticationEnabled(false);    	    
    			SASLAuthentication.supportSASLMechanism("PLAIN", 0);

        		SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 1);
    		}
    		else
    		{
    			mConfig.setSASLAuthenticationEnabled(true);  
    			SASLAuthentication.unsupportSASLMechanism("PLAIN");

        		SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 0);
    		}
    		
    		

    		
    	} else {
    		// if it finds a cert, still use it, but don't check anything since 
    		// TLS errors are not expected by the user
    		mConfig.setSecurityMode(SecurityMode.enabled);
    		tlsCertVerify = false;
    		doVerifyDomain = false;
    		allowSelfSignedCerts = true;
    		// without TLS, use DIGEST-MD5 first
    		SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 0);
    		if(allowPlainAuth)
    			SASLAuthentication.supportSASLMechanism("PLAIN", 1);
    		else
    			SASLAuthentication.unsupportSASLMechanism("PLAIN");
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
		
		
		if (server == null)
			initSSLContext(domain, mConfig);
		else
    		initSSLContext(server, mConfig);
		

		//reconnect please, or no?
		mConfig.setReconnectionAllowed(false);
		
		mConfig.setSendPresence(true);
		mConfig.setRosterLoadedAtLogin(true);

		mConnection = new MyXMPPConnection(mConfig);

		//debug(TAG, "ConnnectionConfiguration.getHost: " + mConfig.getHost() + " getPort: " + mConfig.getPort() + " getServiceName: " + mConfig.getServiceName());
		
		Roster roster = mConnection.getRoster();
		roster.setSubscriptionMode(Roster.SubscriptionMode.manual);			
		getContactListManager().listenToRoster(roster);
		
        mConnection.connect();
        
        //debug(TAG,"is secure connection? " + mConnection.isSecureConnection());
        //debug(TAG,"is using TLS? " + mConnection.isUsingTLS());
        
        mConnection.addPacketListener(new PacketListener() {
			
			@Override
			public void processPacket(Packet packet) {
				org.jivesoftware.smack.packet.Message smackMessage = (org.jivesoftware.smack.packet.Message) packet;
				Message rec = new Message(smackMessage.getBody());
				String address = parseAddressBase(smackMessage.getFrom());
				ChatSession session = findOrCreateSession(address);
				rec.setTo(mUser.getAddress());
				rec.setFrom(session.getParticipant().getAddress());
				rec.setDateTime(new Date());
				session.onReceiveMessage(rec);
			}
		}, new MessageTypeFilter(org.jivesoftware.smack.packet.Message.Type.chat));
        
        mConnection.addPacketListener(new PacketListener() {
			
			@Override
			public void processPacket(Packet packet) {
				
				
				org.jivesoftware.smack.packet.Presence presence = (org.jivesoftware.smack.packet.Presence)packet;
				String address = parseAddressBase(presence.getFrom());
				String name = parseAddressName(presence.getFrom());
				Contact contact = findOrCreateContact(name,address);

				
				if (presence.getType() == Type.subscribe) {
					debug(TAG, "sub request from " + address);
					mContactListManager.getSubscriptionRequestListener().onSubScriptionRequest(contact);
				}
				else 
				{
					int type = parsePresence(presence);

					contact.setPresence(new Presence(type, presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT));
					
				}
			}
		}, new PacketTypeFilter(org.jivesoftware.smack.packet.Presence.class));
        
        mConnection.addConnectionListener(new ConnectionListener() {
			@Override
			public void reconnectionSuccessful() {
				debug(TAG, "reconnection success");
				setState(LOGGED_IN, null);
			}
			
			@Override
			public void reconnectionFailed(Exception e) {
				//debug(TAG, "reconnection failed", e);
				//forced_disconnect(new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
				
				setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
				mExecutor.execute(new Runnable() {
					@Override
					public void run() {
						reconnect();
					}
				});
			}
			
			@Override
			public void reconnectingIn(int seconds) {
				/*
				 * Reconnect happens:
				 * - due to network error
				 * - but not if connectionClosed is fired
				 */
				debug(TAG, "reconnecting in " + seconds);
				setState(LOGGING_IN, null);
			}
			
			@Override
			public void connectionClosedOnError(Exception e) {
				/*
				 * This fires when:
				 * - Packet reader or writer detect an error
				 * - Stream compression failed
				 * - TLS fails but is required
				 */
				Log.e(TAG, "reconnect on error", e);
				if (e == null || e.getMessage() == null)
				{
					setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
					mExecutor.execute(new Runnable() {
						@Override
						public void run() {
							reconnect();
						}
					});
				}
				else if (e.getMessage().contains("conflict")) {
					disconnect();
					disconnected(new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, "logged in from another location"));
				}
				else {
					setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
					mExecutor.execute(new Runnable() {
						@Override
						public void run() {
							reconnect();
						}
					});
				}
			}
			
			@Override
			public void connectionClosed() {
				
				disconnect();
				
				/*
				 * This can be called in these cases:
				 * - Connection is shutting down
				 *   - because we are calling disconnect
				 *     - in do_logout
				 *     
				 * - NOT (fixed in smack)
				 *   - because server disconnected "normally"
				 *   - we were trying to log in (initConnection), but are failing
				 *   - due to network error
				 *   - in forced disconnect
				 *   - due to login failing
				 */
				debug(TAG, "connection closed");
			}
		});
        
       // android.os.Debug.waitForDebugger();
        // dangerous debug statement below, prints password!
        //debug(TAG, "mConnection.login("+userName+", "+password+", "+xmppResource+");");
        mConnection.login(userName, password, xmppResource);
        org.jivesoftware.smack.packet.Presence presence = 
        	new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.available);
        mConnection.sendPacket(presence);
        

	}
	
	private void initSSLContext (String server, ConnectionConfiguration config) throws Exception
	{

       
		ks = KeyStore.getInstance(TRUSTSTORE_TYPE);
         try {
             ks.load(new FileInputStream(TRUSTSTORE_PATH), TRUSTSTORE_PASS.toCharArray());
         }
         catch(Exception e) {
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
	     sTrustManager = new ServerTrustManager(aContext, server, config);
	     
	     sslContext.init(kms,
                 new javax.net.ssl.TrustManager[]{sTrustManager},
                 new java.security.SecureRandom());
        
	    config.setCustomSSLContext(sslContext);
	    
	}

	void disconnected(ImErrorInfo info) {
		Log.w(TAG, "disconnected");
		setState(DISCONNECTED, info);
	}
	
	void forced_disconnect(ImErrorInfo info) {
		// UNUSED
		Log.w(TAG, "forced disconnect");
		try {
			if (mConnection!= null) {
				XMPPConnection conn = mConnection;
				mConnection = null;
				conn.disconnect();
			}
		}
		catch (Exception e) {
			// Ignore
		}
		disconnected(info);
	}

	
	protected static int parsePresence(org.jivesoftware.smack.packet.Presence presence) {
		int type = Presence.AVAILABLE;
		Mode rmode = presence.getMode();
		Type rtype = presence.getType();
		
		if (rmode == Mode.away || rmode == Mode.xa)
			type = Presence.AWAY;
		else if (rmode == Mode.dnd)
			type = Presence.DO_NOT_DISTURB;
		else if (rtype == Type.unavailable)
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
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				do_logout();
			}
		});
	}
	
	private void do_logout() {
		Log.w(TAG, "logout");
		setState(LOGGING_OUT, null);
		disconnect();
		setState(DISCONNECTED, null);
	}

	private void disconnect() {
	 
		
		clearHeartbeat();
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
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				reconnect();
			}
		});
	}
	
	
	@Override
	public void suspend() {

		mConnection.shutdown();
	}

	private ChatSession findOrCreateSession(String address) {
		ChatSession session = mSessionManager.findSession(address);
		
		if (session == null) {
			Contact contact = findOrCreateContact(parseAddressName(address),address);
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
			org.jivesoftware.smack.packet.Message msg =
				new org.jivesoftware.smack.packet.Message(
						message.getTo().getFullName(),
						org.jivesoftware.smack.packet.Message.Type.chat
						);
			msg.setBody(message.getBody());
			mConnection.sendPacket(msg);
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

	public ChatSession findSession (String address)
	{
		return mSessionManager.findSession(address);
	}
	
	public ChatSession createChatSession (Contact contact)
	{
		return mSessionManager.createChatSession(contact);
	}

	private final class XmppContactList extends ContactListManager {
		
		//private Hashtable<String, org.jivesoftware.smack.packet.Presence> unprocdPresence = new Hashtable<String, org.jivesoftware.smack.packet.Presence>();
		
		@Override
		protected void setListNameAsync(final String name, final ContactList list) {
			mExecutor.execute(new Runnable() {
				@Override
				public void run() {
					do_setListName(name, list);
				}
			});
		}
		
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
			
			mExecutor.execute(new Runnable() {
				@Override
				public void run() {
					do_loadContactLists();
				}
			});
			
		}

		/**
		 *  Create new list of contacts from roster entries.
		 *
		 *  @param entryIter	iterator of roster entries to add to contact list
		 *  @param skipList		list of contacts which should be omitted; new contacts are added to this list automatically
		 *  @return				contacts from roster which were not present in skiplist.
		 */
		private Collection<Contact> fillContacts(Collection<RosterEntry> entryIter, Set<String> skipList) {
			

			Roster roster = mConnection.getRoster();
			
			Collection<Contact> contacts = new ArrayList<Contact>();
			for (RosterEntry entry : entryIter)
			{
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
				
				contact.setPresence(new Presence(parsePresence(presence), presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT));

				contacts.add(contact);
				
				
				// getVCard(xaddress.getFullName());  // commented out to fix slow contact loading

				
				
			}
			return contacts;
		}

		private void do_loadContactLists() {
			
			debug(TAG, "load contact lists");
			
			if (mConnection == null)
				return;
			
			Roster roster = mConnection.getRoster();
			
			//Set<String> seen = new HashSet<String>();
			//android.os.Debug.waitForDebugger();
			
			for (Iterator<RosterGroup> giter = roster.getGroups().iterator(); giter.hasNext();) {

				RosterGroup group = giter.next();

				debug(TAG, "loading group: " + group.getName() + " size:" + group.getEntryCount());
						

				Collection<Contact> contacts = fillContacts(group.getEntries(), null);
				
				ContactList cl =
						new ContactList(mUser.getAddress(), group.getName(), false, contacts, this);
				
				notifyContactListCreated(cl);
				
				notifyContactsPresenceUpdated(contacts.toArray(new Contact[contacts.size()]));
		
			//	for (Contact contact : contacts)
				//	notifyContactListUpdated(cl, ContactListListener.LIST_CONTACT_ADDED, contact);

				//notifyContactListLoaded(cl);
			}			
			
			if (roster.getUnfiledEntryCount() > 0) {
				
				String generalGroupName = "Buddies";

				Collection<Contact> contacts = fillContacts(roster.getUnfiledEntries(), null);

				ContactList cl =
						new ContactList(mUser.getAddress(), generalGroupName, true, contacts, this);
				
			
				notifyContactListCreated(cl);

				notifyContactsPresenceUpdated(contacts.toArray(new Contact[contacts.size()]));


				//for (Contact contact : contacts)
					//notifyContactListUpdated(cl, ContactListListener.LIST_CONTACT_ADDED, contact);
						

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
			
			private Stack<String> entriesToAdd = new Stack<String>();
			private Stack<String> entriesToDel = new Stack<String>();

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
		
		
		private void handlePresenceChanged (org.jivesoftware.smack.packet.Presence presence)
		{
	//		android.os.Debug.waitForDebugger();
			
			String name = parseAddressName(presence.getFrom());
			String address = parseAddressBase(presence.getFrom());
			
			XmppAddress xaddress = new XmppAddress(name, address);
			
			Contact contact = getContact(xaddress.getFullName());
			
			/*
			if (mConnection != null)
			{
				Roster roster = mConnection.getRoster();
				
				// Get it from the roster - it handles priorities, etc.
				
				if (roster != null)
					presence = roster.getPresence(address);
			}*/
			
			int type = parsePresence(presence);
			
			if (contact == null)
			{
				contact = new Contact(xaddress, name);
	
				debug(TAG, "got presence updated for NEW user: " + contact.getAddress().getFullName() + " presence:" + type);
				//store the latest presence notification for this user in this queue
				//unprocdPresence.put(user, presence);
				
				
			}
			else
			{
				debug(TAG, "Got present update for EXISTING user: " + contact.getAddress().getFullName()  + " presence:" + type);
		
				Presence p = new Presence(type, presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT);			
				contact.setPresence(p);
				
	
				Contact []contacts = new Contact[] { contact };
	
				notifyContactsPresenceUpdated(contacts);
			}
		}
		
		@Override
		protected ImConnection getConnection() {
			return XmppConnection.this;
		}

		@Override
		protected void doRemoveContactFromListAsync(Contact contact,
				ContactList list) {
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
				group.removeEntry(entry);
			} catch (XMPPException e) {
				Log.e(TAG, "remove entry failed", e);
				throw new RuntimeException(e);
			}
            org.jivesoftware.smack.packet.Presence response =
            	new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.unsubscribed);
            response.setTo(address);
            mConnection.sendPacket(response);
			notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_REMOVED, contact);
		}

		@Override
		protected void doDeleteContactListAsync(ContactList list) {
			// TODO delete contact list
			debug(TAG, "delete contact list " + list.getName());
		}

		@Override
		protected void doCreateContactListAsync(String name,
				Collection<Contact> contacts, boolean isDefault) {
			// TODO create contact list
			debug(TAG, "create contact list " + name + " default " + isDefault);
		}

		@Override
		protected void doBlockContactAsync(String address, boolean block) {
			// TODO block contact
			
		}

		@Override
		protected void doAddContactToListAsync(String address, ContactList list)
				throws ImException {
			debug(TAG, "add contact to " + list.getName());
			org.jivesoftware.smack.packet.Presence response =
				new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.subscribed);
			response.setTo(address);
			mConnection.sendPacket(response);

			Roster roster = mConnection.getRoster();
			String[] groups = new String[] { list.getName() };
			try {
				roster.createEntry(address, parseAddressName(address), groups);
			} catch (XMPPException e) {
				throw new RuntimeException(e);
			}
			
			Contact contact = makeContact(parseAddressName(address),address);
			notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_ADDED, contact);
		}

		@Override
		public void declineSubscriptionRequest(String contact) {
			debug(TAG, "decline subscription");
            org.jivesoftware.smack.packet.Presence response =
            	new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.unsubscribed);
            response.setTo(contact);
            mConnection.sendPacket(response);
            mContactListManager.getSubscriptionRequestListener().onSubscriptionDeclined(contact);
		}

		@Override
		public void approveSubscriptionRequest(String contact) {
			debug(TAG, "approve subscription");
            try {
            	// FIXME maybe need to check if already in another contact list
				mContactListManager.doAddContactToListAsync(contact, getDefaultContactList());
			} catch (ImException e) {
				Log.e(TAG, "failed to add " + contact + " to default list");
			}
            mContactListManager.getSubscriptionRequestListener().onSubscriptionApproved(contact);
		}

		@Override
		public Contact createTemporaryContact(String address) {
			debug(TAG, "create temporary " + address);
			return makeContact(parseAddressName(address),address);
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

	/*
	 * Alarm event fired
	 * @see info.guardianproject.otr.app.im.engine.ImConnection#sendHeartbeat()
	 */
	public void sendHeartbeat() {
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				doHeartbeat();
			}
		});
	}
	
	public void doHeartbeat() {
		if (mConnection == null && mRetryLogin) {
			debug(TAG, "reconnect with login");
			do_login();
		}
		if (mConnection == null)
			return;
		if (mNeedReconnect) {
			retry_reconnect();
		}
		else if (mConnection.isConnected() && getState() == LOGGED_IN) {
			debug(TAG, "ping");
			if (!sendPing()) {
				if (getState() == LOGGED_IN)
				{
					setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network timeout"));
					Log.w(TAG, "reconnect on ping failed");
					
					force_reconnect();
				}
			}
		}
	}
	
	private void clearHeartbeat() {
		debug(TAG, "clear heartbeat");
		mPingCollector = null;
	}
	
	private boolean sendPing() {
		// Check ping result from previous send
		if (mPingCollector != null) {
			
			IQ result = (IQ)mPingCollector.nextResult(SOTIMEOUT);
			mPingCollector.cancel();
			if (result == null || result.getError() != null)
			{
				clearHeartbeat();
				Log.e(TAG, "ping timeout");
				return false;
			}
		}

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
	    return true;
	}

	// watch out, this is a different XMPPConnection class than XmppConnection! ;)
	// org.jivesoftware.smack.XMPPConnection
	//    - vs -
	// info.guardianproject.otr.app.im.plugin.xmpp.XmppConnection
	static class MyXMPPConnection extends XMPPConnection {

		public MyXMPPConnection(ConnectionConfiguration config) {
			super(config);
			
			//this.getConfiguration().setSocketFactory(arg0)
			
		}
		
		public void shutdown() {
			
			try
			{
				shutdown(new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.unavailable));
			
			}
			catch (Exception e)
			{
				Log.e(TAG, "error on shutdown()",e);
			}
		}

	}

	@Override
	public void networkTypeChanged() {
		
		super.networkTypeChanged();
		//android.os.Debug.waitForDebugger();
		Log.w(TAG, "reconnect on network change");

		if (getState() == LOGGED_IN)
			setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network changed"));
		
		if (getState() != LOGGED_IN && getState() != LOGGING_IN)
			return;
		
		if (!mNeedReconnect)
			return;
		
		Log.w(TAG, "reconnect on network change");
		reconnect();
	
		
	}

	/*
	 * Force a disconnect and reconnect, unless we are already reconnecting.
	 */
	private void force_reconnect() {
		debug(TAG, "force_reconnect need=" + mNeedReconnect);
		if (mConnection == null)
			return;
		if (mNeedReconnect)
			return;
				
		try
		{
			if (mConnection != null && mConnection.isConnected())
			{
				mConnection.disconnect();
	//			mConnection.shutdown();
			}
		}
		catch (Exception e) {
			Log.w(TAG, "problem disconnecting on force_reconnect: " + e.getMessage());
		}
		
		mNeedReconnect = true;
		do_login();
	}

	/*
	 * Reconnect unless we are already in the process of doing so.
	 */
	/*
	private void maybe_reconnect() {
		// If we already know we don't have a good connection, the heartbeat
		// will take care of this
		debug(TAG, "maybe_reconnect mNeedReconnect=" + mNeedReconnect);
		// for some reason the mNeedReconnect logic is flipped here, donno why.
		// Added the mConnection test to stop NullPointerExceptions hans@eds.org
		// This is checking whether we are already in the process of reconnecting --devrandom
		if (mNeedReconnect || mConnection == null)
			return;
		mNeedReconnect = true;
		reconnect();
	}
*/
	
	/*
	 * Retry a reconnect on alarm event
	 */
	private void retry_reconnect() {
		// Retry reconnecting if we still need to
		debug(TAG, "retry_reconnect need=" + mNeedReconnect);
		if (mConnection != null && mNeedReconnect)
			reconnect();
	}

	/*
	 * Retry connecting
	 */
	private void reconnect() {
		
		clearHeartbeat();
		
		if (mConnection != null)
		{
			
			if (!mConnection.isConnected())
			{
				try {				
					mConnection.connect();
					
				} catch (Exception e) {
					mNeedReconnect = true;
	
					Log.w(TAG, "reconnection on network change failed: " + e.getMessage());
					setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
					return;
				}
			}
			
			mNeedReconnect = false;
			debug(TAG, "reconnected");
			setState(LOGGED_IN, null);
			
		}
		else
		{
			mNeedReconnect = true;

			debug(TAG, "reconnection on network change failed");
			
			setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "reconnection on network change failed"));
		}
	}
	
	public void debug (String tag, String msg)
	{
	//	Log.d(tag, msg);
	}
}
