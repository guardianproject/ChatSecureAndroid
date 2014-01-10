package info.guardianproject.otr.app.im.plugin.xmpp;

import java.util.ArrayList;

public class XMPPCertPins 
{
    

    public final static String[] SSL_IDEAL_CIPHER_SUITES = { 
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
    
    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
    
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
    
    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_RSA_WITH_RC4_128_SHA",
    
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
    
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
    
    "TLS_RSA_WITH_AES_256_CBC_SHA",
    "TLS_RSA_WITH_AES_128_CBC_SHA"
    
    };

    
    public static ArrayList<String> PINLIST = null;
    
    public static String[] getPinList ()
    {
        
        if (PINLIST == null)
        {
            
            PINLIST = new ArrayList<String>();
/*
## Certificate 0 ##
Subject: CN=xmpp.binaryparadox.net
Issuer: CN=xmpp.binaryparadox.net
SHA1 FP: 0B93EB84CCBB7AA2CB92CF61A0348F63CCED14C1
SPKI Pin: B3A7C02FC620C25F3C395AB043BF3C7729CE3C41

*/
    
        PINLIST.add("B3A7C02FC620C25F3C395AB043BF3C7729CE3C41");
    
    
       /***
       # Connecting to riseup.net [1 of 9 hosts]
               ## Found 2 pinnable certs in chain
               ### Certificate 1
               SubjectDN: CN=*.riseup.net, OU=Gandi Standard Wildcard SSL, OU=Domain Control Validated
               IssuerDN: CN=Gandi Standard SSL CA, O=GANDI SAS, C=FR
               Fingerprint: 1CFD0A83738A497B0399FB74E1E978A459F8546F
               SPKI Pin: 7D58173F276A483786D977DB35E836D482A3B687

               ### Certificate 2
               SubjectDN: CN=Gandi Standard SSL CA, O=GANDI SAS, C=FR
               IssuerDN: CN=UTN-USERFirst-Hardware, OU=http://www.usertrust.com, O=The USERTRUST Network, L=Salt Lake City, ST=UT, C=US
               Fingerprint: A9F79883A075CE82D20D274D1368E876140D33B3
               SPKI Pin: 636AB6EB0296E6C0681DB0C6CF3BB024BE267B8A
               **/
       
       
        PINLIST.add("7D58173F276A483786D977DB35E836D482A3B687");
        PINLIST.add("636AB6EB0296E6C0681DB0C6CF3BB024BE267B8A");
       
       
       /**


               # Connecting to im.mayfirst.org [2 of 9 hosts]
               ## Found 2 pinnable certs in chain
               ### Certificate 1
               SubjectDN: CN=im.mayfirst.org, OU=Domain Control Validated - RapidSSL(R), OU=See www.rapidssl.com/resources/cps (c)13, OU=GT71444429, SERIALNUMBER=oniG-AEd-Uiw40aQJ8j9SIAl7/BOvwOC
               IssuerDN: CN=RapidSSL CA, O="GeoTrust, Inc.", C=US
               Fingerprint: BF32F60E50B20711C4A3F51147F74285414CFE0E
               SPKI Pin: CCB6BAB6E52C4B448C4FFD04F13446E18B1B0420
               **/
        
        PINLIST.add("CCB6BAB6E52C4B448C4FFD04F13446E18B1B0420");

        /**
               ### Certificate 2
               SubjectDN: CN=RapidSSL CA, O="GeoTrust, Inc.", C=US
               IssuerDN: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
               Fingerprint: C039A3269EE4B8E82D00C53FA797B5A19E836F47
               SPKI Pin: A39399C404C3B209B081C21F21622778C2748E4C

    **/
        PINLIST.add("A39399C404C3B209B081C21F21622778C2748E4C");
        /**
         * # Connecting to jabber.ccc.de [1 of 1 hosts]
## Found 2 pinnable certs in chain
### Certificate 1
SubjectDN: CN=jabber.ccc.de, O=Chaos Computer Club e.V., L=Hamburg, ST=Hamburg, C=DE
IssuerDN: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority, OU=http://www.cacert.org, O=Root CA
Fingerprint: 4E09F9D9F224174684768D467A84B139B86A021F
SPKI Pin: 686B3569ABE87202E9018532719CB67DD7EA3356

### Certificate 2
SubjectDN: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority, OU=http://www.cacert.org, O=Root CA
IssuerDN: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority, OU=http://www.cacert.org, O=Root CA
Fingerprint: 135CEC36F49CB8E93B1AB270CD80884676CE8F33
SPKI Pin: 10DA624DEF41A3046DCDBA3D018F19DF3DC9A07C


**/
        
        PINLIST.add("686B3569ABE87202E9018532719CB67DD7EA3356");
        PINLIST.add("10DA624DEF41A3046DCDBA3D018F19DF3DC9A07C");
        
        //added pin from cacert.org downloadable class3 crt
        PINLIST.add("f061d83f958f4d78b147b31339978ea9c251ba9b");
        
        /**

               # Connecting to talk.l.google.com [4 of 9 hosts]
               ## Found 3 pinnable certs in chain
               ### Certificate 1
               SubjectDN: CN=talk.google.com, O=Google Inc, L=Mountain View, ST=California, C=US
               IssuerDN: CN=Google Internet Authority G2, O=Google Inc, C=US
               Fingerprint: 3FC4E2F60E93206A4D6FBB3EECC1626CE2721D73
               SPKI Pin: 63787427C56FBB2E800E55AD540C64F12C2648FF

               ### Certificate 2
               SubjectDN: CN=Google Internet Authority G2, O=Google Inc, C=US
               IssuerDN: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
               Fingerprint: D83C1A7F4D0446BB2081B81A1670F8183451CA24
               SPKI Pin: 43DAD630EE53F8A980CA6EFD85F46AA37990E0EA

               ### Certificate 3
               SubjectDN: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
               IssuerDN: OU=Equifax Secure Certificate Authority, O=Equifax, C=US
               Fingerprint: 7359755C6DF9A0ABC3060BCE369564C8EC4542A3
               SPKI Pin: C07A98688D89FBAB05640C117DAA7D65B8CACC4E
    **/
        PINLIST.add("63787427C56FBB2E800E55AD540C64F12C2648FF");
        PINLIST.add("43DAD630EE53F8A980CA6EFD85F46AA37990E0EA");
        PINLIST.add("C07A98688D89FBAB05640C117DAA7D65B8CACC4E");

        
        /**
               # Connecting to gmail.com [5 of 9 hosts]
               ## Found 3 pinnable certs in chain
               ### Certificate 1
               SubjectDN: CN=gmail.com, O=Google Inc, L=Mountain View, ST=California, C=US
               IssuerDN: CN=Google Internet Authority G2, O=Google Inc, C=US
               Fingerprint: 28DD89D30AA6F0A2B9F877FC55FCAB8518DE13FF
               SPKI Pin: 6D1D4933C8A6723FB112D046EC6C7AD18191231C
    **/
        PINLIST.add("6D1D4933C8A6723FB112D046EC6C7AD18191231C");

        
        /**

               # Connecting to chat.facebook.com [6 of 9 hosts]
               ## Found 3 pinnable certs in chain
               ### Certificate 1
               SubjectDN: CN=chat.facebook.com, O="Facebook, Inc.", L=Palo Alto, ST=California, C=US
               IssuerDN: CN=VeriSign Class 3 Secure Server CA - G3, OU=Terms of use at https://www.verisign.com/rpa (c)10, OU=VeriSign Trust Network, O="VeriSign, Inc.", C=US
               Fingerprint: 6D27CF4E75B340EEE6ADA8AE2974BDC764221187
               SPKI Pin: 72AC3AEDE343CDE867E5E412B502518794F2FF1C

               ### Certificate 2
               SubjectDN: CN=VeriSign Class 3 Secure Server CA - G3, OU=Terms of use at https://www.verisign.com/rpa (c)10, OU=VeriSign Trust Network, O="VeriSign, Inc.", C=US
               IssuerDN: CN=VeriSign Class 3 Public Primary Certification Authority - G5, OU="(c) 2006 VeriSign, Inc. - For authorized use only", OU=VeriSign Trust Network, O="VeriSign, Inc.", C=US
               Fingerprint: 5DEB8F339E264C19F6686F5F8F32B54A4C46B476
               SPKI Pin: 83244223D6CBF0A26FC7DE27CEBCA4BDA32612AD

               ### Certificate 3
               SubjectDN: CN=VeriSign Class 3 Public Primary Certification Authority - G5, OU="(c) 2006 VeriSign, Inc. - For authorized use only", OU=VeriSign Trust Network, O="VeriSign, Inc.", C=US
               IssuerDN: OU=Class 3 Public Primary Certification Authority, O="VeriSign, Inc.", C=US
               Fingerprint: 32F30882622B87CF8856C63DB873DF0853B4DD27
               SPKI Pin: B181081A19A4C0941FFAE89528C124C99B34ACC7
    **/
        
        PINLIST.add("72AC3AEDE343CDE867E5E412B502518794F2FF1C");
        PINLIST.add("83244223D6CBF0A26FC7DE27CEBCA4BDA32612AD");
        PINLIST.add("B181081A19A4C0941FFAE89528C124C99B34ACC7");

        
        /**

              # Connecting to dukgo.com [4 of 5 hosts]
## Found 2 pinnable certs in chain
### Certificate 1
SubjectDN: CN=duck.co, OU=Multi-Domain SSL, O=DuckDuckGo, STREET=20 Paoli Pike, L=Paoli, ST=Pennsylvania, OID.2.5.4.17=19460, C=US
IssuerDN: CN=COMODO High-Assurance Secure Server CA, O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB
Fingerprint: 081F7C200A8048340601F0725EAAAA0E38285CE4
SPKI Pin: 3AE35A2F6960B3267769F8C66BDD517C64463E10

### Certificate 2
SubjectDN: CN=COMODO High-Assurance Secure Server CA, O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB
IssuerDN: CN=AddTrust External CA Root, OU=AddTrust External TTP Network, O=AddTrust AB, C=SE
Fingerprint: B9B4C7A488C0885EC1C83AA87E4EBD2B215F9FA4
SPKI Pin: 4DC08738FE301627BF02D49EE265BD4B7C15D54E


**/

        PINLIST.add("3AE35A2F6960B3267769F8C66BDD517C64463E10");
        PINLIST.add("4DC08738FE301627BF02D49EE265BD4B7C15D54E");
        
        /**

               # Connecting to guardianproject.info [8 of 9 hosts]
               ## Found 1 pinnable certs in chain
               ### Certificate 1
               SubjectDN: CN=hyper.to, O=Chaos Inc., L=San Francisco, ST=California, C=US
               IssuerDN: CN=hyper.to, O=Chaos Inc., L=San Francisco, ST=California, C=US
               Fingerprint: 1064712E64D1AE7F4FDC2DEFDE7F19B1CEEB82B8
               SPKI Pin: 2B1292D6CD084EC90B5DBD398AEA15B853337971


               # Connecting to chat.hyper.to [9 of 9 hosts]

               # Goodbye!
**/
        

        PINLIST.add("2B1292D6CD084EC90B5DBD398AEA15B853337971");
        }
    
        return PINLIST.toArray(new String[PINLIST.size()]);
        
    }
}

