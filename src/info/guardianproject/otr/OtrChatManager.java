package info.guardianproject.otr;

//Originally: package com.zadov.beem;

import info.guardianproject.otr.app.im.plugin.xmpp.XmppConnection;

import java.security.PublicKey;
import java.util.Hashtable;

import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;


import android.util.Log;

public class OtrChatManager implements OtrEngineListener {

	private final static String TAG = "Xmpp";
	protected XmppConnection xConn;
	protected OtrEngineHostImpl myHost;
	private OtrEngineImpl otrEngine;
	
	private Hashtable<String,SessionID> sessions;
	
	public OtrChatManager(XmppConnection xConn){
		this.xConn = xConn;

		try
		{
			myHost = new OtrEngineHostImpl(xConn, 
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
	public boolean isEncryptedSession (String localUserId, String remoteUserId)
	{
		if (otrEngine.getSessionStatus(getSessionId(localUserId,remoteUserId)) == SessionStatus.ENCRYPTED)
			return true;
		else
			return false;
	}

	public void refreshSession (String localUserId, String remoteUserId)
	{
		otrEngine.refreshSession(getSessionId(localUserId,remoteUserId));
	}
	
	public void startSession(String localUserId, String remoteUserId) {

		otrEngine.startSession(getSessionId(localUserId,remoteUserId));

	}
	
	public void endSession(String localUserId, String remoteUserId){
		otrEngine.endSession(getSessionId(localUserId,remoteUserId));
	}

	public void status(String localUserId, String remoteUserId){
		otrEngine.getSessionStatus(getSessionId(localUserId,remoteUserId)).toString();
	}
	
	public String receiveMessage(String localUserId, String remoteUserId, String msg){
		//Log.i(TAG,"session status: " + otrEngine.getSessionStatus(localSessionID));

	//	Log.d(TAG, "in-cypher: "+msg);
		String plain = null;
		
		if(otrEngine != null){
			plain = otrEngine.transformReceiving(getSessionId(localUserId,remoteUserId), msg);
			
			//remove HTML in text
			plain = plain.replaceAll("\\<.*?\\>", "");

		//	Log.d(TAG,"in-plain: "+plain);
		}
		
		return plain;
	}
	
	public String sendMessage(String localUserId, String remoteUserId, String msg){
//		Log.i(TAG,"session status: " + otrEngine.getSessionStatus(localSessionID));

	//	Log.d(TAG, "out-plain: "+msg);
		if(otrEngine != null){

			msg = otrEngine.transformSending(getSessionId(localUserId,remoteUserId), msg);
			
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
