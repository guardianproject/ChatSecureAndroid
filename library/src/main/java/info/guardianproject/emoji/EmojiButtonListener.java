package info.guardianproject.emoji;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

public class EmojiButtonListener implements OnClickListener {

	  LinearLayout ll = null;
	  
	  public EmojiButtonListener (View view)
	  {
		  super ();     
		  ll = ((LinearLayout)view.findViewById(R.id.emoji_box));

	  }
	  
	  
	@Override
	public void onClick(View v) {
		
		if (ll.getVisibility() == View.GONE)
			ll.setVisibility(View.VISIBLE);
		else
			ll.setVisibility(View.GONE);
		
	}
	  
}