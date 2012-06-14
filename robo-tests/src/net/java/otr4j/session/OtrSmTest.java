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

package net.java.otr4j.session;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Properties;

import javax.crypto.interfaces.DHPublicKey;

import net.java.otr4j.OtrException;
import net.java.otr4j.OtrKeyManagerImpl;
import net.java.otr4j.OtrKeyManagerStore;
import net.java.otr4j.crypto.SM;
import net.java.otr4j.crypto.SM.SMException;
import net.java.otr4j.session.OtrSm;
import net.java.otr4j.session.OtrSm.OtrSmEngineHost;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.TLV;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jivesoftware.smack.util.Base64;
import org.junit.Before;
import org.junit.Test;

public class OtrSmTest extends EasyMockSupport {
    class MemoryPropertiesStore implements OtrKeyManagerStore {
        private Properties properties = new Properties();

        public MemoryPropertiesStore() {
        }

        public void setProperty(String id, boolean value) {
            properties.setProperty(id, "true");
        }

        public void setProperty(String id, byte[] value) {
            properties.setProperty(id, new String(Base64.encodeBytes(value)));
        }

        public void removeProperty(String id) {
            properties.remove(id);

        }

        public byte[] getPropertyBytes(String id) {
            String value = properties.getProperty(id);

            if (value != null)
                return Base64.decode(value);
            return null;
        }

        public boolean getPropertyBoolean(String id, boolean defaultValue) {
            try {
                return Boolean.valueOf(properties.get(id).toString());
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    OtrSm sm_a;
    OtrSm sm_b;
    private OtrKeyManagerImpl manager_a;
    private OtrKeyManagerImpl manager_b;
    private SessionID sessionId_a;
    private SessionID sessionId_b;
    private OtrSmEngineHost host_a;
    private OtrSmEngineHost host_b;
    private Session session_a;
    private Session session_b;

    @Before
    public void setUp() throws Exception {
        manager_a = new OtrKeyManagerImpl(new MemoryPropertiesStore());
        manager_b = new OtrKeyManagerImpl(new MemoryPropertiesStore());
        session_a = createMock(Session.class);
        session_b = createMock(Session.class);
        AuthContextImpl ca = new AuthContextImpl(session_a);
        AuthContextImpl cb = new AuthContextImpl(session_b);
        ca.setRemoteDHPublicKey((DHPublicKey) cb.getLocalDHKeyPair().getPublic());
        cb.setRemoteDHPublicKey((DHPublicKey) ca.getLocalDHKeyPair().getPublic());
        EasyMock.expect(session_a.getS()).andStubReturn(ca.getS());
        EasyMock.expect(session_b.getS()).andStubReturn(cb.getS());
        sessionId_a = new SessionID("a1", "ua", "xmpp");
        sessionId_b = new SessionID("a1", "ub", "xmpp");
        manager_a.generateLocalKeyPair(sessionId_a);
        manager_b.generateLocalKeyPair(sessionId_b);
        manager_a.savePublicKey(sessionId_a, manager_b.loadLocalKeyPair(sessionId_b).getPublic());
        manager_b.savePublicKey(sessionId_b, manager_a.loadLocalKeyPair(sessionId_a).getPublic());
        host_a = createNiceMock(OtrSmEngineHost.class);
        host_b = createNiceMock(OtrSmEngineHost.class);
        sm_a = new OtrSm(session_a, manager_a, sessionId_a, host_a);
        sm_b = new OtrSm(session_b, manager_b, sessionId_b, host_b);
    }

    @Test
    public void testSuccess() throws Exception {
        replayAll();
        List<TLV> tlvs = sm_a.initRespondSmp(null, "xyz", true);
        assertEquals(SM.EXPECT2, sm_a.smstate.nextExpected);
        assertEquals(1, tlvs.size());

        runMiddleOfProtocol(tlvs);
        assertTrue(manager_b.isVerified(sessionId_b));

        assertTrue(manager_a.isVerified(sessionId_a));
    }

    @Test
    public void testSuccess_question() throws Exception {
        replayAll();
        List<TLV> tlvs = sm_a.initRespondSmp("qqq", "xyz", true);
        assertEquals(SM.EXPECT2, sm_a.smstate.nextExpected);
        assertEquals(1, tlvs.size());

        runMiddleOfProtocol(tlvs);

        assertTrue(manager_b.isVerified(sessionId_b));

        assertTrue(manager_a.isVerified(sessionId_a));
    }

    @Test
    public void testFailure() throws Exception {
        replayAll();
        List<TLV> tlvs = sm_a.initRespondSmp(null, "abc", true);
        assertEquals(SM.EXPECT2, sm_a.smstate.nextExpected);
        assertEquals(1, tlvs.size());

        runMiddleOfProtocol(tlvs);

        assertFalse(manager_b.isVerified(sessionId_b));

        assertFalse(manager_a.isVerified(sessionId_a));
    }

    @Test
    public void testFailure_question() throws Exception {
        replayAll();
        List<TLV> tlvs = sm_a.initRespondSmp("qqq", "abc", true);
        assertEquals(SM.EXPECT2, sm_a.smstate.nextExpected);
        assertEquals(1, tlvs.size());

        runMiddleOfProtocol(tlvs);

        assertFalse(manager_b.isVerified(sessionId_b));

        assertFalse(manager_a.isVerified(sessionId_a));
    }

    private void runMiddleOfProtocol(List<TLV> tlvs) throws SMException, OtrException {
        sm_b.processTlv(tlvs.get(0));
        assertEquals(SM.EXPECT1, sm_b.smstate.nextExpected);
        assertNull(sm_b.getPendingTlvs());

        tlvs = sm_b.initRespondSmp(null, "xyz", false);
        assertEquals(SM.EXPECT3, sm_b.smstate.nextExpected);
        assertEquals(1, tlvs.size());

        sm_a.processTlv(tlvs.get(0));
        assertEquals(SM.EXPECT4, sm_a.smstate.nextExpected);
        assertEquals(1, sm_a.getPendingTlvs().size());

        assertFalse(manager_a.isVerified(sessionId_a));
        assertFalse(manager_b.isVerified(sessionId_b));

        sm_b.processTlv(sm_a.getPendingTlvs().get(0));
        assertEquals(SM.EXPECT1, sm_b.smstate.nextExpected);
        assertEquals(1, sm_b.getPendingTlvs().size());

        sm_a.processTlv(sm_b.getPendingTlvs().get(0));
        assertEquals(SM.EXPECT1, sm_a.smstate.nextExpected);

        assertNull(sm_a.getPendingTlvs());
    }
}
