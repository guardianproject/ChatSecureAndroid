package info.guardianproject.otr;

import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.MessageListener;

import java.util.ArrayList;
import java.util.List;

import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionStatus;
import net.java.otr4j.session.TLV;

public class OtrChatListener implements MessageListener {

    public static final int TLV_DATA_REQUEST = 0x100;
    public static final int TLV_DATA_RESPONSE = 0x101;
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
        String from = msg.getFrom().getAddress();
        String to = msg.getTo().getAddress();

        SessionStatus otrStatus = mOtrChatManager.getSessionStatus(to, from);

        OtrDebugLogger.log("session status: " + otrStatus.name());

        List<TLV> tlvs = new ArrayList<TLV>();

        try {
            body = mOtrChatManager.decryptMessage(to, from, body, tlvs);

            if (body != null) {
                msg.setBody(body);                 
                mMessageListener.onIncomingMessage(session, msg);
            }
        
        } catch (OtrException e) {
            
            OtrDebugLogger.log("error decrypting message");                
            msg.setBody("error decryption message body");
            mMessageListener.onIncomingMessage(session, msg);
        }
        
        for (TLV tlv : tlvs) {
            if (tlv.getType() == TLV_DATA_REQUEST) {
                mMessageListener.onIncomingDataRequest(session, msg, tlv.getValue());
            } else if (tlv.getType() == TLV_DATA_RESPONSE) {
                mMessageListener.onIncomingDataResponse(session, msg, tlv.getValue());
            }
        }

        if (mOtrChatManager.getSessionStatus(to, from) != otrStatus) {
            mMessageListener.onStatusChanged(session);
        }
        
        return true;
    }
    
    @Override
    public void onIncomingDataRequest(ChatSession session, Message msg, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onIncomingDataResponse(ChatSession session, Message msg, byte[] value) {
        throw new UnsupportedOperationException();
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
