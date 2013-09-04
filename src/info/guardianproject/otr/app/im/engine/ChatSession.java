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
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private OtrChatManager mOtrChatManager;

    private CopyOnWriteArrayList<MessageListener> mListeners;
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
        mListeners = new CopyOnWriteArrayList<MessageListener>();
        mHistoryMessages = new Vector<Message>();
    }

    public ImEntity getParticipant() {
        return mParticipant;
    }

    public void setParticipant(ImEntity participant) {
        mParticipant = participant;
    }

    public void setOtrChatManager(OtrChatManager otrChatManager) {
        mOtrChatManager = otrChatManager;
    }

    public OtrChatManager getOtrChatManager() {
        return mOtrChatManager;
    }

    /**
     * Adds a MessageListener so that it can be notified of any new message in
     * this session.
     * 
     * @param listener
     */
    public synchronized void addMessageListener(MessageListener listener) {
        if ((listener != null) && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * Removes a listener from this session.
     * 
     * @param listener
     */
    public synchronized void removeMessageListener(MessageListener listener) {
        mListeners.remove(listener);
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

        if (message.getTo() == null)
          message.setTo(mParticipant.getAddress());
        
     // TODO OTRCHAT setFrom here, therefore add the mConnection in ChatSession - ??? NF 8/2013 still needs to be done?       
        SessionStatus otrStatus = mOtrChatManager.getSessionStatus(message.getFrom().getAddress(),message.getTo().getAddress());

        if (otrStatus == SessionStatus.ENCRYPTED)
        {
            SessionID sId = mOtrChatManager.getSessionId(message.getFrom().getAddress(),message.getTo().getAddress());            
            boolean verified = mOtrChatManager.getKeyManager().isVerified(sId);
            
            
            if (verified)
            {
                message.setType(Imps.MessageType.OUTGOING_ENCRYPTED_VERIFIED);
            }
            else
            {
                message.setType(Imps.MessageType.OUTGOING_ENCRYPTED);
            }
         
            mHistoryMessages.add(message);

            mOtrChatManager.transformSending(message);

            mManager.sendMessageAsync(this, message);

        }
        else if (otrStatus == SessionStatus.FINISHED)
        {
            
            onSendMessageError(message, new ImErrorInfo(ImErrorInfo.INVALID_SESSION_CONTEXT,"Please turn off encryption"));
        }
        else
        {
            mHistoryMessages.add(message);

            mOtrChatManager.transformSending(message);

            mManager.sendMessageAsync(this, message);

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

        mOtrChatManager.transformSending(message, isResponse, data);

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

        SessionStatus otrStatus = mOtrChatManager.getSessionStatus(message.getTo().getAddress(), message.getFrom().getAddress());

        SessionID sId = mOtrChatManager.getSessionId(message.getTo().getAddress(),message.getFrom().getAddress());            

        if (otrStatus == SessionStatus.ENCRYPTED)
        {
            boolean verified = mOtrChatManager.getKeyManager().isVerified(sId);
            
            if (verified)
            {
                message.setType(Imps.MessageType.INCOMING_ENCRYPTED_VERIFIED);
            }
            else
            {
                message.setType(Imps.MessageType.INCOMING_ENCRYPTED);
            }
                
        }
        
        
        //  BUG it only seems to find the most recently added listener.
        boolean good = true;
        
        for (MessageListener listener : mListeners) {
            good = good && listener.onIncomingMessage(this, message);
        }
        
        return good;
    }

    public void onMessageReceipt(String id) {
        for (MessageListener listener : mListeners) {
            listener.onIncomingReceipt(this, id);
        }
    }

    public void onMessagePostponed(String id) {
        for (MessageListener listener : mListeners) {
            listener.onMessagePostponed(this, id);
        }
    }

    public void onReceiptsExpected() {
        for (MessageListener listener : mListeners) {
            listener.onReceiptsExpected(this);
        }
    }

    /**
     * Called by ChatSessionManager when an error occurs to send a message.
     * 
     * @param message
     * 
     * @param error the error information.
     */
    public void onSendMessageError(Message message, ImErrorInfo error) {
        for (MessageListener listener : mListeners) {
            listener.onSendMessageError(this, message, error);
        }
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
