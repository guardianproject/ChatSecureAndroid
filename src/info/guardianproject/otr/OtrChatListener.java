package info.guardianproject.otr;

import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.MessageListener;
import android.util.Log;

public class OtrChatListener implements MessageListener {

	private OtrChatManager mOtrChatManager;
	private MessageListener mMessageListener;

	private final static String TAG = "OtrChatListener";
	// we want to support OTR v2 only since v1 has security issues
	//private final static String OTR_V12_STRING = "?OTR?v2?"; // this means offering v1 or v2
	//private final static String OTR_V2ONLY_STRING = "?OTRv2?"; // this means offering v2 only
	private final static String OTR_HEADER = "?OTR";
	
	public OtrChatListener (OtrChatManager otrChatManager, MessageListener listener)
	{
		this.mOtrChatManager = otrChatManager;
		this.mMessageListener = listener;
	}

	@Override
	public void onIncomingMessage(ChatSession session, Message msg) {
		
	//	android.os.Debug.waitForDebugger();
		Log.d(TAG, "processing incoming message: " + msg.getID());
		
		String body = msg.getBody();
		String from = msg.getFrom().getFullName();
		String to = msg.getTo().getFullName();
		
 		//remove port number from to/from names
 		String localUserId = OtrChatManager.processUserId(to);
 		String remoteUserId = OtrChatManager.processUserId(from);
		 SessionStatus otrStatus = mOtrChatManager.getSessionStatus(localUserId, remoteUserId);

		 //	        	mOtrChatManager.refreshSession(localUserId, remoteUserId);

		 
			if (otrStatus == SessionStatus.ENCRYPTED)
	        {
				Log.d(TAG, "session status: encrypted");

				body = mOtrChatManager.decryptMessage(localUserId, remoteUserId, body);

				if (body != null)
				{
					msg.setBody(body);
				
					mMessageListener.onIncomingMessage(session, msg);
				}
			}
			else if (otrStatus == SessionStatus.PLAINTEXT || otrStatus == SessionStatus.FINISHED)
			{
				Log.d(TAG, "session status: plaintext");

				//this is most likely a DH setup message, so we will process and swallow it
				body = mOtrChatManager.decryptMessage(localUserId, remoteUserId, body);
				
				otrStatus = mOtrChatManager.getSessionStatus(localUserId, remoteUserId);
				
				//if (body != null && otrStatus != SessionStatus.ENCRYPTED && (!body.startsWith(OTR_HEADER)))
				if (body != null)
				{
					msg.setBody(body);
				
					mMessageListener.onIncomingMessage(session, msg);
				}
			}
			else if (otrStatus == SessionStatus.FINISHED)
			{
				Log.d(TAG, "session status: finished");

				//this is most likely a DH setup message, so we will process and swallow it
				body = mOtrChatManager.decryptMessage(localUserId, remoteUserId, body);
				
				otrStatus = mOtrChatManager.getSessionStatus(localUserId, remoteUserId);
				
				//if (body != null && otrStatus != SessionStatus.ENCRYPTED && (!body.startsWith(OTR_HEADER)))
				if (body != null)
				{
					msg.setBody(body);
				
					mMessageListener.onIncomingMessage(session, msg);
				}
			}
			else
			{
				mMessageListener.onIncomingMessage(session, msg);
			}
		//}
	}

	@Override
	public void onSendMessageError(ChatSession session, Message msg,
			ImErrorInfo error) {
		
		mMessageListener.onSendMessageError(session, msg, error);
		Log.i(TAG, "onSendMessageError: " + msg.toString());
	}
}
