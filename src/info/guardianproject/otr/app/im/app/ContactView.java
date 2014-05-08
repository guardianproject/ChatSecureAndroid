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

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.util.LruCache;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ContactView extends LinearLayout {
    static final String[] CONTACT_PROJECTION = { Imps.Contacts._ID, Imps.Contacts.PROVIDER,
                                                Imps.Contacts.ACCOUNT, Imps.Contacts.USERNAME,
                                                Imps.Contacts.NICKNAME, Imps.Contacts.TYPE,
                                                Imps.Contacts.SUBSCRIPTION_TYPE,
                                                Imps.Contacts.SUBSCRIPTION_STATUS,
                                                Imps.Presence.PRESENCE_STATUS,
                                                Imps.Presence.PRESENCE_CUSTOM_STATUS,
                                                Imps.Chats.LAST_MESSAGE_DATE,
                                                Imps.Chats.LAST_UNREAD_MESSAGE,
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
    static final int COLUMN_AVATAR_DATA = 12;

   
    public ContactView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
    }

    static class ViewHolder 
    {
        //ImageView mPresence;
        TextView mLine1;
        TextView mLine2;
        TextView mTimeStamp;
        ImageView mAvatar;
        View mStatusBlock;
        
    }

    public void bind(Cursor cursor, String underLineText, boolean scrolling) {
        bind(cursor, underLineText, true, scrolling);
    }

    public void bind(Cursor cursor, String underLineText, boolean showChatMsg, boolean scrolling) {
        
        ViewHolder holder = (ViewHolder)getTag();
        
        long providerId = cursor.getLong(COLUMN_CONTACT_PROVIDER);
        String address = cursor.getString(COLUMN_CONTACT_USERNAME);
        String displayName = cursor.getString(COLUMN_CONTACT_NICKNAME);
        int type = cursor.getInt(COLUMN_CONTACT_TYPE);
        String statusText = cursor.getString(COLUMN_CONTACT_CUSTOM_STATUS);
        String lastMsg = cursor.getString(COLUMN_LAST_MESSAGE);

        int presence = cursor.getInt(COLUMN_CONTACT_PRESENCE_STATUS);

        String nickname = displayName + " (" + address + ")";
        
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
        
        if (holder.mAvatar != null)
        {
            if (Imps.Contacts.TYPE_GROUP == type) {
                holder.mAvatar.setImageResource(R.drawable.group_chat);
                
            }
            else
            {
            
                Drawable avatar = DatabaseUtils.getAvatarFromCursor(cursor, COLUMN_AVATAR_DATA, ImApp.DEFAULT_AVATAR_WIDTH,ImApp.DEFAULT_AVATAR_HEIGHT);
                 
                if (avatar != null)
                    holder.mAvatar.setImageDrawable(avatar);
                else
                    holder.mAvatar.setImageDrawable(getContext().getResources().getDrawable(R.drawable.avatar_unknown));
            }
        }
        
        if (showChatMsg && lastMsg != null) {

            
            if (holder.mAvatar != null)
            {
                setBackgroundResource(R.color.holo_blue_bright);
                holder.mLine1.setBackgroundColor(getResources().getColor(R.color.holo_blue_bright));
                holder.mLine1.setTextColor(Color.WHITE);
            }
            else if (holder.mStatusBlock != null)
            {
                holder.mStatusBlock.setBackgroundColor(getResources().getColor(R.color.holo_blue_bright));
                
            }
           
            if (holder.mLine2 != null)
                holder.mLine2.setText(android.text.Html.fromHtml(lastMsg).toString());
                        
        }
        else 
        {
            if (holder.mLine2 != null)                
            {
                
                if (statusText == null || statusText.length() == 0)
                {

                    if (Imps.Contacts.TYPE_GROUP == type) 
                    {
                        statusText = getContext().getString(R.string.menu_new_group_chat);
                    }
                    else
                    {
                        ImApp app = ((ImApp)((Activity) getContext()).getApplication());
                        BrandingResources brandingRes = app.getBrandingResource(providerId);
                        statusText = brandingRes.getString(PresenceUtils.getStatusStringRes(presence));
                    }
                }
                
                holder.mLine2.setText(statusText);
                
            }
            
            
            if (presence == Imps.Presence.AVAILABLE)
            {
                if (holder.mAvatar != null)
                {
                    setBackgroundColor(getResources().getColor(R.color.holo_green_light));
                    holder.mLine1.setBackgroundColor(getResources().getColor(R.color.holo_green_dark));
                    holder.mLine1.setTextColor(getResources().getColor(R.color.contact_status_fg_light));
                }
                else if (holder.mStatusBlock != null)
                {
                    holder.mStatusBlock.setBackgroundColor(getResources().getColor(R.color.holo_green_light));
                    
                }
                
            }
            else if (presence == Imps.Presence.AWAY||presence == Imps.Presence.IDLE)
            {
                if (holder.mAvatar != null)
                {
                    setBackgroundColor(getResources().getColor(R.color.holo_orange_light));
                    holder.mLine1.setBackgroundColor(getResources().getColor(R.color.holo_orange_dark));
                    holder.mLine1.setTextColor(getResources().getColor(R.color.contact_status_fg_light));
                }
                else if (holder.mStatusBlock != null)
                {
                    holder.mStatusBlock.setBackgroundColor(getResources().getColor(R.color.holo_orange_light));
                    
                }
                
            }
            else if (presence == Imps.Presence.DO_NOT_DISTURB)
            {
                if (holder.mAvatar != null)
                {
                    setBackgroundColor(getResources().getColor(R.color.holo_red_light));
                    holder.mLine1.setBackgroundColor(getResources().getColor(R.color.holo_red_dark));
                    holder.mLine1.setTextColor(getResources().getColor(R.color.contact_status_fg_light));
                }
                else if (holder.mStatusBlock != null)
                {
                    holder.mStatusBlock.setBackgroundColor(getResources().getColor(R.color.holo_red_light));
                    
                }
            }   
            else
            {
                if (holder.mAvatar != null)
                {
                    setBackgroundColor(Color.LTGRAY);
                    holder.mLine1.setBackgroundColor(Color.LTGRAY);
                }
                else if (holder.mStatusBlock != null)
                {
                    holder.mStatusBlock.setBackgroundColor(Color.LTGRAY);
                    
                }
            }
            
            
        }
        
       
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
                if (!c.isLast()) {
                    buf.append(',');
                }
            }
        }
        c.close();
        
        return buf.toString();
    }*/
    

}
