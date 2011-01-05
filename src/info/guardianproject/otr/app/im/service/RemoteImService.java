/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.otr.app.im.service;

import info.guardianproject.otr.app.NetworkConnectivityListener;
import info.guardianproject.otr.app.NetworkConnectivityListener.State;
import info.guardianproject.otr.app.im.ImpsConnectionConfig;
import info.guardianproject.otr.app.im.app.ImPluginHelper;
import info.guardianproject.otr.app.im.engine.ConnectionFactory;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImException;
import info.guardianproject.otr.app.im.engine.HeartbeatService.Callback;
import info.guardianproject.otr.app.im.plugin.ImConfigNames;
import info.guardianproject.otr.app.im.plugin.ImPluginInfo;
import info.guardianproject.otr.app.im.plugin.ImpsConfigNames;
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import info.guardianproject.otr.app.im.IConnectionCreationListener;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.IRemoteImService;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


public class RemoteImService extends Service {

    private static final String[] ACCOUNT_PROJECTION = {
        Imps.Account._ID,
        Imps.Account.PROVIDER,
        Imps.Account.USERNAME,
        Imps.Account.PASSWORD,
    };
    private static final int ACCOUNT_ID_COLUMN = 0;
    private static final int ACCOUNT_PROVIDER_COLUMN = 1;
    private static final int ACCOUNT_USERNAME_COLUMN = 2;
    private static final int ACCOUNT_PASSOWRD_COLUMN = 3;

    static final String TAG = "ImService";

    private static final int EVENT_SHOW_TOAST = 100;
    private static final int EVENT_NETWORK_STATE_CHANGED = 200;
	private static final long HEARTBEAT_INTERVAL = 1000 * 60;

    private StatusBarNotifier mStatusBarNotifier;
    private Handler mServiceHandler;
    NetworkConnectivityListener mNetworkConnectivityListener;
    private int mNetworkType;
    private boolean mNeedCheckAutoLogin;

    private boolean mBackgroundDataEnabled;

    private SettingsMonitor mSettingsMonitor;

    private ImPluginHelper mPluginHelper;
    Vector<ImConnectionAdapter> mConnections;
    final RemoteCallbackList<IConnectionCreationListener> mRemoteListeners
            = new RemoteCallbackList<IConnectionCreationListener>();


    public RemoteImService() {
        mConnections = new Vector<ImConnectionAdapter>();
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "ImService started");
        mStatusBarNotifier = new StatusBarNotifier(this);
        mServiceHandler = new ServiceHandler();
        mNetworkConnectivityListener = new NetworkConnectivityListener();
        mNetworkConnectivityListener.registerHandler(mServiceHandler, EVENT_NETWORK_STATE_CHANGED);
        mNetworkConnectivityListener.startListening(this);

