/**
 * 
 */
package info.guardianproject.otr.app.im.app;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.VirtualFileSystem;
import android.content.Context;
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
    }
    
    public static void mount() {
        Log.e(TAG, "mount:" + dbFile);
        vfs.mount(password);
    }
    
    public static void unmount() {
        Log.e(TAG, "unmount:" + dbFile);
        vfs.unmount();
    }
    
    public static void list() {
        File file = new File( "/Download");
        String[] list = file.list();
        Log.e(TAG, "list:" + list.length);
        for (int i = 0 ; i < list.length ; i++) {
            Log.e(TAG, list[i]);
        }
    }
    
}
