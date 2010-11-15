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
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gitian.android.im;

import org.gitian.android.im.IChatSession;
import org.gitian.android.im.engine.Contact;
import org.gitian.android.im.engine.ImErrorInfo;
import org.gitian.android.im.engine.Message;

oneway interface IChatListener {
    /**
     * This method is called when a new message of the ChatSession has arrived.
     */
    void onIncomingMessage(IChatSession ses, in Message msg);

    /**
     * This method is called when an error is found to send a message in the ChatSession.
     */
    void onSendMessageError(IChatSession ses, in Message msg, in ImErrorInfo error);

    /**
     * This method is called when the chat is converted to a group chat.
     */
    void onConvertedToGroupChat(IChatSession ses);

    /**
     * This method is called when a new contact has joined into this ChatSession.
     */
    void onContactJoined(IChatSession ses, in Contact contact);

    /**
     * This method is called when a contact in this ChatSession has left.
     */
    void onContactLeft(IChatSession ses, in Contact contact);

    /**
     * This method is called when an error is found to invite a contact to join
     * this ChatSession.
     */
    void onInviteError(IChatSession ses, in ImErrorInfo error);
}
