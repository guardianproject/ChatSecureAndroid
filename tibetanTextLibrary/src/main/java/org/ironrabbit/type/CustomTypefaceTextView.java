package org.ironrabbit.type;


import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class CustomTypefaceTextView extends TextView {

    boolean mInit = false;
    
    public CustomTypefaceTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
   
       init();
    }

    
    
    public CustomTypefaceTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
	       init();
	}



	public CustomTypefaceTextView(Context context) {
		super(context);
	
	       init();
	}



	private void init() {
    	
		if (!mInit)
        {
			Typeface t = CustomTypefaceManager.getCurrentTypeface(getContext());
			
			if (t != null)
				setTypeface(t);
	    	 
			mInit = true;
        }
        
        /*
        setOnLongClickListener(new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				  ClipboardManager cm = (ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
				  
				  String shareText = getText().toString();
				  
				  if (CustomTypefaceManager.precomposeRequired())
					  shareText = TibConvert.convertPrecomposedTibetanToUnicode(shareText,0,shareText.length());
	               
				  cm.setText(shareText);
	              Toast.makeText(mContext, "Text copied", Toast.LENGTH_SHORT).show();
	            return true;
			}
        });*/
        
    }



	@Override
	public void setText(CharSequence text, BufferType type) {
		init();
		super.setText(text, type);
	}
    



}