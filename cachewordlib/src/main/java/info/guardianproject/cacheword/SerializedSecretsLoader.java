
package info.guardianproject.cacheword;

import java.nio.ByteBuffer;

/**
 * Deserializes secrets, handling upgrading and migration as necessary
 */
public class SerializedSecretsLoader {

    public SerializedSecretsV1 loadSecrets(byte[] secrets) {

        try {
            int version = getVersion(secrets);

            switch (version) {
                case Constants.VERSION_ZERO:
                    return migrateV0toV1(new SerializedSecretsV0(secrets));
                case Constants.VERSION_ONE:
                    return new SerializedSecretsV1(secrets);
                default:
                    return null;
            }
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    public int getVersion(byte[] serialized) throws UnsupportedOperationException {
        ByteBuffer bb = ByteBuffer.wrap(serialized);

        int version = bb.getInt();
        if (version < Constants.VERSION_ZERO || version > Constants.VERSION_MAX) {
            throw new UnsupportedOperationException("Can't load version: " + version);
        }
        return version;
    }

    /**
     * Between V0 and V1 we added the adaptive PBKDF2 iteration count
     */
    private SerializedSecretsV1 migrateV0toV1(SerializedSecretsV0 ss0) {
        // this value used to be in Constants and was assumed for
        // SerializedSecretsV0
        final int old_harcoded_iter_count = 100;

        ss0.parse();
        SerializedSecretsV1 ss1 = new SerializedSecretsV1(Constants.VERSION_ONE,
                old_harcoded_iter_count,
                ss0.salt,
                ss0.iv,
                ss0.ciphertext);

        return ss1;
    }

}
