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

import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;

import info.guardianproject.otr.app.im.R;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

public class SettingActivity extends android.preference.PreferenceActivity {

    private long mProviderId;
    private long mAccountId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        Intent intent = getIntent();
        mProviderId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, -1);
        if (mProviderId < 0) {
            Log.e(ImApp.LOG_TAG,"SettingActivity intent requires provider id extra");
            throw new RuntimeException("SettingActivity must be created with an provider id");
        }
        setInitialValues();
    }

    private void setInitialValues() {
        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                getContentResolver(), mProviderId,
                false /* keep updated */, null /* no handler */);
        //android.os.Debug.waitForDebugger();
        
        // TODO set all preferences here so that the values from the Imps are in the fields, this is needed to support multiple accounts
        ((EditTextPreference) findPreference(getString(R.string.pref_account_domain))).setText(settings.getDomain());
        ((EditTextPreference) findPreference(getString(R.string.pref_account_xmpp_resource))).setText(settings.getXmppResource());
        ((EditTextPreference) findPreference(getString(R.string.pref_account_port))).setText(Integer.toString(settings.getPort()));
        ((EditTextPreference) findPreference(getString(R.string.pref_account_server))).setText(settings.getServer());

        CheckBoxPreference checkbox; 
        checkbox = (CheckBoxPreference) findPreference(getString(R.string.pref_security_allow_plain_auth));
        checkbox.setChecked(settings.getAllowPlainAuth());
        checkbox = (CheckBoxPreference) findPreference(getString(R.string.pref_security_require_tls));
        checkbox.setChecked(settings.getRequireTls());
        checkbox = (CheckBoxPreference) findPreference(getString(R.string.pref_security_tls_cert_verify));
        checkbox.setChecked(settings.getTlsCertVerify());
        checkbox = (CheckBoxPreference) findPreference(getString(R.string.pref_security_do_dns_srv));
        checkbox.setChecked(settings.getDoDnsSrv());
        checkbox = (CheckBoxPreference) findPreference(getString(R.string.pref_hide_offline_contacts));
        checkbox.setChecked(settings.getHideOfflineContacts());
        checkbox = (CheckBoxPreference) findPreference(getString(R.string.pref_enable_notifications));
        checkbox.setChecked(settings.getEnableNotification());
        checkbox = (CheckBoxPreference) findPreference(getString(R.string.pref_notification_vibrate));
        checkbox.setChecked(settings.getVibrate());
        checkbox = (CheckBoxPreference) findPreference(getString(R.string.pref_notification_sound));
        checkbox.setChecked(settings.getRingtoneURI() != null);
        
        settings.close();
    }

    /* save the preferences in Imps so they are accessible everywhere
     * 
     * (non-Javadoc)
     * @see android.preference.PreferenceActivity#onPreferenceTreeClick(android.preference.PreferenceScreen, android.preference.Preference)
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                getContentResolver(), mProviderId,
                false /* don't keep updated */, null /* no handler */);
        String key = preference.getKey();
        if (preference instanceof CheckBoxPreference) {
            boolean value = ((CheckBoxPreference) preference).isChecked();
            if (key.equals(getString(R.string.pref_security_allow_plain_auth))) {
                settings.setAllowPlainAuth(value);
            } else if (key.equals(getString(R.string.pref_security_require_tls))) {
                settings.setRequireTls(value);
            } else if (key.equals(getString(R.string.pref_security_tls_cert_verify))) {
                settings.setTlsCertVerify(value);
            } else if (key.equals(getString(R.string.pref_security_do_dns_srv))) {
                settings.setDoDnsSrv(value);
            } else if (key.equals(getString(R.string.pref_hide_offline_contacts))) {
                settings.setHideOfflineContacts(value);
            } else if (key.equals(getString(R.string.pref_enable_notifications))) {
                settings.setEnableNotification(value);
            } else if (key.equals(getString(R.string.pref_notification_vibrate))) {
                settings.setVibrate(value);
            } else if (key.equals(getString(R.string.pref_notification_sound))){
                if (!value) {
                    settings.setRingtoneURI(null);
                }
            }
            settings.close();
            return true;
        } else if (preference instanceof EditTextPreference) {
        	String value = ((EditTextPreference) preference).getText();
            if (key.equals(getString(R.string.pref_account_user))) {
                ContentResolver cr = getContentResolver();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String password = prefs.getString(getString(R.string.pref_account_pass), null);
                mAccountId = ImApp.insertOrUpdateAccount(cr, mProviderId, value, password);
            } else if (key.equals(getString(R.string.pref_account_pass))) {
                ContentResolver cr = getContentResolver();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String userName = prefs.getString(getString(R.string.pref_account_user), null);
                mAccountId = ImApp.insertOrUpdateAccount(cr, mProviderId, userName, value);
            } else if (key.equals(getString(R.string.pref_account_domain))) {
            	settings.setDomain(value);
            } else if (key.equals(getString(R.string.pref_account_xmpp_resource))) {
            	settings.setXmppResource(value);
            } else if (key.equals(getString(R.string.pref_account_port))) {
            	try {
            		settings.setPort(Integer.parseInt(value));
            	} catch (NumberFormatException nfe) {
            		// TODO port numbers with non-int content should be handled better
            		Toast.makeText(getBaseContext(), "Port number must be a number",
            				Toast.LENGTH_SHORT).show();     
            	}
            } else if (key.equals(getString(R.string.pref_account_server))) {
                settings.setServer(value);
            } else if (key.equals(getString(R.string.pref_security_otr_mode))) {
            	settings.setOtrMode(value);
            }
            settings.close();
            return true;
        }
        
        return false;
    }
}
