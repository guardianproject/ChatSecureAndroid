package info.guardianproject.otr.app.im.ui;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.ChatFileStore;
import info.guardianproject.otr.app.im.app.MessageView;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class SecureCameraActivity extends SurfaceGrabberActivity {

    private final static String TAG = SecureCameraActivity.class.getSimpleName();

    public static final String FILENAME = "filename";
    public static final String THUMBNAIL = "thumbnail";
    public static final String MIMETYPE = "mimeType";

    private String filename = null;
    private String thumbnail = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filename = getIntent().getStringExtra(FILENAME);
        thumbnail = getIntent().getStringExtra(THUMBNAIL);
    }

    @Override
    protected int getLayout() {
        return R.layout.secure_camera;
    }

    @Override
    protected int getCameraDirection() {
        //return CameraInfo.CAMERA_FACING_FRONT;
        return CameraInfo.CAMERA_FACING_BACK;
    }

    @Override
    public void onPictureTaken(final byte[] data, Camera camera) {
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(filename)));
            out.write(data);
            out.flush();
            out.close();

            if (thumbnail != null) {
                Bitmap thumbnailBitmap = getThumbnail(getContentResolver(), filename);
                FileOutputStream fos = new FileOutputStream(thumbnail);
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }

            Intent intent = new Intent();
            intent.putExtra(FILENAME, filename);
            intent.putExtra(THUMBNAIL, thumbnail);
            intent.putExtra(MIMETYPE, "image/*");

            setResult(Activity.RESULT_OK, intent);

            finish();
        } catch (Exception e) {
            e.printStackTrace();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
        finish();
    }

    public final static int THUMBNAIL_SIZE = 800;

    public Bitmap getThumbnail(ContentResolver cr, String filename) throws IOException {

        File file = new File(filename);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inInputShareable = true;
        options.inPurgeable = true;

        FileInputStream fis = new FileInputStream(file);
        BitmapFactory.decodeStream(fis, null, options);
        fis.close();

        if ((options.outWidth == -1) || (options.outHeight == -1))
            throw new IOException("Bad image " + file);

        int originalSize = (options.outHeight > options.outWidth) ? options.outHeight : options.outWidth;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = originalSize / THUMBNAIL_SIZE;

        fis = new FileInputStream(file);
        Bitmap scaledBitmap = BitmapFactory.decodeStream(fis, null, opts);
        return scaledBitmap;
    }

}
