package net.momo.voidstorage.internal.essence;

import com.hypixel.hytale.logger.HytaleLogger;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Tracks orphaned storages (anchors broken but storage preserved via VoidEssence). */
public final class OrphanedStorageRegistry {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final Map<UUID, OrphanedStorage> orphans = new ConcurrentHashMap<>();
    private Duration retentionPeriod = Duration.ofDays(30);

    /** Record of an orphaned storage. */
    public record OrphanedStorage(
        UUID storageId,
        long orphanedAt
    ) {}

    /** Sets how long orphaned storages are kept before cleanup. */
    public void setRetentionPeriod(Duration period) {
        this.retentionPeriod = period;
        logger.at(Level.INFO).log("Orphaned storage retention set to %d days", period.toDays());
    }

    public Duration getRetentionPeriod() {
        return retentionPeriod;
    }

    /** Marks a storage as orphaned (anchor was broken, VoidEssence given to player). */
    public void markOrphaned(UUID storageId) {
        orphans.put(storageId, new OrphanedStorage(storageId, System.currentTimeMillis()));
        logger.at(Level.INFO).log("Storage %s marked as orphaned", storageId);
    }

    /** Registers an orphaned storage (for persistence loading). */
    public void register(OrphanedStorage orphan) {
        orphans.put(orphan.storageId(), orphan);
    }

    /** Reclaims an orphaned storage (player used VoidEssence on new anchor). */
    public boolean reclaim(UUID storageId) {
        OrphanedStorage removed = orphans.remove(storageId);
        if (removed != null) {
            logger.at(Level.INFO).log("Storage %s reclaimed from orphan state", storageId);
            return true;
        }
        return false;
    }

    /** Checks if a storage is orphaned. */
    public boolean isOrphaned(UUID storageId) {
        return orphans.containsKey(storageId);
    }

    /** Gets orphan info if exists. */
    public Optional<OrphanedStorage> get(UUID storageId) {
        return Optional.ofNullable(orphans.get(storageId));
    }

    /** Returns all orphaned storages. */
    public Iterable<OrphanedStorage> getAll() {
        return orphans.values();
    }

    /** Cleans up expired orphaned storages. Returns IDs of cleaned up storages. */
    public java.util.List<UUID> cleanupExpired() {
        long now = System.currentTimeMillis();
        long retentionMs = retentionPeriod.toMillis();

        java.util.List<UUID> expired = new java.util.ArrayList<>();

        orphans.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().orphanedAt();
            if (age > retentionMs) {
                expired.add(entry.getKey());
                logger.at(Level.INFO).log("Orphaned storage %s expired after %d days",
                    entry.getKey(), Duration.ofMillis(age).toDays());
                return true;
            }
            return false;
        });

        return expired;
    }

    public int size() {
        return orphans.size();
    }
}
