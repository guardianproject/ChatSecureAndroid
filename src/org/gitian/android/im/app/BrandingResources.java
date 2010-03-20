/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gitian.android.im.app;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.gitian.android.im.plugin.ImPlugin;
import org.gitian.android.im.plugin.ImPluginInfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * The provider specific branding resources.
 */
public class BrandingResources {
    private static final String TAG = ImApp.LOG_TAG;

    private Map<Integer, Integer> mResMapping;
    private Resources mPackageRes;
    private int[] mSmileyIcons;

    private BrandingResources mDefaultRes;

    /**
     * Creates a new BrandingResource of a specific plug-in. The resources will
     * be retrieved from the plug-in package.
     *
     * @param context The current application context.
     * @param pluginInfo The info about the plug-in.
     * @param defaultRes The default branding resources. If the resource is not
     *            found in the plug-in, the default resource will be returned.
     */
    public BrandingResources(Context context, ImPluginInfo pluginInfo,
            BrandingResources defaultRes) {
        mDefaultRes = defaultRes;

        PackageManager pm = context.getPackageManager();
        try {
            mPackageRes = pm.getResourcesForApplication(pluginInfo.mPackageName);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Can not load resources from package: " + pluginInfo.mPackageName);
        }
        // Load the plug-in directly from the apk instead of binding the service
        // and calling through the IPC binder API. It's more effective in this way
        // and we can avoid the async behaviors of binding service.
        ClassLoader classLoader = context.getClassLoader();
        try {
            Class cls = classLoader.loadClass(pluginInfo.mClassName);
            Method m = cls.getMethod("onBind", Intent.class);
            ImPlugin plugin = (ImPlugin)m.invoke(cls.newInstance(), new Object[]{null});
            mResMapping = plugin.getResourceMap();
            mSmileyIcons = plugin.getSmileyIconIds();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Failed load the plugin resource map", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Failed load the plugin resource map", e);
        } catch (InstantiationException e) {
            Log.e(TAG, "Failed load the plugin resource map", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed load the plugin resource map", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Failed load the plugin resource map", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed load the plugin resource map", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Failed load the plugin resource map", e);
        }
    }

    /**
     * Creates a BrandingResource with application context and the resource ID map.
     * The resource will be retrieved from the context directly instead from the plug-in package.
     *
     * @param context
     * @param resMapping
     */
    public BrandingResources(Context context, Map<Integer, Integer> resMapping,
            BrandingResources defaultRes) {
        this(context.getResources(), resMapping, null, defaultRes);
    }

    public BrandingResources(Resources packageRes, Map<Integer, Integer> resMapping,
            int[] smileyIcons, BrandingResources defaultRes) {
        mPackageRes = packageRes;
        mResMapping = resMapping;
        mSmileyIcons = smileyIcons;
        mDefaultRes = defaultRes;
    }

    /**
     * Gets a drawable object associated with a particular resource ID defined
     * in {@link org.gitian.android.im.plugin.BrandingResourceIDs}
     *
     * @param id The ID defined in
     *            {@link org.gitian.android.im.plugin.BrandingResourceIDs}
     * @return Drawable An object that can be used to draw this resource.
     */
    public Drawable getDrawable(int id) {
        int resId = getPackageResourceId(id);
        if (resId != 0) {
            return mPackageRes.getDrawable(resId);
        } else if (mDefaultRes != null){
            return mDefaultRes.getDrawable(id);
        } else {
            return null;
        }
    }

    /**
     * Gets an array of the IDs of the supported smiley of the provider. Use
     * {@link #getSmileyIcon(int)} to get the drawable object of the smiley.
     *
     * @return An array of the IDs of the supported smileys.
     */
    public int[] getSmileyIcons() {
        return mSmileyIcons;
    }

    /**
     * Gets the drawable associated with particular smiley ID.
     *
     * @param smileyId The ID of the smiley returned in
     *            {@link #getSmileyIcons()}
     * @return Drawable An object that can be used to draw this smiley.
     */
    public Drawable getSmileyIcon(int smileyId){
        if (mPackageRes == null) {
            return null;
        }
        return mPackageRes.getDrawable(smileyId);
    }

    /**
     * Gets the string value associated with a particular resource ID defined in
     * {@link org.gitian.android.im.plugin.BrandingResourceIDs}
     *
     * @param id The ID of the string resource defined in
     *            {@link org.gitian.android.im.plugin.BrandingResourceIDs}
     * @param formatArgs The format arguments that will be used for
     *            substitution.
     * @return The string data associated with the resource
     */
    public String getString(int id, Object... formatArgs) {
        int resId = getPackageResourceId(id);
        if (resId != 0) {
            return mPackageRes.getString(resId, formatArgs);
        } else if (mDefaultRes != null){
            return  mDefaultRes.getString(id, formatArgs);
        } else {
            return null;
        }
    }

    /**
     * Gets the string array associated with a particular resource ID defined in
     * {@link org.gitian.android.im.plugin.BrandingResourceIDs}
     *
     * @param id The ID of the string resource defined in
     *            {@link org.gitian.android.im.plugin.BrandingResourceIDs}
     * @return The string array associated with the resource.
     */
    public String[] getStringArray(int id) {
        int resId = getPackageResourceId(id);
        if (resId != 0) {
            return mPackageRes.getStringArray(resId);
        } else if (mDefaultRes != null){
            return mDefaultRes.getStringArray(id);
        } else {
            return null;
        }
    }

    private int getPackageResourceId(int id) {
        if (mResMapping == null || mPackageRes == null) {
            return 0;
        }
        Integer resId = mResMapping.get(id);
        return resId == null ? 0 : resId;
    }

}
