package me.shenfeng.http;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static me.shenfeng.Utils.getPort;
import static me.shenfeng.dns.DnsClientConstant.DNS_TIMEOUT;
import static me.shenfeng.dns.DnsClientConstant.DNS_UNKOWN_HOST;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.util.ThreadNameDeterminer.CURRENT;
import static org.jboss.netty.util.ThreadRenamingRunnable.setThreadNameDeterminer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import me.shenfeng.PrefixThreadFactory;
import me.shenfeng.dns.DnsClient;
import me.shenfeng.dns.DnsClientConfig;
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
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
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
    private volatile boolean mRunning = true;
    private final DnsClient mDns;
    private Thread mTimeoutThread;
    private final List<HttpResponseFuture> mFutures = new LinkedList<HttpResponseFuture>();

    private void startTimeoutThread() {
        mTimeoutThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (mRunning) {
                        Thread.sleep(mConf.timerInterval);
                        synchronized (mFutures) {
                            final Iterator<HttpResponseFuture> it = mFutures.iterator();
                            while (it.hasNext()) {
                                HttpResponseFuture r = it.next();
                                if (r.isDone() || r.isTimeout()) {
                                    it.remove();
                                }
                            }
                        }
                    }
                } catch (InterruptedException ignore) {
                }
            }
        }, TIMER_NAME);

        mTimeoutThread.start();
    }

    public HttpClient() {
        this(new HttpClientConfig());
    }

    public HttpClient(HttpClientConfig conf) {
        mDns = new DnsClient(new DnsClientConfig(conf.dnsTimeout,
                conf.timerInterval));
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
        startTimeoutThread();
    }

    public void close() {
        mRunning = false;
        mTimeoutThread.interrupt();
        mAllChannels.close().awaitUninterruptibly();
        mBootstrap.releaseExternalResources();
        mDns.close();
    }

    private void connect(URI uri, String ip, HttpResponseFuture futrue,
            Map<String, Object> headers) {
        ChannelFuture cf = mBootstrap.connect(new InetSocketAddress(ip,
                getPort(uri)));
        Channel ch = cf.getChannel();
        futrue.setChannel(ch);
        mAllChannels.add(ch);
        ch.getPipeline().getContext(Decoder.class).setAttachment(futrue);
        cf.addListener(new ConnectionListener(mConf, futrue, headers));
    }

    public HttpResponseFuture execGet(final URI uri,
            final Map<String, Object> headers) {
        final String host = uri.getHost();
        final HttpResponseFuture resp = new HttpResponseFuture(
                mConf.requestTimeoutInMs, uri);
        final DnsResponseFuture dns = mDns.resolve(host);
        final AtomicInteger retry = new AtomicInteger(mConf.dnsRetryLimit + 1);
        final Runnable listener = new Runnable() {
            public void run() {
                String ip = null;
                try {
                    ip = dns.get();
                } catch (Exception ignore) {
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
                    connect(uri, ip, resp, headers);
                    resp.touch();
                }
            }
        };
        dns.addListener(listener);

        synchronized (mFutures) {
            mFutures.add(resp);
        }
        return resp;
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
            logger.trace(future.uri.toString(), cause);
            future.abort(cause);
        } else {
            logger.trace(cause.getMessage(), cause);
        }
    }
}
