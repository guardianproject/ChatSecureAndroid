/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package info.guardianproject.otr.app.im.service;

import info.guardianproject.otr.IOtrKeyManager;
import info.guardianproject.otr.OtrAndroidKeyManagerImpl;
import info.guardianproject.otr.OtrChatManager;
import info.guardianproject.otr.OtrDebugLogger;
import info.guardianproject.otr.app.im.IConnectionCreationListener;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.IRemoteImService;
import info.guardianproject.otr.app.im.ImService;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.DummyActivity;
import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.app.ImPluginHelper;
import info.guardianproject.otr.app.im.app.NetworkConnectivityListener.State;
import info.guardianproject.otr.app.im.app.NewChatActivity;
import info.guardianproject.otr.app.im.engine.ConnectionFactory;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImException;
import info.guardianproject.otr.app.im.plugin.ImPluginInfo;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.util.Debug;
import info.guardianproject.util.LogCleaner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.java.otr4j.OtrEngineListener;
import net.java.otr4j.OtrKeyManager;
import net.java.otr4j.OtrKeyManagerListener;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class RemoteImService extends Service implements OtrEngineListener, ImService {

    private static final String PREV_CONNECTIONS_TRAIL_TAG = "prev_connections";
    private static final String CONNECTIONS_TRAIL_TAG = "connections";
    private static final String LAST_SWIPE_TRAIL_TAG = "last_swipe";
    private static final String SERVICE_DESTROY_TRAIL_TAG = "service_destroy";
    private static final String PREV_SERVICE_CREATE_TRAIL_TAG = "prev_service_create";
    private static final String SERVICE_CREATE_TRAIL_KEY = "service_create";
    private static final String[] ACCOUNT_PROJECTION = { Imps.Account._ID, Imps.Account.PROVIDER,
                                                        Imps.Account.USERNAME,
                                                        Imps.Account.PASSWORD, };
    // TODO why aren't these Imps.Account.* values?
    private static final int ACCOUNT_ID_COLUMN = 0;
    private static final int ACCOUNT_PROVIDER_COLUMN = 1;
    private static final int ACCOUNT_USERNAME_COLUMN = 2;
    private static final int ACCOUNT_PASSOWRD_COLUMN = 3;

    private static final int EVENT_SHOW_TOAST = 100;
    
    private StatusBarNotifier mStatusBarNotifier;
    private Handler mServiceHandler;
    private int mNetworkType;
    private boolean mNeedCheckAutoLogin;

    //private SettingsMonitor mSettingsMonitor;
    private OtrChatManager mOtrChatManager;

    private ImPluginHelper mPluginHelper;
    private Hashtable<String, ImConnectionAdapter> mConnections;

    private Imps.ProviderSettings.QueryMap mGlobalSettings;
    private Handler mHandler;

    final RemoteCallbackList<IConnectionCreationListener> mRemoteListeners = new RemoteCallbackList<IConnectionCreationListener>();
    public long mHeartbeatInterval;
    private WakeLock mWakeLock;
    private State mNetworkState;

    private static final String TAG = "GB.ImService";

    public long getHeartbeatInterval() {
        return mHeartbeatInterval;
    }

    public static void debug(String msg) {
        LogCleaner.debug(TAG, msg);
    }

    public static void debug(String msg, Exception e) {
        LogCleaner.error(TAG, msg, e);
    }

    private synchronized void initOtr() {
        int otrPolicy = convertPolicy();

        if (mOtrChatManager == null) {

            try {
                OtrKeyManager otrKeyManager = OtrAndroidKeyManagerImpl.getInstance(this);
                if (otrKeyManager != null)
                {
                    // TODO OTRCHAT add support for more than one connection type (this is a kludge)
                    mOtrChatManager = OtrChatManager.getInstance(otrPolicy, this, otrKeyManager);
                    mOtrChatManager.addOtrEngineListener(this);
                    
                    otrKeyManager.addListener(new OtrKeyManagerListener() {
                        public void verificationStatusChanged(SessionID session) {
                            boolean isVerified = mOtrChatManager.getKeyManager().isVerified(session);
                            String msg = session + ": verification status=" + isVerified;
        
                            OtrDebugLogger.log(msg);

                        }

                        public void remoteVerifiedUs(SessionID session) {
                            String msg = session + ": remote verified us";
                            OtrDebugLogger.log(msg);
                            
                            showToast(getString(R.string.remote_verified_us),Toast.LENGTH_SHORT);
                         //   if (!isRemoteKeyVerified(session))
                           //     showWarning(session, mContext.getApplicationContext().getString(R.string.remote_verified_us));
                        }
                    });
                }
                else
                {
                    throw new RuntimeException("could not instantiate OTR manager");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            mOtrChatManager.setPolicy(otrPolicy);
        }
    }

    private int convertPolicy() {
        int otrPolicy = OtrPolicy.OPPORTUNISTIC;

        String otrModeSelect = getGlobalSettings().getOtrMode();

        if (otrModeSelect.equals("auto")) {
            otrPolicy = OtrPolicy.OPPORTUNISTIC;
        } else if (otrModeSelect.equals("disabled")) {
            otrPolicy = OtrPolicy.NEVER;

        } else if (otrModeSelect.equals("force")) {
            otrPolicy = OtrPolicy.OTRL_POLICY_ALWAYS;

        } else if (otrModeSelect.equals("requested")) {
            otrPolicy = OtrPolicy.OTRL_POLICY_MANUAL;
        }
        return otrPolicy;
    }

    private Imps.ProviderSettings.QueryMap getGlobalSettings() {
        if (mGlobalSettings == null) {
            mGlobalSettings = new Imps.ProviderSettings.QueryMap(getContentResolver(), true,
                    mHandler);
        }
        return mGlobalSettings;
    }

    @Override
    public void onCreate() {
        debug("ImService started");
        final String prev = Debug.getTrail(this, SERVICE_CREATE_TRAIL_KEY);
        if (prev != null)
            Debug.recordTrail(this, PREV_SERVICE_CREATE_TRAIL_TAG, prev);
        Debug.recordTrail(this, SERVICE_CREATE_TRAIL_KEY, new Date());
        final String prevConnections = Debug.getTrail(this, CONNECTIONS_TRAIL_TAG);
        if (prevConnections != null)
            Debug.recordTrail(this, PREV_CONNECTIONS_TRAIL_TAG, prevConnections);
        Debug.recordTrail(this, CONNECTIONS_TRAIL_TAG, "0");
        
        mConnections = new Hashtable<String, ImConnectionAdapter>();
        mHandler = new Handler();
        
        Debug.onServiceStart();

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IM_WAKELOCK");

        // Clear all account statii to logged-out, since we just got started and we don't want
        // leftovers from any previous crash.
        clearConnectionStatii();
        
        mStatusBarNotifier = new StatusBarNotifier(this);
        mServiceHandler = new ServiceHandler();

        //mSettingsMonitor = new SettingsMonitor();

        /*
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED);
        registerReceiver(mSettingsMonitor, intentFilter);
        */
        
      //  setBackgroundData(ImApp.getApplication().isNetworkAvailableAndConnected());
       
        mPluginHelper = ImPluginHelper.getInstance(this);
        mPluginHelper.loadAvailablePlugins();

        // Have the heartbeat start autoLogin, unless onStart turns this off
        mNeedCheckAutoLogin = true;
                
        HeartbeatService.startBeating(getApplicationContext());
    }
    
    private void startForegroundCompat() {
        Notification notification = new Notification(R.drawable.notify_chatsecure, getString(R.string.app_name),
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, NewChatActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent launchIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
        notification.setLatestEventInfo(getApplicationContext(),
                getString(R.string.app_name),
                getString(R.string.app_unlocked),
                launchIntent);
        
        stopForeground(true);
        startForeground(1000, notification);
    }

    public void sendHeartbeat() {
        Debug.onHeartbeat();
        try {
            if (mNeedCheckAutoLogin && mNetworkState != State.NOT_CONNECTED) {
                debug("autoLogin from heartbeat");
                mNeedCheckAutoLogin = false;
                autoLogin();
            }

            mHeartbeatInterval = getGlobalSettings().getHeartbeatInterval();
            
            for (ImConnectionAdapter conn : mConnections.values())
            {
                conn.sendHeartbeat();
            }
        } finally {
            return;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        if (intent != null && HeartbeatService.HEARTBEAT_ACTION.equals(intent.getAction())) {
            Log.d(TAG, "HEARTBEAT");
            try {
                mWakeLock.acquire();
                sendHeartbeat();
            } finally {
                mWakeLock.release();
            }
            return START_STICKY;
        }
        
        if (intent != null && HeartbeatService.NETWORK_STATE_ACTION.equals(intent.getAction())) {
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(HeartbeatService.NETWORK_INFO_EXTRA);
            State networkState = State.values()[intent.getIntExtra(HeartbeatService.NETWORK_STATE_EXTRA, 0)];
            // TODO(miron) wakelock?
            networkStateChanged(networkInfo, networkState);
            return START_STICKY;
        }


        if (intent != null && intent.hasExtra(ImServiceConstants.EXTRA_CHECK_AUTO_LOGIN))
            mNeedCheckAutoLogin = intent.getBooleanExtra(ImServiceConstants.EXTRA_CHECK_AUTO_LOGIN,
                false);
        else
            mNeedCheckAutoLogin = true;
        
        debug("ImService.onStart, checkAutoLogin=" + mNeedCheckAutoLogin + " intent =" + intent
              + " startId =" + startId);

        // Check and login accounts if network is ready, otherwise it's checked
        // when the network becomes available.
        if (mNeedCheckAutoLogin && mNetworkState != State.NOT_CONNECTED) {
            mNeedCheckAutoLogin = false;
            autoLogin();
        }
        
        return START_STICKY;
    }
    
    

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    
    private void clearConnectionStatii() {
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues(2);

        values.put(Imps.AccountStatus.PRESENCE_STATUS, Imps.Presence.OFFLINE);
        values.put(Imps.AccountStatus.CONNECTION_STATUS, Imps.ConnectionStatus.OFFLINE);
        
        try
        {
            //insert on the "account_status" uri actually replaces the existing value 
            cr.update(Imps.AccountStatus.CONTENT_URI, values, null, null);
        }
        catch (Exception e)
        {
            //this can throw NPE on restart sometimes if database has not been unlocked
            debug("database is not unlocked yet. caught NPE from mDbHelper in ImpsProvider");
        }
    }


    private void autoLogin() {
        // Try empty passphrase.  We can't autologin if this fails.
        if (!Imps.setEmptyPassphrase(this, true)) {
            debug("Cannot autologin with non-empty passphrase");
            return;
        }
        
        if (!mConnections.isEmpty()) {
            // This can happen because the UI process may be restarted and may think that we need
            // to autologin, while we (the Service process) are already up.
            debug("Got autoLogin request, but we have one or more connections");
            return;
        }

        debug("Scanning accounts and login automatically");

        ContentResolver resolver = getContentResolver();

        String where = Imps.Account.KEEP_SIGNED_IN + "=1 AND " + Imps.Account.ACTIVE + "=1";
        Cursor cursor = resolver.query(Imps.Account.CONTENT_URI, ACCOUNT_PROJECTION, where, null,
                null);
        if (cursor == null) {
            Log.w(TAG, "Can't query account!");
            return;
        }
        while (cursor.moveToNext()) {
            long accountId = cursor.getLong(ACCOUNT_ID_COLUMN);
            long providerId = cursor.getLong(ACCOUNT_PROVIDER_COLUMN);
            IImConnection conn = createConnection(providerId, accountId);
            
            try
            {
                if (conn.getState() != ImConnection.LOGGED_IN)
                {
                    try {
                        conn.login(null, true, true);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Logging error while automatically login!");
                    }
                }
            }
            catch (Exception e){
                Log.d(ImApp.LOG_TAG,"error auto logging into ImConnection",e);
            }
        }
        cursor.close();
    }

    private Map<String, String> loadProviderSettings(long providerId) {
        ContentResolver cr = getContentResolver();
        Map<String, String> settings = Imps.ProviderSettings.queryProviderSettings(cr, providerId);

//        NetworkInfo networkInfo = mNetworkConnectivityListener.getNetworkInfo();
        // Insert a fake msisdn on emulator. We don't need this on device
        // because the mobile network will take care of it.
        //        if ("1".equals(SystemProperties.get("ro.kernel.qemu"))) {
        /*
        if (false) {
            settings.put(ImpsConfigNames.MSISDN, "15555218135");
        } else if (networkInfo != null
                && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            if (!TextUtils.isEmpty(settings.get(ImpsConfigNames.SMS_ADDR))) {
                // Send authentication through sms if SMS data channel is
                // supported and WiFi is used.
                settings.put(ImpsConfigNames.SMS_AUTH, "true");
                settings.put(ImpsConfigNames.SECURE_LOGIN, "false");
            } else {
                // Wi-Fi network won't insert a MSISDN, we should get from the SIM
                // card. Assume we can always get the correct MSISDN from SIM, otherwise,
                // the sign in would fail and an error message should be shown to warn
                // the user to contact their operator.
                String msisdn = ""; // TODO TelephonyManager.getDefault().getLine1Number();
                if (TextUtils.isEmpty(msisdn)) {
                    Log.w(TAG, "Can not read MSISDN from SIM, use a fake one."
                         + " SMS related feature won't work.");
                    msisdn = "15555218135";
                }
                settings.put(ImpsConfigNames.MSISDN, msisdn);
            }
        }*/

        return settings;
    }

    @Override
    public void onDestroy() {
        Debug.recordTrail(this, SERVICE_DESTROY_TRAIL_TAG, new Date());
        
        HeartbeatService.stopBeating(getApplicationContext());
        
        Log.w(TAG, "ImService stopped.");
        for (ImConnectionAdapter conn : mConnections.values()) {
            conn.logout();
        }
        
        if (mUseForeground)
            stopForeground(true);

        if (mGlobalSettings != null)
            mGlobalSettings.close();
     
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void showToast(CharSequence text, int duration) {
        Message msg = Message.obtain(mServiceHandler, EVENT_SHOW_TOAST, duration, 0, text);
        msg.sendToTarget();
    }

    public StatusBarNotifier getStatusBarNotifier() {
        return mStatusBarNotifier;
    }

    public OtrChatManager getOtrChatManager() {
        initOtr() ;
        return mOtrChatManager;
    }

    public void scheduleReconnect(long delay) {
        if (!isNetworkAvailable()) {
            // Don't schedule reconnect if no network available. We will try to
            // reconnect when network state become CONNECTED.
            return;
        }
        mServiceHandler.postDelayed(new Runnable() {
            public void run() {
                reestablishConnections();
            }
        }, delay);
    }

    IImConnection createConnection(final long providerId, final long accountId) {
        final IImConnection[] results = new IImConnection[1];
        Debug.wrapExceptions(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                results[0] = do_createConnection(providerId, accountId);
            }
        });
        return results[0];
    }

    private boolean mUseForeground = false;
    
    IImConnection do_createConnection(long providerId, long accountId) {
        
        if (mConnections.size() == 0)
        {
            mUseForeground = getGlobalSettings().getUseForegroundPriority();
            
            if (mUseForeground)
                startForegroundCompat();
        }
        
        Map<String, String> settings = loadProviderSettings(providerId);
        ConnectionFactory factory = ConnectionFactory.getInstance();
        try {
            ImConnection conn = factory.createConnection(settings, this);
            conn.initUser(providerId, accountId);
            ImConnectionAdapter imConnectionAdapter = 
                    new ImConnectionAdapter(providerId, accountId, conn, this);
            
            ContentResolver contentResolver = getContentResolver();
            Imps.ProviderSettings.QueryMap providerSettings = new Imps.ProviderSettings.QueryMap(
                    contentResolver, providerId, false, null);
            String userName = Imps.Account.getUserName(contentResolver, accountId);
            String domain = providerSettings.getDomain();
            providerSettings.close();
            
            mConnections.put(userName + '@' + domain,imConnectionAdapter);
            Debug.recordTrail(this, CONNECTIONS_TRAIL_TAG, "" + mConnections.size());

            initOtr();

            final int N = mRemoteListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                IConnectionCreationListener listener = mRemoteListeners.getBroadcastItem(i);
                try {
                    listener.onConnectionCreated(imConnectionAdapter);
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing the
                    // dead listeners.
                }
            }

            mRemoteListeners.finishBroadcast();

            return imConnectionAdapter;
        } catch (ImException e) {
            debug("Error creating connection", e);
            return null;
        }
    }

    void removeConnection(ImConnectionAdapter connection) {
        
        mConnections.remove(connection);
        
        if (mConnections.size() == 0)
            if (getGlobalSettings().getUseForegroundPriority())
                stopForeground(true);
    }

    private boolean isNetworkAvailable() {
        return mNetworkState == State.CONNECTED;
    }

    void networkStateChanged(NetworkInfo networkInfo, State networkState) {
        mNetworkState = networkState;

        debug("networkStateChanged:" + mNetworkState);

        int oldType = mNetworkType;
        mNetworkType = networkInfo != null ? networkInfo.getType() : -1;

        // Notify the connection that network type has changed. Note that this
        // only work for connected connections, we need to reestablish if it's
        // suspended.
        if (mNetworkType != oldType && isNetworkAvailable()) {
            for (ImConnectionAdapter conn : mConnections.values()) {
                conn.networkTypeChanged();
            }
        }

        NetworkInfo.State state = networkInfo != null ? networkInfo.getState() : NetworkInfo.State.DISCONNECTED;
        switch (state) {
        case CONNECTED:
            if (mNeedCheckAutoLogin) {
                mNeedCheckAutoLogin = false;
                autoLogin();
                break;
            }
            reestablishConnections();
            break;

        case DISCONNECTED:
            if (!isNetworkAvailable()) {
                suspendConnections();
            }
            break;
            // TODO(miron) what about DISCONNECTING?
        default:
            break;
        }
    }

    // package private for inner class access
    void reestablishConnections() {
        if (!isNetworkAvailable()) {
            return;
        }

        for (ImConnectionAdapter conn : mConnections.values()) {
            int connState = conn.getState();
            if (connState == ImConnection.SUSPENDED) {
                conn.reestablishSession();
            }
        }

    }

    private void suspendConnections() {
        for (ImConnectionAdapter conn : mConnections.values()) {
            if (conn.getState() != ImConnection.LOGGED_IN) {
                continue;
            }
            conn.suspend();

        }

    }
    

    public ImConnectionAdapter getConnection(String username) {
       return mConnections.get(username);
    }


    private final IRemoteImService.Stub mBinder = new IRemoteImService.Stub() {

        @Override
        public List<ImPluginInfo> getAllPlugins() {
            return new ArrayList<ImPluginInfo>(mPluginHelper.getPluginsInfo());
        }

        @Override
        public void addConnectionCreatedListener(IConnectionCreationListener listener) {
            if (listener != null) {
                mRemoteListeners.register(listener);
            }
        }

        @Override
        public void removeConnectionCreatedListener(IConnectionCreationListener listener) {
            if (listener != null) {
                mRemoteListeners.unregister(listener);
            }
        }

        @Override
        public IImConnection createConnection(long providerId, long accountId) {
            return RemoteImService.this.createConnection(providerId, accountId);
        }

        @Override
        public List getActiveConnections() {
            ArrayList<IBinder> result = new ArrayList<IBinder>(mConnections.size());
            for (IImConnection conn : mConnections.values()) {
                result.add(conn.asBinder());
            }
            return result;
        }
        
        @Override
        public void dismissNotifications(long providerId) {
            mStatusBarNotifier.dismissNotifications(providerId);
        }

        @Override
        public void dismissChatNotification(long providerId, String username) {
            mStatusBarNotifier.dismissChatNotification(providerId, username);
        }
        
        @Override
        public boolean unlockOtrStore (String password) 
        {
            OtrAndroidKeyManagerImpl.setKeyStorePassword(password);
            return true;
        }
        
        @Override
        public IOtrKeyManager getOtrKeyManager ()
        {
            try {
                return OtrAndroidKeyManagerImpl.getInstance(RemoteImService.this);
            } catch (IOException e) {
                
                OtrDebugLogger.log("unable to get keymanager instance", e);

                return null;
            }
            
        }
        
        
        @Override
        public void setKillProcessOnStop (boolean killProcessOnStop)
        {
            mKillProcessOnStop = killProcessOnStop;
        }
        
        @Override
        public void enableDebugLogging (boolean debug)
        {
            Debug.DEBUG_ENABLED = debug;
        }
    };
    
    private boolean mKillProcessOnStop = false;

    /*
     //the concept of "background data is deprecated from Android
     // the only thing that matters is checking if Network is available and connected
    private final class SettingsMonitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED.equals(action)) {
                ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                setBackgroundData(manager.getBackgroundDataSetting());
                handleBackgroundDataSettingChange();
            }
        }
    }
    */
    private final class ServiceHandler extends Handler {
        public ServiceHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_SHOW_TOAST:
                Toast.makeText(RemoteImService.this, (CharSequence) msg.obj, msg.arg1).show();
                break;

            default:
            }
        }
    }

    @Override
    public void sessionStatusChanged(SessionID sessionID) {

        SessionStatus sStatus = mOtrChatManager.getSessionStatus(sessionID);

        String msg = "";

        if (sStatus == SessionStatus.PLAINTEXT) {
            msg = getString(R.string.otr_session_status_plaintext);

        } else if (sStatus == SessionStatus.ENCRYPTED) {
            msg = getString(R.string.otr_session_status_encrypted);

        } else if (sStatus == SessionStatus.FINISHED) {
            msg = getString(R.string.otr_session_status_finished);
        }

        //showToast(msg, Toast.LENGTH_SHORT);

    }
    
    public void onTaskRemoved(Intent rootIntent) {
        Debug.recordTrail(this, LAST_SWIPE_TRAIL_TAG, new Date());
        Intent intent = new Intent(this, DummyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
