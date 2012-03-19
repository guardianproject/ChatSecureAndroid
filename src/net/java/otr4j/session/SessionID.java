/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.session;

import java.util.Date;

/**
 * 
 * @author George Politis
 * 
 */
public final class SessionID {

	public SessionID(String accountID, String userID, String protocolName) {
		this.accountID = accountID;
		this.userID = userID;
		this.protocolName = protocolName;
	}

	private String accountID;
	private String userID;
	private String protocolName;
	
	private String sessionId;
	
	public static final SessionID Empty = new SessionID(null, null, null);

	public String getAccountID() {
		return accountID;
	}

	
	public String getUserID() {
		return userID;
	}

	

	public String getProtocolName() {
		return protocolName;
	}

	public synchronized String toString() {
		
		  sessionId = getAccountID() + '_' + this.getProtocolName() + '_'
				+ this.getUserID();
		
		return sessionId;
		
	}

	public boolean equals(Object obj) {
		
		if (obj == this)
			return true;
		if (obj == null || obj.getClass() != this.getClass())
			return false;

		SessionID sessionID = (SessionID) obj;

		return this.toString().equals(sessionID.toString());
	}

	
	public int hashCode() {
		return this.toString().hashCode();
	}
}
