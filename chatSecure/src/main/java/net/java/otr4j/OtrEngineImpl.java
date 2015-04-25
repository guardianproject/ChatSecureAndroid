/*
 * otr4j, the open source java otr librar
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.otr4j;

import info.guardianproject.otr.OtrChatListener;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionImpl;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;

/** @author George Politis */
public class OtrEngineImpl implements OtrEngine {

    public OtrEngineImpl(OtrEngineHost host) {
        if (host == null)
            throw new IllegalArgumentException("OtrEgineHost is required.");

        this.setHost(host);

        if (sessions == null)
            sessions = new Hashtable<String, Session>();

    }

    private OtrEngineHost host;
    private Map<String, Session> sessions;

    public Session getSession(SessionID sessionID) {

        if (sessionID == null || sessionID.equals(SessionID.Empty))
            throw new IllegalArgumentException();

        if (!sessions.containsKey(sessionID.toString())) {
            Session session = new SessionImpl(sessionID, getHost());
            sessions.put(sessionID.toString(), session);

            session.addOtrEngineListener(new OtrEngineListener() {

                public void sessionStatusChanged(SessionID sessionID) {
                    for (OtrEngineListener l : listeners)
                        l.sessionStatusChanged(sessionID);
                }
            });
            return session;
        } else
        {
            SessionImpl session = (SessionImpl)sessions.get(sessionID.toString());
            session.setSessionID(sessionID);//make sure latest instance is stored in session (in case JIDs get updated)
            return session;
        }
    }

    public SessionStatus getSessionStatus(SessionID sessionID) {
        return this.getSession(sessionID).getSessionStatus();
    }

    public String transformReceiving(SessionID sessionID, String msgText) throws OtrException {
        return this.getSession(sessionID).transformReceiving(msgText);
    }

    public String transformReceiving(SessionID sessionID, String msgText, List<TLV> tlvs) throws OtrException {
        return this.getSession(sessionID).transformReceiving(msgText, tlvs);
    }

    public String transformSending(SessionID sessionID, String msgText) throws OtrException {
        return this.getSession(sessionID).transformSending(msgText, null);
    }

    public String transformSending(SessionID sessionID, String msgText, List<TLV> tlvs)
            throws OtrException {
        return this.getSession(sessionID).transformSending(msgText, tlvs);
    }

    public String transformSending(SessionID sessionID, String msgText, boolean isResponse, byte[] data)
            throws OtrException {
        List<TLV> tlvs = null;
        if (data != null) {
            tlvs = new ArrayList<TLV>(1);
            tlvs.add(new TLV(isResponse ? OtrChatListener.TLV_DATA_RESPONSE : OtrChatListener.TLV_DATA_REQUEST, data));
        }
        return this.getSession(sessionID).transformSending(msgText, tlvs);
    }


    public void endSession(SessionID sessionID) throws OtrException {
        getSession(sessionID).endSession();
        sessions.remove(sessionID.toString());
    }

    public void startSession(SessionID sessionID) throws OtrException {
        this.getSession(sessionID).refreshSession();
    }

    private void setHost(OtrEngineHost host) {
        this.host = host;
    }

    private OtrEngineHost getHost() {
        return host;
    }

    public void refreshSession(SessionID sessionID) throws OtrException {
        this.getSession(sessionID).refreshSession();
    }

    public PublicKey getRemotePublicKey(SessionID sessionID) {
        return this.getSession(sessionID).getRemotePublicKey();
    }

    private List<OtrEngineListener> listeners = new Vector<OtrEngineListener>();

    public void addOtrEngineListener(OtrEngineListener l) {
        synchronized (listeners) {
            if (!listeners.contains(l))
                listeners.add(l);
        }
    }

    public void removeOtrEngineListener(OtrEngineListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }
}
