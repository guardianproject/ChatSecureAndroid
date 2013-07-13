package info.guardianproject.otr;

import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.DataHandler;
import info.guardianproject.otr.app.im.engine.DataListener;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.util.SystemServices;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.io.AbstractSessionInputBuffer;
import org.apache.http.impl.io.AbstractSessionOutputBuffer;
import org.apache.http.impl.io.HttpRequestParser;
import org.apache.http.impl.io.HttpRequestWriter;
import org.apache.http.impl.io.HttpResponseParser;
import org.apache.http.impl.io.HttpResponseWriter;
import org.apache.http.io.HttpMessageWriter;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineFormatter;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.message.LineFormatter;
import org.apache.http.message.LineParser;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class OtrDataHandler implements DataHandler {
    private static final int MAX_OUTSTANDING = 3;
    private static final int MAX_CHUNK_LENGTH = 32768;

    private static final int MAX_TRANSFER_LENGTH = 1024*1024*64;

    private static final byte[] EMPTY_BODY = new byte[0];

    private static final String TAG = "GB.OtrDataHandler";
    
    private static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);
    private static HttpParams params = new BasicHttpParams();
    private static HttpRequestFactory requestFactory = new MyHttpRequestFactory();
    private static HttpResponseFactory responseFactory = new DefaultHttpResponseFactory();

    private LineParser lineParser = new BasicLineParser(PROTOCOL_VERSION);
    private LineFormatter lineFormatter = new BasicLineFormatter();
    private ChatSession mChatSession;

    private DataListener mDataListener;

    private static String sStashUri;

    public OtrDataHandler(ChatSession chatSession, DataListener dataListener) {
        this.mChatSession = chatSession;
        this.mDataListener = dataListener;
    }

    public static class MyHttpRequestFactory implements HttpRequestFactory {
        public MyHttpRequestFactory() {
            super();
        }

        public HttpRequest newHttpRequest(final RequestLine requestline)
                throws MethodNotSupportedException {
            if (requestline == null) {
                throw new IllegalArgumentException("Request line may not be null");
            }
            //String method = requestline.getMethod();
            return new BasicHttpRequest(requestline);
        }

        public HttpRequest newHttpRequest(final String method, final String uri)
                throws MethodNotSupportedException {
            return new BasicHttpRequest(method, uri);
        }
    }
    
    static class MemorySessionInputBuffer extends AbstractSessionInputBuffer {
        public MemorySessionInputBuffer(byte[] value) {
            init(new ByteArrayInputStream(value), 1000, params);
        }

        @Override
        public boolean isDataAvailable(int timeout) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
    
    static class MemorySessionOutputBuffer extends AbstractSessionOutputBuffer {
        ByteArrayOutputStream outputStream;
        public MemorySessionOutputBuffer() {
            outputStream = new ByteArrayOutputStream(1000);
            init(outputStream, 1000, params);
        }
        
        public byte[] getOutput() {
            return outputStream.toByteArray();
        }
    }
    
    public void onIncomingRequest(Address us, byte[] value) {
        SessionInputBuffer inBuf = new MemorySessionInputBuffer(value); 
        HttpRequestParser parser = new HttpRequestParser(inBuf, lineParser, requestFactory, params);
        HttpRequest req;
        try {
            req = (HttpRequest)parser.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            e.printStackTrace();
            return;
        }
        
        String requestMethod = req.getRequestLine().getMethod();
        String uid = req.getFirstHeader("Request-Id").getValue();

        if (requestMethod.equals("OFFER")) {
            Log.i(TAG, "incoming OFFER");
            String url = req.getRequestLine().getUri();
            if (!url.startsWith("otr-in-band:")) {
                Log.w(TAG, "Unknown url scheme " + url);
                sendResponse(us, 400, "Unknown scheme", uid, EMPTY_BODY);
                return;
            }
            sendResponse(us, 200, "OK", uid, EMPTY_BODY);
            if (!req.containsHeader("File-Length"))
            {
                sendResponse(us, 400, "File-Length must be supplied", uid, EMPTY_BODY);
                return;
            }
            int length = Integer.parseInt(req.getFirstHeader("File-Length").getValue());
            if (!req.containsHeader("File-Hash-SHA1"))
            {
                sendResponse(us, 400, "File-Hash-SHA1 must be supplied", uid, EMPTY_BODY);
                return;
            }
            String sum = req.getFirstHeader("File-Hash-SHA1").getValue();
            Log.i(TAG, "Incoming sha1sum " + sum);
            Transfer transfer = new Transfer(url, length, us, sum);
            transferCache.put(url, transfer);
            // Handle offer
            // TODO ask user to confirm we want this
            transfer.perform();
        } else if (requestMethod.equals("GET")) {
            Log.i(TAG, "incoming GET");
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            try {
                FileInputStream is = new FileInputStream(sStashUri);
                if (!req.containsHeader("Range"))
                {
                    sendResponse(us, 400, "Range must start with bytes=", uid, EMPTY_BODY);
                    return;
                }
                String rangeHeader = req.getFirstHeader("Range").getValue();
                String[] spec = rangeHeader.split("=");
                if (spec.length != 2 || !spec[0].equals("bytes"))
                {
                    sendResponse(us, 400, "Range must start with bytes=", uid, EMPTY_BODY);
                    return;
                }
                String[] startEnd = spec[1].split("-");
                if (startEnd.length != 2)
                {
                    sendResponse(us, 400, "Range must be START-END", uid, EMPTY_BODY);
                    return;
                }

                int start = Integer.parseInt(startEnd[0]);
                int end = Integer.parseInt(startEnd[1]);
                if (end - start + 1 > MAX_CHUNK_LENGTH) {
                    sendResponse(us, 400, "Range must be at most " + MAX_CHUNK_LENGTH, uid, EMPTY_BODY);
                    return;
                }
                readIntoByteBuffer(byteBuffer, is, start, end);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NumberFormatException e) {
                sendResponse(us, 400, "Range is not numeric", uid, EMPTY_BODY);
                return;
            }
            byte[] body = byteBuffer.toByteArray();
            Log.i(TAG, "Sent sha1 is " + sha1sum(body));
            sendResponse(us, 200, "OK", uid, body);
        } else {
            Log.w(TAG, "Unknown method " + requestMethod);
            sendResponse(us, 400, "OK", uid, EMPTY_BODY);
        }
    }

    private void readIntoByteBuffer(ByteArrayOutputStream byteBuffer, FileInputStream is, int start, int end)
            throws IOException {
        if (start != is.skip(start)) {
            return;
        }
        int size = end - start + 1;
        int buffersize = 1024;
        byte[] buffer = new byte[buffersize];

        int len = 0;
        while((len = is.read(buffer)) != -1){
            if (len > size) {
                len = size;
            }
            byteBuffer.write(buffer, 0, len);
            size -= len;
        }
    }

    private void readIntoByteBuffer(ByteArrayOutputStream byteBuffer, SessionInputBuffer sib)
            throws IOException {
        int buffersize = 1024;
        byte[] buffer = new byte[buffersize];

        int len = 0;
        while((len = sib.read(buffer)) != -1){
            byteBuffer.write(buffer, 0, len);
        }
    }

    private void sendResponse(Address us, int code, String statusString, String uid, byte[] body) {
        MemorySessionOutputBuffer outBuf = new MemorySessionOutputBuffer();
        HttpMessageWriter writer = new HttpResponseWriter(outBuf, lineFormatter, params);
        HttpMessage response = new BasicHttpResponse(new BasicStatusLine(PROTOCOL_VERSION, code, statusString));
        response.addHeader("Request-Id", uid);
        try {
            writer.write(response);
            outBuf.write(body);
            outBuf.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            throw new RuntimeException(e);
        }
        byte[] data = outBuf.getOutput();
        Message message = new Message("");
        message.setFrom(us);
        Log.i(TAG, "send response");
        mChatSession.sendDataAsync(message, true, data);
    }

    public void onIncomingResponse(Address us, byte[] value) {
        SessionInputBuffer buffer = new MemorySessionInputBuffer(value); 
        HttpResponseParser parser = new HttpResponseParser(buffer, lineParser, responseFactory, params);
        HttpResponse res;
        try {
            res = (HttpResponse) parser.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            e.printStackTrace();
            return;
        }

        String uid = res.getFirstHeader("Request-Id").getValue();
        Request request = requestCache.getIfPresent(uid);
        if (request == null) {
            Log.w(TAG, "Unknown request ID " + uid);
            return;
        }

        if (request.isSeen()) {
            Log.w(TAG, "Already seen request ID " + uid);
            return;
        }
        
        request.seen();
        int statusCode = res.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            Log.w(TAG, "got status " + statusCode + ": " + res.getStatusLine().getReasonPhrase());
            // TODO handle error
            return;
        }

        // TODO handle success
        try {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            readIntoByteBuffer(byteBuffer, buffer);
            Log.i(TAG, "Received sha1 @" + request.start + " is " + sha1sum(byteBuffer.toByteArray()));
            if (request.method.equals("GET")) {
                Transfer transfer = transferCache.getIfPresent(request.url);
                if (transfer == null) {
                    Log.w(TAG, "Transfer expired for url " + request.url);
                    return;
                }
                transfer.chunkReceived(request, byteBuffer.toByteArray());
                if (transfer.isDone()) {
                    byte[] data = transfer.getData();
                    Log.i(TAG, "Transfer complete for " + request.url);
                    if (transfer.checkSum()) {
                        Log.i(TAG, "Received file len=" + data.length + " sha1=" + sha1sum(data));

                        mDataListener.onTransferComplete(
                                mChatSession.getParticipant().getAddress(),
                                transfer.url,
                                data);
                    } else {
                        mDataListener.onTransferFailed(
                                mChatSession.getParticipant().getAddress(),
                                transfer.url,
                                "checksum");
                        Log.e(TAG, "Wrong checksum for file len= " + data.length + " sha1=" + sha1sum(data));
                    }
                } else {
                    mDataListener.onTransferProgress(mChatSession.getParticipant().getAddress(), transfer.url, 
                            ((float)transfer.chunksReceived) / transfer.chunks);
                    transfer.perform();
                    Log.i(TAG, "Progress " + transfer.chunksReceived + " / " + transfer.chunks);
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not read line from response");
        }
    }

    /**
     * @param headers may be null 
     */
    @Override
    public void offerData(Address us, String localUri, Map<String, String> headers) {
        // TODO stash localUri and intended recipient
        sStashUri = localUri;
        long length = new File(localUri).length();
        if (length > MAX_TRANSFER_LENGTH) {
            throw new RuntimeException("Length too large " + length);
        }
        if (headers == null)
            headers = Maps.newHashMap();
        headers.put("File-Length", String.valueOf(length));
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        try {
            FileInputStream is = new FileInputStream(localUri);
            readIntoByteBuffer(byteBuffer, is, 0, (int)(length - 1));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        headers.put("File-Hash-SHA1", sha1sum(byteBuffer.toByteArray()));
        String[] paths = localUri.split("/");
        String url = "otr-in-band:/storage/" + SystemServices.sanitize(paths[paths.length - 1]);
        sendRequest(us, "OFFER", url, headers, EMPTY_BODY, new Request("OFFER", url));
    }

    public Request performGetData(Address us, String url, Map<String, String> headers, int start, int end) {
        String rangeSpec = "bytes=" + start + "-" + end;
        Log.i(TAG, "Getting range " + rangeSpec);
        headers.put("Range", rangeSpec);
        Request requestMemo = new Request("GET", url, start, end);

        sendRequest(us, "GET", url, headers, EMPTY_BODY, requestMemo);
        return requestMemo;
    }

    static class Request {
        public Request(String method, String url, int start, int end) {
            this.method = method;
            this.url = url;
            this.start = start;
            this.end = end;
        }

        public Request(String method, String url) {
            this(method, url, -1, -1);
        }
        
        public String method;
        public String url;
        public int start;
        public int end;
        public byte[] data;
        public boolean seen = false;
        
        public boolean isSeen() {
            return seen;
        }
        
        public void seen() {
            seen = true;
        }
    }
    
    class Transfer {
        public String url;
        public int chunks = 0;
        public int chunksReceived = 0;
        private int length = 0;
        private int current = 0;
        private Address us;
        private Set<Request> outstanding; 
        private byte[] buffer;
        private String sum;
        
        public Transfer(String url, int length, Address us, String sum) {
            this.url = url;
            this.length = length;
            this.us = us;
            this.sum = sum;
            
            if (length > MAX_TRANSFER_LENGTH || length <= 0) {
                throw new RuntimeException("Invalid transfer size " + length);
            }
            chunks = ((length - 1) / MAX_CHUNK_LENGTH) + 1;
            buffer = new byte[length];
            outstanding = Sets.newHashSet();
        }
        
        public boolean checkSum() {
            return sum.equals(sha1sum(buffer));
        }

        public boolean perform() {
            // TODO global throttle rather than this local hack
            while (outstanding.size() < MAX_OUTSTANDING) {
                if (current >= length)
                    return false;
                int end = current + MAX_CHUNK_LENGTH - 1;
                if (end >= length) {
                    end = length - 1;
                }
                Map<String, String> headers = Maps.newHashMap();
                Request request= performGetData(us, url, headers, current, end);
                outstanding.add(request);
                current = end + 1;
            }
            return true;
        }
        
        public byte[] getData() {
            // TODO Auto-generated method stub
            return buffer;
        }

        public boolean isDone() {
            return chunksReceived == chunks;
        }
        
        public void chunkReceived(Request request, byte[] bs) {
            chunksReceived++;
            System.arraycopy(bs, 0, buffer, request.start, bs.length);
            outstanding.remove(request);
        }

        public String getSum() {
            return sum;
        }
    }
    
    Cache<String, Request> requestCache = CacheBuilder.newBuilder().maximumSize(100).build();
    Cache<String, Transfer> transferCache = CacheBuilder.newBuilder().maximumSize(100).build();
    
    private void sendRequest(Address us, String method, String url, Map<String, String> headers, byte[] body, Request requestMemo) {
        MemorySessionOutputBuffer outBuf = new MemorySessionOutputBuffer();
        HttpMessageWriter writer = new HttpRequestWriter(outBuf, lineFormatter, params);
        HttpMessage req = new BasicHttpRequest(method, url, PROTOCOL_VERSION);
        String uid = UUID.randomUUID().toString();
        req.addHeader("Request-Id", uid);
        if (headers != null) {
            for (Entry<String, String> entry : headers.entrySet()) {
                req.addHeader(entry.getKey(), entry.getValue());
            }
        }
        
        try {
            writer.write(req);
            outBuf.write(body);
            outBuf.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            throw new RuntimeException(e);
        }
        byte[] data = outBuf.getOutput();
        Message message = new Message("");
        message.setFrom(us);
        Log.i(TAG, "send request " + method + " " + url);
        requestCache.put(uid, requestMemo);
        mChatSession.sendDataAsync(message, false, data);
    }
    
    private static String hexChr(int b) {
        return Integer.toHexString(b & 0xF);
    }

    private static String toHex(int b) {
        return hexChr((b & 0xF0) >> 4) + hexChr(b & 0x0F);
    }

    private String sha1sum(byte[] bytes) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        digest.update(bytes, 0, bytes.length);
        byte[] sha1sum = digest.digest();
        String display = "";
        for(byte b : sha1sum)
            display += toHex(b);
        return display;
    }

}
