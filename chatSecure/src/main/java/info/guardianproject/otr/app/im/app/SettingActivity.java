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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.util.Languages;

public class SettingActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
    private static final String TAG = "SettingActivity";
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 1;
    private String currentLanguage;
    ListPreference mOtrMode;
    ListPreference mLanguage;
    CheckBoxPreference mLinkifyOnTor;
    CheckBoxPreference mHideOfflineContacts;
    CheckBoxPreference mDeleteUnsecuredMedia;
    CheckBoxPreference mStoreMediaOnExternalStorage;
    CheckBoxPreference mEnableNotification;
    CheckBoxPreference mNotificationVibrate;
    CheckBoxPreference mNotificationSound;
    CheckBoxPreference mForegroundService;
    EditTextPreference mHeartbeatInterval;

    EditTextPreference mThemeBackground;
    Preference mNotificationRingtone;

    private void setInitialValues() {
        ContentResolver cr = getContentResolver();
        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString( Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS)},null);

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(pCursor, cr,
                Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS,     false /* keep updated */, null /* no handler */);
        mOtrMode.setValue(settings.getOtrMode());
        mLinkifyOnTor.setChecked(settings.getLinkifyOnTor());
        mHideOfflineContacts.setChecked(settings.getHideOfflineContacts());
        mDeleteUnsecuredMedia.setChecked(settings.getDeleteUnsecuredMedia());
        mEnableNotification.setChecked(settings.getEnableNotification());
        mNotificationVibrate.setChecked(settings.getVibrate());
        mNotificationSound.setChecked(settings.getRingtoneURI() != null);

        mForegroundService.setChecked(settings.getUseForegroundPriority());

        long heartbeatInterval = settings.getHeartbeatInterval();
        if (heartbeatInterval == 0) heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
        mHeartbeatInterval.setText(String.valueOf(heartbeatInterval));

        settings.close();

        /* This uses SharedPreferences since it is used before Imps is setup */
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        mStoreMediaOnExternalStorage.setChecked(sharedPrefs.getBoolean(
                getString(R.string.key_store_media_on_external_storage_pref), false));
    }

    /*
     * Warning: must call settings.close() after usage!
     */
    private static Imps.ProviderSettings.QueryMap getSettings(Context context) {
        ContentResolver cr = context.getContentResolver();
        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI,
                new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},
                Imps.ProviderSettings.PROVIDER + "=?",
                new String[] { Long.toString( Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS)},
                null);

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(pCursor,
                cr,
                Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS,
                false /* keep updated */,
                null /* no handler */);
        return settings;
    }

    public static boolean getDeleteUnsecuredMedia(Context context) {
        Imps.ProviderSettings.QueryMap settings = getSettings(context);
        boolean value = settings.getDeleteUnsecuredMedia();
        settings.close();
        return value;
    }


    /* save the preferences in Imps so they are accessible everywhere */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        ContentResolver cr = getContentResolver();
        Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString( Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS)},null);

        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(pCursor, cr,
                Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS,     false /* keep updated */, null /* no handler */);

        if (key.equals("pref_security_otr_mode")) {
            settings.setOtrMode(prefs.getString(key, "auto"));
        } else if (key.equals("pref_linkify_on_tor")) {
            settings.setLinkifyOnTor(prefs.getBoolean(key, false));
        } else if (key.equals("pref_hide_offline_contacts")) {
            settings.setHideOfflineContacts(prefs.getBoolean(key, false));
        } else if (key.equals("pref_delete_unsecured_media")) {
            boolean test = prefs.getBoolean(key, false);
            settings.setDeleteUnsecuredMedia(prefs.getBoolean(key, false));
        } else if (key.equals("pref_store_media_on_external_storage")) {
            /* This uses SharedPreferences since it is used before Imps is setup */
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            Editor editor = sharedPrefs.edit();
            editor.putBoolean(getString(R.string.key_store_media_on_external_storage_pref),
                    prefs.getBoolean(key, false));
            editor.apply();
        } else if (key.equals("pref_enable_notification")) {
            settings.setEnableNotification(prefs.getBoolean(key, true));
        } else if (key.equals("pref_notification_vibrate")) {
            settings.setVibrate(prefs.getBoolean(key, true));
        } else if (key.equals("pref_notification_sound")) {
            /**
            // TODO sort out notification sound pref
            if (prefs.getBoolean(key, true)) {
                settings.setRingtoneURI("android.resource://" + getPackageName() + "/" + R.raw.notify);
            } else {
                settings.setRingtoneURI(null);
            }*/
        } else if (key.equals("pref_enable_custom_notification")) {
            /*
            if (prefs.getBoolean(key, false)) {
                settings.setRingtoneURI("android.resource://" + getPackageName() + "/" + R.raw.notify);
            } else {
                settings.setRingtoneURI(ProviderSettings.RINGTONE_DEFAULT);
            }*/
        }
        else if (key.equals("pref_foreground_enable")) {
            settings.setUseForegroundPriority(prefs.getBoolean(key, true));
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
        else if (key.equals("pref_language"))
        {
            String newLanguage = prefs.getString(key, Languages.USE_SYSTEM_DEFAULT);
            if (!TextUtils.equals(currentLanguage, newLanguage)) {
                ((ImApp)getApplication()).setNewLocale(this, newLanguage);
                setResult(RESULT_OK);
                finish(); // go to main screen to reset language
            }
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

        mOtrMode = (ListPreference) findPreference("pref_security_otr_mode");
        mLanguage = (ListPreference) findPreference("pref_language");
        mLinkifyOnTor = (CheckBoxPreference) findPreference("pref_linkify_on_tor");
        mHideOfflineContacts = (CheckBoxPreference) findPreference("pref_hide_offline_contacts");
        mDeleteUnsecuredMedia = (CheckBoxPreference) findPreference("pref_delete_unsecured_media");
        mStoreMediaOnExternalStorage = (CheckBoxPreference) findPreference("pref_store_media_on_external_storage");
        mEnableNotification = (CheckBoxPreference) findPreference("pref_enable_notification");
        mNotificationVibrate = (CheckBoxPreference) findPreference("pref_notification_vibrate");
        mNotificationSound = (CheckBoxPreference) findPreference("pref_notification_sound");

        mNotificationRingtone = findPreference("pref_notification_ringtone");

        Languages languages = Languages.get(this);
        currentLanguage = getResources().getConfiguration().locale.getLanguage();
        mLanguage.setDefaultValue(currentLanguage);
        mLanguage.setEntries(languages.getAllNames());
        mLanguage.setEntryValues(languages.getSupportedLocales());

        mNotificationRingtone.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {

            @Override
            public boolean onPreferenceClick(Preference arg0) {

                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_ringtone_title));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                startActivityForResult(intent, 5);
                return true;
            }

        });

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

                    //Link to the image
                    String imageFilePath = getRealPathFromURI(_uri);
                    
                    if (imageFilePath != null)                    
                        mThemeBackground.setText(imageFilePath);
                    
                    mThemeBackground.getDialog().cancel();

            }

        }
        else if (resultCode == Activity.RESULT_OK && requestCode == 5)
        {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            ContentResolver cr = getContentResolver();
            Cursor pCursor = cr.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString( Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS)},null);

            Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(pCursor, cr,
                    Imps.ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS,     false /* keep updated */, null /* no handler */);

            if (uri != null)
            {

                settings.setRingtoneURI(uri.toString());

            }
            else
            {
                settings.setRingtoneURI(null);
            }

            settings.close();
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
    

    private String getRealPathFromURI(Uri contentURI) {
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            return contentURI.getPath();
        } else { 
            cursor.moveToFirst(); 
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA); 
            if (idx > -1)
                return cursor.getString(idx);
            else
                return contentURI.toString();
        }
    }


    private void showThemeChooserDialog ()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.dialog_settings_choose_background_title));
        builder.setMessage(getString(R.string.dialog_settings_choose_background_body));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.dialog_settings_choose_background_picker)), 888);

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
