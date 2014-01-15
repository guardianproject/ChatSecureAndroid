/*
 * Copyright (C) 2007 The Android Open Source Project
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

package info.guardianproject.otr.app.im.provider;

import info.guardianproject.otr.app.im.app.ImApp;

import java.util.HashMap;

import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * The IM provider stores all information about roster contacts, chat messages,
 * presence, etc.
 * 
 * @hide
 */
public class Imps {
    /** no public constructor since this is a utility class */
    private Imps() {
    }

    /** The Columns for IM providers (i.e. AIM, Y!, GTalk) */
    public interface ProviderColumns {
        /** The name of the IM provider <P>Type: TEXT</P> */
        String NAME = "name";

        /** The full name of the provider <P>Type: TEXT</P> */
        String FULLNAME = "fullname";

        /**
         * The category for the provider, used to form intent. <P>Type: TEXT</P>
         */
        String CATEGORY = "category";

        /**
         * The url users should visit to create a new account for this provider
         * <P>Type: TEXT</P>
         */
        String SIGNUP_URL = "signup_url";
    }

    /** Known names corresponding to the {@link ProviderColumns#NAME} column */
    public interface ProviderNames {
        //
        //NOTE: update Contacts.java with new providers when they're added.
        //
        String YAHOO = "Yahoo";
        String GTALK = "GTalk";
        String MSN = "MSN";
        String ICQ = "ICQ";
        String AIM = "AIM";
        String XMPP = "XMPP";
        String JABBER = "JABBER";
        String SKYPE = "SKYPE";
        String QQ = "QQ";
    }

    /** This table contains the IM providers */
    public static final class Provider implements BaseColumns, ProviderColumns {
        private Provider() {
        }

        public static final long getProviderIdForName(ContentResolver cr, String providerName) {
            
            
            String select = NAME + "=?";
            String[] selectionArgs = {providerName};

            Cursor cursor = cr.query(CONTENT_URI, PROVIDER_PROJECTION, select, selectionArgs, null);
                    
            long retVal = 0;
            try {
                if (cursor.moveToFirst()) {
                    retVal = cursor.getLong(cursor.getColumnIndexOrThrow(_ID));
                }
            } finally {
                if (cursor != null)
                    cursor.close();
                
            }

            return retVal;
        }
        
        public static final String getProviderNameForId(ContentResolver cr, long providerId) {
            Cursor cursor = cr.query(CONTENT_URI, PROVIDER_PROJECTION, _ID + "=" + providerId,
                    null, null);

            String retVal = null;
            try {
                if (cursor.moveToFirst()) {
                    retVal = cursor.getString(cursor.getColumnIndexOrThrow(NAME));
                }
            } finally {
                cursor.close();
            }

            return retVal;
        }

        private static final String[] PROVIDER_PROJECTION = new String[] { _ID, NAME };

        public static final String ACTIVE_ACCOUNT_ID = "account_id";
        public static final String ACTIVE_ACCOUNT_USERNAME = "account_username";
        public static final String ACTIVE_ACCOUNT_PW = "account_pw";
        public static final String ACTIVE_ACCOUNT_LOCKED = "account_locked";
        public static final String ACTIVE_ACCOUNT_KEEP_SIGNED_IN = "account_keepSignedIn";
        public static final String ACCOUNT_PRESENCE_STATUS = "account_presenceStatus";
        public static final String ACCOUNT_CONNECTION_STATUS = "account_connStatus";

        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/providers");

        public static final Uri CONTENT_URI_WITH_ACCOUNT = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/providers/account");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * people.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-providers";

        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-providers";

        /** The default sort order for this table */
        public static final String DEFAULT_SORT_ORDER = "name ASC";
    }

    /**
     * The columns for IM accounts. There can be more than one account for each
     * IM provider.
     */
    public interface AccountColumns {
        /** The name of the account <P>Type: TEXT</P> */
        String NAME = "name";

        /** The IM provider for this account <P>Type: INTEGER</P> */
        String PROVIDER = "provider";

        /** The username for this account <P>Type: TEXT</P> */
        String USERNAME = "username";

        /** The password for this account <P>Type: TEXT</P> */
        String PASSWORD = "pw";

        /**
         * A boolean value indicates if the account is active. <P>Type:
         * INTEGER</P>
         */
        String ACTIVE = "active";

        /**
         * A boolean value indicates if the account is locked (not editable)
         * <P>Type: INTEGER</P>
         */
        String LOCKED = "locked";

        /**
         * A boolean value to indicate whether this account is kept signed in.
         * <P>Type: INTEGER</P>
         */
        String KEEP_SIGNED_IN = "keep_signed_in";

        /**
         * A boolean value indiciating the last login state for this account
         * <P>Type: INTEGER</P>
         */
        String LAST_LOGIN_STATE = "last_login_state";
    }

    /** This table contains the IM accounts. */
    public static final class Account implements BaseColumns, AccountColumns {
        private Account() {
        }

        public static final long getProviderIdForAccount(ContentResolver cr, long accountId) {
            Cursor cursor = cr.query(CONTENT_URI, PROVIDER_PROJECTION, _ID + "=" + accountId,
                    null /* selection args */, null /* sort order */);

            long providerId = 0;

            try {
                if (cursor.moveToFirst()) {
                    providerId = cursor.getLong(PROVIDER_COLUMN);
                }
            } finally {
                cursor.close();
            }

            return providerId;
        }

        public static final String getUserName(ContentResolver cr, long accountId) {
            Cursor cursor = cr.query(CONTENT_URI, new String[] { USERNAME }, _ID + "=" + accountId,
                    null /* selection args */, null /* sort order */);
            String ret = null;
            try {
                if (cursor.moveToFirst()) {
                    ret = cursor.getString(cursor.getColumnIndexOrThrow(USERNAME));
                }
            } finally {
                cursor.close();
            }

            return ret;
        }

        public static final String getPassword(ContentResolver cr, long accountId) {
            Cursor cursor = cr.query(CONTENT_URI, new String[] { PASSWORD }, _ID + "=" + accountId,
                    null /* selection args */, null /* sort order */);
            String ret = null;
            try {
                if (cursor.moveToFirst()) {
                    ret = cursor.getString(cursor.getColumnIndexOrThrow(PASSWORD));
                }
            } finally {
                cursor.close();
            }

            return ret;
        }

        private static final String[] PROVIDER_PROJECTION = new String[] { PROVIDER };
        private static final int PROVIDER_COLUMN = 0;

        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/accounts");

        /** The content:// style URL for looking up by domain */
        public static final Uri BY_DOMAIN_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/domainAccounts");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * account.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-accounts";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * account.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-accounts";

        /** The default sort order for this table */
        public static final String DEFAULT_SORT_ORDER = "name ASC";

    }

    /** Connection status */
    public interface ConnectionStatus {
        /** The connection is offline, not logged in. */
        int OFFLINE = 0;

        /** The connection is attempting to connect. */
        int CONNECTING = 1;

        /** The connection is suspended due to network not available. */
        int SUSPENDED = 2;

        /** The connection is logged in and online. */
        int ONLINE = 3;
    }

    public interface AccountStatusColumns {
        /** account id <P>Type: INTEGER</P> */
        String ACCOUNT = "account";

        /**
         * User's presence status, see definitions in {#link
         * CommonPresenceColumn} <P>Type: INTEGER</P>
         */
        String PRESENCE_STATUS = "presenceStatus";

        /**
         * The connection status of this account, see {#link ConnectionStatus}
         * <P>Type: INTEGER</P>
         */
        String CONNECTION_STATUS = "connStatus";
    }

    public static final class AccountStatus implements BaseColumns, AccountStatusColumns {
        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/accountStatus");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * account status.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-account-status";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * account status.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-account-status";

        /** The default sort order for this table */
        public static final String DEFAULT_SORT_ORDER = "name ASC";
    }

    /** Columns from the Contacts table. */
    public interface ContactsColumns {
        /** The username <P>Type: TEXT</P> */
        String USERNAME = "username";

        /** The nickname or display name <P>Type: TEXT</P> */
        String NICKNAME = "nickname";

        /** The IM provider for this contact <P>Type: INTEGER</P> */
        String PROVIDER = "provider";

        /**
         * The account (within a IM provider) for this contact <P>Type:
         * INTEGER</P>
         */
        String ACCOUNT = "account";

        /** The contactList this contact belongs to <P>Type: INTEGER</P> */
        String CONTACTLIST = "contactList";

        /** Contact type <P>Type: INTEGER</P> */
        String TYPE = "type";

        /** normal IM contact */
        int TYPE_NORMAL = 0;
        /**
         * temporary contact, someone not in the list of contacts that we
         * subscribe presence for. Usually created because of the user is having
         * a chat session with this contact.
         */
        int TYPE_TEMPORARY = 1;
        /** temporary contact created for group chat. */
        int TYPE_GROUP = 2;
        /** blocked contact. */
        int TYPE_BLOCKED = 3;
        /**
         * the contact is hidden. The client should always display this contact
         * to the user.
         */
        int TYPE_HIDDEN = 4;
        /**
         * the contact is pinned. The client should always display this contact
         * to the user.
         */
        int TYPE_PINNED = 5;

        /** Contact subscription status <P>Type: INTEGER</P> */
        String SUBSCRIPTION_STATUS = "subscriptionStatus";

