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

package info.guardianproject.otr.app.im.service;


import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ContactList;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.engine.ImException;

import android.util.Log;

public class ContactListAdapter extends info.guardianproject.otr.app.im.IContactList.Stub {
    private ContactList mAdaptee;
    private long mDataBaseId;

    public ContactListAdapter(ContactList adaptee, long dataBaseId) {
        mAdaptee = adaptee;
        mDataBaseId = dataBaseId;
    }

    public long getDataBaseId() {
        return mDataBaseId;
    }

    public Address getAddress() {
        return mAdaptee.getAddress();
    }

    public int addContact(String address) {
        if (address == null) {
            Log.e(RemoteImService.TAG, "Address can't be null!");
            return ImErrorInfo.ILLEGAL_CONTACT_ADDRESS;
        }

        try {
            mAdaptee.addContact(address);
        } catch (IllegalArgumentException e) {
            return ImErrorInfo.ILLEGAL_CONTACT_ADDRESS;
        } catch (ImException e) {
            return e.getImError().getCode();
        }

        return ImErrorInfo.NO_ERROR;
    }

    public String getName() {
        return mAdaptee.getName();
    }

    public int removeContact(String address) {
        Contact contact = mAdaptee.getContact(address);
        if (contact == null) {
            return ImErrorInfo.ILLEGAL_CONTACT_ADDRESS;
        }

        try {
            mAdaptee.removeContact(contact);
        } catch (ImException e) {
            return e.getImError().getCode();
        }

        return ImErrorInfo.NO_ERROR;
    }

    public void setDefault(boolean isDefault) {
        mAdaptee.setDefault(isDefault);
    }

    public boolean isDefault() {
        return mAdaptee.isDefault();
    }

    public void setName(String name) {
        if (name == null) {
            Log.e(RemoteImService.TAG, "Name can't be null!");
            return;
        }

        mAdaptee.setName(name);
    }
}
