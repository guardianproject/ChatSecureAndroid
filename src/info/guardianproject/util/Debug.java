package info.guardianproject.util;

public class Debug {
    public static final boolean DEBUG_ENABLED = true;

    public static void onConnectionStart() {
        if (DEBUG_ENABLED)
            android.os.Debug.waitForDebugger();
    }

    public static void onServiceStart() {
        if (DEBUG_ENABLED)
            android.os.Debug.waitForDebugger();
    }
}
