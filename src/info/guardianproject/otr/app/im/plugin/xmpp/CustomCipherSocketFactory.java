package info.guardianproject.otr.app.im.plugin.xmpp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class CustomCipherSocketFactory extends SSLSocketFactory {
    
    private static SSLSocketFactory mSocketFactory = null;
 
    public CustomCipherSocketFactory(SSLSocketFactory socketFactory)
    {
        super();
        
        mSocketFactory = socketFactory;
        
    }
    
    public static synchronized SocketFactory getDefault ()
    {
        return mSocketFactory;
    }
 
    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        
        return mSocketFactory.createSocket(socket, host, port, autoClose);
    }
 
    @Override
    public Socket createSocket() throws IOException {
        return mSocketFactory.createSocket();
    }

    @Override
    public String[] getDefaultCipherSuites() {
       
        return XMPPCertPins.SSL_IDEAL_CIPHER_SUITES;
        
        //return mSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        
       return XMPPCertPins.SSL_IDEAL_CIPHER_SUITES;
      //  return mSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
       
        return mSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      
        return mSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
      
        return mSocketFactory.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
            int localPort) throws IOException {
        
        return mSocketFactory.createSocket(address, port, localAddress, localPort);
    }
 
}