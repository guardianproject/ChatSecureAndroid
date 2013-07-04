/**
 * 
 */
package info.guardianproject.otr;

import net.java.otr4j.session.SessionID;
import android.os.RemoteException;

/** @author n8fr8 */
public class OtrKeyManagerAdapter extends IOtrKeyManager.Stub {

    private OtrAndroidKeyManagerImpl mKeyManager;

    private String mRemoteUserId;

    private String mAccountId;
    private OtrChatManager mOtrChatManager;

    public OtrKeyManagerAdapter(OtrChatManager otrChatManager, String accountId,
            String remoteUserId) {
        this.mKeyManager = otrChatManager.getKeyManager();
        this.mOtrChatManager = otrChatManager;

        // The session ID can change, depending on which remote presence we are talking
        // to at the moment.  Therefore, keep local/remote bare JIDs and look up the session ID
        // on demand.
        this.mRemoteUserId = remoteUserId;
        this.mAccountId = accountId;
    }

    @Override
    public void verifyKey(String address) throws RemoteException {
        SessionID sessionId = mOtrChatManager.getSessionId(mAccountId, address);
        mKeyManager.verifyUser(sessionId.getFullUserID());
    }

    @Override
    public void unverifyKey(String address) throws RemoteException {
        SessionID sessionId = mOtrChatManager.getSessionId(mAccountId, address);
        mKeyManager.unverifyUser(sessionId.getFullUserID());
    }

    @Override
    public boolean isKeyVerified(String address) throws RemoteException {
        SessionID sessionId = mOtrChatManager.getSessionId(mAccountId, address);
        return mKeyManager.isVerifiedUser(sessionId.getFullUserID());
    }

    @Override
    public String getLocalFingerprint() throws RemoteException {
        return mKeyManager.getLocalFingerprint(mAccountId);
    }

    @Override
    public String getRemoteFingerprint() throws RemoteException {
        SessionID sessionId = mOtrChatManager.getSessionId(mAccountId, mRemoteUserId);

        return mKeyManager.getRemoteFingerprint(sessionId);
    }

    @Override
    public void generateLocalKeyPair() throws RemoteException {

        mKeyManager.generateLocalKeyPair(mAccountId);
    }

}
