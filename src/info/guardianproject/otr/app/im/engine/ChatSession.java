/*
 * Copyright (C) 2007 Esmertec AG. Copyright (C) 2007 The Android Open Source
 * Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package info.guardianproject.otr.app.im.engine;

import info.guardianproject.otr.OtrChatManager;
import info.guardianproject.otr.app.im.plugin.xmpp.XmppAddress;
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import android.util.Log;

/**
 * A ChatSession represents a conversation between two users. A ChatSession has
 * a unique participant which is either another user or a group.
 */
public class ChatSession {

    private ImEntity mParticipant;
    private ChatSessionManager mManager;

   // private OtrChatManager mOtrChatManager;

    private MessageListener mListener = null;
    private Vector<Message> mHistoryMessages;
    

    /**
     * Creates a new ChatSession with a particular participant.
     *
     * @param participant the participant with who the user communicates.
     * @param connection the underlying network connection.
     */
    ChatSession(ImEntity participant, ChatSessionManager manager) {
        mParticipant = participant;
        mManager = manager;
        mHistoryMessages = new Vector<Message>();
    }

    public ImEntity getParticipant() {
        return mParticipant;
    }

    public void setParticipant(ImEntity participant) {
        mParticipant = participant;
    }

    /*
    public void setOtrChatManager(OtrChatManager otrChatManager) {
        mOtrChatManager = otrChatManager;
    }

    public OtrChatManager getOtrChatManager() {
        return mOtrChatManager;
    }*/

    /**
     * Adds a MessageListener so that it can be notified of any new message in
     * this session.
     *
     * @param listener
     */
    public void setMessageListener(MessageListener listener) {
        mListener = listener;
    }
    
    public MessageListener getMessageListener ()
    {
        return mListener;
    }

    /**
     * Sends a text message to other participant(s) in this session
     * asynchronously and adds the message to the history. TODO: more docs on
     * async callbacks.
     *
     * @param text the text to send.
     */
    // TODO these sendMessageAsync() should probably be renamed to sendMessageAsyncAndLog()/
    /*
    public void sendMessageAsync(String text) {
        Message message = new Message(text);
        sendMessageAsync(message);
    }*/

    /**
     * Sends a message to other participant(s) in this session asynchronously
     * and adds the message to the history. TODO: more docs on async callbacks.
     *
     * @param message the message to send.
     */
    public int sendMessageAsync(Message message) {

        OtrChatManager cm = OtrChatManager.getInstance();
        SessionID sId = cm.getSessionId(message.getFrom().getAddress(),mParticipant.getAddress().getAddress());
        SessionStatus otrStatus = cm.getSessionStatus(sId);

        message.setTo(new XmppAddress(sId.getRemoteUserId()));
        
        if (otrStatus == SessionStatus.ENCRYPTED)
        {
            boolean verified = cm.getKeyManager().isVerified(sId);

            if (verified)
            {
                message.setType(Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED);
            }
            else
            {
                message.setType(Imps.MessageType.OUTGOING_ENCRYPTED);
            }

            
        }
        else if (otrStatus == SessionStatus.FINISHED)
        {
            message.setType(Imps.MessageType.POSTPONED);
          //  onSendMessageError(message, new ImErrorInfo(ImErrorInfo.INVALID_SESSION_CONTEXT,"error - session finished"));
            return message.getType();
        }
        else
        {
            //not encrypted, send to all
            //message.setTo(new XmppAddress(XmppAddress.stripResource(sId.getRemoteUserId())));        
            message.setType(Imps.MessageType.OUTGOING);
        }

        mHistoryMessages.add(message);
        boolean canSend = cm.transformSending(message);
        
        if (canSend)
        {
            mManager.sendMessageAsync(this, message);
        }
        else
        {
            //can't be sent due to OTR state
            message.setType(Imps.MessageType.POSTPONED);
            
        }
        
        return message.getType();

        
        
    }

    /**
     * Sends message + data to other participant(s) in this session asynchronously.
     *
     * @param message the message to send.
     * @param data the data to send.
     */
    public void sendDataAsync(Message message, boolean isResponse, byte[] data) {
        if (message.getTo() == null)
            message.setTo(mParticipant.getAddress());

        OtrChatManager cm = OtrChatManager.getInstance();

        cm.transformSending(message, isResponse, data);

        mManager.sendMessageAsync(this, message);
    }

    /**
     * Called by ChatSessionManager when received a message of the ChatSession.
     * All the listeners registered in this session will be notified.
     *
     * @param message the received message.
     *
     * @return true if the message was processed correctly, or false
     *   otherwise (e.g. decryption error)
     */
    public boolean onReceiveMessage(Message message) {
        mHistoryMessages.add(message);

        OtrChatManager cm = OtrChatManager.getInstance();

        if (cm != null)
        {
            SessionStatus otrStatus = cm.getSessionStatus(message.getTo().getAddress(), message.getFrom().getAddress());

            SessionID sId = cm.getSessionId(message.getTo().getAddress(),message.getFrom().getAddress());

            if (otrStatus == SessionStatus.ENCRYPTED)
            {
                boolean verified = cm.getKeyManager().isVerified(sId);

                if (verified)
                {
                    message.setType(Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED);
                }
                else
                {
                    message.setType(Imps.MessageType.INCOMING_ENCRYPTED);
                }

            }

        }

        if (mListener != null)
            mListener.onIncomingMessage(this, message);

        return true;
    }

    public void onMessageReceipt(String id) {
        if (mListener != null)
            mListener.onIncomingReceipt(this, id);

    }

    public void onMessagePostponed(String id) {
        if (mListener != null)
            mListener.onMessagePostponed(this, id);
    }

    public void onReceiptsExpected() {
        if (mListener != null)
            mListener.onReceiptsExpected(this);
    }

    /**
     * Called by ChatSessionManager when an error occurs to send a message.
     *
     * @param message
     *
     * @param error the error information.
     */
    public void onSendMessageError(Message message, ImErrorInfo error) {
        if (mListener != null)
            mListener.onSendMessageError(this, message, error);

    }

    public void onSendMessageError(String messageId, ImErrorInfo error) {
        for (Message message : mHistoryMessages) {
            if (messageId.equals(message.getID())) {
                onSendMessageError(message, error);
                return;
            }
        }
        Log.i("ChatSession", "Message has been removed when we get delivery error:" + error);
    }

    /**
     * Returns a unmodifiable list of the history messages in this session.
     *
     * @return a unmodifiable list of the history messages in this session.
     */
    public List<Message> getHistoryMessages() {
        return Collections.unmodifiableList(mHistoryMessages);
    }


}
