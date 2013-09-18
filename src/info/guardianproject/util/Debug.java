package info.guardianproject.util;

import android.os.StrictMode;

public class Debug {
    public static final boolean DEBUG_ENABLED = false;
    public static final boolean DEBUGGER_ATTACH_ENABLED = false;

    public static void onConnectionStart() {
        if (DEBUG_ENABLED) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build());
        }
    }

    public static void onServiceStart() {
        if (DEBUGGER_ATTACH_ENABLED)
            android.os.Debug.waitForDebugger();
    }

    public static void onHeartbeat() {
        if (DEBUG_ENABLED)
            System.gc();
    }
}
