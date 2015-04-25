package info.guardianproject.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

public class AssetUtil {

    /** Read a properties file from /assets.  Returns null if it does not exist. */
    public static Properties getProperties(String name, Context context) {
        Resources resources = context.getResources();
        AssetManager assetManager = resources.getAssets();

        // Read from the /assets directory
        try {
            InputStream inputStream = assetManager.open(name);
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            Log.i("ChatSecure", "no chatsecure.properties available");
            return null;
        }
    }
}
