
package info.guardianproject.cacheword;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A utility class for securely wiping memory locations and handling sensitive
 * strings as character arrays.
 */
public class Wiper {

    // On android the default is UTF-8
    public final static Charset Utf8CharSet = Charset.forName("UTF-8");

    /**
     * Fills the parameter with 0s
     */
    public static void wipe(byte[] bytes) {
        if (bytes == null)
            return;
        Arrays.fill(bytes, (byte) 0);
    }

    /**
     * Fills the parameter with 0s
     */
    public static void wipe(char[] chars) {
        if (chars == null)
            return;
        Arrays.fill(chars, '\0');
    }

    /**
     * Fills the underlying array with 0s
     */
    public static void wipe(ByteBuffer bb) {
        if (bb == null)
            return;
        wipe(bb.array());
    }

    /**
     * Fills the underlying array with 0s
     */
    public static void wipe(CharBuffer cb) {
        if (cb == null)
            return;
        wipe(cb.array());
    }

    public static void wipe(SecretKeySpec key) {
        /*
         * for( Field field : SecretKeySpec.class.getDeclaredFields() ) {
         * Log.d("Wiper", "SecretKeySpec field: " + field.getName()); }
         */
        if (key == null)
            return;

        try {
            Field key_field = SecretKeySpec.class.getDeclaredField("key");
            key_field.setAccessible(true);
            byte[] bytes = (byte[]) key_field.get(key);
            wipe(bytes);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void wipe(PBEKeySpec spec) {
        if (spec == null)
            return;
        spec.clearPassword();
    }

    /**
     * Convert a CharBuffer into a UTF-8 encoded ByteBuffer
     */
    // public static ByteBuffer utf8ToBytes(CharBuffer cb) {
    // return Utf8CharSet.encode(cb);
    // }

    /**
     * Securely convert a char[] to a UTF-8 encoded byte[]. All intermediate
     * memory is wiped.
     * 
     * @return a new byte array containing the encoded characters
     */
    // public static byte[] utf8charsToBytes(char[] chars) {
    // ByteBuffer bb = utf8ToBytes(CharBuffer.wrap(chars));
    // byte[] result = new byte[bb.limit()];
    // System.arraycopy(bb.array(), 0, result, 0, bb.limit());
    // wipe(bb.array());
    // return result;
    // }

    /**
     * Securely convert a UTF-8 encoded byte[] to a char[] All intermediate
     * memory is wiped.
     * 
     * @return a new char array containing the decoded characters
     */
    // public static char[] bytesToUtf8Chars(byte[] bytes) {
    // ByteBuffer bb = ByteBuffer.wrap(bytes);
    // CharBuffer cb = Utf8CharSet.decode(bb);
    // char[] result = new char[cb.limit()];
    // System.arraycopy(cb.array(), 0, result, 0, cb.limit());
    // wipe(cb.array());
    // return result;
    // }
}
