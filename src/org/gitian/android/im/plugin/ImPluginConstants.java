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
package org.gitian.android.im.plugin;

public class ImPluginConstants {

    /**
     * The intent action name for the plugin service.
     */
    public static final String PLUGIN_ACTION_NAME = "org.gitian.android.im.plugin";

    /**
     * The name of the provider. It should match the values defined in
     * {@link org.gitian.android.im.provider.Imps.ProviderNames}.
     */
    public static final String METADATA_PROVIDER_NAME = "org.gitian.android.im.provider_name";

    /**
     * The full name of the provider.
     */
    public static final String METADATA_PROVIDER_FULL_NAME = "org.gitian.android.im.provider_full_name";

    /**
     * The url where the user can register a new account for the provider.
     */
    public static final String METADATA_SIGN_UP_URL = "org.gitian.android.im.signup_url";

    /**
     * Presence status OFFLINE. Should match the value defined in the IM engine.
     */
    public static final int PRESENCE_OFFLINE = 0;

    /**
     * Presence status DO_NOT_DISTURB. Should match the value defined in the IM engine.
     */
    public static final int PRESENCE_DO_NOT_DISTURB = 1;

    /**
     * Presence status AWAY. Should match the value defined in the IM engine.
     */
    public static final int PRESENCE_AWAY = 2;

    /**
     * Presence status IDLE. Should match the value defined in the IM engine.
     */
    public static final int PRESENCE_IDLE = 3;

    /**
     * Presence status AVAILABLE. Should match the value defined in the IM engine.
     */
    public static final int PRESENCE_AVAILABLE = 4;

    public static final String PA_AVAILABLE = "AVAILABLE";
    public static final String PA_NOT_AVAILABLE = "NOT_AVAILABLE";
    public static final String PA_DISCREET = "DISCREET";
}
