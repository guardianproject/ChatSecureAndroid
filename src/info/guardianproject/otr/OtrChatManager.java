package info.guardianproject.otr;

//Originally: package com.zadov.beem;

import info.guardianproject.otr.app.im.app.SmpResponseActivity;
import info.guardianproject.otr.app.im.service.ImConnectionAdapter;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Hashtable;
import java.util.List;

import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
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
	
	private final static String TAG = "OtrChatManager";
	
	private OtrEngineHostImpl mOtrEngineHost;
	private OtrEngineImpl mOtrEngine;	
	private OtrSm mOtrSm;
	private Hashtable<String,SessionID> mSessions;
	private Context mContext;
		
	
	private OtrChatManager (int otrPolicy,Context context) throws Exception
	{
		mOtrEngineHost = new OtrEngineHostImpl(new OtrPolicyImpl(otrPolicy), context);
		
		mOtrEngine = new OtrEngineImpl(mOtrEngineHost);
		mOtrEngine.addOtrEngineListener(this);
		
		mSessions = new Hashtable<String,SessionID>();
		
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
			Log.e(TAG, "refreshSession", e);
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
			Log.e(TAG, "startSession", e);

		}
		
		return null;
	}
	
	public void endSession(String localUserId, String remoteUserId){
		
		try {
			SessionID sessionId = getSessionId(localUserId,remoteUserId);
		
			mOtrEngine.endSession(sessionId);
		} catch (OtrException e) {
			Log.e(TAG, "endSession", e);
		}
	}

	public void status(String localUserId, String remoteUserId){
		mOtrEngine.getSessionStatus(getSessionId(localUserId,remoteUserId)).toString();
	}
	
	public String decryptMessage(String localUserId, String remoteUserId, String msg){

		String plain = null;
		
		SessionID sessionId = getSessionId(localUserId,remoteUserId);
		Log.i(TAG,"session status: " + mOtrEngine.getSessionStatus(sessionId));

		if(mOtrEngine != null && sessionId != null){
			try {

				plain = mOtrEngine.transformReceiving(sessionId, msg);					

				List<TLV> tlvs = mOtrSm.getPendingTlvs();
				if (tlvs != null) {
					String encrypted = mOtrEngine.transformSending(sessionId, "", tlvs);
	    			mOtrEngineHost.injectMessage(sessionId, encrypted);
	    			
				}
				
				if (plain != null && plain.length() == 0)
					return null;
				
				
			} catch (OtrException e) { 
				Log.e(TAG,"error decrypting message",e);
			}

		}
		return plain;
	}
	
	/*
	public void processMessageReceiving(String localUserId, String remoteUserId, String msg)
	{

		
		SessionID sessionId = getSessionId(localUserId,remoteUserId);
		//Log.i(TAG,"session status: " + mOtrEngine.getSessionStatus(sessionId));

		if(mOtrEngine != null && sessionId != null){
			try {
				mOtrEngine.transformReceiving(sessionId, msg);
				
				
			} catch (OtrException e) {
				Log.e(TAG,"error decrypting message",e);
			}

		}
	}*/
	
	public String encryptMessage(String localUserId, String remoteUserId, String msg){
		
		SessionID sessionId = getSessionId(localUserId,remoteUserId);

		Log.i(TAG,"session status: " + mOtrEngine.getSessionStatus(sessionId));

		if(mOtrEngine != null && sessionId != null) {
			try {
				msg = mOtrEngine.transformSending(sessionId, msg);
			} catch (OtrException e) {
				Log.d(TAG, "error encrypting", e);
			}	
		}
		return msg;
	}
	
	
	@Override
	public void sessionStatusChanged(SessionID sessionID) {
		SessionStatus sStatus = mOtrEngine.getSessionStatus(sessionID);
		
		Log.i(TAG,"session status changed: " + sStatus);
		
		if (sStatus == SessionStatus.ENCRYPTED)
		{
			
			PublicKey remoteKey = mOtrEngine.getRemotePublicKey(sessionID);
			mOtrEngineHost.storeRemoteKey(sessionID, remoteKey);
			
			// SMP handler - make sure we only add this once per session!
			mOtrSm = new OtrSm(mOtrEngine.getSession(sessionID), mOtrEngineHost.getKeyManager(), sessionID, OtrChatManager.this);
			mOtrEngine.getSession(sessionID).addTlvHandler(mOtrSm);
		}
		else if (sStatus == SessionStatus.PLAINTEXT)
		{
			if (mOtrSm != null)
			{
				try {
					mOtrEngine.getSession(sessionID).removeTlvHandler(mOtrSm);
					mOtrSm.abortSmp();
					
				} catch (OtrException e) {
					Log.e(TAG, "error aborting SMP",e);
				}
			}
			
			mOtrSm = null;
			
		}
		else if (sStatus == SessionStatus.FINISHED)
		{

			if (mOtrSm != null)
			{
				try {
					mOtrEngine.getSession(sessionID).removeTlvHandler(mOtrSm);
					mOtrSm.abortSmp();
				} catch (OtrException e) {
					Log.e(TAG, "error aborting SMP",e);
				}
			}
			
			mOtrSm = null;
			
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
			Log.i(TAG,"remote key fingerprint: " + rkFingerprint);
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
		
		android.os.Debug.waitForDebugger();
				
		Intent dialog = new Intent(mContext, SmpResponseActivity.class);
		dialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		dialog.putExtra("q", question);
		dialog.putExtra("sid", sessionID.getUserID());
		
		mContext.startActivity(dialog);
		

	}
	
	public void respondSmp  (SessionID sessionID, String secret) throws OtrException
	{

		
		List<TLV> tlvs;
		
		tlvs = mOtrSm.initRespondSmp(null, secret, false);
		String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
		mOtrEngineHost.injectMessage(sessionID, encrypted);
		
	}
	
	public void initSmp (SessionID sessionID, String question, String secret) throws OtrException
	{
		List<TLV> tlvs;		
		tlvs = mOtrSm.initRespondSmp(question, secret, true);
		String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);		
		mOtrEngineHost.injectMessage(sessionID, encrypted);

	}
	
	public void abortSmp (SessionID sessionID) throws OtrException
	{
		List<TLV> tlvs = mOtrSm.abortSmp();
		String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
		mOtrEngineHost.injectMessage(sessionID, encrypted);

	}

}
