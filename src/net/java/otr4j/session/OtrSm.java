package net.java.otr4j.session;

import info.guardianproject.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrKeyManager;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrTlvHandler;
import net.java.otr4j.crypto.SM;
import net.java.otr4j.crypto.SM.SMException;
import net.java.otr4j.crypto.SM.SMState;
import net.java.otr4j.io.OtrOutputStream;

public class OtrSm implements OtrTlvHandler {
    public static interface OtrSmEngineHost extends OtrEngineHost {
        void askForSecret(SessionID sessionID, String question);
    }
    
	SMState smstate;
	private SessionID sessionID;
    private OtrKeyManager keyManager;
    private OtrSmEngineHost engineHost;
	private Session session;
	private List<TLV> pendingTlvs;

	/**
	 * Construct an OTR Socialist Millionaire handler object.
	 * 
	 * @param authContext The encryption context for encrypting the session.
	 * @param keyManager The long-term key manager.
	 * @param sessionId The session ID.
	 * @param engineHost The host where we can present messages or ask for the shared secret.
	 */
	public OtrSm(Session session, OtrKeyManager keyManager, SessionID sessionId,
	        OtrSmEngineHost engineHost) {
		smstate = new SMState();
		this.session = session;
		this.sessionID = sessionId;
		this.keyManager = keyManager;
		this.engineHost = engineHost;
	}

	/* Compute secret session ID as hash of agreed secret */
	private static byte[] computeSessionId(BigInteger s) throws SMException {
		byte[] sdata;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OtrOutputStream oos = new OtrOutputStream(out);
			oos.write(0x00);
			oos.writeBigInt(s);
			sdata = out.toByteArray();
			oos.close();
		} catch (IOException e1) {
			throw new SMException(e1);
		}

