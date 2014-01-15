/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
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

import info.guardianproject.otr.app.im.ISubscriptionListener;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ContactListManager manages the creating, removing and retrieving contact
 * lists.
 */
public abstract class ContactListManager {
    /**
     * ContactListManager state that indicates the contact list(s) has not been
     * loaded.
     */
    public static final int LISTS_NOT_LOADED = 0;

    /** ContactListManager state that indicates the contact list(s) is loading. */
    public static final int LISTS_LOADING = 1;

    /**
     * ContactListManager state that indicates the blocked list has been loaded.
     */
    public static final int BLOCKED_LIST_LOADED = 2;

    /**
     * ContactListManager state that indicates the contact list(s) has been
     * loaded.
     */
    public static final int LISTS_LOADED = 3;

    protected ContactList mDefaultContactList;
    protected Vector<ContactList> mContactLists;

    protected CopyOnWriteArrayList<ContactListListener> mContactListListeners;
    protected ISubscriptionListener mSubscriptionRequestListener;

    protected Vector<Contact> mBlockedList;

    private int mState;

    /**
     * A pending list of blocking contacts which is used for checking duplicated
     * block operation.
     */
    private Vector<String> mBlockPending;
    /**
     * A pending list of deleting contacts which is used for checking duplicated
     * delete operation.
     */
    private Vector<Contact> mDeletePending;

    /**
     * Creates a new ContactListManager.
     * 
     * @param conn The underlying protocol connection.
     */
    protected ContactListManager() {
        mContactLists = new Vector<ContactList>();
        mContactListListeners = new CopyOnWriteArrayList<ContactListListener>();
        mBlockedList = new Vector<Contact>();

        mBlockPending = new Vector<String>(4);
        mDeletePending = new Vector<Contact>(4);

        mState = LISTS_NOT_LOADED;
    }

    /**
     * Set the state of the ContactListManager
     * 
     * @param state the new state
     */
    protected synchronized void setState(int state) {
        if (state < LISTS_NOT_LOADED || state > LISTS_LOADED) {
            throw new IllegalArgumentException();
        }

        mState = state;
    }

    /**
     * Get the state of the ContactListManager
     * 
     * @return the current state of the manager
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Adds a listener to the manager so that it will be notified for contact
     * list changed.
     * 
     * @param listener the listener to add.
     */
    public synchronized void addContactListListener(ContactListListener listener) {
        if ((listener != null) && !mContactListListeners.contains(listener)) {
            mContactListListeners.add(listener);
        }
    }

    /**
     * Removes a listener from this manager.
     * 
     * @param listener the listener to remove.
     */
    public synchronized void removeContactListListener(ContactListListener listener) {
        mContactListListeners.remove(listener);
    }

    /**
     * Sets the SubscriptionRequestListener to the manager so that it will be
     * notified when a subscription request from another user is received.
     * 
     * @param listener the ContactInvitationListener.
     */
    public synchronized void setSubscriptionRequestListener(ISubscriptionListener listener) {
        mSubscriptionRequestListener = listener;
    }

    public synchronized ISubscriptionListener getSubscriptionRequestListener() {
        return mSubscriptionRequestListener;
    }

    /**
     * Gets a collection of the contact lists.
     * 
     * @return a collection of the contact lists.
     */
    public Collection<ContactList> getContactLists() {
        return Collections.unmodifiableCollection(mContactLists);
    }

    /**
     * Gets a contact by address.
     * 
     * @param address the address of the Contact.
     * @return the Contact or null if the Contact doesn't exist in any list.
     */
    public Contact getContact(Address address) {
        return getContact(address.getAddress());
    }

