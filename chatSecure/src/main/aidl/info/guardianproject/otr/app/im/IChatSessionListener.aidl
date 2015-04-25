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

package info.guardianproject.otr.app.im;

import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;

oneway interface IChatSessionListener {
    /**
     * This method is called when a new ChatSession is created. A ChatSession
     * will be created either when the user called explicitly or an incoming
     * message which doesn't belong to any active sessions arrived.
     */
    void onChatSessionCreated(IChatSession session);

    /**
     * This method is called when it failed to create a new ChatSession.
     *
     * @param name the name of the ChatSession failed to create. It's either the
     *      name of the contact or the group.
     * @param error detail error,
     */
    void onChatSessionCreateError(String name, in ImErrorInfo error);
}
