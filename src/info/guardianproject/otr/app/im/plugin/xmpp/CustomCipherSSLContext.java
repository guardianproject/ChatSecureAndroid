package info.guardianproject.otr.app.im.plugin.xmpp;

import java.security.Provider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;

public class CustomCipherSSLContext extends SSLContext
{

    protected CustomCipherSSLContext(SSLContextSpi contextSpi, Provider provider, String protocol) {
        super(contextSpi, provider, protocol);
        
    }

}
