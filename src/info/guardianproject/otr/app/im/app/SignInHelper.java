package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.adapter.ConnectionListenerAdapter;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.util.LogCleaner;

import java.util.Collection;
import java.util.HashSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

/**
 * Handle sign-in process for activities.
 * 
 * @author devrandom
 * 
 * <p>Users of this helper must call {@link SignInHelper#stop()} to clean up callbacks
 * in their onDestroy() or onPause() lifecycle methods.
 * 
 * <p>The helper listens to connection events.  It automatically stops listening when the
 * connection state is logged-in or disconnected (failed).
 */
public class SignInHelper {
    Activity mContext;
    private SimpleAlertHandler mHandler;
    private ImApp mApp;
    private MyConnectionListener mListener;
    private Collection<IImConnection> connections;
    private SignInListener mSignInListener;

    // This can be used to be informed of signin events
    public interface SignInListener {
        void connectedToService();
        void stateChanged(int state, long accountId);
    }
    
    public SignInHelper(Activity context, SignInListener listener) {
        this.mContext = context;
        mHandler = new SimpleAlertHandler(context);
        mListener = new MyConnectionListener(mHandler);
        mSignInListener = listener;
        if (mApp == null) {

            mApp = (ImApp)mContext.getApplication();
        }
        
        connections = new HashSet<IImConnection>();
    }
    
    public SignInHelper(Activity context) {
        this(context, null);
    }
    
    public void setSignInListener(SignInListener listener) {
        mSignInListener = listener;
    }
    
    public void stop() {
        for (IImConnection connection : connections) {
            try {
                connection.unregisterConnectionListener(mListener);
            } catch (RemoteException e) {
                // Ignore
            }
        }
        connections.clear();
    }

    private final class MyConnectionListener extends ConnectionListenerAdapter {
        MyConnectionListener(Handler handler) {
            super(handler);
        }

        @Override
        public void onConnectionStateChange(IImConnection connection, int state, ImErrorInfo error) {
            handleConnectionEvent(connection, state, error);
        }
    }

    private void handleConnectionEvent(IImConnection connection, int state, ImErrorInfo error) {
        long accountId;
        long providerId;
        try {
            accountId = connection.getAccountId();
            providerId = connection.getProviderId();
        } catch (RemoteException e) {
            // Ouch!  Service died!  We'll just disappear.
            Log.w(ImApp.LOG_TAG, "<SigningInActivity> Connection disappeared while signing in!");
            return;
        }

        if (mSignInListener != null)
            mSignInListener.stateChanged(state, accountId);

        // Stop listening if we get into a resting state
        if (state == ImConnection.LOGGED_IN || state == ImConnection.DISCONNECTED) {
            connections.remove(connection);
            try {
                connection.unregisterConnectionListener(mListener);
            } catch (RemoteException e) {

                mHandler.showServiceErrorAlert(e.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "handle connection error",e);
            }
        }
        
        if (state == ImConnection.DISCONNECTED) {
            // sign in failed
            final ProviderDef provider = mApp.getProvider(providerId);
            
            if (provider != null) //a provider might have been deleted
            {
                String providerName = provider.mName;
    
    
                Resources r = mContext.getResources();
                String errMsg = r.getString(R.string.login_service_failed, providerName, // FIXME
                        error == null ? "" : ErrorResUtils.getErrorRes(r, error.getCode()));
                
                Toast.makeText(mContext, errMsg, Toast.LENGTH_LONG).show();
                /*
                new AlertDialog.Builder(mContext).setTitle(R.string.error)
                        .setMessage()
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // FIXME
                            }
                        }).setCancelable(false).show();
                        */
            }
        }
    }

    public void goToAccount(long accountId) {
        Intent intent;
        intent = new Intent(mContext, NewChatActivity.class);
        // clear the back stack of the account setup
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, accountId);

