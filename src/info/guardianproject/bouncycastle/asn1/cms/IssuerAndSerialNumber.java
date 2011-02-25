package info.guardianproject.bouncycastle.asn1.cms;

import info.guardianproject.bouncycastle.asn1.ASN1Encodable;
import info.guardianproject.bouncycastle.asn1.ASN1EncodableVector;
import info.guardianproject.bouncycastle.asn1.ASN1Sequence;
import info.guardianproject.bouncycastle.asn1.DERInteger;
import info.guardianproject.bouncycastle.asn1.DERObject;
import info.guardianproject.bouncycastle.asn1.DERSequence;
import info.guardianproject.bouncycastle.asn1.x500.X500Name;
import info.guardianproject.bouncycastle.asn1.x509.X509Name;

import java.math.BigInteger;


public class IssuerAndSerialNumber
    extends ASN1Encodable
{
    private X500Name    name;
    private DERInteger  serialNumber;

    public static IssuerAndSerialNumber getInstance(
        Object  obj)
    {
        if (obj instanceof IssuerAndSerialNumber)
        {
            return (IssuerAndSerialNumber)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new IssuerAndSerialNumber((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException(
            "Illegal object in IssuerAndSerialNumber: " + obj.getClass().getName());
    }

    public IssuerAndSerialNumber(
        ASN1Sequence    seq)
    {
        this.name = X500Name.getInstance(seq.getObjectAt(0));
        this.serialNumber = (DERInteger)seq.getObjectAt(1);
    }

    public IssuerAndSerialNumber(
        X500Name name,
        BigInteger  serialNumber)
    {
        this.name = name;
        this.serialNumber = new DERInteger(serialNumber);
    }

    /**
     * @deprecated use X500Name constructor
     */
    public IssuerAndSerialNumber(
        X509Name    name,
        BigInteger  serialNumber)
    {
        this.name = X500Name.getInstance(name);
        this.serialNumber = new DERInteger(serialNumber);
    }

    /**
     * @deprecated use X500Name constructor
     */
    public IssuerAndSerialNumber(
        X509Name    name,
        DERInteger  serialNumber)
    {
        this.name = X500Name.getInstance(name);
        this.serialNumber = serialNumber;
    }

    public X500Name getName()
    {
        return name;
    }

    public DERInteger getSerialNumber()
    {
        return serialNumber;
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector    v = new ASN1EncodableVector();

        v.add(name);
        v.add(serialNumber);

        return new DERSequence(v);
    }
}
