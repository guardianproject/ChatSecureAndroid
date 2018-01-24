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
import android.os.AsyncTask;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;
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

               // Toast.makeText(mContext, errMsg, Toast.LENGTH_LONG).show();
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

    private void signInAccount(final String password, final long providerId, final String providerName, final long accountId) {


        try {
            signInAccountAsync(password, providerId, providerName, accountId);
        } catch (RemoteException e) {
            Log.d(ImApp.LOG_TAG,"error signing in",e);
        }



    }

    private void signInAccountAsync(String password, long providerId, String providerName, long accountId) throws RemoteException {
        boolean autoLoadContacts = true;
        boolean autoRetryLogin = true;
        IImConnection conn = null;


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

            conn.login(password, autoLoadContacts, autoRetryLogin);

            /*
            if (mApp.isNetworkAvailableAndConnected()) {

            } else {
             //   promptForBackgroundDataSetting(providerName);
                return;
            }*/

    }

    /**
     * Popup a dialog to ask the user whether he/she wants to enable background
     * connection to continue. If yes, enable the setting and broadcast the
     * change. Otherwise, quit the signing in window immediately.
     */
    private void promptForBackgroundDataSetting(String providerName) {

        Toast.makeText(mContext, mContext.getString(R.string.bg_data_prompt_message, providerName), Toast.LENGTH_LONG).show();


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
