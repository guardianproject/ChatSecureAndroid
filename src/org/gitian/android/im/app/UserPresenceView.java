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

import java.util.ArrayList;
import java.util.List;

import org.gitian.android.im.R;
import org.gitian.android.im.IImConnection;
import org.gitian.android.im.engine.ImErrorInfo;
import org.gitian.android.im.engine.Presence;
import org.gitian.android.im.plugin.ImpsConfigNames;
import org.gitian.android.im.provider.Imps;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UserPresenceView extends LinearLayout {

    private ImageButton mStatusDialogButton;

    // views of the popup window
    TextView mStatusBar;

    private final SimpleAlertHandler mHandler;

    private IImConnection mConn;
    private long mProviderId;
    Presence mPresence;
    Context mContext;

    private String mLastStatusText;
    final List<StatusItem> mStatusItems = new ArrayList<StatusItem>();

    public UserPresenceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mHandler = new SimpleAlertHandler((Activity)context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mStatusDialogButton = (ImageButton)findViewById(R.id.statusDropDownButton);
        mStatusDialogButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showStatusListDialog();
            }
        });
    }

    private void showStatusListDialog() {
        if (mConn == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setAdapter(getStatusAdapter(),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        StatusItem item = mStatusItems.get(which);
                        int oldStatus = mPresence.getStatus();
                        if (item.getStatus() != oldStatus) {
                            updatePresence(item.getStatus(), item.getText().toString());
                        }
                    }
                });
        builder.show();
    }

    private StatusIconAdapter getStatusAdapter() {
        try {
            mStatusItems.clear();
            int[] supportedStatus = mConn.getSupportedPresenceStatus();
            for (int i = 0; i < supportedStatus.length; i++) {
                int s = PresenceUtils.convertStatus(supportedStatus[i]);
                if (s == Imps.Presence.OFFLINE) {
                    s = Imps.Presence.INVISIBLE;
                }
                ImApp app = ImApp.getApplication((Activity)mContext);
                BrandingResources brandingRes = app.getBrandingResource(mProviderId);
                Drawable icon = brandingRes.getDrawable(PresenceUtils.getStatusIconId(s));
                String text = brandingRes.getString(PresenceUtils.getStatusStringRes(s));
                mStatusItems.add(new StatusItem(supportedStatus[i], icon, text));
            }
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }

        return new StatusIconAdapter(mContext, mStatusItems);
    }

    void updateStatusText() {
        String newStatusText = mStatusBar.getText().toString();
        if (TextUtils.isEmpty(newStatusText)) {
            newStatusText = "";
        }
        if (!newStatusText.equals(mLastStatusText)) {
            updatePresence(-1, newStatusText);
        }
    }

    public void setConnection(IImConnection conn) {
        mConn = conn;
        try {
            mPresence = conn.getUserPresence();
            mProviderId = conn.getProviderId();
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }
        if (mPresence == null) {
            mPresence = new Presence();
        }
        updateView();
    }

    private void updateView() {
        ImApp app = ImApp.getApplication((Activity)mContext);
        BrandingResources brandingRes = app.getBrandingResource(mProviderId);
        int status = PresenceUtils.convertStatus(mPresence.getStatus());
        mStatusDialogButton.setImageDrawable(brandingRes.getDrawable(
                PresenceUtils.getStatusIconId(status)));

        String statusText = mPresence.getStatusText();
        if (TextUtils.isEmpty(statusText)) {
            statusText = brandingRes.getString(PresenceUtils.getStatusStringRes(status));
        }
        mLastStatusText = statusText;

        if (mStatusBar == null) {
            mStatusBar = initStatusBar(mProviderId);
        }
        mStatusBar.setText(statusText);

        // Disable the user to edit the custom status text because
        // the AIM and MSN server don't support it now.
        ProviderDef provider = app.getProvider(mProviderId);
        String providerName = provider == null ? null : provider.mName;
        if (Imps.ProviderNames.AIM.equals(providerName)
                || Imps.ProviderNames.MSN.equals(providerName)) {
            mStatusBar.setFocusable(false);
        }
    }

    private TextView initStatusBar(long providerId) {
        String value = Imps.ProviderSettings.getStringValue(
                            mContext.getContentResolver(), providerId,
                            ImpsConfigNames.SUPPORT_USER_DEFINED_PRESENCE);

        if ("true".equalsIgnoreCase(value)) {
            EditText statusEdit = (EditText) findViewById(R.id.statusEdit);
            statusEdit.setVisibility(View.VISIBLE);
            statusEdit.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (KeyEvent.ACTION_DOWN == event.getAction()) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_DPAD_CENTER:
                            case KeyEvent.KEYCODE_ENTER:
                                updateStatusText();
                                return true;
                        }
                    }
                    return false;
                }
            });

            statusEdit.setOnFocusChangeListener(new View.OnFocusChangeListener(){
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        updateStatusText();
                    }
                }
            });

            return statusEdit;
        } else {
            TextView statusView = (TextView) findViewById(R.id.statusView);
            statusView.setVisibility(View.VISIBLE);
            return statusView;
        }
    }

    void updatePresence(int status, String statusText) {
        if (mPresence == null) {
            // We haven't get the connection yet. Don't allow to update presence now.
            return;
        }

        Presence newPresence = new Presence(mPresence);

        if (status != -1) {
            newPresence.setStatus(status);
        }
        newPresence.setStatusText(statusText);

        try {
            int res = mConn.updateUserPresence(newPresence);
            if (res != ImErrorInfo.NO_ERROR) {
                mHandler.showAlert(R.string.error,
                        ErrorResUtils.getErrorRes(getResources(), res));
            } else {
                mPresence = newPresence;
                updateView();
            }
        } catch (RemoteException e) {
            mHandler.showServiceErrorAlert();
        }
    }

    private static class StatusItem implements ImageListAdapter.ImageListItem {
        private final int mStatus;
        private final Drawable mIcon;
        private final String   mText;

        public StatusItem(int status, Drawable icon, String text) {
            mStatus = status;
            mIcon = icon;
            mText = text;
        }

        public Drawable getDrawable() {
            return mIcon;
        }

        public CharSequence getText() {
            return mText;
        }

        public int getStatus() {
            return mStatus;
        }
    }

    private static class StatusIconAdapter extends ImageListAdapter {
        public StatusIconAdapter(Context context, List<StatusItem> data) {
            super(context, data);
        }

        @Override
        public long getItemId(int position) {
            StatusItem item = (StatusItem)getItem(position);
            return item.getStatus();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            return view;
        }
    }
}