        mContext.startActivity(intent);
        // sign in successfully, finish and switch to contact list
      //  mContext.finish();
    }

    public void signIn(final String password, final long providerId, final long accountId,
            final boolean isActive) {

        final ProviderDef provider = mApp.getProvider(providerId);
        
        if (provider != null) //provider may be null if deleted, or db not updated yet
        {
            final String providerName = provider.mName;
    
            if (mApp.serviceConnected()) {
                if (mSignInListener != null)
                    mSignInListener.connectedToService();
                if (!isActive) {
                    activateAccount(providerId, accountId);
                }
                signInAccount(password, providerId, providerName, accountId);
            }
            else
            {
                mApp.callWhenServiceConnected(mHandler, new Runnable() {
                    public void run() {
                        if (mApp.serviceConnected()) {
                            if (mSignInListener != null)
                                mSignInListener.connectedToService();
                            if (!isActive) {
                                activateAccount(providerId, accountId);
                            }
                            signInAccount(password, providerId, providerName, accountId);
                        }
                    }
                });
            }
        }
    }

    private void signInAccount(String password, long providerId, String providerName, long accountId) {
        boolean autoLoadContacts = true;
        boolean autoRetryLogin = true;
        IImConnection conn = null;
        
        try {
            conn = mApp.getConnection(providerId);
            if (conn != null) {
                connections.add(conn);
                conn.registerConnectionListener(mListener);
                int state = conn.getState();
                if (mSignInListener != null)
                    mSignInListener.stateChanged(state, accountId);
                if (state != ImConnection.DISCONNECTED) {
                    // already signed in or in the process
                    if (state == ImConnection.LOGGED_IN) {
                        connections.remove(conn);
                        conn.unregisterConnectionListener(mListener);
                    }
                    handleConnectionEvent(conn, state, null);
                    return;
                }
            } else {
                conn = mApp.createConnection(providerId, accountId);
                if (conn == null) {
                    // This can happen when service did not come up for any reason
                    return;
                }

                connections.add(conn);
                conn.registerConnectionListener(mListener);
            }

            if (mApp.isNetworkAvailableAndConnected()) {
               
                conn.login(password, autoLoadContacts, autoRetryLogin);
            } else {
                promptForBackgroundDataSetting(providerName);
                return;
            }
        } catch (DeadObjectException e) {
           
            try
            {
                conn = mApp.createConnection(providerId, accountId);
                if (conn == null) {
                    // This can happen when service did not come up for any reason
                    return;
                }
    
                connections.add(conn);
                conn.registerConnectionListener(mListener);
                if (mApp.isNetworkAvailableAndConnected()) {
                    
                    conn.login(password, autoLoadContacts, autoRetryLogin);
                }
            } catch (RemoteException e2) {

                mHandler.showServiceErrorAlert(e2.getLocalizedMessage());
                LogCleaner.error(ImApp.LOG_TAG, "sign in account",e2);
            }

        } catch (RemoteException e) {

            mHandler.showServiceErrorAlert(e.getLocalizedMessage());
            LogCleaner.error(ImApp.LOG_TAG, "sign in account",e);
        }
    }

    private static final String SYNC_SETTINGS_ACTION = "android.settings.SYNC_SETTINGS";
    private static final String SYNC_SETTINGS_CATEGORY = "android.intent.category.DEFAULT";

    /**
     * Popup a dialog to ask the user whether he/she wants to enable background
     * connection to continue. If yes, enable the setting and broadcast the
     * change. Otherwise, quit the signing in window immediately.
     */
    private void promptForBackgroundDataSetting(String providerName) {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.bg_data_prompt_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(mContext.getString(R.string.bg_data_prompt_message, providerName))
                .setPositiveButton(R.string.bg_data_prompt_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(SYNC_SETTINGS_ACTION);
                                intent.addCategory(SYNC_SETTINGS_CATEGORY);
                                mContext.startActivity(intent);
                            }
                        })
                .setNegativeButton(R.string.bg_data_prompt_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).show();
    }

    public void activateAccount(long providerId, long accountId) {
        // Update the active value. We restrict to only one active
        // account per provider right now, so update all accounts of
        // this provider to inactive first and then update this
        // account to active.
        ContentValues values = new ContentValues(1);
        values.put(Imps.Account.ACTIVE, 0);
        ContentResolver cr = mContext.getContentResolver();
        cr.update(Imps.Account.CONTENT_URI, values, Imps.Account.PROVIDER + "=" + providerId, null);

        values.put(Imps.Account.ACTIVE, 1);
        cr.update(ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId), values, null,
                null);
    }

}
