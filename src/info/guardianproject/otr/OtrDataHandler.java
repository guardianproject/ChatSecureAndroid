package info.guardianproject.otr;

import info.guardianproject.otr.app.im.engine.Address;
import info.guardianproject.otr.app.im.engine.ChatSession;
import info.guardianproject.otr.app.im.engine.DataHandler;
import info.guardianproject.otr.app.im.engine.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

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

public class OtrDataHandler implements DataHandler {
    private static final byte[] EMPTY_BODY = new byte[0];

    private static final String TAG = "GB.OtrDataHandler";
    
    private static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);
    private static HttpParams params = new BasicHttpParams();
    private static HttpRequestFactory requestFactory = new MyHttpRequestFactory();
    private static HttpResponseFactory responseFactory = new DefaultHttpResponseFactory();

    private LineParser lineParser = new BasicLineParser(PROTOCOL_VERSION);
    private LineFormatter lineFormatter = new BasicLineFormatter();
    private ChatSession mChatSession;

    public OtrDataHandler(ChatSession chatSession) {
        this.mChatSession = chatSession;
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
        if (requestMethod.equals("OFFER")) {
            Log.i(TAG, "incoming OFFER");
            String url = req.getRequestLine().getUri();
            if (!url.startsWith("otr-in-band:")) {
                Log.w(TAG, "Unknown url scheme " + url);
                sendResponse(us, 400, "Unknown scheme", EMPTY_BODY);
                return;
            }
            sendResponse(us, 200, "OK", EMPTY_BODY);
            // Handle offer
            // TODO ask user to confirm we want this
            getData(us, url, null);
        } else if (requestMethod.equals("GET")) {
            Log.i(TAG, "incoming GET");
            byte[] body;
            try {
                body = "this is content".getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            sendResponse(us, 200, "OK", body);
        } else {
            Log.w(TAG, "Unknown method " + requestMethod);
            sendResponse(us, 400, "OK", EMPTY_BODY);
        }
    }

    private void sendResponse(Address us, int code, String statusString, byte[] body) {
        MemorySessionOutputBuffer outBuf = new MemorySessionOutputBuffer();
        HttpMessageWriter writer = new HttpResponseWriter(outBuf, lineFormatter, params);
        HttpMessage response = new BasicHttpResponse(new BasicStatusLine(PROTOCOL_VERSION, code, statusString));
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
        
        int statusCode = res.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            Log.w(TAG, "got status " + statusCode + ": " + res.getStatusLine().getReasonPhrase());
            // TODO handle error
            return;
        }

        // TODO handle success
        try {
            Log.i(TAG, "Response is " + buffer.readLine());
        } catch (IOException e) {
            Log.w(TAG, "Could not read line from response");
        }
    }

    @Override
    public void offerData(Address us, String url, Map<String, String> headers) {
        sendRequest(us, "OFFER", url, EMPTY_BODY);
    }

    @Override
    public void getData(Address us, String url, Map<String, String> headers) {
        sendRequest(us, "GET", url, EMPTY_BODY);
    }

    private void sendRequest(Address us, String method, String url, byte[] body) {
        MemorySessionOutputBuffer outBuf = new MemorySessionOutputBuffer();
        HttpMessageWriter writer = new HttpRequestWriter(outBuf, lineFormatter, params);
        HttpMessage request = new BasicHttpRequest(method, url, PROTOCOL_VERSION);
        try {
            writer.write(request);
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
        mChatSession.sendDataAsync(message, false, data);
    }
}
