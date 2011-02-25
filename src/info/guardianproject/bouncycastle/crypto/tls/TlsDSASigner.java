package info.guardianproject.bouncycastle.crypto.tls;

import info.guardianproject.bouncycastle.crypto.CryptoException;
import info.guardianproject.bouncycastle.crypto.DSA;
import info.guardianproject.bouncycastle.crypto.Signer;
import info.guardianproject.bouncycastle.crypto.digests.NullDigest;
import info.guardianproject.bouncycastle.crypto.digests.SHA1Digest;
import info.guardianproject.bouncycastle.crypto.params.AsymmetricKeyParameter;
import info.guardianproject.bouncycastle.crypto.params.ParametersWithRandom;
import info.guardianproject.bouncycastle.crypto.signers.DSADigestSigner;

import java.security.SecureRandom;


abstract class TlsDSASigner implements TlsSigner
{
    public byte[] calculateRawSignature(SecureRandom secureRandom, AsymmetricKeyParameter privateKey, byte[] md5andsha1)
        throws CryptoException
    {
        // Note: Only use the SHA1 part of the hash
        Signer signer = new DSADigestSigner(createDSAImpl(), new NullDigest());
        signer.init(true, new ParametersWithRandom(privateKey, secureRandom));
        signer.update(md5andsha1, 16, 20);
        return signer.generateSignature();
    }

    public Signer createVerifyer(AsymmetricKeyParameter publicKey)
    {
        Signer verifyer = new DSADigestSigner(createDSAImpl(), new SHA1Digest());
        verifyer.init(false, publicKey);
        return verifyer;
    }

    protected abstract DSA createDSAImpl();
}
