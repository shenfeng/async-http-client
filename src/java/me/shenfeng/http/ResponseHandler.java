package me.shenfeng.http;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseHandler extends SimpleChannelUpstreamHandler {

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