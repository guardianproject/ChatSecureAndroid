package info.guardianproject.bouncycastle.asn1.cms;

import info.guardianproject.bouncycastle.asn1.ASN1Encodable;
import info.guardianproject.bouncycastle.asn1.ASN1EncodableVector;
import info.guardianproject.bouncycastle.asn1.ASN1Sequence;
import info.guardianproject.bouncycastle.asn1.ASN1TaggedObject;
import info.guardianproject.bouncycastle.asn1.DERObject;
import info.guardianproject.bouncycastle.asn1.DERSequence;

import java.util.Enumeration;


public class TimeStampTokenEvidence
    extends ASN1Encodable
{
    private TimeStampAndCRL[] timeStampAndCRLs;

    public TimeStampTokenEvidence(TimeStampAndCRL[] timeStampAndCRLs)
    {
        this.timeStampAndCRLs = timeStampAndCRLs;
    }

    public TimeStampTokenEvidence(TimeStampAndCRL timeStampAndCRL)
    {
        this.timeStampAndCRLs = new TimeStampAndCRL[1];

        timeStampAndCRLs[0] = timeStampAndCRL;
    }

    private TimeStampTokenEvidence(ASN1Sequence seq)
    {
        this.timeStampAndCRLs = new TimeStampAndCRL[seq.size()];

        int count = 0;

        for (Enumeration en = seq.getObjects(); en.hasMoreElements();)
        {
            timeStampAndCRLs[count++] = TimeStampAndCRL.getInstance(en.nextElement());
        }
    }

    public static TimeStampTokenEvidence getInstance(ASN1TaggedObject tagged, boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(tagged, explicit));
    }

    public static TimeStampTokenEvidence getInstance(Object obj)
    {
        if (obj instanceof TimeStampTokenEvidence)
        {
            return (TimeStampTokenEvidence)obj;
        }
        else if (obj != null)
        {
            return new TimeStampTokenEvidence(ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    public TimeStampAndCRL[] toTimeStampAndCRLArray()
    {
        return timeStampAndCRLs;
    }
    
    /**
     * <pre>
     * TimeStampTokenEvidence ::=
     *    SEQUENCE SIZE(1..MAX) OF TimeStampAndCRL
     * </pre>
     * @return
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        for (int i = 0; i != timeStampAndCRLs.length; i++)
        {
            v.add(timeStampAndCRLs[i]);
        }

        return new DERSequence(v);
    }

}
