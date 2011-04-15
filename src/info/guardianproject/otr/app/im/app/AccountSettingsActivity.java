/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

public class AccountSettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private long mProviderId;

    EditTextPreference mXmppResource;
    EditTextPreference mPort;
    EditTextPreference mServer;
    ListPreference mOtrMode;
    CheckBoxPreference mAllowPlainAuth;
    CheckBoxPreference mRequireTls;
    CheckBoxPreference mTlsCertVerify;
    CheckBoxPreference mDoDnsSrv;
    
    private void setInitialValues() {
		ContentResolver cr = getContentResolver();
        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                cr, mProviderId,
                false /* keep updated */, null /* no handler */);        
        String text;

        text = settings.getXmppResource();
        if (text != null) {
        	mXmppResource.setSummary(text);
        	mXmppResource.setText(text);
        }
        text = Integer.toString(settings.getPort());
        if (text != null && text != "5222") {
            mPort.setSummary(text);
            mPort.setText(text);
        }
        text = settings.getServer();
        if (text != null) {
        	mServer.setSummary(text);
        	mServer.setText(text);
        }
        mOtrMode.setValue(settings.getOtrMode());
        mAllowPlainAuth.setChecked(settings.getAllowPlainAuth());
        mRequireTls.setChecked(settings.getRequireTls());
        mTlsCertVerify.setChecked(settings.getTlsCertVerify());
        mDoDnsSrv.setChecked(settings.getDoDnsSrv());
        
        settings.close();
    }

    /* save the preferences in Imps so they are accessible everywhere */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    	final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
    			getContentResolver(), mProviderId,
    			false /* don't keep updated */, null /* no handler */);
    	String value;

    	if (key.equals(getString(R.string.pref_account_xmpp_resource))) {
    		value = prefs.getString(key, null);
    		if (value != null) {
        		settings.setXmppResource(value);
    			mXmppResource.setSummary(value);
    		}
    	} else if (key.equals(getString(R.string.pref_account_port))) {
    		value = prefs.getString(key, "5222");
    		try {
    			settings.setPort(Integer.parseInt(value));
    		} catch (NumberFormatException nfe) {
    			// TODO port numbers with non-int content should be handled better
    			Toast.makeText(getBaseContext(), "Port number must be a number",
    					Toast.LENGTH_SHORT).show();     
    		}
    		if (value != "5222") mPort.setSummary(value);
    	} else if (key.equals(getString(R.string.pref_account_server))) {
    		value = prefs.getString(key, null);
    		if (value != null) {
        		settings.setServer(value);
    			mServer.setSummary(value);
    		}
    	} else if (key.equals(getString(R.string.pref_security_otr_mode))) {
    		settings.setOtrMode(prefs.getString(key, "auto"));
    	} else if (key.equals(getString(R.string.pref_security_allow_plain_auth))) {
    		settings.setAllowPlainAuth(prefs.getBoolean(key, false));
    	} else if (key.equals(getString(R.string.pref_security_require_tls))) {
    		settings.setRequireTls(prefs.getBoolean(key, true));
    	} else if (key.equals(getString(R.string.pref_security_tls_cert_verify))) {
    		settings.setTlsCertVerify(prefs.getBoolean(key, true));
    	} else if (key.equals(getString(R.string.pref_security_do_dns_srv))) {
    		settings.setDoDnsSrv(prefs.getBoolean(key, true));
    	}
    	settings.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	addPreferencesFromResource(R.xml.account_settings);
    	Intent intent = getIntent();
    	mProviderId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, -1);
    	if (mProviderId < 0) {
    		Log.e(ImApp.LOG_TAG,"AccountSettingsActivity intent requires provider id extra");
    		throw new RuntimeException("AccountSettingsActivity must be created with an provider id");
    	}
    	mXmppResource = (EditTextPreference) findPreference(getString(R.string.pref_account_xmpp_resource));
    	mPort = (EditTextPreference) findPreference(getString(R.string.pref_account_port));
    	mServer = (EditTextPreference) findPreference(getString(R.string.pref_account_server));
    	mOtrMode = (ListPreference) findPreference(getString(R.string.pref_security_otr_mode));
    	mAllowPlainAuth = (CheckBoxPreference) findPreference(getString(R.string.pref_security_allow_plain_auth));
    	mRequireTls = (CheckBoxPreference) findPreference(getString(R.string.pref_security_require_tls));
    	mTlsCertVerify = (CheckBoxPreference) findPreference(getString(R.string.pref_security_tls_cert_verify));
    	mDoDnsSrv = (CheckBoxPreference) findPreference(getString(R.string.pref_security_do_dns_srv));

    	setInitialValues();
    }

    @Override
    protected void onResume() {
    	super.onResume();

    	setInitialValues();
    	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
    	super.onPause();

    	getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
    }

}
