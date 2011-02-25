package info.guardianproject.bouncycastle.crypto.agreement;

import info.guardianproject.bouncycastle.crypto.AsymmetricCipherKeyPair;
import info.guardianproject.bouncycastle.crypto.CipherParameters;
import info.guardianproject.bouncycastle.crypto.generators.DHKeyPairGenerator;
import info.guardianproject.bouncycastle.crypto.params.AsymmetricKeyParameter;
import info.guardianproject.bouncycastle.crypto.params.DHKeyGenerationParameters;
import info.guardianproject.bouncycastle.crypto.params.DHParameters;
import info.guardianproject.bouncycastle.crypto.params.DHPrivateKeyParameters;
import info.guardianproject.bouncycastle.crypto.params.DHPublicKeyParameters;
import info.guardianproject.bouncycastle.crypto.params.ParametersWithRandom;

import java.math.BigInteger;
import java.security.SecureRandom;


/**
 * a Diffie-Hellman key exchange engine.
 * <p>
 * note: This uses MTI/A0 key agreement in order to make the key agreement
 * secure against passive attacks. If you're doing Diffie-Hellman and both
 * parties have long term public keys you should look at using this. For
 * further information have a look at RFC 2631.
 * <p>
 * It's possible to extend this to more than two parties as well, for the moment
 * that is left as an exercise for the reader.
 */
public class DHAgreement
{
    private DHPrivateKeyParameters  key;
    private DHParameters            dhParams;
    private BigInteger              privateValue;
    private SecureRandom            random;

    public void init(
        CipherParameters    param)
    {
        AsymmetricKeyParameter  kParam;

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    rParam = (ParametersWithRandom)param;

            this.random = rParam.getRandom();
            kParam = (AsymmetricKeyParameter)rParam.getParameters();
        }
        else
        {
            this.random = new SecureRandom();
            kParam = (AsymmetricKeyParameter)param;
        }

        
        if (!(kParam instanceof DHPrivateKeyParameters))
        {
            throw new IllegalArgumentException("DHEngine expects DHPrivateKeyParameters");
        }

        this.key = (DHPrivateKeyParameters)kParam;
        this.dhParams = key.getParameters();
    }

    /**
     * calculate our initial message.
     */
    public BigInteger calculateMessage()
    {
        DHKeyPairGenerator dhGen = new DHKeyPairGenerator();
        dhGen.init(new DHKeyGenerationParameters(random, dhParams));
        AsymmetricCipherKeyPair dhPair = dhGen.generateKeyPair();

        this.privateValue = ((DHPrivateKeyParameters)dhPair.getPrivate()).getX();

        return ((DHPublicKeyParameters)dhPair.getPublic()).getY();
    }

    /**
     * given a message from a given party and the corresponding public key,
     * calculate the next message in the agreement sequence. In this case
     * this will represent the shared secret.
     */
    public BigInteger calculateAgreement(
        DHPublicKeyParameters   pub,
        BigInteger              message)
    {
        if (!pub.getParameters().equals(dhParams))
        {
            throw new IllegalArgumentException("Diffie-Hellman public key has wrong parameters.");
        }

        BigInteger p = dhParams.getP();

        return message.modPow(key.getX(), p).multiply(pub.getY().modPow(privateValue, p)).mod(p);
    }
}
