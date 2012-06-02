package info.guardianproject.otr.app.im.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.util.Log;

public class ForegroundStarter {
    private static final String TAG = "ForegroundStarter";
    private static final Class<?>[] mSetForegroundSignature = new Class[] { boolean.class };
    private static final Class<?>[] mStartForegroundSignature = new Class[] { int.class,
                                                                             Notification.class };
    private static final Class<?>[] mStopForegroundSignature = new Class[] { boolean.class };

    private NotificationManager mNM;
    private Method mSetForeground;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mSetForegroundArgs = new Object[1];
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    Service mService;
    private int mNotificationId;

    public ForegroundStarter(Service service) {
        mService = service;
        init();
    }

    void invokeMethod(Method method, Object[] args) {
        try {
            method.invoke(mService, args);
        } catch (InvocationTargetException e) {
            // Should not happen.
            Log.w(TAG, "Unable to invoke method", e);
        } catch (IllegalAccessException e) {
            // Should not happen.
            Log.w(TAG, "Unable to invoke method", e);
        }
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            invokeMethod(mStartForeground, mStartForegroundArgs);
            return;
        }

        // Fall back on the old API.
        mSetForegroundArgs[0] = Boolean.TRUE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
        mNM.notify(id, notification);
        mNotificationId = id;
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    void stopForegroundCompat() {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(mStopForeground, mStopForegroundArgs);
            return;
        }

        // Fall back on the old API. Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(mNotificationId);
        mSetForegroundArgs[0] = Boolean.FALSE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
    }

    public void init() {
        mNM = (NotificationManager) mService.getSystemService(Service.NOTIFICATION_SERVICE);
        try {
            mStartForeground = mService.getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = mService.getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
            return;
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        try {
            mSetForeground = mService.getClass()
                    .getMethod("setForeground", mSetForegroundSignature);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "OS doesn't have Service.startForeground OR Service.setForeground!");
        }
    }
}
