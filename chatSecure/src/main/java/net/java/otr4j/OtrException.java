package net.java.otr4j;

@SuppressWarnings("serial")
public class OtrException extends Exception {
    public OtrException(Exception e) {
        super(e);
    }

    public OtrException(String m) {
        super(m);
    }
}
