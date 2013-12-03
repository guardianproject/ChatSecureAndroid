/**
 * 
 */
package info.guardianproject.util;

import info.guardianproject.otr.app.im.R;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * 
 * @author liorsaar
 * 
 */

/*
 * Usage:
 * String filePath = writeFile() ;
 * Uri fileUri = SystemService.Scanner.scan( context, filePath ) ; // scan that one file
 * the notification will launch the target activity with the file uri 
 * SystemServices.Ntfcation.sent( context, fileUri, NewChatActivity.class ) ;
 * in the target activity call:
 * Uri uri = getIntent().getData() ;
 * SystemServices.Viewer.viewImage( context, uri ) ; 
 */
public class SystemServices {
    static class Ntfcation {
        public static void send(Context aContext, Uri aUri, Class<Activity> aTargetActivityClass) {
            NotificationManager mNotificationManager = (NotificationManager)aContext.getSystemService(Context.NOTIFICATION_SERVICE);

            int icon = R.drawable.ic_action_message;
            CharSequence tickerText = "Secured download completed!"; // TODO string
            long when = System.currentTimeMillis();

            Notification notification = new Notification(icon, tickerText, when);
            CharSequence contentTitle = "Gibberbot notification";  // TODO string
            CharSequence contentText = "A secured file was successfuly downloaded.";  // TODO string
            Intent notificationIntent = new Intent(aContext, aTargetActivityClass);
            notificationIntent.setData(aUri); // when the target activity is invoked, extract this uri and call viewImage()
            PendingIntent contentIntent = PendingIntent.getActivity(aContext, 0, notificationIntent, 0);
            notification.setLatestEventInfo(aContext, contentTitle, contentText, contentIntent);
            mNotificationManager.notify(1, notification);
        }
    }

    public static class Scanner {
        // after writing the file to sd, invoke this to scan a single file without callback
        public static Uri scan(Context aContext, String aPath) {
            File file = new File(aPath);
            Uri uri = Uri.fromFile(file);
            Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            aContext.sendBroadcast(scanFileIntent);
            return uri;
        }
    }

    public static class Viewer {
        public static void viewImage(Context aContext, Uri aUri) {
            view(aContext, aUri, "image/*");
        }

        public static void view(Context aContext, Uri aUri, String aMime) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(aUri, aMime);
            aContext.startActivity(intent);
        }

        public static Intent getViewIntent(Uri uri, String type) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, type);
            return intent;
        }
    }

    public static String sanitize(String path) {
        try {
            return URLEncoder.encode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class FileInfo {
        public String path;
        public String type;
    };
    
    public static FileInfo getFileInfoFromURI(Context aContext, Uri uri) throws IllegalArgumentException {
        FileInfo info = new FileInfo();
        if (uri.getScheme() != null && uri.getScheme().equals("file")) {
            info.path = uri.getPath();
            return info;
        }
        
        if (uri.toString().startsWith("content://org.openintents.filemanager/")) {
            // Work around URI escaping brokenness
            info.path = uri.toString().replaceFirst("content://org.openintents.filemanager", "");
            return info;
        }
        
        Cursor cursor = aContext.getContentResolver().query(uri, null, null, null, null);
        
        if (cursor != null)
        {
            cursor.moveToFirst();
            
            //need to check columns for different types
            int dataIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            if (dataIdx != -1) 
            {
                info.path = cursor.getString(dataIdx);
                info.type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
            
                return info;
            }
            
            if (dataIdx == -1)
                dataIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
            
            if (dataIdx != -1)
            {
                info.path = cursor.getString(dataIdx);
                info.type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
            
                return info;
            }
            
            if (dataIdx == -1)
                dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            
            if (dataIdx != -1)
            {
                info.path = cursor.getString(dataIdx);
                info.type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
            
                return info;
            }
            
            if (dataIdx == -1)
                dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            
            if (dataIdx != -1)
            {
                info.path = cursor.getString(dataIdx);
                info.type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE));
            
                return info;
            }
            
         
        }
        
        return null;
    }
}
