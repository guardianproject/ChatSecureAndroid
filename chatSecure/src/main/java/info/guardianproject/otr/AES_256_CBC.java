package info.guardianproject.otr;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.jivesoftware.smack.util.Base64;

/**
 * Class created for StackOverflow by owlstead. This is open source, you are
 * free to copy and use for any purpose.
 * http://stackoverflow.com/questions/11783062
 * /how-to-decrypt-an-encrypted-file-in-java-with-openssl-with-aes
 */
public class AES_256_CBC {
    private static final int INDEX_KEY = 0;
    private static final int INDEX_IV = 1;
    private static final int ITERATIONS = 1;

    private static final int SALT_OFFSET = 8;
    private static final int SALT_SIZE = 8;
    private static final int CIPHERTEXT_OFFSET = SALT_OFFSET + SALT_SIZE;

    private static final int KEY_SIZE_BITS = 256;

    /**
     * Thanks go to Ola Bini for releasing this source on his blog. The source
     * was obtained from <a
     * href="http://olabini.com/blog/tag/evp_bytestokey/">here</a> .
     */
    public static byte[][] EVP_BytesToKey(int key_len, int iv_len, MessageDigest md, byte[] salt,
            byte[] data, int count) {
        byte[][] both = new byte[2][];
        byte[] key = new byte[key_len];
        int key_ix = 0;
        byte[] iv = new byte[iv_len];
        int iv_ix = 0;
        both[0] = key;
        both[1] = iv;
        byte[] md_buf = null;
        int nkey = key_len;
        int niv = iv_len;
        int i = 0;
        if (data == null) {
            return both;
        }
        int addmd = 0;
        for (;;) {
            md.reset();
            if (addmd++ > 0) {
                md.update(md_buf);
            }
            md.update(data);
            if (null != salt) {
                md.update(salt, 0, 8);
            }
            md_buf = md.digest();
            for (i = 1; i < count; i++) {
                md.reset();
                md.update(md_buf);
                md_buf = md.digest();
            }
            i = 0;
            if (nkey > 0) {
                for (;;) {
                    if (nkey == 0)
                        break;
                    if (i == md_buf.length)
                        break;
                    key[key_ix++] = md_buf[i];
                    nkey--;
                    i++;
                }
            }
            if (niv > 0 && i != md_buf.length) {
                for (;;) {
                    if (niv == 0)
                        break;
                    if (i == md_buf.length)
                        break;
                    iv[iv_ix++] = md_buf[i];
                    niv--;
                    i++;
                }
            }
            if (nkey == 0 && niv == 0) {
                break;
            }
        }
        for (i = 0; i < md_buf.length; i++) {
            md_buf[i] = 0;
        }
        return both;
    }

    public static String decryptAscii(File f, String password) throws IOException {
        byte[] headerSaltAndCipherText = null;
        String raw = FileUtils.readFileToString(f);
        headerSaltAndCipherText = Base64.decode(raw);
        return decrypt(headerSaltAndCipherText, password);
    }

    public static String decryptBin(File f, String password) throws IOException {
        return decrypt(FileUtils.readFileToByteArray(f), password);
    }

    public static String decrypt(File f, String password) throws IOException {
        String raw = FileUtils.readFileToString(f);
        if (raw.startsWith("Salted__"))
            return decryptBin(f, password);
        else
            return decryptAscii(f, password);
    }

    // cheap copy of Arrays.copyOfRange() since its not in Android until android-9
    private static byte[] copyOfRange(byte[] in, int from, int to) {
        int size = to - from;
        byte[] out = new byte[size];
        for (int i = 0; i < size; i++)
            out[i] = in[from + i];
        return out;
    }

    public static String decrypt(byte[] headerSaltAndCipherText, String password) {
        try {
            // --- extract salt & encrypted ---

            // header is "Salted__", ASCII encoded, if salt is being used (the
            // default)
            byte[] salt = copyOfRange(headerSaltAndCipherText, SALT_OFFSET, SALT_OFFSET + SALT_SIZE);
            byte[] encrypted = copyOfRange(headerSaltAndCipherText, CIPHERTEXT_OFFSET,
                    headerSaltAndCipherText.length);

            // --- specify cipher and digest for EVP_BytesToKey method ---

            Cipher aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding");
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            // --- create key and IV ---

            // the IV is useless, OpenSSL might as well have use zero's
            final byte[][] keyAndIV = EVP_BytesToKey(KEY_SIZE_BITS / Byte.SIZE,
                    aesCBC.getBlockSize(), md5, salt, password.getBytes("ASCII"), ITERATIONS);
            SecretKeySpec key = new SecretKeySpec(keyAndIV[INDEX_KEY], "AES");
            IvParameterSpec iv = new IvParameterSpec(keyAndIV[INDEX_IV]);

            // --- initialize cipher instance and decrypt ---

            aesCBC.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decrypted = aesCBC.doFinal(encrypted);

            return new String(decrypted, "ASCII");
        } catch (BadPaddingException e) {
            // AKA "something went wrong"
            throw new IllegalStateException(
                    "Bad password, algorithm, mode or padding;"
                            + " no salt, wrong number of iterations or corrupted ciphertext.");
        } catch (IllegalBlockSizeException e) {
            throw new IllegalStateException(
                    "Bad algorithm, mode or corrupted (resized) ciphertext.");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
