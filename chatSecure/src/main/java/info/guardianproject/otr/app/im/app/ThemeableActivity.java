package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.R;

import java.io.File;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.View;

public class ThemeableActivity extends ActionBarActivity {

    private static String mThemeBg = null;
    private static Drawable mThemeDrawable = null;
    
    protected static boolean mHasBackground = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((ImApp)this.getApplication()).setAppTheme(this);

        mHasBackground = setBackgroundImage (this);

        super.onCreate(savedInstanceState);
    }

    public static boolean setBackgroundImage (Activity activity)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean themeDark = settings.getBoolean("themeDark", false);
        String themebg = settings.getString("pref_background", "");

        if (themeDark)
        {

            if (activity != null)
                activity.setTheme(R.style.AppThemeDark);
        }
        else
        {

            if (activity != null)
                activity.setTheme(R.style.AppTheme);
        }


        if (themebg != null && themebg.length() > 0)
        {


            File fileThemeBg = new File(themebg);
            if (!fileThemeBg.exists())
                return false;

            if (mThemeBg == null || (!mThemeBg.equals(themebg)))
            {
                mThemeBg = themebg;

                Display display = activity.getWindowManager().getDefaultDisplay();
                int width = display.getWidth();  // deprecated
                int height = display.getHeight();  // deprecated

                final BitmapFactory.Options options = new BitmapFactory.Options();
                // Calculate inSampleSize
                options.inSampleSize = 4;

                Bitmap b = BitmapFactory.decodeFile(themebg, options);

                float ratio = ((float)width)/((float)height);
                int bgHeight = b.getHeight();
                int bgWidth = (int)(((float)b.getHeight()) * ratio);

                b = Bitmap.createBitmap(b, 0, 0,Math.min(b.getWidth(),bgWidth),bgHeight);

                mThemeDrawable = new BitmapDrawable(b);


            }

            activity.getWindow().setBackgroundDrawable(mThemeDrawable);
            return true;
        }
        
        return false;

    }

    public static void setBackgroundImage (View view, Activity activity)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean themeDark = settings.getBoolean("themeDark", false);
        String themebg = settings.getString("pref_background", "");

        if(themeDark)
            view.setBackgroundColor(activity.getResources().getColor(R.color.background_dark));
        else
            view.setBackgroundColor(activity.getResources().getColor(R.color.background_light));

        if (themebg != null && themebg.length() > 0)
        {

            File fileThemeBg = new File(themebg);
            if (!fileThemeBg.exists())
                return;

            if (mThemeBg == null || (!mThemeBg.equals(themebg)))
            {
                mThemeBg = themebg;

                Display display = activity.getWindowManager().getDefaultDisplay();
                int width = display.getWidth();  // deprecated
                int height = display.getHeight();  // deprecated

                final BitmapFactory.Options options = new BitmapFactory.Options();
                // Calculate inSampleSize
                options.inSampleSize = 4;

                Bitmap b = BitmapFactory.decodeFile(themebg, options);

                float ratio = ((float)width)/((float)height);
                int bgHeight = b.getHeight();
                int bgWidth = (int)(((float)b.getHeight()) * ratio);

                b = Bitmap.createBitmap(b, 0, 0,Math.min(b.getWidth(),bgWidth),bgHeight);

                mThemeDrawable = new BitmapDrawable(b);
                mThemeDrawable.setAlpha(200);
            }

            view.setBackgroundDrawable(mThemeDrawable);
        }

    }

    @Override
    protected void onResume() {
        ((ImApp)this.getApplication()).setAppTheme(this);
        super.onResume();
    }


}
