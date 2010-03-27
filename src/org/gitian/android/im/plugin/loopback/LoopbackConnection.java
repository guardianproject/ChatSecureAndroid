package org.gitian.android.im.plugin.loopback;

import java.util.Collection;
import java.util.HashMap;

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

public class LoopbackConnection extends ImConnection {

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
				// TODO Auto-generated method stub
				
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
				return null;
			}
			
			@Override
			public void loadContactListsAsync() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			protected ImConnection getConnection() {
				// TODO Auto-generated method stub
				return null;
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
		// TODO Auto-generated method stub
		return null;
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

}
