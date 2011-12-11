package me.shenfeng.http;

import static me.shenfeng.Utils.getPath;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT_ENCODING;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.USER_AGENT;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionListener implements ChannelFutureListener {

    private static Logger logger = LoggerFactory
            .getLogger(ConnectionListener.class);
    private final HttpClientConfig mConf;
    private final Proxy mProxy;
    private final HttpResponseFuture mFuture;
    private final Map<String, Object> mHeaders;

    public ConnectionListener(HttpClientConfig conf,
            HttpResponseFuture future, Map<String, Object> header, Proxy proxy) {
        mHeaders = header;
        mConf = conf;
        mFuture = future;
        mProxy = proxy;
    }

    public static ChannelFuture sendRequest(Channel channel, String host,
            String path, String userAgent, Map<String, Object> headers) {
        final HttpRequest request = new DefaultHttpRequest(HTTP_1_1, GET,
                path);
        request.setHeader(HOST, host);
        request.setHeader(USER_AGENT, userAgent);
        request.setHeader(ACCEPT, "*/*");
        request.setHeader(ACCEPT_ENCODING, "gzip, deflate");
        request.setHeader(CONNECTION, "close");

        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            request.setHeader(entry.getKey(), entry.getValue());
        }

        return channel.write(request);
    }

    public void operationComplete(ChannelFuture f) throws Exception {
        Channel channel = f.getChannel();
        if (f.isSuccess()) {
            channel.getPipeline().getContext(ResponseHandler.class)
                    .setAttachment(mFuture);
            URI uri = mFuture.uri;
            String userAgent = mConf.userAgent;
            if (mProxy.type() == Type.SOCKS) {
                channel.getPipeline().addFirst("socks",
                        new SocksHandler(uri, mHeaders, userAgent, mFuture));
                // write version and authen info
                channel.write(wrappedBuffer(SocksHandler.VERSION_AUTH));
            } else {
                String path = mProxy.type() == Type.HTTP ? uri.toString()
                        : getPath(uri);
                sendRequest(channel, uri.getHost(), path, userAgent, mHeaders)
                        .addListener(new ChannelFutureListener() {
                            public void operationComplete(ChannelFuture future)
                                    throws Exception {
                                mFuture.touch();
                            }
                        });
            }

        } else {
            channel.close();
            Throwable cause = f.getCause();
            mFuture.abort(cause);
            logger.trace(mFuture.uri.toString(), cause);
        }
    }
}
