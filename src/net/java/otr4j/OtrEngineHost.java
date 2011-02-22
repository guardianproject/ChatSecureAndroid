/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j;

import java.security.KeyPair;

import net.java.otr4j.session.SessionID;

/**
 * 
 * This interface should be implemented by the host application. It is required
 * for otr4j to work properly.
 * 
 * @author George Politis
 * 
 */
public abstract interface OtrEngineHost {
	public abstract void injectMessage(SessionID sessionID, String msg);

	public abstract void showWarning(SessionID sessionID, String warning);

	public abstract void showError(SessionID sessionID, String error);

	public abstract OtrPolicy getSessionPolicy(SessionID sessionID);

	public abstract KeyPair getKeyPair(SessionID sessionID);
}