        /** no pending subscription */
        int SUBSCRIPTION_STATUS_NONE = 0;
        /** requested to subscribe */
        int SUBSCRIPTION_STATUS_SUBSCRIBE_PENDING = 1;
        /** requested to unsubscribe */
        int SUBSCRIPTION_STATUS_UNSUBSCRIBE_PENDING = 2;

        /** Contact subscription type <P>Type: INTEGER </P> */
        String SUBSCRIPTION_TYPE = "subscriptionType";

        /** The user and contact have no interest in each other's presence. */
        int SUBSCRIPTION_TYPE_NONE = 0;
        /** The user wishes to stop receiving presence updates from the contact. */
        int SUBSCRIPTION_TYPE_REMOVE = 1;
        /**
         * The user is interested in receiving presence updates from the
         * contact.
         */
        int SUBSCRIPTION_TYPE_TO = 2;
        /**
         * The contact is interested in receiving presence updates from the
         * user.
         */
        int SUBSCRIPTION_TYPE_FROM = 3;
        /**
         * The user and contact have a mutual interest in each other's presence.
         */
        int SUBSCRIPTION_TYPE_BOTH = 4;
        /** This is a special type reserved for pending subscription requests */
        int SUBSCRIPTION_TYPE_INVITATIONS = 5;

        /**
         * Quick Contact: derived from Google Contact Extension's
         * "message_count" attribute. <P>Type: INTEGER</P>
         */
        String QUICK_CONTACT = "qc";

        /**
         * Google Contact Extension attribute
         * 
         * Rejected: a boolean value indicating whether a subscription request
         * from this client was ever rejected by the user. "true" indicates that
         * it has. This is provided so that a client can block repeated
         * subscription requests. <P>Type: INTEGER</P>
         */
        String REJECTED = "rejected";

        /**
         * Off The Record status: 0 for disabled, 1 for enabled <P>Type: INTEGER
         * </P>
         */
        String OTR = "otr";
    }

    /** This defines the different type of values of {@link ContactsColumns#OTR} */
    public interface OffTheRecordType {
        /*
         * Off the record not turned on
         */
        int DISABLED = 0;
        /** Off the record turned on, but we don't know who turned it on */
        int ENABLED = 1;
        /** Off the record turned on by the user */
        int ENABLED_BY_USER = 2;
        /** Off the record turned on by the buddy */
        int ENABLED_BY_BUDDY = 3;
    };

    /** This table contains contacts. */
    public static final class Contacts implements BaseColumns, ContactsColumns, PresenceColumns,
            ChatsColumns {
        /** no public constructor since this is a utility class */
        private Contacts() {
        }

        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contacts");

        /** The content:// style URL for contacts joined with presence */
        public static final Uri CONTENT_URI_WITH_PRESENCE = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contactsWithPresence");

        /**
         * The content:// style URL for barebone contacts, not joined with any
         * other table
         */
        public static final Uri CONTENT_URI_CONTACTS_BAREBONE = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contactsBarebone");

        /** The content:// style URL for contacts who have an open chat session */
        public static final Uri CONTENT_URI_CHAT_CONTACTS = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contacts/chatting");

        /** The content:// style URL for contacts who have been blocked */
        public static final Uri CONTENT_URI_BLOCKED_CONTACTS = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contacts/blocked");

        /** The content:// style URL for contacts by provider and account */
        public static final Uri CONTENT_URI_CONTACTS_BY = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contacts");

        /**
         * The content:// style URL for contacts by provider and account, and
         * who have an open chat session
         */
        public static final Uri CONTENT_URI_CHAT_CONTACTS_BY = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contacts/chatting");

        /**
         * The content:// style URL for contacts by provider and account, and
         * who are online
         */
        public static final Uri CONTENT_URI_ONLINE_CONTACTS_BY = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contacts/online");

        /**
         * The content:// style URL for contacts by provider and account, and
         * who are offline
         */
        public static final Uri CONTENT_URI_OFFLINE_CONTACTS_BY = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contacts/offline");

        /** The content:// style URL for operations on bulk contacts */
        public static final Uri BULK_CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/bulk_contacts");

        /**
         * The content:// style URL for the count of online contacts in each
         * contact list by provider and account.
         */
        public static final Uri CONTENT_URI_ONLINE_COUNT = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contacts/onlineCount");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * people.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-contacts";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * person.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-contacts";

        /** The default sort order for this table */
        public static final String DEFAULT_SORT_ORDER = "subscriptionType DESC, last_message_date DESC,"
                                                        + " mode DESC, nickname COLLATE UNICODE ASC";

        public static final String CHATS_CONTACT = "chats_contact";

        public static final String AVATAR_HASH = "avatars_hash";

        public static final String AVATAR_DATA = "avatars_data";
    }

    /** Columns from the ContactList table. */
    public interface ContactListColumns {
        String NAME = "name";
        String PROVIDER = "provider";
        String ACCOUNT = "account";
    }

    /** This table contains the contact lists. */
    public static final class ContactList implements BaseColumns, ContactListColumns {
        private ContactList() {
        }

        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contactLists");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * people.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-contactLists";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * person.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-contactLists";

        /** The default sort order for this table */
        public static final String DEFAULT_SORT_ORDER = "name COLLATE UNICODE ASC";

        public static final String PROVIDER_NAME = "provider_name";

        public static final String ACCOUNT_NAME = "account_name";
    }

    /** Columns from the BlockedList table. */
    public interface BlockedListColumns {
        /** The username of the blocked contact. <P>Type: TEXT</P> */
        String USERNAME = "username";

        /** The nickname of the blocked contact. <P>Type: TEXT</P> */
        String NICKNAME = "nickname";

        /** The provider id of the blocked contact. <P>Type: INT</P> */
        String PROVIDER = "provider";

        /** The account id of the blocked contact. <P>Type: INT</P> */
        String ACCOUNT = "account";
    }

    /** This table contains blocked lists */
    public static final class BlockedList implements BaseColumns, BlockedListColumns {
        private BlockedList() {
        }

        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/blockedList");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * people.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-blockedList";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * person.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-blockedList";

        /** The default sort order for this table */
        public static final String DEFAULT_SORT_ORDER = "nickname ASC";

        public static final String PROVIDER_NAME = "provider_name";

        public static final String ACCOUNT_NAME = "account_name";

        public static final String AVATAR_DATA = "avatars_data";
    }

    /** Columns from the contactsEtag table */
    public interface ContactsEtagColumns {
        /**
         * The roster etag, computed by the server, stored on the client. There
         * is one etag per account roster. <P>Type: TEXT</P>
         */
        String ETAG = "etag";

        /**
         * The OTR etag, computed by the server, stored on the client. There is
         * one OTR etag per account roster. <P>Type: TEXT</P>
         */
        String OTR_ETAG = "otr_etag";

        /** The account id for the etag. <P> Type: INTEGER </P> */
        String ACCOUNT = "account";
    }

    public static final class ContactsEtag implements BaseColumns, ContactsEtagColumns {
        private ContactsEtag() {
        }

        public static final Cursor query(ContentResolver cr, String[] projection) {
            return cr.query(CONTENT_URI, projection, null, null, null);
        }

        public static final Cursor query(ContentResolver cr, String[] projection, String where,
                String orderBy) {
            return cr.query(CONTENT_URI, projection, where, null, orderBy == null ? null : orderBy);
        }

        public static final String getRosterEtag(ContentResolver resolver, long accountId) {
            String retVal = null;

            Cursor c = resolver.query(CONTENT_URI, CONTACT_ETAG_PROJECTION, ACCOUNT + "="
                                                                            + accountId,
                    null /* selection args */, null /* sort order */);

            try {
                if (c.moveToFirst()) {
                    retVal = c.getString(COLUMN_ETAG);
                }
            } finally {
                c.close();
            }

            return retVal;
        }

        public static final String getOtrEtag(ContentResolver resolver, long accountId) {
            String retVal = null;

            Cursor c = resolver.query(CONTENT_URI, CONTACT_OTR_ETAG_PROJECTION, ACCOUNT + "="
                                                                                + accountId,
                    null /* selection args */, null /* sort order */);

            try {
                if (c.moveToFirst()) {
                    retVal = c.getString(COLUMN_OTR_ETAG);
                }
            } finally {
                c.close();
            }

            return retVal;
        }

        private static final String[] CONTACT_ETAG_PROJECTION = new String[] { Imps.ContactsEtag.ETAG // 0
        };

        private static int COLUMN_ETAG = 0;

        private static final String[] CONTACT_OTR_ETAG_PROJECTION = new String[] { Imps.ContactsEtag.OTR_ETAG // 0
        };

        private static int COLUMN_OTR_ETAG = 0;

        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/contactsEtag");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * people.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-contactsEtag";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * person.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-contactsEtag";
    }

