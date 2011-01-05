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

import info.guardianproject.otr.app.im.IImConnection;

import android.os.Handler;
import android.util.Log;
import info.guardianproject.otr.app.im.IConnectionListener;

public class ConnectionListenerAdapter extends IConnectionListener.Stub {

    private static final String TAG = ImApp.LOG_TAG;
    private Handler mHandler;

    public ConnectionListenerAdapter(Handler handler) {
        mHandler = handler;
    }

    public void onConnectionStateChange(IImConnection connection, int state, ImErrorInfo error) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionStateChange(" + state + ", " + error + ")");
        }
    }

    public void onUpdateSelfPresenceError(IImConnection connection, ImErrorInfo error) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onUpdateSelfPresenceError(" + error + ")");
        }
    }

    public void onSelfPresenceUpdated(IImConnection connection) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onSelfPresenceUpdated()");
        }
    }

    final public void onStateChanged(final IImConnection conn,
            final int state, final ImErrorInfo error) {
        mHandler.post(new Runnable() {
            public void run() {
                onConnectionStateChange(conn, state, error);
            }
        });
    }

    final public void onUpdatePresenceError(final IImConnection conn,
            final ImErrorInfo error) {
        mHandler.post(new Runnable() {
            public void run() {
                onUpdateSelfPresenceError(conn, error);
            }
        });
    }

    final public void onUserPresenceUpdated(final IImConnection conn) {
        mHandler.post(new Runnable() {
            public void run() {
                onSelfPresenceUpdated(conn);
            }
        });
    }
}
