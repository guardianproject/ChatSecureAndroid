package info.guardianproject.otr.app.im.plugin.xmpp;

/**
 * $RCSfile$ $Revision: $ $Date: $
 * 
 * Copyright 2003-2005 Jive Software.
 * 
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import info.guardianproject.bouncycastle.asn1.ASN1Object;
import info.guardianproject.bouncycastle.asn1.ASN1OctetString;
import info.guardianproject.bouncycastle.asn1.DERSequence;
import info.guardianproject.bouncycastle.asn1.DERString;
import info.guardianproject.bouncycastle.asn1.x509.GeneralName;
import info.guardianproject.bouncycastle.asn1.x509.X509Extensions;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.service.RemoteImService;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.ConnectionConfiguration;

import android.content.Context;
import android.util.Log;

/**
 * Trust manager that checks all certificates presented by the server. This
 * class is used during TLS negotiation. It is possible to disable/enable some
 * or all checkings by configuring the {@link ConnectionConfiguration}. The
 * truststore file that contains knows and trusted CA root certificates can also
 * be configure in {@link ConnectionConfiguration}.
 * 
 * @author Gaston Dombiak
 */
class ServerTrustManager implements X509TrustManager {

    private static final String TAG = "GB.SSL";
    private final static Pattern cnPattern = Pattern.compile("(?i)(cn=)([^,]*)");
    private final static Pattern oPattern = Pattern.compile("(?i)(o=)([^,]*)");

    private final static String FINGERPRINT_TYPE = "SHA1";

    private ConnectionConfiguration configuration;

    /** Holds the domain of the remote server we are trying to connect */
    private String server;
    private String domain;
    private KeyStore trustStore;

