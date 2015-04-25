/*
 * Copyright (C) 2011 The Guardian Project
 */

package info.guardianproject.otr.app.im;

interface IDataListener {

    void onTransferComplete(boolean outgoing, String offerId, String from, String url, String type, String fileLocalPath);

    void onTransferFailed(boolean outgoing, String offerId, String from, String url, String reason);

    void onTransferProgress(boolean outgoing, String offerId, String from, String url, float f);
    
    boolean onTransferRequested(String offerId, String from, String to, String transferUrl);  
  
}
