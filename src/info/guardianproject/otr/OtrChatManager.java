package info.guardianproject.otr;

//Originally: package com.zadov.beem;

import info.guardianproject.otr.app.im.app.SmpResponseActivity;
import info.guardianproject.otr.app.im.service.ImConnectionAdapter;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Hashtable;
import java.util.List;

import com.google.common.collect.Lists;

import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.io.SerializationUtils;
import net.java.otr4j.io.messages.PlainTextMessage;
import net.java.otr4j.session.OtrSm;
import net.java.otr4j.session.OtrSm.OtrSmEngineHost;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;

/* OtrChatManager keeps track of the status of chats and their OTR stuff
 */
public class OtrChatManager implements OtrEngineListener, OtrSmEngineHost {

	//the singleton instance
	private static OtrChatManager mInstance;	
	
	
	private OtrEngineHostImpl mOtrEngineHost;
	private OtrEngineImpl mOtrEngine;	
	private Hashtable<String,SessionID> mSessions;
	private Hashtable<SessionID,OtrSm> mOtrSms;
	
	private Context mContext;
	
	private OtrChatManager (int otrPolicy,Context context) throws Exception
	{
		mOtrEngineHost = new OtrEngineHostImpl(new OtrPolicyImpl(otrPolicy), context.getApplicationContext());
		
		mOtrEngine = new OtrEngineImpl(mOtrEngineHost);
		mOtrEngine.addOtrEngineListener(this);
		
		mSessions = new Hashtable<String,SessionID>();
		mOtrSms = new Hashtable<SessionID,OtrSm>();
		
		mContext = context;
	}
	
	
	public static synchronized OtrChatManager getInstance(int otrPolicy, Context context) throws Exception
	{
		if (mInstance == null)
		{
			mInstance = new OtrChatManager(otrPolicy, context);
		}
		
		return mInstance;
	}
	
	public void setConnection (ImConnectionAdapter imConnectionAdapter)
	{
		mOtrEngineHost.setConnection(imConnectionAdapter);
	}
	
	
	public void addOtrEngineListener (OtrEngineListener oel)
	{
		mOtrEngine.addOtrEngineListener(oel);
	}
	
	public void setPolicy(int otrPolicy) {
		mOtrEngineHost.setSessionPolicy(new OtrPolicyImpl(otrPolicy));
	}
	
	public OtrAndroidKeyManagerImpl getKeyManager ()
	{
		return mOtrEngineHost.getKeyManager();
	}
	
	public static String processUserId (String userId)
	{
		String result = userId.split(":")[0]; //remove any port indication in the username
		result = userId.split("/")[0];
		
		return result;
	}
	
