package info.guardianproject.otr.app.im.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import info.guardianproject.otr.app.im.R;

public class AccountWizardActivity extends Activity implements OnClickListener
{

	private String accountId = "";
	private String username = "";
	private String hostname = "";
	private String port = "5222";
	

	private EditText editAccountId1;
	private EditText editAccountId2;
	
	private int title[] = {
			R.string.account_wizard_setup_title,
			R.string.account_wizard_account_title,
			R.string.account_wizard_host_title,
			R.string.account_wizard_ready_title

	};
	
	private int msg[] = {
			R.string.account_wizard_setup_body,
			R.string.account_wizard_account_body,
			R.string.account_wizard_host_body,
			R.string.account_wizard_ready_body,

	};
	
	private String fields[][] =
	{
			{null,null},
			{"Account ID",null},
			{"Hostname","Port Number"},
			{null, null},
	};
	
	private String buttons[][] =
	{
			{null,"Next"},
			{null,"Next"},
			{"Back","Save"},
			{"Back","Let's Go!"},
	};
	
	private View.OnClickListener listener[][] =
	{
			{
				null,
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
					
						nextContent();
						
						editAccountId1.setText(accountId);
					}
				}
			},
			{
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						prevContent();

					}
				},
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
					
						parseAccount ();
						hideKeyboard ();
					}
				}
			},

			{
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						
						prevContent();
						editAccountId1.setText(accountId);

					}
				},
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
					
						saveHostValues();
						nextContent();
						hideKeyboard();
						
						
					}
				}
			},
			{
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						prevContent();
						
					}
				},
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
					
						
						Intent intent = new Intent(getBaseContext(), MainActivity.class);
						
						intent.putExtra("doSignIn",true);
						startActivityForResult(intent, 1);
						
					}
				}
			},

			

			
			
	};
	
	private void hideKeyboard ()
	{

		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editAccountId1.getWindowToken(), 0);
	}
	                                 
	
	private int contentIdx = -1;
	
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null)
        {
	        if (savedInstanceState.containsKey("contentIdx"))
	        	contentIdx = savedInstanceState.getInt("contentIdx");
	        
	        if (savedInstanceState.containsKey("accountId"))
	        	contentIdx = savedInstanceState.getInt("accountId");
	        
        }
        
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putInt("contentIdx", contentIdx);
		outState.putString("accountId", accountId);
		
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStart() {
		
		super.onStart();

        setContentView(R.layout.fields_buttons_view);

        if (contentIdx == -1)
        {
        
        	nextContent ();
        }
        else
        {
        	showContent(contentIdx);
        }
	}
	
	private void prevContent ()
	{
		contentIdx--;
		showContent(contentIdx);
	}
	
	private void nextContent ()
	{
		contentIdx++;
		showContent(contentIdx);
	}
	
	
	private void showContent (int contentIdx)
	{
		TextView txtTitle  = ((TextView)findViewById(R.id.WizardTextTitle));
		txtTitle.setText(getString(title[contentIdx]));
        
        TextView txtBody = ((TextView)findViewById(R.id.WizardTextBody));
		txtBody.setText(getString(msg[contentIdx]));
		
		editAccountId1 = ((EditText)findViewById(R.id.edit1));
		editAccountId1.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		
		editAccountId2 = ((EditText)findViewById(R.id.edit2));


        if (fields[contentIdx][0] != null)
        {
        	editAccountId1.setHint(fields[contentIdx][0]);

        	editAccountId1.setVisibility(TextView.VISIBLE);
        	
        }
        else
        {
        	editAccountId1.setVisibility(TextView.GONE);
        }
        	
        if (fields[contentIdx][1] != null)
        {
        	editAccountId2.setHint(fields[contentIdx][1]);

        	editAccountId2.setVisibility(TextView.VISIBLE);
        }
        else
        {
        	editAccountId2.setVisibility(TextView.GONE);

        }
        
        Button btn1 = ((Button)findViewById(R.id.btnWizard1));
        if (buttons[contentIdx][0] != null)
        {
        	btn1.setText(buttons[contentIdx][0]);
        	btn1.setOnClickListener(listener[contentIdx][0]);
        	btn1.setVisibility(Button.VISIBLE);
        	
        }
        else
        {
        	btn1.setVisibility(Button.INVISIBLE);
        }
        
        Button btn2 = ((Button)findViewById(R.id.btnWizard2));
        if (buttons[contentIdx][1] != null)
        {
        	btn2.setText(buttons[contentIdx][1]);
        	btn2.setOnClickListener(listener[contentIdx][1]);
        	btn2.setVisibility(Button.VISIBLE);
        	
        }
        else
        {
        	btn2.setVisibility(Button.INVISIBLE);
        }
        
      
      
	}
	
	

	@Override
	protected void onResume() {
		super.onResume();
	
		
	}




	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		
		
	}
	
	private void parseAccount ()
	{
		boolean isGood = false;
		
		EditText editAccountId = ((EditText)findViewById(R.id.edit1));
        // InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS would also be nice but Android 2.x only
		editAccountId.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		accountId = editAccountId.getText().toString();
		
		String[] split = accountId.split("@");
		username = split[0];
		
		hostname = null;
		port = "5222";
		
		if (split.length > 1)
		{
			hostname = split[1];
			
			split = hostname.split(":");
			
			hostname = split[0];
			
			if(split.length > 1)
				port = split[1];
		}
		
		String errMsg = "";
			
		if (hostname == null)
		{
			isGood = false;
			errMsg = "You didn't enter an @hostname.com part for your account ID. Try again!";
		}
		else if (hostname.indexOf(".")==-1)
		{
			isGood = false;
			errMsg = "Your server hostname didn't have a .com, .net or similar appendix. Try again!";
		}
		else
		{
			isGood = true;
		}
			
		
		if (!isGood)
		{
			Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
		}
		else
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
			
			Editor edit = prefs.edit();
			
			edit.putString("pref_account_user", username);
			edit.remove("pref_account_pass");

			edit.putString("pref_account_domain", hostname.toLowerCase());
			edit.putString("pref_account_port", port);
	
			edit.commit();
		
			
			nextContent();
			
			EditText editAccountId1 = ((EditText)findViewById(R.id.edit1));
			EditText editAccountId2 = ((EditText)findViewById(R.id.edit2));
	
			editAccountId1.setText(hostname);
			editAccountId2.setText(port);
			
			if (hostname.equals("gmail.com") || hostname.equals("jabber.org"))
			{
				nextContent();
			}
			
		}
		
	}
	
	private void saveHostValues ()
	{
		EditText editAccountId1 = ((EditText)findViewById(R.id.edit1));
		EditText editAccountId2 = ((EditText)findViewById(R.id.edit2));

		hostname = editAccountId1.getText().toString();
		port = editAccountId2.getText().toString();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
		
		Editor edit = prefs.edit();
		
		edit.putString("pref_account_domain", hostname.toLowerCase());
		edit.putString("pref_account_port", port);

		
		
		edit.commit();
	}
	
	
}
