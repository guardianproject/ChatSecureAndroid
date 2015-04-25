
package info.guardianproject.cacheword;

import android.annotation.SuppressLint;
import android.content.Context;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PassphraseSecretsImpl {

    @SuppressWarnings("unused")
    private static final String TAG = "PassphraseSecretsImpl";

    // used by initialization and change password routines

    /**
     * Derives an encryption key from x_passphrase, then uses this derived key
     * to encrypt x_plaintext. The resulting cipher text, plus meta data
     * (version, salt, iv, @see SerializedSecretsV1) is serialized and returned.
     *
     * @param ctx
     * @param x_passphrase the passphrase used to PBE on plaintext to NOT WIPED
     * @param x_plaintext the plaintext to encrypt NOT WIPED
     * @return instance of {@link SerializedSecretsV1}
     * @throws GeneralSecurityException
     */
    public SerializedSecretsV1 encryptWithPassphrase(Context ctx, char[] x_passphrase,
            byte[] x_plaintext, int pbkdf2_iter_count) throws GeneralSecurityException {
        SecretKeySpec x_passphraseKey = null;
        try {
            byte[] salt = generateSalt(Constants.PBKDF2_SALT_LEN_BYTES);
            byte[] iv = generateIv(Constants.GCM_IV_LEN_BYTES);
            x_passphraseKey = hashPassphrase(x_passphrase, salt, pbkdf2_iter_count);
            byte[] encryptedSecretKey = encryptSecretKey(x_passphraseKey, iv, x_plaintext);
            SerializedSecretsV1 ss = new SerializedSecretsV1(Constants.VERSION_ONE,
                    pbkdf2_iter_count, salt, iv, encryptedSecretKey);
            return ss;
        } finally {
            Wiper.wipe(x_passphraseKey);
        }
    }

    /**
     * Decrypt the secret and returns the plaintext
     *
     * @param x_passphrase NOT WIPED
     * @return the plaintext
     * @throws GeneralSecurityException
     */
    public byte[] decryptWithPassphrase(char[] x_passphrase, SerializedSecretsV1 ss)
            throws GeneralSecurityException {
        byte[] x_plaintext = null;
        SecretKeySpec x_passphraseKey = null;

        try {
            //ss.parse(); this is called in the SerializedSecretsV1 constructor, and is left over code from v0

            byte[] salt = ss.salt;
            byte[] iv = ss.iv;
            byte[] ciphertext = ss.ciphertext;
            int iterations = ss.pbkdf_iter_count;
            x_passphraseKey = hashPassphrase(x_passphrase, salt, iterations);
            x_plaintext = decryptWithKey(x_passphraseKey, iv, ciphertext);

            return x_plaintext;
        } finally {
            Wiper.wipe(x_passphraseKey);
        }
    }

    // used by initialization and verification routines

    /**
     * Hash the password with PBKDF2 at Constants.PBKDF2_ITER_COUNT iterations
     * Does not wipe the password.
     *
     * @param x_password
     * @param salt
     * @return the AES SecretKeySpec containing the hashed password
     * @throws GeneralSecurityException
     */
    public SecretKeySpec hashPassphrase(char[] x_password, byte[] salt, int pbkdf2_iter_count)
            throws GeneralSecurityException {
        PBEKeySpec x_spec = null;
        try {
            x_spec = new PBEKeySpec(x_password, salt, pbkdf2_iter_count,
                    Constants.PBKDF2_KEY_LEN_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            return new SecretKeySpec(factory.generateSecret(x_spec).getEncoded(), "AES");
        } finally {
            Wiper.wipe(x_spec);
        }
    }

    // verification routines: used to unlock secrets

    /**
     * Decrypt with supplied key
     *
     * @param x_passphraseKey NOT WIPED
     * @param iv
     * @param ciphertext
     * @return the plaintext
     * @throws GeneralSecurityException on MAC failure or wrong key
     */
    public byte[] decryptWithKey(SecretKey x_passphraseKey, byte[] iv, byte[] ciphertext)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, x_passphraseKey, new IvParameterSpec(iv));

        return cipher.doFinal(ciphertext);
    }

    // initialization routines: creates secrets

    /**
     * Encrypts the data with AES GSM Does not wipe the data nor the key
     *
     * @param x_passphraseKey
     * @param iv
     * @param data
     * @return the encrypted key ciphertext
     * @throws GeneralSecurityException
     */
    public byte[] encryptSecretKey(SecretKey x_passphraseKey, byte[] iv, byte[] data)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // TODO(abel) follow this rabbit hole down and wipe it!
        cipher.init(Cipher.ENCRYPT_MODE, x_passphraseKey, new IvParameterSpec(iv));

        return cipher.doFinal(data);
    }

    @SuppressLint("TrulyRandom")
    public byte[] generateIv(int length) throws NoSuchAlgorithmException {
        byte[] iv = new byte[length];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(iv);
        return iv;
    }

    public byte[] generateSalt(int length) throws NoSuchAlgorithmException {
        byte[] salt = new byte[length];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(salt);
        return salt;
    }

    /**
     * Generate a random AES_KEY_LENGTH bit AES key
     */
    public SecretKey generateSecretKey() {
        try {

            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(Constants.AES_KEY_LEN_BITS);

            return generator.generateKey();

        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
