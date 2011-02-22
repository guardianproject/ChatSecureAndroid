/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.io.messages;

/**
 * 
 * @author George Politis
 */
public abstract class AbstractEncodedMessage extends AbstractMessage {
	// Fields.
	public int protocolVersion;

	// Ctor.
	public AbstractEncodedMessage(int messageType, int protocolVersion) {
		super(messageType);
		this.protocolVersion = protocolVersion;
	}

	// Methods.
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + protocolVersion;
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
		AbstractEncodedMessage other = (AbstractEncodedMessage) obj;
		if (protocolVersion != other.protocolVersion)
			return false;
		return true;
	}

	// Encoded Message Types
	public static final int MESSAGE_DH_COMMIT = 0x02;
	public static final int MESSAGE_DATA = 0x03;
	public static final int MESSAGE_DHKEY = 0x0a;
	public static final int MESSAGE_REVEALSIG = 0x11;
	public static final int MESSAGE_SIGNATURE = 0x12;
}
