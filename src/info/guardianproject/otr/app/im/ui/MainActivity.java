package info.guardianproject.otr.app.im.ui;

import java.util.List;

import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.app.ImPluginHelper;
import info.guardianproject.otr.app.im.app.ProviderDef;
import info.guardianproject.otr.app.im.app.SettingActivity;
import info.guardianproject.otr.app.im.app.SigningInActivity;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
    static final int REQUEST_SIGN_IN = RESULT_FIRST_USER + 1;
    long mProviderId = 1;
    // TODO get mAccountId and mProviderId for real
    private long mAccountId;
    ProviderDef provider;
    Uri mAccountUri;
    ImApp app;
    
    boolean autoLaunchedOnce = false;
    
    protected static final int ID_SIGNIN = Menu.FIRST + 1;

    private String user;
    private String host;
    private String port;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        initXmpp();
    }
    
    public void initXmpp ()
    {
    	app = ImApp.getApplication(this);
        ImPluginHelper.getInstance(this).loadAvaiablePlugins();
	    provider = app.getProviders().get(0);//the default provider XMPP

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

        user = prefs.getString("pref_account_user", null);
        host = prefs.getString("pref_account_domain", null);
        port = prefs.getString("pref_account_port", null);
        
    }
    
    private void showUI () {
        setContentView(R.layout.splash_activity);
     
        if (user == null || user.length() == 0) {
	        Button btnSplashAbout = ((Button)findViewById(R.id.btnSplashAbout));
	        btnSplashAbout.setOnClickListener(new OnClickListener()
	        {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getBaseContext(), AboutActivity.class);
			        intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
			        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
					startActivityForResult(intent, 1);
				}
	        });
	        
	        Button btnSplashSetup = ((Button)findViewById(R.id.btnSplashSetup));
	        btnSplashSetup.setOnClickListener(new OnClickListener()
	        {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getBaseContext(), AccountWizardActivity.class);
					intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
					intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
					startActivityForResult(intent, 1);
				}
	        });
        }
        else {
        	View view = findViewById(R.id.splashSetupButtons);
        	view.setVisibility(View.GONE);
        }
    }
    
    public boolean checkAccountAndSignin() {
    	
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        user = prefs.getString("pref_account_user", null);

        
        if (user == null || user.length() == 0)
        {
        	Toast.makeText(getBaseContext(), "Please setup an account to login", Toast.LENGTH_SHORT).show();
        	return false;
        }
        else 
        {
            host = prefs.getString("pref_account_domain", null);
            port = prefs.getString("pref_account_port", null);
            final String pass = prefs.getString("pref_account_pass", null);

            ContentResolver cr = getContentResolver();
            mAccountId = ImApp.insertOrUpdateAccount(cr, mProviderId, user, pass);
            mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, mAccountId);
            signIn();
    
            return true;
        }
    }
    
    void signIn() {
        
    	Intent intent = new Intent(MainActivity.this, SigningInActivity.class);
        intent.setData(mAccountUri);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
        intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

        boolean useTor = prefs.getBoolean("pref_security_use_tor", false);
        
        if (useTor) {
        	// TODO move ImApp.EXTRA_INTENT_PROXY_* to ImServiceConstants
        	intent.putExtra(ImApp.EXTRA_INTENT_PROXY_TYPE,"SOCKS5");
        	intent.putExtra(ImApp.EXTRA_INTENT_PROXY_HOST,"127.0.0.1");
        	intent.putExtra(ImApp.EXTRA_INTENT_PROXY_PORT,9050);
        }
       
    	
        
       // if (mToAddress != null) {
         //   intent.putExtra(ImApp.EXTRA_INTENT_SEND_TO_USER, mToAddress);
        //}

        startActivityForResult(intent, REQUEST_SIGN_IN);
    }
    
    
    @Override
	protected void onResume() {
		super.onResume();
		

		showUI();
       
		boolean doSignIn = true;
		
		Bundle extras = getIntent().getExtras();
        if (extras != null) {
	        boolean showSettings = extras.getBoolean("showSettings", false);
	        if (showSettings) {
	        	Intent intent = new Intent(getBaseContext(), SettingActivity.class);
	        	intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
	        	intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
	        	startActivityForResult(intent, 1);
	        	return;
	        }
	        doSignIn = extras.getBoolean("doSignIn",true);
        }
        
        if(doSignIn && (!autoLaunchedOnce))
        {
        	autoLaunchedOnce = true;
        	checkAccountAndSignin();
        }
	}

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
        if (item.getItemId() == R.id.menu_sign_in) {
           
        	checkAccountAndSignin();
        	
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
        	Intent intent = new Intent(getBaseContext(), SettingActivity.class);
        	intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
        	intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
        	startActivityForResult(intent, 1);
        	
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_SIGN_IN) {
            if (resultCode == RESULT_OK) {
               
             //  finish();
            } else {
                // sign in failed, disable keep sign in, clear the password.
              
              //  ContentValues values = new ContentValues();
               // values.put(Imps.Account.PASSWORD, (String) null);
                //getContentResolver().update(mAccountUri, values, null, null);
            }
        }
    }
}