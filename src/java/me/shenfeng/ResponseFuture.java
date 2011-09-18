package me.shenfeng;

import java.util.concurrent.Future;

public interface ResponseFuture<V> extends Future<V> {

    boolean isTimeout();

    boolean done(V result);

    void touch();

    boolean abort(Throwable t);

    void addListener(Runnable listener);
}
