package info.guardianproject.otr;

import net.java.otr4j.session.SessionID;
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
	private final static String OTR_V12_STRING = "?OTR?v2?"; // this means offering v1 or v2
	private final static String OTR_V2ONLY_STRING = "?OTRv2?"; // this means offering v2 only

	private final static String OTR_HEADER_STRING = "?OTR";
	
	public OtrChatListener (OtrChatManager otrChatManager, MessageListener listener)
	{
		this.mOtrChatManager = otrChatManager;
		this.mMessageListener = listener;
	}

	@Override
	public void onIncomingMessage(ChatSession session, Message msg) {
		String body = msg.getBody();
		String from = msg.getFrom().getFullName();
		String to = msg.getTo().getFullName();
		
 		android.os.Debug.waitForDebugger();
 		
		if (body.indexOf(OTR_V12_STRING) != -1
				|| body.indexOf(OTR_V2ONLY_STRING) != -1) {
			
			if (mOtrChatManager.isEncryptedSession(to, from)) {
				mOtrChatManager.refreshSession(to, from);
			} else {
				mOtrChatManager.startSession(to, from);
				
			}
			
			
			SessionID sessionId = mOtrChatManager.getSessionId(to,from);
			

		} else {


			if (body.indexOf(OTR_HEADER_STRING) != -1) {
				
			//	if (!mOtrChatManager.isEncryptedSession(to, from)) {
				//	mOtrChatManager.refreshSession(to, from);
			//	}
					
				body = mOtrChatManager.decryptMessage(to, from, body);
			}
			Message rec = new Message(body);
			rec.setID(msg.getID());
			rec.setFrom(msg.getFrom());
			rec.setTo(msg.getTo());
			rec.setDateTime(msg.getDateTime());
			mMessageListener.onIncomingMessage(session, rec);
		}
	}

	@Override
	public void onSendMessageError(ChatSession session, Message msg,
			ImErrorInfo error) {
		mMessageListener.onSendMessageError(session, msg, error);
		Log.i(TAG, "onSendMessageError: " + msg.toString());
	}
}
