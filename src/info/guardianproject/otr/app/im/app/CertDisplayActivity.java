package info.guardianproject.otr.app.im.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;

public class CertDisplayActivity extends Activity {

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String msg = getIntent().getStringExtra("msg");
		String issuer = getIntent().getStringExtra("issuer");
		String fingerprint = getIntent().getStringExtra("fingerprint");
		String subject = getIntent().getStringExtra("subject");
		String issuedOn = getIntent().getStringExtra("issued");
		String expiresOn = getIntent().getStringExtra("expires");
		

		showDialog (msg + "\nIssued by: " + issuer  + "\nIssued to: "  + subject  + "\nSHA1 Fingerprint: " + fingerprint
				+ "\nIssued on: " + issuedOn + "\nExpires on" + expiresOn
				);
	}
	
	
	
	private void showDialog (String msg)
	{
		
		AlertDialog ad = new AlertDialog.Builder(this)
	    .setTitle("Certificate Info")
	    .setMessage(msg).show();
		
		ad.setOnDismissListener(new OnDismissListener () {

			@Override
			public void onDismiss(DialogInterface arg0) {
				
				CertDisplayActivity.this.finish();
				
			}
			
		});
		
		
	}
	
	

	
}
