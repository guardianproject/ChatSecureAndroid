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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

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

    private ConnectionConfiguration configuration;

    /** Holds the domain of the remote server we are trying to connect */
    private String server;
    private String domain;
    
    private KeyStore mTrustStore; //root CAs
    private KeyStore mPinnedStore; //pinned certs

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
     * @throws KeyStoreException 
     * @throws IOException 
     * @throws CertificateException 
     * @throws NoSuchAlgorithmException 
     */
    public ServerTrustManager(Context context, String domain, String requestedServer,
            ConnectionConfiguration configuration) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        this.configuration = configuration;
        this.domain = domain;
        this.server = requestedServer;
        if (this.server == null) {
            this.server = domain;
        }

        InputStream in = null;
        
        mTrustStore = KeyStore.getInstance(configuration.getTruststoreType());
        //load our bundled cacerts from raw assets
        in = context.getResources().openRawResource(R.raw.cacerts);
        mTrustStore.load(in, configuration.getTruststorePassword().toCharArray());
        
        mPinnedStore = KeyStore.getInstance(configuration.getTruststoreType());
        //load our bundled cacerts from raw assets
        in = context.getResources().openRawResource(R.raw.pinnedcacerts);
        mPinnedStore.load(in, configuration.getTruststorePassword().toCharArray());
       
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0]; //we accept anyone now, but this should return the list from our trust Root CA Store
    }

    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        //not yet implemented
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String keyExchangeAlgo)
            throws CertificateException {
        
        
        //first check the main cert
        X509Certificate certSite = x509Certificates[0];
        checkStrongCrypto(certSite);        
        checkPinning(certSite);
        if (configuration.isExpiredCertificatesCheckEnabled())
            certSite.checkValidity();
        
        //then go through the chain
        if (configuration.isVerifyChainEnabled())
        {
            boolean verifiedRootCA = false;
            
            // for every certificate in the chain,
            // verify its issuer exists in the chain, or our local root CA store            
            for (int i = 0; i < x509Certificates.length; i++)
            {
                X509Certificate x509certCurr = x509Certificates[i];
                
                debug(i + ": verifying cert issuer for: " + x509certCurr.getSubjectDN() + "; " + x509certCurr.getSigAlgName());

                X509Certificate x509issuer = null;
                boolean isRootCA = false;
                
                for (X509Certificate x509search : x509Certificates)
                {                                      
                    if(checkSubjectMatchesIssuer(x509search.getSubjectX500Principal(),x509certCurr.getIssuerX500Principal()))                 
                    {                                                          
                        x509issuer = x509search;           
                        debug("found issuer for current cert in chain: " + x509issuer.getSubjectDN() + "; " + x509certCurr.getSigAlgName());
                        
                        //now check if it is a root
                        X509Certificate x509root = findCertIssuerInStore(x509certCurr, mTrustStore);
                        if (x509root != null)
                            isRootCA = true;
                        
                        break;
                    }
                }                           
                
                //this is now verifying against the root store
                //did not find signing cert in chain, so check our store
                if (x509issuer == null)
                {
                   x509issuer = findCertIssuerInStore(x509certCurr, mTrustStore);
                   isRootCA = true;
                }
                
                if (x509issuer != null) {
                    
                    try {
                        //check expiry
                        x509issuer.checkValidity();  
                        
                        if (!isRootCA) //MD5 collision not a risk for the Root CA in our store
                            checkStrongCrypto(x509issuer);
                                                
                        //verify cert with issuer public key
                        x509certCurr.verify(x509issuer.getPublicKey());
                        debug("SUCCESS: verified issuer: " + x509certCurr.getIssuerDN());

                        if (isRootCA)
                            verifiedRootCA = true;
                    }
                    

                    catch (GeneralSecurityException gse) {
                        Log.e(TAG,"ERROR: unverified issuer: " + x509certCurr.getIssuerDN());

                        throw new CertificateException("signature chain verification failed of "
                                                       + x509issuer.getIssuerDN().getName() + ": " + gse.getMessage());
                    }
                } 
                else {
                    

                    String errMsg = "Could not find cert issuer certificate in chain: " + x509certCurr.getIssuerDN().getName();
                    
                    Log.e(TAG,errMsg);
                    
                    throw new CertificateException(errMsg);
                }
                
            }
            
            if (configuration.isVerifyRootCAEnabled() && (!verifiedRootCA))
            {
                String errMsg = "Could not find Root CA issuer certificate in chain";
                
                Log.e(TAG,errMsg);
                
                throw new CertificateException(errMsg);
            }
        }
        else if (configuration.isExpiredCertificatesCheckEnabled())
        {    
            // at least check the validity of the chain
            for (X509Certificate x509cert : x509Certificates)
                x509cert.checkValidity();
        
        }        
        
        if (configuration.isSelfSignedCertificateEnabled())
        {
            boolean foundSelfSig = false;
                    
            // for every certificate in the chain,
            // verify its issuer exists in the chain, or our local root CA store            
            for (int i = 0; i < x509Certificates.length; i++)
            {
                X509Certificate x509certCurr = x509Certificates[i];
                
                debug(i + ": verifying cert issuer for: " + x509certCurr.getSubjectDN());

                X509Certificate x509issuer = null;
                
                for (X509Certificate x509search : x509Certificates)
                {                  
                    if(checkSubjectMatchesIssuer(x509search.getSubjectX500Principal(),x509certCurr.getIssuerX500Principal()))                 
                    {                                                          
                        x509issuer = x509search;           
                        debug("found issuer for current cert in chain: " + x509issuer.getSubjectDN());
                       
                        //check expiry
                        x509issuer.checkValidity();
                        
                        
                        try {
                            x509certCurr.verify(x509issuer.getPublicKey());
                            foundSelfSig = true;
                        }

                        catch (GeneralSecurityException gse) {
                            Log.e(TAG,"ERROR: unverified issuer: " + x509certCurr.getIssuerDN());

                            throw new CertificateException("signature chain verification failed of "
                                                           + x509issuer.getIssuerDN().getName() + ": " + gse.getMessage());
                        }
                        
                        debug("SUCCESS: verified issuer: " + x509certCurr.getIssuerDN());

                        break;
                    }
                }                           
            }            
            
            if (!foundSelfSig)
            {
                String errMsg = "Could not find self-signed certificate in chain";
                
                Log.e(TAG,errMsg);
                
                Log.d("GB.TLS", "root certificate not trusted");
                throw new CertificateException(errMsg);
            }
        }

        if (configuration.isNotMatchingDomainCheckEnabled())
        {
            //get peer identities available in the first cert in the chain
            Collection<String> peerIdentities = getPeerIdentity(x509Certificates[0]);
    
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
            for (int i = 0; i < x509Certificates.length; i++) {
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
                if (server.replaceFirst("[^.]+", "").equalsIgnoreCase(stem)
                    || domain.replaceFirst("[^.]+", "").equalsIgnoreCase(stem)        
                        ) {
                    found = true;
                    break;
                }
            } else if (server.equalsIgnoreCase(peerIdentity) 
                    || domain.equalsIgnoreCase(peerIdentity)) {
                    found = true;
                    break;              
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

    private X509Certificate findCertIssuerInStore (X509Certificate x509cert, KeyStore kStore) throws CertificateException
    {
        X509Certificate x509issuer = null;
        
        debug("searching store for issuer: " + x509cert.getIssuerDN());

        //check in our local root CA Store
        Enumeration<String> enumAliases;
        try {
            enumAliases = kStore.aliases();
            X509Certificate x509search = null;
            while (enumAliases.hasMoreElements()) {
                x509search = (X509Certificate) kStore
                        .getCertificate(enumAliases.nextElement());
                
                if(checkSubjectMatchesIssuer(x509search.getSubjectX500Principal(),x509cert.getIssuerX500Principal()))                 
                {                            
                    x509issuer = x509search;                    
                   debug("found issuer for current cert in chain in ROOT CA store: " + x509issuer.getSubjectDN());
                  
                   break;
               }
            }
        } catch (KeyStoreException e) {
          Log.e(TAG, "problem access local ROOT CA store",e);

          throw new CertificateException("problem access local ROOT CA store");
        }
       
        return x509issuer;
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
    
    private boolean checkSubjectMatchesIssuer (X500Principal subject, X500Principal issuer)
    {
        boolean result = false;
                
        if (Arrays.equals(subject.getEncoded(), issuer.getEncoded())) //byte by byte check
            if (subject.getName("RFC1779").equals(issuer.getName("RFC1779"))) //name check
                    result = true;
        
        return result;        
    }
    
    private void debug (String msg)
    {
        //disable for now
        //            Log.d(TAG, msg);
    }

    private void checkStrongCrypto (X509Certificate cert) throws CertificateException
    {
        String algo = cert.getSigAlgName().toLowerCase();
        
        if (!algo.contains("sha1") || algo.contains("sha256"))
        {
            debug("cert uses weak crypto: " + algo);

            Log.d("GB.TLS", "Cert uses weak crypto " + algo);
          // we will just WARN and not block for this, for now
          //  throw new CertificateException("issuer uses weak crypto: " + algo);
        }
        
    }
    
    private void checkPinning (X509Certificate x509cert) throws CertificateException
    {
        
        X500Principal certPrincipal = x509cert.getSubjectX500Principal();
        debug("checking pinning for: " + certPrincipal.getName("RFC1779"));
        
         Enumeration<String> enumAliases;
        try {
            enumAliases = mPinnedStore.aliases();
            X509Certificate x509search = null;
            while (enumAliases.hasMoreElements()) {
                x509search = (X509Certificate) mPinnedStore
                        .getCertificate(enumAliases.nextElement());
                
                X500Principal searchPrincipal = x509search.getSubjectX500Principal();
                debug("checking pinning against: " + searchPrincipal.getName("RFC1779"));
                
                if (certPrincipal.getName("RFC1779").equals(searchPrincipal.getName("RFC1779"))) //name check
                {
                    debug("matched pinning to: " + certPrincipal.getName("RFC1779"));
                    //found matching pinned cert, now check if the certs are the same
                    if (!Arrays.equals(certPrincipal.getEncoded(), searchPrincipal.getEncoded())) //byte by byte check                    
                    {
                        
                        Log.d("GB.TLS", "Provided Certificate Does Not Match PINNED Cert: " + certPrincipal.getName("RFC1779"));
                        
                        // just warn for now, don't block
                       // throw new CertificateException(context.getString(R.string.warning_pinned_cert_mismatch) + certPrincipal.getName("RFC1779"));
                        
                    }
                    
                    break;
                }
            }
        } catch (KeyStoreException e) {
          Log.e(TAG, "problem access local keystore",e);
          throw new CertificateException("problem access local keystore");
        }

    }
    
}
