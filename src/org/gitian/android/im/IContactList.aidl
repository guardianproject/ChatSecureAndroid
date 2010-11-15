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

package org.gitian.android.im;

import org.gitian.android.im.engine.Contact;

interface IContactList {
    /**
     * Gets the name of the list.
     */
    String getName();

    /**
     * Sets the name of the list.
     */
    void setName(String name);

    /**
     * Adds a new contact to the list.
     */
    int addContact(String address);

    /**
     * Removes a contact in the list.
     */
    int removeContact(String address);

    /**
     * Sets the list to the default list.
     */
    void setDefault(boolean isDefault);

    /**
     * Tells if the list is the default list.
     */
    boolean isDefault();
}
