package net.java.otr4j.io.messages;

public class ExtraSymmetricData {
    private int use;
    private byte[] data;
    
    public ExtraSymmetricData(int use, byte[] data) {
        this.use = use;
        this.data = data;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public int getUse() {
        return use;
    }
}
