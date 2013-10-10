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
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.ImpsAddressUtils;
import info.guardianproject.util.LogCleaner;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ContactPresenceActivity extends Activity {

    private IChatSession mChatSession;
    private IOtrChatSession mOtrSession;
    private String remoteAddress;
    
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

        enableButtons (false);
        
        Intent i = getIntent();
        mUri = i.getData();
        if (mUri == null) {
            warning("No data to show");
            finish();
            return;
        }

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

        if (remoteAddress != null)
        {
            try {
                
                try {
                    mChatSession = mApp.getChatSession(providerId, remoteAddress);
                    
                    if (mChatSession != null)
                    {
                        mOtrSession = mChatSession.getOtrChatSession();
                    }
                    
                } catch (RemoteException e) {
                    Log.e(TAG, "error init otr", e);

                }
                
                
            } catch (Exception e) {
               Log.e(TAG,"error reading key data",e);
            }
        }
        
        
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        
        super.onConfigurationChanged(newConfig);
    }

    private void updateUI() {

        
        

        
        TextView txtName = (TextView) findViewById(R.id.txtName);
        TextView txtAddress = (TextView) findViewById(R.id.txtAddress);
        
        TextView txtStatus = (TextView) findViewById(R.id.txtStatus);

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
                
                getWindow().setBackgroundDrawable(avatar);
                
                findViewById(R.id.helpscrollview).setBackgroundColor(getResources().getColor(R.color.contact_status_avatar_overlay));
               
                
            }

            String address = ImpsAddressUtils.getDisplayableAddress(remoteAddress);
            
            if (nickname == null)
                nickname = address;
            
            if (nickname != null)
                txtName.setText(nickname);

            if (address != null)
                txtAddress.setText(address);
            
            String statusString = brandingRes.getString(PresenceUtils.getStatusStringRes(status));
            if (!TextUtils.isEmpty(customStatus)) {
                statusString = "\"" + customStatus + "\"";
            } 
            
            SpannableString s = new SpannableString("+ " + statusString);
            Drawable statusIcon = brandingRes.getDrawable(PresenceUtils.getStatusIconId(status));
            statusIcon.setBounds(0, 0, statusIcon.getIntrinsicWidth(),
                    statusIcon.getIntrinsicHeight());
            s.setSpan(new ImageSpan(statusIcon), 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            txtStatus.setText(s);

            
           
        }
        c.close();
        
        TextView lblFingerprintRemote = (TextView) findViewById(R.id.labelFingerprintRemote);
        TextView txtFingerprintRemote = (TextView) findViewById(R.id.txtFingerprintRemote);

        updateOtrStatus ();
        
        try
        {
            if (mOtrSession != null && mOtrSession.getRemoteFingerprint() != null) {
                
                txtFingerprintRemote.setVisibility(View.VISIBLE);
                lblFingerprintRemote.setVisibility(View.VISIBLE);
                
                String remoteFingerprint = mOtrSession.getRemoteFingerprint();
                boolean remoteFingerprintVerified = mOtrSession.isKeyVerified(remoteAddress);
               
                
                txtFingerprintRemote.setText(prettyPrintFingerprint(remoteFingerprint));
                
                if (remoteFingerprintVerified) {
                    lblFingerprintRemote.setText(R.string.their_fingerprint_verified_);
                    txtFingerprintRemote.setBackgroundColor(getResources().getColor(R.color.otr_green));
                } else
                    txtFingerprintRemote.setBackgroundColor(getResources().getColor(R.color.otr_yellow));
    

                enableButtons(true);
                
            } else {
                txtFingerprintRemote.setVisibility(View.GONE);
                lblFingerprintRemote.setVisibility(View.GONE);
                enableButtons(false);
            }
        }
        catch (RemoteException re)
        {
            txtFingerprintRemote.setVisibility(View.GONE);
            lblFingerprintRemote.setVisibility(View.GONE);
            enableButtons(false);
        }
        

    }
    
    private String prettyPrintFingerprint (String fingerprint)
    {
        StringBuffer spacedFingerprint = new StringBuffer();
        
        for (int i = 0; i + 8 <= fingerprint.length(); i+=8)
        {
            spacedFingerprint.append(fingerprint.subSequence(i,i+8));
            spacedFingerprint.append(' ');
        }
        
        return spacedFingerprint.toString();
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

        try
        {
            StringBuffer message = new StringBuffer();      
            message.append(getString(R.string.fingerprint_for_you)).append("\n").append(prettyPrintFingerprint(mOtrSession.getLocalFingerprint())).append("\n\n");
            message.append(getString(R.string.fingerprint_for_)).append(remoteAddress).append("\n").append(prettyPrintFingerprint(mOtrSession.getRemoteFingerprint())).append("\n\n");
            
            message.append(getString(R.string.are_you_sure_you_want_to_confirm_this_key_));
            
            new AlertDialog.Builder(this).setTitle(R.string.verify_key_).setMessage(message.toString())
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
        catch (RemoteException e)
        {
            LogCleaner.error(ImApp.LOG_TAG, "unable to perform manual key verification", e);
        }
    }

    private void verifyRemoteFingerprint() {


        try {
            IChatSession session = mApp.getChatSession(providerId, remoteAddress);
            
            if (session != null)
            {
                IOtrChatSession iOtrSession = session.getOtrChatSession();                    
                iOtrSession.verifyKey(remoteAddress);

            }
            
        } catch (RemoteException e) {
            Log.e(TAG, "error init otr", e);

        }
        
        
        updateUI();
            

    }

    public void startScan() {
        IntentIntegrator.initiateScan(this);

    }

    

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode,
                intent);

        if (scanResult != null && mOtrSession != null) {

            try
            {
                String otherFingerprint = scanResult.getContents();
    
                if (otherFingerprint != null && otherFingerprint.equalsIgnoreCase(mOtrSession.getRemoteFingerprint())) {
                    verifyRemoteFingerprint();
                }
            }
            catch (RemoteException re)
            {
                LogCleaner.error(ImApp.LOG_TAG, "error validating QR code response", re);
            }
        }
    }


    private void enableButtons (boolean showOtrOptions)
    {
        Button btnVerifyManual = (Button)findViewById(R.id.btnVerifyManual);
        Button btnVerifyScan = (Button)findViewById(R.id.btnVerifyScan);
        Button btnVerifyQuestion = (Button)findViewById(R.id.btnVerifyQuestion);
        View viewVerifyLabel = findViewById(R.id.labelFingerprintActions);
        
        if (!showOtrOptions)
        {
            btnVerifyManual.setVisibility(View.GONE);
            btnVerifyScan.setVisibility(View.GONE);
            btnVerifyQuestion.setVisibility(View.GONE);
            viewVerifyLabel.setVisibility(View.GONE);
            
        }
        else
        {
            btnVerifyManual.setVisibility(View.VISIBLE);
            btnVerifyScan.setVisibility(View.VISIBLE);
            btnVerifyQuestion.setVisibility(View.VISIBLE);
            viewVerifyLabel.setVisibility(View.VISIBLE);
            
            btnVerifyManual.setOnClickListener(new OnClickListener (){
    
                @Override
                public void onClick(View v) {
                  
                        confirmVerify();
                    
                }
                
                
            });
            
            
            btnVerifyScan.setOnClickListener(new OnClickListener (){
    
                @Override
                public void onClick(View v) {
                    startScan();
                }
                
                
            });
            
            btnVerifyQuestion.setOnClickListener(new OnClickListener (){
    
                @Override
                public void onClick(View v) {
                    if (mOtrSession != null)
                        initSmpUI();
                }
                
                
            });
        } 
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
        try {
            IChatSession session = mApp.getChatSession(providerId, remoteAddress);
            
            if (session != null)
            {
                IOtrChatSession iOtrSession = session.getOtrChatSession();
                iOtrSession.initSmpVerification(question, answer);
            }
            
        } catch (RemoteException e) {
            Log.e(TAG, "error init SMP", e);

        }
    }
}
