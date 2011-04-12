/*
 * Copyright (C) 2009 Myriad Group AG.
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.plugin.ImConfigNames;
import info.guardianproject.otr.app.im.plugin.ImPlugin;
import info.guardianproject.otr.app.im.plugin.ImPluginConstants;
import info.guardianproject.otr.app.im.plugin.ImPluginInfo;
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class ImPluginHelper {

    private static final String TAG = "ImPluginUtils";

    private Context mContext;
    private ArrayList<ImPluginInfo> mPluginsInfo;
    private ArrayList<ImPlugin> mPluginObjects;
    private boolean mLoaded;

    private static ImPluginHelper sInstance;
    public static ImPluginHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ImPluginHelper(context);
        }
        return sInstance;
    }

    private ImPluginHelper(Context context) {
        mContext = context;
        mPluginsInfo = new ArrayList<ImPluginInfo>();
        mPluginObjects = new ArrayList<ImPlugin>();
    }

    public ArrayList<ImPluginInfo> getPluginsInfo() {
        if (!mLoaded) {
            loadAvaiablePlugins();
        }
        return mPluginsInfo;
    }

    public ArrayList<ImPlugin> getPluginObjects() {
        if (!mLoaded) {
            loadAvaiablePlugins();
        }
        return mPluginObjects;
    }

    public void loadAvaiablePlugins() {
        if (mLoaded) {
            return;
        }

        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> plugins = pm.queryIntentServices(
                new Intent(ImPluginConstants.PLUGIN_ACTION_NAME), PackageManager.GET_META_DATA);
        for (ResolveInfo info : plugins) {
            Log.d(TAG, "Found plugin " + info);

            ServiceInfo serviceInfo = info.serviceInfo;
            if (serviceInfo == null) {
                Log.e(TAG, "Ignore bad IM plugin: " + info);
                continue;
            }
            String providerName = null;
            String providerFullName = null;
            String signUpUrl = null;
            Bundle metaData = serviceInfo.metaData;
            if (metaData != null) {
                providerName = metaData.getString(ImPluginConstants.METADATA_PROVIDER_NAME);
                providerFullName = metaData.getString(ImPluginConstants.METADATA_PROVIDER_FULL_NAME);
                signUpUrl = metaData.getString(ImPluginConstants.METADATA_SIGN_UP_URL);
            }
            if (TextUtils.isEmpty(providerName) || TextUtils.isEmpty(providerFullName)) {
                Log.e(TAG, "Ignore bad IM plugin: " + info + ". Lack of required meta data");
                continue;
            }

            if (isPluginDuplicated(providerName)) {
                Log.e(TAG, "Ignore duplicated IM plugin: " + info);
                continue;
            }

            if (!serviceInfo.packageName.equals(mContext.getPackageName())) {
                Log.e(TAG, "Ignore plugin in package: " + serviceInfo.packageName);
                continue;
            }
            ImPluginInfo pluginInfo = new ImPluginInfo(providerName, serviceInfo.packageName,
                    serviceInfo.name, serviceInfo.applicationInfo.sourceDir);

            ImPlugin plugin = loadPlugin(pluginInfo);
            if (plugin == null) {
                Log.e(TAG, "Ignore bad IM plugin");
                continue;
            }

            try {
                updateProviderDb(plugin, pluginInfo,providerFullName, signUpUrl);
            } catch (SQLiteFullException e) {
                Log.e(TAG, "Storage full", e);
                return;
            }
            mPluginsInfo.add(pluginInfo);
            mPluginObjects.add(plugin);
        }
        mLoaded = true;
    }

    private boolean isPluginDuplicated(String providerName) {
        for (ImPluginInfo plugin : mPluginsInfo) {
            if (plugin.mProviderName.equals(providerName)) {
                return true;
            }
        }
        return false;
    }

    private ImPlugin loadPlugin(ImPluginInfo pluginInfo) {
        // XXX Load the plug-in implementation directly from the apk rather than
        // binding to the service and call through IPC Binder API. This is much
        // more effective since we don't need to start the service in other
        // process. We can not run the plug-in service in the same process as a
        // local service because that the interface is defined in a shared
        // library in order to compile the plug-in separately. In this case, the
        // interface will be loaded by two class loader separately and a
        // ClassCastException will be thrown if we cast the binder to the
        // interface.
        ClassLoader loader = mContext.getClassLoader();
        try {
            Class<?> cls = loader.loadClass(pluginInfo.mClassName);
            return (ImPlugin) cls.newInstance();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Could not find plugin class", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Could not create plugin instance", e);
        } catch (InstantiationException e) {
            Log.e(TAG, "Could not create plugin instance", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not load plugin", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Could not load plugin", e);
        }
        return null;
    }

    private long updateProviderDb(ImPlugin plugin, ImPluginInfo info,
            String providerFullName, String signUpUrl) {
        Map<String, String> config = loadConfiguration(plugin, info);
        if (config == null) {
            return 0;
        }

        long providerId = 0;
        ContentResolver cr = mContext.getContentResolver();
        String where = Imps.Provider.NAME + "=?";
        String[] selectionArgs = new String[]{info.mProviderName};
        Cursor c = cr.query(Imps.Provider.CONTENT_URI,
                null /* projection */,
                where,
                selectionArgs,
                null /* sort order */);

        boolean pluginChanged;
        try {
            if (c.moveToFirst()) {
                providerId = c.getLong(c.getColumnIndexOrThrow(Imps.Provider._ID));
                pluginChanged = isPluginChanged(cr, providerId, config);
                if (pluginChanged) {
                    // Update the full name, signup url and category each time when the plugin change
                    // instead of specific version change because this is called only once.
                    // It's ok to update them even the values are not changed.
                    // Note that we don't update the provider name because it's used as
                    // identifier at some place and the plugin should never change it.
                    ContentValues values = new ContentValues(3);
                    values.put(Imps.Provider.FULLNAME, providerFullName);
                    values.put(Imps.Provider.SIGNUP_URL, signUpUrl);
                    values.put(Imps.Provider.CATEGORY, ImApp.IMPS_CATEGORY);
                    Uri uri = ContentUris.withAppendedId(Imps.Provider.CONTENT_URI, providerId);
                    cr.update(uri, values, null, null);
                }
            } else {
                ContentValues values = new ContentValues(3);
                values.put(Imps.Provider.NAME, info.mProviderName);
                values.put(Imps.Provider.FULLNAME, providerFullName);
                values.put(Imps.Provider.CATEGORY, ImApp.IMPS_CATEGORY);
                values.put(Imps.Provider.SIGNUP_URL, signUpUrl);

                Uri result = cr.insert(Imps.Provider.CONTENT_URI, values);
                providerId = ContentUris.parseId(result);
                pluginChanged = true;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        if (pluginChanged) {
            // Remove all the old settings
            cr.delete(ContentUris.withAppendedId(
                    Imps.ProviderSettings.CONTENT_URI, providerId),
                    null, /*where*/
                    null /*selectionArgs*/);

            ContentValues[] settingValues = new ContentValues[config.size()];

            int index = 0;
            for (Map.Entry<String, String> entry : config.entrySet()) {
                ContentValues settingValue = new ContentValues();
                settingValue.put(Imps.ProviderSettings.PROVIDER, providerId);
                settingValue.put(Imps.ProviderSettings.NAME, entry.getKey());
                settingValue.put(Imps.ProviderSettings.VALUE, entry.getValue());
                settingValues[index++] = settingValue;
            }
            cr.bulkInsert(Imps.ProviderSettings.CONTENT_URI, settingValues);
        }

        return providerId;
    }

    private Map<String, String> loadConfiguration(ImPlugin plugin,
            ImPluginInfo info) {
        Map<String, String> config = null;

            config = plugin.getProviderConfig();

        if (config != null) {
            config.put(ImConfigNames.PLUGIN_PATH, info.mSrcPath);
            config.put(ImConfigNames.PLUGIN_CLASS, info.mClassName);
        }
        return config;
    }

    private boolean isPluginChanged(ContentResolver cr, long providerId,
            Map<String, String> config) {
        String origVersion = Imps.ProviderSettings.getStringValue(cr, providerId,
                ImConfigNames.PLUGIN_VERSION);
        String newVersion = config.get(ImConfigNames.PLUGIN_VERSION);
        return (origVersion == null && newVersion != null) || !origVersion.equals(newVersion);
    }
}
