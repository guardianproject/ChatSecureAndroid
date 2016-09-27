
package info.guardianproject.cacheword;

import java.nio.ByteBuffer;

/**
 * A ciphertext bundle Encapsulates a ciphertext and associated non-sensitive
 * metadata required to decrypt it by conveniently handling all array
 * manipulation. This class does not handle sensitive data.
 */
public class SerializedSecretsV0 {
    public int version;
    public byte[] salt;
    public byte[] iv;
    public byte[] ciphertext;
    public byte[] serialized;

    public SerializedSecretsV0(int version, byte[] salt, byte[] iv, byte[] ciphertext) {
        this.version = version;
        this.salt = salt;
        this.iv = iv;
        this.ciphertext = ciphertext;
    }

    public SerializedSecretsV0(byte[] serialized) {
        this.serialized = serialized;
    }

    public void parse() {
        salt = new byte[Constants.PBKDF2_SALT_LEN_BYTES];
        iv = new byte[Constants.GCM_IV_LEN_BYTES];
        ciphertext = new byte[serialized.length
                - (Constants.PBKDF2_SALT_LEN_BYTES + Constants.GCM_IV_LEN_BYTES + Constants.INT_LENGTH)];
        ByteBuffer bb = ByteBuffer.wrap(serialized);
        version = bb.getInt();
        bb.get(salt);
        bb.get(iv);
        bb.get(ciphertext);
    }

    public byte[] concatenate() {
        serialized = new byte[Constants.INT_LENGTH + Constants.PBKDF2_SALT_LEN_BYTES
                + Constants.GCM_IV_LEN_BYTES + ciphertext.length];
        ByteBuffer bb = ByteBuffer.wrap(serialized);
        bb.putInt(version);
        bb.put(salt);
        bb.put(iv);
        bb.put(ciphertext);
        serialized = bb.array();
        return serialized;
    }

}
