/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
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

package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.otr.IOtrKeyManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.AccountSettingsActivity;
import info.guardianproject.otr.app.im.app.BrandingResources;
import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.app.ProviderDef;
import info.guardianproject.otr.app.im.app.SignInHelper;
import info.guardianproject.otr.app.im.app.SignoutActivity;
import info.guardianproject.otr.app.im.app.ThemeableActivity;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.plugin.xmpp.auth.GTalkOAuth2;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.Imps.AccountColumns;
import info.guardianproject.otr.app.im.provider.Imps.AccountStatusColumns;
import info.guardianproject.otr.app.im.provider.Imps.CommonPresenceColumns;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import info.guardianproject.util.LogCleaner;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class LLXmppAccountActivity extends ThemeableActivity {

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
    //    private static final int ACCOUNT_KEEP_SIGNED_IN_COLUMN = 4;
    //    private static final int ACCOUNT_LAST_LOGIN_STATE = 5;

    Uri mAccountUri;
    EditText mEditUserAccount;
    EditText mEditPass;
    CheckBox mRememberPass;
    CheckBox mUseTor;
    Button mBtnSignIn;
    Button mBtnDelete;
    
    Button mBtnAdvanced;
    TextView mTxtFingerprint;

    Imps.ProviderSettings.QueryMap settings;
    
    boolean isEdit = false;
    boolean isSignedIn = false;

    String mUserName = "";
    String mDomain = "";
    int mPort = 0;
    private String mOriginalUserAccount = "";

    private final static int DEFAULT_PORT = 5222;

    IOtrKeyManager otrKeyManager;
    private SignInHelper mSignInHelper;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        //getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.account_activity);
        Intent i = getIntent();
        
        getSherlock().getActionBar().setHomeButtonEnabled(true);
        getSherlock().getActionBar().setDisplayHomeAsUpEnabled(true);

        mSignInHelper = new SignInHelper(this);
        SignInHelper.Listener signInListener = new SignInHelper.Listener() {
            public void connectedToService() {
            }
            public void stateChanged(int state, long accountId) {
                if (state == ImConnection.LOGGED_IN)
                {
                    mSignInHelper.goToAccount(accountId);
                    finish();
                }
            }
        };
        mSignInHelper.setSignInListener(signInListener);
        mEditUserAccount = (EditText) findViewById(R.id.edtName);
        mEditUserAccount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                checkUserChanged();
            }
        });

        mEditPass = (EditText) findViewById(R.id.edtPass);
        mRememberPass = (CheckBox) findViewById(R.id.rememberPassword);
        mUseTor = (CheckBox) findViewById(R.id.useTor);
       

        mBtnSignIn = (Button) findViewById(R.id.btnSignIn);
        mBtnAdvanced = (Button) findViewById(R.id.btnAdvanced);
        mBtnDelete = (Button) findViewById(R.id.btnDelete);
        
        mRememberPass.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateWidgetState();
            }
        });

        
        mApp = (ImApp)getApplication();

        String action = i.getAction();

        if (i.hasExtra("isSignedIn"))
            isSignedIn = i.getBooleanExtra("isSignedIn", false);
        

        final ProviderDef provider;

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

        if (Intent.ACTION_INSERT.equals(action)) {
            mOriginalUserAccount = "";
            // TODO once we implement multiple IM protocols
            mProviderId = ContentUris.parseId(i.getData());
            provider = mApp.getProvider(mProviderId);

            if (provider != null)
            {
                setTitle(getResources().getString(R.string.add_account, provider.mFullName));
    
                settings = new Imps.ProviderSettings.QueryMap(
                        cr, mProviderId, false /* don't keep updated */, null /* no handler */);
            }
            else
            {
                finish();
            }

            
        } else if (Intent.ACTION_EDIT.equals(action)) {
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

            settings = new Imps.ProviderSettings.QueryMap(
                    cr, mProviderId, false /* don't keep updated */, null /* no handler */);

            mOriginalUserAccount = cursor.getString(ACCOUNT_USERNAME_COLUMN) + "@"
                                   + settings.getDomain();
            mEditUserAccount.setText(mOriginalUserAccount);
            mEditPass.setText(cursor.getString(ACCOUNT_PASSWORD_COLUMN));

            mRememberPass.setChecked(!cursor.isNull(ACCOUNT_PASSWORD_COLUMN));

            mUseTor.setChecked(settings.getUseTor());
            
            mBtnDelete.setVisibility(View.VISIBLE);


        } else {
            LogCleaner.warn(ImApp.LOG_TAG, "<AccountActivity> unknown intent action " + action);
            finish();
            return;
        }

        if (isSignedIn) {
            mBtnSignIn.setText(getString(R.string.menu_sign_out));
            mBtnSignIn.setBackgroundResource(R.drawable.btn_red);
        }

        final BrandingResources brandingRes = mApp.getBrandingResource(mProviderId);

        mRememberPass.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                CheckBox mRememberPass = (CheckBox) v;

                if (mRememberPass.isChecked()) {
                    String msg = brandingRes
                            .getString(BrandingResourceIDs.STRING_TOAST_CHECK_SAVE_PASSWORD);
                    Toast.makeText(LLXmppAccountActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
        });

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
                    OrbotHelper oh = new OrbotHelper(LLXmppAccountActivity.this);
                    if (!oh.isOrbotRunning())
                    {
                        oh.requestOrbotStart(LLXmppAccountActivity.this);
                        return;
                    }
                }
                

                final String pass = mEditPass.getText().toString();
                final boolean rememberPass = mRememberPass.isChecked();
                final boolean isActive = false; // TODO(miron) does this ever need to be true?
                ContentResolver cr = getContentResolver();

                if (!parseAccount(mEditUserAccount.getText().toString())) {
                    mEditUserAccount.selectAll();
                    mEditUserAccount.requestFocus();
                    return;
                }

                final long accountId = ImApp.insertOrUpdateAccount(cr, mProviderId, mUserName,
                        rememberPass ? pass : null);

                mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);

                //if remember pass is true, set the "keep signed in" property to true

                if (isSignedIn) {
                    signOut();
                    isSignedIn = false;
                } else {
                    ContentValues values = new ContentValues();
                    values.put(AccountColumns.KEEP_SIGNED_IN, rememberPass ? 1 : 0);
                    getContentResolver().update(mAccountUri, values, null, null);

                    if (!mOriginalUserAccount.equals(mUserName + '@' + mDomain)
                        && shouldShowTermOfUse(brandingRes)) {
                        confirmTermsOfUse(brandingRes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSignInHelper.signIn(pass, mProviderId, accountId, isActive);
                            }
                        });
                    } else {
                        mSignInHelper.signIn(pass, mProviderId, accountId, isActive);
                    }
                    isSignedIn = true;
                }
                updateWidgetState();
                
            }
        });
        
        mUseTor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateUseTor(isChecked);
            }
        });

        updateWidgetState();
        

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
            mRememberPass.setChecked(true);
        }


    }
    
    @Override
    protected void onDestroy() {
        mSignInHelper.stop();
        settings.close();
        super.onDestroy();
    }
    
    private void updateUseTor(boolean useTor) {
        checkUserChanged();
    
        final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                getContentResolver(), mProviderId, false /* don't keep updated */, null /* no handler */);
    
        OrbotHelper orbotHelper = new OrbotHelper(this);
        
        if (useTor && (!orbotHelper.isOrbotInstalled()))
        {
            //Toast.makeText(this, "Orbot app is not installed. Please install from Google Play or from https://guardianproject.info/releases", Toast.LENGTH_LONG).show();
            
            orbotHelper.promptToInstall(this);
            
            mUseTor.setChecked(false);
            settings.setUseTor(false);
            settings.setServer("");
        }
        else
        {
        
            // if using Tor, disable DNS SRV to reduce anonymity leaks
            settings.setDoDnsSrv(!useTor);
        
            String server = settings.getServer();
        
            if (useTor && (server == null || server.length() == 0)) {
                server = settings.getDomain();
                String domain = settings.getDomain().toLowerCase();
        
                // a little bit of custom handling here
                if (domain.equals("gmail.com")) {
                    server = "talk.l.google.com";
                } else if (domain.equals("jabber.ccc.de")) {
                    server = "okj7xc6j2szr2y75.onion";
                } else if (domain.equals("jabber.org")) {
                    server = "hermes.jabber.org";
                } else if (domain.equals("chat.facebook.com")) {
                    server = "chat.facebook.com";
                } else if (domain.equals("dukgo.com")) {
                    server = "dukgo.com";
                    //settings.setTlsCertVerify(false); //remove this - MemorizingTrustManager will now prompt
                }
                else
                {
                    Toast.makeText(this, getString(R.string.warning_tor_connect), Toast.LENGTH_LONG).show();
                }
        
            }
            else
            {
                
            }
            
            settings.setServer(server);
            settings.setUseTor(useTor);
        }
        
        settings.close();
      
    }

    private void getOTRKeyInfo() {

        if (mApp != null && mApp.getRemoteImService() != null) {
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

    }

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
        mDomain = null;
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
                            LLXmppAccountActivity.this,
                            "The port value '" + splitColon[1]
                                    + "' after the : could not be parsed as a number!",
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        if (mDomain == null) {
            isGood = false;
            //Toast.makeText(AccountActivity.this, 
            //	R.string.account_wizard_no_domain_warning,
            //	Toast.LENGTH_LONG).show();
        } 
        /*//removing requirement of a . in the domain
        else if (mDomain.indexOf(".") == -1) { 
            isGood = false;
            //	Toast.makeText(AccountActivity.this, 
            //		R.string.account_wizard_no_root_domain_warning,
            //	Toast.LENGTH_LONG).show();
        }*/

        return isGood;
    }

    void settingsForDomain(String domain, int port) {


        if (domain.equals("gmail.com")) {
            // Google only supports a certain configuration for XMPP:
            // http://code.google.com/apis/talk/open_communications.html
            settings.setDoDnsSrv(true);
            settings.setServer("");
            settings.setDomain(domain);
            settings.setPort(DEFAULT_PORT);
            settings.setRequireTls(true);
            settings.setTlsCertVerify(true);
            settings.setAllowPlainAuth(false);
        } 
        else if (mEditPass.getText().toString().startsWith(GTalkOAuth2.NAME))
        {
            //this is not @gmail but IS a google account
            settings.setDoDnsSrv(false);
            settings.setServer("talk.google.com"); //set the google connect server
            settings.setDomain(domain);
            settings.setPort(DEFAULT_PORT);
            settings.setRequireTls(true);
            settings.setTlsCertVerify(true);
            settings.setAllowPlainAuth(false);
        }
        else if (domain.equals("jabber.org")) {
            settings.setDoDnsSrv(true);
            settings.setDomain(domain);
            settings.setPort(DEFAULT_PORT);
            settings.setServer("");
            settings.setRequireTls(true);
            settings.setTlsCertVerify(true);
            settings.setAllowPlainAuth(false);
        } else if (domain.equals("facebook.com")) {
            settings.setDoDnsSrv(false);
            settings.setDomain("chat.facebook.com");
            settings.setPort(DEFAULT_PORT);
            settings.setServer("chat.facebook.com");
            settings.setRequireTls(true); //facebook TLS now seems to be on
            settings.setTlsCertVerify(false); //but cert verify can still be funky - off by default
            settings.setAllowPlainAuth(false);
        } else {
            settings.setDoDnsSrv(true);
            settings.setDomain(domain);
            settings.setPort(port);
            settings.setServer("");
            settings.setRequireTls(true);
            settings.setTlsCertVerify(true);
            settings.setAllowPlainAuth(false);
        }
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

        Intent intent = new Intent(LLXmppAccountActivity.this, SignoutActivity.class);
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

            Toast.makeText(this,
                    getString(R.string.signed_out_prompt, this.mEditUserAccount.getText()),
                    Toast.LENGTH_SHORT).show();
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

        // enable keep sign in only when remember password is checked.
        boolean rememberPass = mRememberPass.isChecked();
        if (rememberPass && !hasNameAndPassword) {
            mRememberPass.setChecked(false);
            rememberPass = false; 
    
        }
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
        Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, mAccountId);
        getContentResolver().delete(accountUri, null, null);
        Uri providerUri = ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, mProviderId);
        getContentResolver().delete(providerUri, null, null);
      
    }
    
    private void showAdvanced() {

        checkUserChanged();

        Intent intent = new Intent(this, AccountSettingsActivity.class);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
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
                if (otrKeyManager != null) {
                    otrKeyManager.generateLocalKeyPair();

                } else {
                    Toast.makeText(LLXmppAccountActivity.this, "OTR is not initialized yet",
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
                    if (otrKeyManager != null) {
                        String lFingerprint = otrKeyManager.getLocalFingerprint();
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

    
   

}
