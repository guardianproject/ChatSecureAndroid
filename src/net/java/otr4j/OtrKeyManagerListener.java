package net.java.otr4j;

import net.java.otr4j.session.SessionID;

public interface OtrKeyManagerListener {
	public abstract void verificationStatusChanged(SessionID session);
}
