/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.io.messages;

import java.io.IOException;
import java.util.Arrays;

import net.java.otr4j.OtrException;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.io.SerializationUtils;

/**
 * 
 * @author George Politis
 */
public class SignatureMessage extends AbstractEncodedMessage {
	// Fields.
	public byte[] xEncrypted;
	public byte[] xEncryptedMAC;

	// Ctor.
	protected SignatureMessage(int messageType, int protocolVersion,
			byte[] xEncrypted, byte[] xEncryptedMAC) {
		super(messageType, protocolVersion);
		this.xEncrypted = xEncrypted;
		this.xEncryptedMAC = xEncryptedMAC;
	}

	public SignatureMessage(int protocolVersion, byte[] xEncrypted,
			byte[] xEncryptedMAC) {
		this(MESSAGE_SIGNATURE, protocolVersion, xEncrypted, xEncryptedMAC);
	}

	// Memthods.
	public byte[] decrypt(byte[] key) throws OtrException {
		return new OtrCryptoEngineImpl().aesDecrypt(key, null, xEncrypted);
	}

	public boolean verify(byte[] key) throws OtrException {
		// Hash the key.
		byte[] xbEncrypted;
		try {
			xbEncrypted = SerializationUtils.writeData(xEncrypted);
		} catch (IOException e) {
			throw new OtrException(e);
		}

		byte[] xEncryptedMAC = new OtrCryptoEngineImpl().sha256Hmac160(
				xbEncrypted, key);
		// Verify signature.
		return Arrays.equals(xEncryptedMAC, xEncryptedMAC);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(xEncrypted);
		result = prime * result + Arrays.hashCode(xEncryptedMAC);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SignatureMessage other = (SignatureMessage) obj;
		if (!Arrays.equals(xEncrypted, other.xEncrypted))
			return false;
		if (!Arrays.equals(xEncryptedMAC, other.xEncryptedMAC))
			return false;
		return true;
	}
}
