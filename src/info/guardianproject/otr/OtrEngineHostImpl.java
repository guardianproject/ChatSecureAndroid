package info.guardianproject.otr;

import info.guardianproject.otr.app.im.engine.ChatSessionManager;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.service.ChatSessionAdapter;
import info.guardianproject.otr.app.im.service.ChatSessionManagerAdapter;
import info.guardianproject.otr.app.im.service.ImConnectionAdapter;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionID;
import android.util.Log;

/* OtrEngineHostImpl is the connects this app and the OtrEngine
 * http://code.google.com/p/otr4j/wiki/QuickStart
 */
public class OtrEngineHostImpl implements OtrEngineHost {
	
	private ImConnectionAdapter mConnection;
	private OtrPolicy policy;
    public String lastInjectedMessage;
    
	private OtrAndroidKeyManagerImpl otrKeyManager;

	private final static String OTR_KEYSTORE_PATH ="otr_keystore";
	
	private final static String TAG = OtrEngineHostImpl.class.getClass().getName();
	
	public OtrEngineHostImpl(ImConnectionAdapter imConnectionAdapter, OtrPolicy policy) throws IOException 
	{
		this.mConnection = imConnectionAdapter;
		this.policy = policy;
		otrKeyManager = new OtrAndroidKeyManagerImpl(OTR_KEYSTORE_PATH);
		
	}
	
	public void storeRemoteKey (SessionID sessionID, PublicKey remoteKey)
	{
		otrKeyManager.savePublicKey(sessionID, remoteKey);
	}
	
	public boolean isRemoteKeyVerified (SessionID sessionID)
	{
		return otrKeyManager.isVerified(sessionID);
	}
	
	public String getLocalKeyFingerprint (SessionID sessionID)
	{
		return otrKeyManager.getLocalFingerprint(sessionID);
	}
	
	public String getRemoteKeyFingerprint (SessionID sessionID)
	{
		return otrKeyManager.getRemoteFingerprint(sessionID);
	}
	
	@Override
	public KeyPair getKeyPair(SessionID sessionID) {
		 
		KeyPair kp = null;
		
		
		kp = otrKeyManager.loadLocalKeyPair(sessionID);
		
		if (kp != null)
			return kp;
		else
		{
		
         	otrKeyManager.generateLocalKeyPair(sessionID);
         	
         	kp = otrKeyManager.loadLocalKeyPair(sessionID);
         	
         	return kp;
		}
	}

	@Override
	public OtrPolicy getSessionPolicy(SessionID sessionID) {
		return this.policy;
	}
	
	
	@Override
	public void injectMessage(SessionID sessionID, String text) {
		ChatSessionManagerAdapter chatSessionManagerAdapter = (ChatSessionManagerAdapter)mConnection.getChatSessionManager();
		ChatSessionAdapter chatSessionAdapter = (ChatSessionAdapter)chatSessionManagerAdapter.getChatSession(sessionID.getUserID());
		ChatSessionManager chatSessionManager = chatSessionManagerAdapter.getChatSessionManager();
		Message msg = new Message(text);
		msg.setFrom(mConnection.getLoginUser().getAddress());
		msg.setTo(chatSessionAdapter.getAdaptee().getParticipant().getAddress());
		chatSessionManager.sendMessageAsync(chatSessionAdapter.getAdaptee(), msg);
	}

	@Override
	public void showError(SessionID sessionID, String error) {
		Log.e(TAG, sessionID.getUserID() + ": " + error);
	}

	@Override
	public void showWarning(SessionID sessionID, String warning) {
		Log.w(TAG, sessionID.getUserID() + ": " +  warning);
	}

}
