/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.io.messages;

import java.util.List;

/**
 * 
 * @author George Politis
 */
public class PlainTextMessage extends QueryMessage {
	// Fields.
	public String cleanText;

	// Ctor.
	public PlainTextMessage(List<Integer> versions, String cleanText) {
		super(MESSAGE_PLAINTEXT, versions);
		this.cleanText = cleanText;
	}

	// Methods.
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((cleanText == null) ? 0 : cleanText.hashCode());
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
		PlainTextMessage other = (PlainTextMessage) obj;
		if (cleanText == null) {
			if (other.cleanText != null)
				return false;
		} else if (!cleanText.equals(other.cleanText))
			return false;
		return true;
	}

}
