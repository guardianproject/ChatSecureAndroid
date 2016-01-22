/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.viewpagerindicator.PageIndicator;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.otr.OtrAndroidKeyManagerImpl;
import info.guardianproject.otr.OtrDebugLogger;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.plugin.xmpp.auth.GTalkOAuth2;
import info.guardianproject.otr.app.im.provider.Imps;
import java.util.List;
import java.util.UUID;

public class AccountWizardActivity extends ThemeableActivity {
    private static final String TAG = ImApp.LOG_TAG;

    private AccountAdapter mAdapter;
    private ImApp mApp;
    private SimpleAlertHandler mHandler;

    private SignInHelper mSignInHelper;

    private static final int REQUEST_CREATE_ACCOUNT = RESULT_FIRST_USER + 2;

    private static final String GOOGLE_ACCOUNT = "google_account";
    private static final String EXISTING_ACCOUNT = "existing_account";
    private static final String NEW_ACCOUNT = "new_account";
    private static final String BONJOUR_ACCOUNT = "bonjour_account";
    private static final String BURNER_ACCOUNT = "secret_account";

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle icicle) {

        if(Build.VERSION.SDK_INT >= 11)
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        super.onCreate(icicle);

        getSupportActionBar().hide();

        mApp = (ImApp)getApplication();
        mApp.maybeInit(this);
        mApp.setAppTheme(this);

        if (!Imps.isUnlocked(this)) {
            onDBLocked();
            return;
        }

        mHandler = new MyHandler(this);
        mSignInHelper = new SignInHelper(this);

        buildAccountList();

        setContentView(R.layout.account_list_activity);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new WizardPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        PageIndicator titleIndicator = (PageIndicator) findViewById(R.id.indicator);
        titleIndicator.setViewPager(mPager);
    }

    AccountAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    protected void onPause() {
        mHandler.unregisterForBroadcastEvents();

        super.onPause();

    }

    @Override
    protected void onDestroy() {
        if (mSignInHelper != null) // if !Imps.isUnlocked(this)
            mSignInHelper.stop();

        if (mAdapter != null)
            mAdapter.swapCursor(null);


        unbindDrawables(findViewById(R.id.RootView));
        System.gc();

        super.onDestroy();
    }

    private void unbindDrawables(View view) {
        if (view != null)
        {
            if (view.getBackground() != null) {
                view.getBackground().setCallback(null);
            }
            if (view instanceof ViewGroup) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                    unbindDrawables(((ViewGroup) view).getChildAt(i));
                }
                ((ViewGroup) view).removeAllViews();
            }
        }
    }



    @Override
    protected void onResume() {

        super.onResume();

        mHandler.registerForBroadcastEvents();

    }


    protected void gotoChats()
    {
        Intent intent = new Intent(this, NewChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("showaccounts", true);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    private String[][] mAccountList;
    private String mNewUser;

    private ImPluginHelper helper = ImPluginHelper.getInstance(this);


    Account[] mGoogleAccounts;

    private void buildAccountList ()
    {
        int i = 0;
        int accountProviders = 0;
        
        mGoogleAccounts = AccountManager.get(this).getAccountsByType(GTalkOAuth2.TYPE_GOOGLE_ACCT);

        if (mGoogleAccounts.length > 0) {
            accountProviders = 5; //potentialProviders + google + create account + burner

            mAccountList = new String[accountProviders][3];

            mAccountList[i][0] = getString(R.string.i_want_to_chat_using_my_google_account);
            mAccountList[i][1] = getString(R.string.account_google_full);
            mAccountList[i][2] = GOOGLE_ACCOUNT;
            i++;
        } else {
            accountProviders = 4;//listProviders.size() + 2; //potentialProviders + create account + burner

            mAccountList = new String[accountProviders][3];
        }


        mAccountList[i][0] = getString(R.string.i_have_an_existing_xmpp_account);
        mAccountList[i][1] = getString(R.string.account_existing_full);
        mAccountList[i][2] = EXISTING_ACCOUNT;
        i++;

        mAccountList[i][0] = getString(R.string.i_need_a_new_account);
        mAccountList[i][1] = getString(R.string.account_new_full);
        mAccountList[i][2] = NEW_ACCOUNT;
        i++;

        mAccountList[i][0] = getString(R.string.i_want_to_chat_on_my_local_wifi_network_bonjour_zeroconf_);
        mAccountList[i][1] = getString(R.string.account_wifi_full);
        mAccountList[i][2] = BONJOUR_ACCOUNT;
        i++;

        mAccountList[i][0] = getString(R.string.i_need_a_burner_one_time_throwaway_account_);
        mAccountList[i][1] = getString(R.string.account_burner_full);
        mAccountList[i][2] = BURNER_ACCOUNT;
        i++;

    }

    private Handler mHandlerGoogleAuth = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    private void addGoogleAccount ()
    {
       // mNewUser = newUser;
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                this);
      //  builderSingle.setTitle("Select One Name:-");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.select_dialog_singlechoice);

        for (Account gAccount : mGoogleAccounts)
            arrayAdapter.add(gAccount.name);

        builderSingle.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        mNewUser = arrayAdapter.getItem(which);

                        Thread thread = new Thread ()
                        {
                            @Override
                            public void run ()
                            {
                                //get the oauth token

                              //don't store anything just make sure it works!
                               String password = GTalkOAuth2.NAME + ':' + GTalkOAuth2.getGoogleAuthTokenAllow(mNewUser, getApplicationContext(), AccountWizardActivity.this,mHandlerGoogleAuth);

                               //use the XMPP type plugin for google accounts, and the .NAME "X-GOOGLE-TOKEN" as the password
                                showSetupAccountForm(helper.getProviderNames().get(0), mNewUser,password, false, getString(R.string.google_account),false);
                            }
                        };
                        thread.start();

                    }
                });
        builderSingle.show();
    }


    public void showSetupAccountForm (String providerType, String username, String token, boolean createAccount, String formTitle, boolean hideTor)
    {
        long providerId = helper.createAdditionalProvider(providerType);//xmpp
    //    mApp.resetProviderSettings(); //clear cached provider list

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_INSERT);

        intent.setData(ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId));
        intent.addCategory(ImApp.IMPS_CATEGORY);

        if (username != null)
            intent.putExtra("newuser", username);

        if (token != null)
            intent.putExtra("newpass", token);

        if (formTitle != null)
            intent.putExtra("title", formTitle);

        intent.putExtra("hideTor", hideTor);

        intent.putExtra("register", createAccount);

        startActivityForResult(intent,REQUEST_CREATE_ACCOUNT);
    }

    public void createBurnerAccount ()
    {

        OrbotHelper oh = new OrbotHelper(this);
        if (!oh.isOrbotInstalled())
        {
            oh.promptToInstall(this);
            return;
        }
        else if (!oh.isOrbotRunning())
        {
            oh.requestOrbotStart(this);
            return;
        }

        //need to generate proper IMA url for account setup
        String regUser = java.util.UUID.randomUUID().toString().substring(0,10).replace('-','a');
        String regPass =  UUID.randomUUID().toString().substring(0,16);
        String regDomain = "jabber.calyxinstitute.org";
        Uri uriAccountData = Uri.parse("ima://" + regUser + ':' + regPass + '@' + regDomain);

        Intent intent = new Intent(this, AccountActivity.class);
        intent.setAction(Intent.ACTION_INSERT);
        intent.setData(uriAccountData);
        intent.putExtra("useTor", true);
        startActivityForResult(intent,REQUEST_CREATE_ACCOUNT);


    }

    private final class MyHandler extends SimpleAlertHandler {

        public MyHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ImApp.EVENT_CONNECTION_DISCONNECTED) {
                promptDisconnectedEvent(msg);
            }
            super.handleMessage(msg);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode,
                    data);
            if (scanResult != null) {
                OtrAndroidKeyManagerImpl.handleKeyScanResult(scanResult.getContents(), this);
            }
        }
        else if (requestCode == REQUEST_CREATE_ACCOUNT)
        {
           // if (resultCode == RESULT_OK)
           // {
                gotoChats();
           // }
        }
    }


    public void onDBLocked() {

        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    public static class WizardPageFragment extends Fragment {

        private TextView mAccountInfo = null;
        private TextView mAccountDetail = null;

        private Button mButtonAddAccount = null;
        private String mAccountInfoText = null;
        private String mAccountDetailText = null;
        private String mButtonText = null;
        private OnClickListener mOcl = null;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.account_wizard_slider, container, false);

            mAccountInfo = (TextView)rootView.findViewById(R.id.lblAccountTypeInfo);
            mAccountDetail = (TextView)rootView.findViewById(R.id.lblAccountTypeDetail);

            mButtonAddAccount = (Button)rootView.findViewById(R.id.btnAddAccount);

            if (mButtonText != null)
                mButtonAddAccount.setText(mButtonText);

            mAccountInfo.setText(mAccountInfoText);
            mAccountDetail.setText(mAccountDetailText);
            mButtonAddAccount.setOnClickListener(mOcl);

            setRetainInstance(true);

            return rootView;
        }

        public void setAccountInfo (String accountInfoText, String accountDetailText, String mButtonText)
        {
            mAccountInfoText = accountInfoText;
            mAccountDetailText = accountDetailText;
        }

        public void setOnClickListener(OnClickListener ocl)
        {
            mOcl = ocl;
        }

    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class WizardPagerAdapter extends FragmentStatePagerAdapter {
        public WizardPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int pos) {
            WizardPageFragment wpf = new WizardPageFragment();
            wpf.setAccountInfo(mAccountList[pos][0],mAccountList[pos][1],null);
            wpf.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View v) {
                    String accountType = mAccountList[pos][2];
                    if (TextUtils.equals(accountType, EXISTING_ACCOUNT))
                    {
                        //otherwise support the actual plugin-type
                        showSetupAccountForm(helper.getProviderNames().get(0),null, null, false,helper.getProviderNames().get(0),false);
                    }
                    else if (TextUtils.equals(accountType, BONJOUR_ACCOUNT))
                    {
                        String username = "";
                        String passwordPlaceholder = "password";//zeroconf doesn't need a password
                        showSetupAccountForm(helper.getProviderNames().get(1),username,passwordPlaceholder, false,helper.getProviderNames().get(1),true);
                    }
                    else if (TextUtils.equals(accountType, NEW_ACCOUNT))
                    {
                        showSetupAccountForm(helper.getProviderNames().get(0), null, null, true, null,false);
                    }
                    else if (TextUtils.equals(accountType, BURNER_ACCOUNT))
                    {
                        createBurnerAccount();
                    }
                    else if (TextUtils.equals(accountType, GOOGLE_ACCOUNT))
                    {
                        addGoogleAccount();
                    }
                    else
                        throw new IllegalArgumentException("Mystery account type!");
                }

            });

            return wpf;
        }

        @Override
        public int getCount() {
            return mAccountList.length;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }
}
