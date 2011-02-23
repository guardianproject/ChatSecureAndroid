/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package info.guardianproject.otr.app.im.ui;

import info.guardianproject.otr.app.im.R;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.KeyEvent;


public class SettingsActivity 
		extends PreferenceActivity implements OnPreferenceClickListener {

	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		
		
	}
	
	
	@Override
	protected void onResume() {
	
		super.onResume();
	
		EditTextPreference pref = (EditTextPreference)((PreferenceCategory)getPreferenceScreen().getPreference(0)).getPreference(0);
		String value = pref.getText();
		
		if (value != null && value.length() > 0)
			pref.setSummary(value);
		
		
		
		
	};
	
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
		//Log.d(getClass().getName(),"Exiting Preferences");
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		
		
		
		
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	Intent intent = new Intent(getBaseContext(), MainActivity.class);
			
			intent.putExtra("showSettings",false);
			startActivityForResult(intent, 1);

	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	

}
