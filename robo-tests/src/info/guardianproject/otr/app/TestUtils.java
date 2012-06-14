package info.guardianproject.otr.app;

import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.app.ImPluginHelper;
import info.guardianproject.otr.app.im.app.ProviderDef;

import java.util.HashMap;

import org.junit.Ignore;

import android.app.Activity;

@Ignore
public class TestUtils {

    public static void setUpApplication(Activity activity) {
        HashMap<Long, ProviderDef> providers = new HashMap<Long, ProviderDef>();
        ImPluginHelper.getInstance(activity).skipLoadingPlugins();
        providers.put(1L, new ProviderDef(1, "XMPP", "XMPP", null));
        ImApp.getApplication(activity).onCreate();
        ImApp.getApplication(activity).setImProviderSettings(providers);

    }

}
