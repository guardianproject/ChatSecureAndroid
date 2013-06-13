package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.otr.app.im.engine.Address;
import android.os.Parcel;

public class XmppAddress extends Address {

    private String address;
    private String name;
    private String resource;
    
    public XmppAddress() {
    }

    public XmppAddress(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public XmppAddress(String address) {
        
        this (address.replaceFirst("@.*", ""), address);
        
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getScreenName() {
        return name;
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