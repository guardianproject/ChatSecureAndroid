
package info.guardianproject.cacheword;

import android.content.Context;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Represents a single 256 AES secret encrypted with a key derived from the
 * user's passphrase. This class handles the PBE key derivation, secret key
 * generation, encryption, and persistence. It also provides a means for
 * fetching (decrypting) the secrets and changing the passphrase. This is the
 * simplest of cases where the application's only secret is the user's
 * passphrase. We do not want to store the passphrase, nor a hash of the
 * passphrase on disk. Initialization process consists of:
 * <ol>
 * <li>1. Run the password through PBKDF2 with a random salt
 * <li>2. Generate a random 256 bit AES key with a random IV
 * <li>3. Use the derived key to encrypt the AES key in GCM mode
 * <li>4. Write the ciphertext, iv, and salt to disk
 * </ol>
 * The exact data written to disk is represented by the SerializedSecretsV1
 * class.
 */
public class PassphraseSecrets implements ICachedSecrets {

    private static final String TAG = "PassphraseSecrets";
    private final SecretKey mSecretKey;

    private PassphraseSecrets(byte[] key) throws GeneralSecurityException {
        mSecretKey = new SecretKeySpec(key, "AES");
    }

    private PassphraseSecrets(SecretKey key) throws GeneralSecurityException {
        mSecretKey = key;
    }

    /**
     * Retrieve the AES secret key
     * @return instance of {@link SecretKey}
     */
    public SecretKey getSecretKey() {
        return mSecretKey;
    }

    /**
     * Generates a random AES key and encrypts it with a PBKDF2 key derived from
     * x_passphrase. The resulting ciphertext is saved to disk. All sensitive
     * variables are wiped.
     *
     * @param ctx
     * @param x_passphrase
     * @return instance of {@link PassphraseSecrets}
     */
    public static PassphraseSecrets initializeSecrets(Context ctx, char[] x_passphrase) {
        PassphraseSecretsImpl crypto = new PassphraseSecretsImpl();
        try {
            SecretKeySpec secretKey = (SecretKeySpec) crypto.generateSecretKey();
            boolean saved = encryptAndSave(ctx, x_passphrase, secretKey.getEncoded());
            SecretsManager.setInitialized(ctx, saved);

            if (saved)
                return new PassphraseSecrets(secretKey);
            else
                return null;
        } catch (GeneralSecurityException e) {
            Log.e(TAG,
                    "initializeSecrets failed: " + e.getClass().getName() + " : " + e.getMessage());
            return null;
        } finally {
            Wiper.wipe(x_passphrase);
        }
    }

    /**
     * Attempts to decrypt the stored secrets with x_passphrase. If successful,
     * returns a PassphraseSecrets initialized with the secret key.
     *
     * @param ctx
     * @param x_passphrase WIPED
     * @return instance of {@link PassphraseSecrets}
     * @throws GeneralSecurityException
     */
    public static PassphraseSecrets fetchSecrets(Context ctx, char[] x_passphrase)
            throws GeneralSecurityException {
        byte[] preparedSecret = SecretsManager.getBytes(ctx, Constants.SHARED_PREFS_SECRETS);
        SerializedSecretsV1 ss = new SerializedSecretsLoader().loadSecrets(preparedSecret);
        byte[] x_rawSecretKey = null;

        try {
            PassphraseSecretsImpl crypto = new PassphraseSecretsImpl();
            x_rawSecretKey = crypto.decryptWithPassphrase(x_passphrase, ss);
            PassphraseSecrets ps = new PassphraseSecrets(x_rawSecretKey);

            // check for insecure iteration counts and upgrade if necessary
            // we do this by "changing" the passphrase to the same passphrase
            // since changePassphrase calls calibrateKDF()
            if (ss.pbkdf_iter_count < Constants.PBKDF2_MINIMUM_ITERATION_COUNT) {
                ps = changePassphrase(ctx, ps, x_passphrase);
                if (ps == null)
                    throw new GeneralSecurityException(
                            "Upgrading iteration count failed during save");
            }
            return ps;
        } finally {
            Wiper.wipe(x_passphrase);
            Wiper.wipe(x_rawSecretKey);
        }
    }

