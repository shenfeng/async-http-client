package me.shenfeng.http;

import static me.shenfeng.Utils.getPath;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT_ENCODING;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.USER_AGENT;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

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
    private final HttpResponseFuture mFuture;
    private final Map<String, Object> mHeaders;

    public ConnectionListener(HttpClientConfig conf,
            HttpResponseFuture future, Map<String, Object> header) {
        mHeaders = header;
        mConf = conf;
        mFuture = future;
    }

    public void operationComplete(ChannelFuture f) throws Exception {
        Channel channel = f.getChannel();
        if (f.isSuccess()) {
            channel.getPipeline().getContext(ResponseHandler.class)
                    .setAttachment(mFuture);

            final HttpRequest request = new DefaultHttpRequest(HTTP_1_1, GET,
                    getPath(mFuture.uri));
            request.setHeader(HOST, mFuture.uri.getHost());
            request.setHeader(USER_AGENT, mConf.userAgent);
            request.setHeader(ACCEPT, "*/*");
            request.setHeader(ACCEPT_ENCODING, "gzip, deflate");
            request.setHeader(CONNECTION, CLOSE);

            for (Map.Entry<String, Object> entry : mHeaders.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }

            channel.write(request).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future)
                        throws Exception {
                    mFuture.touch();
                }
            });

        } else {
            channel.close();
            Throwable cause = f.getCause();
            mFuture.abort(cause);
            logger.trace(mFuture.uri.toString(), cause);
        }
    }
}
