package info.guardianproject.otr;

import info.guardianproject.otr.IOtrChatSession.Stub;
import info.guardianproject.util.Debug;
import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import android.os.RemoteException;

public class OtrChatSessionAdapter extends Stub {

    private OtrChatManager _chatManager;
    private SessionID _sid = null;
    
    public OtrChatSessionAdapter(String localUser, String remoteUser, OtrChatManager chatManager) {

        _chatManager = chatManager;
        _sid = _chatManager.getSessionId(localUser, remoteUser);
        
    }
    
    public void startChatEncryption() throws RemoteException {
        Debug.wrapExceptions(new Runnable() {
            @Override
            public void run() {
                if (_chatManager != null)
                {
                     _chatManager.startSession(_sid);                 
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
                 
                            _chatManager.endSession(_sid);
                        
                  
                }
            }
        });
    }

    @Override
    public boolean isChatEncrypted() throws RemoteException {

        if (_sid != null)
            return _chatManager.getSessionStatus(_sid) == SessionStatus.ENCRYPTED;
        else
            return false;
    }


    @Override
    public int getChatStatus() throws RemoteException {
        
        if (_chatManager != null)
        {
            SessionStatus sessionStatus = _chatManager.getSessionStatus(_sid);
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
            _chatManager.initSmp(_sid, question,
                    secret);
        } catch (OtrException e) {
            OtrDebugLogger.log("initSmp", e);
            throw new RemoteException();
        }
    }

    @Override
    public void respondSmpVerification(String answer) throws RemoteException {

        try {
            _chatManager.respondSmp(_sid, answer);
            
        } catch (OtrException e) {
            OtrDebugLogger.log("respondSmp", e);
            throw new RemoteException();
        }
    }
    
    @Override
    public void verifyKey(String address) throws RemoteException {
        
        _chatManager.getKeyManager().verify(_sid);
        
    }

    @Override
    public void unverifyKey(String address) throws RemoteException {
        _chatManager.getKeyManager().unverify(_sid);
    }

    @Override
    public boolean isKeyVerified(String address) throws RemoteException {
        return _chatManager.getKeyManager().isVerified(_sid);
    }

    @Override
    public String getLocalFingerprint() throws RemoteException {
        
         SessionID sid = _sid;
         
         if (sid != null)
             return _chatManager.getKeyManager().getLocalFingerprint(sid);
         else
             return null;
        
    }

    @Override
    public String getRemoteFingerprint() throws RemoteException {
        return _chatManager.getKeyManager().getRemoteFingerprint(_sid);
    }

    @Override
    public void generateLocalKeyPair() throws RemoteException {
        _chatManager.getKeyManager().generateLocalKeyPair(_sid);
    }

    @Override
    public String getLocalUserId() throws RemoteException {
        return _sid.getLocalUserId();
    }

    @Override
    public String getRemoteUserId() throws RemoteException {
        return _sid.getRemoteUserId();
    }

}