    /** Message type definition */
    public interface MessageType {
        /* sent message */
        int OUTGOING = 0;
        /* received message */
        int INCOMING = 1;
        /* presence became available */
        int PRESENCE_AVAILABLE = 2;
        /* presence became away */
        int PRESENCE_AWAY = 3;
        /* presence became DND (busy) */
        int PRESENCE_DND = 4;
        /* presence became unavailable */
        int PRESENCE_UNAVAILABLE = 5;
        /* the message is converted to a group chat */
        int CONVERT_TO_GROUPCHAT = 6;
        /* generic status */
        int STATUS = 7;
        /* the message cannot be sent now, but will be sent later */
        int POSTPONED = 8;
        /* off The Record status is turned off */
        int OTR_IS_TURNED_OFF = 9;
        /* off the record status is turned on */
        int OTR_IS_TURNED_ON = 10;
        /* off the record status turned on by user */
        int OTR_TURNED_ON_BY_USER = 11;
        /* off the record status turned on by buddy */
        int OTR_TURNED_ON_BY_BUDDY = 12;
        
        /* received message */
        int INCOMING_ENCRYPTED = 13;
        /* received message */
        int INCOMING_ENCRYPTED_VERIFIED = 14;
        
        /* received message */
        int OUTGOING_ENCRYPTED = 15;
        /* received message */
        int OUTGOING_ENCRYPTED_VERIFIED = 16;
    }

    /** The common columns for messages table */
    public interface MessageColumns {
        /**
         * The thread_id column stores the contact id of the contact the message
         * belongs to. For groupchat messages, the thread_id stores the group
         * id, which is the contact id of the temporary group contact created
         * for the groupchat. So there should be no collision between groupchat
         * message thread id and regular message thread id.
         */
        String THREAD_ID = "thread_id";

        /**
         * The nickname. This is used for groupchat messages to indicate the
         * participant's nickname. For non groupchat messages, this field should
         * be left empty.
         */
        String NICKNAME = "nickname";

        /** The body <P>Type: TEXT</P> */
        String BODY = "body";

        /** The date this message is sent or received <P>Type: INTEGER</P> */
        String DATE = "date";

        /** Message Type, see {@link MessageType} <P>Type: INTEGER</P> */
        String TYPE = "type";

        /** Error Code: 0 means no error. <P>Type: INTEGER </P> */
        String ERROR_CODE = "err_code";

        /** Error Message <P>Type: TEXT</P> */
        String ERROR_MESSAGE = "err_msg";

        /**
         * Packet ID, auto assigned by the GTalkService for outgoing messages or
         * the GTalk server for incoming messages. The packet id field is
         * optional for messages, so it could be null. <P>Type: STRING</P>
         */
        String PACKET_ID = "packet_id";

        /** Is groupchat message or not <P>Type: INTEGER</P> */
        String IS_GROUP_CHAT = "is_muc";

        /**
         * A hint that the UI should show the sent time of this message <P>Type:
         * INTEGER</P>
         */
        String DISPLAY_SENT_TIME = "show_ts";

        /** Whether a delivery confirmation was received. <P>Type: INTEGER</P> */
        String IS_DELIVERED = "is_delivered";
        
        /** Mime type.  If non-null, body is a URI. */
        String MIME_TYPE = "mime_type";
    }

    /** This table contains messages. */
    public static final class Messages implements BaseColumns, MessageColumns {
        /** no public constructor since this is a utility class */
        private Messages() {
        }

        /**
         * Gets the Uri to query messages by thread id.
         * 
         * @param threadId the thread id of the message.
         * @return the Uri
         */
        public static final Uri getContentUriByThreadId(long threadId) {
            Uri.Builder builder = CONTENT_URI_MESSAGES_BY_THREAD_ID.buildUpon();
            ContentUris.appendId(builder, threadId);
            return builder.build();
        }

        /**
         * @deprecated
         * 
         *             Gets the Uri to query messages by account and contact.
         * 
         * @param accountId the account id of the contact.
         * @param username the user name of the contact.
         * @return the Uri
         */
        public static final Uri getContentUriByContact(long accountId, String username) {
            Uri.Builder builder = CONTENT_URI_MESSAGES_BY_ACCOUNT_AND_CONTACT.buildUpon();
            ContentUris.appendId(builder, accountId);
            builder.appendPath(username);
            return builder.build();
        }

        /**
         * Gets the Uri to query messages by provider.
         * 
         * @param providerId the service provider id.
         * @return the Uri
         */
        public static final Uri getContentUriByProvider(long providerId) {
            Uri.Builder builder = CONTENT_URI_MESSAGES_BY_PROVIDER.buildUpon();
            ContentUris.appendId(builder, providerId);
            return builder.build();
        }

        /**
         * Gets the Uri to query off the record messages by account.
         * 
         * @param accountId the account id.
         * @return the Uri
         */
        public static final Uri getContentUriByAccount(long accountId) {
            Uri.Builder builder = CONTENT_URI_BY_ACCOUNT.buildUpon();
            ContentUris.appendId(builder, accountId);
            return builder.build();
        }

        /**
         * Gets the Uri to query off the record messages by thread id.
         * 
         * @param threadId the thread id of the message.
         * @return the Uri
         */
        public static final Uri getOtrMessagesContentUriByThreadId(long threadId) {
            Uri.Builder builder = OTR_MESSAGES_CONTENT_URI_BY_THREAD_ID.buildUpon();
            ContentUris.appendId(builder, threadId);
            return builder.build();
        }

        /**
         * @deprecated
         * 
         *             Gets the Uri to query off the record messages by account
         *             and contact.
         * 
         * @param accountId the account id of the contact.
         * @param username the user name of the contact.
         * @return the Uri
         */
        public static final Uri getOtrMessagesContentUriByContact(long accountId, String username) {
            Uri.Builder builder = OTR_MESSAGES_CONTENT_URI_BY_ACCOUNT_AND_CONTACT.buildUpon();
            ContentUris.appendId(builder, accountId);
            builder.appendPath(username);
            return builder.build();
        }

        /**
         * Gets the Uri to query off the record messages by provider.
         * 
         * @param providerId the service provider id.
         * @return the Uri
         */
        public static final Uri getOtrMessagesContentUriByProvider(long providerId) {
            Uri.Builder builder = OTR_MESSAGES_CONTENT_URI_BY_PROVIDER.buildUpon();
            ContentUris.appendId(builder, providerId);
            return builder.build();
        }

        /**
         * Gets the Uri to query off the record messages by account.
         * 
         * @param accountId the account id.
         * @return the Uri
         */
        public static final Uri getOtrMessagesContentUriByAccount(long accountId) {
            Uri.Builder builder = OTR_MESSAGES_CONTENT_URI_BY_ACCOUNT.buildUpon();
            ContentUris.appendId(builder, accountId);
            return builder.build();
        }

        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/messages");

        /** The content:// style URL for messages by thread id */
        public static final Uri CONTENT_URI_MESSAGES_BY_THREAD_ID = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/messagesByThreadId");

        /** The content:// style URL for messages by account and contact */
        public static final Uri CONTENT_URI_MESSAGES_BY_ACCOUNT_AND_CONTACT = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/messagesByAcctAndContact");

        /** The content:// style URL for messages by provider */
        public static final Uri CONTENT_URI_MESSAGES_BY_PROVIDER = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/messagesByProvider");

        /** The content:// style URL for messages by account */
        public static final Uri CONTENT_URI_BY_ACCOUNT = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/messagesByAccount");

        /** The content:// style url for off the record messages */
        public static final Uri OTR_MESSAGES_CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/otrMessages");

        /** The content:// style url for off the record messages by thread id */
        public static final Uri OTR_MESSAGES_CONTENT_URI_BY_THREAD_ID = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/otrMessagesByThreadId");

        /**
         * The content:// style url for off the record messages by account and
         * contact
         */
        public static final Uri OTR_MESSAGES_CONTENT_URI_BY_ACCOUNT_AND_CONTACT = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/otrMessagesByAcctAndContact");

        /** The content:// style URL for off the record messages by provider */
        public static final Uri OTR_MESSAGES_CONTENT_URI_BY_PROVIDER = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/otrMessagesByProvider");

        /** The content:// style URL for off the record messages by account */
        public static final Uri OTR_MESSAGES_CONTENT_URI_BY_ACCOUNT = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/otrMessagesByAccount");

        public static final Uri OTR_MESSAGES_CONTENT_URI_BY_PACKET_ID = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/otrMessagesByPacketId");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * people.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-messages";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * person.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-messages";

        /** The default sort order for this table */
        public static final String DEFAULT_SORT_ORDER = "date ASC";

        /**
         * The "contact" column. This is not a real column in the messages
         * table, but a temoprary column created when querying for messages
         * (joined with the contacts table)
         */
        public static final String CONTACT = "contact";
    }

    /** Columns for the GroupMember table. */
    public interface GroupMemberColumns {
        /** The id of the group this member belongs to. <p>Type: INTEGER</p> */
        String GROUP = "groupId";

        /** The full name of this member. <p>Type: TEXT</p> */
        String USERNAME = "username";

        /** The nick name of this member. <p>Type: TEXT</p> */
        String NICKNAME = "nickname";
    }

    public final static class GroupMembers implements GroupMemberColumns {
        private GroupMembers() {
        }

        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/groupMembers");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of group
         * members.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-groupMembers";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * group member.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-groupMembers";
    }

    /** Columns from the Invitation table. */
    public interface InvitationColumns {
        /** The provider id. <p>Type: INTEGER</p> */
        String PROVIDER = "providerId";

        /** The account id. <p>Type: INTEGER</p> */
        String ACCOUNT = "accountId";

