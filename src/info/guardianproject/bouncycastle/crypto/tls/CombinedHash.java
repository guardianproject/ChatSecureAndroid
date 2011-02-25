package info.guardianproject.bouncycastle.crypto.tls;

import info.guardianproject.bouncycastle.crypto.Digest;
import info.guardianproject.bouncycastle.crypto.digests.MD5Digest;
import info.guardianproject.bouncycastle.crypto.digests.SHA1Digest;

/**
 * A combined hash, which implements md5(m) || sha1(m).
 */
class CombinedHash implements Digest
{
    private MD5Digest md5;
    private SHA1Digest sha1;

    CombinedHash()
    {
        this.md5 = new MD5Digest();
        this.sha1 = new SHA1Digest();
    }

    CombinedHash(CombinedHash t)
    {
        this.md5 = new MD5Digest(t.md5);
        this.sha1 = new SHA1Digest(t.sha1);
    }

    /**
     * @see info.guardianproject.bouncycastle.crypto.Digest#getAlgorithmName()
     */
    public String getAlgorithmName()
    {
        return md5.getAlgorithmName() + " and " + sha1.getAlgorithmName() + " for TLS 1.0";
    }

    /**
     * @see info.guardianproject.bouncycastle.crypto.Digest#getDigestSize()
     */
    public int getDigestSize()
    {
        return 16 + 20;
    }

    /**
     * @see info.guardianproject.bouncycastle.crypto.Digest#update(byte)
     */
    public void update(byte in)
    {
        md5.update(in);
        sha1.update(in);
    }

    /**
     * @see info.guardianproject.bouncycastle.crypto.Digest#update(byte[],int,int)
     */
    public void update(byte[] in, int inOff, int len)
    {
        md5.update(in, inOff, len);
        sha1.update(in, inOff, len);
    }

    /**
     * @see info.guardianproject.bouncycastle.crypto.Digest#doFinal(byte[],int)
     */
    public int doFinal(byte[] out, int outOff)
    {
        int i1 = md5.doFinal(out, outOff);
        int i2 = sha1.doFinal(out, outOff + 16);
        return i1 + i2;
    }

    /**
     * @see info.guardianproject.bouncycastle.crypto.Digest#reset()
     */
    public void reset()
    {
        md5.reset();
        sha1.reset();
    }

}
