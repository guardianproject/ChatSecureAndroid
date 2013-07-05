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
        if (resource != null)
            return address + "/" + resource;
        else
            return address;
    }

    @Override
    public String getScreenName() {
        if (resource != null)
            return name + " (" + resource + ")";
        else 
            return name;
    }
    
    @Override
    public Address appendResource(String resource) {
        XmppAddress result = new XmppAddress(name, address);
        result.resource = resource;
        return result;
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
    
    public String getResource() {
        return resource;
    }
}