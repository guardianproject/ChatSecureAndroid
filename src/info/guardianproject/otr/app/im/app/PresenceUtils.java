/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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
package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.engine.Presence;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;

import android.util.Log;

public final class PresenceUtils {
    private PresenceUtils() {}

    public static int convertStatus(int status) {
        switch (status) {
        case Presence.AVAILABLE:
            return Imps.Presence.AVAILABLE;

        case Presence.AWAY:
            return Imps.Presence.AWAY;

        case Presence.DO_NOT_DISTURB:
            return Imps.Presence.DO_NOT_DISTURB;

        case Presence.IDLE:
            return Imps.Presence.IDLE;

        case Presence.OFFLINE:
            return Imps.Presence.OFFLINE;

        default:
            Log.w(ImApp.LOG_TAG, "[ContactView] Unknown presence status " + status);
            return Imps.Presence.AVAILABLE;
        }
    }

    public static int getStatusStringRes(int status) {
        switch (status) {
        case Imps.Presence.AVAILABLE:
            return BrandingResourceIDs.STRING_PRESENCE_AVAILABLE;

        case Imps.Presence.AWAY:
            return BrandingResourceIDs.STRING_PRESENCE_AWAY;

        case Imps.Presence.DO_NOT_DISTURB:
            return BrandingResourceIDs.STRING_PRESENCE_BUSY;

        case Imps.Presence.IDLE:
            return BrandingResourceIDs.STRING_PRESENCE_IDLE;

        case Imps.Presence.INVISIBLE:
            return BrandingResourceIDs.STRING_PRESENCE_INVISIBLE;

        case Imps.Presence.OFFLINE:
            return BrandingResourceIDs.STRING_PRESENCE_OFFLINE;

        default:
            return BrandingResourceIDs.STRING_PRESENCE_AVAILABLE;
        }
    }

    public static int getStatusIconId(int status) {
        switch (status) {
        case Imps.Presence.AVAILABLE:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_ONLINE;

        case Imps.Presence.IDLE:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_AWAY;

        case Imps.Presence.AWAY:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_AWAY;

        case Imps.Presence.DO_NOT_DISTURB:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_BUSY;

        case Imps.Presence.INVISIBLE:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_INVISIBLE;

        default:
            return BrandingResourceIDs.DRAWABLE_PRESENCE_OFFLINE;
        }
    }

}
