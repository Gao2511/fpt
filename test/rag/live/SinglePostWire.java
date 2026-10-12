package rag.live;

import com.google.gson.JsonObject;
import javax.net.ssl.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Fresh verified TLS socket, exactly one HTTP/1.1 POST write. No HTTP client retry/redirect/proxy machinery. */
public final class SinglePostWire implements LiveSafety.Wire {
    private static final int LIMIT=262144;
    private static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"live-test-deadline");t.setDaemon(true);return t;});
    private static final ExecutorService DNS=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"live-test-dns");t.setDaemon(true);return t;});
    public LiveSafety.Response post(String endpoint,String key,JsonObject body)throws Exception{
        URI uri=URI.create(endpoint);LiveSafety.key(key);
        LiveSafety.require("https".equals(uri.getScheme())&&"generativelanguage.googleapis.com".equals(uri.getHost())&&uri.getPort()==-1&&uri.getQuery()==null&&uri.getFragment()==null&&uri.getUserInfo()==null,"TLS_ENDPOINT");
        try(Socket tcp=new Socket()){
            // Close cancels handshake/read/write as well as slow incremental responses; no reconnect is possible.
            java.util.concurrent.atomic.AtomicBoolean expired=new java.util.concurrent.atomic.AtomicBoolean();
            ScheduledFuture<?> deadline=TIMER.schedule(()->{expired.set(true);try{tcp.close();}catch(IOException ignored){}},10000,TimeUnit.MILLISECONDS);
            try{
                Future<InetAddress> dns=DNS.submit(()->InetAddress.getByName(uri.getHost()));InetAddress address;
                try{address=dns.get(3000,TimeUnit.MILLISECONDS);}catch(TimeoutException e){dns.cancel(true);throw new SocketTimeoutException("DNS_TIMEOUT");}
                tcp.connect(new InetSocketAddress(address,443),5000);
                // Layer with the approved hostname to keep HTTPS peer-name verification independent of DNS/IP.
                try(SSLSocket socket=(SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(tcp,uri.getHost(),443,true)){
                SSLParameters params=socket.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");params.setServerNames(Collections.singletonList(new SNIHostName(uri.getHost())));socket.setSSLParameters(params);
                socket.setSoTimeout(5000);socket.startHandshake();
                byte[] content=LiveSafety.bytes(body.toString());
                String header="POST "+uri.getRawPath()+" HTTP/1.1\r\nHost: "+uri.getHost()+"\r\nContent-Type: application/json; charset=UTF-8\r\nAccept: application/json\r\nAccept-Encoding: identity\r\nx-goog-api-key: "+key+"\r\nContent-Length: "+content.length+"\r\nConnection: close\r\n\r\n";
                OutputStream out=socket.getOutputStream();out.write(header.getBytes(StandardCharsets.US_ASCII));out.write(content);out.flush();
                return read(socket.getInputStream());
                }
            }catch(IOException e){if(expired.get())throw new SocketTimeoutException("REQUEST_DEADLINE");throw e;}finally{deadline.cancel(false);}
        }
    }
    static String line(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();int previous=-1,c;while((c=in.read())!=-1){if(previous==13&&c==10){byte[] bytes=b.toByteArray();return new String(bytes,0,bytes.length-1,StandardCharsets.US_ASCII);}b.write(c);if(b.size()>8192)throw new IOException("HEADER_LIMIT");previous=c;}throw new EOFException("INCOMPLETE_HTTP");}
    static void copy(InputStream in,ByteArrayOutputStream out,int count)throws IOException{if(count<0||out.size()+count>LIMIT)throw new IOException("RESPONSE_LIMIT");byte[] buf=new byte[4096];while(count>0){int n=in.read(buf,0,Math.min(buf.length,count));if(n<0)throw new EOFException("INCOMPLETE_HTTP");out.write(buf,0,n);count-=n;}}
    public static LiveSafety.Response read(InputStream in)throws IOException{
        String first=line(in);if(!first.matches("HTTP/1\\.[01] [0-9]{3}(?: .*)?"))throw new IOException("HTTP_STATUS");int status=Integer.parseInt(first.substring(9,12));
        // Never read/retain raw provider error body, and never follow Location.
        if(status!=200)return new LiveSafety.Response(status,"");
        Map<String,String> headers=new HashMap<>();String l;int headerBytes=0;
        while(!(l=line(in)).isEmpty()){headerBytes+=l.length();if(headerBytes>16384)throw new IOException("HEADER_LIMIT");int colon=l.indexOf(':');if(colon<1)throw new IOException("HTTP_HEADER");String k=l.substring(0,colon).toLowerCase(Locale.ROOT);if(headers.put(k,l.substring(colon+1).trim())!=null)throw new IOException("DUPLICATE_HTTP_HEADER");}
        if(headers.containsKey("content-encoding")&&!headers.get("content-encoding").equalsIgnoreCase("identity"))throw new IOException("UNEXPECTED_ENCODING");
        ByteArrayOutputStream out=new ByteArrayOutputStream();String transfer=headers.get("transfer-encoding");
        if(transfer!=null){
            if(!"chunked".equalsIgnoreCase(transfer)||headers.containsKey("content-length"))throw new IOException("HTTP_FRAMING");
            while(true){String size=line(in).split(";",2)[0];int n;try{n=Integer.parseInt(size,16);}catch(Exception e){throw new IOException("CHUNK_SIZE");}if(n==0){int trailers=0;while(!line(in).isEmpty())if(++trailers>20)throw new IOException("TRAILERS_LIMIT");break;}copy(in,out,n);if(!line(in).isEmpty())throw new IOException("CHUNK_END");}
        }else if(headers.containsKey("content-length")){int n;try{n=Integer.parseInt(headers.get("content-length"));}catch(Exception e){throw new IOException("CONTENT_LENGTH");}copy(in,out,n);}
        else{byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>LIMIT)throw new IOException("RESPONSE_LIMIT");out.write(buf,0,n);}}
        return new LiveSafety.Response(status,new String(out.toByteArray(),StandardCharsets.UTF_8));
    }
}
