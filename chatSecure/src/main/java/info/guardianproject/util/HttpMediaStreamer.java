/**
 *
 */
package info.guardianproject.util;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.net.Uri;
import android.util.Log;

/**
 * Copyright (C) 2014.  All rights reserved.
 *
 * @author liorsaar
 *
 */
public class HttpMediaStreamer {
    private static final String TAG = HttpMediaStreamer.class.getSimpleName();
    private Uri uri;
    private ServerSocket serverSocket;

    public HttpMediaStreamer(String filename, String mimeType) throws IOException {
        uri = create(filename, mimeType);
    }

    public Uri getUri() {
        return uri;
    }

    public void destroy() {
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (Exception e) {
        }
    }

    private Uri create(final String filename, final String mimeType) throws IOException {

        // FIXME generate a random token for security
        final File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("File not found " + filename);
        }

        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (Exception e) {
        }

        serverSocket = new ServerSocket(0); // use random free port
        new Thread() {
            public void run() {
                try {
                    while (true) {
                    Socket socket = serverSocket.accept();

                    byte[] b = new byte[8192];
                    int len;

                    InputStream is = socket.getInputStream();
                    StringBuilder isb = new StringBuilder();
                    len = is.read(b);
                    isb.append(new String(b));

                    //Log.i(TAG, "request: " + isb.toString());

                    StringBuilder sb = new StringBuilder();
                    sb.append("HTTP/1.1 200\r\n");
                    sb.append("Content-Type: " + mimeType + "\r\n");
                    sb.append("Content-Length: " + file.length() + "\r\n\r\n");

                    BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

                    bos.write(sb.toString().getBytes());

                    FileInputStream fis = new FileInputStream(file);

                    int idx = 0;

                    while ((len = fis.read(b)) != -1) {
                        bos.write(b, 0, len);
                        idx += len;
                        Log.d(TAG, "sharing via stream: " + idx);
                    }

                    fis.close();
                    bos.flush();
                    bos.close();

                    socket.close();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "web share error", e);
                }
            }
        }.start();

        Uri uri = Uri.parse("http://localhost:" + serverSocket.getLocalPort() + file.getAbsolutePath());
        return uri;
    }
}
