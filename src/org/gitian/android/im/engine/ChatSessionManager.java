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

import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The ChatSessionManager keeps track of all current chat sessions and is also
 * responsible to dispatch the new incoming message to the right session.
 */
public abstract class ChatSessionManager {

    private CopyOnWriteArrayList<ChatSessionListener> mListeners;

    /** Map session to the participant communicate with. */
    protected Vector<ChatSession> mSessions;

    protected ChatSessionManager() {
        mListeners = new CopyOnWriteArrayList<ChatSessionListener>();
        mSessions = new Vector<ChatSession>();
    }

    /**
     * Registers a ChatSessionListener with the ChatSessionManager to receive
     * events related to ChatSession.
     *
     * @param listener the listener
     */
    public synchronized void addChatSessionListener(ChatSessionListener listener) {
        if ((listener != null) && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * Removes a ChatSessionListener so that it will no longer be notified.
     *
     * @param listener the listener to remove.
     */
    public synchronized void removeChatSessionListener(ChatSessionListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Creates a new ChatSession with specified participant.
     *
     * @param participant the participant.
     * @return the created ChatSession.
     */
    public synchronized ChatSession createChatSession(ImEntity participant) {
        for(ChatSession session : mSessions) {
            if(session.getParticipant().equals(participant)) {
                return session;
            }
        }

        ChatSession session = new ChatSession(participant, this);
        for (ChatSessionListener listener : mListeners) {
            listener.onChatSessionCreated(session);
        }

        mSessions.add(session);

        return session;
    }

    /**
     * Closes a ChatSession. This only removes the session from the list; the
     * protocol implementation should override this if it has special work to
     * do.
     *
     * @param session the ChatSession to close.
     */
    public void closeChatSession(ChatSession session) {
        mSessions.remove(session);
    }

    /**
     * Sends a message to specified participant(s) asynchronously.
     * TODO: more docs on async callbacks.
     *
     * @param message the message to send.
     */
    protected abstract void sendMessageAsync(ChatSession session, Message message);
    
    /**
     * Start encryption for this chat
     */
    public abstract boolean encryptChat(String address);
    
     /**
     * Stop encryption for this chat
     */
    public abstract boolean unencryptChat(String address);
    
    /**
    * Check if session is encrypted
     */
    public abstract boolean isEncryptedSession(String address);
    
      /**
     * Start remote identity verification
     */
    public abstract void verifyRemoteIdentity(String address);
    

    /**
    * Get public key fingerprint
    */
    public abstract String getRemoteKeyFingerprint(String address);
   
   /**
    * Get public key fingerprint
    */
    public abstract String getLocalKeyFingerprint(String address);
}
