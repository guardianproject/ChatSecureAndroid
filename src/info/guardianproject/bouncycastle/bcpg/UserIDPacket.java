package info.guardianproject.bouncycastle.bcpg;

import info.guardianproject.bouncycastle.util.Strings;

import java.io.IOException;


/**
 * Basic type for a user ID packet.
 */
public class UserIDPacket 
    extends ContainedPacket
{    
    private byte[]    idData;
    
    public UserIDPacket(
        BCPGInputStream  in)
        throws IOException
    {
        idData = new byte[in.available()];
        in.readFully(idData);
    }
    
    public UserIDPacket(
        String    id)
    {
        this.idData = Strings.toUTF8ByteArray(id);
    }
    
    public String getID()
    {
        return Strings.fromUTF8ByteArray(idData);
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.writePacket(USER_ID, idData, true);
    }
}
