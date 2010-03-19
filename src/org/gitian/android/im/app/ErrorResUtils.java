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

import org.gitian.android.im.R;
import org.gitian.android.im.engine.ImErrorInfo;
import org.gitian.android.im.imps.ImpsErrorInfo;

import android.content.res.Resources;

public class ErrorResUtils {

    public static String getErrorRes(Resources res, int code, Object... args) {
        int resId = getErrorResId(code);
        if (resId == 0) {
            return res.getString(R.string.general_error, code);
        } else {
            return res.getString(resId, args);
        }
    }

    private static int getErrorResId(int code) {
        switch (code) {
            case ImErrorInfo.ILLEGAL_CONTACT_LIST_MANAGER_STATE:
                return R.string.contact_not_loaded;

            case ImErrorInfo.CONTACT_EXISTS_IN_LIST:
                return R.string.contact_already_exist;

            case ImErrorInfo.CANT_ADD_BLOCKED_CONTACT:
                return R.string.contact_blocked;

            case ImErrorInfo.CANT_CONNECT_TO_SERVER:
                return R.string.cant_connect_to_server;

            case ImErrorInfo.NETWORK_ERROR:
                return R.string.network_error;

            case ImpsErrorInfo.SERVICE_NOT_SUPPORTED:
                return R.string.service_not_support;

            case ImpsErrorInfo.INVALID_PASSWORD:
                return R.string.invalid_password;

            case ImpsErrorInfo.INTERNAL_SERVER_OR_NETWORK_ERROR:
                return R.string.internal_server_error;

            case ImpsErrorInfo.NOT_IMPLMENTED:
                return R.string.not_implemented;

            case ImpsErrorInfo.SERVER_UNAVAILABLE:
                return R.string.service_unavaiable;

            case ImpsErrorInfo.TIMEOUT:
                return R.string.timeout;

            case ImpsErrorInfo.VERSION_NOT_SUPPORTED:
                return R.string.version_not_supported;

            case ImpsErrorInfo.MESSAGE_QUEUE_FULL:
                return R.string.message_queue_full;

            case ImpsErrorInfo.DOMAIN_NOT_SUPPORTED:
                return R.string.domain_not_supported;

            case ImpsErrorInfo.UNKNOWN_USER:
                return R.string.unknown_user;

            case ImpsErrorInfo.RECIPIENT_BLOCKED_SENDER:
                return R.string.recipient_blocked_the_user;

            case ImpsErrorInfo.SESSION_EXPIRED:
                return R.string.session_expired;

            case ImpsErrorInfo.FORCED_LOGOUT:
                return R.string.forced_logout;

            case ImpsErrorInfo.ALREADY_LOGGED:
                return R.string.already_logged_in;

            case ImErrorInfo.NOT_LOGGED_IN:
                return R.string.not_signed_in;
       
            case ImpsErrorInfo.MSISDN_ERROR:
                return R.string.msisdn_error;

            default:
                return 0;
        }
    }
}
