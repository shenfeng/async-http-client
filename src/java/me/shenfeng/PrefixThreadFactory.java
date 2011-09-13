package me.shenfeng;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PrefixThreadFactory implements ThreadFactory {

    private final String mPrefix;
    private final AtomicInteger mNum = new AtomicInteger(0);

    public PrefixThreadFactory(String prefix) {
        mPrefix = prefix;
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, mPrefix + "#" + mNum.incrementAndGet());
        return t;
    }
}