package info.guardianproject.otr.app.im.app.lang;

import info.guardianproject.otr.app.im.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabWidget;

public class BhoTab {
	public View bhoTab;
	
	public BhoTab(Context context, TabWidget tabWidget, String labelText, Drawable iconDrawable) {
		bhoTab = LayoutInflater.from(context).inflate(R.layout.bho_tab, tabWidget, false);
		
		iconDrawable.setBounds(0, 0, 60, 60);
		
		BhoTextView label = (BhoTextView) bhoTab.findViewById(R.id.bho_tab_label);
		label.setText(labelText);
		label.setCompoundDrawables(null, iconDrawable, null, null);
	}
}
