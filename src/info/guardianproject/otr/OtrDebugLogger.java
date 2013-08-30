package info.guardianproject.otr;

import info.guardianproject.util.Debug;
import info.guardianproject.util.LogCleaner;
import android.util.Log;

public class OtrDebugLogger {

    private final static String TAG = "Gibberbot.OTR";

    public static void log(String msg) {
        if (Debug.DEBUG_ENABLED && Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, LogCleaner.clean(msg));
    }

    public static void log(String msg, Exception e) {
        if (Debug.DEBUG_ENABLED)
            Log.e(TAG, LogCleaner.clean(msg), e);
    }
}
