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
 * An abstract interface to access system SMS service.
 */
public interface SmsService {
    /**
     * The listener which will be notified when an incoming SMS is received.
     *
     */
    public interface SmsListener {
        /**
         * Called on new SMS received.
         *
         * @param data
         */
        public void onIncomingSms(byte[] data);
    }

    /**
     * Callback on send SMS failure.
     *
     */
    public interface SmsSendFailureCallback {
        /** Generic failure case.*/
        int ERROR_GENERIC_FAILURE = 1;
        /** Failed because radio was explicitly turned off.*/
        int ERROR_RADIO_OFF = 2;

        /**
         * Called when send an SMS failed.
         *
         * @param errorCode the error code; will be one of
         *            {@link #ERROR_GENERIC_FAILURE},
         *            {@link #ERROR_RADIO_OFF}
         */
        public void onFailure(int errorCode);
    }

    /**
     * The max number of bytes an SMS can take.
     *
     * @return the max number of bytes an SMS can take.
     */
    public int getMaxSmsLength();

    /**
     * Sends a data SMS to the destination.
     *
     * @param dest
     *            The address to send the message to.
     * @param port
     *            The port to deliver the message to.
     * @param data
     *            The body of the message to send.
     */
    public void sendSms(String dest, int port, byte[] data);

    /**
     * Sends a data SMS to the destination.
     *
     * @param dest
     *            The address to send the message to.
     * @param port
     *            The port to deliver the message to.
     * @param data
     *            The body of the message to send.
     * @param callback
     *            If not null, it will be notified if the message could not be
     *            sent.
     */
    public void sendSms(String dest, int port, byte[] data,
            SmsSendFailureCallback callback);

    /**
     * Add a SmsListener so that it can be notified when new SMS from specific
     * address and application port has been received.
     *
     * @param from
     *            The address of the sender.
     * @param port
     *            The application port.
     * @param listener
     *            The listener which will be notified when SMS received.
     */
    public void addSmsListener(String from, int port, SmsListener listener);

    /**
     * Remove a SmsListener from the service so that it won't be notified
     * anymore.
     *
     * @param listener
     *            The listener to be removed.
     */
    public void removeSmsListener(SmsListener listener);
}
