package org.gitian.android.im.plugin.xmpp;

import java.util.HashMap;

import org.gitian.android.im.engine.ChatGroupManager;
import org.gitian.android.im.engine.ChatSessionManager;
import org.gitian.android.im.engine.Contact;
import org.gitian.android.im.engine.ContactListManager;
import org.gitian.android.im.engine.ImConnection;
import org.gitian.android.im.engine.LoginInfo;
import org.gitian.android.im.engine.Presence;

public class XmppConnection extends ImConnection {

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContactListManager getContactListManager() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub

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
