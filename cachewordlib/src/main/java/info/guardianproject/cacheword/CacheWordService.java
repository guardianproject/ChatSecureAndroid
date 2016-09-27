
package info.guardianproject.cacheword;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Observable;
import java.util.Observer;

public class CacheWordService extends Service implements Observer {

    private final static String TAG = "CacheWordService";

    private final IBinder mBinder = new CacheWordBinder();

    private ICachedSecrets mSecrets = null;

    private PendingIntent mTimeoutIntent;
    private Intent mBroadcastIntent = new Intent(Constants.INTENT_NEW_SECRETS);

    private int mSubscriberCount = 0;
    private boolean mIsForegrounded = false;

    private CacheWordSettings mSettings = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.d(TAG, "onStart: null intent");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (action == null) {
            Log.d(TAG, "onStart: null action");
            return START_NOT_STICKY;
        }

        Log.d(TAG, "onStart: with intent " + action);

        if (action.equals(Constants.INTENT_PASS_EXPIRED)) {
            Log.d(TAG, "onStart: LOCK COMMAND received..locking");
            expirePassphrase();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mSettings = new CacheWordSettings(this);
        mSettings.addObserver(this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved()");
        if (!mIsForegrounded) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(Constants.SERVICE_BACKGROUND_ID);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSecrets != null) {
            Log.d(TAG, "onDestroy() killed secrets");
            mSecrets.destroy();
            mSecrets = null;
        } else {
            Log.d(TAG, "onDestroy() secrets already null");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // note: this method is called when ALL clients
        // have unbound, and not per-client.
        resetTimeout();
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // note: this method is called on the first binding
        // not per-client
        return mBinder;
    }

    // API for Clients
    // //////////////////////////////////////

    public synchronized ICachedSecrets getCachedSecrets() {
        return mSecrets;
    }

    public synchronized void setCachedSecrets(ICachedSecrets secrets) {
        Log.d(TAG, "setCachedSecrets()");
        mSecrets = secrets;

        handleNewSecrets(true);
    }

    public CacheWordSettings settings() {
        return mSettings;
    }

    public void setSettings(CacheWordSettings settings) {
        mSettings.updateWith(settings);
    }

    public synchronized boolean isLocked() {
        return mSecrets == null;
    }

    public void manuallyLock() {
        expirePassphrase();
    }

    public synchronized void attachSubscriber() {
        mSubscriberCount++;
        Log.d(TAG, "attachSubscriber(): " + mSubscriberCount);
        resetTimeout();
    }

    public synchronized void detachSubscriber() {
        mSubscriberCount--;
        Log.d(TAG, "detachSubscriber(): " + mSubscriberCount);
        resetTimeout();
    }

    // / private methods
    // ////////////////////////////////////

    private void handleNewSecrets(boolean notify) {

        if (!SecretsManager.isInitialized(this)) {
            return;
        }

        if (shouldForeground())
            goForeground();
        else
            goBackground();
        resetTimeout();
        if (notify)
            LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);
    }

    private void expirePassphrase() {
        Log.d(TAG, "expirePassphrase");

        synchronized (this) {
            if (mSecrets != null) {
                mSecrets.destroy();
                mSecrets = null;
            }
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(mBroadcastIntent);

        if (mIsForegrounded) {
            stopForeground(true);
            mIsForegrounded = false;
        } else {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(Constants.SERVICE_BACKGROUND_ID);
        }
        stopSelf();
    }

    private void resetTimeout() {
        int timeoutSeconds = mSettings.getTimeoutSeconds();
        boolean timeoutEnabled = timeoutSeconds >= 0;

        Log.d(TAG, "timeout enabled: " + timeoutEnabled + ", seconds=" + timeoutSeconds);
        Log.d(TAG, "mSubscriberCount: " + mSubscriberCount);

        if (timeoutEnabled && mSubscriberCount == 0) {
            startTimeout(timeoutSeconds * 1000);
        } else {
            Log.d(TAG, "disabled timeout alarm");
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.cancel(mTimeoutIntent);
        }
    }

    /**
     * @param milliseconds timeout interval in milliseconds
     */
    private void startTimeout(long milliseconds) {
        if (milliseconds <= 0) {
            Log.d(TAG, "immediate timeout");
            expirePassphrase();
            return;
        }
        Log.d(TAG, "starting timeout: " + milliseconds);

        if (mTimeoutIntent == null) {
            Intent passExpiredIntent = CacheWordService
                    .getBlankServiceIntent(getApplicationContext());
            passExpiredIntent.setAction(Constants.INTENT_PASS_EXPIRED);
            mTimeoutIntent = PendingIntent.getService(getApplicationContext(), 0,
                    passExpiredIntent, 0);
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + milliseconds, mTimeoutIntent);

    }

    private Notification buildNotification() {

        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setSmallIcon(R.drawable.cacheword_notification_icon);
        b.setContentTitle(getText(R.string.cacheword_notification_cached_title));
        b.setContentText(getText(R.string.cacheword_notification_cached_message));
        b.setTicker(getText(R.string.cacheword_notification_cached));
        if (mSettings.getVibrateSetting())
            b.setDefaults(Notification.DEFAULT_VIBRATE);
        b.setWhen(System.currentTimeMillis());
        b.setOngoing(true);

        PendingIntent i = null;
        if (mSettings.getNotificationIntent() != null) {
            Log.d(TAG, "non-default NotificationItent found!");
            i = mSettings.getNotificationIntent();
        } else {
            Intent notificationIntent = CacheWordService
                    .getBlankServiceIntent(getApplicationContext());
            Log.d(TAG, "using default NotificationItent (lock app)");
            notificationIntent.setAction(Constants.INTENT_PASS_EXPIRED);
            i = PendingIntent.getService(getApplicationContext(), 0, notificationIntent, 0);
        }
        b.setContentIntent(i);

        return b.build();
    }

    private void goForeground() {
        Log.d(TAG, "goForeground()");

        stopForeground(true);
        startForeground(Constants.SERVICE_FOREGROUND_ID, buildNotification());
        mIsForegrounded = true;
    }

    private void goBackground() {
        Log.d(TAG, "goBackground()");

        if (mIsForegrounded) {
            stopForeground(true);
            mIsForegrounded = false;
        }

        if (mSettings.getNotificationEnabled()) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(Constants.SERVICE_BACKGROUND_ID, buildNotification());
        }

    }

    public class CacheWordBinder extends Binder implements ICacheWordBinder {

        @Override
        public CacheWordService getService() {
            Log.d("CacheWordBinder", "giving service");
            return CacheWordService.this;
        }
    }

    /**
     * Create a blank intent to start the CachewordService Blank means only the
     * Component field is set
     * 
     * @param applicationContext
     */
    static public Intent getBlankServiceIntent(Context applicationContext) {
        Intent i = new Intent();
        i.setClassName(applicationContext, Constants.SERVICE_CLASS_NAME);
        return i;
    }

    private boolean shouldForeground() {
        return getSharedPreferences(Constants.SHARED_PREFS, 0).getBoolean(
                Constants.SHARED_PREFS_FOREGROUND, false);
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable == mSettings) {
            resetTimeout();
            // update backgrounding & notification without alerting the
            // subscribers
            handleNewSecrets(false);
        }
    }
}
