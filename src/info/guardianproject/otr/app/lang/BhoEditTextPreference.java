package info.guardianproject.otr.app.lang;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class BhoEditTextPreference extends EditTextPreference {
    Context c;
    private static Typeface t;
    
    public BhoEditTextPreference(Context context) {
        super(context);
        
        this.c = context;
        
    }
    
    public BhoEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        this.c = context;
        
    }

}
