package info.guardianproject.otr.app.im.plugin;

import info.guardianproject.otr.app.im.engine.Address;
import android.os.Parcel;

public class XmppAddress extends Address {

    private String address;
    private String name;

    public XmppAddress() {
    }

    public XmppAddress(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public XmppAddress(String address) {
        this.name = address.replaceFirst("@.*", "");
        this.address = address;
    }

    @Override
    public String getFullName() {
        return address;
    }

    @Override
    public String getScreenName() {
        return name;
    }
    
    @Override
    public Address appendResource(String resource) {
        if (resource == null || "".equals(resource))
            return this;
        return new XmppAddress(name, address + "/" + resource);
    }

    @Override
    public void readFromParcel(Parcel source) {
        name = source.readString();
        address = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest) {
        dest.writeString(name);
        dest.writeString(address);
    }

}