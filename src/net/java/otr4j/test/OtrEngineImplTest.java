package net.java.otr4j.test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;

public class OtrEngineImplTest extends junit.framework.TestCase {

	private SessionID aliceSessionID = new SessionID("Alice@Wonderland",
			"Bob@Wonderland", "Scytale");
	private SessionID bobSessionID = new SessionID("Bob@Wonderland",
			"Alice@Wonderland", "Scytale");

	private static Logger logger = Logger.getLogger(OtrEngineImplTest.class
			.getName());

	class DummyOtrEngineHost implements OtrEngineHost {

		public DummyOtrEngineHost(OtrPolicy policy) {
			this.policy = policy;
		}

		private OtrPolicy policy;
		public String lastInjectedMessage;

		public OtrPolicy getSessionPolicy(SessionID ctx) {
			return this.policy;
		}

		public void injectMessage(SessionID sessionID, String msg) {

			this.lastInjectedMessage = msg;
			String msgDisplay = (msg.length() > 10) ? msg.substring(0, 10)
					+ "..." : msg;
			logger.finest("IM injects message: " + msgDisplay);
		}

		public void showError(SessionID sessionID, String error) {
			logger.severe("IM shows error to user: " + error);
		}

		public void showWarning(SessionID sessionID, String warning) {
			logger.warning("IM shows warning to user: " + warning);
		}

		public void sessionStatusChanged(SessionID sessionID) {
			// don't care.
		}

		public KeyPair getKeyPair(SessionID paramSessionID) {
			KeyPairGenerator kg;
			try {
				kg = KeyPairGenerator.getInstance("DSA");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				return null;
			}
			return kg.genKeyPair();
		}

	}

	public void testSession() throws Exception {

		this.startSession();
		this.exchageMessages();
		this.endSession();
	}

	private OtrEngineImpl usAlice;
	private OtrEngineImpl usBob;
	private DummyOtrEngineHost host;

	private void startSession() throws OtrException {
		host = new DummyOtrEngineHost(new OtrPolicyImpl(OtrPolicy.ALLOW_V2
				| OtrPolicy.ERROR_START_AKE));

		usAlice = new OtrEngineImpl(host);
		usBob = new OtrEngineImpl(host);

		usAlice.startSession(aliceSessionID);

		// Bob receives query, sends D-H commit.

		usBob.transformReceiving(bobSessionID, host.lastInjectedMessage);

		// Alice received D-H Commit, sends D-H key.
		usAlice
				.transformReceiving(aliceSessionID,
						host.lastInjectedMessage);

		// Bob receives D-H Key, sends reveal signature.
		usBob.transformReceiving(bobSessionID, host.lastInjectedMessage);

		// Alice receives Reveal Signature, sends signature and goes secure.
		usAlice
				.transformReceiving(aliceSessionID,
						host.lastInjectedMessage);

		// Bobs receives Signature, goes secure.
		usBob.transformReceiving(bobSessionID, host.lastInjectedMessage);

		if (usBob.getSessionStatus(bobSessionID) != SessionStatus.ENCRYPTED
				|| usAlice.getSessionStatus(aliceSessionID) != SessionStatus.ENCRYPTED)
			fail("Could not establish a secure session.");
	}

	private void exchageMessages() throws OtrException {
		// We are both secure, send encrypted message.
		String clearTextMessage = "Hello Bob, this new IM software you installed on my PC the other day says we are talking Off-the-Record, what is that supposed to mean?";
		String sentMessage = usAlice.transformSending(aliceSessionID,
				clearTextMessage);

		// Receive encrypted message.
		String receivedMessage = usBob.transformReceiving(bobSessionID,
				sentMessage);

		if (!clearTextMessage.equals(receivedMessage))
			fail();

		// Send encrypted message.
		clearTextMessage = "Hey Alice, it means that our communication is encrypted and authenticated.";
		sentMessage = usBob.transformSending(bobSessionID, clearTextMessage);

		// Receive encrypted message.
		receivedMessage = usAlice.transformReceiving(aliceSessionID,
				sentMessage);
		if (!clearTextMessage.equals(receivedMessage))
			fail();

		// Send encrypted message.
		clearTextMessage = "Oh, is that all?";
		sentMessage = usAlice
				.transformSending(aliceSessionID, clearTextMessage);

		// Receive encrypted message.
		receivedMessage = usBob.transformReceiving(bobSessionID, sentMessage);
		if (!clearTextMessage.equals(receivedMessage))
			fail();

		// Send encrypted message.
		clearTextMessage = "Actually no, our communication has the properties of perfect forward secrecy and deniable authentication.";
		sentMessage = usBob.transformSending(bobSessionID, clearTextMessage);

		// Receive encrypted message.
		receivedMessage = usAlice.transformReceiving(aliceSessionID,
				sentMessage);
		if (!clearTextMessage.equals(receivedMessage))
			fail();

		// Send encrypted message. Test UTF-8 space characters.
		clearTextMessage = "Oh really?! pouvons-nous parler en français?";
		sentMessage = usAlice
				.transformSending(aliceSessionID, clearTextMessage);

		// Receive encrypted message.
		receivedMessage = usBob.transformReceiving(bobSessionID, sentMessage);
		if (!clearTextMessage.equals(receivedMessage))
			fail();
		
	}

	private void endSession() throws OtrException {
		usBob.endSession(bobSessionID);
		usAlice.endSession(aliceSessionID);

		if (usBob.getSessionStatus(bobSessionID) != SessionStatus.PLAINTEXT
				|| usAlice.getSessionStatus(aliceSessionID) != SessionStatus.PLAINTEXT)
			fail("Failed to end session.");
	}
}
