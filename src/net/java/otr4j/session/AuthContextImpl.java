/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.otr4j.session;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import javax.crypto.interfaces.DHPublicKey;

import net.java.otr4j.OtrException;
import net.java.otr4j.crypto.OtrCryptoEngine;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.io.SerializationUtils;
import net.java.otr4j.io.messages.DHCommitMessage;
import net.java.otr4j.io.messages.DHKeyMessage;
import net.java.otr4j.io.messages.AbstractEncodedMessage;
import net.java.otr4j.io.messages.AbstractMessage;
import net.java.otr4j.io.messages.SignatureM;
import net.java.otr4j.io.messages.SignatureX;
import net.java.otr4j.io.messages.QueryMessage;
import net.java.otr4j.io.messages.RevealSignatureMessage;
import net.java.otr4j.io.messages.SignatureMessage;

/**
 * 
 * @author George Politis
 */
class AuthContextImpl implements AuthContext {

	public AuthContextImpl(Session session) {
		this.setSession(session);
		this.reset();
	}

	private Session session;

	private int authenticationState;
	private byte[] r;

	private DHPublicKey remoteDHPublicKey;
	private byte[] remoteDHPublicKeyEncrypted;
	private byte[] remoteDHPublicKeyHash;

	private KeyPair localDHKeyPair;
	private int localDHPrivateKeyID;
	private byte[] localDHPublicKeyBytes;
	private byte[] localDHPublicKeyHash;
	private byte[] localDHPublicKeyEncrypted;

	private BigInteger s;
	private byte[] c;
	private byte[] m1;
	private byte[] m2;
	private byte[] cp;
	private byte[] m1p;
	private byte[] m2p;

	private KeyPair localLongTermKeyPair;
	private Boolean isSecure = false;
	private int protocolVersion;

	private int getProtocolVersion() {
		return this.protocolVersion;
	}

	private void setProtocolVersion(int protoVersion) {
		this.protocolVersion = protoVersion;
	}

	private static Logger logger = Logger.getLogger(AuthContextImpl.class
			.getName());

	class MessageFactory {

		private QueryMessage getQueryMessage() {
			Vector<Integer> versions = new Vector<Integer>();
			versions.add(2);
			return new QueryMessage(versions);
		}

		private DHCommitMessage getDHCommitMessage() throws OtrException {
			return new DHCommitMessage(getProtocolVersion(),
					getLocalDHPublicKeyHash(), getLocalDHPublicKeyEncrypted());
		}

		private DHKeyMessage getDHKeyMessage() throws OtrException {
			return new DHKeyMessage(getProtocolVersion(),
					(DHPublicKey) getLocalDHKeyPair().getPublic());
		}

		private RevealSignatureMessage getRevealSignatureMessage()
				throws OtrException {
			try {
				SignatureM m = new SignatureM((DHPublicKey) getLocalDHKeyPair()
						.getPublic(), getRemoteDHPublicKey(),
						getLocalLongTermKeyPair().getPublic(),
						getLocalDHKeyPairID());

				OtrCryptoEngine otrCryptoEngine = new OtrCryptoEngineImpl();
				byte[] mhash = otrCryptoEngine.sha256Hmac(SerializationUtils
						.toByteArray(m), getM1());
				byte[] signature = otrCryptoEngine.sign(mhash,
						getLocalLongTermKeyPair().getPrivate());

				SignatureX mysteriousX = new SignatureX(
						getLocalLongTermKeyPair().getPublic(),
						getLocalDHKeyPairID(), signature);
				byte[] xEncrypted = otrCryptoEngine.aesEncrypt(getC(), null,
						SerializationUtils.toByteArray(mysteriousX));

				byte[] tmp = SerializationUtils.writeData(xEncrypted);

				byte[] xEncryptedHash = otrCryptoEngine.sha256Hmac160(tmp,
						getM2());
				return new RevealSignatureMessage(getProtocolVersion(),
						xEncrypted, xEncryptedHash, getR());
			} catch (IOException e) {
				throw new OtrException(e);
			}
		}

