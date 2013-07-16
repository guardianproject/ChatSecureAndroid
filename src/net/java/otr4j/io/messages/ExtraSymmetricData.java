package net.java.otr4j.io.messages;

import java.util.ArrayList;
import java.util.List;

public class ExtraSymmetricData {
    public static class Data {
        private int use;
        private byte[] data;

        public Data(int use, byte[] data) {
            this.use = use;
            this.data = data;
        }

        /** Data to be interpreted by the application */
        public byte[] getData() {
            return data;
        }

        /** Application identifier */
        public int getUse() {
            return use;
        }
    }
    
    private List<Data> datas;
    private byte[] extraKey;
    
    public ExtraSymmetricData() {
    }
    
    /** In-band data */
    public List<Data> getDatas() {
        return datas;
    }
    
    public void addData(Data data) {
        if (datas == null)
            datas = new ArrayList<Data>();
        datas.add(data);
    }
    
    /** Symmetric key for encrypting out-of-band data (outside of the OTR protocol) */
    public byte[] getExtraKey() {
        return extraKey;
    }
    
    public void setExtraKey(byte[] extraKey) {
        this.extraKey = extraKey;
    }
}
