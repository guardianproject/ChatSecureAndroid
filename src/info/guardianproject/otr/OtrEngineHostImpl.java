package info.guardianproject.otr;

import info.guardianproject.otr.app.im.engine.ChatSessionManager;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.service.ChatSessionAdapter;
import info.guardianproject.otr.app.im.service.ChatSessionManagerAdapter;
import info.guardianproject.otr.app.im.service.ImConnectionAdapter;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Date;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionID;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/* OtrEngineHostImpl is the connects this app and the OtrEngine
 * http://code.google.com/p/otr4j/wiki/QuickStart
 */
public class OtrEngineHostImpl implements OtrEngineHost {
	
	private ImConnectionAdapter mConnection;
	private OtrPolicy policy;
    
	private OtrAndroidKeyManagerImpl mOtrKeyManager;

	private final static String OTR_KEYSTORE_PATH ="otr_keystore";
	
	private final static String TAG = "OtrEngineHostImpl";
	
	public OtrEngineHostImpl(OtrPolicy policy, Context context) throws IOException 
	{
		this.policy = policy;
		
		File storeFile = new File(context.getFilesDir(), OTR_KEYSTORE_PATH);
		
		mOtrKeyManager = OtrAndroidKeyManagerImpl.getInstance(storeFile.getAbsolutePath());
	}
	
	public void setConnection (ImConnectionAdapter imConnectionAdapter)
	{
		this.mConnection = imConnectionAdapter;

	}
	
	public OtrAndroidKeyManagerImpl getKeyManager ()
	{
		return mOtrKeyManager;
	}
	
	public void storeRemoteKey (SessionID sessionID, PublicKey remoteKey)
	{
		mOtrKeyManager.savePublicKey(sessionID, remoteKey);
	}
	
	public boolean isRemoteKeyVerified (SessionID sessionID)
	{
		return mOtrKeyManager.isVerified(sessionID);
	}
	
	public String getLocalKeyFingerprint (SessionID sessionID)
	{
		return mOtrKeyManager.getLocalFingerprint(sessionID);
	}
	
	public String getRemoteKeyFingerprint (SessionID sessionID)
	{
		return mOtrKeyManager.getRemoteFingerprint(sessionID);
	}
	
	@Override
	public KeyPair getKeyPair(SessionID sessionID) {
		KeyPair kp = null;
		kp = mOtrKeyManager.loadLocalKeyPair(sessionID);
		
		if (kp == null)
		{
         	mOtrKeyManager.generateLocalKeyPair(sessionID);	
         	kp = mOtrKeyManager.loadLocalKeyPair(sessionID);
		}
     	return kp;
	}

	@Override
	public OtrPolicy getSessionPolicy(SessionID sessionID) {
		return this.policy;
	}
	
	public void setSessionPolicty (OtrPolicy policy)
	{
		this.policy = policy;
	}
	
	private void sendMessage (SessionID sessionID, String body)
	{
		ChatSessionManagerAdapter chatSessionManagerAdapter = (ChatSessionManagerAdapter)mConnection.getChatSessionManager();
		ChatSessionAdapter chatSessionAdapter = (ChatSessionAdapter)chatSessionManagerAdapter.getChatSession(sessionID.getUserID());
		ChatSessionManager chatSessionManager = chatSessionManagerAdapter.getChatSessionManager();
		
		Message msg = new Message(body);
		
		msg.setFrom(mConnection.getLoginUser().getAddress());
		msg.setTo(chatSessionAdapter.getAdaptee().getParticipant().getAddress());
		msg.setDateTime(new Date());
		msg.setID(msg.getFrom() + ":" + msg.getDateTime().getTime());
		chatSessionManager.sendMessageAsync(chatSessionAdapter.getAdaptee(), msg);
	}
	
	@Override
	public void injectMessage(SessionID sessionID, String text) {
		
		Log.i(TAG, sessionID.toString() + ": injecting message: " + text);
		
		sendMessage(sessionID,text);
		
	}

	@Override
	public void showError(SessionID sessionID, String error) {
		Log.e(TAG, sessionID.toString() + ": " + error);
		
		sendMessage(sessionID,error);
	}

	@Override
	public void showWarning(SessionID sessionID, String warning) {
		Log.w(TAG, sessionID.toString() + ": " +  warning);
		
		sendMessage(sessionID,warning);
    
	}

}