		private SignatureMessage getSignatureMessage() throws OtrException {
			SignatureM m = new SignatureM((DHPublicKey) getLocalDHKeyPair()
					.getPublic(), getRemoteDHPublicKey(),
					getLocalLongTermKeyPair().getPublic(),
					getLocalDHKeyPairID());

			OtrCryptoEngine otrCryptoEngine = new OtrCryptoEngineImpl();
			byte[] mhash;
			try {
				mhash = otrCryptoEngine.sha256Hmac(SerializationUtils
						.toByteArray(m), getM1p());
			} catch (IOException e) {
				throw new OtrException(e);
			}

			byte[] signature = otrCryptoEngine.sign(mhash,
					getLocalLongTermKeyPair().getPrivate());

			SignatureX mysteriousX = new SignatureX(getLocalLongTermKeyPair()
					.getPublic(), getLocalDHKeyPairID(), signature);

			byte[] xEncrypted;
			try {
				xEncrypted = otrCryptoEngine.aesEncrypt(getCp(), null,
						SerializationUtils.toByteArray(mysteriousX));
				byte[] tmp = SerializationUtils.writeData(xEncrypted);
				byte[] xEncryptedHash = otrCryptoEngine.sha256Hmac160(tmp,
						getM2p());
				return new SignatureMessage(getProtocolVersion(), xEncrypted,
						xEncryptedHash);
			} catch (IOException e) {
				throw new OtrException(e);
			}
		}
	}

	private MessageFactory messageFactory = new MessageFactory();

	public void reset() {
		logger.finest("Resetting authentication state.");
		authenticationState = AuthContext.NONE;
		r = null;

		remoteDHPublicKey = null;
		remoteDHPublicKeyEncrypted = null;
		remoteDHPublicKeyHash = null;

		localDHKeyPair = null;
		localDHPrivateKeyID = 1;
		localDHPublicKeyBytes = null;
		localDHPublicKeyHash = null;
		localDHPublicKeyEncrypted = null;

		s = null;
		c = m1 = m2 = cp = m1p = m2p = null;

		localLongTermKeyPair = null;
		protocolVersion = 0;
		setIsSecure(false);
	}

	private void setIsSecure(Boolean isSecure) {
		this.isSecure = isSecure;
	}

	public boolean getIsSecure() {
		return isSecure;
	}

	private void setAuthenticationState(int authenticationState) {
		this.authenticationState = authenticationState;
	}

	private int getAuthenticationState() {
		return authenticationState;
	}

	private byte[] getR() {
		if (r == null) {
			logger.finest("Picking random key r.");
			r = new byte[OtrCryptoEngine.AES_KEY_BYTE_LENGTH];
			new Random().nextBytes(r);
		}
		return r;
	}

	public void setRemoteDHPublicKey(DHPublicKey dhPublicKey) {
		// Verifies that Alice's gy is a legal value (2 <= gy <= modulus-2)
		if (dhPublicKey.getY().compareTo(OtrCryptoEngine.MODULUS_MINUS_TWO) > 0) {
			throw new IllegalArgumentException(
					"Illegal D-H Public Key value, Ignoring message.");
		} else if (dhPublicKey.getY().compareTo(OtrCryptoEngine.BIGINTEGER_TWO) < 0) {
			throw new IllegalArgumentException(
					"Illegal D-H Public Key value, Ignoring message.");
		}
		logger.finest("Received D-H Public Key is a legal value.");

		this.remoteDHPublicKey = dhPublicKey;
	}

	public DHPublicKey getRemoteDHPublicKey() {
		return remoteDHPublicKey;
	}

	private void setRemoteDHPublicKeyEncrypted(byte[] remoteDHPublicKeyEncrypted) {
		logger.finest("Storing encrypted remote public key.");
		this.remoteDHPublicKeyEncrypted = remoteDHPublicKeyEncrypted;
	}

	private byte[] getRemoteDHPublicKeyEncrypted() {
		return remoteDHPublicKeyEncrypted;
	}

	private void setRemoteDHPublicKeyHash(byte[] remoteDHPublicKeyHash) {
		logger.finest("Storing encrypted remote public key hash.");
		this.remoteDHPublicKeyHash = remoteDHPublicKeyHash;
	}

	private byte[] getRemoteDHPublicKeyHash() {
		return remoteDHPublicKeyHash;
	}

	public KeyPair getLocalDHKeyPair() throws OtrException {
		if (localDHKeyPair == null) {
			localDHKeyPair = new OtrCryptoEngineImpl().generateDHKeyPair();
			logger.finest("Generated local D-H key pair.");
		}
		return localDHKeyPair;
	}

	private int getLocalDHKeyPairID() {
		return localDHPrivateKeyID;
	}

	private byte[] getLocalDHPublicKeyHash() throws OtrException {
		if (localDHPublicKeyHash == null) {
			localDHPublicKeyHash = new OtrCryptoEngineImpl()
					.sha256Hash(getLocalDHPublicKeyBytes());
			logger.finest("Hashed local D-H public key.");
		}
		return localDHPublicKeyHash;
	}

