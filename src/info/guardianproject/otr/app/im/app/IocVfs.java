/**
 * 
 */
package info.guardianproject.otr.app.im.app;

import java.io.FileNotFoundException;

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
    private static final String BLOB_NAME = "blob.db";
    private static String password;
    
    public static void init( Context context ) {
        password = "password";
        dbFile = context.getDir("vfs", Context.MODE_PRIVATE).getAbsolutePath() + "/" + BLOB_NAME;
        Log.e(TAG, "init:" + dbFile);
    }
    
    public static void init() {
        if (vfs != null) 
            return;
        
        password = "password";
        dbFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + BLOB_NAME;
        vfs = new VirtualFileSystem(dbFile);
        Log.e(TAG, "init:" + dbFile);
        mount();
        
        list("/");
        
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
                Log.e(TAG, fullname);
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
        
        IocVfs.init();
        
        File image = new File(uri.getPath());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inInputShareable = true;
        options.inPurgeable = true;
        
//        BitmapFactory.decodeFile(image.getPath(), options);
        try {
            FileInputStream fis = new FileInputStream(new File(image.getPath()));
            BitmapFactory.decodeStream(fis, null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
}
