/**
 *
 */
package info.guardianproject.otr.app.im.app;

import java.util.ArrayList;
import java.util.List;

import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.provider.Imps;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AccountAdapter extends CursorAdapter {
    private LayoutInflater mInflater;
    private int mResId;
    private Cursor mStashCursor;
    private AsyncTask<Void, Void, List<AccountSetting>> mBindTask;
    private Listener mListener;

    public AccountAdapter(Activity context,
            LayoutInflater.Factory factory, int resId) {
        super(context, null, 0);
        mInflater = LayoutInflater.from(context).cloneInContext(context);
        mInflater.setFactory(factory);
        mResId = resId;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if(mBindTask != null) {
            mBindTask.cancel(false);
            mBindTask = null ;
        }

        if (mStashCursor != null && (!mStashCursor.isClosed()))
                mStashCursor.close();

        mStashCursor = newCursor;

        if (mStashCursor != null) {
            // Delay swapping in the cursor until we get the extra info
           // List<AccountInfo> accountInfoList = getAccountInfoList(mStashCursor) ;
           // runBindTask((Activity)mContext, accountInfoList);
        }
        return super.swapCursor(mStashCursor);
    };

    /**
     * @param mStashCursor
     * @return
     */
    private List<AccountInfo> getAccountInfoList(Cursor cursor) {
        List<AccountInfo> aiList = new ArrayList<AccountInfo>();
        cursor.moveToPosition(-1);
        while( cursor.moveToNext() ) {
            aiList.add( getAccountInfo(cursor));
        }
        return aiList;
    }

    static class AccountInfo {
        int providerId;
        String activeUserName;
        int dbConnectionStatus;
        int presenceStatus;
    }

    static class AccountSetting {
        String mProviderNameText;
        String mSecondRowText;
        boolean mSwitchOn;
        String activeUserName;
        int connectionStatus ;

        String domain;
        String host;
        int port;
        boolean isTor;

    }

    AccountInfo getAccountInfo( Cursor cursor ) {
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.providerId = cursor.getInt(cursor.getColumnIndexOrThrow(Imps.Provider._ID));
        if (!cursor.isNull(cursor.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_ID))) {
            accountInfo.activeUserName = cursor.getString(cursor.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_USERNAME));
            accountInfo.dbConnectionStatus = cursor.getInt(cursor.getColumnIndexOrThrow(Imps.Provider.ACCOUNT_PRESENCE_STATUS));
            accountInfo.presenceStatus = cursor.getInt(cursor.getColumnIndexOrThrow(Imps.Provider.ACCOUNT_CONNECTION_STATUS));
        }
        return accountInfo;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // create a custom view, so we can manage it ourselves. Mainly, we want to
        // initialize the widget views (by calling getViewById()) in newView() instead of in
        // bindView(), which can be called more often.
        ProviderListItem view = (ProviderListItem) mInflater.inflate(mResId, parent, false);
        boolean showLongName = false;
        view.init(cursor, showLongName);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((ProviderListItem) view).bindView(cursor);

    }

    private void runBindTask( final Activity context, final List<AccountInfo> accountInfoList ) {
        final Resources resources = context.getResources();
        final ContentResolver resolver = context.getContentResolver();
        final ImApp mApp = (ImApp)context.getApplication();

        // if called multiple times
        if (mBindTask != null)
            mBindTask.cancel(false);
        //


        mBindTask = new AsyncTask<Void, Void, List<AccountSetting>>() {

            @Override
            protected List<AccountSetting> doInBackground(Void... params) {
                List<AccountSetting> accountSettingList = new ArrayList<AccountSetting>();
                for( AccountInfo ai : accountInfoList ) {
                    accountSettingList.add( getAccountSettings(ai) );
                }
                return accountSettingList;
            }

            private AccountSetting getAccountSettings(AccountInfo ai) {
                AccountSetting as = new AccountSetting();


                Cursor pCursor = resolver.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString(ai.providerId)},null);

                if (pCursor != null)
                {
                    Imps.ProviderSettings.QueryMap settings =
                            new Imps.ProviderSettings.QueryMap(pCursor, resolver, ai.providerId, false , null);

                    as.connectionStatus = ai.dbConnectionStatus;
                    as.activeUserName = ai.activeUserName;
                    as.domain = settings.getDomain();
                    as.host = settings.getServer();
                    as.port = settings.getPort();
                    as.isTor = settings.getUseTor();

                    IImConnection conn = mApp.getConnection(ai.providerId);
                    if (conn == null) {
                        as.connectionStatus = ImConnection.DISCONNECTED;
                    } else {
                        try {
                            as.connectionStatus = conn.getState();
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    settings.close();
                }
                return as;
            }

            @Override
            protected void onPostExecute(List<AccountSetting> result) {
                // store
                mBindTask = null;
                // swap
                AccountAdapter.super.swapCursor(mStashCursor);
                if (mListener != null)
                    mListener.onPopulate();
            }
        };
        mBindTask.execute();
    }

    public interface Listener {
        void onPopulate();
    }
}