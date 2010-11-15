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

/**
 * Interface that allows the implementing classes to listen to connection
 * relative events. Listeners are registered with ImConnection.
 */
public interface ConnectionListener {
    /**
     * Called when the connection's state has changed.
     *
     * @param state
     *        the new state of the connection.
     * @param error
     *        the error which caused the state change or <code>null</code>
     *        it's a normal state change.
     */
    public void onStateChanged(int state, ImErrorInfo error);

    public void onUserPresenceUpdated();

    public void onUpdatePresenceError(ImErrorInfo error);
}
