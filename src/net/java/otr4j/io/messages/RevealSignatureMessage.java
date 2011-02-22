/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.io.messages;

import java.util.Arrays;

/**
 * 
 * @author George Politis
 */
public class RevealSignatureMessage extends SignatureMessage {
	// Fields.
	public byte[] revealedKey;

	// Ctor.
	public RevealSignatureMessage(int protocolVersion, byte[] xEncrypted,
			byte[] xEncryptedMAC, byte[] revealedKey) {
		super(MESSAGE_REVEALSIG, protocolVersion, xEncrypted, xEncryptedMAC);

		this.revealedKey = revealedKey;
	}

	// Methods.
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(revealedKey);
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
		RevealSignatureMessage other = (RevealSignatureMessage) obj;
		if (!Arrays.equals(revealedKey, other.revealedKey))
			return false;
		return true;
	}
}
