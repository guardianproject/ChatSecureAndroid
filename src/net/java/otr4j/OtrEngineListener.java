package net.java.otr4j;

import net.java.otr4j.session.SessionID;

/**
 * This interface should be implemented by the host application. It notifies
 * about session status changes.
 * 
 * @author George Politis
 * 
 */
public interface OtrEngineListener {
	public abstract void sessionStatusChanged(SessionID sessionID);
}
