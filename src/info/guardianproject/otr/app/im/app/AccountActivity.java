/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;

import info.guardianproject.otr.app.im.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AccountActivity extends Activity {
    public static final String TAG = "AccountActivity";
    private static final String ACCOUNT_URI_KEY = "accountUri";

    static final int REQUEST_SIGN_IN = RESULT_FIRST_USER + 1;

    private static final String[] ACCOUNT_PROJECTION = {
        Imps.Account._ID,
        Imps.Account.PROVIDER,
        Imps.Account.USERNAME,
        Imps.Account.PASSWORD,
        Imps.Account.KEEP_SIGNED_IN,
    };

    private static final int ACCOUNT_PROVIDER_COLUMN = 1;
    private static final int ACCOUNT_USERNAME_COLUMN = 2;
    private static final int ACCOUNT_PASSWORD_COLUMN = 3;
    private static final int ACCOUNT_KEEP_SIGNED_IN_COLUMN = 4;

    Uri mAccountUri;

    EditText mEditName;
    EditText mEditPass;
    CheckBox mRememberPass;
    CheckBox mKeepSignIn;
    CheckBox mUseTor;
    Button   mBtnSignIn;

    String mToAddress;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.account_activity);
        mEditName = (EditText)findViewById(R.id.edtName);
        mEditPass = (EditText)findViewById(R.id.edtPass);
        mRememberPass = (CheckBox)findViewById(R.id.rememberPassword);
        mKeepSignIn = (CheckBox)findViewById(R.id.keepSignIn);
        mUseTor = (CheckBox)findViewById(R.id.useTor);
        mBtnSignIn = (Button)findViewById(R.id.btnSignIn);
        mRememberPass.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                updateWidgetState();
            }
        });

        ImApp app = ImApp.getApplication(this);
        Intent i = getIntent();
        String action = i.getAction();
        mToAddress = i.getStringExtra(ImApp.EXTRA_INTENT_SEND_TO_USER);
        final String origUserName;
        final long providerId;
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

        if(Intent.ACTION_INSERT.equals(action)) {
            origUserName = "";
            // TODO once we implement multiple IM protocols
            providerId = ContentUris.parseId(i.getData());
            provider = app.getProvider(providerId);
            setTitle(getResources().getString(R.string.add_account, provider.mFullName));
        } else if(Intent.ACTION_EDIT.equals(action)) {
            if ((uri == null) || !Imps.Account.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {
                Log.w(ImApp.LOG_TAG, "<AccountActivity>Bad data");
                return;
            }

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

            providerId = cursor.getLong(ACCOUNT_PROVIDER_COLUMN);
            provider = app.getProvider(providerId);

            origUserName = cursor.getString(ACCOUNT_USERNAME_COLUMN);
            mEditName.setText(origUserName);
            mEditPass.setText(cursor.getString(ACCOUNT_PASSWORD_COLUMN));

            mRememberPass.setChecked(!cursor.isNull(ACCOUNT_PASSWORD_COLUMN));

            boolean keepSignIn = cursor.getInt(ACCOUNT_KEEP_SIGNED_IN_COLUMN) == 1;
            mKeepSignIn.setChecked(keepSignIn);

            cursor.close();
        } else {
            Log.w(ImApp.LOG_TAG, "<AccountActivity> unknown intent action " + action);
            finish();
            return;
        }

        final BrandingResources brandingRes = app.getBrandingResource(providerId);
        mKeepSignIn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                CheckBox keepSignIn = (CheckBox) v;
                if ( keepSignIn.isChecked() ) {
                    String msg = brandingRes.getString(BrandingResourceIDs.STRING_TOAST_CHECK_AUTO_SIGN_IN);
                    Toast.makeText(AccountActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
        });
        mRememberPass.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                CheckBox keepSignIn = (CheckBox) v;
                if ( keepSignIn.isChecked() ) {
                    String msg = brandingRes.getString(BrandingResourceIDs.STRING_TOAST_CHECK_SAVE_PASSWORD);
                    Toast.makeText(AccountActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
        });

        mEditName.addTextChangedListener(mTextWatcher);
        mEditPass.addTextChangedListener(mTextWatcher);

        mBtnSignIn.setOnClickListener(new OnClickListener() {
        	String username;
        	String domain;
        	int port;
            public void onClick(View v) {
                final String pass = mEditPass.getText().toString();
                final boolean rememberPass = mRememberPass.isChecked();

                ContentResolver cr = getContentResolver();

                if (! parseAccount(mEditName.getText().toString())) {
                	mEditName.selectAll();
                	mEditName.requestFocus();
                	return;
                }
                
                long accountId = ImApp.insertOrUpdateAccount(cr, providerId, username,
                        rememberPass ? pass : null);
                
                mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);

                if (!origUserName.equals(username) && shouldShowTermOfUse(brandingRes)) {
                    comfirmTermsOfUse(brandingRes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            signIn(rememberPass, pass);
                        }
                    });
                } else {
                    signIn(rememberPass, pass);
                }
            }

            private boolean parseAccount(String userField) {
            	boolean isGood = true;
            	String[] splitAt = userField.split("@");
            	username = splitAt[0];
            	domain = null;
            	port = 5222;

            	if (splitAt.length > 1) {
            		domain = splitAt[1].toLowerCase();
            		String[] splitColon = domain.split(":");
            		domain = splitColon[0];
            		if(splitColon.length > 1) {
            			try {
            				port = Integer.parseInt(splitColon[1]);
            			} catch (NumberFormatException e) {
            				// TODO move these strings to strings.xml
            				isGood = false;
            				Toast.makeText(AccountActivity.this, 
            						"The port value '" + splitColon[1] +
            						"' after the : could not be parsed as a number!",
            						Toast.LENGTH_LONG).show();
            			}
            		}
            	}

            	// TODO move these strings to strings.xml
            	if (domain == null) {
            		isGood = false;
            		Toast.makeText(AccountActivity.this, 
            				"You didn't enter an @hostname.com part for your account ID. Try again!",
            				Toast.LENGTH_LONG).show();
            	} else if (domain.indexOf(".") == -1) {
            		isGood = false;
            		Toast.makeText(AccountActivity.this, 
            				"Your server hostname didn't have a .com, .net or similar appendix. Try again!",
            				Toast.LENGTH_LONG).show();
            	}

            	return isGood;
            }

            void signIn(boolean rememberPass, String pass) {
                final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                        getContentResolver(),
                        providerId,
                        false /* don't keep updated */,
                        null /* no handler */);
                
        		settingsForDomain(settings, domain, port);
                
                Intent intent = new Intent(AccountActivity.this, SigningInActivity.class);
                intent.setData(mAccountUri);
                if (!rememberPass) {
                    intent.putExtra(ImApp.EXTRA_INTENT_PASSWORD, pass);
                }

                if (mUseTor.isChecked()) {	
                	settings.setUseTor(true);
                	// TODO move proxy settings to a central location
                	intent.putExtra(ImApp.EXTRA_INTENT_PROXY_TYPE,"SOCKS5");
                	intent.putExtra(ImApp.EXTRA_INTENT_PROXY_HOST,"127.0.0.1");
                	intent.putExtra(ImApp.EXTRA_INTENT_PROXY_PORT,9050);
                } else
                	settings.setUseTor(false);

                if (mToAddress != null) {
                    intent.putExtra(ImApp.EXTRA_INTENT_SEND_TO_USER, mToAddress);
                }
                
        		settings.close();
                startActivityForResult(intent, REQUEST_SIGN_IN);
            }
        });

        // Make link for signing up.
        String publicXmppServices = "http://xmpp.org/services/";
        	
        String text = brandingRes.getString(BrandingResourceIDs.STRING_LABEL_SIGN_UP);
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        builder.setSpan(new URLSpan(publicXmppServices), 0, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView signUp = (TextView)findViewById(R.id.signUp);
        signUp.setText(builder);
        signUp.setMovementMethod(LinkMovementMethod.getInstance());

        updateWidgetState();
        
        // TODO move this warning to strings.xml
        String msg = "WARNING: This is an ALPHA release of Gibberbot that may still include security holes or bugs. Experimental use only!";
        Toast.makeText(AccountActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    void settingsForDomain(Imps.ProviderSettings.QueryMap settings, String domain, int port) {
    	if (domain.equals("gmail.com")) {
			// Google only supports a certain configuration for XMPP:
			// http://code.google.com/apis/talk/open_communications.html
    		// TODO we should probably use DNS SRV for gmail.com so we can validate the cert
    		// then perhaps we could enable RequireTls
    		settings.setDoDnsSrv(false);
    		settings.setDomain(domain);
    		settings.setPort(5222);
    		settings.setServer("talk.google.com");
    		settings.setRequireTls(false);
    		settings.setTlsCertVerify(false);
    	} else if (domain.equals("jabber.org")) {
    		settings.setDoDnsSrv(false);
    		settings.setDomain(domain);
    		settings.setPort(5222);
    		settings.setServer(null);
    		settings.setRequireTls(true);
    		settings.setTlsCertVerify(true);
    	} else if (domain.equals("chat.facebook.com")) {
    		settings.setDoDnsSrv(false);
    		settings.setDomain(domain);
    		settings.setPort(5222);
    		settings.setServer(domain);
    		settings.setRequireTls(false);
    		settings.setTlsCertVerify(false);
    	} else {
    		settings.setDoDnsSrv(true);
    		settings.setDomain(domain);
    		settings.setPort(port);
    		settings.setServer(null);
    		settings.setRequireTls(false);
    		settings.setTlsCertVerify(true);
    	}
    }

    void comfirmTermsOfUse(BrandingResources res, DialogInterface.OnClickListener accept) {
        SpannableString message = new SpannableString(
                res.getString(BrandingResourceIDs.STRING_TOU_MESSAGE));
        Linkify.addLinks(message, Linkify.ALL);

        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(res.getString(BrandingResourceIDs.STRING_TOU_TITLE))
            .setMessage(message)
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                boolean keepSignIn = mKeepSignIn.isChecked();
                updateKeepSignedIn(keepSignIn);
                finish();
            } else {
                // sign in failed, disable keep sign in, clear the password.
                mKeepSignIn.setChecked(false);
                updateKeepSignedIn(false);
                mEditPass.setText("");
                ContentValues values = new ContentValues();
                values.put(Imps.Account.PASSWORD, (String) null);
                getContentResolver().update(mAccountUri, values, null, null);
            }
        }
    }

    void updateKeepSignedIn(boolean keepSignIn) {
        ContentValues values = new ContentValues();
        values.put(Imps.Account.KEEP_SIGNED_IN, keepSignIn ? 1 : 0);
        getContentResolver().update(mAccountUri, values, null, null);
    }

    void updateWidgetState() {
        boolean goodUsername = mEditName.getText().length() > 0;
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

        if (!rememberPass) {
            mKeepSignIn.setChecked(false);
        }
        mKeepSignIn.setEnabled(rememberPass);
        mKeepSignIn.setFocusable(rememberPass);

        mBtnSignIn.setEnabled(hasNameAndPassword);
        mBtnSignIn.setFocusable(hasNameAndPassword);
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int before, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int after) {
            updateWidgetState();
        }

        public void afterTextChanged(Editable s) {
        }
    };

}
