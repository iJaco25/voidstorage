package net.momo.silo.mechanic;

import net.momo.platform.hytale.adapter.WorldAdapter;

/** Mutable context for tick mechanic interception. Designed for object pooling. */
public final class MutableTickContext {

    private TickMechanic mechanic;
    private WorldAdapter world;

    public MutableTickContext() {}

    public MutableTickContext set(TickMechanic mechanic, WorldAdapter world) {
        this.mechanic = mechanic;
        this.world = world;
        return this;
    }

    public void reset() {
        this.mechanic = null;
        this.world = null;
    }

    public TickMechanic mechanic() { return mechanic; }
    public WorldAdapter world() { return world; }
}
