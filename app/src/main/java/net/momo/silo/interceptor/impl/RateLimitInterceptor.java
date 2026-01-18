package net.momo.silo.interceptor.impl;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.silo.interceptor.Interceptor;
import net.momo.silo.interceptor.InterceptorChain;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Rate limiter using sliding window algorithm. Limits per key (e.g., per player). */
public final class RateLimitInterceptor<C, R> implements Interceptor<C, R> {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0003-000000000003");

    private final int maxRequests;
    private final long windowMs;
    private final Function<C, String> keyExtractor;
    private final Supplier<R> rejectedResult;

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    private RateLimitInterceptor(Builder<C, R> builder) {
        this.maxRequests = builder.maxRequests;
        this.windowMs = builder.windowMs;
        this.keyExtractor = builder.keyExtractor;
        this.rejectedResult = builder.rejectedResult;
    }

    public static <C, R> Builder<C, R> builder() {
        return new Builder<>();
    }

    @Override
    public UUID id() {
        return ID;
    }

    @Override
    public int priority() {
        return 800; // Execute early to reject before expensive work
    }

    @Override
    public R intercept(C context, InterceptorChain<C, R> chain) {
        String key = keyExtractor.apply(context);
        RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> new RateLimitBucket());

        if (!bucket.tryAcquire()) {
            logger.at(Level.FINE).log("Rate limit exceeded for %s (%d/%d in %dms)",
                key, bucket.getCount(), maxRequests, windowMs);
            return rejectedResult.get();
        }

        return chain.proceed(context);
    }

    /** Cleans up expired buckets. Call periodically to prevent memory leaks. */
    public void cleanup() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> 
            now - entry.getValue().windowStart.get() > windowMs * 2);
    }

    /** Returns current request count for a key. */
    public int getCount(String key) {
        RateLimitBucket bucket = buckets.get(key);
        return bucket != null ? bucket.getCount() : 0;
    }

    private final class RateLimitBucket {
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger count = new AtomicInteger(0);

        boolean tryAcquire() {
            long now = System.currentTimeMillis();
            long start = windowStart.get();

            // Reset window if expired
            if (now - start >= windowMs) {
                if (windowStart.compareAndSet(start, now)) {
                    count.set(0);
                }
            }

            // Try to increment
            int current = count.get();
            if (current >= maxRequests) {
                return false;
            }

            return count.compareAndSet(current, current + 1) || tryAcquire();
        }

        int getCount() {
            long now = System.currentTimeMillis();
            if (now - windowStart.get() >= windowMs) {
                return 0;
            }
            return count.get();
        }
    }

    public static final class Builder<C, R> {
        private int maxRequests = 10;
        private long windowMs = 1000;
        private Function<C, String> keyExtractor = Object::toString;
        private Supplier<R> rejectedResult = () -> null;

        /** Max requests per window. Default: 10. */
        public Builder<C, R> maxRequests(int max) {
            this.maxRequests = max;
            return this;
        }

        /** Window size in ms. Default: 1000 (1 second). */
        public Builder<C, R> window(long ms) {
            this.windowMs = ms;
            return this;
        }

        /** Extracts the rate limit key from context (e.g., player UUID). */
        public Builder<C, R> keyExtractor(Function<C, String> extractor) {
            this.keyExtractor = extractor;
            return this;
        }

        /** Result to return when rate limited. */
        public Builder<C, R> rejectedResult(Supplier<R> result) {
            this.rejectedResult = result;
            return this;
        }

        public RateLimitInterceptor<C, R> build() {
            return new RateLimitInterceptor<>(this);
        }
    }
}
