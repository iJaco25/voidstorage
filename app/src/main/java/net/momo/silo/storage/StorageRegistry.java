package net.momo.silo.storage;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/** Registry mapping UUIDs to Storage instances. Thread-safe. */
public final class StorageRegistry {

    private static final long DEFAULT_CAPACITY = 100_000;

    private final Map<UUID, Storage> storages = new ConcurrentHashMap<>();

    public Storage getOrCreate(UUID id) {
        return storages.computeIfAbsent(id, k -> new Storage(DEFAULT_CAPACITY));
    }

    public Storage getOrCreate(UUID id, long capacity) {
        return storages.computeIfAbsent(id, k -> new Storage(capacity));
    }

    public Storage getOrCreate(UUID id, BigInteger capacity) {
        return storages.computeIfAbsent(id, k -> new Storage(capacity));
    }

    public Optional<Storage> get(UUID id) {
        return Optional.ofNullable(storages.get(id));
    }

    public void register(UUID id, Storage storage) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(storage, "storage");
        storages.put(id, storage);
    }

    public boolean unregister(UUID id) {
        return storages.remove(id) != null;
    }

    public boolean exists(UUID id) {
        return storages.containsKey(id);
    }

    public void forEach(BiConsumer<UUID, Storage> consumer) {
        storages.forEach(consumer);
    }

    public int size() {
        return storages.size();
    }

    public void clear() {
        storages.clear();
    }
}
