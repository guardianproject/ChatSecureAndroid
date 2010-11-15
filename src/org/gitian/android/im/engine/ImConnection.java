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

package org.gitian.android.im.engine;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An <code>ImConnection</code> is an abstract representation of a connection
 * to the IM server.
 */
public abstract class ImConnection {
    /**
     * Connection state that indicates the connection is not connected yet.
     */
    public static final int DISCONNECTED = 0;

    /**
     * Connection state that indicates the user is logging into the server.
     */
    public static final int LOGGING_IN = 1;

    /**
     * Connection state that indicates the user has logged into the server.
     */
    public static final int LOGGED_IN = 2;

    /**
     * Connection state that indicates the user is logging out the server.
     */
    public static final int LOGGING_OUT = 3;

    /**
     * Connection state that indicate the connection is suspending.
     */
    public static final int SUSPENDING = 4;

    /**
     * Connection state that indicate the connection has been suspended.
     */
    public static final int SUSPENDED = 5;

    /**
     * The capability of supporting group chat.
     */
    public static final int CAPABILITY_GROUP_CHAT = 1;
    /**
     * The capability of supporting session re-establishment.
     */
    public static final int CAPABILITY_SESSION_REESTABLISHMENT = 2;

    /**
     * The current state of the connection.
     */
    protected int mState;

    protected CopyOnWriteArrayList<ConnectionListener> mConnectionListeners;
    protected Presence mUserPresence;

    protected ImConnection() {
        mConnectionListeners = new CopyOnWriteArrayList<ConnectionListener>();
        mState = DISCONNECTED;
    }

    public void addConnectionListener(ConnectionListener listener) {
        if (listener != null) {
            mConnectionListeners.add(listener);
        }
    }

    public void removeConnectionListener(ConnectionListener listener) {
        mConnectionListeners.remove(listener);
    }

    public abstract Contact getLoginUser();

    public String getLoginUserName() {
        Contact loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getName();
    }

    public abstract int[] getSupportedPresenceStatus();

    public Presence getUserPresence() {
        if (mState == SUSPENDING || mState == SUSPENDED) {
            return new Presence();
        }

        if (mState != LOGGED_IN) {
            // In most cases we have a valid mUserPresence instance also
            // in the LOGGING_OUT state. However there is one exception:
            // if logout() is called before login finishes, the state may
            // jump from LOGGING_IN directly to LOGGING_OUT, skipping the
            // LOGGED_IN state. In this case we won't have a valid Presence
            // in the LOGGING_OUT state.
            return null;
        }

        return new Presence(mUserPresence);
    }

    public void updateUserPresenceAsync(Presence newPresence) throws ImException {
        if (mState != LOGGED_IN) {
            throw new ImException(ImErrorInfo.NOT_LOGGED_IN, "NOT logged in");
        }

        doUpdateUserPresenceAsync(newPresence);
    }

    /**
     * Tells the engine that the network type has changed, e.g. switch from gprs
     * to wifi. The engine should drop all the network connections created before
     * because they are not available anymore.
     *
     * The engine might also need to redo authentication on the new network depending
     * on the underlying protocol.
     */
    public void networkTypeChanged(){
    }

    /**
     * Tells the current state of the connection.
     */
    public int getState() {
        return mState;
    }

    /**
     * Sets the state of the connection.
     *
     * @param state the new state of the connection.
     * @param error the error information which caused the state change or null.
     */
    protected void setState(int state, ImErrorInfo error) {
        if(state < DISCONNECTED || state > SUSPENDED){
            throw new IllegalArgumentException("Invalid state: " + state);
        }
        if(mState != state){
            mState = state;
            for(ConnectionListener listener : mConnectionListeners){
                listener.onStateChanged(state, error);
            }
        }
    }

    protected void notifyUserPresenceUpdated() {
        for (ConnectionListener listener : mConnectionListeners) {
            listener.onUserPresenceUpdated();
        }
    }

    protected void notifyUpdateUserPresenceError(ImErrorInfo error) {
        for (ConnectionListener listener : mConnectionListeners) {
            listener.onUpdatePresenceError(error);
        }
    }

    /**
     * Gets bit-or of capabilities supported by the underlying protocol. Valid
     * capability bits are: {@value #CAPABILITY_GROUP_CHAT},
     * {@value #CAPABILITY_SESSION_REESTABLISHMENT}
     *
     * @return bit-or of capabilities supported by the underlying protocol
     */
    public abstract int getCapability();

    /**
     * Log in to the IM server.
     *
     * @param loginInfo the login information.
     */
    public abstract void loginAsync(LoginInfo loginInfo, boolean retry);

    /**
     * Re-establish previous session using the session context persisted by the
     * client. Only sessions that were dropped unexpectedly(e.g. power loss, crash,
     * etc) can be re-established using the stored session context. If the
     * session was terminated normally by either user logging out or server
     * initiated disconnection, it can't be re-established again therefore the
     * stored context should be removed by the client.
     * <p>
     * The client can query if session re-establishment is supported through
     * {@link #getCapability()}.
     *
     * @param sessionContext
     *            the session context which was fetched from previous session by
     *            {@link #getSessionContext()} and persisted by the client.
     * @throws UnsupportedOperationException
     *             if session re-establishment is not supported by the
     *             underlying protocol.
     */
    public abstract void reestablishSessionAsync(HashMap<String, String> sessionContext);

    /**
     * Log out from the IM server.
     */
    public abstract void logoutAsync();

    /**
     * Suspend connection with the IM server.
     */
    public abstract void suspend();

    /**
     * Gets the cookie of the current session. The client could store the
     * context and use it to re-establish the session by
     * {@link #reestablishSessionAsync(HashMap)}}. The stored context MUST be
     * removed upon the connection logout/disconnect.
     *
     * @return the context of the current session or <code>null</code> if the
     *         user has not logged in yet.
     * @throws UnsupportedOperationException
     *             if session re-establishment is not supported by the
     *             underlying protocol.
     */
    public abstract HashMap<String, String> getSessionContext();

    /**
     * Gets the instance of ChatSessionManager for the connection.
     *
     * @return the instance of ChatSessionManager for the connection.
     */
    public abstract ChatSessionManager getChatSessionManager();

    /**
     * Gets the instance of ContactListManager for the connection.
     *
     * @return the instance of ContactListManager for the connection.
     */
    public abstract ContactListManager getContactListManager();

    /**
     * Gets the instance of ChatGroupManager for the connection.
     *
     * @return the instance of ChatGroupManager for the connection.
     * @throws UnsupportedOperationException
     *             if group chat is not supported by the underlying protocol.
     */
    public abstract ChatGroupManager getChatGroupManager();

    protected abstract void doUpdateUserPresenceAsync(Presence presence);

    public abstract void sendHeartbeat();
    
    public abstract void setProxy (String type, String host, int port);
}
