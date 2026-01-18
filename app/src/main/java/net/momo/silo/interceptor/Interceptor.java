package net.momo.silo.interceptor;

import java.util.UUID;

/** Intercepts operations at defined points. Implementations must be thread-safe. */
public interface Interceptor<C, R> {

    /** Unique identifier for this interceptor. */
    UUID id();

    /** Intercepts the operation. Call chain.proceed() to continue, or return early to short-circuit. */
    R intercept(C context, InterceptorChain<C, R> chain);

    /** Priority for execution order. Higher values execute first. */
    default int priority() {
        return 0;
    }
}
