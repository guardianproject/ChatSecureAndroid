/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package info.guardianproject.otr.app.im.app.adapter;

import info.guardianproject.otr.app.im.IChatListener;
import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.util.LogCleaner;
import android.os.RemoteException;
import android.util.Log;

public class ChatListenerAdapter extends IChatListener.Stub {

    private static final String TAG = ImApp.LOG_TAG;

    public void onContactJoined(IChatSession ses, Contact contact) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onContactJoined(" + ses + ", " + contact + ")");
        }
    }

    public void onContactLeft(IChatSession ses, Contact contact) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onContactLeft(" + ses + ", " + contact + ")");
        }
    }

    public boolean onIncomingMessage(IChatSession ses, Message msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onIncomingMessage(" + ses + ", " + msg + ")");
        }

        return true;
    }

    public void onIncomingData(IChatSession ses, byte[] data) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onIncomingMessage(" + ses + ", len=" + data.length + ")");
        }
    }

    public void onSendMessageError(IChatSession ses, Message msg, ImErrorInfo error) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onSendMessageError(" + ses + ", " + msg + ", " + error + ")");
        }
    }

    public void onInviteError(IChatSession ses, ImErrorInfo error) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onInviteError(" + ses + ", " + error + ")");
        }
    }

    public void onConvertedToGroupChat(IChatSession ses) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onConvertedToGroupChat(" + ses + ")");
        }
    }

    @Override
    public void onIncomingReceipt(IChatSession ses, String packetId) throws RemoteException {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onIncomingReceipt(" + ses + "," + packetId + ")");
        }
    }

    @Override
    public void onStatusChanged(IChatSession ses) throws RemoteException {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onStatusChanged(" + ses + ")");
        }
    }

    @Override
    public void onIncomingFileTransfer(String from, String file) throws RemoteException {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onIncomingFileTransfer(" + from + "," + file + ")");
        }
    }

    @Override
    public void onIncomingFileTransferProgress(String file, int percent) throws RemoteException {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onIncomingFileTransferProgress(" + file + "," + percent + ")");
        }
    }

    @Override
    public void onIncomingFileTransferError(String file, String message) throws RemoteException {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            LogCleaner.debug(TAG, "onIncomingFileTransferError(" + file + "," + message + ")");
        }

    }

}
