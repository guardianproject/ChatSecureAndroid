package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.IOtrChatSession;

import java.util.List;

import net.java.otr4j.OtrException;
import net.java.otr4j.session.TLV;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.EditText;

public class SmpResponseActivity extends Activity {

	private EditText mInputSMP;
	private String mSessionId;
	private String mQuestion;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mInputSMP = new EditText(this);

		mSessionId = getIntent().getStringExtra("sid");
		mQuestion = getIntent().getStringExtra("q");
		showQuestionDialog ();
	}
	
	
	
	private void showQuestionDialog ()
	{
		
		new AlertDialog.Builder(this)
	    .setTitle("OTR Verification")
	    .setMessage(mQuestion)
	    .setView(mInputSMP)
	    .setPositiveButton("Answer", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            
	            String secret = mInputSMP.getText().toString();
	    		respondSmp(mSessionId, secret);
	    		
	    		SmpResponseActivity.this.finish();
	        }
	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            // Do nothing.
	        }
	    }).show();
		
	}
	
	private void respondSmp (String sid, String answer)
    {
     	ImApp app = ImApp.getApplication(this);

     	IOtrChatSession iOtrSession;
 		try {
 			iOtrSession = app.getActiveConnections().get(0).getChatSessionManager().getChatSession(sid).getOtrChatSession();
 			iOtrSession.respondSmpVerification(answer);
 	    	
 		} catch (RemoteException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
    }

	
}
