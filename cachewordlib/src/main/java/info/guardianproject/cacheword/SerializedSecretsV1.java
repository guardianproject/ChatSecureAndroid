
package info.guardianproject.cacheword;

import java.nio.ByteBuffer;

/**
 * A ciphertext bundle Encapsulates a ciphertext and associated non-sensitive
 * metadata required to decrypt it by conveniently handling all array
 * manipulation. This class does not handle sensitive data.
 */
public class SerializedSecretsV1 {
    public int version;
    public int pbkdf_iter_count;
    public byte[] salt;
    public byte[] iv;
    public byte[] ciphertext;
    public byte[] serialized;

    public SerializedSecretsV1(int version, int iterations, byte[] salt, byte[] iv,
            byte[] ciphertext) {
        this.version = version;
        this.pbkdf_iter_count = iterations;
        this.salt = salt;
        this.iv = iv;
        this.ciphertext = ciphertext;
    }

    public SerializedSecretsV1(byte[] serialized) {
        this.serialized = serialized;
        parse();
    }

    public void parse() {
        salt = new byte[Constants.PBKDF2_SALT_LEN_BYTES];
        iv = new byte[Constants.GCM_IV_LEN_BYTES];
        ciphertext = new byte[serialized.length - constants_length()];
        ByteBuffer bb = ByteBuffer.wrap(serialized);

        version = bb.getInt();
        pbkdf_iter_count = bb.getInt();
        bb.get(salt);
        bb.get(iv);
        bb.get(ciphertext);
    }

    public byte[] concatenate() {
        serialized = new byte[constants_length() + ciphertext.length];
        ByteBuffer bb = ByteBuffer.wrap(serialized);
        bb.putInt(version);
        bb.putInt(pbkdf_iter_count);
        bb.put(salt);
        bb.put(iv);
        bb.put(ciphertext);
        serialized = bb.array();
        return serialized;
    }

    public static int constants_length() {
        int bytes = Constants.INT_LENGTH * 2 + Constants.PBKDF2_SALT_LEN_BYTES
                + Constants.GCM_IV_LEN_BYTES;
        return bytes;
    }

}
