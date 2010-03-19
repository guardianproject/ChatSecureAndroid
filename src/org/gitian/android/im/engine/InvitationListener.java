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
 * Interface that allows the implementing class to listen to invitation from
 * other users.
 */
public interface InvitationListener {
    /**
     * Calls when an invitation to join a certain group from another user
     * received. The user should accept or reject the invitation by
     * {@link ChatGroupManager#acceptInvitationAsync(Invitation) acceptInvitation} or
     * {@link ChatGroupManager#rejectInvitationAsync(Invitation) rejectInvitation}
     *
     * @param invitation the invitation received.
     */
    public void onGroupInvitation(Invitation invitation);
}
