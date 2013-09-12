/*
 * Copyright (C) 2011 The Guardian Project
 */

package info.guardianproject.otr.app.im;

interface IDataListener {

    void onTransferComplete(String from, String url, String type, String fileLocalPath);

    void onTransferFailed(String from, String url, String reason);

    void onTransferProgress(String from, String url, float f);
    
    boolean onTransferRequested(String from, String transferUrl);  
  
}
