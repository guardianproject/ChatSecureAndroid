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
import info.guardianproject.otr.OtrAndroidKeyManagerImpl;
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.ImpsAddressUtils;

import java.io.IOException;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ContactPresenceActivity extends ThemeableActivity {

    private String remoteFingerprint;
    private boolean remoteFingerprintVerified = false;
    private String remoteAddress;

    private String localFingerprint;
    private long providerId;
    private ImApp mApp;

    private final static String TAG = ImApp.LOG_TAG;

    public ContactPresenceActivity() {
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mApp = (ImApp)getApplication();

        setContentView(R.layout.contact_presence_activity);

        ImageView imgAvatar = (ImageView) findViewById(R.id.imgAvatar);
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
            String nickname = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.NICKNAME));
            int status = c.getInt(c.getColumnIndexOrThrow(Imps.Contacts.PRESENCE_STATUS));
//            int clientType = c.getInt(c.getColumnIndexOrThrow(Imps.Contacts.CLIENT_TYPE));
            String customStatus = c.getString(c
                    .getColumnIndexOrThrow(Imps.Contacts.PRESENCE_CUSTOM_STATUS));
            
            

            BrandingResources brandingRes = mApp.getBrandingResource(providerId);
            setTitle(brandingRes.getString(BrandingResourceIDs.STRING_CONTACT_INFO_TITLE));

            Drawable avatar = DatabaseUtils.getAvatarFromCursor(c,
                    c.getColumnIndexOrThrow(Imps.Contacts.AVATAR_DATA),ImApp.DEFAULT_AVATAR_WIDTH*2,ImApp.DEFAULT_AVATAR_HEIGHT*2);
            
            imgAvatar.setImageDrawable(avatar);

            String address = ImpsAddressUtils.getDisplayableAddress(remoteAddress);
            String displayName = nickname;
            
            if (!nickname.equals(address))
                displayName = nickname + "\n" + address;
             
            txtName.setText(displayName);

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
            
            updateOtrStatus();
        }
        c.close();
        

        updateUI();
    }

    private void updateOtrStatus ()
    {

        IImConnection conn = ((ImApp)getApplication()).getConnection(providerId);
        
        
        
        try {
            
            IOtrKeyManager keyManager = conn.getChatSessionManager().getChatSession(remoteAddress).getOtrKeyManager();

                remoteFingerprint = keyManager.getRemoteFingerprint();

                if (remoteFingerprint != null) {
                    remoteFingerprint = remoteFingerprint.toUpperCase(Locale.ENGLISH);

                   remoteFingerprintVerified = keyManager.isKeyVerified(remoteAddress);
                            
                    
                }
                
               localFingerprint = keyManager.getLocalFingerprint();
                
                if (localFingerprint != null)
                    localFingerprint = localFingerprint.toUpperCase(Locale.ENGLISH);

            
            
        } catch (Exception e) {
           Log.e(TAG,"error reading key data",e);
        }
        
        
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        
        super.onConfigurationChanged(newConfig);
    }

    private void updateUI() {

        TextView lblFingerprintLocal = (TextView) findViewById(R.id.labelFingerprintLocal);
        TextView lblFingerprintRemote = (TextView) findViewById(R.id.labelFingerprintRemote);
        TextView txtFingerprintRemote = (TextView) findViewById(R.id.txtFingerprintRemote);
        TextView txtFingerprintLocal = (TextView) findViewById(R.id.txtFingerprintLocal);

        updateOtrStatus ();
        
        if (remoteFingerprint != null) {
            
            
            txtFingerprintRemote.setText(remoteFingerprint);
            

            if (remoteFingerprintVerified) {
                lblFingerprintRemote.setText(R.string.their_fingerprint_verified_);
                txtFingerprintRemote.setBackgroundColor(getResources().getColor(R.color.otr_green));
            } else
                txtFingerprintRemote.setBackgroundColor(getResources().getColor(R.color.otr_yellow));

            txtFingerprintRemote.setTextColor(Color.BLACK);

            if (localFingerprint != null)
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
        String message = getString(R.string.are_you_sure_you_want_to_confirm_this_key_);

        new AlertDialog.Builder(this).setTitle(R.string.verify_key_).setMessage(message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        verifyRemoteFingerprint();
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
    }

    private void verifyRemoteFingerprint() {

        try {

            IImConnection conn = ((ImApp)getApplication()).getConnection(providerId);

            IOtrKeyManager keyManager = conn.getChatSessionManager().getChatSession(remoteAddress).getOtrKeyManager();

            keyManager.verifyKey(remoteAddress);

            updateUI();
            
        } catch (RemoteException e) {
            Log.e(TAG, "error verifying remote fingerprint", e);
        }

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

            if (otherFingerprint != null && otherFingerprint.equalsIgnoreCase(remoteFingerprint)) {
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

        if (viewSmp != null)
        {
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
