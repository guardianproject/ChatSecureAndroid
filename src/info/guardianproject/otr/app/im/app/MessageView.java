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
import info.guardianproject.otr.app.im.engine.Presence;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.ui.ImageViewActivity;
import info.guardianproject.otr.app.im.ui.LetterAvatar;
import info.guardianproject.otr.app.im.ui.RoundedAvatarDrawable;
import info.guardianproject.util.AudioPlayer;
import info.guardianproject.util.LogCleaner;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Browser;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import info.guardianproject.util.LinkifyHelper;

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

    private Context context;
    private boolean linkify = false;

    public MessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    private ViewHolder mHolder = null;

    private final static DateFormat MESSAGE_DATETIME_FORMAT = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private final static DateFormat MESSAGE_TIME_FORMAT = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
    private static final SimpleDateFormat FMT_SAME_DAY = new SimpleDateFormat("yyyyMMdd");

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

        ViewHolder() {
            // disable built-in autoLink so we can add custom ones
            mTextViewForMessages.setAutoLinkMask(0);
        }

        public void setOnClickListenerMediaThumbnail( final String mimeType, final Uri mediaUri ) {
            mMediaThumbnail.setOnClickListener( new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickMediaIcon( mimeType, mediaUri );
                }
            });
            mMediaThumbnail.setOnLongClickListener( new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    onLongClickMediaIcon( mimeType, mediaUri );
                    return false;
                }
            });
        }

        public void resetOnClickListenerMediaThumbnail() {
            mMediaThumbnail.setOnClickListener( null );
        }

       long mTimeDiff = -1;
    }

    /**
     * This trickery is needed in order to have clickable links that open things
     * in a new {@code Task} rather than in ChatSecure's {@code Task.} Thanks to @commonsware
     * https://stackoverflow.com/a/11417498
     *
     */
    class NewTaskUrlSpan extends ClickableSpan {

        private String urlString;

        NewTaskUrlSpan(String urlString) {
            this.urlString = urlString;
        }

        @Override
        public void onClick(View widget) {
            Uri uri = Uri.parse(urlString);
            Context context = widget.getContext();
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    class URLSpanConverter implements LinkifyHelper.SpanConverter<URLSpan, ClickableSpan> {
        @Override
        public NewTaskUrlSpan convert(URLSpan span) {
            return (new NewTaskUrlSpan(span.getURL()));
        }
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

    public void setLinkify(boolean linkify) {
        this.linkify = linkify;
    }

    public void setMessageBackground (Drawable d) {
        mHolder.mContainer.setBackgroundDrawable(d);
    }

    public URLSpan[] getMessageLinks() {
        return mHolder.mTextViewForMessages.getUrls();
    }


    public String getLastMessage () {
        return lastMessage.toString();
    }

    public void bindIncomingMessage(int id, int messageType, String address, String nickname, final String mimeType, final String body, Date date, Markup smileyRes,
            boolean scrolling, EncryptionState encryption, boolean showContact, int presenceStatus) {

        mHolder = (ViewHolder)getTag();

        mHolder.mTextViewForMessages.setVisibility(View.VISIBLE);

        if (nickname == null)
            nickname = address;

        if (showContact && nickname != null)
        {
            String[] nickParts = nickname.split("/");

            lastMessage = nickParts[nickParts.length-1] + ": " + formatMessage(body);
            showAvatar(address,nickname,true,presenceStatus);

        }
        else
        {
            lastMessage = formatMessage(body);
            showAvatar(address,nickname,true,presenceStatus);

            mHolder.resetOnClickListenerMediaThumbnail();
            if( mimeType != null ) {

                mHolder.mTextViewForMessages.setVisibility(View.GONE);
                mHolder.mMediaThumbnail.setVisibility(View.VISIBLE);

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
               tsText = formatTimeStamp(date,messageType,MESSAGE_TIME_FORMAT, null, encryption);
           else
               tsText = formatTimeStamp(date,messageType,MESSAGE_DATETIME_FORMAT, null, encryption);

         mHolder.mTextViewForTimestamp.setText(tsText);
         mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);

        }
        else
        {

            mHolder.mTextViewForTimestamp.setText("");
            //mHolder.mTextViewForTimestamp.setVisibility(View.GONE);

        }
        if (linkify)
            LinkifyHelper.addLinks(mHolder.mTextViewForMessages, new URLSpanConverter());
    }

    private void showMediaThumbnail (String mimeType, Uri mediaUri, int id, ViewHolder holder)
    {
        /* Guess the MIME type in case we received a file that we can display or play*/
        if (TextUtils.isEmpty(mimeType) || mimeType.startsWith("application")) {
            String guessed = URLConnection.guessContentTypeFromName(mediaUri.toString());
            if (!TextUtils.isEmpty(guessed)) {
                if (TextUtils.equals(guessed, "video/3gpp"))
                    mimeType = "audio/3gpp";
                else
                    mimeType = guessed;
            }
        }
        holder.setOnClickListenerMediaThumbnail(mimeType, mediaUri);

        holder.mMediaThumbnail.setVisibility(View.VISIBLE);
        holder.mTextViewForMessages.setText(lastMessage);
        holder.mTextViewForMessages.setVisibility(View.GONE);

        if( mimeType.startsWith("image/") ) {
            setImageThumbnail( getContext().getContentResolver(), id, holder, mediaUri );
            holder.mMediaThumbnail.setBackgroundColor(Color.TRANSPARENT);
           // holder.mMediaThumbnail.setBackgroundColor(Color.WHITE);

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
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onClickMediaIcon(String mimeType, Uri mediaUri) {

        if (IocVfs.isVfsUri(mediaUri)) {
            if (mimeType.startsWith("image")) {
                Intent intent = new Intent(context, ImageViewActivity.class);
                intent.putExtra( ImageViewActivity.FILENAME, mediaUri.getPath());
                context.startActivity(intent);
                return;
            }
            if (mimeType.startsWith("audio")) {
                new AudioPlayer(getContext(), mediaUri.getPath(), mimeType).play();
                return;
            }
            return;
        }
        else
        {


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
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 11)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

            //set a general mime type not specific
            intent.setDataAndType(Uri.parse( body ), mimeType);

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
    }

    protected void onLongClickMediaIcon(final String mimeType, final Uri mediaUri) {

        final java.io.File exportPath = IocVfs.exportPath(mimeType, mediaUri);

        new AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.export_media))
        .setMessage(context.getString(R.string.export_media_file_to, exportPath.getAbsolutePath()))
        .setPositiveButton(R.string.export, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    IocVfs.exportContent(mimeType, mediaUri, exportPath);
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exportPath));
                    shareIntent.setType(mimeType);
                    context.startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.export_media)));
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Export Failed " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                return;
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                return;
            }
        })
        .create().show();
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

        setThumbnail(contentResolver, aHolder, mediaUri);


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
                    // set the thumbnail
                    aHolder.mMediaThumbnail.setImageBitmap(result);
                }
            }
        }.execute();
    }

    public final static int THUMBNAIL_SIZE_DEFAULT = 400;

    public static Bitmap getThumbnail(ContentResolver cr, Uri uri) {
     //   Log.e( MessageView.class.getSimpleName(), "getThumbnail uri:" + uri);
        if (IocVfs.isVfsUri(uri)) {
            return IocVfs.getThumbnailVfs(uri, THUMBNAIL_SIZE_DEFAULT);
        }
        return getThumbnailFile(uri, THUMBNAIL_SIZE_DEFAULT);
    }

    public static Bitmap getThumbnailFile(Uri uri, int thumbnailSize) {

        java.io.File image = new java.io.File(uri.getPath());

        if (!image.exists())
        {
            image = new info.guardianproject.iocipher.File(uri.getPath());
            if (!image.exists())
                return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inInputShareable = true;
        options.inPurgeable = true;


        BitmapFactory.decodeFile(image.getPath(), options);
        if ((options.outWidth == -1) || (options.outHeight == -1))
            return null;

        int originalSize = (options.outHeight > options.outWidth) ? options.outHeight
                : options.outWidth;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = originalSize / thumbnailSize;

        Bitmap scaledBitmap = BitmapFactory.decodeFile(image.getPath(), opts);

        return scaledBitmap;
    }

    private String formatMessage (String body)
    {
        if (body != null)
            return android.text.Html.fromHtml(body).toString();
        else
            return null;
    }

    public void bindOutgoingMessage(int id, int messageType, String address, final String mimeType, final String body, Date date, Markup smileyRes, boolean scrolling,
            DeliveryState delivery, EncryptionState encryption) {

        mHolder = (ViewHolder)getTag();

        mHolder.mTextViewForMessages.setVisibility(View.VISIBLE);
        mHolder.resetOnClickListenerMediaThumbnail();
        if( mimeType != null ) {

            lastMessage = "";
            Uri mediaUri = Uri.parse( body ) ;

            showMediaThumbnail(mimeType, mediaUri, id, mHolder);


            mHolder.mTextViewForMessages.setVisibility(View.GONE);
            mHolder.mMediaThumbnail.setVisibility(View.VISIBLE);

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

        if (date != null)
        {

            CharSequence tsText = null;

            if (isSameDay(date,DATE_NOW))
                tsText = formatTimeStamp(date,messageType, MESSAGE_TIME_FORMAT, delivery, encryption);
            else
                tsText = formatTimeStamp(date,messageType, MESSAGE_DATETIME_FORMAT, delivery, encryption);

            mHolder.mTextViewForTimestamp.setText(tsText);

            mHolder.mTextViewForTimestamp.setVisibility(View.VISIBLE);

        }
        else
        {
            mHolder.mTextViewForTimestamp.setText("");

        }
        if (linkify)
            LinkifyHelper.addLinks(mHolder.mTextViewForMessages, new URLSpanConverter());
    }

    private void showAvatar (String address, String nickname, boolean isLeft, int presenceStatus)
    {
        if (mHolder.mAvatar == null)
            return;

        mHolder.mAvatar.setVisibility(View.GONE);

        if (address != null && isLeft)
        {

            RoundedAvatarDrawable avatar = null;

            try { avatar = DatabaseUtils.getAvatarFromAddress(this.getContext().getContentResolver(),address, ImApp.DEFAULT_AVATAR_WIDTH,ImApp.DEFAULT_AVATAR_HEIGHT);}
            catch (Exception e){}

            if (avatar != null)
            {
                mHolder.mAvatar.setVisibility(View.VISIBLE);
                mHolder.mAvatar.setImageDrawable(avatar);

                setAvatarBorder(presenceStatus, avatar);

            }
            else
            {
                int color = getAvatarBorder(presenceStatus);
                int padding = 16;
                LetterAvatar lavatar = new LetterAvatar(getContext(), color, nickname.substring(0,1).toUpperCase(), padding);

                mHolder.mAvatar.setVisibility(View.VISIBLE);
                mHolder.mAvatar.setImageDrawable(lavatar);
            }
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

    private SpannableString formatTimeStamp(Date date, int messageType, DateFormat format, MessageView.DeliveryState delivery, EncryptionState encryptionState) {


        StringBuilder deliveryText = new StringBuilder();
        deliveryText.append(format.format(date));
        deliveryText.append(' ');

        if (delivery != null)
        {
            //this is for delivery
            if (delivery == DeliveryState.DELIVERED) {

                deliveryText.append(DELIVERED_SUCCESS);

            } else if (delivery == DeliveryState.UNDELIVERED) {

                deliveryText.append(DELIVERED_FAIL);

            }

        }

        if (messageType != Imps.MessageType.POSTPONED)
            deliveryText.append(DELIVERED_SUCCESS);//this is for sent, so we know show 2 checks like WhatsApp!

        SpannableString spanText = null;

        if (encryptionState == EncryptionState.ENCRYPTED)
        {
            deliveryText.append('X');
            spanText = new SpannableString(deliveryText.toString());
            int len = spanText.length();

            spanText.setSpan(new ImageSpan(getContext(), R.drawable.lock16), len-1,len,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        }
        else if (encryptionState == EncryptionState.ENCRYPTED_AND_VERIFIED)
        {
            deliveryText.append('X');
            spanText = new SpannableString(deliveryText.toString());
            int len = spanText.length();

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
            avatar.setBorderColor(getResources().getColor(R.color.holo_grey_light));
            avatar.setAlpha(150);
            break;


        default:
        }
    }
}
