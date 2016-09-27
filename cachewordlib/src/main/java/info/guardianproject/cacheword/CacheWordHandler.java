
package info.guardianproject.cacheword;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * This class is designed to accompany any Activity that is interested in the
 * secrets cached by CacheWord. <i>The context provided in the constructor must
 * implement the ICacheWordSubscriber interface.</i> This is so the Activity can
 * be alerted to the state change events.
 */
public class CacheWordHandler {
    private static final String TAG = "CacheWordHandler";

    private Context mContext;
    private CacheWordService mCacheWordService;
    private ICacheWordSubscriber mSubscriber;
    private CacheWordSettings mSettings;

    /**
     * Tracking service connection state is a bit of a mess. There are three
     * tricky situations:
     * <ol>
     * <li>We call bindService() which returns successfully, but we are told to
     * disconnect before the connection completes (onServiceConnected() is
     * called). This can occur when the activity opens and closes quickly.</li>
     * <li>2. We shouldn't call unbind() if we didn't bind successfully. Doing
     * so produces Service not registered:
     * info.guardianproject.cacheword.CacheWordHandler</li>
     * <li>3. Conversely, We MUST call unbind() if bindService() was called
     * Failing to do so results in Activity FOOBAR has leaked ServiceConnection
     * We must track the connection state separately from the bound state.</li>
     * </ol>
     * We use this flag to help prevent a race condition described in (1).
     */
    private ServiceConnectionState mConnectionState = ServiceConnectionState.CONNECTION_NULL;

    // We use this flag to determine whether or not unbind() should be called
    // as described in #2 and #3
    private BindState mBoundState = BindState.BIND_NULL;

    enum ServiceConnectionState {
        CONNECTION_NULL,
        CONNECTION_INPROGRESS,
        CONNECTION_CANCELED,
        CONNECTION_ACTIVE
    }

    enum BindState {
        BIND_NULL,
        BIND_REQUESTED,
        BIND_COMPLETED

    }

    /**
     * Initializes the CacheWordHandler with the default {@link CacheWordSettings}
     *
     * @see #CacheWordHandler(Context context, CacheWordSettings settings)
     * @param context
     */
    public CacheWordHandler(Context context) {
        this(context, (ICacheWordSubscriber) context, null);
    }

    /**
     * Initializes the CacheWordHandler with distinct Context and
     * ICacheWordSubscriber objects. Uses default CacheWordSettings
     */
    public CacheWordHandler(Context context, ICacheWordSubscriber subscriber) {
        this(context, subscriber, null);
    }

