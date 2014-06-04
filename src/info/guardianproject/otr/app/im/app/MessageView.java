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
import info.guardianproject.otr.app.im.ui.RoundedAvatarDrawable;
import info.guardianproject.util.LogCleaner;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MessageView extends FrameLayout {
    
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
    
    private final static DateFormat MESSAGE_DATETIME_FORMAT = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private final static DateFormat MESSAGE_TIME_FORMAT = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
    private static final SimpleDateFormat FMT_SAME_DAY = new SimpleDateFormat("yyyyMMdd");;
    
    private final static Date DATE_NOW = new Date();

    private final static char DELIVERED_SUCCESS = '\u2714';
    private final static char DELIVERED_FAIL = '\u2718';
    private final static String LOCK_CHAR = "Secure";
    		

    class ViewHolder 
    {

        TextView mTextViewForMessages = (TextView) findViewById(R.id.message);
        TextView mTextViewForTimestamp = (TextView) findViewById(R.id.messagets);
        ImageView mAvatar = (ImageView) findViewById(R.id.avatar);              
       // View mStatusBlock = findViewById(R.id.status_block);        
        ImageView mMediaThumbnail = (ImageView) findViewById(R.id.media_thumbnail);
        View mContainer = findViewById(R.id.message_container);
        
        // save the media uri while the MediaScanner is creating the thumbnail
        // if the holder was reused, the pair is broken
        Uri mMediaUri = null;
        
        public void setOnClickListenerMediaThumbnail( final String mimeType, final Uri mediaUri ) {
            mMediaThumbnail.setOnClickListener( new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickMediaIcon( mimeType, mediaUri );
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
        
      
        mHolder = (ViewHolder)getTag();
        
        if (mHolder == null)
        {
            mHolder = new ViewHolder();   
            setTag(mHolder);
            
        }

    }
    
    public void setMessageBackground (Drawable d)
    {
        mHolder.mContainer.setBackgroundDrawable(d);
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
        
        if (nickname == null)
            nickname = address;
        
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
    
                Uri mediaUri = Uri.parse( body ) ;
                lastMessage = "";
                showMediaThumbnail(mimeType, mediaUri, id, mHolder);
               
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
           CharSequence tsText = null;
            
           if (isSameDay(date,DATE_NOW))
               tsText = formatTimeStamp(date,MESSAGE_TIME_FORMAT, null, encryption);
           else
               tsText = formatTimeStamp(date,MESSAGE_DATETIME_FORMAT, null, encryption);
         
         mHolder.mTextViewForTimestamp.setText(tsText);
         mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);
        
        }
        else
        {
            
            mHolder.mTextViewForTimestamp.setText("");
            //mHolder.mTextViewForTimestamp.setVisibility(View.GONE);
           
        }
       
        Linkify.addLinks(mHolder.mTextViewForMessages, Linkify.ALL);
        
    }
    
    private void showMediaThumbnail (String mimeType, Uri mediaUri, int id, ViewHolder holder)
    {
        holder.setOnClickListenerMediaThumbnail(mimeType, mediaUri);     
        
        holder.mMediaThumbnail.setVisibility(View.VISIBLE);                       
        holder.mTextViewForMessages.setText(lastMessage);
        holder.mTextViewForMessages.setVisibility(View.GONE);
        if( mimeType.startsWith("image/")||mimeType.startsWith("video/") ) {
            setImageThumbnail( getContext().getContentResolver(), id, holder, mediaUri );                
            holder.mMediaThumbnail.setBackgroundColor(Color.WHITE);
            
        }
        else if (mimeType.startsWith("audio"))
        {
            holder.mMediaThumbnail.setImageResource(R.drawable.media_audio_play);
            holder.mMediaThumbnail.setBackgroundColor(Color.TRANSPARENT);
        }
        else
        {
            holder.mMediaThumbnail.setImageResource(R.drawable.ic_file); // generic file icon
            
        }
        
        holder.mContainer.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        
     
     
    }
    
    
    private boolean isSameDay (Date date1, Date date2)
    {        
        return FMT_SAME_DAY.format(date1).equals(FMT_SAME_DAY.format(date2));
    }
    
    protected String convertMediaUriToPath(Uri uri) {
        String path = null;
        
        String [] proj={MediaStore.Images.Media.DATA};
        Cursor cursor = getContext().getContentResolver().query(uri, proj,  null, null, null);
        if (cursor != null && (!cursor.isClosed()))
        {
            if (cursor.isBeforeFirst())
            {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);            
                cursor.moveToFirst();
                path = cursor.getString(column_index);
            }
            
            cursor.close();
        }
        
        return path;
    }
    
    private MediaPlayer mMediaPlayer = null;
    
    /**
     * @param mimeType
     * @param body
     */
    protected void onClickMediaIcon(String mimeType, Uri mediaUri) {
        
        String body = convertMediaUriToPath(mediaUri);
        
        if (body == null)
            body = new File(mediaUri.getPath()).getAbsolutePath();
        
        if (mimeType.startsWith("audio") || (body.endsWith("3gp")||body.endsWith("3gpp")||body.endsWith("amr")))
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
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
      
        //set a general mime type not specific
        if (mimeType != null)
        {
            intent.setDataAndType(Uri.parse( body ), mimeType);
        }
        else
        {
            intent.setData(Uri.parse( body ));
        }
        
        Context context = getContext().getApplicationContext();
        
        if (isIntentAvailable(context,intent))
        {        
            context.startActivity(intent);
        }
        else
        {
            Toast.makeText(getContext(), R.string.there_is_no_viewer_available_for_this_file_format, Toast.LENGTH_LONG).show();
        }
        
    }
    
    public static boolean isIntentAvailable(Context context, Intent intent) {  
        final PackageManager packageManager = context.getPackageManager();  
        List<ResolveInfo> list =  
                packageManager.queryIntentActivities(intent,  
                        PackageManager.MATCH_DEFAULT_ONLY);  
        return list.size() > 0;  
    }  


    /**
     * @param contentResolver 
     * @param id 
     * @param aHolder
     * @param mediaUri
     */
    private void setImageThumbnail(final ContentResolver contentResolver, final int id, final ViewHolder aHolder, final Uri mediaUri) {
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
            
            lastMessage = "";
            Uri mediaUri = Uri.parse( body ) ;
            
            showMediaThumbnail(mimeType, mediaUri, id, mHolder);
            

            
        } else {
            mHolder.mMediaThumbnail.setVisibility(View.GONE);
            lastMessage = formatMessage(body);
         
             try {

                 SpannableString spannablecontent=new SpannableString(lastMessage);
    
                 EmojiManager.getInstance(getContext()).addEmoji(getContext(), spannablecontent);
                 
                 mHolder.mTextViewForMessages.setText(spannablecontent);
             } catch (IOException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
             }
        }
         
       
        /**
        mHolder.mStatusBlock.setVisibility(VISIBLE);

//        mHolder.mMessageContainer.setBackgroundResource(R.drawable.background_plaintext);
        
        if (encryption == EncryptionState.NONE)
        {

            mHolder.mStatusBlock.setBackgroundResource(R.color.holo_red_dark);

               
        }
        else if (encryption == EncryptionState.ENCRYPTED)
        {
            mHolder.mStatusBlock.setBackgroundResource(R.color.holo_orange_light);            
     
        }
        
        else if (encryption == EncryptionState.ENCRYPTED_AND_VERIFIED)
        {
            mHolder.mStatusBlock.setBackgroundResource(R.color.holo_green_dark);
            
        }*/


        if (date != null)
        {
            
            CharSequence tsText = null;
            
            if (isSameDay(date,DATE_NOW))
                tsText = formatTimeStamp(date,MESSAGE_TIME_FORMAT, delivery, encryption);
            else
                tsText = formatTimeStamp(date,MESSAGE_DATETIME_FORMAT, delivery, encryption);
            
            mHolder.mTextViewForTimestamp.setText(tsText);    
            
            mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);            
         
        }
        else
        {
            mHolder.mTextViewForTimestamp.setText("");      

        }
        
                  
        Linkify.addLinks(mHolder.mTextViewForMessages, Linkify.ALL);
        
    }

    private static Drawable AVATAR_DEFAULT;
    
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
                if (AVATAR_DEFAULT == null)
                {
                    AVATAR_DEFAULT = new RoundedAvatarDrawable(BitmapFactory.decodeResource(getResources(),
                            R.drawable.avatar_unknown));
                }
                
                avatar = AVATAR_DEFAULT;
                mHolder.mAvatar.setVisibility(View.VISIBLE);
                mHolder.mAvatar.setImageDrawable(avatar);
                
            }
                
            
        }    
    }

    public void bindPresenceMessage(String contact, int type, boolean isGroupChat, boolean scrolling) {
        
        mHolder = (ViewHolder)getTag();

        CharSequence message = formatPresenceUpdates(contact, type, isGroupChat, scrolling);
        mHolder.mTextViewForMessages.setText(message);
     //   mHolder.mTextViewForMessages.setTextColor(getResources().getColor(R.color.chat_msg_presence));

    }

    public void bindErrorMessage(int errCode) {
        
        mHolder = (ViewHolder)getTag();

        mHolder.mTextViewForMessages.setText(R.string.msg_sent_failed);
        mHolder.mTextViewForMessages.setTextColor(getResources().getColor(R.color.error));

    }

    private SpannableString formatTimeStamp(Date date, DateFormat format, MessageView.DeliveryState delivery, EncryptionState encryptionState) {
        

        StringBuilder deliveryText = new StringBuilder();
        deliveryText.append(format.format(date));
        deliveryText.append(' ');
        
        if (delivery != null)
        {
            if (delivery == DeliveryState.DELIVERED) {
                
                deliveryText.append(DELIVERED_SUCCESS);
                    
            } else if (delivery == DeliveryState.UNDELIVERED) {
    
                deliveryText.append(DELIVERED_FAIL);
                
            } 
        }
        

        SpannableString spanText = null;
        
        if (encryptionState == EncryptionState.ENCRYPTED || encryptionState == EncryptionState.ENCRYPTED_AND_VERIFIED)
        {
            deliveryText.append('X');
            spanText = new SpannableString(deliveryText.toString());
            int len = spanText.length();
            
        //    spanText.setSpan(new ImageSpan(getContext(), entry.getValue()), index, index + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanText.setSpan(new ImageSpan(getContext(), R.drawable.lock16), len-1,len,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
        }
        else
        {
            spanText = new SpannableString(deliveryText.toString());
            int len = spanText.length();
            
        }
        
     //   spanText.setSpan(new StyleSpan(Typeface.SANS_SERIF), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
       // spanText.setSpan(new RelativeSizeSpan(0.8f), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    //    spanText.setSpan(new ForegroundColorSpan(R.color.soft_grey),
      //        0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
     
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
