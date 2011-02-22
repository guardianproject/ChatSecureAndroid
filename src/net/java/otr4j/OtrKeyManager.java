package net.java.otr4j;

import java.security.KeyPair;
import java.security.PublicKey;

import net.java.otr4j.session.SessionID;

public abstract interface OtrKeyManager {

	public abstract void addListener(OtrKeyManagerListener l);

	public abstract void removeListener(OtrKeyManagerListener l);

	public abstract void verify(SessionID sessionID);

	public abstract void unverify(SessionID sessionID);

	public abstract boolean isVerified(SessionID sessionID);

	public abstract String getRemoteFingerprint(SessionID sessionID);

	public abstract String getLocalFingerprint(SessionID sessionID);

	public abstract void savePublicKey(SessionID sessionID, PublicKey pubKey);

	public abstract PublicKey loadRemotePublicKey(SessionID sessionID);

	public abstract KeyPair loadLocalKeyPair(SessionID sessionID);

	public abstract void generateLocalKeyPair(SessionID sessionID);
}
