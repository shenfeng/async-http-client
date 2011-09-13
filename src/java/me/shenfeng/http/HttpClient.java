package me.shenfeng.http;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static me.shenfeng.dns.DnsResponseFuture.RESOLVE_FAIL;
import static me.shenfeng.http.Utils.getPath;
import static me.shenfeng.http.Utils.getPort;
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

import me.shenfeng.ListenableFuture;
import me.shenfeng.ListenableFuture.Listener;
import me.shenfeng.PrefixThreadFactory;
import me.shenfeng.dns.DnsClient;

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
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient {

    public final static HttpResponse ABORT = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(471, "client abort"));
    public final static HttpResponse TOO_LARGE = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(513, "body too large"));
    public final static HttpResponse TIMEOUT = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(520, "server timeout"));
    public final static HttpResponse CONNECTION_ERROR = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(170, "connecton error"));
    public final static HttpResponse CONNECTION_TIMEOUT = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(172, "connecton timeout"));
    public final static HttpResponse CONNECTION_RESET = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(175, "connecton reset"));
    public final static HttpResponse UNKOWN_HOST = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(171, "unknow host"));
    public final static HttpResponse UNKOWN_ERROR = new DefaultHttpResponse(
            HTTP_1_1, new HttpResponseStatus(180, "unknow error"));

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
        mBootstrap.setOption("reuseAddress", true);
        mAllChannels = new DefaultChannelGroup("client");
    }

    public void close() {
        mAllChannels.close().awaitUninterruptibly();
        mBootstrap.releaseExternalResources();
    }

    public HttpResponseFuture execGet(final URI uri,
            final Map<String, Object> headers) {
        final String host = uri.getHost();
        final HttpResponseFuture future = new HttpResponseFuture(
                mConf.requestTimeoutInMs, uri);

        mDns.resolve(host).addistener(new Listener<String>() {
            public void run(ListenableFuture<String> l, String result) {
                if (RESOLVE_FAIL.equals(result)) {
                    future.done(UNKOWN_HOST);
                } else {
                    final HttpRequest request = new DefaultHttpRequest(
                            HTTP_1_1, GET, getPath(uri));
                    request.setHeader(HOST, host);
                    request.setHeader(USER_AGENT, mConf.userAgent);
                    request.setHeader(ACCEPT, "*/*");
                    request.setHeader(ACCEPT_ENCODING, "gzip, deflate");
                    request.setHeader(CONNECTION, CLOSE);

                    for (Map.Entry<String, Object> entry : headers.entrySet()) {
                        request.addHeader(entry.getKey(), entry.getValue());
                    }
                    ChannelFuture cf = mBootstrap
                            .connect(new InetSocketAddress(result,
                                    getPort(uri)));
                    Channel ch = cf.getChannel();
                    future.setChannel(ch);
                    mAllChannels.add(ch);
                    future.touch();
                    ch.getPipeline().getContext(Decoder.class)
                            .setAttachment(future);
                    cf.addListener(new ConnectionListener(request, future,
                            uri));
                }
            }
        });
        return future;
    }
}

class HttpClientPipelineFactory implements ChannelPipelineFactory {

    public ChannelPipeline getPipeline() throws Exception {

        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("decoder", new Decoder());
        pipeline.addLast("encoder", new HttpRequestEncoder());
        pipeline.addLast("inflater", new HttpContentDecompressor());
        pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast("handler", new ResponseHandler());
        return pipeline;
    }
}

class Decoder extends HttpResponseDecoder {
    @Override
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

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        HttpResponseFuture future = (HttpResponseFuture) ctx.getAttachment();

        HttpResponse response = (HttpResponse) e.getMessage();
        ctx.getChannel().close();
        future.done(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        ctx.getChannel().close();
        Throwable cause = e.getCause();
        HttpResponseFuture future = (HttpResponseFuture) ctx.getAttachment();
        if (future != null) {
            logger.trace(future.mUri.toString(), cause);
            future.abort(cause);
        } else {
            logger.trace("exceptionCaught", e);
        }
    }
}
