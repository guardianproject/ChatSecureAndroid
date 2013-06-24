package info.guardianproject.otr.app.im.engine;

public interface DataListener {
    void onTransferComplete(String from, byte[] data);
}
