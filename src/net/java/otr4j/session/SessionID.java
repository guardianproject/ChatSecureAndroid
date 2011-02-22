/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.session;

/**
 * 
 * @author George Politis
 * 
 */
public final class SessionID {

	public SessionID(String accountID, String userID, String protocolName) {
		this.setAccountID(accountID);
		this.setUserID(userID);
		this.setProtocolName(protocolName);
	}

	private String accountID;
	private String userID;
	private String protocolName;
	public static final SessionID Empty = new SessionID(null, null, null);

	public void setAccountID(String accountID) {
		this.accountID = accountID;
	}

	public String getAccountID() {
		return accountID;
	}

	private void setUserID(String userID) {
		this.userID = userID;
	}

	public String getUserID() {
		return userID;
	}

	private void setProtocolName(String protocolName) {
		this.protocolName = protocolName;
	}

	public String getProtocolName() {
		return protocolName;
	}

	public String toString() {
		return this.getAccountID() + "_" + this.getProtocolName() + "_"
				+ this.getUserID();
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
