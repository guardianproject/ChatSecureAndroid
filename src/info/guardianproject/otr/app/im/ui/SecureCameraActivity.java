package info.guardianproject.otr.app.im.ui;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.otr.app.im.R;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class SecureCameraActivity extends SurfaceGrabberActivity {

    private final static String TAG = SecureCameraActivity.class.getSimpleName();

    public static final String FILENAME = "filename";
    public static final String MIMETYPE = "mimeType";

    private String filename = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filename = getIntent().getStringExtra(FILENAME);
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

            Intent intent = new Intent();
            intent.putExtra(FILENAME, filename);
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
}