    public Contact getContact(String address) {
        for (ContactList list : mContactLists) {
            Contact c = list.getContact(normalizeAddress(address));
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    public abstract String normalizeAddress(String address);

    /**
     * Creates a temporary contact. It's usually used when we want to create a
     * chat with someone not in the list.
     * 
     * @param address the address of the temporary contact.
     * @return the created temporary contact
     */
    public abstract Contact[] createTemporaryContacts(String[] addresses);
    
    /**
     * Tell whether the manager contains the specified contact
     * 
     * @param contact the specified contact
     * @return true if the contact is contained in the lists of the manager,
     *         otherwise, return false
     */
    public boolean containsContact(Contact contact) {
        for (ContactList list : mContactLists) {
            if (list.containsContact(contact)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets a contact list by name.
     * 
     * @param name the name of the contact list.
     * @return the ContactList or null if the contact list doesn't exist.
     */
    public ContactList getContactList(String name) {
        for (ContactList list : mContactLists) {
            if (list.getName() != null && list.getName().equals(name)) {
                return list;
            }
        }
        return null;
    }

    /**
     * Get the contact list by the address
     * 
     * @param address the address of the contact list
     * @return the <code>ContactList</code> or null if the list doesn't exist
     */
    public ContactList getContactList(Address address) {
        for (ContactList list : mContactLists) {
            if (list.getAddress().equals(address)) {
                return list;
            }
        }

        return null;
    }

    /**
     * Gets the default contact list.
     * 
     * @return the default contact list.
     * @throws ImException
     */
    public ContactList getDefaultContactList() throws ImException {
        checkState();
        return mDefaultContactList;
    }

    /**
     * Create a contact list with the specified name asynchronously.
     * 
     * @param name the specific name of the contact list
     * @throws ImException
     */
    public void createContactListAsync(String name) throws ImException {
        createContactListAsync(name, null, false);
    }

    /**
     * Create a contact list with specified name and whether it is to be created
     * as the default list.
     * 
     * @param name the specific name of the contact list
     * @param isDefault whether the contact list is to be created as the default
     *            list
     * @throws ImException
     */
    public void createContactListAsync(String name, boolean isDefault) throws ImException {
        createContactListAsync(name, null, isDefault);
    }

    /**
     * Create a contact list with specified name and contacts asynchronously.
     * 
     * @param name the specific name of the contact list
     * @param contacts the initial contacts of the contact list
     * @throws ImException
     */
    public void createContactListAsync(String name, Collection<Contact> contacts)
            throws ImException {
        createContactListAsync(name, contacts, false);
    }

    /**
     * Create a contact list with specified name and contacts asynchronously,
     * and whether it is to be created as the default contact list.
     * 
     * @param name the name of the contact list
     * @param contacts the initial contacts of the list
     * @param isDefault whether the contact list is the default list
     * @throws ImException
     */
    public synchronized void createContactListAsync(String name, Collection<Contact> contacts,
            boolean isDefault) throws ImException {
        checkState();

        if (getContactList(name) != null) {
            throw new ImException(ImErrorInfo.CONTACT_LIST_EXISTS, "Contact list already exists");
        }

        if (mContactLists.isEmpty()) {
            isDefault = true;
        }

        doCreateContactListAsync(name, contacts, isDefault);
    }

    /**
     * Delete a contact list of the specified name asynchronously
     * 
     * @param name the specific name of the contact list
     * @throws ImException
     */
    public void deleteContactListAsync(String name) throws ImException {
        deleteContactListAsync(getContactList(name));
    }

    /**
     * Delete a specified contact list asynchronously
     * 
     * @param list the contact list to be deleted
     * @throws ImException if any error raised
     */
    public synchronized void deleteContactListAsync(ContactList list) throws ImException {
        checkState();

        if (null == list) {
            throw new ImException(ImErrorInfo.CONTACT_LIST_NOT_FOUND, "Contact list doesn't exist");
        }

        doDeleteContactListAsync(list);
    }

    public void blockContactAsync(Contact contact) throws ImException {
        blockContactAsync(contact.getAddress().getAddress());
    }

    /**
     * Blocks a certain Contact. The contact will be removed from any
     * ContactList after be blocked. If the contact has already been blocked,
     * the method does nothing.
     * 
     * @param address the address of the contact to block.
     * @throws ImException if an error occurs
     */
    public void blockContactAsync(String address) throws ImException {
        checkState();

        if (isBlocked(address)) {
            return;
        }

        if (mBlockPending.contains(address)) {
            return;
        }
        doBlockContactAsync(address, true);
    }

    public void unblockContactAsync(Contact contact) throws ImException {
        unblockContactAsync(contact.getAddress().getAddress());
    }

    /**
     * Unblock a certain contact. It will removes the contact from the blocked
     * list and allows the contact to send message or invitation to the client
     * again. If the contact is not blocked on the client, this method does
     * nothing. Whether the unblocked contact will be added to the ContactList
     * it belongs before blocked or not depends on the underlying protocol
     * implementation.
     * 
     * @param address the address of the contact to unblock.
     * @throws ImException if the current state is illegal
     */
    public void unblockContactAsync(String address) throws ImException {
        checkState();

        if (!isBlocked(address)) {
            return;
        }

        doBlockContactAsync(address, false);
    }

    protected void addContactToListAsync(Contact address, ContactList list) throws ImException {
        checkState();

        doAddContactToListAsync(address, list);
    }

    protected void removeContactFromListAsync(Contact contact, ContactList list) throws ImException {
        checkState();

        if (mDeletePending.contains(contact)) {
            return;
        }

        doRemoveContactFromListAsync(contact, list);
    }
    
    /**
     * @param address
     * @param name
     * @return
     * @throws ImException 
     */
    public void setContactName(String address, String name) throws ImException {
        checkState();

        doSetContactName(address,name);
        updateCache(address,name); // used to refresh the display
    }
    
    protected abstract void doSetContactName(String address, String name) throws ImException;
    
    protected void updateCache(String address, String name) {
        // each contact list holds a cache
        for (ContactList list : mContactLists) {
            Contact contact = list.getContact(normalizeAddress(address));
            if (contact != null) {
                // refresh the cache
                contact.setName(name);
                list.insertToCache(contact);
            }
        }
    }
    
    /**
     * Gets a unmodifiable list of blocked contacts.
     * 
     * @return a unmodifiable list of blocked contacts.
     * @throws ImException
     */
    public List<Contact> getBlockedList() throws ImException {
        checkState();

        return Collections.unmodifiableList(mBlockedList);
    }

    /**
     * Checks if a contact is blocked.
     * 
     * @param contact the contact.
     * @return true if it's blocked, false otherwise.
     * @throws ImException if contacts has not been loaded.
     */
    public boolean isBlocked(Contact contact) throws ImException {
        return isBlocked(contact.getAddress().getAddress());
    }

    /**
     * Checks if a contact is blocked.
     * 
     * @param address the address of the contact.
     * @return true if it's blocked, false otherwise.
     * @throws ImException if contacts has not been loaded.
     */
    public synchronized boolean isBlocked(String address) throws ImException {
        if (mState < BLOCKED_LIST_LOADED) {
            // throw new ImException(ImErrorInfo.ILLEGAL_CONTACT_LIST_MANAGER_STATE,
            //   "Blocked list hasn't been loaded");

            for (Contact c : mBlockedList) {
                if (c.getAddress().getAddress().equals(address)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check the state of the ContactListManager. Only the LIST_LOADED state is
     * permitted.
     * 
     * @throws ImException if the current state is not LIST_LOADED
     */
    protected void checkState() throws ImException {
        if (getConnection().getState() != ImConnection.LOGGED_IN) {
            throw new ImException(ImErrorInfo.CANT_CONNECT_TO_SERVER, "Can't connect to server");
        }

        if (getState() != LISTS_LOADED) {
            throw new ImException(ImErrorInfo.ILLEGAL_CONTACT_LIST_MANAGER_STATE,
                    "Illegal contact list manager state");
        }
    }

    /**
     * Load the contact lists from the server. This method will normally called
     * after the user logged in to get the initial/saved contact lists from
     * server. After called once, this method should not be called again.
     */
    public abstract void loadContactListsAsync();

    public abstract void approveSubscriptionRequest(Contact contact);

    public abstract void declineSubscriptionRequest(Contact contact);

    protected abstract ImConnection getConnection();

    /**
     * Block or unblock a contact.
     * 
     * @param address the address of the contact to block or unblock.
     * @param block <code>true</code> to block the contact; <code>false</code>
     *            to unblock the contact.
     */
    protected abstract void doBlockContactAsync(String address, boolean block);

    protected abstract void doCreateContactListAsync(String name, Collection<Contact> contacts,
            boolean isDefault);

    protected abstract void doDeleteContactListAsync(ContactList list);

    /**
     * Notify that the presence of the contact has been updated
     * 
     * @param contacts the contacts who have updated presence information
     */
    public void notifyContactsPresenceUpdated(Contact[] contacts) {
        for (ContactListListener listener : mContactListListeners) {
            listener.onContactsPresenceUpdate(contacts);
        }
    }

    /**
     * Notify that a contact list related error has been raised.
     * 
     * @param type the type of the error
     * @param error the raised error
     * @param listName the list name, if any, associated with the error
     * @param contact the contact, if any, associated with the error
     */
    protected void notifyContactError(int type, ImErrorInfo error, String listName, Contact contact) {
        if (type == ContactListListener.ERROR_REMOVING_CONTACT) {
            mDeletePending.remove(contact);
        } else if (type == ContactListListener.ERROR_BLOCKING_CONTACT) {
            mBlockPending.remove(contact.getAddress().getAddress());
        }
        for (ContactListListener listener : mContactListListeners) {
            listener.onContactError(type, error, listName, contact);
        }
    }

    /**
     * Notify that a contact list has been loaded
     * 
     * @param list the loaded list
     */
    protected void notifyContactListLoaded(ContactList list) {
        for (ContactListListener listener : mContactListListeners) {
            listener.onContactChange(ContactListListener.LIST_LOADED, list, null);

        }
    }

    /** Notify that all contact lists has been loaded */
    protected void notifyContactListsLoaded() {
        setState(LISTS_LOADED);
        for (ContactListListener listener : mContactListListeners) {
            listener.onAllContactListsLoaded();
        }
    }

    /**
     * Notify that a contact has been added to or removed from a list.
     * 
     * @param list the updated contact list
     * @param type the type of the update
     * @param contact the involved contact, null if no contact involved.
     */
    protected void notifyContactListUpdated(ContactList list, int type, Contact contact) {
        synchronized (this) {
            if (type == ContactListListener.LIST_CONTACT_ADDED) {
                list.insertToCache(contact);
            } else if (type == ContactListListener.LIST_CONTACT_REMOVED) {
                list.removeFromCache(contact);
                mDeletePending.remove(contact);
            }
        }

        for (ContactListListener listener : mContactListListeners) {
            listener.onContactChange(type, list, contact);
        }
    }

    /**
     * Notify that the name of the specified contact list has been updated.
     * 
     * @param list
     * @param name the new name of the list
     */
    protected void notifyContactListNameUpdated(ContactList list, String name) {
        list.mName = name;

        for (ContactListListener listener : mContactListListeners) {
            listener.onContactChange(ContactListListener.LIST_RENAMED, list, null);
        }
    }

    /**
     * Notify that a contact list has been created.
     * 
     * @param list the created list
     */
    protected void notifyContactListCreated(ContactList list) {
        synchronized (this) {
            if (list.isDefault()) {
                for (ContactList l : mContactLists) {
                    l.setDefault(false);
                }
                mDefaultContactList = list;
            }
            mContactLists.add(list);
        }

        for (ContactListListener listener : mContactListListeners) {
            listener.onContactChange(ContactListListener.LIST_CREATED, list, null);
        }
    }

    /**
     * Notify that a contact list has been deleted
     * 
     * @param list the deleted list
     */
    protected void notifyContactListDeleted(ContactList list) {
        synchronized (this) {
            mContactLists.remove(list);
            if (list.isDefault() && mContactLists.size() > 0) {
                mContactLists.get(0).setDefault(true);
            }
        }

        for (ContactListListener listener : mContactListListeners) {
            listener.onContactChange(ContactListListener.LIST_DELETED, list, null);
        }
    }

    /**
     * Notify that a contact has been blocked/unblocked.
     * 
     * @param contact the blocked/unblocked contact
     */
    protected void notifyBlockContact(Contact contact, boolean blocked) {
        synchronized (this) {
            if (blocked) {
                mBlockedList.add(contact);
                String addr = contact.getAddress().getAddress();
                mBlockPending.remove(addr);
            } else {
                mBlockedList.remove(contact);
            }
        }
        for (ContactListListener listener : mContactListListeners) {
            listener.onContactChange(blocked ? ContactListListener.CONTACT_BLOCKED
                                            : ContactListListener.CONTACT_UNBLOCKED, null, contact);
        }
    }

    protected abstract void doAddContactToListAsync(Contact contact, ContactList list)
            throws ImException;

    protected abstract void doRemoveContactFromListAsync(Contact contact, ContactList list);

    protected abstract void setListNameAsync(String name, ContactList list);

}
