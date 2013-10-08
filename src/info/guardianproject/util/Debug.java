package info.guardianproject.util;

import android.os.StrictMode;

public class Debug {
    public static final boolean DEBUG_ENABLED = false;
    public static final boolean DEBUGGER_ATTACH_ENABLED = false;
    public static final boolean DEBUG_INJECT_ERRORS = false;
    private static int injectCount = 0;

    public static void onConnectionStart() {
        if (DEBUG_ENABLED) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build());
        }
    }
    
    public static void onAppStart() {
        // Same StrictMode policy
        onConnectionStart();
    }

    public static void onServiceStart() {
        if (DEBUGGER_ATTACH_ENABLED)
            android.os.Debug.waitForDebugger();
    }

    public static void onHeartbeat() {
        if (DEBUG_ENABLED)
            System.gc();
    }

    public static String injectErrors(String body) {
        if (!DEBUG_INJECT_ERRORS)
            return body;
        // Inject an error every few blocks
        if (++injectCount % 5 == 0 && body.length() > 5)
            body = body.substring(0, 5) + 'X' + body.substring(6);
        return body;
    }
}
