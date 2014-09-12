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
    
    public OtrChatSessionAdapter(String localUser, String remoteUser, OtrChatManager chatManager) {

        _chatManager = chatManager;
        _localUser = localUser;
        _remoteUser = remoteUser;
    }
    
    public void end () throws RemoteException 
    {
        
        Debug.wrapExceptions(new Runnable() {
            @Override
            public void run() {
                _chatManager.removeSession(_chatManager.getSessionId(_localUser, _remoteUser));
            }
        });
    }

    public void startChatEncryption() throws RemoteException {
        Debug.wrapExceptions(new Runnable() {
            @Override
            public void run() {
                if (_chatManager != null)
                {
                    SessionID sid = _chatManager.getSessionId(_localUser, _remoteUser);
                    if (sid != null)
                        _chatManager.startSession(sid);
                }
            }
        });
    }
    
    @Override
    public void stopChatEncryption() throws RemoteException {
        Debug.wrapExceptions(new Runnable() {
            @Override
            public void run() {
                if (_chatManager != null)
                {
                    SessionID sid = _chatManager.getSessionId(_localUser, _remoteUser);
                    if (sid != null)
                        _chatManager.endSession(sid);
                }
            }
        });
    }

    @Override
    public boolean isChatEncrypted() throws RemoteException {

        SessionID sid = _chatManager.getSessionId(_localUser, _remoteUser);
        
        if (sid != null)
            return _chatManager.getSessionStatus(sid) == SessionStatus.ENCRYPTED;
        else
            return false;
    }


    @Override
    public int getChatStatus() throws RemoteException {
        
        if (_chatManager != null)
        {
            SessionStatus sessionStatus = _chatManager.getSessionStatus(_chatManager.getSessionId(_localUser, _remoteUser));
            if (sessionStatus == null)
                sessionStatus = SessionStatus.PLAINTEXT;
            return sessionStatus.ordinal();
        }
        else
        {
            return SessionStatus.PLAINTEXT.ordinal();
        }
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
        
        _chatManager.getKeyManager().verify(_chatManager.getSessionId(_localUser, _remoteUser));
        
    }

    @Override
    public void unverifyKey(String address) throws RemoteException {
        _chatManager.getKeyManager().unverify(_chatManager.getSessionId(_localUser, _remoteUser));
    }

    @Override
    public boolean isKeyVerified(String address) throws RemoteException {
        return _chatManager.getKeyManager().isVerified(_chatManager.getSessionId(_localUser, _remoteUser));
    }

    @Override
    public String getLocalFingerprint() throws RemoteException {
        return _chatManager.getKeyManager().getLocalFingerprint(_chatManager.getSessionId(_localUser, _remoteUser));
        
    }

    @Override
    public String getRemoteFingerprint() throws RemoteException {
        return _chatManager.getKeyManager().getRemoteFingerprint(_chatManager.getSessionId(_localUser, _remoteUser));
    }

    @Override
    public void generateLocalKeyPair() throws RemoteException {
        _chatManager.getKeyManager().generateLocalKeyPair(_chatManager.getSessionId(_localUser, _remoteUser));
    }

    @Override
    public String getLocalUserId() throws RemoteException {
        return _localUser;
    }

    @Override
    public String getRemoteUserId() throws RemoteException {
        return _remoteUser;
    }

    
}
