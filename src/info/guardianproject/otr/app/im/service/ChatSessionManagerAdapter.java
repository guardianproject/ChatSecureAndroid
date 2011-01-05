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

package info.guardianproject.otr.app.im.service;

import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.ChatGroup;
import info.guardianproject.otr.app.im.engine.ChatGroupManager;
import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.ChatSessionListener;
import info.guardianproject.otr.app.im.engine.ChatSessionManager;
import info.guardianproject.otr.app.im.engine.Contact;
import info.guardianproject.otr.app.im.engine.GroupListener;
import info.guardianproject.otr.app.im.engine.ImConnection;
import info.guardianproject.otr.app.im.engine.ImErrorInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionListener;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

public class ChatSessionManagerAdapter extends info.guardianproject.otr.app.im.IChatSessionManager.Stub {
    static final String TAG = RemoteImService.TAG;

    ImConnectionAdapter mConnection;
    ChatSessionManager mSessionManager;
    ChatGroupManager mGroupManager;
    HashMap<String, ChatSessionAdapter> mActiveSessions;
    ChatSessionListenerAdapter mSessionListenerAdapter;
    final RemoteCallbackList<IChatSessionListener> mRemoteListeners
            = new RemoteCallbackList<IChatSessionListener>();

    public ChatSessionManagerAdapter(ImConnectionAdapter connection) {
        mConnection = connection;
        ImConnection connAdaptee = connection.getAdaptee();
        mSessionManager = connAdaptee.getChatSessionManager();
        mActiveSessions = new HashMap<String, ChatSessionAdapter>();
        mSessionListenerAdapter = new ChatSessionListenerAdapter();
        mSessionManager.addChatSessionListener(mSessionListenerAdapter);

        if((connAdaptee.getCapability() & ImConnection.CAPABILITY_GROUP_CHAT) != 0) {
            mGroupManager = connAdaptee.getChatGroupManager();
            mGroupManager.addGroupListener(new ChatGroupListenerAdpater());
        }
    }

    public IChatSession createChatSession(String contactAddress) {
        ContactListManagerAdapter listManager =
            (ContactListManagerAdapter) mConnection.getContactListManager();
        Contact contact = listManager.getContactByAddress(contactAddress);
        if(contact == null) {
            try {
                contact = listManager.createTemporaryContact(contactAddress);
            } catch (IllegalArgumentException e) {
                mSessionListenerAdapter.notifyChatSessionCreateFailed(contactAddress,
                        new ImErrorInfo(ImErrorInfo.ILLEGAL_CONTACT_ADDRESS,
                                "Invalid contact address:" + contactAddress));
                return null;
            }
        }
        ChatSession session = mSessionManager.createChatSession(contact);
        return getChatSessionAdapter(session);
    }

    public void closeChatSession(ChatSessionAdapter adapter) {
        synchronized (mActiveSessions) {
            ChatSession session = adapter.getAdaptee();
            mSessionManager.closeChatSession(session);
            mActiveSessions.remove(adapter.getAddress());
        }
    }

    public void closeAllChatSessions() {
        synchronized (mActiveSessions) {
            ArrayList<ChatSessionAdapter> sessions =
                new ArrayList<ChatSessionAdapter>(mActiveSessions.values());
            for (ChatSessionAdapter ses : sessions) {
                ses.leave();
            }
        }
    }

    public void updateChatSession(String oldAddress, ChatSessionAdapter adapter) {
        synchronized (mActiveSessions) {
            mActiveSessions.remove(oldAddress);
            mActiveSessions.put(adapter.getAddress(), adapter);
        }
    }

    public IChatSession getChatSession(String address) {
        synchronized (mActiveSessions) {
            return mActiveSessions.get(address);
        }
    }

    public List getActiveChatSessions() {
        synchronized (mActiveSessions) {
            return new ArrayList<ChatSessionAdapter>(mActiveSessions.values());
        }
    }

    public int getChatSessionCount() {
        synchronized (mActiveSessions) {
            return mActiveSessions.size();
        }
    }

    public void registerChatSessionListener(IChatSessionListener listener) {
        if (listener != null) {
            mRemoteListeners.register(listener);
        }
    }

    public void unregisterChatSessionListener(IChatSessionListener listener) {
        if (listener != null) {
            mRemoteListeners.unregister(listener);
        }
    }

    ChatSessionAdapter getChatSessionAdapter(ChatSession session) {
        synchronized (mActiveSessions) {
            Address participantAddress = session.getParticipant().getAddress();
            String key = participantAddress.getFullName();
            ChatSessionAdapter adapter = mActiveSessions.get(key);
            if (adapter == null) {
                adapter = new ChatSessionAdapter(session, mConnection);
                mActiveSessions.put(key, adapter);
            }
            return adapter;
        }
    }

    class ChatSessionListenerAdapter implements ChatSessionListener {

        public void onChatSessionCreated(ChatSession session) {
            final IChatSession sessionAdapter = getChatSessionAdapter(session);
            final int N = mRemoteListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                IChatSessionListener listener = mRemoteListeners.getBroadcastItem(i);
                try {
                    listener.onChatSessionCreated(sessionAdapter);
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing the
                    // dead listeners.
                }
            }
            mRemoteListeners.finishBroadcast();
        }

        public void notifyChatSessionCreateFailed(final String name, final ImErrorInfo error) {
            final int N = mRemoteListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                IChatSessionListener listener = mRemoteListeners.getBroadcastItem(i);
                try {
                    listener.onChatSessionCreateError(name, error);
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing the
                    // dead listeners.
                }
            }
            mRemoteListeners.finishBroadcast();
        }
    }

    class ChatGroupListenerAdpater implements GroupListener {
        public void onGroupCreated(ChatGroup group) {
        }

        public void onGroupDeleted(ChatGroup group) {
            closeSession(group);
        }

        public void onGroupError(int errorType, String name, ImErrorInfo error) {
            if(errorType == ERROR_CREATING_GROUP) {
                mSessionListenerAdapter.notifyChatSessionCreateFailed(name, error);
            }
        }

        public void onJoinedGroup(ChatGroup group) {
            mSessionManager.createChatSession(group);
        }

        public void onLeftGroup(ChatGroup group) {
            closeSession(group);
        }

        private void closeSession(ChatGroup group) {
            String address = group.getAddress().getFullName();
            IChatSession session = getChatSession(address);
            if(session != null) {
                closeChatSession((ChatSessionAdapter) session);
            }
        }
    }

	@Override
	public boolean encryptChat(String address) throws RemoteException {
		return mSessionManager.encryptChat(address);
	}

	@Override
	public boolean unencryptChat(String address) throws RemoteException {
		
        
		// TODO Auto-generated method stub
		return mSessionManager.unencryptChat(address);
	}
	
	@Override
	public boolean isEncryptedSession(String address) throws RemoteException {
		
		return mSessionManager.isEncryptedSession(address);
	}

	@Override
	public void verifyRemoteIdentity(String address)
			throws RemoteException {
		
		
		
	}
	

    /**
    * Get public key fingerprint
    */
    public String getRemoteKeyFingerprint(String address)
    {
    	return mSessionManager.getRemoteKeyFingerprint(address);
    }
   
   /**
    * Get public key fingerprint
    */
    public String getLocalKeyFingerprint(String address)
    {
    	return mSessionManager.getLocalKeyFingerprint(address);
    }
}
