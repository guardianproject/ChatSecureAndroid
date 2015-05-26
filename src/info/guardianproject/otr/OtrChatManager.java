package info.guardianproject.otr;

// Originally: package com.zadov.beem;

import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.app.SmpResponseActivity;
import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.service.ImConnectionAdapter;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.otr.app.im.service.RemoteImService;
import info.guardianproject.util.Debug;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrKeyManager;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.OtrSm;
import net.java.otr4j.session.OtrSm.OtrSmEngineHost;
import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 * OtrChatManager keeps track of the status of chats and their OTR stuff
 */
public class OtrChatManager implements OtrEngineListener, OtrSmEngineHost {

    //the singleton instance
    private static OtrChatManager mInstance;

    private OtrEngineHostImpl mOtrEngineHost;
    private OtrEngineImpl mOtrEngine;
    private Hashtable<String, SessionID> mSessions;
    private Hashtable<String, OtrSm> mOtrSms;

    private Context mContext;

    private OtrChatManager(int otrPolicy, RemoteImService imService, OtrKeyManager otrKeyManager) throws Exception {

        mContext = (Context)imService;

        mOtrEngineHost = new OtrEngineHostImpl(new OtrPolicyImpl(otrPolicy),
                mContext, otrKeyManager, imService);

        mOtrEngine = new OtrEngineImpl(mOtrEngineHost);
        mOtrEngine.addOtrEngineListener(this);

        mSessions = new Hashtable<String, SessionID>();
        mOtrSms = new Hashtable<String, OtrSm>();



    }


    public static synchronized OtrChatManager getInstance(int otrPolicy, RemoteImService imService, OtrKeyManager otrKeyManager)
            throws Exception {
        if (mInstance == null) {
            mInstance = new OtrChatManager(otrPolicy, imService,otrKeyManager);
        }

        return mInstance;
    }

    public static OtrChatManager getInstance()
    {
        return mInstance;
    }

    public static void endAllSessions() {
        if (mInstance == null) {
            return;
        }
        Collection<SessionID> sessionIDs = mInstance.mSessions.values();
        for (SessionID sessionId : sessionIDs) {
            mInstance.endSession(sessionId);
        }
    }

    public static void endSessionsForAccount(Contact localUserContact) {
        if (mInstance == null) {
            return;
        }
        String localUserId = localUserContact.getAddress().getBareAddress();
        
        Enumeration<String> sKeys = mInstance.mSessions.keys();
        
        while (sKeys.hasMoreElements())
        {
            String sKey = sKeys.nextElement();
            if (sKey.contains(localUserId))
            {
                SessionID sessionId = mInstance.mSessions.get(sKey);
                
                if (sessionId != null)
                    mInstance.endSession(sessionId);
            }
        }
    }

    public void addOtrEngineListener(OtrEngineListener oel) {
        mOtrEngine.addOtrEngineListener(oel);
    }

    public void setPolicy(int otrPolicy) {
        mOtrEngineHost.setSessionPolicy(new OtrPolicyImpl(otrPolicy));
    }

    public OtrKeyManager getKeyManager() {
        return mOtrEngineHost.getKeyManager();
    }

    public static String processResource(String userId) {
        String[] splits = userId.split("/", 2);
        if (splits.length > 1)
            return splits[1];
        else
            return "UNKNOWN";
    }

    public SessionID getSessionId(String localUserId, String remoteUserId) {

        SessionID sIdTemp = new SessionID(localUserId, remoteUserId, "XMPP");
        SessionID sessionId = mSessions.get(sIdTemp.toString());

        if (sessionId == null)
        {
         // or we didn't have a session yet.
            sessionId = sIdTemp;
            mSessions.put(sessionId.toString(), sessionId);
        }
        else if ((!sessionId.getRemoteUserId().equals(remoteUserId)) &&
                        remoteUserId.contains("/")) {
            // Remote has changed (either different presence, or from generic JID to specific presence),
            // Create or replace sessionId with one that is specific to the new presence.

            //sessionId.updateRemoteUserId(remoteUserId);
            sessionId = sIdTemp;
            mSessions.put(sessionId.toString(), sessionId);

            if (Debug.DEBUG_ENABLED)
                Log.d(ImApp.LOG_TAG,"getting new otr session id: " + sessionId);

        }
        return sessionId;
    }

