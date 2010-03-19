/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gitian.android.im.imps;

import org.gitian.android.im.engine.ImErrorInfo;

public class ImpsErrorInfo extends ImErrorInfo {

    /** The client error definition* */
    public static final int UNAUTHORIZED = 401;
    public static final int BAD_PARAMETER = 402;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SERVICE_NOT_SUPPORTED = 405;
    public static final int REQUEST_TIMEOUT = 408;
    public static final int INVALID_PASSWORD = 409;
    public static final int UNABLE_TO_DELIVER = 410;
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int INVALID_TRANSACTION_ID = 420;
    public static final int USERID_AND_CLIENTID_NOT_MACTH = 422;
    public static final int INVALID_INVITATIO_ID = 423;
    public static final int INVALID_SEARCH_ID = 424;
    public static final int INVALID_SEARCH_INDEX = 425;
    public static final int INVALID_MESSAGE_ID = 426;
    public static final int UNAUTHORIZED_GROUP_MEMBERSHIP = 431;
    public static final int RESPONSE_TOO_LARGE = 432;

    /** The server error definition* */
    public static final int INTERNAL_SERVER_OR_NETWORK_ERROR = 500;
    public static final int NOT_IMPLMENTED = 501;
    public static final int SERVER_UNAVAILABLE = 503;
    public static final int TIMEOUT = 504;
    public static final int VERSION_NOT_SUPPORTED = 505;
    public static final int SERVICE_NOT_AGREED = 506;
    public static final int MESSAGE_QUEUE_FULL = 507;
    public static final int DOMAIN_NOT_SUPPORTED = 516;
    public static final int UNRESPONDED_PRESENCE_REQUEST = 521;
    public static final int UNRESPONDED_GROUP_REQUEST = 522;
    public static final int UNKNOWN_USER = 531;
    public static final int RECIPIENT_BLOCKED_SENDER = 532;
    public static final int MESSAGE_RECIPIENT_NOT_LOGGED = 533;
    public static final int MESSAGE_RECIPIENT_UNAUTHORIZED = 534;
    public static final int SEARCH_TIMEOUT = 535;
    public static final int TOO_MANY_HITS = 536;
    public static final int TOO_BROAD_SEARCH_CRITERIA = 537;
    public static final int MESSAGE_REJECTED = 538;
    public static final int HEADER_ENCODING_NOT_SUPPORTED = 540;
    public static final int MESSAGE_FORWARDED = 541;
    public static final int MESSAGE_EXPIRED = 542;
    public static final int NO_MATCHING_DIGEST_SCHEME_SUPPORTED = 543;

    /** The session error definition* */
    public static final int SESSION_EXPIRED = 600;
    public static final int FORCED_LOGOUT = 601;
    public static final int ALREADY_LOGGED = 603;
    public static final int INVALID_SESSION = 604;
    public static final int NEW_VALUE_NOT_ACCEPTED = 605;

    /** The presence and contact list error definition* */
    public static final int CONTACT_LIST_NOT_EXIST = 700;
    public static final int CONTACT_LIST_ALREADY_EXISTS = 701;
    public static final int INVALID_OR_UNSUPPORTED_USER_PROPERTIES = 702;
    public static final int INVALID_OR_UNSUPPORTED_PRESENCE_ATTRIBUTE = 750;
    public static final int INVALID_OR_UNSUPPORTED_RRESENCE_VALUE = 751;
    public static final int INVALID_OR_UNSUPPORTED_CONTACT_LIST_PROPERTY = 752;
    public static final int MAX_NUMBER_OF_CONTACT_LIST_REACHED = 753;
    public static final int MAX_NUMBER_OF_CONTACTS_REACHED = 754;
    public static final int MAX_NUMBER_OF_ATTRIBUTE_LISTS_REACHED = 755;
    public static final int AUTOMATIC_SUBSCRIPTION_NOT_SUPPORTED = 760;

    /** The general error definition* */
    public static final int MULTIPLE_ERRORS = 900;
    public static final int GENERAL_ADDRESS_ERROR = 901;
    public static final int NOT_ENOUGH_CREDIT_TO_COMPLETE_REQUESTED_OPERATION = 902;
    public static final int OPERATION_REQUIRES_HIGHER_CLASS_SERVICE = 903;

    public static final int MSISDN_ERROR = 920;    

    ImpsErrorInfo(int code, String description) {
        super(code, description);
    }
    /* TODO private final Primitive mPrimitive;

    ImpsErrorInfo(int code, String description, Primitive primitive) {
        super(code, description);
        mPrimitive = primitive;
    }

    Primitive getPrimitive() {
        return mPrimitive;
    }
	*/
}
