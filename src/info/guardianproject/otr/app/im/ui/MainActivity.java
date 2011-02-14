package info.guardianproject.otr.app.im.ui;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.app.ImPluginHelper;
import info.guardianproject.otr.app.im.app.ProviderDef;
import info.guardianproject.otr.app.im.app.SigningInActivity;
import info.guardianproject.otr.app.im.provider.Imps;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
    static final int REQUEST_SIGN_IN = RESULT_FIRST_USER + 1;
    long providerId = 1;
    ProviderDef provider;
    Uri mAccountUri;

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        
        
        ImApp app = ImApp.getApplication(this);
        ImPluginHelper.getInstance(this).loadAvaiablePlugins();

        provider = app.getProviders().get(0);//the default provider XMPP
        
        setContentView(R.layout.splash_activity);
        
        /*ImageView imgSplash = ((ImageView)findViewById(R.id.imgSplash));
        
        imgSplash.setOnClickListener(new OnClickListener() {
           

         
        });
*/
        
       
        
        Button btnSplashAbout = ((Button)findViewById(R.id.btnSplashAbout));
        btnSplashAbout.setOnClickListener(new OnClickListener()
        {

			@Override
			public void onClick(View v) {
				
				startActivityForResult(new Intent(getBaseContext(), AboutActivity.class), 1);

				
			}
        	
        	
        });
        
        Button btnSplashSetup = ((Button)findViewById(R.id.btnSplashSetup));
        btnSplashSetup.setOnClickListener(new OnClickListener()
        {

			@Override
			public void onClick(View v) {
				
				startActivityForResult(new Intent(getBaseContext(), SettingsActivity.class), 1);

				
			}
        	
        	
        });
    }
    
    public void checkAccountAndSignin() {
    	
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

        String user = prefs.getString("pref_account_user", null);
        String host = prefs.getString("pref_account_host", null);
        String port = prefs.getString("pref_account_port", null);
        
        if (user == null)
        {
        	Toast.makeText(getBaseContext(), "Please setup an account to login", Toast.LENGTH_SHORT).show();
        }
        else
        {
            String userHostKey = java.net.URLEncoder.encode(user) + '@' + host + ':' + port;
            
            final String pass = prefs.getString("pref_account_pass", null);
            final boolean rememberPass = true;

            ContentResolver cr = getContentResolver();

            long accountId = ImApp.insertOrUpdateAccount(cr, providerId, userHostKey,
                    rememberPass ? pass : null);
            
            
            mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
            signIn(rememberPass, pass);
        }
    }
    
    void signIn(boolean rememberPass, String pass) {
        Intent intent = new Intent(MainActivity.this, SigningInActivity.class);
        intent.setData(mAccountUri);
        
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

        boolean useTor = prefs.getBoolean("pref_security_use_tor", false);
        
        if (useTor)
        {
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
		
		Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
	        boolean showSettings = extras.getBoolean("showSettings", false);
	        if (showSettings)
	        {
	        	startActivityForResult(new Intent(getBaseContext(), SettingsActivity.class), 1);
	        	return;
	        }
	       
        }
        
        checkAccountAndSignin();
        
	}




	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_SIGN_IN) {
            if (resultCode == RESULT_OK) {
               
                finish();
            } else {
                // sign in failed, disable keep sign in, clear the password.
              
              //  ContentValues values = new ContentValues();
               // values.put(Imps.Account.PASSWORD, (String) null);
                //getContentResolver().update(mAccountUri, values, null, null);
            }
        }
    }
}