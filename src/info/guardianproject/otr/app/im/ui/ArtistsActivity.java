package info.guardianproject.otr.app.im.ui;

import info.guardianproject.otr.app.im.R;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ArtistsActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TextView textview = new TextView(this);
        textview.setText("This is the Artists tab");
        setContentView(textview);
    }
}