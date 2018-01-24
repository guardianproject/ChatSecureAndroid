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
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProviderListItem extends LinearLayout {
    private Activity mActivity;
    //private SignInManager mSignInManager;
    private ContentResolver mResolver;
  //  private CompoundButton mSignInSwitch;

    //private boolean mUserChanged = false;
    private boolean mIsSignedIn;

    private TextView mProviderName;
    private TextView mLoginName;

    private int mProviderIdColumn;
    private int mActiveAccountIdColumn;
    private int mActiveAccountUserNameColumn;
    private int mAccountPresenceStatusColumn;
    private int mAccountConnectionStatusColumn;

    private long mAccountId;

    private boolean mShowLongName = false;
    private ImApp mApp = null;

    private static Handler mHandler = new Handler()
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
        //mSignInManager = signInManager;

        mApp = (ImApp)activity.getApplication();

        mResolver = mApp.getContentResolver();

    }

    public void init(Cursor c, boolean showLongName) {


        mShowLongName = showLongName;

        mProviderIdColumn = c.getColumnIndexOrThrow(Imps.Provider._ID);

        //mSignInSwitch = (CompoundButton) findViewById(R.id.statusSwitch);
        mProviderName = (TextView) findViewById(R.id.providerName);
        mLoginName = (TextView) findViewById(R.id.loginName);

        mActiveAccountIdColumn = c.getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_ID);
        mActiveAccountUserNameColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACTIVE_ACCOUNT_USERNAME);
        mAccountPresenceStatusColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACCOUNT_PRESENCE_STATUS);
        mAccountConnectionStatusColumn = c
                .getColumnIndexOrThrow(Imps.Provider.ACCOUNT_CONNECTION_STATUS);

        setOnClickListener(new OnClickListener ()
        {

            @Override
            public void onClick(View v) {


                Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(
                        Imps.Account.CONTENT_URI, mAccountId));
                intent.addCategory(ImApp.IMPS_CATEGORY);

                intent.putExtra("isSignedIn", mIsSignedIn);

                mActivity.startActivity(intent);
            }

        });

        /*
        if (mSignInSwitch != null)
        {
            mProviderName.setOnClickListener(new OnClickListener ()
            {

                @Override
                public void onClick(View v) {


                    Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(
                            Imps.Account.CONTENT_URI, mAccountId));
                    intent.addCategory(ImApp.IMPS_CATEGORY);
                    mActivity.startActivity(intent);
                }

            });

            mLoginName.setOnClickListener(new OnClickListener ()
            {

                @Override
                public void onClick(View v) {


                    Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(
                            Imps.Account.CONTENT_URI, mAccountId));
                    intent.addCategory(ImApp.IMPS_CATEGORY);
                    mActivity.startActivity(intent);
                }

            });

            mSignInSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if (isChecked)
                        mSignInManager.signIn(mAccountId);
                    else
                        mSignInManager.signOut(mAccountId);

                    mUserChanged = true;
                }

            });


        }
      */

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

            mHandler.postDelayed(new Runnable () {
                public void run ()
                {
                    runBindTask(r, providerId, activeUserName, connectionStatus, presenceString);
                }
            }
                    , 200l);

        }
    }

    @Override
    protected void onDetachedFromWindow() {

        super.onDetachedFromWindow();
    }

    private void runBindTask(final Resources r, final int providerId, final String activeUserName,
            final int dbConnectionStatus, final String presenceString) {

            String mProviderNameText;
            String mSecondRowText;

            try
            {
                    Cursor pCursor = mResolver.query(Imps.ProviderSettings.CONTENT_URI,new String[] {Imps.ProviderSettings.NAME, Imps.ProviderSettings.VALUE},Imps.ProviderSettings.PROVIDER + "=?",new String[] { Long.toString( providerId)},null);

                    Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(pCursor, mResolver,
                            providerId,     false /* keep updated */, mHandler /* no handler */);

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
                        mSecondRowText = r.getString(R.string.signing_in_wait);
                        mIsSignedIn = true;

                        break;

                    case ImConnection.SUSPENDING:
                    case ImConnection.SUSPENDED:
                        mSecondRowText = r.getString(R.string.error_suspended_connection);
                        mIsSignedIn = true;

                        break;



                    case ImConnection.LOGGED_IN:
                        mIsSignedIn = true;
                        mSecondRowText = computeSecondRowText(presenceString, r, settings, true);

                        break;

                    case ImConnection.LOGGING_OUT:
                        mIsSignedIn = false;
                        mSecondRowText = r.getString(R.string.signing_out_wait);

                        break;

                    default:

                        mIsSignedIn = false;
                        mSecondRowText = computeSecondRowText(presenceString, r, settings, true);
                        break;
                    }

                    settings.close();
                    pCursor.close();

                    applyView(mProviderNameText, mIsSignedIn, mSecondRowText);
                }
                catch (NullPointerException npe)
                {
                    Log.d(ImApp.LOG_TAG,"null on QueryMap (this shouldn't happen anymore, but just in case)",npe);
                }




    }

    private void applyView(String providerNameText, boolean isSignedIn, String secondRowText) {

        if (isSignedIn)
        {
            setBackgroundColor(getResources().getColor(R.color.holo_blue_dark));
        }
        else
        {
            setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        if (mProviderName != null)
        {
            mProviderName.setText(providerNameText);

            if (isSignedIn)
                mProviderName.setTextColor(Color.WHITE);
            else
                mProviderName.setTextColor(Color.LTGRAY);


            if (mLoginName != null)
            {
                mLoginName.setText(secondRowText);

                if (isSignedIn)
                    mLoginName.setTextColor(Color.WHITE);
                else
                    mLoginName.setTextColor(Color.LTGRAY);


            }
        }

    }

    private String computeSecondRowText(String presenceString, Resources r,
            final Imps.ProviderSettings.QueryMap settings, boolean showPresence) {
        String secondRowText;
        StringBuffer secondRowTextBuffer = new StringBuffer();


        if (showPresence && presenceString.length() > 0)
        {
            secondRowTextBuffer.append(presenceString);
            secondRowTextBuffer.append(" - ");
        }


        if (settings.getServer() != null && settings.getServer().length() > 0)
        {

            secondRowTextBuffer.append(settings.getServer());

        }
        else if (settings.getDomain() != null & settings.getDomain().length() > 0)
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
            return "";
        }
    }

    public interface SignInManager
    {
        public void signIn (long accountId);
        public void signOut (long accountId);
    }




}

