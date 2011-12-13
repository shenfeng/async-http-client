package me.shenfeng.http;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static me.shenfeng.Utils.getPort;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT_ENCODING;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.USER_AGENT;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpMethod.POST;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static org.jboss.netty.util.ThreadNameDeterminer.CURRENT;
import static org.jboss.netty.util.ThreadRenamingRunnable.setThreadNameDeterminer;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLEngine;

import me.shenfeng.PrefixThreadFactory;
import me.shenfeng.Utils;
import me.shenfeng.ssl.SslContextFactory;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient implements HttpClientConstant {

    static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private final ClientBootstrap mHttpBootstrap;
    private final ClientBootstrap mHttpsBootstrap;
    private volatile long mLastCheckTime = currentTimeMillis();
    private final HttpClientConfig mConf;
    private final Queue<HttpResponseFuture> mFutures = new ConcurrentLinkedQueue<HttpResponseFuture>();

    public HttpClient() {
        this(new HttpClientConfig());
    }

    public HttpClient(HttpClientConfig conf) {
        mConf = conf;
        setThreadNameDeterminer(CURRENT);
        ExecutorService boss = newCachedThreadPool(new PrefixThreadFactory(
                conf.bossNamePrefix));
        ExecutorService worker = newCachedThreadPool(new PrefixThreadFactory(
                conf.workerNamePrefix));
        NioClientSocketChannelFactory factory = new NioClientSocketChannelFactory(
                boss, worker, conf.workerThread);

        mHttpsBootstrap = new ClientBootstrap(factory);
        mHttpsBootstrap.setPipelineFactory(new HttpsClientPipelineFactory(
                mConf.maxLength, conf));

        mHttpBootstrap = new ClientBootstrap(factory);
        mHttpBootstrap.setPipelineFactory(new HttpClientPipelineFactory(
                mConf.maxLength, conf));

        conf(mHttpBootstrap);
        conf(mHttpsBootstrap);

    }

    private HttpRequest buildRequest(HttpMethod method, URI uri,
            Map<String, Object> headers, Map<String, Object> params,
            Proxy proxy) {

        String path = proxy.type() == Type.HTTP ? uri.toString() : Utils
                .getPath(uri);

        HttpRequest request = new DefaultHttpRequest(HTTP_1_1, method, path);

        request.setHeader(HOST, uri.getHost());
        request.setHeader(USER_AGENT, mConf.userAgent);
        request.setHeader(ACCEPT, "*/*");
        request.setHeader(ACCEPT_ENCODING, "gzip, deflate");

        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            request.setHeader(entry.getKey(), entry.getValue());
        }

        if (params != null) {
            StringBuilder sb = new StringBuilder(32);
            for (java.util.Map.Entry<String, Object> e : params.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                try {
                    sb.append(URLEncoder.encode(e.getKey(), "utf8"));
                    sb.append("=");
                    sb.append(URLEncoder.encode(e.getValue().toString(),
                            "utf8"));
                } catch (UnsupportedEncodingException ignore) {
                }
            }

            byte[] data = sb.toString().getBytes(UTF_8);
            request.setHeader(CONTENT_TYPE,
                    "application/x-www-form-urlencoded");
            request.setHeader(CONTENT_LENGTH, data.length);
            request.setContent(wrappedBuffer(data));
        }

        return request;

    }

    private void checkTimeoutIfNeeded() {
        if (currentTimeMillis() - mLastCheckTime > mConf.timerInterval) {
            // thread safe
            final Iterator<HttpResponseFuture> it = mFutures.iterator();
            while (it.hasNext()) {
                HttpResponseFuture r = it.next();
                if (r.isDone() || r.isTimeout()) {
                    it.remove();
                }
            }
            mLastCheckTime = currentTimeMillis();
        }
    }

    public void close() {
        mHttpBootstrap.releaseExternalResources();
        mHttpsBootstrap.releaseExternalResources();
    }

    private void conf(ClientBootstrap bootstrap) {
        bootstrap.setOption("connectTimeoutMillis",
                mConf.connectionTimeOutInMs);
        bootstrap.setOption("receiveBufferSize", mConf.receiveBuffer);
        bootstrap.setOption("sendBufferSize", mConf.sendBuffer);
        bootstrap.setOption("reuseAddress", true);
    }

    public HttpResponseFuture execGet(final URI uri,
            final Map<String, Object> headers) {
        return execGet(uri, headers, Proxy.NO_PROXY);
    }

    public HttpResponseFuture execGet(URI uri, Map<String, Object> headers,
            Proxy proxy) {
        HttpRequest request = buildRequest(GET, uri, headers, null, proxy);
        return execRequest(request, uri, proxy);
    }

    public HttpResponseFuture execPost(URI uri, Map<String, Object> headers,
            Map<String, Object> params) {
        HttpRequest request = buildRequest(POST, uri, headers, params,
                Proxy.NO_PROXY);
        return execRequest(request, uri, Proxy.NO_PROXY);
    }

    public HttpResponseFuture execPost(URI uri, Map<String, Object> headers,
            Map<String, Object> params, Proxy proxy) {
        HttpRequest request = buildRequest(POST, uri, headers, params, proxy);
        return execRequest(request, uri, proxy);
    }

    private HttpResponseFuture execRequest(HttpRequest request, URI uri,
            Proxy proxy) {
        checkTimeoutIfNeeded();
        final HttpResponseFuture future = new HttpResponseFuture(
                mConf.requestTimeoutInMs, request);
        boolean ssl = "https".equals(uri.getScheme());

        try {
            InetSocketAddress addr = proxy.type() == Type.DIRECT ? new InetSocketAddress(
                    InetAddress.getByName(uri.getHost()), getPort(uri))
                    : (InetSocketAddress) proxy.address();

            ClientBootstrap bootstrap = ssl ? mHttpsBootstrap
                    : mHttpBootstrap;
            ChannelFuture cf = bootstrap.connect(addr);
            Channel ch = cf.getChannel();
            future.setChannel(ch);
            ch.getPipeline().getContext(Decoder.class).setAttachment(future);
            cf.addListener(new ConnectionListener(future, uri,
                    proxy.type() == Type.SOCKS));
            mFutures.add(future);
        } catch (UnknownHostException e) {
            future.done(UNKOWN_HOST);
        }
        return future;
    }
}

class HttpClientPipelineFactory implements ChannelPipelineFactory {

    final int maxLength;
    final HttpClientConfig conf;

    public HttpClientPipelineFactory(int maxLength, HttpClientConfig conf) {
        this.maxLength = maxLength;
        this.conf = conf;
    }

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("decoder", new Decoder(conf));
        pipeline.addLast("encoder", new HttpRequestEncoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(maxLength));
        pipeline.addLast("handler", new ResponseHandler());
        return pipeline;
    }
}

class HttpsClientPipelineFactory implements ChannelPipelineFactory {

    final int maxLength;
    final HttpClientConfig conf;

    public HttpsClientPipelineFactory(int maxLength, HttpClientConfig conf) {
        this.maxLength = maxLength;
        this.conf = conf;
    }

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();
        SSLEngine engine = SslContextFactory.getClientContext()
                .createSSLEngine();
        engine.setUseClientMode(true);
        pipeline.addLast("ssl", new SslHandler(engine));

        pipeline.addLast("decoder", new Decoder(conf));
        pipeline.addLast("encoder", new HttpRequestEncoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(maxLength));
        pipeline.addLast("handler", new ResponseHandler());
        return pipeline;
    }

}
