package info.guardianproject.otr.app.im.plugin.loopback;

import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.ChatGroupManager;
import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.ChatSessionManager;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ContactList;
import info.guardianproject.otr.app.im.engine.ContactListListener;
import info.guardianproject.otr.app.im.engine.ContactListManager;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImException;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.Presence;
import info.guardianproject.otr.app.im.plugin.xmpp.XmppAddress;
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.ContentResolver;
import android.os.Parcel;
import android.os.Parcelable;

public class LoopbackConnection extends ImConnection {

    protected static final String TAG = "LoopbackConnection";
    private LoopbackContactList mContactListManager;
    private Contact mUser;

    public LoopbackConnection() {
        super(null);
    }

    @Override
    protected void doUpdateUserPresenceAsync(Presence presence) {
        // mimic presence
        ContactList cl;
        try {
            cl = mContactListManager.getDefaultContactList();
        } catch (ImException e) {
            throw new RuntimeException(e);
        }
        if (cl == null)
            return;
        Collection<Contact> contacts = cl.getContacts();
        for (Iterator<Contact> iter = contacts.iterator(); iter.hasNext();) {
            Contact contact = iter.next();
            contact.setPresence(presence);
        }
        Contact[] contacts_array = new Contact[contacts.size()];
        contacts.toArray(contacts_array);
        mContactListManager.doPresence(contacts_array);
    }

    public void initUser(long providerId, long accountId)
    {
       
        mUser = makeUser();
    }
    
    private Contact makeUser() {
       
        return new Contact(new XmppAddress("test@foo"), "test");
    }
    
    @Override
    public int getCapability() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ChatGroupManager getChatGroupManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChatSessionManager getChatSessionManager() {
        return new ChatSessionManager() {

            @Override
            public void sendMessageAsync(ChatSession session, Message message) {
                // Echo
                Message rec = new Message(message.getBody());
                rec.setFrom(message.getTo());
                rec.setDateTime(new Date());
                session.onReceiveMessage(rec);
            }

        };
    }

    @Override
    public ContactListManager getContactListManager() {
        mContactListManager = new LoopbackContactList();
        return mContactListManager;
    }

    @Override
    public Contact getLoginUser() {
        return mUser;
    }

    @Override
    public HashMap<String, String> getSessionContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int[] getSupportedPresenceStatus() {
        return new int[] { Presence.AVAILABLE, Presence.AWAY, Presence.IDLE, Presence.OFFLINE,
                          Presence.DO_NOT_DISTURB, };
    }

    @Override
    public void loginAsync(long accountId, String passwordTemp, long providerId, boolean retry) {
        ContentResolver contentResolver = mContext.getContentResolver();
        String userName = Imps.Account.getUserName(contentResolver, accountId);
        mUserPresence = new Presence(Presence.AVAILABLE, "available", null, null,
                Presence.CLIENT_TYPE_DEFAULT);
        mUser = new Contact(new LoopbackAddress(userName + "!", "loopback", null), userName);
        setState(LOGGED_IN, null);
    }

    @Override
    public void logoutAsync() {
        // TODO Auto-generated method stub

    }

    @Override
    public void reestablishSessionAsync(Map<String, String> sessionContext) {
        // TODO Auto-generated method stub

    }

    @Override
    public void suspend() {
        // TODO Auto-generated method stub

    }

    private final class LoopbackContactList extends ContactListManager {
        @Override
        protected void setListNameAsync(String name, ContactList list) {
            // TODO Auto-generated method stub

        }

        @Override
        public String normalizeAddress(String address) {
            return address;
        }

        @Override
        public void loadContactListsAsync() {
            Collection<Contact> contacts = new ArrayList<Contact>();
            Contact[] contacts_array = new Contact[1];
            contacts.toArray(contacts_array);
            Address dummy_addr = new LoopbackAddress("dummy", "dummy@google.com",null);

            Contact dummy = new Contact(dummy_addr, "dummy");
            dummy.setPresence(new Presence(Presence.AVAILABLE, "available", null, null,
                    Presence.CLIENT_TYPE_DEFAULT));
            contacts.add(dummy);

            ContactList cl = new ContactList(mUser.getAddress(), "default", true, contacts, this);
            mContactLists.add(cl);
            mDefaultContactList = cl;
            notifyContactListLoaded(cl);
            notifyContactsPresenceUpdated(contacts.toArray(contacts_array));
            notifyContactListsLoaded();
        }

        public void doPresence(Contact[] contacts) {
            notifyContactsPresenceUpdated(contacts);
        }

        @Override
        protected ImConnection getConnection() {
            return LoopbackConnection.this;
        }

        @Override
        protected void doRemoveContactFromListAsync(Contact contact, ContactList list) {
            // TODO Auto-generated method stub

        }

        @Override
        protected void doDeleteContactListAsync(ContactList list) {
            // TODO Auto-generated method stub

        }

        @Override
        protected void doCreateContactListAsync(String name, Collection<Contact> contacts,
                boolean isDefault) {
            // TODO Auto-generated method stub
            return;

        }

        @Override
        protected void doBlockContactAsync(String address, boolean block) {
            // TODO Auto-generated method stub

        }

        @Override
        protected void doAddContactToListAsync(Contact contact, ContactList list) throws ImException {
            contact.setPresence(new Presence(Presence.AVAILABLE, "available", null, null,
                    Presence.CLIENT_TYPE_DEFAULT));
            notifyContactListUpdated(list, ContactListListener.LIST_CONTACT_ADDED, contact);
            Contact[] contacts = new Contact[] { contact };
            mContactListManager.doPresence(contacts);
        }

        @Override
        public void declineSubscriptionRequest(Contact contact) {
            // TODO Auto-generated method stub

        }

        @Override
        public Contact[] createTemporaryContacts(String[] address) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void approveSubscriptionRequest(Contact contact) {
            // TODO Auto-generated method stub
            return;
        }

        @Override
        protected void doSetContactName(String address, String name) throws ImException {
            // stub - no server
        }
    }

    class LoopbackAddress extends Address {

        private String address;
        private String name;
        private String resource;
        
        public LoopbackAddress() {
        }

        public LoopbackAddress(String name, String address, String resource) {
            this.name = name;
            this.address = address;
            this.resource = resource;
        }

        @Override
        public String getBareAddress() {
            return address;
        }
        
        @Override
        public String getAddress() {
            return address;
        }

        @Override
        public String getUser() {
            return name;
        }
        
        @Override
        public String getResource() {
            return null;
        }

        @Override
        public void readFromParcel(Parcel source) {
            name = source.readString();
            address = source.readString();
            resource = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest) {
            dest.writeString(name);
            dest.writeString(address);
            dest.writeString(resource);
        }


    }

    public void sendHeartbeat(long heartbeatInterval) {
    }

    public void setProxy(String type, String host, int port) {
    }

    @Override
    public void logout() {
    }
}
