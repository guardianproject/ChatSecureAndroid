package info.guardianproject.otr.app.im.app;

import java.io.File;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.View;

import com.actionbarsherlock.app.SherlockActivity;

public class ThemeableActivity extends SherlockActivity {

    private static String mThemeBg = null;
    private static Drawable mThemeDrawable = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        ((ImApp)this.getApplication()).setAppTheme(this);
        
        setBackgroundImage (this);
        
        super.onCreate(savedInstanceState);
    }

    public static void setBackgroundImage (Activity activity)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        //int themeId = settings.getInt("theme", R.style.Theme_Gibberbot_Light);
        boolean themeDark = settings.getBoolean("themeDark", false);
        String themebg = settings.getString("pref_background", "");
        
        
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
            
            
            activity.getWindow().setBackgroundDrawable(mThemeDrawable);
        }
        
    }
    
    public static void setBackgroundImage (View view, Activity activity)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        //int themeId = settings.getInt("theme", R.style.Theme_Gibberbot_Light);
        boolean themeDark = settings.getBoolean("themeDark", false);
        String themebg = settings.getString("pref_background", "");
        
        
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
