package net.java.otr4j.session;

public class TLV {
    /* This is just padding for the encrypted message, and should be ignored. */
    public static final int PADDING=0;
    /* The sender has thrown away his OTR session keys with you */
    public static final int DISCONNECTED=0x0001;

    /* The message contains a step in the Socialist Millionaires' Protocol. */ 
    public static final int SMP1=0x0002;
    public static final int SMP2=0x0003;
    public static final int SMP3=0x0004;
    public static final int SMP4=0x0005;
    public static final int SMP_ABORT=0x0006;
    /* Like OTRL_TLV_SMP1, but there's a question for the buddy at the
     * beginning */
    public static final int SMP1Q=0x0007;

	public TLV(int type, byte[] value) {
		this.setType(type);
		this.setValue(value);
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public void setValue(byte[] value) {
		this.value = value;
	}

	public byte[] getValue() {
		return value;
	}

	int type;
	byte[] value;
}