package org.gitian.android.im.plugin.xmpp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.gitian.android.im.engine.Address;
import org.gitian.android.im.engine.ChatGroupManager;
import org.gitian.android.im.engine.ChatSession;
import org.gitian.android.im.engine.ChatSessionManager;
import org.gitian.android.im.engine.Contact;
import org.gitian.android.im.engine.ContactList;
import org.gitian.android.im.engine.ContactListListener;
import org.gitian.android.im.engine.ContactListManager;
import org.gitian.android.im.engine.ImConnection;
import org.gitian.android.im.engine.ImErrorInfo;
import org.gitian.android.im.engine.ImException;
import org.gitian.android.im.engine.LoginInfo;
import org.gitian.android.im.engine.Message;
import org.gitian.android.im.engine.Presence;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
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

import android.os.Parcel;
import android.util.Log;

public class XmppConnection extends ImConnection {

	protected static final String TAG = "XmppConnection";
	private static final long PING_TIMEOUT = 5000;
	private XmppContactList mContactListManager;
	private Contact mUser;
	private MyXMPPConnection mConnection;
	private XmppChatSessionManager mSessionManager;

	public XmppConnection() {
		Log.d(TAG, "created");
		ReconnectionManager.activate();
		SmackConfiguration.setKeepAliveInterval(-1);
	}
	
	// TODO !tests
	// TODO !battery tests
	// TODO !OTR
	// TODO !beta
	// FIXME NPEs in contact handling
	// FIXME leaking binder threads
	// FIXME IllegalStateException in ReconnectionManager
	
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
        else if (presence.getStatus() == Presence.DO_NOT_DISTURB) {
        	priority = 1;
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
	public void loginAsync(LoginInfo loginInfo) {
		Log.i(TAG, "loggin in " + loginInfo.getUserName());
		setState(LOGGING_IN, null);
		mUserPresence = new Presence(Presence.AVAILABLE, "available", null, null, Presence.CLIENT_TYPE_DEFAULT);
		String username = loginInfo.getUserName();
		String []comps = username.split("@");
		if (comps.length != 2)
			throw new RuntimeException("username should be user@host");
		try {
			initConnection(comps[1], comps[0], loginInfo.getPassword(), "Android");
		} catch (XMPPException e) {
			forced_disconnect(new ImErrorInfo(ImErrorInfo.CANT_CONNECT_TO_SERVER, e.getMessage()));
			return;
		}
		mUser = new Contact(new XmppAddress(comps[0], username), username);
		setState(LOGGED_IN, null);
		Log.i(TAG, "logged in");
	}

	private void initConnection(String serverHost, String login, String password, String resource) throws XMPPException {
    	ConnectionConfiguration config = new ConnectionConfiguration(serverHost);
		mConnection = new MyXMPPConnection(config);
		startPingTask();
        mConnection.connect();
        mConnection.addPacketListener(new PacketListener() {
			
			@Override
			public void processPacket(Packet packet) {
				org.jivesoftware.smack.packet.Message message = (org.jivesoftware.smack.packet.Message) packet;
				Message rec = new Message(message.getBody());
				String address = parseAddressBase(message.getFrom());
				ChatSession session = findOrCreateSession(address);
				rec.setFrom(session.getParticipant().getAddress());
				rec.setDateTime(new Date());
				session.onReceiveMessage(rec);
			}
		}, new MessageTypeFilter(org.jivesoftware.smack.packet.Message.Type.chat));
        mConnection.addPacketListener(new PacketListener() {
			
			@Override
			public void processPacket(Packet packet) {
				org.jivesoftware.smack.packet.Presence presence = (org.jivesoftware.smack.packet.Presence)packet;
				if (presence.getType() == Type.subscribe) {
					String address = parseAddressBase(presence.getFrom());
					Log.i(TAG, "sub request from " + address);
					Contact contact = findOrCreateContact(address);
					mContactListManager.getSubscriptionRequestListener().onSubScriptionRequest(contact);
				}
			}
		}, new PacketTypeFilter(org.jivesoftware.smack.packet.Presence.class));
        mConnection.addConnectionListener(new ConnectionListener() {
			@Override
			public void reconnectionSuccessful() {
				Log.i(TAG, "reconnection success");
			}
			
			@Override
			public void reconnectionFailed(Exception e) {
				// TODO reconnection failed
				Log.i(TAG, "reconnection failed");
				forced_disconnect(new ImErrorInfo(ImErrorInfo.NETWORK_ERROR, e.getMessage()));
			}
			
			@Override
			public void reconnectingIn(int seconds) {
				// TODO reconnecting
				Log.i(TAG, "reconnecting in " + seconds);
				if (seconds == 0)
					startPingTask();
			}
			
			@Override
			public void connectionClosedOnError(Exception e) {
				Log.i(TAG, "closed on error", e);
				pingThread = null;
			}
			
			@Override
			public void connectionClosed() {
				Log.i(TAG, "connection closed");
				forced_disconnect(null);
			}
		});
        mConnection.login(login, password, resource);
        org.jivesoftware.smack.packet.Presence presence = 
        	new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.available);
        mConnection.sendPacket(presence);
	}
	
