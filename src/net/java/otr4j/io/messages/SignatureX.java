/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.io.messages;

import java.security.PublicKey;
import java.util.Arrays;

/**
 * 
 * @author George Politis
 */
public class SignatureX {
	// Fields.
	public PublicKey longTermPublicKey;
	public int dhKeyID;
	public byte[] signature;

	// Ctor.
	public SignatureX(PublicKey ourLongTermPublicKey, int ourKeyID,
			byte[] signature) {
		this.longTermPublicKey = ourLongTermPublicKey;
		this.dhKeyID = ourKeyID;
		this.signature = signature;
	}

	// Methods.
	@Override
	public int hashCode() {
		// TODO: Needs work.
		final int prime = 31;
		int result = 1;
		result = prime * result + dhKeyID;
		result = prime
				* result
				+ ((longTermPublicKey == null) ? 0 : longTermPublicKey
						.hashCode());
		result = prime * result + Arrays.hashCode(signature);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO: Needs work.
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SignatureX other = (SignatureX) obj;
		if (dhKeyID != other.dhKeyID)
			return false;
		if (longTermPublicKey == null) {
			if (other.longTermPublicKey != null)
				return false;
		} else if (!longTermPublicKey.equals(other.longTermPublicKey))
			return false;
		if (!Arrays.equals(signature, other.signature))
			return false;
		return true;
	}

}