        /** The invitation id. <p>Type: TEXT</p> */
        String INVITE_ID = "inviteId";

        /** The name of the sender of the invitation. <p>Type: TEXT</p> */
        String SENDER = "sender";

        /**
         * The name of the group which the sender invite you to join. <p>Type:
         * TEXT</p>
         */
        String GROUP_NAME = "groupName";

        /** A note <p>Type: TEXT</p> */
        String NOTE = "note";

        /** The current status of the invitation. <p>Type: TEXT</p> */
        String STATUS = "status";

        int STATUS_PENDING = 0;
        int STATUS_ACCEPTED = 1;
        int STATUS_REJECTED = 2;
    }

    /** This table contains the invitations received from others. */
    public final static class Invitation implements InvitationColumns, BaseColumns {
        private Invitation() {
        }

        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/invitations");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * invitations.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-invitations";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * invitation.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-invitations";
    }

    /** Columns from the Avatars table */
    public interface AvatarsColumns {
        /** The contact this avatar belongs to <P>Type: TEXT</P> */
        String CONTACT = "contact";

        String PROVIDER = "provider_id";

        String ACCOUNT = "account_id";

        /** The hash of the image data <P>Type: TEXT</P> */
        String HASH = "hash";

        /** raw image data <P>Type: BLOB</P> */
        String DATA = "data";
    }

    /** This table contains avatars. */
    public static final class Avatars implements BaseColumns, AvatarsColumns {
        /** no public constructor since this is a utility class */
        private Avatars() {
        }

        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/avatars");

        /**
         * The content:// style URL for avatars by provider, account and contact
         */
        public static final Uri CONTENT_URI_AVATARS_BY = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/avatarsBy");

        /** The MIME type of {@link #CONTENT_URI} providing the avatars */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-avatars";

        /** The MIME type of a {@link #CONTENT_URI} */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-avatars";

        /** The default sort order for this table */
        public static final String DEFAULT_SORT_ORDER = "contact ASC";

    }

    /**
     * Common presence columns shared between the IM and contacts presence
     * tables
     */
    public interface CommonPresenceColumns {
        /** The priority, an integer, used by XMPP presence <P>Type: INTEGER</P> */
        String PRIORITY = "priority";

        /**
         * The server defined status. <P>Type: INTEGER (one of the values
         * below)</P>
         */
        String PRESENCE_STATUS = "mode";

        /** Presence Status definition */
        int OFFLINE = 0;
        int INVISIBLE = 1;
        int AWAY = 2;
        int IDLE = 3;
        int DO_NOT_DISTURB = 4;
        int AVAILABLE = 5;
        
        int NEW_ACCOUNT = -99;
        

        /** The user defined status line. <P>Type: TEXT</P> */
        String PRESENCE_CUSTOM_STATUS = "status";
    }

    /** Columns from the Presence table. */
    public interface PresenceColumns extends CommonPresenceColumns {
        /** The contact id <P>Type: INTEGER</P> */
        String CONTACT_ID = "contact_id";

        /**
         * The contact's JID resource, only relevant for XMPP contact <P>Type:
         * TEXT</P>
         */
        String JID_RESOURCE = "jid_resource";

        /** The contact's client type */
        String CLIENT_TYPE = "client_type";

        /** client type definitions */
        int CLIENT_TYPE_DEFAULT = 0;
        int CLIENT_TYPE_MOBILE = 1;
        int CLIENT_TYPE_ANDROID = 2;
    }

    /** Contains presence infomation for contacts. */
    public static final class Presence implements BaseColumns, PresenceColumns {
        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/presence");

        /** The content URL for IM presences for an account */
        public static final Uri CONTENT_URI_BY_ACCOUNT = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/presence/account");

        /** The content:// style URL for operations on bulk contacts */
        public static final Uri BULK_CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/bulk_presence");

        /**
         * The content:// style URL for seeding presences for a given account
         * id.
         */
        public static final Uri SEED_PRESENCE_BY_ACCOUNT_CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/seed_presence/account");

        /**
         * The MIME type of a {@link #CONTENT_URI} providing a directory of
         * presence
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-presence";

        /** The default sort order for this table */
        public static final String DEFAULT_SORT_ORDER = "mode DESC";
    }

    /** Columns from the Chats table. */
    public interface ChatsColumns {
        /**
         * The contact ID this chat belongs to. The value is a long. <P>Type:
         * INT</P>
         */
        String CONTACT_ID = "contact_id";

        /** The GTalk JID resource. The value is a string. <P>Type: TEXT</P> */
        String JID_RESOURCE = "jid_resource";

        /** Whether this is a groupchat or not. <P>Type: INT</P> */
        String GROUP_CHAT = "groupchat";

        /**
         * The last unread message. This both indicates that there is an unread
         * message, and what the message is. <P>Type: TEXT</P>
         */
        String LAST_UNREAD_MESSAGE = "last_unread_message";

        /** The last message timestamp <P>Type: INT</P> */
        String LAST_MESSAGE_DATE = "last_message_date";

        /**
         * A message that is being composed. This indicates that there was a
         * message being composed when the chat screen was shutdown, and what
         * the message is. <P>Type: TEXT</P>
         */
        String UNSENT_COMPOSED_MESSAGE = "unsent_composed_message";

        /**
         * A value from 0-9 indicating which quick-switch chat screen slot this
         * chat is occupying. If none (for instance, this is the 12th active
         * chat) then the value is -1. <P>Type: INT</P>
         */
        String SHORTCUT = "shortcut";
    }

    /** Contains ongoing chat sessions. */
    public static final class Chats implements BaseColumns, ChatsColumns {
        /** no public constructor since this is a utility class */
        private Chats() {
        }

        /** The content:// style URL for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/chats");

        /** The content URL for all chats that belong to the account */
        public static final Uri CONTENT_URI_BY_ACCOUNT = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/chats/account");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of chats.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/imps-chats";

        /**
         * The MIME type of a {@link #CONTENT_URI} subdirectory of a single
         * chat.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/imps-chats";

        /** The default sort order for this table */
        public static final String DEFAULT_SORT_ORDER = "last_message_date ASC";
    }

    /** Columns from session cookies table. Used for IMPS. */
    public static interface SessionCookiesColumns {
        String NAME = "name";
        String VALUE = "value";
        String PROVIDER = "provider";
        String ACCOUNT = "account";
    }

    /** Contains IMPS session cookies. */
    public static class SessionCookies implements SessionCookiesColumns, BaseColumns {
        private SessionCookies() {
        }

        /** The content:// style URI for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/sessionCookies");

        /** The content:// style URL for session cookies by provider and account */
        public static final Uri CONTENT_URI_SESSION_COOKIES_BY = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/sessionCookiesBy");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * people.
         */
        public static final String CONTENT_TYPE = "vnd.android-dir/imps-sessionCookies";
    }

    /** Columns from ProviderSettings table */
    public static interface ProviderSettingsColumns {
        /**
         * The id in database of the related provider
         * 
         * <P>Type: INT</P>
         */
        String PROVIDER = "provider";

        /** The name of the setting <P>Type: TEXT</P> */
        String NAME = "name";

        /** The value of the setting <P>Type: TEXT</P> */
        String VALUE = "value";
    }

    public static class ProviderSettings implements ProviderSettingsColumns {
        // Global settings are saved with this provider ID, for backward compatibility
        public static final int PROVIDER_ID_FOR_GLOBAL_SETTINGS = 1;

        private ProviderSettings() {
        }

        /** The content:// style URI for this table */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/providerSettings");

        /** The MIME type of {@link #CONTENT_URI} providing provider settings */
        public static final String CONTENT_TYPE = "vnd.android-dir/imps-providerSettings";

        // ACCOUNT SETTINGS (username and password are part of Account above)

        // TODO since everything is here except username/password, perhaps it should also be moved here?

        /** the domain name of the account, i.e. @gmail.com for XMPP */
        public static final String DOMAIN = "pref_account_domain";

        /** The XMPP Resource string */
        public static final String XMPP_RESOURCE = "pref_account_xmpp_resource";

        /** The XMPP Resource priority string */
        public static final String XMPP_RESOURCE_PRIO = "pref_account_xmpp_resource_prio";

        /** the port number to connect to */
        public static final String PORT = "pref_account_port";

        /** The hostname or IP of the server to connect to */
        public static final String SERVER = "pref_account_server";

        // ENCRYPTION AND ANONYMITY SETTINGS

        /** allow plain text authentication */
        public static final String ALLOW_PLAIN_AUTH = "pref_security_allow_plain_auth";

        /** boolean for the required use of a TLS connection */
        public static final String REQUIRE_TLS = "pref_security_require_tls";

        /** boolean for whether the TLS certificate should be verified */
        public static final String TLS_CERT_VERIFY = "pref_security_tls_cert_verify";

        /**
         * Global setting controlling how OTR engine initiates: auto, force,
         * requested, disabled
         */
        public static final String OTR_MODE = "pref_security_otr_mode";

        /** boolean to specify whether to use Tor proxying or not */
        public static final String USE_TOR = "pref_security_use_tor";

        /**
         * boolean to control whether DNS SRV lookups are used to find the
         * server
         */
        public static final String DO_DNS_SRV = "pref_security_do_dns_srv";

