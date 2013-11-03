package info.guardianproject.otr.app.im.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

public class AccountsFragment extends ListFragment {
        private AccountListActivity mActivity;
        
        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mActivity = (AccountListActivity)activity;
            setListAdapter(mActivity.getAdapter());
            initView();
        }
        
        @Override
        public void onDetach() {
            super.onDetach();
            mActivity = null;
        }
        
        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
        }

        private void initView() {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean themeDark = settings.getBoolean("themeDark", false);
            String themebg = settings.getString("pref_background", null);

/*            ViewGroup godfatherView = (ViewGroup) this.getWindow().getDecorView();

            View emptyView = getLayoutInflater().inflate(R.layout.empty_account_view, godfatherView, false);
            setContentView(android.R.layout.list_content);

            //     emptyView.setVisibility(View.GONE);
            ((ViewGroup)getListView().getParent()).addView(emptyView);

            getListView().setEmptyView(emptyView);

            if (themebg == null && (!themeDark))
            {
                getListView().setBackgroundColor(getResources().getColor(android.R.color.white));
                emptyView.setBackgroundColor(getResources().getColor(android.R.color.white));
            }

            emptyView.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View arg0) {

                    if (getListView().getCount() == 0)
                    {
                        showExistingAccountListDialog();
                    }


                }

            });
*/        }
    }