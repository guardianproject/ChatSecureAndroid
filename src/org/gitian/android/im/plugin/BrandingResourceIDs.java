/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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
package org.gitian.android.im.plugin;

/**
 * Defines the IDs of branding resources.
 *
 */
public interface BrandingResourceIDs {

    /**
     * The logo icon of the provider which is displayed in the landing page.
     */
    public static final int DRAWABLE_LOGO                = 100;
    /**
     * The icon of online presence status.
     */
    public static final int DRAWABLE_PRESENCE_ONLINE     = 102;
    /**
     * The icon of busy presence status.
     */
    public static final int DRAWABLE_PRESENCE_BUSY       = 103;
    /**
     * The icon of away presence status.
     */
    public static final int DRAWABLE_PRESENCE_AWAY       = 104;
    /**
     * The icon of invisible presence status.
     */
    public static final int DRAWABLE_PRESENCE_INVISIBLE  = 105;
    /**
     * The icon of offline presence status.
     */
    public static final int DRAWABLE_PRESENCE_OFFLINE    = 106;
    /**
     * The label of the menu to go to the contact list screen.
     */
    public static final int STRING_MENU_CONTACT_LIST     = 107;

    /**
     * The image displayed on the splash screen while logging in.
     */
    public static final int DRAWABLE_SPLASH_SCREEN       = 200;
    /**
     * The icon for blocked contacts.
     */
    public static final int DRAWABLE_BLOCK               = 201;
    /**
     * The water mark background for chat screen.
     */
    public static final int DRAWABLE_CHAT_WATERMARK      = 202;
    /**
     * The icon for the read conversation.
     */
    public static final int DRAWABLE_READ_CHAT           = 203;
    /**
     * The icon for the unread conversation.
     */
    public static final int DRAWABLE_UNREAD_CHAT         = 204;

    /**
     * The title of buddy list screen. It's conjuncted with the current username
     * and should be formatted as a string like
     * "Contact List - &lt;xliff:g id="username"&gt;%1$s&lt;/xliff:g&gt;
     */
    public static final int STRING_BUDDY_LIST_TITLE      = 301;

    /**
     * A string array of the smiley names.
     */
    public static final int STRING_ARRAY_SMILEY_NAMES    = 302;
    /**
     * A string array of the smiley texts.
     */
    public static final int STRING_ARRAY_SMILEY_TEXTS    = 303;

    /**
     * The string of available presence status.
     */
    public static final int STRING_PRESENCE_AVAILABLE    = 304;
    /**
     * The string of away presence status.
     */
    public static final int STRING_PRESENCE_AWAY         = 305;
    /**
     * The string of busy presence status.
     */
    public static final int STRING_PRESENCE_BUSY         = 306;
    /**
     * The string of the idle presence status.
     */
    public static final int STRING_PRESENCE_IDLE         = 307;
    /**
     * The string of the invisible presence status.
     */
    public static final int STRING_PRESENCE_INVISIBLE    = 308;
    /**
     * The string of the offline presence status.
     */
    public static final int STRING_PRESENCE_OFFLINE      = 309;

    /**
     * The label of username displayed on the account setup screen.
     */
    public static final int STRING_LABEL_USERNAME        = 310;
    /**
     * The label of the ongoing conversation group.
     */
    public static final int STRING_ONGOING_CONVERSATION  = 311;
    /**
     * The title of add contact screen.
     */
    public static final int STRING_ADD_CONTACT_TITLE     = 312;
    /**
     * The label of the contact input box on the add contact screen.
     */
    public static final int STRING_LABEL_INPUT_CONTACT   = 313;
    /**
     * The label of the add contact button on the add contact screen
     */
    public static final int STRING_BUTTON_ADD_CONTACT    = 314;
    /**
     * The title of the contact info dialog.
     */
    public static final int STRING_CONTACT_INFO_TITLE    = 315;
    /**
     * The label of the menu to add a contact.
     */
    public static final int STRING_MENU_ADD_CONTACT      = 316;
    /**
     * The label of the menu to start a conversation.
     */
    public static final int STRING_MENU_START_CHAT       = 317;
    /**
     * The label of the menu to view contact profile info.
     */
    public static final int STRING_MENU_VIEW_PROFILE     = 318;
    /**
     * The label of the menu to end a conversation.
     */
    public static final int STRING_MENU_END_CHAT         = 319;
    /**
     * The label of the menu to block a contact.
     */
    public static final int STRING_MENU_BLOCK_CONTACT    = 320;
    /**
     * The label of the menu to delete a contact.
     */
    public static final int STRING_MENU_DELETE_CONTACT   = 321;
    /**
     * The label of the menu to insert a smiley.
     */
    public static final int STRING_MENU_INSERT_SMILEY    = 322;
    /**
     * The label of the menu to switch conversations.
     */
    public static final int STRING_MENU_SWITCH_CHATS     = 323;
    /**
     * The string of the toast displayed when auto sign in button on the account
     * setup screen is checked.
     */
    public static final int STRING_TOAST_CHECK_AUTO_SIGN_IN  = 324;
    /**
     * The string of the toast displayed when the remember password button on
     * the account setup screen is checked.
     */
    public static final int STRING_TOAST_CHECK_SAVE_PASSWORD = 325;
    /**
     * The label of sign up a new account on the account setup screen.
     */
    public static final int STRING_LABEL_SIGN_UP         = 326;
    /**
     * The term of use message. If provided, a dialog will be shown at the first
     * time login to ask the user if he would accept the term or not.
     */
    public static final int STRING_TOU_MESSAGE           = 327;
    /**
     * The title of the term of use dialog.
     */
    public static final int STRING_TOU_TITLE             = 328;
    /**
     * The label of the button to accept the term of use.
     */
    public static final int STRING_TOU_ACCEPT            = 329;
    /**
     * The label of the button to decline the term of use.
     */
    public static final int STRING_TOU_DECLINE           = 330;
}