    /**
     * Tell if the session represented by a local user account and a remote user
     * account is currently encrypted or not.
     *
     * @param localUserId
     * @param remoteUserId
     * @return state
     */
    public SessionStatus getSessionStatus(String localUserId, String remoteUserId) {
        SessionID sessionId = getSessionId(localUserId, remoteUserId);
        if (sessionId == null)
            return null;


        return mOtrEngine.getSessionStatus(sessionId);

    }

    public SessionStatus getSessionStatus(SessionID sessionId) {

        return mOtrEngine.getSessionStatus(sessionId);

    }

    public void refreshSession(String localUserId, String remoteUserId) {
        try {
            mOtrEngine.refreshSession(getSessionId(localUserId, remoteUserId));
        } catch (OtrException e) {
            OtrDebugLogger.log("refreshSession", e);
        }
    }

    /**
     * Start a new OTR encryption session for the chat session represented by a
     * local user address and a remote user address.
     *
     * @param localUserId i.e. the account of the user of this phone
     * @param remoteUserId i.e. the account that this user is talking to
     */
    private SessionID startSession(String localUserId, String remoteUserId) {

        SessionID sessionId = getSessionId(localUserId, remoteUserId);

        try {

            mOtrEngine.startSession(sessionId);



            return sessionId;

        } catch (OtrException e) {
            OtrDebugLogger.log("startSession", e);

            showError(sessionId,"Unable to start OTR session: " + e.getLocalizedMessage());

        }

        return null;
    }


    /**
     * Start a new OTR encryption session for the chat session represented by a
     * local user address and a remote user address.
     *
     * @param localUserId i.e. the account of the user of this phone
     * @param remoteUserId i.e. the account that this user is talking to
     */
    public SessionID startSession(SessionID sessionId) {

        try {

            mOtrEngine.startSession(sessionId);
            
            return sessionId;

        } catch (OtrException e) {
            OtrDebugLogger.log("startSession", e);

            showError(sessionId,"Unable to start OTR session: " + e.getLocalizedMessage());

        }

        return null;
    }




    public void endSession(SessionID sessionId) {

        try {

            mOtrEngine.endSession(sessionId);
            mSessions.remove(sessionId.toString());

        } catch (OtrException e) {
            OtrDebugLogger.log("endSession", e);
        }
    }

    public void endSession(String localUserId, String remoteUserId) {

        SessionID sessionId = getSessionId(localUserId, remoteUserId);
        endSession(sessionId);

    }

    public void status(String localUserId, String remoteUserId) {
        mOtrEngine.getSessionStatus(getSessionId(localUserId, remoteUserId)).toString();
    }

    public String decryptMessage(String localUserId, String remoteUserId, String msg, List<TLV> tlvs) throws OtrException {
        String plain = null;

        SessionID sessionId = getSessionId(localUserId, remoteUserId);
       // OtrDebugLogger.log("session status: " + mOtrEngine.getSessionStatus(sessionId));

        if (mOtrEngine != null && sessionId != null) {

            mOtrEngineHost.putSessionResource(sessionId, processResource(remoteUserId));
            plain = mOtrEngine.transformReceiving(sessionId, msg, tlvs);
            OtrSm otrSm = mOtrSms.get(sessionId.toString());

            if (otrSm != null) {
                List<TLV> smTlvs = otrSm.getPendingTlvs();
                if (smTlvs != null) {
                    String encrypted = mOtrEngine.transformSending(sessionId, "", smTlvs);
                    mOtrEngineHost.injectMessage(sessionId, encrypted);
                }
            }

            if (plain != null && plain.length() == 0)
                return null;
        }
        return plain;
    }

    public boolean transformSending(Message message) {
        return transformSending(message, false, null);
    }

