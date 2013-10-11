package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.StatusBarNotifier;
import info.guardianproject.util.Debug;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
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
            Debug.onServiceStart();
            if (prefStartOnBoot)
            {
                if (Imps.isUnencrypted(context))
                {
                    Log.d(ImApp.LOG_TAG, "autostart");
                    new ImApp(context).startImServiceIfNeed(true);
                    Log.d(ImApp.LOG_TAG, "autostart done");
                }
                else
                {
                    //show unlock notification
                    StatusBarNotifier sbn = new StatusBarNotifier(context);
                    sbn.notifyLocked();
                }
            }
        }
        
       
        
    }
    
    
    
   
}
