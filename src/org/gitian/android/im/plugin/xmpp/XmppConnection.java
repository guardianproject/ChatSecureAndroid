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
import org.gitian.android.im.engine.ImException;
import org.gitian.android.im.engine.LoginInfo;
import org.gitian.android.im.engine.Message;
import org.gitian.android.im.engine.Presence;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;

import android.os.Parcel;
import android.util.Log;

public class XmppConnection extends ImConnection {

	protected static final String TAG = "XmppConnection";
	private XmppContactList mContactListManager;
	private Contact mUser;
	private XMPPConnection mConnection;
	private XmppChatSessionManager mSessionManager;

	public XmppConnection() {
	}
	
	// TODO !connection drop
	// TODO !tests
	// TODO !battery tests
	// TODO !OTR
	// TODO !beta
	
	@Override
	protected void doUpdateUserPresenceAsync(Presence presence) {
		// mimic presence
		ContactList cl;
		try {
			cl = mContactListManager.getDefaultContactList();
		} catch (ImException e) {
			throw new RuntimeException(e);
		}
		if (cl == null)
			return;
		Collection<Contact> contacts = cl.getContacts();
		for (Iterator<Contact> iter = contacts.iterator(); iter.hasNext();) {
			Contact contact = iter.next();
			contact.setPresence(presence);
		}
		Contact[] contacts_array = new Contact[contacts.size()];
		contacts.toArray(contacts_array);
		mContactListManager.doPresence(contacts_array);
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
		mUserPresence = new Presence(Presence.AVAILABLE, "available", null, null, Presence.CLIENT_TYPE_DEFAULT);
		String username = loginInfo.getUserName();
		String []comps = username.split("@");
		if (comps.length != 2)
			throw new RuntimeException("username should be user@host");
		try {
			initConnection(comps[1], comps[0], loginInfo.getPassword(), "Android");
		} catch (XMPPException e) {
			throw new RuntimeException(e);
		}
		mUser = new Contact(new XmppAddress(comps[0], username), username);
		setState(LOGGED_IN, null);
	}

	private void initConnection(String serverHost, String login, String password, String resource) throws XMPPException {
    	ConnectionConfiguration config = new ConnectionConfiguration(serverHost);
		mConnection = new XMPPConnection(config);
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
					Contact contact = findOrCreateContact(address);
					mContactListManager.getSubscriptionRequestListener().onSubScriptionRequest(contact);
				}
			}
		}, new PacketTypeFilter(org.jivesoftware.smack.packet.Presence.class));
        mConnection.login(login, password, resource);
        org.jivesoftware.smack.packet.Presence presence = 
        	new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.available);
        mConnection.sendPacket(presence);
	}

	protected static String parseAddressBase(String from) {
		return from.replaceFirst("/.*", "");
	}

	protected static String parseAddressUser(String from) {
		return from.replaceFirst("@.*", "");
	}

	@Override
	public void logoutAsync() {
		setState(LOGGING_OUT, null);
		mConnection.disconnect();
		setState(DISCONNECTED, null);
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
			mConnection.getRoster().getGroup(list.getName()).setName(name);
			notifyContactListNameUpdated(list, name);
		}

		@Override
		public String normalizeAddress(String address) {
			return address;
		}

		@Override
		public void loadContactListsAsync() {
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
				}
				
				@Override
				public void entriesDeleted(Collection<String> addresses) {
					// TODO delete contacts from remote
					
				}
				
				@Override
				public void entriesAdded(Collection<String> addresses) {
					// TODO add contacts from remote
				}
			});
		}

		public void doPresence(Contact[] contacts) {
			notifyContactsPresenceUpdated(contacts);
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
			
		}

		@Override
		protected void doCreateContactListAsync(String name,
				Collection<Contact> contacts, boolean isDefault) {
			// TODO create contact list
		}

		@Override
		protected void doBlockContactAsync(String address, boolean block) {
			// TODO block contact
			
		}

		@Override
		protected void doAddContactToListAsync(String address, ContactList list)
				throws ImException {
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
            org.jivesoftware.smack.packet.Presence response =
            	new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.unsubscribed);
            response.setTo(contact);
            mConnection.sendPacket(response);
            mContactListManager.getSubscriptionRequestListener().onSubscriptionDeclined(contact);
		}

		@Override
		public Contact createTemporaryContact(String address) {
			return makeContact(address);
		}

		@Override
		public void approveSubscriptionRequest(String contact) {
            try {
            	// FIXME maybe need to check if already in another contact list
				mContactListManager.doAddContactToListAsync(contact, getDefaultContactList());
			} catch (ImException e) {
				Log.e(TAG, "failed to add " + contact + " to default list");
			}
            mContactListManager.getSubscriptionRequestListener().onSubscriptionApproved(contact);
		}
	}

	static class XmppAddress extends Address {
		
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
}
