package info.guardianproject.otr.app.lang;

import info.guardianproject.otr.app.lang.BhoRadioButtonListAdapter.OnBhoSelectedListener;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;

public class BhoListPreference extends ListPreference implements OnBhoSelectedListener {
    Context context;
    List<BhoOptions> options;
    int selectedItem;
    
    public BhoListPreference(Context context) {
        super(context);
        this.context = context;
    }
    
    public BhoListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }
    
    public void getOptions() {
        
        options = new ArrayList<BhoOptions>();
        CharSequence[] entryValues = getEntryValues();
        
        String value = PreferenceManager.getDefaultSharedPreferences(context).getString(this.getKey(), "");
                
        int i = 0;
        for(CharSequence cs : getEntries()) {
            Log.d(BhoTyper.BHOTAG, entryValues[i].toString());
            try {
                if(value.equals(entryValues[i]))
                    options.add(new BhoOptions(cs.toString(), true));
                else
                    options.add(new BhoOptions(cs.toString(), false));
            } catch(NullPointerException e) {
                options.add(new BhoOptions(cs.toString(), false));
            }
            i++;
        }
    }
    
    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        getOptions();
        BhoRadioButtonListAdapter adapter = new BhoRadioButtonListAdapter(this, context, options);
        builder.setAdapter(adapter, null);
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    public void onItemSelected(int which) {
        setValueIndex(which);
        this.persistString(this.getValue());
    }

}
