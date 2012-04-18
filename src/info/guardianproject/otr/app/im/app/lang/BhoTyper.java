package info.guardianproject.otr.app.im.app.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
	    
	}
}
