package net.java.otr4j.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.interfaces.DHPublicKey;

import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.io.messages.SignatureX;

public class OtrInputStream extends FilterInputStream implements
		SerializationConstants {

	public OtrInputStream(InputStream in) {
		super(in);
	}

	private int readNumber(int length) throws IOException {
		byte[] b = new byte[length];
		read(b);

		int value = 0;
		for (int i = 0; i < b.length; i++) {
			int shift = (b.length - 1 - i) * 8;
			value += (b[i] & 0x000000FF) << shift;
		}

		return value;
	}

	public int readByte() throws IOException {
		return readNumber(TYPE_LEN_BYTE);
	}

	public int readInt() throws IOException {
		return readNumber(TYPE_LEN_INT);
	}

	public int readShort() throws IOException {
		return readNumber(TYPE_LEN_SHORT);
	}

	public byte[] readCtr() throws IOException {
		byte[] b = new byte[TYPE_LEN_CTR];
		read(b);
		return b;
	}

	public byte[] readMac() throws IOException {
		byte[] b = new byte[TYPE_LEN_MAC];
		read(b);
		return b;
	}

	public BigInteger readBigInt() throws IOException {
		byte[] b = readData();
		return new BigInteger(1, b);
	}

	public byte[] readData() throws IOException {
		int dataLen = readNumber(DATA_LEN);
		byte[] b = new byte[dataLen];
		read(b);
		return b;
	}

	public PublicKey readPublicKey() throws IOException {
		int type = readShort();
		switch (type) {
		case 0:
			BigInteger p = readBigInt();
			BigInteger q = readBigInt();
			BigInteger g = readBigInt();
			BigInteger y = readBigInt();
			DSAPublicKeySpec keySpec = new DSAPublicKeySpec(y, p, q, g);
			KeyFactory keyFactory;
			try {
				keyFactory = KeyFactory.getInstance("DSA");
			} catch (NoSuchAlgorithmException e) {
				throw new IOException();
			}
			try {
				return keyFactory.generatePublic(keySpec);
			} catch (InvalidKeySpecException e) {
				throw new IOException();
			}
		default:
			throw new UnsupportedOperationException();
		}
	}

	public DHPublicKey readDHPublicKey() throws IOException {
		BigInteger gyMpi = readBigInt();
		try {
			return new OtrCryptoEngineImpl().getDHPublicKey(gyMpi);
		} catch (Exception ex) {
			throw new IOException();
		}
	}

	public byte[] readTlvData() throws IOException {
		int len = readNumber(TYPE_LEN_SHORT);

		byte[] b = new byte[len];
		in.read(b);
		return b;
	}

	public byte[] readSignature(PublicKey pubKey) throws IOException {
		if (!pubKey.getAlgorithm().equals("DSA"))
			throw new UnsupportedOperationException();

		DSAPublicKey dsaPubKey = (DSAPublicKey) pubKey;
		DSAParams dsaParams = dsaPubKey.getParams();
		byte[] sig = new byte[dsaParams.getQ().bitLength() / 4];
		read(sig);
		return sig;
	}

	public SignatureX readMysteriousX() throws IOException {
		PublicKey pubKey = readPublicKey();
		int dhKeyID = readInt();
		byte[] sig = readSignature(pubKey);
		return new SignatureX(pubKey, dhKeyID, sig);
	}
}
