package info.guardianproject.otr.app.im.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import info.guardianproject.otr.app.im.R;

public class AccountWizardActivity extends Activity implements OnClickListener
{
	//WizardHelper wizard = null;
	
	private int title[] = {
			R.string.account_wizard_setup_title,
			R.string.account_wizard_host_title,
			R.string.account_wizard_ready_title

	};
	
	private int msg[] = {
			R.string.account_wizard_setup_body,
			R.string.account_wizard_host_body,
			R.string.account_wizard_ready_body,

	};
	
	private String fields[][] =
	{
			{"Account ID",null},
			{"Hostname","Port Number"},
			{null, null},
	};
	
	private String buttons[][] =
	{
			{null,"Next"},
			{"Back","Look's Good!"},
			{"Back","Login"},
	};
	
	private View.OnClickListener listener[][] =
	{
			{
				null,
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
					
						parseAccount ();
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
					
						saveHostValues();
						nextContent();
						
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
	                                 
	
	private int contentIdx = -1;
	
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		
        if (contentIdx == -1)
        {
            setContentView(R.layout.fields_buttons_view);
        
        	nextContent ();
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
		
        TextView txtField1 = ((TextView)findViewById(R.id.lbl1));
        TextView txtField2 = ((TextView)findViewById(R.id.lbl2));
		EditText editAccountId1 = ((EditText)findViewById(R.id.edit1));
		EditText editAccountId2 = ((EditText)findViewById(R.id.edit2));

		

        if (fields[contentIdx][0] != null)
        {
        	txtField1.setText(fields[contentIdx][0]);

        	txtField1.setVisibility(TextView.VISIBLE);
        	editAccountId1.setVisibility(TextView.VISIBLE);
        }
        else
        {
        	txtField1.setVisibility(TextView.INVISIBLE);
        	editAccountId1.setVisibility(TextView.INVISIBLE);

        }
        	
        if (fields[contentIdx][1] != null)
        {
        	txtField2.setText(fields[contentIdx][1]);

        	txtField2.setVisibility(TextView.VISIBLE);
        	editAccountId2.setVisibility(TextView.VISIBLE);
        }
        else
        {
        	txtField2.setVisibility(TextView.INVISIBLE);
        	editAccountId2.setVisibility(TextView.INVISIBLE);

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
		
		String accountId = editAccountId.getText().toString();
		
		String[] split = accountId.split("@");
		String username = split[0];
		
		String hostname = null;
		String port = "5222";
		
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

			edit.putString("pref_account_host", hostname);
			edit.putString("pref_account_port", port);
	
			
			
			edit.commit();
			
			nextContent();
			
			EditText editAccountId1 = ((EditText)findViewById(R.id.edit1));
			EditText editAccountId2 = ((EditText)findViewById(R.id.edit2));
	
			editAccountId1.setText(hostname);
			editAccountId2.setText(port);
		}
		
	}
	
	private void saveHostValues ()
	{
		EditText editAccountId1 = ((EditText)findViewById(R.id.edit1));
		EditText editAccountId2 = ((EditText)findViewById(R.id.edit2));

		String hostname = editAccountId1.getText().toString();
		String port = editAccountId2.getText().toString();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
		
		Editor edit = prefs.edit();
		
		edit.putString("pref_account_host", hostname);
		edit.putString("pref_account_port", port);

		
		
		edit.commit();
	}
	
	
}
