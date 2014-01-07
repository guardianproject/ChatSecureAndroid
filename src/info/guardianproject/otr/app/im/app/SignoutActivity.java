/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
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

import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.util.LogCleaner;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class SignoutActivity extends ThemeableActivity {

    private String[] ACCOUNT_SELECTION = new String[] { Imps.Account._ID, Imps.Account.PROVIDER, };
    private ImApp mApp;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data == null) {
            Log.e(ImApp.LOG_TAG, "Need account data to sign in");
            //finish();
            return;
        }

        ContentResolver cr = getContentResolver();
        
        Cursor c = cr.query(data, ACCOUNT_SELECTION, null /* selection */,
                null /* selection args */, null /* sort order */);
        final long providerId;
        final long accountId;

        try {
            if (!c.moveToFirst()) {
                LogCleaner.warn(ImApp.LOG_TAG, "[SignoutActivity] No data for " + data);
             //   finish();
                return;
            }

            providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Account.PROVIDER));
            accountId = c.getLong(c.getColumnIndexOrThrow(Imps.Account._ID));
        } finally {
            c.close();
        }


        mApp = (ImApp)getApplication();
        mApp.callWhenServiceConnected(mHandler, new Runnable() {
            public void run() {
                signOut(providerId, accountId);
            }
        });
    }

    private void signOut(long providerId, long accountId) {
        try {

            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                conn.logout();
            } else {
                // Normally, we can always get the connection when user chose to
                // sign out. However, if the application crash unexpectedly, the
                // status will never be updated. Clear the status in this case
                // to make it recoverable from the crash.
                ContentValues values = new ContentValues(2);
                values.put(Imps.AccountStatus.PRESENCE_STATUS, Imps.Presence.OFFLINE);
                values.put(Imps.AccountStatus.CONNECTION_STATUS, Imps.ConnectionStatus.OFFLINE);
                String where = Imps.AccountStatus.ACCOUNT + "=?";
                getContentResolver().update(Imps.AccountStatus.CONTENT_URI, values, where,
                        new String[] { Long.toString(accountId) });
            }
        } catch (RemoteException ex) {
            Log.e(ImApp.LOG_TAG, "signout: caught ", ex);
        } finally {
            //finish();

         //   Toast.makeText(this, getString(R.string.signed_out_prompt), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // always call finish here, because we don't want to be in the backlist ever, and
        // we don't handle onRestart()

    }

    static void log(String msg) {
        Log.d(ImApp.LOG_TAG, "[Signout] " + msg);
    }
}