    /**
     * Construct a trust manager for XMPP connections. Certificates are
     * considered verified if:
     * 
     * <ul> <li>The root certificate is in our trust store <li>The chain is
     * valid <li>The leaf certificate contains the identity of the domain or the
     * requested server </ul>
     * 
     * @param context - the Android context for presenting notifications
     * @param domain - the domain requested by the user
     * @param requestedServer - the connect server requested by the user
     * @param configuration - the XMPP configuration
     */
    public ServerTrustManager(Context context, String domain, String requestedServer,
            ConnectionConfiguration configuration) {
        this.configuration = configuration;
        this.domain = domain;
        this.server = requestedServer;
        if (this.server == null) {
            this.server = domain;
        }

        InputStream in = null;
        try {
            trustStore = KeyStore.getInstance(configuration.getTruststoreType());

            //TODO add the ability to load  custom cacerts file from SDCard
            /*
             *
            if (new File(configuration.getTruststorePath()).exists())
            	in = new FileInputStream(configuration.getTruststorePath());
            else
            */

            //load our bundled cacerts from raw assets
            in = context.getResources().openRawResource(R.raw.cacerts);

            trustStore.load(in, configuration.getTruststorePassword().toCharArray());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            // Disable root CA checking
            configuration.setVerifyRootCAEnabled(false);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // Ignore.
                }
            }
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String arg1)
            throws CertificateException {

       // android.os.Debug.waitForDebugger();
        
        int nSize = x509Certificates.length;

        Collection<String> peerIdentities = getPeerIdentity(x509Certificates[0]);

        if (configuration.isVerifyChainEnabled()) {
            // Working down the chain, for every certificate in the chain,
            // verify that the subject of the certificate is the issuer of the
            // next certificate in the chain.
            Principal principalLast = null;
            for (int i = nSize - 1; i >= 0; i--) {
                X509Certificate x509certificate = x509Certificates[i];

                Principal principalIssuer = x509certificate.getIssuerDN();
                Principal principalSubject = x509certificate.getSubjectDN();
                if (principalLast != null) {
                    if (principalIssuer.equals(principalLast)) {
                        try {
                            PublicKey publickey = x509Certificates[i + 1].getPublicKey();
                            x509Certificates[i].verify(publickey);
                        }

                        catch (GeneralSecurityException generalsecurityexception) {
                            Log.d("GB.TLS", "signature verification failed " + principalIssuer.getName());

                            throw new CertificateException("signature verification failed of "
                                                           + principalIssuer.getName());
                        }
                    } else {
                        Log.d("GB.TLS", "subject/issuer verification failed " + principalIssuer.getName());

                        throw new CertificateException("subject/issuer verification failed of "
                                                       + principalIssuer.getName());
                    }
                }
                principalLast = principalSubject;
            }
        }

        if (configuration.isVerifyRootCAEnabled()) {
            // Verify that the the last certificate in the chain was issued
            // by a third-party that the client trusts.
            boolean trusted = false;

            try {

                if (configuration.isSelfSignedCertificateEnabled()) {
                    
                    String fingerprint = "";
                    
                    try 
                    {
                        fingerprint = getFingerprint(x509Certificates[0], FINGERPRINT_TYPE);
                    }
                    catch (Exception e)
                    {
                        fingerprint = "unable to read fingerprint";
                    }
                    
                    Log.d("GB.TLS", "self-signed certificate");

                    trusted = true;
                } else {

                    X509Certificate certFinal = x509Certificates[nSize - 1];

                    Enumeration<String> enumAliases = trustStore.aliases();
                    while (enumAliases.hasMoreElements()) {

                        X509Certificate cert = (X509Certificate) trustStore
                                .getCertificate(enumAliases.nextElement());

                        String caSubject = cert.getSubjectDN().getName();
                        String issuerSubject = certFinal.getIssuerDN().getName();

                        Matcher matcher = oPattern.matcher(caSubject);
                        if (matcher.find()) {
                            caSubject = matcher.group(2);
                        }

                        matcher = oPattern.matcher(issuerSubject);
                        if (matcher.find()) {
                            issuerSubject = matcher.group(2);
                        }

                        if (caSubject.equals(issuerSubject)) {
                            try {
                                certFinal.verify(cert.getPublicKey());
                                trusted = true;

                          //      Log.d(TAG, "TLS/SSL Certificate Verified " + getFingerprint(certFinal, FINGERPRINT_TYPE));
//                                showCertMessage("TLS/SSL Certificate Verified",
//                                        getFingerprint(certFinal, FINGERPRINT_TYPE), certFinal);

                            } catch (Exception e) {
                                RemoteImService.debug("error on ssl verify", e);
                                trusted = false;
                            }

                        }

                        if (trusted) {
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                RemoteImService.debug("error on ssl verify", e);
                trusted = false;
               
            }

            if (!trusted) {
                
                String fingerprint = "";
                
                try
                {
                    fingerprint = getFingerprint(x509Certificates[0], FINGERPRINT_TYPE);
                }
                catch (Exception e)
                {
                    //may not even be able to get fingerprint properly
                    fingerprint = "unable to read fingerprint";
                }
                
                Log.d("GB.TLS", "root certificate not trusted");
                throw new CertificateException("root certificate not trusted of " + peerIdentities);
            }
        }

        if (configuration.isNotMatchingDomainCheckEnabled()) {
            // Verify that the first certificate in the chain corresponds to
            // the server we desire to authenticate.
            boolean found = checkMatchingDomain(domain, server, peerIdentities);

            if (!found) {
                Log.d("GB.TLS", "domain check failed " + join(peerIdentities) + " does not contain '"
                                                       + server + "' or '" + domain + "'");

                throw new CertificateException("target verification failed of " + peerIdentities);
            }
        }

        if (configuration.isExpiredCertificatesCheckEnabled()) {
            // For every certificate in the chain, verify that the certificate
            // is valid at the current time.
            Date date = new Date();
            for (int i = 0; i < nSize; i++) {
                try {
                    x509Certificates[i].checkValidity(date);
                } catch (GeneralSecurityException generalsecurityexception) {
                    Log.d("GB.TLS", "certificate expired " + x509Certificates[i].getNotAfter());
                    throw new CertificateException("invalid date of " + server);
                }
            }
        }

    }

    static boolean checkMatchingDomain(String domain, String server, Collection<String> peerIdentities) {
        boolean found = false;
        for (String peerIdentity : peerIdentities) {
            // Check if the certificate uses a wildcard.
            // This indicates that immediate subdomains are valid.
            if (peerIdentity.startsWith("*.")) {
                // Remove wildcard: *.foo.info -> .foo.info
                String stem = peerIdentity.substring(1);

                // Remove a single label: baz.bar.foo.info -> .bar.foo.info and compare
                if (server.equalsIgnoreCase(stem)
                        || domain.equalsIgnoreCase(stem)
                        || server.replaceFirst("[^.]+", "").equalsIgnoreCase(stem)
                    || domain.replaceFirst("[^.]+", "").equalsIgnoreCase(stem)
        
                        ) {
                    found = true;
                    break;
                }
            } else {
                if (server.equalsIgnoreCase(peerIdentity) || domain.equalsIgnoreCase(peerIdentity)) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    private String join(Collection<String> strs) {
        boolean first = true;
        StringBuffer buf = new StringBuffer();
        for (String str : strs) {
            if (!first) {
                buf.append(':');
            }
            first = false;
            buf.append(str);
        }
        return buf.toString();
    }

    /**
     * Returns the identity of the remote server as defined in the specified
     * certificate. The identity is defined in the subjectDN of the certificate
     * and it can also be defined in the subjectAltName extension of type
     * "xmpp". When the extension is being used then the identity defined in the
     * extension in going to be returned. Otherwise, the value stored in the
     * subjectDN is returned.
     * 
     * @param x509Certificate the certificate the holds the identity of the
     *            remote server.
     * @return the identity of the remote server as defined in the specified
     *         certificate.
     */
    public static Collection<String> getPeerIdentity(X509Certificate x509Certificate) {
        // Look the identity in the subjectAltName extension if available
        Collection<String> names = getSubjectAlternativeNames(x509Certificate);
        if (names.isEmpty()) {
            String name = x509Certificate.getSubjectDN().getName();
            Matcher matcher = cnPattern.matcher(name);
            if (matcher.find()) {
                name = matcher.group(2);
            }
            // Create an array with the unique identity
            names = new ArrayList<String>();
            names.add(name);
        }
        return names;
    }

    /**
     * Returns the JID representation of an XMPP entity contained as a
     * SubjectAltName extension in the certificate. If none was found then
     * return <tt>null</tt>.
     * 
     * @param certificate the certificate presented by the remote entity.
     * @return the JID representation of an XMPP entity contained as a
     *         SubjectAltName extension in the certificate. If none was found
     *         then return <tt>null</tt>.
     */
    static Collection<String> getSubjectAlternativeNames(X509Certificate certificate) {
        List<String> identities = new ArrayList<String>();
        try {
            byte[] extVal = certificate.getExtensionValue(X509Extensions.SubjectAlternativeName.getId());
            // Check that the certificate includes the SubjectAltName extension
            if (extVal == null) {
                return Collections.emptyList();
            }

            ASN1OctetString octs = (ASN1OctetString)ASN1Object.fromByteArray(extVal);
            
            @SuppressWarnings("rawtypes")
            Enumeration it = DERSequence.getInstance(ASN1Object.fromByteArray(octs.getOctets())).getObjects();
            
            while (it.hasMoreElements())
            {
                GeneralName genName = GeneralName.getInstance(it.nextElement());
                switch (genName.getTagNo())
                {
                case GeneralName.dNSName:
                    identities.add(((DERString)genName.getName()).getString());
                    break;
                }
            }
            return Collections.unmodifiableCollection(identities);
        } catch (IOException e) {
            Log.w(TAG, e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return identities;
    }

    public String getFingerprint(X509Certificate cert, String type) throws NoSuchAlgorithmException, CertificateEncodingException 
    {
        
            MessageDigest md = MessageDigest.getInstance(type);
            byte[] publicKey = md.digest(cert.getEncoded());

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {

                String appendString = Integer.toHexString(0xFF & publicKey[i]);

                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(' ');
            }

            return hexString.toString();

    }

}
