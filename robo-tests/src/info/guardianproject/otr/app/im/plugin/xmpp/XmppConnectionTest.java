/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package info.guardianproject.otr.app.im.plugin.xmpp;

import static org.easymock.EasyMock.*;

import java.util.ArrayList;

import static junit.framework.Assert.*;

import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ContactList;
import info.guardianproject.otr.app.im.engine.ContactListListener;
import info.guardianproject.otr.app.im.engine.SubscriptionRequestListener;
import info.guardianproject.otr.app.im.plugin.XmppAddress;
import info.guardianproject.otr.app.im.plugin.xmpp.XmppConnection;
import info.guardianproject.otr.app.im.plugin.xmpp.XmppConnection.MyXMPPConnection;
import info.guardianproject.otr.app.im.plugin.xmpp.XmppConnection.XmppContactList;

import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Packet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.RobolectricTestRunner;

/**
 * @author devrandom
 */
@RunWith(RobolectricTestRunner.class)
public class XmppConnectionTest extends EasyMockSupport {
    private static final String DEFAULT_GROUP_NAME = "Buddies";
    private static final String TEST_CONTACT_NAME = "test";
    private static final String TEST_CONTACT = "test@test.com";
    private XmppConnection con;
    private Roster roster;
    private MyXMPPConnection smackCon;
    private ImApp app;
    private XmppContactList contactListManager;
    private SubscriptionRequestListener subscriptionRequestListener;
    private ContactListListener listener;

    @Before
    public void setUp() throws Exception {
        app = new ImApp();
        con = new XmppConnection(app);
        roster = createMock(Roster.class);
        smackCon = createMock(MyXMPPConnection.class);
        expect(smackCon.getRoster()).andStubReturn(roster);
        Contact user = new Contact(new XmppAddress(TEST_CONTACT), TEST_CONTACT_NAME);
        con.initConnection(smackCon, user, XmppConnection.LOGGED_IN);
        contactListManager = con.getContactListManager();
        subscriptionRequestListener = createMock(SubscriptionRequestListener.class);
        contactListManager.setSubscriptionRequestListener(subscriptionRequestListener);
        listener = createMock(ContactListListener.class);
        contactListManager.addContactListListener(listener);
        expect(smackCon.isConnected()).andStubReturn(true);
    }

    @Test
    public void testApproveSubscription() throws Exception {
        expectInitialListCreation();
        Capture<RosterListener> listenerCapture = new Capture<RosterListener>();
        roster.addRosterListener(capture(listenerCapture));
        expectLastCall();
        subscriptionRequestListener.onSubscriptionApproved(TEST_CONTACT);
        expectLastCall().times(2);
        smackCon.sendPacket(anyObject(Packet.class));
        expectLastCall().times(2);
        expect(roster.getGroups()).andReturn(new ArrayList<RosterGroup>());
        expect(roster.getUnfiledEntryCount()).andStubReturn(0);
        roster.createEntry(eq(TEST_CONTACT), eq(TEST_CONTACT_NAME),
                aryEq(new String[] { DEFAULT_GROUP_NAME }));
        expectLastCall().times(2);
        listener.onContactChange(eq(ContactListListener.LIST_CONTACT_ADDED),
                anyObject(ContactList.class), anyObject(Contact.class));
        expectLastCall();
        replayAll();
        contactListManager.listenToRoster(roster);
        contactListManager.loadContactLists();
        contactListManager.approveSubscriptionRequest(TEST_CONTACT);
        // Second time should not call notifyContactListUpdated, since contact
        // already exists
        contactListManager.approveSubscriptionRequest(TEST_CONTACT);
        assertEquals(1, contactListManager.getContactLists().size());
        assertNotNull(contactListManager.getContactList(DEFAULT_GROUP_NAME));
        assertTrue(con.joinGracefully());
        verifyAll();
    }

    // Approve a subscription while the server already has a Buddies group
    @Test
    public void testApproveSubscription_serverBuddies() throws Exception {
        expectInitialListCreation();
        Capture<RosterListener> listenerCapture = new Capture<RosterListener>();
        roster.addRosterListener(capture(listenerCapture));
        expectLastCall();
        subscriptionRequestListener.onSubscriptionApproved(TEST_CONTACT);
        expectLastCall();
        smackCon.sendPacket(anyObject(Packet.class));
        expectLastCall();
        final ArrayList<RosterGroup> groups = new ArrayList<RosterGroup>();
        RosterGroup buddiesGroup = createNiceMock(RosterGroup.class);
        expect(buddiesGroup.getName()).andStubReturn(DEFAULT_GROUP_NAME);
        expect(buddiesGroup.getEntries()).andStubReturn(new ArrayList<RosterEntry>());
        groups.add(buddiesGroup);
        expect(roster.getGroups()).andReturn(groups);
        expect(roster.getUnfiledEntryCount()).andStubReturn(0);
        roster.createEntry(eq(TEST_CONTACT), eq(TEST_CONTACT_NAME),
                aryEq(new String[] { DEFAULT_GROUP_NAME }));
        expectLastCall();
        listener.onContactChange(eq(ContactListListener.LIST_CONTACT_ADDED),
                anyObject(ContactList.class), anyObject(Contact.class));
        expectLastCall();
        replayAll();
        contactListManager.listenToRoster(roster);
        contactListManager.loadContactLists();
        contactListManager.approveSubscriptionRequest(TEST_CONTACT);
        assertEquals(1, contactListManager.getContactLists().size());
        assertNotNull(contactListManager.getContactList(DEFAULT_GROUP_NAME));
        assertTrue(con.joinGracefully());
        verifyAll();
    }

    private void expectInitialListCreation() {
        listener.onContactChange(eq(ContactListListener.LIST_CREATED),
                anyObject(ContactList.class), eq((Contact) null));
        expectLastCall();
        listener.onContactsPresenceUpdate(anyObject(Contact[].class));
        expectLastCall();
        listener.onAllContactListsLoaded();
        expectLastCall();
    }
}
