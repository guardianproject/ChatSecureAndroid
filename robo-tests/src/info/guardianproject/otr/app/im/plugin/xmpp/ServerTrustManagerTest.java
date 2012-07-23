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

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

/**
 * @author devrandom
 */
public class ServerTrustManagerTest extends EasyMockSupport {
    X509Certificate cert;
    
    /**
     * Set up server trust manager tests.
     * 
     * <p>IMPORTANT: assumes working directory is parent project.
     * If you are in Eclipse, you have to change the working directory
     * of the run configuration.
     */
    @Before
    public void setUp() throws Exception {
        InputStream inStream = new FileInputStream("robo-tests/etc/jabber.org.pem");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        cert = (X509Certificate)cf.generateCertificate(inStream);
    }

    @Test
    public void testGetPeerIdentity() throws Exception {
        Collection<String> names = ServerTrustManager.getPeerIdentity(cert);
        assertArrayEquals(new String[]{"*.jabber.org", "jabber.org"}, names.toArray(new String[0]));
    }

    @Test
    public void testCheckMatchingDomain() throws Exception {
        Collection<String> names = ServerTrustManager.getPeerIdentity(cert);
        assertTrue(ServerTrustManager.checkMatchingDomain("jabber.org", "", names));
        assertTrue(ServerTrustManager.checkMatchingDomain("JabBer.Org", "", names));
        assertTrue(ServerTrustManager.checkMatchingDomain("xyz.jabber.org", "", names));
        assertFalse(ServerTrustManager.checkMatchingDomain("abc.xyz.jabber.org", "", names));
        assertFalse(ServerTrustManager.checkMatchingDomain("org", "", names));
        assertFalse(ServerTrustManager.checkMatchingDomain("xjabber.org", "", names));
        assertTrue(ServerTrustManager.checkMatchingDomain("", "jabber.org", names));
        assertFalse(ServerTrustManager.checkMatchingDomain("", "xjabber.org", names));
    }
}
