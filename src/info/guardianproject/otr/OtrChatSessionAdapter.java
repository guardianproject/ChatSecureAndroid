package info.guardianproject.otr;

import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
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
	
	public OtrChatSessionAdapter (String localUser, String remoteUser, OtrAndroidKeyManagerImpl keyManager, OtrChatManager chatManager)
	{
		
		_keyManager = keyManager;
		_chatManager = chatManager;
		
		_localUser = localUser;
		_remoteUser = remoteUser;
		
		
	}
	
	public void startChatEncryption() throws RemoteException {
		
		_sessionId = _chatManager.startSession(_localUser, _remoteUser);
	}

	@Override
	public void stopChatEncryption() throws RemoteException {
	
		_chatManager.endSession(_localUser, _remoteUser);
	}

	@Override
	public boolean isChatEncrypted() throws RemoteException {
		
		return _chatManager.getSessionStatus(_localUser, _remoteUser) == SessionStatus.ENCRYPTED;
		
	}

}
