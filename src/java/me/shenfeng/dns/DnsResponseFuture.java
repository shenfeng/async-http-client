package me.shenfeng.dns;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static me.shenfeng.dns.DnsClientConstant.DNS_TIMEOUT;
import static me.shenfeng.dns.DnsClientConstant.DNS_UNKOWN_HOST;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import me.shenfeng.AbstractResponseFuture;

public class DnsResponseFuture extends AbstractResponseFuture<String> {

    private final long mStartTime;
    private final int mTimeout;

    public DnsResponseFuture(int timeout) {
        mTimeout = timeout;
        mStartTime = currentTimeMillis();
    }

    public String get() throws InterruptedException, ExecutionException {
        try {
            return get(mTimeout, MILLISECONDS);
        } catch (TimeoutException e) {
            done(DNS_TIMEOUT);
            return DNS_TIMEOUT;
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
        done(DNS_TIMEOUT);
        return mResult.get();
    }

    public void touch() {
        // blank
    }

    public boolean abort(Throwable t) {
        return done(DNS_UNKOWN_HOST);
    }

    public boolean isTimeout() {
        if (mTimeout + mStartTime - currentTimeMillis() < 0) {
            return done(DNS_TIMEOUT);
        }
        return false;
    }
}
