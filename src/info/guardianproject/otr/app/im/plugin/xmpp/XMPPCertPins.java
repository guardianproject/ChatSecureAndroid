package info.guardianproject.otr.app.im.plugin.xmpp;

import java.util.ArrayList;
import java.util.HashSet;


public class XMPPCertPins
{

    // Use the following rules
    // https://wiki.mozilla.org/Security/Server_Side_TLS
    // AEADs over everything else
    // PFS over non-PFS
    // AES-128 over AES-256 ( https://www.schneier.com/blog/archives/2009/07/another_new_aes.html )
    // Avoid SHA-1
    // Remove RC4, MD5, DES
    public final static String[] SSL_IDEAL_CIPHER_SUITES_API_20 = {
     "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
     "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
     "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
     "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
     "TLS_DHE_RSA_WITH_AES128_GCM_SHA256",
     "TLS_DHE_RSA_WITH_AES256_GCM_SHA384",

     "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
     "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
     "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
     "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
     "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
     "TLS_DHE_RSA_WITH_AES_256_CBC_SHA384",

     "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
     "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
     "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
     "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
     "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
     "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
     "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
     "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",

     "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
     "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
     "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
     "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
     "TLS_RSA_WITH_AES_128_CBC_SHA256",
     "TLS_RSA_WITH_AES_256_CBC_SHA256",
     "TLS_RSA_WITH_AES_128_CBC_SHA",
     "TLS_RSA_WITH_AES_256_CBC_SHA"
    };

    // Follow above rules but as closely as possible but if we have to use RC4, use it last
    public final static String[] SSL_IDEAL_CIPHER_SUITES = {
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",

    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",

    "TLS_RSA_WITH_AES_128_CBC_SHA",
    "TLS_RSA_WITH_AES_256_CBC_SHA",

    // UNCOMMENT THIS BLOCK ONLY IF ABSOLUTELY NECESSARY
    /*
    "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
    "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
    "TLS_ECDH_RSA_WITH_RC4_128_SHA",
    "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
    */
    };

    public static ArrayList<String> PINLIST = null;

    /**
     * These are currently all pins of the CA's signing keys for the CAs used by
     * servers that we trust. AndroidPinning always validates using the normal
     * CA method, so there is no use to include cacert.org, similar CAs, or
     * self-signed certificates here. AndroidPinning will fail anyway when it
     * runs its built-in check against the system's trust manager.
     *
     * @return
     */
    public static String[] getPinList() {
        if (PINLIST == null) {
            PINLIST = new ArrayList<String>();
            // generated using http://gitlab.doeg.gy/cpu/jabberpinfetch

            /* chat.facebook.com
            SubjectDN: CN=DigiCert High Assurance CA-3, OU=www.digicert.com, O=DigiCert Inc, C=US
            IssuerDN: CN=DigiCert High Assurance EV Root CA, OU=www.digicert.com, O=DigiCert Inc, C=US
            Fingerprint: 42857855FB0EA43F54C9911E30E7791D8CE82705
            SPKI Pin: 95F9D7434B1CE71DEF4211EE6BE3C0E0256FAD95
             */
            PINLIST.add("95F9D7434B1CE71DEF4211EE6BE3C0E0256FAD95");

            /* gmail.com
            SubjectDN: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
            IssuerDN: OU=Equifax Secure Certificate Authority, O=Equifax, C=US
            Fingerprint: 7359755C6DF9A0ABC3060BCE369564C8EC4542A3
            SPKI Pin: C07A98688D89FBAB05640C117DAA7D65B8CACC4E
             */
            PINLIST.add("C07A98688D89FBAB05640C117DAA7D65B8CACC4E");

            /* duck.co/dukgo.com im.mayfirst.org jabberpl.org neko.im riseup.net
            SubjectDN: CN=COMODO RSA Certification Authority, O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB
            IssuerDN: CN=AddTrust External CA Root, OU=AddTrust External TTP Network, O=AddTrust AB, C=SE
            Fingerprint: F5AD0BCC1AD56CD150725B1C866C30AD92EF21B0
            SPKI Pin: 6E584E3375BD57F6D5421B1601C2D8C0F53A9F6E
             */
            PINLIST.add("6E584E3375BD57F6D5421B1601C2D8C0F53A9F6E");

            /* jabber.calyxinstitute.org
            SubjectDN: CN=RapidSSL CA, O="GeoTrust, Inc.", C=US
            IssuerDN: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
            Fingerprint: C039A3269EE4B8E82D00C53FA797B5A19E836F47
            SPKI Pin: A39399C404C3B209B081C21F21622778C2748E4C
             */
            PINLIST.add("A39399C404C3B209B081C21F21622778C2748E4C");

            /* xmpp.jp
            SubjectDN: CN=StartCom Certification Authority, OU=Secure Digital Certificate Signing, O=StartCom Ltd., C=IL
            IssuerDN: CN=StartCom Certification Authority, OU=Secure Digital Certificate Signing, O=StartCom Ltd., C=IL
            Fingerprint: 3E2BF7F2031B96F38CE6C4D8A85D3E2D58476A0F
            SPKI Pin: 234B71255613E130DDE34269C9CC30D46F0841E0
            */
            PINLIST.add("234B71255613E130DDE34269C9CC30D46F0841E0");

            /* The following pins are for self-signed certificates and the
             * cacert.org Certificate Authority certificate.  AndroidPinning
             * will always fail on these unless they have been manually
             * installed into the system's keystore.  AndroidPinning always
             * does a check using the system's default trust manager.
             */

            /*
            SubjectDN: CN=jabber.ccc.de, O=Chaos Computer Club e.V., L=Hamburg, ST=Hamburg, C=DE
            IssuerDN: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority, OU=http://www.cacert.org, O=Root CA
            Fingerprint: 4E09F9D9F224174684768D467A84B139B86A021F
            SPKI Pin: 686B3569ABE87202E9018532719CB67DD7EA3356
            */
            PINLIST.add("686B3569ABE87202E9018532719CB67DD7EA3356");

            /*
            SubjectDN: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority, OU=http://www.cacert.org, O=Root CA
            IssuerDN: EMAILADDRESS=support@cacert.org, CN=CA Cert Signing Authority, OU=http://www.cacert.org, O=Root CA
            Fingerprint: 135CEC36F49CB8E93B1AB270CD80884676CE8F33
            SPKI Pin: 10DA624DEF41A3046DCDBA3D018F19DF3DC9A07C
            */
            PINLIST.add("10DA624DEF41A3046DCDBA3D018F19DF3DC9A07C");
            //added pin from cacert.org downloadable class3 crt
            PINLIST.add("f061d83f958f4d78b147b31339978ea9c251ba9b");

            /* guardianproject.info/hyper.to self-signed
            SubjectDN: CN=hyper.to, O=Chaos Inc., L=San Francisco, ST=California, C=US
            IssuerDN: CN=hyper.to, O=Chaos Inc., L=San Francisco, ST=California, C=US
            Fingerprint: 1064712E64D1AE7F4FDC2DEFDE7F19B1CEEB82B8
            SPKI Pin: 2B1292D6CD084EC90B5DBD398AEA15B853337971
            */
            PINLIST.add("2B1292D6CD084EC90B5DBD398AEA15B853337971");

            // double check there are no duplicates by mistake
            if (PINLIST.size() != new HashSet<String>(PINLIST).size())
                throw new SecurityException("PINLIST has duplicate entries!");
        }

        return PINLIST.toArray(new String[PINLIST.size()]);

    }
}
