package net.momo.silo.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Simple thread-safe object pool for reducing GC pressure. */
public final class ObjectPool<T> {

    private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final Consumer<T> reset;
    private final int maxSize;

    public ObjectPool(Supplier<T> factory, Consumer<T> reset, int maxSize) {
        this.factory = factory;
        this.reset = reset;
        this.maxSize = maxSize;
    }

    public ObjectPool(Supplier<T> factory, int maxSize) {
        this(factory, obj -> {}, maxSize);
    }

    /** Acquires an object from the pool or creates a new one. */
    public T acquire() {
        T obj = pool.poll();
        return obj != null ? obj : factory.get();
    }

    /** Returns an object to the pool for reuse. */
    public void release(T obj) {
        if (obj == null) {
            return;
        }
        if (pool.size() < maxSize) {
            reset.accept(obj);
            pool.offer(obj);
        }
    }

    /** Returns the current pool size. */
    public int size() {
        return pool.size();
    }

    /** Clears all pooled objects. */
    public void clear() {
        pool.clear();
    }
}
