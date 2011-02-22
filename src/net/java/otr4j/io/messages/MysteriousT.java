package net.java.otr4j.io.messages;

import java.util.Arrays;

import javax.crypto.interfaces.DHPublicKey;

public class MysteriousT {
	// Fields.
	public int protocolVersion;
	public int messageType;
	public int flags;
	public int senderKeyID;
	public int recipientKeyID;
	public DHPublicKey nextDH;
	public byte[] ctr;
	public byte[] encryptedMessage;

	// Ctor.
	public MysteriousT(int protocolVersion, int flags, int senderKeyID,
			int recipientKeyID, DHPublicKey nextDH, byte[] ctr,
			byte[] encryptedMessage) {

		this.protocolVersion = protocolVersion;
		this.messageType = AbstractEncodedMessage.MESSAGE_DATA;
		this.flags = flags;
		this.senderKeyID = senderKeyID;
		this.recipientKeyID = recipientKeyID;
		this.nextDH = nextDH;
		this.ctr = ctr;
		this.encryptedMessage = encryptedMessage;
	}

	// Methods.
	@Override
	public int hashCode() {
		// TODO: Needs work.
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(ctr);
		result = prime * result + Arrays.hashCode(encryptedMessage);
		result = prime * result + flags;
		result = prime * result + messageType;
		result = prime * result + ((nextDH == null) ? 0 : nextDH.hashCode());
		result = prime * result + protocolVersion;
		result = prime * result + recipientKeyID;
		result = prime * result + senderKeyID;
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
		MysteriousT other = (MysteriousT) obj;
		if (!Arrays.equals(ctr, other.ctr))
			return false;
		if (!Arrays.equals(encryptedMessage, other.encryptedMessage))
			return false;
		if (flags != other.flags)
			return false;
		if (messageType != other.messageType)
			return false;
		if (nextDH == null) {
			if (other.nextDH != null)
				return false;
		} else if (!nextDH.equals(other.nextDH))
			return false;
		if (protocolVersion != other.protocolVersion)
			return false;
		if (recipientKeyID != other.recipientKeyID)
			return false;
		if (senderKeyID != other.senderKeyID)
			return false;
		return true;
	}

}
