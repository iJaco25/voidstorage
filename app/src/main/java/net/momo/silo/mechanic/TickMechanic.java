package net.momo.silo.mechanic;

import net.momo.platform.hytale.adapter.WorldAdapter;

import java.util.UUID;

/** Contract for periodic game loop mechanics. Register with MechanicRunner. */
public interface TickMechanic {

    /** Unique identifier for this mechanic. */
    UUID id();

    /** Tick interval in milliseconds. */
    long intervalMs();

    /** Called each tick for each loaded world. */
    void tick(WorldAdapter world);

    /** Called when mechanic starts. Use for initialization. */
    default void onStart() {}

    /** Called when mechanic stops. Use for cleanup. */
    default void onStop() {}

    /** Whether this mechanic should run. Checked before each tick. */
    default boolean isEnabled() {
        return true;
    }

    /** Initial delay before first tick in milliseconds. Default is intervalMs(). */
    default long initialDelayMs() {
        return intervalMs();
    }
}
