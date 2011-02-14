/**
 * 
 */
package info.guardianproject.otr;

import net.java.otr4j.session.SessionID;
import android.os.RemoteException;
import info.guardianproject.otr.IOtrKeyManager.Stub;
import info.guardianproject.otr.app.im.engine.ChatSession;

/**
 * @author n8fr8
 *
 */
public class OtrKeyManagerAdapter extends IOtrKeyManager.Stub {

	private OtrAndroidKeyManagerImpl _keyManager;
	private OtrChatManager _chatManager;
	private ChatSession _chatSession;

	private SessionID sessionId;
	
	public OtrKeyManagerAdapter (String localUser, ChatSession chatSession, OtrAndroidKeyManagerImpl keyManager, OtrChatManager chatManager)
	{
		_keyManager = keyManager;
		_chatManager = chatManager;
		_chatSession = chatSession;
		
		String remoteUser = _chatSession.getParticipant().getAddress().getFullName();

		
		sessionId = _chatManager.getSessionId(localUser, remoteUser);
		
		
	}	
	



	/* (non-Javadoc)
	 * @see info.guardianproject.otr.IOtrKeyManager#verifyKey(java.lang.String)
	 */
	@Override
	public void verifyKey(String address) throws RemoteException {
		
		
		_keyManager.verifyUser(address);
		
	}

	/* (non-Javadoc)
	 * @see info.guardianproject.otr.IOtrKeyManager#unverifyKey(java.lang.String)
	 */
	@Override
	public void unverifyKey(String address) throws RemoteException {
		
		_keyManager.unverifyUser(address);

	}

	/* (non-Javadoc)
	 * @see info.guardianproject.otr.IOtrKeyManager#isKeyVerified(java.lang.String)
	 */
	@Override
	public boolean isKeyVerified(String address) throws RemoteException {
		return _keyManager.isVerifiedUser(address);
	}

	/* (non-Javadoc)
	 * @see info.guardianproject.otr.IOtrKeyManager#getLocalFingerprint(java.lang.String)
	 */
	@Override
	public String getLocalFingerprint() throws RemoteException {
		
		return _keyManager.getLocalFingerprint(sessionId);
		
	}

	/* (non-Javadoc)
	 * @see info.guardianproject.otr.IOtrKeyManager#getRemoteFingerprint(java.lang.String)
	 */
	@Override
	public String getRemoteFingerprint() throws RemoteException {
		
		return _keyManager.getRemoteFingerprint(sessionId);
	}

	/* (non-Javadoc)
	 * @see info.guardianproject.otr.IOtrKeyManager#generateLocalKeyPair(java.lang.String)
	 */
	@Override
	public void generateLocalKeyPair(String address) throws RemoteException {
		
		_keyManager.generateLocalKeyPair(address);

	}

}
