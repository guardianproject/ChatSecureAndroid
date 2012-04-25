package info.guardianproject.otr.app.im.app.lang;

import info.guardianproject.otr.app.im.R;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BhoAlertDialog extends AlertDialog.Builder {
	BhoButton bnt1, btn2, btn3;
	View messageView, titleView;
	Context context;
	
	public BhoAlertDialog(Context context) {
       super(context);
       this.context = context;
       
       LayoutInflater li = LayoutInflater.from(context);
       messageView = li.inflate(context.getResources().getLayout(R.layout.bho_alert_dialog), null);
       titleView = li.inflate(context.getResources().getLayout(R.layout.bho_alert_dialog_title), null);
       
    }
	
	@Override
	public Builder setTitle(CharSequence title) {
		BhoTextView titleHolder = (BhoTextView) titleView.findViewById(R.id.bho_alert_title);
		titleHolder.setText(title);
		
		return setCustomTitle(titleView);
	}
	
	@Override
	public Builder setTitle(int titleId) {
		BhoTextView titleHolder = (BhoTextView) titleView.findViewById(R.id.bho_alert_title);
		titleHolder.setText(context.getResources().getString(titleId));
		
		return setCustomTitle(titleView);
	}
    
	@Override 
	public Builder setMessage(CharSequence message) {
		BhoTextView messageHolder = (BhoTextView) messageView.findViewById(R.id.bho_alert_message);
		messageHolder.setText(message);
		this.setView(messageView);
		
		return this;
	}
	
	@Override 
	public Builder setMessage(int messageId) {
		BhoTextView messageHolder = (BhoTextView) messageView.findViewById(R.id.bho_alert_message);
		messageHolder.setText(context.getResources().getString(messageId));
		this.setView(messageView);
		
		return this;
	}
	

}
