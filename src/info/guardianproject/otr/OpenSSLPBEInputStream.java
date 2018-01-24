package info.guardianproject.otr;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class OpenSSLPBEInputStream extends InputStream {

    private final static int READ_BLOCK_SIZE = 64 * 1024;

    private final Cipher cipher;
    private final InputStream inStream;
    private final byte[] bufferCipher = new byte[READ_BLOCK_SIZE];

    private byte[] bufferClear = null;

    private int index = Integer.MAX_VALUE;
    private int maxIndex = 0;

    public OpenSSLPBEInputStream(final InputStream streamIn, String algIn, int iterationCount, char[] password)
            throws IOException {
        this.inStream = streamIn;
        try {
            byte[] salt = readSalt();
            cipher = OpenSSLPBECommon.initializeCipher(password, salt, Cipher.DECRYPT_MODE, algIn, iterationCount);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public int available() throws IOException {
        return inStream.available();
    }

    @Override
    public int read() throws IOException {

        if (index > maxIndex) {
            index = 0;
            int read = inStream.read(bufferCipher);
            if (read != -1) {
                bufferClear = cipher.update(bufferCipher, 0, read);
            }
            if (read == -1 || bufferClear == null || bufferClear.length == 0) {
                try {
                    bufferClear = cipher.doFinal();
                } catch (Exception e) {
                    bufferClear = null;
                }
            }
            if (bufferClear == null || bufferClear.length == 0) {
                return -1;
            }
            maxIndex = bufferClear.length - 1;
        }

        if (bufferClear == null || bufferClear.length == 0) {
            return -1;
        }

        return bufferClear[index++] & 0xff;

    }

    private byte[] readSalt() throws IOException {

        byte[] headerBytes = new byte[OpenSSLPBECommon.OPENSSL_HEADER_STRING.length()];
        inStream.read(headerBytes);
        String headerString = new String(headerBytes, OpenSSLPBECommon.OPENSSL_HEADER_ENCODE);

        if (!OpenSSLPBECommon.OPENSSL_HEADER_STRING.equals(headerString)) {
            throw new IOException("unexpected file header " + headerString);
        }

        byte[] salt = new byte[OpenSSLPBECommon.SALT_SIZE_BYTES];
        inStream.read(salt);

        return salt;
    }

}