/**
 * 
 */
package info.guardianproject.otr;

import net.java.otr4j.OtrKeyManager;
import net.java.otr4j.session.SessionID;
import android.os.RemoteException;

/** @author n8fr8 */
public class OtrKeyManagerAdapter extends IOtrKeyManager.Stub {

    private OtrKeyManager mKeyManager;

    private String mRemoteUserId;

    private String mAccountId;
    private OtrChatManager mOtrChatManager;

    private SessionID mSessionId;

    public OtrKeyManagerAdapter(OtrChatManager otrChatManager, String accountId,
            String remoteUserId) {
        
        this.mKeyManager = otrChatManager.getKeyManager();
        this.mOtrChatManager = otrChatManager;

        // The session ID can change, depending on which remote presence we are talking
        // to at the moment.  Therefore, keep local/remote bare JIDs and look up the session ID
        // on demand.
        this.mRemoteUserId = remoteUserId;
        this.mAccountId = accountId;
        
        mSessionId = mOtrChatManager.getSessionId(mAccountId, mRemoteUserId);
    }

    @Override
    public void verifyKey(String address) throws RemoteException {
        
        SessionID sessionId = mOtrChatManager.getSessionId(mAccountId, address);
        mKeyManager.verify(sessionId);
        
    }

    @Override
    public void unverifyKey(String address) throws RemoteException {
        SessionID sessionId = mOtrChatManager.getSessionId(mAccountId, address);
        mKeyManager.unverify(sessionId);
    }

    @Override
    public boolean isKeyVerified(String address) throws RemoteException {
        SessionID sessionId = mOtrChatManager.getSessionId(mAccountId, address);
        return mKeyManager.isVerified(sessionId);
    }

    @Override
    public String getLocalFingerprint() throws RemoteException {
        return mKeyManager.getLocalFingerprint(mSessionId);
        
    }

    @Override
    public String getRemoteFingerprint() throws RemoteException {
        SessionID sessionId = mOtrChatManager.getSessionId(mAccountId, mRemoteUserId);

        return mKeyManager.getRemoteFingerprint(sessionId);
    }

    @Override
    public void generateLocalKeyPair() throws RemoteException {
        mKeyManager.generateLocalKeyPair(mSessionId);
    }

}
