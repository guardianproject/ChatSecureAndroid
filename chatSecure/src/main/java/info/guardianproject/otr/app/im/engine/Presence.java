/*
 * Copyright (C) 2007 Esmertec AG. Copyright (C) 2007 The Android Open Source
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

package info.guardianproject.otr.app.im.engine;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.Map;

/**
 * A <code>Presence</code> is an abstract presentation of the user's presence
 * information.
 *
 * Note that changes made to the Presence data won't be reflected to the server
 * until <code>ImConnection.updateUserPresence</code> is called. Only the logged
 * in user can update its own presence data via
 * <code>ImConnection.updateUserPresence</code>. Changes to any other contact's
 * presence data won't be saved or sent to the server.
 */
public final class Presence implements Parcelable {
    
    public static final int MIN_PRESENCE = 0;
    public static final int OFFLINE = 0;
    public static final int INVISIBLE = 1;    
    public static final int AWAY = 2;
    public static final int IDLE = 3;
    public static final int DO_NOT_DISTURB = 4;
    public static final int AVAILABLE = 5;
    //public static final int NOT_SUBSCRIBED = 5;
    public static final int MAX_PRESENCE = 5;
    
    
    /**
     * 
        int OFFLINE = 0;
        int INVISIBLE = 1;
        int AWAY = 2;
        int IDLE = 3;
        int DO_NOT_DISTURB = 4;
        int AVAILABLE = 5;
     */

    public static final int CLIENT_TYPE_DEFAULT = 0;
    public static final int CLIENT_TYPE_MOBILE = 1;

    private int mStatus;
    private String mStatusText;
    private byte[] mAvatarData;
    private String mAvatarType;
    private int mClientType;
    private String mResource;
    private int mPriority = 0;
    private Map<String, String> mExtendedInfo;

    public Presence() {
        this(Presence.OFFLINE, null, null, null, CLIENT_TYPE_DEFAULT, null, null);
    }

    public Presence(int status, String statusText, int clientType) {
        this(status, statusText, null, null, clientType);
    }

    public Presence(int status, String statusText, byte[] avatarData, String avatarType,
            int clientType) {
        this(status, statusText, avatarData, avatarType, clientType, null, null);
    }

    public Presence(int status, String statusText, byte[] avatarData, String avatarType,
            int clientType, Map<String, String> extendedInfo, String resource) {
        setStatus(status);
        mStatusText = statusText;
        setAvatar(avatarData, avatarType);
        mClientType = clientType;
        mExtendedInfo = extendedInfo;
        mResource = resource;
    }

    public Presence(Presence p) {
        this(p.mStatus, p.mStatusText, p.mAvatarData, p.mAvatarType, p.mClientType, p.mExtendedInfo, p.mResource);
    }

    public Presence(Parcel source) {
        mStatus = source.readInt();
        mStatusText = source.readString();
        mAvatarData = source.createByteArray();
        mAvatarType = source.readString();
        mClientType = source.readInt();
        // TODO - what ClassLoader should be passed to readMap?
        // TODO - switch to Bundle
        mExtendedInfo = source.readHashMap(null);

        //this may not exist for older persisted presence data
        if (source.dataAvail() > 0)
            mResource = source.readString();        
    }

    /**
     * Get avatar bitmap.
     *
     * @return Avatar bitmap. Note any changes made to the bitmap itself won't
     *         be saved or sent back to the server. To change avatar call
     *         <code>setAvatar</code> with a <b>new</b> bitmap instance. FIXME:
     *         Avatar is stored as a byte array and a type string now, it will
     *         be encapsulated with an Object after we change to
     *         ContentProvider.
     */
    public byte[] getAvatarData() {
        if (mAvatarData == null) {
            return null;
        } else {
            byte[] data = new byte[mAvatarData.length];
            System.arraycopy(mAvatarData, 0, data, 0, mAvatarData.length);
            return data;
        }
    }

    /**
     * Get the MIME type of avatar.
     *
     * @return the MIME type of avatar.
     */
    public String getAvatarType() {
        return mAvatarType;
    }

    public int getClientType() {
        return mClientType;
    }

    public Map<String, String> getExtendedInfo() {
        return mExtendedInfo == null ? null : Collections.unmodifiableMap(mExtendedInfo);
    }

    public boolean isOnline() {
        return mStatus != OFFLINE;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        if (status < MIN_PRESENCE || status > MAX_PRESENCE) {
            throw new IllegalArgumentException("invalid presence status value");
        }
        mStatus = status;
    }

    public String getStatusText() {
        return mStatusText;
    }

    public void setAvatar(byte[] data, String type) {
        if (data != null) {
            mAvatarData = new byte[data.length];
            System.arraycopy(data, 0, mAvatarData, 0, data.length);
        } else {
            mAvatarData = null;
        }
        mAvatarType = type;
    }

    public void setStatusText(String statusText) {
        mStatusText = statusText;
    }

    public void setClientType(int clientType) {
        mClientType = clientType;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mStatus);
        dest.writeString(mStatusText);
        dest.writeByteArray(mAvatarData);
        dest.writeString(mAvatarType);
        dest.writeInt(mClientType);
        dest.writeMap(mExtendedInfo);
        dest.writeString(mResource);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Presence> CREATOR = new Parcelable.Creator<Presence>() {
        @Override
        public Presence createFromParcel(Parcel source) {
            return new Presence(source);
        }

        @Override
        public Presence[] newArray(int size) {
            return new Presence[size];
        }
    };

    public String getResource ()
    {
        return mResource;
    }

    public void setResource (String resource)
    {
        mResource = resource;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int mPriority) {
        this.mPriority = mPriority;
    }

    
}
