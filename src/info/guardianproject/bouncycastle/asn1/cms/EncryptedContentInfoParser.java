package info.guardianproject.bouncycastle.asn1.cms;

import info.guardianproject.bouncycastle.asn1.ASN1SequenceParser;
import info.guardianproject.bouncycastle.asn1.ASN1TaggedObjectParser;
import info.guardianproject.bouncycastle.asn1.DEREncodable;
import info.guardianproject.bouncycastle.asn1.DERObjectIdentifier;
import info.guardianproject.bouncycastle.asn1.x509.AlgorithmIdentifier;

import java.io.IOException;


/**
 * <pre>
 * EncryptedContentInfo ::= SEQUENCE {
 *     contentType ContentType,
 *     contentEncryptionAlgorithm ContentEncryptionAlgorithmIdentifier,
 *     encryptedContent [0] IMPLICIT EncryptedContent OPTIONAL 
 * }
 * </pre>
 */
public class EncryptedContentInfoParser
{
    private DERObjectIdentifier    _contentType;
    private AlgorithmIdentifier     _contentEncryptionAlgorithm;
    private ASN1TaggedObjectParser _encryptedContent;

    public EncryptedContentInfoParser(
        ASN1SequenceParser  seq) 
        throws IOException
    {
        _contentType = (DERObjectIdentifier)seq.readObject();
        _contentEncryptionAlgorithm = AlgorithmIdentifier.getInstance(seq.readObject().getDERObject());
        _encryptedContent = (ASN1TaggedObjectParser)seq.readObject();
    }
    
    public DERObjectIdentifier getContentType()
    {
        return _contentType;
    }
    
    public AlgorithmIdentifier getContentEncryptionAlgorithm()
    {
        return _contentEncryptionAlgorithm;
    }

    public DEREncodable getEncryptedContent(
        int  tag) 
        throws IOException
    {
        return _encryptedContent.getObjectParser(tag, false);
    }
}
