package info.guardianproject.otr.app.lang;

import info.guardianproject.otr.app.im.R;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

public class BhoAlertDialog extends AlertDialog.Builder {
	BhoButton btn1, btn2, btn3;
	View messageView, titleView;
	Context context;
	
	boolean hasImage;
	List<BhoButton> hasButtons;
	
	public BhoAlertDialog(Context context) {
       super(context);
       this.context = context;
       
       LayoutInflater li = LayoutInflater.from(context);
       messageView = li.inflate(context.getResources().getLayout(R.layout.bho_alert_dialog), null);
       titleView = li.inflate(context.getResources().getLayout(R.layout.bho_alert_dialog_title), null);
       
       setView(messageView);
       setCustomTitle(titleView);
       
       hasImage = false;
       hasButtons = new ArrayList<BhoButton>();
    }
	
	@Override
	public Builder setTitle(CharSequence title) {
		BhoTextView titleHolder = (BhoTextView) titleView.findViewById(R.id.bho_alert_title);
		titleHolder.setText(title);
		
		return rebuild();
	}
	
	@Override
	public Builder setTitle(int titleId) {
		BhoTextView titleHolder = (BhoTextView) titleView.findViewById(R.id.bho_alert_title);
		titleHolder.setText(context.getResources().getString(titleId));
		
		return rebuild();
	}
    
	@Override 
	public Builder setMessage(CharSequence message) {
		BhoTextView messageHolder = (BhoTextView) messageView.findViewById(R.id.bho_alert_message);
		messageHolder.setText(message);
		
		
		return rebuild();
	}
	
	@Override 
	public Builder setMessage(int messageId) {
		BhoTextView messageHolder = (BhoTextView) messageView.findViewById(R.id.bho_alert_message);
		messageHolder.setText(context.getResources().getString(messageId));		
		return rebuild();
	}
	
	@Override
	public Builder setIcon(int drawable) {
		hasImage = true;
		return rebuild();
	}
	
	@Override
	public Builder setIcon(Drawable drawable) {
		hasImage = true;
		return rebuild();
	}
	
	@Override
	public Builder setPositiveButton(int textId, DialogInterface.OnClickListener ocl) {
		btn1 = (BhoButton) messageView.findViewById(R.id.button1);
		btn1.setText(context.getResources().getString(textId));
		
		if(!hasButtons.contains(btn1))
			hasButtons.add(btn1);
		
		return rebuild();
	}
	
	@Override
	public Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener ocl) {
		btn1 = (BhoButton) messageView.findViewById(R.id.button1);
		btn1.setText(text);
		
		
		if(!hasButtons.contains(btn1))
			hasButtons.add(btn1);
		
		return rebuild();
	}
	
	@Override
	public Builder setNegativeButton(int textId, DialogInterface.OnClickListener ocl) {
		btn2 = (BhoButton) messageView.findViewById(R.id.button1);
		btn2.setText(context.getResources().getString(textId));
		
		if(!hasButtons.contains(btn2))
			hasButtons.add(btn2);
		
		
		return rebuild();
	}
	
	@Override
	public Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener ocl) {
		btn2 = (BhoButton) messageView.findViewById(R.id.button1);
		btn2.setText(text);
		
		if(!hasButtons.contains(btn2))
			hasButtons.add(btn2);
		
		return rebuild();
	}
	
	@Override
	public Builder setNeutralButton(int textId, DialogInterface.OnClickListener ocl) {
		btn3 = (BhoButton) messageView.findViewById(R.id.button1);
		btn3.setText(context.getResources().getString(textId));
		
		if(!hasButtons.contains(btn3))
			hasButtons.add(btn3);
		
		return rebuild();
	}
	
	@Override
	public Builder setNeutralButton(CharSequence text, DialogInterface.OnClickListener ocl) {
		btn3 = (BhoButton) messageView.findViewById(R.id.button1);
		btn3.setText(text);
		
		if(!hasButtons.contains(btn3))
			hasButtons.add(btn3);
		
		return rebuild();
	}
	
	public Builder rebuild() {
		if(hasImage) {
			
		}
		
		for(BhoButton b : hasButtons)
			b.setVisibility(View.VISIBLE);
		
		return this;
	}
	

}