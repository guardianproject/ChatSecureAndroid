/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package info.guardianproject.otr.app.im.app;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.otr.IOtrChatSession;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.plugin.xmpp.XmppConnection;
import info.guardianproject.otr.app.im.plugin.xmpp.auth.GTalkOAuth2;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.Imps.AccountColumns;
import info.guardianproject.otr.app.im.provider.Imps.AccountStatusColumns;
import info.guardianproject.otr.app.im.provider.Imps.CommonPresenceColumns;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.util.LogCleaner;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AccountActivity extends Activity {

    public static final String TAG = "AccountActivity";
    private static final String ACCOUNT_URI_KEY = "accountUri";
    private long mProviderId = 0;
    private long mAccountId = 0;
    static final int REQUEST_SIGN_IN = RESULT_FIRST_USER + 1;
    private static final String[] ACCOUNT_PROJECTION = { Imps.Account._ID, Imps.Account.PROVIDER,
                                                        Imps.Account.USERNAME,
                                                        Imps.Account.PASSWORD,
                                                        Imps.Account.KEEP_SIGNED_IN,
                                                        Imps.Account.LAST_LOGIN_STATE };
    private static final int ACCOUNT_PROVIDER_COLUMN = 1;
    private static final int ACCOUNT_USERNAME_COLUMN = 2;
    private static final int ACCOUNT_PASSWORD_COLUMN = 3;
    
    public final static String DEFAULT_SERVER_GOOGLE = "talk.l.google.com";
    public final static String DEFAULT_SERVER_FACEBOOK = "chat.facebook.com";
    public final static String DEFAULT_SERVER_JABBERORG = "hermes2.jabber.org";
    public final static String DEFAULT_SERVER_DUKGO = "dukgo.com";
    public final static String ONION_JABBERCCC = "okj7xc6j2szr2y75.onion";
    
    //    private static final int ACCOUNT_KEEP_SIGNED_IN_COLUMN = 4;
    //    private static final int ACCOUNT_LAST_LOGIN_STATE = 5;

    Uri mAccountUri;
    EditText mEditUserAccount;
    EditText mEditPass;
    EditText mEditPassConfirm;
    CheckBox mRememberPass;
    CheckBox mUseTor;
    Button mBtnSignIn;
    Button mBtnDelete;
    Spinner mSpinnerDomains;
    
    Button mBtnAdvanced;
    TextView mTxtFingerprint;

    //Imps.ProviderSettings.QueryMap settings;
    
    boolean isEdit = false;
    boolean isSignedIn = false;

    String mUserName = "";
    String mDomain = "";
    int mPort = 0;
    private String mOriginalUserAccount = "";

    private final static int DEFAULT_PORT = 5222;

    IOtrChatSession mOtrChatSession;
    private SignInHelper mSignInHelper;

    private boolean mIsNewAccount = false;

    private AsyncTask<String, Void, String> mCreateAccountTask = null;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent i = getIntent();
        
        mApp = (ImApp)getApplication();

        String action = i.getAction();

        if (i.hasExtra("isSignedIn"))
            isSignedIn = i.getBooleanExtra("isSignedIn", false);
        

        final ProviderDef provider;
        
        mSignInHelper = new SignInHelper(this);
        SignInHelper.SignInListener signInListener = new SignInHelper.SignInListener() {
            public void connectedToService() {
            }
            public void stateChanged(int state, long accountId) {
                if (state == ImConnection.LOGGING_IN || state == ImConnection.LOGGED_IN)
                {
                    mSignInHelper.goToAccount(accountId);
                    finish();
                }
            }
        };
        
        mSignInHelper.setSignInListener(signInListener);
        

        ContentResolver cr = getContentResolver();

        Uri uri = i.getData();
        // check if there is account information and direct accordingly
        if (Intent.ACTION_INSERT_OR_EDIT.equals(action)) {
            if ((uri == null) || !Imps.Account.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {
                action = Intent.ACTION_INSERT;
            } else {
                action = Intent.ACTION_EDIT;
            }
        }

        if (Intent.ACTION_INSERT.equals(action) && uri.getScheme().equals("ima")) {
            ImPluginHelper helper = ImPluginHelper.getInstance(this);
            String authority = uri.getAuthority();
            String[] userpass_host = authority.split("@");
            String[] user_pass = userpass_host[0].split(":");
            mUserName = user_pass[0];
            String pass = user_pass[1];
            mDomain = userpass_host[1];
            mPort = 0;
            Cursor cursor = openAccountByUsernameAndDomain(cr);
            boolean exists = cursor.moveToFirst();
            long accountId;
            if (exists) {
                accountId = cursor.getLong(0);
                mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
                pass = cursor.getString(ACCOUNT_PASSWORD_COLUMN);
                
                setAccountKeepSignedIn(true);
                mSignInHelper.activateAccount(mProviderId, accountId);
                mSignInHelper.signIn(pass, mProviderId, accountId, true);
                setResult(RESULT_OK);
                cursor.close();
                finish();
                return;
                
            } else {
                mProviderId = helper.createAdditionalProvider(helper.getProviderNames().get(0)); //xmpp FIXME
                accountId = ImApp.insertOrUpdateAccount(cr, mProviderId, mUserName, pass);
                mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
                mSignInHelper.activateAccount(mProviderId, accountId);
                createNewAccount(mUserName, pass, accountId);
                cursor.close();
                return;
            }
           
           
        
            
        } else if (Intent.ACTION_INSERT.equals(action)) {
            

            setupUIPre();
            
            mOriginalUserAccount = "";
            // TODO once we implement multiple IM protocols
            mProviderId = ContentUris.parseId(uri);
            provider = mApp.getProvider(mProviderId);

            if (provider != null)
            {
                setTitle(getResources().getString(R.string.add_account, provider.mFullName));
    
            }
            else
            {
                finish();
            }

            
        } else if (Intent.ACTION_EDIT.equals(action)) {
            

            setupUIPre();
            
            if ((uri == null) || !Imps.Account.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {
                LogCleaner.warn(ImApp.LOG_TAG, "<AccountActivity>Bad data");
                return;
            }

            isEdit = true;

            Cursor cursor = cr.query(uri, ACCOUNT_PROJECTION, null, null, null);

            if (cursor == null) {
                finish();
                return;
            }

            if (!cursor.moveToFirst()) {
                cursor.close();
                finish();
                return;
            }

            setTitle(R.string.sign_in);

            mAccountId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));

            mProviderId = cursor.getLong(ACCOUNT_PROVIDER_COLUMN);
            provider = mApp.getProvider(mProviderId);

            Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                    cr, mProviderId, false /* don't keep updated */, null /* no handler */);

            try {
                mOriginalUserAccount = cursor.getString(ACCOUNT_USERNAME_COLUMN) + "@"
                                       + settings.getDomain();
                mEditUserAccount.setText(mOriginalUserAccount);
                mEditPass.setText(cursor.getString(ACCOUNT_PASSWORD_COLUMN));
                mRememberPass.setChecked(!cursor.isNull(ACCOUNT_PASSWORD_COLUMN));
                mUseTor.setChecked(settings.getUseTor());
                mBtnDelete.setVisibility(View.VISIBLE);
            } finally {
                settings.close();
                cursor.close();
            }


        } else {
            LogCleaner.warn(ImApp.LOG_TAG, "<AccountActivity> unknown intent action " + action);
            finish();
            return;
        }

       setupUIPost();

    }
    
    private void setupUIPre ()
    {
        setContentView(R.layout.account_activity);
        
        mIsNewAccount = getIntent().getBooleanExtra("register", false);
        
        
        mEditUserAccount = (EditText) findViewById(R.id.edtName);
        mEditUserAccount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                checkUserChanged();
            }
        });

        mEditPass = (EditText) findViewById(R.id.edtPass);
        
        mEditPassConfirm = (EditText) findViewById(R.id.edtPassConfirm);
        mSpinnerDomains = (Spinner) findViewById(R.id.spinnerDomains);
        
        if (mIsNewAccount)
        {
            mEditPassConfirm.setVisibility(View.VISIBLE);
            mSpinnerDomains.setVisibility(View.VISIBLE);
            mEditUserAccount.setHint(R.string.account_setup_new_username);
        }
        
        mRememberPass = (CheckBox) findViewById(R.id.rememberPassword);
        mUseTor = (CheckBox) findViewById(R.id.useTor);
       

        mBtnSignIn = (Button) findViewById(R.id.btnSignIn);
        
        if (mIsNewAccount)
            mBtnSignIn.setText(R.string.btn_create_account);
        
        mBtnAdvanced = (Button) findViewById(R.id.btnAdvanced);
        mBtnDelete = (Button) findViewById(R.id.btnDelete);
        
        mRememberPass.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateWidgetState();
            }
        });

    }
    
    private void setupUIPost ()
    {
        Intent i = getIntent();
        
        if (isSignedIn) {
            mBtnSignIn.setText(getString(R.string.menu_sign_out));
            mBtnSignIn.setBackgroundResource(R.drawable.btn_red);
        }

        final BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);

        mEditUserAccount.addTextChangedListener(mTextWatcher);
        mEditPass.addTextChangedListener(mTextWatcher);

        mBtnAdvanced.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showAdvanced();
            }
        });
        
        mBtnDelete.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v) {
               
                deleteAccount();
                finish();
                
            }
            
        });

        mBtnSignIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                checkUserChanged();
                
                if (mUseTor.isChecked())
                {
                    OrbotHelper oh = new OrbotHelper(AccountActivity.this);
                    if (!oh.isOrbotRunning())
                    {
                        oh.requestOrbotStart(AccountActivity.this);
                        return;
                    }
                }
                

                final String pass = mEditPass.getText().toString();
                final String passConf = mEditPassConfirm.getText().toString();
                final boolean rememberPass = mRememberPass.isChecked();
                final boolean isActive = false; // TODO(miron) does this ever need to be true?
                ContentResolver cr = getContentResolver();

                if (mIsNewAccount)
                {
                    mDomain = (String)mSpinnerDomains.getSelectedItem();
                    String fullUser = mEditUserAccount.getText().toString();
                    
                    if (fullUser.indexOf("@")==-1)
                        fullUser += '@' + mDomain;
                    
                    if (!parseAccount(fullUser)) {
                        mEditUserAccount.selectAll();
                        mEditUserAccount.requestFocus();
                        return;
                    }
                    
                    ImPluginHelper helper = ImPluginHelper.getInstance(AccountActivity.this);
                    mProviderId = helper.createAdditionalProvider(helper.getProviderNames().get(0)); //xmpp FIXME

                }
                else
                {
                    if (!parseAccount(mEditUserAccount.getText().toString())) {
                        mEditUserAccount.selectAll();
                        mEditUserAccount.requestFocus();
                        return;
                    }
                    else
                    {
                        settingsForDomain(mDomain,mPort);//apply final settings
                    }
                }

     
                mAccountId = ImApp.insertOrUpdateAccount(cr, mProviderId, mUserName,
                        rememberPass ? pass : null);
                
                mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, mAccountId);
                
                //if remember pass is true, set the "keep signed in" property to true
                if (mIsNewAccount)
                {
                    if (pass.equals(passConf))
                    {
                        createNewAccount(mUserName, pass, mAccountId);
                        setAccountKeepSignedIn(rememberPass);
                        mSignInHelper.activateAccount(mProviderId, mAccountId);
                       // setResult(RESULT_OK);
                        //mSignInHelper.signIn(pass, mProviderId, accountId, isActive);
                        //isSignedIn = true;
                        //updateWidgetState();
                       // finish();
                    }
                    else
                    {
                       Toast.makeText(AccountActivity.this, "Your passwords do not match", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    if (isSignedIn) {
                        signOut();
                        isSignedIn = false;
                    } else {
                        setAccountKeepSignedIn(rememberPass);
                        
                        if (!mOriginalUserAccount.equals(mUserName + '@' + mDomain)
                            && shouldShowTermOfUse(brandingRes)) {
                            confirmTermsOfUse(brandingRes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mSignInHelper.signIn(pass, mProviderId, mAccountId, isActive);
                                }
                            });
                        } else {
                            mSignInHelper.signIn(pass, mProviderId, mAccountId, isActive);
                        }
                      
                        isSignedIn = true;
                    }
                    updateWidgetState();
                    setResult(RESULT_OK);
                    finish();
                }
                
            }
        });
        
        mUseTor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateUseTor(isChecked);
            }
        });

        updateWidgetState();
        
        if (i.hasExtra("title"))
        {
            String title = i.getExtras().getString("title");
            setTitle(title);
        }
        
        if (i.hasExtra("newuser"))
        {
            String newuser = i.getExtras().getString("newuser");
            mEditUserAccount.setText(newuser);
            
            parseAccount(newuser);
            settingsForDomain(mDomain,mPort);
            
        }
        
        if (i.hasExtra("newpass"))
        {
            mEditPass.setText(i.getExtras().getString("newpass"));
            mEditPass.setVisibility(View.GONE);
            mRememberPass.setChecked(true);
            mRememberPass.setVisibility(View.GONE);
        }

        if (i.getBooleanExtra("hideTor", false))
        {
            mUseTor.setVisibility(View.GONE);
        }
    }

    private Cursor openAccountByUsernameAndDomain(ContentResolver cr) {
        String clauses = Imps.Account.USERNAME + " = ? AND " + Imps.ProviderSettings.VALUE + " = ?";
        String args[] = new String[2];
        args[0] = mUserName;
        args[1] = mDomain;

        String[] projection = { Imps.Account._ID };
        Cursor cursor = cr.query(Imps.Account.BY_DOMAIN_URI, projection, clauses, args, null);
        return cursor;
    }
    
    @Override
    protected void onDestroy() {
       
        if (mCreateAccountTask != null && (!mCreateAccountTask.isCancelled()))
        {
            mCreateAccountTask.cancel(true);           
        }
        
        if (mSignInHelper != null)
            mSignInHelper.stop();
                
        super.onDestroy();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            checkUserChanged();
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void updateUseTor(boolean useTor) {
        checkUserChanged();
    
        OrbotHelper orbotHelper = new OrbotHelper(this);
        
        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
               getContentResolver(), mProviderId, false /* don't keep updated */, null /* no handler */);

        if (useTor && (!orbotHelper.isOrbotInstalled()))
        {
            //Toast.makeText(this, "Orbot app is not installed. Please install from Google Play or from https://guardianproject.info/releases", Toast.LENGTH_LONG).show();
            
            orbotHelper.promptToInstall(this);
            
            mUseTor.setChecked(false);
            settings.setUseTor(false);
        }
        else
        {
            settings.setUseTor(useTor);
        }
        
        settingsForDomain(settings.getDomain(),settings.getPort(),settings);
        settings.close();
    }