        // GENERAL PREFERENCES

        /** controls whether this provider should show the offline contacts */
        public static final String SHOW_OFFLINE_CONTACTS = "show_offline_contacts";

        /** controls whether the GTalk service automatically connect to server. */
        public static final String AUTOMATICALLY_CONNECT_GTALK = "gtalk_auto_connect";

        /**
         * controls whether the IM service will be automatically started after
         * boot
         */
        public static final String AUTOMATICALLY_START_SERVICE = "auto_start_service";

        /**
         * Global setting which controls whether the offline contacts will be
         * hid.
         */
        public static final String HIDE_OFFLINE_CONTACTS = "hide_offline_contacts";

        /** Global setting which controls whether enable the IM notification */
        public static final String ENABLE_NOTIFICATION = "enable_notification";

        /** Global setting which specifies whether to vibrate */
        public static final String NOTIFICATION_VIBRATE = "vibrate";

        /** Global setting which specifies the Uri string of the ringtone */
        public static final String NOTIFICATION_RINGTONE = "ringtone";

        /** Global setting which specifies the Uri of the default ringtone */
        public static final String RINGTONE_DEFAULT = "content://settings/system/notification_sound";

        /** specifies whether to show mobile indicator to friends */
        public static final String SHOW_MOBILE_INDICATOR = "mobile_indicator";

        /** specifies whether to show as away when device is idle */
        public static final String SHOW_AWAY_ON_IDLE = "show_away_on_idle";

        /** controls whether the service gets foreground priority */
        public static final String USE_FOREGROUND_PRIORITY = "use_foreground_priority";

        /** specifies whether to upload heartbeat stat upon login */
        public static final String UPLOAD_HEARTBEAT_STAT = "upload_heartbeat_stat";

        /** specifies the last heartbeat interval received from the server */
        public static final String HEARTBEAT_INTERVAL = "heartbeat_interval";

        /** specifiy the JID resource used for Google Talk connection */
        public static final String JID_RESOURCE = "jid_resource";

        /**
         * Used for reliable message queue (RMQ). This is for storing the last
         * rmq id received from the GTalk server
         */
        public static final String LAST_RMQ_RECEIVED = "last_rmq_rec";
        
        /**
         * use for status persistence
         */
        public static final String PRESENCE_STATE = "presence_state";
        public static final String PRESENCE_STATUS_MESSAGE = "presence_status_message";
        

        /**
         * Query the settings of the provider specified by id
         * 
         * @param cr the relative content resolver
         * @param providerId the specified id of provider
         * @return a HashMap which contains all the settings for the specified
         *         provider
         */
        public static HashMap<String, String> queryProviderSettings(ContentResolver cr,
                long providerId) {
            HashMap<String, String> settings = new HashMap<String, String>();

            String[] projection = { NAME, VALUE };
            Cursor c = cr.query(ContentUris.withAppendedId(CONTENT_URI, providerId), projection,
                    null, null, null);
            if (c == null) {
                return null;
            }

            while (c.moveToNext()) {
                settings.put(c.getString(0), c.getString(1));
            }

            c.close();

            return settings;
        }

        /**
         * Get the string value of setting which is specified by provider id and
         * the setting name.
         * 
         * @param cr The ContentResolver to use to access the settings table.
         * @param providerId The id of the provider.
         * @param settingName The name of the setting.
         * @return The value of the setting if the setting exist, otherwise
         *         return null.
         */
        public static String getStringValue(ContentResolver cr, long providerId, String settingName) {
            String ret = null;
            Cursor c = getSettingValue(cr, providerId, settingName);
            if (c != null) {
                ret = c.getString(0);
                c.close();
            }

            return ret;
        }
        
        /**
         * Get the string value of setting which is specified by provider id and
         * the setting name.
         * 
         * @param cr The ContentResolver to use to access the settings table.
         * @param providerId The id of the provider.
         * @param settingName The name of the setting.
         * @return The value of the setting if the setting exist, otherwise
         *         return null.
         */
        public static int getIntValue(ContentResolver cr, long providerId, String settingName) {
            int ret = -1;
            
            Cursor c = getSettingValue(cr, providerId, settingName);
            if (c != null) {
                ret = c.getInt(0);
                c.close();
            }

            return ret;
        }

        /**
         * Get the boolean value of setting which is specified by provider id
         * and the setting name.
         * 
         * @param cr The ContentResolver to use to access the settings table.
         * @param providerId The id of the provider.
         * @param settingName The name of the setting.
         * @return The value of the setting if the setting exist, otherwise
         *         return false.
         */
        public static boolean getBooleanValue(ContentResolver cr, long providerId,
                String settingName) {
            boolean ret = false;
            Cursor c = getSettingValue(cr, providerId, settingName);
            if (c != null) {
                ret = c.getInt(0) != 0;
                c.close();
            }
            return ret;
        }

        private static Cursor getSettingValue(ContentResolver cr, long providerId,
                String settingName) {
            Cursor c = cr.query(ContentUris.withAppendedId(CONTENT_URI, providerId),
                    new String[] { VALUE }, NAME + "=?", new String[] { settingName }, null);
            if (c != null) {
                if (!c.moveToFirst()) {
                    c.close();
                    return null;
                }
            }
            return c;
        }

        /**
         * Save a long value of setting in the table providerSetting.
         * 
         * @param cr The ContentProvider used to access the providerSetting
         *            table.
         * @param providerId The id of the provider.
         * @param name The name of the setting.
         * @param value The value of the setting.
         */
        public static void putLongValue(ContentResolver cr, long providerId, String name, long value) {
            ContentValues v = new ContentValues(3);
            v.put(PROVIDER, providerId);
            v.put(NAME, name);
            v.put(VALUE, value);

            cr.insert(CONTENT_URI, v);
        }

        /**
         * Save a long value of setting in the table providerSetting.
         * 
         * @param cr The ContentProvider used to access the providerSetting
         *            table.
         * @param providerId The id of the provider.
         * @param name The name of the setting.
         * @param value The value of the setting.
         */
        public static void putIntValue(ContentResolver cr, long providerId, String name, int value) {
            ContentValues v = new ContentValues(3);
            v.put(PROVIDER, providerId);
            v.put(NAME, name);
            v.put(VALUE, value);

            cr.insert(CONTENT_URI, v);
        }
        
        /**
         * Save a boolean value of setting in the table providerSetting.
         * 
         * @param cr The ContentProvider used to access the providerSetting
         *            table.
         * @param providerId The id of the provider.
         * @param name The name of the setting.
         * @param value The value of the setting.
         */
        public static void putBooleanValue(ContentResolver cr, long providerId, String name,
                boolean value) {
            ContentValues v = new ContentValues(3);
            v.put(PROVIDER, providerId);
            v.put(NAME, name);
            v.put(VALUE, Boolean.toString(value));

            cr.insert(CONTENT_URI, v);
        }

        /**
         * Save a string value of setting in the table providerSetting.
         * 
         * @param cr The ContentProvider used to access the providerSetting
         *            table.
         * @param providerId The id of the provider.
         * @param name The name of the setting.
         * @param value The value of the setting.
         */
        public static void putStringValue(ContentResolver cr, long providerId, String name,
                String value) {
            ContentValues v = new ContentValues(3);
            v.put(PROVIDER, providerId);
            v.put(NAME, name);
            v.put(VALUE, value);

            cr.insert(CONTENT_URI, v);
            
            
        }

        /**
         * A convenience method to set the domain name affiliated with an
         * account
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param domain The domain name to use for the account
         */
        public static void setDomain(ContentResolver cr, long providerId, String domain) {
            putStringValue(cr, providerId, DOMAIN, domain);
        }

        /**
         * A convenience method to set the XMPP Resource string
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param xmppResource the XMPP Resource string
         */
        public static void setXmppResource(ContentResolver cr, long providerId, String xmppResource) {
            putStringValue(cr, providerId, XMPP_RESOURCE, xmppResource);
        }

        /**
         * A convenience method to set the XMPP Resource priority
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param priority the XMPP Resource priority
         */
        public static void setXmppResourcePrio(ContentResolver cr, long providerId, int priority) {
            putLongValue(cr, providerId, XMPP_RESOURCE_PRIO, (long) priority);
        }

        /**
         * A convenience method to set the TCP/IP port number to connect to
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param port the TCP/IP port number to connect to
         */
        public static void setPort(ContentResolver cr, long providerId, int port) {
            putLongValue(cr, providerId, PORT, (long) port);
        }

        /**
         * A convenience method to set the hostname or IP of the server to
         * connect to
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param server the hostname or IP of the server to connect to
         */
        public static void setServer(ContentResolver cr, long providerId, String server) {
            putStringValue(cr, providerId, SERVER, server);
        }

        /**
         * A convenience method to set whether to allow plain text auth
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param allowPlainAuth
         */
        public static void setAllowPlainAuth(ContentResolver cr, long providerId,
                boolean allowPlainAuth) {
            putBooleanValue(cr, providerId, ALLOW_PLAIN_AUTH, allowPlainAuth);
        }

        /**
         * A convenience method to set whether to require TLS
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param requireTls
         */
        public static void setRequireTls(ContentResolver cr, long providerId, boolean requireTls) {
            putBooleanValue(cr, providerId, REQUIRE_TLS, requireTls);
        }

