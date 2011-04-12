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
package info.guardianproject.otr.app.im.plugin.loopback;

import info.guardianproject.otr.app.im.plugin.ImPluginConstants;
import info.guardianproject.otr.app.im.plugin.PresenceMapping;

import java.util.Map;


/**
 * A simple implementation of PresenceMaping for the provider.
 *
 */
public class LoopbackPresenceMapping implements PresenceMapping {

    public int[] getSupportedPresenceStatus() {
        return new int[] {
                ImPluginConstants.PRESENCE_AVAILABLE,
                ImPluginConstants.PRESENCE_DO_NOT_DISTURB,
                ImPluginConstants.PRESENCE_OFFLINE
        };
    }

    public boolean getOnlineStatus(int status) {
        return status != ImPluginConstants.PRESENCE_OFFLINE;
    }

    public String getUserAvaibility(int status) {
        switch (status) {
            case ImPluginConstants.PRESENCE_AVAILABLE:
                return ImPluginConstants.PA_AVAILABLE;

            case ImPluginConstants.PRESENCE_DO_NOT_DISTURB:
                return ImPluginConstants.PA_DISCREET;

            case ImPluginConstants.PRESENCE_OFFLINE:
                return ImPluginConstants.PA_NOT_AVAILABLE;

            default:
                return null;
        }
    }

    public Map<String, Object> getExtra(int status) {
        // We don't have extra values except OnlineStatus and UserAvaibility
        // need to be sent to the server. If we do need other values to the server,
        // return a map the values structured the same as they are defined in the spec.
        //
        // e.g.
        // Map<String, Object> extra = new HashMap<String, Object>();
        //
        // HashMap<String, Object> commCap = new HashMap<String, Object>();
        //
        // HashMap<String, Object> commC = new HashMap<String, Object>();
        // commC.put("Qualifier", "T");
        // commC.put("Cap", "IM");
        // commC.put("Status", "Open");
        //
        // commCap.put("Qualifier", "T");
        // commCap.put("CommC", commC);
        //
        // extra.put("CommCap", commCap);
        // return extra;
        return null;
    }

    public boolean requireAllPresenceValues() {
        // Return false since we don't need all values received from the server
        // when map it to the predefined presence status.
        return false;
    }

    public int getPresenceStatus(boolean onlineStatus, String userAvailability,
            Map allValues) {
        if (!onlineStatus) {
            return ImPluginConstants.PRESENCE_OFFLINE;
        }
        if (ImPluginConstants.PA_NOT_AVAILABLE.equals(userAvailability)) {
            return ImPluginConstants.PRESENCE_AWAY;
        } else if (ImPluginConstants.PA_DISCREET.equals(userAvailability)) {
            return ImPluginConstants.PRESENCE_DO_NOT_DISTURB;
        } else {
            return ImPluginConstants.PRESENCE_AVAILABLE;
        }
    }

}
