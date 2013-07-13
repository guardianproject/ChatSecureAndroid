package info.guardianproject.otr;

// Originally: package com.zadov.beem;

import info.guardianproject.otr.app.im.ImService;
import info.guardianproject.otr.app.im.app.SmpResponseActivity;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.service.ImConnectionAdapter;
import info.guardianproject.otr.app.im.service.ImServiceConstants;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.OtrSm;
import net.java.otr4j.session.OtrSm.OtrSmEngineHost;
import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;
import android.content.Intent;

/*
 * OtrChatManager keeps track of the status of chats and their OTR stuff
 */
public class OtrChatManager implements OtrEngineListener, OtrSmEngineHost {

    //the singleton instance
    private static OtrChatManager mInstance;

    private OtrEngineHostImpl mOtrEngineHost;
    private OtrEngineImpl mOtrEngine;
    private Hashtable<String, SessionID> mSessions;
    private Hashtable<SessionID, OtrSm> mOtrSms;

    private ImService mContext;

    private OtrChatManager(int otrPolicy, ImService context) throws Exception {
        mOtrEngineHost = new OtrEngineHostImpl(new OtrPolicyImpl(otrPolicy),
                context);

        mOtrEngine = new OtrEngineImpl(mOtrEngineHost);
        mOtrEngine.addOtrEngineListener(this);

        mSessions = new Hashtable<String, SessionID>();
        mOtrSms = new Hashtable<SessionID, OtrSm>();

        mContext = context;
    }

    public static synchronized OtrChatManager getInstance(int otrPolicy, ImService context)
            throws Exception {
        if (mInstance == null) {
            mInstance = new OtrChatManager(otrPolicy, context);
        }

        return mInstance;
    }

    public void addConnection(ImConnectionAdapter imConnectionAdapter) {
        mOtrEngineHost.addConnection(imConnectionAdapter);
    }

    public void removeConnection(ImConnectionAdapter imConnectionAdapter) {
        mOtrEngineHost.removeConnection(imConnectionAdapter);
    }

    public void addOtrEngineListener(OtrEngineListener oel) {
        mOtrEngine.addOtrEngineListener(oel);
    }

    public void setPolicy(int otrPolicy) {
        mOtrEngineHost.setSessionPolicy(new OtrPolicyImpl(otrPolicy));
    }

    public OtrAndroidKeyManagerImpl getKeyManager() {
        return mOtrEngineHost.getKeyManager();
    }

    public static String processUserId(String userId) {
        String result = userId.split(":")[0]; //remove any port indication in the username
        result = userId.split("/")[0];

        return result;
    }

    public static String processResource(String userId) {
        String[] splits = userId.split("/", 2);
        if (splits.length > 1)
            return splits[1];
        else
            return "UNKNOWN";
    }

    public SessionID getSessionId(String localUserId, String remoteUserId) {
        String sessionIdKey = processUserId(localUserId) + "+" + processUserId(remoteUserId);

        SessionID sessionId = mSessions.get(sessionIdKey);
        if (sessionId == null ||
                (!sessionId.getFullUserID().equals(remoteUserId) &&
                        remoteUserId.contains("/"))) {
            // Remote has changed (either different presence, or from generic JID to specific presence),
            // or we didn't have a session yet.
            // Create or replace sessionId with one that is specific to the new presence.
            sessionId = new SessionID(processUserId(localUserId), remoteUserId, "XMPP");
            mSessions.put(sessionIdKey, sessionId);
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

    public void endSession(String localUserId, String remoteUserId) {

        try {
            SessionID sessionId = getSessionId(localUserId, remoteUserId);

            mOtrEngine.endSession(sessionId);

        } catch (OtrException e) {
            OtrDebugLogger.log("endSession", e);
        }
    }

    public void status(String localUserId, String remoteUserId) {
        mOtrEngine.getSessionStatus(getSessionId(localUserId, remoteUserId)).toString();
    }

    public String decryptMessage(String localUserId, String remoteUserId, String msg, List<TLV> tlvs) throws OtrException {
        String plain = null;

        SessionID sessionId = getSessionId(localUserId, remoteUserId);
        OtrDebugLogger.log("session status: " + mOtrEngine.getSessionStatus(sessionId));

        if (mOtrEngine != null && sessionId != null) {
            mOtrEngineHost.putSessionResource(sessionId, processResource(remoteUserId));
            plain = mOtrEngine.transformReceiving(sessionId, msg, tlvs);
            OtrSm otrSm = mOtrSms.get(sessionId);

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

    public void transformSending(Message message) {
        transformSending(message, false, null);
    }
    
    public void transformSending(Message message, boolean isResponse, byte[] data) {
        String localUserId = message.getFrom().getAddress();
        String remoteUserId = message.getTo().getAddress();
        String body = message.getBody();

        SessionID sessionId = getSessionId(localUserId, remoteUserId);

        if (mOtrEngine != null && sessionId != null) {
            SessionStatus sessionStatus = mOtrEngine.getSessionStatus(sessionId);
            OtrDebugLogger.log("session status: " + sessionStatus);

            try {
                OtrPolicy sessionPolicy = getSessionPolicy(sessionId);

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
    }

    @Override
    public void sessionStatusChanged(SessionID sessionID) {
        SessionStatus sStatus = mOtrEngine.getSessionStatus(sessionID);

        OtrDebugLogger.log("session status changed: " + sStatus);

        final Session session = mOtrEngine.getSession(sessionID);
        OtrSm otrSm = mOtrSms.get(sessionID);

        
        if (sStatus == SessionStatus.ENCRYPTED) {

            PublicKey remoteKey = mOtrEngine.getRemotePublicKey(sessionID);
            mOtrEngineHost.storeRemoteKey(sessionID, remoteKey);

            if (otrSm == null) {
                // SMP handler - make sure we only add this once per session!
                otrSm = new OtrSm(session, mOtrEngineHost.getKeyManager(),
                        sessionID, OtrChatManager.this);
                session.addTlvHandler(otrSm);

                mOtrSms.put(sessionID, otrSm);
            }
        } else if (sStatus == SessionStatus.PLAINTEXT) {
            if (otrSm != null) {
                session.removeTlvHandler(otrSm);
                mOtrSms.remove(sessionID);
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
        dialog.putExtra("sid", sessionID.getUserID());
        ImConnectionAdapter connection = mOtrEngineHost.findConnection(sessionID.getAccountID());
        if (connection == null) {
            OtrDebugLogger.log("Could ask for secret - no connection for " + sessionID.getAccountID());
            return;
        }
        dialog.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, connection.getProviderId());

        mContext.getApplicationContext().startActivity(dialog);

    }

    public void respondSmp(SessionID sessionID, String secret) throws OtrException {
        OtrSm otrSm = mOtrSms.get(sessionID);

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
        OtrSm otrSm = mOtrSms.get(sessionID);

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
        OtrSm otrSm = mOtrSms.get(sessionID);
        
        if (otrSm == null)
            return;

        List<TLV> tlvs = otrSm.abortSmp();
        String encrypted = mOtrEngine.transformSending(sessionID, "", tlvs);
        mOtrEngineHost.injectMessage(sessionID, encrypted);
    }

}
