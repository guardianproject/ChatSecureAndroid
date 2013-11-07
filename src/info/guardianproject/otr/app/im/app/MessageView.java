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

import info.guardianproject.emoji.EmojiManager;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ContactView.ViewHolder;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.util.LogCleaner;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
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
    private CharSequence lastMessage = null;
    
    public MessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
    }

    private ViewHolder mHolder = null;
    private Resources mResources = null;
    
    class ViewHolder 
    {

        View mMessageContainer = findViewById (R.id.message_container);
              
        TextView mTextViewForMessages = (TextView) findViewById(R.id.message);
        TextView mTextViewForTimestamp = (TextView) findViewById(R.id.messagets);
        ImageView mDeliveryIcon = (ImageView) findViewById(R.id.iconDelivery);
        ImageView mAvatarLeft = (ImageView) findViewById(R.id.avatar_left);
        ImageView mAvatarRight = (ImageView) findViewById(R.id.avatar_right);
        ImageView mEncryptionIconLeft = (ImageView) findViewById(R.id.iconEncryptionLeft);
        ImageView mEncryptionIconRight = (ImageView) findViewById(R.id.iconEncryptionRight);
        View mStatusBlockLeft = findViewById(R.id.status_block_left);
        View mStatusBlockRight = findViewById(R.id.status_block_right);
        
        
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHolder = (ViewHolder)getTag();
        
        if (mHolder == null)
        {
            mHolder = new ViewHolder();
            
            setTag(mHolder);
        }

        mResources = getResources();
    }
    

    public URLSpan[] getMessageLinks() {
        return mHolder.mTextViewForMessages.getUrls();
    }
    

    public String getLastMessage () {
        return lastMessage.toString();
    }
    public void bindIncomingMessage(String address, String nickname, String body, Date date, Markup smileyRes,
            boolean scrolling, EncryptionState encryption, boolean showContact) {
      

        mHolder = (ViewHolder)getTag();
        
        ListView.LayoutParams lp = new ListView.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        setGravity(Gravity.LEFT);
        setLayoutParams(lp);     
        setPadding(3,0,100,3);
        
        //showAvatar(address,true);
        
        if (showContact)
        {
            String[] nickParts = nickname.split("/");
            
            lastMessage = nickParts[nickParts.length-1] + ": " + formatMessage(body);
            
        }
        else
        {
            lastMessage = formatMessage(body);
        }
        
        if (lastMessage.length() > 0)
        {
            try {
                SpannableString spannablecontent=new SpannableString(lastMessage);
                EmojiManager.getInstance(getContext()).addEmoji(getContext(), spannablecontent);
                
                mHolder.mTextViewForMessages.setText(spannablecontent);
            } catch (IOException e) {
                LogCleaner.error(ImApp.LOG_TAG, "error processing message", e);
            }
        }
        else
        {
            mHolder.mTextViewForMessages.setText(lastMessage);
        }
        
        mHolder.mDeliveryIcon.setVisibility(GONE);
        mHolder.mStatusBlockLeft.setVisibility(VISIBLE);
        mHolder.mStatusBlockRight.setVisibility(GONE);
        
        if (date != null)
        {
         CharSequence tsText = formatTimeStamp(date);
         
         mHolder.mTextViewForTimestamp.setText(tsText);
         mHolder.mTextViewForTimestamp.setGravity(Gravity.CENTER);
         mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);
        
        }
        else
        {
            
            mHolder.mTextViewForTimestamp.setText("");
            mHolder.mTextViewForTimestamp.setVisibility(View.GONE);
           
        }

        mHolder.mMessageContainer.setBackgroundResource(R.drawable.background_plaintext);

        if (encryption == EncryptionState.NONE)
        {
            mHolder.mEncryptionIconLeft.setVisibility(GONE);
            mHolder.mEncryptionIconRight.setVisibility(GONE);
            mHolder.mStatusBlockLeft.setBackgroundColor(Color.LTGRAY);
               
        }
        else if (encryption == EncryptionState.ENCRYPTED)
        {
            mHolder.mEncryptionIconLeft.setVisibility(VISIBLE);
            mHolder.mStatusBlockLeft.setBackgroundResource(R.color.holo_orange_light);
            mHolder.mEncryptionIconLeft.setImageResource(R.drawable.ic_menu_encrypted);
            
            
        }
        else if (encryption == EncryptionState.ENCRYPTED_AND_VERIFIED)
        {
            mHolder.mEncryptionIconLeft.setVisibility(VISIBLE);
            mHolder.mStatusBlockLeft.setBackgroundResource(R.color.holo_purple);
            mHolder.mEncryptionIconLeft.setImageResource(R.drawable.ic_menu_verified);
            
        }
        
        mHolder.mTextViewForMessages.setTextColor(getResources().getColor(R.color.incoming_message_fg));
       

    }

    private String formatMessage (String body)
    {
        return android.text.Html.fromHtml(body).toString();
    }
    
    public void bindOutgoingMessage(String address, String body, Date date, Markup smileyRes, boolean scrolling,
            DeliveryState delivery, EncryptionState encryption) {
        
        
        
        ListView.LayoutParams lp = new ListView.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
       // lp.setMargins(3,3,100,3);
        setLayoutParams(lp);
        setGravity(Gravity.RIGHT);

        setPadding(100, 0, 3, 3);
        
      //  showAvatar(address,false);
    
        
        lastMessage = body;//formatMessage(body);
         
         try {
             mHolder.mMessageContainer.setBackgroundResource(R.drawable.background_plaintext);
             SpannableString spannablecontent=new SpannableString(lastMessage);

             EmojiManager.getInstance(getContext()).addEmoji(getContext(), spannablecontent);
             
             mHolder.mTextViewForMessages.setText(spannablecontent);
         } catch (IOException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
         
        if (delivery == DeliveryState.DELIVERED) {
            mHolder.mDeliveryIcon.setVisibility(VISIBLE);
            mHolder.mDeliveryIcon.setImageResource(R.drawable.ic_chat_msg_status_ok);

        } else if (delivery == DeliveryState.UNDELIVERED) {
            mHolder.mDeliveryIcon.setImageResource(R.drawable.ic_chat_msg_status_failed);
            mHolder.mDeliveryIcon.setVisibility(VISIBLE);
        } else {
            mHolder.mDeliveryIcon.setVisibility(GONE);
        }
        

        mHolder.mStatusBlockLeft.setVisibility(GONE);
        mHolder.mStatusBlockRight.setVisibility(VISIBLE);

        mHolder.mMessageContainer.setBackgroundResource(R.drawable.background_plaintext);
        
        if (encryption == EncryptionState.NONE)
        {

            mHolder.mEncryptionIconLeft.setVisibility(GONE);
            mHolder.mEncryptionIconRight.setVisibility(GONE);
            
            mHolder.mStatusBlockRight.setBackgroundColor(Color.LTGRAY);

               
        }
        else if (encryption == EncryptionState.ENCRYPTED)
        {
            mHolder.mStatusBlockRight.setBackgroundResource(R.color.holo_orange_light);
            mHolder.mEncryptionIconRight.setVisibility(VISIBLE);
            mHolder.mEncryptionIconRight.setImageResource(R.drawable.ic_menu_encrypted);
        }
        
        else if (encryption == EncryptionState.ENCRYPTED_AND_VERIFIED)
        {
            mHolder.mStatusBlockRight.setBackgroundResource(R.color.holo_purple);
            mHolder.mEncryptionIconRight.setVisibility(VISIBLE);
            mHolder.mEncryptionIconRight.setImageResource(R.drawable.ic_menu_verified);
        }

        if (date != null)
        {
            mHolder.mTextViewForTimestamp.setText(formatTimeStamp(date));
            mHolder.mTextViewForTimestamp.setGravity(Gravity.CENTER);
            mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);
            mHolder.mTextViewForTimestamp.setPadding(0,0,0,12);

        }
        else
        {
            mHolder.mTextViewForTimestamp.setText("");
            mHolder.mTextViewForTimestamp.setVisibility(View.GONE);
            mHolder.mTextViewForTimestamp.setPadding(0,0,0,0);

        }
        
                  
        mHolder.mTextViewForMessages.setTextColor(getResources().getColor(R.color.outgoing_message_fg));
    }

    private void showAvatar (String address, boolean isLeft)
    {

        mHolder.mAvatarLeft.setVisibility(View.GONE);
        mHolder.mAvatarRight.setVisibility(View.GONE);
        
        if (address != null)
        {
            
            Drawable avatar = DatabaseUtils.getAvatarFromAddress(this.getContext().getContentResolver(),address, ImApp.DEFAULT_AVATAR_WIDTH,ImApp.DEFAULT_AVATAR_HEIGHT);
    
            if (avatar != null)
            {
                if (isLeft)
                {
                    mHolder.mAvatarLeft.setVisibility(View.VISIBLE);
                    mHolder.mAvatarLeft.setImageDrawable(avatar);
                }
                else
                {
                    mHolder.mAvatarRight.setVisibility(View.VISIBLE);
                    mHolder.mAvatarRight.setImageDrawable(avatar);
                }
            }
            else
            {
                mHolder.mAvatarLeft.setVisibility(View.GONE);
                mHolder.mAvatarRight.setVisibility(View.GONE);
            }
                
            
        }    
    }

    public void bindPresenceMessage(String contact, int type, boolean isGroupChat, boolean scrolling) {
        CharSequence message = formatPresenceUpdates(contact, type, isGroupChat, scrolling);
        mHolder.mTextViewForMessages.setText(message);
        mHolder.mTextViewForMessages.setTextColor(mResources.getColor(R.color.chat_msg_presence));
        mHolder.mDeliveryIcon.setVisibility(INVISIBLE);
    }

    public void bindErrorMessage(int errCode) {
        mHolder.mTextViewForMessages.setText(R.string.msg_sent_failed);
        mHolder.mTextViewForMessages.setTextColor(mResources.getColor(R.color.error));
        mHolder.mDeliveryIcon.setVisibility(INVISIBLE);
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
