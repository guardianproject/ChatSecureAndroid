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
public class BootCompletedListener extends BroadcastReceiver {
    
    public final static String BOOTFLAG = "BOOTFLAG";
    
    @Override
    public synchronized void onReceive(Context context, Intent intent) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        boolean prefStartOnBoot = prefs.getBoolean("pref_start_on_boot", true); 
        
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            if (prefStartOnBoot)
            {
              //  ImApp.getApplication().startImServiceIfNeed(true);
            }
            else
            {
                /*
                Log.d(ImApp.LOG_TAG,"killing auto-connect process");
                android.os.Process.killProcess(android.os.Process.myPid()); 
                System.exit(0);
                */
            }
        }
        
       
        
    }
    
   
}
