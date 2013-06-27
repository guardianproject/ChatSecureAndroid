package info.guardianproject.otr.app.im.engine;

public interface DataListener {
    void onTransferComplete(String from, String url, byte[] data);

    void onTransferFailed(String screenName, String url, String reason);

    void onTransferProgress(String screenName, String url, float f);
}
