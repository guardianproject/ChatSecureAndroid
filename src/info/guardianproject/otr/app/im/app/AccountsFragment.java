package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;

public class AccountsFragment extends ListFragment implements ProviderListItem.SignInManager {

        private FragmentActivity mActivity;
        private int mAccountLayoutView;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            mActivity = (FragmentActivity)activity;

            mAccountLayoutView = R.layout.account_view;
            if (mActivity instanceof NewChatActivity)
                mAccountLayoutView = R.layout.account_view_sidebar;


            initProviderCursor();

            
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mActivity = null;
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {


            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            super.onViewCreated(view, savedInstanceState);
        }

        private void initProviderCursor()
        {
            final Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;

            mAdapter = new AccountAdapter(mActivity, new ProviderListItemFactory(), mAccountLayoutView);
            setListAdapter(mAdapter);

            mActivity.getSupportLoaderManager().initLoader(ACCOUNT_LOADER_ID, null, new LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    CursorLoader loader = new CursorLoader(mActivity, uri, PROVIDER_PROJECTION,
                            Imps.Provider.CATEGORY + "=?" + " AND " + Imps.Provider.ACTIVE_ACCOUNT_USERNAME + " NOT NULL" /* selection */,
                            new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                            Imps.Provider.DEFAULT_SORT_ORDER);
                    loader.setUpdateThrottle(100l);

                    return loader;
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {
                    mAdapter.swapCursor(newCursor);
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                    mAdapter.swapCursor(null);

                }
            });

        }


        private class ProviderListItemFactory implements LayoutInflater.Factory {
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                if (name != null && name.equals(ProviderListItem.class.getName())) {
                    return new ProviderListItem(context, mActivity, AccountsFragment.this);
                }
                return null;
            }
        }


        public void signIn(long accountId) {
            if (accountId <= 0) {
                return;
            }
            Cursor cursor = mAdapter.getCursor();

            cursor.moveToFirst();
            while (!cursor.isAfterLast())
            {
                long cAccountId = cursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);

                if (cAccountId == accountId)
                    break;

                cursor.moveToNext();
            }

            // Remember that the user signed in.
            setKeepSignedIn(accountId, true);

            long providerId = cursor.getLong(PROVIDER_ID_COLUMN);
            String password = cursor.getString(ACTIVE_ACCOUNT_PW_COLUMN);

            boolean isActive = false; // TODO(miron)

            new SignInHelper(mActivity).signIn(password, providerId, accountId, isActive);

            cursor.moveToPosition(-1);
        }


        public void signOut(final long accountId) {
            // Remember that the user signed out and do not auto sign in until they
            // explicitly do so
            setKeepSignedIn(accountId, false);

            try {
                IImConnection conn =  ((ImApp)mActivity.getApplication()).getConnectionByAccount(accountId);
                if (conn != null) {
                    conn.logout();
                }
            } catch (Exception ex) {
            }
        }

        private void setKeepSignedIn(final long accountId, boolean signin) {
            Uri mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
            ContentValues values = new ContentValues();
            values.put(Imps.Account.KEEP_SIGNED_IN, signin);
            mActivity.getContentResolver().update(mAccountUri, values, null, null);
        }

        AccountAdapter mAdapter;

        private static final String[] PROVIDER_PROJECTION = {
                                                             Imps.Provider._ID,
                                                             Imps.Provider.NAME,
                                                             Imps.Provider.FULLNAME,
                                                             Imps.Provider.CATEGORY,
                                                             Imps.Provider.ACTIVE_ACCOUNT_ID,
                                                             Imps.Provider.ACTIVE_ACCOUNT_USERNAME,
                                                             Imps.Provider.ACTIVE_ACCOUNT_PW,
                                                             Imps.Provider.ACTIVE_ACCOUNT_LOCKED,
                                                             Imps.Provider.ACTIVE_ACCOUNT_KEEP_SIGNED_IN,
                                                             Imps.Provider.ACCOUNT_PRESENCE_STATUS,
                                                             Imps.Provider.ACCOUNT_CONNECTION_STATUS
                                                            };

        static final int PROVIDER_ID_COLUMN = 0;
        static final int PROVIDER_NAME_COLUMN = 1;
        static final int PROVIDER_FULLNAME_COLUMN = 2;
        static final int PROVIDER_CATEGORY_COLUMN = 3;
        static final int ACTIVE_ACCOUNT_ID_COLUMN = 4;
        static final int ACTIVE_ACCOUNT_USERNAME_COLUMN = 5;
        static final int ACTIVE_ACCOUNT_PW_COLUMN = 6;
        static final int ACTIVE_ACCOUNT_LOCKED = 7;
        static final int ACTIVE_ACCOUNT_KEEP_SIGNED_IN = 8;
        static final int ACCOUNT_PRESENCE_STATUS = 9;
        static final int ACCOUNT_CONNECTION_STATUS = 10;

        private static final int ACCOUNT_LOADER_ID = 1000;

    }