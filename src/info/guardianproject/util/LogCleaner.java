package info.guardianproject.util;

import java.net.URLEncoder;

import android.util.Log;

public class LogCleaner {
    private static final boolean DEBUG = false;

    public static String clean (String msg)
    {
        if (DEBUG)
            return msg;
        else
            return URLEncoder.encode(msg);
    }
    
    public static void warn (String tag, String msg)
    {
        Log.w(tag, clean(msg));
    }
    
    public static void debug (String tag, String msg)
    {
        Log.d(tag, clean(msg));
    }
    
    public static void error (String tag, String msg, Exception e)
    {
        Log.e(tag, clean(msg),e);
    }
    

    public static void error (String tag, String msg, Throwable e)
    {
        Log.e(tag, clean(msg),e);
    }
}
