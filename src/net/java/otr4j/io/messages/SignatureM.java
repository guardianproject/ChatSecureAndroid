/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.io.messages;

import java.security.PublicKey;

import javax.crypto.interfaces.DHPublicKey;

/**
 * 
 * @author George Politis
 */
public class SignatureM {
	// Fields.
	public DHPublicKey localPubKey;
	public DHPublicKey remotePubKey;
	public PublicKey localLongTermPubKey;
	public int keyPairID;
	
	// Ctor.
	public SignatureM(DHPublicKey localPubKey, DHPublicKey remotePublicKey,
			PublicKey localLongTermPublicKey, int keyPairID) {

		this.localPubKey = localPubKey;
		this.remotePubKey = remotePublicKey;
		this.localLongTermPubKey = localLongTermPublicKey;
		this.keyPairID = keyPairID;
	}

	// Methods.
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + keyPairID;
		// TODO: Needs work.
		result = prime
				* result
				+ ((localLongTermPubKey == null) ? 0 : localLongTermPubKey
						.hashCode());
		result = prime * result
				+ ((localPubKey == null) ? 0 : localPubKey.hashCode());
		result = prime * result
				+ ((remotePubKey == null) ? 0 : remotePubKey.hashCode());
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
		SignatureM other = (SignatureM) obj;
		if (keyPairID != other.keyPairID)
			return false;
		if (localLongTermPubKey == null) {
			if (other.localLongTermPubKey != null)
				return false;
		} else if (!localLongTermPubKey.equals(other.localLongTermPubKey))
			return false;
		if (localPubKey == null) {
			if (other.localPubKey != null)
				return false;
		} else if (!localPubKey.equals(other.localPubKey))
			return false;
		if (remotePubKey == null) {
			if (other.remotePubKey != null)
				return false;
		} else if (!remotePubKey.equals(other.remotePubKey))
			return false;
		return true;
	}

}
