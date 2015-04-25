/* See LICENSE for licensing information */

package org.ironrabbit.type;


import android.os.Bundle;
import android.preference.PreferenceActivity;


public class TypefacePreferences 
		extends PreferenceActivity  {

	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.typefaceprefs);
		
	}
	
	
	@Override
	protected void onResume() {
	
		super.onResume();
	
		
		
	};
	
	
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		
		//Log.d(getClass().getName(),"Exiting Preferences");
	}

	
}
