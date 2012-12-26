package info.guardianproject.otr;

import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.MessageListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionStatus;

public class OtrChatListener implements MessageListener {

    private OtrChatManager mOtrChatManager;
    private MessageListener mMessageListener;

    public OtrChatListener(OtrChatManager otrChatManager, MessageListener listener) {
        this.mOtrChatManager = otrChatManager;
        this.mMessageListener = listener;
    }

    @Override
    public boolean onIncomingMessage(ChatSession session, Message msg) {

        OtrDebugLogger.log("processing incoming message: " + msg.getID());

        String body = msg.getBody();
        String from = msg.getFrom().getFullName();
        String to = msg.getTo().getFullName();

        SessionStatus otrStatus = mOtrChatManager.getSessionStatus(to, from);

        OtrDebugLogger.log("session status: " + otrStatus.name());

        try {
            body = mOtrChatManager.decryptMessage(to, from, body);
        } catch (OtrException e) {
            OtrDebugLogger.log("error decrypting message", e);
            return false;
        }

        if (body != null) {
            msg.setBody(body);
            mMessageListener.onIncomingMessage(session, msg);
        }

        if (mOtrChatManager.getSessionStatus(to, from) != otrStatus) {
            mMessageListener.onStatusChanged(session);
        }
        
        return true;
    }

    @Override
    public void onSendMessageError(ChatSession session, Message msg, ImErrorInfo error) {

        mMessageListener.onSendMessageError(session, msg, error);
        OtrDebugLogger.log("onSendMessageError: " + msg.toString());
    }

    @Override
    public void onIncomingReceipt(ChatSession ses, String id) {
        mMessageListener.onIncomingReceipt(ses, id);
    }

    @Override
    public void onMessagePostponed(ChatSession ses, String id) {
        mMessageListener.onMessagePostponed(ses, id);
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
