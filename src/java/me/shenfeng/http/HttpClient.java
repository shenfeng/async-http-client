package me.shenfeng.http;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static me.shenfeng.Utils.getPort;
import static org.jboss.netty.channel.Channels.pipeline;
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
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class HttpClient implements HttpClientConstant {

    static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private final ChannelGroup mAllChannels;
    private final ClientBootstrap mBootstrap;
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

        mBootstrap = new ClientBootstrap(factory);
        mBootstrap.setPipelineFactory(new HttpClientPipelineFactory(
                mConf.maxLength));
        mBootstrap.setOption("connectTimeoutMillis",
                conf.connectionTimeOutInMs);
        mBootstrap.setOption("receiveBufferSize", conf.receiveBuffer);
        mBootstrap.setOption("sendBufferSize", conf.sendBuffer);
        mBootstrap.setOption("reuseAddress", true);
        mAllChannels = new DefaultChannelGroup("client");
    }

    public void close() {
        mAllChannels.close().awaitUninterruptibly();
        mBootstrap.releaseExternalResources();
    }

    private void connect(InetSocketAddress addr, HttpResponseFuture futrue,
            Map<String, Object> headers, Proxy proxy) {
        ChannelFuture cf = mBootstrap.connect(addr);
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
                InetSocketAddress addr = new InetSocketAddress(
                        InetAddress.getByName(uri.getHost()), getPort(uri));
                connect(addr, resp, headers, proxy);
            } catch (UnknownHostException e) {
                resp.done(UNKOWN_HOST);
            }
            break;
        case HTTP:
            connect((InetSocketAddress) proxy.address(), resp, headers, proxy);
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

    public HttpClientPipelineFactory(int maxLength) {
        this.maxLength = maxLength;
    }

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("decoder", new Decoder());
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
