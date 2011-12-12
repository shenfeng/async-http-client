package me.shenfeng.http;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

import java.net.URI;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionListener implements ChannelFutureListener {

    private static Logger logger = LoggerFactory
            .getLogger(ConnectionListener.class);
    private boolean mIsSocks;
    private HttpResponseFuture mFuture;
    private URI mUri;

    public ConnectionListener(HttpResponseFuture future, URI uri,
            boolean isSocks) {
        mFuture = future;
        mIsSocks = isSocks;
        mUri = uri;
    }

    public void operationComplete(ChannelFuture f) throws Exception {
        Channel channel = f.getChannel();
        if (f.isSuccess()) {
            channel.getPipeline().getContext(ResponseHandler.class)
                    .setAttachment(mFuture);
            if (mIsSocks) {
                channel.getPipeline().addFirst("socks",
                        new SocksHandler(mFuture, mUri));
                // write version and authen info
                channel.write(wrappedBuffer(SocksHandler.VERSION_AUTH));
            } else {
                channel.write(mFuture.request).addListener(
                        new ChannelFutureListener() {
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
            logger.trace(mFuture.request.toString(), cause);
        }
    }
}
