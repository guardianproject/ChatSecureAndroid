package info.guardianproject.otr.app.im.ui;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ChatListActivity;
import info.guardianproject.otr.app.im.app.ContactListActivity;
import info.guardianproject.otr.app.im.app.ImApp;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TabHost;

public class OldTabbedContainer extends TabActivity {

    @Override
    public void onCreate(Bundle smurfy) {
        super.onCreate(smurfy);

        // Create an bundle  to recieve the intent sent in from the launching activity
        String passThruAction = getIntent().getAction();
        Uri passThruData = getIntent().getData();
        Bundle passThruExtras = getIntent().getExtras();

        setContentView(R.layout.tab_container);

        Resources res = getResources(); // Resource object to get Drawables 
        TabHost tabHost = getTabHost(); // The activity TabHost
        TabHost.TabSpec spec; // Resusable TabSpec for each tab
        Intent intent; // Reusable Intent for each tab

        // Do the same for the other tabs
        intent = new Intent().setClass(this, ContactListActivity.class);
        intent.setAction(passThruAction);
        intent.setData(passThruData);
        intent.putExtras(passThruExtras);
        spec = tabHost
                .newTabSpec("contacts")
                .setIndicator(getString(R.string.menu_contact_list),
                        res.getDrawable(R.drawable.ic_tab_contacts)).setContent(intent);
        tabHost.addTab(spec);

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, ChatListActivity.class);
        intent.setAction(passThruAction);
        intent.setData(passThruData);
        intent.putExtras(passThruExtras);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost
                .newTabSpec("chats")
                .setIndicator(getString(R.string.title_chats),
                        res.getDrawable(R.drawable.ic_tab_chats)).setContent(intent);
        tabHost.addTab(spec);

        //Value in parathes controls which tab element to view 
        tabHost.setCurrentTab(0);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        boolean updated = ((ImApp)getApplication()).checkLocale();
        
        if (updated)
        {
                     
           startActivity(getIntent());
           finish(); 
        }
        
    }

}
