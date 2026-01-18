package net.momo.silo.interaction;

import net.momo.platform.hytale.adapter.InteractionContextAdapter;

import java.util.UUID;

/** Mutable context for interaction interception. Designed for object pooling. */
public final class MutableInteractionContext {

    private InteractionHandler handler;
    private InteractionContextAdapter adapter;
    private UUID playerId;

    public MutableInteractionContext() {}

    public MutableInteractionContext set(InteractionHandler handler, InteractionContextAdapter adapter, UUID playerId) {
        this.handler = handler;
        this.adapter = adapter;
        this.playerId = playerId;
        return this;
    }

    public void reset() {
        this.handler = null;
        this.adapter = null;
        this.playerId = null;
    }

    public InteractionHandler handler() { return handler; }
    public InteractionContextAdapter adapter() { return adapter; }
    public UUID playerId() { return playerId; }
}
