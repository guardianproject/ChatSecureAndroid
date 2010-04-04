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

package org.gitian.android.im.engine;

import org.gitian.android.im.plugin.xmpp.XmppConnection;

import android.os.Parcel;
import android.util.Log;

/**
 * A helper for marshalling and unmarshaling an Address Object to a Parcel.
 * The Address is only an abstract representation, the underlying protocol
 * implementation MUST provide a public default constructor and register
 * their implementing class here.
 */
public class AddressParcelHelper {
    private static Class[] sAddressClasses = new Class[] {
        // TODO ImpsUserAddress.class,
        //ImpsGroupAddress.class,
        //ImpsContactListAddress.class,
    	XmppConnection.XmppAddress.class
    };

    private AddressParcelHelper() {
    }

    public static Address readFromParcel(Parcel source) {
        int classIndex = source.readInt();
        if(classIndex == -1) {
            return null;
        }
        if(classIndex < 0 || classIndex >= sAddressClasses.length) {
            throw new RuntimeException("Unknown Address type index: " + classIndex);
        }
        try {
            Address address = (Address)sAddressClasses[classIndex].newInstance();
            address.readFromParcel(source);
            return address;
        } catch (InstantiationException e) {
            Log.e("AddressParcel", "Default constructor are required on Class"
                    + sAddressClasses[classIndex].getName());
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            Log.e("AddressParcel", "Default constructor are required on Class"
                    + sAddressClasses[classIndex].getName());
            throw new RuntimeException(e);
        }
    }

    public static void writeToParcel(Parcel dest, Address address) {
        if(address == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(getClassIndex(address));
            address.writeToParcel(dest);
        }
    }

    private static int getClassIndex(Address address) {
        for(int i = 0; i < sAddressClasses.length; i++) {
            if(address.getClass() == sAddressClasses[i]) {
                return i;
            }
        }
        throw new RuntimeException("Unregistered Address type: " + address);
    }
}
