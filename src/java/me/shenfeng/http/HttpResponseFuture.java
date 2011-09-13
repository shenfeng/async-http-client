package me.shenfeng.http;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static me.shenfeng.http.HttpClient.CONNECTION_ERROR;
import static me.shenfeng.http.HttpClient.CONNECTION_RESET;
import static me.shenfeng.http.HttpClient.CONNECTION_TIMEOUT;
import static me.shenfeng.http.HttpClient.TIMEOUT;
import static me.shenfeng.http.HttpClient.TOO_LARGE;
import static me.shenfeng.http.HttpClient.UNKOWN_ERROR;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import me.shenfeng.ListenableFuture;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponseFuture implements ListenableFuture<HttpResponse> {
    private final static Logger logger = LoggerFactory
            .getLogger(HttpResponseFuture.class);

    private final CountDownLatch mLatch = new CountDownLatch(1);
    private final AtomicReference<HttpResponse> mResponse = new AtomicReference<HttpResponse>();
    private final ArrayList<Listener<HttpResponse>> mListeners = new ArrayList<Listener<HttpResponse>>(
            1);
    private Object mAttachment;
    private Channel mChannel;
    private long mTouchTime;
    private final int mTimeout;
    final URI mUri;// package private

    public HttpResponseFuture(int timeout, URI uri) {
        mTimeout = timeout;
        mUri = uri;
        mTouchTime = currentTimeMillis();
    }

    public void checkTimeout(Runnable pre) {
        if (mTimeout + mTouchTime - currentTimeMillis() < 0) {
            if (pre != null)
                pre.run();
            done(TIMEOUT);
        }
    }

    public void setAttachment(Object att) {
        mAttachment = att;
    }

    public Object getAttachment() {
        return mAttachment;
    }

    public void abort(Throwable t) {
        HttpResponse resp = UNKOWN_ERROR;
        if (t instanceof TooLongFrameException) {
            resp = TOO_LARGE;
        } else if (t instanceof ConnectException) {
            if (t.getMessage().indexOf("timed out") != -1)
                resp = CONNECTION_TIMEOUT;
            else
                resp = CONNECTION_ERROR;
        } else if (t instanceof IOException
                && t.getMessage().indexOf("reset") != -1) {
            resp = CONNECTION_RESET;
        }
        done(resp);
    }

    @Override
    public ListenableFuture<HttpResponse> addistener(
            Listener<HttpResponse> listener) {
        if (mLatch.getCount() == 0) {
            listener.run(this, mResponse.get());
        } else {
            mListeners.add(listener);
        }
        return this;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public void done(HttpResponse response) {
        if (mResponse.compareAndSet(null, response)) {
            mLatch.countDown();
            if (mChannel != null)
                mChannel.close();// in all condition, it's need to be closed
            for (Listener<HttpResponse> listener : mListeners) {
                try {
                    listener.run(this, response);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
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
            wait = time + mTouchTime - System.currentTimeMillis();
        }
        done(TIMEOUT);
        return mResponse.get();
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return mResponse.get() == null;
    }

    public void setChannel(Channel ch) {
        mChannel = ch;
    }

    public void touch() {
        mTouchTime = currentTimeMillis();
    }
}
