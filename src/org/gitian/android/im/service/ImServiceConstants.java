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

package org.gitian.android.im.service;

import android.content.ComponentName;

public class ImServiceConstants {
    /**
     * RemoteImService name, used for start or stop the IM service.
     */
    public static final ComponentName IM_SERVICE_COMPONENT = new ComponentName(
            "org.gitian.android.im",
            "org.gitian.android.im.service.RemoteImService");

    /**
     * Broadcast action: broadcast event for avatar changed.
     */
    public static final String ACTION_AVATAR_CHANGED =
            "android.intent.action.IM_AVATAR_CHANGED";

    /**
     * Intent action for managing a subscription request.
     */
    public static final String ACTION_MANAGE_SUBSCRIPTION =
            "android.intent.action.IM_MANAGE_SUBSCRIPTION";

    /**
     * Use EXTRA_INTENT_FROM_ADDRESS to include the from address of a contact in an intent.
     */
    public static final String EXTRA_INTENT_FROM_ADDRESS = "from";

    /**
     * Use EXTRA_INTENT_PROVIDER_ID to include the provider id in an intent.
     */
    public static final String EXTRA_INTENT_PROVIDER_ID = "providerId";

    /**
     * Use EXTRA_INTENT_ACCOUNT_ID to include the account id in an intent.
     */
    public static final String EXTRA_INTENT_ACCOUNT_ID = "accountId";

    /**
     * Use EXTRA_INTENT_LIST_NAME to include the contact list name in an intent.
     */
    public static final String EXTRA_INTENT_LIST_NAME = "listName";

    public static final String EXTRA_CHECK_AUTO_LOGIN = "autologin";

    /**
     * USE EXTRA_INTENT_SHOW_MULTIPLE to inform the activity to show multiple chat notifications
     */
    public static final String EXTRA_INTENT_SHOW_MULTIPLE = "show_multiple";
}
