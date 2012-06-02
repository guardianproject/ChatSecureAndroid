package info.guardianproject.otr.app.im.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;

public class WarningDialogActivity extends Activity {

    private AlertDialog ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String title = getIntent().getStringExtra("title");

        String msg = getIntent().getStringExtra("msg");

        showDialog(title, msg);
    }

    private void showDialog(String title, String msg) {

        ad = new AlertDialog.Builder(this).setTitle(title)
                .setIcon(android.R.drawable.ic_dialog_alert).setMessage(msg).show();

        ad.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {

                WarningDialogActivity.this.finish();

            }

        });

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (ad != null)
            ad.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (ad != null)
            ad.cancel();

    }

}
