package info.guardianproject.otr.app.im.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Automatically initiate the service and connect when the network comes on,
 * including on boot.
 */
public class AutoConnectListener extends BroadcastReceiver {
    private static final String TAG = "Gibberbot.AutoConnectListener";
    static boolean firstCall = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AppConnectivityListener");
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            
            boolean noConnectivity = intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            boolean prefStartOnBoot = prefs.getBoolean("pref_start_on_boot", true); 
            
            Log.d(TAG, "autostart IM service firstCall=" + firstCall + " noconn=" + noConnectivity);
            if (firstCall && !noConnectivity && prefStartOnBoot) {
                ImApp.getApplication().startImServiceIfNeed(true);
                firstCall = false;
            }
        }
    }
}
