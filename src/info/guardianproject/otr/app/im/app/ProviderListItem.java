/*
 * Copyright (C) 2009 Myriad Group AG Copyright (C) 2009 The Android Open Source
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

import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.util.FontUtils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProviderListItem extends LinearLayout {
    private static final String TAG = "IM";
    private static final boolean LOCAL_DEBUG = false;

    private Activity mActivity;
    //private ImageView mProviderIcon;
    private ImageView mStatusIcon;
    private TextView mProviderName;
    private TextView mLoginName;
    private TextView mChatView;
    private View mUnderBubble;
    private Drawable mBubbleDrawable;
    private Drawable mDefaultBackground;

    private int mProviderIdColumn;
    private int mProviderFullnameColumn;
    private int mActiveAccountIdColumn;
    private int mActiveAccountUserNameColumn;
    private int mAccountPresenceStatusColumn;
    private int mAccountConnectionStatusColumn;

    private ColorStateList mProviderNameColors;
    private ColorStateList mLoginNameColors;
    private ColorStateList mChatViewColors;
    
    private long mAccountId;

    public ProviderListItem(Context context, Activity activity) {
        super(context);
        mActivity = activity;
    }

    public void init(Cursor c) {


        mProviderIdColumn = c.getColumnIndexOrThrow(Imps.Provider._ID);

        //mProviderIcon = (ImageView) findViewById(R.id.providerIcon);
        mStatusIcon = (ImageView) findViewById(R.id.statusIcon);
        mProviderName = (TextView) findViewById(R.id.providerName);
        mLoginName = (TextView) findViewById(R.id.loginName);
        mChatView = (TextView) findViewById(R.id.conversations);
        mUnderBubble = findViewById(R.id.underBubble);
        mBubbleDrawable = getResources().getDrawable(R.drawable.bubble);
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mProviderFullnameColumn = c.getColumnIndexOrThrow(Imps.Provider.FULLNAME);
        mActiveAccountIdColumn = c.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_ID);
        mActiveAccountUserNameColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_USERNAME);
        mAccountPresenceStatusColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACCOUNT_PRESENCE_STATUS);
        mAccountConnectionStatusColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACCOUNT_CONNECTION_STATUS);

        mProviderNameColors = mProviderName.getTextColors();
        mLoginNameColors = mLoginName.getTextColors();
        mChatViewColors = mChatView.getTextColors();
    }

    public void bindView(Cursor cursor) {
        Resources r = getResources();
       // ImageView providerIcon = mProviderIcon;
        ImageView statusIcon = mStatusIcon;
        TextView providerName = mProviderName;
        TextView loginName = mLoginName;
        TextView chatView = mChatView;

        int providerId = cursor.getInt(mProviderIdColumn);
        String providerDisplayName = cursor.getString(mProviderFullnameColumn);

        final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(getContext().getContentResolver(),
                providerId, false , null);
      
        String userDomain = settings.getDomain();
        
        
        mAccountId = cursor.getLong(mActiveAccountIdColumn);
        

        ImApp app = (ImApp)mActivity.getApplication();
        BrandingResources brandingRes = app.getBrandingResource(providerId);
        //providerIcon.setImageDrawable(brandingRes.getDrawable(BrandingResourceIDs.DRAWABLE_LOGO));

        mUnderBubble.setBackgroundDrawable(mDefaultBackground);
        statusIcon.setVisibility(View.GONE);

        providerName.setTextColor(mProviderNameColors);
       loginName.setTextColor(mLoginNameColors);
       chatView.setTextColor(mChatViewColors);

        if (!cursor.isNull(mActiveAccountIdColumn)) {
            mLoginName.setVisibility(View.VISIBLE);
            providerName.setVisibility(View.VISIBLE);
            
            mProviderName.setEnabled(false);
            mLoginName.setEnabled(false);
            
            String activeUserName = cursor.getString(mActiveAccountUserNameColumn);
            
            providerName.setText(activeUserName + '@' + userDomain);

            long accountId = cursor.getLong(mActiveAccountIdColumn);
            int connectionStatus = cursor.getInt(mAccountConnectionStatusColumn);

            StringBuffer secondRowText = new StringBuffer();

            chatView.setVisibility(View.GONE);

            switch (connectionStatus) {
            case Imps.ConnectionStatus.CONNECTING:
                secondRowText.append(r.getString(R.string.signing_in_wait));

                mProviderName.setEnabled(true);
                mLoginName.setEnabled(true);
                break;

            case Imps.ConnectionStatus.ONLINE:

                mProviderName.setEnabled(true);
                mLoginName.setEnabled(true);
                
                int presenceIconId = getPresenceIconId(cursor);
                statusIcon.setImageDrawable(brandingRes.getDrawable(presenceIconId));
                statusIcon.setVisibility(View.VISIBLE);
                ContentResolver cr = mActivity.getContentResolver();

                int count = getConversationCount(cr, accountId);

                secondRowText.append(getPresenceString(cursor, getContext()));

                if (count > 0) {
                    

                    secondRowText.append(" - ");
                    secondRowText.append(count);
                    secondRowText.append(r.getString(R.string._open_conversations));
                }
                else
                {
                    secondRowText.append(" - ");
                    
                    if (settings.getServer() != null && settings.getServer().length() > 0)
                    {
                        secondRowText.append(settings.getServer());
                            
                    }
                    else
                    {
                        secondRowText.append(settings.getDomain());
                    }
                 
                    
                    if (settings.getPort() != 5222 && settings.getPort() != 0)
                        secondRowText.append(':').append(settings.getPort());
                    
                    
                    if (settings.getUseTor())
                    {
                        secondRowText.append(" - ");
                        secondRowText.append(r.getString(R.string._via_orbot));
                    }
                    
                    
                }
         //       chatView.setVisibility(View.GONE);
                
                break;

            default:
                

                secondRowText.append(providerDisplayName);

                secondRowText.append(" - ");
                
                if (settings.getServer() != null && settings.getServer().length() > 0)
                {
                    secondRowText.append(settings.getServer());
                       
                }
                else
                {
                    secondRowText.append(settings.getDomain());
                }
                
                
                if (settings.getPort() != 5222 && settings.getPort() != 0)
                    secondRowText.append(':').append(settings.getPort());
                
               
                
                if (settings.getUseTor())
                {
                    secondRowText.append(" - ");
                    secondRowText.append(r.getString(R.string._via_orbot));
                }
                
                break;
            }

            loginName.setText(secondRowText);
            
            FontUtils.setRobotoFont(mActivity.getApplicationContext(), loginName);
            FontUtils.setRobotoFont(mActivity.getApplicationContext(), mChatView);
            FontUtils.setRobotoFont(mActivity.getApplicationContext(), mProviderName);


        } else {
            // No active account, show add account
            mLoginName.setVisibility(View.GONE);
            mChatView.setVisibility(View.GONE);
            mProviderName.setText(providerDisplayName);
        }
        
        settings.close();
    }
    
    public Long getAccountID ()
    {
        return mAccountId;
    }

    private int getConversationCount(ContentResolver cr, long accountId) {
        
        // TODO: this is code used to get Google Talk's chat count. Not sure if this will work
        // for IMPS chat count.
        
        StringBuilder where = new StringBuilder();
        where.append(Imps.Chats.CONTACT_ID);
        where.append(" in (select _id from contacts where ");
        where.append(Imps.Contacts.ACCOUNT);
        where.append("=");
        where.append(accountId);
        where.append(")");        

        Cursor cursor = cr.query(Imps.Chats.CONTENT_URI, null, where.toString(), null, null);

        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }
    
    private String getPresenceString(Cursor cursor, Context context) {
        int presenceStatus = cursor.getInt(mAccountPresenceStatusColumn);

        switch (presenceStatus) {


        case Imps.Presence.AVAILABLE:
            return context.getString(R.string.presence_available);

        case Imps.Presence.IDLE:
            return context.getString(R.string.presence_idle);
            
        case Imps.Presence.AWAY:
            return context.getString(R.string.presence_away);

        case Imps.Presence.DO_NOT_DISTURB:

            return context.getString(R.string.presence_busy);

        case Imps.Presence.INVISIBLE:
            return context.getString(R.string.presence_invisible);

        default:
            return context.getString(R.string.presence_offline);
        }
    }

    private int getPresenceIconId(Cursor cursor) {
        int presenceStatus = cursor.getInt(mAccountPresenceStatusColumn);

        if (LOCAL_DEBUG)
            log("getPresenceIconId: presenceStatus=" + presenceStatus);

        switch (presenceStatus) {
        case Imps.Presence.AVAILABLE:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_ONLINE;

        case Imps.Presence.IDLE:
        case Imps.Presence.AWAY:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_AWAY;

        case Imps.Presence.DO_NOT_DISTURB:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_BUSY;

        case Imps.Presence.INVISIBLE:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_INVISIBLE;

        default:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_OFFLINE;
        }
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }
}
