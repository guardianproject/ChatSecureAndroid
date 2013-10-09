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

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProviderListItem extends LinearLayout {
    private static final String TAG = "IM";
    private static final boolean LOCAL_DEBUG = false;

    private Activity mActivity;
    private SignInManager mSignInManager;
    
    private CompoundButton mSignInSwitch;
    private OnCheckedChangeListener mCheckedChangeListner = new OnCheckedChangeListener(){

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
           
            if (isChecked)
                mSignInManager.signIn(mAccountId);
            else
                mSignInManager.signOut(mAccountId);
            
            mUserChanged = true;
        }
        
    };
    private boolean mUserChanged = false;
    
    private TextView mProviderName;
    private TextView mLoginName;
    private TextView mChatView;
    private View mUnderBubble;
 //   private Drawable mBubbleDrawable;
  //  private Drawable mDefaultBackground;

    private ImageView mBtnSettings;
    
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

    private boolean mShowLongName = false;
    
    public ProviderListItem(Context context, Activity activity, SignInManager signInManager) {
        super(context);
        mActivity = activity;
        mSignInManager = signInManager;
    }

    public void init(Cursor c, boolean showLongName) {

        
        mShowLongName = showLongName;
        
        mProviderIdColumn = c.getColumnIndexOrThrow(Imps.Provider._ID);

        //mProviderIcon = (ImageView) findViewById(R.id.providerIcon);
   //     mStatusIcon = (ImageView) findViewById(R.id.statusIcon);
        mSignInSwitch = (CompoundButton) findViewById(R.id.statusSwitch);
        mProviderName = (TextView) findViewById(R.id.providerName);
        mLoginName = (TextView) findViewById(R.id.loginName);
        mChatView = (TextView) findViewById(R.id.conversations);
        mUnderBubble = findViewById(R.id.underBubble);
     //   mBubbleDrawable = getResources().getDrawable(R.drawable.bubble);
    //    mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mBtnSettings = (ImageView)findViewById(R.id.btnSettings);
        
        mProviderFullnameColumn = c.getColumnIndexOrThrow(Imps.Provider.FULLNAME);
        mActiveAccountIdColumn = c.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_ID);
        mActiveAccountUserNameColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_USERNAME);
        mAccountPresenceStatusColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACCOUNT_PRESENCE_STATUS);
        mAccountConnectionStatusColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACCOUNT_CONNECTION_STATUS);

        mProviderNameColors = mProviderName.getTextColors();
        
        if (mLoginName != null)
            mLoginNameColors = mLoginName.getTextColors();
        
        if (mChatView != null)
            mChatViewColors = mChatView.getTextColors();
        
        if (mSignInSwitch != null)
        {
            mProviderName.setOnClickListener(new OnClickListener ()
            {

                @Override
                public void onClick(View v) {
                   

                    Intent intent = new Intent(getContext(), NewChatActivity.class);
                    intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
                    getContext().startActivity(intent);
                }
                
            });
            
            mLoginName.setOnClickListener(new OnClickListener ()
            {

                @Override
                public void onClick(View v) {
                   

                    Intent intent = new Intent(getContext(), NewChatActivity.class);
                    intent.putExtra(ImServiceConstants.EXTRA_INTENT_ACCOUNT_ID, mAccountId);
                    getContext().startActivity(intent);
                }
                
            });
            
            mSignInSwitch.setOnCheckedChangeListener(mCheckedChangeListner);
         
            
            if (mBtnSettings != null)
            {
                mBtnSettings.setOnClickListener(new OnClickListener()
                {

                    @Override
                    public void onClick(View v) {
                        
                        Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(
                                Imps.Account.CONTENT_URI, mAccountId));
                        intent.addCategory(ImApp.IMPS_CATEGORY);
                        mActivity.startActivity(intent);
                    }
                    
                });
            }
        }
      
/* 
        mStatusSwitch.setOnClickListener(new OnClickListener (){

            @Override
            public void onClick(View v) {
               
                if (mStatusSwitch.isChecked())
                    mSignInManager.signIn(mAccountId);
                else
                    mSignInManager.signOut(mAccountId);
                
            }
            
        });*/
    }

    public void bindView(Cursor cursor) {
        Resources r = getResources();
       // ImageView providerIcon = mProviderIcon;

        int providerId = cursor.getInt(mProviderIdColumn);
     
        final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(getContext().getContentResolver(),
                providerId, false , null);
      
        String userDomain = settings.getDomain();
        
        mAccountId = cursor.getLong(mActiveAccountIdColumn);
        setTag(mAccountId);

        //if (mUnderBubble != null)
         //   mUnderBubble.setBackgroundDrawable(mDefaultBackground);

        /*
        mProviderName.setTextColor(mProviderNameColors);
        
        if (mLoginNameColors != null)
       mLoginName.setTextColor(mLoginNameColors);
        
        if (mChatViewColors != null)
       mChatView.setTextColor(mChatViewColors);
       */

        if (!cursor.isNull(mActiveAccountIdColumn)) {
            
            String activeUserName = cursor.getString(mActiveAccountUserNameColumn);
            
            if (mShowLongName)
                mProviderName.setText(activeUserName + '@' + userDomain);
            else
                mProviderName.setText(activeUserName);

            
            int connectionStatus = cursor.getInt(mAccountConnectionStatusColumn);

            StringBuffer secondRowText = new StringBuffer();

            mChatView.setVisibility(View.GONE);

            switch (connectionStatus) {
            
            case Imps.ConnectionStatus.CONNECTING:
                secondRowText.append(r.getString(R.string.signing_in_wait));

                if (mSignInSwitch != null && (!mUserChanged))
                {
                    mSignInSwitch.setOnCheckedChangeListener(null);
                    mSignInSwitch.setChecked(true);
                    mSignInSwitch.setOnCheckedChangeListener(mCheckedChangeListner);
                }
                
                break;

            case Imps.ConnectionStatus.ONLINE:
            
                if (mSignInSwitch != null && (!mUserChanged))
                {
                    mSignInSwitch.setOnCheckedChangeListener(null);
                    mSignInSwitch.setChecked(true);
                    mSignInSwitch.setOnCheckedChangeListener(mCheckedChangeListner);
                }
                
             
                secondRowText.append(getPresenceString(cursor, getContext()));

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

            default:
                

                if (mSignInSwitch != null && (!mUserChanged))
                {
                    mSignInSwitch.setOnCheckedChangeListener(null);
                    mSignInSwitch.setChecked(false);
                    mSignInSwitch.setOnCheckedChangeListener(mCheckedChangeListner);
                }
                
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

            mLoginName.setText(secondRowText);

        } 
        
        settings.close();
    }
    
    public Long getAccountID ()
    {
        return mAccountId;
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
    
    public interface SignInManager
    {
        public void signIn (long accountId);
        public void signOut (long accountId);
    };

}

