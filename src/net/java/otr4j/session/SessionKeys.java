package net.java.otr4j.session;

import java.math.BigInteger;
import java.security.KeyPair;

import javax.crypto.interfaces.DHPublicKey;

import net.java.otr4j.OtrException;

public interface SessionKeys {

	public static final int Previous = 0;
	public static final int Current = 1;
	public static final byte HIGH_SEND_BYTE = (byte) 0x01;
	public static final byte HIGH_RECEIVE_BYTE = (byte) 0x02;
	public static final byte LOW_SEND_BYTE = (byte) 0x02;
	public static final byte LOW_RECEIVE_BYTE = (byte) 0x01;

	public abstract void setLocalPair(KeyPair keyPair, int localPairKeyID);

	public abstract void setRemoteDHPublicKey(DHPublicKey pubKey,
			int remoteKeyID);

	public abstract void incrementSendingCtr();

	public abstract byte[] getSendingCtr();

	public abstract byte[] getReceivingCtr();

	public abstract void setReceivingCtr(byte[] ctr);

	public abstract byte[] getSendingAESKey() throws OtrException;

	public abstract byte[] getReceivingAESKey() throws OtrException;

	public abstract byte[] getSendingMACKey() throws OtrException;

	public abstract byte[] getReceivingMACKey() throws OtrException;

	public abstract void setS(BigInteger s);

	public abstract void setIsUsedReceivingMACKey(Boolean isUsedReceivingMACKey);

	public abstract Boolean getIsUsedReceivingMACKey();

	public abstract int getLocalKeyID();

	public abstract int getRemoteKeyID();

	public abstract DHPublicKey getRemoteKey();

	public abstract KeyPair getLocalPair();

}