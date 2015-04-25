package info.guardianproject.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;

public class BackgroundBitmapLoaderTask extends AsyncTask<Integer, Void, Bitmap> {
    private final Resources resources;
    private final WeakReference<LinearLayout> linearLayoutReference;

    public BackgroundBitmapLoaderTask(Context context, LinearLayout linearLayout) {
        resources = context.getResources();
        // Use a WeakReference to ensure the LinearLayout can be garbage collected
        linearLayoutReference = new WeakReference<LinearLayout>(linearLayout);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
            int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth,
            int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    @Override
    protected Bitmap doInBackground(Integer... resIds) {
        return decodeSampledBitmapFromResource(resources, resIds[0], 100, 100);
    }

    // Once complete, see if LinearLayout is still around and set bitmap.
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (linearLayoutReference != null && bitmap != null) {
            final LinearLayout linearLayout = linearLayoutReference.get();
            if (linearLayout != null) {
                if (Build.VERSION.SDK_INT >= 16)
                    linearLayout.setBackground(new BitmapDrawable(resources, bitmap));
                else
                    linearLayout.setBackgroundDrawable(new BitmapDrawable(resources, bitmap));
            }
        }
    }
}
