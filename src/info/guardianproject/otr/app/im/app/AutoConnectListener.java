package info.guardianproject.otr.app.im.app;

import java.io.File;
import java.io.IOException;

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

    public final static String BOOTFLAG = "BOOTFLAG";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AppConnectivityListener");
        
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            
            try
            {
                setBootFlag(context);
            }
            catch (Exception e)
            {
                Log.e(TAG, "Unable to set BOOTFLAG file",e);
            }
        }
        
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            
            boolean noConnectivity = intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            boolean prefStartOnBoot = prefs.getBoolean("pref_start_on_boot", true); 
            boolean hasBootFlag = hasBootFlag (context);
            
            Log.d(TAG, "autostart IM service firstCall=" + firstCall + " noconn=" + noConnectivity);
            if (firstCall && !noConnectivity) {
                
                if ((!hasBootFlag) || prefStartOnBoot) //either we have already booted, so let's restart, or we want to start on boot
                {
                    ImApp.getApplication().startImServiceIfNeed(true);
                    firstCall = false;
                }
            }
        }
    }
    
    private void setBootFlag (Context context) throws IOException
    {
        File file = new File(context.getFilesDir(),BOOTFLAG);
        file.createNewFile();
    }
    
    private boolean hasBootFlag (Context context)
    {
        return new File(context.getFilesDir(),BOOTFLAG).exists();
    }
}
