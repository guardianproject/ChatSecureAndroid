package info.guardianproject.bouncycastle.crypto.tls;

import info.guardianproject.bouncycastle.crypto.DSA;
import info.guardianproject.bouncycastle.crypto.params.AsymmetricKeyParameter;
import info.guardianproject.bouncycastle.crypto.params.ECPublicKeyParameters;
import info.guardianproject.bouncycastle.crypto.signers.ECDSASigner;

class TlsECDSASigner extends TlsDSASigner
{
    public boolean isValidPublicKey(AsymmetricKeyParameter publicKey)
    {
        return publicKey instanceof ECPublicKeyParameters;
    }

    protected DSA createDSAImpl()
    {
        return new ECDSASigner();
    }
}
