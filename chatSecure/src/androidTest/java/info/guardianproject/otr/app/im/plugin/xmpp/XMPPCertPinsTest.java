
package info.guardianproject.otr.app.im.plugin.xmpp;

import android.content.Context;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import info.guardianproject.cacheword.PRNGFixes;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.AccountActivity;
import info.guardianproject.util.Debug;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.thoughtcrime.ssl.pinning.PinningTrustManager;
import org.thoughtcrime.ssl.pinning.SystemKeyStore;

public class XMPPCertPinsTest extends AndroidTestCase {
    private static final String TAG = "XMPPCertPinsTest";

    SystemKeyStore systemKeyStore;
    PinningTrustManager pinningTrustManager;
    SSLContext sslContext;
    SecureRandom secureRandom;
    String domainsWithPins[];
    String domainsWithoutPins[] = {
            // signed by cacert.org, can't be pinned with AndroidPinning
            "jabber.ccc.de",
            // "vodka-pomme.net",
            // "jabber.cn"
    };

    @Override
    public void setUp() {
        Context c = getContext();
        PRNGFixes.apply();
        systemKeyStore = SystemKeyStore.getInstance(c);
        pinningTrustManager = new PinningTrustManager(systemKeyStore,
                XMPPCertPins.getPinList(), 0);
        secureRandom = new java.security.SecureRandom();
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            assert true;
        }
        ArrayList<String> domains = new ArrayList<String>(
                Arrays.asList(c.getResources().getStringArray(R.array.account_domains)));
        domains.add(AccountActivity.DEFAULT_SERVER_FACEBOOK);
        domains.add(AccountActivity.DEFAULT_SERVER_JABBERORG);
        // currently fails here, needs SRV tricks
        // domains.add(AccountActivity.DEFAULT_SERVER_GOOGLE);
        domainsWithPins = domains.toArray(new String[domains.size()]);
    }

    private ConnectionConfiguration getConfig(String domain) throws KeyManagementException {
        ConnectionConfiguration config = new ConnectionConfiguration(domain, 5222);
        config.setDebuggerEnabled(Debug.DEBUG_ENABLED);
        config.setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
        config.setSecurityMode(SecurityMode.required);
        config.setVerifyChainEnabled(true);
        config.setVerifyRootCAEnabled(true);
        config.setExpiredCertificatesCheckEnabled(true);
        config.setNotMatchingDomainCheckEnabled(true);
        config.setSelfSignedCertificateEnabled(false);

        return config;
    }

    public void testDomainsWithPins() {
        XMPPConnection connection = null;
        try {
            for (String domain : domainsWithPins) {
                Log.i(TAG, "TESTING DOMAINS WITH PINS: " + domain);
                ConnectionConfiguration config = getConfig(domain);
                sslContext.init(null, new javax.net.ssl.TrustManager[] {
                        pinningTrustManager
                }, secureRandom);
                config.setCustomSSLContext(sslContext);
                connection = new XMPPConnection(config);
                connection.addConnectionListener(new ConnectionListener() {

                    @Override
                    public void reconnectionSuccessful() {
                        Log.i(TAG, "reconnectionSuccessful");
                        assertTrue(false);
                    }

                    @Override
                    public void reconnectionFailed(Exception e) {
                        Log.i(TAG, "reconnectionSuccessful");
                        e.printStackTrace();
                        assertTrue(false);
                    }

                    @Override
                    public void reconnectingIn(int arg0) {
                        Log.i(TAG, "reconnectingIn " + arg0);
                        assertTrue(false);
                    }

                    @Override
                    public void connectionClosedOnError(Exception e) {
                        Log.i(TAG, "connectionClosedOnError");
                        e.printStackTrace();
                        assertTrue(false);
                    }

                    @Override
                    public void connectionClosed() {
                        Log.i(TAG, "connectionClosed");
                    }
                });
                connection.connect();
                assertTrue(connection.isConnected());
            }
        } catch (KeyManagementException e) {
            Log.e(TAG, "KeyManagementException");
            e.printStackTrace();
            assertTrue(false);
        } catch (XMPPException e) {
            Log.e(TAG, "XMPPException");
            e.printStackTrace();
            assertTrue(false);
        }
        if (connection != null)
            connection.disconnect();
    }

    public void testSettingCipherSuites() {
        try {
            sslContext.init(null, new javax.net.ssl.TrustManager[] {
                    pinningTrustManager
            }, secureRandom);

            sslContext.getDefaultSSLParameters().getCipherSuites();

            if (Build.VERSION.SDK_INT >= 20) {
                sslContext.getDefaultSSLParameters().setCipherSuites(
                        XMPPCertPins.SSL_IDEAL_CIPHER_SUITES_API_20);
            }
            else
            {
                sslContext.getDefaultSSLParameters().setCipherSuites(
                        XMPPCertPins.SSL_IDEAL_CIPHER_SUITES);
            }
        } catch (KeyManagementException e) {
            e.printStackTrace();
            assert true;
        }
    }
}
