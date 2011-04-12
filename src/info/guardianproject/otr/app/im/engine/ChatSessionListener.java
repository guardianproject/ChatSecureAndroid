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

package info.guardianproject.otr.app.im.engine;

/**
 * Interface that allows the implementing classes to listen for ChatSession
 * creation. The typical implementation will register MessageListener with the
 * created session so that it will be notified when new message arrived to the
 * session. Listeners are registered with ChatSessionManager.
 */
public interface ChatSessionListener {

    /**
     * Called when a new ChatSession is created.
     *
     * @param session the created ChatSession.
     */
    public void onChatSessionCreated(ChatSession session);
}
