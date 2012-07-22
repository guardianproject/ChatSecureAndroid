package info.guardianproject.otr.app.lang;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class BhoEditTextPreference extends EditTextPreference {
    Context c;
    private static Typeface t;
        
    public BhoEditTextPreference(Context context) {
        super(context);
        this.c = context;
        
    }
    
    public BhoEditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextPreferenceStyle);
    }
    
    public BhoEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.c = context;
        
        if(t == null)
            t = Typeface.createFromAsset(this.c.getAssets(), BhoTyper.FONT);
        
        this.getEditText().setTypeface(t);
        
    }
    
    @Override
    protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
        super.onAddEditTextToDialogView(dialogView, editText);
        
        BhoTyper.parseForBhoViews(c, (ViewGroup) dialogView);
    }
}
