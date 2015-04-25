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

package info.guardianproject.otr.app.im;

import info.guardianproject.otr.app.im.IContactList;
import info.guardianproject.otr.app.im.IContactListListener;
import info.guardianproject.otr.app.im.ISubscriptionListener;
import info.guardianproject.otr.app.im.engine.Contact;

interface IContactListManager {
    void registerContactListListener(IContactListListener listener);
    void unregisterContactListListener(IContactListListener listener);

    void registerSubscriptionListener(ISubscriptionListener listener);
    void unregisterSubscriptionListener(ISubscriptionListener listener);

    /**
     * Gets all the contact lists of this account.
     */
    List getContactLists();

    /**
     * Gets a contact list with specific name, return null if no contact list is found.
     */
    IContactList getContactList(String name);

    /**
     * Creates a contact list with given name and a list of initial contacts.
     *
     * @param name the name of the list to create.
     * @param contacts a list of contacts will be added to the new contact list, can be null.
     */
    int createContactList(String name, in List<Contact> contacts);

    /**
     * Deletes a contact list with given name.
     *
     * @param name the name of the list to delete.
     */
    int deleteContactList(String name);

    /**
     * Removes a contact from all contact lists. Note the temporary contacts
     * can only be removed by this method.
     *
     * @param address the address of the contact to be removed.
     */
    int removeContact(String address);

    /**
     * Set a contact's nickname 
     *
     * @param address the address of the contact to be updates
     * @param name the new name
     */
    int setContactName(String address, String name);

    /**
     * Approves a subscription request from another user.
     */
    void approveSubscription(in Contact address);

    /**
     * Declines a subscription request from another user.
     */
    void declineSubscription(in Contact address);

    /**
     * Blocks a contact. The ContactListListener will be notified when the contact is blocked
     * successfully or any error occurs.
     *
     * @param address the address of the contact to block.
     * @return ILLEGAL_CONTACT_LIST_MANAGER_STATE if contact lists is not loaded.
     */
    int blockContact(String address);

    /**
     * Unblocks a contact.The ContactListListener will be notified when the contact is blocked
     * successfully or any error occurs.
     *
     * @param address the address of the contact to unblock.
     * @return ILLEGAL_CONTACT_LIST_MANAGER_STATE if contact lists is not loaded.
     */
    int unBlockContact(String address);

    /**
     * Tells if a certain contact is blocked.
     *
     * @param address the address of the contact.
     * @return true if it's blocked; false otherwise.
     */
    boolean isBlocked(String address);

    /**
     * Explicitly load contact lists from the server. The user only needs to call this method if
     * autoLoadContacts is false when login; otherwise, contact lists will be downloaded from the
     * server automatically after login.
     */
    void loadContactLists();

    /**
     * Gets the state of the manager.
     *
     * @return the state of the manager.
     */
    int getState();
}
