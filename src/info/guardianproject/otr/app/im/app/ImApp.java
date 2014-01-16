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

package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.Broadcaster;
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IConnectionCreationListener;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.IRemoteImService;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.adapter.ConnectionListenerAdapter;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.plugin.ImPlugin;
import info.guardianproject.otr.app.im.plugin.ImPluginInfo;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.util.AssetUtil;
import info.guardianproject.util.Debug;
import info.guardianproject.util.PRNGFixes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class ImApp extends Application {
    
    public static final String LOG_TAG = "GB.ImApp";

    public static final String EXTRA_INTENT_SEND_TO_USER = "Send2_U";
    public static final String EXTRA_INTENT_PASSWORD = "password";

    public static final String EXTRA_INTENT_PROXY_TYPE = "proxy.type";
    public static final String EXTRA_INTENT_PROXY_HOST = "proxy.host";
    public static final String EXTRA_INTENT_PROXY_PORT = "proxy.port";

    public static final String IMPS_CATEGORY = "info.guardianproject.otr.app.im.IMPS_CATEGORY";
    public static final String ACTION_QUIT = "info.guardianproject.otr.app.im.QUIT";

    public static final int DEFAULT_AVATAR_WIDTH = 120;
    public static final int DEFAULT_AVATAR_HEIGHT = 120;

    public static final String HOCKEY_APP_ID = "2fa3b9252319e47367f1f125bb3adcd1";

    public static final String DEFAULT_TIMEOUT_CACHEWORD = "-1"; //one day
    
    public static final String CACHEWORD_PASSWORD_KEY = "pkey";
    public static final String CLEAR_PASSWORD_KEY = "clear_key";

    public static final String NO_CREATE_KEY = "nocreate";
    
    //ACCOUNT SETTINGS Imps defaults
    public static final String DEFAULT_XMPP_RESOURCE = "ChatSecure";
    public static final int DEFAULT_XMPP_PRIORITY = 20;
    public static final String DEFAULT_XMPP_OTR_MODE = "auto";
    
    private Locale locale = null;

    private static ImApp sImApp;

    IRemoteImService mImService;

    HashMap<Long, IImConnection> mConnections;
    MyConnListener mConnectionListener;
    HashMap<Long, ProviderDef> mProviders;

    Broadcaster mBroadcaster;
    
    public static boolean mUsingCacheword = false;

    /**
     * A queue of messages that are waiting to be sent when service is
     * connected.
     */
    ArrayList<Message> mQueue = new ArrayList<Message>();

    /** A flag indicates that we have called tomServiceStarted start the service. */
//    private boolean mServiceStarted;
    private Context mApplicationContext;
    private Resources mPrivateResources;

    private HashMap<String, BrandingResources> mBrandingResources;
    private BrandingResources mDefaultBrandingResources;

    public static final int EVENT_SERVICE_CONNECTED = 100;
    public static final int EVENT_CONNECTION_CREATED = 150;
    public static final int EVENT_CONNECTION_LOGGING_IN = 200;
    public static final int EVENT_CONNECTION_LOGGED_IN = 201;
    public static final int EVENT_CONNECTION_LOGGING_OUT = 202;
    public static final int EVENT_CONNECTION_DISCONNECTED = 203;
    public static final int EVENT_CONNECTION_SUSPENDED = 204;
    public static final int EVENT_USER_PRESENCE_UPDATED = 300;
    public static final int EVENT_UPDATE_USER_PRESENCE_ERROR = 301;

    private static final String[] PROVIDER_PROJECTION = { Imps.Provider._ID, Imps.Provider.NAME,
                                                         Imps.Provider.FULLNAME,
                                                         Imps.Provider.SIGNUP_URL, };

    private static final String[] ACCOUNT_PROJECTION = { Imps.Account._ID, Imps.Account.PROVIDER,
                                                        Imps.Account.NAME, Imps.Account.USERNAME,
                                                        Imps.Account.PASSWORD, };

    static final void log(String log) {
        Log.d(LOG_TAG, log);
    }

    /*
    public static ImApp getApplication(Activity activity) {
        // TODO should this be synchronized?
        if (sImApp == null) {
            initialize(activity);
        }

        return sImApp;
    }

    public static ImApp getApplication() {
        // TODO should this be synchronized?
        if (sImApp == null) {
            new ImApp();
        }

        return sImApp;
    }*/

    /**
     * Initialize performs the manual ImApp instantiation and initialization.
     * When the ImApp is started first in the process, the ImApp public
     * constructor should be called, and sImApp initialized. So calling
     * initialize() later should have no effect. However, if another application
     * runs in the same process and is started first, the ImApp application
     * object won't be instantiated, and we need to call initialize() manually
     * to instantiate and initialize it.
     */
    /*
    public void initialize(Activity activity) {
        // construct the TalkApp manually and call onCreate().
        sImApp = new ImApp();
        sImApp.mApplicationContext = activity.getApplication();
        sImApp.mPrivateResources = activity.getResources();
        sImApp.onCreate();
    }
*/
    
    @Override
    public Resources getResources() {
        if (mApplicationContext == this) {
            return super.getResources();
        }

        return mPrivateResources;
    }

    @Override
    public ContentResolver getContentResolver() {
        if (mApplicationContext == this) {
            return super.getContentResolver();
        }

        return mApplicationContext.getContentResolver();
    }

    public ImApp() {
        super();
        mConnections = new HashMap<Long, IImConnection>();
        mApplicationContext = this;
        sImApp = this;
    }

    public ImApp(Context context) {
        super();
        mConnections = new HashMap<Long, IImConnection>();
        mApplicationContext = context;
        sImApp = this;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (locale != null) {
            // We have to create a new configuration, because changing the passed-in Configuration
            // object causes an infinite relaunch loop in Android 4.2 (JB MR1)
            Configuration myConfig = new Configuration(newConfig);
            myConfig.locale = locale;
            
            Locale.setDefault(locale);
            getResources().updateConfiguration(myConfig, getResources().getDisplayMetrics());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Debug.onAppStart();
        
        PRNGFixes.apply(); //Google's fix for SecureRandom bug: http://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html
        
        mBroadcaster = new Broadcaster();

        setAppTheme(null);
        
        checkLocale();
    }
    
    public void setAppTheme (Activity activity)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        
        boolean themeDark = settings.getBoolean("themeDark", false);
        
        if (themeDark)
        {            
            setTheme(R.style.AppThemeDark);
            
            if (activity != null)
                activity.setTheme(R.style.AppThemeDark);
        }
        else
        {
            setTheme(R.style.AppTheme);
            
            
            if (activity != null)
                activity.setTheme(R.style.AppTheme);
        }
        
        Configuration config = getResources().getConfiguration();
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
     
        if (mImService != null)
        {
            boolean debugOn = settings.getBoolean("prefDebug", false);
            try {
                mImService.enableDebugLogging(debugOn);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean checkLocale ()
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        Configuration config = getResources().getConfiguration();

        String lang = settings.getString(getString(R.string.pref_default_locale), "");
        
        
        if ("".equals(lang)) {
            Properties props = AssetUtil.getProperties("gibberbot.properties", this);
            if (props != null) {
                String configuredLocale = props.getProperty("locale");
                if (configuredLocale != null && !"CHOOSE".equals(configuredLocale)) {
                    lang = configuredLocale;
                    Editor editor = settings.edit();
                    editor.putString(getString(R.string.pref_default_locale), lang);
                    editor.commit();
                }
            }
        }
        
        boolean updatedLocale = false;
        
        if (!"".equals(lang) && !config.locale.getLanguage().equals(lang)) {
            locale = new Locale(lang);            
            config.locale = locale;
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
            updatedLocale = true;
        }

        loadDefaultBrandingRes();
        
        return updatedLocale;
    }

    public boolean setNewLocale(Context context, String localeString) {

        /*
        Locale locale = new Locale(localeString);
       
        Configuration config = context.getResources().getConfiguration();
        config.locale = locale;
        
        context.getResources().updateConfiguration(config,
                context.getResources().getDisplayMetrics());

        Log.d(LOG_TAG, "locale = " + locale.getDisplayName());
        */
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Editor prefEdit = prefs.edit();
        prefEdit.putString(context.getString(R.string.pref_default_locale), localeString);
        prefEdit.commit();
        
        Configuration config = getResources().getConfiguration();

        locale = new Locale(localeString);            
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
      
        
        return true;
    }
    
    @Override
    public void onTerminate() {
        stopImServiceIfInactive();
        if (mImService != null) {
            try {
                mImService.removeConnectionCreatedListener(mConnCreationListener);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "failed to remove ConnectionCreatedListener");
            }
        }

        Imps.clearPassphrase(this);
        super.onTerminate();
    }

    public synchronized void startImServiceIfNeed() {
        startImServiceIfNeed(false);
    }

    public synchronized void startImServiceIfNeed(boolean isBoot) {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
            log("start ImService");
        
        Intent serviceIntent = new Intent();
        serviceIntent.setComponent(ImServiceConstants.IM_SERVICE_COMPONENT);
        serviceIntent.putExtra(ImServiceConstants.EXTRA_CHECK_AUTO_LOGIN, isBoot);
        
        if (mImService == null)
        {
            mApplicationContext.startService(serviceIntent);
            if (!isBoot) {
                mConnectionListener = new MyConnListener(new Handler());
            }
        }
        
        if (mImServiceConn != null && !isBoot)
            mApplicationContext
                .bindService(serviceIntent, mImServiceConn, Context.BIND_AUTO_CREATE);

                   
    }

    public boolean hasActiveConnections ()
    {
        return !mConnections.isEmpty();
        
    }
    
    public synchronized void stopImServiceIfInactive() {
        boolean hasActiveConnection = true;
        hasActiveConnection = !mConnections.isEmpty();
        
        if (!hasActiveConnection) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                log("stop ImService because there's no active connections");

            if (mImService != null) {
                mApplicationContext.unbindService(mImServiceConn);
                mImService = null;
            }
            Intent intent = new Intent();
            intent.setComponent(ImServiceConstants.IM_SERVICE_COMPONENT);
            mApplicationContext.stopService(intent);
         
        }
    }
    
    
    public synchronized void forceStopImService() 
    {
        if (mImService != null) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                log("stop ImService");

            mApplicationContext.unbindService(mImServiceConn);
            mImService = null;

            Intent intent = new Intent();
            intent.setComponent(ImServiceConstants.IM_SERVICE_COMPONENT);
            mApplicationContext.stopService(intent);
         
        }
    }
    
    private ServiceConnection mImServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                log("service connected");

            mImService = IRemoteImService.Stub.asInterface(service);
            fetchActiveConnections();
            
            synchronized (mQueue) {
                for (Message msg : mQueue) {
                    msg.sendToTarget();
                }
                mQueue.clear();
            }
            Message msg = Message.obtain(null, EVENT_SERVICE_CONNECTED);
            mBroadcaster.broadcast(msg);
            
            /*
            if (mKillServerOnStart)
            {
                forceStopImService();
            }*/
        }

        public void onServiceDisconnected(ComponentName className) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                log("service disconnected");

            mConnections.clear();
            mImService = null;
        }
    };

    public boolean serviceConnected() {
        return mImService != null;
    }

 //   public boolean isBackgroundDataEnabled() { //"background data" is a deprectaed concept
    public boolean isNetworkAvailableAndConnected () {
        ConnectivityManager manager = (ConnectivityManager) mApplicationContext
                .getSystemService(CONNECTIVITY_SERVICE);
      
        NetworkInfo nInfo = manager.getActiveNetworkInfo();

        if (nInfo != null)
        {
            Log.d(LOG_TAG,"network state: available=" + nInfo.isAvailable() + " connected/connecting=" + nInfo.isConnectedOrConnecting());
            return nInfo.isAvailable() && nInfo.isConnectedOrConnecting();
        }
        else
            return false; //no network info is a bad idea
    }

    public static long insertOrUpdateAccount(ContentResolver cr, long providerId, String userName,
            String pw) {
        String selection = Imps.Account.PROVIDER + "=? AND " + Imps.Account.USERNAME + "=?";
        String[] selectionArgs = { Long.toString(providerId), userName };

        Cursor c = cr.query(Imps.Account.CONTENT_URI, ACCOUNT_PROJECTION, selection, selectionArgs,
                null);
        if (c != null && c.moveToFirst()) {
            long id = c.getLong(c.getColumnIndexOrThrow(Imps.Account._ID));

            ContentValues values = new ContentValues(1);
            values.put(Imps.Account.PASSWORD, pw);
            Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, id);
            cr.update(accountUri, values, null, null);

            c.close();
            return id;
        } else {
            ContentValues values = new ContentValues(4);
            values.put(Imps.Account.PROVIDER, providerId);
            values.put(Imps.Account.NAME, userName);
            values.put(Imps.Account.USERNAME, userName);
            values.put(Imps.Account.PASSWORD, pw);

            if (pw != null && pw.length() > 0) {
                values.put(Imps.Account.KEEP_SIGNED_IN, true);
            }

            Uri result = cr.insert(Imps.Account.CONTENT_URI, values);
            if (c != null)
                c.close();
            return ContentUris.parseId(result);
        }
    }

    /** Used to reset the provider settings if a reload is required. */
    public void resetProviderSettings() {
        mProviders = null;
    }

    // For testing
    public void setImProviderSettings(HashMap<Long, ProviderDef> providers) {
        mProviders = providers;
    }

    private void loadImProviderSettings() {

        mProviders = new HashMap<Long, ProviderDef>();
        ContentResolver cr = getContentResolver();

        String selectionArgs[] = new String[1];
        selectionArgs[0] = ImApp.IMPS_CATEGORY;

        Cursor c = cr.query(Imps.Provider.CONTENT_URI, PROVIDER_PROJECTION, Imps.Provider.CATEGORY
                                                                            + "=?", selectionArgs,
                null);
        if (c == null) {
            return;
        }

        try {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String providerName = c.getString(1);
                String fullName = c.getString(2);
                String signUpUrl = c.getString(3);

                mProviders.put(id, new ProviderDef(id, providerName, fullName, signUpUrl));
            }
        } finally {
            c.close();
        }
    }

    private void loadDefaultBrandingRes() {
        HashMap<Integer, Integer> resMapping = new HashMap<Integer, Integer>();

        resMapping.put(BrandingResourceIDs.DRAWABLE_LOGO, R.drawable.ic_launcher_gibberbot);
        resMapping.put(BrandingResourceIDs.DRAWABLE_PRESENCE_ONLINE,
                android.R.drawable.presence_online);
        resMapping
                .put(BrandingResourceIDs.DRAWABLE_PRESENCE_AWAY, android.R.drawable.presence_away);
        resMapping
                .put(BrandingResourceIDs.DRAWABLE_PRESENCE_BUSY, android.R.drawable.presence_busy);
        resMapping.put(BrandingResourceIDs.DRAWABLE_PRESENCE_INVISIBLE,
                android.R.drawable.presence_invisible);
        resMapping.put(BrandingResourceIDs.DRAWABLE_PRESENCE_OFFLINE,
                android.R.drawable.presence_offline);
        resMapping.put(BrandingResourceIDs.DRAWABLE_READ_CHAT, R.drawable.status_chat);
        resMapping.put(BrandingResourceIDs.DRAWABLE_UNREAD_CHAT, R.drawable.status_chat_new);
        resMapping.put(BrandingResourceIDs.DRAWABLE_BLOCK, R.drawable.ic_im_block);

        resMapping.put(BrandingResourceIDs.STRING_ARRAY_SMILEY_NAMES, R.array.default_smiley_names);
        resMapping.put(BrandingResourceIDs.STRING_ARRAY_SMILEY_TEXTS, R.array.default_smiley_texts);

        resMapping.put(BrandingResourceIDs.STRING_PRESENCE_AVAILABLE, R.string.presence_available);
        resMapping.put(BrandingResourceIDs.STRING_PRESENCE_BUSY, R.string.presence_busy);
        resMapping.put(BrandingResourceIDs.STRING_PRESENCE_AWAY, R.string.presence_away);
        resMapping.put(BrandingResourceIDs.STRING_PRESENCE_IDLE, R.string.presence_idle);
        resMapping.put(BrandingResourceIDs.STRING_PRESENCE_OFFLINE, R.string.presence_offline);
        resMapping.put(BrandingResourceIDs.STRING_PRESENCE_INVISIBLE, R.string.presence_invisible);
        resMapping.put(BrandingResourceIDs.STRING_LABEL_USERNAME, R.string.label_username);
        resMapping.put(BrandingResourceIDs.STRING_ONGOING_CONVERSATION,
                R.string.ongoing_conversation);
        resMapping.put(BrandingResourceIDs.STRING_ADD_CONTACT_TITLE, R.string.add_contact_title);
        resMapping
                .put(BrandingResourceIDs.STRING_LABEL_INPUT_CONTACT, R.string.input_contact_label);
        resMapping.put(BrandingResourceIDs.STRING_BUTTON_ADD_CONTACT, R.string.invite_label);
        resMapping.put(BrandingResourceIDs.STRING_CONTACT_INFO_TITLE,
                R.string.contact_profile_title);

        resMapping.put(BrandingResourceIDs.STRING_MENU_ADD_CONTACT, R.string.menu_add_contact);
        resMapping.put(BrandingResourceIDs.STRING_MENU_BLOCK_CONTACT, R.string.menu_block_contact);
        resMapping.put(BrandingResourceIDs.STRING_MENU_CONTACT_LIST,
                R.string.menu_view_contact_list);
        resMapping
                .put(BrandingResourceIDs.STRING_MENU_DELETE_CONTACT, R.string.menu_remove_contact);
        resMapping.put(BrandingResourceIDs.STRING_MENU_END_CHAT, R.string.menu_end_conversation);
        resMapping.put(BrandingResourceIDs.STRING_MENU_INSERT_SMILEY, R.string.menu_insert_smiley);
        resMapping.put(BrandingResourceIDs.STRING_MENU_START_CHAT, R.string.menu_start_chat);
        resMapping.put(BrandingResourceIDs.STRING_MENU_VIEW_PROFILE, R.string.menu_view_profile);
        resMapping.put(BrandingResourceIDs.STRING_MENU_SWITCH_CHATS, R.string.menu_switch_chats);

        resMapping.put(BrandingResourceIDs.STRING_TOAST_CHECK_SAVE_PASSWORD,
                R.string.check_save_password);
        resMapping.put(BrandingResourceIDs.STRING_TOAST_CHECK_AUTO_SIGN_IN,
                R.string.check_auto_sign_in);

        resMapping.put(BrandingResourceIDs.STRING_LABEL_SIGN_UP, R.string.sign_up);

        mDefaultBrandingResources = new BrandingResources(this, resMapping, null /* default res */);
    }

    private void loadThirdPartyResources() {
        ImPluginHelper helper = ImPluginHelper.getInstance(this);
        helper.loadAvailablePlugins();
        ArrayList<ImPlugin> pluginList = helper.getPluginObjects();
        ArrayList<ImPluginInfo> infoList = helper.getPluginsInfo();
        int N = pluginList.size();
        PackageManager pm = getPackageManager();
        for (int i = 0; i < N; i++) {
            ImPlugin plugin = pluginList.get(i);
            ImPluginInfo pluginInfo = infoList.get(i);

            try {
                Resources packageRes = pm.getResourcesForApplication(pluginInfo.mPackageName);

                Map<Integer, Integer> resMap = plugin.getResourceMap();
                //int[] smileyIcons = plugin.getSmileyIconIds();

                
                BrandingResources res = new BrandingResources(packageRes, resMap,
                        mDefaultBrandingResources);
                mBrandingResources.put(pluginInfo.mProviderName, res);
            } catch (NameNotFoundException e) {
                Log.e(LOG_TAG, "Failed to load third party resources.", e);
            }
        }
    }

    public long getProviderId(String name) {
        loadImProviderSettings();
        for (ProviderDef provider : mProviders.values()) {
            if (provider.mName.equals(name)) {
                return provider.mId;
            }
        }
        return -1;
    }

    public ProviderDef getProvider(long id) {
        loadImProviderSettings();
        return mProviders.get(id);
    }

    public List<ProviderDef> getProviders() {
        loadImProviderSettings();
        ArrayList<ProviderDef> result = new ArrayList<ProviderDef>();
        result.addAll(mProviders.values());
        return result;
    }

    public BrandingResources getBrandingResource(long providerId) {
        ProviderDef provider = getProvider(providerId);
        if (provider == null) {
            return mDefaultBrandingResources;
        }
        if (mBrandingResources == null) {
            mBrandingResources = new HashMap<String, BrandingResources>();
            loadThirdPartyResources();
        }
        BrandingResources res = mBrandingResources.get(provider.mName);
        return res == null ? mDefaultBrandingResources : res;
    }

    public IImConnection createConnection(long providerId, long accountId) throws RemoteException {
        
        if (mImService == null) {
            // Service hasn't been connected or has died.
            return null;
        }
        
        IImConnection conn = getConnection(providerId);
        if (conn == null) {
            conn = mImService.createConnection(providerId, accountId);
        }
        return conn;
    }

    public IImConnection getConnection(long providerId) {
        synchronized (mConnections) {
            
            IImConnection im = mConnections.get(providerId);
            
            if (im != null)
            {
                try 
                {
                    im.getState();
                }
                catch (RemoteException doe)
                {
                    mConnections.clear();
                    //something is wrong
                    fetchActiveConnections();
                    im = mConnections.get(providerId);
                }
            }
            
            return im;
        }
    }

    public IImConnection getConnectionByAccount(long accountId) {
        synchronized (mConnections) {
            for (IImConnection conn : mConnections.values()) {
                try {
                    if (conn.getAccountId() == accountId) {
                        return conn;
                    }
                } catch (RemoteException e) {
                    // No server!
                }
            }
            return null;
        }
    }

    public Collection<IImConnection> getActiveConnections() {

        return mConnections.values();
    }

    public void callWhenServiceConnected(Handler target, Runnable callback) {
        Message msg = Message.obtain(target, callback);
        if (serviceConnected() && msg != null) {
            msg.sendToTarget();
        } else {
            startImServiceIfNeed();
            synchronized (mQueue) {
                mQueue.add(msg);
            }
        }
    }

    public void deleteAccount (long accountId, long providerId)
    {
        ContentResolver resolver = getContentResolver();
        
        Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
        resolver.delete(accountUri, null, null);
        
        Uri providerUri = ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId);
        resolver.delete(providerUri, null, null);
      
        Uri.Builder builder = Imps.Contacts.CONTENT_URI_CONTACTS_BY.buildUpon();
        ContentUris.appendId(builder, providerId);
        ContentUris.appendId(builder, accountId);        
        resolver.delete(builder.build(), null, null);
        
        
        
    }
    
    public void removePendingCall(Handler target) {
        synchronized (mQueue) {
            Iterator<Message> iter = mQueue.iterator();
            while (iter.hasNext()) {
                Message msg = iter.next();
                if (msg.getTarget() == target) {
                    iter.remove();
                }
            }
        }
    }

    public void registerForBroadcastEvent(int what, Handler target) {
        mBroadcaster.request(what, target, what);
    }

    public void unregisterForBroadcastEvent(int what, Handler target) {
        mBroadcaster.cancelRequest(what, target, what);
    }

    public void registerForConnEvents(Handler handler) {
        mBroadcaster.request(EVENT_CONNECTION_CREATED, handler, EVENT_CONNECTION_CREATED);
        mBroadcaster.request(EVENT_CONNECTION_LOGGING_IN, handler, EVENT_CONNECTION_LOGGING_IN);
        mBroadcaster.request(EVENT_CONNECTION_LOGGED_IN, handler, EVENT_CONNECTION_LOGGED_IN);
        mBroadcaster.request(EVENT_CONNECTION_LOGGING_OUT, handler, EVENT_CONNECTION_LOGGING_OUT);
        mBroadcaster.request(EVENT_CONNECTION_SUSPENDED, handler, EVENT_CONNECTION_SUSPENDED);
        mBroadcaster.request(EVENT_CONNECTION_DISCONNECTED, handler, EVENT_CONNECTION_DISCONNECTED);
        mBroadcaster.request(EVENT_USER_PRESENCE_UPDATED, handler, EVENT_USER_PRESENCE_UPDATED);
        mBroadcaster.request(EVENT_UPDATE_USER_PRESENCE_ERROR, handler,
                EVENT_UPDATE_USER_PRESENCE_ERROR);
    }

    public void unregisterForConnEvents(Handler handler) {
        mBroadcaster.cancelRequest(EVENT_CONNECTION_CREATED, handler, EVENT_CONNECTION_CREATED);
        mBroadcaster.cancelRequest(EVENT_CONNECTION_LOGGING_IN, handler,
                EVENT_CONNECTION_LOGGING_IN);
        mBroadcaster.cancelRequest(EVENT_CONNECTION_LOGGED_IN, handler, EVENT_CONNECTION_LOGGED_IN);
        mBroadcaster.cancelRequest(EVENT_CONNECTION_LOGGING_OUT, handler,
                EVENT_CONNECTION_LOGGING_OUT);
        mBroadcaster.cancelRequest(EVENT_CONNECTION_SUSPENDED, handler, EVENT_CONNECTION_SUSPENDED);
        mBroadcaster.cancelRequest(EVENT_CONNECTION_DISCONNECTED, handler,
                EVENT_CONNECTION_DISCONNECTED);
        mBroadcaster.cancelRequest(EVENT_USER_PRESENCE_UPDATED, handler,
                EVENT_USER_PRESENCE_UPDATED);
        mBroadcaster.cancelRequest(EVENT_UPDATE_USER_PRESENCE_ERROR, handler,
                EVENT_UPDATE_USER_PRESENCE_ERROR);
    }

    void broadcastConnEvent(int what, long providerId, ImErrorInfo error) {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            log("broadcasting connection event " + what + ", provider id " + providerId);
        }
        android.os.Message msg = android.os.Message.obtain(null, what, (int) (providerId >> 32),
                (int) providerId, error);
        mBroadcaster.broadcast(msg);
    }

    public void dismissNotifications(long providerId) {
        if (mImService != null) {
            try {
                mImService.dismissNotifications(providerId);
            } catch (RemoteException e) {
            }
        }
    }

    public void dismissChatNotification(long providerId, String username) {
        if (mImService != null) {
            try {
                mImService.dismissChatNotification(providerId, username);
            } catch (RemoteException e) {
            }
        }
    }

    private void fetchActiveConnections() {
        if (mImService != null)
        {
            try {
                // register the listener before fetch so that we won't miss any connection.
                mImService.addConnectionCreatedListener(mConnCreationListener);                
                synchronized (mConnections) {
                    
                    for (IBinder binder : (List<IBinder>) mImService.getActiveConnections()) {
                        IImConnection conn = IImConnection.Stub.asInterface(binder);
                        long providerId = conn.getProviderId();
                        if (!mConnections.containsKey(providerId)) {
                            mConnections.put(providerId, conn);
                            conn.registerConnectionListener(mConnectionListener);
                        }
                    }
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "fetching active connections", e);
            }
        }
    }

    private final IConnectionCreationListener mConnCreationListener = new IConnectionCreationListener.Stub() {
        public void onConnectionCreated(IImConnection conn) throws RemoteException {
            long providerId = conn.getProviderId();
            synchronized (mConnections) {
                if (!mConnections.containsKey(providerId)) {
                    mConnections.put(providerId, conn);
                    conn.registerConnectionListener(mConnectionListener);
                }
            }
            broadcastConnEvent(EVENT_CONNECTION_CREATED, providerId, null);
        }
    };

    private final class MyConnListener extends ConnectionListenerAdapter {
        public MyConnListener(Handler handler) {
            super(handler);
        }

        @Override
        public void onConnectionStateChange(IImConnection conn, int state, ImErrorInfo error) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                log("onConnectionStateChange(" + state + ", " + error + ")");
            }

            try {

               // fetchActiveConnections();
                
                int what = -1;
                long providerId = conn.getProviderId();
                switch (state) {
                case ImConnection.LOGGED_IN:
                    what = EVENT_CONNECTION_LOGGED_IN;
                    break;

                case ImConnection.LOGGING_IN:
                    what = EVENT_CONNECTION_LOGGING_IN;
                    break;

                case ImConnection.LOGGING_OUT:
                    // NOTE: if this logic is changed, the logic in ImConnectionAdapter.ConnectionAdapterListener must be changed to match
                    what = EVENT_CONNECTION_LOGGING_OUT;

                    break;

                case ImConnection.DISCONNECTED:
                    // NOTE: if this logic is changed, the logic in ImConnectionAdapter.ConnectionAdapterListener must be changed to match
                    what = EVENT_CONNECTION_DISCONNECTED;                    
                    mConnections.remove(providerId);                    
                    // stop the service if there isn't an active connection anymore.
                    //stopImServiceIfInactive();
                    
                    break;

                case ImConnection.SUSPENDED:
                    what = EVENT_CONNECTION_SUSPENDED;
                    break;
                }
                if (what != -1) {
                    broadcastConnEvent(what, providerId, error);
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "onConnectionStateChange", e);
            }
        }

        @Override
        public void onUpdateSelfPresenceError(IImConnection connection, ImErrorInfo error) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                log("onUpdateUserPresenceError(" + error + ")");
            }
            try {
                long providerId = connection.getProviderId();
                broadcastConnEvent(EVENT_UPDATE_USER_PRESENCE_ERROR, providerId, error);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "onUpdateUserPresenceError", e);
            }
        }

        @Override
        public void onSelfPresenceUpdated(IImConnection connection) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG))
                log("onUserPresenceUpdated");

            try {
                long providerId = connection.getProviderId();
                broadcastConnEvent(EVENT_USER_PRESENCE_UPDATED, providerId, null);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "onUserPresenceUpdated", e);
            }
        }
    }

    public IRemoteImService getRemoteImService() {
        return mImService;
    }

   
    public IChatSession getChatSession(long providerId, String remoteAddress) {
        IImConnection conn = getConnection(providerId);

        IChatSessionManager chatSessionManager = null;
        if (conn != null) {
            try {
                chatSessionManager = conn.getChatSessionManager();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "error in getting ChatSessionManager", e);
            }
        }

        if (chatSessionManager != null) {
            try {
                return chatSessionManager.getChatSession(remoteAddress);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "error in getting ChatSession", e);
            }
        }

        return null;
    }

    public void maybeInit(Activity activity) {
        startImServiceIfNeed();
        setAppTheme(activity);
        ImPluginHelper.getInstance(this).loadAvailablePlugins();
    }

    public void checkForCrashes(final Activity activity) {
        CrashManager.register(activity, ImApp.HOCKEY_APP_ID, new CrashManagerListener() {
            @Override
            public String getDescription() {
                return Debug.getTrail(activity);
            }
        });
    }
}
