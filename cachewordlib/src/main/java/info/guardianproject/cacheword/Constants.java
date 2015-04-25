
package info.guardianproject.cacheword;

public class Constants {

    // Service class name
    public static final String SERVICE_CLASS_NAME = "info.guardianproject.cacheword.CacheWordService";

    // Intents
    public static final String INTENT_PASS_EXPIRED = "info.guardianproject.cacheword.PASS_EXPIRED";
    public static final String INTENT_NEW_SECRETS = "info.guardianproject.cacheword.NEW_SECRETS";

    // Values
    public static final String SHARED_PREFS = "info.guardianproject.cacheword.prefs";
    public static final int SHARED_PREFS_PRIVATE_MODE = 0;
    public static final String SHARED_PREFS_INITIALIZED = "initialized";
    public static final String SHARED_PREFS_SECRETS = "encrypted_secrets";
    public static final String SHARED_PREFS_FOREGROUND = "foreground";
    public static final String SHARED_PREFS_TIMEOUT_SECONDS = "cacheword_timeout_seconds";
    public static final String SHARED_PREFS_USE_NOTIFICATION = "enable_notification";
    public static final String SHARED_PREFS_VIBRATE = "cacheword_vibrate";

    public static final String SHARED_PREFS_SQLCIPHER_V3_MIGRATE = "cacheword_sqlcipher_v3_migrated";

    public static final int SERVICE_FOREGROUND_ID = 81231;
    public static final int SERVICE_BACKGROUND_ID = 13218;

    public final static int VERSION_ZERO = 0;
    public final static int VERSION_ONE = 1;
    public final static int VERSION_MAX = VERSION_ONE;

    public static final int STATE_UNKNOWN = -1;
    public static final int STATE_UNINITIALIZED = 0;
    public static final int STATE_LOCKED = 1;
    public static final int STATE_UNLOCKED = 2;

    // encryption constants
    public static final int AES_KEY_LEN_BITS        = 256; // bits
    public static final int GCM_IV_LEN_BYTES        = 12;  // 96 bits
    public static final int INT_LENGTH              = 4;   // length of integer in the JVM
    // key derivation constants
    public static final int PBKDF2_KEY_LEN_BITS     = 128; // bits
    public static final int PBKDF2_SALT_LEN_BYTES   = 16;  // bytes, 128 bits
    public static final int PBKDF2_MINIMUM_ITERATION_COUNT = 1024;

}
