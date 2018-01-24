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

import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionListener;

interface IChatSessionManager {
    void registerChatSessionListener(IChatSessionListener listener);
    void unregisterChatSessionListener(IChatSessionListener listener);

    /**
     * Create a ChatSession with the specified contact. If the contact does not exist in any
     * of the user's contact lists, it will be added to the temporary list.
     *
     * @param contactAddress the address of the contact.
     */
    IChatSession createChatSession(String contactAddress, boolean isNewSession);

    /**
     * Create a MultiUserChatSession with the specified room. 
     *
     * @param contactAddress the address of the contact.
     */
    IChatSession createMultiUserChatSession(String roomAddress, String nickname, boolean isNewSession);

	/**
	* Get the default MUC server so we can show it to the user
	*/
	String getDefaultMultiUserChatServer();

    /**
     * Get the ChatSession that is associated with the specified contact or group.
     *
     * @param the address of the contact or group.
     * @return the ChatSession with the contact or group or <code>null</code> if
     *       there isn't any active ChatSession with the contact or group.
     */
    IChatSession getChatSession(String address);

    /**
     * Get a list of all active ChatSessions.
     *
     * @return a list of IBinders of all active ChatSessions.
     */
    List getActiveChatSessions();
}
