package net.momo.silo.mechanic;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import net.momo.platform.hytale.adapter.WorldAdapter;
import net.momo.platform.hytale.impl.HytaleWorldAdapter;
import net.momo.silo.interceptor.InterceptorRegistry;

import net.momo.silo.util.ObjectPool;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/** Schedules and runs tick mechanics with interceptor support. */
public final class MechanicRunner {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final int CONTEXT_POOL_SIZE = 32;

    private final Map<UUID, TickMechanic> mechanics = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicBoolean> running = new ConcurrentHashMap<>();
    private final InterceptorRegistry<MutableTickContext, Void> interceptors = new InterceptorRegistry<>();
    private final ObjectPool<MutableTickContext> contextPool = new ObjectPool<>(
        MutableTickContext::new,
        MutableTickContext::reset,
        CONTEXT_POOL_SIZE
    );
    private volatile boolean started = false;

    /** Returns the interceptor registry for adding custom interceptors. */
    public InterceptorRegistry<MutableTickContext, Void> interceptors() {
        return interceptors;
    }

    /** Registers a mechanic. Must be called before start(). */
    public void register(TickMechanic mechanic) {
        if (started) {
            throw new IllegalStateException("Cannot register mechanics after start()");
        }
        mechanics.put(mechanic.id(), mechanic);
        running.put(mechanic.id(), new AtomicBoolean(false));
        logger.at(Level.FINE).log("Registered mechanic: %s (interval=%dms)", mechanic.id(), mechanic.intervalMs());
    }

    /** Unregisters a mechanic. */
    public void unregister(UUID id) {
        mechanics.remove(id);
        running.remove(id);
        ScheduledFuture<?> task = tasks.remove(id);
        if (task != null) {
            task.cancel(false);
        }
    }

    /** Starts all registered mechanics. */
    public void start() {
        if (started) {
            return;
        }
        started = true;

        for (TickMechanic mechanic : mechanics.values()) {
            mechanic.onStart();
            scheduleMechanic(mechanic);
        }

        logger.at(Level.INFO).log("Started %d mechanics", mechanics.size());
    }

    private void scheduleMechanic(TickMechanic mechanic) {
        ScheduledFuture<?> task = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            () -> tickMechanic(mechanic),
            mechanic.initialDelayMs(),
            mechanic.intervalMs(),
            TimeUnit.MILLISECONDS
        );
        tasks.put(mechanic.id(), task);
    }

    private void tickMechanic(TickMechanic mechanic) {
        if (!mechanic.isEnabled()) {
            return;
        }

        AtomicBoolean isRunning = running.get(mechanic.id());
        if (isRunning == null || !isRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            for (World world : Universe.get().getWorlds().values()) {
                WorldAdapter adapter = new HytaleWorldAdapter(world);
                MutableTickContext ctx = contextPool.acquire().set(mechanic, adapter);
                try {
                    interceptors.execute(ctx, this::executeTick);
                } finally {
                    contextPool.release(ctx);
                }
            }
        } finally {
            isRunning.set(false);
        }
    }

    private Void executeTick(MutableTickContext ctx) {
        try {
            ctx.mechanic().tick(ctx.world());
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("Error in mechanic %s for world", ctx.mechanic().id());
        }
        return null;
    }

    /** Stops all mechanics. */
    public void stop() {
        for (ScheduledFuture<?> task : tasks.values()) {
            task.cancel(false);
        }
        tasks.clear();

        for (TickMechanic mechanic : mechanics.values()) {
            mechanic.onStop();
        }

        started = false;
        logger.at(Level.INFO).log("Stopped %d mechanics", mechanics.size());
    }

    /** Gets a mechanic by ID. */
    public TickMechanic get(UUID id) {
        return mechanics.get(id);
    }

    /** Returns the number of registered mechanics. */
    public int size() {
        return mechanics.size();
    }
}
