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

import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.engine.Address;
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
import android.os.RemoteException;
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
    
    public static final String[] CONTACT_PROJECTION = { Imps.Contacts._ID, Imps.Contacts.PROVIDER,
                                                Imps.Contacts.ACCOUNT, Imps.Contacts.USERNAME,
                                                Imps.Contacts.NICKNAME, Imps.Contacts.TYPE,
                                                Imps.Contacts.SUBSCRIPTION_TYPE,
                                                Imps.Contacts.SUBSCRIPTION_STATUS,
                                                Imps.Presence.PRESENCE_STATUS,
                                                Imps.Presence.PRESENCE_CUSTOM_STATUS,
                                                Imps.Chats.LAST_MESSAGE_DATE,
                                                Imps.Chats.LAST_UNREAD_MESSAGE                                                
                                                
                                                
    };

    public static final String[] CONTACT_PROJECTION_AVATAR = { Imps.Contacts._ID, Imps.Contacts.PROVIDER,
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

   
    private Drawable mAvatarUnknown;
   
    private Context mContext; 
    
    private static final int cacheSize = 300; // 4MiB
    private static LruCache<String, Drawable> bitmapCache = new LruCache<String, Drawable>(cacheSize);
    
    private IContactListManager mContactListManager;
    
    public ContactView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        mContext = context;       
        mAvatarUnknown = context.getResources().getDrawable(R.drawable.avatar_unknown);
        
    }

    private ViewHolder mHolder = null;
    
    class ViewHolder 
    {
        //ImageView mPresence;
        TextView mLine1;
       // TextView mLine2;
       // TextView mTimeStamp;
        ImageView mAvatar;
        
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHolder = (ViewHolder)getTag();
        
        if (mHolder == null)
        {
            mHolder = new ViewHolder();
            
            //mPresence = (ImageView) findViewById(R.id.presence);
            mHolder.mLine1 = (TextView) findViewById(R.id.contactStatus);
         //   mHolder.mLine2 = (TextView) findViewById(R.id.line2);
           
          //  mHolder.mTimeStamp = (TextView) findViewById(R.id.timestamp);
            mHolder.mAvatar = (ImageView)findViewById(R.id.contactAvatar);
            
            setTag(mHolder);
        }
    }

    public void bind(Cursor cursor, String underLineText, boolean scrolling) {
        bind(cursor, underLineText, true, scrolling, null);
    }
        
    public void bind(Cursor cursor, String underLineText, boolean scrolling, IContactListManager contactListManager) {
        bind(cursor, underLineText, true, scrolling, contactListManager);
    }

    public void bind(Cursor cursor, String underLineText, boolean showChatMsg, boolean scrolling, IContactListManager contactListManager) {
        
        mHolder = (ViewHolder)getTag();
        mContactListManager = contactListManager;
        
        Resources r = getResources();
        long providerId = cursor.getLong(COLUMN_CONTACT_PROVIDER);
        
      //  mHolder.mLine2.setCompoundDrawablePadding(5);
        
        String address = cursor.getString(COLUMN_CONTACT_USERNAME);
        String nickname = cursor.getString(COLUMN_CONTACT_NICKNAME);
        int type = cursor.getInt(COLUMN_CONTACT_TYPE);
        String statusText = cursor.getString(COLUMN_CONTACT_CUSTOM_STATUS);
        String lastMsg = cursor.getString(COLUMN_LAST_MESSAGE);

        boolean hasChat = !cursor.isNull(COLUMN_LAST_MESSAGE);

        ImApp app = (ImApp)((Activity)mContext).getApplication();
                
        BrandingResources brandingRes = app.getBrandingResource(providerId);

        int presence = cursor.getInt(COLUMN_CONTACT_PRESENCE_STATUS);
        
        
        
        //mPresence.setImageDrawable(brandingRes.getDrawable(iconId));
       // Drawable presenceIcon = brandingRes.getDrawable(iconId);

        // line1
        CharSequence contact;
        /*
        if (Imps.Contacts.TYPE_GROUP == type) {
            ContentResolver resolver = getContext().getContentResolver();
            long id = cursor.getLong(ContactView.COLUMN_CONTACT_ID);
            contact = queryGroupMembers(resolver, id);
        } else {
        */

            //contact = TextUtils.isEmpty(nickname) ? ImpsAddressUtils.getDisplayableAddress(username)
            // String address = ImpsAddressUtils.getDisplayableAddress(username);
             contact = nickname;
             
             if (address.indexOf('/')!=-1)
             {
                 contact = nickname + " (" + address.substring(address.indexOf('/')+1) + ")";
                 
             }
             
            if (!TextUtils.isEmpty(underLineText)) {
                // highlight/underline the word being searched
                String lowercase = contact.toString().toLowerCase();
                int start = lowercase.indexOf(underLineText.toLowerCase());
                if (start >= 0) {
                    int end = start + underLineText.length();
                    SpannableString str = new SpannableString(contact);
                    str.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    contact = str;
                }
            }
            
            if ( mHolder.mAvatar != null)
            {
                if (Imps.Contacts.TYPE_GROUP == type) {
                    mHolder.mAvatar.setImageResource(R.drawable.group_chat);
                    
                }
                else 
                {
                    Drawable avatar = (Drawable)bitmapCache.get(Address.stripResource(address));
                    
                    if (avatar == null)
                    {
                        
                        mHolder.mAvatar.setImageDrawable(mAvatarUnknown);
                        avatar = loadAvatar (address, cursor);
                        
                        if (avatar != null)
                            mHolder.mAvatar.setImageDrawable(avatar);
                    }
                    else
                    {   
                        mHolder.mAvatar.setImageDrawable(avatar);
                        
                    }
                    
                    
                   
                }

            }
            
            mHolder.mLine1.setText(contact);

        // time stamp
         
            /*
        if (showChatMsg && hasChat) {
            mHolder.mTimeStamp.setVisibility(VISIBLE);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(cursor.getLong(COLUMN_LAST_MESSAGE_DATE));
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT);
            mHolder. mTimeStamp.setText(formatter.format(cal.getTime()));
        }
    */
            
        // line2
        String status = null;
        if (showChatMsg && lastMsg != null && lastMsg.length() > 0) {

            //remove HTML tags since we can't display HTML
            status = lastMsg.replaceAll("\\<.*?\\>", "");                                                          
            //setBackgroundResource(R.color.holo_blue_bright);
          
            mHolder.mLine1.setBackgroundColor(getResources().getColor(R.color.holo_blue_bright));
            mHolder.mLine1.setTextColor(Color.WHITE);
          //  mHolder.mLine1.setText(lastMsg);
                        
        }
        else 
        {
            if (presence == Imps.Presence.AVAILABLE)
            {
                mHolder.mLine1.setBackgroundColor(getResources().getColor(R.color.android_green_light));
            }
            else if (presence == Imps.Presence.AWAY||presence == Imps.Presence.IDLE)
            {
                mHolder.mLine1.setBackgroundColor(getResources().getColor(R.color.holo_orange_light));
            }
            else if (presence == Imps.Presence.DO_NOT_DISTURB)
            {
                mHolder.mLine1.setBackgroundColor(getResources().getColor(R.color.holo_red_light));
            }            
            else
            {
                mHolder.mLine1.setBackgroundColor(getResources().getColor(R.color.contact_status_bg));
            }
            
            mHolder.mLine1.setTextColor(Color.DKGRAY);
     //       mHolder.mLine2.setVisibility(View.VISIBLE);
       //     mHolder.mLine2.setTextAppearance(mContext, Typeface.NORMAL);
      //      setBackgroundResource(android.R.color.transparent);
        }
        
        if (TextUtils.isEmpty(status)) {
            if (Imps.Contacts.TYPE_GROUP == type) {
                // Show nothing in line2 if it's a group and don't
                // have any unread message.
                status = null;
            } else {
                // Show the custom status text if there's no new message.
                status = statusText;
            }
        }

        if (TextUtils.isEmpty(status)) {
            // Show a string of presence if there is neither new message nor
            // custom status text.
            status = brandingRes.getString(PresenceUtils.getStatusStringRes(presence));
        }

        /*
        if (Imps.Contacts.TYPE_GROUP == type)
            mHolder.mLine2.setText("");
        else
            mHolder.mLine2.setText(status);
        */
       // mLine2.setCompoundDrawablesWithIntrinsicBounds(null, null, presenceIcon, null);

        View contactInfoPanel = findViewById(R.id.contactInfo);
        if (hasChat && showChatMsg) { // HERE the bubble is set
        //    contactInfoPanel.setBackgroundResource(R.drawable.bubble);
      //      mLine1.setTextColor(r.getColor(R.color.chat_contact));
        } else {
         //   contactInfoPanel.setBackgroundDrawable(null);
          //  contactInfoPanel.setPadding(4, 0, 0, 0);
         //   mLine1.setTextColor(r.getColor(R.color.nonchat_contact));
        }
    }
    
    private Drawable loadAvatar (final String address, Cursor cursor)
    {
     
        try {
            if (mContactListManager != null)
            {
                byte[] data = mContactListManager.getAvatar(address);
                if (data != null && data.length > 1)
                {
                    Drawable avatar = DatabaseUtils.decodeAvatar(data, ImApp.DEFAULT_AVATAR_WIDTH, ImApp.DEFAULT_AVATAR_HEIGHT);
                      if (avatar != null)
                      {
                          bitmapCache.put(Address.stripResource(address), avatar);           

                          return avatar;    
                      }
                      
                }
            }
            
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
             
        return null;
    }
        
    
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
    }
    
    public static Drawable getAvatar (String address)
    {
        return (Drawable) bitmapCache.get(address);
    }

}
