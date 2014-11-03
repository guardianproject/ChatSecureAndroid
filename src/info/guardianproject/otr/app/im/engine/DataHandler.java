package info.guardianproject.otr.app.im.engine;

import info.guardianproject.otr.app.im.IDataListener;

import java.io.IOException;
import java.util.Map;

import net.java.otr4j.session.SessionStatus;

public interface DataHandler {
    /**
     * @param from this is OUR address
     * @param session the chat session
     * @param value the serialized request
     */
    void onIncomingRequest(Address from, Address to, byte[] value);

    /**
     * @param from this is OUR address
     * @param session the chat session
     * @param value the serialized response
     */
    void onIncomingResponse(Address from, Address to, byte[] value);

    /**
     * Offer data to peer
     *
     * @param offerId offer ID
     * @param us our Address
     * @param localUri URI of data
     * @param headers extra headrs or null
     */
    void offerData(String offerId, Address us, String localUri, Map<String, String> headers) throws IOException;

    void setDataListener(IDataListener dataListener);

    void onOtrStatusChanged(SessionStatus status);
}
