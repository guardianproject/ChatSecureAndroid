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

package org.gitian.android.im.app;

import org.gitian.android.im.R;
import org.gitian.android.im.plugin.BrandingResourceIDs;
import org.gitian.android.im.provider.Imps;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactPresenceActivity extends Activity {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setTheme(android.R.style.Theme_Dialog);
        setContentView(R.layout.contact_presence_activity);

        ImageView imgAvatar = (ImageView) findViewById(R.id.imgAvatar);
        TextView labelName = (TextView) findViewById(R.id.labelName);
        TextView txtName = (TextView) findViewById(R.id.txtName);
        TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
        TextView txtClientType = (TextView) findViewById(R.id.txtClientType);
        TextView txtCustomStatus = (TextView) findViewById(R.id.txtStatusText);

        Intent i = getIntent();
        Uri uri = i.getData();
        if(uri == null) {
            warning("No data to show");
            finish();
            return;
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
            String username = c.getString(c.getColumnIndexOrThrow(Imps.Contacts.USERNAME));
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

            labelName.setText(brandingRes.getString(
                    BrandingResourceIDs.STRING_LABEL_USERNAME));
            txtName.setText(ImpsAddressUtils.getDisplayableAddress(username));

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

            txtClientType.setText(getClientTypeString(clientType));

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
}
