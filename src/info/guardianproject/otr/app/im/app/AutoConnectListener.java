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
  //  static boolean firstCall = true;
    public final static String BOOTFLAG = "BOOTFLAG";
    
    @Override
    public synchronized void onReceive(Context context, Intent intent) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean hasConnectivity = !intent.getBooleanExtra(
                ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        
        boolean prefStartOnBoot = prefs.getBoolean("pref_start_on_boot", true); 
        boolean hasBgData = ImApp.getApplication().isBackgroundDataEnabled();
        
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            if (prefStartOnBoot)
            {
                ImApp.getApplication().startImServiceIfNeed(true);
            }
            
            return;
        }
        
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            
            if (hasConnectivity) {
                
                if (ImApp.getApplication().hasActiveConnections() && hasBgData)
                {
                    ImApp.getApplication().startImServiceIfNeed(true);
                
                }
                else
                {
                    android.os.Process.killProcess(android.os.Process.myPid()); 
                    System.exit(0);
                    
                }
            }
        }
        
    }
    
    /*
    public static void setBootFlag (Context context) throws IOException
    {
        File file = new File(context.getFilesDir(),BOOTFLAG);
        file.createNewFile();
    }
    
    public static boolean hasBootFlag (Context context)
    {
        return new File(context.getFilesDir(),BOOTFLAG).exists();
    }
    
    public static void clearBootFlag (Context context)
    {
        File file = new File(context.getFilesDir(),AutoConnectListener.BOOTFLAG);
        if (file.exists())
            file.delete();
    }
    */
}
