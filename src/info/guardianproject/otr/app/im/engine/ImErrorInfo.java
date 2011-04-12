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

package info.guardianproject.otr.app.im.engine;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a generic error returned from the server. The IM servers can
 * respond to an error condition with an error code and possibly a description
 * of the problem. Different IM protocol may have different set of error codes
 * and descriptions.
 */
public class ImErrorInfo implements Parcelable, Serializable {
    public static final int NO_ERROR = 0;

    public static final int ILLEGAL_CONTACT_LIST_MANAGER_STATE = -100;
    public static final int CONTACT_LIST_EXISTS = -101;
    public static final int CONTACT_LIST_NOT_FOUND = -102;

    public static final int INVALID_HOST_NAME = -200;
    public static final int UNKNOWN_SERVER = -201;
    public static final int CANT_CONNECT_TO_SERVER = -202;
    public static final int INVALID_USERNAME = -203;
    public static final int INVALID_SESSION_CONTEXT = -204;
    public static final int UNKNOWN_LOGIN_ERROR = -300;
    public static final int NOT_LOGGED_IN = 301;

    public static final int UNSUPPORTED_CIR_CHANNEL = -400;

    public static final int ILLEGAL_CONTACT_ADDRESS = -500;
    public static final int CONTACT_EXISTS_IN_LIST =  -501;
    public static final int CANT_ADD_BLOCKED_CONTACT = -600;

    public static final int PARSER_ERROR = -700;
    public static final int SERIALIZER_ERROR = -750;

    public static final int NETWORK_ERROR = -800;

    public static final int ILLEGAL_SERVER_RESPONSE = -900;

    public static final int UNKNOWN_ERROR = -1000;

    private int mCode;
    private String mDescription;

    /**
     * Creates a new error with specified code and description.
     *
     * @param code the error code.
     * @param description the description of the error.
     */
    public ImErrorInfo(int code, String description) {
        mCode = code;
        mDescription = description;
    }

    public ImErrorInfo(Parcel source) {
        mCode = source.readInt();
        mDescription = source.readString();
    }

    /**
     * Gets the error code.
     *
     * @return the error code.
     */
    public int getCode() {
        return mCode;
    }

    /**
     * Gets the description of the error.
     *
     * @return the description of the error.
     */
    public String getDescription() {
        return mDescription;
    }

    @Override
    public String toString() {
        return mCode + " - " + mDescription;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCode);
        dest.writeString(mDescription);
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ImErrorInfo> CREATOR = new Parcelable.Creator<ImErrorInfo>() {
        public ImErrorInfo createFromParcel(Parcel source) {
            return new ImErrorInfo(source);
        }

        public ImErrorInfo[] newArray(int size) {
            return new ImErrorInfo[size];
        }
    };

}
