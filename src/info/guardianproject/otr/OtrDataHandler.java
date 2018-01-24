package info.guardianproject.otr;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.RandomAccessFile;
import info.guardianproject.otr.app.im.IDataListener;
import info.guardianproject.otr.app.im.app.ImApp;
import info.guardianproject.otr.app.im.app.ChatFileStore;
import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.DataHandler;
import info.guardianproject.otr.app.im.engine.Message;
import info.guardianproject.util.Debug;
import info.guardianproject.util.LogCleaner;
import info.guardianproject.util.SystemServices;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.java.otr4j.session.SessionStatus;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
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

import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class OtrDataHandler implements DataHandler {
    public static final String URI_PREFIX_OTR_IN_BAND = "otr-in-band:/storage/";
    private static final int MAX_OUTSTANDING = 5;

    private static final int MAX_CHUNK_LENGTH = 32768/2;

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
    private long mChatId;

    private IDataListener mDataListener;
    private SessionStatus mOtrStatus;

    public OtrDataHandler(ChatSession chatSession) {
        this.mChatSession = chatSession;
    }

    public void setChatId(long chatId) {
        this.mChatId = chatId;
    }

    public void onOtrStatusChanged(SessionStatus status) {
        mOtrStatus = status;
        if (status == SessionStatus.ENCRYPTED) {
            retryRequests();
        }
    }

    private void retryRequests() {
        // Resend all unfilled requests
        for (Request request: requestCache.asMap().values()) {
            if (!request.isSeen())
                sendRequest(request);
        }
    }

    public void setDataListener (IDataListener dataListener)
    {
        mDataListener = dataListener;
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

    public void onIncomingRequest(Address requestThem, Address requestUs, byte[] value) {
        //Log.e( TAG, "onIncomingRequest:" + requestThem);

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
        String url = req.getRequestLine().getUri();

        if (requestMethod.equals("OFFER")) {
            debug("incoming OFFER " + url);
            if (!url.startsWith(URI_PREFIX_OTR_IN_BAND)) {
                debug("Unknown url scheme " + url);
                sendResponse(requestUs, 400, "Unknown scheme", uid, EMPTY_BODY);
                return;
            }
            sendResponse(requestUs, 200, "OK", uid, EMPTY_BODY);
            if (!req.containsHeader("File-Length"))
            {
                sendResponse(requestUs, 400, "File-Length must be supplied", uid, EMPTY_BODY);
                return;
            }
            int length = Integer.parseInt(req.getFirstHeader("File-Length").getValue());
            if (!req.containsHeader("File-Hash-SHA1"))
            {
                sendResponse(requestUs, 400, "File-Hash-SHA1 must be supplied", uid, EMPTY_BODY);
                return;
            }
            String sum = req.getFirstHeader("File-Hash-SHA1").getValue();
            String type = null;
            if (req.containsHeader("Mime-Type")) {
                type = req.getFirstHeader("Mime-Type").getValue();
            }
            debug("Incoming sha1sum " + sum);

            VfsTransfer transfer;
            try {
                transfer = new VfsTransfer(url, type, length, requestUs, sum);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            transferCache.put(url, transfer);

            // Handle offer

            // TODO ask user to confirm we want this
            boolean accept = false;

            if (mDataListener != null)
            {
                try {
                    mDataListener.onTransferRequested(url, requestThem.getAddress(),requestUs.getAddress(),transfer.url);

                    //callback is now async, via "acceptTransfer" method
                 //   if (accept)
                   //     transfer.perform();

                } catch (RemoteException e) {
                    LogCleaner.error(ImApp.LOG_TAG, "error approving OTRDATA transfer request", e);
                }

            }

        } else if (requestMethod.equals("GET") && url.startsWith(URI_PREFIX_OTR_IN_BAND)) {
            debug("incoming GET " + url);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int reqEnd;

            try {
                Offer offer = offerCache.getIfPresent(url);
                if (offer == null) {
                    sendResponse(requestUs, 400, "No such offer made", uid, EMPTY_BODY);
                    return;
                }

                offer.seen(); // in case we don't see a response to underlying request, but peer still proceeds

                if (!req.containsHeader("Range"))
                {
                    sendResponse(requestUs, 400, "Range must start with bytes=", uid, EMPTY_BODY);
                    return;
                }
                String rangeHeader = req.getFirstHeader("Range").getValue();
                String[] spec = rangeHeader.split("=");
                if (spec.length != 2 || !spec[0].equals("bytes"))
                {
                    sendResponse(requestUs, 400, "Range must start with bytes=", uid, EMPTY_BODY);
                    return;
                }
                String[] startEnd = spec[1].split("-");
                if (startEnd.length != 2)
                {
                    sendResponse(requestUs, 400, "Range must be START-END", uid, EMPTY_BODY);
                    return;
                }

                int start = Integer.parseInt(startEnd[0]);
                int end = Integer.parseInt(startEnd[1]);
                if (end - start + 1 > MAX_CHUNK_LENGTH) {
                    sendResponse(requestUs, 400, "Range must be at most " + MAX_CHUNK_LENGTH, uid, EMPTY_BODY);
                    return;
                }


                File fileGet = new File(offer.getUri());
                FileInputStream is = new FileInputStream(fileGet);
                readIntoByteBuffer(byteBuffer, is, start, end);
                is.close();

                if (mDataListener != null)
                {
                    float percent = ((float)end) / ((float)fileGet.length());

                    mDataListener.onTransferProgress(true, offer.getId(), requestThem.getAddress(), offer.getUri(),
                        percent);

                    String mimeType = null;
                    if (req.getFirstHeader("Mime-Type") != null)
                        mimeType = req.getFirstHeader("Mime-Type").getValue();
                    mDataListener.onTransferComplete(true, offer.getId(), requestThem.getAddress(), offer.getUri(), mimeType, offer.getUri());
                
                }

            } catch (UnsupportedEncodingException e) {
            //    throw new RuntimeException(e);
                sendResponse(requestUs, 400, "Unsupported encoding", uid, EMPTY_BODY);
                return;
            } catch (IOException e) {
                //throw new RuntimeException(e);
                sendResponse(requestUs, 400, "IOException", uid, EMPTY_BODY);
                return;
            } catch (NumberFormatException e) {
                sendResponse(requestUs, 400, "Range is not numeric", uid, EMPTY_BODY);
                return;
            } catch (Exception e) {
                sendResponse(requestUs, 500, "Unknown error", uid, EMPTY_BODY);
                return;
            }


            byte[] body = byteBuffer.toByteArray();
            debug("Sent sha1 is " + sha1sum(body));
            sendResponse(requestUs, 200, "OK", uid, body);


        } else {
            debug("Unknown method / url " + requestMethod + " " + url);
            sendResponse(requestUs, 400, "OK", uid, EMPTY_BODY);
        }
    }

    public void acceptTransfer (String url)
    {
        Transfer transfer = transferCache.getIfPresent(url);
        if (transfer != null)
        {
            transfer.perform();

        }

    }

    private static void readIntoByteBuffer(ByteArrayOutputStream byteBuffer, FileInputStream is, int start, int end)
            throws IOException {
        //Log.e( TAG, "readIntoByteBuffer:" + (end-start));
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

    private static void readIntoByteBuffer(ByteArrayOutputStream byteBuffer, SessionInputBuffer sib)
            throws IOException {
        //Log.e( TAG, "readIntoByteBuffer:");
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
        debug("send response " + statusString + " for " + uid);
        mChatSession.sendDataAsync(message, true, data);
    }

    public void onIncomingResponse(Address from, Address to, byte[] value) {
        //Log.e( TAG, "onIncomingResponse:" + value.length);
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
            debug("Unknown request ID " + uid);
            return;
        }

        if (request.isSeen()) {
            debug("Already seen request ID " + uid);
            return;
        }

        request.seen();
        int statusCode = res.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            debug("got status " + statusCode + ": " + res.getStatusLine().getReasonPhrase());
            // TODO handle error
            return;
        }

        // TODO handle success
        try {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            readIntoByteBuffer(byteBuffer, buffer);
            debug("Received sha1 @" + request.start + " is " + sha1sum(byteBuffer.toByteArray()));
            if (request.method.equals("GET")) {
                VfsTransfer transfer = transferCache.getIfPresent(request.url);
                if (transfer == null) {
                    debug("Transfer expired for url " + request.url);
                    return;
                }
                transfer.chunkReceived(request, byteBuffer.toByteArray());
                if (transfer.isDone()) {
                    //Log.e( TAG, "onIncomingResponse: isDone");
                    debug("Transfer complete for " + request.url);
                    String filename = transfer.closeFile();
                    Uri vfsUri = ChatFileStore.vfsUri(filename);
                    if (transfer.checkSum()) {

                        //Log.e( TAG, "onIncomingResponse: writing");
                        if (mDataListener != null)
                            mDataListener.onTransferComplete(
                                    false,
                                    null,
                                mChatSession.getParticipant().getAddress().getAddress(),
                                transfer.url,
                                transfer.type,
                                vfsUri.toString());
                    } else {
                        if (mDataListener != null)
                            mDataListener.onTransferFailed(
                                    false,
                                    null,
                                mChatSession.getParticipant().getAddress().getAddress(),
                                transfer.url,
                                "checksum");
                        debug( "Wrong checksum for file");
                    }
                } else {
                    if (mDataListener != null)
                        mDataListener.onTransferProgress(true, null, mChatSession.getParticipant().getAddress().getAddress(), transfer.url,
                            ((float)transfer.chunksReceived) / transfer.chunks);
                    transfer.perform();
                    debug("Progress " + transfer.chunksReceived + " / " + transfer.chunks);
                }
            }
        } catch (IOException e) {
            debug("Could not read line from response");
        } catch (RemoteException e) {
            debug("Could not read remote exception");
        }

    }

    private String getFilenameFromUrl(String url) {
        String[] path = url.split("/");
        String sanitizedPath = SystemServices.sanitize(path[path.length - 1]);
        return sanitizedPath;
    }

    /**
    private File writeDataToStorage (String url, byte[] data)
    {
        debug( "writeDataToStorage:" + url + " " + data.length);

        String[] path = url.split("/");
        String sanitizedPath = SystemServices.sanitize(path[path.length - 1]);

        File fileDownloadsDir = new File(Environment.DIRECTORY_DOWNLOADS);
        fileDownloadsDir.mkdirs();

        info.guardianproject.iocipher.File file = new info.guardianproject.iocipher.File(fileDownloadsDir, sanitizedPath);
        debug( "writeDataToStorage:" + file.getAbsolutePath() );

        try {
            OutputStream output = (new info.guardianproject.iocipher.FileOutputStream(file));
            output.write(data);
            output.flush();
            output.close();
            return file;
        } catch (IOException e) {
            OtrDebugLogger.log("error writing file", e);
            return null;
        }
    }*/

    @Override
    public void offerData(String id, Address us, String localUri, Map<String, String> headers) throws IOException {
        
        // TODO stash localUri and intended recipient
        
        
        long length = new File(localUri).length();
        if (length > MAX_TRANSFER_LENGTH) {
            throw new IOException("Length too large: " + length);
        }
        if (headers == null)
            headers = Maps.newHashMap();
        headers.put("File-Length", String.valueOf(length));

        try {
            
            FileInputStream is = new FileInputStream(localUri);
            headers.put("File-Hash-SHA1", sha1sum(is));
            is.close();

            String[] paths = localUri.split("/");
            String url = URI_PREFIX_OTR_IN_BAND + SystemServices.sanitize(paths[paths.length - 1]);
            Request request = new Request("OFFER", us, url, headers);
            offerCache.put(url, new Offer(id, localUri, request));
            sendRequest(request);

        } catch (IOException e) {
            Log.e(ImApp.LOG_TAG,"error opening file",e);
        }
    }

    public Request performGetData(Address us, String url, Map<String, String> headers, int start, int end) {
        String rangeSpec = "bytes=" + start + "-" + end;
        headers.put("Range", rangeSpec);
        Request request = new Request("GET", us, url, start, end, headers, EMPTY_BODY);

        sendRequest(request);
        return request;
    }

    static class Offer {
        private String mId;
        private String mUri;
        private Request request;

        public Offer(String id, String uri, Request request) {
            this.mId = id;
            this.mUri = uri;
            this.request = request;
        }

        public String getUri() {
            return mUri;
        }

        public String getId() {
            return mId;
        }

        public Request getRequest() {
            return request;
        }

        public void seen() {
            request.seen();
        }
    }

    static class Request {

        public Request(String method, Address us, String url, int start, int end, Map<String, String> headers, byte[] body) {
            this.method = method;
            this.url = url;
            this.start = start;
            this.end = end;
            this.us = us;
            this.headers = headers;
            this.body = body;
        }

        public Request(String method, Address us, String url, Map<String, String> headers) {
            this(method, us, url, -1, -1, headers, null);
        }

        public String method;
        public String url;
        public int start;
        public int end;
        public byte[] data;
        public boolean seen = false;
        public Address us;
        public Map<String, String> headers;
        public byte[] body;

        public boolean isSeen() {
            return seen;
        }

        public void seen() {
            seen = true;
        }
    }

    public class Transfer {
        public final String TAG = Transfer.class.getSimpleName();
        public String url;
        public String type;
        public int chunks = 0;
        public int chunksReceived = 0;
        private int length = 0;
        private int current = 0;
        private Address us;
        protected Set<Request> outstanding;
        private byte[] buffer;
        protected String sum;

        public Transfer(String url, String type, int length, Address us, String sum) {
            this.url = url;
            this.type = type;
            this.length = length;
            this.us = us;
            this.sum = sum;

            //Log.e(TAG, "url:"+url + " type:"+ type + " length:"+length) ;

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

        public boolean isDone() {
            //Log.e( TAG, "isDone:" + chunksReceived + " " + chunks);
            return chunksReceived == chunks;
        }

        public void chunkReceived(Request request, byte[] bs) {
            //Log.e( TAG, "chunkReceived:" + bs.length);
            chunksReceived++;
            System.arraycopy(bs, 0, buffer, request.start, bs.length);
            outstanding.remove(request);
        }

        public String getSum() {
            return sum;
        }
    }

    public class VfsTransfer extends Transfer {
        String localFilename;
        private RandomAccessFile raf;

        public VfsTransfer(String url, String type, int length, Address us, String sum) throws FileNotFoundException {
            super(url, type, length, us, sum);
        }

        @Override
        public void chunkReceived(Request request, byte[] bs) {
            debug( "chunkReceived: start: :" + request.start + " length " + bs.length) ;
            chunksReceived++;
            try {
                raf.seek( request.start );
                raf.write(bs) ;
            } catch (IOException e) {
                e.printStackTrace();
            }
            outstanding.remove(request);
        }

        @Override
        public boolean checkSum() {
            try {
                File file = new File(localFilename);
                return sum.equals( checkSum(file.getAbsolutePath()) );
            } catch (IOException e) {
                debug("checksum IOException");
                return false;
            }
        }

        @Override
        public boolean perform() {
            boolean result = super.perform();
            try {
                if (raf == null) {
                    raf = openFile(url);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return result;
        }

        private RandomAccessFile openFile(String url) throws FileNotFoundException {
            debug( "openFile: url " + url) ;
            String sessionId = ""+ mChatId;
            String filename = getFilenameFromUrl(url);
            localFilename = ChatFileStore.getDownloadFilename( sessionId, filename );
            debug( "openFile: localFilename " + localFilename) ;
            info.guardianproject.iocipher.RandomAccessFile ras = new info.guardianproject.iocipher.RandomAccessFile(localFilename, "rw");
            return ras;
        }

        public String closeFile() throws IOException {
            //Log.e(TAG, "closeFile") ;
            raf.close();
            File file = new File(localFilename);
            String newPath = file.getCanonicalPath();
            if(true) return newPath;

            newPath = newPath.substring(0,newPath.length()-4); // remove the .tmp
            //Log.e(TAG, "vfsCloseFile: rename " + newPath) ;
            File newPathFile = new File(newPath);
            boolean success = file.renameTo(newPathFile);
            if (!success) {
                throw new IOException("Rename error " + newPath );
            }
            return newPath;
        }

        private String checkSum(String filename) throws IOException {
            FileInputStream fis = new FileInputStream(new File(filename));
            String sum = sha1sum(fis);
            fis.close();
            return sum;
        }
    }

    Cache<String, Offer> offerCache = CacheBuilder.newBuilder().maximumSize(100).build();
    Cache<String, Request> requestCache = CacheBuilder.newBuilder().maximumSize(100).build();
    Cache<String, VfsTransfer> transferCache = CacheBuilder.newBuilder().maximumSize(100).build();

    private void sendRequest(Request request) {
        MemorySessionOutputBuffer outBuf = new MemorySessionOutputBuffer();
        HttpMessageWriter writer = new HttpRequestWriter(outBuf, lineFormatter, params);
        HttpMessage req = new BasicHttpRequest(request.method, request.url, PROTOCOL_VERSION);
        String uid = UUID.randomUUID().toString();
        req.addHeader("Request-Id", uid);
        if (request.headers != null) {
            for (Entry<String, String> entry : request.headers.entrySet()) {
                req.addHeader(entry.getKey(), entry.getValue());
            }
        }

        try {
            writer.write(req);
            outBuf.write(request.body);
            outBuf.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            throw new RuntimeException(e);
        }
        byte[] data = outBuf.getOutput();
        Message message = new Message("");
        message.setFrom(request.us);
        if (req.containsHeader("Range"))
            debug("send request " + request.method + " " + request.url + " " + req.getFirstHeader("Range"));
        else
            debug("send request " + request.method + " " + request.url);
        requestCache.put(uid, request);
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

    private String sha1sum(java.io.InputStream is) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");

            DigestInputStream dig = new DigestInputStream(is, digest);
            IOUtils.copy( dig, new NullOutputStream() );

            byte[] sha1sum = digest.digest();
            String display = "";
            for(byte b : sha1sum)
                display += toHex(b);
            return display;
        }
        catch (Exception npe)
        {
            Log.e(ImApp.LOG_TAG,"unable to hash file",npe);
            return null;
        }

    }


    private void debug (String msg)
    {
        if (Debug.DEBUG_ENABLED)
            Log.d(ImApp.LOG_TAG,msg);
    }
}
