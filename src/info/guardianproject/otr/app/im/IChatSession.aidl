/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package info.guardianproject.otr.app.im;

import info.guardianproject.otr.app.im.IChatListener;
import info.guardianproject.otr.app.im.IDataListener;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.otr.IOtrChatSession;



interface IChatSession {
    /**
     * Registers a ChatListener with this ChatSession to listen to incoming
     * message and participant change events.
     */
    void registerChatListener(IChatListener listener);

    /**
     * Unregisters the ChatListener so that it won't be notified again.
     */
    void unregisterChatListener(IChatListener listener);

    /**
     * Tells if this ChatSession is a group session.
     */
    boolean isGroupChatSession();

    /**
     * Gets the name of ChatSession.
     */
    String getName();

    /**
     * Gets the id of the ChatSession in content provider.
     */
    long getId();

    /**
     * Gets the participants of this ChatSession.
     */
    String[] getParticipants();

    /**
     * Convert a single chat to a group chat. If the chat session is already a
     * group chat or it's converting to group chat.
     */
    void convertToGroupChat(String nickname);

    /**
     * Invites a contact to join this ChatSession. The user can only invite
     * contacts to join this ChatSession if it's a group session. Nothing will
     * happen if this is a simple one-to-one ChatSession.
     */
    void inviteContact(String contact);

    /**
     * Leaves this ChatSession.
     */
    void leave();

    /**
     * Leaves this ChatSession if there isn't any message sent or received in it.
     */
    void leaveIfInactive();

    /**
     * Sends a message to all participants in this ChatSession.
     */
    void sendMessage(String text);

    /**
     * Sends data to all participants in this ChatSession.
     */
    boolean offerData(String offerId, String localUri, String type);

    /**
     * Mark this chat session as read.
     */
    void markAsRead();   
    
    /**
    * Get OTR Session Manager
    */
    IOtrChatSession getOtrChatSession();
    
     /**
    * set class for handling incoming data transfers
    */
    void setDataListener (IDataListener dataListener);
    
    /**
    * respond to incoming data request
    */
    void setIncomingFileResponse (boolean acceptThis, boolean acceptAll);
    
    /**
    * reinit chatsession if we are starting a new chat
    */
    void reInit();
}

