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
import static org.junit.Assert.*;

import java.util.List;

import info.guardianproject.otr.app.im.plugin.xmpp.XmppConnection.MyXMPPConnection;
import info.guardianproject.otr.app.im.plugin.xmpp.XmppStreamHandler.StreamHandlingPacket;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMockSupport;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.RobolectricTestRunner;

/**
 * @author devrandom
 */
@RunWith(RobolectricTestRunner.class)
public class XmppStreamHandlerTest extends EasyMockSupport {
    private MyXMPPConnection connection;
    private XmppStreamHandler handler;
    
    private Capture<ConnectionListener> connectionListenerCapture;
    private Capture<PacketInterceptor> interceptorCapture;
    private PacketInterceptor interceptor;
    private Capture<PacketListener> listenerCapture;
    private PacketListener listener;
    private Capture<Packet> packetCapture;
    
    @Before
    public void setUp() throws Exception {
        connection = createMock(MyXMPPConnection.class);
        connectionListenerCapture = new Capture<ConnectionListener>();
        interceptorCapture = new Capture<PacketInterceptor>();
        listenerCapture = new Capture<PacketListener>();
        packetCapture = new Capture<Packet>(CaptureType.ALL);

        connection.addConnectionListener(capture(connectionListenerCapture));
        expectLastCall().anyTimes();
        connection.addPacketListener(capture(listenerCapture), anyObject(PacketFilter.class));
        expectLastCall().anyTimes();
        connection.addPacketInterceptor(capture(interceptorCapture), anyObject(PacketFilter.class));
        expectLastCall().anyTimes();
        connection.sendPacket(capture(packetCapture));
        expectLastCall().anyTimes();
        
        Roster roster = createNiceMock(Roster.class);
        expect(connection.getRoster()).andStubReturn(roster);
        
        replay(connection, roster);
        handler = new XmppStreamHandler(connection);
        // Set max queue size to 10 and acks at 10/2 = 5
        handler.setMaxOutgoingQueueSize(10);
        listener = listenerCapture.getValue();
        interceptor = interceptorCapture.getValue();
    }

    @Test
    public void testNothing() throws Exception {
        sendInitial();
        
        incoming("r");
        expectOutgoing("a", "h", "0");
        
        incoming("r");
        expectOutgoing("a", "h", "0");
    }

    @Test
    public void testSomething() throws Exception {
        sendInitial();
        
        incoming("foo");
        incoming("bar");
        incoming("r");
        expectOutgoing("a", "h", "2");
    }

    @Test
    public void testResumeIncoming() throws Exception {
        sendInitial();
        
        incoming("foo");
        incoming("bar");
        
        connectionListenerCapture.getValue().connectionClosedOnError(new RuntimeException());
        incoming("sm");
        connectionListenerCapture.getValue().reconnectionSuccessful();
        expectOutgoing("resume", "previd", "123", "h", "2");
        incoming("resumed", "previd", "123", "h", "0");
    }

    @Test
    public void testResumeOutgoing() throws Exception {
        sendInitial();
        
        interceptor.interceptPacket(new Message("1@foo"));
        interceptor.interceptPacket(new Message("2@foo"));
        interceptor.interceptPacket(new Message("3@foo"));
        interceptor.interceptPacket(new Message("4@foo"));
        interceptor.interceptPacket(new Message("5@foo"));
        expectOutgoing("r");
        interceptor.interceptPacket(new Message("6@foo"));
        expectOutgoing("r");
        incoming("a", "h", "1");
        
        connectionListenerCapture.getValue().connectionClosedOnError(new RuntimeException());
        incoming("sm");
        connectionListenerCapture.getValue().reconnectionSuccessful();
        expectOutgoing("resume", "previd", "123", "h", "1");
        incoming("resumed", "previd", "123", "h", "4");
        List<Packet> packets = packetCapture.getValues();
        assertEquals(2, packets.size());
        assertEquals("5@foo", packets.get(0).getTo());
        assertEquals("6@foo", packets.get(1).getTo());
    }

    private void incoming(String name, String attribute, String value) {
        StreamHandlingPacket packet = new StreamHandlingPacket(name, XmppStreamHandler.URN_SM_2);
        packet.addAttribute(attribute, value);
        listener.processPacket(packet);
    }

    private void incoming(String name, String a1, String v1, String a2, String v2) {
        StreamHandlingPacket packet = new StreamHandlingPacket(name, XmppStreamHandler.URN_SM_2);
        packet.addAttribute(a1, v1);
        packet.addAttribute(a2, v2);
        listener.processPacket(packet);
    }

    private void incoming(String name) {
        listener.processPacket(new StreamHandlingPacket(name, XmppStreamHandler.URN_SM_2));
    }

    private void expectOutgoing(String name, String attribute, String value) {
        StreamHandlingPacket packet = (StreamHandlingPacket)packetCapture.getValue();
        assertEquals(name, packet.getElementName());
        assertEquals(value, packet.getAttribute(attribute));
        packetCapture.reset();
        // Feed expected packet into interceptor, because the connection is mocked out
        interceptor.interceptPacket(packet);
    }

    private void expectOutgoing(String name, String a1, String v1, String a2, String v2) {
        StreamHandlingPacket packet = (StreamHandlingPacket)packetCapture.getValue();
        assertEquals(name, packet.getElementName());
        assertEquals(v1, packet.getAttribute(a1));
        assertEquals(v2, packet.getAttribute(a2));
        packetCapture.reset();
        // Feed expected packet into interceptor, because the connection is mocked out
        interceptor.interceptPacket(packet);
    }

    private void expectOutgoing(String name) {
        StreamHandlingPacket packet = (StreamHandlingPacket)packetCapture.getValue();
        assertEquals(name, packet.getElementName());
        packetCapture.reset();
        // Feed expected packet into interceptor, because the connection is mocked out
        interceptor.interceptPacket(packet);
    }

    private void sendInitial() {
        incoming("sm");
        handler.notifyInitialLogin();
        expectOutgoing("enable");
        
        incoming("enabled", "id", "123", "resume", "1");
    }
}
