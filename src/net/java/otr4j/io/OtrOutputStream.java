package net.java.otr4j.io;

import info.guardianproject.bouncycastle.util.BigIntegers;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;

import javax.crypto.interfaces.DHPublicKey;

import net.java.otr4j.io.messages.SignatureM;
import net.java.otr4j.io.messages.MysteriousT;
import net.java.otr4j.io.messages.SignatureX;


public class OtrOutputStream extends FilterOutputStream implements
		SerializationConstants {

	public OtrOutputStream(OutputStream out) {
		super(out);
	}

	private void writeNumber(int value, int length) throws IOException {
		byte[] b = new byte[length];
		for (int i = 0; i < length; i++) {
			int offset = (b.length - 1 - i) * 8;
			b[i] = (byte) ((value >>> offset) & 0xFF);
		}
		write(b);
	}

	public void writeBigInt(BigInteger bi) throws IOException {
		byte[] b = BigIntegers.asUnsignedByteArray(bi);
		writeData(b);
	}

	public void writeByte(int b) throws IOException {
		writeNumber(b, TYPE_LEN_BYTE);
	}

	public void writeData(byte[] b) throws IOException {
		int len = (b == null || b.length < 0) ? 0 : b.length;
		writeNumber(len, DATA_LEN);
		if (len > 0)
			write(b);
	}

	public void writeInt(int i) throws IOException {
		writeNumber(i, TYPE_LEN_INT);

	}

	public void writeShort(int s) throws IOException {
		writeNumber(s, TYPE_LEN_SHORT);

	}

	public void writeMac(byte[] mac) throws IOException {
		if (mac == null || mac.length != TYPE_LEN_MAC)
			throw new IllegalArgumentException();

		write(mac);
	}

	public void writeCtr(byte[] ctr) throws IOException {
		if (ctr == null || ctr.length < 1)
			return;

		int i = 0;
		while (i < TYPE_LEN_CTR && i < ctr.length) {
			write(ctr[i]);
			i++;
		}
	}

	public void writeDHPublicKey(DHPublicKey dhPublicKey) throws IOException {
		byte[] b = BigIntegers.asUnsignedByteArray(dhPublicKey.getY());
		writeData(b);
	}

	public void writePublicKey(PublicKey pubKey) throws IOException {
		if (!(pubKey instanceof DSAPublicKey))
			throw new UnsupportedOperationException(
					"Key types other than DSA are not supported at the moment.");

		DSAPublicKey dsaKey = (DSAPublicKey) pubKey;

		writeShort(0);

		DSAParams dsaParams = dsaKey.getParams();
		writeBigInt(dsaParams.getP());
		writeBigInt(dsaParams.getQ());
		writeBigInt(dsaParams.getG());
		writeBigInt(dsaKey.getY());

	}

	public void writeTlvData(byte[] b) throws IOException {
		int len = (b == null || b.length < 0) ? 0 : b.length;
		writeNumber(len, TLV_LEN);
		if (len > 0)
			write(b);
	}

	public void writeSignature(byte[] signature, PublicKey pubKey)
			throws IOException {
		if (!pubKey.getAlgorithm().equals("DSA"))
			throw new UnsupportedOperationException();
		out.write(signature);
	}

	public void writeMysteriousX(SignatureX x) throws IOException {
		writePublicKey(x.longTermPublicKey);
		writeInt(x.dhKeyID);
		writeSignature(x.signature, x.longTermPublicKey);
	}

	public void writeMysteriousX(SignatureM m) throws IOException {
		writeBigInt(m.localPubKey.getY());
		writeBigInt(m.remotePubKey.getY());
		writePublicKey(m.localLongTermPubKey);
		writeInt(m.keyPairID);
	}

	public void writeMysteriousT(MysteriousT t) throws IOException {
		writeShort(t.protocolVersion);
		writeByte(t.messageType);
		writeByte(t.flags);

		writeInt(t.senderKeyID);
		writeInt(t.recipientKeyID);
		writeDHPublicKey(t.nextDH);
		writeCtr(t.ctr);
		writeData(t.encryptedMessage);

	}
}