		/* Calculate the session id */
		MessageDigest sha256;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new SMException("cannot find SHA-256");
		}
		byte[] res = sha256.digest(sdata);
		byte[] secure_session_id = new byte[8];
		System.arraycopy(res, 0, secure_session_id, 0, 8);
		return secure_session_id;

	}

	/**
	 *  Respond to or initiate an SMP negotiation
	 *  
	 *  @param question The question to present to the peer, if initiating.  May be null for no question.
	 *  @param secret The secret.
	 *  @param initiating Whether we are initiating or responding to an initial request.
	 *  
	 *  @return TLVs to send to the peer
	 */
	public List<TLV> initRespondSmp(String question, String secret, boolean initiating) throws OtrException {
		if (question != null && !initiating)
			throw new IllegalArgumentException("Only supply a question if initiating");

		/*
		 * Construct the combined secret as a SHA256 hash of:
		 * Version byte (0x01), Initiator fingerprint (20 bytes),
		 * responder fingerprint (20 bytes), secure session id, input secret
		 */
		byte[] our_fp = Hex.decode(keyManager.getLocalFingerprint(sessionID));
		byte[] their_fp = Hex.decode(keyManager.getRemoteFingerprint(sessionID));

		byte[] sessionId;
		try {
			sessionId = computeSessionId(session.getS());
		} catch (SMException ex) {
			throw new OtrException(ex);
		}

		int combined_buf_len = 41 + sessionId.length + secret.length();
		byte[] combined_buf = new byte[combined_buf_len];
		combined_buf[0]=1;
		if (initiating){
			System.arraycopy(our_fp, 0, combined_buf, 1, 20);
			System.arraycopy(their_fp, 0, combined_buf, 21, 20);
		} else {
			System.arraycopy(their_fp, 0, combined_buf, 1, 20);
			System.arraycopy(our_fp, 0, combined_buf, 21, 20);
		}
		System.arraycopy(sessionId, 0, combined_buf, 41, sessionId.length);
		System.arraycopy(secret.getBytes(), 0, 
				combined_buf, 41 + sessionId.length, secret.length());

		MessageDigest sha256;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {
			throw new OtrException(ex);
		}

		byte[] combined_secret = sha256.digest(combined_buf);
		byte[] smpmsg;
		try {
			if (initiating) {
				smpmsg = SM.step1(smstate, combined_secret);
			} else {
				smpmsg = SM.step2b(smstate, combined_secret);
			}
		} catch (SMException ex) {
			throw new OtrException(ex);
		}

		// If we've got a question, attach it to the smpmsg 
		if (question != null){
			byte[] qsmpmsg = new byte[question.length() + 1 + smpmsg.length];
			System.arraycopy(question.getBytes(), 0, qsmpmsg, 0, question.length());
			System.arraycopy(smpmsg, 0, qsmpmsg, question.length() + 1, smpmsg.length);
			smpmsg = qsmpmsg;
		}

		TLV sendtlv = new TLV(initiating? 
				(question != null ? TLV.SMP1Q:TLV.SMP1) : TLV.SMP2, smpmsg);
		smstate.nextExpected = initiating? SM.EXPECT2 : SM.EXPECT3;
        return makeTlvList(sendtlv);
	}

	/**
	 *  Create an abort TLV and reset our state.
	 *  
	 *  @return TLVs to send to the peer
	 */
	public List<TLV> abortSmp() throws OtrException {
		TLV sendtlv = new TLV(TLV.SMP_ABORT, new byte[0]);
		smstate.nextExpected = SM.EXPECT1;
        return makeTlvList(sendtlv);
	}

	public List<TLV> getPendingTlvs() {
		return pendingTlvs;
	}
	/** Process an incoming TLV and optionally send back TLVs to peer. */
	public void processTlv(TLV tlv) throws OtrException {
		try {
			pendingTlvs = doProcessTlv(tlv);
		} catch (SMException ex) {
			throw new OtrException(ex);
		}
	}
	
	private List<TLV> doProcessTlv(TLV tlv) throws SMException {
		/* If TLVs contain SMP data, process it */
		int nextMsg = smstate.nextExpected;

		int tlvType = tlv.getType();

		if (tlvType == TLV.SMP1Q && nextMsg == SM.EXPECT1) {
			/* We can only do the verification half now.
			 * We must wait for the secret to be entered
			 * to continue. */
			byte[] question = tlv.getValue();
			int qlen=0;
			for(; qlen!=question.length && question[qlen]!=0; qlen++){
			}
			if (qlen == question.length) qlen=0;
			else qlen++;
			byte[] input = new byte[question.length-qlen];
			System.arraycopy(question, qlen, input, 0, question.length-qlen);
			SM.step2a(smstate, input, 1);
			if (qlen != 0) qlen--;
			byte[] plainq = new byte[qlen];
			System.arraycopy(question, 0, plainq, 0, qlen);
			if (smstate.smProgState != SM.PROG_CHEATED){
			    engineHost.askForSecret(sessionID, new String(plainq));
			} else {
			    engineHost.showError(sessionID, "Peer attempted to cheat during verification");
				smstate.nextExpected = SM.EXPECT1;
				smstate.smProgState = SM.PROG_OK;
			}
		} else if (tlvType == TLV.SMP1Q) {
            engineHost.showError(sessionID, "Error during verification (step 1q)");
		} else if (tlvType == TLV.SMP1 && nextMsg == SM.EXPECT1) {
			/* We can only do the verification half now.
			 * We must wait for the secret to be entered
			 * to continue. */
			SM.step2a(smstate, tlv.getValue(), 0);
			if (smstate.smProgState!=SM.PROG_CHEATED) {
                engineHost.askForSecret(sessionID, null);
			} else {
                engineHost.showError(sessionID, "Peer attempted to cheat during verification");
				smstate.nextExpected = SM.EXPECT1;
				smstate.smProgState = SM.PROG_OK;
			}
		} else if (tlvType == TLV.SMP1) {
            engineHost.showError(sessionID, "Error during verification (step 1)");
		} else if (tlvType == TLV.SMP2 && nextMsg == SM.EXPECT2) {
			byte[] nextmsg = SM.step3(smstate, tlv.getValue());
			if (smstate.smProgState != SM.PROG_CHEATED){
				/* Send msg with next smp msg content */
				TLV sendtlv = new TLV(TLV.SMP3, nextmsg);
				smstate.nextExpected = SM.EXPECT4;
				return makeTlvList(sendtlv);
			} else {
                engineHost.showError(sessionID, "Peer attempted to cheat during verification");
				smstate.nextExpected = SM.EXPECT1;
				smstate.smProgState = SM.PROG_OK;
			}
		} else if (tlvType == TLV.SMP2){
            engineHost.showError(sessionID, "Error during verification (step 2)");
		} else if (tlvType == TLV.SMP3 && nextMsg == SM.EXPECT3) {
			byte[] nextmsg = SM.step4(smstate, tlv.getValue());
			/* Set trust level based on result */
			if (smstate.smProgState == SM.PROG_SUCCEEDED){
				keyManager.verify(sessionID);
			} else {
				keyManager.unverify(sessionID);
			}
			if (smstate.smProgState != SM.PROG_CHEATED){
				/* Send msg with next smp msg content */
				TLV sendtlv = new TLV(TLV.SMP4, nextmsg);
				smstate.nextExpected = SM.EXPECT1;
                return makeTlvList(sendtlv);
			} else {
                engineHost.showError(sessionID, "Peer attempted to cheat during verification");
				smstate.nextExpected = SM.EXPECT1;
				smstate.smProgState = SM.PROG_OK;
			}
		} else if (tlvType == TLV.SMP3){
            engineHost.showError(sessionID, "Error during verification (step 3)");
		} else if (tlvType == TLV.SMP4 && nextMsg == SM.EXPECT4) {

			SM.step5(smstate, tlv.getValue());
			if (smstate.smProgState == SM.PROG_SUCCEEDED){
				keyManager.verify(sessionID);
			} else {
				keyManager.unverify(sessionID);
			}
			if (smstate.smProgState != SM.PROG_CHEATED){
				smstate.nextExpected = SM.EXPECT1;
			} else {
                engineHost.showError(sessionID, "Peer attempted to cheat during verification");
				smstate.nextExpected = SM.EXPECT1;
				smstate.smProgState = SM.PROG_OK;
			}

		} else if (tlvType == TLV.SMP4){
            engineHost.showError(sessionID, "Error during verification (step 4)");
		} else if (tlvType == TLV.SMP_ABORT){
			smstate.nextExpected = SM.EXPECT1;
		}

		// Nothing to send
		return null;
	}

    private List<TLV> makeTlvList(TLV sendtlv) {
        List<TLV> tlvs = new ArrayList<TLV>(1);
        tlvs.add(sendtlv);
        return tlvs;
    }
}
