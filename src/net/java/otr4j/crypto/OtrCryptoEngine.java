/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.otr4j.crypto;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.interfaces.DHPublicKey;

/**
 * 
 * @author George Politis
 * 
 */
public interface OtrCryptoEngine {

	public static final String MODULUS_TEXT = "00FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF";
	public static final BigInteger MODULUS = new BigInteger(MODULUS_TEXT, 16);
	public static final BigInteger BIGINTEGER_TWO = BigInteger.valueOf(2);
	public static final BigInteger MODULUS_MINUS_TWO = MODULUS
			.subtract(BIGINTEGER_TWO);

	public static String GENERATOR_TEXT = "2";
	public static BigInteger GENERATOR = new BigInteger(GENERATOR_TEXT, 10);

	public static final int AES_KEY_BYTE_LENGTH = 16;
	public static final int SHA256_HMAC_KEY_BYTE_LENGTH = 32;
	public static final int DH_PRIVATE_KEY_MINIMUM_BIT_LENGTH = 320;
	public static final byte[] ZERO_CTR = new byte[] { 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00 };

	public static final int DSA_PUB_TYPE = 0;

	public abstract KeyPair generateDHKeyPair() throws OtrCryptoException;

	public abstract DHPublicKey getDHPublicKey(byte[] mpiBytes)
			throws OtrCryptoException;

	public abstract DHPublicKey getDHPublicKey(BigInteger mpi)
			throws OtrCryptoException;

	public abstract byte[] sha256Hmac(byte[] b, byte[] key)
			throws OtrCryptoException;

	public abstract byte[] sha256Hmac(byte[] b, byte[] key, int length)
			throws OtrCryptoException;

	public abstract byte[] sha1Hmac(byte[] b, byte[] key, int length)
			throws OtrCryptoException;

	public abstract byte[] sha256Hmac160(byte[] b, byte[] key)
			throws OtrCryptoException;

	public abstract byte[] sha256Hash(byte[] b) throws OtrCryptoException;

	public abstract byte[] sha1Hash(byte[] b) throws OtrCryptoException;

	public abstract byte[] aesDecrypt(byte[] key, byte[] ctr, byte[] b)
			throws OtrCryptoException;

	public abstract byte[] aesEncrypt(byte[] key, byte[] ctr, byte[] b)
			throws OtrCryptoException;

	public abstract BigInteger generateSecret(PrivateKey privKey,
			PublicKey pubKey) throws OtrCryptoException;

	public abstract byte[] sign(byte[] b, PrivateKey privatekey)
			throws OtrCryptoException;

	public abstract boolean verify(byte[] b, PublicKey pubKey, byte[] rs)
			throws OtrCryptoException;

	public abstract String getFingerprint(PublicKey pubKey)
			throws OtrCryptoException;

	public abstract byte[] getFingerprintRaw(PublicKey pubKey)
			throws OtrCryptoException;
}
