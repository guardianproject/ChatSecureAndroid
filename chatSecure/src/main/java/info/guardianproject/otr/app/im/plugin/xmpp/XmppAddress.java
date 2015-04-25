package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.otr.app.im.engine.Address;
import android.os.Parcel;
import android.os.Parcelable;

public class XmppAddress extends Address {

    private String mAddress;
    private String mUser;
    private String mResource;

    public XmppAddress() {}

    @Override
    public String getBareAddress() {
        int resIdx;
        if ((resIdx = mAddress.indexOf("/"))!=-1)
            return mAddress.substring(0, resIdx);
        else
            return mAddress;
    }

    public XmppAddress(String fullJid) {

        mUser = fullJid.replaceFirst("@.*", "");
        mAddress = fullJid;

        String[] presenceParts = fullJid.split("/");
        if (presenceParts.length > 1)
            mResource = presenceParts[1];

    }

    @Override
    public String getAddress() {
        return mAddress;
    }

    @Override
    public String getUser() {
        return mUser;
    }

    @Override
    public void readFromParcel(Parcel source) {
        mUser = source.readString();
        mAddress = source.readString();
        mResource = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest) {
        dest.writeString(mUser);
        dest.writeString(mAddress);
        dest.writeString(mResource);
    }

    @Override
    public String getResource() {
        return mResource;
    }

}