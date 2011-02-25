package info.guardianproject.bouncycastle.crypto.tls;

import info.guardianproject.bouncycastle.crypto.CryptoException;
import info.guardianproject.bouncycastle.crypto.Signer;
import info.guardianproject.bouncycastle.crypto.digests.NullDigest;
import info.guardianproject.bouncycastle.crypto.encodings.PKCS1Encoding;
import info.guardianproject.bouncycastle.crypto.engines.RSABlindedEngine;
import info.guardianproject.bouncycastle.crypto.params.AsymmetricKeyParameter;
import info.guardianproject.bouncycastle.crypto.params.ParametersWithRandom;
import info.guardianproject.bouncycastle.crypto.params.RSAKeyParameters;
import info.guardianproject.bouncycastle.crypto.signers.GenericSigner;

import java.security.SecureRandom;


class TlsRSASigner implements TlsSigner
{
    public byte[] calculateRawSignature(SecureRandom random, AsymmetricKeyParameter privateKey, byte[] md5andsha1)
        throws CryptoException
    {
        Signer sig = new GenericSigner(new PKCS1Encoding(new RSABlindedEngine()), new NullDigest());
        sig.init(true, new ParametersWithRandom(privateKey, random));
        sig.update(md5andsha1, 0, md5andsha1.length);
        return sig.generateSignature();
    }

    public Signer createVerifyer(AsymmetricKeyParameter publicKey)
    {
        Signer s = new GenericSigner(new PKCS1Encoding(new RSABlindedEngine()), new CombinedHash());
        s.init(false, publicKey);
        return s;
    }

    public boolean isValidPublicKey(AsymmetricKeyParameter publicKey)
    {
        return publicKey instanceof RSAKeyParameters && !publicKey.isPrivate();
    }
}
