package info.guardianproject.otr.app.im.plugin.xmpp.auth;


import java.io.IOException;
import java.net.URLEncoder;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLMechanism;

import android.util.Base64;
import android.util.Log;


public class GTalkOAuth2 extends SASLMechanism {
public static final String NAME="X-GOOGLE-TOKEN";

/*
 * Taken from here: http://stackoverflow.com/questions/7358392/how-to-authenticate-to-google-talk-with-accountmanagers-authentication-token-us
 */
public GTalkOAuth2(SASLAuthentication saslAuthentication) {
    super(saslAuthentication);
}

@Override
protected String getName() {
    return NAME;
}

static void enable() { }

@Override
protected void authenticate() throws IOException, XMPPException
{
    String authCode = password;
    String jidAndToken = "\0" + URLEncoder.encode( authenticationId, "utf-8" ) + "\0" + authCode;

    StringBuilder stanza = new StringBuilder();
    stanza.append( "<auth mechanism=\"" ).append( getName() );
    stanza.append( "\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">" );
    stanza.append( new String(Base64.encode( jidAndToken.getBytes( "UTF-8" ), Base64.DEFAULT ) ) );

    stanza.append( "</auth>" );

  //  Log.v("BlueTalk", "Authentication text is "+stanza);
    // Send the authentication to the server
    getSASLAuthentication().send( new Auth2Mechanism(stanza.toString()) );
}

public class Auth2Mechanism extends Packet {
    String stanza;
    public Auth2Mechanism(String txt) {
        stanza = txt;
    }
    public String toXML() {
        return stanza;
    }
}

/**
 * Initiating SASL authentication by select a mechanism.
 */
public class AuthMechanism extends Packet {
    final private String name;
    final private String authenticationText;

    public AuthMechanism(String name, String authenticationText) {
        if (name == null) {
            throw new NullPointerException("SASL mechanism name shouldn't be null.");
        }
        this.name = name;
        this.authenticationText = authenticationText;
    }

    public String toXML() {
        StringBuilder stanza = new StringBuilder();
        stanza.append("<auth mechanism=\"").append(name);
        stanza.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        if (authenticationText != null &&
                authenticationText.trim().length() > 0) {
            stanza.append(authenticationText);
        }
        stanza.append("</auth>");
        return stanza.toString();
    }
    }
}

/*
 *              this.youTubeName = ((GlsAuthorizer)authorizer).getAccount(null).name;

 this.authorizer.fetchAuthToken(accountName, activity, new AuthorizationListener<String>() {
      @Override
      public void onCanceled() {
      }

      @Override
      public void onError(Exception e) {
          Log.e("YouTube","error on auth",e);
      }

      @Override
      public void onSuccess(String result) {
        YouTubeSubmit.this.clientLoginToken = result;
        Log.d("YouTube","got client token: " + result);
        upload(youTubeName,videoFile, videoContentType);
      }});
 *  this.authorizer = new GlsAuthorizer.GlsAuthorizerFactory().getAuthorizer(activity,
        GlsAuthorizer.YOUTUBE_AUTH_TOKEN_TYPE);
        
                this.clientLoginToken = authorizer.getFreshAuthToken(youTubeName, clientLoginToken);

 */
/*


SASLAuthentication.registerSASLMechanism( GTalkOAuth2.NAME, GTalkOAuth2.class );
SASLAuthentication.supportSASLMechanism( GTalkOAuth2.NAME, 0 );
config.setSASLAuthenticationEnabled(true);

String saslAuthString = getAuthToken(acct.name);
connection = new XMPPConnection(config);
try {
    connection.connect();
    connection.login(name, saslAuthString);
} catch (XMPPException e) {
    // Most likely an expired token
    // Invalidate the token and start over. There are example of this available
}
*/