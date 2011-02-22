/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.io.messages;

import javax.crypto.interfaces.DHPublicKey;

/**
 * 
 * @author George Politis
 */
public class DHKeyMessage extends AbstractEncodedMessage {

	// Fields.
	public DHPublicKey dhPublicKey;

	// Ctor.
	public DHKeyMessage(int protocolVersion, DHPublicKey dhPublicKey) {
		super(MESSAGE_DHKEY, protocolVersion);
		this.dhPublicKey = dhPublicKey;
	}

	// Methods.
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		// TODO: Needs work.
		result = prime * result
				+ ((dhPublicKey == null) ? 0 : dhPublicKey.hashCode());
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
		DHKeyMessage other = (DHKeyMessage) obj;
		if (dhPublicKey == null) {
			if (other.dhPublicKey != null)
				return false;
		} else if (dhPublicKey.getY().compareTo(other.dhPublicKey.getY()) != 0)
			return false;
		return true;
	}
}
