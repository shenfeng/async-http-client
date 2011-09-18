package me.shenfeng;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractResponseFuture<V> implements ResponseFuture<V> {

    final static Logger logger = LoggerFactory
            .getLogger(AbstractResponseFuture.class);

    protected Runnable mFirstListener;
    protected final CountDownLatch mLatch = new CountDownLatch(1);
    protected final AtomicReference<V> mResult = new AtomicReference<V>();

    protected List<Runnable> mOtherListeners;

    public synchronized void addListener(Runnable listener) {
        if (mLatch.getCount() == 0) {
            notifyListener(listener);
        } else {
            if (mFirstListener == null) {
                mFirstListener = listener;
            } else {
                if (mOtherListeners == null) {
                    mOtherListeners = new ArrayList<Runnable>(1);
                }
                mOtherListeners.add(listener);
            }
        }
    }

    protected void notifyListener(Runnable l) {
        try {
            l.run();// only called once
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return mResult.get() != null;
    }

    public synchronized boolean done(V result) {
        if (mResult.compareAndSet(null, result)) {
            mLatch.countDown();
            if (mFirstListener != null) {
                notifyListener(mFirstListener);
                mFirstListener = null;
                if (mOtherListeners != null) {
                    for (Runnable r : mOtherListeners) {
                        notifyListener(r);
                    }
                    mOtherListeners = null;
                }
            }
            return true;
        }
        return false;
    }
}
