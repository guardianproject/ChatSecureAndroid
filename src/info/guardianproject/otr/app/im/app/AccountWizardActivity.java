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
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.viewpagerindicator.PageIndicator;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.otr.OtrAndroidKeyManagerImpl;
import info.guardianproject.otr.OtrDebugLogger;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.plugin.xmpp.auth.GTalkOAuth2;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.util.BackgroundBitmapLoaderTask;

import java.util.List;
import java.util.UUID;

public class AccountWizardActivity extends ThemeableActivity implements View.OnCreateContextMenuListener {

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

        /*
        if (!mHasBackground) {
            LinearLayout rootView = (LinearLayout) findViewById(R.id.RootView);
            BackgroundBitmapLoaderTask task = new BackgroundBitmapLoaderTask(this, rootView);
            task.execute(R.drawable.csbackground);
        }*/

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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void doHardShutdown() {

        for (IImConnection conn : mApp.getActiveConnections())
        {
               try {
                conn.logout();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
        // Request lock
        intent.putExtra("doLock", true);
        // Clear the backstack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 11)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
   }
    public void signOut(final long accountId) {
        // Remember that the user signed out and do not auto sign in until they
        // explicitly do so
        setKeepSignedIn(accountId, false);

        try {
            IImConnection conn = mApp.getConnectionByAccount(accountId);
            if (conn != null) {
                conn.logout();
            }
        } catch (Exception ex) {
            Log.e(TAG, "signOut failed", ex);
        }
    }


    private void setKeepSignedIn(final long accountId, boolean signin) {
        Uri mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
        ContentValues values = new ContentValues();
        values.put(Imps.Account.KEEP_SIGNED_IN, signin);
        getContentResolver().update(mAccountUri, values, null, null);
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
        List<String> listProviders = helper.getProviderNames();

        mGoogleAccounts = AccountManager.get(this).getAccountsByType(GTalkOAuth2.TYPE_GOOGLE_ACCT);

        if (mGoogleAccounts.length > 0) {
            accountProviders = listProviders.size() + 3; //potentialProviders + google + create account + burner

            mAccountList = new String[accountProviders][3];

            mAccountList[i][0] = getString(R.string.i_want_to_chat_using_my_google_account);
            mAccountList[i][1] = getString(R.string.account_google_full);
            mAccountList[i][2] = GOOGLE_ACCOUNT;
            i++;
        } else {
            accountProviders = listProviders.size() + 2; //potentialProviders + create account + burner

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

    /* CHANGE phoenix_nz - add "Create Account" to List */
    /**
    void showExistingAccountListDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.account_select_type);



        builder.setItems(mAccountList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {

                if (pos == 0) //xmpp
                {
                    //otherwise support the actual plugin-type
                    showSetupAccountForm(helper.getProviderNames().get(0),null, null, false,helper.getProviderNames().get(0),false);
                }
                else if (pos == mAccountList.length-2) //create account
                {
                    String username = "";
                    String passwordPlaceholder = "password";//zeroconf doesn't need a password
                    showSetupAccountForm(helper.getProviderNames().get(1),username,passwordPlaceholder, false,helper.getProviderNames().get(1),true);
                }
                else if (pos == mAccountList.length-3) //create account
                {
                    showSetupAccountForm(helper.getProviderNames().get(0), null, null, true, null,false);
                }
                else if (pos == mAccountList.length-1) //create account
                {
                    createBurnerAccount();
                }
                else
                {
                    addGoogleAccount(mGoogleAccounts[pos-1].name);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }
    */

    private Handler mHandlerGoogleAuth = new Handler ()
    {

        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);

           // Log.d(TAG,"Got handler callback from auth: " + msg.what);
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
        mApp.resetProviderSettings(); //clear cached provider list

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




    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        /*
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor providerCursor = (Cursor) getListAdapter().getItem(info.position);
        menu.setHeaderTitle(providerCursor.getString(PROVIDER_FULLNAME_COLUMN));

        if (providerCursor.isNull(ACTIVE_ACCOUNT_ID_COLUMN)) {
            menu.add(0, ID_ADD_ACCOUNT, 0, R.string.menu_edit_account);
            menu.add(0, ID_REMOVE_ACCOUNT, 0, R.string.menu_remove_account).setIcon(
                    android.R.drawable.ic_menu_delete);
            return;
        }

        long providerId = providerCursor.getLong(PROVIDER_ID_COLUMN);
        boolean isLoggingIn = isSigningIn(providerCursor);
        boolean isLoggedIn = isSignedIn(providerCursor);

        BrandingResources brandingRes = mApp.getBrandingResource(providerId);
        //menu.add(0, ID_VIEW_CONTACT_LIST, 0,
          //      brandingRes.getString(BrandingResourceIDs.STRING_MENU_CONTACT_LIST));

        if (isLoggedIn) {
            menu.add(0, ID_SIGN_OUT, 0, R.string.menu_sign_out).setIcon(
                    android.R.drawable.ic_menu_close_clear_cancel);
        } else if (isLoggingIn) {
            menu.add(0, ID_SIGN_OUT, 0, R.string.menu_cancel_signin).setIcon(
                    android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            menu.add(0, ID_SIGN_IN, 0, R.string.sign_in)
            // TODO .setIcon(info.guardianproject.otr.app.internal.R.drawable.ic_menu_login)
            ;
        }

        boolean isAccountEditable = providerCursor.getInt(ACTIVE_ACCOUNT_LOCKED) == 0;
        if (isAccountEditable && !isLoggingIn && !isLoggedIn) {
            menu.add(0, ID_EDIT_ACCOUNT, 0, R.string.menu_edit_account).setIcon(
                    android.R.drawable.ic_menu_edit);
            menu.add(0, ID_REMOVE_ACCOUNT, 0, R.string.menu_remove_account).setIcon(
                    android.R.drawable.ic_menu_delete);
        }*/
    }


    @SuppressWarnings("deprecation")
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        /*
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        long providerId = info.id;
        Cursor providerCursor = (Cursor) getListAdapter().getItem(info.position);
        long accountId = providerCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN);

        mProviderCursor.moveToPosition(info.position);
                    Intent intent = new Intent(getContext(), NewChatActivity.class);
                    intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
                    getContext().startActivity(intent);

        switch (item.getItemId()) {
        case ID_EDIT_ACCOUNT: {
            startActivity(getEditAccountIntent());
            return true;
        }

        case ID_REMOVE_ACCOUNT: {
            Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
            getContentResolver().delete(accountUri, null, null);
            Uri providerUri = ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId);
            getContentResolver().delete(providerUri, null, null);
            // Requery the cursor to force refreshing screen
            providerCursor.requery();
            return true;
        }

        case ID_VIEW_CONTACT_LIST: {
            Intent intent = getViewContactsIntent();
            startActivity(intent);
            return true;
        }
        case ID_ADD_ACCOUNT: {

            showNewAccountListDialog();

            return true;
        }

        case ID_SIGN_IN: {
            signIn(accountId);
            return true;
        }

        case ID_SIGN_OUT: {
            // TODO: progress bar
            signOut(accountId);
            return true;
        }

        }
    */

        return false;
    }

    /*
    Intent getCreateAccountIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_INSERT);

        long providerId = mProviderCursor.getLong(PROVIDER_ID_COLUMN);
        intent.setData(ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId));
        intent.addCategory(getProviderCategory(mProviderCursor));
        return intent;
    }*/


    /*
    Intent getViewChatsIntent() {
        Intent intent = new Intent(this, ChatListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mProviderCursor.getLong(ACTIVE_ACCOUNT_ID_COLUMN));
        return intent;
    }*/

    /*
    private String getProviderCategory(Cursor cursor) {
        return cursor.getString(PROVIDER_CATEGORY_COLUMN);
    }*/

    static void log(String msg) {
        Log.d(TAG, "[LandingPage]" + msg);
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

        if (requestCode == IntentIntegrator.REQUEST_CODE)
        {

          Object keyMan = null;
          boolean keyStoreImported = false;

            try {

                keyStoreImported = OtrAndroidKeyManagerImpl.handleKeyScanResult(requestCode, resultCode, data, this);

            } catch (Exception e) {
                OtrDebugLogger.log("error importing keystore",e);
            }

            if (keyStoreImported)
            {
                Toast.makeText(this, R.string.successfully_imported_otr_keyring, Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, R.string.otr_keyring_not_imported_please_check_the_file_exists_in_the_proper_format_and_location, Toast.LENGTH_SHORT).show();

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
