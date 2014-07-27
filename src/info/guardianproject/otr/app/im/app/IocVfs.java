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
    private String dbFile;
    private String root = "/";
    private VirtualFileSystem vfs;
    private static final String BLOB_NAME = "blob.db";
    private static String password;
    
    public IocVfs( Context context ) {
        password = "password";
        dbFile = context.getDir("vfs", Context.MODE_PRIVATE).getAbsolutePath() + "/" + BLOB_NAME;
        Log.e(TAG, "construct:" + dbFile);
    }
    
    public IocVfs() {
        password = "password";
        dbFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + BLOB_NAME;
        Log.e(TAG, "construct:" + dbFile);
    }
    
    public void mount() {
        Log.e(TAG, "mount:" + dbFile);
        vfs = new VirtualFileSystem(dbFile);
        vfs.mount(password);
    }
    
    public void unmount() {
        vfs.unmount();
    }
    
    public void list() {
        File file = new File( "/Download");
        String[] list = file.list();
        Log.e(TAG, "list:" + list.length);
        for (int i = 0 ; i < list.length ; i++) {
            Log.e(TAG, list[i]);
        }
    }
    
    public static void test() {
        String dbFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/myfiles.db";
        VirtualFileSystem vfs = new VirtualFileSystem(dbFile);
        // TODO don't use a hard-coded password! prompt for the password
        vfs.mount("my fake password");
        
        File file = new File("/");
        String[] dirList = file.list();
        Log.e( "VFS", ""+dirList.length);
        
    }

}
