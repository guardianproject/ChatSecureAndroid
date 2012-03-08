/*
 * otr4j, the open source java otr librar
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.otr4j;

import java.security.PublicKey;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionImpl;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;

/**
 * 
 * @author George Politis
 * 
 */
public class OtrEngineImpl implements OtrEngine {

	public OtrEngineImpl(OtrEngineHost host) {
		if (host == null)
			throw new IllegalArgumentException("OtrEgineHost is required.");

		this.setHost(host);
	}

	private OtrEngineHost host;
	private Map<SessionID, Session> sessions;

	public Session getSession(SessionID sessionID) {

		if (sessionID == null || sessionID.equals(SessionID.Empty))
			throw new IllegalArgumentException();

		if (sessions == null)
			sessions = new Hashtable<SessionID, Session>();

		if (!sessions.containsKey(sessionID)) {
			Session session = new SessionImpl(sessionID, getHost());
			sessions.put(sessionID, session);

			session.addOtrEngineListener(new OtrEngineListener() {

				public void sessionStatusChanged(SessionID sessionID) {
					for (OtrEngineListener l : listeners)
						l.sessionStatusChanged(sessionID);
				}
			});
			return session;
		} else
			return sessions.get(sessionID);
	}

	public SessionStatus getSessionStatus(SessionID sessionID) {
		return this.getSession(sessionID).getSessionStatus();
	}

	public String transformReceiving(SessionID sessionID, String msgText)
			throws OtrException {
		return this.getSession(sessionID).transformReceiving(msgText);
	}

	public String transformSending(SessionID sessionID, String msgText)
			throws OtrException {
		return this.getSession(sessionID).transformSending(msgText, null);
	}

	public String transformSending(SessionID sessionID, String msgText, List<TLV> tlvs)
			throws OtrException {
		return this.getSession(sessionID).transformSending(msgText, tlvs);
	}

	public void endSession(SessionID sessionID) throws OtrException {
		this.getSession(sessionID).endSession();
	}

	public void startSession(SessionID sessionID) throws OtrException {
		this.getSession(sessionID).startSession();
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
