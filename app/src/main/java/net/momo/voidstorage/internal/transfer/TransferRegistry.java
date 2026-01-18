package net.momo.voidstorage.internal.transfer;

import net.momo.silo.util.Position;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/** Registry for Transfers. Thread-safe with sharded locks for scalability. */
public final class TransferRegistry {

    private static final int SHARD_COUNT = 16;
    private static final int SHARD_MASK = SHARD_COUNT - 1;

    private final Shard[] shards;
    private final Map<UUID, Transfer> byId = new ConcurrentHashMap<>();
    private final Map<Long, Transfer> byPosition = new ConcurrentHashMap<>();

    public TransferRegistry() {
        this.shards = new Shard[SHARD_COUNT];
        for (int i = 0; i < SHARD_COUNT; i++) {
            shards[i] = new Shard();
        }
    }

    public void register(Transfer transfer) {
        Objects.requireNonNull(transfer, "transfer");
        Shard shard = shardFor(transfer.anchorId());
        long stamp = shard.lock.writeLock();
        try {
            byId.put(transfer.id(), transfer);
            byPosition.put(transfer.position().toKey(), transfer);
            shard.byAnchorId.computeIfAbsent(transfer.anchorId(), k -> ConcurrentHashMap.newKeySet())
                            .add(transfer.id());
        } finally {
            shard.lock.unlockWrite(stamp);
        }
    }

    public boolean unregister(UUID id) {
        Transfer transfer = byId.get(id);
        if (transfer == null) {
            return false;
        }
        Shard shard = shardFor(transfer.anchorId());
        long stamp = shard.lock.writeLock();
        try {
            Transfer removed = byId.remove(id);
            if (removed != null) {
                byPosition.remove(removed.position().toKey());
                Set<UUID> anchorTransfers = shard.byAnchorId.get(removed.anchorId());
                if (anchorTransfers != null) {
                    anchorTransfers.remove(id);
                    if (anchorTransfers.isEmpty()) {
                        shard.byAnchorId.remove(removed.anchorId());
                    }
                }
                return true;
            }
            return false;
        } finally {
            shard.lock.unlockWrite(stamp);
        }
    }

    public int unregisterByAnchor(UUID anchorId) {
        Shard shard = shardFor(anchorId);
        long stamp = shard.lock.writeLock();
        try {
            Set<UUID> transferIds = shard.byAnchorId.remove(anchorId);
            if (transferIds == null || transferIds.isEmpty()) {
                return 0;
            }

            int count = 0;
            for (UUID transferId : transferIds) {
                Transfer removed = byId.remove(transferId);
                if (removed != null) {
                    byPosition.remove(removed.position().toKey());
                    count++;
                }
            }
            return count;
        } finally {
            shard.lock.unlockWrite(stamp);
        }
    }

    public void update(Transfer transfer) {
        if (byId.containsKey(transfer.id())) {
            byId.put(transfer.id(), transfer);
            byPosition.put(transfer.position().toKey(), transfer);
        }
    }

    public Optional<Transfer> get(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<Transfer> getAtPosition(Position pos) {
        return Optional.ofNullable(byPosition.get(pos.toKey()));
    }

    public List<Transfer> getByAnchor(UUID anchorId) {
        Shard shard = shardFor(anchorId);
        long stamp = shard.lock.tryOptimisticRead();
        Set<UUID> transferIds = shard.byAnchorId.get(anchorId);
        if (!shard.lock.validate(stamp)) {
            stamp = shard.lock.readLock();
            try {
                transferIds = shard.byAnchorId.get(anchorId);
            } finally {
                shard.lock.unlockRead(stamp);
            }
        }
        if (transferIds == null || transferIds.isEmpty()) {
            return Collections.emptyList();
        }
        return transferIds.stream()
            .map(byId::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<Transfer> getByMode(TransferMode mode) {
        return byId.values().stream()
            .filter(t -> t.mode() == mode)
            .collect(Collectors.toList());
    }

    public Collection<Transfer> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() {
        return byId.size();
    }

    public void clear() {
        for (Shard shard : shards) {
            long stamp = shard.lock.writeLock();
            try {
                shard.byAnchorId.clear();
            } finally {
                shard.lock.unlockWrite(stamp);
            }
        }
        byId.clear();
        byPosition.clear();
    }

    private Shard shardFor(UUID anchorId) {
        return shards[(anchorId.hashCode() & 0x7FFFFFFF) & SHARD_MASK];
    }

    private static final class Shard {
        final StampedLock lock = new StampedLock();
        final Map<UUID, Set<UUID>> byAnchorId = new HashMap<>();
    }
}
