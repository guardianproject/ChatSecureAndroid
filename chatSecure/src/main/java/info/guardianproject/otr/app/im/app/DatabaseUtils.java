/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
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
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.ui.RoundedAvatarDrawable;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

public class DatabaseUtils {

    private static final String TAG = ImApp.LOG_TAG;

    private DatabaseUtils() {
    }

    public static Cursor queryAccountsForProvider(ContentResolver cr, String[] projection,
            long providerId) {
        StringBuilder where = new StringBuilder(Imps.Account.ACTIVE);
        where.append("=1 AND ").append(Imps.Account.PROVIDER).append('=').append(providerId);
        Cursor c = cr.query(Imps.Account.CONTENT_URI, projection, where.toString(), null, null);
        if (c != null && !c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }

    public static RoundedAvatarDrawable getAvatarFromCursor(Cursor cursor, int dataColumn, int width, int height) throws DecoderException {
        String hexData = cursor.getString(dataColumn);
        if (hexData.equals("NULL")) {
            return null;
        }

        byte[] data = Hex.decodeHex(hexData.substring(2, hexData.length() - 1).toCharArray());
        return decodeAvatar(data, width, height);
    }

    public static RoundedAvatarDrawable getAvatarFromAddress(ContentResolver cr, String address, int width, int height) throws DecoderException {

        String[] projection =  {Imps.Contacts.AVATAR_DATA};
        String[] args = {address};
        String query = Imps.Contacts.USERNAME + " LIKE ?";
        Cursor cursor = cr.query(Imps.Contacts.CONTENT_URI,projection,
             query, args, Imps.Contacts.DEFAULT_SORT_ORDER);

        if (cursor.moveToFirst())
        {
            String hexData = cursor.getString(0);
            cursor.close();
            if (hexData.equals("NULL")) {
                return null;
            }

            byte[] data = Hex.decodeHex(hexData.substring(2, hexData.length() - 1).toCharArray());

            return decodeAvatar(data, width, height);
        }
        else
        {

            cursor.close();
            return null;
        }
    }


    public static Uri getAvatarUri(Uri baseUri, long providerId, long accountId) {
        Uri.Builder builder = baseUri.buildUpon();
        ContentUris.appendId(builder, providerId);
        ContentUris.appendId(builder, accountId);
        return builder.build();
    }

    public static void updateAvatarBlob(ContentResolver resolver, Uri updateUri, byte[] data,
            String username) {
        ContentValues values = new ContentValues(3);
        values.put(Imps.Avatars.DATA, data);

        StringBuilder buf = new StringBuilder(Imps.Avatars.CONTACT);
        buf.append("=?");

        String[] selectionArgs = new String[] { username };

        resolver.update(updateUri, values, buf.toString(), selectionArgs);

    }

    public static boolean hasAvatarContact(ContentResolver resolver, Uri updateUri,
            String username) {
        ContentValues values = new ContentValues(3);
        values.put(Imps.Avatars.CONTACT, username);

        StringBuilder buf = new StringBuilder(Imps.Avatars.CONTACT);
        buf.append("=?");

        String[] selectionArgs = new String[] { username };

        return resolver.update(updateUri, values, buf.toString(), selectionArgs) > 0;

    }

    public static boolean doesAvatarHashExist(ContentResolver resolver, Uri queryUri,
            String jid, String hash) {

        StringBuilder buf = new StringBuilder(Imps.Avatars.CONTACT);
        buf.append("=?");
        buf.append(" AND ");
        buf.append(Imps.Avatars.HASH);
        buf.append("=?");

        String[] selectionArgs = new String[] { jid, hash };

        Cursor cursor = resolver.query(queryUri, null, buf.toString(), selectionArgs, null);
        if (cursor == null)
            return false;
        try {
            return cursor.getCount() > 0;
        } finally {
            cursor.close();
        }
    }

    public static void insertAvatarBlob(ContentResolver resolver, Uri updateUri, long providerId, long accountId, byte[] data, String hash,
            String contact) {

        ContentValues values = new ContentValues(3);
        values.put(Imps.Avatars.DATA, data);
        values.put(Imps.Avatars.CONTACT, contact);
        values.put(Imps.Avatars.PROVIDER, providerId);
        values.put(Imps.Avatars.ACCOUNT, accountId);
        values.put(Imps.Avatars.HASH, hash);
        resolver.insert(updateUri, values);
        
        

    }


    private static RoundedAvatarDrawable decodeAvatar(byte[] data, int width, int height) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length,options);
        options.inSampleSize = calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length,options);
        if (b != null)
        {
            RoundedAvatarDrawable avatar = new RoundedAvatarDrawable(b);
            return avatar;
        }
        else
            return null;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        // Calculate ratios of height and width to requested height and width
        final int heightRatio = Math.round((float) height / (float) reqHeight);
        final int widthRatio = Math.round((float) width / (float) reqWidth);

        // Choose the smallest ratio as inSampleSize value, this will guarantee
        // a final image with both dimensions larger than or equal to the
        // requested height and width.
        inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
    }

    return inSampleSize;
}

    /**
     * Update IM provider database for a plugin using newly loaded information.
     *
     * @param cr the resolver
     * @param providerName the plugin provider name
     * @param providerFullName the full name
     * @param signUpUrl the plugin's service signup URL
     * @param config the plugin's settings
     * @return the provider ID of the plugin
     */
    public static long updateProviderDb(ContentResolver cr, String providerName,
            String providerFullName, String signUpUrl, Map<String, String> config) {
        boolean versionChanged;

        // query provider data
        long providerId = Imps.Provider.getProviderIdForName(cr, providerName);
        if (providerId > 0) {
            // already loaded, check if version changed
            String pluginVersion = config.get(ImConfigNames.PLUGIN_VERSION);
            if (!isPluginVersionChanged(cr, providerId, pluginVersion)) {
                // no change, just return
                return providerId;
            }
            // changed, update provider meta data
            updateProviderRow(cr, providerId, providerFullName, signUpUrl);
            // clear branding resource map cache
            clearBrandingResourceMapCache(cr, providerId);

            Log.d(TAG, "Plugin " + providerName + "(" + providerId
                       + ") has a version change. Database updated.");
        } else {
            // new plugin, not loaded before, insert the provider data
            providerId = insertProviderRow(cr, providerName, providerFullName, signUpUrl);

            Log.d(TAG, "Plugin " + providerName + "(" + providerId
                       + ") is new. Provider added to IM db.");
        }

        // plugin provider has been inserted/updated, we need to update settings
        saveProviderSettings(cr, providerId, config);

        return providerId;
    }

    /** Clear the branding resource map cache. */
    private static int clearBrandingResourceMapCache(ContentResolver cr, long providerId) {
        StringBuilder where = new StringBuilder();
        where.append(Imps.BrandingResourceMapCache.PROVIDER_ID);
        where.append('=');
        where.append(providerId);
        return cr.delete(Imps.BrandingResourceMapCache.CONTENT_URI, where.toString(), null);
    }

    /** Insert the plugin settings into the database. */
    private static int saveProviderSettings(ContentResolver cr, long providerId,
            Map<String, String> config) {
        ContentValues[] settingValues = new ContentValues[config.size()];
        int index = 0;
        for (Map.Entry<String, String> entry : config.entrySet()) {
            ContentValues settingValue = new ContentValues();
            settingValue.put(Imps.ProviderSettings.PROVIDER, providerId);
            settingValue.put(Imps.ProviderSettings.NAME, entry.getKey());
            settingValue.put(Imps.ProviderSettings.VALUE, entry.getValue());
            settingValues[index++] = settingValue;
        }
        return cr.bulkInsert(Imps.ProviderSettings.CONTENT_URI, settingValues);
    }

    /** Insert a new plugin provider to the provider table. */
    private static long insertProviderRow(ContentResolver cr, String providerName,
            String providerFullName, String signUpUrl) {
        ContentValues values = new ContentValues(3);
        values.put(Imps.Provider.NAME, providerName);
        values.put(Imps.Provider.FULLNAME, providerFullName);
        values.put(Imps.Provider.CATEGORY, ImApp.IMPS_CATEGORY);
        values.put(Imps.Provider.SIGNUP_URL, signUpUrl);
        Uri result = cr.insert(Imps.Provider.CONTENT_URI, values);
        return ContentUris.parseId(result);
    }

    /** Update the data of a plugin provider. */
    private static int updateProviderRow(ContentResolver cr, long providerId,
            String providerFullName, String signUpUrl) {
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
        return cr.update(uri, values, null, null);
    }

    /**
     * Compare the saved version of a plugin provider with the newly loaded
     * version.
     */
    private static boolean isPluginVersionChanged(ContentResolver cr, long providerId,
            String newVersion) {
        String oldVersion = Imps.ProviderSettings.getStringValue(cr, providerId,
                ImConfigNames.PLUGIN_VERSION);
        if (oldVersion == null) {
            return true;
        }
        return !oldVersion.equals(newVersion);
    }
}
