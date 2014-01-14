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

public class Contact extends ImEntity implements Parcelable {
    private Address mAddress;
    private String mName;
    private Presence mPresence;

    public Contact(Address address, String name) {
        mAddress = address;
        mName = name;

        mPresence = new Presence();
    }

    public Contact(Parcel source) {
        mAddress = AddressParcelHelper.readFromParcel(source);
        mName = source.readString();
        mPresence = new Presence(source);
    }

    public Address getAddress() {
        return mAddress;
    }

    public String getName() {
        return mName;
    }
    
    public void setName( String aName ) {
        mName = aName;
    }

    public Presence getPresence() {
        return mPresence;
    }

    public boolean equals(Object other) {
        
        return other instanceof Contact && mAddress.getBareAddress().equals(((Contact) other).getAddress().getBareAddress());
    }

    public int hashCode() {
        return mAddress.hashCode();
    }

    /* Set the presence of the Contact. Note that this method is public but not
     * provide to the user.
     * 
     * @param presence the new presence
     */
    public void setPresence(Presence presence) {
        mPresence = presence;
    }

    public void writeToParcel(Parcel dest, int flags) {
        AddressParcelHelper.writeToParcel(dest, mAddress);
        dest.writeString(mName);
        mPresence.writeToParcel(dest, 0);
    }

    public int describeContents() {
        return 0;
    }

    public final static Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
        public Contact createFromParcel(Parcel source) {
            return new Contact(source);
        }

        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
}
