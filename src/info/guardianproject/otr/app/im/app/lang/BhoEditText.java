package info.guardianproject.otr.app.im.app.lang;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

public class BhoEditText extends EditText {
	Context c;
	private static Typeface t;
	
<<<<<<< HEAD
	public BhoEditText(Context context) {
		super(context);
		this.c = context;
		
		if(t == null)
			t = Typeface.createFromAsset(this.c.getAssets(), BhoTyper.FONT);
		
		setTypeface(t);
		
	}
	
=======
>>>>>>> 11c0a53ade8fd7484e982783986a06cd8f5a564a
	public BhoEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.c = context;
		
		if(t == null)
			t = Typeface.createFromAsset(this.c.getAssets(), BhoTyper.FONT);
		
		setTypeface(t);
		
	}
	
	@Override
	public void setTypeface(Typeface typeface) {
		super.setTypeface(typeface);
	}

}
