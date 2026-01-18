package net.momo.silo.interceptor.impl;

import net.momo.silo.interceptor.Interceptor;
import net.momo.silo.interceptor.InterceptorChain;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/** Collects execution metrics: count, latency, errors. */
public final class MetricsInterceptor<C, R> implements Interceptor<C, R> {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0003-000000000004");

    private final Function<C, String> metricKeyExtractor;
    private final Map<String, OperationMetrics> metrics = new ConcurrentHashMap<>();

    private MetricsInterceptor(Builder<C, R> builder) {
        this.metricKeyExtractor = builder.metricKeyExtractor;
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
        return 950; // Execute early to capture accurate timing
    }

    @Override
    public R intercept(C context, InterceptorChain<C, R> chain) {
        String key = metricKeyExtractor.apply(context);
        OperationMetrics opMetrics = metrics.computeIfAbsent(key, k -> new OperationMetrics());

        long start = System.nanoTime();
        try {
            R result = chain.proceed(context);
            long elapsed = System.nanoTime() - start;
            opMetrics.recordSuccess(elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.nanoTime() - start;
            opMetrics.recordError(elapsed);
            throw e;
        }
    }

    /** Returns metrics for a specific operation key. */
    public OperationMetrics getMetrics(String key) {
        return metrics.get(key);
    }

    /** Returns all collected metrics. */
    public Map<String, OperationMetrics> getAllMetrics() {
        return Map.copyOf(metrics);
    }

    /** Resets all metrics. */
    public void reset() {
        metrics.clear();
    }

    /** Metrics for a single operation type. Thread-safe. */
    public static final class OperationMetrics {
        private final LongAdder totalCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final LongAdder totalLatencyNanos = new LongAdder();
        private final AtomicLong minLatencyNanos = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxLatencyNanos = new AtomicLong(0);

        void recordSuccess(long latencyNanos) {
            totalCount.increment();
            recordLatency(latencyNanos);
        }

        void recordError(long latencyNanos) {
            totalCount.increment();
            errorCount.increment();
            recordLatency(latencyNanos);
        }

        private void recordLatency(long latencyNanos) {
            totalLatencyNanos.add(latencyNanos);
            updateMin(latencyNanos);
            updateMax(latencyNanos);
        }

        private void updateMin(long value) {
            long current;
            do {
                current = minLatencyNanos.get();
                if (value >= current) return;
            } while (!minLatencyNanos.compareAndSet(current, value));
        }

        private void updateMax(long value) {
            long current;
            do {
                current = maxLatencyNanos.get();
                if (value <= current) return;
            } while (!maxLatencyNanos.compareAndSet(current, value));
        }

        public long totalCount() {
            return totalCount.sum();
        }

        public long successCount() {
            return totalCount.sum() - errorCount.sum();
        }

        public long errorCount() {
            return errorCount.sum();
        }

        public double errorRate() {
            long total = totalCount.sum();
            return total == 0 ? 0.0 : (double) errorCount.sum() / total;
        }

        public long minLatencyMs() {
            long min = minLatencyNanos.get();
            return min == Long.MAX_VALUE ? 0 : min / 1_000_000;
        }

        public long maxLatencyMs() {
            return maxLatencyNanos.get() / 1_000_000;
        }

        public double avgLatencyMs() {
            long total = totalCount.sum();
            return total == 0 ? 0.0 : (double) totalLatencyNanos.sum() / total / 1_000_000;
        }

        @Override
        public String toString() {
            return String.format(
                "count=%d, errors=%d (%.1f%%), latency=[min=%dms, avg=%.1fms, max=%dms]",
                totalCount(), errorCount(), errorRate() * 100,
                minLatencyMs(), avgLatencyMs(), maxLatencyMs()
            );
        }
    }

    public static final class Builder<C, R> {
        private Function<C, String> metricKeyExtractor = Object::toString;

        /** Extracts the metric key from context (e.g., handler name). */
        public Builder<C, R> keyExtractor(Function<C, String> extractor) {
            this.metricKeyExtractor = extractor;
            return this;
        }

        public MetricsInterceptor<C, R> build() {
            return new MetricsInterceptor<>(this);
        }
    }
}
