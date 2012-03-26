package info.guardianproject.otr;

public interface OtrConstants {

	/*
	 * If Alice wishes to communicate to Bob that she is willing to use OTR, she can attach a special whitespace tag to any plaintext message she sends him. This tag may occur anywhere in the message, and may be hidden from the user (as in the Query Messages, above).

The tag consists of the following 16 bytes, followed by one or more sets of 8 bytes indicating the version of OTR Alice is willing to use:

Always send "\x20\x09\x20\x20\x09\x09\x09\x09" "\x20\x09\x20\x09\x20\x09\x20\x20", followed by one or more of:
"\x20\x09\x20\x09\x20\x20\x09\x20" to indicate a willingness to use OTR version 1 with Bob (note: this string must come before all other whitespace version tags, if it is present, for backwards compatibility)
"\x20\x20\x09\x09\x20\x20\x09\x20" to indicate a willingness to use OTR version 2 with
	 */
	

	public static final String QueryMessage_Bizzare = "?OTRv?";
	public static final String QueryMessage_V1_CASE1 = "?OTR?";
	public static final String QueryMessage_V1_CASE2 = "?OTR?v?";
	public static final String QueryMessage_V2 = "?OTRv2?";
	public static final String QueryMessage_V12 = "?OTR?v2?";
	public static final String QueryMessage_V14x = "?OTRv24x?";
	public static final String QueryMessage_V124x = "?OTR?v24x?";
	public static final String QueryMessage_CommonRequest = "?OTR?v2? You are being requested to have an Off-the-Record private conversation &lt;http://otr.cypherpunks.ca/&gt;.  However, you do not have a plugin to support that. See http://otr.cypherpunks.ca/ for more information.";
	public static final String PlainText_V12 = "This is a plain text that has hidden support for V1 and V2! 	  				 	 	 	    		  	  	 	  	 ";
	public static final String PlainText_V1 = "This is a plain text that has hidden support for V1! 	  				 	 	 	    		  	 ";
	public static final String CommonRequest = " You are being requested to have an Off-the-Record private conversation.  However, you do not have a plugin to support that. See http://otr.cypherpunks.ca/ for more information.";
}
