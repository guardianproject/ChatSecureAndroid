package info.guardianproject.bouncycastle.asn1.pkcs;

import info.guardianproject.bouncycastle.asn1.ASN1Encodable;
import info.guardianproject.bouncycastle.asn1.ASN1EncodableVector;
import info.guardianproject.bouncycastle.asn1.ASN1OctetString;
import info.guardianproject.bouncycastle.asn1.ASN1Sequence;
import info.guardianproject.bouncycastle.asn1.DERInteger;
import info.guardianproject.bouncycastle.asn1.DERObject;
import info.guardianproject.bouncycastle.asn1.DEROctetString;
import info.guardianproject.bouncycastle.asn1.DERSequence;
import info.guardianproject.bouncycastle.asn1.x509.DigestInfo;

import java.math.BigInteger;


public class MacData
    extends ASN1Encodable
{
    private static final BigInteger ONE = BigInteger.valueOf(1);

    DigestInfo                  digInfo;
    byte[]                      salt;
    BigInteger                  iterationCount;

    public static MacData getInstance(
        Object  obj)
    {
        if (obj instanceof MacData)
        {
            return (MacData)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new MacData((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public MacData(
        ASN1Sequence seq)
    {
        this.digInfo = DigestInfo.getInstance(seq.getObjectAt(0));

        this.salt = ((ASN1OctetString)seq.getObjectAt(1)).getOctets();

        if (seq.size() == 3)
        {
            this.iterationCount = ((DERInteger)seq.getObjectAt(2)).getValue();
        }
        else
        {
            this.iterationCount = ONE;
        }
    }

    public MacData(
        DigestInfo  digInfo,
        byte[]      salt,
        int         iterationCount)
    {
        this.digInfo = digInfo;
        this.salt = salt;
        this.iterationCount = BigInteger.valueOf(iterationCount);
    }

    public DigestInfo getMac()
    {
        return digInfo;
    }

    public byte[] getSalt()
    {
        return salt;
    }

    public BigInteger getIterationCount()
    {
        return iterationCount;
    }

    /**
     * <pre>
     * MacData ::= SEQUENCE {
     *     mac      DigestInfo,
     *     macSalt  OCTET STRING,
     *     iterations INTEGER DEFAULT 1
     *     -- Note: The default is for historic reasons and its use is deprecated. A
     *     -- higher value, like 1024 is recommended.
     * </pre>
     * @return the basic DERObject construction.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(digInfo);
        v.add(new DEROctetString(salt));
        
        if (!iterationCount.equals(ONE))
        {
            v.add(new DERInteger(iterationCount));
        }

        return new DERSequence(v);
    }
}
