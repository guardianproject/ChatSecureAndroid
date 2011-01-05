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
 * A wrapper of the login information.
 */
public class LoginInfo {
    private String mUserName;
    private String mPassword;

    public LoginInfo(String userName, String password) {
        mUserName = userName;
        mPassword = password;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getPassword() {
        return mPassword;
    }
}
