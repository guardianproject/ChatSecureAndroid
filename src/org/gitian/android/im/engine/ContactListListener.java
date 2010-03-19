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

/**
 * Interfaces that allows the implementing classes to listen to contact list
 * relative events. Listeners are registered with ContactListManager.
 */
public interface ContactListListener {
    public static final int LIST_CREATED        = 1;
    public static final int LIST_DELETED        = 2;
    public static final int LIST_LOADED         = 3;
    public static final int LIST_RENAMED        = 4;
    public static final int LIST_CONTACT_ADDED  = 5;
    public static final int LIST_CONTACT_REMOVED = 6;
    public static final int CONTACT_BLOCKED     = 7;
    public static final int CONTACT_UNBLOCKED   = 8;

    public static final int ERROR_CREATING_LIST = -1;
    public static final int ERROR_DELETING_LIST = -2;
    public static final int ERROR_RENAMING_LIST = -3;

    public static final int ERROR_LOADING_LIST          = -4;
    public static final int ERROR_LOADING_BLOCK_LIST    = -5;

    public static final int ERROR_RETRIEVING_PRESENCE   = -6;

    public static final int ERROR_ADDING_CONTACT    = -7;
    public static final int ERROR_REMOVING_CONTACT  = -8;
    public static final int ERROR_BLOCKING_CONTACT  = -9;
    public static final int ERROR_UNBLOCKING_CONTACT = -10;

    /**
     * Called when:
     *  <ul>
     *  <li> a contact list has been created, deleted, renamed or loaded, or
     *  <li> a contact has been added to or removed from a list, or
     *  <li> a contact has been blocked or unblocked
     *  </ul>
     *
     * @param type one of the following values:
     *      <ul>
     *      <li>{@link #LIST_CREATED} list: the newly created list;
     *          contact: null
     *      <li>{@link #LIST_DELETED} list: the delete list; contact: null
     *      <li>{@link #LIST_LOADED}  list: the newly loaded list;
     *          contact: null
     *      <li>{@link #LIST_RENAMED} list: the list renamed; contact: null
     *      <li>{@link #LIST_CONTACT_ADDED}   list: the list to which the
     *          contact is added, contact: the added contact
     *      <li>{@link #LIST_CONTACT_REMOVED} list: the list from which the
     *          contact is removed, contact: the removed contact
     *      <li>{@link #CONTACT_BLOCKED}   list: null, contact: the blocked
     *          contact
     *      <li>{@link #CONTACT_UNBLOCKED} list: null, contact: the unblocked
     *          contact
     *      </ul>
     * @param list
     * @param contact
     */
    public void onContactChange(int type, ContactList list, Contact contact);

    /**
     * Called when all the contact lists (including the block list) and
     * contacts have been loaded from the server.
     */
    public void onAllContactListsLoaded();

    /**
     * Called when received one or more contacts' updated presence
     * information from the server.
     *
     * @param contacts one or more contacts that have updated presence
     *      information.
     */
    public void onContactsPresenceUpdate(Contact[] contacts);

    /**
     *
     * @param errorType one of the following values:
     *      <ul>
     *      <li>{@link #ERROR_CREATING_LIST} listName: the name of the list
     *          to be created; contact: null
     *      <li>{@link #ERROR_DELETING_LIST} listName: the name of the list
     *          to be deleted; contact: null
     *      <li>{@link #ERROR_LOADING_LIST}  listName: the name of the list
     *          to be loaded, or null if the error occurred while fetching
     *          the list of contact lists; contact: null
     *      <li>{@link #ERROR_RENAMING_LIST} listName: the original name of
     *          the list to be renamed; contact: null
     *      <li>{@link #ERROR_LOADING_BLOCK_LIST} list: null; contact: null
     *      <li>{@link #ERROR_RETRIEVING_PRESENCE}
     *          <ul>
     *          <li>when retrieving presence for a list: listName: the name
     *              of the list, or null, depending on the implementation;
     *              contact: null
     *          <li>when retrieving presence for a contact: listName: the
     *              name of the list that the contact belongs to, or null,
     *              depending on the implementation;
     *              contact: the contact
     *          </ul>
     *      <li>{@link #ERROR_ADDING_CONTACT} listName: the name of the list
     *          to which the contact was to be added;
     *          contact: the contact to add
     *      <li>{@link #ERROR_REMOVING_CONTACT} listName: the name of the
     *          list from which the contact was to be removed;
     *          contact: the contact to remove
     *      <li>{@link #ERROR_BLOCKING_CONTACT} list: null;
     *          contact: the contact to block
     *      <li>{@link #ERROR_UNBLOCKING_CONTACT} list: null;
     *          contact: the contact to be unblocked
     *      </ul>
     * @param error
     * @param listName
     * @param contact
     */
    public void onContactError(int errorType, ImErrorInfo error,
            String listName, Contact contact);
}
