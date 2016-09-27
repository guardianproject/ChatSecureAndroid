package net.java.otr4j.session;

import java.net.ProtocolException;

@SuppressWarnings("serial")
public class UnknownInstanceException extends ProtocolException {

	public UnknownInstanceException(String host) {
		super(host);
	}

}
