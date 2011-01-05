package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.otr.OtrChatManager;
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
import info.guardianproject.otr.app.im.engine.LoginInfo;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.Presence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jivesoftware.smack.ConnectionConfiguration;
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

import android.os.Parcel;
import android.util.Log;

public class XmppConnection extends ImConnection {

	private final static String TAG = "Xmpp";
	private XmppContactList mContactListManager;
	private Contact mUser;
	
	private MyXMPPConnection mConnection;
	private XmppChatSessionManager mSessionManager;
	private ConnectionConfiguration mConfig;
	private XmppChatPacketListener mChatListener;
	
	private OtrChatManager mOtrMgr;

	
	private boolean mNeedReconnect;
	private LoginInfo mLoginInfo;
	private boolean mRetryLogin;
	private Executor mExecutor;

	private XmppConnection xmppConnection;
	
	private ProxyInfo mProxyInfo = null;
	
	public XmppConnection() {
		Log.w(TAG, "created");
		//ReconnectionManager.activate();
		SmackConfiguration.setKeepAliveInterval(-1);
		mExecutor = Executors.newSingleThreadExecutor();
		xmppConnection = this;
		
	}
	
	public void sendMessage(org.jivesoftware.smack.packet.Message msg) {
	//	android.os.Debug.waitForDebugger();

		if (mOtrMgr != null && mOtrMgr.isEncryptedSession(msg.getFrom(),msg.getTo()))
		{
			msg.setBody(mOtrMgr.sendMessage(msg.getFrom(),msg.getTo(), msg.getBody()));
		}
		
		
		mConnection.sendPacket(msg);
		
	}

	
	@Override
	protected void doUpdateUserPresenceAsync(Presence presence) {
		
		//android.os.Debug.waitForDebugger();
		
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
	public ChatSessionManager getChatSessionManager() {
		mSessionManager = new XmppChatSessionManager();
		return mSessionManager;
	}

	@Override
	public ContactListManager getContactListManager() {
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
	public void loginAsync(final LoginInfo loginInfo, boolean retry) {
		mLoginInfo = loginInfo;
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
		Log.i(TAG, "logging in " + mLoginInfo.getUserName());
		mNeedReconnect = true;
		setState(LOGGING_IN, null);
		mUserPresence = new Presence(Presence.AVAILABLE, "Online", null, null, Presence.CLIENT_TYPE_DEFAULT);
		String username = mLoginInfo.getUserName();
		String []comps = username.split("@");
		if (comps.length != 2) {
			disconnected(new ImErrorInfo(ImErrorInfo.INVALID_USERNAME, "username should be user@host"));
			mRetryLogin = false;
			mNeedReconnect = false;
			return;
		}
		try {
			
			initConnection(comps[1], comps[0], mLoginInfo.getPassword(), "Android");
		} catch (XMPPException e) {
			Log.e(TAG, "login failed", e);
			mConnection = null;
			ImErrorInfo info = new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, e.getMessage());
			if (e.getMessage().contains("not-authorized")) {
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
		mUser = new Contact(new XmppAddress(comps[0], username), username);
		setState(LOGGED_IN, null);
		Log.i(TAG, "logged in");
	}

	public void setProxy (String type, String host, int port)
	{
	//	android.os.Debug.waitForDebugger();

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
	
	private void initConnection(String serverHost, String login, String password, String resource) throws XMPPException {
    	
		//Android.os.Debug.waitForDebugger();

    	if (mProxyInfo == null)
    		 mProxyInfo = ProxyInfo.forNoProxy();
    	
    	
    	if (serverHost.equals("gmail.com") || serverHost.equals("talk.google.com") || serverHost.equals("googlemail.com"))
    	{
    		mConfig = new ConnectionConfiguration("talk.google.com",5222,"gmail.com", mProxyInfo);
    		 // You have to put this code before you login
    	     SASLAuthentication.supportSASLMechanism("PLAIN", 0);
    	     mConfig.setSecurityMode(SecurityMode.required);
    	     login = login + "@gmail.com";
    	}
    	else
    	{
    		
    		//SASLAuthentication.supportSASLMechanism("DIGEST-MD5",0);
    	//	SASLAuthentication.supportSASLMechanism("PLAIN", 0);
    		  
    	    mConfig = new ConnectionConfiguration(serverHost, mProxyInfo);

    		//mConfig = new ConnectionConfiguration(serverHost, 5223, serverHost, mProxyInfo);
    		
    		mConfig.setSecurityMode(SecurityMode.required);
  		  
//    		mConfig.setSASLAuthenticationEnabled(false);
    		//mConfig.setCompressionEnabled(false);
    		
    //		mConfig.setSelfSignedCertificateEnabled(true);
   // 		mConfig.setVerifyChainEnabled(false);
   // 		mConfig.setVerifyRootCAEnabled(false);
    		
    	}

		mConfig.setReconnectionAllowed(true);
		
		mConnection = new MyXMPPConnection(mConfig);
		mChatListener = new XmppChatPacketListener(this);
		
		
        mConnection.connect();
        
        Log.i(TAG,"is secure connection? " + mConnection.isSecureConnection());
        Log.i(TAG,"is using TLS? " + mConnection.isUsingTLS());

        
        mConnection.addPacketListener(mChatListener, new MessageTypeFilter(org.jivesoftware.smack.packet.Message.Type.chat));
        
        mConnection.addPacketListener(new PacketListener() {
			
			@Override
			public void processPacket(Packet packet) {
				
				//android.os.Debug.waitForDebugger();

				org.jivesoftware.smack.packet.Presence presence = (org.jivesoftware.smack.packet.Presence)packet;
				if (presence.getType() == Type.subscribe) {
					String address = parseAddressBase(presence.getFrom());
					Log.i(TAG, "sub request from " + address);
					Contact contact = findOrCreateContact(address);
					mContactListManager.getSubscriptionRequestListener().onSubScriptionRequest(contact);
				}
				else if (presence.getType() == Type.available)
				{
					String address = parseAddressBase(presence.getFrom());
					Log.i(TAG, "got 'available' from " + address);
					
					
				}
				else if (presence.getType() == Type.unavailable)
				{
					String address = parseAddressBase(presence.getFrom());
					Log.i(TAG, "got 'unavailable' from " + address);
				}
			
			}
		}, new PacketTypeFilter(org.jivesoftware.smack.packet.Presence.class));
        
        mConnection.addConnectionListener(new ConnectionListener() {
			@Override
			public void reconnectionSuccessful() {
				Log.i(TAG, "reconnection success");
				setState(LOGGED_IN, null);
			}
			
			@Override
			public void reconnectionFailed(Exception e) {
				Log.i(TAG, "reconnection failed", e);
				//forced_disconnect(new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
			}
			
			@Override
			public void reconnectingIn(int seconds) {
				/*
				 * Reconnect happens:
				 * - due to network error
				 * - but not if connectionClosed is fired
				 */
				Log.i(TAG, "reconnecting in " + seconds);
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
				Log.i(TAG, "reconnect on error", e);
				if (e.getMessage().contains("conflict")) {
					disconnect();
					disconnected(new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, "logged in from another location"));
				}
				else {
					setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
					mExecutor.execute(new Runnable() {
						@Override
						public void run() {
							maybe_reconnect();
						}
					});
				}
			}
			
			@Override
			public void connectionClosed() {
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
				Log.i(TAG, "connection closed");
			}
		});
        
        mConnection.login(login, password, resource);
        
        org.jivesoftware.smack.packet.Presence presence = 
        	new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.available);
       
        mConnection.sendPacket(presence);
        
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

	protected static String parseAddressBase(String from) {
		return from.replaceFirst("/.*", "");
	}

	protected static String parseAddressUser(String from) {
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
	public void reestablishSessionAsync(HashMap<String, String> sessionContext) {

	}

	@Override
	public void suspend() {

	}

	private ChatSession findOrCreateSession(String address) {
		ChatSession session = mSessionManager.findSession(address);
		if (session == null) {
			Contact contact = findOrCreateContact(address);
			session = mSessionManager.createChatSession(contact);
		}
		return session;
	}

	Contact findOrCreateContact(String address) {
		Contact contact = mContactListManager.getContact(address);
		if (contact == null) {
			contact = makeContact(address);
		}
		return contact;
	}

	private static String makeNameFromAddress(String address) {
		return address;
	}

	private static Contact makeContact(String address) {
		Contact contact = new Contact(new XmppAddress(address), address);
		return contact;
	}

	private final class XmppChatSessionManager extends ChatSessionManager {
		@Override
		protected void sendMessageAsync(ChatSession session, Message message) {

			org.jivesoftware.smack.packet.Message msg =
				new org.jivesoftware.smack.packet.Message(
						message.getTo().getFullName(),
						org.jivesoftware.smack.packet.Message.Type.chat
						);
			
			if (mOtrMgr != null && mOtrMgr.isEncryptedSession(msg.getFrom(),msg.getTo()))
			{
				msg.setBody(mOtrMgr.sendMessage(msg.getFrom(),msg.getTo(), message.getBody()));
			}
			else
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
		

		/**
	     * Start encryption for this chat
	     */
	    public boolean encryptChat(String address)
	    {
	    	Log.i(TAG, "Got encryptChat() request for: " + address);
	    	
	    	if (address == null || address.length() == 0 || address.equals(mUser.getAddress().getFullName()))
	    		return false;
	    	
	    	if (mOtrMgr == null)
	    	{
				mOtrMgr = new OtrChatManager(xmppConnection);	
				mChatListener.setOtrMgr(mOtrMgr);

	    	}
	    	
	    	
	    	
			if (!mOtrMgr.isEncryptedSession(mUser.getAddress().getFullName(), address))
				mOtrMgr.startSession(mUser.getAddress().getFullName(), address);
			
			
	    	
	    	return true;
	    }
	    
	     /**
	     * Stop encryption for this chat
	     */
	    public boolean unencryptChat(String remoteAddress)
	    {
	    	if (mOtrMgr != null)
	    	{
	    		mOtrMgr.endSession(mUser.getAddress().getFullName(), remoteAddress);
	    	}
	    	
	    	return true;
	    }
	    
	      /**
	     * Start remote identity verification
	     */
	    public void verifyRemoteIdentity(String address)
	    {
	    	
	    }
	    
	    /**
	     * Get remote key fingerprint
	     */
	    public String getRemoteKeyFingerprint(String remoteAddress)
	    {
	    	return mOtrMgr.getRemoteKeyFingerprint(mUser.getAddress().getFullName(), remoteAddress);
	    }
	    
	    
	    /**
	     * Get remote key fingerprint
	     */
	    public String getLocalKeyFingerprint(String remoteAddress)
	    {
	    	return mOtrMgr.getLocalKeyFingerprint(mUser.getAddress().getFullName(), remoteAddress);
	    }
	    
	    /**
	     * Start remote identity verification
	     */
	    public boolean isEncryptedSession(String remoteAddress)
	    {
		//	android.os.Debug.waitForDebugger();

	    	if (mOtrMgr!=null)
	    		return (mOtrMgr.isEncryptedSession(mUser.getAddress().getFullName(), remoteAddress));
	    	else
	    		return false;
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
		
		private Hashtable<String, org.jivesoftware.smack.packet.Presence> unprocdPresence = new Hashtable<String, org.jivesoftware.smack.packet.Presence>();
		
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
			Log.d(TAG, "set list name");
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
		private Collection<Contact> fillContacts(Iterator<RosterEntry> entryIter, Set<String> skipList) {
			Collection<Contact> contacts = new ArrayList<Contact>();
			for (; entryIter.hasNext();) {
				RosterEntry entry = entryIter.next();
				String address = parseAddressBase(entry.getUser());

				/* Skip entries present in the skip list */
				if (skipList != null && !skipList.add(address))
					continue;

				String name = entry.getName();
				if (name == null)
					name = address;
				XmppAddress xaddress = new XmppAddress(name, address);
				Contact contact = new Contact(xaddress, name);
				contacts.add(contact);
				
				
			}
			return contacts;
		}

		private void do_loadContactLists() {
			
			//android.os.Debug.waitForDebugger();
			Log.d(TAG, "load contact lists");
			Roster roster = mConnection.getRoster();
			roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
			listenToRoster(roster);
			
			Set<String> seen = new HashSet<String>();
			
			
			
			for (Iterator<RosterGroup> giter = roster.getGroups().iterator(); giter.hasNext();) {
				
				RosterGroup group = giter.next();
				Collection<Contact> contacts = fillContacts(group.getEntries().iterator(), seen);
				ContactList cl = new ContactList(mUser.getAddress(), group.getName(), true, contacts, this);
				mContactLists.add(cl);
				if (mDefaultContactList == null)
					mDefaultContactList = cl;
				notifyContactListLoaded(cl);
				notifyContactsPresenceUpdated(contacts.toArray(new Contact[0]));
				
				processQueuedPresenceNotifications (contacts);
				
				
			}
			
			if (roster.getUnfiledEntryCount() > 0) {
				
				String generalGroupName = "General";
				
				RosterGroup group = roster.getGroup(generalGroupName);
				
				if (group == null)
					group = roster.createGroup(generalGroupName);
				
				Collection<Contact> contacts = fillContacts(roster.getUnfiledEntries().iterator(), null);
				ContactList cl = new ContactList(mUser.getAddress(), generalGroupName , true, contacts, this);
				
				//n8fr8: adding this contact list to the master list manager seems like the righ thing to do
				mContactLists.add(cl);
				mDefaultContactList = cl;
				notifyContactListLoaded(cl);
				
				processQueuedPresenceNotifications(contacts);
				
				//n8fr8: also needs to ping the presence status change, too!
				notifyContactsPresenceUpdated(contacts.toArray(new Contact[0]));

			}
			
			notifyContactListsLoaded();
			
			
		}

		/*
		 * iterators through a list of contacts to see if there were any Presence
		 * notifications sent before the contact was loaded
		 */
		private void processQueuedPresenceNotifications (Collection<Contact> contacts)
		{
			//now iterate through the list of queued up unprocessed presence changes
			for (Contact contact : contacts)
			{
				
				String address = parseAddressBase(contact.getAddress().getFullName());
				
				org.jivesoftware.smack.packet.Presence presence = unprocdPresence.get(address);
				
				if (presence != null)
				{
					unprocdPresence.remove(address);
					
					int type = Presence.AVAILABLE;
					Mode rmode = presence.getMode();
					Type rtype = presence.getType();
					
					if (rmode == Mode.away || rmode == Mode.xa)
						type = Presence.AWAY;
					if (rmode == Mode.dnd)
						type = Presence.DO_NOT_DISTURB;
					if (rtype == Type.unavailable)
						type = Presence.OFFLINE;
					
					contact.setPresence(new Presence(type, presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT));
					
				}
			}
		}
		
		private void listenToRoster(final Roster roster) {
			
			roster.addRosterListener(new RosterListener() {
				
				@Override
				public void presenceChanged(org.jivesoftware.smack.packet.Presence presence) {
					
					//android.os.Debug.waitForDebugger(); //this is for debugging a service

					String user = parseAddressBase(presence.getFrom());
					
					Contact contact = mContactListManager.getContact(user);
					
					if (contact == null)
					{
						Log.d(TAG, "Got present update for NULL user: " + user);
						//store the latest presence notification for this user in this queue
						unprocdPresence.put(user, presence);
						return;
					}
					
					Contact []contacts = new Contact[] { contact };
					// Get it from the roster - it handles priorities, etc.
					presence = roster.getPresence(user);
					
					int type = Presence.AVAILABLE;
					Mode rmode = presence.getMode();
					Type rtype = presence.getType();
					
					if (rmode == Mode.away || rmode == Mode.xa)
						type = Presence.AWAY;
					if (rmode == Mode.dnd)
						type = Presence.DO_NOT_DISTURB;
					if (rtype == Type.unavailable)
						type = Presence.OFFLINE;
					
					contact.setPresence(new Presence(type, presence.getStatus(), null, null, Presence.CLIENT_TYPE_DEFAULT));
					notifyContactsPresenceUpdated(contacts);
				}
				
				@Override
				public void entriesUpdated(Collection<String> addresses) {
					// TODO update contact list entries from remote
					Log.d(TAG, "roster entries updated");
					
					

				}
				
				@Override
				public void entriesDeleted(Collection<String> addresses) {
					// TODO delete contacts from remote
					Log.d(TAG, "roster entries deleted");
				}
				
				@Override
				public void entriesAdded(Collection<String> addresses) {
					// TODO add contacts from remote
					Log.d(TAG, "roster entries added");
				}
			});
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
			mConnection.sendPacket(response);

			Roster roster = mConnection.getRoster();
			String[] groups = new String[] { list.getName() };
			try {
				roster.createEntry(address, makeNameFromAddress(address), groups);
			} catch (XMPPException e) {
				throw new RuntimeException(e);
			}
			
			Contact contact = makeContact(address);
			notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_ADDED, contact);
		}

		@Override
		public void declineSubscriptionRequest(String contact) {
			Log.d(TAG, "decline subscription");
            org.jivesoftware.smack.packet.Presence response =
            	new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.unsubscribed);
            response.setTo(contact);
            mConnection.sendPacket(response);
            mContactListManager.getSubscriptionRequestListener().onSubscriptionDeclined(contact);
		}

		@Override
		public void approveSubscriptionRequest(String contact) {
			Log.d(TAG, "approve subscription");
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
			Log.d(TAG, "create temporary " + address);
			return makeContact(address);
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
			this.name = makeNameFromAddress(address);
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

	private PacketCollector mPingCollector;

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
		if (mConnection == null && mRetryLogin && mLoginInfo != null) {
			Log.i(TAG, "reconnect with login");
			do_login();
		}
		if (mConnection == null)
			return;
		if (mNeedReconnect) {
			retry_reconnect();
		}
		else if (mConnection.isConnected() && getState() == LOGGED_IN) {
			Log.d(TAG, "ping");
			if (!sendPing()) {
				if (getState() == LOGGED_IN)
					setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network timeout"));
				Log.w(TAG, "reconnect on ping failed");
				force_reconnect();
			}
		}
	}
	
	private void clearHeartbeat() {
		Log.d(TAG, "clear heartbeat");
		mPingCollector = null;
	}
	
	private boolean sendPing() {
		// Check ping result from previous send
		if (mPingCollector != null) {
			IQ result = (IQ)mPingCollector.nextResult(0);
			mPingCollector.cancel();
			if (result == null)
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

	static class MyXMPPConnection extends XMPPConnection {

		public MyXMPPConnection(ConnectionConfiguration config) {
			super(config);
			
			//this.getConfiguration().setSocketFactory(arg0)
			
		}

	}

	@Override
	public void networkTypeChanged() {
		super.networkTypeChanged();
		if (getState() == LOGGED_IN)
			setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, "network changed"));
		if (getState() != LOGGED_IN && getState() != LOGGING_IN)
			return;
		if (mNeedReconnect)
			return;
		Log.w(TAG, "reconnect on network change");
		force_reconnect();
	}

	/*
	 * Force a disconnect and reconnect, unless we are already reconnecting.
	 */
	private void force_reconnect() {
		Log.d(TAG, "force_reconnect need=" + mNeedReconnect);
		if (mConnection == null)
			return;
		if (mNeedReconnect)
			return;
		mConnection.disconnect();
		mNeedReconnect = true;
		reconnect();
	}
	
	/*
	 * Reconnect unless we are already in the process of doing so.
	 */
	private void maybe_reconnect() {
		// If we already know we don't have a good connection, the heartbeat will take care of this
		Log.d(TAG, "maybe_reconnect need=" + mNeedReconnect);
		if (mNeedReconnect)
			return;
		mNeedReconnect = true;
		reconnect();
	}
	
	/*
	 * Retry a reconnect on alarm event
	 */
	private void retry_reconnect() {
		// Retry reconnecting if we still need to
		Log.d(TAG, "retry_reconnect need=" + mNeedReconnect);
		if (mConnection != null && mNeedReconnect)
			reconnect();
	}

	/*
	 * Retry connecting
	 */
	private void reconnect() {
		Log.i(TAG, "reconnect");
		clearHeartbeat();
		try {
			mConnection.connect();
			mNeedReconnect = false;
			Log.i(TAG, "reconnected");
			setState(LOGGED_IN, null);
		} catch (XMPPException e) {
			Log.e(TAG, "reconnection on network change failed", e);
			setState(LOGGING_IN, new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
		}
	}
	

	/**
	 * @return the mOtrMgr
	 */
	public OtrChatManager getOtrManager() {
		return mOtrMgr;
	}

	/**
	 * @param mOtrMgr the mOtrMgr to set
	 */
	public void setOtrManager(OtrChatManager mOtrMgr) {
		this.mOtrMgr = mOtrMgr;
	}

}

