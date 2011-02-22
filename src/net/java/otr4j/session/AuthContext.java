/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.session;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PublicKey;

import javax.crypto.interfaces.DHPublicKey;

import net.java.otr4j.OtrException;
import net.java.otr4j.io.messages.AbstractMessage;

/**
 * 
 * @author George Politis
 */
interface AuthContext {

	public static final int NONE = 0;
	public static final int AWAITING_DHKEY = 1;
	public static final int AWAITING_REVEALSIG = 2;
	public static final int AWAITING_SIG = 3;
	public static final int V1_SETUP = 4;
	public static final byte C_START = (byte) 0x01;
	public static final byte M1_START = (byte) 0x02;
	public static final byte M2_START = (byte) 0x03;
	public static final byte M1p_START = (byte) 0x04;
	public static final byte M2p_START = (byte) 0x05;

	public abstract void reset();

	public abstract boolean getIsSecure();

	public abstract DHPublicKey getRemoteDHPublicKey();

	public abstract KeyPair getLocalDHKeyPair() throws OtrException;

	public abstract BigInteger getS() throws OtrException;

	public abstract void handleReceivingMessage(AbstractMessage m)
			throws OtrException;

	public abstract void startV2Auth() throws OtrException;

	public abstract void respondV2Auth() throws OtrException;

	public abstract PublicKey getRemoteLongTermPublicKey();

	public abstract KeyPair getLocalLongTermKeyPair();
}