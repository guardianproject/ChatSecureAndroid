package info.guardianproject.otr.app.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.ironrabbit.TibConvert;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BhoTyper {
	
	public static String BHOTAG = "******************* LANG SERVICE **************";
	public static String FONT = "monlambodyig.ttf";
	
	Typeface bho;
	ArrayList<TextView> textViews = new ArrayList<TextView>();
	Context c;
	View root;
	
	public BhoTyper(Context c, View root) {
		this.c = c;
		this.root = root;
		bho = Typeface.createFromAsset(this.c.getAssets(), FONT);
	    
		refreshBho();
	}
	
	public static void parseForBhoViews(Context c, ViewGroup viewGroup) {
	    Typeface t = Typeface.createFromAsset(c.getAssets(), FONT);
	    Log.d(BhoTyper.BHOTAG, "view group with " + viewGroup.getChildCount() + " children");
	    for(int i=0; i<viewGroup.getChildCount(); i++) {
	        View v = viewGroup.getChildAt(i);
	        if(v instanceof LinearLayout)
	            parseForBhoViews(c, (ViewGroup) v);
	        else {
	            Log.d(BhoTyper.BHOTAG, "child: " + v.getClass().getName());
	            if(v instanceof TextView) {
	                ((TextView) v).setTypeface(t);
	                Log.d(BhoTyper.BHOTAG, "says: " + ((TextView) v).getText());
	            } else if(v instanceof EditText) {
	                ((EditText) v).setTypeface(t);
	                Log.d(BhoTyper.BHOTAG, "says: " + ((EditText) v).getText());
	            }
	        }
	    }
	}
	
	public void refreshBho() {
		parseForTextViews(this.root);

	    for(View v : textViews) {
	    	if(!(v instanceof android.widget.EditText)) {
	    		
	    		String oldText = ((TextView) v).getText().toString();
	    		String bhoText = TibConvert.convertUnicodeToPrecomposedTibetan(oldText);
	    		
	    		((TextView) v).setTypeface(bho, TextUtils.CAP_MODE_CHARACTERS);
	    		((TextView) v).setText(bhoText);
	    	} else {
	    		String oldHint = ((EditText) v).getHint().toString();
	    		String bhoHint = TibConvert.convertUnicodeToPrecomposedTibetan(oldHint);
	    		
	    		((EditText) v).setTypeface(bho, TextUtils.CAP_MODE_CHARACTERS);
	    		((EditText) v).setHint(bhoHint);
	    	}
	    }
	}
	
	public static int getIntValueFromContextualMenu(Map<Integer, String> opts, int which) {
		int item = -1;
	    int match = 0;
	    
	    Iterator<Integer> i = opts.keySet().iterator();
	    while(i.hasNext()) {
	    	int opt = i.next();
	    	if(match == which)
	    		item = opt;
	    	
	    	match++;
	    }
	    
	    return item;
	}
	
	private void parseForTextViews(View view) {
		try {
			if(view instanceof android.widget.TextView)
				textViews.add((TextView) view);
			else {
				ViewGroup vg = (ViewGroup) view;
				for(int v=0; v< vg.getChildCount(); v++)
					parseForTextViews((View) vg.getChildAt(v));
			}
		} catch(ClassCastException e) {}
	}
}
