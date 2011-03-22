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

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;

public class SettingActivity extends android.preference.PreferenceActivity {

    private long mProviderId;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
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

        CheckBoxPreference pref = (CheckBoxPreference) findPreference(getString(R.string.pref_hide_offline_contacts));
        pref.setChecked(settings.getHideOfflineContacts());

        pref = (CheckBoxPreference) findPreference(getString(R.string.pref_enable_notifications));
        pref.setChecked(settings.getEnableNotification());

        pref = (CheckBoxPreference) findPreference(getString(R.string.pref_notification_vibrate));
        pref.setChecked(settings.getVibrate());

        pref = (CheckBoxPreference) findPreference(getString(R.string.pref_notification_sound));
        pref.setChecked(settings.getRingtoneURI() != null);
        settings.close();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof CheckBoxPreference) {
            final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                    getContentResolver(), mProviderId,
                    false /* keep updated */, null /* no handler */);
            String key = preference.getKey();
            boolean value = ((CheckBoxPreference) preference).isChecked();

            if (key.equals(getString(R.string.pref_hide_offline_contacts))) {
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
        }
        
        return false;
    }
}
