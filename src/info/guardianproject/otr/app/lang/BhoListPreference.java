package info.guardianproject.otr.app.lang;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class BhoListPreference extends ListPreference {
    
    public BhoListPreference(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }
    
    public BhoListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        Log.d(BhoTyper.BHOTAG, "creating this view!");
        super.onPrepareDialogBuilder(builder);
    }

}