	void forced_disconnect(ImErrorInfo info) {
		setState(DISCONNECTED, info);
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
		Log.i(TAG, "connection cleaned up");
	}

	protected static String parseAddressBase(String from) {
		return from.replaceFirst("/.*", "");
	}

	protected static String parseAddressUser(String from) {
		return from.replaceFirst("@.*", "");
	}

	@Override
	public void logoutAsync() {
		Log.i(TAG, "logout");
		setState(LOGGING_OUT, null);
		XMPPConnection conn = mConnection;
		mConnection = null;
		conn.disconnect();
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

	private Contact findOrCreateContact(String address) {
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

	private final class XmppContactList extends ContactListManager {
		@Override
		protected void setListNameAsync(String name, ContactList list) {
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
			Log.d(TAG, "load contact lists");
			Roster roster = mConnection.getRoster();
			roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
			listenToRoster(roster);
			boolean haveGroup = false;
			for (Iterator<RosterGroup> giter = roster.getGroups().iterator(); giter.hasNext();) {
				haveGroup = true;
				RosterGroup group = giter.next();
				Collection<Contact> contacts = new ArrayList<Contact>();
				Contact[] contacts_array = new Contact[group.getEntryCount()];
				for (Iterator<RosterEntry> iter = group.getEntries().iterator(); iter.hasNext();) {
					RosterEntry entry = iter.next();
					XmppAddress addr = new XmppAddress(entry.getName(), parseAddressBase(entry.getUser()));
					Contact contact = new Contact(addr, entry.getName());
					contacts.add(contact);
				}
				ContactList cl = new ContactList(mUser.getAddress(), group.getName(), true, contacts, this);
				mContactLists.add(cl);
				if (mDefaultContactList == null)
					mDefaultContactList = cl;
				notifyContactListLoaded(cl);
				notifyContactsPresenceUpdated(contacts.toArray(contacts_array));
			}
			if (!haveGroup) {
				roster.createGroup("Friends");
				ContactList cl = new ContactList(mUser.getAddress(), "Friends" , true, new ArrayList<Contact>(), this);
				mDefaultContactList = cl;
				notifyContactListLoaded(cl);
			}
			notifyContactListsLoaded();
		}

		private void listenToRoster(final Roster roster) {
			roster.addRosterListener(new RosterListener() {
				
				@Override
				public void presenceChanged(org.jivesoftware.smack.packet.Presence presence) {
					String user = parseAddressBase(presence.getFrom());
					Contact contact = mContactListManager.getContact(user);
					if (contact == null)
						return;
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

	Thread pingThread;

	void startPingTask() {
		// Schedule a ping task to run.
		PingTask task = new PingTask(PING_TIMEOUT);
		pingThread = new Thread(task);
		task.setThread(pingThread);
		pingThread.setDaemon(true);
		pingThread.setName("XmppConnection Pinger");
		pingThread.start();
	}

	public boolean sendPing() {
		IQ req = new IQ() {
			public String getChildElementXML() {
				return "<ping xmlns='urn:xmpp:ping'/>";
			}
		};
		req.setType(IQ.Type.GET);
        PacketFilter filter = new AndFilter(new PacketIDFilter(req.getPacketID()),
                new PacketTypeFilter(IQ.class));
        PacketCollector collector = mConnection.createPacketCollector(filter);
        mConnection.sendPacket(req);
        IQ result = (IQ)collector.nextResult(PING_TIMEOUT);
        if (result == null)
        {
        	Log.e(TAG, "ping timeout");
        	return false;
        }
        collector.cancel();
        return true;
	}
	
	/**
	 * A TimerTask that keeps connections to the server alive by sending a space
	 * character on an interval.
	 */
	class PingTask implements Runnable {

		private static final int INITIAL_PING_DELAY = 15000;
		private long delay;
		private Thread thread;

		public PingTask(long delay) {
			this.delay = delay;
		}

		protected void setThread(Thread thread) {
			this.thread = thread;
		}

		public void run() {
			try {
				// Sleep 15 seconds before sending first heartbeat. This will give time to
				// properly finish TLS negotiation and then start sending heartbeats.
				Thread.sleep(INITIAL_PING_DELAY);
			}
			catch (InterruptedException ie) {
				// Do nothing
			}
			while (mConnection != null && pingThread == thread) {
				Log.d(TAG, "ping");
				if (mConnection.isConnected() && !sendPing()) {
					Log.i(TAG, "ping failed");
					mConnection.force_shutdown();
				}
				try {
					// Sleep until we should write the next keep-alive.
					Thread.sleep(delay);
				}
				catch (InterruptedException ie) {
					// Do nothing
				}
			}
		}
	}
	
	static class MyXMPPConnection extends XMPPConnection {

		public MyXMPPConnection(ConnectionConfiguration config) {
			super(config);
		}

	}
}

