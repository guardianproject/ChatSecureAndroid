package info.guardianproject.otr.app.im.ui;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ThemeableActivity;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AppPassphraseActivity extends ThemeableActivity {

    private Dialog dl;

    private void gotCredentials(String usr, String pwd) {

    }

    private void foo() {
        dl = new Dialog(this);
        dl.setTitle("Information Prompt");

        dl.setContentView(R.layout.auth_view);
        EditText inputBox1 = (EditText) dl.findViewById(R.id.user);
        inputBox1.setText("");
        EditText inputBox2 = (EditText) dl.findViewById(R.id.pwd);
        inputBox2.setText("");

        Button bOk = (Button) dl.findViewById(R.id.ok);
        bOk.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditText inputBox1 = (EditText) dl.findViewById(R.id.user);
                String usr = inputBox1.getText().toString();
                inputBox1.setText("");

                EditText inputBox2 = (EditText) dl.findViewById(R.id.pwd);
                String pwd = inputBox2.getText().toString();
                inputBox2.setText("");

                dl.dismiss();

                gotCredentials(usr, pwd);
            }
        });
    }

    private void showPasswordDialog() {
        dl = new Dialog(this);
        dl.setTitle("Enter Password Please");

        dl.setContentView(R.layout.password_prompt);
        EditText inputBox1 = (EditText) dl.findViewById(R.id.pwd);
        inputBox1.setText("");

        Button bOk = (Button) dl.findViewById(R.id.ok);
        bOk.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                EditText inputBox2 = (EditText) dl.findViewById(R.id.pwd);
                String pwd = inputBox2.getText().toString();
                inputBox2.setText("");

                dl.dismiss();

                gotCredentials(null, pwd);
            }
        });
    }

    private int contentIdx = -1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onStart() {

        super.onStart();

        setContentView(R.layout.passphrase_view);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

}
