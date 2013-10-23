package info.guardianproject.otr;

import info.guardianproject.otr.IOtrChatSession.Stub;
import info.guardianproject.util.Debug;
import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import android.os.RemoteException;

public class OtrChatSessionAdapter extends Stub {

    private OtrChatManager _chatManager;
    private String _localUser;
    private String _remoteUser;
    private SessionID _sessionId;
    
    public OtrChatSessionAdapter(String localUser, String remoteUser, OtrChatManager chatManager) {

        _chatManager = chatManager;
        _localUser = localUser;
        _remoteUser = remoteUser;

        _sessionId = chatManager.getSessionId(localUser, remoteUser);
    }

    public void startChatEncryption() throws RemoteException {
        Debug.wrapExceptions(new Runnable() {
            @Override
            public void run() {
                _chatManager.startSession(_localUser, _remoteUser);
            }
        });
    }
    
    @Override
    public void stopChatEncryption() throws RemoteException {
        Debug.wrapExceptions(new Runnable() {
            @Override
            public void run() {
                _chatManager.endSession(_localUser, _remoteUser);
            }
        });
    }

    @Override
    public boolean isChatEncrypted() throws RemoteException {

        return _chatManager.getSessionStatus(_localUser, _remoteUser) == SessionStatus.ENCRYPTED;

    }


    @Override
    public int getChatStatus() throws RemoteException {
        SessionStatus sessionStatus = _chatManager.getSessionStatus(_localUser, _remoteUser);
        if (sessionStatus == null)
            sessionStatus = SessionStatus.PLAINTEXT;
        return sessionStatus.ordinal();
    }

    @Override
    public void initSmpVerification(String question, String secret) throws RemoteException {

        try {
            _chatManager.initSmp(_chatManager.getSessionId(_localUser, _remoteUser), question,
                    secret);
        } catch (OtrException e) {
            OtrDebugLogger.log("initSmp", e);
            throw new RemoteException();
        }
    }

    @Override
    public void respondSmpVerification(String answer) throws RemoteException {

        try {
            _chatManager.respondSmp(_chatManager.getSessionId(_localUser, _remoteUser), answer);
            
        } catch (OtrException e) {
            OtrDebugLogger.log("respondSmp", e);
            throw new RemoteException();
        }
    }
    
    @Override
    public void verifyKey(String address) throws RemoteException {
        
        SessionID sessionId = _chatManager.getSessionId(_localUser, address);
        _chatManager.getKeyManager().verify(sessionId);
        
    }

    @Override
    public void unverifyKey(String address) throws RemoteException {
        SessionID sessionId = _chatManager.getSessionId(_localUser, address);
        _chatManager.getKeyManager().unverify(sessionId);
    }

    @Override
    public boolean isKeyVerified(String address) throws RemoteException {
        SessionID sessionId = _chatManager.getSessionId(_localUser, address);
        return _chatManager.getKeyManager().isVerified(sessionId);
    }

    @Override
    public String getLocalFingerprint() throws RemoteException {
        return _chatManager.getKeyManager().getLocalFingerprint(_sessionId);
        
    }

    @Override
    public String getRemoteFingerprint() throws RemoteException {
        SessionID sessionId = _chatManager.getSessionId(_localUser, _remoteUser);

        return _chatManager.getKeyManager().getRemoteFingerprint(sessionId);
    }

    @Override
    public void generateLocalKeyPair() throws RemoteException {
        _chatManager.getKeyManager().generateLocalKeyPair(_sessionId);
    }

}
