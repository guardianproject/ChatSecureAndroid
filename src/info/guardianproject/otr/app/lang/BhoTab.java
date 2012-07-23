package info.guardianproject.otr.app.lang;

import info.guardianproject.otr.app.im.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabWidget;

public class BhoTab {
    public View tab;
    
    public BhoTab(Context c, TabWidget t, String label, int resId) {
        tab = LayoutInflater.from(c).inflate(R.layout.bho_tab_indicator, t, false);
        BhoTextView title = (BhoTextView) tab.findViewById(R.id.title);
        title.setText(label);
        
        if(resId != 0) {
            ImageView icon = (ImageView) tab.findViewById(R.id.icon);
            icon.setImageResource(resId);
        }
    }
}
