package me.shenfeng.dns;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import me.shenfeng.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsResponseFuture implements ListenableFuture<String> {

    public static final String RESOLVE_FAIL = "fail!";
    private final static Logger logger = LoggerFactory
            .getLogger(DnsResponseFuture.class);

    private final String mHost;
    private final AtomicReference<String> mIp = new AtomicReference<String>();
    private final CountDownLatch mLatch = new CountDownLatch(1);
    private final ArrayList<Listener<String>> mListeners = new ArrayList<Listener<String>>(
            2);
    private final long mStartTime;

    public DnsResponseFuture(String host) {
        mHost = host;
        mStartTime = currentTimeMillis();
    }

    public String getHost() {
        return mHost;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {

        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return mIp.get() != null;
    }

    public String get() throws InterruptedException, ExecutionException {
        try {
            return get(4000, MILLISECONDS);
        } catch (TimeoutException e) {
            done(RESOLVE_FAIL);
            return RESOLVE_FAIL;
        }
    }

    public String get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        timeout = unit.toMillis(timeout);
        long wait = timeout + mStartTime - currentTimeMillis();
        while (mLatch.getCount() > 0 && wait > 0) {
            mLatch.await(wait, MILLISECONDS);
            wait = timeout + mStartTime - currentTimeMillis();
        }
        done(RESOLVE_FAIL);
        return mIp.get();
    }

    public void touch() {
        // blank
    }

    public void abort(Throwable t) {
        done(RESOLVE_FAIL);
    }

    public ListenableFuture<String> addistener(Listener<String> listener) {
        if (mLatch.getCount() == 0) {
            listener.run(this, mIp.get());
        } else {
            mListeners.add(listener);
        }
        return this;
    }

    public void done(String result) {
        if (mIp.compareAndSet(null, result)) {
            mLatch.countDown();
            for (Listener<String> listener : mListeners) {
                try {
                    listener.run(this, result);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