    public boolean transformSending(Message message, boolean isResponse, byte[] data) {
        String localUserId = message.getFrom().getAddress();
        String remoteUserId = message.getTo().getAddress();
        String body = message.getBody();

        SessionID sessionId = getSessionId(localUserId, remoteUserId);

        if (mOtrEngine != null && sessionId != null) {
            SessionStatus sessionStatus = mOtrEngine.getSessionStatus(sessionId);
            if (data != null && sessionStatus != SessionStatus.ENCRYPTED) {
                // Cannot send data without OTR, so start a session and drop message.
                // Message will be resent by caller when session is encrypted.
                startSession(sessionId);
                OtrDebugLogger.log("auto-start OTR on data send request");
                return false;
            }
            OtrDebugLogger.log("session status: " + sessionStatus);

            try {
                OtrPolicy sessionPolicy = getSessionPolicy(sessionId);

                if (sessionStatus == SessionStatus.PLAINTEXT && sessionPolicy.getRequireEncryption())
                {
                    startSession(sessionId);
                    return false;
                }
                if (sessionStatus != SessionStatus.PLAINTEXT || sessionPolicy.getRequireEncryption()) {
                    body = mOtrEngine.transformSending(sessionId, body, isResponse, data);
                    message.setTo(mOtrEngineHost.appendSessionResource(sessionId, message.getTo()));
                } else if (sessionStatus == SessionStatus.PLAINTEXT && sessionPolicy.getAllowV2()
                           && sessionPolicy.getSendWhitespaceTag()) {
                    // Work around asmack not sending whitespace tag for auto discovery
                    body += " \t  \t\t\t\t \t \t \t   \t \t  \t   \t\t  \t ";

                }
            } catch (OtrException e) {
                OtrDebugLogger.log("error encrypting", e);
            }
        }

        message.setBody(body);
        
        return true;
    }

    @Override
    public void sessionStatusChanged(SessionID sessionID) {
        SessionStatus sStatus = mOtrEngine.getSessionStatus(sessionID);

        OtrDebugLogger.log("session status changed: " + sStatus);

        final Session session = mOtrEngine.getSession(sessionID);
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        if (sStatus == SessionStatus.ENCRYPTED) {

            PublicKey remoteKey = mOtrEngine.getRemotePublicKey(sessionID);
            mOtrEngineHost.storeRemoteKey(sessionID, remoteKey);

            if (otrSm == null) {
                // SMP handler - make sure we only add this once per session!
                otrSm = new OtrSm(session, mOtrEngineHost.getKeyManager(),
                        sessionID, OtrChatManager.this);
                session.addTlvHandler(otrSm);

                mOtrSms.put(sessionID.toString(), otrSm);
            }
        } else if (sStatus == SessionStatus.PLAINTEXT) {
            if (otrSm != null) {
                session.removeTlvHandler(otrSm);
                mOtrSms.remove(sessionID.toString());
            }
            mOtrEngineHost.removeSessionResource(sessionID);
        } else if (sStatus == SessionStatus.FINISHED) {
            // Do nothing.  The user must take affirmative action to
            // restart or end the session, so that they don't send
            // plaintext by mistake.
        }

    }

    public String getLocalKeyFingerprint(String localUserId, String remoteUserId) {
        return mOtrEngineHost.getLocalKeyFingerprint(getSessionId(localUserId, remoteUserId));
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

        Intent dialog = new Intent(mContext.getApplicationContext(), SmpResponseActivity.class);
        dialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        dialog.putExtra("q", question);
        dialog.putExtra("sid", sessionID.getRemoteUserId());//yes "sid" = remoteUserId in this case - see SMPResponseActivity
        ImConnectionAdapter connection = mOtrEngineHost.findConnection(sessionID);
        if (connection == null) {
            OtrDebugLogger.log("Could ask for secret - no connection for " + sessionID.toString());
            return;
        }

        dialog.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, connection.getProviderId());

        mContext.getApplicationContext().startActivity(dialog);

    }

    public void respondSmp(SessionID sessionID, String secret) throws OtrException {
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        List<TLV> tlvs;

        if (otrSm == null) {
            showError(sessionID, "Could not respond to verification because conversation is not encrypted");
            return;
        }

        tlvs = otrSm.initRespondSmp(null, secret, false);
        String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
        mOtrEngineHost.injectMessage(sessionID, encrypted);
    }

    public void initSmp(SessionID sessionID, String question, String secret) throws OtrException {
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        List<TLV> tlvs;

        if (otrSm == null) {
            showError(sessionID, "Could not perform verification because conversation is not encrypted");
            return;
        }

        tlvs = otrSm.initRespondSmp(question, secret, true);
        String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
        mOtrEngineHost.injectMessage(sessionID, encrypted);
    }

    public void abortSmp(SessionID sessionID) throws OtrException {
        OtrSm otrSm = mOtrSms.get(sessionID.toString());

        if (otrSm == null)
            return;

        List<TLV> tlvs = otrSm.abortSmp();
        String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
        mOtrEngineHost.injectMessage(sessionID, encrypted);
    }


}