	public SessionID getSessionId (String localUserId, String remoteUserId)
	{
		String sessionIdKey = processUserId(localUserId)+"+"+processUserId(remoteUserId);
		
		SessionID sessionId = mSessions.get(sessionIdKey);
		
		if (sessionId == null)
		{
			sessionId = new SessionID(processUserId(localUserId), processUserId(remoteUserId), "XMPP");
			mSessions.put(sessionIdKey, sessionId);
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
	public SessionStatus getSessionStatus (String localUserId, String remoteUserId)
	{
		SessionID sessionId = getSessionId(localUserId,remoteUserId);
		
		return mOtrEngine.getSessionStatus(sessionId);
		
	}

	public SessionStatus getSessionStatus (SessionID sessionId)
	{
		
		return mOtrEngine.getSessionStatus(sessionId);
		
	}
	
	public void refreshSession (String localUserId, String remoteUserId)
	{
		try {
			mOtrEngine.refreshSession(getSessionId(localUserId,remoteUserId));
		} catch (OtrException e) {
			OtrDebugLogger.log("refreshSession", e);
		}
	}
	
	/**
	 * Start a new OTR encryption session for the chat session represented by a
	 * local user address and a remote user address. 
	 * @param localUserId i.e. the account of the user of this phone
	 * @param remoteUserId i.e. the account that this user is talking to
	 */
	public SessionID startSession(String localUserId, String remoteUserId) {
		
	
		try {
			SessionID sessionId = getSessionId(localUserId, remoteUserId);
			mOtrEngine.startSession(sessionId);
			
			return sessionId;
			
		} catch (OtrException e) {
			OtrDebugLogger.log("startSession", e);

		}
		
		return null;
	}
	
	public void endSession(String localUserId, String remoteUserId){
		
		try {
			SessionID sessionId = getSessionId(localUserId,remoteUserId);
		
			mOtrEngine.endSession(sessionId);
			
			
		} catch (OtrException e) {
			OtrDebugLogger.log( "endSession", e);
		}
	}

	public void status(String localUserId, String remoteUserId){
		mOtrEngine.getSessionStatus(getSessionId(localUserId,remoteUserId)).toString();
	}
	
	public String decryptMessage(String localUserId, String remoteUserId, String msg){

		String plain = null;
		
		SessionID sessionId = getSessionId(localUserId,remoteUserId);
		OtrDebugLogger.log("session status: " + mOtrEngine.getSessionStatus(sessionId));

		if(mOtrEngine != null && sessionId != null){
			try {

				plain = mOtrEngine.transformReceiving(sessionId, msg);					
				OtrSm otrSm = mOtrSms.get(sessionId);
				
				if (otrSm != null)
				{
					List<TLV> tlvs = otrSm.getPendingTlvs();
					if (tlvs != null) {
						String encrypted = mOtrEngine.transformSending(sessionId, "", tlvs);
		    			mOtrEngineHost.injectMessage(sessionId, encrypted);
		    			
					}
				}
				
				if (plain != null && plain.length() == 0)
					return null;
				
			} catch (OtrException e) { 
				OtrDebugLogger.log("error decrypting message",e);
			}

		}
		return plain;
	}
	
	public void processMessageReceiving(String localUserId, String remoteUserId, String msg)
	{

		
		SessionID sessionId = getSessionId(localUserId,remoteUserId);

		if(mOtrEngine != null && sessionId != null){
			try {
				mOtrEngine.transformReceiving(sessionId, msg);
				
				
			} catch (OtrException e) {
				OtrDebugLogger.log("error decrypting message",e);
			}

		}
	}
	
	public String transformSending(String localUserId, String remoteUserId, String msg){
		
		SessionID sessionId = getSessionId(localUserId,remoteUserId);

		if (mOtrEngine != null && sessionId != null) {
			SessionStatus sessionStatus = mOtrEngine.getSessionStatus(sessionId);
			OtrDebugLogger.log("session status: " + sessionStatus);

			try {
				OtrPolicy sessionPolicy = getSessionPolicy(sessionId);
				
				if (sessionStatus == SessionStatus.ENCRYPTED) {
					msg = mOtrEngine.transformSending(sessionId, msg);
				} else if (sessionPolicy.getRequireEncryption()) {
					mOtrEngine.startSession(sessionId);
					msg = null;
					// TODO postpone this message until encryption negotiated
				} else if (sessionStatus == SessionStatus.PLAINTEXT &&
						sessionPolicy.getAllowV2() &&
						sessionPolicy.getSendWhitespaceTag()) {
					// Work around asmack not sending whitespace tag for auto discovery
					msg += " \t  \t\t\t\t \t \t \t   \t \t  \t   \t\t  \t ";
					  
				}
			} catch (OtrException e) {
				OtrDebugLogger.log( "error encrypting", e);
			}	
		}
		return msg;
	}
	
	
	@Override
	public void sessionStatusChanged(SessionID sessionID) {
		SessionStatus sStatus = mOtrEngine.getSessionStatus(sessionID);
		
		OtrDebugLogger.log("session status changed: " + sStatus);
		
		if (sStatus == SessionStatus.ENCRYPTED)
		{
			
			PublicKey remoteKey = mOtrEngine.getRemotePublicKey(sessionID);
			mOtrEngineHost.storeRemoteKey(sessionID, remoteKey);
			
			OtrSm otrSm = mOtrSms.get(sessionID);
			
			if (otrSm == null)
			{
				// SMP handler - make sure we only add this once per session!
				otrSm = new OtrSm(mOtrEngine.getSession(sessionID), mOtrEngineHost.getKeyManager(), sessionID, OtrChatManager.this);
				mOtrEngine.getSession(sessionID).addTlvHandler(otrSm);
				
				mOtrSms.put(sessionID, otrSm);
			}
		}
		else if (sStatus == SessionStatus.PLAINTEXT)
		{
			
			try {
				mOtrEngine.endSession(sessionID);
			} catch (OtrException e) {
				OtrDebugLogger.log("error ending session", e);
			}
			
		}
		else if (sStatus == SessionStatus.FINISHED)
		{
			try {
				mOtrEngine.endSession(sessionID);
			} catch (OtrException e) {
				OtrDebugLogger.log("error ending session", e);
			}
			mOtrSms.remove(sessionID);
			
		}
		
	}
	
	public String getLocalKeyFingerprint (String localUserId, String remoteUserId)
	{
		return mOtrEngineHost.getLocalKeyFingerprint(getSessionId(localUserId,remoteUserId));
	}
	
	public String getRemoteKeyFingerprint(String localUserId, String remoteUserId)
	{
		SessionID sessionID = getSessionId(localUserId,remoteUserId);
		String rkFingerprint = mOtrEngineHost.getRemoteKeyFingerprint(sessionID);

		if (rkFingerprint == null)
		{
			PublicKey remoteKey = mOtrEngine.getRemotePublicKey(sessionID);
			mOtrEngineHost.storeRemoteKey(sessionID, remoteKey);
			rkFingerprint = mOtrEngineHost.getRemoteKeyFingerprint(sessionID);
			OtrDebugLogger.log("remote key fingerprint: " + rkFingerprint);
		}
		return rkFingerprint;
	}


	@Override
	public void injectMessage(SessionID sessionID, String msg) {
		
		mOtrEngineHost.injectMessage(sessionID, msg);
	}


	@Override
	public void showWarning(SessionID sessionID, String warning) {
		
		mOtrEngineHost.showWarning(sessionID, warning);
		
	}


	@Override
	public void showError(SessionID sessionID, String error) {
		mOtrEngineHost.showError(sessionID, error);
		
	}


	@Override
	public OtrPolicy getSessionPolicy(SessionID sessionID) {
		
		return mOtrEngineHost.getSessionPolicy(sessionID);
	}


	@Override
	public KeyPair getKeyPair(SessionID sessionID) { 
		return mOtrEngineHost.getKeyPair(sessionID);
	}

	
	@Override
	public void askForSecret(SessionID sessionID, String question) {
		
		Intent dialog = new Intent(mContext, SmpResponseActivity.class);
		dialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		dialog.putExtra("q", question);
		dialog.putExtra("sid", sessionID.getUserID());
		
		mContext.startActivity(dialog);
		

	}
	
	public void respondSmp  (SessionID sessionID, String secret) throws OtrException
	{

		
		OtrSm otrSm = mOtrSms.get(sessionID);
		
		List<TLV> tlvs;
		
		tlvs = otrSm.initRespondSmp(null, secret, false);
		String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
		mOtrEngineHost.injectMessage(sessionID, encrypted);
		
	}
	
	public void initSmp (SessionID sessionID, String question, String secret) throws OtrException
	{
		OtrSm otrSm = mOtrSms.get(sessionID);
		
		List<TLV> tlvs;		
		tlvs = otrSm.initRespondSmp(question, secret, true);
		String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);		
		mOtrEngineHost.injectMessage(sessionID, encrypted);

	}
	
	public void abortSmp (SessionID sessionID) throws OtrException
	{
		OtrSm otrSm = mOtrSms.get(sessionID);
		
		List<TLV> tlvs = otrSm.abortSmp();
		String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
		mOtrEngineHost.injectMessage(sessionID, encrypted);

		
	}

}
