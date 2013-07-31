/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
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
import info.guardianproject.otr.app.im.provider.Imps;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class MessageView extends LinearLayout {
    public enum DeliveryState {
        NEUTRAL, DELIVERED, UNDELIVERED
    }

    public enum EncryptionState {
        NONE, ENCRYPTED, ENCRYPTED_AND_VERIFIED
        
    }
    private View mMessageContainer;
    private TextView mTextViewForMessages;
    private TextView mTextViewForTimestamp;
    
    private ImageView mDeliveryIcon;
    private Resources mResources;
    private ImageView mAvatarLeft;
    private ImageView mAvatarRight;
    
    private CharSequence lastMessage = null;
    
    private static final int cacheSize = 10; // 4MiB
    private static LruCache bitmapCache = new LruCache(cacheSize);

    private static Drawable mAvatarUnknown;
    
    public MessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        if (mAvatarUnknown == null)
            mAvatarUnknown = context.getResources().getDrawable(R.drawable.avatar_unknown);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mMessageContainer = findViewById (R.id.message_container);
        mTextViewForMessages = (TextView) findViewById(R.id.message);
        mTextViewForTimestamp = (TextView) findViewById(R.id.messagets);
        mDeliveryIcon = (ImageView) findViewById(R.id.iconView);
        mAvatarLeft = (ImageView) findViewById(R.id.avatar_left);
        mAvatarRight = (ImageView) findViewById(R.id.avatar_right);
        
        mResources = getResources();

       
    }

    public URLSpan[] getMessageLinks() {
        return mTextViewForMessages.getUrls();
    }
    

    public String getLastMessage () {
        return lastMessage.toString();
    }
    public void bindIncomingMessage(String address, String nickname, String body, Date date, Markup smileyRes,
            boolean scrolling, EncryptionState encryption, boolean showContact) {
      
        ListView.LayoutParams lp = new ListView.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        setGravity(Gravity.LEFT);
        setLayoutParams(lp);     
        setPadding(3,0,100,3);
        
        showAvatar(address,true);
        
       lastMessage = formatMessage(body);
        mTextViewForMessages.setText(lastMessage);
        
       mDeliveryIcon.setVisibility(INVISIBLE);
        
        if (date != null)
        {
         CharSequence tsText = formatTimeStamp(date);
         
         mTextViewForTimestamp.setText(tsText);
         mTextViewForTimestamp.setGravity(Gravity.CENTER);
         mTextViewForTimestamp.setVisibility(View.VISIBLE);
        
        }
        else
        {
            if (showContact)
            {
                mTextViewForTimestamp.setText(address);
            }
            else
            {
            mTextViewForTimestamp.setText("");
            mTextViewForTimestamp.setVisibility(View.GONE);
            }
        }
        
        mMessageContainer.setBackgroundResource(R.color.incoming_message);
       

    }

    public void bindOutgoingMessage(String address, String body, Date date, Markup smileyRes, boolean scrolling,
            DeliveryState delivery, EncryptionState encryption) {
        
        
        
        ListView.LayoutParams lp = new ListView.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
       // lp.setMargins(3,3,100,3);
        setLayoutParams(lp);
        setGravity(Gravity.RIGHT);

        setPadding(100, 0, 3, 3);
        
        showAvatar(address,false);
    
        
        lastMessage = formatMessage(body);
         mTextViewForMessages.setText(lastMessage);
         
     //   mTextViewForMessages.setTextColor(mResources.getColor(R.color.chat_msg));
        
        if (delivery == DeliveryState.DELIVERED) {
            mDeliveryIcon.setImageResource(R.drawable.ic_chat_msg_status_ok);
            mDeliveryIcon.setVisibility(VISIBLE);
        } else if (delivery == DeliveryState.UNDELIVERED) {
            mDeliveryIcon.setImageResource(R.drawable.ic_chat_msg_status_failed);
            mDeliveryIcon.setVisibility(VISIBLE);
        } else {
            mDeliveryIcon.setVisibility(GONE);
        }
        

        if (date != null)
        {
            mTextViewForTimestamp.setText(formatTimeStamp(date));
            mTextViewForTimestamp.setGravity(Gravity.CENTER);
            mTextViewForTimestamp.setVisibility(View.VISIBLE);
            mTextViewForTimestamp.setPadding(0,0,0,12);

        }
        else
        {
            mTextViewForTimestamp.setText("");
            mTextViewForTimestamp.setVisibility(View.GONE);
            mTextViewForTimestamp.setPadding(0,0,0,0);

        }
        
        mMessageContainer.setBackgroundResource(R.color.outgoing_message);
        
        
    }

    private void showAvatar (String address, boolean isLeft)
    {

        mAvatarLeft.setVisibility(View.GONE);
        mAvatarRight.setVisibility(View.GONE);
        
        if (address != null)
        {
            Drawable avatar = ContactView.getAvatar(address);
            
            if (avatar == null)
            {
                avatar = mAvatarUnknown;
                
            }
        
           
            if (isLeft)
            {
                mAvatarLeft.setVisibility(View.VISIBLE);
                mAvatarLeft.setImageDrawable(avatar);
            }
            else
            {
                mAvatarRight.setVisibility(View.VISIBLE);
                mAvatarRight.setImageDrawable(avatar);
            }
        }    
    }
    public void bindPresenceMessage(String contact, int type, boolean isGroupChat, boolean scrolling) {
        CharSequence message = formatPresenceUpdates(contact, type, isGroupChat, scrolling);
        mTextViewForMessages.setText(message);
        mTextViewForMessages.setTextColor(mResources.getColor(R.color.chat_msg_presence));
        mDeliveryIcon.setVisibility(INVISIBLE);
    }

    public void bindErrorMessage(int errCode) {
        mTextViewForMessages.setText(R.string.msg_sent_failed);
        mTextViewForMessages.setTextColor(mResources.getColor(R.color.error));
        mDeliveryIcon.setVisibility(INVISIBLE);
    }

    private CharSequence formatMessage(String body) {
        
        /*
        if (body.indexOf('\r') != -1) {
            // first convert \r\n pair to \n, then single \r to \n.
            // here we can't use HideReturnsTransformationMethod because
            // it does only 1 to 1 transformation and is unable to handle
            // the "\r\n" case.
            body = body.replace("\r\n", "\n").replace('\r', '\n');
        }*/

        //remove HTML tags since we can't display HTML
       body = body.replaceAll("\\<.*?\\>", "");

        SpannableStringBuilder buf = new SpannableStringBuilder();
        
        /*

        if (scrolling) {
            buf.append(body);
        } else {
            
          //  buf.setSpan(ChatView.STYLE_NORMAL, 0, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            buf.append(body);

           
        }
        */
        
        buf.append(body);
        
        /*
        if (date != null) {
            appendTimeStamp(buf, date);
        }*/
        
        return buf;
    }

    private SpannableString formatTimeStamp(Date date) {
    //    DateFormat format = new SimpleDateFormat(mResources.getString(R.string.time_stamp));
        
        DateFormat format = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String dateStr = format.format(date);
        SpannableString spanText = new SpannableString(dateStr);
        int len = spanText.length();
        spanText.setSpan(new StyleSpan(Typeface.ITALIC), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanText.setSpan(new RelativeSizeSpan(0.8f), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanText.setSpan(new ForegroundColorSpan(mResources.getColor(android.R.color.darker_gray)),
                0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      
        return spanText;
    }

    private CharSequence formatPresenceUpdates(String contact, int type, boolean isGroupChat,
            boolean scrolling) {
        String body;
        switch (type) {
        case Imps.MessageType.PRESENCE_AVAILABLE:
            body = mResources.getString(isGroupChat ? R.string.contact_joined
                                                   : R.string.contact_online, contact);
            break;

        case Imps.MessageType.PRESENCE_AWAY:
            body = mResources.getString(R.string.contact_away, contact);
            break;

        case Imps.MessageType.PRESENCE_DND:
            body = mResources.getString(R.string.contact_busy, contact);
            break;

        case Imps.MessageType.PRESENCE_UNAVAILABLE:
            body = mResources.getString(isGroupChat ? R.string.contact_left
                                                   : R.string.contact_offline, contact);
            break;

        default:
            return null;
        }

        if (scrolling) {
            return body;
        } else {
            SpannableString spanText = new SpannableString(body);
            int len = spanText.length();
            spanText.setSpan(new StyleSpan(Typeface.ITALIC), 0, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanText.setSpan(new RelativeSizeSpan((float) 0.8), 0, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spanText;
        }
    }
}
