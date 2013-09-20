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
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.ImpsAddressUtils;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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

    private Timer timer;

    int DELAY_INTERVAL = 500;
    int UPDATE_INTERVAL = 1000;
    
    Uri mUri = null;
    
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);   
        
        timer = new Timer();

        mApp = (ImApp)getApplication();

        setContentView(R.layout.contact_presence_activity);

        Intent i = getIntent();
        mUri = i.getData();
        if (mUri == null) {
            warning("No data to show");
            finish();
            return;
        }

        remoteAddress = i.getStringExtra("jid");
        


        updateUI();
        
        timer.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        mHandlerUI.sendEmptyMessage(0);
                    }
                },
                DELAY_INTERVAL,
                UPDATE_INTERVAL
        );

    }
    
    Handler mHandlerUI = new Handler ()
    {

        @Override
        public void handleMessage(Message msg) {
            updateUI();
        }
        
    };

    @Override
    protected void onDestroy() { 
        super.onDestroy();
        
        timer.cancel();
    }

    private void updateOtrStatus ()
    {

        
        try {
            
            IOtrKeyManager otrKeyMgr = ((ImApp)getApplication()).getRemoteImService().getOtrKeyManager();
            
            remoteFingerprint = otrKeyMgr.getRemoteFingerprint(remoteAddress);
            remoteFingerprintVerified = otrKeyMgr.isVerifiedUser(remoteAddress);
            
            if (remoteFingerprint == null)
            {
                String[] rfs = otrKeyMgr.getRemoteFingerprints(remoteAddress);
                
                if (rfs != null && rfs.length > 0)
                {
                    remoteFingerprint = rfs[0];
                }
            }
            
        } catch (Exception e) {
           Log.e(TAG,"error reading key data",e);
        }
        
        
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        
        super.onConfigurationChanged(newConfig);
    }

    private void updateUI() {

        
        


        ImageView imgAvatar = (ImageView) findViewById(R.id.imgAvatar);
        TextView txtName = (TextView) findViewById(R.id.txtName);
        TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
        TextView txtCustomStatus = (TextView) findViewById(R.id.txtStatusText);

        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(mUri, null, null, null, null);
        if (c == null) {
            warning("Database error when query " + mUri);
            finish();
            return;
        }

        if (c.moveToFirst()) {
            
            providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts.PROVIDER));
            
            if (remoteAddress == null)
                remoteAddress = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            
            String nickname = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.NICKNAME));
            int status = c.getInt(c.getColumnIndexOrThrow(Imps.Contacts.PRESENCE_STATUS));
//            int clientType = c.getInt(c.getColumnIndexOrThrow(Imps.Contacts.CLIENT_TYPE));
            String customStatus = c.getString(c
                    .getColumnIndexOrThrow(Imps.Contacts.PRESENCE_CUSTOM_STATUS));
            
            BrandingResources brandingRes = mApp.getBrandingResource(providerId);
            setTitle(brandingRes.getString(BrandingResourceIDs.STRING_CONTACT_INFO_TITLE));

            Drawable avatar = DatabaseUtils.getAvatarFromCursor(c,
                    c.getColumnIndexOrThrow(Imps.Contacts.AVATAR_DATA),ImApp.DEFAULT_AVATAR_WIDTH*4,ImApp.DEFAULT_AVATAR_HEIGHT*4);
            
            if (avatar != null)
            {                
                imgAvatar.setVisibility(View.GONE);
                
                getWindow().setBackgroundDrawable(avatar);
                
                findViewById(R.id.helpscrollview).setBackgroundColor(getResources().getColor(R.color.contact_status_avatar_overlay));
               
                
            }
            else
            {
                imgAvatar.setVisibility(View.GONE);
            }

            String address = ImpsAddressUtils.getDisplayableAddress(remoteAddress);
            
            if (nickname == null)
                nickname = address;
            else if (!nickname.equals(address))
                nickname += "\n" + address;
             
            if (nickname != null)
                txtName.setText(nickname);

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
        
        TextView lblFingerprintRemote = (TextView) findViewById(R.id.labelFingerprintRemote);
        TextView txtFingerprintRemote = (TextView) findViewById(R.id.txtFingerprintRemote);

        updateOtrStatus ();
        
        if (remoteFingerprint != null) {
            
            txtFingerprintRemote.setVisibility(View.VISIBLE);
            lblFingerprintRemote.setVisibility(View.VISIBLE);
            
            txtFingerprintRemote.setText(remoteFingerprint);
            
            if (remoteFingerprintVerified) {
                lblFingerprintRemote.setText(R.string.their_fingerprint_verified_);
                txtFingerprintRemote.setBackgroundColor(getResources().getColor(R.color.otr_green));
            } else
                txtFingerprintRemote.setBackgroundColor(getResources().getColor(R.color.otr_yellow));

            txtFingerprintRemote.setTextColor(Color.BLACK);

            
        } else {
            txtFingerprintRemote.setVisibility(View.GONE);
            lblFingerprintRemote.setVisibility(View.GONE);
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

            IOtrChatSession otrChatSession = conn.getChatSessionManager().getChatSession(remoteAddress).getOtrChatSession();
            otrChatSession.verifyKey(remoteAddress);

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
