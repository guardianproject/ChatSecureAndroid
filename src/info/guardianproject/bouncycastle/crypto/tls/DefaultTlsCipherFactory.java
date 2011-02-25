package info.guardianproject.bouncycastle.crypto.tls;

import info.guardianproject.bouncycastle.crypto.BlockCipher;
import info.guardianproject.bouncycastle.crypto.Digest;
import info.guardianproject.bouncycastle.crypto.digests.MD5Digest;
import info.guardianproject.bouncycastle.crypto.digests.SHA1Digest;
import info.guardianproject.bouncycastle.crypto.digests.SHA256Digest;
import info.guardianproject.bouncycastle.crypto.digests.SHA384Digest;
import info.guardianproject.bouncycastle.crypto.engines.AESFastEngine;
import info.guardianproject.bouncycastle.crypto.engines.DESedeEngine;
import info.guardianproject.bouncycastle.crypto.modes.CBCBlockCipher;

import java.io.IOException;


public class DefaultTlsCipherFactory implements TlsCipherFactory
{
    public TlsCipher createCipher(TlsClientContext context, int encryptionAlgorithm, int digestAlgorithm) throws IOException
    {
        switch (encryptionAlgorithm)
        {
            case EncryptionAlgorithm._3DES_EDE_CBC:
                return createDESedeCipher(context, 24, digestAlgorithm);
            case EncryptionAlgorithm.AES_128_CBC:
                return createAESCipher(context, 16, digestAlgorithm);
            case EncryptionAlgorithm.AES_256_CBC:
                return createAESCipher(context, 32, digestAlgorithm);
            default:
                throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }

    protected TlsCipher createAESCipher(TlsClientContext context, int cipherKeySize, int digestAlgorithm) throws IOException
    {
        return new TlsBlockCipher(context, createAESBlockCipher(),
            createAESBlockCipher(), createDigest(digestAlgorithm), createDigest(digestAlgorithm), cipherKeySize);
    }

    protected TlsCipher createDESedeCipher(TlsClientContext context, int cipherKeySize, int digestAlgorithm) throws IOException
    {
        return new TlsBlockCipher(context, createDESedeBlockCipher(),
            createDESedeBlockCipher(), createDigest(digestAlgorithm), createDigest(digestAlgorithm), cipherKeySize);
    }

    protected BlockCipher createAESBlockCipher()
    {
        return new CBCBlockCipher(new AESFastEngine());
    }

    protected BlockCipher createDESedeBlockCipher()
    {
        return new CBCBlockCipher(new DESedeEngine());
    }

    protected Digest createDigest(int digestAlgorithm) throws IOException
    {
        switch (digestAlgorithm)
        {
            case DigestAlgorithm.MD5:
                return new MD5Digest();
            case DigestAlgorithm.SHA:
                return new SHA1Digest();
            case DigestAlgorithm.SHA256:
                return new SHA256Digest();
            case DigestAlgorithm.SHA384:
                return new SHA384Digest();
            default:
                throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }
}
