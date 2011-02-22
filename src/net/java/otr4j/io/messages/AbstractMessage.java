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
public abstract class AbstractMessage {
	// Fields.
	public int messageType;

	// Ctor.
	public AbstractMessage(int messageType) {
		this.messageType = messageType;
	}

	// Methods.
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + messageType;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractMessage other = (AbstractMessage) obj;
		if (messageType != other.messageType)
			return false;
		return true;
	}

	// Unencoded
	public static final int MESSAGE_ERROR = 0xff;
	public static final int MESSAGE_QUERY = 0x100;
	public static final int MESSAGE_PLAINTEXT = 0x102;
}
