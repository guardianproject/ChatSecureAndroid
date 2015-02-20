package info.guardianproject.otr.app.im.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.otr.app.im.R;

import java.io.IOException;

public class ImageViewActivity extends Activity {

    public static final String FILENAME = "filename";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view_activity);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final String filename = getIntent().getStringExtra(FILENAME);

        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                display(filename);
            }
        });
    }

    private void display( String filename ) {
        try {
            Bitmap bitmap = fitToScreen(filename);
            PZSImageView imageView = (PZSImageView) findViewById(R.id.pzs_image_view);
            imageView.setImageBitmap(bitmap);
        } catch (Throwable t) { // may run Out Of Memory
            findViewById(R.id.pzs_image_view).setVisibility(View.INVISIBLE);
            findViewById(R.id.pzs_broken_image_view).setVisibility(View.VISIBLE);
        }
    }

    private Bitmap fitToScreen( String filename ) throws IOException {
        // read in dimensions only
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inInputShareable = true;
        options.inPurgeable = true;

        FileInputStream fis = new FileInputStream(new File(filename));
        BitmapFactory.decodeStream(fis, null, options);
        fis.close();

        if ((options.outWidth <= 0) || (options.outHeight <= 0))
            throw new IOException( "Image dimensions unknown");

        // calculate down sampling ratio to fit screen
        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;

        options = new BitmapFactory.Options();
        Point screenDimensions = getScreenDimensions();
        if (imageHeight > imageWidth) {
            options.inSampleSize = imageHeight / screenDimensions.y;
        } else {
            options.inSampleSize = imageWidth / screenDimensions.x;
        }

        // read in downsampled image
        fis = new FileInputStream(new File(filename));
        Bitmap scaledBitmap = BitmapFactory.decodeStream(fis, null, options);
        fis.close();
        return scaledBitmap;
    }

    @SuppressLint("NewApi")
    private Point getScreenDimensions() {
        Display display = getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            return size;
        }
        return new Point( display.getWidth(), display.getHeight());
    }

}