        /**
         * A convenience method to set whether to verify the TLS cert
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param tlsCertVerify
         */
        public static void setTlsCertVerify(ContentResolver cr, long providerId,
                boolean tlsCertVerify) {
            putBooleanValue(cr, providerId, TLS_CERT_VERIFY, tlsCertVerify);
        }

        /**
         * A convenience method to set the mode of operation for the OTR Engine
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param otrMode OTR Engine mode (force, auto, requested, disabled)
         */
        public static void setOtrMode(ContentResolver cr, long providerId, String otrMode) {
            putStringValue(cr, providerId, OTR_MODE, otrMode);
        }

        /**
         * A convenience method to set whether to use Tor
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param useTor
         */
        public static void setUseTor(ContentResolver cr, long providerId, boolean useTor) {
            putBooleanValue(cr, providerId, USE_TOR, useTor);
        }

        /**
         * A convenience method to set whether to use DNS SRV lookups to find
         * the server
         * 
         * @param cr The ContentResolver to use to access the settings table
         * @param providerId used to identify the set of settings for a given
         *            provider
         * @param doDnsSrv
         */
        public static void setDoDnsSrv(ContentResolver cr, long providerId, boolean doDnsSrv) {
            putBooleanValue(cr, providerId, DO_DNS_SRV, doDnsSrv);
        }

        /**
         * A convenience method to set whether or not the GTalk service should
         * be started automatically.
         * 
         * @param contentResolver The ContentResolver to use to access the
         *            settings table
         * @param autoConnect Whether the GTalk service should be started
         *            automatically.
         */
        public static void setAutomaticallyConnectGTalk(ContentResolver contentResolver,
                long providerId, boolean autoConnect) {
            putBooleanValue(contentResolver, providerId, AUTOMATICALLY_CONNECT_GTALK, autoConnect);
        }

        /**
         * A convenience method to set whether or not the offline contacts
         * should be hided
         * 
         * @param contentResolver The ContentResolver to use to access the
         *            setting table
         * @param hideOfflineContacts Whether the offline contacts should be
         *            hided
         */
        public static void setHideOfflineContacts(ContentResolver contentResolver, long providerId,
                boolean hideOfflineContacts) {
            putBooleanValue(contentResolver, providerId, HIDE_OFFLINE_CONTACTS, hideOfflineContacts);
        }

        public static void setUseForegroundPriority(ContentResolver contentResolver,
                long providerId, boolean flag) {
            putBooleanValue(contentResolver, providerId, USE_FOREGROUND_PRIORITY, flag);
        }

        /**
         * A convenience method to set whether or not enable the IM
         * notification.
         * 
         * @param contentResolver The ContentResolver to use to access the
         *            setting table.
         * @param enable Whether enable the IM notification
         */
        public static void setEnableNotification(ContentResolver contentResolver, long providerId,
                boolean enable) {
            putBooleanValue(contentResolver, providerId, ENABLE_NOTIFICATION, enable);
        }

        /**
         * A convenience method to set whether or not to vibrate.
         * 
         * @param contentResolver The ContentResolver to use to access the
         *            setting table.
         * @param vibrate Whether or not to vibrate
         */
        public static void setVibrate(ContentResolver contentResolver, long providerId,
                boolean vibrate) {
            putBooleanValue(contentResolver, providerId, NOTIFICATION_VIBRATE, vibrate);
        }

        /**
         * A convenience method to set the Uri String of the ringtone.
         * 
         * @param contentResolver The ContentResolver to use to access the
         *            setting table.
         * @param ringtoneUri The Uri String of the ringtone to be set.
         */
        public static void setRingtoneURI(ContentResolver contentResolver, long providerId,
                String ringtoneUri) {
            putStringValue(contentResolver, providerId, NOTIFICATION_RINGTONE, ringtoneUri);
        }

        /**
         * A convenience method to set whether or not to show mobile indicator.
         * 
         * @param contentResolver The ContentResolver to use to access the
         *            setting table.
         * @param showMobileIndicator Whether or not to show mobile indicator.
         */
        public static void setShowMobileIndicator(ContentResolver contentResolver, long providerId,
                boolean showMobileIndicator) {
            putBooleanValue(contentResolver, providerId, SHOW_MOBILE_INDICATOR, showMobileIndicator);
        }

        /**
         * A convenience method to set whether or not to show as away when
         * device is idle.
         * 
         * @param contentResolver The ContentResolver to use to access the
         *            setting table.
         * @param showAway Whether or not to show as away when device is idle.
         */
        public static void setShowAwayOnIdle(ContentResolver contentResolver, long providerId,
                boolean showAway) {
            putBooleanValue(contentResolver, providerId, SHOW_AWAY_ON_IDLE, showAway);
        }

        /**
         * A convenience method to set whether or not to upload heartbeat stat.
         * 
         * @param contentResolver The ContentResolver to use to access the
         *            setting table.
         * @param uploadStat Whether or not to upload heartbeat stat.
         */
        public static void setUploadHeartbeatStat(ContentResolver contentResolver, long providerId,
                boolean uploadStat) {
            putBooleanValue(contentResolver, providerId, UPLOAD_HEARTBEAT_STAT, uploadStat);
        }

        /**
         * A convenience method to set the heartbeat interval last received from
         * the server.
         * 
         * @param contentResolver The ContentResolver to use to access the
         *            setting table.
         * @param interval The heartbeat interval last received from the server.
         */
        public static void setHeartbeatInterval(ContentResolver contentResolver, long providerId,
                long interval) {
            putLongValue(contentResolver, providerId, HEARTBEAT_INTERVAL, interval);
        }

        /**
         * A convenience method to user configure presence state and status
         * 
         * @param contentResolver The ContentResolver to use to access the
         *            setting table.
         * @param interval The heartbeat interval last received from the server.
         */
        public static void setPresence(ContentResolver contentResolver, long providerId,
                int state, String statusMessage) {
            
            if (state != -1)
                putIntValue(contentResolver, providerId, PRESENCE_STATE, state);
            
            if (statusMessage != null)
                putStringValue(contentResolver, providerId, PRESENCE_STATUS_MESSAGE, statusMessage);
        }

        
        
        
        /** A convenience method to set the jid resource. */
        public static void setJidResource(ContentResolver contentResolver, long providerId,
                String jidResource) {
            putStringValue(contentResolver, providerId, JID_RESOURCE, jidResource);
        }

        public static class QueryMap extends ContentQueryMap {
            private ContentResolver mContentResolver;
            private long mProviderId;
            private Exception mStacktrace;

            public QueryMap(ContentResolver contentResolver, boolean keepUpdated,
                    Handler handlerForUpdateNotifications) {
                this(contentResolver, ProviderSettings.PROVIDER_ID_FOR_GLOBAL_SETTINGS,
                        keepUpdated, handlerForUpdateNotifications);
            }

            public QueryMap(ContentResolver contentResolver, long providerId, boolean keepUpdated,
                    Handler handlerForUpdateNotifications) {
                
                super(contentResolver.query(CONTENT_URI,new String[] {NAME, VALUE},PROVIDER + "=?",new String[] { Long.toString(providerId)},
                        null), // no sort order
                        NAME, keepUpdated, handlerForUpdateNotifications);
               
                mContentResolver = contentResolver;
                mProviderId = providerId;
                mStacktrace = new Exception();
            }
            @Override
            public synchronized void close() {
                mStacktrace = null;
                super.close();
            }
            
            @Override
            protected void finalize() throws Throwable {
                if (mStacktrace != null) {
                    Log.w("GB.Imps", "QueryMap cursor not closed before finalize", mStacktrace);
                }
                super.finalize();
            }
            /**
             * Set if the GTalk service should automatically connect to server.
             * 
             * @param autoConnect if the GTalk service should auto connect to
             *            server.
             */
            public void setAutomaticallyConnectToGTalkServer(boolean autoConnect) {
                ProviderSettings.setAutomaticallyConnectGTalk(mContentResolver, mProviderId,
                        autoConnect);
            }

            /**
             * Check if the GTalk service should automatically connect to
             * server.
             * 
             * @return if the GTalk service should automatically connect to
             *         server.
             */
            public boolean getAutomaticallyConnectToGTalkServer() {
                return getBoolean(AUTOMATICALLY_CONNECT_GTALK, true /* default to automatically sign in */);
            }

            public void setDomain(String domain) {
                ProviderSettings.setDomain(mContentResolver, mProviderId, domain);
            }

            public String getDomain() {
                return getString(DOMAIN, "");
            }

            public void setXmppResource(String resource) {
                ProviderSettings.setXmppResource(mContentResolver, mProviderId, resource);
            }

            public String getXmppResource() {
                return getString(XMPP_RESOURCE, ImApp.DEFAULT_XMPP_RESOURCE);
            }

            public void setXmppResourcePrio(int prio) {
                ProviderSettings.setXmppResourcePrio(mContentResolver, mProviderId, prio);
            }

            public int getXmppResourcePrio() {
                return (int) getLong(XMPP_RESOURCE_PRIO, ImApp.DEFAULT_XMPP_PRIORITY);
            }

            public void setPort(int port) {
                ProviderSettings.setPort(mContentResolver, mProviderId, port);
            }

