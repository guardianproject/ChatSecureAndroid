/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

import android.os.Parcel;
import android.os.Parcelable;

public class Invitation implements Parcelable {
    private String mId;
    private Address mGroupAddress;
    private Address mSender;
    private String mReason;

    public Invitation(String id, Address groupAddress, Address sender,
            String resean) {
        mId = id;
        mGroupAddress = groupAddress;
        mSender = sender;
        mReason = resean;
    }

    public Invitation(Parcel source) {
        mId = source.readString();
        mGroupAddress = AddressParcelHelper.readFromParcel(source);
        mSender = AddressParcelHelper.readFromParcel(source);
        mReason = source.readString();
    }

    public String getInviteID() {
        return mId;
    }

    public Address getGroupAddress() {
        return mGroupAddress;
    }

    public Address getSender() {
        return mSender;
    }

    public String getReason() {
        return mReason;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        AddressParcelHelper.writeToParcel(dest, mGroupAddress);
        AddressParcelHelper.writeToParcel(dest, mSender);
        dest.writeString(mReason);
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Invitation> CREATOR = new Parcelable.Creator<Invitation>() {
        public Invitation createFromParcel(Parcel source) {
            return new Invitation(source);
        }

        public Invitation[] newArray(int size) {
            return new Invitation[size];
        }
    };
}
