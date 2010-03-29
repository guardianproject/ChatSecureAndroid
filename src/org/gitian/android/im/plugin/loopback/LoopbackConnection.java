package org.gitian.android.im.plugin.loopback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.gitian.android.im.engine.Address;
import org.gitian.android.im.engine.ChatGroupManager;
import org.gitian.android.im.engine.ChatSession;
import org.gitian.android.im.engine.ChatSessionManager;
import org.gitian.android.im.engine.Contact;
import org.gitian.android.im.engine.ContactList;
import org.gitian.android.im.engine.ContactListManager;
import org.gitian.android.im.engine.ImConnection;
import org.gitian.android.im.engine.ImException;
import org.gitian.android.im.engine.LoginInfo;
import org.gitian.android.im.engine.Message;
import org.gitian.android.im.engine.Presence;

import android.os.Parcel;
import android.text.method.DateTimeKeyListener;

public class LoopbackConnection extends ImConnection {

	protected static final String TAG = "LoopbackConnection";

	@Override
	protected void doUpdateUserPresenceAsync(Presence presence) {
		// TODO Auto-generated method stub

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
		};
	}

	@Override
	public ContactListManager getContactListManager() {
		// TODO Auto-generated method stub
		return new ContactListManager() {
			
			@Override
			protected void setListNameAsync(String name, ContactList list) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public String normalizeAddress(String address) {
				// TODO Auto-generated method stub
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
				
				ContactList cl = new ContactList(null, "default", true, contacts, this);
				mContactLists.add(cl);
				notifyContactListLoaded(cl);
				notifyContactsPresenceUpdated(contacts.toArray(contacts_array));
				notifyContactListsLoaded();
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
				// TODO Auto-generated method stub
				return;
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
		};
	}

	@Override
	public Contact getLoginUser() {
		// TODO Auto-generated method stub
		return null;
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
	public void loginAsync(LoginInfo loginInfo) {
		mUserPresence = new Presence(Presence.AVAILABLE, "available", null, null, Presence.CLIENT_TYPE_DEFAULT);
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
}
