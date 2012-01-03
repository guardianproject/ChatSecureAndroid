package info.guardianproject.otr.app.im.plugin.xmpp;

import java.io.IOException;
import java.util.HashMap;

import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.Base64;

import de.measite.smack.Sasl;
 
public class MySASLDigestMD5Mechanism extends SASLMechanism
{
 
    public MySASLDigestMD5Mechanism(SASLAuthentication saslAuthentication)
    {
        super(saslAuthentication);
    }
 
    protected void authenticate()
        throws IOException, XMPPException
    {
        String mechanisms[] = {
            getName()
        };
        java.util.Map props = new HashMap();
        sc = Sasl.createSaslClient(mechanisms, null, "xmpp", hostname, props, this);
        super.authenticate();
    }
 
    public void authenticate(String username, String host, String password)
        throws IOException, XMPPException
    {
        authenticationId = username;
        this.password = password;
        hostname = host;
        String mechanisms[] = {
            getName()
        };
        java.util.Map props = new HashMap();
        sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, this);
        super.authenticate();
    }
 
    public void authenticate(String username, String host, CallbackHandler cbh)
        throws IOException, XMPPException
    {
        String mechanisms[] = {
            getName()
        };
        
        java.util.Map props = new HashMap();
        sc = Sasl.createSaslClient(mechanisms, "", "xmpp", host, props, cbh);
        super.authenticate();
    }
 
    protected String getName()
    {
        return "DIGEST-MD5";
    }
 
    public void challengeReceived(String challenge)
        throws IOException
    {
        //StringBuilder stanza = new StringBuilder();
        byte response[];
        if(challenge != null)
            response = sc.evaluateChallenge(Base64.decode(challenge));
        else
            //response = sc.evaluateChallenge(null);
            response = sc.evaluateChallenge(new byte[0]);
        //String authenticationText = "";
        Packet responseStanza;
        //if(response != null)
        //{
            //authenticationText = Base64.encodeBytes(response, 8);
            //if(authenticationText.equals(""))
                //authenticationText = "=";
           
            if (response == null){
                responseStanza = new Response();
            } else {
                responseStanza = new Response(Base64.encodeBytes(response,Base64.DONT_BREAK_LINES));   
            }
        //}
        //stanza.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        //stanza.append(authenticationText);
        //stanza.append("</response>");
        //getSASLAuthentication().send(stanza.toString());
        getSASLAuthentication().send(responseStanza);
    }
}