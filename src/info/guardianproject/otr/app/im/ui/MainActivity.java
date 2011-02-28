package info.guardianproject.otr.app.im.ui;

import java.util.List;

import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.app.ImPluginHelper;
import info.guardianproject.otr.app.im.app.ProviderDef;
import info.guardianproject.otr.app.im.app.SigningInActivity;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.provider.Imps;
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
    long providerId = 1;
    ProviderDef provider;
    Uri mAccountUri;
    ImApp app;
    
    boolean autoLaunchedOnce = false;
    
    protected static final int ID_SIGNIN = Menu.FIRST + 1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        initXmpp();
    
        showUI();
    }
   
    public void initXmpp ()
    {
    	app = ImApp.getApplication(this);
        ImPluginHelper.getInstance(this).loadAvaiablePlugins();
	    provider = app.getProviders().get(0);//the default provider XMPP
        
    }
    
    private void showUI ()
    {
        setContentView(R.layout.splash_activity);
     
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
				
				startActivityForResult(new Intent(getBaseContext(), AccountWizardActivity.class), 1);

				
			}
        	
        	
        });
    }
    
    public void checkAccountAndSignin() {
    	
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

        String user = prefs.getString("pref_account_user", null);
        String host = prefs.getString("pref_account_host", null);
        String port = prefs.getString("pref_account_port", null);
        
        if (user == null || user.length() == 0)
        {
        	Toast.makeText(getBaseContext(), "Please setup an account to login", Toast.LENGTH_SHORT).show();
        }
        else 
        {
        	        	
            String userHostKey = java.net.URLEncoder.encode(user) + '@' + host + ':' + port;
            
            final String pass = prefs.getString("pref_account_pass", null);

            ContentResolver cr = getContentResolver();
            
            long accountId = ImApp.insertOrUpdateAccount(cr, providerId, userHostKey,pass);
            
            mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
            signIn();
    
        }
    }
    
    void signIn() {
        
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
		
		boolean doSignIn = true;
		
		Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
	        boolean showSettings = extras.getBoolean("showSettings", false);
	        if (showSettings)
	        {
	        	startActivityForResult(new Intent(getBaseContext(), SettingsActivity.class), 1);
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
           
        	startActivityForResult(new Intent(getBaseContext(), SettingsActivity.class), 1);
        	
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