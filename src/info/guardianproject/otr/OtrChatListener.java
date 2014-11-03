package info.guardianproject.otr;

import info.guardianproject.otr.OtrDataHandler.Transfer;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.app.im.engine.MessageListener;
import info.guardianproject.util.Debug;

import java.util.ArrayList;
import java.util.List;

import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionID;
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

  //      OtrDebugLogger.log("processing incoming message: " + msg.getID());

        String body = msg.getBody();
        String from = msg.getFrom().getAddress();
        String to = msg.getTo().getAddress();

        body = Debug.injectErrors(body);

        SessionID sessionID = mOtrChatManager.getSessionId(to, from);
        SessionStatus otrStatus = mOtrChatManager.getSessionStatus(sessionID);

        List<TLV> tlvs = new ArrayList<TLV>();

        try {
            // No OTR for groups (yet)
            if (!session.getParticipant().isGroup()) {
                body = mOtrChatManager.decryptMessage(to, from, body, tlvs);
            }

            if (body != null) {
                msg.setBody(body);
                mMessageListener.onIncomingMessage(session, msg);
            }

        } catch (OtrException e) {

         //   OtrDebugLogger.log("error decrypting message",e);

           // msg.setBody("[" + "You received an unreadable encrypted message" + "]");
           // mMessageListener.onIncomingMessage(session, msg);
        //   mOtrChatManager.injectMessage(sessionID, "[error please stop/start encryption]");

        }

        for (TLV tlv : tlvs) {
            if (tlv.getType() == TLV_DATA_REQUEST) {
                mMessageListener.onIncomingDataRequest(session, msg, tlv.getValue());
            } else if (tlv.getType() == TLV_DATA_RESPONSE) {
                mMessageListener.onIncomingDataResponse(session, msg, tlv.getValue());
            }
        }

        SessionStatus newStatus = mOtrChatManager.getSessionStatus(to, from);
        if (newStatus != otrStatus) {
            mMessageListener.onStatusChanged(session, newStatus);
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
    public void onStatusChanged(ChatSession session, SessionStatus status) {
        mMessageListener.onStatusChanged(session, status);
    }

    @Override
    public void onIncomingTransferRequest(Transfer transfer) {
        mMessageListener.onIncomingTransferRequest(transfer);
    }
}
