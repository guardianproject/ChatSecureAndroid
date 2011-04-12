/**
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

/**
 * RingtonePreference subclass to save/restore ringtone value from ImProvider.
 */
public class ImRingtonePreference extends RingtonePreference {
    private long mProviderId;

    public ImRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        Intent intent = ((Activity)context).getIntent();
        mProviderId = intent.getLongExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, -1);
        if (mProviderId < 0) {
            Log.e(ImApp.LOG_TAG,"ImRingtonePreference intent requires provider id extra");
            throw new RuntimeException("ImRingtonePreference must be created with an provider id");
        }
    }

    @Override
    protected Uri onRestoreRingtone() {
        final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                getContext().getContentResolver(), mProviderId, 
                false /* keep updated */, null /* no handler */);
        
        String uri = settings.getRingtoneURI();
        if (Log.isLoggable(ImApp.LOG_TAG, Log.VERBOSE)) {
            Log.v(ImApp.LOG_TAG, "onRestoreRingtone() finds uri=" + uri + " key=" + getKey());
        }

        
        if (TextUtils.isEmpty(uri)) {
            return null;
        }
        
        Uri result = Uri.parse(uri);
        
        settings.close();
        
        return result;
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                getContext().getContentResolver(), mProviderId, 
               false /* keep updated */, null /* no handler */);
        
        // When ringtoneUri is null, that means 'Silent' was chosen
        settings.setRingtoneURI(ringtoneUri == null ? "" : ringtoneUri.toString());
        settings.close();
    }
}

