package info.guardianproject.otr;

import android.util.Log;

public class OtrDebugLogger {

    private final static String TAG = "Gibberbot.OTR";
    public static boolean debugLog = true;
    public static boolean errorLog = true;

    public static void log(String msg) {
        if (debugLog && Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, msg);
    }

    public static void log(String msg, Exception e) {
        if (errorLog)
            Log.e(TAG, msg, e);
    }
}
