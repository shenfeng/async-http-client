package me.shenfeng.http;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static me.shenfeng.Utils.getPath;
import static me.shenfeng.Utils.getPort;
import static me.shenfeng.dns.DnsClientConstant.*;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT_ENCODING;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.USER_AGENT;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.CLOSE;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.jboss.netty.util.ThreadNameDeterminer.CURRENT;
import static org.jboss.netty.util.ThreadRenamingRunnable.setThreadNameDeterminer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import me.shenfeng.PrefixThreadFactory;
import me.shenfeng.dns.DnsClient;
import me.shenfeng.dns.DnsResponseFuture;

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
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient implements HttpClientConstant {

    static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private final ClientBootstrap mBootstrap;
    private final ChannelGroup mAllChannels;
    private final HttpClientConfig mConf;
    private final ExecutorService mWorker;
    private final ExecutorService mBoss;
    private final DnsClient mDns;

    public HttpClient() {
        this(new HttpClientConfig());
    }

    public HttpClient(HttpClientConfig conf) {
        mDns = new DnsClient();
        mConf = conf;
        setThreadNameDeterminer(CURRENT);
        mBoss = newCachedThreadPool(new PrefixThreadFactory(
                conf.bossNamePrefix));
        mWorker = newCachedThreadPool(new PrefixThreadFactory(
                conf.workerNamePrefix));
        NioClientSocketChannelFactory factory = new NioClientSocketChannelFactory(
                mBoss, mWorker, conf.workerThread);

        mBootstrap = new ClientBootstrap(factory);
        mBootstrap.setPipelineFactory(new HttpClientPipelineFactory());
        mBootstrap.setOption("connectTimeoutMillis",
                conf.connectionTimeOutInMs);
        mBootstrap.setOption("receiveBufferSize", conf.receiveBuffer);
        mBootstrap.setOption("sendBufferSize", conf.sendBuffer);
        mBootstrap.setOption("reuseAddress", true);
        mAllChannels = new DefaultChannelGroup("client");
    }

    public void close() {
        mAllChannels.close().awaitUninterruptibly();
        mDns.close();
        mBootstrap.releaseExternalResources();
    }

    private void sendRequest(URI uri, String ip, HttpResponseFuture resp,
            Map<String, Object> headers) {
        final HttpRequest request = new DefaultHttpRequest(HTTP_1_1, GET,
                getPath(uri));
        request.setHeader(HOST, uri.getHost());
        request.setHeader(USER_AGENT, mConf.userAgent);
        request.setHeader(ACCEPT, "*/*");
        request.setHeader(ACCEPT_ENCODING, "gzip, deflate");
        request.setHeader(CONNECTION, CLOSE);

        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
        ChannelFuture cf = mBootstrap.connect(new InetSocketAddress(ip,
                getPort(uri)));
        Channel ch = cf.getChannel();
        resp.setChannel(ch);
        mAllChannels.add(ch);
        ch.getPipeline().getContext(Decoder.class).setAttachment(resp);
        cf.addListener(new ConnectionListener(request, resp, uri));
    }

    public HttpResponseFuture execGet(final URI uri,
            final Map<String, Object> headers) {
        final String host = uri.getHost();
        final HttpResponseFuture resp = new HttpResponseFuture(
                mConf.requestTimeoutInMs, uri);
        final DnsResponseFuture dns = mDns.resolve(host);
        final AtomicInteger retry = new AtomicInteger(3); // retry 2 times
        final Runnable listener = new Runnable() {
            public void run() {
                String ip = null;
                try {
                    ip = dns.get(); // can not fail
                } catch (Exception e) {
                }

                if (DNS_UNKOWN_HOST.equals(ip)) {
                    resp.done(UNKOWN_HOST);
                } else if (DNS_TIMEOUT.equals(ip)) {
                    if (retry.decrementAndGet() <= 0) {
                        resp.done(UNKOWN_HOST);
                    } else {
                        logger.trace("resolve {} timeout, retry {}", host,
                                retry.get());
                        resp.touch();
                        mDns.resolve(host).addListener(this);
                    }
                } else {
                    sendRequest(uri, ip, resp, headers);
                    resp.touch();
                }
            }
        };
        dns.addListener(listener);
        return resp;
    }
}

class HttpClientPipelineFactory implements ChannelPipelineFactory {

    private final static HttpContentDecompressor inflater = new HttpContentDecompressor();
    private final static ResponseHandler handler = new ResponseHandler();

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("decoder", new Decoder());
        pipeline.addLast("encoder", new HttpRequestEncoder());
        pipeline.addLast("inflater", inflater);
        pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast("handler", handler);
        return pipeline;
    }
}

class Decoder extends HttpResponseDecoder {
    protected Object decode(ChannelHandlerContext ctx, Channel channel,
            ChannelBuffer buffer, State state) throws Exception {
        HttpResponseFuture future = (HttpResponseFuture) ctx.getAttachment();
        if (future != null) {
            future.touch();
        }
        return super.decode(ctx, channel, buffer, state);
    }
}

class ResponseHandler extends SimpleChannelUpstreamHandler {

    private static Logger logger = LoggerFactory
            .getLogger(ResponseHandler.class);

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        HttpResponseFuture future = (HttpResponseFuture) ctx.getAttachment();
        HttpResponse response = (HttpResponse) e.getMessage();
        ctx.getChannel().close();
        future.done(response);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        ctx.getChannel().close();
        Throwable cause = e.getCause();
        HttpResponseFuture future = (HttpResponseFuture) ctx.getAttachment();
        if (future != null) {
            logger.trace(future.mUri.toString(), cause);
            future.abort(cause);
        } else {
            logger.trace(cause.getMessage(), cause);
        }
    }
}