	private byte[] getLocalDHPublicKeyEncrypted() throws OtrException {
		if (localDHPublicKeyEncrypted == null) {
			localDHPublicKeyEncrypted = new OtrCryptoEngineImpl().aesEncrypt(
					getR(), null, getLocalDHPublicKeyBytes());
			logger.finest("Encrypted our D-H public key.");
		}
		return localDHPublicKeyEncrypted;
	}

	public BigInteger getS() throws OtrException {
		if (s == null) {
			s = new OtrCryptoEngineImpl().generateSecret(this
					.getLocalDHKeyPair().getPrivate(), this
					.getRemoteDHPublicKey());
			logger.finest("Generated shared secret.");
		}
		return s;
	}

	private byte[] getC() throws OtrException {
		if (c != null)
			return c;

		byte[] h2 = h2(C_START);
		ByteBuffer buff = ByteBuffer.wrap(h2);
		this.c = new byte[OtrCryptoEngine.AES_KEY_BYTE_LENGTH];
		buff.get(this.c);
		logger.finest("Computed c.");
		return c;

	}

	private byte[] getM1() throws OtrException {
		if (m1 != null)
			return m1;

		byte[] h2 = h2(M1_START);
		ByteBuffer buff = ByteBuffer.wrap(h2);
		byte[] m1 = new byte[OtrCryptoEngine.SHA256_HMAC_KEY_BYTE_LENGTH];
		buff.get(m1);
		logger.finest("Computed m1.");
		this.m1 = m1;
		return m1;
	}

	private byte[] getM2() throws OtrException {
		if (m2 != null)
			return m2;

		byte[] h2 = h2(M2_START);
		ByteBuffer buff = ByteBuffer.wrap(h2);
		byte[] m2 = new byte[OtrCryptoEngine.SHA256_HMAC_KEY_BYTE_LENGTH];
		buff.get(m2);
		logger.finest("Computed m2.");
		this.m2 = m2;
		return m2;
	}

	private byte[] getCp() throws OtrException {
		if (cp != null)
			return cp;

		byte[] h2 = h2(C_START);
		ByteBuffer buff = ByteBuffer.wrap(h2);
		byte[] cp = new byte[OtrCryptoEngine.AES_KEY_BYTE_LENGTH];
		buff.position(OtrCryptoEngine.AES_KEY_BYTE_LENGTH);
		buff.get(cp);
		logger.finest("Computed c'.");
		this.cp = cp;
		return cp;
	}

	private byte[] getM1p() throws OtrException {
		if (m1p != null)
			return m1p;

		byte[] h2 = h2(M1p_START);
		ByteBuffer buff = ByteBuffer.wrap(h2);
		byte[] m1p = new byte[OtrCryptoEngine.SHA256_HMAC_KEY_BYTE_LENGTH];
		buff.get(m1p);
		this.m1p = m1p;
		logger.finest("Computed m1'.");
		return m1p;
	}

	private byte[] getM2p() throws OtrException {
		if (m2p != null)
			return m2p;

		byte[] h2 = h2(M2p_START);
		ByteBuffer buff = ByteBuffer.wrap(h2);
		byte[] m2p = new byte[OtrCryptoEngine.SHA256_HMAC_KEY_BYTE_LENGTH];
		buff.get(m2p);
		this.m2p = m2p;
		logger.finest("Computed m2'.");
		return m2p;
	}

	public KeyPair getLocalLongTermKeyPair() {
		if (localLongTermKeyPair == null) {
			localLongTermKeyPair = getSession().getLocalKeyPair();
		}
		return localLongTermKeyPair;
	}

	private byte[] h2(byte b) throws OtrException {
		byte[] secbytes;
		try {
			secbytes = SerializationUtils.writeMpi(getS());
		} catch (IOException e) {
			throw new OtrException(e);
		}

		int len = secbytes.length + 1;
		ByteBuffer buff = ByteBuffer.allocate(len);
		buff.put(b);
		buff.put(secbytes);
		byte[] sdata = buff.array();
		return new OtrCryptoEngineImpl().sha256Hash(sdata);
	}

	private byte[] getLocalDHPublicKeyBytes() throws OtrException {
		if (localDHPublicKeyBytes == null) {
			try {
				this.localDHPublicKeyBytes = SerializationUtils
						.writeMpi(((DHPublicKey) getLocalDHKeyPair()
								.getPublic()).getY());

			} catch (IOException e) {
				throw new OtrException(e);
			}

		}
		return localDHPublicKeyBytes;
	}

