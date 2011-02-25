package info.guardianproject.bouncycastle.asn1.esf;

import info.guardianproject.bouncycastle.asn1.ASN1Encodable;
import info.guardianproject.bouncycastle.asn1.ASN1Sequence;
import info.guardianproject.bouncycastle.asn1.DERObject;
import info.guardianproject.bouncycastle.asn1.DERSequence;

import java.util.Enumeration;


/**
 * <pre>
 * CRLListID ::= SEQUENCE {
 *     crls SEQUENCE OF CrlValidatedID }
 * </pre>
 */
public class CrlListID
    extends ASN1Encodable
{

    private ASN1Sequence crls;

    public static CrlListID getInstance(Object obj)
    {
        if (obj instanceof CrlListID)
        {
            return (CrlListID)obj;
        }
        else if (obj != null)
        {
            return new CrlListID(ASN1Sequence.getInstance(obj));
        }

        throw new IllegalArgumentException("null value in getInstance");
    }

    private CrlListID(ASN1Sequence seq)
    {
        this.crls = (ASN1Sequence)seq.getObjectAt(0);
        Enumeration e = this.crls.getObjects();
        while (e.hasMoreElements())
        {
            CrlValidatedID.getInstance(e.nextElement());
        }
    }

    public CrlListID(CrlValidatedID[] crls)
    {
        this.crls = new DERSequence(crls);
    }

    public CrlValidatedID[] getCrls()
    {
        CrlValidatedID[] result = new CrlValidatedID[this.crls.size()];
        for (int idx = 0; idx < result.length; idx++)
        {
            result[idx] = CrlValidatedID
                .getInstance(this.crls.getObjectAt(idx));
        }
        return result;
    }

    public DERObject toASN1Object()
    {
        return new DERSequence(this.crls);
    }
}
