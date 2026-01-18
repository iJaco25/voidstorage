package net.momo.silo.interceptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Registry and executor for interceptors. Thread-safe. */
public final class InterceptorRegistry<C, R> {

    private final Map<UUID, Interceptor<C, R>> interceptors = new ConcurrentHashMap<>();
    private volatile List<Interceptor<C, R>> sortedChain = List.of();

    public void register(Interceptor<C, R> interceptor) {
        interceptors.put(interceptor.id(), interceptor);
        rebuildChain();
    }

    public void unregister(UUID id) {
        interceptors.remove(id);
        rebuildChain();
    }

    public boolean isEmpty() {
        return interceptors.isEmpty();
    }

    public int size() {
        return interceptors.size();
    }

    /** Execute the interceptor chain with a terminal operation. */
    public R execute(C context, Function<C, R> terminal) {
        List<Interceptor<C, R>> chain = sortedChain;
        if (chain.isEmpty()) {
            return terminal.apply(context);
        }
        return new ChainExecutor<>(chain, terminal).proceed(context);
    }

    private void rebuildChain() {
        List<Interceptor<C, R>> list = new ArrayList<>(interceptors.values());
        list.sort(Comparator.<Interceptor<C, R>>comparingInt(i -> i.priority()).reversed());
        sortedChain = List.copyOf(list);
    }

    private static final class ChainExecutor<C, R> implements InterceptorChain<C, R> {
        private final List<Interceptor<C, R>> chain;
        private final Function<C, R> terminal;
        private int index = 0;

        ChainExecutor(List<Interceptor<C, R>> chain, Function<C, R> terminal) {
            this.chain = chain;
            this.terminal = terminal;
        }

        @Override
        public R proceed(C context) {
            if (index < chain.size()) {
                return chain.get(index++).intercept(context, this);
            }
            return terminal.apply(context);
        }
    }
}
