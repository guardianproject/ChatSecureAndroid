package info.guardianproject.otr;

//Originally: package com.zadov.beem;

import info.guardianproject.otr.app.im.service.ImConnectionAdapter;

import java.security.PublicKey;
import java.util.Hashtable;

import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;


import android.util.Log;

/* OtrChatManager keeps track of the status of chats and their OTR stuff
 */
public class OtrChatManager implements OtrEngineListener {

	private final static String TAG = "OtrChatManager";
	protected OtrEngineHostImpl myHost;
	private OtrEngineImpl otrEngine;
	
	private Hashtable<String,SessionID> sessions;
	
	public OtrChatManager(ImConnectionAdapter imConnectionAdapter){
		try
		{
			myHost = new OtrEngineHostImpl(imConnectionAdapter, 
				new OtrPolicyImpl(OtrPolicy.ALLOW_V2
						| OtrPolicy.ERROR_START_AKE));
		
			otrEngine = new OtrEngineImpl(myHost);
			otrEngine.addOtrEngineListener(this);
			
			sessions = new Hashtable<String,SessionID>();
			
		}
		catch (Exception re)
		{
			Log.w(TAG, re);
		}
	}
	
	public SessionID getSessionId (String localUserId, String remoteUserId)
	{
		SessionID sessionId = sessions.get(remoteUserId);
		
		if (sessionId == null)
		{
			sessionId = new SessionID(localUserId, remoteUserId, "XMPP");
			sessions.put(remoteUserId, sessionId);
		}
		return sessionId;
	}
	
	/**
	 * Tell if the session represented by a local user account and a 
	 * remote user account is currently encrypted or not.
	 * @param localUserId
	 * @param remoteUserId
	 * @return state
	 */
	public boolean isEncryptedSession (String localUserId, String remoteUserId)
	{
		if (otrEngine.getSessionStatus(getSessionId(localUserId,remoteUserId)) == SessionStatus.ENCRYPTED)
			return true;
		else
			return false;
	}

	public void refreshSession (String localUserId, String remoteUserId)
	{
		try {
			otrEngine.refreshSession(getSessionId(localUserId,remoteUserId));
		} catch (OtrException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Start a new OTR encryption session for the chat session represented by a
	 * local user address and a remote user address. 
	 * @param localUserId i.e. the account of the user of this phone
	 * @param remoteUserId i.e. the account that this user is talking to
	 */
	public void startSession(String localUserId, String remoteUserId) {
		try {
			otrEngine.startSession(getSessionId(localUserId,remoteUserId));
		} catch (OtrException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void endSession(String localUserId, String remoteUserId){
		try {
			otrEngine.endSession(getSessionId(localUserId,remoteUserId));
		} catch (OtrException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void status(String localUserId, String remoteUserId){
		otrEngine.getSessionStatus(getSessionId(localUserId,remoteUserId)).toString();
	}
	
	public String decryptMessage(String localUserId, String remoteUserId, String msg){
		//Log.i(TAG,"session status: " + otrEngine.getSessionStatus(localSessionID));

	//	Log.d(TAG, "in-cypher: "+msg);
		String plain = null;
		
		if(otrEngine != null){
			try {
				plain = otrEngine.transformReceiving(getSessionId(localUserId,remoteUserId), msg);
			} catch (OtrException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// TODO why remove HTML in text after decrypting?
			//remove HTML in text
			plain = plain.replaceAll("\\<.*?\\>", "");

		//	Log.d(TAG,"in-plain: "+plain);
		}
		return plain;
	}
	
	public String encryptMessage(String localUserId, String remoteUserId, String msg){
//		Log.i(TAG,"session status: " + otrEngine.getSessionStatus(localSessionID));

	//	Log.d(TAG, "out-plain: "+msg);
		if(otrEngine != null) {
			try {
				msg = otrEngine.transformSending(getSessionId(localUserId,remoteUserId), msg);
			} catch (OtrException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		//	Log.d(TAG, "out-cypher: "+msg);
		}
		return msg;
	}

	@Override
	public void sessionStatusChanged(SessionID sessionID) {
		Log.i(TAG,"session status changed: " + otrEngine.getSessionStatus(sessionID));
		if (otrEngine.getSessionStatus(sessionID) == SessionStatus.ENCRYPTED)
		{
			String sKey = sessionID.getUserID();
			if (sKey.indexOf("/")!=-1)
				sKey = sKey.substring(0,sKey.indexOf("/"));
			
			this.sessions.put(sKey, sessionID);
			
			if (!myHost.isRemoteKeyVerified(sessionID))
			{
				PublicKey remoteKey = otrEngine.getRemotePublicKey(sessionID);
				myHost.storeRemoteKey(sessionID, remoteKey);
				String rkFingerprint = myHost.getRemoteKeyFingerprint(sessionID);
				Log.i(TAG,"remote key fingerprint: " + rkFingerprint);			
			}
		}
		else if (otrEngine.getSessionStatus(sessionID) == SessionStatus.PLAINTEXT)
		{
		}
		else if (otrEngine.getSessionStatus(sessionID) == SessionStatus.FINISHED)
		{
		}
	}
	
	public String getLocalKeyFingerprint (String localUserId, String remoteUserId)
	{
		return myHost.getLocalKeyFingerprint(getSessionId(localUserId,remoteUserId));
	}
	
	public String getRemoteKeyFingerprint(String localUserId, String remoteUserId)
	{
		SessionID sessionID = getSessionId(localUserId,remoteUserId);
		String rkFingerprint = myHost.getRemoteKeyFingerprint(sessionID);

		if (rkFingerprint == null)
		{
			PublicKey remoteKey = otrEngine.getRemotePublicKey(sessionID);
			myHost.storeRemoteKey(sessionID, remoteKey);
			rkFingerprint = myHost.getRemoteKeyFingerprint(sessionID);
			Log.i(TAG,"remote key fingerprint: " + rkFingerprint);
		}
		return rkFingerprint;
	}
}
