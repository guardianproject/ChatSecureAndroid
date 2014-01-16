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
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.util.LogCleaner;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MessageView extends LinearLayout {
    
    private static int sCacheSize = 512; // 1MiB
    private static LruCache<String,Bitmap> mBitmapCache = new LruCache<String,Bitmap>(sCacheSize);
            
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
    
    private final static DateFormat MESSAGE_DATE_FORMAT = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    
    class ViewHolder 
    {

        TextView mTextViewForMessages = (TextView) findViewById(R.id.message);
        TextView mTextViewForTimestamp = (TextView) findViewById(R.id.messagets);
        ImageView mAvatar = (ImageView) findViewById(R.id.avatar);              
        View mStatusBlock = findViewById(R.id.status_block);        
        ImageView mMediaThumbnail = (ImageView) findViewById(R.id.media_thumbnail);
        ImageView mMediaDelivery = (ImageView) findViewById(R.id.message_delivered);
        View mContainer = findViewById(R.id.message_container);
        
        // save the media uri while the MediaScanner is creating the thumbnail
        // if the holder was reused, the pair is broken
        Uri mMediaUri = null;
        
        public void setOnClickListenerMediaThumbnail( final String mimeType, final String body ) {
            mMediaThumbnail.setOnClickListener( new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickMediaIcon( mimeType, body );
                }
            });
        }
        
        public void resetOnClickListenerMediaThumbnail() {
            mMediaThumbnail.setOnClickListener( null );
        }
        
       long mTimeDiff = -1;
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        if (!isInEditMode() )
        {
        
            mHolder = (ViewHolder)getTag();
            
            if (mHolder == null)
            {
                mHolder = new ViewHolder();
                
                setTag(mHolder);
                
                
            }
        }

    }
    

    public URLSpan[] getMessageLinks() {
        return mHolder.mTextViewForMessages.getUrls();
    }
    

    public String getLastMessage () {
        return lastMessage.toString();
    }
    
    public void bindIncomingMessage(int id, String address, String nickname, final String mimeType, final String body, Date date, Markup smileyRes,
            boolean scrolling, EncryptionState encryption, boolean showContact) {
      
        mHolder = (ViewHolder)getTag();

        mHolder.mTextViewForMessages.setVisibility(View.VISIBLE);
        
        if (showContact && nickname != null)
        {
            String[] nickParts = nickname.split("/");
            
            lastMessage = nickParts[nickParts.length-1] + ": " + formatMessage(body);
            
        }
        else
        {
            lastMessage = formatMessage(body);
            showAvatar(address,true);
        
        mHolder.resetOnClickListenerMediaThumbnail();     
        if( mimeType != null ) {
            mHolder.setOnClickListenerMediaThumbnail(mimeType, body);     
            lastMessage = "";
            // if a new uri, display generic icon first, then set it to the media icon/thumbnail
            Uri mediaUri = Uri.parse( body ) ;
            
            mHolder.mMediaThumbnail.setVisibility(View.VISIBLE);                       
            mHolder.mTextViewForMessages.setText(lastMessage);
            mHolder.mTextViewForMessages.setVisibility(View.GONE);
            if( mimeType.startsWith("image/")||mimeType.startsWith("video/") ) {
                setIncomingImageThumbnail( getContext().getContentResolver(), id, mHolder, mediaUri );
            }
            else if (mimeType.startsWith("audio"))
            {
                mHolder.mMediaThumbnail.setImageResource(R.drawable.media_audio_play);
            }
            else
            {
                mHolder.mMediaThumbnail.setImageResource(R.drawable.ic_file); // generic file icon
                
            }
            
        } else {
            mHolder.mMediaThumbnail.setVisibility(View.GONE);
            if (showContact)
            {
                String[] nickParts = nickname.split("/");
                
                lastMessage = nickParts[nickParts.length-1] + ": " + formatMessage(body);
                
            }
            else
            {
                lastMessage = formatMessage(body);
            }
        }
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
        
        
        if (date != null)
        {
         CharSequence tsText = formatTimeStamp(date,MESSAGE_DATE_FORMAT);
         
         mHolder.mTextViewForTimestamp.setText(tsText);
         mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);
        
        }
        else
        {
            
            mHolder.mTextViewForTimestamp.setText("");
            //mHolder.mTextViewForTimestamp.setVisibility(View.GONE);
           
        }


        if (encryption == EncryptionState.NONE)
        {
            
            mHolder.mStatusBlock.setBackgroundResource(R.color.holo_red_dark);
               
        }
        else if (encryption == EncryptionState.ENCRYPTED)
        {
            
            mHolder.mStatusBlock.setBackgroundResource(R.color.holo_orange_light);
            
            //mHolder.mEncryptionIcon.setImageResource(R.drawable.lock16);
            
            
        }
        else if (encryption == EncryptionState.ENCRYPTED_AND_VERIFIED)
        {
            
            mHolder.mStatusBlock.setBackgroundResource(R.color.holo_green_dark);
            
            //mHolder.mEncryptionIcon.setImageResource(R.drawable.lock16);
            
        }
        
        mHolder.mTextViewForMessages.setTextColor(getResources().getColor(R.color.incoming_message_fg));
       
        Linkify.addLinks(mHolder.mTextViewForMessages, Linkify.ALL);
        
    }
    
    private MediaPlayer mMediaPlayer = null;
    
    /**
     * @param mimeType
     * @param body
     */
    protected void onClickMediaIcon(String mimeType, String body) {
        
        if (mimeType.startsWith("audio") || (body.endsWith("3gp")||body.endsWith("amr")))
        {
           
            if (mMediaPlayer != null)
                mMediaPlayer.release();
            
            try
            {
                mMediaPlayer = new  MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(body);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
                
                return;
            } catch (IOException e) {
                Log.e(ImApp.LOG_TAG,"error playing audio: " + body,e);
            }
            
            
        }
        
        Intent intent = new Intent(Intent.ACTION_VIEW);  
        //intent.setDataAndType(Uri.parse( body ), mimeType);
        intent.setData(Uri.parse( body ));
        getContext().startActivity(intent);
        
    }


    /**
     * @param contentResolver 
     * @param id 
     * @param aHolder
     * @param mediaUri
     */
    private void setIncomingImageThumbnail(final ContentResolver contentResolver, final int id, final ViewHolder aHolder, final Uri mediaUri) {
        // pair this holder to the uri. if the holder is recycled, the pairing is broken
        aHolder.mMediaUri = mediaUri;
        // if a content uri - already scanned
        if( mediaUri.getScheme() != null ) {
            setThumbnail(contentResolver, aHolder, mediaUri);
            return;
        }
        
        // new file - scan
        File file = new File(mediaUri.getPath());
        final Handler handler = new Handler();
        MediaScannerConnection.scanFile(
                getContext(), new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, final Uri uri) {
                        // write the uri into the db
                        Imps.updateMessageBody(contentResolver, id, uri.toString() );
                        handler.post( new Runnable() {
                            @Override
                            public void run() {
                                // confirm the holder is still paired to this uri
                                if( aHolder.mMediaUri == mediaUri ) {
                                    setThumbnail(contentResolver, aHolder, uri);
                                }
                            }
                        });
                    }
                });
                
    }
    
    /**
     * @param contentResolver 
     * @param aHolder
     * @param uri
     */
    private void setThumbnail(final ContentResolver contentResolver, final ViewHolder aHolder, final Uri uri) {
        new AsyncTask<String, Void, Bitmap>() {
            
            @Override
            protected Bitmap doInBackground(String... params) {
                
                Bitmap result = mBitmapCache.get(uri.toString());
                
                if (result == null)                
                    return getThumbnail( contentResolver, uri );
                else
                    return result;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                
                if (uri != null && result != null)
                {
                    mBitmapCache.put(uri.toString(), result);
                    
                    // confirm the holder is still paired to this uri
                    if( ! uri.equals( aHolder.mMediaUri ) ) {
                        return ;
                    }
                    // thumbnail extraction failed, use bropken image icon
                    if( result == null ) {
                        mHolder.mMediaThumbnail.setImageResource(R.drawable.ic_broken_image);
                        return ;
                    }
                    // set the thumbnail
                    aHolder.mMediaThumbnail.setImageBitmap(result);
                }
            }
        }.execute();
    }

    public static Bitmap getThumbnail(ContentResolver cr, Uri uri) {
        String[] projection = {MediaStore.Images.Media._ID};
        Cursor cursor = cr.query( uri, projection, null, null, null);
        if( cursor == null || cursor.getCount() == 0 ) {
            return null ;
        }
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(projection[0]);
        int id = cursor.getInt(columnIndex);
        cursor.close();
        
        Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(cr, id, MediaStore.Images.Thumbnails.MINI_KIND, null );
        return bitmap;
    }    
    

    private String formatMessage (String body)
    {
        return android.text.Html.fromHtml(body).toString();
    }
    
    public void bindOutgoingMessage(int id, String address, final String mimeType, final String body, Date date, Markup smileyRes, boolean scrolling,
            DeliveryState delivery, EncryptionState encryption) {
        
        mHolder = (ViewHolder)getTag();

        mHolder.mTextViewForMessages.setVisibility(View.VISIBLE);
        mHolder.resetOnClickListenerMediaThumbnail();     
        if( mimeType != null ) {
            mHolder.setOnClickListenerMediaThumbnail(mimeType, body);     
            lastMessage = "";
            // if a new uri, display generic icon first, then set it to the media icon/thumbnail
            Uri mediaUri = Uri.parse( body ) ;
            
            mHolder.mTextViewForMessages.setText("");//no message if there is a file mimeType
            mHolder.mTextViewForMessages.setVisibility(View.GONE);
            mHolder.mMediaThumbnail.setVisibility(View.VISIBLE);            
            if( mimeType.startsWith("image/")||mimeType.startsWith("video/") ) {
                setIncomingImageThumbnail( getContext().getContentResolver(), id, mHolder, mediaUri );
            }
            else if (mimeType.startsWith("audio"))
            {
                mHolder.mMediaThumbnail.setImageResource(R.drawable.media_audio_play);
            }
            else
            {
                mHolder.mMediaThumbnail.setImageResource(R.drawable.ic_file); // generic file icon
                
            }
            
        } else {
            mHolder.mMediaThumbnail.setVisibility(View.GONE);
            lastMessage = body;//formatMessage(body);
         
             try {

                 SpannableString spannablecontent=new SpannableString(lastMessage);
    
                 EmojiManager.getInstance(getContext()).addEmoji(getContext(), spannablecontent);
                 
                 mHolder.mTextViewForMessages.setText(spannablecontent);
             } catch (IOException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
             }
        }
         
        if (delivery == DeliveryState.DELIVERED) {
            
            mHolder.mMediaDelivery.setVisibility(View.VISIBLE);
            mHolder.mMediaDelivery.setImageResource(R.drawable.check);
            
            
        } else if (delivery == DeliveryState.UNDELIVERED) {

            mHolder.mMediaDelivery.setVisibility(View.VISIBLE);
            mHolder.mMediaDelivery.setImageResource(R.drawable.navigation_cancel);
            
        } 
        else
        {
            mHolder.mMediaDelivery.setVisibility(View.GONE);
            
        }
        


        mHolder.mStatusBlock.setVisibility(VISIBLE);

//        mHolder.mMessageContainer.setBackgroundResource(R.drawable.background_plaintext);
        
        if (encryption == EncryptionState.NONE)
        {

            mHolder.mStatusBlock.setBackgroundResource(R.color.holo_red_dark);

               
        }
        else if (encryption == EncryptionState.ENCRYPTED)
        {
            mHolder.mStatusBlock.setBackgroundResource(R.color.holo_orange_light);            
     
            
            //mHolder.mEncryptionIcon.setImageResource(R.drawable.lock16);
        }
        
        else if (encryption == EncryptionState.ENCRYPTED_AND_VERIFIED)
        {
            mHolder.mStatusBlock.setBackgroundResource(R.color.holo_green_dark);
            
            //mHolder.mEncryptionIcon.setImageResource(R.drawable.lock16);
        }

        if (date != null)
        {
            mHolder.mTextViewForTimestamp.setText(formatTimeStamp(date,MESSAGE_DATE_FORMAT));            
            mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);            
         
        }
        else
        {
            mHolder.mTextViewForTimestamp.setText("");
//            mHolder.mTextViewForTimestamp.setVisibility(View.GONE);            

        }
        
                  
        mHolder.mTextViewForMessages.setTextColor(getResources().getColor(R.color.outgoing_message_fg));
        
        Linkify.addLinks(mHolder.mTextViewForMessages, Linkify.ALL);
        
    }

    private void showAvatar (String address, boolean isLeft)
    {

        mHolder.mAvatar.setVisibility(View.GONE);        
        
        if (address != null)
        {
            
            Drawable avatar = DatabaseUtils.getAvatarFromAddress(this.getContext().getContentResolver(),address, ImApp.DEFAULT_AVATAR_WIDTH,ImApp.DEFAULT_AVATAR_HEIGHT);
    
            if (avatar != null)
            {
                if (isLeft)
                {
                    mHolder.mAvatar.setVisibility(View.VISIBLE);
                    mHolder.mAvatar.setImageDrawable(avatar);
                }
            }
            else
            {
                mHolder.mAvatar.setVisibility(View.GONE);
            }
                
            
        }    
    }

    public void bindPresenceMessage(String contact, int type, boolean isGroupChat, boolean scrolling) {
        
        mHolder = (ViewHolder)getTag();

        CharSequence message = formatPresenceUpdates(contact, type, isGroupChat, scrolling);
        mHolder.mTextViewForMessages.setText(message);
        mHolder.mTextViewForMessages.setTextColor(getResources().getColor(R.color.chat_msg_presence));

    }

    public void bindErrorMessage(int errCode) {
        
        mHolder = (ViewHolder)getTag();

        mHolder.mTextViewForMessages.setText(R.string.msg_sent_failed);
        mHolder.mTextViewForMessages.setTextColor(getResources().getColor(R.color.error));

    }

   

    private SpannableString formatTimeStamp(Date date, DateFormat format) {
        String dateStr = format.format(date);
        SpannableString spanText = new SpannableString(dateStr);
        int len = spanText.length();
        spanText.setSpan(new StyleSpan(Typeface.ITALIC), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        spanText.setSpan(new RelativeSizeSpan(0.8f), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanText.setSpan(new ForegroundColorSpan(R.color.soft_grey),
              0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
     
        return spanText;
    }

    private CharSequence formatPresenceUpdates(String contact, int type, boolean isGroupChat,
            boolean scrolling) {
        String body;
        
        Resources resources =getResources();
        
        switch (type) {
        case Imps.MessageType.PRESENCE_AVAILABLE:
            body = resources.getString(isGroupChat ? R.string.contact_joined
                                                   : R.string.contact_online, contact);
            break;

        case Imps.MessageType.PRESENCE_AWAY:
            body = resources.getString(R.string.contact_away, contact);
            break;

        case Imps.MessageType.PRESENCE_DND:
            body = resources.getString(R.string.contact_busy, contact);
            break;

        case Imps.MessageType.PRESENCE_UNAVAILABLE:
            body = resources.getString(isGroupChat ? R.string.contact_left
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
