package net.momo.platform.hytale.adapter;

import net.momo.silo.util.Position;

import javax.annotation.Nullable;
import java.util.UUID;

/** Adapter interface for interaction context from SimpleInteraction. */
public interface InteractionContextAdapter {
    UUID getPlayerId();
    @Nullable Position getTargetBlockPosition();
    @Nullable Position getPlayerPosition();
    WorldAdapter getWorld();
    InventoryAdapter getInventory();
    boolean isFirstRun();
}