    /**
     * Initializes the CacheWordHandler. Use this form when your Context (e.g,
     * the Activity) also implements the ICacheWordSubscriber interface. Context
     * MUST impement ICacheWordSubscriber, else IllegalArgumentException will be
     * thrown at runtime
     *
     * @param context must implement the ICacheWordSubscriber interface
     * @param settings the settings for CacheWord
     */
    public CacheWordHandler(Context context, CacheWordSettings settings) {

        try {
            // shame we have to do this at runtime.
            // must ponder a way to enforce this relationship at compile time
            mSubscriber = (ICacheWordSubscriber) context;
            mContext = context;
            mSettings = settings;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                    "CacheWordHandler passed invalid Activity. Expects class that implements ICacheWordSubscriber");
        }
    }

    /**
     * Initializes the CacheWordHandler with distinct Context and
     * ICacheWordSubscriber objects
     *
     * @param context your application's or activity's context
     * @param subscriber the object to notify of CW events
     * @param settings the settings for CacheWord
     */
    public CacheWordHandler(Context context, ICacheWordSubscriber subscriber,
            CacheWordSettings settings) {
        mContext = context;
        mSubscriber = subscriber;
        mSettings = settings;
    }

    /**
     * Connect to the CacheWord service, starting it if necessary. Once
     * connected, the attached Context will begin receiving CacheWord events.
     */
    public synchronized void connectToService() {
        if (isCacheWordConnected())
            return;

        Intent cacheWordIntent = CacheWordService
                .getBlankServiceIntent(mContext.getApplicationContext());
        /*
         * We start AND bind the service starting - ensures the cacheword
         * service will outlive the activity binding - allows us to notify the
         * service of active subscribers
         */
        mContext.startService(cacheWordIntent);
        if (mContext.bindService(cacheWordIntent, mCacheWordServiceConnection,
                Context.BIND_AUTO_CREATE)) {
            mBoundState = BindState.BIND_REQUESTED;
        }
        mConnectionState = ServiceConnectionState.CONNECTION_INPROGRESS;

    }

    /**
     * Detach but don't disconnect from the CacheWord service. CacheWord events
     * will continue to be received, but this client will not be considered when
     * performing automatic timeouts.
     */
    public void detach() {
        if (mCacheWordService != null) {
            mCacheWordService.detachSubscriber();
        }
    }

    /**
     * Reattach to the CacheWord service.
     */
    public void reattach() {
        if (mCacheWordService != null) {
            mCacheWordService.attachSubscriber();
        }
    }

    /**
     * Disconnect from the CacheWord service. No further CacheWord events will
     * be received.
     */
    public void disconnect() {
        synchronized (this) {
            mConnectionState = ServiceConnectionState.CONNECTION_CANCELED;

            if (mBoundState == BindState.BIND_COMPLETED) {
                if (mCacheWordService != null) {
                    mCacheWordService.detachSubscriber();
                    mCacheWordService = null;
                }
                mContext.unbindService(mCacheWordServiceConnection);
                mBoundState = BindState.BIND_NULL;
                unregisterBroadcastRecevier();
            }
        }
    }

    /**
     * Fetch the secrets from CacheWord
     *
     * @return the secrets or null on failure
     */
    public ICachedSecrets getCachedSecrets() {
        if (!isCacheWordConnected())
            return null;

        return mCacheWordService.getCachedSecrets();
    }

    public byte[] getEncryptionKey() {
        final ICachedSecrets s = getCachedSecrets();
        if (s instanceof PassphraseSecrets) {
            return ((PassphraseSecrets) s).getSecretKey().getEncoded();
        }
        return null;
    }

    /**
     * Write the secrets into CacheWord, initializing the cache if necessary.
     *
     * @param secrets
     */
    public void setCachedSecrets(ICachedSecrets secrets) {
        if (!isCacheWordConnected())
            return;

        mCacheWordService.setCachedSecrets(secrets);
    }

    /**
     * Use the basic PassphraseSecrets implementation to derive encryption keys
     * securely. Initializes cacheword if necessary.
     *
     * @param passphrase
     * @throws GeneralSecurityException on invalid password
     */
    public void setPassphrase(char[] passphrase) throws GeneralSecurityException {
        final PassphraseSecrets ps;
        if (SecretsManager.isInitialized(mContext)) {
            ps = PassphraseSecrets.fetchSecrets(mContext, passphrase);
        } else {
            ps = PassphraseSecrets.initializeSecrets(mContext, passphrase);
            if (ps == null)
                throw new GeneralSecurityException("initializeSecrets could not save the secrets.");
        }
        setCachedSecrets(ps);
    }

    /**
     * Changes the passphrase used to encrypt the derived encryption keys. Since
     * the derived encryption key stays the same, this can safely be called even
     * when the secrets are in use. Only works if you're using the
     * PassphraseSecrets implementation. (i.e., Are you using setPassphrase() or
     * setCachedSecrets()?)
     *
     * @param current_secrets the current secrets you're using
     * @param new_passphrase the new passphrase to encrypt the old secrets with
     * @return null on error or current_secrets on success
     * @throws IOException
     */
    public PassphraseSecrets changePassphrase(PassphraseSecrets current_secrets,
            char[] new_passphrase) throws IOException {
        if (!SecretsManager.isInitialized(mContext)) {
            throw new IllegalStateException(
                    "CacheWord is not initialized. Passphrase can't be changed");
        }
        PassphraseSecrets new_secrets = PassphraseSecrets.changePassphrase(mContext,
                current_secrets, new_passphrase);
        if (new_secrets != null)
            return new_secrets;
        else
            throw new IOException("changePassphrase could not save the secrets");
    }

    /**
     * Clear the secrets from memory. This is only a request to CacheWord. The
     * cache should not be considered wiped and locked until the onLockEvent is
     * received.
     */
    public void manuallyLock() {
        if (!isPrepared())
            return;
        mCacheWordService.manuallyLock();
    }

    /**
     * @return true if the cache is locked or uninitialized, false otherwise
     */
    public boolean isLocked() {
        if (!isPrepared())
            return true;
        return mCacheWordService.isLocked();
    }

    public void setTimeoutSeconds(int seconds) throws IllegalStateException {
        if (!isCacheWordConnected())
            throw new IllegalStateException("CacheWord not connected");
        mCacheWordService.settings().setTimeoutSeconds(seconds);
    }

    public int getTimeoutSeconds() throws IllegalStateException {
        if (!isCacheWordConnected())
            throw new IllegalStateException("CacheWord not connected");
        return mCacheWordService.settings().getTimeoutSeconds();
    }

    public void setVibrateSetting(boolean vibrate) throws IllegalStateException {
        if (!isCacheWordConnected())
            throw new IllegalStateException("CacheWord not connected");
        mCacheWordService.settings().setVibrateSetting(vibrate);
    }

    public boolean getVibrateSetting() throws IllegalStateException {
        if (!isCacheWordConnected())
            throw new IllegalStateException("CacheWord not connected");
        return mCacheWordService.settings().getVibrateSetting();
    }

    public void setNotificationIntent(PendingIntent intent) {
        if (!isCacheWordConnected())
            throw new IllegalStateException("CacheWord not connected");

        mCacheWordService.settings().setNotificationIntent(intent);
    }

    // / private helpers
    // /////////////////////////////////////////

    private void registerBroadcastReceiver() {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mCacheWordReceiver,
                new IntentFilter(Constants.INTENT_NEW_SECRETS));
    }

    private void unregisterBroadcastRecevier() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mCacheWordReceiver);

    }

    private void checkCacheWordState() {
        // this is ugly as all hell

        int newState = Constants.STATE_UNKNOWN;

        if (!isCacheWordConnected()) {
            newState = Constants.STATE_UNKNOWN;
            Log.d(TAG, "checkCacheWordState: not connected");
        } else if (!isCacheWordInitialized()) {
            newState = Constants.STATE_UNINITIALIZED;
            Log.d(TAG, "checkCacheWordState: STATE_UNINITIALIZED");
        } else if (isCacheWordConnected() && mCacheWordService.isLocked()) {
            newState = Constants.STATE_LOCKED;
            Log.d(TAG, "checkCacheWordState: STATE_LOCKED, but isCacheWordConnected()=="
                    + isCacheWordConnected());
        } else {
            newState = Constants.STATE_UNLOCKED;
            Log.d(TAG, "checkCacheWordState: STATE_UNLOCKED");
        }

        if (newState == Constants.STATE_UNINITIALIZED) {
            mSubscriber.onCacheWordUninitialized();
        } else if (newState == Constants.STATE_LOCKED) {
            mSubscriber.onCacheWordLocked();
        } else if (newState == Constants.STATE_UNLOCKED) {
            mSubscriber.onCacheWordOpened();
        } else {
            Log.e(TAG, "Unknown CacheWord state entered!");

        }
    }

    /**
     * @return true if cacheword is connected and available for calling
     */
    private boolean isCacheWordConnected() {
        return mCacheWordService != null;
    }

    private boolean isCacheWordInitialized() {
        return SecretsManager.isInitialized(mContext);
    }

    public void deinitialize() {
        SecretsManager.setInitialized(mContext, false);
    }

    private boolean isPrepared() {
        return isCacheWordConnected() && isCacheWordInitialized();
    }

    private BroadcastReceiver mCacheWordReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_NEW_SECRETS)) {
                if (isCacheWordConnected()) {
                    checkCacheWordState();
                }
            }
        }
    };

    private ServiceConnection mCacheWordServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ICacheWordBinder cwBinder = (ICacheWordBinder) binder;
            if (cwBinder != null) {
                Log.d(TAG, "onServiceConnected");
                synchronized (CacheWordHandler.this) {
                    if (mConnectionState == ServiceConnectionState.CONNECTION_INPROGRESS) {
                        mCacheWordService = cwBinder.getService();
                        registerBroadcastReceiver();
                        mCacheWordService.attachSubscriber();
                        if (mSettings != null)
                            mCacheWordService.setSettings(mSettings);
                        mConnectionState = ServiceConnectionState.CONNECTION_ACTIVE;
                        mBoundState = BindState.BIND_COMPLETED;
                        checkCacheWordState();
                    } else if (mConnectionState == ServiceConnectionState.CONNECTION_CANCELED) {
                        // race condition hit
                        if (mBoundState != BindState.BIND_NULL) {
                            mContext.unbindService(mCacheWordServiceConnection);
                            mBoundState = BindState.BIND_NULL;
                        }
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisonnected");
            synchronized (CacheWordHandler.this) {
                if (mBoundState != BindState.BIND_NULL) {
                    mContext.unbindService(mCacheWordServiceConnection);
                    mBoundState = BindState.BIND_NULL;
                    unregisterBroadcastRecevier();
                }
                mCacheWordService = null;
            }

        }

    };

}
