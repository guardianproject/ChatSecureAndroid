package info.guardianproject.otr;

import info.guardianproject.otr.IOtrChatSession.Stub;
import info.guardianproject.otr.app.im.engine.ChatSession;
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

        _localUser = localUser;
        _remoteUser = remoteUser;
        _chatManager = chatManager;
    }

    private SessionID getSessionID ()
    {
        if (_chatManager != null)
            return _chatManager.getSessionId(_localUser, _remoteUser);
        else
            return null;
    }

    public void startChatEncryption() throws RemoteException {
        Debug.wrapExceptions(new Runnable() {
            @Override
            public void run() {
                if (_chatManager != null)
                {
                     _chatManager.startSession(getSessionID ());
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

                            _chatManager.endSession(getSessionID ());


                }
            }
        });
    }

    @Override
    public boolean isChatEncrypted() throws RemoteException {

        if (getSessionID () != null)
            return _chatManager.getSessionStatus(getSessionID ()) == SessionStatus.ENCRYPTED;
        else
            return false;
    }


    @Override
    public int getChatStatus() throws RemoteException {

        if (_chatManager != null && getSessionID () != null)
        {
            SessionStatus sessionStatus = _chatManager.getSessionStatus(getSessionID ());
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
            _chatManager.initSmp(getSessionID (), question,
                    secret);
        } catch (OtrException e) {
            OtrDebugLogger.log("initSmp", e);
            throw new RemoteException();
        }
    }

    @Override
    public void respondSmpVerification(String answer) throws RemoteException {

        try {
            _chatManager.respondSmp(getSessionID (), answer);

        } catch (OtrException e) {
            OtrDebugLogger.log("respondSmp", e);
            throw new RemoteException();
        }
    }

    @Override
    public void verifyKey(String address) throws RemoteException {

        _chatManager.getKeyManager().verify(getSessionID ());

    }

    @Override
    public void unverifyKey(String address) throws RemoteException {
        _chatManager.getKeyManager().unverify(getSessionID ());
    }

    @Override
    public boolean isKeyVerified(String address) throws RemoteException {
        return _chatManager.getKeyManager().isVerified(getSessionID ());
    }

    @Override
    public String getLocalFingerprint() throws RemoteException {

         SessionID sid = getSessionID ();

         if (sid != null)
             return _chatManager.getKeyManager().getLocalFingerprint(sid);
         else
             return null;

    }

    @Override
    public String getRemoteFingerprint() throws RemoteException {
        return _chatManager.getKeyManager().getRemoteFingerprint(getSessionID ());
    }

    @Override
    public void generateLocalKeyPair() throws RemoteException {
        _chatManager.getKeyManager().generateLocalKeyPair(getSessionID ());
    }

    @Override
    public String getLocalUserId() throws RemoteException {
        return getSessionID ().getLocalUserId();
    }

    @Override
    public String getRemoteUserId() throws RemoteException {
        return getSessionID ().getRemoteUserId();
    }

}
