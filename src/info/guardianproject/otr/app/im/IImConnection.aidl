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

import info.guardianproject.otr.app.im.IConnectionListener;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IContactListManager;
import info.guardianproject.otr.app.im.IInvitationListener;
import info.guardianproject.otr.app.im.engine.Presence;

interface IImConnection {
    void registerConnectionListener(IConnectionListener listener);
    void unregisterConnectionListener(IConnectionListener listener);

    void setInvitationListener(IInvitationListener listener);

    IContactListManager getContactListManager();
    IChatSessionManager getChatSessionManager();

    /**
     * Login the IM server.
     *
     * @param accountId the id of the account in content provider.
     * @param userName the useName.
     * @param password the password.
     * @param autoLoadContacts if true, contacts will be loaded from the server
     *          automatically after the user successfully login; otherwise, the
     *          client must load contacts manually.
     */
    void login(long accountId, String userName, String password, boolean autoLoadContacts, boolean retry);
    void logout();
    void cancelLogin();

    Presence getUserPresence();
    int updateUserPresence(in Presence newPresence);

    /**
     * Gets an array of presence status which are supported by the IM provider.
     */
    int[] getSupportedPresenceStatus();

    int getState();

    /**
     * Gets the count of active ChatSessions of this connection.
     */
    int getChatSessionCount();

    long getProviderId();
    long getAccountId();

    void acceptInvitation(long id);
    void rejectInvitation(long id);
    void sendHeartbeat();
    
    void setProxy(String type, String host, int port);
}
