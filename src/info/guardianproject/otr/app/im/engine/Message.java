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

package info.guardianproject.otr.app.im.engine;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an instant message send between users.
 */
public class Message implements Parcelable {
    private String mId;
    private Address mFrom;
    private Address mTo;
    private String mBody;
    private Date mDate;

    /**
     *
     * @param msg
     * @throws NullPointerException if msg is null.
     */
    public Message(String msg) {
        if (msg == null) {
            throw new NullPointerException("null msg");
        }
        mBody = msg;
    }

    public Message(Parcel source) {
        mId = source.readString();
        mFrom = AddressParcelHelper.readFromParcel(source);
        mTo = AddressParcelHelper.readFromParcel(source);
        mBody = source.readString();
        long time = source.readLong();
        if(time != -1) {
            mDate = new Date(time);
        }
    }

    /**
     * Gets an identifier of this message. May be <code>null</code> if the
     * underlying protocol doesn't support it.
     *
     * @return the identifier of this message.
     */
    public String getID() {
        return mId;
    }

    /**
     * Gets the body of this message.
     *
     * @return the body of this message.
     */
    public String getBody() {
        return mBody;
    }

    /**
     * Gets the address where the message is sent from.
     *
     * @return the address where the message is sent from.
     */
    public Address getFrom() {
        return mFrom;
    }

    /**
     * Gets the address where the message is sent to.
     *
     * @return the address where the message is sent to.
     */
    public Address getTo() {
        return mTo;
    }

    /**
     * Gets the date time associated with this message. If it's a message sent
     * from this client, the date time is when the message is sent. If it's a
     * message received from other users, the date time is either when the
     * message was received or sent, depending on the underlying protocol.
     *
     * @return the date time.
     */
    public Date getDateTime() {
        if (mDate == null) {
            return null;
        }
        return new Date(mDate.getTime());
    }

    public void setID(String id) {
        mId = id;
    }

    public void setBody(String body) {
        mBody = body;
    }

    public void setFrom(Address from) {
        mFrom = from;
    }

    public void setTo(Address to) {
        mTo = to;
    }

    public void setDateTime(Date dateTime) {
        long time = dateTime.getTime();
        if (mDate == null) {
            mDate = new Date(time);
        } else {
            mDate.setTime(time);
        }
    }

    public String toString() {
        return "From: " + mFrom.getScreenName() + " To: " + mTo.getScreenName()
                + " " + mBody;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        AddressParcelHelper.writeToParcel(dest, mFrom);
        AddressParcelHelper.writeToParcel(dest, mTo);
        dest.writeString(mBody);
        dest.writeLong(mDate == null ? -1 : mDate.getTime());
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}
