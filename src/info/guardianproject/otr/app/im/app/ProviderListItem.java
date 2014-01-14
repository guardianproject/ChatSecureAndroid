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

import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProviderListItem extends LinearLayout {
    private Activity mActivity;
    private SignInManager mSignInManager;
    private ContentResolver mResolver;
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

    private ImageView mBtnSettings;
    
    private int mProviderIdColumn;
    private int mActiveAccountIdColumn;
    private int mActiveAccountUserNameColumn;
    private int mAccountPresenceStatusColumn;
    private int mAccountConnectionStatusColumn;

    private long mAccountId;

    private boolean mShowLongName = false;
    private ImApp mApp = null;
    private AsyncTask<Void, Void, Void> mBindTask;
    
    private Handler mHandler = new Handler()
    {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            
            //update notifications from async task
        }
        
    };
    
    public ProviderListItem(Context context, Activity activity, SignInManager signInManager) {
        super(context);
        mActivity = activity;
        mSignInManager = signInManager;
        
        mApp = (ImApp)activity.getApplication();
        
        mResolver = mApp.getContentResolver();
        
    }

    public void init(Cursor c, boolean showLongName) {

        
        mShowLongName = showLongName;
        
        mProviderIdColumn = c.getColumnIndexOrThrow(Imps.Provider._ID);

        mSignInSwitch = (CompoundButton) findViewById(R.id.statusSwitch);
        mProviderName = (TextView) findViewById(R.id.providerName);
        mLoginName = (TextView) findViewById(R.id.loginName);
        mChatView = (TextView) findViewById(R.id.conversations);

        mBtnSettings = (ImageView)findViewById(R.id.btnSettings);
        
        mActiveAccountIdColumn = c.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_ID);
        mActiveAccountUserNameColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_USERNAME);
        mAccountPresenceStatusColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACCOUNT_PRESENCE_STATUS);
        mAccountConnectionStatusColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACCOUNT_CONNECTION_STATUS);

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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }
    
    public void bindView(Cursor cursor) {
        final Resources r = getResources();

        final int providerId = cursor.getInt(mProviderIdColumn);
     
        mAccountId = cursor.getLong(mActiveAccountIdColumn);
        setTag(mAccountId);

        if (!cursor.isNull(mActiveAccountIdColumn)) {
            
            final String activeUserName = cursor.getString(mActiveAccountUserNameColumn);
            
            final int connectionStatus = cursor.getInt(mAccountConnectionStatusColumn);
            final String presenceString = getPresenceString(cursor, getContext());
            if (mChatView != null)
                mChatView.setVisibility(View.GONE);
            runBindTask(r, providerId, activeUserName, connectionStatus, presenceString);
        } 
    }
    
    @Override
    protected void onDetachedFromWindow() {
        if (mBindTask != null)
            mBindTask.cancel(false);
        mBindTask = null;
        super.onDetachedFromWindow();
    }

    private void runBindTask(final Resources r, final int providerId, final String activeUserName,
            final int dbConnectionStatus, final String presenceString) {
        if (mBindTask != null)
            mBindTask.cancel(false);
        
        mBindTask = new AsyncTask<Void, Void, Void>() {
            private String mProviderNameText;
            private String mSecondRowText;
            private boolean mSwitchOn;

            @Override
            protected Void doInBackground(Void... params) {
                
                if (providerId != -1)
                {
                    try
                    {
                        Imps.ProviderSettings.QueryMap settings =
                                new Imps.ProviderSettings.QueryMap(mResolver,
                                        providerId, false, mHandler);
                        
                        String userDomain = settings.getDomain();
                        int connectionStatus = dbConnectionStatus;
                        
                        IImConnection conn = mApp.getConnection(providerId);
                        if (conn == null)
                        {
                            connectionStatus = ImConnection.DISCONNECTED;
                        }
                        else
                        {
                            try {
                                connectionStatus = conn.getState();
                            } catch (RemoteException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
        
                        if (mShowLongName)
                            mProviderNameText = activeUserName + '@' + userDomain;
                        else
                            mProviderNameText = activeUserName;
        
                        switch (connectionStatus) {
                        
                        case ImConnection.LOGGING_IN:
                        case ImConnection.SUSPENDING:
                        case ImConnection.SUSPENDED:
                            mSecondRowText = r.getString(R.string.signing_in_wait);
                            mSwitchOn = true;
                            break;
        
                        case ImConnection.LOGGED_IN:
                            mSwitchOn = true;
                            mSecondRowText = computeSecondRowText(presenceString, r, settings, true);
        
                            break;
        
                        case ImConnection.LOGGING_OUT:
                            mSwitchOn = false;
                            mSecondRowText = r.getString(R.string.signing_out_wait);
        
                            break;
                            
                        default:
        
                            mSwitchOn = false;
                            mSecondRowText = computeSecondRowText(presenceString, r, settings, false);
                            break;
                        }
        
                        settings.close();
                    }
                    catch (NullPointerException npe)
                    {
                        Log.d(ImApp.LOG_TAG,"null on QueryMap (this shouldn't happen anymore, but just in case)",npe);
                    }
                }
                
                return null;
            }
            
            @Override
            protected void onPostExecute(Void result) {
                
                if (mProviderNameText != null)
                    applyView(mProviderNameText, mSwitchOn, mSecondRowText);
            }
        };
        mBindTask.execute();
    }

    private void applyView(String providerNameText, boolean switchOn, String secondRowText) {
        mProviderName.setText(providerNameText);
        if (mSignInSwitch != null && (!mUserChanged))
        {
            mSignInSwitch.setOnCheckedChangeListener(null);
            mSignInSwitch.setChecked(switchOn);
            mSignInSwitch.setOnCheckedChangeListener(mCheckedChangeListner);
        }

        if (mLoginName != null)
            mLoginName.setText(secondRowText);
    }

    private String computeSecondRowText(String presenceString, Resources r,
            final Imps.ProviderSettings.QueryMap settings, boolean showPresence) {
        String secondRowText;
        StringBuffer secondRowTextBuffer = new StringBuffer();


        if (showPresence)
        {
            secondRowTextBuffer.append(presenceString);
            secondRowTextBuffer.append(" - ");
        }
            
        
        if (settings.getServer() != null && settings.getServer().length() > 0)
        {
            secondRowTextBuffer.append(settings.getServer());

        }
        else
        {
            secondRowTextBuffer.append(settings.getDomain());
        }


        if (settings.getPort() != 5222 && settings.getPort() != 0)
            secondRowTextBuffer.append(':').append(settings.getPort());


        if (settings.getUseTor())
        {
            secondRowTextBuffer.append(" - ");
            secondRowTextBuffer.append(r.getString(R.string._via_orbot));
        }

        secondRowText = secondRowTextBuffer.toString();
        return secondRowText;
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

    public interface SignInManager
    {
        public void signIn (long accountId);
        public void signOut (long accountId);
    }

    public void applyView( AccountAdapter.AccountSetting accountSetting ) {
        // provide name
        String providerNameText = accountSetting.activeUserName;
        if (mShowLongName)
            providerNameText += '@' + accountSetting.domain;
        mProviderName.setText(providerNameText);
        // switch
        boolean switchOn = false;
        String secondRowText;
        
        switch (accountSetting.connectionStatus) {
        
        case ImConnection.LOGGING_IN:
        case ImConnection.SUSPENDING:
        case ImConnection.SUSPENDED:
            switchOn = true;
            secondRowText = getResources().getString(R.string.signing_in_wait);
            break;

        case ImConnection.LOGGED_IN:
            switchOn = true;
            secondRowText = computeSecondRowText(accountSetting, true);
            break;

        default:
            switchOn = false;
            secondRowText = computeSecondRowText(accountSetting, false);
            break;
        }
        
        if (mSignInSwitch != null && (!mUserChanged))
        {
            mSignInSwitch.setOnCheckedChangeListener(null);
            mSignInSwitch.setChecked(switchOn);
            mSignInSwitch.setOnCheckedChangeListener(mCheckedChangeListner);
        }
        // login name
        if (mLoginName != null)
            mLoginName.setText(secondRowText);

    };
    
    private String getPresenceString( Context context, int presenceStatus) {

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
    
    private String computeSecondRowText( AccountAdapter.AccountSetting accountSetting, boolean showPresence ) {
        StringBuffer secondRowTextBuffer = new StringBuffer();

        if (showPresence)
        {
            secondRowTextBuffer.append( getPresenceString(mActivity, accountSetting.connectionStatus));
            secondRowTextBuffer.append(" - ");
        }
            
        if (accountSetting.host != null && accountSetting.host.length() > 0) {
            secondRowTextBuffer.append(accountSetting.host);
        } else {
            secondRowTextBuffer.append(accountSetting.domain);
        }

        if (accountSetting.port != 5222 && accountSetting.port != 0)
            secondRowTextBuffer.append(':').append(accountSetting.port);

        if (accountSetting.isTor) {
            secondRowTextBuffer.append(" - ");
            secondRowTextBuffer.append(mActivity.getString(R.string._via_orbot));
        }

        return secondRowTextBuffer.toString();
    }

}

