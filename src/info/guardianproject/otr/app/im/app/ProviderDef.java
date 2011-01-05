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

package info.guardianproject.otr.app.im.app;

public class ProviderDef {
    public long mId;
    public String mName;
    public String mFullName;
    public String mSignUpUrl;

    public ProviderDef(long id, String name, String fullName, String signUpUrl) {
        mId = id;
        mName = name;
        if (fullName != null) {
            mFullName = fullName;
        } else {
            mFullName = name;
        }
        mSignUpUrl = signUpUrl;
    }
}