            public int getPort() {
                return (int) getLong(PORT, 0 /* by default use XMPP's default port */);
            }

            public void setServer(String server) {
                ProviderSettings.setServer(mContentResolver, mProviderId, server);
            }

            public String getServer() {
                return getString(SERVER, "");
            }

            public void setAllowPlainAuth(boolean value) {
                ProviderSettings.setAllowPlainAuth(mContentResolver, mProviderId, value);
            }

            public boolean getAllowPlainAuth() {
                return getBoolean(ALLOW_PLAIN_AUTH, false /* by default do not send passwords in the clear */);
            }

            public void setRequireTls(boolean value) {
                ProviderSettings.setRequireTls(mContentResolver, mProviderId, value);
            }

            public boolean getRequireTls() {
                return getBoolean(REQUIRE_TLS, true /* by default attempt TLS but don't require */);
                //n8fr8 2011/04/20 i think we should require it by default so i set to 'true'
            }

            public void setTlsCertVerify(boolean value) {
                ProviderSettings.setTlsCertVerify(mContentResolver, mProviderId, value);
            }

            public boolean getTlsCertVerify() {
                return getBoolean(TLS_CERT_VERIFY, true /* by default try to verify the TLS Cert */);
            }

            public void setOtrMode(String otrMode) {
                ProviderSettings.setOtrMode(mContentResolver, mProviderId, otrMode);
            }

            public String getOtrMode() {
                return getString(OTR_MODE, ImApp.DEFAULT_XMPP_OTR_MODE /* by default, try to use OTR */);
            }

            public void setUseTor(boolean value) {
                ProviderSettings.setUseTor(mContentResolver, mProviderId, value);
            }

            public boolean getUseTor() {
                return getBoolean(USE_TOR, false /* by default do not use Tor */);
            }

            public void setDoDnsSrv(boolean value) {
                ProviderSettings.setDoDnsSrv(mContentResolver, mProviderId, value);
            }

            public boolean getDoDnsSrv() {
                return getBoolean(DO_DNS_SRV, true /* by default use DNS SRV to find the server */);
            }

            /**
             * Set whether or not the offline contacts should be hided.
             * 
             * @param hideOfflineContacts Whether or not the offline contacts
             *            should be hided.
             */
            public void setHideOfflineContacts(boolean hideOfflineContacts) {
                ProviderSettings.setHideOfflineContacts(mContentResolver, mProviderId,
                        hideOfflineContacts);
            }

            /**
             * Check if the offline contacts should be hided.
             * 
             * @return Whether or not the offline contacts should be hided.
             */
            public boolean getHideOfflineContacts() {
                return getBoolean(HIDE_OFFLINE_CONTACTS, false /* default*/);
            }

            public void setUseForegroundPriority(boolean flag) {
                ProviderSettings.setUseForegroundPriority(mContentResolver, mProviderId, flag);
            }

            public boolean getUseForegroundPriority() {
                return getBoolean(USE_FOREGROUND_PRIORITY, false /* default */);
            }

            /**
             * Set whether or not enable the IM notification.
             * 
             * @param enable Whether or not enable the IM notification.
             */
            public void setEnableNotification(boolean enable) {
                ProviderSettings.setEnableNotification(mContentResolver, mProviderId, enable);
            }

            /**
             * Check if the IM notification is enabled.
             * 
             * @return Whether or not enable the IM notification.
             */
            public boolean getEnableNotification() {
                return getBoolean(ENABLE_NOTIFICATION, true/* by default enable the notification */);
            }

            /**
             * Set whether or not to vibrate on IM notification.
             * 
             * @param vibrate Whether or not to vibrate.
             */
            public void setVibrate(boolean vibrate) {
                ProviderSettings.setVibrate(mContentResolver, mProviderId, vibrate);
            }

            /**
             * Gets whether or not to vibrate on IM notification.
             * 
             * @return Whether or not to vibrate.
             */
            public boolean getVibrate() {
                return getBoolean(NOTIFICATION_VIBRATE, true /* by default enable vibrate */);
            }

            /**
             * Set the Uri for the ringtone.
             * 
             * @param ringtoneUri The Uri of the ringtone to be set.
             */
            public void setRingtoneURI(String ringtoneUri) {
                ProviderSettings.setRingtoneURI(mContentResolver, mProviderId, ringtoneUri);
            }

            /**
             * Get the Uri String of the current ringtone.
             * 
             * @return The Uri String of the current ringtone.
             */
            public String getRingtoneURI() {
                return getString(NOTIFICATION_RINGTONE, RINGTONE_DEFAULT);
            }

            /**
             * Set whether or not to show mobile indicator to friends.
             * 
             * @param showMobile whether or not to show mobile indicator.
             */
            public void setShowMobileIndicator(boolean showMobile) {
                ProviderSettings.setShowMobileIndicator(mContentResolver, mProviderId, showMobile);
            }

            /**
             * Gets whether or not to show mobile indicator.
             * 
             * @return Whether or not to show mobile indicator.
             */
            public boolean getShowMobileIndicator() {
                return getBoolean(SHOW_MOBILE_INDICATOR, true /* by default show mobile indicator */);
            }

            /**
             * Set whether or not to show as away when device is idle.
             * 
             * @param showAway whether or not to show as away when device is
             *            idle.
             */
            public void setShowAwayOnIdle(boolean showAway) {
                ProviderSettings.setShowAwayOnIdle(mContentResolver, mProviderId, showAway);
            }

            /**
             * Get whether or not to show as away when device is idle.
             * 
             * @return Whether or not to show as away when device is idle.
             */
            public boolean getShowAwayOnIdle() {
                return getBoolean(SHOW_AWAY_ON_IDLE, true /* by default show as away on idle*/);
            }

            /**
             * Set whether or not to upload heartbeat stat.
             * 
             * @param uploadStat whether or not to upload heartbeat stat.
             */
            public void setUploadHeartbeatStat(boolean uploadStat) {
                ProviderSettings.setUploadHeartbeatStat(mContentResolver, mProviderId, uploadStat);
            }

            /**
             * Get whether or not to upload heartbeat stat.
             * 
             * @return Whether or not to upload heartbeat stat.
             */
            public boolean getUploadHeartbeatStat() {
                return getBoolean(UPLOAD_HEARTBEAT_STAT, false /* by default do not upload */);
            }

            /**
             * Set the heartbeat interval.
             */
            public void setHeartbeatInterval(long interval) {
                if (interval <= 0) {
                    interval = 1;
                } else if (interval > 99) {
                    interval = 99;
                }
                ProviderSettings.setHeartbeatInterval(mContentResolver, mProviderId, interval);
            }

            /**
             * Get the heartbeat interval, default to 1.
             */
            public long getHeartbeatInterval() {
                return getLong(HEARTBEAT_INTERVAL, 1);
            }

            /**
             * Set the JID resource.
             * 
             * @param jidResource the jid resource to be stored.
             */
            public void setJidResource(String jidResource) {
                ProviderSettings.setJidResource(mContentResolver, mProviderId, jidResource);
            }

            /**
             * Get the JID resource used for the Google Talk connection
             * 
             * @return the JID resource stored.
             */
            public String getJidResource() {
                return getString(JID_RESOURCE, null);
            }

            /**
             * Convenience function for retrieving a single settings value as a
             * boolean.
             * 
             * @param name The name of the setting to retrieve.
             * @param def Value to return if the setting is not defined.
             * @return The setting's current value, or 'def' if it is not
             *         defined.
             */
            private boolean getBoolean(String name, boolean def) {
                ContentValues values = getValues(name);
                return values != null ? values.getAsBoolean(VALUE) : def;
            }

            /**
             * Convenience function for retrieving a single settings value as a
             * String.
             * 
             * @param name The name of the setting to retrieve.
             * @param def The value to return if the setting is not defined.
             * @return The setting's current value or 'def' if it is not
             *         defined.
             */
            private String getString(String name, String def) {
                ContentValues values = getValues(name);
                return values != null ? values.getAsString(VALUE) : def;
            }

            /**
             * Convenience function for retrieving a single settings value as an
             * Integer.
             * 
             * @param name The name of the setting to retrieve.
             * @param def The value to return if the setting is not defined.
             * @return The setting's current value or 'def' if it is not
             *         defined.
             */
            private int getInteger(String name, int def) {
                ContentValues values = getValues(name);
                return values != null ? values.getAsInteger(VALUE) : def;
            }

            /**
             * Convenience function for retrieving a single settings value as a
             * Long.
             * 
             * @param name The name of the setting to retrieve.
             * @param def The value to return if the setting is not defined.
             * @return The setting's current value or 'def' if it is not
             *         defined.
             */
            private long getLong(String name, long def) {
                ContentValues values = getValues(name);
                return values != null ? values.getAsLong(VALUE) : def;
            }
        }

    }

    /**
     * Columns for IM branding resource map cache table. This table caches the
     * result of loading the branding resources to speed up IM landing page
     * start.
     */
    public interface BrandingResourceMapCacheColumns {
        /** The provider ID <P>Type: INTEGER</P> */
        String PROVIDER_ID = "provider_id";
        /** The application resource ID <P>Type: INTEGER</P> */
        String APP_RES_ID = "app_res_id";
        /** The plugin resource ID <P>Type: INTEGER</P> */
        String PLUGIN_RES_ID = "plugin_res_id";
    }

