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
 * Interface for group change events.
 */
public interface GroupListener {
    public static final int ERROR_CREATING_GROUP = -1;
    public static final int ERROR_DELETING_GROUP = -2;
    public static final int ERROR_JOINING_IN_GROUP = -3;
    public static final int ERROR_LEAVING_GROUP = -4;

    /**
     * Called when a chat group was successfully created.
     *
     * @param group the group was created.
     */
    public void onGroupCreated(ChatGroup group);

    /**
     * Called when a chat group was successfully deleted.
     *
     * @param group the group was deleted.
     */
    public void onGroupDeleted(ChatGroup group);

    /**
     * Called on joining in a chat group successfully.
     *
     * @param group the group which was joined into.
     */
    public void onJoinedGroup(ChatGroup group);

    /**
     * Called on leaving a chat group. It may be triggered by the user leaving
     * a group or a server initiated group leaving, e.g. the user got kicked
     * out of the group, the group is deleted, etc.
     *
     * @param group the group has left.
     */
    public void onLeftGroup(ChatGroup group);

    /**
     * Called when an error occurs with a certain group operation.
     *
     * @param errorType the type of the error
     * @param error the error information.
     */
    public void onGroupError(int errorType, String groupName, ImErrorInfo error);
}
