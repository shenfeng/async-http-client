package me.shenfeng.http;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static me.shenfeng.Utils.getPort;
import static me.shenfeng.http.HttpClientConstant.TOO_LARGE;
import static me.shenfeng.http.HttpClientConstant.UNKNOWN_CONTENT;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.handler.codec.http.HttpHeaders.getContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.util.ThreadNameDeterminer.CURRENT;
import static org.jboss.netty.util.ThreadRenamingRunnable.setThreadNameDeterminer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import javax.management.RuntimeErrorException;
import javax.net.ssl.SSLEngine;

import me.shenfeng.PrefixThreadFactory;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Decoder extends HttpResponseDecoder {
    final HttpClientConfig conf;

    protected Object decode(ChannelHandlerContext ctx, Channel channel,
            ChannelBuffer buffer, State state) throws Exception {
        Object o = super.decode(ctx, channel, buffer, state);

        HttpResponseFuture future = (HttpResponseFuture) ctx.getAttachment();
        if (o instanceof HttpMessage && future != null) {
            future.touch();
            HttpMessage msg = (HttpMessage) o;
            String type = msg.getHeader(CONTENT_TYPE);
            if (getContentLength(msg) > conf.maxLength) {
                future.done(TOO_LARGE);
            } else if (type != null && conf.acceptedContentTypes != null) {
                boolean accept = false;
                for (String t : conf.acceptedContentTypes) {
                    if (type.contains(t)) {
                        accept = true;
                        break;
                    }
                }
                if (!accept) {
                    future.done(UNKNOWN_CONTENT);
                }
            }
        }
        return o;
    }

    public Decoder(HttpClientConfig conf) {
        // 128k, less chunks, and allow other logic to abort early when
        // resource is large, but unwanted
        super(4096, 8192, 128 * 1024);
        this.conf = conf;
    }
}

public class HttpClient implements HttpClientConstant {

    static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private final ChannelGroup mAllChannels;
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

        mAllChannels = new DefaultChannelGroup("client");
    }

    private void conf(ClientBootstrap bootstrap) {
        bootstrap.setOption("connectTimeoutMillis",
                mConf.connectionTimeOutInMs);
        bootstrap.setOption("receiveBufferSize", mConf.receiveBuffer);
        bootstrap.setOption("sendBufferSize", mConf.sendBuffer);
        bootstrap.setOption("reuseAddress", true);
    }

    public void close() {
        mAllChannels.close().awaitUninterruptibly();
        mHttpBootstrap.releaseExternalResources();
    }

    private void connect(InetSocketAddress addr, HttpResponseFuture futrue,
            Map<String, Object> headers, Proxy proxy, boolean ssl) {
        ClientBootstrap bootstrap = ssl ? mHttpsBootstrap : mHttpBootstrap;
        ChannelFuture cf = bootstrap.connect(addr);
        Channel ch = cf.getChannel();
        futrue.setChannel(ch);
        mAllChannels.add(ch);
        ch.getPipeline().getContext(Decoder.class).setAttachment(futrue);
        cf.addListener(new ConnectionListener(mConf, futrue, headers, proxy));
    }

    public HttpResponseFuture execGet(final URI uri,
            final Map<String, Object> headers) {
        return execGet(uri, headers, Proxy.NO_PROXY);
    }

    public HttpResponseFuture execGet(final URI uri,
            final Map<String, Object> headers, Proxy proxy) {

        checkTimeoutIfNeeded();

        final HttpResponseFuture resp = new HttpResponseFuture(
                mConf.requestTimeoutInMs, uri);
        switch (proxy.type()) {
        case DIRECT:
            try {
                boolean ssl = "https".equals(uri.getScheme());
                InetSocketAddress addr = new InetSocketAddress(
                        InetAddress.getByName(uri.getHost()), getPort(uri));
                connect(addr, resp, headers, proxy, ssl);
            } catch (UnknownHostException e) {
                resp.done(UNKOWN_HOST);
            }
            break;
        case HTTP:
            connect((InetSocketAddress) proxy.address(), resp, headers,
                    proxy, false);
            break;
        default:
            throw new RuntimeErrorException(null,
                    "Only http proxy is supported currently");

        }
        mFutures.add(resp);
        return resp;
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

class ResponseHandler extends SimpleChannelUpstreamHandler {

    private static Logger logger = LoggerFactory
            .getLogger(ResponseHandler.class);

    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        ctx.getChannel().close();
        Throwable cause = e.getCause();
        HttpResponseFuture future = (HttpResponseFuture) ctx.getAttachment();
        if (future != null) {
            logger.trace(future.uri.toString(), cause);
            future.abort(cause);
        } else {
            logger.trace(cause.getMessage(), cause);
        }
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        HttpResponseFuture future = (HttpResponseFuture) ctx.getAttachment();
        HttpResponse response = (HttpResponse) e.getMessage();
        ctx.getChannel().close();
        future.done(response);
    }
}
