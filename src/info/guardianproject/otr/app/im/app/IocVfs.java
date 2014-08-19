/**
 * 
 */
package info.guardianproject.otr.app.im.app;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.VirtualFileSystem;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
    
    private static void init(Context context) {
        if (vfs != null) 
            return;
        
        dbFile = context.getExternalFilesDir(null) + "/" + BLOB_NAME;
        vfs = new VirtualFileSystem(dbFile);
        Log.e(TAG, "init:" + dbFile);
        mount();
        //list("/");
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
                Log.e(TAG, fullname + " " + full.length());
            }
        }
    }
    
    public static void deleteSession( String username ) throws IOException {
        String dirName = strip(username);
        File file = new File(dirName);
        // if the session doesnt have any ul/dl files - bail
        if (!file.exists()) {
            return;
        }
        // delete recursive 
        delete( dirName );
    }
    
    private static void delete(String parentName) throws IOException {
        File parent = new File(parentName);
        // if a file or an empty directory - delete it
        if (!parent.isDirectory()  ||  parent.list().length == 0 ) {
            Log.e(TAG, "delete:" + parent );
            if (!parent.delete()) {
                throw new IOException("Error deleting " + parent);
            }
            return;
        }
        // directory - recurse
        String[] list = parent.list();
        for (int i = 0 ; i < list.length ; i++) {
            String childName = parentName + "/" + list[i];
            delete( childName );
        }
        delete( parentName );
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

    /**
     * Copy device content into vfs. 
     * All imported content is stored under /SESSION_NAME/
     * The original full path is retained to facilitate browsing
     * The session content can be deleted when the session is over 
     * @param sourcePath
     * @return vfs uri
     * @throws IOException 
     */
    public static Uri importContent(String username, String sourcePath) throws IOException {
        list("/");
        File sourceFile = new File(sourcePath);
        String targetPath = "/" + strip(username) + "/upload/" + sourceFile.getName();
        targetPath = createUniqueFilename(targetPath);
        copyToVfs( sourcePath, targetPath );
        list("/");
        return vfsUri(targetPath);
    }
    
    public static void copyToVfs(String sourcePath, String targetPath) throws IOException {
        // create the target directories tree
        mkdirs( targetPath );
        // copy
        java.io.FileInputStream fis = new java.io.FileInputStream(new java.io.File(sourcePath));
        FileOutputStream fos = new FileOutputStream(new File(targetPath), false);
        
        byte[] b = new byte[8*1024];
        int length;

        while ((length = fis.read(b)) != -1) {
            fos.write(b, 0, length);
        }

        fos.close();
        fis.close();
    }
    
    private static void mkdirs(String targetPath) throws IOException {
        File targetFile = new File(targetPath);
        if (!targetFile.exists()) {
            String dirPath = targetFile.getAbsolutePath().substring(0, targetFile.getAbsolutePath().lastIndexOf(File.separator));
            File dirFile = new File(dirPath);
            if (!dirFile.exists()) {
                boolean created = dirFile.mkdirs();
                if (!created) {
                    throw new IOException("Error creating " + targetPath);
                }
            }
        }
    }

    public static boolean exists(String path) {
        return new File(path).exists();
    }
    
    public static boolean userExists(String username) {
        return exists( "/" + strip(username) );
    }
    
    private static String createUniqueFilename( String filename ) {
        if (!exists(filename)) {
            return filename;
        }
        int count = 1;
        String uniqueName;
        File file;
        do {
            uniqueName = formatUnique(filename, count++);
            file = new File(uniqueName);
        } while(file.exists());
        
        return uniqueName;
    }
    
    private static String formatUnique(String filename, int counter) {
        int lastDot = filename.lastIndexOf(".");
        String name = filename.substring(0,lastDot);
        String ext = filename.substring(lastDot);
        return name + "(" + counter + ")" + ext;
    }
    
    public static String strip(String string) {
        return string.replace("@", "_").replace(".", "_");
    }

    public static String getDownloadFilename(String username, String filenameFromUrl) {
        String filename = "/" + strip(username) + "/download/" + filenameFromUrl;
        String uniqueFilename = createUniqueFilename(filename);
        return uniqueFilename;
    }

}
