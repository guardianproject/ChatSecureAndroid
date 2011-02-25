/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.otr.app.im.app;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import info.guardianproject.otr.IOtrKeyManager;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;

import info.guardianproject.otr.app.im.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ContactPresenceActivity extends Activity {

	private String remoteFingerprint;
	private boolean remoteFingerprintVerified = false;
	private String remoteAddress;
	
	private String localFingerprint;
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setTheme(android.R.style.Theme_Dialog);
        setContentView(R.layout.contact_presence_activity);

        ImageView imgAvatar = (ImageView) findViewById(R.id.imgAvatar);
        TextView txtName = (TextView) findViewById(R.id.txtName);
        TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
        TextView txtCustomStatus = (TextView) findViewById(R.id.txtStatusText);
        TextView lblFingerprintRemote = (TextView) findViewById(R.id.labelFingerprintRemote);
        TextView txtFingerprintRemote = (TextView) findViewById(R.id.txtFingerprintRemote);
        TextView txtFingerprintLocal = (TextView) findViewById(R.id.txtFingerprintLocal);

        Intent i = getIntent();
        Uri uri = i.getData();
        if(uri == null) {
            warning("No data to show");
            finish();
            return;
        }

        if (i.getExtras() != null)
        {
	        remoteFingerprint = i.getExtras().getString("remoteFingerprint");
	        
	        if (remoteFingerprint != null)
	        {
	        	remoteFingerprintVerified = i.getExtras().getBoolean("remoteVerified");
	        	localFingerprint = i.getExtras().getString("localFingerprint");
	        
	        	txtFingerprintRemote.setText(remoteFingerprint);
	        	
	        	if (remoteFingerprintVerified)
	        	{
	        		lblFingerprintRemote.setText("Their Fingerprint (Verified)");
	        		txtFingerprintRemote.setBackgroundColor(Color.GREEN);
	        	}
	        	else
	        		txtFingerprintRemote.setBackgroundColor(Color.YELLOW);

	        	txtFingerprintRemote.setTextColor(Color.BLACK);
	        	
	        	txtFingerprintLocal.setText(localFingerprint);
	        }
        }
        
        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(uri, null, null, null, null);
        if(c == null) {
            warning("Database error when query " + uri);
            finish();
            return;
        }

        if(c.moveToFirst()) {
            long providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Contacts.PROVIDER));
            remoteAddress = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
            String nickname   = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.NICKNAME));
            int status    = c.getInt(c.getColumnIndexOrThrow(Imps.Contacts.PRESENCE_STATUS));
            int clientType = c.getInt(c.getColumnIndexOrThrow(Imps.Contacts.CLIENT_TYPE));
            String customStatus = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.PRESENCE_CUSTOM_STATUS));

            ImApp app = ImApp.getApplication(this);
            
            BrandingResources brandingRes = app.getBrandingResource(providerId);
            setTitle(brandingRes.getString(BrandingResourceIDs.STRING_CONTACT_INFO_TITLE));

            Drawable avatar = DatabaseUtils.getAvatarFromCursor(c,
                    c.getColumnIndexOrThrow(Imps.Contacts.AVATAR_DATA));
            if (avatar != null) {
                imgAvatar.setImageDrawable(avatar);
            } else {
                imgAvatar.setImageResource(R.drawable.avatar_unknown);
            }

            txtName.setText(ImpsAddressUtils.getDisplayableAddress(remoteAddress));

            String statusString = brandingRes.getString(
                    PresenceUtils.getStatusStringRes(status));
            SpannableString s = new SpannableString("+ " + statusString);
            Drawable statusIcon = brandingRes.getDrawable(
                    PresenceUtils.getStatusIconId(status));
            statusIcon.setBounds(0, 0, statusIcon.getIntrinsicWidth(),
                    statusIcon.getIntrinsicHeight());
            s.setSpan(new ImageSpan(statusIcon), 0, 1,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            txtStatus.setText(s);

       //     txtClientType.setText(getClientTypeString(clientType));

            if (!TextUtils.isEmpty(customStatus)) {
                txtCustomStatus.setVisibility(View.VISIBLE);
                txtCustomStatus.setText("\"" + customStatus + "\"");
            } else {
                txtCustomStatus.setVisibility(View.GONE);
            }
        }
        c.close();
    }

    private String getClientTypeString(int clientType) {
        Resources res = getResources();
        switch (clientType) {
            case Imps.Contacts.CLIENT_TYPE_MOBILE:
                return res.getString(R.string.client_type_mobile);

            default:
                return res.getString(R.string.client_type_computer);
        }
    }

    private static void warning(String msg) {
        Log.w(ImApp.LOG_TAG, "<ContactPresenceActivity> " + msg);
    }
    
    private void verifyRemoteFingerprint ()
    {
    	Toast.makeText(this, "The remote key fingerprint has been verified!", Toast.LENGTH_SHORT).show();
    	
        ImApp app = ImApp.getApplication(this);


    	IOtrKeyManager okm;
		try {
			okm = app.getActiveConnections().get(0).getChatSessionManager().getChatSession(remoteAddress).getOtrKeyManager();
	    	okm.verifyKey(remoteAddress);

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
                
    }
    
    public void startScan ()
    {
    	IntentIntegrator.initiateScan(this);
    	
    }
    
    public void displayQRCode (String text)
    {
    	IntentIntegrator.shareText(this, text);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	     
    	IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
	     
	     if (scanResult != null) {
	        
	    	 String otherFingerprint = scanResult.getContents();
	    	 
	    	 if (otherFingerprint != null && otherFingerprint.equals(remoteFingerprint))
	    	 {
	    		 verifyRemoteFingerprint();
	    	 }
	    	 
	    	
	      }
	     
    // else continue with any other code you need in the method
     }
    
    public boolean onCreateOptionsMenu(Menu menu) {
    	
    	super.onCreateOptionsMenu(menu);
        
        MenuItem mItem = null;
        
        
        mItem = menu.add(0, 1, Menu.NONE, "Scan Fingerprint");
        
        mItem = menu.add(0, 2, Menu.NONE, "Your Fingerprint");
        
        mItem = menu.add(0, 3, Menu.NONE, "Verify Fingerprint");
        
       
        return true;
    }
    
    /* When a menu item is selected launch the appropriate view or activity
     * (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		super.onMenuItemSelected(featureId, item);
		
		if (item.getItemId() == 1)
		{
			startScan();
		}
		else if (item.getItemId() == 2)
		{
			displayQRCode(localFingerprint);
		}
		else if (item.getItemId() == 3)
		{
			verifyRemoteFingerprint();
		}
		
		
        return true;
	}
}
