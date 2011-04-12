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
 * The interface to access system service objects.
 * 
 */
public abstract class SystemService {
    /**
     * Gets the default instance of the system service.
     * 
     * @return the default instance of the system service.
     */
    public static SystemService getDefault() {
        // TODO return AndroidSystemService.getInstance();
    	return null;
    }

    /**
     * Gets the system HeartbeatService.
     * 
     * @return the instance of the HeartbeatService.
     */
    public abstract HeartbeatService getHeartbeatService();

    /**
     * Gets the system SmsService.
     * 
     * @return the instance of the SmsService.
     */
    // TODO public abstract SmsService getSmsService();
}
