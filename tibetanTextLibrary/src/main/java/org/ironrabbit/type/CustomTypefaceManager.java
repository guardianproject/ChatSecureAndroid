package org.ironrabbit.type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;

public class CustomTypefaceManager {

	private static Typeface mTypeface = null;

	public static Typeface getCurrentTypeface (Context context)
	{
		return mTypeface;
	}
	
	public static void loadFromKeyboard (Context context)
	{
		PackageManager packageManager = context.getPackageManager();
		
		String fontName = "DDC_Uchen.ttf";
		
		try {
			Resources res = packageManager.getResourcesForApplication("org.ironrabbit.bhoboard");						
			InputStream reader = res.getAssets().open(fontName);
			File fileFont = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),fontName);			
			OutputStream writer = new FileOutputStream(fileFont);
			byte[] buffer = new byte[32000];
			int l = 0;
		    while((l = reader.read(buffer)) > 0)
		    {
		    	writer.write(buffer, 0, l);
		    }
		    writer.close();
			
			mTypeface = Typeface.createFromFile(fileFont);

		} catch (Exception e) {
			Log.e("CustomTypeface","can't find assets",e);
		}
	
	}
	
	public static void setTypeface (Typeface typeface)
	{
		mTypeface = typeface;
	}
	
	public static void setTypefaceFromAsset (Context context, String path)
	{
		mTypeface = Typeface.createFromAsset(context.getAssets(), path);
		
	}
	
	public static void setTypefaceFromFile (Context context, String path)
	{    	
		File fileFont = new File(path);
		
		if (fileFont.exists())
			mTypeface = Typeface.createFromFile(fileFont);
	}
	
	public static boolean precomposeRequired ()
	{
		return (android.os.Build.VERSION.SDK_INT < 17);
	}
	
	public static String handlePrecompose (String text)
	{
		if (precomposeRequired ())
			return TibConvert.convertUnicodeToPrecomposedTibetan(text);
		else
			return text;
	}
}
