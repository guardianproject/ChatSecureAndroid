package info.guardianproject.otr.app.im.app;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;

public class ThemeableActivity extends SherlockActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        ((ImApp)this.getApplication()).setAppTheme(this);
        
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        ((ImApp)this.getApplication()).setAppTheme(this);
        super.onResume();
    }

    
}