	public void handleReceivingMessage(AbstractMessage m) throws OtrException {

		switch (m.messageType) {
		case AbstractEncodedMessage.MESSAGE_DH_COMMIT:
			handleDHCommitMessage((DHCommitMessage) m);
			break;
		case AbstractEncodedMessage.MESSAGE_DHKEY:
			handleDHKeyMessage((DHKeyMessage) m);
			break;
		case AbstractEncodedMessage.MESSAGE_REVEALSIG:
			handleRevealSignatureMessage((RevealSignatureMessage) m);
			break;
		case AbstractEncodedMessage.MESSAGE_SIGNATURE:
			handleSignatureMessage((SignatureMessage) m);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	private void handleSignatureMessage(SignatureMessage m) throws OtrException {
		Session session = getSession();
		SessionID sessionID = session.getSessionID();
		logger.finest(sessionID.getAccountID()
				+ " received a signature message from " + sessionID.getUserID()
				+ " throught " + sessionID.getProtocolName() + ".");
		if (!session.getSessionPolicy().getAllowV2()) {
			logger.finest("Policy does not allow OTRv2, ignoring message.");
			return;
		}

		switch (this.getAuthenticationState()) {
		case AWAITING_SIG:
			// Verify MAC.
			if (!m.verify(this.getM2p())) {
				logger
						.finest("Signature MACs are not equal, ignoring message.");
				return;
			}

			// Decrypt X.
			byte[] remoteXDecrypted = m.decrypt(this.getCp());
			SignatureX remoteX;
			try {
				remoteX = SerializationUtils.toMysteriousX(remoteXDecrypted);
			} catch (IOException e) {
				throw new OtrException(e);
			}
			// Compute signature.
			PublicKey remoteLongTermPublicKey = remoteX.longTermPublicKey;
			SignatureM remoteM = new SignatureM(this.getRemoteDHPublicKey(),
					(DHPublicKey) this.getLocalDHKeyPair().getPublic(),
					remoteLongTermPublicKey, remoteX.dhKeyID);
			OtrCryptoEngine otrCryptoEngine = new OtrCryptoEngineImpl();
			// Verify signature.
			byte[] signature;
			try {
				signature = otrCryptoEngine.sha256Hmac(SerializationUtils
						.toByteArray(remoteM), this.getM1p());
			} catch (IOException e) {
				throw new OtrException(e);
			}
			if (!otrCryptoEngine.verify(signature, remoteLongTermPublicKey,
					remoteX.signature)) {
				logger.finest("Signature verification failed.");
				return;
			}

			this.setIsSecure(true);
			this.setRemoteLongTermPublicKey(remoteLongTermPublicKey);
			break;
		default:
			logger
					.finest("We were not expecting a signature, ignoring message.");
			return;
		}
	}

	private void handleRevealSignatureMessage(RevealSignatureMessage m)
			throws OtrException {
		Session session = getSession();
		SessionID sessionID = session.getSessionID();
		logger.finest(sessionID.getAccountID()
				+ " received a reveal signature message from "
				+ sessionID.getUserID() + " throught "
				+ sessionID.getProtocolName() + ".");

		if (!session.getSessionPolicy().getAllowV2()) {
			logger.finest("Policy does not allow OTRv2, ignoring message.");
			return;
		}

		switch (this.getAuthenticationState()) {
		case AWAITING_REVEALSIG:
			// Use the received value of r to decrypt the value of gx
			// received
			// in the D-H Commit Message, and verify the hash therein.
			// Decrypt
			// the encrypted signature, and verify the signature and the
			// MACs.
			// If everything checks out:

			// * Reply with a Signature Message.
			// * Transition authstate to AUTHSTATE_NONE.
			// * Transition msgstate to MSGSTATE_ENCRYPTED.
			// * TODO If there is a recent stored message, encrypt it and
			// send
			// it as a Data Message.

			OtrCryptoEngine otrCryptoEngine = new OtrCryptoEngineImpl();
			// Uses r to decrypt the value of gx sent earlier
			byte[] remoteDHPublicKeyDecrypted = otrCryptoEngine.aesDecrypt(
					m.revealedKey, null, this.getRemoteDHPublicKeyEncrypted());

			// Verifies that HASH(gx) matches the value sent earlier
			byte[] remoteDHPublicKeyHash = otrCryptoEngine
					.sha256Hash(remoteDHPublicKeyDecrypted);
			if (!Arrays.equals(remoteDHPublicKeyHash, this
					.getRemoteDHPublicKeyHash())) {
				logger.finest("Hashes don't match, ignoring message.");
				return;
			}

			// Verifies that Bob's gx is a legal value (2 <= gx <=
			// modulus-2)
			BigInteger remoteDHPublicKeyMpi;
			try {
				remoteDHPublicKeyMpi = SerializationUtils
						.readMpi(remoteDHPublicKeyDecrypted);
			} catch (IOException e) {
				throw new OtrException(e);
			}

			this.setRemoteDHPublicKey(otrCryptoEngine
					.getDHPublicKey(remoteDHPublicKeyMpi));

			// Verify received Data.
			if (!m.verify(this.getM2())) {
				logger
						.finest("Signature MACs are not equal, ignoring message.");
				return;
			}

			// Decrypt X.
			byte[] remoteXDecrypted = m.decrypt(this.getC());
			SignatureX remoteX;
			try {
				remoteX = SerializationUtils.toMysteriousX(remoteXDecrypted);
			} catch (IOException e) {
				throw new OtrException(e);
			}

			// Compute signature.
			PublicKey remoteLongTermPublicKey = remoteX.longTermPublicKey;
			SignatureM remoteM = new SignatureM(this.getRemoteDHPublicKey(),
					(DHPublicKey) this.getLocalDHKeyPair().getPublic(),
					remoteLongTermPublicKey, remoteX.dhKeyID);

			// Verify signature.
			byte[] signature;
			try {
				signature = otrCryptoEngine.sha256Hmac(SerializationUtils
						.toByteArray(remoteM), this.getM1());
			} catch (IOException e) {
				throw new OtrException(e);
			}

			if (!otrCryptoEngine.verify(signature, remoteLongTermPublicKey,
					remoteX.signature)) {
				logger.finest("Signature verification failed.");
				return;
			}

			logger.finest("Signature verification succeeded.");

			this.setAuthenticationState(AuthContext.NONE);
			this.setIsSecure(true);
			this.setRemoteLongTermPublicKey(remoteLongTermPublicKey);
			getSession().injectMessage(messageFactory.getSignatureMessage());
			break;
		default:
			logger.finest("Ignoring message.");
			break;
		}
	}

	private void handleDHKeyMessage(DHKeyMessage m) throws OtrException {
		Session session = getSession();
		SessionID sessionID = session.getSessionID();
		logger.finest(sessionID.getAccountID()
				+ " received a D-H key message from " + sessionID.getUserID()
				+ " throught " + sessionID.getProtocolName() + ".");

		if (!session.getSessionPolicy().getAllowV2()) {
			logger.finest("If ALLOW_V2 is not set, ignore this message.");
			return;
		}

		switch (this.getAuthenticationState()) {
		case AWAITING_DHKEY:
			// Reply with a Reveal Signature Message and transition
			// authstate to
			// AUTHSTATE_AWAITING_SIG
			this.setRemoteDHPublicKey(m.dhPublicKey);
			this.setAuthenticationState(AuthContext.AWAITING_SIG);
			getSession().injectMessage(
					messageFactory.getRevealSignatureMessage());
			logger.finest("Sent Reveal Signature.");
			break;
		case AWAITING_SIG:

			if (m.dhPublicKey.getY().equals(this.getRemoteDHPublicKey().getY())) {
				// If this D-H Key message is the same the one you received
				// earlier (when you entered AUTHSTATE_AWAITING_SIG):
				// Retransmit
				// your Reveal Signature Message.
				getSession().injectMessage(
						messageFactory.getRevealSignatureMessage());
				logger.finest("Resent Reveal Signature.");
			} else {
				// Otherwise: Ignore the message.
				logger.finest("Ignoring message.");
			}
			break;
		default:
			// Ignore the message
			break;
		}
	}

	private void handleDHCommitMessage(DHCommitMessage m) throws OtrException {
		Session session = getSession();
		SessionID sessionID = session.getSessionID();
		logger.finest(sessionID.getAccountID()
				+ " received a D-H commit message from "
				+ sessionID.getUserID() + " throught "
				+ sessionID.getProtocolName() + ".");

		if (!session.getSessionPolicy().getAllowV2()) {
			logger.finest("ALLOW_V2 is not set, ignore this message.");
			return;
		}

		switch (this.getAuthenticationState()) {
		case NONE:
			// Reply with a D-H Key Message, and transition authstate to
			// AUTHSTATE_AWAITING_REVEALSIG.
			this.reset();
			this.setProtocolVersion(2);
			this.setRemoteDHPublicKeyEncrypted(m.dhPublicKeyEncrypted);
			this.setRemoteDHPublicKeyHash(m.dhPublicKeyHash);
			this.setAuthenticationState(AuthContext.AWAITING_REVEALSIG);
			getSession().injectMessage(messageFactory.getDHKeyMessage());
			logger.finest("Sent D-H key.");
			break;

		case AWAITING_DHKEY:
			// This is the trickiest transition in the whole protocol. It
			// indicates that you have already sent a D-H Commit message to
			// your
			// correspondent, but that he either didn't receive it, or just
			// didn't receive it yet, and has sent you one as well. The
			// symmetry
			// will be broken by comparing the hashed gx you sent in your
			// D-H
			// Commit Message with the one you received, considered as
			// 32-byte
			// unsigned big-endian values.
			BigInteger ourHash = new BigInteger(1, this
					.getLocalDHPublicKeyHash());
			BigInteger theirHash = new BigInteger(1, m.dhPublicKeyHash);

			if (theirHash.compareTo(ourHash) == -1) {
				// Ignore the incoming D-H Commit message, but resend your
				// D-H
				// Commit message.
				getSession().injectMessage(messageFactory.getDHCommitMessage());
				logger
						.finest("Ignored the incoming D-H Commit message, but resent our D-H Commit message.");
			} else {
				// *Forget* your old gx value that you sent (encrypted)
				// earlier,
				// and pretend you're in AUTHSTATE_NONE; i.e. reply with a
				// D-H
				// Key Message, and transition authstate to
				// AUTHSTATE_AWAITING_REVEALSIG.
				this.reset();
				this.setProtocolVersion(2);
				this.setRemoteDHPublicKeyEncrypted(m.dhPublicKeyEncrypted);
				this.setRemoteDHPublicKeyHash(m.dhPublicKeyHash);
				this.setAuthenticationState(AuthContext.AWAITING_REVEALSIG);
				getSession().injectMessage(messageFactory.getDHKeyMessage());
				logger
						.finest("Forgot our old gx value that we sent (encrypted) earlier, and pretended we're in AUTHSTATE_NONE -> Sent D-H key.");
			}
			break;

		case AWAITING_REVEALSIG:
			// Retransmit your D-H Key Message (the same one as you sent
			// when
			// you entered AUTHSTATE_AWAITING_REVEALSIG). Forget the old D-H
			// Commit message, and use this new one instead.
			this.setRemoteDHPublicKeyEncrypted(m.dhPublicKeyEncrypted);
			this.setRemoteDHPublicKeyHash(m.dhPublicKeyHash);
			getSession().injectMessage(messageFactory.getDHKeyMessage());
			logger.finest("Sent D-H key.");
			break;
		case AWAITING_SIG:
			// Reply with a new D-H Key message, and transition authstate to
			// AUTHSTATE_AWAITING_REVEALSIG
			this.reset();
			this.setRemoteDHPublicKeyEncrypted(m.dhPublicKeyEncrypted);
			this.setRemoteDHPublicKeyHash(m.dhPublicKeyHash);
			this.setAuthenticationState(AuthContext.AWAITING_REVEALSIG);
			getSession().injectMessage(messageFactory.getDHKeyMessage());
			logger.finest("Sent D-H key.");
			break;
		case V1_SETUP:
			throw new UnsupportedOperationException();
		}
	}

	public void startV2Auth() throws OtrException {
		logger
				.finest("Starting Authenticated Key Exchange, sending query message");
		getSession().injectMessage(messageFactory.getQueryMessage());
	}

	public void respondV2Auth() throws OtrException {
		logger.finest("Responding to Query Message");
		this.reset();
		this.setProtocolVersion(2);
		this.setAuthenticationState(AuthContext.AWAITING_DHKEY);
		logger.finest("Sending D-H Commit.");
		getSession().injectMessage(messageFactory.getDHCommitMessage());
	}

	private void setSession(Session session) {
		this.session = session;
	}

	private Session getSession() {
		return session;
	}

	private PublicKey remoteLongTermPublicKey;

	public PublicKey getRemoteLongTermPublicKey() {
		return remoteLongTermPublicKey;
	}

	private void setRemoteLongTermPublicKey(PublicKey pubKey) {
		this.remoteLongTermPublicKey = pubKey;
	}
}
