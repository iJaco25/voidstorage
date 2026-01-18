package net.momo.silo.interceptor;

/** Chain for passing control to the next interceptor or terminal operation. */
public interface InterceptorChain<C, R> {

    /** Proceed to the next interceptor or terminal operation. */
    R proceed(C context);
}
