package info.guardianproject.otr.app.im.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

/**
 * This service exists because a foreground service receiving a wakeup alarm from the OS will cause
 * the service process to lose its foreground status and be killed.  This service runs in the UI process instead.
 * 
 * @author devrandom
 *
 */
public class HeartbeatService extends Service {
    private static final String TAG = "GB.HeartbeatService";
    private PendingIntent mPendingIntent;
    private Intent mRelayIntent;
    public static final String HEARTBEAT_ACTION = "info.guardianproject.otr.app.im.SERVICE.HEARTBEAT";

    // Our heartbeat interval in seconds.
    // The user controlled preference heartbeat interval is in these units (i.e. minutes).
    private static final long HEARTBEAT_INTERVAL = 1000 * 60;


    @Override
    public void onCreate() {
        super.onCreate();
        this.mPendingIntent = PendingIntent.getService(this, 0, new Intent(HEARTBEAT_ACTION, null,
                this, HeartbeatService.class), 0);
        this.mRelayIntent = new Intent(HEARTBEAT_ACTION, null, this, RemoteImService.class);
        startHeartbeat(HEARTBEAT_INTERVAL);
    }
    
    void startHeartbeat(long interval) {
        AlarmManager alarmManager = (AlarmManager)this.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(mPendingIntent);
        if (interval > 0)
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, mPendingIntent);
    }

    @Override
    public void onDestroy() {
        startHeartbeat(0);
        super.onDestroy();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "HEARTBEAT");
        if (intent != null && HEARTBEAT_ACTION.equals(intent.getAction())) {
            startHeartbeat(HEARTBEAT_INTERVAL);
            startService(mRelayIntent);
        }
        return START_NOT_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public static void startBeating(Context context) {
        context.startService(new Intent(context, HeartbeatService.class));
    }

    public static void stopBeating(Context context) {
        context.stopService(new Intent(context, HeartbeatService.class));
    }
}
