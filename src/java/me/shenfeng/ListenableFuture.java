package me.shenfeng;

import java.util.concurrent.Future;

public interface ListenableFuture<V> extends Future<V> {

    static interface Listener<V> {
        void run(ListenableFuture<V> t, V result);
    }

    void done(V result);

    void touch();

    void abort(Throwable t);

    ListenableFuture<V> addistener(Listener<V> listener);

}
