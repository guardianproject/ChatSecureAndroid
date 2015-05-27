/**
 *
 */
package info.guardianproject.otr.app.im.app;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.VirtualFileSystem;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.util.LogCleaner;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * Copyright (C) 2014 Guardian Project.  All rights reserved.
 *
 * @author liorsaar
 *
 */
public class ChatFileStore {
    public static final String TAG = ChatFileStore.class.getName();
    private static String dbFilePath;
    private static final String BLOB_NAME = "media.db";

    public static void unmount() {
        VirtualFileSystem.get().unmount();
    }

    public static void list(String parent) {
        File file = new File(parent);
        String[] list = file.list();
     //  Log.e(TAG, file.getAbsolutePath());
        for (int i = 0 ; i < list.length ; i++) {
            String fullname = parent + list[i];
            File child = new File(fullname);
            if (child.isDirectory()) {
                list(fullname+"/");
            } else {
                File full = new File(fullname);
         //       Log.e(TAG, fullname + " " + full.length());
            }
        }
    }

    public static void deleteSession( String sessionId ) throws IOException {
        String dirName = "/" + sessionId;
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
        //    Log.e(TAG, "delete:" + parent );
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

    public static boolean isVfsUri(Uri uri) {
        return TextUtils.equals(VFS_SCHEME, uri.getScheme());
    }

    public static boolean isVfsUri(String uriString) {
        if (TextUtils.isEmpty(uriString))
            return false;
        else
            return uriString.startsWith(VFS_SCHEME + ":/");
    }

    public static Bitmap getThumbnailVfs(Uri uri, int thumbnailSize) {
        
        if (!VirtualFileSystem.get().isMounted())
            return null;
        
        File image = new File(uri.getPath());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inInputShareable = true;
        options.inPurgeable = true;

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
        opts.inSampleSize = originalSize / thumbnailSize;

        try {
            FileInputStream fis = new FileInputStream(new File(image.getPath()));
            Bitmap scaledBitmap = BitmapFactory.decodeStream(fis, null, opts);
            return scaledBitmap;
        } catch (FileNotFoundException e) {
            LogCleaner.error(ImApp.LOG_TAG, "can't find IOcipher file: " + image.getPath(), e);
            return null;
        }
        catch (OutOfMemoryError oe)
        {
            LogCleaner.error(ImApp.LOG_TAG, "out of memory loading thumbnail: " + image.getPath(), oe);

            return null;
        }
    }

    /**
     * Setup IOCipher VirtualFileSystem without a user-provided password.
     * @param context
     */
    public static void initWithoutPassword(Context context) {
        init(context, null);
    }

    /**
     * Careful! All of the {@code File}s in this method are {@link java.io.File}
     * not {@link info.guardianproject.iocipher.File}s
     *
     * @param context
     * @param key
     * @throws IllegalArgumentException
     */
    public static void init(Context context, byte[] key) throws IllegalArgumentException {
        // there is only one VFS, so if its already mounted, nothing to do
        VirtualFileSystem vfs = VirtualFileSystem.get();
        if (vfs.isMounted()) {
            Log.w(TAG, "VFS " + vfs.getContainerPath() + " is already mounted, skipping init()");
            return;
        }

        /* TODO None of these key/keyText transformations are necessary since IOCipher/SQLCipher
         * will handle long strings and blank strings, but changing it might break existing
         * DBs.  These transformations where gathered from a couple places to be centralized here.
         */
        String keyText;
        if (key == null)
            keyText = "";
        else
            keyText = new String(Hex.encodeHex(key));
        if (keyText.length() > 32)
            keyText = keyText.substring(0, 32);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (settings.getBoolean(
                context.getString(R.string.key_store_media_on_external_storage_pref), false))
            dbFilePath = getExternalDbFilePath(context);
        else
            dbFilePath = getInternalDbFilePath(context);

        if (!new java.io.File(dbFilePath).exists()) {
            vfs.createNewContainer(dbFilePath, keyText);
        }
        vfs.mount(dbFilePath, keyText);
    }

    /**
     * get the external storage path for the chat media file storage file.
     */
    public static String getExternalDbFilePath(Context c) {
        java.io.File externalFilesDir = c.getExternalFilesDir(null);
        if (externalFilesDir == null)
            return null;
        else
            return externalFilesDir.getAbsolutePath() + "/" + BLOB_NAME;
    }

    /**
     * get the internal storage path for the chat media file storage file.
     */
    public static String getInternalDbFilePath(Context c) {
        return c.getFilesDir() + "/" + BLOB_NAME;
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
    public static Uri importContent(String sessionId, String sourcePath) throws IOException {
        list("/");
        File sourceFile = new File(sourcePath);
        String targetPath = "/" + sessionId + "/upload/" + sourceFile.getName();
        targetPath = createUniqueFilename(targetPath);
        copyToVfs( sourcePath, targetPath );
        list("/");
        return vfsUri(targetPath);
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
    public static Uri importContent(String sessionId, String fileName, InputStream sourceStream) throws IOException {
        list("/");
        String targetPath = "/" + sessionId + "/upload/" + fileName;
        targetPath = createUniqueFilename(targetPath);
        copyToVfs( sourceStream, targetPath );
        list("/");
        return vfsUri(targetPath);
    }


    /**
     * Resize an image to an efficient size for sending via OTRDATA, then copy
     * that resized version into vfs. All imported content is stored under
     * /SESSION_NAME/ The original full path is retained to facilitate browsing
     * The session content can be deleted when the session is over
     *
     * @param imagePath
     * @return vfs uri
     * @throws IOException
     */
    public static Uri resizeAndImportImage(Context context, String sessionId, Uri uri, String mimeType)
            throws IOException {
        String imagePath = uri.getPath();
        String targetPath = "/" + sessionId + "/upload/" + imagePath;
        targetPath = createUniqueFilename(targetPath);

        int defaultImageWidth = 600;
        //load lower-res bitmap
        Bitmap bmp = getThumbnailFile(context, uri, defaultImageWidth);
        
        File file = new File(targetPath);
        FileOutputStream out = new FileOutputStream(file);
        
        if (imagePath.endsWith(".png") || mimeType.contains("png")) //preserve alpha channel
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        else
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);

        out.flush();
        out.close();        
        bmp.recycle();        

        return vfsUri(targetPath);
    }

    public static Bitmap getThumbnailFile(Context context, Uri uri, int thumbnailSize) throws IOException {

        InputStream is = context.getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inInputShareable = true;
        options.inPurgeable = true;
        
        BitmapFactory.decodeStream(is, null, options);
        
        if ((options.outWidth == -1) || (options.outHeight == -1))
            return null;

        int originalSize = (options.outHeight > options.outWidth) ? options.outHeight
                : options.outWidth;

        is.close();
        is = context.getContentResolver().openInputStream(uri);
        
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = originalSize / thumbnailSize;

        Bitmap scaledBitmap = BitmapFactory.decodeStream(is, null, opts);

        is.close();
        return scaledBitmap;
    }

    public static void exportAll(String sessionId ) throws IOException {
    }

    public static void exportContent(String mimeType, Uri mediaUri, java.io.File exportPath) throws IOException {
        String sourcePath = mediaUri.getPath();
        copyToExternal( sourcePath, exportPath);
    }

    public static java.io.File exportPath(String mimeType, Uri mediaUri) {
        java.io.File targetFilename;
        if (mimeType.startsWith("image")) {
            targetFilename = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),mediaUri.getLastPathSegment());
        } else if (mimeType.startsWith("audio")) {
            targetFilename = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),mediaUri.getLastPathSegment());
        } else {
            targetFilename = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),mediaUri.getLastPathSegment());
        }
        java.io.File targetUniqueFilename = createUniqueFilenameExternal(targetFilename);
        return targetFilename;
    }

    public static void copyToVfs(String sourcePath, String targetPath) throws IOException {
        // create the target directories tree
        mkdirs( targetPath );
        // copy
        java.io.FileInputStream fis = new java.io.FileInputStream(new java.io.File(sourcePath));
        FileOutputStream fos = new FileOutputStream(new File(targetPath), false);

        IOUtils.copyLarge(fis, fos);

        fos.close();
        fis.close();
    }
    
    public static void copyToVfs(InputStream sourceIS, String targetPath) throws IOException {
        // create the target directories tree
        mkdirs( targetPath );
        // copy
        FileOutputStream fos = new FileOutputStream(new File(targetPath), false);

        IOUtils.copyLarge(sourceIS, fos);

        fos.close();
        sourceIS.close();
    }

    /**
     * Write a {@link byte[]} into an IOCipher File
     * @param sessionId
     * @param buf
     * @return
     * @throws IOException
     */
    public static void copyToVfs(byte buf[], String targetPath) throws IOException {
        File file = new File(targetPath);
        FileOutputStream out = new FileOutputStream(file);
        out.write(buf);
        out.close();
    }
    

    public static void copyToExternal(String sourcePath, java.io.File targetPath) throws IOException {
        // copy
        FileInputStream fis = new FileInputStream(new File(sourcePath));
        java.io.FileOutputStream fos = new java.io.FileOutputStream(targetPath, false);

        IOUtils.copyLarge(fis, fos);

        fos.close();
        fis.close();
    }

    private static void mkdirs(String targetPath) throws IOException {
        File targetFile = new File(targetPath);
        if (!targetFile.exists()) {
            File dirFile = targetFile.getParentFile();
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

    public static boolean sessionExists(String sessionId) {
        return exists( "/" + sessionId );
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
        if (lastDot != -1)
        {
            String name = filename.substring(0,lastDot);
            String ext = filename.substring(lastDot);
            return name + "-" + counter + "." + ext;
        }
        else
        {
            return filename + counter;
        }
    }

    public static String getDownloadFilename(String sessionId, String filenameFromUrl) {
        String filename = "/" + sessionId + "/download/" + filenameFromUrl;
        String uniqueFilename = createUniqueFilename(filename);
        return uniqueFilename;
    }

    private static java.io.File createUniqueFilenameExternal(java.io.File filename ) {
        if (!filename.exists()) {
            return filename;
        }
        int count = 1;
        String uniqueName;
        java.io.File file;
        do {
            uniqueName = formatUnique(filename.getName(), count++);
            file = new java.io.File(filename.getParentFile(),uniqueName);
        } while(file.exists());

        return file;
    }
}
