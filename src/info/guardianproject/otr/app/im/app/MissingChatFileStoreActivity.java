package info.guardianproject.otr.app.im.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import info.guardianproject.otr.app.im.R;

import java.io.File;

public class MissingChatFileStoreActivity extends ThemeableActivity {
    private static final String TAG = "MissingChatFileStoreActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.missing_chat_file_store);

        TextView titleTextView = (TextView) findViewById(R.id.title);
        TextView messageTextView = (TextView) findViewById(R.id.message);
        Button deleteChatLogButton = (Button) findViewById(R.id.delete_chat_log);
        Button shutdownAndLockButton = (Button) findViewById(R.id.shutdown_and_exit);

        if (getExternalFilesDir(null) == null) {
            titleTextView.setText(R.string.external_storage_missing_title);
            messageTextView.setText(R.string.external_storage_missing_message);
        } else {
            titleTextView.setText(R.string.media_store_file_missing_title);
            messageTextView.setText(R.string.media_store_file_missing_message);
        }
        deleteChatLogButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "init try again onClick");
                Context c = getApplicationContext();
                new File(ChatFileStore.getInternalDbFilePath(c)).delete();
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
                Editor editor = settings.edit();
                editor.putBoolean(getString(R.string.key_store_media_on_external_storage_pref),
                        false);
                editor.apply();
                finish();
            }
        });
        shutdownAndLockButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "shutdownAndLock onClick");
                WelcomeActivity.shutdownAndLock(MissingChatFileStoreActivity.this);
            }
        });
    }

}
