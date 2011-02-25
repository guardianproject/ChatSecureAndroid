package info.guardianproject.bouncycastle.crypto.params;

import info.guardianproject.bouncycastle.math.ec.ECConstants;
import info.guardianproject.bouncycastle.math.ec.ECCurve;
import info.guardianproject.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;


public class ECDomainParameters
    implements ECConstants
{
    ECCurve     curve;
    byte[]      seed;
    ECPoint     G;
    BigInteger  n;
    BigInteger  h;

    public ECDomainParameters(
        ECCurve     curve,
        ECPoint     G,
        BigInteger  n)
    {
        this.curve = curve;
        this.G = G;
        this.n = n;
        this.h = ONE;
        this.seed = null;
    }

    public ECDomainParameters(
        ECCurve     curve,
        ECPoint     G,
        BigInteger  n,
        BigInteger  h)
    {
        this.curve = curve;
        this.G = G;
        this.n = n;
        this.h = h;
        this.seed = null;
    }

    public ECDomainParameters(
        ECCurve     curve,
        ECPoint     G,
        BigInteger  n,
        BigInteger  h,
        byte[]      seed)
    {
        this.curve = curve;
        this.G = G;
        this.n = n;
        this.h = h;
        this.seed = seed;
    }

    public ECCurve getCurve()
    {
        return curve;
    }

    public ECPoint getG()
    {
        return G;
    }

    public BigInteger getN()
    {
        return n;
    }

    public BigInteger getH()
    {
        return h;
    }

    public byte[] getSeed()
    {
        return seed;
    }
}