/*
    private void getOTRKeyInfo() {

        if (mApp != null && FFF != null) {
            try {
                otrKeyManager = mApp.getRemoteImService().getOtrKeyManager(mOriginalUserAccount);

                if (otrKeyManager == null) {
                    mTxtFingerprint = ((TextView) findViewById(R.id.txtFingerprint));

                    String localFingerprint = otrKeyManager.getLocalFingerprint();
                    if (localFingerprint != null) {
                        ((TextView) findViewById(R.id.lblFingerprint)).setVisibility(View.VISIBLE);
                        mTxtFingerprint.setText(processFingerprint(localFingerprint));
                    } else {
                        ((TextView) findViewById(R.id.lblFingerprint)).setVisibility(View.GONE);
                        mTxtFingerprint.setText("");
                    }
                } else {
                    //don't need to notify people if there is nothing to show here
//                    Toast.makeText(this, "OTR is not initialized yet", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Log.e(ImApp.LOG_TAG, "error on create", e);

            }
        }

    }*/

    private void checkUserChanged() {
        String username = mEditUserAccount.getText().toString().trim();

        if ((!username.equals(mOriginalUserAccount)) && parseAccount(username)) {
            //Log.i(TAG, "Username changed: " + mOriginalUserAccount + " != " + username);
            settingsForDomain(mDomain, mPort);
            mOriginalUserAccount = username;
            
        }
        
        
        
    }
    
    boolean parseAccount(String userField) {
        boolean isGood = true;
        String[] splitAt = userField.trim().split("@");
        mUserName = splitAt[0];
        mDomain = "";
        mPort = 0;

        if (splitAt.length > 1) {
            mDomain = splitAt[1].toLowerCase();
            String[] splitColon = mDomain.split(":");
            mDomain = splitColon[0];
            if (splitColon.length > 1) {
                try {
                    mPort = Integer.parseInt(splitColon[1]);
                } catch (NumberFormatException e) {
                    // TODO move these strings to strings.xml
                    isGood = false;
                    Toast.makeText(
                            AccountActivity.this,
                            "The port value '" + splitColon[1]
                                    + "' after the : could not be parsed as a number!",
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        //its okay if domain is null;
        
//        if (mDomain == null) {
  //          isGood = false;
            //Toast.makeText(AccountActivity.this, 
            //	R.string.account_wizard_no_domain_warning,
            //	Toast.LENGTH_LONG).show();
    //    } 
        /*//removing requirement of a . in the domain
        else if (mDomain.indexOf(".") == -1) { 
            isGood = false;
            //	Toast.makeText(AccountActivity.this, 
            //		R.string.account_wizard_no_root_domain_warning,
            //	Toast.LENGTH_LONG).show();
        }*/

        return isGood;
    }

    /*
     * If we know the direct XMPP server for a domain, we should turn off DNS lookup
     * because it is slow, error prone, and a way to leak information from third parties
     */
    void settingsForDomain(String domain,int port) {

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                getContentResolver(), mProviderId, false /* don't keep updated */, null /* no handler */);
    
        settingsForDomain(domain, port, settings);
   
        settings.close();
   
    }

    private void settingsForDomain(String domain, int port, Imps.ProviderSettings.QueryMap settings) {
        if (domain.equals("gmail.com")) {
            // Google only supports a certain configuration for XMPP:
            // http://code.google.com/apis/talk/open_communications.html
            
            settings.setDoDnsSrv(false);
            settings.setServer(DEFAULT_SERVER_GOOGLE);            
            settings.setDomain(domain);
            settings.setPort(DEFAULT_PORT);
            settings.setRequireTls(true);
            settings.setTlsCertVerify(true);
            settings.setAllowPlainAuth(false);
        } 
        //mEditPass can be NULL if this activity is used in "headless" mode for auto account setup
        else if (mEditPass != null && mEditPass.getText().toString().startsWith(GTalkOAuth2.NAME))
        {
            //this is not @gmail but IS a google account
            settings.setDoDnsSrv(false);
            settings.setServer(DEFAULT_SERVER_GOOGLE); //set the google connect server
            settings.setDomain(domain);
            settings.setPort(DEFAULT_PORT);
            settings.setRequireTls(true);
            settings.setTlsCertVerify(true);
            settings.setAllowPlainAuth(false);
        }
        else if (domain.equals("jabber.org")) {
            settings.setDoDnsSrv(false);
            settings.setServer(DEFAULT_SERVER_JABBERORG);            
            settings.setDomain(domain);
            settings.setPort(DEFAULT_PORT);            
            settings.setRequireTls(true);
            settings.setTlsCertVerify(true);
            settings.setAllowPlainAuth(false);
        } else if (domain.equals("facebook.com")) {
            settings.setDoDnsSrv(false);
            settings.setDomain(DEFAULT_SERVER_FACEBOOK);
            settings.setPort(DEFAULT_PORT);
            settings.setServer(DEFAULT_SERVER_FACEBOOK);
            settings.setRequireTls(true); //facebook TLS now seems to be on
            settings.setTlsCertVerify(true); //but cert verify can still be funky - off by default
            settings.setAllowPlainAuth(false);
        } 
        else if (domain.equals("jabber.ccc.de")) {
            
            if (settings.getUseTor())
            {                
                settings.setDoDnsSrv(false);
                settings.setServer(ONION_JABBERCCC);
            }
            else
            {
                settings.setDoDnsSrv(true);
                settings.setServer("");
            }
            
            settings.setDomain(domain);
            settings.setPort(DEFAULT_PORT);            
            settings.setRequireTls(true);
            settings.setTlsCertVerify(true);
            settings.setAllowPlainAuth(false);
        }        
        else {
          
            settings.setDomain(domain);
            settings.setPort(port);
            
            //if use Tor, turn off DNS resolution, and set Server manually from Domain
            if (settings.getUseTor())
            {
                settings.setDoDnsSrv(false);
                
                //if Tor is off, and the user has not provided any values here, set to the @domain
                if (settings.getServer() == null || settings.getServer().length() == 0)
                    settings.setServer(domain);
            }
            else if (settings.getServer() == null || settings.getServer().length() == 0)
            {
                //if Tor is off, and the user has not provided any values here, then reset to nothing
                settings.setDoDnsSrv(true);
                settings.setServer("");
            }
            
            settings.setRequireTls(true);
            settings.setTlsCertVerify(true);
            settings.setAllowPlainAuth(false);
        }
        
        settings.requery();
    }

    void confirmTermsOfUse(BrandingResources res, DialogInterface.OnClickListener accept) {
        SpannableString message = new SpannableString(
                res.getString(BrandingResourceIDs.STRING_TOU_MESSAGE));
        Linkify.addLinks(message, Linkify.ALL);

        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(res.getString(BrandingResourceIDs.STRING_TOU_TITLE)).setMessage(message)
                .setPositiveButton(res.getString(BrandingResourceIDs.STRING_TOU_DECLINE), null)
                .setNegativeButton(res.getString(BrandingResourceIDs.STRING_TOU_ACCEPT), accept)
                .show();
    }

    boolean shouldShowTermOfUse(BrandingResources res) {
        return !TextUtils.isEmpty(res.getString(BrandingResourceIDs.STRING_TOU_MESSAGE));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAccountUri = savedInstanceState.getParcelable(ACCOUNT_URI_KEY);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ACCOUNT_URI_KEY, mAccountUri);
    }

    void signOutUsingActivity() {

        Intent intent = new Intent(AccountActivity.this, SignoutActivity.class);
        intent.setData(mAccountUri);

        startActivity(intent);
    }

    private Handler mHandler = new Handler();
    private ImApp mApp = null;

    void signOut() {
        //if you are signing out, then we will deactive "auto" sign in
        ContentValues values = new ContentValues();
        values.put(AccountColumns.KEEP_SIGNED_IN, 0);
        getContentResolver().update(mAccountUri, values, null, null);

        mApp = (ImApp)getApplication();
        
        mApp.callWhenServiceConnected(mHandler, new Runnable() {
            @Override
            public void run() {

                signOut(mProviderId, mAccountId);
            }
        });

    }

    void signOut(long providerId, long accountId) {

        try {

            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                conn.logout();
            } else {
                // Normally, we can always get the connection when user chose to
                // sign out. However, if the application crash unexpectedly, the
                // status will never be updated. Clear the status in this case
                // to make it recoverable from the crash.
                ContentValues values = new ContentValues(2);
                values.put(AccountStatusColumns.PRESENCE_STATUS, CommonPresenceColumns.OFFLINE);
                values.put(AccountStatusColumns.CONNECTION_STATUS, Imps.ConnectionStatus.OFFLINE);
                String where = AccountStatusColumns.ACCOUNT + "=?";
                getContentResolver().update(Imps.AccountStatus.CONTENT_URI, values, where,
                        new String[] { Long.toString(accountId) });
            }
        } catch (RemoteException ex) {
            Log.e(ImApp.LOG_TAG, "signout: caught ", ex);
        } finally {

        //    Toast.makeText(this,
        //            getString(R.string.signed_out_prompt, this.mEditUserAccount.getText()),
        //            Toast.LENGTH_SHORT).show();
            isSignedIn = false;

            mBtnSignIn.setText(getString(R.string.sign_in));
            mBtnSignIn.setBackgroundResource(R.drawable.btn_green);
        }
    }
    
    void createNewaccount (long accountId) 
    {
       
            ContentValues values = new ContentValues(2);

            values.put(AccountStatusColumns.PRESENCE_STATUS, CommonPresenceColumns.NEW_ACCOUNT);
            values.put(AccountStatusColumns.CONNECTION_STATUS, Imps.ConnectionStatus.OFFLINE);
            String where = AccountStatusColumns.ACCOUNT + "=?";
            getContentResolver().update(Imps.AccountStatus.CONTENT_URI, values, where,
                    new String[] { Long.toString(accountId) });
            
        
       
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == REQUEST_SIGN_IN) {
            if (resultCode == RESULT_OK) {

                finish();
            } else {
                // sign in failed, let's show the screen!
            }
        }
    }

    void updateWidgetState() {
        boolean goodUsername = mEditUserAccount.getText().length() > 0;
        boolean goodPassword = mEditPass.getText().length() > 0;
        boolean hasNameAndPassword = goodUsername && goodPassword;

        mEditPass.setEnabled(goodUsername);
        mEditPass.setFocusable(goodUsername);
        mEditPass.setFocusableInTouchMode(goodUsername);

        mRememberPass.setEnabled(hasNameAndPassword);
        mRememberPass.setFocusable(hasNameAndPassword);

        mEditUserAccount.setEnabled(!isSignedIn);
        mEditPass.setEnabled(!isSignedIn);

        if (!isSignedIn) {
            mBtnSignIn.setEnabled(hasNameAndPassword);
            mBtnSignIn.setFocusable(hasNameAndPassword);
        }
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int before, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int after) {
            updateWidgetState();

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void deleteAccount ()
    {
      
        //need to delete 
        ((ImApp)getApplication()).deleteAccount(mAccountId, mProviderId);
    }
    
    private void showAdvanced() {

        checkUserChanged();

        Intent intent = new Intent(this, AccountSettingsActivity.class);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_settings_menu, menu);

        if (isEdit) {
            //add delete menu option
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            finish();
            return true;
            
        case R.id.menu_gen_key:
            otrGenKey();
            return true;


        }
        return super.onOptionsItemSelected(item);
    }

    ProgressDialog pbarDialog;

    private void otrGenKey() {

        pbarDialog = new ProgressDialog(this);

        pbarDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pbarDialog.setMessage(getString(R.string.otr_gen_key));
        pbarDialog.show();

        KeyGenThread kgt = new KeyGenThread();
        kgt.start();

    }

    private class KeyGenThread extends Thread {

        public KeyGenThread() {

        }

        @Override
        public void run() {

            try {
                if (mOtrChatSession != null) {
                    mOtrChatSession.generateLocalKeyPair();

                } else {
                    Toast.makeText(AccountActivity.this, "OTR is not initialized yet",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("OTR", "could not gen local key pair", e);
            } finally {
                handler.sendEmptyMessage(0);
            }

        }

        private Handler handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {

                pbarDialog.dismiss();

                try {
                    if (mOtrChatSession != null) {
                        String lFingerprint = mOtrChatSession.getLocalFingerprint();
                        mTxtFingerprint.setText(processFingerprint(lFingerprint));
                    }

                } catch (Exception e) {
                    Log.e("OTR", "could not gen local key pair", e);
                }

            }
        };
    }

    private String processFingerprint(String fingerprint) {
        StringBuffer out = new StringBuffer();

        for (int n = 0; n < fingerprint.length(); n++) {
            for (int i = n; n < i + 4; n++) {
                out.append(fingerprint.charAt(n));
            }

            out.append(' ');
        }

        return out.toString();
    }

    public void createNewAccount (String usernameNew, String passwordNew, final long newAccountId)
    {
        if (mCreateAccountTask != null && (!mCreateAccountTask.isCancelled()))
        {
            mCreateAccountTask.cancel(true);           
        }
        
        mCreateAccountTask = new AsyncTask<String, Void, String>() {
            
            private ProgressDialog dialog;
            
            
            @Override
            protected void onPreExecute() {
                dialog = new ProgressDialog(AccountActivity.this);
                dialog.setCancelable(true);
                dialog.setMessage(getString(R.string.registering_new_account_));
                dialog.show();
            }
            
            @Override
            protected String doInBackground(String... params) {
                Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                        getContentResolver(), mProviderId, false /* don't keep updated */, null /* no handler */);

                try {
                                        
                    settingsForDomain(mDomain, mPort, settings);
                    
                    XmppConnection xmppConn = new XmppConnection(AccountActivity.this);
                    xmppConn.initUser(mProviderId, newAccountId);
                    xmppConn.registerAccount(settings, params[0], params[1]);
                    // settings closed in registerAccount
                } catch (Exception e) {
                    LogCleaner.error(ImApp.LOG_TAG, "error registering new account", e);
                   
                    return e.getLocalizedMessage();
                } finally {
                    
                    settings.close();
                }
                
                return null;
              }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                
                try
                {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                        dialog = null;
                    }
                }
                catch ( java.lang.IllegalArgumentException iae)
                {
                    //dialog may not be attached to window if Activity was closed
                }
                
                if (result != null)
                {
                    Toast.makeText(AccountActivity.this, "error creating account: " + result, Toast.LENGTH_LONG).show();
                }
                
                AccountActivity.this.setResult(RESULT_OK);
                AccountActivity.this.finish();
                
               
            }
        }.execute(usernameNew, passwordNew);
        
        
    }
   

    private void setAccountKeepSignedIn(final boolean rememberPass) {
        ContentValues values = new ContentValues();
        values.put(AccountColumns.KEEP_SIGNED_IN, rememberPass ? 1 : 0);
        getContentResolver().update(mAccountUri, values, null, null);
    }
}
