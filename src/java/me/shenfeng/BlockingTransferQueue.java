package me.shenfeng;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class BlockingTransferQueue<T extends ResponseFuture<?>> {
    private final Semaphore mSemaphore;
    private final Queue<T> mDones;
    private final Queue<T> mPendings;

    public BlockingTransferQueue(int maxPermits) {
        mSemaphore = new Semaphore(maxPermits);
        mDones = new LinkedList<T>();
        mPendings = new LinkedList<T>();
    }

    public int pendingSize() {
        return mPendings.size();
    }

    public T take() throws InterruptedException {
        while (true) {
            synchronized (mDones) {
                if (mDones.size() > 0) {
                    mSemaphore.release();
                    return mDones.poll();
                }
                mDones.wait(200);// wait for timeout or success
            }

            synchronized (mPendings) {
                final Iterator<T> iterator = mPendings.iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().isTimeout()) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void put(T future) throws InterruptedException {
        mSemaphore.acquire();
        synchronized (mPendings) {
            mPendings.add(future);
        }
    }

    public void done(T future) {
        synchronized (mDones) {
            synchronized (mPendings) {
                mPendings.remove(future);// TODO, maybe time consuming
                mDones.add(future); // add last
                mDones.notify();
            }
        }
    }
}
