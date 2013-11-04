package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;

public class AccountsFragment extends ListFragment {
        private AccountListActivity mActivity;
        private View mEmptyView;
        
        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mActivity = (AccountListActivity)activity;
        }
        
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            setListAdapter(mActivity.getAdapter());
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mActivity = null;
        }
        
        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mEmptyView = inflater.inflate(R.layout.empty_account_view, container, false);

            mEmptyView.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View arg0) {

                    if (getListView().getCount() == 0)
                    {
                        mActivity.showExistingAccountListDialog();
                    }
                }

            });

            return super.onCreateView(inflater, container, savedInstanceState);
        }
        
        public void onViewCreated(View view, Bundle savedInstanceState) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean themeDark = settings.getBoolean("themeDark", false);
            String themebg = settings.getString("pref_background", null);

            ((ViewGroup)getListView().getParent()).addView(mEmptyView);

            getListView().setEmptyView(mEmptyView);

            if (themebg == null && (!themeDark))
            {
                getListView().setBackgroundColor(getResources().getColor(android.R.color.white));
                mEmptyView.setBackgroundColor(getResources().getColor(android.R.color.white));
            }
        }
    }