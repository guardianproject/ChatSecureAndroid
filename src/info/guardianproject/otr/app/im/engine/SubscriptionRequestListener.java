/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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
 * Interface that allows the implementing class to listen to subscription
 * request from other users.
 */
public interface SubscriptionRequestListener {
    /**
     * Called when a subscription request from another user is received.
     *
     * @param from
     */
    void onSubScriptionRequest(Contact from);

    /**
     * Called when a subscription request is approved.
     *
     * @param contact
     */
    void onSubscriptionApproved(String contact);

    /**
     * Called when a subscription request is declined.
     *
     * @param contact
     */
    void onSubscriptionDeclined(String contact);

    /**
     * Called when an error occurs during approving a subscription request.
     *
     * @param contact
     * @param error
     */
    void onApproveSubScriptionError(String contact, ImErrorInfo error);

    /**
     * Called when an error occurs during declining a subscription request.
     * @param contact
     * @param error
     */
    void onDeclineSubScriptionError(String contact, ImErrorInfo error);
}
