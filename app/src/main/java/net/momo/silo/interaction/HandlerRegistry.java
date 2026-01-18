package net.momo.silo.interaction;

import com.hypixel.hytale.logger.HytaleLogger;
import net.momo.platform.hytale.adapter.InteractionContextAdapter;
import net.momo.silo.interceptor.InterceptorRegistry;
import net.momo.silo.util.ObjectPool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Registry for interaction handlers with rate limiting and interceptor support. */
public final class HandlerRegistry {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final long RATE_LIMIT_MS = 200;
    private static final int CONTEXT_POOL_SIZE = 64;

    private final Map<UUID, InteractionHandler> handlers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastInteraction = new ConcurrentHashMap<>();
    private final InterceptorRegistry<MutableInteractionContext, InteractionResult> interceptors = new InterceptorRegistry<>();
    private final ObjectPool<MutableInteractionContext> contextPool = new ObjectPool<>(
        MutableInteractionContext::new,
        MutableInteractionContext::reset,
        CONTEXT_POOL_SIZE
    );

    /** Returns the interceptor registry for adding custom interceptors. */
    public InterceptorRegistry<MutableInteractionContext, InteractionResult> interceptors() {
        return interceptors;
    }

    /** Registers a handler. Overwrites any existing handler with the same ID. */
    public void register(InteractionHandler handler) {
        handlers.put(handler.id(), handler);
        logger.at(Level.FINE).log("Registered handler: %s", handler.id());
    }

    /** Unregisters a handler by ID. */
    public void unregister(UUID id) {
        handlers.remove(id);
    }

    /** Gets a handler by ID. */
    public Optional<InteractionHandler> get(UUID id) {
        return Optional.ofNullable(handlers.get(id));
    }

    /** Gets all handlers sorted by priority (highest first). */
    public List<InteractionHandler> getAll() {
        List<InteractionHandler> list = new ArrayList<>(handlers.values());
        list.sort(Comparator.comparingInt(InteractionHandler::priority).reversed());
        return list;
    }

    /** Dispatches an interaction to a specific handler with rate limiting. */
    public InteractionResult dispatch(UUID handlerId, InteractionContextAdapter context) {
        InteractionHandler handler = handlers.get(handlerId);
        if (handler == null) {
            return InteractionResult.skipped("Handler not found: " + handlerId);
        }
        return dispatch(handler, context);
    }

    /** Dispatches an interaction to a handler with rate limiting and interceptors. */
    public InteractionResult dispatch(InteractionHandler handler, InteractionContextAdapter context) {
        if (!context.isFirstRun()) {
            return InteractionResult.skipped("Not first run");
        }

        UUID playerId = context.getPlayerId();
        if (playerId == null) {
            return InteractionResult.skipped("No player");
        }

        if (!checkRateLimit(playerId)) {
            return InteractionResult.skipped("Rate limited");
        }

        MutableInteractionContext ctx = contextPool.acquire().set(handler, context, playerId);
        try {
            return interceptors.execute(ctx, this::executeHandler);
        } finally {
            contextPool.release(ctx);
        }
    }

    private InteractionResult executeHandler(MutableInteractionContext ctx) {
        try {
            return ctx.handler().handle(ctx.adapter());
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("Error in handler %s", ctx.handler().id());
            return InteractionResult.failed(e.getMessage());
        }
    }

    private boolean checkRateLimit(UUID playerId) {
        long now = System.currentTimeMillis();
        Long last = lastInteraction.get(playerId);

        if (last != null && now - last < RATE_LIMIT_MS) {
            return false;
        }

        lastInteraction.put(playerId, now);
        return true;
    }

    /** Cleans up stale rate limit entries. */
    public void cleanup() {
        long threshold = System.currentTimeMillis() - 60_000;
        lastInteraction.entrySet().removeIf(e -> e.getValue() < threshold);
    }
}
