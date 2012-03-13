package info.guardianproject.otr;

import java.util.List;

import info.guardianproject.otr.IOtrChatSession.Stub;
import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;
import android.os.RemoteException;
import android.util.Log;

public class OtrChatSessionAdapter extends Stub {

	private OtrChatManager _chatManager;
	
	private String _localUser;
	private String _remoteUser;
	
	public OtrChatSessionAdapter (String localUser, String remoteUser, OtrChatManager chatManager)
	{
		
		_chatManager = chatManager;
		
		_localUser = localUser;
		_remoteUser = remoteUser;
		
		
	}
	
	public void startChatEncryption() throws RemoteException {
		
		_chatManager.startSession(_localUser, _remoteUser);
	}

	@Override
	public void stopChatEncryption() throws RemoteException {
	
		_chatManager.endSession(_localUser, _remoteUser);
		
	}

	@Override
	public boolean isChatEncrypted() throws RemoteException {
		
		return _chatManager.getSessionStatus(_localUser, _remoteUser) == SessionStatus.ENCRYPTED;
		
	}

	@Override
	public void initSmpVerification(String question, String secret)
			throws RemoteException {

		
		try {
			_chatManager.initSmp(_chatManager.getSessionId(_localUser, _remoteUser), question, secret);
		} catch (OtrException e) {
			OtrDebugLogger.log("initSmp",e);
			throw new RemoteException ();
		}
	}

	@Override
	public void respondSmpVerification(String answer)
			throws RemoteException {
		

		try {
			_chatManager.respondSmp(_chatManager.getSessionId(_localUser, _remoteUser), answer);
		} catch (OtrException e) {
			OtrDebugLogger.log("respondSmp",e);
			throw new RemoteException ();
		}
	}
	
	
	

}
