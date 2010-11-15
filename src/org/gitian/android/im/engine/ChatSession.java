/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gitian.android.im.engine;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import android.util.Log;

/**
 * A ChatSession represents a conversation between two users. A ChatSession has
 * a unique participant which is either another user or a group.
 */
public class ChatSession {
    private ImEntity mParticipant;
    private ChatSessionManager mManager;
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
     * asynchronously.
     * TODO: more docs on async callbacks.
     *
     * @param text the text to send.
     */
    public void sendMessageAsync(String text) {
        Message message = new Message(text);
        sendMessageAsync(message);
    }

    /**
     * Sends a message to other participant(s) in this session asynchronously.
     * TODO: more docs on async callbacks.
     *
     * @param msg the message to send.
     */
    public void sendMessageAsync(Message msg) {
        msg.setTo(mParticipant.getAddress());

        mHistoryMessages.add(msg);
        mManager.sendMessageAsync(this, msg);
    }

    /**
     * Called by ChatSessionManager when received a message of the ChatSession.
     * All the listeners registered in this session will be notified.
     *
     * @param msg the received message.
     */
    public void onReceiveMessage(Message msg) {
        mHistoryMessages.add(msg);

        for (MessageListener listener : mListeners) {
            listener.onIncomingMessage(this, msg);
        }
    }

    /**
     * Called by ChatSessionManager when an error occurs to send a message.
     * @param message
     *
     * @param error the error information.
     */
    public void onSendMessageError(Message message, ImErrorInfo error) {
        for (MessageListener listener : mListeners) {
            listener.onSendMessageError(this, message, error);
        }
    }

    public void onSendMessageError(String msgId, ImErrorInfo error) {
        for(Message msg : mHistoryMessages){
            if(msgId.equals(msg.getID())){
                onSendMessageError(msg, error);
                return;
            }
        }
        Log.i("ChatSession", "Message has been removed when we get delivery error:"
                + error);
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
