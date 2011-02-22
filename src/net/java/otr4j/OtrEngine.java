package net.java.otr4j;

import java.security.PublicKey;

import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;

/**
 * 
 * @author George Politis
 * 
 */
public interface OtrEngine {

	/**
	 * 
	 * @param sessionID
	 *            The session identifier.
	 * @param content
	 *            The message content to be transformed.
	 * @return The transformed message content.
	 * @throws OtrException 
	 */
	public abstract String transformReceiving(SessionID sessionID,
			String content) throws OtrException;

	/**
	 * 
	 * @param sessionID
	 *            The session identifier.
	 * @param content
	 *            The message content to be transformed.
	 * @return The transformed message content.
	 * @throws OtrException 
	 */
	public abstract String transformSending(SessionID sessionID, String content) throws OtrException;

	/**
	 * Starts an Off-the-Record session, if there is no active one.
	 * 
	 * @param sessionID
	 *            The session identifier.
	 * @throws OtrException 
	 */
	public abstract void startSession(SessionID sessionID) throws OtrException;

	/**
	 * Ends the Off-the-Record session, if exists.
	 * 
	 * @param sessionID
	 *            The session identifier.
	 * @throws OtrException 
	 */
	public abstract void endSession(SessionID sessionID) throws OtrException;

	/**
	 * Stops/Starts the Off-the-Record session.
	 * 
	 * @param sessionID
	 *            The session identifier.
	 * @throws OtrException 
	 */
	public abstract void refreshSession(SessionID sessionID) throws OtrException;

	/**
	 * 
	 * @param sessionID
	 *            The session identifier.
	 * @return The status of an Off-the-Record session.
	 */
	public abstract SessionStatus getSessionStatus(SessionID sessionID);

	/**
	 * 
	 * @param sessionID
	 *            The session identifier.
	 * @return The remote public key.
	 */
	public abstract PublicKey getRemotePublicKey(SessionID sessionID);

	public abstract void addOtrEngineListener(OtrEngineListener l);

	public abstract void removeOtrEngineListener(OtrEngineListener l);
}
