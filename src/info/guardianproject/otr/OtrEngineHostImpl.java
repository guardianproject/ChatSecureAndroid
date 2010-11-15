package info.guardianproject.otr;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionID;

import org.gitian.android.im.plugin.xmpp.XmppConnection;

import android.util.Log;

public class OtrEngineHostImpl implements OtrEngineHost{
	
	private XmppConnection xConn;
	private OtrPolicy policy;
    public String lastInjectedMessage;
    
	private OtrAndroidKeyManagerImpl otrKeyManager;

	private final static String OTR_KEYSTORE_PATH ="otr_keystore";
	
	private final static String TAG = OtrEngineHostImpl.class.getClass().getName();
	
	public OtrEngineHostImpl(XmppConnection xConn, OtrPolicy policy) throws IOException 
	{
		this.xConn = xConn;
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
	public void injectMessage(SessionID sessionID, String msg) {
		
		org.jivesoftware.smack.packet.Message xMsg =
			new org.jivesoftware.smack.packet.Message(
					sessionID.getUserID(),
					org.jivesoftware.smack.packet.Message.Type.chat
					);
		
		
		xMsg.setBody(msg);
		
		xConn.sendMessage(xMsg);
		
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
