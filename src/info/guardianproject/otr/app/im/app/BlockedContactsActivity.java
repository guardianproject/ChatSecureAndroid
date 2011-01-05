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

import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IImConnection;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

public class BlockedContactsActivity extends ListActivity {
    ImApp mApp;
    SimpleAlertHandler mHandler;

    private static final String[] PROJECTION = {
        Imps.BlockedList._ID,
        Imps.BlockedList.ACCOUNT,
        Imps.BlockedList.PROVIDER,
        Imps.BlockedList.NICKNAME,
        Imps.BlockedList.USERNAME,
        Imps.BlockedList.AVATAR_DATA,
    };

    static final int ACCOUNT_COLUMN  = 1;
    static final int PROVIDER_COLUMN = 2;
    static final int NICKNAME_COLUMN = 3;
    static final int USERNAME_COLUMN = 4;
    static final int AVATAR_COLUMN = 5;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.blocked_contacts_activity);
        mHandler = new SimpleAlertHandler(this);

        mApp = ImApp.getApplication(this);
        mApp.startImServiceIfNeed();
        if (!resolveIntent()) {
            finish();
            return;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Cursor c = (Cursor) l.getAdapter().getItem(position);
        if (c == null) {
            mHandler.showAlert(R.string.error, R.string.select_contact);
            return;
        }
        long providerId = c.getLong(PROVIDER_COLUMN);
        String username = c.getString(USERNAME_COLUMN);
        String nickname = c.getString(NICKNAME_COLUMN);
        mApp.callWhenServiceConnected(mHandler, new UnblockAction(providerId, username, nickname));
    }

    private boolean resolveIntent() {
        Intent i = getIntent();
        Uri uri = i.getData();
        if (uri == null) {
            warning("No data to show");
            return false;
        }

        long accountId = ContentUris.parseId(uri);
        Uri accountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);
        Cursor accountCursor = getContentResolver().query(accountUri, null, null, null, null);
        if (accountCursor == null) {
            warning("Bad account");
            return false;
        }
        if (!accountCursor.moveToFirst()) {
            warning("Bad account");
            accountCursor.close();
            return false;
        }

        long providerId = accountCursor.getLong(
                accountCursor.getColumnIndexOrThrow(Imps.Account.PROVIDER));
        String username = accountCursor.getString(
                accountCursor.getColumnIndexOrThrow(Imps.Account.USERNAME));

        BrandingResources brandingRes = mApp.getBrandingResource(providerId);
        getWindow().setFeatureDrawable(Window.FEATURE_LEFT_ICON,
                brandingRes.getDrawable(BrandingResourceIDs.DRAWABLE_LOGO));

        setTitle(getResources().getString(R.string.blocked_list_title, username));
        accountCursor.close();

        Cursor c = managedQuery(uri, PROJECTION, null, null, Imps.BlockedList.DEFAULT_SORT_ORDER);
        if (c == null) {
            warning("Database error when query " + uri);
            return false;
        }

        ListAdapter adapter = new BlockedContactsAdapter(c, this);
        setListAdapter(adapter);
        return true;
    }

    private static void warning(String msg) {
        Log.w(ImApp.LOG_TAG, "<BlockContactsActivity> " + msg);
    }

    private static class BlockedContactsAdapter extends ResourceCursorAdapter {
        public BlockedContactsAdapter(Cursor c, Context context) {
            super(context, R.layout.blocked_contact_view, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            if (view instanceof BlockedContactView) {
                ((BlockedContactView) view).bind(cursor, context);
            }
        }
    }

    private class UnblockAction implements Runnable {
        private long mProviderId;
        String mUserName;
        private String mNickName;

        public UnblockAction(long providerId, String userName, String nickName) {
            mProviderId = providerId;
            mUserName = userName;
            mNickName = nickName;
        }

        public void run() {
            final IImConnection conn = mApp.getConnection(mProviderId);
            if (conn == null) {
                mHandler.showAlert(R.string.error, R.string.disconnected);
                return;
            }
            DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int whichButton) {
                    try {
                        IContactListManager manager = conn.getContactListManager();
                        manager.unBlockContact(mUserName);
                    } catch (RemoteException e) {
                        mHandler.showServiceErrorAlert();
                    }
                }
            };
            Resources r = getResources();

            new AlertDialog.Builder(BlockedContactsActivity.this)
                .setTitle(R.string.confirm)
                .setMessage(r.getString(R.string.confirm_unblock_contact, mNickName))
                .setPositiveButton(R.string.yes, confirmListener) // default button
                .setNegativeButton(R.string.no, null)
                .setCancelable(false)
                .show();
        }

    }
}
