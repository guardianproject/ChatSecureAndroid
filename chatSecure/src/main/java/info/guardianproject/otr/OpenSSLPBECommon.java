package info.guardianproject.otr;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

class OpenSSLPBECommon {

protected static final int SALT_SIZE_BYTES = 8;
protected static final String OPENSSL_HEADER_STRING = "Salted__";
protected static final String OPENSSL_HEADER_ENCODE = "ASCII";

protected static Cipher initializeCipher(char[] password, byte[] salt, int cipherMode,
                                         final String algorithm, int iterationCount) throws NoSuchAlgorithmException, InvalidKeySpecException,
        InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException {

    PBEKeySpec keySpec = new PBEKeySpec(password);
    SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
    SecretKey key = factory.generateSecret(keySpec);

    Cipher cipher = Cipher.getInstance(algorithm);
    cipher.init(cipherMode, key, new PBEParameterSpec(salt, iterationCount));

    return cipher;
}

}
