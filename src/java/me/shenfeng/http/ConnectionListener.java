package me.shenfeng.http;

import java.net.URI;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionListener implements ChannelFutureListener {

    private static Logger logger = LoggerFactory
            .getLogger(ConnectionListener.class);

    private final HttpRequest mRequest;
    private final HttpResponseFuture mFuture;
    private final URI mUri;

    public ConnectionListener(HttpRequest request, HttpResponseFuture future,
            URI uri) {
        mRequest = request;
        mFuture = future;
        mUri = uri;
    }

    public void operationComplete(ChannelFuture f) throws Exception {
        Channel channel = f.getChannel();
        if (f.isSuccess()) {
            channel.getPipeline().getContext(ResponseHandler.class)
                    .setAttachment(mFuture);
            channel.write(mRequest).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future)
                        throws Exception {
                    mFuture.touch();
                }
            });

        } else {
            channel.close();
            Throwable cause = f.getCause();
            mFuture.abort(cause);
            logger.trace(mUri.toString(), cause);
        }
    }
}
