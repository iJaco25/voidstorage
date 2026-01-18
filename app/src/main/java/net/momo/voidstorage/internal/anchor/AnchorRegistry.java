package net.momo.voidstorage.internal.anchor;

import net.momo.silo.util.Position;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

/** Registry for StorageAnchors. Thread-safe with sharded locks for scalability. */
public final class AnchorRegistry {

    private static final int SHARD_COUNT = 64;
    private static final int SHARD_MASK = SHARD_COUNT - 1;

    private final Shard[] shards;
    private final Map<UUID, StorageAnchor> byId = new ConcurrentHashMap<>();

    public AnchorRegistry() {
        this.shards = new Shard[SHARD_COUNT];
        for (int i = 0; i < SHARD_COUNT; i++) {
            shards[i] = new Shard();
        }
    }

    public void register(StorageAnchor anchor) {
        Objects.requireNonNull(anchor, "anchor");
        Shard shard = shardFor(anchor.position());
        long stamp = shard.lock.writeLock();
        try {
            byId.put(anchor.id(), anchor);
            shard.byPosition.put(anchor.position().toKey(), anchor);
        } finally {
            shard.lock.unlockWrite(stamp);
        }
    }

    public boolean unregister(UUID id) {
        StorageAnchor anchor = byId.get(id);
        if (anchor == null) {
            return false;
        }
        Shard shard = shardFor(anchor.position());
        long stamp = shard.lock.writeLock();
        try {
            StorageAnchor removed = byId.remove(id);
            if (removed != null) {
                shard.byPosition.remove(removed.position().toKey());
                return true;
            }
            return false;
        } finally {
            shard.lock.unlockWrite(stamp);
        }
    }

    public Optional<StorageAnchor> get(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<StorageAnchor> getAtPosition(Position pos) {
        Shard shard = shardFor(pos);
        long stamp = shard.lock.tryOptimisticRead();
        StorageAnchor anchor = shard.byPosition.get(pos.toKey());
        if (!shard.lock.validate(stamp)) {
            stamp = shard.lock.readLock();
            try {
                anchor = shard.byPosition.get(pos.toKey());
            } finally {
                shard.lock.unlockRead(stamp);
            }
        }
        return Optional.ofNullable(anchor);
    }

    public Collection<StorageAnchor> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() {
        return byId.size();
    }

    public void clear() {
        for (Shard shard : shards) {
            long stamp = shard.lock.writeLock();
            try {
                shard.byPosition.clear();
            } finally {
                shard.lock.unlockWrite(stamp);
            }
        }
        byId.clear();
    }

    private Shard shardFor(Position pos) {
        int chunkX = pos.x() >> 4;
        int chunkZ = pos.z() >> 4;
        int hash = chunkX * 31 + chunkZ;
        return shards[(hash & 0x7FFFFFFF) & SHARD_MASK];
    }

    private static final class Shard {
        final StampedLock lock = new StampedLock();
        final Map<Long, StorageAnchor> byPosition = new HashMap<>();
    }
}
