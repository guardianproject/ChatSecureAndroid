package info.guardianproject.otr.app.im.plugin.xmpp;

import info.guardianproject.otr.app.im.engine.Address;
import android.os.Parcel;

public class XmppAddress extends Address {

    private String mAddress;
    private String mScreenName;
    private String mResource;
    
    public XmppAddress() {
        
    }
    public XmppAddress(String name, String address, String resource) {
        mScreenName = name;
        mAddress = address;
        mResource = resource;
    }
    
    public XmppAddress(String name, String address) {
        mScreenName = name;
        mAddress = address;
        
        int resIdx;
        
        if ((resIdx = mAddress.indexOf("/"))!=-1)
                mResource = mAddress.substring(resIdx+1);
     
    }

    public XmppAddress(String fullJid) {
        
        mScreenName = fullJid.replaceFirst("@.*", "");
        mAddress = fullJid;
        
        int resIdx;
        if ((resIdx = fullJid.indexOf("/"))!=-1)
            mResource = fullJid.substring(resIdx+1);
        
    }

    @Override
    public String getAddress() {
        return mAddress;
    }

    @Override
    public String getScreenName() {
        return mScreenName;
    }
   
    @Override
    public void readFromParcel(Parcel source) {
        mScreenName = source.readString();
        mAddress = source.readString();
        mResource = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest) {
        dest.writeString(mScreenName);
        dest.writeString(mAddress);
        dest.writeString(mResource);
    }
    
    @Override
    public String getResource() {
        return mResource;
    }
    
}