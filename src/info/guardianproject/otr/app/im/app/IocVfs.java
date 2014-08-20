/**
 * 
 */
package info.guardianproject.otr.app.im.app;

import java.io.FileNotFoundException;

import info.guardianproject.cacheword.CacheWordActivityHandler;
import info.guardianproject.cacheword.SQLCipherOpenHelper;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.VirtualFileSystem;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

/**
 * Copyright (C) 2014 Guardian Project.  All rights reserved.
 *
 * @author liorsaar
 *
 */
public class IocVfs {
    public static final String TAG = IocVfs.class.getName();
    private static String dbFile;
    private static VirtualFileSystem vfs;
    private static final String BLOB_NAME = "media.db";
    private static String password;
    
    public static void init(Context context) {
        if (vfs != null) 
            return;
        
        dbFile = context.getExternalFilesDir(null) + "/" + BLOB_NAME;
        vfs = new VirtualFileSystem(dbFile);
       
        if (password != null)
            mount();
        
    }
    
    public static void mount() {
        Log.e(TAG, "mount:" + dbFile);
        vfs.mount(password);
    }
    
    public static void unmount() {
        Log.e(TAG, "unmount:" + dbFile);
        vfs.unmount();
    }
    
    public static void list(String parent) {
        File file = new File(parent);
        String[] list = file.list();
        Log.e(TAG, file.getAbsolutePath());
        for (int i = 0 ; i < list.length ; i++) {
            String fullname = parent + list[i];
            File child = new File(fullname);
            if (child.isDirectory()) {
                list(fullname+"/");
            } else {
                File full = new File(fullname);
                Log.e(TAG, fullname + "  " + full.exists());
            }
        }
    }
    
    private static final String VFS_SCHEME = "vfs";

    public static Uri vfsUri(String filename) {
        return Uri.parse(VFS_SCHEME + ":" + filename);
    }
    
    public static boolean isVfsScheme(String scheme) {
        return VFS_SCHEME.equals(scheme);
    }
    
    public static Bitmap getThumbnailVfs(ContentResolver cr, Uri uri) {
        
        File image = new File(uri.getPath());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inInputShareable = true;
        options.inPurgeable = true;
        
//        BitmapFactory.decodeFile(image.getPath(), options);
        try {
            FileInputStream fis = new FileInputStream(new File(image.getPath()));
            BitmapFactory.decodeStream(fis, null, options);
        } catch (Exception e) {
            Log.e(ImApp.LOG_TAG,"unable to read vfs thumbnail",e);
            return null;
        }

        if ((options.outWidth == -1) || (options.outHeight == -1))
            return null;

        int originalSize = (options.outHeight > options.outWidth) ? options.outHeight
                : options.outWidth;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = originalSize / MessageView.THUMBNAIL_SIZE;

//        Bitmap scaledBitmap = BitmapFactory.decodeFile(image.getPath(), opts);
        try {
            FileInputStream fis = new FileInputStream(new File(image.getPath()));
            Bitmap scaledBitmap = BitmapFactory.decodeStream(fis, null, opts);
            return scaledBitmap;     
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param mCacheWord
     */
    public static void init(Context context, String password) {
        Log.w(TAG, "init with password of length " + password.length());
        if (password.length() > 32)
            password = password.substring(0, 32);
        IocVfs.password = password;
        init(context);
    }
}