    /** The table for caching the result of loading IM branding resources. */
    public static final class BrandingResourceMapCache implements BaseColumns,
            BrandingResourceMapCacheColumns {
        /** The content:// style URL for this table. */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/brandingResMapCache");
    }

    /**
     * //TODO: move these to MCS specific provider. The following are MCS stuff,
     * and should really live in a separate provider specific to MCS code.
     */

    /** Columns from OutgoingRmq table */
    public interface OutgoingRmqColumns {
        String RMQ_ID = "rmq_id";
        String TIMESTAMP = "ts";
        String DATA = "data";
        String PROTOBUF_TAG = "type";
    }

    /**
     * //TODO: we should really move these to their own provider and database.
     * The table for storing outgoing rmq packets.
     */
    public static final class OutgoingRmq implements BaseColumns, OutgoingRmqColumns {
        private static String[] RMQ_ID_PROJECTION = new String[] { RMQ_ID, };

        /**
         * queryHighestRmqId
         * 
         * @param resolver the content resolver
         * @return the highest rmq id assigned to the rmq packet, or 0 if there
         *         are no rmq packets in the OutgoingRmq table.
         */
        public static final long queryHighestRmqId(ContentResolver resolver) {
            Cursor cursor = resolver.query(Imps.OutgoingRmq.CONTENT_URI_FOR_HIGHEST_RMQ_ID,
                    RMQ_ID_PROJECTION, null, // selection
                    null, // selection args
                    null // sort
                    );

            long retVal = 0;
            try {
                //if (DBG) log("initializeRmqid: cursor.count= " + cursor.count());

                if (cursor.moveToFirst()) {
                    retVal = cursor.getLong(cursor.getColumnIndexOrThrow(RMQ_ID));
                }
            } finally {
                cursor.close();
            }

            return retVal;
        }

        /** The content:// style URL for this table. */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/outgoingRmqMessages");

        /**
         * The content:// style URL for the highest rmq id for the outgoing rmq
         * messages
         */
        public static final Uri CONTENT_URI_FOR_HIGHEST_RMQ_ID = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/outgoingHighestRmqId");

        /** The default sort order for this table. */
        public static final String DEFAULT_SORT_ORDER = "rmq_id ASC";
    }

    /**
     * Columns for the LastRmqId table, which stores a single row for the last
     * client rmq id sent to the server.
     */
    public interface LastRmqIdColumns {
        String RMQ_ID = "rmq_id";
    }

    /**
     * //TODO: move these out into their own provider and database The table for
     * storing the last client rmq id sent to the server.
     */
    public static final class LastRmqId implements BaseColumns, LastRmqIdColumns {
        private static String[] PROJECTION = new String[] { RMQ_ID, };

        /**
         * queryLastRmqId
         * 
         * queries the last rmq id saved in the LastRmqId table.
         * 
         * @param resolver the content resolver.
         * @return the last rmq id stored in the LastRmqId table, or 0 if not
         *         found.
         */
        public static final long queryLastRmqId(ContentResolver resolver) {
            Cursor cursor = resolver.query(Imps.LastRmqId.CONTENT_URI, PROJECTION, null, // selection
                    null, // selection args
                    null // sort
                    );

            long retVal = 0;
            try {
                if (cursor.moveToFirst()) {
                    retVal = cursor.getLong(cursor.getColumnIndexOrThrow(RMQ_ID));
                }
            } finally {
                cursor.close();
            }

            return retVal;
        }

        /**
         * saveLastRmqId
         * 
         * saves the rmqId to the lastRmqId table. This will override the
         * existing row if any, as we only keep one row of data in this table.
         * 
         * @param resolver the content resolver.
         * @param rmqId the rmq id to be saved.
         */
        public static final void saveLastRmqId(ContentResolver resolver, long rmqId) {
            ContentValues values = new ContentValues();

            // always replace the first row.
            values.put(_ID, 1);
            values.put(RMQ_ID, rmqId);
            resolver.insert(CONTENT_URI, values);
        }

        /** The content:// style URL for this table. */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/lastRmqId");
    }

    /**
     * Columns for the s2dRmqIds table, which stores the server-to-device
     * message persistent ids. These are used in the RMQ2 protocol, where in the
     * login request, the client selective acks these s2d ids to the server.
     */
    public interface ServerToDeviceRmqIdsColumn {
        String RMQ_ID = "rmq_id";
    }

    public static final class ServerToDeviceRmqIds implements BaseColumns,
            ServerToDeviceRmqIdsColumn {

        /** The content:// style URL for this table. */
        public static final Uri CONTENT_URI = Uri
                .parse("content://info.guardianproject.otr.app.im.provider.Imps/s2dids");
    }

    public static boolean isUnlocked(Context context)
    {
        try {
            Cursor cursor = null;
            
            Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;
            
            Builder builder = uri.buildUpon();
            builder = builder.appendQueryParameter(ImApp.NO_CREATE_KEY, "1");
            
            uri = builder.build();
            
            cursor = context.getContentResolver().query(
                    uri, null, Imps.Provider.CATEGORY + "=?" /* selection */,
                    new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                    null);
 
            if (cursor != null)
            {
                cursor.close();
                return true;
            }
            else
            {
                return false;
            }
            
        } catch (Exception e) {
            // Only complain if we thought this password should succeed
            
             Log.e(ImApp.LOG_TAG, e.getMessage(), e);
            
            // needs to be unlocked
            return false;
        }
    }
    

    public static boolean isUnencrypted(Context context) {
        try {
            Cursor cursor = null;
            
            Uri uri = Imps.Provider.CONTENT_URI_WITH_ACCOUNT;
            
            Builder builder = uri.buildUpon();
            builder.appendQueryParameter(ImApp.CACHEWORD_PASSWORD_KEY, "");
            builder = builder.appendQueryParameter(ImApp.NO_CREATE_KEY, "1");
            
            uri = builder.build();
            
            cursor = context.getContentResolver().query(
                    uri, null, Imps.Provider.CATEGORY + "=?" /* selection */,
                    new String[] { ImApp.IMPS_CATEGORY } /* selection args */,
                    null);
 
            if (cursor != null)
            {
               cursor.close();
                return true;
            }
            else
            {
                return false;
            }
            
        } catch (Exception e) {
            // Only complain if we thought this password should succeed
            
             Log.e(ImApp.LOG_TAG, e.getMessage(), e);
            
            // needs to be unlocked
            return false;
        }
    }
    public static boolean setEmptyPassphrase(Context ctx, boolean noCreate) {
        String pkey = "";
    
        Uri uri = Provider.CONTENT_URI_WITH_ACCOUNT;
    
        Builder builder = uri.buildUpon().appendQueryParameter(ImApp.CACHEWORD_PASSWORD_KEY, pkey);
        if (noCreate) {
            builder.appendQueryParameter(ImApp.NO_CREATE_KEY, "1");
        }
        uri = builder.build();
    
        Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            cursor.close();
            return true;
        }
        return false;
    }

    public static void clearPassphrase(Context ctx) {
        Uri uri = Provider.CONTENT_URI_WITH_ACCOUNT;
    
        Builder builder = uri.buildUpon().appendQueryParameter(ImApp.CLEAR_PASSWORD_KEY, "1");
        uri = builder.build();
    
        Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            throw new RuntimeException("Unexpected cursor returned");
        }
    }

    public static Uri insertMessageInDb(ContentResolver resolver,
            boolean isGroup,
            long contactId,
            boolean isEncrypted,
            String contact,
            String body,
            long time,
            int type,
            int errCode,
            String id,
            String mimeType) {
        
        ContentValues values = new ContentValues();
        values.put(Imps.Messages.BODY, body);
        values.put(Imps.Messages.DATE, time);
        values.put(Imps.Messages.TYPE, type);
        values.put(Imps.Messages.ERROR_CODE, errCode);
        if (isGroup) {
            values.put(Imps.Messages.NICKNAME, contact);
            values.put(Imps.Messages.IS_GROUP_CHAT, 1);
        }
        values.put(Imps.Messages.IS_DELIVERED, 0);
        values.put(Imps.Messages.MIME_TYPE, mimeType);
        values.put(Imps.Messages.PACKET_ID, id);

        return resolver.insert(isEncrypted ? Messages.getOtrMessagesContentUriByThreadId(contactId) : Messages.getContentUriByThreadId(contactId), values);
    }

    public static int updateMessageBody(ContentResolver resolver, int id, String body) {
        
        Uri.Builder builder = Imps.Messages.OTR_MESSAGES_CONTENT_URI.buildUpon();
        builder.appendPath(String.valueOf(id));
        
        ContentValues values = new ContentValues();
        values.put(Imps.Messages.BODY, body);
        return resolver.update(builder.build(), values, null, null);
    }

    public static int updateConfirmInDb(ContentResolver resolver, String id, boolean isDelivered) {
        Uri.Builder builder = Imps.Messages.OTR_MESSAGES_CONTENT_URI_BY_PACKET_ID.buildUpon();
        builder.appendPath(id);
        
        ContentValues values = new ContentValues(1);
        values.put(Imps.Messages.IS_DELIVERED, isDelivered);
        return resolver.update(builder.build(), values, null, null);
    }


    
}
