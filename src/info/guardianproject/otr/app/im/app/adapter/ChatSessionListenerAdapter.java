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

package info.guardianproject.otr.app.im.app.adapter;

import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;

import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionListener;

import android.util.Log;

public class ChatSessionListenerAdapter extends IChatSessionListener.Stub {

    private static final String TAG = ImApp.LOG_TAG;

    public void onChatSessionCreated(IChatSession session) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "notifyChatSessionCreated(" + session + ")");
        }
    }

    public void onChatSessionCreateError(String name, ImErrorInfo error) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "notifyChatSessionCreateError(" + name + ", " + error + ")");
        }
    }

}
