package info.guardianproject.otr;

import android.util.Log;

public class OtrDebugLogger {


	private final static String TAG = "Gibberbot.OTR";
	public static boolean debugLog = false;
	
	public static void log (String msg)
	{
		if (debugLog)
			Log.d(TAG, msg);
	}
	
	public static void log (String msg, Exception e)
	{
		if (debugLog)
			Log.e(TAG, msg, e);
	}
}
