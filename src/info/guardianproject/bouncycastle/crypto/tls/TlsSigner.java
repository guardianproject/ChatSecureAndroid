package info.guardianproject.bouncycastle.crypto.tls;

import info.guardianproject.bouncycastle.crypto.CryptoException;
import info.guardianproject.bouncycastle.crypto.Signer;
import info.guardianproject.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.security.SecureRandom;


interface TlsSigner
{
    byte[] calculateRawSignature(SecureRandom random, AsymmetricKeyParameter privateKey, byte[] md5andsha1)
        throws CryptoException;

    Signer createVerifyer(AsymmetricKeyParameter publicKey);

    boolean isValidPublicKey(AsymmetricKeyParameter publicKey);
}
