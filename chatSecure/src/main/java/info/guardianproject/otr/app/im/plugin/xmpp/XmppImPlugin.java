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
public class XmppImPlugin extends Service implements ImPlugin {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** The implementation of IImPlugin defined through AIDL. */
    public Map getProviderConfig() {
        HashMap<String, String> config = new HashMap<String, String>();
        // The protocol name MUST be IMPS now.
        config.put(ImConfigNames.PROTOCOL_NAME, "XMPP");
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

        /*
        resMapping.put(BrandingResourceIDs.DRAWABLE_LOGO, R.drawable.im_logo);
        resMapping.put(BrandingResourceIDs.DRAWABLE_PRESENCE_ONLINE,
        		android.R.drawable.presence_online);
        resMapping.put(BrandingResourceIDs.DRAWABLE_PRESENCE_AWAY,
        		android.R.drawable.presence_away);
        resMapping.put(BrandingResourceIDs.DRAWABLE_PRESENCE_BUSY,
        		android.R.drawable.presence_busy);
        resMapping.put(BrandingResourceIDs.DRAWABLE_PRESENCE_INVISIBLE,
        		android.R.drawable.presence_invisible);
        resMapping.put(BrandingResourceIDs.DRAWABLE_PRESENCE_OFFLINE,
        		android.R.drawable.presence_offline);
        resMapping.put(BrandingResourceIDs.DRAWABLE_SPLASH_SCREEN,
        		R.drawable.im_logo_splashscr);
        resMapping.put(BrandingResourceIDs.DRAWABLE_READ_CHAT,
        		R.drawable.chat);
        resMapping.put(BrandingResourceIDs.DRAWABLE_UNREAD_CHAT,
        		R.drawable.chat_new);

        resMapping.put(BrandingResourceIDs.STRING_BUDDY_LIST_TITLE,
        		R.string.buddy_list_title);
        resMapping.put(BrandingResourceIDs.STRING_ARRAY_SMILEY_NAMES,
        		R.array.smiley_names);
        resMapping.put(BrandingResourceIDs.STRING_ARRAY_SMILEY_TEXTS,
        		R.array.smiley_texts);
        resMapping.put(BrandingResourceIDs.STRING_PRESENCE_AVAILABLE,
        		R.string.presence_available);

        resMapping.put(BrandingResourceIDs.STRING_LABEL_USERNAME,
        		R.string.label_username);
        resMapping.put(BrandingResourceIDs.STRING_ONGOING_CONVERSATION,
        		R.string.ongoing_conversation);
        resMapping.put(BrandingResourceIDs.STRING_ADD_CONTACT_TITLE,
        		R.string.add_contact_title);
        resMapping.put(BrandingResourceIDs.STRING_LABEL_INPUT_CONTACT,
        		R.string.input_contact_label);
        resMapping.put(BrandingResourceIDs.STRING_BUTTON_ADD_CONTACT,
        		R.string.invite_label);
        resMapping.put(BrandingResourceIDs.STRING_CONTACT_INFO_TITLE,
        		R.string.contact_profile_title);

        resMapping.put(BrandingResourceIDs.STRING_MENU_ADD_CONTACT,
        		R.string.menu_add_contact);
        resMapping.put(BrandingResourceIDs.STRING_MENU_BLOCK_CONTACT,
        		R.string.menu_block_contact);
        resMapping.put(BrandingResourceIDs.STRING_MENU_CONTACT_LIST,
        		R.string.menu_contact_list);
        resMapping.put(BrandingResourceIDs.STRING_MENU_DELETE_CONTACT,
        		R.string.menu_remove_contact);
        resMapping.put(BrandingResourceIDs.STRING_MENU_END_CHAT,
        		R.string.menu_end_conversation);
        resMapping.put(BrandingResourceIDs.STRING_MENU_INSERT_SMILEY,
        		R.string.menu_insert_smiley);
        resMapping.put(BrandingResourceIDs.STRING_MENU_START_CHAT,
        		R.string.menu_start_chat);
        resMapping.put(BrandingResourceIDs.STRING_MENU_VIEW_PROFILE,
        		R.string.menu_view_profile);
        resMapping.put(BrandingResourceIDs.STRING_MENU_SWITCH_CHATS,
        		R.string.menu_switch_chats);

        resMapping.put(BrandingResourceIDs.STRING_TOAST_CHECK_SAVE_PASSWORD,
        		R.string.check_save_password);
        resMapping.put(BrandingResourceIDs.STRING_LABEL_SIGN_UP,
        		R.string.sign_up);
        		*/
        return resMapping;
    }

}
