package info.guardianproject.otr.app.im.ui;

import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.provider.Imps;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class OnBootReceiver extends BroadcastReceiver {
	
	private boolean autoLaunchedOnce = false;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent serviceIntent = new Intent();
	//	serviceIntent.setAction("org.torproject.android.service.TorService");
		context.startService(serviceIntent);

		MainActivity mainActivity = new MainActivity();
		mainActivity.initXmpp();
		mainActivity.checkAccountAndSignin();
	}

	
}

