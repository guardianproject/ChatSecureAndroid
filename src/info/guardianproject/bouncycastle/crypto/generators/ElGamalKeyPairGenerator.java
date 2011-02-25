package info.guardianproject.bouncycastle.crypto.generators;

import info.guardianproject.bouncycastle.crypto.AsymmetricCipherKeyPair;
import info.guardianproject.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import info.guardianproject.bouncycastle.crypto.KeyGenerationParameters;
import info.guardianproject.bouncycastle.crypto.params.DHParameters;
import info.guardianproject.bouncycastle.crypto.params.ElGamalKeyGenerationParameters;
import info.guardianproject.bouncycastle.crypto.params.ElGamalParameters;
import info.guardianproject.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
import info.guardianproject.bouncycastle.crypto.params.ElGamalPublicKeyParameters;

import java.math.BigInteger;


/**
 * a ElGamal key pair generator.
 * <p>
 * This generates keys consistent for use with ElGamal as described in
 * page 164 of "Handbook of Applied Cryptography".
 */
public class ElGamalKeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator
{
    private ElGamalKeyGenerationParameters param;

    public void init(
        KeyGenerationParameters param)
    {
        this.param = (ElGamalKeyGenerationParameters)param;
    }

    public AsymmetricCipherKeyPair generateKeyPair()
    {
        DHKeyGeneratorHelper helper = DHKeyGeneratorHelper.INSTANCE;
        ElGamalParameters egp = param.getParameters();
        DHParameters dhp = new DHParameters(egp.getP(), egp.getG(), null, egp.getL());  

        BigInteger x = helper.calculatePrivate(dhp, param.getRandom()); 
        BigInteger y = helper.calculatePublic(dhp, x);

        return new AsymmetricCipherKeyPair(
            new ElGamalPublicKeyParameters(y, egp),
            new ElGamalPrivateKeyParameters(x, egp));
    }
}
