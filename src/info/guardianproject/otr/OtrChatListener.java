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

	public OtrChatListener (OtrChatManager otrChatManager, MessageListener listener)
	{
		this.mOtrChatManager = otrChatManager;
		this.mMessageListener = listener;
	}

	@Override
	public void onIncomingMessage(ChatSession session, Message msg) {
		
		OtrDebugLogger.log( "processing incoming message: " + msg.getID());
		
		String body = msg.getBody();
		String from = msg.getFrom().getFullName();
		String to = msg.getTo().getFullName();
		
 		//remove port number from to/from names
 		String localUserId = OtrChatManager.processUserId(to);
 		String remoteUserId = OtrChatManager.processUserId(from);
 		SessionStatus otrStatus = mOtrChatManager.getSessionStatus(localUserId, remoteUserId);

		 
			if (otrStatus == SessionStatus.ENCRYPTED)
	        {
				OtrDebugLogger.log( "session status: encrypted");

				body = mOtrChatManager.decryptMessage(localUserId, remoteUserId, body);

				if (body != null)
				{
					msg.setBody(body);				
					mMessageListener.onIncomingMessage(session, msg);
				}
			}
			else if (otrStatus == SessionStatus.PLAINTEXT || otrStatus == SessionStatus.FINISHED)
			{
				OtrDebugLogger.log( "session status: plaintext");

				//this is most likely a DH setup message, so we will process and swallow it
				body = mOtrChatManager.decryptMessage(localUserId, remoteUserId, body);
				
				if (body != null)
				{
					msg.setBody(body);
					mMessageListener.onIncomingMessage(session, msg);
				}
			}
			else if (otrStatus == SessionStatus.FINISHED)
			{
				OtrDebugLogger.log( "session status: finished");

				//this is most likely a DH setup message, so we will process and swallow it
				body = mOtrChatManager.decryptMessage(localUserId, remoteUserId, body);
				
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
			
		if (mOtrChatManager.getSessionStatus(localUserId, remoteUserId) != otrStatus) {
			mMessageListener.onStatusChanged(session);
		}
	}

	@Override
	public void onSendMessageError(ChatSession session, Message msg,
			ImErrorInfo error) {
		
		mMessageListener.onSendMessageError(session, msg, error);
		OtrDebugLogger.log( "onSendMessageError: " + msg.toString());
	}

	@Override
	public void onIncomingReceipt(ChatSession ses, String id) {
		mMessageListener.onIncomingReceipt(ses, id);
	}

	@Override
	public void onReceiptsExpected(ChatSession ses) {
		mMessageListener.onReceiptsExpected(ses);
	}

	@Override
	public void onStatusChanged(ChatSession session) {
		mMessageListener.onStatusChanged(session);
	}
}
