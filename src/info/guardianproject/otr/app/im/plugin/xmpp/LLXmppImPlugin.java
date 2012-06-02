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
package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.plugin.ImConfigNames;
import info.guardianproject.otr.app.im.plugin.ImPlugin;
import info.guardianproject.otr.app.im.plugin.ImpsConfigNames;

import java.util.HashMap;
import java.util.Map;

import info.guardianproject.otr.app.im.R;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** Simple example of writing a plug-in for the IM application. */
public class LLXmppImPlugin extends Service implements ImPlugin {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** The implementation of IImPlugin defined through AIDL. */
    public Map getProviderConfig() {
        HashMap<String, String> config = new HashMap<String, String>();
        // The protocol name MUST be IMPS now.
        config.put(ImConfigNames.PROTOCOL_NAME, "LLXMPP");
        config.put(ImConfigNames.PLUGIN_VERSION, "0.1");
        config.put(ImpsConfigNames.HOST, "http://xmpp.org/services/");
        config.put(ImpsConfigNames.SUPPORT_USER_DEFINED_PRESENCE, "true");
        config.put(ImpsConfigNames.CUSTOM_PRESENCE_MAPPING,
                "info.guardianproject.otr.app.im.plugin.xmpp.XmppPresenceMapping");
        return config;
    }

    public Map getResourceMap() {
        HashMap<Integer, Integer> resMapping = new HashMap<Integer, Integer>();

        resMapping.put(BrandingResourceIDs.STRING_MENU_VIEW_PROFILE,
                R.string.menu_view_encrypt_chat);

        return resMapping;
    }

    public int[] getSmileyIconIds() {
        return SMILEY_RES_IDS;
    }

    /**
     * An array of the smiley icon IDs. Note that the sequence of the array MUST
     * match the smiley texts and smiley names defined in strings.xml.
     */
    static final int[] SMILEY_RES_IDS = { R.drawable.emo_im_happy, R.drawable.emo_im_sad,
                                         R.drawable.emo_im_winking,
                                         R.drawable.emo_im_tongue_sticking_out,
                                         R.drawable.emo_im_surprised, R.drawable.emo_im_kissing,
                                         R.drawable.emo_im_yelling, R.drawable.emo_im_cool,
                                         R.drawable.emo_im_money_mouth,
                                         R.drawable.emo_im_foot_in_mouth,
                                         R.drawable.emo_im_embarrassed, R.drawable.emo_im_angel,
                                         R.drawable.emo_im_undecided, R.drawable.emo_im_crying,
                                         R.drawable.emo_im_lips_are_sealed,
                                         R.drawable.emo_im_laughing, R.drawable.emo_im_wtf };
}
