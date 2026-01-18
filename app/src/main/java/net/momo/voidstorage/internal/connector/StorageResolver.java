package net.momo.voidstorage.internal.connector;

import net.momo.silo.storage.Storage;
import net.momo.silo.util.Position;

import java.util.Optional;
import java.util.UUID;

/** Resolves storage for a player. Implementations determine scope behavior. */
@FunctionalInterface
public interface StorageResolver {

    /**
     * Resolves storage for the given player.
     *
     * @param playerId the player requesting storage access
     * @param position the player's position (may be null for non-world scopes)
     * @return the resolved storage, or empty if no storage available
     */
    Optional<Storage> resolve(UUID playerId, Position position);
}
