package info.guardianproject.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.content.Context;
import java.io.PrintWriter;

import org.apache.commons.io.output.StringBuilderWriter;

import android.os.StrictMode;

public class Debug {
    
    public static boolean DEBUG_ENABLED = false;
    public static final boolean DEBUGGER_ATTACH_ENABLED = false;
    public static final boolean DEBUG_INJECT_ERRORS = false;
    private static int injectCount = 0;

    public static void recordTrail(Context context, String key, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        recordTrail(context, key, sdf.format(date));
    }

    public static String getTrail(Context context) {
        File trail = new File(context.getFilesDir(), "trail.properties");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(trail));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
            reader.close();
            return builder.toString();
        } catch (IOException e) {
            return "#notrail";
        }
    }
    
    public static void recordTrail(Context context, String key, String value) {
        File trail = new File(context.getFilesDir(), "trail.properties");
        Properties props = new Properties();
        try {
            FileReader reader = new FileReader(trail);
            props.load(reader);
            reader.close();
        } catch (IOException e) {
            // ignore
        }
        
        try {
            FileWriter writer = new FileWriter(trail);
            props.put(key, value);
            props.store(writer, "ChatSecure debug trail file");
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String getTrail(Context context, String key) {
        File trail = new File(context.getFilesDir(), "trail.properties");
        Properties props = new Properties();
        try {
            FileReader reader = new FileReader(trail);
            props.load(reader);
            reader.close();
            return props.getProperty(key);
        } catch (IOException e) {
            return null;
        }
    }
    
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

    static public void wrapExceptions(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            StringBuilderWriter writer = new StringBuilderWriter();
            PrintWriter pw = new PrintWriter(writer, true);
            t.printStackTrace(pw);
            writer.flush();
            throw new IllegalStateException("service throwable: " + writer.getBuilder().toString());
        }
    }
}
