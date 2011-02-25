package info.guardianproject.bouncycastle.bcpg.sig;

import info.guardianproject.bouncycastle.bcpg.SignatureSubpacket;
import info.guardianproject.bouncycastle.bcpg.SignatureSubpacketTags;

/**
 * Packet embedded signature
 */
public class EmbeddedSignature
    extends SignatureSubpacket
{
    public EmbeddedSignature(
        boolean    critical,
        byte[]     data)
    {
        super(SignatureSubpacketTags.EMBEDDED_SIGNATURE, critical, data);
    }
}