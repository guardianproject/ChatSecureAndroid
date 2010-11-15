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

package org.gitian.android.im.app.adapter;

import org.gitian.android.im.IContactList;
import org.gitian.android.im.IContactListListener;
import org.gitian.android.im.app.ImApp;
import org.gitian.android.im.app.SimpleAlertHandler;
import org.gitian.android.im.engine.Contact;
import org.gitian.android.im.engine.ImErrorInfo;

import android.util.Log;
public class ContactListListenerAdapter extends IContactListListener.Stub {

    private static final String TAG = ImApp.LOG_TAG;

    private final SimpleAlertHandler mHandler;

    public ContactListListenerAdapter(SimpleAlertHandler handler) {
        mHandler = handler;
    }

    public void onContactChange(int type, IContactList list,
            Contact contact) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onContactListChanged(" + type + ", " + list + ", "
                    + contact + ")");
        }
    }

    public void onAllContactListsLoaded() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onAllContactListsLoaded");
        }
    }

    public void onContactsPresenceUpdate(Contact[] contacts) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onContactsPresenceUpdate(" + contacts.length + ")");
        }
    }

    public void onContactError(int errorType, ImErrorInfo error,
            String listName, Contact contact) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onContactError(" + errorType + ", " + error + ", "
                    + listName + ", " + contact + ")");
        }
        // TODO mHandler.showContactError(errorType, error, listName, contact);
    }
}
