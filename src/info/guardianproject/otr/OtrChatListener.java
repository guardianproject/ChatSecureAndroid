package info.guardianproject.otr;

import java.util.ArrayList;
import java.util.List;

import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.MessageListener;
import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;

public class OtrChatListener implements MessageListener {

    private static final int TLV_APP_DATA = 0x100;
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

        List<TLV> tlvs = new ArrayList<TLV>();

        try {
            body = mOtrChatManager.decryptMessage(to, from, body, tlvs);
        } catch (OtrException e) {
            OtrDebugLogger.log("error decrypting message", e);
            return false;
        }

        if (body != null) {
            msg.setBody(body);
            mMessageListener.onIncomingMessage(session, msg);
        }
        
        for (TLV tlv : tlvs) {
            if (tlv.getType() == TLV_APP_DATA) {
                mMessageListener.onIncomingData(session, tlv.getValue());
            }
        }

        if (mOtrChatManager.getSessionStatus(to, from) != otrStatus) {
            mMessageListener.onStatusChanged(session);
        }
        
        return true;
    }
    
    @Override
    public void onIncomingData(ChatSession session, byte[] value) {
        mMessageListener.onIncomingData(session, value);
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
