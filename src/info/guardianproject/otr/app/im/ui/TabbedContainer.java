package info.guardianproject.otr.app.im.ui;


import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ChatListActivity;
import info.guardianproject.otr.app.im.app.ContactListActivity;


import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;

public class TabbedContainer extends  TabActivity {
	private TabHost tabHost;
	public void onCreate(Bundle smurfy) {
	    super.onCreate(smurfy);
	    
	    // Create an bundle  to recieve the intent sent in from the launching activity
	    Bundle justPassingThrough = getIntent().getExtras();

	
	    
	    setContentView(R.layout.tab_container);

	    Resources res = getResources(); // Resource object to get Drawables 
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	   
	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, ChatListActivity.class);
	    intent.putExtras(justPassingThrough);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("chats").setIndicator("Chats",
	                      res.getDrawable(R.drawable.ic_tab_chats))
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, ContactListActivity.class);
	    intent.putExtras(justPassingThrough);
	    spec = tabHost.newTabSpec("contacts").setIndicator("Contacts",
	                      res.getDrawable(R.drawable.ic_tab_chats))
	                  .setContent(intent);
	    tabHost.addTab(spec);

//Value in parathes controls which tab element to view 
	    tabHost.setCurrentTab(0);
	    
	    
	}

}
