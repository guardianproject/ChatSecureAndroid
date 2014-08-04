package info.guardianproject.otr;

import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.util.LogCleaner;
import android.util.Log;

public class OtrDebugLogger {

    public static void log(String msg) {
    //    if (Debug.DEBUG_ENABLED && Log.isLoggable(TAG, Log.DEBUG))
            Log.d(ImApp.LOG_TAG, LogCleaner.clean(msg));
    }

    public static void log(String msg, Exception e) {
        Log.e(ImApp.LOG_TAG, LogCleaner.clean(msg), e);
    }
}
