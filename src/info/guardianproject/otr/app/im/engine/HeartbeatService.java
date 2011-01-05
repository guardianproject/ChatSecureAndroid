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

public interface HeartbeatService {
    public interface Callback {
        /**
         * Called on heartbeat schedule.
         *
         * @return the offset in milliseconds that the method wants to
         * be called the next time. Return 0 or negative value indicates to stop
         * the schedule of this callback.
         */
        public long sendHeartbeat();
    }

    /**
     * Start to schedule a heartbeat operation.
     *
     * @param callback The operation wants to be called repeat.
     * @param triggerTime The time(in milliseconds) until the operation
     *            will be executed the first time.
     */
    public void startHeartbeat(Callback callback, long triggerTime);

    /**
     * Stop scheduling a heartbeat operation.
     *
     * @param callback The operation will be stopped.
     */
    public void stopHeartbeat(Callback callback);
}
