package info.guardianproject.bouncycastle.crypto.tls;

import info.guardianproject.bouncycastle.crypto.DSA;
import info.guardianproject.bouncycastle.crypto.params.AsymmetricKeyParameter;
import info.guardianproject.bouncycastle.crypto.params.DSAPublicKeyParameters;
import info.guardianproject.bouncycastle.crypto.signers.DSASigner;

class TlsDSSSigner extends TlsDSASigner
{
    public boolean isValidPublicKey(AsymmetricKeyParameter publicKey)
    {
        return publicKey instanceof DSAPublicKeyParameters;
    }

    protected DSA createDSAImpl()
    {
        return new DSASigner();
    }
}
