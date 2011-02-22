/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.session;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.crypto.interfaces.DHPublicKey;

import net.java.otr4j.OtrException;
import net.java.otr4j.crypto.OtrCryptoEngine;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.io.SerializationUtils;

/**
 * 
 * @author George Politis
 */
class SessionKeysImpl implements SessionKeys {

	private static Logger logger = Logger.getLogger(SessionKeysImpl.class
			.getName());
	private String keyDescription;

	public SessionKeysImpl(int localKeyIndex, int remoteKeyIndex) {
		if (localKeyIndex == 0)
			keyDescription = "(Previous local, ";
		else
			keyDescription = "(Most recent local, ";

		if (remoteKeyIndex == 0)
			keyDescription += "Previous remote)";
		else
			keyDescription += "Most recent remote)";

	}

	public void setLocalPair(KeyPair keyPair, int localPairKeyID) {
		this.localPair = keyPair;
		this.setLocalKeyID(localPairKeyID);
		logger.finest(keyDescription + " current local key ID: "
				+ this.getLocalKeyID());
		this.reset();
	}

	public void setRemoteDHPublicKey(DHPublicKey pubKey, int remoteKeyID) {
		this.setRemoteKey(pubKey);
		this.setRemoteKeyID(remoteKeyID);
		logger.finest(keyDescription + " current remote key ID: "
				+ this.getRemoteKeyID());
		this.reset();
	}

	private byte[] sendingCtr = new byte[16];
	private byte[] receivingCtr = new byte[16];

	public void incrementSendingCtr() {
		logger.finest("Incrementing counter for (localkeyID, remoteKeyID) = ("
				+ getLocalKeyID() + "," + getRemoteKeyID() + ")");
		// logger.debug("Counter prior increament: " +
		// Utils.dump(sendingCtr,
		// true, 16));
		for (int i = 7; i >= 0; i--)
			if (++sendingCtr[i] != 0)
				break;
		// logger.debug("Counter after increament: " +
		// Utils.dump(sendingCtr,
		// true, 16));
	}

	public byte[] getSendingCtr() {
		return sendingCtr;
	}

	public byte[] getReceivingCtr() {
		return receivingCtr;
	}

	public void setReceivingCtr(byte[] ctr) {
		for (int i = 0; i < ctr.length; i++)
			receivingCtr[i] = ctr[i];
	}

	private void reset() {
		logger.finest("Resetting " + keyDescription + " session keys.");
		Arrays.fill(this.sendingCtr, (byte) 0x00);
		Arrays.fill(this.receivingCtr, (byte) 0x00);
		this.sendingAESKey = null;
		this.receivingAESKey = null;
		this.sendingMACKey = null;
		this.receivingMACKey = null;
		this.setIsUsedReceivingMACKey(false);
		this.s = null;
		if (getLocalPair() != null && getRemoteKey() != null) {
			this.isHigh = ((DHPublicKey) getLocalPair().getPublic()).getY()
					.abs().compareTo(getRemoteKey().getY().abs()) == 1;
		}

	}

	private byte[] h1(byte b) throws OtrException {

		try {
			byte[] secbytes = SerializationUtils.writeMpi(getS());

			int len = secbytes.length + 1;
			ByteBuffer buff = ByteBuffer.allocate(len);
			buff.put(b);
			buff.put(secbytes);
			byte[] result = new OtrCryptoEngineImpl().sha1Hash(buff.array());
			return result;
		} catch (Exception e) {
			throw new OtrException(e);
		}
	}

	public byte[] getSendingAESKey() throws OtrException {
		if (sendingAESKey != null)
			return sendingAESKey;

		byte sendbyte = LOW_SEND_BYTE;
		if (this.isHigh)
			sendbyte = HIGH_SEND_BYTE;

		byte[] h1 = h1(sendbyte);

		byte[] key = new byte[OtrCryptoEngine.AES_KEY_BYTE_LENGTH];
		ByteBuffer buff = ByteBuffer.wrap(h1);
		buff.get(key);
		logger.finest("Calculated sending AES key.");
		this.sendingAESKey = key;
		return sendingAESKey;
	}

	public byte[] getReceivingAESKey() throws OtrException {
		if (receivingAESKey != null)
			return receivingAESKey;

		byte receivebyte = LOW_RECEIVE_BYTE;
		if (this.isHigh)
			receivebyte = HIGH_RECEIVE_BYTE;

		byte[] h1 = h1(receivebyte);

		byte[] key = new byte[OtrCryptoEngine.AES_KEY_BYTE_LENGTH];
		ByteBuffer buff = ByteBuffer.wrap(h1);
		buff.get(key);
		logger.finest("Calculated receiving AES key.");
		this.receivingAESKey = key;

		return receivingAESKey;
	}

	public byte[] getSendingMACKey() throws OtrException {
		if (sendingMACKey != null)
			return sendingMACKey;

		sendingMACKey = new OtrCryptoEngineImpl().sha1Hash(getSendingAESKey());
		logger.finest("Calculated sending MAC key.");
		return sendingMACKey;
	}

	public byte[] getReceivingMACKey() throws OtrException {
		if (receivingMACKey == null) {
			receivingMACKey = new OtrCryptoEngineImpl()
					.sha1Hash(getReceivingAESKey());
			logger.finest("Calculated receiving AES key.");
		}
		return receivingMACKey;
	}

	private BigInteger getS() throws OtrException {
		if (s == null) {
			s = new OtrCryptoEngineImpl().generateSecret(getLocalPair()
					.getPrivate(), getRemoteKey());
			logger.finest("Calculating shared secret S.");
		}
		return s;
	}

	public void setS(BigInteger s) {
		this.s = s;
	}

	public void setIsUsedReceivingMACKey(Boolean isUsedReceivingMACKey) {
		this.isUsedReceivingMACKey = isUsedReceivingMACKey;
	}

	public Boolean getIsUsedReceivingMACKey() {
		return isUsedReceivingMACKey;
	}

	private void setLocalKeyID(int localKeyID) {
		this.localKeyID = localKeyID;
	}

	public int getLocalKeyID() {
		return localKeyID;
	}

	private void setRemoteKeyID(int remoteKeyID) {
		this.remoteKeyID = remoteKeyID;
	}

	public int getRemoteKeyID() {
		return remoteKeyID;
	}

	private void setRemoteKey(DHPublicKey remoteKey) {
		this.remoteKey = remoteKey;
	}

	public DHPublicKey getRemoteKey() {
		return remoteKey;
	}

	public KeyPair getLocalPair() {
		return localPair;
	}

	private int localKeyID;
	private int remoteKeyID;
	private DHPublicKey remoteKey;
	private KeyPair localPair;

	private byte[] sendingAESKey;
	private byte[] receivingAESKey;
	private byte[] sendingMACKey;
	private byte[] receivingMACKey;
	private Boolean isUsedReceivingMACKey;
	private BigInteger s;
	private Boolean isHigh;
}
