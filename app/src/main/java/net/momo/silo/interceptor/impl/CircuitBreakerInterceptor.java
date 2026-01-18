package net.momo.silo.interceptor.impl;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.silo.interceptor.Interceptor;
import net.momo.silo.interceptor.InterceptorChain;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Circuit breaker that stops calling failing operations after threshold failures. */
public final class CircuitBreakerInterceptor<C, R> implements Interceptor<C, R> {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0003-000000000002");

    private final int failureThreshold;
    private final long resetTimeoutMs;
    private final Supplier<R> fallbackResult;
    private final Function<C, String> keyExtractor;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private volatile State state = State.CLOSED;

    private enum State {
        CLOSED,      // Normal operation
        OPEN,        // Rejecting calls
        HALF_OPEN    // Testing if service recovered
    }

    private CircuitBreakerInterceptor(Builder<C, R> builder) {
        this.failureThreshold = builder.failureThreshold;
        this.resetTimeoutMs = builder.resetTimeoutMs;
        this.fallbackResult = builder.fallbackResult;
        this.keyExtractor = builder.keyExtractor;
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
        return 900; // Execute early to short-circuit before expensive operations
    }

    @Override
    public R intercept(C context, InterceptorChain<C, R> chain) {
        if (shouldReject()) {
            String key = keyExtractor.apply(context);
            logger.at(Level.WARNING).log("Circuit OPEN for %s, returning fallback", key);
            return fallbackResult.get();
        }

        try {
            R result = chain.proceed(context);
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    private boolean shouldReject() {
        if (state == State.CLOSED) {
            return false;
        }

        if (state == State.OPEN) {
            long elapsed = System.currentTimeMillis() - lastFailureTime.get();
            if (elapsed >= resetTimeoutMs) {
                state = State.HALF_OPEN;
                logger.at(Level.INFO).log("Circuit entering HALF_OPEN state");
                return false; // Allow one request through
            }
            return true;
        }

        // HALF_OPEN - allow request
        return false;
    }

    private void onSuccess() {
        if (state != State.CLOSED) {
            logger.at(Level.INFO).log("Circuit CLOSED after successful request");
            state = State.CLOSED;
            failureCount.set(0);
        }
    }

    private void onFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int failures = failureCount.incrementAndGet();

        if (state == State.HALF_OPEN) {
            state = State.OPEN;
            logger.at(Level.WARNING).log("Circuit OPEN again after HALF_OPEN failure");
        } else if (failures >= failureThreshold) {
            state = State.OPEN;
            logger.at(Level.WARNING).log("Circuit OPEN after %d failures", failures);
        }
    }

    /** Returns current circuit state. */
    public State currentState() {
        return state;
    }

    /** Returns current failure count. */
    public int failureCount() {
        return failureCount.get();
    }

    public static final class Builder<C, R> {
        private int failureThreshold = 5;
        private long resetTimeoutMs = 30_000;
        private Supplier<R> fallbackResult = () -> null;
        private Function<C, String> keyExtractor = Object::toString;

        /** Number of failures before opening circuit. Default: 5. */
        public Builder<C, R> failureThreshold(int threshold) {
            this.failureThreshold = threshold;
            return this;
        }

        /** Time in ms before attempting to close circuit. Default: 30000. */
        public Builder<C, R> resetTimeout(long ms) {
            this.resetTimeoutMs = ms;
            return this;
        }

        /** Result to return when circuit is open. */
        public Builder<C, R> fallback(Supplier<R> fallback) {
            this.fallbackResult = fallback;
            return this;
        }

        /** How to identify the context for logging. */
        public Builder<C, R> keyExtractor(Function<C, String> extractor) {
            this.keyExtractor = extractor;
            return this;
        }

        public CircuitBreakerInterceptor<C, R> build() {
            return new CircuitBreakerInterceptor<>(this);
        }
    }
}
