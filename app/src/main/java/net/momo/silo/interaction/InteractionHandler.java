package net.momo.silo.interaction;

import net.momo.platform.hytale.adapter.InteractionContextAdapter;

import java.util.UUID;

/** Contract for handling block/item interactions. Register with HandlerRegistry. */
public interface InteractionHandler {

    /** Unique identifier for this handler. */
    UUID id();

    /** Handles an interaction. Returns result indicating success/skip/failure. */
    InteractionResult handle(InteractionContextAdapter context);

    /** Priority for handler execution. Higher values execute first. Default is 0. */
    default int priority() {
        return 0;
    }
}
