package info.guardianproject.otr.app.im.plugin.loopback;

import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.ChatGroupManager;
import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.ChatSessionManager;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ContactList;
import info.guardianproject.otr.app.im.engine.ContactListListener;
import info.guardianproject.otr.app.im.engine.ContactListManager;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImException;
import info.guardianproject.otr.app.im.engine.LoginInfo;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.Presence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;


import android.os.Parcel;

public class LoopbackConnection extends ImConnection {

	protected static final String TAG = "LoopbackConnection";
	private LoopbackContactList mContactListManager;
	private Contact mUser;

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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ChatGroupManager getChatGroupManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChatSessionManager getChatSessionManager() {
		return new ChatSessionManager() {
			
			@Override
			protected void sendMessageAsync(ChatSession session, Message message) {
				// Echo
				Message rec = new Message(message.getBody());
				rec.setFrom(message.getTo());
				rec.setDateTime(new Date());
				session.onReceiveMessage(rec);
			}
			
			/**
		     * Start encryption for this chat
		     */
		    public  boolean encryptChat(String address) { return false; }
		    
		     /**
		     * Stop encryption for this chat
		     */
		    public  boolean unencryptChat(String address) { return false; }
		    
		    
		    /**
		     * Is definitely not encrypted
		     */
		    public  boolean isEncryptedSession(String address) { return false; }
		    
		      /**
		     * Start remote identity verification
		     */
		    public void verifyRemoteIdentity(String address) {}
		    

		    /**
		    * Get public key fingerprint
		    */
		    public String getRemoteKeyFingerprint(String address) { return null; }
		   
		   /**
		    * Get public key fingerprint
		    */
		    public String getLocalKeyFingerprint(String address) { return null; }
		};
	}

	@Override
	public ContactListManager getContactListManager() {
		mContactListManager = new LoopbackContactList();
		return mContactListManager;
	}

	@Override
	public Contact getLoginUser() {
		return mUser;
	}

	@Override
	public HashMap<String, String> getSessionContext() {
		// TODO Auto-generated method stub
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
	public void loginAsync(LoginInfo loginInfo, boolean retry) {
		mUserPresence = new Presence(Presence.AVAILABLE, "available", null, null, Presence.CLIENT_TYPE_DEFAULT);
		mUser = new Contact(new LoopbackAddress(loginInfo.getUserName() + "!", "loopback"), loginInfo.getUserName());
		setState(LOGGED_IN, null);
	}

	@Override
	public void logoutAsync() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reestablishSessionAsync(HashMap<String, String> sessionContext) {
		// TODO Auto-generated method stub

	}

	@Override
	public void suspend() {
		// TODO Auto-generated method stub

	}

	private final class LoopbackContactList extends ContactListManager {
		@Override
		protected void setListNameAsync(String name, ContactList list) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String normalizeAddress(String address) {
			return address;
		}

		@Override
		public void loadContactListsAsync() {
			Collection<Contact> contacts = new ArrayList<Contact>();
			Contact[] contacts_array = new Contact[1];
			contacts.toArray(contacts_array);
			Address dummy_addr = new LoopbackAddress("dummy", "dummy@google.com");
			
			Contact dummy = new Contact(dummy_addr, "dummy");
			dummy.setPresence(new Presence(Presence.AVAILABLE, "available", null, null, Presence.CLIENT_TYPE_DEFAULT));
			contacts.add(dummy);
			
			ContactList cl = new ContactList(mUser.getAddress(), "default", true, contacts, this);
			mContactLists.add(cl);
			mDefaultContactList = cl;
			notifyContactListLoaded(cl);
			notifyContactsPresenceUpdated(contacts.toArray(contacts_array));
			notifyContactListsLoaded();
		}

		public void doPresence(Contact[] contacts) {
			notifyContactsPresenceUpdated(contacts);
		}

		@Override
		protected ImConnection getConnection() {
			return LoopbackConnection.this;
		}

		@Override
		protected void doRemoveContactFromListAsync(Contact contact,
				ContactList list) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void doDeleteContactListAsync(ContactList list) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void doCreateContactListAsync(String name,
				Collection<Contact> contacts, boolean isDefault) {
			// TODO Auto-generated method stub
			return;
			
		}

		@Override
		protected void doBlockContactAsync(String address, boolean block) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void doAddContactToListAsync(String address, ContactList list)
				throws ImException {
			Contact contact = new Contact(new LoopbackAddress(address, address), address);
			contact.setPresence(new Presence(Presence.AVAILABLE, "available", null, null, Presence.CLIENT_TYPE_DEFAULT));
			notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_ADDED, contact);
			Contact[] contacts = new Contact[] { contact };
			mContactListManager.doPresence(contacts);
		}

		@Override
		public void declineSubscriptionRequest(String contact) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Contact createTemporaryContact(String address) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void approveSubscriptionRequest(String contact) {
			// TODO Auto-generated method stub
			return;
		}
	}

	class LoopbackAddress extends Address {
		
		private String address;
		private String name;

		public LoopbackAddress() {
		}
		
		public LoopbackAddress(String name, String address) {
			this.name = name;
			this.address = address;
		}

		@Override
		public String getFullName() {
			return name;
		}

		@Override
		public String getScreenName() {
			return address;
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
	
	public void sendHeartbeat() {
	}
	
	public void setProxy (String type, String host, int port)
	{
		
	}

}
