package me.shenfeng.http;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static me.shenfeng.http.HttpClientConstant.CONN_ERROR;
import static me.shenfeng.http.HttpClientConstant.CONN_RESET;
import static me.shenfeng.http.HttpClientConstant.CONN_TIMEOUT;
import static me.shenfeng.http.HttpClientConstant.TIMEOUT;
import static me.shenfeng.http.HttpClientConstant.TOO_LARGE;
import static me.shenfeng.http.HttpClientConstant.UNKOWN_ERROR;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import me.shenfeng.AbstractResponseFuture;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class HttpResponseFuture extends AbstractResponseFuture<HttpResponse> {

    private volatile Object mAttachment;
    private volatile Channel mChannel;
    private volatile long mTouchTime;
    private final int mTimeout;
    final URI uri;// package private

    public HttpResponseFuture(int timeout, URI uri) {
        mTimeout = timeout;
        this.uri = uri;
        mTouchTime = currentTimeMillis();
    }

    public boolean isTimeout() {
        if (mTimeout + mTouchTime - currentTimeMillis() < 0) {
            return done(TIMEOUT);
        }
        return false;
    }

    public boolean done(HttpResponse result) {
        if (mChannel != null)
            mChannel.close();
        return super.done(result);
    }

    public void setAttachment(Object att) {
        mAttachment = att;
    }

    public Object getAttachment() {
        return mAttachment;
    }

    public boolean abort(Throwable t) {
        HttpResponse resp = UNKOWN_ERROR;
        String msg = t.getMessage();
        if (t instanceof TooLongFrameException) {
            resp = TOO_LARGE;
        } else if (t instanceof ConnectException) {
            if (msg != null && msg.indexOf("timed out") != -1)
                resp = CONN_TIMEOUT;
            else
                resp = CONN_ERROR;
        } else if (t instanceof IOException && msg != null
                && msg.indexOf("reset") != -1) {
            resp = CONN_RESET;
        }
        return done(resp);
    }

    public HttpResponse get() throws InterruptedException, ExecutionException {
        try {
            return get(mTimeout, MILLISECONDS);
        } catch (TimeoutException e) {
            // ignore
            return TIMEOUT;
        }
    }

    public HttpResponse get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long time = unit.toMillis(timeout);
        long wait = time + mTouchTime - currentTimeMillis();
        while (mLatch.getCount() > 0 && wait > 0) {
            mLatch.await(wait, MILLISECONDS);
            wait = time + mTouchTime - currentTimeMillis();
        }
        done(TIMEOUT);
        return mResult.get();
    }

    public void setChannel(Channel ch) {
        mChannel = ch;
    }

    public void touch() {
        mTouchTime = currentTimeMillis();
    }
}
