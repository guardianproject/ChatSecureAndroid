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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.jivesoftware.smack.ConnectionConfiguration;
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
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
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
	// Synchronized by executor thread
	private MyXMPPConnection mConnection;

	private XmppChatSessionManager mSessionManager;
	private ConnectionConfiguration mConfig;
	
	// True if we are in the process of reconnecting.  Reconnection is retried once per heartbeat.
	// Synchronized by executor thread.
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
	private final static String TRUSTSTORE_PATH = "/system/etc/security/cacerts.bks";
	
	private final static int SOTIMEOUT = 15000;
	
	private PacketCollector mPingCollector;

	public XmppConnection(Context context) {
		super(context);
		
		Log.w(TAG, "created");
		
		// The reconnection manager is not reliable - we use our own Android based heartbeat
		//ReconnectionManager.activate();
		//SmackConfiguration.setKeepAliveInterval(-1);
		
		SmackConfiguration.setPacketReplyTimeout(SOTIMEOUT);
		
		// Create a single threaded executor.  This will serialize actions on the underlying connection.
		
		//mExecutor = Executors.newSingleThreadExecutor();
		
		mExecutor = Executors.newCachedThreadPool();
	}
	
	private boolean execute(Runnable runnable) {
		try {
			mExecutor.execute(runnable);
		} catch (RejectedExecutionException ex) {
			return false;
		}
		return true;
	}
	
	public void sendPacket(final org.jivesoftware.smack.packet.Packet packet) {
		execute(new Runnable() {
			@Override
			public void run() {
				if (mConnection == null) {
					Log.w(TAG, "dropped packet to " + packet.getTo() + " because we are not connected");
					return;
				}
				if (!mConnection.isConnected()) {
					Log.w(TAG, "dropped packet to " + packet.getTo() + " because socket is disconnected");
				}
				mConnection.sendPacket(packet);		
			}
		});
		//if (mConnection != null)
		//	mConnection.sendPacket(msg);		
	}
	
	 public VCard getVCard(String myJID) {
	        

		// android.os.Debug.waitForDebugger();
		 
	        VCard vCard = new VCard();
	        
	        try {       
	        	// FIXME synchronize this to executor thread
	            vCard.load(mConnection, myJID);
	            
	            // If VCard is loaded, then save the avatar to the personal folder.
	            byte[] bytes = vCard.getAvatar();
	            
	            if (bytes != null)
	            {
	            	try {
	            		String filename = vCard.getAvatarHash() + ".jpg";
	            		File sdCard = Environment.getExternalStorageDirectory();
	            		File file = new File(sdCard, filename);
	            		OutputStream output = new FileOutputStream(file);
	            		output.write(bytes);
	            		output.close();
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
		
        sendPacket(packet);
		mUserPresence = presence;
        notifyUserPresenceUpdated();
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
			
			Log.e(TAG, "login failed", e);
			mConnection = null;
			ImErrorInfo info = new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, e.getMessage());
			
			if (e.getMessage().contains("not-authorized")) {
				
				Log.w(TAG, "not authorized - will not retry");
				info = new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "invalid user/password");
				disconnected(info);
				mRetryLogin = false;
			}/*
			else if (mRetryLogin) {
				Log.w(TAG, "will retry");
				setState(LOGGING_IN, info);
			}*/
			else {
				Log.w(TAG, "will not retry");
				mConnection = null;
				mRetryLogin = false;
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
		Log.i(TAG, "logged in");
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
	
	// Runs in executor thread
	private void initConnection(String userName, final String password, 
			Imps.ProviderSettings.QueryMap providerSettings) throws XMPPException {

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
		debug(TAG, "plain auth? " + allowPlainAuth);
    	
    	if (mProxyInfo == null)
    		 mProxyInfo = ProxyInfo.forNoProxy();

    	// TODO try getting a connection without DNS SRV first, and if that doesn't work and the prefs allow it, use DNS SRV
    	if (doDnsSrv) {
    		debug(TAG, "(DNS SRV) ConnectionConfiguration("+domain+", mProxyInfo);");
    		mConfig = new ConnectionConfiguration(domain, mProxyInfo);
    	} else if (server == null) { // no server specified in prefs, use the domain
    		debug(TAG, "(use domain) ConnectionConfiguration("+domain+", "+serverPort+", "+domain+", mProxyInfo);");
    		mConfig = new ConnectionConfiguration(domain, serverPort, domain, mProxyInfo);
    	} else {	
    		debug(TAG, "(use server) ConnectionConfiguration("+server+", "+serverPort+", "+domain+", mProxyInfo);");
    		mConfig = new ConnectionConfiguration(server, serverPort, domain, mProxyInfo);
    	}

    	//mConfig.setDebuggerEnabled(true);
    	
    	if (requireTls) {
    		mConfig.setSecurityMode(SecurityMode.required);
    		
    		if(allowPlainAuth)
    		{
    			mConfig.setSASLAuthenticationEnabled(false);    	    
    			SASLAuthentication.supportSASLMechanism("PLAIN", 0);
    		}
    		else
    		{
    			mConfig.setSASLAuthenticationEnabled(true);  
    			SASLAuthentication.unsupportSASLMechanism("PLAIN");
    		}
    		
    		SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 1);
    		
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
    		{
    			mConfig.setSASLAuthenticationEnabled(false);  
    			SASLAuthentication.supportSASLMechanism("PLAIN", 1);
    		}
    		else
    		{
    			mConfig.setSASLAuthenticationEnabled(true);
    			SASLAuthentication.unsupportSASLMechanism("PLAIN");
    		}
    	}
    	// Android has no support for Kerberos or GSSAPI, so disable completely
    	SASLAuthentication.unregisterSASLMechanism("KERBEROS_V4");
    	SASLAuthentication.unregisterSASLMechanism("GSSAPI");

    	mConfig.setVerifyChainEnabled(tlsCertVerify);
    	mConfig.setVerifyRootCAEnabled(tlsCertVerify);
    	mConfig.setExpiredCertificatesCheckEnabled(tlsCertVerify);
    	mConfig.setNotMatchingDomainCheckEnabled(doVerifyDomain);
    	
    	 // Android doesn't support the default "jks" Java Key Store, it uses "bks" instead
	     // this should probably be set to our own, if we are going to save self-signed certs
		mConfig.setTruststoreType(TRUSTSTORE_TYPE);
		mConfig.setTruststorePath(TRUSTSTORE_PATH);

		
		if (allowSelfSignedCerts) {
			Log.i(TAG, "allowing self-signed certs");
			mConfig.setSelfSignedCertificateEnabled(true);
		}
    	
		//reconnect please
		mConfig.setReconnectionAllowed(false);		
		mConfig.setSendPresence(true);
		mConfig.setRosterLoadedAtLogin(true);
		
		//Log.i(TAG, "ConnnectionConfiguration.getHost: " + mConfig.getHost() + " getPort: " + mConfig.getPort() + " getServiceName: " + mConfig.getServiceName());
		
		mConnection = new MyXMPPConnection(mConfig);
		
		Roster roster = mConnection.getRoster();
		roster.setSubscriptionMode(Roster.SubscriptionMode.manual);			
		getContactListManager().listenToRoster(roster);
		
        mConnection.connect();
        
        //Log.i(TAG,"is secure connection? " + mConnection.isSecureConnection());
        //Log.i(TAG,"is using TLS? " + mConnection.isUsingTLS());
        
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
					Log.i(TAG, "sub request from " + address);
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
        	/**
        	 * Called from smack when connect() is fully successful
        	 * 
        	 * This is called on the executor thread while we are in reconnect()
        	 */
			@Override
			public void reconnectionSuccessful() {
				mNeedReconnect = false;
				Log.i(TAG, "reconnected");
				setState(LOGGED_IN, null);
			}
			
			@Override
			public void reconnectionFailed(Exception e) {
				// We are not using the reconnection manager
				throw new UnsupportedOperationException();
				//Log.i(TAG, "reconnection failed", e);
				//forced_disconnect(new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
			/*	
				setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
				mExecutor.execute(new Runnable() {
					@Override
					public void run() {
						reconnect();
					}
				});
*/
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
				Log.i(TAG, "reconnect on error", e);
				if (e.getMessage().contains("conflict")) {
					execute(new Runnable() {
						@Override
						public void run() {
							disconnect();
							disconnected(new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER,
									"logged in from another location"));
						};
					});
				}
				else if (!mNeedReconnect) {
					execute(new Runnable() {
						@Override
						public void run() {
							if (getState() == LOGGED_IN)
								setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
							maybe_reconnect();
						//	reconnect();
						}
					});
				}
			}
			
			@Override
			public void connectionClosed() {
				
				Log.i(TAG, "connection closed");
				
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
        
       // android.os.Debug.waitForDebugger();
        // dangerous debug statement below, prints password!
        //Log.i(TAG, "mConnection.login("+userName+", "+password+", "+xmppResource+");");
        mConnection.login(userName, password, xmppResource);
        org.jivesoftware.smack.packet.Presence presence = 
        	new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.available);
        mConnection.sendPacket(presence);
        

	}

	void disconnected(ImErrorInfo info) {
		Log.w(TAG, "disconnected");
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
		execute(new Runnable() {
			@Override
			public void run() {
				do_logout();
			}
		});
	}
	
	// Runs in executor thread
	private void do_logout() {
		Log.w(TAG, "logout");
		setState(LOGGING_OUT, null);
		disconnect();
		setState(DISCONNECTED, null);
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
/*
	public void reestablishSessionAsync(Map<String, String> sessionContext) {
		execute(new Runnable() {
			@Override
			public void run() {
				if (getState() == SUSPENDED) {
					debug(TAG, "reestablish");
					setState(LOGGING_IN, null);
					maybe_reconnect();
				}
*/
	public void reestablishSessionAsync(HashMap<String, String> sessionContext) {
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				reconnect();
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
				//mConnection.shutdown();
			}
		});

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

		/**
		 *  Create new list of contacts from roster entries.
		 *  
		 *  Runs in executor thread
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

		// Runs in executor thread
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
				group.removeEntry(entry);
			} catch (XMPPException e) {
				Log.e(TAG, "remove entry failed", e);
				throw new RuntimeException(e);
			}
            org.jivesoftware.smack.packet.Presence response =
            	new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.unsubscribed);
            response.setTo(address);
            sendPacket(response);
			notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_REMOVED, contact);
		}

		@Override
		protected void doDeleteContactListAsync(ContactList list) {
			// TODO delete contact list
			Log.i(TAG, "delete contact list " + list.getName());
		}

		@Override
		protected void doCreateContactListAsync(String name,
				Collection<Contact> contacts, boolean isDefault) {
			// TODO create contact list
			Log.i(TAG, "create contact list " + name + " default " + isDefault);
		}

		@Override
		protected void doBlockContactAsync(String address, boolean block) {
			// TODO block contact
			
		}

		@Override
		protected void doAddContactToListAsync(String address, ContactList list)
				throws ImException {
			Log.i(TAG, "add contact to " + list.getName());
			org.jivesoftware.smack.packet.Presence response =
				new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.subscribed);
			response.setTo(address);

			sendPacket(response);

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
            sendPacket(response);
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
		execute(new Runnable() {
			@Override
			public void run() {
				debug(TAG, "heartbeat");
				doHeartbeat();
			}
		});
	}
	
	// Runs in executor thread
	public void doHeartbeat() {
		if (mConnection == null && mRetryLogin) {
			Log.i(TAG, "reconnect with login");
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
		}
		else if (mConnection.isConnected() && getState() == LOGGED_IN) {
			debug(TAG, "ping");
			if (!sendPing()) {
				Log.w(TAG, "reconnect on ping failed");
				setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network timeout"));
				force_reconnect();
			}
		}
	}
	
	private void clearPing() {
		debug(TAG, "clear ping");
		mPingCollector = null;
	}
	
	// Runs in executor thread
	private boolean sendPing() {
		// Check ping result from previous send
		if (mPingCollector != null) {
			IQ result = (IQ)mPingCollector.pollResult();
			
		//	IQ result = (IQ)mPingCollector.nextResult(SOTIMEOUT);
			mPingCollector.cancel();
			if (result == null || result.getError() != null)
			{
				clearPing();
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
		
		/*
		public void shutdown() {
			
			try
			{
				
				
				// Be forceful in shutting down since SSL can get stuck
				try { socket.shutdownInput(); } catch (Exception e) {}
				socket.close();
				shutdown(new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.unavailable));
			
			}
			catch (Exception e)
			{
				Log.e(TAG, "error on shutdown()",e);
			}
		}*/
		
	}

	@Override
	public void networkTypeChanged() {
		
		super.networkTypeChanged();
		//android.os.Debug.waitForDebugger();
		Log.w(TAG, "reconnect on network change");

		/*
		if (getState() == LOGGED_IN)
			setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network changed"));
		
		if (getState() != LOGGED_IN && getState() != LOGGING_IN)
			return;
		
		if (mNeedReconnect)
			return;
		Log.w(TAG, "reconnect on network change");
		force_reconnect();
		*/
		
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

		if (mConnection != null)
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
		
		reconnect();
		mNeedReconnect = true;
		do_login();
	}

	/*
	 * Reconnect unless we are already in the process of doing so.
	 * 
	 * Runs in executor thread.
	 */
	/*
	private void maybe_reconnect() {
		debug(TAG, "maybe_reconnect mNeedReconnect=" + mNeedReconnect + " state=" + getState() +
				" connection?=" + (mConnection != null));

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
*/
	
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
			Thread.sleep(2000);  // Wait for network to settle
		} catch (InterruptedException e) { /* ignore */ }
		
		if (mConnection != null)
		{
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
				mConnection.connect();
				// XMPP errors are swallowed during authentication, so kill the connection here if
				// we are not authenticated.
				if (!mConnection.isAuthenticated()) {
					Log.e(TAG, "authentication failed in connect() - shutdown and retry later");
					
					setState(DISCONNECTED, null);
				}
			} catch (XMPPException e) {
				Log.e(TAG, "reconnection attempt failed", e);
				setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
/*
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
			Log.i(TAG, "reconnect");
		
			
			Log.i(TAG, "reconnected");
			setState(LOGGED_IN, null);
			
		}
		else
		{
			mNeedReconnect = true;

			Log.d(TAG, "reconnection on network change failed");
			
			setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "reconnection on network change failed"));
		}
*/
	}
	
	@Override
	protected void setState(int state, ImErrorInfo error) {
		debug(TAG, "setState to " + state);
		super.setState(state, error);
	}

	public void debug (String tag, String msg)
	{
		Log.d(tag, msg);
	}
}