    /**
     * Re-encrypts the secret key in current_secrets with a new derived key from
     * x_new_passphrase. The resulting ciphertext is saved to disk.
     *
     * @param ctx
     * @param current_secrets NOT WIPED
     * @param x_new_passphrase WIPED
     * @return instance of {@link PassphraseSecrets}
     */
    public static PassphraseSecrets changePassphrase(Context ctx,
            PassphraseSecrets current_secrets, char[] x_new_passphrase) {
        byte[] x_rawSecretKey = null;
        try {
            x_rawSecretKey = current_secrets.getSecretKey().getEncoded();
            boolean saved = encryptAndSave(ctx, x_new_passphrase, x_rawSecretKey);

            if (saved)
                return current_secrets;
            else
                return null;
        } catch (GeneralSecurityException e) {
            Log.e(TAG,
                    "changePassphrase failed: " + e.getClass().getName() + " : " + e.getMessage());
            return null;
        } finally {
            Wiper.wipe(x_new_passphrase);
            Wiper.wipe(x_rawSecretKey);
        }
    }

    /**
     * Encrypts the plaintext with the passphrase and saves the ciphertext
     * bundle to disk.
     *
     * @param ctx
     * @param x_passphrase the passphrase used to PBE on plaintext to NOT WIPED
     * @param x_plaintext the plaintext to encrypt NOT WIPED
     * @return instance of {@link PassphraseSecrets}
     * @throws GeneralSecurityException
     */
    private static boolean encryptAndSave(Context ctx, char[] x_passphrase, byte[] x_plaintext)
            throws GeneralSecurityException {
        PassphraseSecretsImpl crypto = new PassphraseSecretsImpl();
        int pbkdf2_iter_count = calibrateKDF(ctx);
        SerializedSecretsV1 ss = crypto.encryptWithPassphrase(ctx, x_passphrase, x_plaintext,
                pbkdf2_iter_count);
        byte[] preparedSecret = ss.concatenate();
        boolean saved = SecretsManager.saveBytes(ctx, Constants.SHARED_PREFS_SECRETS,
                preparedSecret);

        return saved;
    }

    /**
     * returns the number of iterations to use (always a power of 2)
     * <ul>
     * <li>Iteration Count: the minimum iteration count value used in the PBKDF2
     * key hashing step. The larger this value the more secure the user's
     * password will be against offline cracking attempts, but the longer the
     * unlocking process will take. The ideal number is one which results in
     * approximately 1 sec of unlock time on the device, however this changes
     * from device to device due to varying hardware.</li>
     * <li>Auto Calibrate: Cacheword will attempt to calculate the best
     * iteration count for the device at runtime using an adaptive algorithim
     * that takes less than a second. If auto_calibrate is set to false, then
     * Cacheword will use the minimum value in all cases. If auto_calibrate is
     * set to true and the detected iteration count (from an existing
     * installation) is lower than the new minimum, a new calibration will be
     * performed.</li>
     * </ul>
     */

    private static int calibrateKDF(Context ctx) {
        // use the SQLCipher v2.x iteration count by default
        int iterations = 4096;
        if (android.os.Build.CPU_ABI.equals("x86")) {
            iterations = 40 * getBogoMips();
        } else if (android.os.Build.CPU_ABI.equals("armeabi-v7a") ||
                android.os.Build.CPU_ABI2.equals("armeabi-v7a")) {
            iterations = 20 * getBogoMips();
        } else if (android.os.Build.CPU_ABI.equals("armeabi") ||
                android.os.Build.CPU_ABI2.equals("armeabi")) {
            iterations = 10 * getBogoMips();
        }
        int r = Math.max(Constants.PBKDF2_MINIMUM_ITERATION_COUNT, iterations);
        /* round off to a power of two to ease data recovery */
        int power = 1;
        while (power < r)
            power *= 2;
        Log.d(TAG, "calibrateKDF() selected: " + iterations + "  using: " + r);
        return r;
    }

    private static int getBogoMips() {
        try {
            FileReader br = new FileReader("/proc/cpuinfo");
            char[] buffer = new char[8192];
            br.read(buffer);
            br.close();
            String cpuinfo = new String(buffer);
            Pattern p = Pattern.compile(".*[Bb][Oo][Gg][Oo][Mm][Ii][Pp][Ss]\\s*:\\s*([0-9.]+).*");
            Matcher matcher = p.matcher(cpuinfo);
            if (matcher.find()) {
                return (int) Float.parseFloat(matcher.group(1));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 500; // return some kind of low/mid range guess
    }

    @Override
    public void destroy() {
        Log.d(TAG, "destroy()");
        Wiper.wipe((SecretKeySpec) mSecretKey);
    }

}
