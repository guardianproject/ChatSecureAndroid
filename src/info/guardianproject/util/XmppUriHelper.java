package info.guardianproject.util;

import android.net.Uri;

public class XmppUriHelper {

    public static final String SCHEME = "xmpp";
    public static final String SCHEME_OPERATOR = "://";
    public static final String OTR_QUERY_PARAM = "otr-fingerprint";
    
    public static String getUri (String address, String otrFingerprint)
    {
        /**
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME);
        builder.appendPath(address);
        
        if (otrFingerprint != null)
            builder.appendQueryParameter(OTR_QUERY_PARAM, otrFingerprint);
            
        
        return builder.toString();
        **/
        
        StringBuilder builder = new StringBuilder();
        
        builder.append(SCHEME);
        builder.append(SCHEME_OPERATOR);
        
        builder.append(address);
        
        builder.append('?').append(OTR_QUERY_PARAM).append('=');
        builder.append(otrFingerprint);
        
        return builder.toString();
    }
}
