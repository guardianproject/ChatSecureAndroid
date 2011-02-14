package info.guardianproject.otr.app.im.ui;

import info.guardianproject.otr.app.im.R;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AppPassphraseActivity extends Activity implements OnClickListener
{
	
	private String buttons[][] =
	{
			{"Cancel","Next"}
			
	};
	
	private View.OnClickListener listener[][] =
	{
		
			{
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
				
						startActivityForResult(new Intent(getBaseContext(), MainActivity.class), 1);

					}
				},
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
					
						startActivityForResult(new Intent(getBaseContext(), SettingsActivity.class), 1);

						
					}
				}
			}

			
	};
	                                 
	
	private int contentIdx = -1;
	
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		
        setContentView(R.layout.passphrase_view);
        
        showContent ();

	}
	
	private void showContent ()
	{
		contentIdx = 0;
		
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
	
	
	
}
