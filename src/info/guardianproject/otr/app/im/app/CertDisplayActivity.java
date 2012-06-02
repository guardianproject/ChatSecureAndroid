package info.guardianproject.otr.app.im.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;

public class CertDisplayActivity extends Activity {

    private AlertDialog ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String issuer = getIntent().getStringExtra("issuer");
        String fingerprint = getIntent().getStringExtra("fingerprint");
        String subject = getIntent().getStringExtra("subject");
        String issuedOn = getIntent().getStringExtra("issued");
        String expiresOn = getIntent().getStringExtra("expires");

        showDialog("Issued by: " + issuer + "\nIssued to: " + subject + "\nSHA1 Fingerprint: "
                   + fingerprint + "\nIssued on: " + issuedOn + "\nExpires on" + expiresOn);
    }

    private void showDialog(String msg) {

        ad = new AlertDialog.Builder(this).setTitle("Certificate Info").setMessage(msg).show();

        ad.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {

                CertDisplayActivity.this.finish();

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
