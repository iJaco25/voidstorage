package net.momo.voidstorage.internal.anchor;

import net.momo.voidstorage.internal.connector.StorageResolver;
import net.momo.silo.storage.StorageRegistry;
import net.momo.silo.storage.Storage;
import net.momo.silo.util.Position;

import java.util.Optional;
import java.util.UUID;

/** Resolves storage by finding the nearest anchor within range. World scope behavior. */
public final class AnchorStorageResolver implements StorageResolver {

    private final AnchorRegistry anchorRegistry;
    private final StorageRegistry storageRegistry;

    public AnchorStorageResolver(AnchorRegistry anchorRegistry, StorageRegistry storageRegistry) {
        this.anchorRegistry = anchorRegistry;
        this.storageRegistry = storageRegistry;
    }

    @Override
    public Optional<Storage> resolve(UUID playerId, Position position) {
        if (position == null) {
            return Optional.empty();
        }
        return findNearestAnchor(position)
            .flatMap(anchor -> storageRegistry.get(anchor.id()));
    }

    /** Finds the nearest anchor within access range of the given position. */
    public Optional<StorageAnchor> findNearestAnchor(Position position) {
        StorageAnchor nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (StorageAnchor anchor : anchorRegistry.getAll()) {
            double distance = calculateDistance(position, anchor.position());
            int range = anchor.accessRange();

            if (distance <= range && distance < nearestDistance) {
                nearest = anchor;
                nearestDistance = distance;
            }
        }

        return Optional.ofNullable(nearest);
    }

    /** Returns the anchor registry. */
    public AnchorRegistry anchorRegistry() {
        return anchorRegistry;
    }

    private double calculateDistance(Position a, Position b) {
        int dx = a.x() - b.x();
        int dy = a.y() - b.y();
        int dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
