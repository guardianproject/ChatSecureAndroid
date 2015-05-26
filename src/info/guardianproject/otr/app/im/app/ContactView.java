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
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.engine.Presence;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.ui.LetterAvatar;
import info.guardianproject.otr.app.im.ui.RoundedAvatarDrawable;
import info.guardianproject.util.SystemServices;
import info.guardianproject.util.SystemServices.FileInfo;
import net.java.otr4j.session.SessionStatus;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactView extends FrameLayout {
    static final String[] CONTACT_PROJECTION = { Imps.Contacts._ID, Imps.Contacts.PROVIDER,
                                                Imps.Contacts.ACCOUNT, Imps.Contacts.USERNAME,
                                                Imps.Contacts.NICKNAME, Imps.Contacts.TYPE,
                                                Imps.Contacts.SUBSCRIPTION_TYPE,
                                                Imps.Contacts.SUBSCRIPTION_STATUS,
                                                Imps.Presence.PRESENCE_STATUS,
                                                Imps.Presence.PRESENCE_CUSTOM_STATUS,
                                                Imps.Chats.LAST_MESSAGE_DATE,
                                                Imps.Chats.LAST_UNREAD_MESSAGE,
                                                Imps.Contacts.AVATAR_HASH,
                                                Imps.Contacts.AVATAR_DATA

    };


    static final int COLUMN_CONTACT_ID = 0;
    static final int COLUMN_CONTACT_PROVIDER = 1;
    static final int COLUMN_CONTACT_ACCOUNT = 2;
    static final int COLUMN_CONTACT_USERNAME = 3;
    static final int COLUMN_CONTACT_NICKNAME = 4;
    static final int COLUMN_CONTACT_TYPE = 5;
    static final int COLUMN_SUBSCRIPTION_TYPE = 6;
    static final int COLUMN_SUBSCRIPTION_STATUS = 7;
    static final int COLUMN_CONTACT_PRESENCE_STATUS = 8;
    static final int COLUMN_CONTACT_CUSTOM_STATUS = 9;
    static final int COLUMN_LAST_MESSAGE_DATE = 10;
    static final int COLUMN_LAST_MESSAGE = 11;
    static final int COLUMN_AVATAR_HASH = 12;
    static final int COLUMN_AVATAR_DATA = 13;
    
    

    private ImApp app = null;
    static Drawable AVATAR_DEFAULT_GROUP = null;

    public ContactView(Context context, AttributeSet attrs) {
        super(context, attrs);

        app = ((ImApp)((Activity) getContext()).getApplication());


    }

    static class ViewHolder
    {

        TextView mLine1;
        TextView mLine2;
        ImageView mAvatar;
        ImageView mStatusIcon;
        ImageView mEncryptionIcon;
        View mContainer;
        ImageView mMediaThumb;
    }

    public void bind(Cursor cursor, String underLineText, boolean scrolling) {
        bind(cursor, underLineText, true, scrolling);
    }

    public void bind(Cursor cursor, String underLineText, boolean showChatMsg, boolean scrolling) {

        /*
        if (Debug.DEBUG_ENABLED)
        {
            StringBuffer debug = new StringBuffer();
            for (int i = 0; i < cursor.getColumnCount();i++)
            {
                String name = cursor.getColumnName(i);
                String value = cursor.getString(i);
                if (value != null && value.length() < 100)
                    debug.append(name+":" + value+",");
                else if (value == null)
                    debug.append(name+":(null)");
            }

           Log.d(ImApp.LOG_TAG,"contact:" + debug.toString());

        }*/
        
        ViewHolder holder = (ViewHolder)getTag();
        
        final long providerId = cursor.getLong(COLUMN_CONTACT_PROVIDER);
        final String address = cursor.getString(COLUMN_CONTACT_USERNAME);

        final String displayName = cursor.getString(COLUMN_CONTACT_NICKNAME);

        final int type = cursor.getInt(COLUMN_CONTACT_TYPE);
        final String lastMsg = cursor.getString(COLUMN_LAST_MESSAGE);

        final int presence = cursor.getInt(COLUMN_CONTACT_PRESENCE_STATUS);

        final int subType = cursor.getInt(COLUMN_SUBSCRIPTION_TYPE);
        final int subStatus = cursor.getInt(COLUMN_SUBSCRIPTION_STATUS);

        String statusText = cursor.getString(COLUMN_CONTACT_CUSTOM_STATUS);

        String nickname = displayName;

        if (nickname == null)
        {
            nickname = address.split("@")[0];
        }
        else if (nickname.indexOf('@')!=-1)
        {
            nickname = nickname.split("@")[0];
        }



        if (!TextUtils.isEmpty(underLineText)) {
            // highlight/underline the word being searched 
            String lowercase = nickname.toLowerCase();
            int start = lowercase.indexOf(underLineText.toLowerCase());
            if (start >= 0) {
                int end = start + underLineText.length();
                SpannableString str = new SpannableString(nickname);
                str.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                holder.mLine1.setText(str);

            }
            else
                holder.mLine1.setText(nickname);

        }
        else
            holder.mLine1.setText(nickname);

        /*
        if (holder.mStatusIcon != null)
        {
            Drawable statusIcon = brandingRes.getDrawable(PresenceUtils.getStatusIconId(presence));
            //statusIcon.setBounds(0, 0, statusIcon.getIntrinsicWidth(),
              //      statusIcon.getIntrinsicHeight());
            holder.mStatusIcon.setImageDrawable(statusIcon);address
        }*/


        holder.mStatusIcon.setVisibility(View.GONE);

        if (holder.mAvatar != null)
        {
            if (Imps.Contacts.TYPE_GROUP == type) {

                holder.mAvatar.setVisibility(View.VISIBLE);

                if (AVATAR_DEFAULT_GROUP == null)
                    AVATAR_DEFAULT_GROUP = new RoundedAvatarDrawable(BitmapFactory.decodeResource(getResources(),
                            R.drawable.group_chat));


                    holder.mAvatar.setImageDrawable(AVATAR_DEFAULT_GROUP);


            }
            else if (cursor.getColumnIndex(Imps.Contacts.AVATAR_DATA)!=-1)
            {
//                holder.mAvatar.setVisibility(View.GONE);

                RoundedAvatarDrawable avatar = null;

                try
                {
                  //  avatar = DatabaseUtils.getAvatarFromAddress(this.getContext().getContentResolver(),address, ImApp.DEFAULT_AVATAR_WIDTH,ImApp.DEFAULT_AVATAR_HEIGHT);
                   avatar = DatabaseUtils.getAvatarFromCursor(cursor, COLUMN_AVATAR_DATA, ImApp.DEFAULT_AVATAR_WIDTH,ImApp.DEFAULT_AVATAR_HEIGHT);
                }
                catch (Exception e)
                {
                    //problem decoding avatar
                    Log.e(ImApp.LOG_TAG,"error decoding avatar",e);
                }

                try
                {
                    if (avatar != null)
                    {
                        setAvatarBorder(presence,avatar);
                        holder.mAvatar.setImageDrawable(avatar);
                    }
                    else
                    {
                        String letterString = null;
                                
                        if (nickname.length() > 0)
                            letterString = nickname.substring(0,1).toUpperCase();
                        else
                            letterString = "?"; //the unknown name!
                         
                        int color = getAvatarBorder(presence);
                        int padding = 24;
                        LetterAvatar lavatar = new LetterAvatar(getContext(), color, letterString, padding);
                        
                        holder.mAvatar.setImageDrawable(lavatar);

                    }

                    holder.mAvatar.setVisibility(View.VISIBLE);
                }
                catch (OutOfMemoryError ome)
                {
                    //this seems to happen now and then even on tiny images; let's catch it and just not set an avatar
                }

            }
            else
            {
                //holder.mAvatar.setImageDrawable(getContext().getResources().getDrawable(R.drawable.avatar_unknown));
                holder.mAvatar.setVisibility(View.GONE);



            }
        }

        if (showChatMsg && lastMsg != null) {


            if (holder.mLine2 != null)
            {
                if (ChatFileStore.isVfsUri(lastMsg))
                {
                    FileInfo fInfo = SystemServices.getFileInfoFromURI(getContext(), Uri.parse(lastMsg));
                    
                    if (fInfo.type == null || fInfo.type.startsWith("image"))
                    {
                        
                        if (holder.mMediaThumb != null)
                        {
                            holder.mMediaThumb.setVisibility(View.VISIBLE);
                        
                            Bitmap b = MessageView.getThumbnail(getContext().getContentResolver(), Uri.parse(lastMsg));
                            holder.mMediaThumb.setImageBitmap(b);
                            
                            holder.mLine2.setVisibility(View.GONE);
                                    
                        }
                    }
                    else
                    {
                        holder.mLine2.setText("");
                    }

                }
                else
                {
                    if (holder.mMediaThumb != null)
                        holder.mMediaThumb.setVisibility(View.GONE);
                    
                    holder.mLine2.setVisibility(View.VISIBLE);
                    holder.mLine2.setText(android.text.Html.fromHtml(lastMsg).toString());
                }
            }

        }
        else if (holder.mLine2 != null)
        {

            /*
            if (statusText == null || statusText.length() == 0)
            {
                if (Imps.Contacts.TYPE_GROUP == type)
                {
                    statusText = getContext().getString(R.string.menu_new_group_chat);
                }
                else
                {
                    statusText = address;//brandingRes.getString(PresenceUtils.getStatusStringRes(presence));
                }
            }

            holder.mLine2.setText(statusText);
            */

            statusText = address;
            holder.mLine2.setText(statusText);
        }




        if (subType == Imps.ContactsColumns.SUBSCRIPTION_TYPE_INVITATIONS)
        {
        //    if (holder.mLine2 != null)
          //      holder.mLine2.setText("Contact List Request");
        }

        holder.mLine1.setVisibility(View.VISIBLE);

        getEncryptionState (providerId, address, holder);
    }

    private void getEncryptionState (long providerId, String address, ViewHolder holder)
    {

         try {
             IImConnection conn = app.getConnection(providerId);
             if (conn == null || conn.getChatSessionManager() == null)
                 return;

            IChatSession chatSession = conn.getChatSessionManager().getChatSession(address);

            if (chatSession != null)
            {
                IOtrChatSession otrChatSession = chatSession.getOtrChatSession();
                if (otrChatSession != null)
                {
                    SessionStatus chatStatus = SessionStatus.values()[otrChatSession.getChatStatus()];

                    if (chatStatus == SessionStatus.ENCRYPTED)
                    {
                        boolean isVerified = otrChatSession.isKeyVerified(address);

                        if (isVerified)
                            holder.mStatusIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_black_encrypted_and_verified));
                        else
                            holder.mStatusIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_black_encrypted_not_verified));

                        holder.mStatusIcon.setVisibility(View.VISIBLE);
                    }
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



                //mCurrentChatSession.getOtrChatSession();

    }

    public void setAvatarBorder(int status, RoundedAvatarDrawable avatar) {
        switch (status) {
        case Presence.AVAILABLE:
            avatar.setBorderColor(getResources().getColor(R.color.holo_green_light));
            avatar.setAlpha(255);
            break;

        case Presence.IDLE:
            avatar.setBorderColor(getResources().getColor(R.color.holo_green_dark));
            avatar.setAlpha(255);

            break;

        case Presence.AWAY:
            avatar.setBorderColor(getResources().getColor(R.color.holo_orange_light));
            avatar.setAlpha(255);
            break;

        case Presence.DO_NOT_DISTURB:
            avatar.setBorderColor(getResources().getColor(R.color.holo_red_dark));
            avatar.setAlpha(255);

            break;

        case Presence.OFFLINE:
            avatar.setBorderColor(getResources().getColor(android.R.color.transparent));
            avatar.setAlpha(100);
            break;


        default:
        }
    }
    
    public int getAvatarBorder(int status) {
        switch (status) {
        case Presence.AVAILABLE:
            return (getResources().getColor(R.color.holo_green_light));

        case Presence.IDLE:
            return (getResources().getColor(R.color.holo_green_dark));
        case Presence.AWAY:
            return (getResources().getColor(R.color.holo_orange_light));

        case Presence.DO_NOT_DISTURB:
            return(getResources().getColor(R.color.holo_red_dark));

        case Presence.OFFLINE:
            return(getResources().getColor(R.color.holo_grey_dark));

        default:
        }

        return Color.TRANSPARENT;
    }

    
    /*
    private String queryGroupMembers(ContentResolver resolver, long groupId) {
        String[] projection = { Imps.GroupMembers.NICKNAME };
        Uri uri = ContentUris.withAppendedId(Imps.GroupMembers.CONTENT_URI, groupId);
        Cursor c = resolver.query(uri, projection, null, null, null);
        StringBuilder buf = new StringBuilder();
        if (c != null) {
            while (c.moveToNext()) {
                buf.append(c.getString(0));
                                                Imps.Avatars.DATA
                if (!c.isLast()) {
                    buf.append(',');
                }
            }
        }
        c.close();

        return buf.toString();
    }*/


}
