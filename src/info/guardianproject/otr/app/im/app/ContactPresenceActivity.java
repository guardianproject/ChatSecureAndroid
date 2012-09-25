/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.IOtrChatSession;
import info.guardianproject.otr.IOtrKeyManager;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.ImpsAddressUtils;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ContactPresenceActivity extends SherlockActivity {

    private String remoteFingerprint;
    private boolean remoteFingerprintVerified = false;
    private String remoteAddress;

    private String localFingerprint;
    private long providerId;
    private ImApp mApp;

    private final static String TAG = "Gibberbot";

    public ContactPresenceActivity() {
        mApp = ImApp.getApplication(this);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.contact_presence_activity);

        //   ImageView imgAvatar = (ImageView) findViewById(R.id.imgAvatar);
        TextView txtName = (TextView) findViewById(R.id.txtName);
        TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
        TextView txtCustomStatus = (TextView) findViewById(R.id.txtStatusText);

        Intent i = getIntent();
        Uri uri = i.getData();
        if (uri == null) {
            warning("No data to show");
            finish();
            return;
        }

        if (i.getExtras() != null) {
            remoteFingerprint = i.getExtras().getString("remoteFingerprint");

            if (remoteFingerprint != null) {
                remoteFingerprintVerified = i.getExtras().getBoolean("remoteVerified");
                localFingerprint = i.getExtras().getString("localFingerprint");
            }

        }

        updateUI();

        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(uri, null, null, null, null);
        if (c == null) {
            warning("Database error when query " + uri);
            finish();
            return;
        }

        if (c.moveToFirst()) {
            providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts.PROVIDER));
            remoteAddress = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
//            String nickname = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.NICKNAME));
            int status = c.getInt(c.getColumnIndexOrThrow(Imps.Contacts.PRESENCE_STATUS));
//            int clientType = c.getInt(c.getColumnIndexOrThrow(Imps.Contacts.CLIENT_TYPE));
            String customStatus = c.getString(c
                    .getColumnIndexOrThrow(Imps.Contacts.PRESENCE_CUSTOM_STATUS));

            BrandingResources brandingRes = mApp.getBrandingResource(providerId);
            setTitle(brandingRes.getString(BrandingResourceIDs.STRING_CONTACT_INFO_TITLE));

//            Drawable avatar = DatabaseUtils.getAvatarFromCursor(c,
//                    c.getColumnIndexOrThrow(Imps.Contacts.AVATAR_DATA));

            txtName.setText(ImpsAddressUtils.getDisplayableAddress(remoteAddress));

            String statusString = brandingRes.getString(PresenceUtils.getStatusStringRes(status));
            SpannableString s = new SpannableString("+ " + statusString);
            Drawable statusIcon = brandingRes.getDrawable(PresenceUtils.getStatusIconId(status));
            statusIcon.setBounds(0, 0, statusIcon.getIntrinsicWidth(),
                    statusIcon.getIntrinsicHeight());
            s.setSpan(new ImageSpan(statusIcon), 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            txtStatus.setText(s);

            if (!TextUtils.isEmpty(customStatus)) {
                txtCustomStatus.setVisibility(View.VISIBLE);
                txtCustomStatus.setText("\"" + customStatus + "\"");
            } else {
                txtCustomStatus.setVisibility(View.GONE);
            }
        }
        c.close();
    }

    private void updateUI() {

        TextView lblFingerprintLocal = (TextView) findViewById(R.id.labelFingerprintLocal);
        TextView lblFingerprintRemote = (TextView) findViewById(R.id.labelFingerprintRemote);
        TextView txtFingerprintRemote = (TextView) findViewById(R.id.txtFingerprintRemote);
        TextView txtFingerprintLocal = (TextView) findViewById(R.id.txtFingerprintLocal);

        if (remoteFingerprint != null) {
            txtFingerprintRemote.setText(remoteFingerprint);

            if (remoteFingerprintVerified) {
                lblFingerprintRemote.setText("Their Fingerprint (Verified)");
                txtFingerprintRemote.setBackgroundColor(Color.GREEN);
            } else
                txtFingerprintRemote.setBackgroundColor(Color.YELLOW);

            txtFingerprintRemote.setTextColor(Color.BLACK);

            txtFingerprintLocal.setText(localFingerprint);
        } else {
            txtFingerprintRemote.setVisibility(View.GONE);
            txtFingerprintLocal.setVisibility(View.GONE);
            lblFingerprintRemote.setVisibility(View.GONE);
            lblFingerprintLocal.setVisibility(View.GONE);
        }

    }

//    private String getClientTypeString(int clientType) {
//        Resources res = getResources();
//        switch (clientType) {
//        case Imps.Contacts.CLIENT_TYPE_MOBILE:
//            return res.getString(R.string.client_type_mobile);
//
//        default:
//            return res.getString(R.string.client_type_computer);
//        }
//    }

    private static void warning(String msg) {
        Log.w(ImApp.LOG_TAG, "<ContactPresenceActivity> " + msg);
    }

    private void confirmVerify() {
        String message = "Are you sure you want to confirm this key?";

        new AlertDialog.Builder(this).setTitle("Verify key?").setMessage(message)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        verifyRemoteFingerprint();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
    }

    private void verifyRemoteFingerprint() {
        Toast.makeText(this, "The remote key fingerprint has been verified!", Toast.LENGTH_SHORT)
                .show();

        IOtrKeyManager okm;
        try {
            okm = mApp.getChatSession(providerId, remoteAddress).getOtrKeyManager();
            okm.verifyKey(remoteAddress);
            remoteFingerprintVerified = true;
            updateUI();

        } catch (RemoteException e) {
            Log.e(TAG, "error verifying remote fingerprint", e);
        }

        updateUI();

    }

    public void startScan() {
        IntentIntegrator.initiateScan(this);

    }

    public void displayQRCode(String text) {
        IntentIntegrator.shareText(this, text);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode,
                intent);

        if (scanResult != null) {

            String otherFingerprint = scanResult.getContents();

            if (otherFingerprint != null && otherFingerprint.equals(remoteFingerprint)) {
                verifyRemoteFingerprint();
            }

        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        if (remoteFingerprint != null) {
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.contact_info_menu, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_scan:
            startScan();
            return true;

        case R.id.menu_fingerprint:
            if (remoteFingerprint != null)
                displayQRCode(localFingerprint);
            return true;

        case R.id.menu_verify_fingerprint:
            if (remoteFingerprint != null)
                confirmVerify();
            return true;

        case R.id.menu_verify_secret:
            if (remoteFingerprint != null)
                initSmpUI();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initSmpUI() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View viewSmp = inflater.inflate(R.layout.smp_question_dialog, null, false);

        new AlertDialog.Builder(this).setTitle("OTR Q&A Verification").setView(viewSmp)
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        EditText eiQuestion = (EditText) viewSmp.findViewById(R.id.editSmpQuestion);
                        EditText eiAnswer = (EditText) viewSmp.findViewById(R.id.editSmpAnswer);
                        String question = eiQuestion.getText().toString();
                        String answer = eiAnswer.getText().toString();
                        initSmp(question, answer);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
    }

    private void initSmp(String question, String answer) {
        IOtrChatSession iOtrSession;
        try {
            iOtrSession = mApp.getChatSession(providerId, remoteAddress).getOtrChatSession();
            iOtrSession.initSmpVerification(question, answer);

        } catch (RemoteException e) {
            Log.e(TAG, "error init SMP", e);

        }
    }
}
