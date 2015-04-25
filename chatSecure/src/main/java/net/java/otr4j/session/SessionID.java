/*
 * otr4j, the open source java otr library.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.otr4j.session;

import info.guardianproject.otr.app.im.engine.Address;

/** @author George Politis */
public final class SessionID {

    private String mLocalUserId;
    private String mRemoteUserId;
    private String mProtocolName;

    private String mSessionId;

    public static final SessionID Empty = new SessionID(null, null, null);

    public SessionID(String localUserId, String remoteUserId, String protocolName) {
       mLocalUserId = localUserId;
       mRemoteUserId = remoteUserId;
       mProtocolName = protocolName;
       mSessionId = Address.stripResource(mLocalUserId) + '_' + mProtocolName + '_' + Address.stripResource(mRemoteUserId);
    }

    public String getLocalUserId ()
    {
        return mLocalUserId;
    }

    public String getRemoteUserId ()
    {
        return mRemoteUserId;
    }

    public String getProtocolName ()
    {
        return mProtocolName;
    }

    public String toString() {
        return mSessionId;

    }

    public boolean equals(Object obj) {

        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;

        SessionID sessionID = (SessionID) obj;

        return this.toString().equals(sessionID.toString());
    }

    public int hashCode() {
        return this.toString().hashCode();
    }
}
