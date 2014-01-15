/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
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

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.Imps.ProviderSettings;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class SettingActivity extends SherlockPreferenceActivity implements
        OnSharedPreferenceChangeListener {
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 1;
    ListPreference mOtrMode;
    CheckBoxPreference mHideOfflineContacts;
    CheckBoxPreference mEnableNotification;
    CheckBoxPreference mNotificationVibrate;
    CheckBoxPreference mNotificationSound;
    CheckBoxPreference mForegroundService;
    EditTextPreference mHeartbeatInterval;
    
    EditTextPreference mThemeBackground;

    private void setInitialValues() {
        ContentResolver cr = getContentResolver();
        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(cr,
                false /* keep updated */, null /* no handler */);
        mOtrMode.setValue(settings.getOtrMode());
        mHideOfflineContacts.setChecked(settings.getHideOfflineContacts());
        mEnableNotification.setChecked(settings.getEnableNotification());
        mNotificationVibrate.setChecked(settings.getVibrate());
        mNotificationSound.setChecked(settings.getRingtoneURI() != null);
        
        mForegroundService.setChecked(settings.getUseForegroundPriority());
        
        long heartbeatInterval = settings.getHeartbeatInterval();
        if (heartbeatInterval == 0) heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
        mHeartbeatInterval.setText(String.valueOf(heartbeatInterval));

        settings.close();
    }

    /* save the preferences in Imps so they are accessible everywhere */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                getContentResolver(), false /* don't keep updated */, null /* no handler */);

        if (key.equals("pref_security_otr_mode")) {
            settings.setOtrMode(prefs.getString(key, "auto"));
        } else if (key.equals("pref_hide_offline_contacts")) {
            settings.setHideOfflineContacts(prefs.getBoolean(key, false));
        } else if (key.equals("pref_enable_notification")) {
            settings.setEnableNotification(prefs.getBoolean(key, true));
        } else if (key.equals("pref_notification_vibrate")) {
            settings.setVibrate(prefs.getBoolean(key, true));
        } else if (key.equals("pref_notification_sound")) {
            // TODO sort out notification sound pref
            if (prefs.getBoolean(key, true)) {
                settings.setRingtoneURI("android.resource://" + getPackageName() + "/" + R.raw.notify);
            } else {
                settings.setRingtoneURI(null);
            }
        } else if (key.equals("pref_enable_custom_notification")) {
            if (prefs.getBoolean(key, false)) {
                settings.setRingtoneURI("android.resource://" + getPackageName() + "/" + R.raw.notify);
            } else {
                settings.setRingtoneURI(ProviderSettings.RINGTONE_DEFAULT);
            }
        }
        else if (key.equals("pref_foreground_enable")) {
            settings.setUseForegroundPriority(prefs.getBoolean(key, false));
        } else if (key.equals("pref_heartbeat_interval")) {
            try
            {
                settings.setHeartbeatInterval(Integer.valueOf(prefs.getString(key, String.valueOf(DEFAULT_HEARTBEAT_INTERVAL))));
            }
            catch (NumberFormatException nfe)
            {
                settings.setHeartbeatInterval((DEFAULT_HEARTBEAT_INTERVAL));
            }
        }
        else if (key.equals("pref_default_locale"))
        {
           ((ImApp)getApplication()).setNewLocale(this, prefs.getString(key, ""));
           setResult(RESULT_OK);
           
        }
        else if (key.equals("themeDark"))
        {
         
            setResult(RESULT_OK);
        }
        
        settings.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mHideOfflineContacts = (CheckBoxPreference) findPreference("pref_hide_offline_contacts");
        mOtrMode = (ListPreference) findPreference("pref_security_otr_mode");
        mEnableNotification = (CheckBoxPreference) findPreference("pref_enable_notification");
        mNotificationVibrate = (CheckBoxPreference) findPreference("pref_notification_vibrate");
        mNotificationSound = (CheckBoxPreference) findPreference("pref_notification_sound");
        // TODO re-enable Ringtone preference
        //mNotificationRingtone = (CheckBoxPreference) findPreference("pref_notification_ringtone");
        mForegroundService = (CheckBoxPreference) findPreference("pref_foreground_enable");
        mHeartbeatInterval = (EditTextPreference) findPreference("pref_heartbeat_interval");
        
        mThemeBackground = (EditTextPreference) findPreference("pref_background");
        
        mThemeBackground.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
              
                showThemeChooserDialog ();
                return true;
            }
            
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 888 && data != null && data.getData() != null){
            Uri _uri = data.getData();

            if (_uri != null) {
                //User had pick an image.
                Cursor cursor = getContentResolver().query(_uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
              
                if (cursor != null)
                {
                    cursor.moveToFirst();
    
                    //Link to the image
                    final String imageFilePath = cursor.getString(0);
                    mThemeBackground.setText(imageFilePath);                
                    mThemeBackground.getDialog().cancel();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
        
    }

    private void showThemeChooserDialog ()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Choose Background");
        builder.setMessage("Do you want to select a background image from the Gallery?");

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 888);

                dialog.dismiss();
            }

        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // I do not need any action here you might
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
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

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
    }

}
