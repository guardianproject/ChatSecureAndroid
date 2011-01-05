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

import android.os.Parcel;

/**
 * An abstract representation of the address to any addressable entities such as
 * User, Contact list, User Group, etc.
 */
public abstract class Address {
    /**
     * Gets a string representation of this address.
     *
     * @return a string representation of this address.
     */
    public abstract String getFullName();

    /**
     * Gets a user friendly screen name of this address object.
     * @return the screen name.
     */
    public abstract String getScreenName();

    public abstract void writeToParcel(Parcel dest);

    public abstract void readFromParcel(Parcel source);

    public boolean equals(Object other) {
        return other instanceof Address
                && this.getFullName().equals(((Address)other).getFullName());
    }

    public int hashCode() {
        return getFullName().hashCode();
    }
}
