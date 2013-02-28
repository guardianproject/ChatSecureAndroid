package info.guardianproject.otr;

import info.guardianproject.util.LogCleaner;
import android.util.Log;

public class OtrDebugLogger {

    private final static String TAG = "Gibberbot.OTR";
    public static boolean debugLog = true;
    public static boolean errorLog = true;

    public static void log(String msg) {
        if (debugLog && Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, LogCleaner.clean(msg));
    }

    public static void log(String msg, Exception e) {
        if (errorLog)
            Log.e(TAG, LogCleaner.clean(msg), e);
    }
}
