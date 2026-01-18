package net.momo.silo.interceptor.impl;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.silo.interceptor.Interceptor;
import net.momo.silo.interceptor.InterceptorChain;

import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;

/** Logs execution of operations with timing information. */
public final class LoggingInterceptor<C, R> implements Interceptor<C, R> {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0003-000000000001");

    private final Function<C, String> contextDescriptor;
    private final Function<R, String> resultDescriptor;
    private final Level level;
    private final long slowThresholdMs;

    private LoggingInterceptor(Builder<C, R> builder) {
        this.contextDescriptor = builder.contextDescriptor;
        this.resultDescriptor = builder.resultDescriptor;
        this.level = builder.level;
        this.slowThresholdMs = builder.slowThresholdMs;
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
        return 1000; // Execute early to capture full timing
    }

    @Override
    public R intercept(C context, InterceptorChain<C, R> chain) {
        String desc = contextDescriptor.apply(context);
        long start = System.nanoTime();

        try {
            R result = chain.proceed(context);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            if (elapsedMs >= slowThresholdMs) {
                logger.at(Level.WARNING).log("[SLOW] %s took %dms (threshold: %dms) -> %s",
                    desc, elapsedMs, slowThresholdMs, resultDescriptor.apply(result));
            } else {
                logger.at(level).log("%s completed in %dms -> %s",
                    desc, elapsedMs, resultDescriptor.apply(result));
            }

            return result;
        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            logger.at(Level.SEVERE).withCause(e).log("%s failed after %dms", desc, elapsedMs);
            throw e;
        }
    }

    public static final class Builder<C, R> {
        private Function<C, String> contextDescriptor = Object::toString;
        private Function<R, String> resultDescriptor = Object::toString;
        private Level level = Level.FINE;
        private long slowThresholdMs = 100;

        /** How to describe the context in logs. */
        public Builder<C, R> contextDescriptor(Function<C, String> descriptor) {
            this.contextDescriptor = descriptor;
            return this;
        }

        /** How to describe the result in logs. */
        public Builder<C, R> resultDescriptor(Function<R, String> descriptor) {
            this.resultDescriptor = descriptor;
            return this;
        }

        /** Log level for normal operations. Default: FINE. */
        public Builder<C, R> level(Level level) {
            this.level = level;
            return this;
        }

        /** Threshold in ms to log as WARNING. Default: 100ms. */
        public Builder<C, R> slowThreshold(long ms) {
            this.slowThresholdMs = ms;
            return this;
        }

        public LoggingInterceptor<C, R> build() {
            return new LoggingInterceptor<>(this);
        }
    }
}
