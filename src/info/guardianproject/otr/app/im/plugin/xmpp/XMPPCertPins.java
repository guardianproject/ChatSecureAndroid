package info.guardianproject.otr.app.im.plugin.xmpp;

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

    
    public final static String[] PINLIST = {XMPPCertPins.TALKGOOGLE1,XMPPCertPins.TALKGOOGLE2, XMPPCertPins.DUKGO1,XMPPCertPins.DUKGO2, XMPPCertPins.CHATFACEBOOK1,XMPPCertPins.CHATFACEBOOK2, XMPPCertPins.JABBERCCCDE1,XMPPCertPins.JABBERCCCDE2,XMPPCertPins.JABBERCCCDE3, XMPPCertPins.BINARYPARADOX};

    
/*
## Certificate 0 ##
Subject: CN=xmpp.binaryparadox.net
Issuer: CN=xmpp.binaryparadox.net
SHA1 FP: 0B93EB84CCBB7AA2CB92CF61A0348F63CCED14C1
SPKI Pin: B3A7C02FC620C25F3C395AB043BF3C7729CE3C41

Connecting to jabber.ccc.de [2 of 4 hosts]
There were 3 certs in chain.
*/
       public final static String BINARYPARADOX = "B3A7C02FC620C25F3C395AB043BF3C7729CE3C41";

       /*
        * 
Connecting to jabber.ccc.de [2 of 4 hosts]
There were 3 certs in chain.

## Certificate 0 ##
Subject: CN=jabber.ccc.de, O=Chaos Computer Club e.V., L=Hamburg, ST=Hamburg,
C=DE
Issuer: CN=CAcert Class 3 Root, OU=http://www.CAcert.org, O=CAcert Inc.
SHA1 FP: 8155CF376967A47417A7BEAA9B712AC63D161D50
SPKI Pin: ADE7618FE3BB26C20FC089F3EF9963D548D21457

## Certificate 1 ##
Subject: CN=CAcert Class 3 Root, OU=http://www.CAcert.org, O=CAcert Inc.
Issuer: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority,
OU=http://www.cacert.org, O=Root CA
SHA1 FP: DB4C4269073FE9C2A37D890A5C1B18C4184E2A2D
SPKI Pin: F061D83F958F4D78B147B31339978EA9C251BA9B

## Certificate 2 ##
Subject: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority,
OU=http://www.cacert.org, O=Root CA
Issuer: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority,
OU=http://www.cacert.org, O=Root CA
SHA1 FP: 135CEC36F49CB8E93B1AB270CD80884676CE8F33
SPKI Pin: 10DA624DEF41A3046DCDBA3D018F19DF3DC9A07C
*/
       
       public final static String JABBERCCCDE1 = "ADE7618FE3BB26C20FC089F3EF9963D548D21457";
       public final static String JABBERCCCDE2 = "F061D83F958F4D78B147B31339978EA9C251BA9B";
       public final static String JABBERCCCDE3 = "10DA624DEF41A3046DCDBA3D018F19DF3DC9A07C";

       /*
## Certificate 1 ##
Subject: CN=CAcert Class 3 Root, OU=http://www.CAcert.org, O=CAcert Inc.
Issuer: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority,
OU=http://www.cacert.org, O=Root CA
SHA1 FP: DB4C4269073FE9C2A37D890A5C1B18C4184E2A2D
SPKI Pin: F061D83F958F4D78B147B31339978EA9C251BA9B
*/
       
       /*
## Certificate 2 ##
Subject: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority,
OU=http://www.cacert.org, O=Root CA
Issuer: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority,
OU=http://www.cacert.org, O=Root CA
SHA1 FP: 135CEC36F49CB8E93B1AB270CD80884676CE8F33
SPKI Pin: 10DA624DEF41A3046DCDBA3D018F19DF3DC9A07C
*/
       
       /*
Connecting to chat.facebook.com [3 of 4 hosts]
There were 2 certs in chain.

## Certificate 0 ##
Subject: CN=chat.facebook.com, O="Facebook, Inc.", L=Palo Alto, ST=California,
C=US
Issuer: CN=DigiCert High Assurance CA-3, OU=www.digicert.com, O=DigiCert Inc,
C=US
SHA1 FP: 22E50EEEAF2DAF8E440377196C4D95734DEE94D9
SPKI Pin: 1C5CC68C8ABE4AA0DBC7729BEA05A4EC756464B6

## Certificate 1 ##
Subject: CN=DigiCert High Assurance CA-3, OU=www.digicert.com, O=DigiCert Inc,
C=US
Issuer: CN=DigiCert High Assurance EV Root CA, OU=www.digicert.com, O=DigiCert
Inc, C=US
SHA1 FP: A2E32A1A2E9FAB6EAD6B05F64EA0641339E10011
SPKI Pin: 95F9D7434B1CE71DEF4211EE6BE3C0E0256FAD95
*/
       
       public final static String CHATFACEBOOK1 = "1C5CC68C8ABE4AA0DBC7729BEA05A4EC756464B6";
       public final static String CHATFACEBOOK2 = "95F9D7434B1CE71DEF4211EE6BE3C0E0256FAD95";
       

       /*
Connecting to dukgo.com [4 of 4 hosts]
There were 2 certs in chain.

## Certificate 0 ##
Subject: CN=*.dukgo.com, OU=EssentialSSL Wildcard, OU=Domain Control Validated
Issuer: CN=EssentialSSL CA, O=COMODO CA Limited, L=Salford, ST=Greater
Manchester, C=GB
SHA1 FP: 7727F3D42E00BDBFBEF697470F013B9E1C41A8CB
SPKI Pin: F44CF8786F4346082E18AB760CC49B6167B1B9D8

## Certificate 1 ##
Subject: CN=EssentialSSL CA, O=COMODO CA Limited, L=Salford, ST=Greater
Manchester, C=GB
Issuer: CN=COMODO Certification Authority, O=COMODO CA Limited, L=Salford,
ST=Greater Manchester, C=GB
SHA1 FP: 73820A20F8F47A457CD0B54CC4E4E31CEFA5C1E7
SPKI Pin: CA91EDBE3EEF0F1736BDA1BA53E48E79B8ED7389
*/
       public final static String DUKGO1 = "F44CF8786F4346082E18AB760CC49B6167B1B9D8";
       public final static String DUKGO2 = "CA91EDBE3EEF0F1736BDA1BA53E48E79B8ED7389";
 
       /* Gmail/ Gtalk
        * Calculating PIN for certificate: C=US, ST=California, L=Mountain View, O=Google Inc, CN=gmail.com
Pin Value: 4b09f2c32d093a31a175168346a459e2f0179d89

        */

       /*
        * 
## Certificate 1 ##
Subject: CN=gmail.com, O=Google Inc, L=Mountain View, ST=California, C=US
Issuer: CN=Google Internet Authority G2, O=Google Inc, C=US
SHA1 FP: 28DD89D30AA6F0A2B9F877FC55FCAB8518DE13FF
SPKI Pin: 6D1D4933C8A6723FB112D046EC6C7AD18191231C

## Certificate 2 ##
Subject: CN=Google Internet Authority G2, O=Google Inc, C=US
Issuer: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
SHA1 FP: D83C1A7F4D0446BB2081B81A1670F8183451CA24
SPKI Pin: 43DAD630EE53F8A980CA6EFD85F46AA37990E0EA

## Certificate 3 ##
Subject: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
Issuer: OU=Equifax Secure Certificate Authority, O=Equifax, C=US
SHA1 FP: 7359755C6DF9A0ABC3060BCE369564C8EC4542A3
SPKI Pin: C07A98688D89FBAB05640C117DAA7D65B8CACC4E


Connecting to talk.google.com [2 of 2 hosts]
There were 3 certs in chain.

## Certificate 1 ##
Subject: CN=gmail.com, O=Google Inc, L=Mountain View, ST=California, C=US
Issuer: CN=Google Internet Authority G2, O=Google Inc, C=US
SHA1 FP: 28DD89D30AA6F0A2B9F877FC55FCAB8518DE13FF
SPKI Pin: 6D1D4933C8A6723FB112D046EC6C7AD18191231C

## Certificate 2 ##
Subject: CN=Google Internet Authority G2, O=Google Inc, C=US
Issuer: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
SHA1 FP: D83C1A7F4D0446BB2081B81A1670F8183451CA24
SPKI Pin: 43DAD630EE53F8A980CA6EFD85F46AA37990E0EA

## Certificate 3 ##
Subject: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
Issuer: OU=Equifax Secure Certificate Authority, O=Equifax, C=US
SHA1 FP: 7359755C6DF9A0ABC3060BCE369564C8EC4542A3
SPKI Pin: C07A98688D89FBAB05640C117DAA7D65B8CACC4E
        */
       
       public final static String TALKGOOGLE1 = "C07A98688D89FBAB05640C117DAA7D65B8CACC4E";
       public final static String TALKGOOGLE2 = "43DAD630EE53F8A980CA6EFD85F46AA37990E0EA";

       
}
