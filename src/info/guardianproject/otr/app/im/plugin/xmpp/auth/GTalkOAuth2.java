package info.guardianproject.otr.app.im.plugin.xmpp.auth;


import java.io.IOException;
import java.net.URLEncoder;

import org.apache.commons.codec.binary.Base64;
import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLMechanism;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;


public class GTalkOAuth2 extends SASLMechanism {

public static final String NAME="X-GOOGLE-TOKEN";//"X-OAUTH2";//
private static final String TOKEN_TYPE = "mail";// "https://www.googleapis.com/auth/googletalk";//"mail";
public static final String TYPE_GOOGLE_ACCT = "com.google";

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

static void enable() {

      //  Log.d(NAME,"Google OAuth2 enabled");
}

@Override
public void authenticate(String arg0, String arg1, CallbackHandler arg2) throws IOException,
        XMPPException {
    super.authenticate(arg0, arg1, arg2);
}

@Override
public void authenticate(String arg0, String arg1, String arg2) throws IOException, XMPPException {
    super.authenticate(arg0, arg1, arg2);
}

@Override
public void challengeReceived(String arg0) throws IOException {
    super.challengeReceived(arg0);
}

@Override
protected void authenticate() throws IOException, XMPPException
{
   //Log.d(NAME, "authId=" + authenticationId + "; password=" + password);

    String jidAndToken = "\0" + URLEncoder.encode( authenticationId, "utf-8" ) + "\0" + password;

    StringBuilder stanza = new StringBuilder();
    stanza.append( "<auth mechanism=\"" ).append( getName() );
    stanza.append( "\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">" );

    stanza.append( new String(Base64.encodeBase64( jidAndToken.getBytes( "UTF-8" ) ) ));
    stanza.append( "</auth>" );

    getSASLAuthentication().send( new Auth2Mechanism(stanza.toString()) );

}

/*
 * This is used the first time, on account setup, in order to display the proper perms dialog
 */
public static String getGoogleAuthTokenAllow(String name, Context context, Activity activity, Handler handler)
{
    AccountManager aMgr = AccountManager.get(context);

    String retVal = null;
    Account account = getAccount(TYPE_GOOGLE_ACCT,name, aMgr);
    Bundle bundle = new Bundle();

    AccountManagerFuture<Bundle> accFut = aMgr.getAuthToken(account, TOKEN_TYPE, bundle, activity,
            new AccountManagerCallback<Bundle>() {

                @Override
                public void run(AccountManagerFuture<Bundle> bundle) {

                    //this is the result bundle?


                }} ,handler);

    try
    {
        Bundle authTokenBundle = accFut.getResult();
        retVal = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
    }
    catch (Exception e)
    {
        Log.e(NAME,"error getting auth token result",e);
    }
    return retVal;
}

/*
 * need to refresh the google auth token everytime you login (no user prompt)
 */
public static String getGoogleAuthToken(String accountName, Context context) {
 //   Log.d(NAME,"Getting authToken for " + accountName);
    String authTokenType = TOKEN_TYPE;
    AccountManager aMgr = AccountManager.get(context);
    Account account = getAccount(TYPE_GOOGLE_ACCT,accountName,aMgr);
    if (accountName == null)
        accountName = account.name;

    if (account != null) {
      try {

          //aMgr.updateCredentials(account, authTokenType, options, activity, callback, handler);

        return aMgr.blockingGetAuthToken(account, authTokenType, true);
      } catch (OperationCanceledException e) {
        Log.e(NAME, "auth canceled", e);
      } catch (IOException e) {
        Log.e(NAME, "auth io problem", e);
      } catch (AuthenticatorException e) {
        Log.e(NAME, "auth authenticator exc", e);
      }
    }
    return null;
  }



//help method for getting proper account
public static Account getAccount(String type, String name, AccountManager aMgr) {
    Account[] accounts = aMgr.getAccountsByType(type);

    if (name == null)
        return accounts[0];

    for (Account account : accounts) {
      if (account.name.equals(name)) {
        return account;
      }
    }
    return null;
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