        mSettingsMonitor = new SettingsMonitor();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED);
        registerReceiver(mSettingsMonitor, intentFilter);

        ConnectivityManager manager
            = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        setBackgroundData(manager.getBackgroundDataSetting());

        mPluginHelper = ImPluginHelper.getInstance(this);
        mPluginHelper.loadAvaiablePlugins();
        AndroidSystemService.getInstance().initialize(this);
        AndroidSystemService.getInstance().getHeartbeatService().startHeartbeat(new HeartbeatHandler(), HEARTBEAT_INTERVAL);
        
        // Have the heartbeat start autoLogin, unless onStart turns this off
        mNeedCheckAutoLogin = true;
    }
    
    class HeartbeatHandler implements Callback {

		@SuppressWarnings("finally")
		@Override
		public long sendHeartbeat() {
			try {
				if (mNeedCheckAutoLogin && mNetworkConnectivityListener.getState() != State.NOT_CONNECTED) {
					Log.d(TAG, "autoLogin from heartbeat");
					autoLogin();
				}
				for (Iterator<ImConnectionAdapter> iter = mConnections.iterator(); iter
						.hasNext();) {
					 ImConnectionAdapter conn =  iter.next();
					 conn.sendHeartbeat();
				}
			}
			finally {
				return HEARTBEAT_INTERVAL;
			}
		}
    	
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        mNeedCheckAutoLogin = intent.getBooleanExtra(ImServiceConstants.EXTRA_CHECK_AUTO_LOGIN, false);

        Log.d(TAG, "ImService.onStart, checkAutoLogin=" + mNeedCheckAutoLogin + " intent =" + intent + " startId =" + startId);

        // Check and login accounts if network is ready, otherwise it's checked
        // when the network becomes available.
        if (mNeedCheckAutoLogin &&
                mNetworkConnectivityListener.getState() != State.NOT_CONNECTED) {
            mNeedCheckAutoLogin = false;
            autoLogin();
        }
    }

    private void autoLogin() {
        Log.d(TAG, "Scaning accounts and login automatically");

        ContentResolver resolver = getContentResolver();

        String where = Imps.Account.KEEP_SIGNED_IN + "=1 AND " + Imps.Account.ACTIVE + "=1";
        Cursor cursor = resolver.query(Imps.Account.CONTENT_URI,
                ACCOUNT_PROJECTION, where, null, null);
        if (cursor == null) {
            Log.w(TAG, "Can't query account!");
            return;
        }
        while (cursor.moveToNext()) {
            long accountId = cursor.getLong(ACCOUNT_ID_COLUMN);
            long providerId = cursor.getLong(ACCOUNT_PROVIDER_COLUMN);
            String username = cursor.getString(ACCOUNT_USERNAME_COLUMN);
            String password = cursor.getString(ACCOUNT_PASSOWRD_COLUMN);

            IImConnection conn = createConnection(providerId);

            try {
                conn.login(accountId, username, password, true, true);
            } catch (RemoteException e) {
                Log.w(TAG, "Logging error while automatically login!");
            }
        }
        cursor.close();
    }

    private Map<String, String> loadProviderSettings(long providerId) {
        ContentResolver cr = getContentResolver();
        Map<String, String> settings = Imps.ProviderSettings.queryProviderSettings(cr, providerId);

        NetworkInfo networkInfo = mNetworkConnectivityListener.getNetworkInfo();
        // Insert a fake msisdn on emulator. We don't need this on device
        // because the mobile network will take care of it.
//        if ("1".equals(SystemProperties.get("ro.kernel.qemu"))) {
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
        }
        return settings;
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "ImService stopped.");
        for (ImConnectionAdapter conn : mConnections) {
            conn.logout();
        }

        AndroidSystemService.getInstance().shutdown();

        mNetworkConnectivityListener.unregisterHandler(mServiceHandler);
        mNetworkConnectivityListener.stopListening();
        mNetworkConnectivityListener = null;

        unregisterReceiver(mSettingsMonitor);
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

    IImConnection createConnection(long providerId) {
        Map<String, String> settings = loadProviderSettings(providerId);
        ConnectionFactory factory = ConnectionFactory.getInstance();
        try {
            ImConnection conn = factory.createConnection(settings);
            ImConnectionAdapter result = new ImConnectionAdapter(providerId,
                    conn, this);
            mConnections.add(result);

            final int N = mRemoteListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                IConnectionCreationListener listener = mRemoteListeners.getBroadcastItem(i);
                try {
                    listener.onConnectionCreated(result);
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing the
                    // dead listeners.
                }
            }
            mRemoteListeners.finishBroadcast();
            return result;
        } catch (ImException e) {
            Log.e(TAG, "Error creating connection", e);
            return null;
        }
    }

    void removeConnection(IImConnection connection) {
        mConnections.remove(connection);
    }

    private boolean isNetworkAvailable() {
        return mNetworkConnectivityListener.getState() == State.CONNECTED;
    }

    private boolean isBackgroundDataEnabled() {
        return mBackgroundDataEnabled;
    }

    private void setBackgroundData(boolean flag) {
        mBackgroundDataEnabled = flag;
    }

    void handleBackgroundDataSettingChange(){
        if (!isBackgroundDataEnabled()) {
            for (ImConnectionAdapter conn : mConnections) {
                conn.logout();
            }
        }
    }

    void networkStateChanged() {
        if (mNetworkConnectivityListener == null) {
            return;
        }
        NetworkInfo networkInfo = mNetworkConnectivityListener.getNetworkInfo();
        NetworkInfo.State state = networkInfo.getState();

        Log.d(TAG, "networkStateChanged:" + state);

        int oldType = mNetworkType;
        mNetworkType = networkInfo.getType();

        // Notify the connection that network type has changed. Note that this
        // only work for connected connections, we need to reestablish if it's
        // suspended.
        if (mNetworkType != oldType
                && isNetworkAvailable()) {
            for (ImConnectionAdapter conn : mConnections) {
                conn.networkTypeChanged();
            }
        }

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
        }
    }

    // package private for inner class access
    void reestablishConnections() {
        if (!isNetworkAvailable()) {
            return;
        }

        for (ImConnectionAdapter conn : mConnections) {
            int connState = conn.getState();
            if (connState == ImConnection.SUSPENDED) {
                conn.reestablishSession();
            }
        }
    }

    private void suspendConnections() {
        for (ImConnectionAdapter conn : mConnections) {
            if (conn.getState() != ImConnection.LOGGED_IN) {
                continue;
            }
            conn.suspend();
        }
    }

    private final IRemoteImService.Stub mBinder = new IRemoteImService.Stub() {

        public List<ImPluginInfo> getAllPlugins() {
            return new ArrayList<ImPluginInfo>(mPluginHelper.getPluginsInfo());
        }

        public void addConnectionCreatedListener(IConnectionCreationListener listener) {
            if (listener != null) {
                mRemoteListeners.register(listener);
            }
        }

        public void removeConnectionCreatedListener(IConnectionCreationListener listener) {
            if (listener != null) {
                mRemoteListeners.unregister(listener);
            }
        }

        public IImConnection createConnection(long providerId) {
            return RemoteImService.this.createConnection(providerId);
        }

        public List getActiveConnections() {
            ArrayList<IBinder> result = new ArrayList<IBinder>(mConnections.size());
            for(IImConnection conn : mConnections) {
                result.add(conn.asBinder());
            }
            return result;
        }

        public void dismissNotifications(long providerId) {
            mStatusBarNotifier.dismissNotifications(providerId);
        }

        public void dismissChatNotification(long providerId, String username) {
            mStatusBarNotifier.dismissChatNotification(providerId, username);
        }
    };

    private final class SettingsMonitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED.equals(action)) {
                ConnectivityManager manager =
                    (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                setBackgroundData(manager.getBackgroundDataSetting());
                handleBackgroundDataSettingChange();
            }
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SHOW_TOAST:
                    Toast.makeText(RemoteImService.this,
                            (CharSequence) msg.obj, msg.arg1).show();
                    break;

                case EVENT_NETWORK_STATE_CHANGED:
                    networkStateChanged();
                    break;

                default:
            }
        }
    }
}
