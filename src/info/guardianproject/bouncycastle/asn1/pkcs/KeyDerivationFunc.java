package info.guardianproject.bouncycastle.asn1.pkcs;

import info.guardianproject.bouncycastle.asn1.ASN1Encodable;
import info.guardianproject.bouncycastle.asn1.ASN1Sequence;
import info.guardianproject.bouncycastle.asn1.DERObjectIdentifier;
import info.guardianproject.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class KeyDerivationFunc
    extends AlgorithmIdentifier
{
    KeyDerivationFunc(
        ASN1Sequence  seq)
    {
        super(seq);
    }
    
    public KeyDerivationFunc(
        DERObjectIdentifier id,
        ASN1Encodable       params)
    {
        super(id, params);
    }
}
