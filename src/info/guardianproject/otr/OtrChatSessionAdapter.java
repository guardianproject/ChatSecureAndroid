package info.guardianproject.otr;

import net.java.otr4j.session.SessionID;
import android.os.RemoteException;
import info.guardianproject.otr.IOtrChatSession.Stub;
import info.guardianproject.otr.app.im.engine.ChatSession;

public class OtrChatSessionAdapter extends Stub {

	private OtrAndroidKeyManagerImpl _keyManager;
	private OtrChatManager _chatManager;
	private ChatSession _chatSession;

	private SessionID _sessionId;
	
	private String _localUser;
	private String _remoteUser;
	
	public OtrChatSessionAdapter (String localUser, ChatSession chatSession, OtrAndroidKeyManagerImpl keyManager, OtrChatManager chatManager)
	{
		_keyManager = keyManager;
		_chatManager = chatManager;
		_chatSession = chatSession;
		_localUser = localUser;
		_remoteUser = _chatSession.getParticipant().getAddress().getFullName();
	
		
	}
	
	public void startChatEncryption() throws RemoteException {
		
		_chatManager.startSession(_localUser, _remoteUser);
		_sessionId = _chatManager.getSessionId(_localUser, _remoteUser);
	}

	@Override
	public void stopChatEncryption() throws RemoteException {
	
		_chatManager.endSession(_localUser, _remoteUser);
	}

	@Override
	public boolean isChatEncrypted() throws RemoteException {
		
		return _chatManager.isEncryptedSession(_localUser, _remoteUser);
		
	}

}
